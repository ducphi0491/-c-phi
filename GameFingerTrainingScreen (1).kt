package ducphi.tunnerinstrument

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

private data class FingerTrainingStep(
    val stringNumber: Int,
    val fret: Int,
    val finger: Int,
    val midi: Int,
    val technique: String = ""
)

private data class FingerTrainingRoute(
    val key: String,
    val number: String,
    val titleVi: String,
    val titleEn: String,
    val descriptionVi: String,
    val descriptionEn: String,
    val tipVi: String,
    val tipEn: String,
    val recommendedBpm: Float,
    val accent: Color
) {
    fun title(lang: String): String = fingerUiText(lang, titleVi, titleEn)
    fun description(lang: String): String = fingerUiText(lang, descriptionVi, descriptionEn)
    fun tip(lang: String): String = fingerUiText(lang, tipVi, tipEn)
}

private data class FingerTrainingStage(
    val number: String,
    val titleVi: String,
    val titleEn: String,
    val routeKeys: List<String>
) {
    fun title(lang: String): String = fingerUiText(lang, titleVi, titleEn)
}


private enum class FingerPracticeSection {
    EXERCISES,
    HAND_SHAPES
}

private enum class CagedShape(
    val key: String,
    val titleVi: String,
    val titleEn: String,
    val descriptionVi: String,
    val descriptionEn: String,
    val rootString: Int,
    val lowerFretOffsetFromRoot: Int,
    val upperFretOffsetFromRoot: Int,
    val supportsOpenPosition: Boolean,
    val accent: Color
) {
    C(
        key = "C",
        titleVi = "Dạng C",
        titleEn = "C shape",
        descriptionVi = "Gốc trên dây 5 và dây 2; vùng C trưởng mẫu nằm ở ngăn 0-3",
        descriptionEn = "Roots on strings 5 and 2; the C-major reference box spans frets 0-3",
        rootString = 5,
        lowerFretOffsetFromRoot = 3,
        upperFretOffsetFromRoot = 0,
        supportsOpenPosition = false,
        accent = Color(0xFF22C58B)
    ),
    A(
        key = "A",
        titleVi = "Dạng A",
        titleEn = "A shape",
        descriptionVi = "Gốc trên dây 5 và dây 3; vùng C trưởng mẫu nằm ở ngăn 2-6",
        descriptionEn = "Roots on strings 5 and 3; the C-major reference box spans frets 2-6",
        rootString = 5,
        lowerFretOffsetFromRoot = 1,
        upperFretOffsetFromRoot = 3,
        supportsOpenPosition = true,
        accent = Color(0xFF5C9DFF)
    ),
    G(
        key = "G",
        titleVi = "Dạng G",
        titleEn = "G shape",
        descriptionVi = "Gốc trên dây 6, dây 3 và dây 1; vùng C trưởng mẫu nằm ở ngăn 4-8",
        descriptionEn = "Roots on strings 6, 3 and 1; the C-major reference box spans frets 4-8",
        rootString = 6,
        lowerFretOffsetFromRoot = 4,
        upperFretOffsetFromRoot = 0,
        supportsOpenPosition = false,
        accent = Color(0xFFF2B94B)
    ),
    E(
        key = "E",
        titleVi = "Dạng E",
        titleEn = "E shape",
        descriptionVi = "Gốc trên dây 6, dây 4 và dây 1; vùng C trưởng mẫu nằm ở ngăn 7-10",
        descriptionEn = "Roots on strings 6, 4 and 1; the C-major reference box spans frets 7-10",
        rootString = 6,
        lowerFretOffsetFromRoot = 1,
        upperFretOffsetFromRoot = 2,
        supportsOpenPosition = true,
        accent = Color(0xFFFF8A50)
    ),
    D(
        key = "D",
        titleVi = "Dạng D",
        titleEn = "D shape",
        descriptionVi = "Gốc trên dây 4 và dây 2; vùng C trưởng mẫu nằm ở ngăn 9-13",
        descriptionEn = "Roots on strings 4 and 2; the C-major reference box spans frets 9-13",
        rootString = 4,
        lowerFretOffsetFromRoot = 1,
        upperFretOffsetFromRoot = 3,
        supportsOpenPosition = true,
        accent = Color(0xFFB274F2)
    );

    fun title(lang: String): String = fingerUiText(lang, titleVi, titleEn)
    fun description(lang: String): String = fingerUiText(lang, descriptionVi, descriptionEn)
}

private enum class HandShapeScaleType(
    val key: String,
    val titleVi: String,
    val titleEn: String,
    val intervals: Set<Int>
) {
    MAJOR(
        key = "MAJOR",
        titleVi = "Trưởng",
        titleEn = "Major",
        intervals = setOf(0, 2, 4, 5, 7, 9, 11)
    ),
    NATURAL_MINOR(
        key = "NATURAL_MINOR",
        titleVi = "Thứ tự nhiên",
        titleEn = "Natural minor",
        intervals = setOf(0, 2, 3, 5, 7, 8, 10)
    ),
    MAJOR_PENTATONIC(
        key = "MAJOR_PENTATONIC",
        titleVi = "Ngũ cung trưởng",
        titleEn = "Major pentatonic",
        intervals = setOf(0, 2, 4, 7, 9)
    ),
    MINOR_PENTATONIC(
        key = "MINOR_PENTATONIC",
        titleVi = "Ngũ cung thứ",
        titleEn = "Minor pentatonic",
        intervals = setOf(0, 3, 5, 7, 10)
    );

    fun title(lang: String): String = fingerUiText(lang, titleVi, titleEn)
}

private data class HandShapeNote(
    val stringNumber: Int,
    val fret: Int,
    val finger: Int,
    val midi: Int,
    val noteName: String,
    val isRoot: Boolean
)

private data class HandShapePattern(
    val tonicName: String,
    val scaleType: HandShapeScaleType,
    val shape: CagedShape,
    val startFret: Int,
    val endFret: Int,
    val notes: List<HandShapeNote>
)

private enum class HarmonyPracticeType(
    val key: String,
    val titleVi: String,
    val titleEn: String,
    val noteCount: Int
) {
    TRIAD(
        key = "TRIAD",
        titleVi = "Triad 3 nốt",
        titleEn = "3-note triad",
        noteCount = 3
    ),
    SEVENTH_ARPEGGIO(
        key = "SEVENTH_ARPEGGIO",
        titleVi = "Arpeggio hợp âm 7",
        titleEn = "7th-chord arpeggio",
        noteCount = 4
    );

    fun title(lang: String): String = fingerUiText(lang, titleVi, titleEn)
}

private enum class HarmonyTonality(
    val titleVi: String,
    val titleEn: String,
    val degreeSemitones: List<Int>,
    val scaleIntervals: Set<Int>
) {
    MAJOR(
        titleVi = "Trưởng",
        titleEn = "Major",
        degreeSemitones = listOf(0, 2, 4, 5, 7, 9, 11),
        scaleIntervals = setOf(0, 2, 4, 5, 7, 9, 11)
    ),
    MINOR(
        titleVi = "Thứ",
        titleEn = "Minor",
        degreeSemitones = listOf(0, 2, 3, 5, 7, 8, 10),
        scaleIntervals = setOf(0, 2, 3, 5, 7, 8, 10)
    );

    fun title(lang: String): String = fingerUiText(lang, titleVi, titleEn)
}

private enum class HarmonyChordQuality(
    val suffix: String,
    val intervals: List<Int>
) {
    MAJOR("", listOf(0, 4, 7)),
    MINOR("m", listOf(0, 3, 7)),
    DIMINISHED("dim", listOf(0, 3, 6)),
    MAJ7("maj7", listOf(0, 4, 7, 11)),
    MIN7("m7", listOf(0, 3, 7, 10)),
    DOM7("7", listOf(0, 4, 7, 10)),
    HALF_DIM("m7♭5", listOf(0, 3, 6, 10)),
    DIM7("dim7", listOf(0, 3, 6, 9)),
    MIN_MAJ7("mMaj7", listOf(0, 3, 7, 11))
}

private data class HarmonyDegreeSpec(
    val degree: Int,
    val roman: String,
    val triadQuality: HarmonyChordQuality,
    val seventhQuality: HarmonyChordQuality
)

private data class HarmonyProgression(
    val key: String,
    val titleVi: String,
    val titleEn: String,
    val descriptionVi: String,
    val descriptionEn: String,
    val tonality: HarmonyTonality,
    val degrees: List<HarmonyDegreeSpec>
) {
    fun title(lang: String): String = fingerUiText(lang, titleVi, titleEn)
    fun description(lang: String): String = fingerUiText(lang, descriptionVi, descriptionEn)
}

private data class HarmonyStringSet(
    val key: String,
    val strings: List<Int>,
    val titleVi: String,
    val titleEn: String
) {
    fun title(lang: String): String = fingerUiText(lang, titleVi, titleEn)
}

private enum class HarmonyInversionMode(
    val key: String,
    val inversionIndex: Int,
    val titleVi: String,
    val titleEn: String
) {
    AUTO(
        key = "AUTO",
        inversionIndex = -1,
        titleVi = "Tự động dẫn giọng",
        titleEn = "Auto voice-leading"
    ),
    ROOT(
        key = "ROOT",
        inversionIndex = 0,
        titleVi = "Thế gốc",
        titleEn = "Root position"
    ),
    FIRST(
        key = "FIRST",
        inversionIndex = 1,
        titleVi = "Đảo 1",
        titleEn = "1st inversion"
    ),
    SECOND(
        key = "SECOND",
        inversionIndex = 2,
        titleVi = "Đảo 2",
        titleEn = "2nd inversion"
    ),
    THIRD(
        key = "THIRD",
        inversionIndex = 3,
        titleVi = "Đảo 3",
        titleEn = "3rd inversion"
    );

    fun title(lang: String): String = fingerUiText(lang, titleVi, titleEn)
}

private enum class HarmonyRunDirection(
    val key: String,
    val titleVi: String,
    val titleEn: String
) {
    ASCENDING("ASCENDING", "Đi lên", "Ascending"),
    DESCENDING("DESCENDING", "Đi xuống", "Descending"),
    ALTERNATING("ALTERNATING", "Zíc-zắc", "Alternating");

    fun title(lang: String): String = fingerUiText(lang, titleVi, titleEn)
}

private data class HarmonyChord(
    val index: Int,
    val roman: String,
    val rootName: String,
    val rootPitchClass: Int,
    val quality: HarmonyChordQuality,
    val symbol: String,
    val intervals: List<Int>
)

private data class HarmonyFretNote(
    val stringNumber: Int,
    val fret: Int,
    val midi: Int,
    val pitchClass: Int,
    val noteName: String,
    val interval: Int,
    val roleLabel: String
) {
    val isRoot: Boolean get() = interval == 0
    val isThird: Boolean get() = interval == 3 || interval == 4
}

private data class HarmonyVoicing(
    val chord: HarmonyChord,
    val notes: List<HarmonyFretNote>,
    val inversionIndex: Int,
    val fretSpan: Int,
    val averageFret: Float
)

private data class HarmonyPlaybackStep(
    val chordIndex: Int,
    val noteIndex: Int,
    val note: HarmonyFretNote,
    val voicing: HarmonyVoicing
)

/** Màn con cần mở khi quay lại khu vực luyện ngón/nhận diện cao độ. */
enum class FingerTrainingLandingPage { ENTRY, PITCH_MENU }

/** Cấu hình dùng để mở thẳng màn nhận diện cao độ cũ ở đúng chế độ đã chọn. */
data class PitchRecognitionLaunchConfig(
    val practiceType: String = "NOTE",
    val readingMode: String = "FREE",
    val autoEvaluate: Boolean = false
)

private fun fingerUiText(lang: String, vi: String, en: String): String {
    val code = lang.lowercase(Locale.ROOT).substringBefore('-').substringBefore('_')
    return if (code == "vi") vi else en
}

private val harmonyNoteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

private fun harmonyProgressions(): List<HarmonyProgression> {
    val majorI = HarmonyDegreeSpec(1, "I", HarmonyChordQuality.MAJOR, HarmonyChordQuality.MAJ7)
    val majorIi = HarmonyDegreeSpec(2, "ii", HarmonyChordQuality.MINOR, HarmonyChordQuality.MIN7)
    val majorIv = HarmonyDegreeSpec(4, "IV", HarmonyChordQuality.MAJOR, HarmonyChordQuality.MAJ7)
    val majorV = HarmonyDegreeSpec(5, "V", HarmonyChordQuality.MAJOR, HarmonyChordQuality.DOM7)
    val majorVi = HarmonyDegreeSpec(6, "vi", HarmonyChordQuality.MINOR, HarmonyChordQuality.MIN7)

    val minorI = HarmonyDegreeSpec(1, "i", HarmonyChordQuality.MINOR, HarmonyChordQuality.MIN_MAJ7)
    val minorIi = HarmonyDegreeSpec(2, "iiø", HarmonyChordQuality.DIMINISHED, HarmonyChordQuality.HALF_DIM)
    val minorIii = HarmonyDegreeSpec(3, "III", HarmonyChordQuality.MAJOR, HarmonyChordQuality.MAJ7)
    val minorIv = HarmonyDegreeSpec(4, "iv", HarmonyChordQuality.MINOR, HarmonyChordQuality.MIN7)
    val minorV = HarmonyDegreeSpec(5, "V", HarmonyChordQuality.MAJOR, HarmonyChordQuality.DOM7)
    val minorVi = HarmonyDegreeSpec(6, "VI", HarmonyChordQuality.MAJOR, HarmonyChordQuality.MAJ7)
    val minorVii = HarmonyDegreeSpec(7, "VII", HarmonyChordQuality.MAJOR, HarmonyChordQuality.DOM7)

    val bluesI = HarmonyDegreeSpec(1, "I7", HarmonyChordQuality.MAJOR, HarmonyChordQuality.DOM7)
    val bluesIv = HarmonyDegreeSpec(4, "IV7", HarmonyChordQuality.MAJOR, HarmonyChordQuality.DOM7)
    val bluesV = HarmonyDegreeSpec(5, "V7", HarmonyChordQuality.MAJOR, HarmonyChordQuality.DOM7)

    return listOf(
        HarmonyProgression(
            key = "II_V_I_MAJOR",
            titleVi = "ii – V – I · Trưởng",
            titleEn = "ii – V – I · Major",
            descriptionVi = "Vòng jazz kinh điển để luyện nốt mục tiêu và cảm giác giải quyết.",
            descriptionEn = "The classic jazz cadence for target notes and harmonic resolution.",
            tonality = HarmonyTonality.MAJOR,
            degrees = listOf(majorIi, majorV, majorI)
        ),
        HarmonyProgression(
            key = "I_IV_V_MAJOR",
            titleVi = "I – IV – V · Trưởng",
            titleEn = "I – IV – V · Major",
            descriptionVi = "Vòng nền tảng của Pop, Rock, Blues và Folk.",
            descriptionEn = "A foundation of pop, rock, blues and folk.",
            tonality = HarmonyTonality.MAJOR,
            degrees = listOf(majorI, majorIv, majorV)
        ),
        HarmonyProgression(
            key = "I_V_VI_IV",
            titleVi = "I – V – vi – IV · Pop",
            titleEn = "I – V – vi – IV · Pop",
            descriptionVi = "Vòng Pop/Ballad hiện đại, rất phù hợp để tập nối thế tay.",
            descriptionEn = "A modern pop/ballad loop ideal for connected positions.",
            tonality = HarmonyTonality.MAJOR,
            degrees = listOf(majorI, majorV, majorVi, majorIv)
        ),
        HarmonyProgression(
            key = "VI_II_V_I",
            titleVi = "vi – ii – V – I · Turnaround",
            titleEn = "vi – ii – V – I · Turnaround",
            descriptionVi = "Turnaround phổ biến trong Jazz, Ballad và nhạc xưa.",
            descriptionEn = "A common turnaround in jazz, ballads and standards.",
            tonality = HarmonyTonality.MAJOR,
            degrees = listOf(majorVi, majorIi, majorV, majorI)
        ),
        HarmonyProgression(
            key = "I_VI_IV_V",
            titleVi = "I – vi – IV – V · Cổ điển",
            titleEn = "I – vi – IV – V · Classic",
            descriptionVi = "Vòng 1-6-4-5 quen thuộc cho đệm hát và luyện chuyển hợp âm.",
            descriptionEn = "The classic 1-6-4-5 loop for accompaniment and chord changes.",
            tonality = HarmonyTonality.MAJOR,
            degrees = listOf(majorI, majorVi, majorIv, majorV)
        ),
        HarmonyProgression(
            key = "II_V_I_MINOR",
            titleVi = "iiø – V – i · Thứ",
            titleEn = "iiø – V – i · Minor",
            descriptionVi = "Vòng 2-5-1 thứ với iiø và dominant V7 giàu lực hút.",
            descriptionEn = "A minor 2-5-1 with half-diminished ii and a strong V7 pull.",
            tonality = HarmonyTonality.MINOR,
            degrees = listOf(minorIi, minorV, minorI)
        ),
        HarmonyProgression(
            key = "I_VI_III_VII_MINOR",
            titleVi = "i – VI – III – VII · Thứ",
            titleEn = "i – VI – III – VII · Minor",
            descriptionVi = "Vòng thứ quen thuộc trong Rock, Pop và nhạc phim.",
            descriptionEn = "A familiar minor-key loop in rock, pop and cinematic music.",
            tonality = HarmonyTonality.MINOR,
            degrees = listOf(minorI, minorVi, minorIii, minorVii)
        ),
        HarmonyProgression(
            key = "I_IV_V_MINOR",
            titleVi = "i – iv – V · Thứ",
            titleEn = "i – iv – V · Minor",
            descriptionVi = "Vòng chức năng thứ cơ bản với hợp âm V trưởng để trở về i.",
            descriptionEn = "A basic minor functional cadence with a major V resolving to i.",
            tonality = HarmonyTonality.MINOR,
            degrees = listOf(minorI, minorIv, minorV)
        ),
        HarmonyProgression(
            key = "TWELVE_BAR_BLUES",
            titleVi = "12-bar Blues",
            titleEn = "12-bar Blues",
            descriptionVi = "Mười hai ô nhịp I7-IV7-V7 để luyện groove và dominant arpeggio.",
            descriptionEn = "Twelve bars of I7-IV7-V7 for groove and dominant arpeggios.",
            tonality = HarmonyTonality.MAJOR,
            degrees = listOf(
                bluesI, bluesI, bluesI, bluesI,
                bluesIv, bluesIv, bluesI, bluesI,
                bluesV, bluesIv, bluesI, bluesV
            )
        )
    )
}

private fun harmonyStringSets(type: HarmonyPracticeType): List<HarmonyStringSet> = when (type) {
    HarmonyPracticeType.TRIAD -> listOf(
        HarmonyStringSet("STRINGS_123", listOf(3, 2, 1), "Dây 1-2-3", "Strings 1-2-3"),
        HarmonyStringSet("STRINGS_234", listOf(4, 3, 2), "Dây 2-3-4", "Strings 2-3-4"),
        HarmonyStringSet("STRINGS_345", listOf(5, 4, 3), "Dây 3-4-5", "Strings 3-4-5"),
        HarmonyStringSet("STRINGS_456", listOf(6, 5, 4), "Dây 4-5-6", "Strings 4-5-6")
    )

    HarmonyPracticeType.SEVENTH_ARPEGGIO -> listOf(
        HarmonyStringSet("STRINGS_1234", listOf(4, 3, 2, 1), "Dây 1-2-3-4", "Strings 1-2-3-4"),
        HarmonyStringSet("STRINGS_2345", listOf(5, 4, 3, 2), "Dây 2-3-4-5", "Strings 2-3-4-5"),
        HarmonyStringSet("STRINGS_3456", listOf(6, 5, 4, 3), "Dây 3-4-5-6", "Strings 3-4-5-6")
    )
}

private fun harmonyInversionModes(type: HarmonyPracticeType): List<HarmonyInversionMode> =
    if (type == HarmonyPracticeType.TRIAD) {
        listOf(
            HarmonyInversionMode.AUTO,
            HarmonyInversionMode.ROOT,
            HarmonyInversionMode.FIRST,
            HarmonyInversionMode.SECOND
        )
    } else {
        HarmonyInversionMode.values().toList()
    }

private fun harmonyRoleLabel(interval: Int): String = when (normalizedPitchClass(interval)) {
    0 -> "R"
    3 -> "♭3"
    4 -> "3"
    6 -> "♭5"
    7 -> "5"
    8 -> "♯5"
    9 -> "6"
    10 -> "♭7"
    11 -> "7"
    else -> interval.toString()
}

private fun buildHarmonyChords(
    keyName: String,
    progression: HarmonyProgression,
    practiceType: HarmonyPracticeType
): List<HarmonyChord> {
    val keyPitchClass = harmonyNoteNames.indexOf(keyName).coerceAtLeast(0)
    return progression.degrees.mapIndexed { index, degreeSpec ->
        val degreeIndex = (degreeSpec.degree - 1).coerceIn(0, 6)
        val rootPitchClass = normalizedPitchClass(
            keyPitchClass + progression.tonality.degreeSemitones[degreeIndex]
        )
        val quality = if (practiceType == HarmonyPracticeType.TRIAD) {
            degreeSpec.triadQuality
        } else {
            degreeSpec.seventhQuality
        }
        val rootName = harmonyNoteNames[rootPitchClass]
        HarmonyChord(
            index = index,
            roman = degreeSpec.roman,
            rootName = rootName,
            rootPitchClass = rootPitchClass,
            quality = quality,
            symbol = rootName + quality.suffix,
            intervals = quality.intervals
        )
    }
}

private fun harmonyPermutations(values: List<Int>): List<List<Int>> {
    if (values.size <= 1) return listOf(values)
    val result = mutableListOf<List<Int>>()
    values.forEachIndexed { index, value ->
        val rest = values.toMutableList().apply { removeAt(index) }
        harmonyPermutations(rest).forEach { tail ->
            result += listOf(value) + tail
        }
    }
    return result
}

private fun harmonyFretsForPitchClass(
    stringNumber: Int,
    pitchClass: Int,
    fretRange: IntRange
): List<Int> {
    val openMidi = guitarOpenMidiByString.getValue(stringNumber)
    return fretRange.filter { fret ->
        normalizedPitchClass(openMidi + fret) == normalizedPitchClass(pitchClass)
    }
}

private fun buildHarmonyVoicingCandidates(
    chord: HarmonyChord,
    stringSet: HarmonyStringSet,
    fretRange: IntRange
): List<HarmonyVoicing> {
    val strings = stringSet.strings.sortedDescending()
    if (strings.size != chord.intervals.size) return emptyList()

    val candidates = mutableListOf<HarmonyVoicing>()
    harmonyPermutations(chord.intervals).forEach { assignedIntervals ->
        val fretOptions = strings.mapIndexed { index, stringNumber ->
            val interval = assignedIntervals[index]
            val targetPitchClass = normalizedPitchClass(chord.rootPitchClass + interval)
            harmonyFretsForPitchClass(stringNumber, targetPitchClass, fretRange)
        }
        if (fretOptions.any { it.isEmpty() }) return@forEach

        fun collect(index: Int, notes: MutableList<HarmonyFretNote>) {
            if (index >= strings.size) {
                val orderedNotes = notes.sortedBy { it.midi }
                val lowest = orderedNotes.firstOrNull() ?: return
                val inversionIndex = chord.intervals.indexOf(lowest.interval).coerceAtLeast(0)
                val frets = orderedNotes.map { it.fret }
                candidates += HarmonyVoicing(
                    chord = chord,
                    notes = orderedNotes,
                    inversionIndex = inversionIndex,
                    fretSpan = (frets.maxOrNull() ?: 0) - (frets.minOrNull() ?: 0),
                    averageFret = if (frets.isEmpty()) 0f else frets.average().toFloat()
                )
                return
            }

            val stringNumber = strings[index]
            val interval = assignedIntervals[index]
            val targetPitchClass = normalizedPitchClass(chord.rootPitchClass + interval)
            val openMidi = guitarOpenMidiByString.getValue(stringNumber)
            fretOptions[index].forEach { fret ->
                notes += HarmonyFretNote(
                    stringNumber = stringNumber,
                    fret = fret,
                    midi = openMidi + fret,
                    pitchClass = targetPitchClass,
                    noteName = harmonyNoteNames[targetPitchClass],
                    interval = interval,
                    roleLabel = harmonyRoleLabel(interval)
                )
                collect(index + 1, notes)
                notes.removeAt(notes.lastIndex)
            }
        }

        collect(0, mutableListOf())
    }

    return candidates.distinctBy { voicing ->
        voicing.notes
            .sortedByDescending { it.stringNumber }
            .joinToString("|") { "${it.stringNumber}:${it.fret}:${it.interval}" }
    }
}

