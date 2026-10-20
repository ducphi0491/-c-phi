@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package duc_phi.music

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayDeque
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Shared, lock-free controls read from ExoPlayer's audio thread. */
internal class MeloWaveDspSettings(
    normalizationEnabled: Boolean = false,
    targetLufs: Float = -14f,
    limiterEnabled: Boolean = true,
    limiterCeilingDb: Float = -1f,
) {
    @Volatile var normalizationEnabled: Boolean = normalizationEnabled
    @Volatile var targetLufs: Float = targetLufs
    @Volatile var limiterEnabled: Boolean = limiterEnabled
    @Volatile var limiterCeilingDb: Float = limiterCeilingDb
    @Volatile var maxNormalizationBoostDb: Float = 8f
}

internal data class MeloWaveDspSnapshot(
    val active: Boolean = false,
    val momentaryLufs: Float = -70f,
    val integratedLufs: Float = -70f,
    val normalizationGainDb: Float = 0f,
    val inputPeakDbfs: Float = -90f,
    val limiterReductionDb: Float = 0f,
)

internal class MeloWaveDspMetrics {
    @Volatile private var active = false
    @Volatile private var momentaryLufs = -70f
    @Volatile private var integratedLufs = -70f
    @Volatile private var normalizationGainDb = 0f
    @Volatile private var inputPeakDbfs = -90f
    @Volatile private var limiterReductionDb = 0f

    internal fun update(
        active: Boolean,
        momentaryLufs: Float,
        integratedLufs: Float,
        normalizationGainDb: Float,
        inputPeakDbfs: Float,
        limiterReductionDb: Float,
    ) {
        this.active = active
        this.momentaryLufs = momentaryLufs
        this.integratedLufs = integratedLufs
        this.normalizationGainDb = normalizationGainDb
        this.inputPeakDbfs = inputPeakDbfs
        this.limiterReductionDb = limiterReductionDb
    }

    internal fun reset() = update(false, -70f, -70f, 0f, -90f, 0f)

    fun snapshot(): MeloWaveDspSnapshot = MeloWaveDspSnapshot(
        active = active,
        momentaryLufs = momentaryLufs,
        integratedLufs = integratedLufs,
        normalizationGainDb = normalizationGainDb,
        inputPeakDbfs = inputPeakDbfs,
        limiterReductionDb = limiterReductionDb,
    )
}

/** One DSP chain belongs to exactly one ExoPlayer/AudioSink. */
internal class MeloWaveAudioDspChain(
    context: Context,
    settings: MeloWaveDspSettings,
) {
    val metrics = MeloWaveDspMetrics()
    private val processor = MeloWaveR128LimiterProcessor(settings, metrics)

    val renderersFactory = object : DefaultRenderersFactory(context) {
        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean,
        ): AudioSink = DefaultAudioSink.Builder(context)
            // User AudioProcessors are fed 16-bit PCM by DefaultAudioSink.
            .setEnableFloatOutput(false)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .setAudioProcessors(arrayOf<AudioProcessor>(processor))
            .build()
    }
}

/**
 * Real-time BS.1770/R128-style loudness measurement + smooth normalization and a look-ahead
 * dynamic peak limiter. The processor receives 16-bit PCM after Media3's ToInt16 processor.
 */