private fun nearestHarmonyFret(
    stringNumber: Int,
    pitchClass: Int,
    targetFret: Float
): Int {
    val openMidi = guitarOpenMidiByString.getValue(stringNumber)
    return (0..20)
        .filter { fret -> normalizedPitchClass(openMidi + fret) == normalizedPitchClass(pitchClass) }
        .minByOrNull { fret -> kotlin.math.abs(fret - targetFret) }
        ?: 0
}

private fun fallbackHarmonyVoicing(
    chord: HarmonyChord,
    stringSet: HarmonyStringSet,
    fretRange: IntRange,
    inversionMode: HarmonyInversionMode
): HarmonyVoicing {
    val strings = stringSet.strings.sortedDescending()
    val targetInversion = inversionMode.inversionIndex
        .takeIf { it in chord.intervals.indices }
        ?: 0
    val orderedIntervals = chord.intervals.drop(targetInversion) + chord.intervals.take(targetInversion)
    val center = (fretRange.first + fretRange.last) / 2f
    val notes = strings.mapIndexed { index, stringNumber ->
        val interval = orderedIntervals[index % orderedIntervals.size]
        val pitchClass = normalizedPitchClass(chord.rootPitchClass + interval)
        val fret = nearestHarmonyFret(stringNumber, pitchClass, center)
        HarmonyFretNote(
            stringNumber = stringNumber,
            fret = fret,
            midi = guitarOpenMidiByString.getValue(stringNumber) + fret,
            pitchClass = pitchClass,
            noteName = harmonyNoteNames[pitchClass],
            interval = interval,
            roleLabel = harmonyRoleLabel(interval)
        )
    }.sortedBy { it.midi }
    val frets = notes.map { it.fret }
    return HarmonyVoicing(
        chord = chord,
        notes = notes,
        inversionIndex = chord.intervals.indexOf(notes.first().interval).coerceAtLeast(0),
        fretSpan = (frets.maxOrNull() ?: 0) - (frets.minOrNull() ?: 0),
        averageFret = if (frets.isEmpty()) center else frets.average().toFloat()
    )
}

private fun harmonyVoicingScore(
    candidate: HarmonyVoicing,
    previous: HarmonyVoicing?,
    fretRange: IntRange,
    inversionMode: HarmonyInversionMode
): Float {
    val rangeCenter = (fretRange.first + fretRange.last) / 2f
    var score = candidate.fretSpan * 5f + kotlin.math.abs(candidate.averageFret - rangeCenter) * 1.5f

    if (inversionMode != HarmonyInversionMode.AUTO &&
        candidate.inversionIndex != inversionMode.inversionIndex
    ) {
        score += 1_000f
    }

    if (previous == null) {
        if (inversionMode == HarmonyInversionMode.AUTO) {
            score += candidate.inversionIndex * 1.25f
        }
    } else {
        val previousByString = previous.notes.associateBy { it.stringNumber }
        candidate.notes.forEach { note ->
            previousByString[note.stringNumber]?.let { prior ->
                score += kotlin.math.abs(note.fret - prior.fret) * 8f
                score += kotlin.math.abs(note.midi - prior.midi) * 0.22f
            }
        }
        score += kotlin.math.abs(candidate.averageFret - previous.averageFret) * 2f
    }

    return score
}

private fun selectHarmonyVoicing(
    candidates: List<HarmonyVoicing>,
    previous: HarmonyVoicing?,
    fretRange: IntRange,
    inversionMode: HarmonyInversionMode
): HarmonyVoicing? {
    if (candidates.isEmpty()) return null
    val exact = if (inversionMode == HarmonyInversionMode.AUTO) {
        candidates
    } else {
        candidates.filter { it.inversionIndex == inversionMode.inversionIndex }
    }
    val pool = exact.ifEmpty { candidates }
    return pool.minByOrNull { candidate ->
        harmonyVoicingScore(candidate, previous, fretRange, inversionMode)
    }
}

private fun buildHarmonyVoicings(
    chords: List<HarmonyChord>,
    stringSet: HarmonyStringSet,
    fretRange: IntRange,
    inversionMode: HarmonyInversionMode
): List<HarmonyVoicing> {
    val result = mutableListOf<HarmonyVoicing>()
    var previous: HarmonyVoicing? = null

    chords.forEach { chord ->
        val ranges = listOf(
            fretRange,
            maxOf(0, fretRange.first - 1)..minOf(20, fretRange.last + 1),
            maxOf(0, fretRange.first - 2)..minOf(20, fretRange.last + 2),
            0..15
        ).distinct()

        var selected: HarmonyVoicing? = null
        for (candidateRange in ranges) {
            val candidates = buildHarmonyVoicingCandidates(chord, stringSet, candidateRange)
            selected = selectHarmonyVoicing(
                candidates = candidates,
                previous = previous,
                fretRange = fretRange,
                inversionMode = inversionMode
            )
            if (selected != null) break
        }

        val resolved = selected ?: fallbackHarmonyVoicing(
            chord = chord,
            stringSet = stringSet,
            fretRange = fretRange,
            inversionMode = inversionMode
        )
        result += resolved
        previous = resolved
    }

    return result
}

private fun buildHarmonyPlaybackSequence(
    voicings: List<HarmonyVoicing>,
    direction: HarmonyRunDirection
): List<HarmonyPlaybackStep> = voicings.flatMapIndexed { chordIndex, voicing ->
    val ascending = voicing.notes.sortedBy { it.midi }
    val ordered = when (direction) {
        HarmonyRunDirection.ASCENDING -> ascending
        HarmonyRunDirection.DESCENDING -> ascending.asReversed()
        HarmonyRunDirection.ALTERNATING -> if (chordIndex % 2 == 0) ascending else ascending.asReversed()
    }
    ordered.mapIndexed { noteIndex, note ->
        HarmonyPlaybackStep(
            chordIndex = chordIndex,
            noteIndex = noteIndex,
            note = note,
            voicing = voicing
        )
    }
}

private fun midiDisplayName(midi: Int): String {
    val names = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val safeMidi = midi.coerceIn(0, 127)
    val octave = safeMidi / 12 - 1
    return "${names[safeMidi % 12]}$octave"
}

private fun fingerTrainingRoutes(tunaGreen: Color): List<FingerTrainingRoute> = listOf(
    FingerTrainingRoute(
        key = "1234",
        number = "01",
        titleVi = "Làm quen bốn ngón",
        titleEn = "Four-finger basics",
        descriptionVi = "Mẫu 1-2-3-4 trên từng dây · bài nên bắt đầu",
        descriptionEn = "Pattern 1-2-3-4 on each string · recommended first lesson",
        tipVi = "Bắt đầu ở ngăn 5 để tay ít phải căng; giữ các ngón gần dây.",
        tipEn = "Start at fret 5 to reduce stretching and keep every finger close to the strings.",
        recommendedBpm = 45f,
        accent = tunaGreen
    ),
    FingerTrainingRoute(
        key = "STRING_CROSSING",
        number = "02",
        titleVi = "Chuyển dây",
        titleEn = "String crossing",
        descriptionVi = "Phối hợp tay trái và tay gảy khi đổi qua các dây",
        descriptionEn = "Coordinate both hands while moving between strings",
        tipVi = "Gảy xuống - lên xen kẽ và giữ chuyển động tay gảy thật nhỏ.",
        tipEn = "Use alternate picking and keep the picking motion compact.",
        recommendedBpm = 45f,
        accent = Color(0xFF65B9F5)
    ),
    FingerTrainingRoute(
        key = "1324",
        number = "03",
        titleVi = "Độc lập ngón",
        titleEn = "Finger independence",
        descriptionVi = "Mẫu 1-3-2-4 giúp tách chuyển động của từng ngón",
        descriptionEn = "Pattern 1-3-2-4 separates the movement of each finger",
        tipVi = "Chỉ nhấc ngón cần di chuyển, tránh nâng cả bàn tay khỏi cần đàn.",
        tipEn = "Move only the required finger without lifting the whole hand.",
        recommendedBpm = 45f,
        accent = Color(0xFFF2B94B)
    ),
    FingerTrainingRoute(
        key = "SPIDER",
        number = "04",
        titleVi = "Phối hợp chéo",
        titleEn = "Spider coordination",
        descriptionVi = "Bài Spider di chuyển chéo giữa hai dây liền nhau",
        descriptionEn = "The Spider exercise moves diagonally across adjacent strings",
        tipVi = "Giữ các ngón đã bấm và chỉ chuyển ngón kế tiếp sang dây bên cạnh.",
        tipEn = "Keep planted fingers down and move only the next finger.",
        recommendedBpm = 45f,
        accent = Color(0xFFFF8A50)
    ),
    FingerTrainingRoute(
        key = "SCALE_SEQUENCE",
        number = "05",
        titleVi = "Chuỗi âm giai",
        titleEn = "Scale sequence",
        descriptionVi = "Chạy liên tục nhóm 1-2-3 rồi 2-3-4 trên từng dây",
        descriptionEn = "Play continuous 1-2-3 and 2-3-4 groups on every string",
        tipVi = "Giữ nhịp đều khi chuyển từ nhóm 1-2-3 sang nhóm 2-3-4.",
        tipEn = "Keep an even pulse between the 1-2-3 and 2-3-4 groups.",
        recommendedBpm = 50f,
        accent = Color(0xFF45CBA5)
    ),
    FingerTrainingRoute(
        key = "HAMMER_PULL",
        number = "06",
        titleVi = "Hammer-on / Pull-off",
        titleEn = "Hammer-on / Pull-off",
        descriptionVi = "Kỹ thuật tay trái sau khi đã kiểm soát tốt bốn ngón",
        descriptionEn = "A left-hand technique for after the four fingers are controlled",
        tipVi = "Gảy nốt đầu; các nốt sau dùng lực ngón tay trái và vẫn giữ đúng nhịp.",
        tipEn = "Pick the first note, then use the fretting hand while keeping steady time.",
        recommendedBpm = 45f,
        accent = Color(0xFFB274F2)
    )
)

private fun fingerTrainingStages(): List<FingerTrainingStage> = listOf(
    FingerTrainingStage(
        number = "1",
        titleVi = "Làm quen",
        titleEn = "Getting started",
        routeKeys = listOf("1234")
    ),
    FingerTrainingStage(
        number = "2",
        titleVi = "Chuyển dây",
        titleEn = "String changes",
        routeKeys = listOf("STRING_CROSSING")
    ),
    FingerTrainingStage(
        number = "3",
        titleVi = "Độc lập ngón",
        titleEn = "Finger independence",
        routeKeys = listOf("1324", "SPIDER")
    ),
    FingerTrainingStage(
        number = "4",
        titleVi = "Chuỗi nốt",
        titleEn = "Note sequences",
        routeKeys = listOf("SCALE_SEQUENCE")
    ),
    FingerTrainingStage(
        number = "5",
        titleVi = "Kỹ thuật tay trái",
        titleEn = "Left-hand techniques",
        routeKeys = listOf("HAMMER_PULL")
    )
)


private val handShapeNoteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

private val guitarOpenMidiByString = mapOf(
    6 to 40,
    5 to 45,
    4 to 50,
    3 to 55,
    2 to 59,
    1 to 64
)

private fun tonicPitchClass(tonicName: String): Int =
    handShapeNoteNames.indexOf(tonicName).coerceAtLeast(0)

private fun normalizedPitchClass(value: Int): Int = ((value % 12) + 12) % 12

private fun handShapeRootFret(tonicName: String, shape: CagedShape): Int {
    val tonicPc = tonicPitchClass(tonicName)
    val openMidi = guitarOpenMidiByString.getValue(shape.rootString)
    var rootFret = normalizedPitchClass(tonicPc - normalizedPitchClass(openMidi))

    /*
     * A, E và D có dạng mở hợp lệ khi nốt gốc nằm ngay trên dây buông.
     * C và G không được ép về đầu cần nếu phần thấp của khuôn nằm dưới nut;
     * khi đó dùng cùng dạng ở quãng tám kế tiếp.
     */
    val canUseOpenPosition = shape.supportsOpenPosition && rootFret == 0
    if (!canUseOpenPosition && rootFret - shape.lowerFretOffsetFromRoot < 0) {
        rootFret += 12
    }
    return rootFret
}

private fun handShapeFretWindow(tonicName: String, shape: CagedShape): IntRange {
    val rootFret = handShapeRootFret(tonicName, shape)
    val startFret = maxOf(0, rootFret - shape.lowerFretOffsetFromRoot)
    val endFret = rootFret + shape.upperFretOffsetFromRoot
    return startFret..endFret
}

private fun handFingerForNote(
    shape: CagedShape,
    scaleType: HandShapeScaleType,
    startFret: Int,
    stringNumber: Int,
    fret: Int
): Int {
    if (fret == 0) return 0

    val relativeFret = fret - startFret
    val useMajorFingering = scaleType == HandShapeScaleType.MAJOR ||
            scaleType == HandShapeScaleType.MAJOR_PENTATONIC

    if (useMajorFingering) {
        val mappedFinger: Int? = when (shape) {
            CagedShape.A -> when (stringNumber) {
                6 -> when (relativeFret) {
                    1 -> 2
                    3 -> 4
                    else -> null
                }
                5, 4 -> when (relativeFret) {
                    0 -> 1
                    1 -> 2
                    3 -> 4
                    else -> null
                }
                3 -> when (relativeFret) {
                    0 -> 1
                    2 -> 3
                    3 -> 4
                    else -> null
                }
                2 -> when (relativeFret) {
                    1 -> 1
                    3 -> 3
                    4 -> 4
                    else -> null
                }
                1 -> when (relativeFret) {
                    1 -> 1
                    3 -> 3
                    else -> null
                }
                else -> null
            }

            CagedShape.E -> when (stringNumber) {
                6, 5 -> when (relativeFret) {
                    0 -> 1
                    1 -> 2
                    3 -> 4
                    else -> null
                }
                4, 3 -> when (relativeFret) {
                    0 -> 1
                    2 -> 3
                    3 -> 4
                    else -> null
                }
                2 -> when (relativeFret) {
                    1 -> 1
                    3 -> 3
                    else -> null
                }
                1 -> when (relativeFret) {
                    0 -> 1
                    1 -> 2
                    3 -> 4
                    else -> null
                }
                else -> null
            }

            CagedShape.D -> when (stringNumber) {
                6 -> when (relativeFret) {
                    1 -> 1
                    3 -> 3
                    4 -> 4
                    else -> null
                }
                5 -> when (relativeFret) {
                    1 -> 1
                    3 -> 3
                    else -> null
                }
                4, 3 -> when (relativeFret) {
                    0 -> 1
                    1 -> 2
                    3 -> 4
                    else -> null
                }
                2, 1 -> when (relativeFret) {
                    1 -> 1
                    3 -> 3
                    4 -> 4
                    else -> null
                }
                else -> null
            }

            CagedShape.G -> when (stringNumber) {
                // C trưởng - Dạng G chuẩn vùng ngăn 4-8:
                // 6E: 5-7-8 | 5A: 5-7-8 | 4D: 5-7 |
                // 3G: 4-5-7 | 2B: 5-6-8 | 1e: 5-7-8.
                // Riêng dây 3 có nốt B ở ngăn 4 - nốt bị thiếu ở bản trước.
                6, 5, 1 -> when (relativeFret) {
                    1 -> 1
                    3 -> 3
                    4 -> 4
                    else -> null
                }
                4 -> when (relativeFret) {
                    1 -> 1
                    3 -> 3
                    else -> null
                }
                3 -> when (relativeFret) {
                    0 -> 1
                    1 -> 2
                    3 -> 4
                    else -> null
                }
                2 -> when (relativeFret) {
                    1 -> 1
                    2 -> 2
                    4 -> 4
                    else -> null
                }
                else -> null
            }

            /*
             * Dạng C dùng công thức dự phòng theo cột ngăn.
             */
            CagedShape.C -> null
        }
        if (mappedFinger != null) return mappedFinger
    }

    val firstFingerFret = maxOf(1, startFret)
    return (fret - firstFingerFret + 1).coerceIn(1, 4)
}

private fun buildHandShapePattern(
    tonicName: String,
    scaleType: HandShapeScaleType,
    shape: CagedShape
): HandShapePattern {
    val tonicPc = tonicPitchClass(tonicName)
    val fretWindow = handShapeFretWindow(tonicName, shape)
    val startFret = fretWindow.first
    val endFret = fretWindow.last
    val notes = mutableListOf<HandShapeNote>()

    /*
     * Mỗi dạng CAGED có vùng ngăn riêng, không dùng chung một khung 5 ngăn:
     * C trưởng làm mốc: C 0-3, A 2-6, G 4-8, E 7-10, D 9-13.
     * Khi đổi tông, toàn bộ vùng được tịnh tiến theo nốt gốc của dạng đó.
     */
    for (stringNumber in 6 downTo 1) {
        val openMidi = guitarOpenMidiByString.getValue(stringNumber)
        for (fret in fretWindow) {
            val midi = openMidi + fret
            val pitchClass = normalizedPitchClass(midi)
            val interval = normalizedPitchClass(pitchClass - tonicPc)
            if (interval in scaleType.intervals) {
                notes += HandShapeNote(
                    stringNumber = stringNumber,
                    fret = fret,
                    finger = handFingerForNote(
                        shape = shape,
                        scaleType = scaleType,
                        startFret = startFret,
                        stringNumber = stringNumber,
                        fret = fret
                    ),
                    midi = midi,
                    noteName = handShapeNoteNames[pitchClass],
                    isRoot = pitchClass == tonicPc
                )
            }
        }
    }

    return HandShapePattern(
        tonicName = tonicName,
        scaleType = scaleType,
        shape = shape,
        startFret = startFret,
        endFret = endFret,
        notes = notes
    )
}

private fun buildHandShapeSequence(
    pattern: HandShapePattern,
    directionKey: String
): List<HandShapeNote> {
    val forward = pattern.notes.sortedWith(
        compareBy<HandShapeNote> { it.midi }
            .thenByDescending { it.stringNumber }
            .thenBy { it.fret }
    )
    val reverse = forward.asReversed()

    return when (directionKey) {
        "REVERSE" -> reverse
        "BOTH" -> if (forward.size <= 1) forward else forward + reverse.drop(1)
        else -> forward
    }
}

private fun handShapeScaleTypes(): List<HandShapeScaleType> = HandShapeScaleType.values().toList()

private fun cagedShapes(): List<CagedShape> = CagedShape.values().toList()

private fun buildFingerSequence(
    instrument: String,
    routeKey: String,
    directionKey: String,
    startFret: Int
): List<FingerTrainingStep> {
    val isUkulele = instrument == "Ukulele"
    val safeStartFret = startFret.coerceIn(1, 12)

    // Index 0 = dây 1. Cùng quy ước với GameChordScreen hiện tại.
    val openMidiByString = if (isUkulele) {
        listOf(69, 64, 60, 67) // A4, E4, C4, G4
    } else {
        listOf(64, 59, 55, 50, 45, 40) // E4, B3, G3, D3, A2, E2
    }

    val stringNumbers = if (isUkulele) (4 downTo 1).toList() else (6 downTo 1).toList()

    fun step(stringNumber: Int, finger: Int, technique: String = ""): FingerTrainingStep {
        val safeFinger = finger.coerceIn(1, 4)
        val fret = safeStartFret + safeFinger - 1
        val openMidi = openMidiByString.getOrElse(stringNumber - 1) { 40 }
        return FingerTrainingStep(
            stringNumber = stringNumber,
            fret = fret,
            finger = safeFinger,
            midi = openMidi + fret,
            technique = technique
        )
    }

    fun makePass(reverse: Boolean): List<FingerTrainingStep> {
        return when (routeKey) {
            "1324" -> {
                val fingers = if (reverse) listOf(4, 2, 3, 1) else listOf(1, 3, 2, 4)
                stringNumbers.flatMap { stringNumber -> fingers.map { step(stringNumber, it) } }
            }

            "SPIDER" -> {
                val fingers = if (reverse) listOf(4, 3, 2, 1) else listOf(1, 2, 3, 4)
                stringNumbers.zipWithNext().flatMap { (lowString, highString) ->
                    fingers.mapIndexed { index, finger ->
                        val targetString = if (index % 2 == 0) lowString else highString
                        step(targetString, finger, technique = "Spider")
                    }
                }
            }

            "HAMMER_PULL" -> {
                val fingers = if (reverse) listOf(4, 3, 2, 1) else listOf(1, 2, 3, 4)
                val technique = if (reverse) "Pull-off" else "Hammer-on"
                stringNumbers.flatMap { stringNumber ->
                    fingers.map { finger -> step(stringNumber, finger, technique) }
                }
            }

            "STRING_CROSSING" -> {
                val fingers = if (reverse) listOf(4, 3, 2, 1) else listOf(1, 2, 3, 4)
                stringNumbers.zipWithNext().flatMap { (lowString, highString) ->
                    fingers.flatMap { finger ->
                        listOf(
                            step(lowString, finger, technique = "Cross"),
                            step(highString, finger, technique = "Cross")
                        )
                    }
                }
            }

            "SCALE_SEQUENCE" -> {
                val fingers = if (reverse) {
                    listOf(4, 3, 2, 3, 2, 1)
                } else {
                    listOf(1, 2, 3, 2, 3, 4)
                }
                stringNumbers.flatMap { stringNumber ->
                    fingers.map { finger -> step(stringNumber, finger, technique = "Sequence") }
                }
            }

            else -> {
                val fingers = if (reverse) listOf(4, 3, 2, 1) else listOf(1, 2, 3, 4)
                stringNumbers.flatMap { stringNumber -> fingers.map { step(stringNumber, it) } }
            }
        }
    }

    return when (directionKey) {
        "REVERSE" -> makePass(reverse = true)
        "BOTH" -> makePass(reverse = false) + makePass(reverse = true)
        else -> makePass(reverse = false)
    }
}

private fun routePatternLabel(routeKey: String, directionKey: String): String {
    fun byDirection(forward: String, reverse: String): String = when (directionKey) {
        "REVERSE" -> reverse
        "BOTH" -> "$forward / $reverse"
        else -> forward
    }

    return when (routeKey) {
        "1324" -> byDirection("1 → 3 → 2 → 4", "4 ← 2 ← 3 ← 1")
        "SPIDER" -> byDirection("1 ↗ 2 ↙ 3 ↗ 4", "4 ↙ 3 ↗ 2 ↙ 1")
        "HAMMER_PULL" -> byDirection("1H2H3H4", "4P3P2P1")
        "STRING_CROSSING" -> byDirection("1 ↔ 2 ↔ 3 ↔ 4", "4 ↔ 3 ↔ 2 ↔ 1")
        "SCALE_SEQUENCE" -> byDirection("1-2-3 / 2-3-4", "4-3-2 / 3-2-1")
        else -> byDirection("1 → 2 → 3 → 4", "4 ← 3 ← 2 ← 1")
    }
}