private class MeloWaveR128LimiterProcessor(
    private val settings: MeloWaveDspSettings,
    private val metrics: MeloWaveDspMetrics,
) : BaseAudioProcessor() {
    private var sampleRate = 48_000
    private var channelCount = 2
    private var filters: Array<KWeightingFilter> = emptyArray()

    private var stepFrames = 4_800
    private var stepFrameCount = 0
    private var stepEnergy = 0.0
    private val recentStepEnergies = ArrayDeque<Double>()
    private val gatedBlockEnergies = ArrayDeque<Double>()
    private var momentaryLufs = -70f
    private var integratedLufs = -70f

    private var targetNormalizationGainDb = 0f
    private var currentNormalizationGainDb = 0f
    private var normalizationGainLinear = 1f

    private var lookAheadFrames = 384
    private var ringCapacity = 385
    private var delayedSamples = FloatArray(ringCapacity * channelCount)
    private var delayedPeaks = FloatArray(ringCapacity)
    private var delayedSequence = LongArray(ringCapacity)
    private var writeFrame = 0
    private var readFrame = 0
    private var delayedFrameCount = 0
    private var sequenceCounter = 0L
    private data class PeakPoint(val sequence: Long, val peak: Float)
    private val peakDeque = ArrayDeque<PeakPoint>()

    private var limiterGain = 1f
    private var inputPeak = 0f
    private var lastPublishedFrames = 0L
    private var processedFrames = 0L

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount <= 0) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun onFlush() {
        sampleRate = inputAudioFormat.sampleRate.coerceAtLeast(8_000)
        channelCount = inputAudioFormat.channelCount.coerceAtLeast(1)
        filters = Array(channelCount) { KWeightingFilter(sampleRate.toDouble()) }
        stepFrames = max(1, sampleRate / 10)
        lookAheadFrames = max(1, (sampleRate * 0.008).toInt())
        ringCapacity = lookAheadFrames + 1
        delayedSamples = FloatArray(ringCapacity * channelCount)
        delayedPeaks = FloatArray(ringCapacity)
        delayedSequence = LongArray(ringCapacity)
        resetStreamState()
    }

    override fun onReset() {
        filters = emptyArray()
        resetStreamState()
    }

    private fun resetStreamState() {
        filters.forEach { it.reset() }
        stepFrameCount = 0
        stepEnergy = 0.0
        recentStepEnergies.clear()
        gatedBlockEnergies.clear()
        momentaryLufs = -70f
        integratedLufs = -70f
        targetNormalizationGainDb = 0f
        currentNormalizationGainDb = 0f
        normalizationGainLinear = 1f
        writeFrame = 0
        readFrame = 0
        delayedFrameCount = 0
        sequenceCounter = 0L
        peakDeque.clear()
        limiterGain = 1f
        inputPeak = 0f
        lastPublishedFrames = 0L
        processedFrames = 0L
        metrics.reset()
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        inputBuffer.order(ByteOrder.nativeOrder())
        val bytesPerFrame = channelCount * 2
        val inputFrames = inputBuffer.remaining() / bytesPerFrame
        if (inputFrames <= 0) return
        val maxOutputFrames = inputFrames
        val output = replaceOutputBuffer(maxOutputFrames * bytesPerFrame)
        val frame = FloatArray(channelCount)
        repeat(inputFrames) {
            var framePeak = 0f
            var weightedEnergy = 0.0
            for (channel in 0 until channelCount) {
                val sample = inputBuffer.short / 32768f
                frame[channel] = sample
                framePeak = max(framePeak, kotlin.math.abs(sample))
                val weighted = filters[channel].process(sample.toDouble())
                val channelWeight = when {
                    channelCount <= 3 -> 1.0
                    channel == 3 -> 0.0 // LFE is excluded by BS.1770.
                    channel >= 4 -> 1.41 // Surround/back channels.
                    else -> 1.0
                }
                weightedEnergy += weighted * weighted * channelWeight
            }
            updateLoudness(weightedEnergy)
            updateNormalizationGain()

            val preGainPeak = framePeak * normalizationGainLinear
            inputPeak = max(inputPeak * 0.9995f, preGainPeak)
            val sequence = sequenceCounter++
            val sampleOffset = writeFrame * channelCount
            for (channel in 0 until channelCount) {
                delayedSamples[sampleOffset + channel] = frame[channel] * normalizationGainLinear
            }
            delayedPeaks[writeFrame] = preGainPeak
            delayedSequence[writeFrame] = sequence
            while (peakDeque.isNotEmpty() && peakDeque.last().peak <= preGainPeak) peakDeque.removeLast()
            peakDeque.addLast(PeakPoint(sequence, preGainPeak))
            writeFrame = (writeFrame + 1) % ringCapacity
            delayedFrameCount++

            if (delayedFrameCount > lookAheadFrames) {
                writeDelayedFrame(output)
            }
            processedFrames++
            maybePublishMetrics()
        }
        output.flip()
    }

    override fun onQueueEndOfStream() {
        if (delayedFrameCount <= 0) return
        val output = replaceOutputBuffer(delayedFrameCount * channelCount * 2)
        while (delayedFrameCount > 0) writeDelayedFrame(output)
        output.flip()
        maybePublishMetrics(force = true)
    }

    private fun writeDelayedFrame(output: ByteBuffer) {
        val maxPeak = peakDeque.firstOrNull()?.peak ?: delayedPeaks[readFrame]
        val ceilingLinear = 10.0.pow(settings.limiterCeilingDb.coerceIn(-6f, -0.1f) / 20.0).toFloat()
        val targetGain = if (settings.limiterEnabled && maxPeak > ceilingLinear && maxPeak > 0f) {
            (ceilingLinear / maxPeak).coerceIn(0.05f, 1f)
        } else 1f
        val attackSeconds = 0.002f
        val releaseSeconds = 0.080f
        val time = if (targetGain < limiterGain) attackSeconds else releaseSeconds
        val coefficient = exp(-1.0 / max(1.0, sampleRate.toDouble() * time.toDouble())).toFloat()
        limiterGain = targetGain + (limiterGain - targetGain) * coefficient
        if (targetGain < limiterGain && maxPeak * limiterGain > ceilingLinear) {
            // Look-ahead normally makes the envelope reach the target in time; this final clamp
            // guarantees that no sample exceeds the selected ceiling.
            limiterGain = min(limiterGain, ceilingLinear / max(maxPeak, 1e-9f))
        }

        val offset = readFrame * channelCount
        for (channel in 0 until channelCount) {
            val value = (delayedSamples[offset + channel] * limiterGain).coerceIn(-1f, 0.999969f)
            output.putShort((value * 32767f).toInt().toShort())
        }
        val outputSequence = delayedSequence[readFrame]
        if (peakDeque.isNotEmpty() && peakDeque.first().sequence <= outputSequence) peakDeque.removeFirst()
        readFrame = (readFrame + 1) % ringCapacity
        delayedFrameCount--
    }

    private fun updateLoudness(frameEnergy: Double) {
        stepEnergy += frameEnergy
        stepFrameCount++
        if (stepFrameCount < stepFrames) return
        val averageStepEnergy = stepEnergy / stepFrameCount.toDouble()
        recentStepEnergies.addLast(averageStepEnergy)
        while (recentStepEnergies.size > 4) recentStepEnergies.removeFirst()
        if (recentStepEnergies.size == 4) {
            val blockEnergy = recentStepEnergies.sum() / 4.0
            val blockLufs = energyToLufs(blockEnergy)
            momentaryLufs = blockLufs
            if (blockLufs > -70f) {
                gatedBlockEnergies.addLast(blockEnergy)
                if (gatedBlockEnergies.size > 18_000) gatedBlockEnergies.removeFirst()
                integratedLufs = calculateIntegratedLufs()
                if (gatedBlockEnergies.size >= 10 && settings.normalizationEnabled) {
                    targetNormalizationGainDb = (settings.targetLufs.coerceIn(-23f, -9f) - integratedLufs)
                        .coerceIn(-12f, settings.maxNormalizationBoostDb.coerceIn(0f, 12f))
                } else if (!settings.normalizationEnabled) {
                    targetNormalizationGainDb = 0f
                }
            }
        }
        stepEnergy = 0.0
        stepFrameCount = 0
    }

    private fun calculateIntegratedLufs(): Float {
        if (gatedBlockEnergies.isEmpty()) return -70f
        val absolute = gatedBlockEnergies.filter { energyToLufs(it) > -70f }
        if (absolute.isEmpty()) return -70f
        val ungatedMean = absolute.average()
        val relativeGate = energyToLufs(ungatedMean) - 10f
        val finalGate = max(-70f, relativeGate)
        val relative = absolute.filter { energyToLufs(it) > finalGate }
        return energyToLufs(if (relative.isEmpty()) ungatedMean else relative.average())
    }

    private fun updateNormalizationGain() {
        if (!settings.normalizationEnabled) targetNormalizationGainDb = 0f
        val target = targetNormalizationGainDb
        val timeSeconds = if (target < currentNormalizationGainDb) 0.35f else 2.5f
        val coefficient = exp(-1.0 / max(1.0, sampleRate.toDouble() * timeSeconds.toDouble())).toFloat()
        currentNormalizationGainDb = target + (currentNormalizationGainDb - target) * coefficient
        normalizationGainLinear = 10.0.pow(currentNormalizationGainDb / 20.0).toFloat()
    }

    private fun maybePublishMetrics(force: Boolean = false) {
        val interval = max(1, sampleRate / 4)
        if (!force && processedFrames - lastPublishedFrames < interval) return
        lastPublishedFrames = processedFrames
        val reductionDb = if (limiterGain >= 0.99999f) 0f else (-20f * log10(max(limiterGain, 1e-6f)))
        val peakDb = if (inputPeak <= 1e-9f) -90f else 20f * log10(inputPeak)
        metrics.update(
            active = true,
            momentaryLufs = momentaryLufs,
            integratedLufs = integratedLufs,
            normalizationGainDb = currentNormalizationGainDb,
            inputPeakDbfs = peakDb,
            limiterReductionDb = reductionDb,
        )
    }

    private fun energyToLufs(energy: Double): Float = if (energy <= 1e-12) -70f else {
        (-0.691 + 10.0 * log10(energy)).toFloat().coerceIn(-70f, 12f)
    }
}