@Composable
fun GameFingerTrainingScreen(
    instrument: String,
    currentLang: String,
    bgColor: Color,
    surfaceColor: Color,
    textColor: Color,
    tunaGreen: Color,
    initialPage: FingerTrainingLandingPage = FingerTrainingLandingPage.ENTRY,
    onOpenPitchRecognition: (PitchRecognitionLaunchConfig) -> Unit,
    onClose: () -> Unit
) {
    val routes = remember(tunaGreen) { fingerTrainingRoutes(tunaGreen) }
    var page by rememberSaveable(instrument, initialPage) { mutableStateOf(initialPage.name) }
    var selectedHandShapeKey by rememberSaveable(instrument) { mutableStateOf(CagedShape.C.key) }

    val selectedHandShape = cagedShapes().firstOrNull { it.key == selectedHandShapeKey }
        ?: CagedShape.C
    val initialExerciseKey = routes.firstOrNull()?.key ?: "1234"

    when {
        // PITCH_MENU được giữ trong enum để tương thích với GameChordScreen cũ,
        // nhưng không còn hiển thị màn chọn 5 bước trung gian. Cả ENTRY và
        // PITCH_MENU đều trở về trang có 3 lựa chọn chính.
        page == FingerTrainingLandingPage.ENTRY.name ||
                page == FingerTrainingLandingPage.PITCH_MENU.name -> {
            FingerTrainingEntryContent(
                instrument = instrument,
                currentLang = currentLang,
                bgColor = bgColor,
                surfaceColor = surfaceColor,
                textColor = textColor,
                tunaGreen = tunaGreen,
                onOpenFingerTraining = {
                    // Bỏ màn danh sách 6 bài. Mở thẳng player và chọn bài ngay trong dropdown.
                    page = "EXERCISE_PLAYER"
                },
                onOpenHarmonyPractice = {
                    if (instrument == "Guitar") {
                        page = "HARMONY_PLAYER"
                    }
                },
                onOpenPitchRecognition = {
                    // Mở thẳng màn thực hành. Người dùng đổi Nốt/Hợp âm/Bài hát
                    // ngay trong danh sách chọn đã có sẵn trên màn hình đó.
                    onOpenPitchRecognition(
                        PitchRecognitionLaunchConfig(
                            practiceType = "NOTE",
                            readingMode = "FREE",
                            autoEvaluate = false
                        )
                    )
                },
                onClose = onClose
            )
        }

        page == "HARMONY_PLAYER" -> {
            HarmonyProgressionPlayerScreen(
                currentLang = currentLang,
                bgColor = bgColor,
                surfaceColor = surfaceColor,
                textColor = textColor,
                tunaGreen = tunaGreen,
                onClose = {
                    page = FingerTrainingLandingPage.ENTRY.name
                }
            )
        }

        page == "HAND_SHAPE_PLAYER" -> {
            HandShapeTrainingPlayerScreen(
                initialShape = selectedHandShape,
                instrument = instrument,
                currentLang = currentLang,
                bgColor = bgColor,
                surfaceColor = surfaceColor,
                textColor = textColor,
                tunaGreen = tunaGreen,
                onOpenExercises = {
                    page = "EXERCISE_PLAYER"
                },
                onClose = {
                    page = FingerTrainingLandingPage.ENTRY.name
                }
            )
        }

        else -> {
            FingerTrainingPlayerScreen(
                routes = routes,
                initialRouteKey = initialExerciseKey,
                instrument = instrument,
                currentLang = currentLang,
                bgColor = bgColor,
                surfaceColor = surfaceColor,
                textColor = textColor,
                onOpenHandShapes = {
                    if (instrument == "Guitar") {
                        selectedHandShapeKey = CagedShape.C.key
                        page = "HAND_SHAPE_PLAYER"
                    }
                },
                onClose = {
                    page = FingerTrainingLandingPage.ENTRY.name
                }
            )
        }
    }
}