private class KWeightingFilter(sampleRate: Double) {
    private val shelf = Biquad.highShelf(sampleRate, 1_681.974450955533, 3.999843853973347, 1.0)
    private val highPass = Biquad.highPass(sampleRate, 38.13547087602444, 0.5003270373238773)
    fun process(sample: Double): Double = highPass.process(shelf.process(sample))
    fun reset() { shelf.reset(); highPass.reset() }
}

private class Biquad(
    private val b0: Double,
    private val b1: Double,
    private val b2: Double,
    private val a1: Double,
    private val a2: Double,
) {
    private var z1 = 0.0
    private var z2 = 0.0
    fun process(input: Double): Double {
        val output = b0 * input + z1
        z1 = b1 * input - a1 * output + z2
        z2 = b2 * input - a2 * output
        return output
    }
    fun reset() { z1 = 0.0; z2 = 0.0 }

    companion object {
        fun highPass(sampleRate: Double, frequency: Double, q: Double): Biquad {
            val w0 = 2.0 * PI * frequency / sampleRate
            val c = cos(w0)
            val s = sin(w0)
            val alpha = s / (2.0 * q)
            val a0 = 1.0 + alpha
            return Biquad(
                b0 = ((1.0 + c) / 2.0) / a0,
                b1 = (-(1.0 + c)) / a0,
                b2 = ((1.0 + c) / 2.0) / a0,
                a1 = (-2.0 * c) / a0,
                a2 = (1.0 - alpha) / a0,
            )
        }

        fun highShelf(sampleRate: Double, frequency: Double, gainDb: Double, slope: Double): Biquad {
            val a = 10.0.pow(gainDb / 40.0)
            val w0 = 2.0 * PI * frequency / sampleRate
            val c = cos(w0)
            val s = sin(w0)
            val alpha = s / 2.0 * sqrt((a + 1.0 / a) * (1.0 / slope - 1.0) + 2.0)
            val beta = 2.0 * sqrt(a) * alpha
            val a0 = (a + 1.0) - (a - 1.0) * c + beta
            return Biquad(
                b0 = a * ((a + 1.0) + (a - 1.0) * c + beta) / a0,
                b1 = -2.0 * a * ((a - 1.0) + (a + 1.0) * c) / a0,
                b2 = a * ((a + 1.0) + (a - 1.0) * c - beta) / a0,
                a1 = 2.0 * ((a - 1.0) - (a + 1.0) * c) / a0,
                a2 = ((a + 1.0) - (a - 1.0) * c - beta) / a0,
            )
        }
    }
}