@Composable
private fun FingerTrainingRouteMenu(
    instrument: String,
    currentLang: String,
    bgColor: Color,
    surfaceColor: Color,
    textColor: Color,
    routes: List<FingerTrainingRoute>,
    onRouteSelected: (FingerTrainingRoute) -> Unit,
    onHandShapeSelected: (CagedShape) -> Unit,
    onClose: () -> Unit
) {
    val activity = LocalActivity.current
    val effectiveInstrument = if (instrument == "Ukulele") "Ukulele" else "Guitar"
    val stages = remember { fingerTrainingStages() }
    val routeByKey = remember(routes) { routes.associateBy { it.key } }

    BackHandler(onBack = onClose)

    DisposableEffect(activity) {
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            if (activity != null && previousOrientation != null) {
                activity.requestedOrientation = previousOrientation
            }
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = bgColor) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp)
            ) {
                FingerSectionTopBar(
                    title = fingerUiText(currentLang, "Lộ trình luyện ngón", "Finger-training path"),
                    subtitle = "$effectiveInstrument · ${fingerUiText(currentLang, "5 chặng · 6 bài", "5 stages · 6 lessons")}",
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    onBack = onClose
                )

                // Chỉ giữ 2 lựa chọn cấp cao. "Luyện thế tay" mở thẳng màn CAGED
                // vì ngay trong player đã có dropdown chọn Dạng C/A/G/E/D.
                FingerPracticeSectionSelector(
                    selectedSection = FingerPracticeSection.EXERCISES,
                    currentLang = currentLang,
                    exerciseAccent = routes.firstOrNull()?.accent ?: Color(0xFF22C58B),
                    handShapeAccent = Color(0xFFE85D86),
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    onSelected = { section ->
                        if (section == FingerPracticeSection.HAND_SHAPES && effectiveInstrument == "Guitar") {
                            onHandShapeSelected(CagedShape.C)
                        }
                    }
                )

                Spacer(Modifier.height(7.dp))

                Text(
                    text = fingerUiText(
                        currentLang,
                        "Học theo thứ tự từ Bài 1 đến Bài 6. Chọn 'Luyện thế tay' phía trên để mở trực tiếp màn CAGED.",
                        "Follow lessons 1 through 6 in order. Tap 'Hand positions' above to open CAGED practice directly."
                    ),
                    color = textColor.copy(alpha = 0.62f),
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    stages.forEachIndexed { stageIndex, stage ->
                        item(key = "stage-${stage.number}") {
                            FingerTrainingStageHeader(
                                stage = stage,
                                currentLang = currentLang,
                                textColor = textColor,
                                accent = stage.routeKeys.firstOrNull()?.let(routeByKey::get)?.accent
                                    ?: textColor
                            )
                        }

                        val stageRoutes = stage.routeKeys.mapNotNull { key -> routeByKey[key] }
                        items(
                            items = stageRoutes,
                            key = { route -> route.key }
                        ) { route ->
                            val badgeText = when (route.key) {
                                "1234" -> fingerUiText(currentLang, "Nên bắt đầu", "Start here")
                                "STRING_CROSSING" -> fingerUiText(currentLang, "Bài tiếp theo", "Next lesson")
                                "HAMMER_PULL" -> fingerUiText(currentLang, "Học sau", "Learn later")
                                else -> null
                            }
                            FingerTrainingRouteCard(
                                route = route,
                                currentLang = currentLang,
                                surfaceColor = surfaceColor,
                                textColor = textColor,
                                numStrings = if (effectiveInstrument == "Ukulele") 4 else 6,
                                badgeText = badgeText,
                                onClick = { onRouteSelected(route) }
                            )
                        }

                        if (stageIndex < stages.lastIndex) {
                            item(key = "stage-gap-${stage.number}") {
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FingerPracticeSectionSelector(
    selectedSection: FingerPracticeSection,
    currentLang: String,
    exerciseAccent: Color,
    handShapeAccent: Color,
    surfaceColor: Color,
    textColor: Color,
    compact: Boolean = false,
    onSelected: (FingerPracticeSection) -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 34.dp else 42.dp)
            .clip(shape)
            .background(surfaceColor)
            .border(1.dp, textColor.copy(alpha = 0.09f), shape)
            .padding(if (compact) 3.dp else 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        val entries = listOf(
            Triple(
                FingerPracticeSection.EXERCISES,
                fingerUiText(currentLang, "Chạy ngón", "Finger exercises"),
                exerciseAccent
            ),
            Triple(
                FingerPracticeSection.HAND_SHAPES,
                fingerUiText(currentLang, "Luyện thế tay", "Hand positions"),
                handShapeAccent
            )
        )

        entries.forEach { (section, label, accent) ->
            val selected = selectedSection == section
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (selected) accent.copy(alpha = 0.15f) else Color.Transparent)
                    .then(
                        if (selected) Modifier.border(
                            1.dp,
                            accent.copy(alpha = 0.48f),
                            RoundedCornerShape(11.dp)
                        ) else Modifier
                    )
                    .clickable { onSelected(section) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(if (compact) 18.dp else 21.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = if (selected) 0.22f else 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (section == FingerPracticeSection.EXERCISES) "1" else "2",
                        color = accent,
                        fontSize = if (compact) 8.sp else 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(Modifier.width(if (compact) 5.dp else 7.dp))
                Text(
                    text = label,
                    color = if (selected) accent else textColor.copy(alpha = 0.68f),
                    fontSize = if (compact) 9.sp else 12.sp,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HandShapeRouteCard(
    shape: CagedShape,
    currentLang: String,
    surfaceColor: Color,
    textColor: Color,
    badgeText: String?,
    onClick: () -> Unit
) {
    val previewPattern = remember(shape) {
        buildHandShapePattern("C", HandShapeScaleType.MAJOR, shape)
    }
    val cardShape = RoundedCornerShape(15.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(98.dp)
            .clickable(onClick = onClick),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, shape.accent.copy(alpha = 0.32f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 82.dp, height = 70.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(shape.accent.copy(alpha = 0.08f))
                    .border(1.dp, shape.accent.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
                    .padding(5.dp)
            ) {
                HandShapePreviewCanvas(
                    pattern = previewPattern,
                    accent = shape.accent,
                    textColor = textColor,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.width(11.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shape.title(currentLang),
                    color = textColor,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = shape.description(currentLang),
                    color = textColor.copy(alpha = 0.58f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = fingerUiText(
                        currentLang,
                        "Ví dụ C trưởng · Ngăn ${previewPattern.startFret}-${previewPattern.endFret}",
                        "C major example · Frets ${previewPattern.startFret}-${previewPattern.endFret}"
                    ),
                    color = shape.accent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(6.dp))
            Column(horizontalAlignment = Alignment.End) {
                if (badgeText != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = shape.accent.copy(alpha = 0.13f)
                    ) {
                        Text(
                            text = badgeText,
                            color = shape.accent,
                            fontSize = 7.8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(9.dp))
                } else {
                    Spacer(Modifier.height(23.dp))
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.40f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun HandShapeUnsupportedCard(
    currentLang: String,
    surfaceColor: Color,
    textColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.10f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = textColor.copy(alpha = 0.48f),
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = fingerUiText(currentLang, "Chỉ hỗ trợ Guitar", "Guitar only"),
                    color = textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = fingerUiText(
                        currentLang,
                        "Hệ thế tay CAGED được bố trí theo cần đàn Guitar 6 dây.",
                        "The CAGED position system is arranged for a six-string guitar fretboard."
                    ),
                    color = textColor.copy(alpha = 0.55f),
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun HandShapePreviewCanvas(
    pattern: HandShapePattern,
    accent: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val left = size.width * 0.12f
        val right = size.width * 0.92f
        val top = size.height * 0.08f
        val bottom = size.height * 0.94f
        val stringGap = (right - left) / 5f
        val fretCells = (pattern.endFret - pattern.startFret + 1).coerceAtLeast(1)
        val fretGap = (bottom - top) / fretCells.toFloat()

        for (stringIndex in 0..5) {
            val x = left + stringIndex * stringGap
            drawLine(
                color = textColor.copy(alpha = 0.26f),
                start = Offset(x, top),
                end = Offset(x, bottom),
                strokeWidth = 1.2f + (5 - stringIndex) * 0.15f
            )
        }
        for (line in 0..fretCells) {
            val y = top + line * fretGap
            drawLine(
                color = textColor.copy(alpha = 0.18f),
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = 1.3f
            )
        }

        pattern.notes.forEach { note ->
            val stringColumn = 6 - note.stringNumber
            val x = left + stringColumn * stringGap
            val row = note.fret - pattern.startFret
            val y = top + (row + 0.5f) * fretGap
            drawCircle(
                color = if (note.isRoot) Color(0xFFE84D78) else accent.copy(alpha = 0.75f),
                radius = if (note.isRoot) 4.8f else 3.6f,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun FingerTrainingStageHeader(
    stage: FingerTrainingStage,
    currentLang: String,
    textColor: Color,
    accent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 5.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.15f))
                .border(1.dp, accent.copy(alpha = 0.38f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stage.number,
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fingerUiText(currentLang, "Chặng ${stage.number}", "Stage ${stage.number}"),
                color = textColor.copy(alpha = 0.48f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stage.title(currentLang),
                color = textColor,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun FingerTrainingRouteCard(
    route: FingerTrainingRoute,
    currentLang: String,
    surfaceColor: Color,
    textColor: Color,
    numStrings: Int,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(15.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, route.accent.copy(alpha = 0.30f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 82.dp, height = 72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(route.accent.copy(alpha = 0.09f))
                    .border(1.dp, route.accent.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                    .padding(5.dp)
            ) {
                FingerRoutePreviewCanvas(
                    routeKey = route.key,
                    numStrings = numStrings,
                    accent = route.accent,
                    textColor = textColor,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.width(11.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = route.accent.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = route.number,
                            color = route.accent,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = route.title(currentLang),
                        color = textColor,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = route.description(currentLang),
                    color = textColor.copy(alpha = 0.58f),
                    fontSize = 11.2.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(6.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${route.recommendedBpm.roundToInt()} BPM",
                    color = route.accent,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )
                if (badgeText != null) {
                    Spacer(Modifier.height(5.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = route.accent.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = badgeText,
                            color = route.accent,
                            fontSize = 7.8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Spacer(Modifier.height(7.dp))
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.40f),
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
private fun FingerRoutePreviewCanvas(
    routeKey: String,
    numStrings: Int,
    accent: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val left = size.width * 0.10f
        val right = size.width * 0.96f
        val top = size.height * 0.14f
        val bottom = size.height * 0.86f
        val fretGap = (right - left) / 4f
        val stringGap = if (numStrings > 1) (bottom - top) / (numStrings - 1) else 0f

        fun point(stringRow: Int, fret: Int): Offset = Offset(
            x = left + (fret - 0.5f) * fretGap,
            y = top + stringRow.coerceIn(0, numStrings - 1) * stringGap
        )

        for (fret in 0..4) {
            val x = left + fret * fretGap
            drawLine(
                color = textColor.copy(alpha = if (fret == 0) 0.45f else 0.16f),
                start = Offset(x, top),
                end = Offset(x, bottom),
                strokeWidth = if (fret == 0) 4f else 1.4f
            )
        }
        for (row in 0 until numStrings) {
            val y = top + row * stringGap
            drawLine(
                color = textColor.copy(alpha = 0.28f),
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = 1.2f + row * 0.18f
            )
        }

        val points: List<Offset> = when (routeKey) {
            "1324" -> listOf(1, 3, 2, 4).map { point(numStrings - 1, it) }
            "SPIDER" -> listOf(
                point(numStrings - 1, 1),
                point((numStrings - 2).coerceAtLeast(0), 2),
                point(numStrings - 1, 3),
                point((numStrings - 2).coerceAtLeast(0), 4)
            )
            "STRING_CROSSING" -> listOf(
                point(numStrings - 1, 1),
                point((numStrings - 2).coerceAtLeast(0), 1),
                point(numStrings - 1, 2),
                point((numStrings - 2).coerceAtLeast(0), 2),
                point(numStrings - 1, 3),
                point((numStrings - 2).coerceAtLeast(0), 3)
            )
            "SCALE_SEQUENCE" -> listOf(1, 2, 3, 2, 3, 4).map { point(numStrings - 1, it) }
            else -> listOf(1, 2, 3, 4).map { point(numStrings - 1, it) }
        }

        points.zipWithNext().forEach { (a, b) ->
            drawLine(
                color = accent.copy(alpha = 0.50f),
                start = a,
                end = b,
                strokeWidth = 2.2f,
                cap = StrokeCap.Round
            )
        }

        points.forEachIndexed { index, p ->
            drawCircle(
                color = accent.copy(alpha = if (index == 0) 0.95f else 0.56f),
                radius = if (index == 0) 5.5f else 4.3f,
                center = p
            )
        }

        if (routeKey == "HAMMER_PULL") {
            val paint = android.graphics.Paint().apply {
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textSize = 15f
                color = accent.toArgb()
            }
            drawContext.canvas.nativeCanvas.drawText("H / P", size.width * 0.58f, top + 13f, paint)
        }
    }
}

@Composable
private fun FingerTrainingPlayerScreen(
    routes: List<FingerTrainingRoute>,
    initialRouteKey: String,
    instrument: String,
    currentLang: String,
    bgColor: Color,
    surfaceColor: Color,
    textColor: Color,
    onOpenHandShapes: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val effectiveInstrument = if (instrument == "Ukulele") "Ukulele" else "Guitar"
    val isVietnamese = currentLang.lowercase(Locale.ROOT).startsWith("vi")

    var selectedRouteKey by rememberSaveable(effectiveInstrument) {
        mutableStateOf(initialRouteKey)
    }
    val route = routes.firstOrNull { it.key == selectedRouteKey }
        ?: routes.firstOrNull()
        ?: return

    val uiAccent = route.accent
    val currentAccent = Color(0xFF35A7FF)
    val pendingNoteColor = Color(0xFFD0D4D8)
    val playAccent = Color(0xFFF2B94B)
    val borderColor = textColor.copy(alpha = 0.10f)
    val muted = textColor.copy(alpha = 0.58f)

    var directionKey by rememberSaveable(route.key) { mutableStateOf("FORWARD") }
    var startFret by rememberSaveable(route.key) { mutableIntStateOf(1) }
    var fretViewKey by rememberSaveable(route.key) { mutableStateOf("FULL") }
    var bpm by rememberSaveable(route.key) { mutableFloatStateOf(route.recommendedBpm) }
    var soundEnabled by rememberSaveable(route.key) { mutableStateOf(true) }
    var repeatEnabled by rememberSaveable(route.key) { mutableStateOf(true) }
    var isPlaying by rememberSaveable(route.key) { mutableStateOf(false) }
    var countdownValue by rememberSaveable(route.key) { mutableIntStateOf(0) }
    var hasPlaybackStarted by rememberSaveable(route.key) { mutableStateOf(false) }
    var currentIndex by rememberSaveable(route.key) { mutableIntStateOf(0) }
    var completedRounds by rememberSaveable(route.key) { mutableIntStateOf(0) }

    val sequence = remember(effectiveInstrument, route.key, directionKey, startFret) {
        buildFingerSequence(effectiveInstrument, route.key, directionKey, startFret)
    }

    val currentStep = sequence.getOrNull(currentIndex)
    val nextStep = when {
        currentIndex < sequence.lastIndex -> sequence.getOrNull(currentIndex + 1)
        repeatEnabled && sequence.isNotEmpty() -> sequence.first()
        else -> null
    }
    val progress = if (sequence.isEmpty()) 0f else (currentIndex + 1f) / sequence.size.toFloat()

    val fullFretEnd = maxOf(12, startFret + 3)
    val visibleStartFret = if (fretViewKey == "EXERCISE") startFret else 1
    val visibleEndFret = if (fretViewKey == "EXERCISE") startFret + 3 else fullFretEnd

    fun resetSession() {
        isPlaying = false
        countdownValue = 0
        hasPlaybackStarted = false
        currentIndex = 0
        completedRounds = 0
    }

    fun stopAndClose() {
        isPlaying = false
        countdownValue = 0
        onClose()
    }

    BackHandler {
        if (countdownValue > 0) {
            countdownValue = 0
        } else {
            stopAndClose()
        }
    }

    DisposableEffect(activity) {
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            if (activity != null && !activity.isChangingConfigurations && previousOrientation != null) {
                activity.requestedOrientation = previousOrientation
            }
        }
    }

    LaunchedEffect(effectiveInstrument) {
        runCatching { CoreAudioPlayer.loadInstrument(context, effectiveInstrument) }
    }

    LaunchedEffect(sequence) {
        resetSession()
    }

    LaunchedEffect(countdownValue) {
        if (countdownValue <= 0) return@LaunchedEffect

        delay(1_000L)
        if (countdownValue > 1) {
            countdownValue--
        } else {
            countdownValue = 0
            hasPlaybackStarted = true
            isPlaying = true
        }
    }

    LaunchedEffect(isPlaying, bpm, repeatEnabled, sequence) {
        if (!isPlaying || sequence.isEmpty()) return@LaunchedEffect

        while (isPlaying) {
            val intervalMs = (60_000f / bpm.coerceIn(40f, 160f)).toLong()
            delay(intervalMs)

            if (currentIndex < sequence.lastIndex) {
                currentIndex++
            } else {
                completedRounds++
                if (repeatEnabled) {
                    currentIndex = 0
                } else {
                    isPlaying = false
                    countdownValue = 0
                }
            }
        }
    }

    LaunchedEffect(isPlaying, currentIndex, soundEnabled, sequence) {
        if (isPlaying && soundEnabled) {
            sequence.getOrNull(currentIndex)?.let { step ->
                runCatching {
                    CoreAudioPlayer.playChord(listOf(step.midi), effectiveInstrument)
                }
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "fingerPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(560, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fingerPulseValue"
    )

    val configuration = LocalConfiguration.current
    val veryCompactHeight = configuration.screenHeightDp < 620
    val compactHeight = configuration.screenHeightDp < 720
    val compactWidth = configuration.screenWidthDp < 380

    val outerPadding = if (compactWidth) 6.dp else 8.dp
    val topBarHeight = when {
        veryCompactHeight -> 46.dp
        compactHeight -> 52.dp
        else -> 58.dp
    }
    val bottomControlHeight = when {
        veryCompactHeight -> 50.dp
        compactHeight -> 56.dp
        else -> 62.dp
    }
    val panelGap = if (veryCompactHeight) 3.dp else 5.dp
    val panelShape = RoundedCornerShape(if (compactWidth) 13.dp else 16.dp)

    val exerciseOptions = routes.map { exercise ->
        exercise.key to "${exercise.number} · ${exercise.title(currentLang)}"
    }
    val directionOptions = listOf(
        "FORWARD" to fingerUiText(currentLang, "Đi lên", "Ascending"),
        "REVERSE" to fingerUiText(currentLang, "Đi xuống", "Descending"),
        "BOTH" to fingerUiText(currentLang, "Cả hai", "Both")
    )
    // Danh sách luôn xếp từ ngăn thấp đến ngăn cao; mặc định là Ngăn 1-4.
    val positionOptions = listOf(1, 3, 5, 7).map { fret ->
        fret.toString() to fingerUiText(
            currentLang,
            "Ngăn $fret-${fret + 3}",
            "Frets $fret-${fret + 3}"
        )
    }
    val fretViewOptions = listOf(
        // Mặc định hiển thị toàn bộ cần đàn để người mới dễ hình dung vị trí bài tập.
        "FULL" to fingerUiText(
            currentLang,
            "Ngăn 1-$fullFretEnd",
            "Frets 1-$fullFretEnd"
        ),
        "EXERCISE" to fingerUiText(
            currentLang,
            "Vùng tập $startFret-${startFret + 3}",
            "Practice area $startFret-${startFret + 3}"
        )
    )

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = bgColor) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = outerPadding, vertical = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(topBarHeight),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { stopAndClose() },
                        modifier = Modifier
                            .size(if (veryCompactHeight) 34.dp else 40.dp)
                            .clip(CircleShape)
                            .background(surfaceColor)
                            .border(1.dp, borderColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = if (isVietnamese) "Quay lại" else "Back",
                            tint = textColor,
                            modifier = Modifier.size(if (veryCompactHeight) 18.dp else 21.dp)
                        )
                    }

                    Spacer(Modifier.width(if (compactWidth) 7.dp else 10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = route.title(currentLang),
                            color = textColor,
                            fontSize = when {
                                veryCompactHeight -> 16.sp
                                compactHeight -> 18.sp
                                else -> 20.sp
                            },
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${fingerUiText(currentLang, "Luyện ngón", "Finger training")} · ${routePatternLabel(route.key, directionKey)}",
                            color = muted,
                            fontSize = if (veryCompactHeight) 8.5.sp else 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = uiAccent.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            uiAccent.copy(alpha = 0.42f)
                        )
                    ) {
                        Text(
                            text = "${bpm.roundToInt()} BPM",
                            color = uiAccent,
                            fontSize = if (veryCompactHeight) 10.sp else 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(
                                horizontal = if (compactWidth) 9.dp else 13.dp,
                                vertical = 5.dp
                            )
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    IconButton(
                        onClick = { resetSession() },
                        modifier = Modifier
                            .size(if (veryCompactHeight) 34.dp else 40.dp)
                            .clip(CircleShape)
                            .background(surfaceColor)
                            .border(1.dp, borderColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = if (isVietnamese) "Đặt lại" else "Reset",
                            tint = textColor,
                            modifier = Modifier.size(if (veryCompactHeight) 17.dp else 20.dp)
                        )
                    }
                }

                if (effectiveInstrument == "Guitar") {
                    FingerPracticeSectionSelector(
                        selectedSection = FingerPracticeSection.EXERCISES,
                        currentLang = currentLang,
                        exerciseAccent = uiAccent,
                        handShapeAccent = Color(0xFFE85D86),
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        compact = veryCompactHeight,
                        onSelected = { section ->
                            if (section == FingerPracticeSection.HAND_SHAPES) {
                                isPlaying = false
                                countdownValue = 0
                                onOpenHandShapes()
                            }
                        }
                    )
                    Spacer(Modifier.height(panelGap))
                } else {
                    Spacer(Modifier.height(panelGap))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(panelGap)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(if (compactWidth) 1.28f else 1.42f)
                            .fillMaxHeight(),
                        shape = panelShape,
                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    horizontal = if (compactWidth) 4.dp else 6.dp,
                                    vertical = 4.dp
                                )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentStep?.let { step ->
                                        if (isVietnamese) {
                                            "Dây ${step.stringNumber} · Ngăn ${step.fret} · Ngón ${step.finger}"
                                        } else {
                                            "String ${step.stringNumber} · Fret ${step.fret} · Finger ${step.finger}"
                                        }
                                    } ?: route.title(currentLang),
                                    color = textColor,
                                    fontSize = if (veryCompactHeight) 9.5.sp else 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = if (sequence.isEmpty()) {
                                        "0/0"
                                    } else {
                                        "${currentIndex + 1}/${sequence.size}"
                                    },
                                    color = uiAccent,
                                    fontSize = if (veryCompactHeight) 8.sp else 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(Modifier.height(2.dp))

                            FingerTrainingFretboardCanvas(
                                instrument = effectiveInstrument,
                                sequence = sequence,
                                currentIndex = currentIndex,
                                currentStep = currentStep,
                                nextStep = nextStep,
                                visibleStartFret = visibleStartFret,
                                visibleEndFret = visibleEndFret,
                                textColor = textColor,
                                currentAccent = currentAccent,
                                completedAccent = uiAccent,
                                pendingNoteColor = pendingNoteColor,
                                highlightCurrent = hasPlaybackStarted,
                                pulse = if (isPlaying) pulse else 1f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )

                            LinearProgressIndicator(
                                progress = progress.coerceIn(0f, 1f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = uiAccent,
                                trackColor = textColor.copy(alpha = 0.08f)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(if (compactWidth) 1f else 0.96f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(panelGap)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1.72f),
                            shape = panelShape,
                            colors = CardDefaults.cardColors(containerColor = surfaceColor),
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(if (veryCompactHeight) 4.dp else 6.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                HandShapeSideDropdown(
                                    label = fingerUiText(currentLang, "Bài tập", "Exercise"),
                                    selectedKey = route.key,
                                    selectedLabel = exerciseOptions.firstOrNull {
                                        it.first == route.key
                                    }?.second.orEmpty(),
                                    options = exerciseOptions,
                                    accent = uiAccent,
                                    surfaceColor = bgColor.copy(alpha = 0.45f),
                                    textColor = textColor,
                                    compact = veryCompactHeight,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    onSelected = { key ->
                                        if (key != selectedRouteKey) {
                                            isPlaying = false
                                            countdownValue = 0
                                            currentIndex = 0
                                            completedRounds = 0
                                            selectedRouteKey = key
                                        }
                                    }
                                )
                                HandShapeSideDropdown(
                                    label = fingerUiText(currentLang, "Hướng luyện", "Direction"),
                                    selectedKey = directionKey,
                                    selectedLabel = directionOptions.firstOrNull {
                                        it.first == directionKey
                                    }?.second.orEmpty(),
                                    options = directionOptions,
                                    accent = uiAccent,
                                    surfaceColor = bgColor.copy(alpha = 0.45f),
                                    textColor = textColor,
                                    compact = veryCompactHeight,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    onSelected = {
                                        isPlaying = false
                                        countdownValue = 0
                                        directionKey = it
                                    }
                                )
                                HandShapeSideDropdown(
                                    label = fingerUiText(currentLang, "Vị trí tay", "Hand position"),
                                    selectedKey = startFret.toString(),
                                    selectedLabel = positionOptions.firstOrNull {
                                        it.first == startFret.toString()
                                    }?.second.orEmpty(),
                                    options = positionOptions,
                                    accent = uiAccent,
                                    surfaceColor = bgColor.copy(alpha = 0.45f),
                                    textColor = textColor,
                                    compact = veryCompactHeight,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    onSelected = { key ->
                                        isPlaying = false
                                        countdownValue = 0
                                        startFret = key.toIntOrNull() ?: 1
                                    }
                                )
                                HandShapeSideDropdown(
                                    label = fingerUiText(currentLang, "Phạm vi cần đàn", "Fret range"),
                                    selectedKey = fretViewKey,
                                    selectedLabel = fretViewOptions.firstOrNull {
                                        it.first == fretViewKey
                                    }?.second.orEmpty(),
                                    options = fretViewOptions,
                                    accent = uiAccent,
                                    surfaceColor = bgColor.copy(alpha = 0.45f),
                                    textColor = textColor,
                                    compact = veryCompactHeight,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    onSelected = { fretViewKey = it }
                                )
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.68f),
                            shape = panelShape,
                            colors = CardDefaults.cardColors(containerColor = surfaceColor),
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (hasPlaybackStarted) {
                                        currentAccent.copy(alpha = 0.18f)
                                    } else {
                                        pendingNoteColor.copy(alpha = 0.16f)
                                    },
                                    modifier = Modifier.size(if (veryCompactHeight) 26.dp else 31.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = currentStep?.finger?.toString() ?: "-",
                                            color = if (hasPlaybackStarted) currentAccent else textColor.copy(alpha = 0.64f),
                                            fontSize = if (veryCompactHeight) 12.sp else 15.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                                Spacer(Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentStep?.let { step ->
                                            "${midiDisplayName(step.midi)} · ${fingerUiText(currentLang, "Vòng", "Round")} ${completedRounds + 1}"
                                        } ?: "-",
                                        color = textColor,
                                        fontSize = if (veryCompactHeight) 8.5.sp else 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = nextStep?.let { step ->
                                            if (isVietnamese) {
                                                "Tiếp: Dây ${step.stringNumber} · Ngón ${step.finger}"
                                            } else {
                                                "Next: String ${step.stringNumber} · Finger ${step.finger}"
                                            }
                                        } ?: route.tip(currentLang),
                                        color = muted,
                                        fontSize = if (veryCompactHeight) 7.sp else 8.2.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.72f),
                            shape = panelShape,
                            colors = CardDefaults.cardColors(containerColor = surfaceColor),
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    HandShapeLegendEntry(
                                        color = currentAccent,
                                        label = fingerUiText(currentLang, "Hiện tại", "Current"),
                                        textColor = textColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                    HandShapeLegendEntry(
                                        color = uiAccent,
                                        label = fingerUiText(currentLang, "Đã tập", "Done"),
                                        textColor = textColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    HandShapeLegendEntry(
                                        color = Color.White,
                                        label = fingerUiText(currentLang, "Tiếp theo", "Next"),
                                        textColor = textColor,
                                        outlined = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    HandShapeLegendEntry(
                                        color = pendingNoteColor,
                                        label = fingerUiText(currentLang, "Chưa tập", "Pending"),
                                        textColor = textColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.82f),
                            shape = panelShape,
                            colors = CardDefaults.cardColors(containerColor = surfaceColor),
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                HandShapeSwitchRow(
                                    icon = if (soundEnabled) {
                                        Icons.Default.VolumeUp
                                    } else {
                                        Icons.Default.VolumeOff
                                    },
                                    label = fingerUiText(currentLang, "Âm mẫu", "Sound"),
                                    checked = soundEnabled,
                                    accent = uiAccent,
                                    textColor = textColor,
                                    compact = veryCompactHeight,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    onCheckedChange = { soundEnabled = it }
                                )
                                HorizontalDivider(color = borderColor)
                                HandShapeSwitchRow(
                                    icon = Icons.Default.Refresh,
                                    label = fingerUiText(currentLang, "Lặp lại", "Repeat"),
                                    checked = repeatEnabled,
                                    accent = uiAccent,
                                    textColor = textColor,
                                    compact = veryCompactHeight,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    onCheckedChange = { repeatEnabled = it }
                                )
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.50f),
                            shape = panelShape,
                            colors = CardDefaults.cardColors(containerColor = surfaceColor),
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                IconButton(
                                    onClick = { bpm = (bpm - 5f).coerceAtLeast(40f) },
                                    modifier = Modifier
                                        .size(if (veryCompactHeight) 30.dp else 34.dp)
                                        .clip(CircleShape)
                                        .background(uiAccent.copy(alpha = 0.16f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = if (isVietnamese) "Giảm BPM" else "Decrease BPM",
                                        tint = uiAccent,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Text(
                                    text = "${bpm.roundToInt()} BPM",
                                    color = textColor,
                                    fontSize = if (veryCompactHeight) 9.5.sp else 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { bpm = (bpm + 5f).coerceAtMost(160f) },
                                    modifier = Modifier
                                        .size(if (veryCompactHeight) 30.dp else 34.dp)
                                        .clip(CircleShape)
                                        .background(uiAccent.copy(alpha = 0.16f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = if (isVietnamese) "Tăng BPM" else "Increase BPM",
                                        tint = uiAccent,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(panelGap))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(bottomControlHeight),
                    shape = panelShape,
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HandShapeBottomStepButton(
                            icon = Icons.Default.SkipPrevious,
                            label = fingerUiText(currentLang, "Trước", "Previous"),
                            textColor = textColor,
                            surfaceColor = bgColor.copy(alpha = 0.40f),
                            modifier = Modifier
                                .weight(0.72f)
                                .fillMaxHeight(),
                            onClick = {
                                isPlaying = false
                                countdownValue = 0
                                currentIndex = (currentIndex - 1).coerceAtLeast(0)
                            }
                        )

                        Button(
                            onClick = {
                                when {
                                    isPlaying -> {
                                        isPlaying = false
                                        countdownValue = 0
                                    }

                                    countdownValue > 0 -> {
                                        countdownValue = 0
                                    }

                                    sequence.isNotEmpty() -> {
                                        if (currentIndex >= sequence.lastIndex && !repeatEnabled) {
                                            currentIndex = 0
                                        }
                                        countdownValue = 3
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1.38f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = playAccent),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFF1C1D1F),
                                modifier = Modifier.size(if (veryCompactHeight) 18.dp else 22.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = when {
                                    isPlaying -> fingerUiText(currentLang, "Tạm dừng", "Pause")
                                    countdownValue > 0 -> fingerUiText(currentLang, "Hủy", "Cancel")
                                    else -> fingerUiText(currentLang, "Bắt đầu", "Start")
                                },
                                color = Color(0xFF1C1D1F),
                                fontSize = if (veryCompactHeight) 11.sp else 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }

                        HandShapeBottomStepButton(
                            icon = Icons.Default.SkipNext,
                            label = fingerUiText(currentLang, "Sau", "Next"),
                            textColor = textColor,
                            surfaceColor = bgColor.copy(alpha = 0.40f),
                            modifier = Modifier
                                .weight(0.72f)
                                .fillMaxHeight(),
                            onClick = {
                                isPlaying = false
                                countdownValue = 0
                                if (sequence.isNotEmpty()) {
                                    currentIndex = (currentIndex + 1).coerceAtMost(sequence.lastIndex)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (countdownValue > 0) {
        PracticeCountdownOverlay(
            value = countdownValue,
            accent = uiAccent
        )
    }
}

@Composable
private fun HandShapeTrainingPlayerScreen(
    initialShape: CagedShape,
    instrument: String,
    currentLang: String,
    bgColor: Color,
    surfaceColor: Color,
    textColor: Color,
    tunaGreen: Color,
    onOpenExercises: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val isVietnamese = currentLang.lowercase(Locale.ROOT).startsWith("vi")
    val effectiveInstrument = "Guitar"

    val uiAccent = tunaGreen
    val rootAccent = Color(0xFFE84D78)
    val currentAccent = Color(0xFF35A7FF)
    val scaleNoteColor = Color(0xFFD0D4D8)
    val playAccent = Color(0xFFF2B94B)
    val borderColor = textColor.copy(alpha = 0.10f)
    val muted = textColor.copy(alpha = 0.58f)

    var tonicKey by rememberSaveable(initialShape.key) { mutableStateOf("C") }
    var scaleTypeKey by rememberSaveable(initialShape.key) {
        mutableStateOf(HandShapeScaleType.MAJOR.key)
    }
    var shapeKey by rememberSaveable(initialShape.key) { mutableStateOf(initialShape.key) }
    var directionKey by rememberSaveable(initialShape.key) { mutableStateOf("FORWARD") }
    var fretViewKey by rememberSaveable(initialShape.key) { mutableStateOf("FULL") }
    var bpm by rememberSaveable(initialShape.key) { mutableFloatStateOf(60f) }
    var soundEnabled by rememberSaveable(initialShape.key) { mutableStateOf(true) }
    var repeatEnabled by rememberSaveable(initialShape.key) { mutableStateOf(true) }
    var isPlaying by rememberSaveable(initialShape.key) { mutableStateOf(false) }
    var countdownValue by rememberSaveable(initialShape.key) { mutableIntStateOf(0) }
    var hasPlaybackStarted by rememberSaveable(initialShape.key) { mutableStateOf(false) }
    var currentIndex by rememberSaveable(initialShape.key) { mutableIntStateOf(0) }
    var completedRounds by rememberSaveable(initialShape.key) { mutableIntStateOf(0) }

    val selectedShape = cagedShapes().firstOrNull { it.key == shapeKey } ?: initialShape
    val selectedScaleType = handShapeScaleTypes().firstOrNull { it.key == scaleTypeKey }
        ?: HandShapeScaleType.MAJOR
    val pattern = remember(tonicKey, selectedScaleType, selectedShape) {
        buildHandShapePattern(tonicKey, selectedScaleType, selectedShape)
    }
    val sequence = remember(pattern, directionKey) {
        buildHandShapeSequence(pattern, directionKey)
    }

    val currentNote = sequence.getOrNull(currentIndex)
    val nextNote = when {
        currentIndex < sequence.lastIndex -> sequence.getOrNull(currentIndex + 1)
        repeatEnabled && sequence.isNotEmpty() -> sequence.first()
        else -> null
    }
    val progress = if (sequence.isEmpty()) 0f else (currentIndex + 1f) / sequence.size.toFloat()

    val fullFretEnd = maxOf(12, pattern.endFret)
    val visibleStartFret = if (fretViewKey == "SHAPE") pattern.startFret else 0
    val visibleEndFret = if (fretViewKey == "SHAPE") pattern.endFret else fullFretEnd

    fun resetSession() {
        isPlaying = false
        countdownValue = 0
        hasPlaybackStarted = false
        currentIndex = 0
        completedRounds = 0
    }

    fun stopAndClose() {
        isPlaying = false
        countdownValue = 0
        onClose()
    }

    BackHandler {
        if (countdownValue > 0) {
            countdownValue = 0
        } else {
            stopAndClose()
        }
    }

    DisposableEffect(activity) {
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            if (activity != null && !activity.isChangingConfigurations && previousOrientation != null) {
                activity.requestedOrientation = previousOrientation
            }
        }
    }

    LaunchedEffect(Unit) {
        runCatching { CoreAudioPlayer.loadInstrument(context, effectiveInstrument) }
    }

    LaunchedEffect(sequence) {
        resetSession()
    }

    LaunchedEffect(countdownValue) {
        if (countdownValue <= 0) return@LaunchedEffect

        delay(1_000L)
        if (countdownValue > 1) {
            countdownValue--
        } else {
            countdownValue = 0
            hasPlaybackStarted = true
            isPlaying = true
        }
    }

    LaunchedEffect(isPlaying, bpm, repeatEnabled, sequence) {
        if (!isPlaying || sequence.isEmpty()) return@LaunchedEffect

        while (isPlaying) {
            val intervalMs = (60_000f / bpm.coerceIn(40f, 160f)).toLong()
            delay(intervalMs)

            if (currentIndex < sequence.lastIndex) {
                currentIndex++
            } else {
                completedRounds++
                if (repeatEnabled) {
                    currentIndex = 0
                } else {
                    isPlaying = false
                    countdownValue = 0
                }
            }
        }
    }

    LaunchedEffect(isPlaying, currentIndex, soundEnabled, sequence) {
        if (isPlaying && soundEnabled) {
            sequence.getOrNull(currentIndex)?.let { note ->
                runCatching {
                    CoreAudioPlayer.playChord(listOf(note.midi), effectiveInstrument)
                }
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "handShapePulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(560, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "handShapePulseValue"
    )

    val configuration = LocalConfiguration.current
    val veryCompactHeight = configuration.screenHeightDp < 620
    val compactHeight = configuration.screenHeightDp < 720
    val compactWidth = configuration.screenWidthDp < 380

    val outerPadding = if (compactWidth) 6.dp else 8.dp
    val topBarHeight = when {
        veryCompactHeight -> 46.dp
        compactHeight -> 52.dp
        else -> 58.dp
    }
    val bottomControlHeight = when {
        veryCompactHeight -> 50.dp
        compactHeight -> 56.dp
        else -> 62.dp
    }
    val panelGap = if (veryCompactHeight) 3.dp else 5.dp
    val panelShape = RoundedCornerShape(if (compactWidth) 13.dp else 16.dp)

    val tonicOptions = handShapeNoteNames.map { it to it }
    val scaleOptions = handShapeScaleTypes().map { it.key to it.title(currentLang) }
    val shapeOptions = cagedShapes().map { it.key to it.title(currentLang) }
    val directionOptions = listOf(
        "FORWARD" to fingerUiText(currentLang, "Đi lên", "Ascending"),
        "REVERSE" to fingerUiText(currentLang, "Đi xuống", "Descending"),
        "BOTH" to fingerUiText(currentLang, "Cả hai", "Both")
    )
    val fretViewOptions = listOf(
        // Các thế tay CAGED cũng mặc định hiển thị toàn cần để thấy vùng liên kết giữa các dạng.
        "FULL" to fingerUiText(
            currentLang,
            "Ngăn 1-$fullFretEnd",
            "Frets 1-$fullFretEnd"
        ),
        "SHAPE" to fingerUiText(
            currentLang,
            "Vùng thế tay ${maxOf(1, pattern.startFret)}-${pattern.endFret}",
            "Shape area ${maxOf(1, pattern.startFret)}-${pattern.endFret}"
        )
    )

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = bgColor) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = outerPadding, vertical = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(topBarHeight),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { stopAndClose() },
                        modifier = Modifier
                            .size(if (veryCompactHeight) 34.dp else 40.dp)
                            .clip(CircleShape)
                            .background(surfaceColor)
                            .border(1.dp, borderColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = if (isVietnamese) "Quay lại" else "Back",
                            tint = textColor,
                            modifier = Modifier.size(if (veryCompactHeight) 18.dp else 21.dp)
                        )
                    }

                    Spacer(Modifier.width(if (compactWidth) 7.dp else 10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fingerUiText(currentLang, "Luyện thế tay", "Hand-position practice"),
                            color = textColor,
                            fontSize = when {
                                veryCompactHeight -> 16.sp
                                compactHeight -> 18.sp
                                else -> 20.sp
                            },
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "CAGED · ${selectedShape.title(currentLang)}",
                            color = muted,
                            fontSize = if (veryCompactHeight) 8.5.sp else 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = uiAccent.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            uiAccent.copy(alpha = 0.42f)
                        )
                    ) {
                        Text(
                            text = "${bpm.roundToInt()} BPM",
                            color = uiAccent,
                            fontSize = if (veryCompactHeight) 10.sp else 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(
                                horizontal = if (compactWidth) 9.dp else 13.dp,
                                vertical = 5.dp
                            )
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    IconButton(
                        onClick = { resetSession() },
                        modifier = Modifier
                            .size(if (veryCompactHeight) 34.dp else 40.dp)
                            .clip(CircleShape)
                            .background(surfaceColor)
                            .border(1.dp, borderColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = if (isVietnamese) "Đặt lại" else "Reset",
                            tint = textColor,
                            modifier = Modifier.size(if (veryCompactHeight) 17.dp else 20.dp)
                        )
                    }
                }

                FingerPracticeSectionSelector(
                    selectedSection = FingerPracticeSection.HAND_SHAPES,
                    currentLang = currentLang,
                    exerciseAccent = Color(0xFF22C58B),
                    handShapeAccent = rootAccent,
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    compact = veryCompactHeight,
                    onSelected = { section ->
                        if (section == FingerPracticeSection.EXERCISES) {
                            isPlaying = false
                            countdownValue = 0
                            onOpenExercises()
                        }
                    }
                )

                Spacer(Modifier.height(panelGap))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(panelGap)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(if (compactWidth) 1.28f else 1.42f)
                            .fillMaxHeight(),
                        shape = panelShape,
                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    horizontal = if (compactWidth) 4.dp else 6.dp,
                                    vertical = 4.dp
                                )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$tonicKey ${selectedScaleType.title(currentLang)}",
                                    color = textColor,
                                    fontSize = if (veryCompactHeight) 9.5.sp else 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = if (sequence.isEmpty()) {
                                        "0/0"
                                    } else {
                                        "${currentIndex + 1}/${sequence.size}"
                                    },
                                    color = uiAccent,
                                    fontSize = if (veryCompactHeight) 8.sp else 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(Modifier.height(2.dp))

                            HandShapeFretboardCanvas(
                                pattern = pattern,
                                sequence = sequence,
                                currentIndex = currentIndex,
                                currentNote = currentNote,
                                nextNote = nextNote,
                                visibleStartFret = visibleStartFret,
                                visibleEndFret = visibleEndFret,
                                textColor = textColor,
                                currentAccent = currentAccent,
                                completedAccent = uiAccent,
                                rootAccent = rootAccent,
                                scaleNoteColor = scaleNoteColor,
                                highlightCurrent = hasPlaybackStarted,
                                pulse = if (isPlaying) pulse else 1f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )

                            LinearProgressIndicator(
                                progress = progress.coerceIn(0f, 1f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = uiAccent,
                                trackColor = textColor.copy(alpha = 0.08f)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(if (compactWidth) 1f else 0.96f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(panelGap)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1.48f),
                            shape = panelShape,
                            colors = CardDefaults.cardColors(containerColor = surfaceColor),
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(if (veryCompactHeight) 4.dp else 6.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                HandShapeSideDropdown(
                                    label = fingerUiText(currentLang, "Tông (Key)", "Key"),
                                    selectedKey = tonicKey,
                                    selectedLabel = tonicKey,
                                    options = tonicOptions,
                                    accent = uiAccent,
                                    surfaceColor = bgColor.copy(alpha = 0.45f),
                                    textColor = textColor,
                                    compact = veryCompactHeight,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    onSelected = {
                                        isPlaying = false
                                        countdownValue = 0
                                        tonicKey = it
                                    }
                                )
                                HandShapeSideDropdown(
                                    label = fingerUiText(currentLang, "Âm giai", "Scale"),
                                    selectedKey = scaleTypeKey,
                                    selectedLabel = selectedScaleType.title(currentLang),
                                    options = scaleOptions,
                                    accent = uiAccent,
                                    surfaceColor = bgColor.copy(alpha = 0.45f),
                                    textColor = textColor,
                                    compact = veryCompactHeight,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    onSelected = {
                                        isPlaying = false
                                        countdownValue = 0
                                        scaleTypeKey = it
                                    }
                                )
                                HandShapeSideDropdown(
                                    label = fingerUiText(currentLang, "Thế tay", "Shape"),
                                    selectedKey = shapeKey,
                                    selectedLabel = selectedShape.title(currentLang),
                                    options = shapeOptions,
                                    accent = uiAccent,
                                    surfaceColor = bgColor.copy(alpha = 0.45f),
                                    textColor = textColor,
                                    compact = veryCompactHeight,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    onSelected = {
                                        isPlaying = false
                                        countdownValue = 0
                                        shapeKey = it
                                    }
                                )
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.72f),
                            shape = panelShape,
                            colors = CardDefaults.cardColors(containerColor = surfaceColor),
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    HandShapeLegendEntry(
                                        color = rootAccent,
                                        label = fingerUiText(currentLang, "Nốt gốc", "Root"),
                                        textColor = textColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                    HandShapeLegendEntry(
                                        color = scaleNoteColor,
                                        label = fingerUiText(currentLang, "Âm giai", "Scale"),
                                        textColor = textColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    HandShapeLegendEntry(
                                        color = currentAccent,
                                        label = fingerUiText(currentLang, "Hiện tại", "Current"),
                                        textColor = textColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                    HandShapeLegendEntry(
                                        color = Color.White,
                                        label = fingerUiText(currentLang, "Tiếp theo", "Next"),
                                        textColor = textColor,
                                        outlined = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1.02f),
                            shape = panelShape,
                            colors = CardDefaults.cardColors(containerColor = surfaceColor),
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(if (veryCompactHeight) 4.dp else 6.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                HandShapeSideDropdown(
                                    label = fingerUiText(currentLang, "Hướng luyện", "Direction"),
                                    selectedKey = directionKey,
                                    selectedLabel = directionOptions.firstOrNull {
                                        it.first == directionKey
                                    }?.second.orEmpty(),
                                    options = directionOptions,
                                    accent = uiAccent,
                                    surfaceColor = bgColor.copy(alpha = 0.45f),
                                    textColor = textColor,
                                    compact = veryCompactHeight,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    onSelected = {
                                        isPlaying = false
                                        countdownValue = 0
                                        directionKey = it
                                    }
                                )
                                HandShapeSideDropdown(
                                    label = fingerUiText(currentLang, "Vị trí cần đàn", "Fret range"),
                                    selectedKey = fretViewKey,
                                    selectedLabel = fretViewOptions.firstOrNull {
                                        it.first == fretViewKey
                                    }?.second.orEmpty(),
                                    options = fretViewOptions,
                                    accent = uiAccent,
                                    surfaceColor = bgColor.copy(alpha = 0.45f),
                                    textColor = textColor,
                                    compact = veryCompactHeight,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    onSelected = { fretViewKey = it }
                                )
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.82f),
                            shape = panelShape,
                            colors = CardDefaults.cardColors(containerColor = surfaceColor),
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                HandShapeSwitchRow(
                                    icon = if (soundEnabled) {
                                        Icons.Default.VolumeUp
                                    } else {
                                        Icons.Default.VolumeOff
                                    },
                                    label = fingerUiText(currentLang, "Âm mẫu", "Sound"),
                                    checked = soundEnabled,
                                    accent = uiAccent,
                                    textColor = textColor,
                                    compact = veryCompactHeight,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    onCheckedChange = { soundEnabled = it }
                                )
                                HorizontalDivider(color = borderColor)
                                HandShapeSwitchRow(
                                    icon = Icons.Default.Refresh,
                                    label = fingerUiText(currentLang, "Lặp lại", "Repeat"),
                                    checked = repeatEnabled,
                                    accent = uiAccent,
                                    textColor = textColor,
                                    compact = veryCompactHeight,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    onCheckedChange = { repeatEnabled = it }
                                )
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.50f),
                            shape = panelShape,
                            colors = CardDefaults.cardColors(containerColor = surfaceColor),
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                IconButton(
                                    onClick = { bpm = (bpm - 5f).coerceAtLeast(40f) },
                                    modifier = Modifier
                                        .size(if (veryCompactHeight) 30.dp else 34.dp)
                                        .clip(CircleShape)
                                        .background(uiAccent.copy(alpha = 0.16f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = if (isVietnamese) "Giảm BPM" else "Decrease BPM",
                                        tint = uiAccent,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Text(
                                    text = "${bpm.roundToInt()} BPM",
                                    color = textColor,
                                    fontSize = if (veryCompactHeight) 9.5.sp else 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { bpm = (bpm + 5f).coerceAtMost(160f) },
                                    modifier = Modifier
                                        .size(if (veryCompactHeight) 30.dp else 34.dp)
                                        .clip(CircleShape)
                                        .background(uiAccent.copy(alpha = 0.16f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = if (isVietnamese) "Tăng BPM" else "Increase BPM",
                                        tint = uiAccent,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(panelGap))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(bottomControlHeight),
                    shape = panelShape,
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HandShapeBottomStepButton(
                            icon = Icons.Default.SkipPrevious,
                            label = fingerUiText(currentLang, "Trước", "Previous"),
                            textColor = textColor,
                            surfaceColor = bgColor.copy(alpha = 0.40f),
                            modifier = Modifier
                                .weight(0.72f)
                                .fillMaxHeight(),
                            onClick = {
                                isPlaying = false
                                countdownValue = 0
                                currentIndex = (currentIndex - 1).coerceAtLeast(0)
                            }
                        )

                        Button(
                            onClick = {
                                when {
                                    isPlaying -> {
                                        isPlaying = false
                                        countdownValue = 0
                                    }

                                    countdownValue > 0 -> {
                                        countdownValue = 0
                                    }

                                    sequence.isNotEmpty() -> {
                                        if (currentIndex >= sequence.lastIndex && !repeatEnabled) {
                                            currentIndex = 0
                                        }
                                        countdownValue = 3
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1.38f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = playAccent),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFF1C1D1F),
                                modifier = Modifier.size(if (veryCompactHeight) 18.dp else 22.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = when {
                                    isPlaying -> fingerUiText(currentLang, "Tạm dừng", "Pause")
                                    countdownValue > 0 -> fingerUiText(currentLang, "Hủy", "Cancel")
                                    else -> fingerUiText(currentLang, "Bắt đầu", "Start")
                                },
                                color = Color(0xFF1C1D1F),
                                fontSize = if (veryCompactHeight) 11.sp else 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }

                        HandShapeBottomStepButton(
                            icon = Icons.Default.SkipNext,
                            label = fingerUiText(currentLang, "Sau", "Next"),
                            textColor = textColor,
                            surfaceColor = bgColor.copy(alpha = 0.40f),
                            modifier = Modifier
                                .weight(0.72f)
                                .fillMaxHeight(),
                            onClick = {
                                isPlaying = false
                                countdownValue = 0
                                if (sequence.isNotEmpty()) {
                                    currentIndex = (currentIndex + 1).coerceAtMost(sequence.lastIndex)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (countdownValue > 0) {
        PracticeCountdownOverlay(
            value = countdownValue,
            accent = uiAccent
        )
    }
}


@Composable
private fun HarmonyProgressionPlayerScreen(
    currentLang: String,
    bgColor: Color,
    surfaceColor: Color,
    textColor: Color,
    tunaGreen: Color,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val isVietnamese = currentLang.lowercase(Locale.ROOT).startsWith("vi")
    val progressions = remember { harmonyProgressions() }

    var practiceTypeKey by rememberSaveable { mutableStateOf(HarmonyPracticeType.TRIAD.key) }
    var keyName by rememberSaveable { mutableStateOf("C") }
    var progressionKey by rememberSaveable { mutableStateOf("II_V_I_MAJOR") }
    var stringSetKey by rememberSaveable { mutableStateOf("STRINGS_123") }
    var inversionKey by rememberSaveable { mutableStateOf(HarmonyInversionMode.AUTO.key) }
    var directionKey by rememberSaveable { mutableStateOf(HarmonyRunDirection.ASCENDING.key) }
    var positionStart by rememberSaveable { mutableIntStateOf(5) }
    var bpm by rememberSaveable { mutableFloatStateOf(60f) }
    var soundEnabled by rememberSaveable { mutableStateOf(true) }
    var repeatEnabled by rememberSaveable { mutableStateOf(true) }
    var lookAheadEnabled by rememberSaveable { mutableStateOf(true) }
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var countdownValue by rememberSaveable { mutableIntStateOf(0) }
    var hasPlaybackStarted by rememberSaveable { mutableStateOf(false) }
    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    var completedRounds by rememberSaveable { mutableIntStateOf(0) }

    val practiceType = HarmonyPracticeType.values().firstOrNull { it.key == practiceTypeKey }
        ?: HarmonyPracticeType.TRIAD
    val progression = progressions.firstOrNull { it.key == progressionKey }
        ?: progressions.first()
    val stringSetOptions = remember(practiceType) { harmonyStringSets(practiceType) }
    val stringSet = stringSetOptions.firstOrNull { it.key == stringSetKey }
        ?: stringSetOptions.first()
    val inversionOptions = remember(practiceType) { harmonyInversionModes(practiceType) }
    val inversionMode = inversionOptions.firstOrNull { it.key == inversionKey }
        ?: HarmonyInversionMode.AUTO
    val direction = HarmonyRunDirection.values().firstOrNull { it.key == directionKey }
        ?: HarmonyRunDirection.ASCENDING

    LaunchedEffect(practiceType.key) {
        if (stringSetOptions.none { it.key == stringSetKey }) {
            stringSetKey = stringSetOptions.first().key
        }
        if (inversionOptions.none { it.key == inversionKey }) {
            inversionKey = HarmonyInversionMode.AUTO.key
        }
    }

    val fretRange = positionStart..(positionStart + 4)
    val chords = remember(keyName, progression.key, practiceType.key) {
        buildHarmonyChords(keyName, progression, practiceType)
    }
    val voicings = remember(chords, stringSet.key, fretRange, inversionMode.key) {
        buildHarmonyVoicings(
            chords = chords,
            stringSet = stringSet,
            fretRange = fretRange,
            inversionMode = inversionMode
        )
    }
    val sequence = remember(voicings, direction.key) {
        buildHarmonyPlaybackSequence(voicings, direction)
    }

    val currentStep = sequence.getOrNull(currentIndex)
    val currentChordIndex = currentStep?.chordIndex ?: 0
    val currentVoicing = voicings.getOrNull(currentChordIndex)
    val nextChordIndex = if (voicings.isEmpty()) 0 else (currentChordIndex + 1) % voicings.size
    val nextVoicing = if (lookAheadEnabled) voicings.getOrNull(nextChordIndex) else null
    val currentChord = currentVoicing?.chord ?: chords.firstOrNull()
    val nextStep = sequence.getOrNull(currentIndex + 1)
        ?: if (repeatEnabled) sequence.firstOrNull() else null
    val completedCurrentNotes = remember(sequence, currentIndex, currentChordIndex) {
        sequence.take(currentIndex)
            .filter { it.chordIndex == currentChordIndex }
            .map { it.note.stringNumber to it.note.fret }
            .toSet()
    }
    val allVoicingFrets = voicings.flatMap { voicing -> voicing.notes.map { it.fret } }
    val visibleStartFret = minOf(positionStart, allVoicingFrets.minOrNull() ?: positionStart)
        .coerceAtLeast(0)
    val visibleEndFret = maxOf(positionStart + 4, allVoicingFrets.maxOrNull() ?: positionStart + 4)
        .coerceAtMost(20)
    val keyPitchClass = harmonyNoteNames.indexOf(keyName).coerceAtLeast(0)
    val progress = if (sequence.isEmpty()) 0f else (currentIndex + 1f) / sequence.size.toFloat()

    val rootAccent = tunaGreen
    val thirdAccent = Color(0xFFFF8A50)
    val chordToneAccent = Color(0xFF6D91E8)
    val seventhAccent = Color(0xFFB274F2)
    val currentAccent = Color(0xFFF2B94B)
    val uiAccent = Color(0xFFE85D86)
    val borderColor = textColor.copy(alpha = 0.10f)
    val muted = textColor.copy(alpha = 0.58f)

    fun resetSession() {
        isPlaying = false
        countdownValue = 0
        hasPlaybackStarted = false
        currentIndex = 0
        completedRounds = 0
    }

    fun stopAndClose() {
        isPlaying = false
        countdownValue = 0
        onClose()
    }

    BackHandler {
        if (countdownValue > 0) {
            countdownValue = 0
        } else {
            stopAndClose()
        }
    }

    DisposableEffect(activity) {
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            if (activity != null && !activity.isChangingConfigurations && previousOrientation != null) {
                activity.requestedOrientation = previousOrientation
            }
        }
    }

    LaunchedEffect(Unit) {
        runCatching { CoreAudioPlayer.loadInstrument(context, "Guitar") }
    }

    LaunchedEffect(sequence) {
        resetSession()
    }

    LaunchedEffect(countdownValue) {
        if (countdownValue <= 0) return@LaunchedEffect
        delay(1_000L)
        if (countdownValue > 1) {
            countdownValue--
        } else {
            countdownValue = 0
            hasPlaybackStarted = true
            isPlaying = true
        }
    }

    LaunchedEffect(isPlaying, bpm, repeatEnabled, sequence) {
        if (!isPlaying || sequence.isEmpty()) return@LaunchedEffect
        while (isPlaying) {
            val intervalMs = (60_000f / bpm.coerceIn(40f, 180f)).toLong()
            delay(intervalMs)
            if (currentIndex < sequence.lastIndex) {
                currentIndex++
            } else {
                completedRounds++
                if (repeatEnabled) {
                    currentIndex = 0
                } else {
                    isPlaying = false
                    countdownValue = 0
                }
            }
        }
    }

    LaunchedEffect(isPlaying, currentIndex, soundEnabled, sequence) {
        if (isPlaying && soundEnabled) {
            sequence.getOrNull(currentIndex)?.let { step ->
                runCatching {
                    CoreAudioPlayer.playChord(listOf(step.note.midi), "Guitar")
                }
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "harmonyPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(560, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "harmonyPulseValue"
    )

    val configuration = LocalConfiguration.current
    val veryCompactHeight = configuration.screenHeightDp < 620
    val compactHeight = configuration.screenHeightDp < 720
    val compactWidth = configuration.screenWidthDp < 380
    val outerPadding = if (compactWidth) 6.dp else 8.dp
    val topBarHeight = if (veryCompactHeight) 46.dp else 52.dp
    val selectorHeight = if (veryCompactHeight) 39.dp else 44.dp
    val summaryHeight = if (veryCompactHeight) 58.dp else 68.dp
    val bottomControlHeight = if (veryCompactHeight) 49.dp else 56.dp
    val panelGap = if (veryCompactHeight) 3.dp else 5.dp
    val panelShape = RoundedCornerShape(if (compactWidth) 13.dp else 16.dp)

    val practiceTypeOptions = HarmonyPracticeType.values().map { it.key to it.title(currentLang) }
    val keyOptions = harmonyNoteNames.map { it to it }
    val progressionOptions = progressions.map { it.key to it.title(currentLang) }
    val stringOptions = stringSetOptions.map { it.key to it.title(currentLang) }
    val inversionDropdownOptions = inversionOptions.map { it.key to it.title(currentLang) }
    val directionOptions = HarmonyRunDirection.values().map { it.key to it.title(currentLang) }
    val positionOptions = listOf(1, 3, 5, 7, 9).map { start ->
        start.toString() to fingerUiText(
            currentLang,
            "Ngăn $start-${start + 4}",
            "Frets $start-${start + 4}"
        )
    }
    val chordFlow = chords.joinToString("  →  ") { chord -> "${chord.roman} ${chord.symbol}" }
    val inversionText = currentVoicing?.let { voicing ->
        when (voicing.inversionIndex) {
            0 -> fingerUiText(currentLang, "Thế gốc", "Root")
            1 -> fingerUiText(currentLang, "Đảo 1", "1st inv.")
            2 -> fingerUiText(currentLang, "Đảo 2", "2nd inv.")
            else -> fingerUiText(currentLang, "Đảo 3", "3rd inv.")
        }
    }.orEmpty()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = bgColor) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = outerPadding, vertical = 3.dp),
                verticalArrangement = Arrangement.spacedBy(panelGap)
            ) {
                // Header remains fixed, as in the standard Finger Training player.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(topBarHeight),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { stopAndClose() },
                        modifier = Modifier
                            .size(if (veryCompactHeight) 34.dp else 40.dp)
                            .clip(CircleShape)
                            .background(surfaceColor)
                            .border(1.dp, borderColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = if (isVietnamese) "Quay lại" else "Back",
                            tint = textColor,
                            modifier = Modifier.size(if (veryCompactHeight) 18.dp else 21.dp)
                        )
                    }

                    Spacer(Modifier.width(if (compactWidth) 7.dp else 10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fingerUiText(currentLang, "Triad & Arpeggio", "Triads & Arpeggios"),
                            color = textColor,
                            fontSize = if (veryCompactHeight) 16.sp else 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$keyName ${progression.tonality.title(currentLang)} · ${progression.title(currentLang)}",
                            color = muted,
                            fontSize = if (veryCompactHeight) 8.5.sp else 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { resetSession() },
                        modifier = Modifier
                            .size(if (veryCompactHeight) 34.dp else 40.dp)
                            .clip(CircleShape)
                            .background(surfaceColor)
                            .border(1.dp, borderColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = if (isVietnamese) "Đặt lại" else "Reset",
                            tint = textColor,
                            modifier = Modifier.size(if (veryCompactHeight) 17.dp else 20.dp)
                        )
                    }
                }

                // Keep the current chord and progression readable at full width.
                // The fretboard and all practice settings now share the area below.
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(summaryHeight),
                    shape = panelShape,
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentChord?.let { "${it.roman} · ${it.symbol}" }
                                        ?: progression.title(currentLang),
                                    color = textColor,
                                    fontSize = if (veryCompactHeight) 13.sp else 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = currentStep?.let { step ->
                                        "${midiDisplayName(step.note.midi)} · ${step.note.roleLabel} · $inversionText"
                                    } ?: progression.description(currentLang),
                                    color = currentAccent,
                                    fontSize = if (veryCompactHeight) 8.5.sp else 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = if (sequence.isEmpty()) "0/0" else "${currentIndex + 1}/${sequence.size}",
                                color = uiAccent,
                                fontSize = if (veryCompactHeight) 9.sp else 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text(
                            text = chordFlow,
                            color = muted,
                            fontSize = if (veryCompactHeight) 7.8.sp else 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.weight(1f))
                        LinearProgressIndicator(
                            progress = progress.coerceIn(0f, 1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(50)),
                            color = uiAccent,
                            trackColor = textColor.copy(alpha = 0.08f)
                        )
                    }
                }

                // Side-by-side portrait layout: fretboard LEFT, settings RIGHT.
                // The settings alone scroll on short displays; the fretboard and
                // playback buttons never move while parameters are being changed.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(panelGap)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(if (compactWidth) 1.32f else 1.43f)
                            .fillMaxHeight(),
                        shape = panelShape,
                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = if (compactWidth) 2.dp else 4.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = fingerUiText(
                                    currentLang,
                                    "Ngăn $visibleStartFret-$visibleEndFret · Vòng ${completedRounds + 1}",
                                    "Frets $visibleStartFret-$visibleEndFret · Round ${completedRounds + 1}"
                                ),
                                color = textColor,
                                fontSize = if (compactWidth) 8.sp else 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
                            )

                            HarmonyFretboardCanvas(
                                currentVoicing = currentVoicing,
                                nextVoicing = nextVoicing,
                                currentNote = if (hasPlaybackStarted) currentStep?.note else null,
                                nextNote = if (hasPlaybackStarted) nextStep?.note else null,
                                completedPositions = completedCurrentNotes,
                                keyPitchClass = keyPitchClass,
                                scaleIntervals = progression.tonality.scaleIntervals,
                                visibleStartFret = visibleStartFret,
                                visibleEndFret = visibleEndFret,
                                textColor = textColor,
                                rootAccent = rootAccent,
                                thirdAccent = thirdAccent,
                                chordToneAccent = chordToneAccent,
                                seventhAccent = seventhAccent,
                                currentAccent = currentAccent,
                                showLookAhead = lookAheadEnabled,
                                pulse = if (isPlaying) pulse else 1f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (veryCompactHeight) 18.dp else 22.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HarmonyLegendItem("R", rootAccent, textColor)
                                HarmonyLegendItem("3", thirdAccent, textColor)
                                HarmonyLegendItem("5", chordToneAccent, textColor)
                                if (practiceType == HarmonyPracticeType.SEVENTH_ARPEGGIO) {
                                    HarmonyLegendItem("7", seventhAccent, textColor)
                                }
                                HarmonyLegendItem(
                                    fingerUiText(currentLang, "Tiếp", "Next"),
                                    Color.White.copy(alpha = 0.78f),
                                    textColor,
                                    outlined = true
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(if (compactWidth) 1.00f else 0.99f)
                            .fillMaxHeight(),
                        shape = panelShape,
                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 4.dp, vertical = 5.dp),
                            verticalArrangement = Arrangement.spacedBy(panelGap)
                        ) {
                            Text(
                                text = fingerUiText(currentLang, "THIẾT LẬP", "SETTINGS"),
                                color = muted,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(start = 3.dp, bottom = 1.dp)
                            )

                            CompactDropdownSelector(
                                label = fingerUiText(currentLang, "Kiểu", "Type"),
                                selectedKey = practiceType.key,
                                selectedLabel = practiceType.title(currentLang),
                                options = practiceTypeOptions,
                                accent = uiAccent,
                                surfaceColor = bgColor.copy(alpha = 0.50f),
                                textColor = textColor,
                                modifier = Modifier.fillMaxWidth().height(selectorHeight),
                                onSelected = { practiceTypeKey = it }
                            )
                            CompactDropdownSelector(
                                label = fingerUiText(currentLang, "Tông", "Key"),
                                selectedKey = keyName,
                                selectedLabel = keyName,
                                options = keyOptions,
                                accent = rootAccent,
                                surfaceColor = bgColor.copy(alpha = 0.50f),
                                textColor = textColor,
                                modifier = Modifier.fillMaxWidth().height(selectorHeight),
                                onSelected = { keyName = it }
                            )
                            CompactDropdownSelector(
                                label = fingerUiText(currentLang, "Vòng", "Progression"),
                                selectedKey = progression.key,
                                selectedLabel = progression.title(currentLang),
                                options = progressionOptions,
                                accent = uiAccent,
                                surfaceColor = bgColor.copy(alpha = 0.50f),
                                textColor = textColor,
                                modifier = Modifier.fillMaxWidth().height(selectorHeight),
                                onSelected = { progressionKey = it }
                            )
                            CompactDropdownSelector(
                                label = fingerUiText(currentLang, "Nhóm dây", "String set"),
                                selectedKey = stringSet.key,
                                selectedLabel = stringSet.title(currentLang),
                                options = stringOptions,
                                accent = chordToneAccent,
                                surfaceColor = bgColor.copy(alpha = 0.50f),
                                textColor = textColor,
                                modifier = Modifier.fillMaxWidth().height(selectorHeight),
                                onSelected = { stringSetKey = it }
                            )
                            CompactDropdownSelector(
                                label = fingerUiText(currentLang, "Thế đảo", "Inversion"),
                                selectedKey = inversionMode.key,
                                selectedLabel = inversionMode.title(currentLang),
                                options = inversionDropdownOptions,
                                accent = thirdAccent,
                                surfaceColor = bgColor.copy(alpha = 0.50f),
                                textColor = textColor,
                                modifier = Modifier.fillMaxWidth().height(selectorHeight),
                                onSelected = { inversionKey = it }
                            )
                            CompactDropdownSelector(
                                label = fingerUiText(currentLang, "Chiều", "Pattern"),
                                selectedKey = direction.key,
                                selectedLabel = direction.title(currentLang),
                                options = directionOptions,
                                accent = seventhAccent,
                                surfaceColor = bgColor.copy(alpha = 0.50f),
                                textColor = textColor,
                                modifier = Modifier.fillMaxWidth().height(selectorHeight),
                                onSelected = { directionKey = it }
                            )
                            CompactDropdownSelector(
                                label = fingerUiText(currentLang, "Vị trí", "Position"),
                                selectedKey = positionStart.toString(),
                                selectedLabel = positionOptions.firstOrNull { it.first == positionStart.toString() }?.second
                                    ?: "${positionStart}-${positionStart + 4}",
                                options = positionOptions,
                                accent = rootAccent,
                                surfaceColor = bgColor.copy(alpha = 0.50f),
                                textColor = textColor,
                                modifier = Modifier.fillMaxWidth().height(selectorHeight),
                                onSelected = { positionStart = it.toIntOrNull() ?: 5 }
                            )

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(selectorHeight),
                                shape = RoundedCornerShape(11.dp),
                                colors = CardDefaults.cardColors(containerColor = bgColor.copy(alpha = 0.50f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { bpm = (bpm - 5f).coerceAtLeast(40f) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = if (isVietnamese) "Giảm BPM" else "Decrease BPM",
                                            tint = uiAccent,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Text(
                                        text = "${bpm.roundToInt()} BPM",
                                        color = textColor,
                                        fontSize = if (compactWidth) 9.sp else 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { bpm = (bpm + 5f).coerceAtMost(180f) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = if (isVietnamese) "Tăng BPM" else "Increase BPM",
                                            tint = uiAccent,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }

                            HarmonySideToggleRow(
                                icon = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                label = fingerUiText(currentLang, "Âm thanh", "Sound"),
                                selected = soundEnabled,
                                accent = uiAccent,
                                textColor = textColor,
                                backgroundColor = bgColor.copy(alpha = 0.50f),
                                onClick = { soundEnabled = !soundEnabled }
                            )
                            HarmonySideToggleRow(
                                icon = Icons.Default.ArrowForward,
                                label = fingerUiText(currentLang, "Nhìn trước", "Look ahead"),
                                selected = lookAheadEnabled,
                                accent = rootAccent,
                                textColor = textColor,
                                backgroundColor = bgColor.copy(alpha = 0.50f),
                                onClick = { lookAheadEnabled = !lookAheadEnabled }
                            )
                            HarmonySideToggleRow(
                                icon = Icons.Default.Refresh,
                                label = fingerUiText(currentLang, "Lặp lại", "Repeat"),
                                selected = repeatEnabled,
                                accent = chordToneAccent,
                                textColor = textColor,
                                backgroundColor = bgColor.copy(alpha = 0.50f),
                                onClick = { repeatEnabled = !repeatEnabled }
                            )
                        }
                    }
                }

                // The transport remains fixed at the bottom, like Finger Training.
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(bottomControlHeight),
                    shape = panelShape,
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HandShapeBottomStepButton(
                            icon = Icons.Default.SkipPrevious,
                            label = fingerUiText(currentLang, "Trước", "Previous"),
                            textColor = textColor,
                            surfaceColor = bgColor.copy(alpha = 0.40f),
                            modifier = Modifier
                                .weight(0.72f)
                                .fillMaxHeight(),
                            onClick = {
                                isPlaying = false
                                countdownValue = 0
                                currentIndex = (currentIndex - 1).coerceAtLeast(0)
                                hasPlaybackStarted = true
                            }
                        )

                        Button(
                            onClick = {
                                when {
                                    isPlaying -> {
                                        isPlaying = false
                                        countdownValue = 0
                                    }
                                    countdownValue > 0 -> {
                                        countdownValue = 0
                                    }
                                    sequence.isNotEmpty() -> {
                                        if (currentIndex >= sequence.lastIndex && !repeatEnabled) {
                                            currentIndex = 0
                                        }
                                        countdownValue = 3
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1.38f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = currentAccent),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFF1C1D1F),
                                modifier = Modifier.size(if (veryCompactHeight) 18.dp else 22.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = when {
                                    isPlaying -> fingerUiText(currentLang, "Tạm dừng", "Pause")
                                    countdownValue > 0 -> fingerUiText(currentLang, "Hủy", "Cancel")
                                    else -> fingerUiText(currentLang, "Bắt đầu", "Start")
                                },
                                color = Color(0xFF1C1D1F),
                                fontSize = if (veryCompactHeight) 11.sp else 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }

                        HandShapeBottomStepButton(
                            icon = Icons.Default.SkipNext,
                            label = fingerUiText(currentLang, "Sau", "Next"),
                            textColor = textColor,
                            surfaceColor = bgColor.copy(alpha = 0.40f),
                            modifier = Modifier
                                .weight(0.72f)
                                .fillMaxHeight(),
                            onClick = {
                                isPlaying = false
                                countdownValue = 0
                                if (sequence.isNotEmpty()) {
                                    currentIndex = (currentIndex + 1).coerceAtMost(sequence.lastIndex)
                                    hasPlaybackStarted = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (countdownValue > 0) {
        PracticeCountdownOverlay(value = countdownValue, accent = uiAccent)
    }
}

@Composable
private fun HarmonySideToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    accent: Color,
    textColor: Color,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.13f) else backgroundColor)
            .border(
                1.dp,
                if (selected) accent.copy(alpha = 0.40f) else textColor.copy(alpha = 0.10f),
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) accent else textColor.copy(alpha = 0.50f),
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) accent else textColor.copy(alpha = 0.68f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(if (selected) accent else Color.Transparent)
                .border(
                    1.dp,
                    if (selected) accent else textColor.copy(alpha = 0.50f),
                    CircleShape
                )
        )
    }
}

@Composable
private fun HarmonyLegendItem(
    label: String,
    color: Color,
    textColor: Color,
    outlined: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(if (outlined) Color.Transparent else color)
                .border(1.dp, color, CircleShape)
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = label,
            color = textColor.copy(alpha = 0.64f),
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun HarmonyFretboardCanvas(
    currentVoicing: HarmonyVoicing?,
    nextVoicing: HarmonyVoicing?,
    currentNote: HarmonyFretNote?,
    nextNote: HarmonyFretNote?,
    completedPositions: Set<Pair<Int, Int>>,
    keyPitchClass: Int,
    scaleIntervals: Set<Int>,
    visibleStartFret: Int,
    visibleEndFret: Int,
    textColor: Color,
    rootAccent: Color,
    thirdAccent: Color,
    chordToneAccent: Color,
    seventhAccent: Color,
    currentAccent: Color,
    showLookAhead: Boolean,
    pulse: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val stringNames = mapOf(6 to "E", 5 to "A", 4 to "D", 3 to "G", 2 to "B", 1 to "e")
    val sideLabelPx = with(density) { 22.dp.toPx() }
    val rightPaddingPx = with(density) { 6.dp.toPx() }
    val stringLabelHeightPx = with(density) { 21.dp.toPx() }
    val openRowHeightPx = with(density) { 18.dp.toPx() }
    val bottomPaddingPx = with(density) { 4.dp.toPx() }
    val stringTextSizePx = with(density) { 8.sp.toPx() }
    val fretTextSizePx = with(density) { 8.sp.toPx() }
    val noteTextSizePx = with(density) { 8.2.sp.toPx() }

    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas

        val stringNumbers = (6 downTo 1).toList()
        val left = sideLabelPx
        val right = (size.width - rightPaddingPx).coerceAtLeast(left + 1f)
        val labelY = stringLabelHeightPx * 0.55f
        val showOpenRow = visibleStartFret <= 0
        val boardStartFret = maxOf(1, visibleStartFret)
        val boardEndFret = maxOf(boardStartFret, visibleEndFret)
        val fretCells = boardEndFret - boardStartFret + 1
        val nutY = stringLabelHeightPx + if (showOpenRow) {
            openRowHeightPx
        } else {
            with(density) { 3.dp.toPx() }
        }
        val bottom = (size.height - bottomPaddingPx).coerceAtLeast(nutY + 1f)
        val stringSpacing = (right - left) / 5f
        val fretSpacing = (bottom - nutY) / fretCells.toFloat()

        fun xForString(stringNumber: Int): Float = left + (6 - stringNumber) * stringSpacing
        fun yForFret(fret: Int): Float {
            if (fret == 0 && showOpenRow) {
                return nutY - openRowHeightPx * 0.52f
            }
            val safeFret = fret.coerceIn(boardStartFret, boardEndFret)
            return nutY + (safeFret - boardStartFret + 0.5f) * fretSpacing
        }
        fun isVisibleFret(fret: Int): Boolean =
            fret == 0 && showOpenRow || fret in boardStartFret..boardEndFret

        fun roleColor(note: HarmonyFretNote): Color = when {
            note.isRoot -> rootAccent
            note.isThird -> thirdAccent
            note.interval == 9 || note.interval == 10 || note.interval == 11 -> seventhAccent
            else -> chordToneAccent
        }

        val neckLeft = left - stringSpacing * 0.30f
        val neckRight = right + stringSpacing * 0.30f
        drawRoundRect(
            color = Color(0xFF1A1716),
            topLeft = Offset(neckLeft, nutY),
            size = androidx.compose.ui.geometry.Size(neckRight - neckLeft, bottom - nutY),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                with(density) { 9.dp.toPx() },
                with(density) { 9.dp.toPx() }
            )
        )

        for (cell in 0 until fretCells) {
            val cellTop = nutY + cell * fretSpacing
            drawRect(
                color = if (cell % 2 == 0) Color(0xFF241D1A) else Color(0xFF1D1918),
                topLeft = Offset(neckLeft, cellTop),
                size = androidx.compose.ui.geometry.Size(neckRight - neckLeft, fretSpacing)
            )
        }

        val markerFrets = setOf(3, 5, 7, 9, 12, 15, 17, 19)
        for (fret in boardStartFret..boardEndFret) {
            if (fret in markerFrets) {
                val markerY = yForFret(fret)
                val radius = minOf(with(density) { 2.8.dp.toPx() }, fretSpacing * 0.15f)
                if (fret % 12 == 0) {
                    drawCircle(textColor.copy(alpha = 0.28f), radius, Offset((xForString(5) + xForString(4)) / 2f, markerY))
                    drawCircle(textColor.copy(alpha = 0.28f), radius, Offset((xForString(3) + xForString(2)) / 2f, markerY))
                } else {
                    drawCircle(textColor.copy(alpha = 0.24f), radius, Offset((left + right) / 2f, markerY))
                }
            }
        }

        for (line in 0..fretCells) {
            val y = nutY + line * fretSpacing
            val isNut = showOpenRow && line == 0
            drawLine(
                color = if (isNut) Color(0xFFE6C785) else Color(0xFFB8BEC4).copy(alpha = 0.72f),
                start = Offset(neckLeft, y),
                end = Offset(neckRight, y),
                strokeWidth = if (isNut) {
                    with(density) { 3.5.dp.toPx() }
                } else {
                    with(density) { 1.dp.toPx() }
                },
                cap = StrokeCap.Round
            )
        }

        stringNumbers.forEach { stringNumber ->
            val x = xForString(stringNumber)
            val baseWidth = when (stringNumber) {
                6 -> 2.35f
                5 -> 2.05f
                4 -> 1.75f
                3 -> 1.35f
                2 -> 1.05f
                else -> 0.85f
            }
            drawLine(
                color = if (stringNumber >= 4) Color(0xFFC78A4D) else Color(0xFFD5D9DE),
                start = Offset(x, nutY),
                end = Offset(x, bottom),
                strokeWidth = with(density) { baseWidth.dp.toPx() },
                cap = StrokeCap.Round
            )
        }

        val stringPaint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textSize = stringTextSizePx
            color = textColor.toArgb()
        }
        val labelRadius = minOf(stringSpacing * 0.36f, with(density) { 10.5.dp.toPx() })
        stringNumbers.forEach { stringNumber ->
            val center = Offset(xForString(stringNumber), labelY)
            drawCircle(textColor.copy(alpha = 0.06f), labelRadius, center)
            drawCircle(
                textColor.copy(alpha = 0.25f),
                labelRadius,
                center,
                style = Stroke(width = with(density) { 0.8.dp.toPx() })
            )
            val label = "$stringNumber${stringNames[stringNumber].orEmpty()}"
            val textY = center.y - (stringPaint.descent() + stringPaint.ascent()) / 2f
            drawContext.canvas.nativeCanvas.drawText(label, center.x, textY, stringPaint)
        }

        val fretPaint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.RIGHT
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textSize = fretTextSizePx
            color = textColor.copy(alpha = 0.62f).toArgb()
        }
        for (fret in boardStartFret..boardEndFret) {
            val y = yForFret(fret) - (fretPaint.descent() + fretPaint.ascent()) / 2f
            drawContext.canvas.nativeCanvas.drawText(
                fret.toString(),
                neckLeft - with(density) { 5.dp.toPx() },
                y,
                fretPaint
            )
        }

        val backgroundRadius = minOf(
            stringSpacing * 0.12f,
            fretSpacing * 0.12f,
            with(density) { 3.4.dp.toPx() }
        ).coerceAtLeast(with(density) { 1.8.dp.toPx() })
        stringNumbers.forEach { stringNumber ->
            val openMidi = guitarOpenMidiByString.getValue(stringNumber)
            val visibleFrets = if (showOpenRow) {
                listOf(0) + (boardStartFret..boardEndFret).toList()
            } else {
                (boardStartFret..boardEndFret).toList()
            }
            visibleFrets.forEach { fret ->
                val interval = normalizedPitchClass(openMidi + fret - keyPitchClass)
                if (interval in scaleIntervals) {
                    drawCircle(
                        color = textColor.copy(alpha = 0.10f),
                        radius = backgroundRadius,
                        center = Offset(xForString(stringNumber), yForFret(fret))
                    )
                }
            }
        }

        val normalRadius = minOf(
            stringSpacing * 0.34f,
            fretSpacing * 0.34f,
            with(density) { 12.dp.toPx() }
        ).coerceAtLeast(with(density) { 4.6.dp.toPx() })

        if (showLookAhead && nextVoicing != null) {
            nextVoicing.notes
                .filter { isVisibleFret(it.fret) }
                .forEach { note ->
                    val center = Offset(xForString(note.stringNumber), yForFret(note.fret))
                    drawCircle(
                        color = Color.White.copy(alpha = 0.58f),
                        radius = normalRadius * 0.93f,
                        center = center,
                        style = Stroke(width = with(density) { 1.2.dp.toPx() })
                    )
                }
        }

        currentVoicing?.notes
            ?.sortedBy { it.midi }
            ?.zipWithNext()
            ?.forEach { (from, to) ->
                if (isVisibleFret(from.fret) && isVisibleFret(to.fret)) {
                    drawLine(
                        color = roleColor(from).copy(alpha = 0.22f),
                        start = Offset(xForString(from.stringNumber), yForFret(from.fret)),
                        end = Offset(xForString(to.stringNumber), yForFret(to.fret)),
                        strokeWidth = with(density) { 1.1.dp.toPx() },
                        cap = StrokeCap.Round
                    )
                }
            }

        currentVoicing?.notes
            ?.filter { isVisibleFret(it.fret) }
            ?.forEach { note ->
                val position = note.stringNumber to note.fret
                val center = Offset(xForString(note.stringNumber), yForFret(note.fret))
                val isCurrent = currentNote?.let {
                    it.stringNumber == note.stringNumber && it.fret == note.fret
                } == true
                val isNextNote = nextNote?.let {
                    it.stringNumber == note.stringNumber && it.fret == note.fret
                } == true
                val isCompleted = position in completedPositions
                val baseColor = roleColor(note)
                val radius = if (isCurrent) normalRadius * 1.12f * pulse else normalRadius

                if (isCurrent) {
                    drawCircle(currentAccent.copy(alpha = 0.18f), radius * 1.48f, center)
                    drawCircle(currentAccent, radius, center)
                    drawCircle(
                        baseColor,
                        radius,
                        center,
                        style = Stroke(width = with(density) { 1.7.dp.toPx() })
                    )
                } else {
                    drawCircle(
                        color = if (isCompleted) baseColor.copy(alpha = 0.52f) else baseColor.copy(alpha = 0.90f),
                        radius = if (isCompleted) radius * 0.90f else radius,
                        center = center
                    )
                    if (isNextNote) {
                        drawCircle(
                            Color.White.copy(alpha = 0.92f),
                            radius * 1.08f,
                            center,
                            style = Stroke(width = with(density) { 1.3.dp.toPx() })
                        )
                    }
                }

                val paint = android.graphics.Paint().apply {
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    textSize = if (isCurrent) noteTextSizePx * pulse else noteTextSizePx
                    color = android.graphics.Color.WHITE
                }
                val textY = center.y - (paint.descent() + paint.ascent()) / 2f
                drawContext.canvas.nativeCanvas.drawText(note.roleLabel, center.x, textY, paint)
            }
    }
}


@Composable
private fun PracticeCountdownOverlay(
    value: Int,
    accent: Color
) {
    Popup(
        alignment = Alignment.Center,
        properties = PopupProperties(focusable = false)
    ) {
        Surface(
            modifier = Modifier
                .size(88.dp)
                .border(3.dp, accent, CircleShape),
            shape = CircleShape,
            color = Color(0xE61A1D20)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value.toString(),
                    color = accent,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun HandShapeSideDropdown(
    label: String,
    selectedKey: String,
    selectedLabel: String,
    options: List<Pair<String, String>>,
    accent: Color,
    surfaceColor: Color,
    textColor: Color,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(if (compact) 8.dp else 10.dp)

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(surfaceColor)
                .border(1.dp, textColor.copy(alpha = 0.10f), shape)
                .clickable { expanded = true }
                .padding(horizontal = if (compact) 6.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = textColor.copy(alpha = 0.58f),
                    fontSize = if (compact) 7.sp else 8.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = selectedLabel,
                    color = textColor,
                    fontSize = if (compact) 9.sp else 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(if (compact) 15.dp else 18.dp)
            )
        }

        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(surfaceColor)
        ) {
            options.forEach { (key, optionLabel) ->
                androidx.compose.material3.DropdownMenuItem(
                    text = {
                        Text(
                            text = optionLabel,
                            color = if (key == selectedKey) accent else textColor,
                            fontSize = 12.sp,
                            fontWeight = if (key == selectedKey) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(key)
                    }
                )
            }
        }
    }
}

@Composable
private fun HandShapeLegendEntry(
    color: Color,
    label: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    outlined: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(11.dp)
                .clip(CircleShape)
                .background(if (outlined) Color.Transparent else color)
                .border(
                    width = if (outlined) 1.2.dp else 0.dp,
                    color = if (outlined) color else Color.Transparent,
                    shape = CircleShape
                )
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            color = textColor.copy(alpha = 0.66f),
            fontSize = 7.8.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HandShapeSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    checked: Boolean,
    accent: Color,
    textColor: Color,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor.copy(alpha = 0.72f),
            modifier = Modifier.size(if (compact) 15.dp else 18.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            color = textColor.copy(alpha = 0.82f),
            fontSize = if (compact) 8.5.sp else 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(
                width = if (compact) 34.dp else 39.dp,
                height = if (compact) 20.dp else 23.dp
            ),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accent,
                uncheckedThumbColor = textColor.copy(alpha = 0.62f),
                uncheckedTrackColor = textColor.copy(alpha = 0.14f)
            )
        )
    }
}

@Composable
private fun HandShapeBottomStepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    textColor: Color,
    surfaceColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(surfaceColor)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = textColor,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            color = textColor.copy(alpha = 0.82f),
            fontSize = 7.6.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun HandShapeFretboardCanvas(
    pattern: HandShapePattern,
    sequence: List<HandShapeNote>,
    currentIndex: Int,
    currentNote: HandShapeNote?,
    nextNote: HandShapeNote?,
    visibleStartFret: Int,
    visibleEndFret: Int,
    textColor: Color,
    currentAccent: Color,
    completedAccent: Color,
    rootAccent: Color,
    scaleNoteColor: Color,
    highlightCurrent: Boolean,
    pulse: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val completedPositions = remember(sequence, currentIndex, highlightCurrent) {
        if (highlightCurrent) {
            sequence.take(currentIndex).map { it.stringNumber to it.fret }.toSet()
        } else {
            emptySet()
        }
    }
    val stringNames = mapOf(6 to "E", 5 to "A", 4 to "D", 3 to "G", 2 to "B", 1 to "e")

    val sideLabelPx = with(density) { 22.dp.toPx() }
    val rightPaddingPx = with(density) { 6.dp.toPx() }
    val stringLabelHeightPx = with(density) { 22.dp.toPx() }
    val openRowHeightPx = with(density) { 18.dp.toPx() }
    val bottomPaddingPx = with(density) { 4.dp.toPx() }
    val stringTextSizePx = with(density) { 8.5.sp.toPx() }
    val fretTextSizePx = with(density) { 8.5.sp.toPx() }
    val noteTextSizePx = with(density) { 8.5.sp.toPx() }

    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas

        val stringNumbers = (6 downTo 1).toList()
        val left = sideLabelPx
        val right = (size.width - rightPaddingPx).coerceAtLeast(left + 1f)
        val labelY = stringLabelHeightPx * 0.58f
        val showOpenRow = visibleStartFret <= 0
        val nutY = stringLabelHeightPx + if (showOpenRow) openRowHeightPx else with(density) { 4.dp.toPx() }
        val bottom = (size.height - bottomPaddingPx).coerceAtLeast(nutY + 1f)
        val boardStartFret = maxOf(1, visibleStartFret)
        val boardEndFret = maxOf(boardStartFret, visibleEndFret)
        val fretCells = boardEndFret - boardStartFret + 1
        val stringSpacing = (right - left) / 5f
        val fretSpacing = (bottom - nutY) / fretCells.toFloat()

        fun xForString(stringNumber: Int): Float {
            val column = 6 - stringNumber
            return left + column * stringSpacing
        }

        fun yForFret(fret: Int): Float {
            return if (fret == 0 && showOpenRow) {
                nutY - openRowHeightPx * 0.52f
            } else {
                val row = fret - boardStartFret
                nutY + (row + 0.5f) * fretSpacing
            }
        }

        val neckLeft = left - stringSpacing * 0.30f
        val neckRight = right + stringSpacing * 0.30f

        drawRoundRect(
            color = Color(0xFF1A1716),
            topLeft = Offset(neckLeft, nutY),
            size = androidx.compose.ui.geometry.Size(neckRight - neckLeft, bottom - nutY),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                with(density) { 9.dp.toPx() },
                with(density) { 9.dp.toPx() }
            )
        )

        for (cell in 0 until fretCells) {
            val cellTop = nutY + cell * fretSpacing
            drawRect(
                color = if (cell % 2 == 0) {
                    Color(0xFF241D1A).copy(alpha = 0.92f)
                } else {
                    Color(0xFF1D1918).copy(alpha = 0.92f)
                },
                topLeft = Offset(neckLeft, cellTop),
                size = androidx.compose.ui.geometry.Size(neckRight - neckLeft, fretSpacing)
            )
        }

        val markerFrets = setOf(3, 5, 7, 9, 12, 15, 17, 19, 21, 24)
        for (fret in boardStartFret..boardEndFret) {
            if (fret in markerFrets) {
                val markerY = yForFret(fret)
                val markerRadius = minOf(
                    with(density) { 3.dp.toPx() },
                    fretSpacing * 0.16f
                )
                if (fret % 12 == 0) {
                    drawCircle(
                        color = textColor.copy(alpha = 0.32f),
                        radius = markerRadius,
                        center = Offset((xForString(5) + xForString(4)) / 2f, markerY)
                    )
                    drawCircle(
                        color = textColor.copy(alpha = 0.32f),
                        radius = markerRadius,
                        center = Offset((xForString(3) + xForString(2)) / 2f, markerY)
                    )
                } else {
                    drawCircle(
                        color = textColor.copy(alpha = 0.28f),
                        radius = markerRadius,
                        center = Offset((left + right) / 2f, markerY)
                    )
                }
            }
        }

        for (line in 0..fretCells) {
            val y = nutY + line * fretSpacing
            val isNut = visibleStartFret <= 0 && line == 0
            drawLine(
                color = if (isNut) Color(0xFFE6C785) else Color(0xFFB8BEC4).copy(alpha = 0.72f),
                start = Offset(neckLeft, y),
                end = Offset(neckRight, y),
                strokeWidth = if (isNut) {
                    with(density) { 4.dp.toPx() }
                } else {
                    with(density) { 1.dp.toPx() }
                },
                cap = StrokeCap.Round
            )
        }

        val bassStringColor = Color(0xFFC78A4D)
        val trebleStringColor = Color(0xFFD5D9DE)
        stringNumbers.forEach { stringNumber ->
            val x = xForString(stringNumber)
            val baseWidth = when (stringNumber) {
                6 -> 2.35f
                5 -> 2.05f
                4 -> 1.75f
                3 -> 1.35f
                2 -> 1.05f
                else -> 0.85f
            }
            val activeString = highlightCurrent && currentNote?.stringNumber == stringNumber
            drawLine(
                color = if (stringNumber >= 4) bassStringColor else trebleStringColor,
                start = Offset(x, nutY),
                end = Offset(x, bottom),
                strokeWidth = with(density) {
                    (baseWidth + if (activeString) 0.45f else 0f).dp.toPx()
                },
                cap = StrokeCap.Round
            )
        }

        val stringPaint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textSize = stringTextSizePx
            color = textColor.toArgb()
        }

        val labelRadius = minOf(
            stringSpacing * 0.37f,
            with(density) { 11.dp.toPx() }
        )
        stringNumbers.forEach { stringNumber ->
            val center = Offset(xForString(stringNumber), labelY)
            drawCircle(
                color = textColor.copy(alpha = 0.06f),
                radius = labelRadius,
                center = center
            )
            drawCircle(
                color = textColor.copy(alpha = 0.26f),
                radius = labelRadius,
                center = center,
                style = Stroke(width = with(density) { 0.8.dp.toPx() })
            )
            val label = "$stringNumber${stringNames[stringNumber].orEmpty()}"
            val textY = center.y - (stringPaint.descent() + stringPaint.ascent()) / 2f
            drawContext.canvas.nativeCanvas.drawText(label, center.x, textY, stringPaint)
        }

        val fretPaint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.RIGHT
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textSize = fretTextSizePx
            color = textColor.copy(alpha = 0.64f).toArgb()
        }
        for (fret in boardStartFret..boardEndFret) {
            val y = yForFret(fret) - (fretPaint.descent() + fretPaint.ascent()) / 2f
            drawContext.canvas.nativeCanvas.drawText(
                fret.toString(),
                neckLeft - with(density) { 5.dp.toPx() },
                y,
                fretPaint
            )
        }

        val normalRadius = minOf(
            stringSpacing * 0.34f,
            fretSpacing * 0.34f,
            with(density) { 12.dp.toPx() }
        ).coerceAtLeast(with(density) { 4.5.dp.toPx() })
        val activeRadius = normalRadius * 1.12f * pulse
        val nextRadius = normalRadius * 1.02f

        pattern.notes
            .filter { note ->
                note.fret == 0 && showOpenRow || note.fret in boardStartFret..boardEndFret
            }
            .forEach { note ->
                val position = note.stringNumber to note.fret
                val center = Offset(xForString(note.stringNumber), yForFret(note.fret))
                val isCurrent = highlightCurrent && currentNote?.let {
                    it.stringNumber == note.stringNumber && it.fret == note.fret
                } == true
                val isNext = highlightCurrent && nextNote?.let {
                    it.stringNumber == note.stringNumber && it.fret == note.fret
                } == true
                val isCompleted = position in completedPositions

                val baseColor = if (note.isRoot) rootAccent else scaleNoteColor

                when {
                    isCurrent -> {
                        drawCircle(
                            color = currentAccent.copy(alpha = 0.18f),
                            radius = activeRadius * 1.48f,
                            center = center
                        )
                        drawCircle(
                            color = currentAccent,
                            radius = activeRadius,
                            center = center
                        )
                        if (note.isRoot) {
                            drawCircle(
                                color = rootAccent,
                                radius = activeRadius,
                                center = center,
                                style = Stroke(width = with(density) { 1.8.dp.toPx() })
                            )
                        }
                    }

                    isNext -> {
                        drawCircle(
                            color = Color.Transparent,
                            radius = nextRadius,
                            center = center
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.92f),
                            radius = nextRadius,
                            center = center,
                            style = Stroke(width = with(density) { 1.4.dp.toPx() })
                        )
                    }

                    isCompleted -> {
                        drawCircle(
                            color = completedAccent.copy(alpha = 0.58f),
                            radius = normalRadius * 0.90f,
                            center = center
                        )
                        if (note.isRoot) {
                            drawCircle(
                                color = rootAccent.copy(alpha = 0.86f),
                                radius = normalRadius * 0.90f,
                                center = center,
                                style = Stroke(width = with(density) { 1.2.dp.toPx() })
                            )
                        }
                    }

                    else -> {
                        drawCircle(
                            color = baseColor.copy(alpha = if (note.isRoot) 0.96f else 0.84f),
                            radius = normalRadius * 0.86f,
                            center = center
                        )
                    }
                }

                val notePaint = android.graphics.Paint().apply {
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    textSize = if (isCurrent) noteTextSizePx * pulse else noteTextSizePx
                    color = when {
                        isNext -> android.graphics.Color.WHITE
                        isCurrent || note.isRoot || isCompleted -> android.graphics.Color.WHITE
                        else -> android.graphics.Color.rgb(38, 40, 43)
                    }
                }
                val label = if (note.finger == 0) "O" else note.finger.toString()
                val textY = center.y - (notePaint.descent() + notePaint.ascent()) / 2f
                drawContext.canvas.nativeCanvas.drawText(label, center.x, textY, notePaint)
            }

        if (highlightCurrent && currentNote != null && nextNote != null) {
            val currentVisible = currentNote.fret == 0 && showOpenRow ||
                    currentNote.fret in boardStartFret..boardEndFret
            val nextVisible = nextNote.fret == 0 && showOpenRow ||
                    nextNote.fret in boardStartFret..boardEndFret
            if (currentVisible && nextVisible) {
                drawLine(
                    color = currentAccent.copy(alpha = 0.20f),
                    start = Offset(xForString(currentNote.stringNumber), yForFret(currentNote.fret)),
                    end = Offset(xForString(nextNote.stringNumber), yForFret(nextNote.fret)),
                    strokeWidth = with(density) { 1.2.dp.toPx() },
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private fun directionLabel(directionKey: String, lang: String): String = when (directionKey) {
    "REVERSE" -> fingerUiText(lang, "Ngược", "Reverse")
    "BOTH" -> fingerUiText(lang, "Cả hai", "Both")
    else -> fingerUiText(lang, "Xuôi", "Forward")
}

@Composable
private fun CompactDropdownSelector(
    label: String,
    selectedKey: String,
    selectedLabel: String,
    options: List<Pair<String, String>>,
    accent: Color,
    surfaceColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(11.dp)

    Box(modifier = modifier.fillMaxHeight()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(surfaceColor)
                .border(1.dp, textColor.copy(alpha = 0.10f), shape)
                .clickable { expanded = true }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label.uppercase(Locale.getDefault()),
                    color = textColor.copy(alpha = 0.45f),
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = selectedLabel,
                    color = accent,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }

        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(surfaceColor)
        ) {
            options.forEach { (key, optionLabel) ->
                androidx.compose.material3.DropdownMenuItem(
                    text = {
                        Text(
                            text = optionLabel,
                            color = if (key == selectedKey) accent else textColor,
                            fontSize = 13.sp,
                            fontWeight = if (key == selectedKey) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(key)
                    }
                )
            }
        }
    }
}

@Composable
private fun CompactToggleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    accent: Color,
    surfaceColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(11.dp)
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.14f) else surfaceColor)
            .border(
                1.dp,
                if (selected) accent.copy(alpha = 0.45f) else textColor.copy(alpha = 0.10f),
                shape
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) accent else textColor.copy(alpha = 0.60f),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = if (selected) accent else textColor.copy(alpha = 0.60f),
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun PlayerSmallIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    surfaceColor: Color,
    borderColor: Color,
    textColor: Color,
    compact: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(if (compact) 38.dp else 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = textColor,
            modifier = Modifier.size(if (compact) 18.dp else 20.dp)
        )
    }
}


@Composable
private fun FingerTrainingFretboardCanvas(
    instrument: String,
    sequence: List<FingerTrainingStep>,
    currentIndex: Int,
    currentStep: FingerTrainingStep?,
    nextStep: FingerTrainingStep?,
    visibleStartFret: Int,
    visibleEndFret: Int,
    textColor: Color,
    currentAccent: Color,
    completedAccent: Color,
    pendingNoteColor: Color,
    highlightCurrent: Boolean,
    pulse: Float,
    modifier: Modifier = Modifier
) {
    val isUkulele = instrument == "Ukulele"
    val stringNumbers = if (isUkulele) (4 downTo 1).toList() else (6 downTo 1).toList()
    val stringNames = if (isUkulele) {
        mapOf(4 to "G", 3 to "C", 2 to "E", 1 to "A")
    } else {
        mapOf(6 to "E", 5 to "A", 4 to "D", 3 to "G", 2 to "B", 1 to "e")
    }

    // Luyện ngón bắt đầu từ ngăn 1. Không tạo/nghi chú "ngăn 0".
    val safeStartFret = visibleStartFret.coerceAtLeast(1)
    val safeEndFret = visibleEndFret.coerceAtLeast(safeStartFret)
    val density = LocalDensity.current

    val completedPositions = remember(sequence, currentIndex, highlightCurrent) {
        if (highlightCurrent) {
            sequence.take(currentIndex).map { it.stringNumber to it.fret }.toSet()
        } else {
            emptySet()
        }
    }
    val exercisePositions = remember(sequence, safeStartFret, safeEndFret) {
        sequence
            .filter { it.fret in safeStartFret..safeEndFret }
            .map { it.stringNumber to it.fret }
            .toSet()
    }
    val fingerByPosition = remember(sequence) {
        sequence.associate { (it.stringNumber to it.fret) to it.finger }
    }

    val sideLabelPx = with(density) { 22.dp.toPx() }
    val rightPaddingPx = with(density) { 6.dp.toPx() }
    val stringLabelHeightPx = with(density) { 24.dp.toPx() }
    val bottomPaddingPx = with(density) { 4.dp.toPx() }
    val stringTextSizePx = with(density) { 8.5.sp.toPx() }
    val fretTextSizePx = with(density) { 8.5.sp.toPx() }
    val noteTextSizePx = with(density) { 8.5.sp.toPx() }

    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas

        val left = sideLabelPx
        val right = (size.width - rightPaddingPx).coerceAtLeast(left + 1f)
        val labelY = stringLabelHeightPx * 0.48f
        val nutY = stringLabelHeightPx
        val bottom = (size.height - bottomPaddingPx).coerceAtLeast(nutY + 1f)
        val fretCells = (safeEndFret - safeStartFret + 1).coerceAtLeast(1)
        val stringSpacing = if (stringNumbers.size > 1) {
            (right - left) / (stringNumbers.size - 1)
        } else {
            right - left
        }
        val fretSpacing = (bottom - nutY) / fretCells.toFloat()

        fun xForString(stringNumber: Int): Float {
            val column = stringNumbers.indexOf(stringNumber).coerceAtLeast(0)
            return left + column * stringSpacing
        }

        fun yForFret(fret: Int): Float {
            val row = fret - safeStartFret
            return nutY + (row + 0.5f) * fretSpacing
        }

        val neckLeft = left - stringSpacing * 0.30f
        val neckRight = right + stringSpacing * 0.30f
        val neckCorner = with(density) { 9.dp.toPx() }

        // Nền cần đàn giống màn luyện thế tay.
        drawRoundRect(
            color = Color(0xFF1A1716),
            topLeft = Offset(neckLeft, nutY),
            size = androidx.compose.ui.geometry.Size(neckRight - neckLeft, bottom - nutY),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(neckCorner, neckCorner)
        )

        // Mỗi ngăn xen kẽ nhẹ để dễ quan sát khi khoảng cách được thu gọn.
        for (cell in 0 until fretCells) {
            val cellTop = nutY + cell * fretSpacing
            drawRect(
                color = if (cell % 2 == 0) {
                    Color(0xFF241D1A).copy(alpha = 0.92f)
                } else {
                    Color(0xFF1D1918).copy(alpha = 0.92f)
                },
                topLeft = Offset(neckLeft, cellTop),
                size = androidx.compose.ui.geometry.Size(neckRight - neckLeft, fretSpacing)
            )
        }

        // Chấm định vị chuẩn trên cần đàn.
        val markerFrets = setOf(3, 5, 7, 9, 12, 15, 17, 19, 21, 24)
        for (fret in safeStartFret..safeEndFret) {
            if (fret in markerFrets) {
                val markerY = yForFret(fret)
                val markerRadius = minOf(
                    with(density) { 3.dp.toPx() },
                    fretSpacing * 0.16f
                )
                if (fret % 12 == 0 && stringNumbers.size >= 4) {
                    val first = stringNumbers[(stringNumbers.size / 2 - 1).coerceAtLeast(0)]
                    val second = stringNumbers[(stringNumbers.size / 2).coerceAtMost(stringNumbers.lastIndex)]
                    drawCircle(
                        color = textColor.copy(alpha = 0.30f),
                        radius = markerRadius,
                        center = Offset(xForString(first), markerY)
                    )
                    drawCircle(
                        color = textColor.copy(alpha = 0.30f),
                        radius = markerRadius,
                        center = Offset(xForString(second), markerY)
                    )
                } else {
                    drawCircle(
                        color = textColor.copy(alpha = 0.25f),
                        radius = markerRadius,
                        center = Offset((left + right) / 2f, markerY)
                    )
                }
            }
        }

        // Thanh nut luôn ở phía trên. Ngăn đầu tiên hiển thị là ngăn 1, không có ngăn 0.
        for (line in 0..fretCells) {
            val y = nutY + line * fretSpacing
            drawLine(
                color = if (line == 0) Color(0xFFE6C785) else Color(0xFFB8BEC4).copy(alpha = 0.72f),
                start = Offset(neckLeft, y),
                end = Offset(neckRight, y),
                strokeWidth = if (line == 0) {
                    with(density) { 4.dp.toPx() }
                } else {
                    with(density) { 1.dp.toPx() }
                },
                cap = StrokeCap.Round
            )
        }

        // Dây bass màu đồng, dây treble sáng như màn CAGED.
        val bassStringColor = Color(0xFFC78A4D)
        val trebleStringColor = Color(0xFFD5D9DE)
        stringNumbers.forEach { stringNumber ->
            val x = xForString(stringNumber)
            val baseWidth = if (isUkulele) {
                when (stringNumber) {
                    3 -> 1.70f
                    4 -> 1.20f
                    else -> 1.45f
                }
            } else {
                when (stringNumber) {
                    6 -> 2.35f
                    5 -> 2.05f
                    4 -> 1.75f
                    3 -> 1.35f
                    2 -> 1.05f
                    else -> 0.85f
                }
            }
            val activeString = highlightCurrent && currentStep?.stringNumber == stringNumber
            val stringColor = if (isUkulele) {
                if (activeString) currentAccent else textColor.copy(alpha = 0.58f)
            } else {
                if (stringNumber >= 4) bassStringColor else trebleStringColor
            }
            drawLine(
                color = if (activeString) stringColor.copy(alpha = 0.96f) else stringColor,
                start = Offset(x, nutY),
                end = Offset(x, bottom),
                strokeWidth = with(density) {
                    (baseWidth + if (activeString) 0.40f else 0f).dp.toPx()
                },
                cap = StrokeCap.Round
            )
        }

        // Tên dây trong vòng tròn ở phía trên giống màn luyện thế tay.
        val stringPaint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textSize = stringTextSizePx
            color = textColor.toArgb()
        }
        val labelRadius = minOf(
            stringSpacing * 0.37f,
            with(density) { 11.dp.toPx() }
        )
        stringNumbers.forEach { stringNumber ->
            val center = Offset(xForString(stringNumber), labelY)
            val activeString = highlightCurrent && currentStep?.stringNumber == stringNumber
            drawCircle(
                color = if (activeString) currentAccent.copy(alpha = 0.18f) else textColor.copy(alpha = 0.06f),
                radius = labelRadius,
                center = center
            )
            drawCircle(
                color = if (activeString) currentAccent.copy(alpha = 0.80f) else textColor.copy(alpha = 0.26f),
                radius = labelRadius,
                center = center,
                style = Stroke(width = with(density) { 0.8.dp.toPx() })
            )
            stringPaint.color = if (activeString) currentAccent.toArgb() else textColor.toArgb()
            val label = "$stringNumber${stringNames[stringNumber].orEmpty()}"
            val textY = center.y - (stringPaint.descent() + stringPaint.ascent()) / 2f
            drawContext.canvas.nativeCanvas.drawText(label, center.x, textY, stringPaint)
        }

        // Số ngăn bắt đầu từ 1.
        val fretPaint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.RIGHT
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textSize = fretTextSizePx
            color = textColor.copy(alpha = 0.64f).toArgb()
        }
        for (fret in safeStartFret..safeEndFret) {
            val y = yForFret(fret) - (fretPaint.descent() + fretPaint.ascent()) / 2f
            drawContext.canvas.nativeCanvas.drawText(
                fret.toString(),
                neckLeft - with(density) { 5.dp.toPx() },
                y,
                fretPaint
            )
        }

        val normalRadius = minOf(
            stringSpacing * 0.34f,
            fretSpacing * 0.34f,
            with(density) { 12.dp.toPx() }
        ).coerceAtLeast(with(density) { 4.5.dp.toPx() })
        val activeRadius = normalRadius * 1.12f * pulse
        val nextRadius = normalRadius * 1.02f

        // Đường chỉ hướng hiện tại -> tiếp theo.
        if (
            highlightCurrent &&
            currentStep != null &&
            nextStep != null &&
            currentStep.fret in safeStartFret..safeEndFret &&
            nextStep.fret in safeStartFret..safeEndFret
        ) {
            drawLine(
                color = currentAccent.copy(alpha = 0.22f),
                start = Offset(xForString(currentStep.stringNumber), yForFret(currentStep.fret)),
                end = Offset(xForString(nextStep.stringNumber), yForFret(nextStep.fret)),
                strokeWidth = with(density) { 1.2.dp.toPx() },
                cap = StrokeCap.Round
            )
        }

        exercisePositions.forEach { (stringNumber, fret) ->
            val position = stringNumber to fret
            val center = Offset(xForString(stringNumber), yForFret(fret))
            val isCurrent = highlightCurrent && currentStep?.let {
                it.stringNumber == stringNumber && it.fret == fret
            } == true
            val isNext = highlightCurrent && nextStep?.let {
                it.stringNumber == stringNumber && it.fret == fret
            } == true
            val isCompleted = position in completedPositions

            when {
                isCurrent -> {
                    drawCircle(
                        color = currentAccent.copy(alpha = 0.18f),
                        radius = activeRadius * 1.48f,
                        center = center
                    )
                    drawCircle(
                        color = currentAccent,
                        radius = activeRadius,
                        center = center
                    )
                }

                isNext -> {
                    drawCircle(
                        color = Color.Transparent,
                        radius = nextRadius,
                        center = center
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.94f),
                        radius = nextRadius,
                        center = center,
                        style = Stroke(width = with(density) { 1.4.dp.toPx() })
                    )
                }

                isCompleted -> {
                    drawCircle(
                        color = completedAccent.copy(alpha = 0.62f),
                        radius = normalRadius * 0.90f,
                        center = center
                    )
                }

                else -> {
                    drawCircle(
                        color = pendingNoteColor.copy(alpha = 0.86f),
                        radius = normalRadius * 0.86f,
                        center = center
                    )
                }
            }

            val finger = fingerByPosition[position] ?: 1
            val notePaint = android.graphics.Paint().apply {
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textSize = if (isCurrent) noteTextSizePx * pulse else noteTextSizePx
                color = when {
                    isCurrent || isCompleted -> android.graphics.Color.WHITE
                    isNext -> android.graphics.Color.WHITE
                    else -> android.graphics.Color.rgb(38, 40, 43)
                }
            }
            val textY = center.y - (notePaint.descent() + notePaint.ascent()) / 2f
            drawContext.canvas.nativeCanvas.drawText(finger.toString(), center.x, textY, notePaint)
        }
    }
}

/**
 * Màn vào khu vực luyện ngón/nhận diện cao độ.
 * Toàn bộ hình minh họa được vẽ bằng Canvas, không cần ảnh/drawable.
 */
@Composable
private fun FingerTrainingEntryContent(
    instrument: String,
    currentLang: String,
    bgColor: Color,
    surfaceColor: Color,
    textColor: Color,
    tunaGreen: Color,
    onOpenFingerTraining: () -> Unit,
    onOpenHarmonyPractice: () -> Unit,
    onOpenPitchRecognition: () -> Unit,
    onClose: () -> Unit
) {
    val activity = LocalActivity.current
    val configuration = LocalConfiguration.current
    val compactHeight = configuration.screenHeightDp < 660
    val isVietnamese = currentLang.lowercase(Locale.ROOT)
        .substringBefore('-')
        .substringBefore('_') == "vi"

    DisposableEffect(activity) {
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            if (activity != null && previousOrientation != null) {
                activity.requestedOrientation = previousOrientation
            }
        }
    }
    BackHandler(onBack = onClose)

    val fingerEnabled = instrument != "Piano"
    val harmonyEnabled = instrument == "Guitar"
    val harmonyAccent = Color(0xFFE85D86)
    val pitchAccent = Color(0xFF6D91E8)
    val panelShape = RoundedCornerShape(if (compactHeight) 13.dp else 16.dp)
    // Ba lộ trình được giữ gọn để vẫn hiển thị tốt trên điện thoại màn hình thấp.
    val entryCardHeight = if (compactHeight) 92.dp else 104.dp

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = bgColor) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = if (compactHeight) 12.dp else 14.dp)
            ) {
                FingerSectionTopBar(
                    title = if (isVietnamese) "Luyện ngón, Hòa âm & Cao độ" else "Finger, Harmony & Pitch",
                    subtitle = instrument,
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    onBack = onClose
                )

                Spacer(Modifier.height(if (compactHeight) 1.dp else 3.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(panelShape)
                        .background(surfaceColor.copy(alpha = 0.58f))
                        .border(1.dp, textColor.copy(alpha = 0.07f), panelShape)
                        .padding(
                            horizontal = if (compactHeight) 10.dp else 12.dp,
                            vertical = if (compactHeight) 5.dp else 6.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isVietnamese) "Chọn nội dung luyện tập" else "Choose a practice path",
                            color = textColor,
                            fontSize = if (compactHeight) 13.sp else 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (isVietnamese) {
                                "Ba lộ trình: kỹ thuật ngón, hòa âm ứng dụng và phản xạ cao độ."
                            } else {
                                "Three paths: finger technique, applied harmony and pitch response."
                            },
                            color = textColor.copy(alpha = 0.52f),
                            fontSize = if (compactHeight) 8.7.sp else 9.3.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = tunaGreen.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, tunaGreen.copy(alpha = 0.26f))
                    ) {
                        Text(
                            text = instrument,
                            color = tunaGreen,
                            fontSize = if (compactHeight) 8.4.sp else 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(Modifier.height(if (compactHeight) 5.dp else 7.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(if (compactHeight) 6.dp else 8.dp)
                ) {
                    ModernPracticeEntryCard(
                        modifier = Modifier.height(entryCardHeight),
                        number = "01",
                        title = if (isVietnamese) "Luyện ngón" else "Finger training",
                        description = if (isVietnamese) {
                            "Chạy ngón theo 6 bài và luyện 5 thế tay CAGED trực quan trên cần đàn."
                        } else {
                            "Practice six finger drills and five visual CAGED positions on the fretboard."
                        },
                        metaLeft = if (isVietnamese) "6 bài · 5 thế tay" else "6 drills · 5 shapes",
                        metaRight = if (isVietnamese) "Nên bắt đầu" else "Start first",
                        accent = tunaGreen,
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        enabled = fingerEnabled,
                        compact = compactHeight,
                        disabledText = if (isVietnamese) "Chỉ hỗ trợ Guitar / Ukulele" else "Guitar / Ukulele only",
                        preview = {
                            MiniFingerFretboardCanvas(
                                numStrings = if (instrument == "Ukulele") 4 else 6,
                                accent = tunaGreen,
                                textColor = textColor,
                                modifier = Modifier.fillMaxSize()
                            )
                        },
                        onClick = onOpenFingerTraining
                    )

                    ModernPracticeEntryCard(
                        modifier = Modifier.height(entryCardHeight),
                        number = "02",
                        title = if (isVietnamese) "Triad & Arpeggio" else "Triads & Arpeggios",
                        description = if (isVietnamese) {
                            "Chạy hợp âm theo ii-V-I, I-IV-V và các vòng phổ biến với dẫn giọng tự động."
                        } else {
                            "Run ii-V-I, I-IV-V and common progressions with automatic voice-leading."
                        },
                        metaLeft = if (isVietnamese) "9 vòng · 12 tông" else "9 loops · 12 keys",
                        metaRight = "Voice leading",
                        accent = harmonyAccent,
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        enabled = harmonyEnabled,
                        compact = compactHeight,
                        disabledText = if (isVietnamese) "Hiện hỗ trợ Guitar" else "Guitar only for now",
                        preview = {
                            MiniHarmonyProgressionCanvas(
                                accent = harmonyAccent,
                                rootAccent = tunaGreen,
                                textColor = textColor,
                                modifier = Modifier.fillMaxSize()
                            )
                        },
                        onClick = onOpenHarmonyPractice
                    )

                    ModernPracticeEntryCard(
                        modifier = Modifier.height(entryCardHeight),
                        number = "03",
                        title = if (isVietnamese) "Nhận diện nốt & cao độ" else "Note & pitch recognition",
                        description = if (isVietnamese) {
                            "5 bước từ chơi đúng nốt, giữ nhịp đến áp dụng trực tiếp vào bài hát."
                        } else {
                            "Five steps from playing the right note to rhythm and song practice."
                        },
                        metaLeft = if (isVietnamese) "5 bước" else "5 steps",
                        metaRight = if (isVietnamese) "Dùng mic" else "Microphone",
                        accent = pitchAccent,
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        enabled = true,
                        compact = compactHeight,
                        disabledText = "",
                        preview = {
                            MiniPitchRecognitionCanvas(
                                accent = pitchAccent,
                                textColor = textColor,
                                modifier = Modifier.fillMaxSize()
                            )
                        },
                        onClick = onOpenPitchRecognition
                    )
                }

                Spacer(Modifier.height(if (compactHeight) 5.dp else 7.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .background(tunaGreen.copy(alpha = 0.07f))
                        .border(1.dp, tunaGreen.copy(alpha = 0.18f), RoundedCornerShape(13.dp))
                        .padding(horizontal = 10.dp, vertical = if (compactHeight) 5.dp else 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = tunaGreen,
                        modifier = Modifier.size(if (compactHeight) 16.dp else 18.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = if (isVietnamese) {
                            if (fingerEnabled) {
                                "Người mới: bắt đầu Bài 1; sau đó thử Triad ii–V–I ở 60 BPM."
                            } else {
                                "Với Piano, bắt đầu ở Bước 1: Chơi đúng nốt."
                            }
                        } else {
                            if (fingerEnabled) {
                                "Begin with Lesson 1, then try a ii–V–I triad at 60 BPM."
                            } else {
                                "For Piano, begin with Step 1: Play the correct note."
                            }
                        },
                        color = textColor.copy(alpha = 0.62f),
                        fontSize = if (compactHeight) 9.sp else 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(if (compactHeight) 4.dp else 7.dp))
            }
        }
    }
}

@Composable
private fun FingerSectionTopBar(
    title: String,
    subtitle: String,
    surfaceColor: Color,
    textColor: Color,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(surfaceColor)
                .border(1.dp, textColor.copy(alpha = 0.08f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = textColor,
                modifier = Modifier.size(21.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 11.dp)
        ) {
            Text(
                text = title,
                color = textColor,
                fontSize = 18.5.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = textColor.copy(alpha = 0.48f),
                fontSize = 9.8.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ModernPracticeEntryCard(
    modifier: Modifier = Modifier,
    number: String,
    title: String,
    description: String,
    metaLeft: String,
    metaRight: String,
    accent: Color,
    surfaceColor: Color,
    textColor: Color,
    enabled: Boolean,
    compact: Boolean = false,
    disabledText: String,
    preview: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(if (compact) 14.dp else 16.dp)
    val alpha = if (enabled) 1f else 0.46f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.97f)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            accent.copy(alpha = if (enabled) 0.34f else 0.13f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            accent.copy(alpha = if (enabled) 0.10f else 0.025f),
                            accent.copy(alpha = if (enabled) 0.025f else 0.01f),
                            Color.Transparent
                        )
                    )
                )
                .padding(
                    horizontal = if (compact) 9.dp else 11.dp,
                    vertical = if (compact) 7.dp else 8.dp
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = accent.copy(alpha = 0.17f)
                    ) {
                        Text(
                            text = number,
                            color = accent.copy(alpha = alpha),
                            fontSize = if (compact) 8.5.sp else 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = title,
                        color = textColor.copy(alpha = alpha),
                        fontSize = if (compact) 14.sp else 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (!enabled) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = textColor.copy(alpha = 0.34f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(if (compact) 4.dp else 5.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Text(
                            text = if (enabled) description else disabledText,
                            color = textColor.copy(alpha = if (enabled) 0.62f else 0.36f),
                            fontSize = if (compact) 9.sp else 9.7.sp,
                            lineHeight = if (compact) 11.5.sp else 12.5.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.weight(1f))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MetaPill(metaLeft, accent, textColor, enabled)
                            Spacer(Modifier.width(6.dp))
                            MetaPill(metaRight, accent, textColor, enabled)
                        }
                    }

                    Spacer(Modifier.width(if (compact) 7.dp else 8.dp))

                    Box(
                        modifier = Modifier
                            .width(if (compact) 72.dp else 82.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(if (compact) 10.dp else 12.dp))
                            .background(textColor.copy(alpha = 0.025f))
                            .border(
                                1.dp,
                                accent.copy(alpha = if (enabled) 0.16f else 0.06f),
                                RoundedCornerShape(if (compact) 10.dp else 12.dp)
                            )
                            .padding(if (compact) 5.dp else 6.dp)
                    ) {
                        preview()
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaPill(text: String, accent: Color, textColor: Color, enabled: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = if (enabled) 0.11f else 0.04f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = if (enabled) accent else textColor.copy(alpha = 0.32f),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun MiniFingerFretboardCanvas(
    numStrings: Int,
    accent: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val safeStringCount = numStrings.coerceAtLeast(2)
    var activeStep by remember(safeStringCount) { mutableIntStateOf(0) }

    // Mô phỏng mẫu 1-2-3-4 rồi chuyển sang dây kế tiếp.
    LaunchedEffect(safeStringCount) {
        while (true) {
            delay(520L)
            activeStep = (activeStep + 1) % 8
        }
    }

    val transition = rememberInfiniteTransition(label = "miniFingerPreviewPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "miniFingerPreviewPulseValue"
    )

    val density = LocalDensity.current
    val normalTextSize = with(density) { 6.5.sp.toPx() }
    val activeTextSize = with(density) { 7.5.sp.toPx() }
    val thinStroke = with(density) { 0.7.dp.toPx() }
    val normalStroke = with(density) { 0.9.dp.toPx() }
    val nutStroke = with(density) { 2.7.dp.toPx() }

    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas

        val w = size.width
        val h = size.height
        val left = w * 0.15f
        val right = w * 0.88f

        // Giữ thẻ menu gọn nhưng tăng chiều cao riêng của cần đàn để tránh bị bóp dẹt.
        // Hai thẻ vẫn dùng cùng entryCardHeight; chỉ phần Canvas luyện ngón được kéo cao hơn.
        val desiredNeckHeight = minOf(h * 0.86f, w * 1.10f)
        val top = (h - desiredNeckHeight) / 2f
        val bottom = top + desiredNeckHeight
        val fretCount = 4
        val stringGap = (right - left) / (safeStringCount - 1)
        val fretGap = desiredNeckHeight / fretCount

        fun xForString(stringIndex: Int): Float = left + stringIndex * stringGap
        fun yForFret(fret: Int): Float = top + (fret - 0.5f) * fretGap

        drawRoundRect(
            color = textColor.copy(alpha = 0.035f),
            topLeft = Offset(left - stringGap * 0.46f, top - fretGap * 0.08f),
            size = androidx.compose.ui.geometry.Size(
                (right - left) + stringGap * 0.92f,
                desiredNeckHeight + fretGap * 0.14f
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                with(density) { 7.dp.toPx() },
                with(density) { 7.dp.toPx() }
            )
        )

        // Thanh phím nằm ngang; nut trên cùng đậm hơn.
        for (fretLine in 0..fretCount) {
            val y = top + fretLine * fretGap
            drawLine(
                color = if (fretLine == 0) {
                    Color(0xFFD8B16A).copy(alpha = 0.88f)
                } else {
                    textColor.copy(alpha = 0.24f)
                },
                start = Offset(left - stringGap * 0.24f, y),
                end = Offset(right + stringGap * 0.24f, y),
                strokeWidth = if (fretLine == 0) nutStroke else normalStroke,
                cap = StrokeCap.Round
            )
        }

        // Dây đàn chạy dọc; dây bass ở bên trái dày và có sắc đồng.
        for (stringIndex in 0 until safeStringCount) {
            val x = xForString(stringIndex)
            val bassFactor = (safeStringCount - stringIndex).toFloat() / safeStringCount.toFloat()
            drawLine(
                color = if (stringIndex < safeStringCount / 2) {
                    Color(0xFFD2A15F).copy(alpha = 0.72f)
                } else {
                    textColor.copy(alpha = 0.58f)
                },
                start = Offset(x, top),
                end = Offset(x, bottom),
                strokeWidth = thinStroke + normalStroke * bassFactor,
                cap = StrokeCap.Round
            )
        }

        val previewSteps = buildList {
            for (stringIndex in 0..minOf(1, safeStringCount - 1)) {
                for (finger in 1..4) add(stringIndex to finger)
            }
        }
        val safeActiveStep = activeStep.coerceIn(0, previewSteps.lastIndex)

        previewSteps.zipWithNext().forEach { (from, to) ->
            drawLine(
                color = accent.copy(alpha = 0.18f),
                start = Offset(xForString(from.first), yForFret(from.second)),
                end = Offset(xForString(to.first), yForFret(to.second)),
                strokeWidth = normalStroke,
                cap = StrokeCap.Round
            )
        }

        val baseRadius = minOf(
            stringGap * 0.30f,
            fretGap * 0.28f,
            with(density) { 6.3.dp.toPx() }
        ).coerceAtLeast(with(density) { 3.4.dp.toPx() })

        previewSteps.forEachIndexed { index, (stringIndex, finger) ->
            val center = Offset(xForString(stringIndex), yForFret(finger))
            val isCurrent = index == safeActiveStep
            val isCompleted = index < safeActiveStep
            val isNext = index == (safeActiveStep + 1).coerceAtMost(previewSteps.lastIndex)
            val radius = if (isCurrent) baseRadius * 1.18f * pulse else baseRadius

            if (isCurrent) {
                drawCircle(
                    color = accent.copy(alpha = 0.15f),
                    radius = radius * 1.52f,
                    center = center
                )
            }

            drawCircle(
                color = when {
                    isCurrent -> accent
                    isCompleted -> accent.copy(alpha = 0.48f)
                    else -> textColor.copy(alpha = 0.15f)
                },
                radius = radius,
                center = center
            )

            if (isNext && !isCurrent) {
                drawCircle(
                    color = textColor.copy(alpha = 0.88f),
                    radius = radius * 1.12f,
                    center = center,
                    style = Stroke(width = normalStroke)
                )
            }

            val paint = android.graphics.Paint().apply {
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textSize = if (isCurrent) activeTextSize * pulse else normalTextSize
                color = when {
                    isCurrent || isCompleted -> android.graphics.Color.WHITE
                    else -> textColor.copy(alpha = 0.72f).toArgb()
                }
            }
            val textY = center.y - (paint.descent() + paint.ascent()) / 2f
            drawContext.canvas.nativeCanvas.drawText(finger.toString(), center.x, textY, paint)
        }

        val current = previewSteps[safeActiveStep]
        val next = previewSteps.getOrNull(safeActiveStep + 1)
        if (next != null && current.first != next.first) {
            drawLine(
                color = accent.copy(alpha = 0.72f),
                start = Offset(xForString(current.first), yForFret(current.second)),
                end = Offset(xForString(next.first), yForFret(next.second)),
                strokeWidth = normalStroke * 1.5f,
                cap = StrokeCap.Round
            )
        }
    }
}


@Composable
private fun MiniHarmonyProgressionCanvas(
    accent: Color,
    rootAccent: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    var activeChord by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(680L)
            activeChord = (activeChord + 1) % 3
        }
    }

    val transition = rememberInfiniteTransition(label = "miniHarmonyPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "miniHarmonyPulseValue"
    )
    val density = LocalDensity.current
    val labelTextSize = with(density) { 7.sp.toPx() }
    val noteTextSize = with(density) { 6.sp.toPx() }

    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas

        val left = size.width * 0.13f
        val right = size.width * 0.87f
        val top = size.height * 0.16f
        val bottom = size.height * 0.88f
        val chordXs = listOf(left, (left + right) / 2f, right)
        val romans = listOf("ii", "V", "I")
        val noteRows = listOf(
            listOf(0.70f, 0.48f, 0.28f),
            listOf(0.66f, 0.44f, 0.24f),
            listOf(0.72f, 0.50f, 0.30f)
        )

        for (stringIndex in 0..3) {
            val x = size.width * (0.20f + stringIndex * 0.20f)
            drawLine(
                color = textColor.copy(alpha = 0.18f),
                start = Offset(x, top + size.height * 0.13f),
                end = Offset(x, bottom),
                strokeWidth = with(density) { (1.2f - stringIndex * 0.12f).dp.toPx() },
                cap = StrokeCap.Round
            )
        }
        for (fretLine in 0..3) {
            val y = top + size.height * 0.16f + fretLine * (bottom - top - size.height * 0.16f) / 3f
            drawLine(
                color = textColor.copy(alpha = 0.13f),
                start = Offset(size.width * 0.14f, y),
                end = Offset(size.width * 0.86f, y),
                strokeWidth = with(density) { 0.7.dp.toPx() }
            )
        }

        chordXs.zipWithNext().forEach { (from, to) ->
            drawLine(
                color = accent.copy(alpha = 0.42f),
                start = Offset(from + size.width * 0.055f, top * 0.82f),
                end = Offset(to - size.width * 0.055f, top * 0.82f),
                strokeWidth = with(density) { 1.dp.toPx() },
                cap = StrokeCap.Round
            )
        }

        val romanPaint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textSize = labelTextSize
        }
        val notePaint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textSize = noteTextSize
            color = android.graphics.Color.WHITE
        }

        chordXs.forEachIndexed { chordIndex, x ->
            val isActive = chordIndex == activeChord
            romanPaint.color = (if (isActive) accent else textColor.copy(alpha = 0.54f)).toArgb()
            drawContext.canvas.nativeCanvas.drawText(
                romans[chordIndex],
                x,
                top * 0.78f,
                romanPaint
            )

            noteRows[chordIndex].forEachIndexed { noteIndex, ratioY ->
                val center = Offset(
                    x + (noteIndex - 1) * size.width * 0.035f,
                    top + (bottom - top) * ratioY
                )
                val baseRadius = minOf(size.width, size.height) * 0.075f
                val radius = if (isActive && noteIndex == activeChord % 3) {
                    baseRadius * pulse
                } else {
                    baseRadius * 0.82f
                }
                val noteColor = when (noteIndex) {
                    0 -> rootAccent
                    1 -> Color(0xFFFF8A50)
                    else -> Color(0xFF6D91E8)
                }
                if (isActive && noteIndex == activeChord % 3) {
                    drawCircle(noteColor.copy(alpha = 0.18f), radius * 1.45f, center)
                }
                drawCircle(
                    color = noteColor.copy(alpha = if (isActive) 0.96f else 0.42f),
                    radius = radius,
                    center = center
                )
                val label = when (noteIndex) {
                    0 -> "R"
                    1 -> "3"
                    else -> "5"
                }
                val textY = center.y - (notePaint.descent() + notePaint.ascent()) / 2f
                drawContext.canvas.nativeCanvas.drawText(label, center.x, textY, notePaint)
            }
        }
    }
}


@Composable
private fun MiniPitchRecognitionCanvas(
    accent: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    var activeNote by remember { mutableIntStateOf(0) }

    // Chuỗi minh họa thống nhất giữa khuông nhạc và TAB:
    // C3 - D3 - E3 - F3 - G3 trên guitar.
    // TAB tương ứng: A3 - D0 - D2 - D3 - G0.
    LaunchedEffect(Unit) {
        while (true) {
            delay(680L)
            activeNote = (activeNote + 1) % 5
        }
    }

    val transition = rememberInfiniteTransition(label = "miniPitchPreviewPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "miniPitchPreviewPulseValue"
    )

    val density = LocalDensity.current
    val labelTextSize = with(density) { 6.sp.toPx() }
    val tabTextSize = with(density) { 7.sp.toPx() }
    val clefTextSize = with(density) { 15.sp.toPx() }
    val lineStroke = with(density) { 0.75.dp.toPx() }
    val activeStroke = with(density) { 1.15.dp.toPx() }

    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas

        val w = size.width
        val h = size.height
        val staffLeft = w * 0.055f
        val notationStart = w * 0.27f
        val systemRight = w * 0.97f

        val staffTop = h * 0.08f
        val staffBottom = h * 0.39f
        val tabTop = h * 0.59f
        val tabBottom = h * 0.92f
        val staffGap = (staffBottom - staffTop) / 4f
        val tabGap = (tabBottom - tabTop) / 5f
        val barLineX = notationStart

        drawRoundRect(
            color = textColor.copy(alpha = 0.022f),
            topLeft = Offset.Zero,
            size = androidx.compose.ui.geometry.Size(w, h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                with(density) { 7.dp.toPx() },
                with(density) { 7.dp.toPx() }
            )
        )

        // Năm dòng khuông nhạc.
        repeat(5) { index ->
            val y = staffTop + index * staffGap
            drawLine(
                color = textColor.copy(alpha = 0.34f),
                start = Offset(staffLeft, y),
                end = Offset(systemRight, y),
                strokeWidth = lineStroke
            )
        }

        // Sáu dây TAB: e, B, G, D, A, E từ trên xuống dưới.
        repeat(6) { index ->
            val y = tabTop + index * tabGap
            drawLine(
                color = textColor.copy(alpha = 0.30f),
                start = Offset(notationStart, y),
                end = Offset(systemRight, y),
                strokeWidth = lineStroke
            )
        }

        drawLine(
            color = textColor.copy(alpha = 0.52f),
            start = Offset(barLineX, staffTop),
            end = Offset(barLineX, staffBottom),
            strokeWidth = activeStroke
        )
        drawLine(
            color = textColor.copy(alpha = 0.52f),
            start = Offset(barLineX, tabTop),
            end = Offset(barLineX, tabBottom),
            strokeWidth = activeStroke
        )

        // Khóa Sol. Chuỗi đang minh họa giọng C trưởng nên không vẽ dấu hóa.
        val clefPaint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.NORMAL)
            textSize = clefTextSize
            color = textColor.copy(alpha = 0.88f).toArgb()
        }
        val staffCenterY = (staffTop + staffBottom) / 2f
        val clefBaseline = staffCenterY - (clefPaint.ascent() + clefPaint.descent()) / 2f
        drawContext.canvas.nativeCanvas.drawText(
            "𝄞",
            staffLeft + (notationStart - staffLeft) * 0.42f,
            clefBaseline,
            clefPaint
        )

        val tabLabelPaint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textSize = labelTextSize
            color = textColor.copy(alpha = 0.70f).toArgb()
        }
        val tabLabelX = staffLeft + (notationStart - staffLeft) * 0.40f
        val tabLabelCenter = (tabTop + tabBottom) / 2f
        listOf("T", "A", "B").forEachIndexed { index, value ->
            drawContext.canvas.nativeCanvas.drawText(
                value,
                tabLabelX,
                tabLabelCenter + (index - 1) * labelTextSize * 1.05f,
                tabLabelPaint
            )
        }

        // Bậc khuông được tính tương đối với dòng dưới cùng E4:
        // C4=-2, D4=-1, E4=0, F4=1, G4=2.
        // Guitar phát thấp hơn ký âm một quãng tám, nên các vị trí TAB bên dưới
        // (C3-D3-E3-F3-G3) khớp chính xác với các nốt viết C4-D4-E4-F4-G4.
        val staffSteps = listOf(-2, -1, 0, 1, 2)
        val tabRows = listOf(4, 3, 3, 3, 2)
        val tabFrets = listOf(3, 0, 2, 3, 0)
        val xStart = notationStart + (systemRight - notationStart) * 0.10f
        val xEnd = systemRight - (systemRight - notationStart) * 0.07f
        val noteGap = (xEnd - xStart) / 4f

        fun xForNote(index: Int): Float = xStart + index * noteGap
        fun yForStaffStep(step: Int): Float = staffBottom - step * (staffGap / 2f)
        fun yForTabRow(row: Int): Float = tabTop + row * tabGap

        val safeActive = activeNote.coerceIn(0, staffSteps.lastIndex)
        val playHeadX = xForNote(safeActive)
        drawLine(
            color = accent.copy(alpha = 0.13f),
            start = Offset(playHeadX, staffTop - staffGap * 0.25f),
            end = Offset(playHeadX, tabBottom + tabGap * 0.20f),
            strokeWidth = activeStroke,
            cap = StrokeCap.Round
        )

        staffSteps.indices.forEach { index ->
            val x = xForNote(index)
            val step = staffSteps[index]
            val staffY = yForStaffStep(step)
            val tabY = yForTabRow(tabRows[index])
            val isCurrent = index == safeActive
            val isCompleted = index < safeActive
            val noteColor = when {
                isCurrent -> accent
                isCompleted -> accent.copy(alpha = 0.52f)
                else -> textColor.copy(alpha = 0.78f)
            }

            val noteWidth = with(density) { 5.5.dp.toPx() } * if (isCurrent) pulse else 1f
            val noteHeight = with(density) { 3.9.dp.toPx() } * if (isCurrent) pulse else 1f

            // Dòng phụ cho nốt C4 nằm dưới khuông.
            if (step < 0) {
                for (ledgerStep in -2 downTo step step 2) {
                    val ledgerY = yForStaffStep(ledgerStep)
                    drawLine(
                        color = textColor.copy(alpha = 0.70f),
                        start = Offset(x - noteWidth * 0.95f, ledgerY),
                        end = Offset(x + noteWidth * 0.95f, ledgerY),
                        strokeWidth = lineStroke
                    )
                }
            }

            if (isCurrent) {
                drawCircle(
                    color = accent.copy(alpha = 0.12f),
                    radius = noteWidth * 1.45f,
                    center = Offset(x, staffY)
                )
            }

            drawOval(
                color = noteColor,
                topLeft = Offset(x - noteWidth / 2f, staffY - noteHeight / 2f),
                size = androidx.compose.ui.geometry.Size(noteWidth, noteHeight)
            )
            drawLine(
                color = noteColor,
                start = Offset(x + noteWidth * 0.42f, staffY),
                end = Offset(x + noteWidth * 0.42f, staffY - staffGap * 1.65f),
                strokeWidth = if (isCurrent) activeStroke else lineStroke,
                cap = StrokeCap.Round
            )

            // Che dây TAB ngay dưới số ngăn để số luôn rõ.
            val fretText = tabFrets[index].toString()
            val textHalfWidth = with(density) {
                if (fretText.length > 1) 5.5.dp.toPx() else 4.dp.toPx()
            }
            drawRoundRect(
                color = Color(0xFF111417),
                topLeft = Offset(x - textHalfWidth, tabY - with(density) { 4.dp.toPx() }),
                size = androidx.compose.ui.geometry.Size(
                    textHalfWidth * 2f,
                    with(density) { 8.dp.toPx() }
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                    with(density) { 2.dp.toPx() },
                    with(density) { 2.dp.toPx() }
                )
            )

            val tabPaint = android.graphics.Paint().apply {
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textSize = tabTextSize * if (isCurrent) pulse else 1f
                color = when {
                    isCurrent -> accent.toArgb()
                    isCompleted -> accent.copy(alpha = 0.72f).toArgb()
                    else -> textColor.copy(alpha = 0.82f).toArgb()
                }
            }
            val tabTextY = tabY - (tabPaint.descent() + tabPaint.ascent()) / 2f
            drawContext.canvas.nativeCanvas.drawText(fretText, x, tabTextY, tabPaint)
        }
    }
}
