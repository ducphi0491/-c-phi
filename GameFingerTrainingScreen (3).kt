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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

// ============================================================
// ACADEMY-GUIDED PRACTICE
// A compact curriculum is intentionally kept in this file so the
// user only has one source file to manage and replace.
// ============================================================
private enum class GuidedLessonKind { FINGER, HARMONY }

private enum class PracticeSubdivision(
    val key: String,
    val titleVi: String,
    val titleEn: String,
    val stepsPerBeat: Int,
    val shortLabel: String
) {
    QUARTER("QUARTER", "Nốt đen 1/4", "Quarter notes", 1, "1/4"),
    EIGHTH("EIGHTH", "Móc đơn 1/8", "Eighth notes", 2, "1/8"),
    TRIPLET("TRIPLET", "Chùm ba", "Triplets", 3, "3"),
    SIXTEENTH("SIXTEENTH", "Móc kép 1/16", "Sixteenth notes", 4, "1/16");

    fun title(lang: String): String = fingerUiText(lang, titleVi, titleEn)
}

private enum class PracticePickingPattern(
    val key: String,
    val titleVi: String,
    val titleEn: String
) {
    ALTERNATE("ALTERNATE", "Gảy xen kẽ ↓↑", "Alternate ↓↑"),
    DOWN_ONLY("DOWN_ONLY", "Chỉ gảy xuống ↓", "Downstrokes ↓"),
    UP_ONLY("UP_ONLY", "Chỉ gảy lên ↑", "Upstrokes ↑"),
    SWEEP("SWEEP", "Sweep theo chiều", "Directional sweep"),
    LEGATO("LEGATO", "Legato", "Legato");

    fun title(lang: String): String = fingerUiText(lang, titleVi, titleEn)
}

private data class GuidedPracticeLesson(
    val id: String,
    val number: String,
    val kind: GuidedLessonKind,
    val chapterKey: String? = null,
    val titleVi: String,
    val titleEn: String,
    val objectiveVi: String,
    val objectiveEn: String,
    val theoryVi: String,
    val theoryEn: String,
    val applicationVi: String,
    val applicationEn: String,
    val routeKey: String? = null,
    val fingerDirectionKey: String = "FORWARD",
    val fingerStartFret: Int = 5,
    val practiceTypeKey: String = "TRIAD",
    val keyName: String = "C",
    val progressionKey: String = "II_V_I_MAJOR",
    val stringSetKey: String = "STRINGS_123",
    val inversionKey: String = "AUTO",
    val harmonyDirectionKey: String = "ASCENDING",
    val positionStart: Int = 5,
    val bpmStart: Int,
    val bpmTarget: Int,
    val subdivisionKey: String = "QUARTER",
    val beatsPerChord: Int = 4,
    val pickingPatternKey: String = "ALTERNATE",
    val targetRounds: Int = 3,
    val accent: Color
) {
    fun title(lang: String): String = fingerUiText(lang, titleVi, titleEn)
    fun objective(lang: String): String = fingerUiText(lang, objectiveVi, objectiveEn)
    fun theory(lang: String): String = fingerUiText(lang, theoryVi, theoryEn)
    fun application(lang: String): String = fingerUiText(lang, applicationVi, applicationEn)
}

private data class GuidedPracticeChapter(
    val key: String,
    val number: String,
    val titleVi: String,
    val titleEn: String,
    val descriptionVi: String,
    val descriptionEn: String,
    val accent: Color,
    val secondaryAccent: Color
) {
    fun title(lang: String): String = fingerUiText(lang, titleVi, titleEn)
    fun description(lang: String): String = fingerUiText(lang, descriptionVi, descriptionEn)
}

private const val SEVENTH_CIRCLE_CHAPTER_KEY = "SEVENTH_CIRCLE_FOURTHS"

private fun guidedPracticeChapter(key: String?): GuidedPracticeChapter? = when (key) {
    SEVENTH_CIRCLE_CHAPTER_KEY -> GuidedPracticeChapter(
        key = SEVENTH_CIRCLE_CHAPTER_KEY,
        number = "13",
        titleVi = "Hợp âm 7 theo vòng quãng 4",
        titleEn = "Seventh chords through the circle of fourths",
        descriptionVi = "13A Trưởng  →  13B So sánh Em7/E7  →  13C Thứ",
        descriptionEn = "13A Major  →  13B Compare Em7/E7  →  13C Minor",
        accent = Color(0xFFE85D86),
        secondaryAccent = Color(0xFFFF8A50)
    )
    else -> null
}

private fun guidedCurriculumUnitKey(lesson: GuidedPracticeLesson): String =
    lesson.chapterKey ?: lesson.id

private fun guidedCurriculumUnitCount(lessons: List<GuidedPracticeLesson>): Int =
    lessons.map(::guidedCurriculumUnitKey).distinct().size

private fun guidedCurriculumCompletedCount(
    lessons: List<GuidedPracticeLesson>,
    completedIds: Set<String>
): Int = lessons
    .groupBy(::guidedCurriculumUnitKey)
    .count { (_, parts) -> parts.all { it.id in completedIds } }

private object GuidedPracticeProgressStore {
    private const val PREF_NAME = "tuner_tools_guided_practice_v1"
    private const val COMPLETED_KEY = "completed_lessons"

    fun completedIds(context: android.content.Context): Set<String> =
        context.getSharedPreferences(PREF_NAME, android.content.Context.MODE_PRIVATE)
            .getStringSet(COMPLETED_KEY, emptySet<String>())
            ?.toSet()
            .orEmpty()

    fun bestBpm(context: android.content.Context, lessonId: String): Int =
        context.getSharedPreferences(PREF_NAME, android.content.Context.MODE_PRIVATE)
            .getInt("best_bpm_$lessonId", 0)

    fun markCompleted(context: android.content.Context, lessonId: String, bpm: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, android.content.Context.MODE_PRIVATE)
        val completed = prefs.getStringSet(COMPLETED_KEY, emptySet<String>()).orEmpty().toMutableSet()
        completed += lessonId
        val oldBest = prefs.getInt("best_bpm_$lessonId", 0)
        prefs.edit()
            .putStringSet(COMPLETED_KEY, completed)
            .putInt("best_bpm_$lessonId", maxOf(oldBest, bpm))
            .apply()
    }
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
    val formula: String,
    val groupKey: String,
    val summaryVi: String,
    val summaryEn: String,
    val usageVi: String,
    val usageEn: String,
    val degreeLabels: List<String>,
    val intervals: List<Int>,
    val usesMajorCagedFingering: Boolean,
    val characteristicIntervals: List<Int> = emptyList(),
    val supportsDominantContext: Boolean = false,
    val levelKey: String = "BASIC"
) {
    MAJOR(
        // Keep the original key so saved selections from older builds remain valid.
        key = "MAJOR",
        titleVi = "Ionian (Trưởng)",
        titleEn = "Ionian (Major)",
        formula = "1 · 2 · 3 · 4 · 5 · 6 · 7",
        groupKey = "MODES",
        summaryVi = "Ionian là âm giai trưởng chuẩn. Bậc 7 trưởng tạo lực hút rõ về nốt gốc.",
        summaryEn = "Ionian is the standard major scale. Its major 7th creates a strong pull toward the tonic.",
        usageVi = "Dùng trên hợp âm trưởng, maj7 và các giai điệu trưởng thông dụng.",
        usageEn = "Use it over major and maj7 harmony and conventional major-key melodies.",
        degreeLabels = listOf("1", "2", "3", "4", "5", "6", "7"),
        intervals = listOf(0, 2, 4, 5, 7, 9, 11),
        usesMajorCagedFingering = true,
        characteristicIntervals = listOf(11)
    ),
    DORIAN(
        key = "DORIAN",
        titleVi = "Dorian",
        titleEn = "Dorian",
        formula = "1 · 2 · ♭3 · 4 · 5 · 6 · ♭7",
        groupKey = "MODES",
        summaryVi = "Dorian là mode thứ có bậc 6 trưởng. Bậc 6 giúp màu thứ sáng và linh hoạt hơn Aeolian.",
        summaryEn = "Dorian is a minor mode with a natural 6th, giving it a brighter sound than Aeolian.",
        usageVi = "Phù hợp hợp âm m7, m6, funk, jazz và các vòng hòa âm Dorian.",
        usageEn = "Useful over m7 and m6 harmony, funk, jazz and Dorian vamps.",
        degreeLabels = listOf("1", "2", "♭3", "4", "5", "6", "♭7"),
        intervals = listOf(0, 2, 3, 5, 7, 9, 10),
        usesMajorCagedFingering = false,
        characteristicIntervals = listOf(9)
    ),
    PHRYGIAN(
        key = "PHRYGIAN",
        titleVi = "Phrygian",
        titleEn = "Phrygian",
        formula = "1 · ♭2 · ♭3 · 4 · 5 · ♭6 · ♭7",
        groupKey = "MODES",
        summaryVi = "Phrygian là mode thứ có bậc ♭2 đặc trưng, tạo cảm giác tối và căng gần nốt gốc.",
        summaryEn = "Phrygian is a minor mode whose characteristic ♭2 creates a dark, tense pull near the tonic.",
        usageVi = "Phù hợp các nền hợp âm thứ tĩnh, metal và màu âm Tây Ban Nha; không đồng nhất với Phrygian dominant.",
        usageEn = "Useful over static minor harmony, metal and Spanish-flavoured lines; it is not the same as Phrygian dominant.",
        degreeLabels = listOf("1", "♭2", "♭3", "4", "5", "♭6", "♭7"),
        intervals = listOf(0, 1, 3, 5, 7, 8, 10),
        usesMajorCagedFingering = false,
        characteristicIntervals = listOf(1)
    ),
    LYDIAN(
        key = "LYDIAN",
        titleVi = "Lydian",
        titleEn = "Lydian",
        formula = "1 · 2 · 3 · ♯4 · 5 · 6 · 7",
        groupKey = "MODES",
        summaryVi = "Lydian là mode trưởng có bậc ♯4 đặc trưng, tạo màu sáng, rộng và ít va chạm với bậc 3.",
        summaryEn = "Lydian is a major mode with a characteristic ♯4, producing a bright and open sound.",
        usageVi = "Phù hợp hợp âm maj7, maj7♯11 và các nền trưởng cần màu sắc bay, hiện đại.",
        usageEn = "Useful over maj7 and maj7♯11 harmony and modern, floating major sounds.",
        degreeLabels = listOf("1", "2", "3", "♯4", "5", "6", "7"),
        intervals = listOf(0, 2, 4, 6, 7, 9, 11),
        usesMajorCagedFingering = true,
        characteristicIntervals = listOf(6)
    ),
    MIXOLYDIAN(
        key = "MIXOLYDIAN",
        titleVi = "Mixolydian",
        titleEn = "Mixolydian",
        formula = "1 · 2 · 3 · 4 · 5 · 6 · ♭7",
        groupKey = "MODES",
        summaryVi = "Mixolydian là mode trưởng có bậc ♭7. Cặp 3 trưởng và ♭7 thể hiện rõ chức năng dominant.",
        summaryEn = "Mixolydian is a major mode with a ♭7. The major 3rd and ♭7 clearly outline dominant function.",
        usageVi = "Dùng trên hợp âm dominant 7, blues, rock, funk và các vòng hòa âm Mixolydian.",
        usageEn = "Use it over dominant-seventh harmony, blues, rock, funk and Mixolydian progressions.",
        degreeLabels = listOf("1", "2", "3", "4", "5", "6", "♭7"),
        intervals = listOf(0, 2, 4, 5, 7, 9, 10),
        usesMajorCagedFingering = true,
        characteristicIntervals = listOf(4, 10),
        supportsDominantContext = true
    ),
    NATURAL_MINOR(
        // Keep the original key so saved selections from older builds remain valid.
        key = "NATURAL_MINOR",
        titleVi = "Aeolian (Thứ tự nhiên)",
        titleEn = "Aeolian (Natural minor)",
        formula = "1 · 2 · ♭3 · 4 · 5 · ♭6 · ♭7",
        groupKey = "MODES",
        summaryVi = "Aeolian chính là âm giai thứ tự nhiên. Bậc ♭6 là màu quan trọng phân biệt với Dorian.",
        summaryEn = "Aeolian is the natural minor scale. Its ♭6 is an important contrast with Dorian.",
        usageVi = "Dùng trên hợp âm thứ, các vòng hòa âm thứ tự nhiên và giai điệu có màu buồn, ổn định.",
        usageEn = "Use it over minor harmony, natural-minor progressions and stable, darker melodies.",
        degreeLabels = listOf("1", "2", "♭3", "4", "5", "♭6", "♭7"),
        intervals = listOf(0, 2, 3, 5, 7, 8, 10),
        usesMajorCagedFingering = false,
        characteristicIntervals = listOf(8)
    ),
    LOCRIAN(
        key = "LOCRIAN",
        titleVi = "Locrian",
        titleEn = "Locrian",
        formula = "1 · ♭2 · ♭3 · 4 · ♭5 · ♭6 · ♭7",
        groupKey = "MODES",
        summaryVi = "Locrian là mode thứ có ♭5 và ♭2, nên hợp âm chủ không ổn định như các mode còn lại.",
        summaryEn = "Locrian is a minor mode with a ♭5 and ♭2, so its tonic harmony is inherently unstable.",
        usageVi = "Phù hợp hợp âm m7♭5 (half-diminished), bài tập nghe và các đoạn cần độ căng cao.",
        usageEn = "Useful over m7♭5 (half-diminished) harmony, ear training and highly tense passages.",
        degreeLabels = listOf("1", "♭2", "♭3", "4", "♭5", "♭6", "♭7"),
        intervals = listOf(0, 1, 3, 5, 6, 8, 10),
        usesMajorCagedFingering = false,
        characteristicIntervals = listOf(6, 1)
    ),
    HARMONIC_MINOR(
        key = "HARMONIC_MINOR",
        titleVi = "Thứ hòa thanh",
        titleEn = "Harmonic minor",
        formula = "1 · 2 · ♭3 · 4 · 5 · ♭6 · 7",
        groupKey = "MINOR_SCALES",
        summaryVi = "Âm giai thứ hòa thanh nâng bậc 7 của thứ tự nhiên. Khoảng ♭6–7 tạo lực hút mạnh về nốt gốc và màu hòa âm đặc trưng.",
        summaryEn = "Harmonic minor raises the 7th of natural minor. The ♭6–7 span creates a strong pull to the tonic and its characteristic harmonic colour.",
        usageVi = "Dùng trong hòa âm thứ, hợp âm V7 dẫn về i, metal cổ điển, neoclassical và nhiều ngữ cảnh flamenco.",
        usageEn = "Use it in minor-key harmony, especially V7 resolving to i, as well as classical metal, neoclassical and many flamenco contexts.",
        degreeLabels = listOf("1", "2", "♭3", "4", "5", "♭6", "7"),
        intervals = listOf(0, 2, 3, 5, 7, 8, 11),
        usesMajorCagedFingering = false,
        characteristicIntervals = listOf(8, 11)
    ),
    MELODIC_MINOR(
        key = "MELODIC_MINOR",
        titleVi = "Thứ giai điệu (Jazz)",
        titleEn = "Melodic minor (Jazz)",
        formula = "1 · 2 · ♭3 · 4 · 5 · 6 · 7",
        groupKey = "MINOR_SCALES",
        summaryVi = "Đây là dạng jazz melodic minor: cùng một tập nốt khi đi lên và đi xuống. Nó là âm giai trưởng hạ bậc 3.",
        summaryEn = "This is the jazz melodic-minor form: the same collection ascending and descending. It can be heard as a major scale with a lowered 3rd.",
        usageVi = "Dùng trên hợp âm m(maj7), m6 và làm âm giai mẹ cho Lydian dominant, Altered cùng các mode melodic minor khác.",
        usageEn = "Use it over m(maj7) and m6 harmony and as the parent scale of Lydian dominant, Altered and the other melodic-minor modes.",
        degreeLabels = listOf("1", "2", "♭3", "4", "5", "6", "7"),
        intervals = listOf(0, 2, 3, 5, 7, 9, 11),
        usesMajorCagedFingering = false,
        characteristicIntervals = listOf(9, 11)
    ),
    MAJOR_PENTATONIC(
        key = "MAJOR_PENTATONIC",
        titleVi = "Ngũ cung trưởng",
        titleEn = "Major pentatonic",
        formula = "1 · 2 · 3 · 5 · 6",
        groupKey = "BASIC_PENTATONIC",
        summaryVi = "Ngũ cung sáng, dễ nghe và ít nốt tránh.",
        summaryEn = "A bright, accessible pentatonic scale with few avoid notes.",
        usageVi = "Phù hợp pop, country và solo trên hợp âm trưởng.",
        usageEn = "Useful for pop, country and soloing over major chords.",
        degreeLabels = listOf("1", "2", "3", "5", "6"),
        intervals = listOf(0, 2, 4, 7, 9),
        usesMajorCagedFingering = true
    ),
    MINOR_PENTATONIC(
        key = "MINOR_PENTATONIC",
        titleVi = "Ngũ cung thứ",
        titleEn = "Minor pentatonic",
        formula = "1 · ♭3 · 4 · 5 · ♭7",
        groupKey = "BASIC_PENTATONIC",
        summaryVi = "Ngũ cung thứ quen thuộc trong blues và rock.",
        summaryEn = "The familiar minor pentatonic sound used in blues and rock.",
        usageVi = "Phù hợp blues, rock và solo trên hợp âm thứ.",
        usageEn = "Useful for blues, rock and soloing over minor chords.",
        degreeLabels = listOf("1", "♭3", "4", "5", "♭7"),
        intervals = listOf(0, 3, 5, 7, 10),
        usesMajorCagedFingering = false
    ),
    MIXOLYDIAN_PENTATONIC(
        // Keep the original key so previously saved user preferences remain valid.
        key = "MIXOLYDIAN_PENTATONIC",
        titleVi = "Mixolydian ngũ cung (có 4)",
        titleEn = "Mixolydian pentatonic (with 4)",
        formula = "1 · 3 · 4 · 5 · ♭7",
        groupKey = "MODAL_PENTATONIC",
        summaryVi = "Biến thể bỏ bậc 2 và 6 khỏi Mixolydian. Tên được ghi kèm “có 4” vì các giáo trình không dùng hoàn toàn thống nhất.",
        summaryEn = "A variant formed by omitting degrees 2 and 6 from Mixolydian. “With 4” is shown because methods do not use one universal naming convention.",
        usageVi = "Giữ 3 trưởng và ♭7, đồng thời thêm bậc 4; phù hợp dominant 7, 7sus4, funk và rock.",
        usageEn = "Keeps the major 3rd and ♭7 and adds the 4th; useful over dominant 7, 7sus4, funk and rock.",
        degreeLabels = listOf("1", "3", "4", "5", "♭7"),
        intervals = listOf(0, 4, 5, 7, 10),
        usesMajorCagedFingering = true,
        characteristicIntervals = listOf(4, 10),
        supportsDominantContext = true
    ),
    DOMINANT_PENTATONIC(
        key = "DOMINANT_PENTATONIC",
        titleVi = "Ngũ cung dominant (có 2)",
        titleEn = "Dominant pentatonic (with 2)",
        formula = "1 · 2 · 3 · 5 · ♭7",
        groupKey = "MODAL_PENTATONIC",
        summaryVi = "Biến thể dominant gồm các nốt của hợp âm 7 cộng bậc 2/9. Một số giáo trình cũng gọi đây là Mixolydian ngũ cung.",
        summaryEn = "A dominant-seventh outline plus degree 2/9. Some methods also call it Mixolydian pentatonic.",
        usageVi = "Phù hợp dominant 7/9, jazz, blues, funk và các câu solo cần tránh bậc 4.",
        usageEn = "Useful over dominant 7/9 harmony in jazz, blues and funk, especially when avoiding the 4th.",
        degreeLabels = listOf("1", "2", "3", "5", "♭7"),
        intervals = listOf(0, 2, 4, 7, 10),
        usesMajorCagedFingering = true,
        characteristicIntervals = listOf(4, 10),
        supportsDominantContext = true
    ),
    MAJOR_BLUES(
        key = "MAJOR_BLUES",
        titleVi = "Blues trưởng",
        titleEn = "Major blues",
        formula = "1 · 2 · ♭3 · 3 · 5 · 6",
        groupKey = "BLUES",
        summaryVi = "Blues trưởng là ngũ cung trưởng cộng ♭3. Sự va chạm biểu cảm giữa ♭3 và 3 tạo màu blues.",
        summaryEn = "The major blues scale is major pentatonic plus ♭3. The expressive tension between ♭3 and 3 creates its blues colour.",
        usageVi = "Phù hợp major blues, country, rock and roll và solo trên nền trưởng hoặc dominant khi xử lý ♭3 như blue note.",
        usageEn = "Useful for major blues, country and rock and roll over major or dominant harmony, treating ♭3 as a blue note.",
        degreeLabels = listOf("1", "2", "♭3", "3", "5", "6"),
        intervals = listOf(0, 2, 3, 4, 7, 9),
        usesMajorCagedFingering = true,
        characteristicIntervals = listOf(3, 4)
    ),
    MINOR_BLUES(
        key = "MINOR_BLUES",
        titleVi = "Blues thứ",
        titleEn = "Minor blues",
        formula = "1 · ♭3 · 4 · ♭5 · 5 · ♭7",
        groupKey = "BLUES",
        summaryVi = "Blues thứ là ngũ cung thứ cộng ♭5. Bậc ♭5 là blue note đặc trưng, thường được lướt hoặc bend về bậc 5.",
        summaryEn = "The minor blues scale is minor pentatonic plus ♭5. The ♭5 is its characteristic blue note, often approached or bent toward 5.",
        usageVi = "Phù hợp blues, rock và các câu solo trên nền thứ hoặc dominant tùy ngữ cảnh hòa âm.",
        usageEn = "Useful for blues and rock over minor or dominant harmony, depending on the harmonic context.",
        degreeLabels = listOf("1", "♭3", "4", "♭5", "5", "♭7"),
        intervals = listOf(0, 3, 5, 6, 7, 10),
        usesMajorCagedFingering = false,
        characteristicIntervals = listOf(6, 3)
    ),
    DORIAN_FLAT_2(
        key = "DORIAN_FLAT_2",
        titleVi = "Dorian ♭2",
        titleEn = "Dorian ♭2",
        formula = "1 · ♭2 · ♭3 · 4 · 5 · 6 · ♭7",
        groupKey = "MELODIC_MINOR_MODES",
        summaryVi = "Mode 2 của melodic minor. Nó giữ màu Dorian nhờ bậc 6 trưởng nhưng có thêm ♭2 tối và căng.",
        summaryEn = "The second mode of melodic minor. It keeps Dorian's natural 6th while adding a dark, tense ♭2.",
        usageVi = "Phù hợp hợp âm m7(♭9), nền minor sus và các đoạn jazz/fusion cần màu Dorian tối hơn.",
        usageEn = "Useful over m7(♭9), minor-sus harmony and jazz/fusion passages requiring a darker Dorian colour.",
        degreeLabels = listOf("1", "♭2", "♭3", "4", "5", "6", "♭7"),
        intervals = listOf(0, 1, 3, 5, 7, 9, 10),
        usesMajorCagedFingering = false,
        characteristicIntervals = listOf(1, 9),
        levelKey = "ADVANCED"
    ),
    LYDIAN_AUGMENTED(
        key = "LYDIAN_AUGMENTED",
        titleVi = "Lydian Augmented",
        titleEn = "Lydian augmented",
        formula = "1 · 2 · 3 · ♯4 · ♯5 · 6 · 7",
        groupKey = "MELODIC_MINOR_MODES",
        summaryVi = "Mode 3 của melodic minor, kết hợp ♯4 của Lydian với ♯5 của hợp âm augmented.",
        summaryEn = "The third mode of melodic minor, combining Lydian's ♯4 with the augmented chord's ♯5.",
        usageVi = "Dùng trên maj7♯5, maj7♯11♯5 và các hợp âm trưởng tăng có màu hiện đại.",
        usageEn = "Use it over maj7♯5, maj7♯11♯5 and modern augmented-major harmony.",
        degreeLabels = listOf("1", "2", "3", "♯4", "♯5", "6", "7"),
        intervals = listOf(0, 2, 4, 6, 8, 9, 11),
        usesMajorCagedFingering = true,
        characteristicIntervals = listOf(6, 8),
        levelKey = "ADVANCED"
    ),
    LYDIAN_DOMINANT(
        key = "LYDIAN_DOMINANT",
        titleVi = "Lydian Dominant",
        titleEn = "Lydian dominant",
        formula = "1 · 2 · 3 · ♯4 · 5 · 6 · ♭7",
        groupKey = "MELODIC_MINOR_MODES",
        summaryVi = "Mode 4 của melodic minor, còn gọi Lydian ♭7. Nó kết hợp ♯4/♯11 với 3 trưởng và ♭7 của dominant.",
        summaryEn = "The fourth mode of melodic minor, also called Lydian ♭7. It combines ♯4/♯11 with the dominant major 3rd and ♭7.",
        usageVi = "Dùng trên hợp âm 7♯11, dominant không cần giải quyết truyền thống, jazz và fusion.",
        usageEn = "Use it over 7♯11 chords, non-resolving dominants, jazz and fusion.",
        degreeLabels = listOf("1", "2", "3", "♯4", "5", "6", "♭7"),
        intervals = listOf(0, 2, 4, 6, 7, 9, 10),
        usesMajorCagedFingering = true,
        characteristicIntervals = listOf(6, 10),
        supportsDominantContext = true,
        levelKey = "ADVANCED"
    ),
    MIXOLYDIAN_FLAT_6(
        key = "MIXOLYDIAN_FLAT_6",
        titleVi = "Mixolydian ♭6",
        titleEn = "Mixolydian ♭6",
        formula = "1 · 2 · 3 · 4 · 5 · ♭6 · ♭7",
        groupKey = "MELODIC_MINOR_MODES",
        summaryVi = "Mode 5 của melodic minor. Đây là Mixolydian có ♭6, tạo màu dominant tối hơn và gần âm hưởng minor.",
        summaryEn = "The fifth mode of melodic minor. It is Mixolydian with a ♭6, producing a darker dominant sound with a minor inflection.",
        usageVi = "Dùng trên dominant 7(♭13), các dominant dẫn về minor và vòng hòa âm có màu điện ảnh.",
        usageEn = "Use it over 7(♭13), dominants resolving to minor and cinematic dominant harmony.",
        degreeLabels = listOf("1", "2", "3", "4", "5", "♭6", "♭7"),
        intervals = listOf(0, 2, 4, 5, 7, 8, 10),
        usesMajorCagedFingering = true,
        characteristicIntervals = listOf(4, 8),
        supportsDominantContext = true,
        levelKey = "ADVANCED"
    ),
    LOCRIAN_NATURAL_2(
        key = "LOCRIAN_NATURAL_2",
        titleVi = "Locrian ♮2",
        titleEn = "Locrian ♮2",
        formula = "1 · 2 · ♭3 · 4 · ♭5 · ♭6 · ♭7",
        groupKey = "MELODIC_MINOR_MODES",
        summaryVi = "Mode 6 của melodic minor. So với Locrian thông thường, bậc 2 được nâng thành tự nhiên nên bớt căng ở gần nốt gốc.",
        summaryEn = "The sixth mode of melodic minor. Compared with ordinary Locrian, its natural 2nd softens the tension next to the tonic.",
        usageVi = "Dùng trên m7♭5 khi bậc 9 tự nhiên phù hợp với hòa âm, đặc biệt trong jazz minor ii–V–i.",
        usageEn = "Use it over m7♭5 when a natural 9 fits the harmony, especially in minor jazz ii–V–i progressions.",
        degreeLabels = listOf("1", "2", "♭3", "4", "♭5", "♭6", "♭7"),
        intervals = listOf(0, 2, 3, 5, 6, 8, 10),
        usesMajorCagedFingering = false,
        characteristicIntervals = listOf(2, 6),
        levelKey = "ADVANCED"
    ),
    ALTERED(
        key = "ALTERED",
        titleVi = "Altered",
        titleEn = "Altered",
        formula = "1 · ♭9 · ♯9 · 3 · ♭5 · ♭13 · ♭7",
        groupKey = "MELODIC_MINOR_MODES",
        summaryVi = "Mode 7 của melodic minor. Ngoài 1, 3 và ♭7, nó chứa các tension dominant biến đổi: ♭9, ♯9, ♭5/♯11 và ♭13.",
        summaryEn = "The seventh mode of melodic minor. Beyond 1, 3 and ♭7, it contains the altered dominant tensions ♭9, ♯9, ♭5/♯11 and ♭13.",
        usageVi = "Dùng trên hợp âm 7alt, 7♭9, 7♯9, 7♭5 hoặc 7♭13 khi dominant cần giải quyết mạnh.",
        usageEn = "Use it over 7alt, 7♭9, 7♯9, 7♭5 or 7♭13 when a dominant chord requires strong resolution.",
        degreeLabels = listOf("1", "♭9", "♯9", "3", "♭5", "♭13", "♭7"),
        intervals = listOf(0, 1, 3, 4, 6, 8, 10),
        usesMajorCagedFingering = false,
        characteristicIntervals = listOf(1, 3),
        supportsDominantContext = true,
        levelKey = "ADVANCED"
    ),
    LOCRIAN_NATURAL_6(
        key = "LOCRIAN_NATURAL_6",
        titleVi = "Locrian ♮6",
        titleEn = "Locrian ♮6",
        formula = "1 · ♭2 · ♭3 · 4 · ♭5 · 6 · ♭7",
        groupKey = "HARMONIC_MINOR_MODES",
        summaryVi = "Mode 2 của harmonic minor. Nó giữ ♭2 và ♭5 của Locrian nhưng nâng ♭6 thành bậc 6 tự nhiên.",
        summaryEn = "The second mode of harmonic minor. It keeps Locrian's ♭2 and ♭5 while raising ♭6 to a natural 6th.",
        usageVi = "Dùng trên m7♭5 khi bậc 13 tự nhiên phù hợp, hoặc để tạo màu half-diminished sáng hơn Locrian thường.",
        usageEn = "Use it over m7♭5 when a natural 13 fits, or for a brighter half-diminished colour than ordinary Locrian.",
        degreeLabels = listOf("1", "♭2", "♭3", "4", "♭5", "6", "♭7"),
        intervals = listOf(0, 1, 3, 5, 6, 9, 10),
        usesMajorCagedFingering = false,
        characteristicIntervals = listOf(6, 9),
        levelKey = "ADVANCED"
    ),
    IONIAN_AUGMENTED(
        key = "IONIAN_AUGMENTED",
        titleVi = "Ionian Augmented",
        titleEn = "Ionian augmented",
        formula = "1 · 2 · 3 · 4 · ♯5 · 6 · 7",
        groupKey = "HARMONIC_MINOR_MODES",
        summaryVi = "Mode 3 của harmonic minor. Đây là Ionian có ♯5, tạo hợp âm chủ maj7♯5 nhưng vẫn giữ bậc 4 tự nhiên.",
        summaryEn = "The third mode of harmonic minor. It is Ionian with a ♯5, creating a maj7♯5 tonic while retaining a natural 4th.",
        usageVi = "Dùng trên maj7♯5, augmented major và các đoạn cần màu trưởng tăng nhưng không có ♯4 của Lydian augmented.",
        usageEn = "Use it over maj7♯5 and augmented-major harmony when the natural 4th is preferred to Lydian augmented's ♯4.",
        degreeLabels = listOf("1", "2", "3", "4", "♯5", "6", "7"),
        intervals = listOf(0, 2, 4, 5, 8, 9, 11),
        usesMajorCagedFingering = true,
        characteristicIntervals = listOf(8, 11),
        levelKey = "ADVANCED"
    ),
    DORIAN_SHARP_4(
        key = "DORIAN_SHARP_4",
        titleVi = "Dorian ♯4 (Romanian)",
        titleEn = "Dorian ♯4 (Romanian)",
        formula = "1 · 2 · ♭3 · ♯4 · 5 · 6 · ♭7",
        groupKey = "HARMONIC_MINOR_MODES",
        summaryVi = "Mode 4 của harmonic minor. Nó giữ bậc 6 trưởng của Dorian nhưng thay bậc 4 tự nhiên bằng ♯4.",
        summaryEn = "The fourth mode of harmonic minor. It keeps Dorian's natural 6th while replacing the natural 4th with ♯4.",
        usageVi = "Dùng trên m7♯11, fusion, metal và các nền minor cần màu sáng nhưng căng hơn Dorian thường.",
        usageEn = "Use it over m7♯11, fusion, metal and minor vamps requiring a brighter yet tenser colour than ordinary Dorian.",
        degreeLabels = listOf("1", "2", "♭3", "♯4", "5", "6", "♭7"),
        intervals = listOf(0, 2, 3, 6, 7, 9, 10),
        usesMajorCagedFingering = false,
        characteristicIntervals = listOf(6, 9),
        levelKey = "ADVANCED"
    ),
    PHRYGIAN_DOMINANT(
        key = "PHRYGIAN_DOMINANT",
        titleVi = "Phrygian Dominant",
        titleEn = "Phrygian dominant",
        formula = "1 · ♭2 · 3 · 4 · 5 · ♭6 · ♭7",
        groupKey = "HARMONIC_MINOR_MODES",
        summaryVi = "Mode 5 của harmonic minor. Bậc ♭2 kết hợp với 3 trưởng tạo màu dominant rất rõ và khác Phrygian thường.",
        summaryEn = "The fifth mode of harmonic minor. Its ♭2 combined with a major 3rd creates a distinctive dominant sound unlike ordinary Phrygian.",
        usageVi = "Dùng trên V7 trong giọng thứ, dominant ♭9/♭13, metal, fusion và nhiều ngữ cảnh flamenco.",
        usageEn = "Use it over V7 in minor keys, ♭9/♭13 dominants, metal, fusion and many flamenco contexts.",
        degreeLabels = listOf("1", "♭2", "3", "4", "5", "♭6", "♭7"),
        intervals = listOf(0, 1, 4, 5, 7, 8, 10),
        usesMajorCagedFingering = true,
        characteristicIntervals = listOf(1, 4),
        supportsDominantContext = true,
        levelKey = "ADVANCED"
    ),
    LYDIAN_SHARP_2(
        key = "LYDIAN_SHARP_2",
        titleVi = "Lydian ♯2",
        titleEn = "Lydian ♯2",
        formula = "1 · ♯2 · 3 · ♯4 · 5 · 6 · 7",
        groupKey = "HARMONIC_MINOR_MODES",
        summaryVi = "Mode 6 của harmonic minor. Nó kết hợp ♯2 với ♯4 của Lydian, tạo khoảng tăng 2 ngay đầu âm giai.",
        summaryEn = "The sixth mode of harmonic minor. It combines ♯2 with Lydian's ♯4, creating an augmented second near the tonic.",
        usageVi = "Dùng trên maj7♯11 khi ♯9/♯2 phù hợp với giai điệu, chủ yếu trong sáng tác và jazz nâng cao.",
        usageEn = "Use it over maj7♯11 when a ♯9/♯2 fits the line, mainly in advanced composition and jazz contexts.",
        degreeLabels = listOf("1", "♯2", "3", "♯4", "5", "6", "7"),
        intervals = listOf(0, 3, 4, 6, 7, 9, 11),
        usesMajorCagedFingering = true,
        characteristicIntervals = listOf(3, 6),
        levelKey = "ADVANCED"
    ),
    ULTRA_LOCRIAN(
        key = "ULTRA_LOCRIAN",
        titleVi = "Ultra Locrian",
        titleEn = "Ultra Locrian",
        formula = "1 · ♭2 · ♭3 · ♭4 · ♭5 · ♭6 · ♭♭7",
        groupKey = "HARMONIC_MINOR_MODES",
        summaryVi = "Mode 7 của harmonic minor. Đây là tập nốt cực căng với ♭4, ♭5 và bậc 7 giáng kép; không đồng nhất với Altered/Super Locrian của melodic minor.",
        summaryEn = "The seventh mode of harmonic minor. It is highly tense, with ♭4, ♭5 and a doubly lowered 7th; it is not the melodic-minor Altered/Super Locrian scale.",
        usageVi = "Dùng hạn chế trong sáng tác, hòa âm diminished/altered và luyện nghe nâng cao; nên luôn đọc kèm công thức.",
        usageEn = "Use sparingly in composition, diminished/altered harmony and advanced ear training; always read it together with its formula.",
        degreeLabels = listOf("1", "♭2", "♭3", "♭4", "♭5", "♭6", "♭♭7"),
        intervals = listOf(0, 1, 3, 4, 6, 8, 9),
        usesMajorCagedFingering = false,
        characteristicIntervals = listOf(4, 9),
        levelKey = "ADVANCED"
    ),
    WHOLE_TONE(
        key = "WHOLE_TONE",
        titleVi = "Toàn cung",
        titleEn = "Whole tone",
        formula = "1 · 2 · 3 · ♯4 · ♯5 · ♭7",
        groupKey = "SYMMETRIC",
        summaryVi = "Âm giai sáu nốt chỉ gồm các quãng toàn cung. Vì không có nửa cung, nó tạo cảm giác lơ lửng và không ổn định.",
        summaryEn = "A six-note scale built entirely from whole steps. With no semitones, it sounds floating and unresolved.",
        usageVi = "Dùng trên hợp âm augmented và dominant có ♯5/♭5 khi cần màu mơ hồ, đối xứng.",
        usageEn = "Use it over augmented harmony and dominant chords with ♯5/♭5 for an ambiguous, symmetrical colour.",
        degreeLabels = listOf("1", "2", "3", "♯4", "♯5", "♭7"),
        intervals = listOf(0, 2, 4, 6, 8, 10),
        usesMajorCagedFingering = true,
        characteristicIntervals = listOf(6, 8),
        supportsDominantContext = true,
        levelKey = "ADVANCED"
    ),
    DIMINISHED_HALF_WHOLE(
        key = "DIMINISHED_HALF_WHOLE",
        titleVi = "Diminished nửa–cung / toàn–cung",
        titleEn = "Diminished half–whole",
        formula = "1 · ♭9 · ♯9 · 3 · ♯11 · 5 · 13 · ♭7",
        groupKey = "SYMMETRIC",
        summaryVi = "Âm giai tám nốt đối xứng, luân phiên nửa cung rồi toàn cung. Nó chứa hợp âm dominant cùng nhiều tension biến đổi.",
        summaryEn = "An eight-note symmetrical scale alternating half and whole steps. It contains a dominant chord plus several altered tensions.",
        usageVi = "Dùng trên dominant 7♭9, 7♯9, 13♭9 và dominant diminished; đặc biệt hiệu quả trước khi giải quyết.",
        usageEn = "Use it over 7♭9, 7♯9, 13♭9 and dominant-diminished harmony, especially before resolution.",
        degreeLabels = listOf("1", "♭9", "♯9", "3", "♯11", "5", "13", "♭7"),
        intervals = listOf(0, 1, 3, 4, 6, 7, 9, 10),
        usesMajorCagedFingering = false,
        characteristicIntervals = listOf(1, 3),
        supportsDominantContext = true,
        levelKey = "ADVANCED"
    ),
    DIMINISHED_WHOLE_HALF(
        key = "DIMINISHED_WHOLE_HALF",
        titleVi = "Diminished toàn–cung / nửa–cung",
        titleEn = "Diminished whole–half",
        formula = "1 · 2 · ♭3 · 4 · ♭5 · ♭6 · 6 · 7",
        groupKey = "SYMMETRIC",
        summaryVi = "Âm giai tám nốt đối xứng, luân phiên toàn cung rồi nửa cung. Nó phác họa hợp âm diminished seventh từ nốt gốc.",
        summaryEn = "An eight-note symmetrical scale alternating whole and half steps. It outlines a diminished-seventh chord from the tonic.",
        usageVi = "Dùng trên hợp âm °7, các chuỗi diminished và bài tập chuyển dịch theo quãng 3 thứ.",
        usageEn = "Use it over °7 harmony, diminished sequences and minor-third transposition practice.",
        degreeLabels = listOf("1", "2", "♭3", "4", "♭5", "♭6", "6", "7"),
        intervals = listOf(0, 2, 3, 5, 6, 8, 9, 11),
        usesMajorCagedFingering = false,
        characteristicIntervals = listOf(6, 11),
        levelKey = "ADVANCED"
    ),
    CHROMATIC(
        key = "CHROMATIC",
        titleVi = "Chromatic (12 nốt)",
        titleEn = "Chromatic (12 notes)",
        formula = "1 · ♭2 · 2 · ♭3 · 3 · 4 · ♭5 · 5 · ♭6 · 6 · ♭7 · 7",
        groupKey = "SYMMETRIC",
        summaryVi = "Âm giai chromatic chứa đủ 12 cao độ trong quãng tám. Đây không phải mode của âm giai trưởng mà là tập hợp toàn bộ bán cung.",
        summaryEn = "The chromatic scale contains all 12 pitch classes in the octave. It is not a major-scale mode but the complete semitone collection.",
        usageVi = "Dùng luyện ngón, nối hợp âm bằng nốt tiếp cận, luyện nghe bán cung và phát triển câu chromatic.",
        usageEn = "Use it for technique, chromatic approaches between chord tones, semitone ear training and chromatic line development.",
        degreeLabels = listOf("1", "♭2", "2", "♭3", "3", "4", "♭5", "5", "♭6", "6", "♭7", "7"),
        intervals = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
        usesMajorCagedFingering = false,
        levelKey = "ADVANCED"
    ),
    DOMINANT_BEBOP(
        key = "DOMINANT_BEBOP",
        titleVi = "Bebop Dominant",
        titleEn = "Dominant bebop",
        formula = "1 · 2 · 3 · 4 · 5 · 6 · ♭7 · 7",
        groupKey = "BEBOP",
        summaryVi = "Mixolydian cộng thêm bậc 7 trưởng làm nốt chuyển tiếp. Khi chạy móc đơn từ đúng điểm đặt, các nốt hợp âm có thể rơi đều vào phách mạnh.",
        summaryEn = "Mixolydian plus a major 7th passing tone. With suitable rhythmic placement, chord tones can align consistently with strong beats.",
        usageVi = "Dùng trên hợp âm dominant 7 trong bebop và jazz; cần luyện cùng nhịp chứ không chỉ chạy lên xuống máy móc.",
        usageEn = "Use it over dominant-seventh harmony in bebop and jazz; practise it rhythmically rather than only as a mechanical up-and-down scale.",
        degreeLabels = listOf("1", "2", "3", "4", "5", "6", "♭7", "7"),
        intervals = listOf(0, 2, 4, 5, 7, 9, 10, 11),
        usesMajorCagedFingering = true,
        characteristicIntervals = listOf(10, 11),
        supportsDominantContext = true,
        levelKey = "ADVANCED"
    ),
    HARMONIC_MAJOR(
        key = "HARMONIC_MAJOR",
        titleVi = "Trưởng hòa thanh",
        titleEn = "Harmonic major",
        formula = "1 · 2 · 3 · 4 · 5 · ♭6 · 7",
        groupKey = "SPECIAL",
        summaryVi = "Âm giai trưởng hạ bậc 6. Nó kết hợp hợp âm chủ trưởng với màu ♭6 thường gặp trong hòa âm thứ.",
        summaryEn = "A major scale with a lowered 6th, combining a major tonic with the ♭6 colour often associated with minor harmony.",
        usageVi = "Dùng cho hợp âm maj7(♭13), hòa âm vay mượn và các đường giai điệu trưởng có màu tối.",
        usageEn = "Use it over maj7(♭13), borrowed harmony and major-key lines requiring a darker colour.",
        degreeLabels = listOf("1", "2", "3", "4", "5", "♭6", "7"),
        intervals = listOf(0, 2, 4, 5, 7, 8, 11),
        usesMajorCagedFingering = true,
        characteristicIntervals = listOf(8, 11),
        levelKey = "ADVANCED"
    ),
    DOUBLE_HARMONIC_MAJOR(
        key = "DOUBLE_HARMONIC_MAJOR",
        titleVi = "Double Harmonic Major",
        titleEn = "Double harmonic major",
        formula = "1 · ♭2 · 3 · 4 · 5 · ♭6 · 7",
        groupKey = "SPECIAL",
        summaryVi = "Âm giai trưởng có ♭2 và ♭6, tạo hai khoảng tăng 2 đặc trưng. Nó còn được gọi Byzantine trong một số tài liệu phương Tây.",
        summaryEn = "A major scale with ♭2 and ♭6, creating two characteristic augmented seconds. Some Western sources also call it Byzantine.",
        usageVi = "Dùng cho màu Trung Đông/Địa Trung Hải theo cách gọi phương Tây, metal, fusion và sáng tác cần độ căng mạnh.",
        usageEn = "Use it for Western-labelled Middle Eastern/Mediterranean colours, metal, fusion and composition requiring strong tension.",
        degreeLabels = listOf("1", "♭2", "3", "4", "5", "♭6", "7"),
        intervals = listOf(0, 1, 4, 5, 7, 8, 11),
        usesMajorCagedFingering = true,
        characteristicIntervals = listOf(1, 8),
        levelKey = "ADVANCED"
    ),
    RITUSEN(
        key = "RITUSEN",
        titleVi = "Ritusen ngũ cung",
        titleEn = "Ritusen pentatonic",
        formula = "1 · 2 · 4 · 5 · 6",
        groupKey = "WORLD_PENTATONIC",
        summaryVi = "Tập nốt ngũ cung không có bậc 3 và bậc 7. Tuner Tools tách riêng để không gọi nhầm là Mixolydian ngũ cung.",
        summaryEn = "A pentatonic collection without the 3rd or 7th. Tuner Tools keeps it separate to avoid mislabeling it as Mixolydian pentatonic.",
        usageVi = "Cho màu âm mở, lơ lửng; phù hợp giai điệu dân gian, sus và luyện nghe quãng.",
        usageEn = "Creates an open, suspended sound suitable for folk-like melodies, sus harmony and interval training.",
        degreeLabels = listOf("1", "2", "4", "5", "6"),
        intervals = listOf(0, 2, 5, 7, 9),
        usesMajorCagedFingering = true,
        levelKey = "TRADITIONAL"
    ),
    HIRAJOSHI(
        key = "HIRAJOSHI",
        titleVi = "Hirajoshi (dạng tham chiếu)",
        titleEn = "Hirajoshi (reference form)",
        formula = "1 · 2 · ♭3 · 5 · ♭6",
        groupKey = "WORLD_PENTATONIC",
        summaryVi = "Tuner Tools dùng dạng tham chiếu phương Tây 1–2–♭3–5–♭6. Tên và cao độ có thể thay đổi theo cách lên dây, nhạc cụ và truyền thống Nhật Bản.",
        summaryEn = "Tuner Tools uses the common Western reference form 1–2–♭3–5–♭6. Naming and pitches can vary with tuning, instrument and Japanese tradition.",
        usageVi = "Phù hợp sáng tác giai điệu ngũ cung có khoảng trống rộng, màu tối và luyện kiểm soát quãng.",
        usageEn = "Useful for pentatonic melody writing with wide gaps, a dark colour and interval-control practice.",
        degreeLabels = listOf("1", "2", "♭3", "5", "♭6"),
        intervals = listOf(0, 2, 3, 7, 8),
        usesMajorCagedFingering = false,
        characteristicIntervals = listOf(3, 8),
        levelKey = "TRADITIONAL"
    ),
    INSEN(
        key = "INSEN",
        titleVi = "Insen (dạng tham chiếu)",
        titleEn = "Insen (reference form)",
        formula = "1 · ♭2 · 4 · 5 · ♭7",
        groupKey = "WORLD_PENTATONIC",
        summaryVi = "Dạng tham chiếu phương Tây 1–♭2–4–5–♭7 tạo màu sus tối. Thuật ngữ truyền thống có thể khác theo nguồn và cách lên dây.",
        summaryEn = "The Western reference form 1–♭2–4–5–♭7 creates a dark suspended colour. Traditional terminology can vary by source and tuning.",
        usageVi = "Phù hợp nền sus, giai điệu tối, ambient, metal và bài tập nhấn ♭2.",
        usageEn = "Useful over sus harmony, dark melody, ambient, metal and exercises emphasizing the ♭2.",
        degreeLabels = listOf("1", "♭2", "4", "5", "♭7"),
        intervals = listOf(0, 1, 5, 7, 10),
        usesMajorCagedFingering = false,
        characteristicIntervals = listOf(1, 10),
        levelKey = "TRADITIONAL"
    ),
    IWATO(
        key = "IWATO",
        titleVi = "Iwato (dạng tham chiếu)",
        titleEn = "Iwato (reference form)",
        formula = "1 · ♭2 · 4 · ♭5 · ♭7",
        groupKey = "WORLD_PENTATONIC",
        summaryVi = "Dạng tham chiếu phương Tây 1–♭2–4–♭5–♭7 có ♭2 và ♭5 rất căng. Tên gọi lịch sử không nên hiểu như một mode phương Tây cố định tuyệt đối.",
        summaryEn = "The Western reference form 1–♭2–4–♭5–♭7 has strongly tense ♭2 and ♭5 degrees. Its historical name should not be treated as one absolutely fixed Western mode.",
        usageVi = "Phù hợp giai điệu tối, sound design, metal và luyện nghe các quãng căng.",
        usageEn = "Useful for dark melody, sound design, metal and ear training with tense intervals.",
        degreeLabels = listOf("1", "♭2", "4", "♭5", "♭7"),
        intervals = listOf(0, 1, 5, 6, 10),
        usesMajorCagedFingering = false,
        characteristicIntervals = listOf(1, 6),
        levelKey = "TRADITIONAL"
    );

    fun title(lang: String): String = fingerUiText(lang, titleVi, titleEn)
    fun summary(lang: String): String = fingerUiText(lang, summaryVi, summaryEn)
    fun usage(lang: String): String = fingerUiText(lang, usageVi, usageEn)

    fun degreeLabelForInterval(interval: Int): String =
        intervals.indexOf(interval).takeIf { it >= 0 }?.let(degreeLabels::get).orEmpty()

    fun degreeNumberForInterval(interval: Int): Int =
        degreeLabelForInterval(interval).filter(Char::isDigit).toIntOrNull() ?: 1
}

private data class HandShapeNote(
    val stringNumber: Int,
    val fret: Int,
    val finger: Int,
    val midi: Int,
    val noteName: String,
    val intervalFromRoot: Int,
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
        titleVi = "Arpeggio 7th · 4 nốt",
        titleEn = "7th arpeggio · 4 notes",
        noteCount = 4
    ),
    NINTH_ARPEGGIO(
        key = "NINTH_ARPEGGIO",
        titleVi = "Arpeggio 9th · 5 nốt",
        titleEn = "9th arpeggio · 5 notes",
        noteCount = 5
    ),
    ELEVENTH_ARPEGGIO(
        key = "ELEVENTH_ARPEGGIO",
        titleVi = "Arpeggio 11th · 6 nốt",
        titleEn = "11th arpeggio · 6 notes",
        noteCount = 6
    ),
    THIRTEENTH_ARPEGGIO(
        key = "THIRTEENTH_ARPEGGIO",
        titleVi = "Arpeggio 13th · 7 nốt",
        titleEn = "13th arpeggio · 7 notes",
        noteCount = 7
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
    ALL_TRIAD_INVERSIONS(
        key = "ALL_TRIAD_INVERSIONS",
        inversionIndex = -2,
        titleVi = "Cả 3 thế đảo",
        titleEn = "All 3 triad inversions"
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
    ),
    FOURTH("FOURTH", 4, "Đảo 4", "4th inversion"),
    FIFTH("FIFTH", 5, "Đảo 5", "5th inversion"),
    SIXTH("SIXTH", 6, "Đảo 6", "6th inversion");

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
    val voicing: HarmonyVoicing,
    val slotIndex: Int = 0,
    val beatNumber: Int = 1,
    val subdivisionIndex: Int = 0,
    val stepsPerBeat: Int = 1,
    val pickSymbol: String = "↓"
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

private fun guidedPracticeLessons(tunaGreen: Color): List<GuidedPracticeLesson> = listOf(
    GuidedPracticeLesson(
        id = "finger_01_four_fingers",
        number = "01",
        kind = GuidedLessonKind.FINGER,
        titleVi = "Đồng bộ bốn ngón",
        titleEn = "Four-finger coordination",
        objectiveVi = "Giữ tay thả lỏng và chơi 1-2-3-4 đều trên sáu dây.",
        objectiveEn = "Keep the hand relaxed and play 1-2-3-4 evenly across six strings.",
        theoryVi = "Mỗi ngón phụ trách một ngăn; gảy xuống-lên xen kẽ để hai tay hoạt động cùng nhịp.",
        theoryEn = "Each finger owns one fret; alternate picking synchronizes both hands.",
        applicationVi = "Áp dụng cùng nhịp vào câu chạy chromatic ngắn.",
        applicationEn = "Apply the same pulse to a short chromatic line.",
        routeKey = "1234",
        fingerStartFret = 5,
        bpmStart = 45,
        bpmTarget = 60,
        targetRounds = 3,
        accent = tunaGreen
    ),
    GuidedPracticeLesson(
        id = "finger_02_string_crossing",
        number = "02",
        kind = GuidedLessonKind.FINGER,
        titleVi = "Chuyển dây chính xác",
        titleEn = "Accurate string crossing",
        objectiveVi = "Đổi dây mà không dừng nhịp và giữ chuyển động tay gảy nhỏ.",
        objectiveEn = "Change strings without breaking the pulse and keep the picking motion compact.",
        theoryVi = "Gảy xen kẽ phải tiếp tục liên tục khi đổi dây, không khởi động lại ở mỗi dây.",
        theoryEn = "Alternate picking continues through string changes instead of restarting on each string.",
        applicationVi = "Dùng kỹ thuật này trong riff và câu solo qua nhiều dây.",
        applicationEn = "Use this motion in riffs and multi-string solo lines.",
        routeKey = "STRING_CROSSING",
        fingerStartFret = 5,
        bpmStart = 45,
        bpmTarget = 65,
        targetRounds = 3,
        accent = Color(0xFF65B9F5)
    ),
    GuidedPracticeLesson(
        id = "finger_03_independence",
        number = "03",
        kind = GuidedLessonKind.FINGER,
        titleVi = "Độc lập ngón 1-3-2-4",
        titleEn = "1-3-2-4 independence",
        objectiveVi = "Tách chuyển động từng ngón, đặc biệt cặp ngón 3-4.",
        objectiveEn = "Separate each finger movement, especially fingers 3 and 4.",
        theoryVi = "Chỉ nhấc ngón cần di chuyển; các ngón còn lại giữ gần dây đàn.",
        theoryEn = "Lift only the moving finger and keep the others close to the strings.",
        applicationVi = "Chuẩn bị cho các câu scale và arpeggio đổi thứ tự ngón.",
        applicationEn = "Prepare for scale and arpeggio lines with changing finger order.",
        routeKey = "1324",
        fingerStartFret = 5,
        bpmStart = 45,
        bpmTarget = 60,
        targetRounds = 3,
        accent = Color(0xFFF2B94B)
    ),
    GuidedPracticeLesson(
        id = "finger_04_spider",
        number = "04",
        kind = GuidedLessonKind.FINGER,
        titleVi = "Spider phối hợp chéo",
        titleEn = "Spider coordination",
        objectiveVi = "Giữ ngón đã bấm và chuyển ngón kế tiếp sang dây liền kề.",
        objectiveEn = "Keep planted fingers down while the next finger crosses to the adjacent string.",
        theoryVi = "Bài Spider rèn độc lập ngón và kiểm soát chuyển dây ở cả hai tay.",
        theoryEn = "The Spider drill develops finger independence and two-hand string control.",
        applicationVi = "Hữu ích cho hợp âm, legato và câu chạy có chuyển dây chéo.",
        applicationEn = "Useful for chords, legato and diagonal string-crossing lines.",
        routeKey = "SPIDER",
        fingerStartFret = 5,
        bpmStart = 40,
        bpmTarget = 55,
        targetRounds = 3,
        accent = Color(0xFFFF8A50)
    ),
    GuidedPracticeLesson(
        id = "finger_05_sequence",
        number = "05",
        kind = GuidedLessonKind.FINGER,
        titleVi = "Chuỗi ba nốt",
        titleEn = "Three-note sequences",
        objectiveVi = "Giữ nhịp đều giữa nhóm 1-2-3 và 2-3-4.",
        objectiveEn = "Keep an even pulse between 1-2-3 and 2-3-4 groups.",
        theoryVi = "Chuỗi nốt tạo cảm giác câu nhạc thay vì chỉ chạy thẳng lên xuống.",
        theoryEn = "Sequences create musical phrasing instead of merely running straight up and down.",
        applicationVi = "Dùng mẫu nhóm ba để phát triển câu solo theo âm giai.",
        applicationEn = "Use three-note groups to build scale-based solo phrases.",
        routeKey = "SCALE_SEQUENCE",
        fingerStartFret = 5,
        bpmStart = 50,
        bpmTarget = 70,
        targetRounds = 3,
        accent = Color(0xFF45CBA5)
    ),
    GuidedPracticeLesson(
        id = "finger_06_legato",
        number = "06",
        kind = GuidedLessonKind.FINGER,
        titleVi = "Hammer-on & Pull-off",
        titleEn = "Hammer-on & pull-off",
        objectiveVi = "Tạo âm đều bằng tay trái mà không gảy lại mọi nốt.",
        objectiveEn = "Produce even notes with the fretting hand without repicking every note.",
        theoryVi = "Gảy nốt đầu, sau đó dùng lực ngón tay trái nhưng vẫn giữ đúng nhịp.",
        theoryEn = "Pick the first note, then articulate with the fretting hand while staying in time.",
        applicationVi = "Áp dụng vào câu legato và liên kết các vị trí trên cần đàn.",
        applicationEn = "Apply it to legato phrases and position connections.",
        routeKey = "HAMMER_PULL",
        fingerStartFret = 5,
        bpmStart = 45,
        bpmTarget = 65,
        pickingPatternKey = "LEGATO",
        targetRounds = 3,
        accent = Color(0xFFB274F2)
    ),
    // GIAI ĐOẠN TRIAD: cấu tạo → thế đảo → bản đồ giọng → ứng dụng vòng hợp âm.
    GuidedPracticeLesson(
        id = "harmony_01_triad_structure",
        number = "07",
        kind = GuidedLessonKind.HARMONY,
        titleVi = "Triad · Cấu tạo 1-3-5",
        titleEn = "Triads · 1-3-5 structure",
        objectiveVi = "Nghe và nhìn nốt gốc, bậc 3 và bậc 5 trong vòng I-IV-V.",
        objectiveEn = "Hear and see the root, third and fifth through an I-IV-V progression.",
        theoryVi = "Bậc 3 quyết định tính chất trưởng/thứ; bậc 5 tạo độ ổn định.",
        theoryEn = "The third defines major or minor quality; the fifth adds stability.",
        applicationVi = "Chơi C-F-G trên nhóm dây 1-2-3 với nhịp đều.",
        applicationEn = "Play C-F-G on strings 1-2-3 with an even pulse.",
        practiceTypeKey = "TRIAD",
        progressionKey = "I_IV_V_MAJOR",
        stringSetKey = "STRINGS_123",
        inversionKey = "ROOT",
        positionStart = 3,
        bpmStart = 50,
        bpmTarget = 60,
        subdivisionKey = "QUARTER",
        beatsPerChord = 4,
        targetRounds = 3,
        accent = Color(0xFFE85D86)
    ),
    GuidedPracticeLesson(
        id = "harmony_02_inversions",
        number = "08",
        kind = GuidedLessonKind.HARMONY,
        titleVi = "Triad · Ba thế đảo",
        titleEn = "Triads · Three inversions",
        objectiveVi = "Nhận biết thế gốc, đảo 1 và đảo 2 trong cùng vùng cần đàn.",
        objectiveEn = "Recognize root position, first and second inversion in one fretboard area.",
        theoryVi = "Nốt thấp nhất quyết định tên thế đảo, nhưng ba nốt của hợp âm không đổi.",
        theoryEn = "The lowest note determines the inversion while the three chord tones remain the same.",
        applicationVi = "Chuyển I-IV-V bằng thế gần nhất thay vì nhảy vị trí xa.",
        applicationEn = "Connect I-IV-V with the nearest inversion instead of large position jumps.",
        practiceTypeKey = "TRIAD",
        progressionKey = "I_IV_V_MAJOR",
        stringSetKey = "STRINGS_123",
        inversionKey = "AUTO",
        positionStart = 5,
        bpmStart = 50,
        bpmTarget = 65,
        subdivisionKey = "QUARTER",
        beatsPerChord = 4,
        targetRounds = 3,
        accent = Color(0xFF65B9F5)
    ),
    GuidedPracticeLesson(
        id = "harmony_03_diatonic_major_triads",
        number = "09",
        kind = GuidedLessonKind.HARMONY,
        titleVi = "Triad · 7 hợp âm trong giọng trưởng",
        titleEn = "Triads · Seven chords in a major key",
        objectiveVi = "Chơi I-ii-iii-IV-V-vi-vii° và nhận biết thế gốc, đảo 1, đảo 2 trên dây 1-2-3.",
        objectiveEn = "Play I-ii-iii-IV-V-vi-vii° and recognize root, first and second inversion on strings 1-2-3.",
        theoryVi = "Trong mọi giọng trưởng, tính chất bảy hợp âm luôn là: Trưởng-Thứ-Thứ-Trưởng-Trưởng-Thứ-Giảm.",
        theoryEn = "In every major key, the seven triads follow: Major-Minor-Minor-Major-Major-Minor-Diminished.",
        applicationVi = "Tông C: C-Dm-Em-F-G-Am-Bdim; đây là bản đồ nền để đệm hát, phân tích bài và tạo vòng hợp âm.",
        applicationEn = "In C: C-Dm-Em-F-G-Am-Bdim; this map supports accompaniment, analysis and progression building.",
        practiceTypeKey = "TRIAD",
        keyName = "C",
        progressionKey = "DIATONIC_TRIADS_MAJOR",
        stringSetKey = "STRINGS_123",
        inversionKey = "ALL_TRIAD_INVERSIONS",
        positionStart = 0,
        bpmStart = 50,
        bpmTarget = 50,
        subdivisionKey = "QUARTER",
        beatsPerChord = 4,
        targetRounds = 1,
        accent = Color(0xFF22C58B)
    ),
    GuidedPracticeLesson(
        id = "harmony_03_ii_v_i_triad",
        number = "10",
        kind = GuidedLessonKind.HARMONY,
        titleVi = "Triad · ii-V-I",
        titleEn = "Triads · ii-V-I",
        objectiveVi = "Nối Dm-G-C bằng chuyển động tay tối thiểu.",
        objectiveEn = "Connect Dm-G-C with minimal hand movement.",
        theoryVi = "ii tạo chuẩn bị, V tạo lực hút và I tạo cảm giác giải quyết.",
        theoryEn = "ii prepares, V creates tension and I provides resolution.",
        applicationVi = "Dùng trong Ballad, Jazz và câu solo theo hợp âm.",
        applicationEn = "Use it in ballads, jazz and chord-tone soloing.",
        practiceTypeKey = "TRIAD",
        progressionKey = "II_V_I_MAJOR",
        stringSetKey = "STRINGS_123",
        inversionKey = "AUTO",
        positionStart = 5,
        bpmStart = 55,
        bpmTarget = 70,
        subdivisionKey = "EIGHTH",
        beatsPerChord = 4,
        targetRounds = 3,
        accent = Color(0xFFF2B94B)
    ),
    GuidedPracticeLesson(
        id = "harmony_05_pop_voice_leading",
        number = "11",
        kind = GuidedLessonKind.HARMONY,
        titleVi = "Triad · Voice-leading vòng Pop",
        titleEn = "Triads · Pop voice-leading",
        objectiveVi = "Nối I-V-vi-IV bằng nốt chung và thế đảo gần nhất.",
        objectiveEn = "Connect I-V-vi-IV using common tones and nearest inversions.",
        theoryVi = "Giữ nốt chung giúp chuyển hợp âm mượt và giảm chuyển động thừa.",
        theoryEn = "Retaining common tones smooths chord changes and reduces unnecessary motion.",
        applicationVi = "Áp dụng trực tiếp vào Pop và Ballad hiện đại.",
        applicationEn = "Apply it directly to modern pop and ballad accompaniment.",
        practiceTypeKey = "TRIAD",
        progressionKey = "I_V_VI_IV",
        stringSetKey = "STRINGS_234",
        inversionKey = "AUTO",
        positionStart = 5,
        bpmStart = 60,
        bpmTarget = 75,
        subdivisionKey = "EIGHTH",
        beatsPerChord = 4,
        targetRounds = 3,
        accent = Color(0xFF45CBA5)
    ),

    // GIAI ĐOẠN HỢP ÂM 7: guide tones → chương 13 gồm Trưởng, so sánh dominant và Thứ.
    GuidedPracticeLesson(
        id = "harmony_04_seventh",
        number = "12",
        kind = GuidedLessonKind.HARMONY,
        titleVi = "Hợp âm 7 · Arpeggio & Guide Tones",
        titleEn = "Seventh chords · Arpeggios & guide tones",
        objectiveVi = "Chơi Dm7-G7-Cmaj7 và nghe màu sắc của bậc 7.",
        objectiveEn = "Play Dm7-G7-Cmaj7 and hear the color of each seventh.",
        theoryVi = "Bậc 3 và bậc 7 là guide tones thể hiện rõ chức năng hòa âm.",
        theoryEn = "The third and seventh are guide tones that reveal harmonic function.",
        applicationVi = "Kết câu vào bậc 3 hoặc bậc 7 khi solo trên ii-V-I.",
        applicationEn = "Resolve phrases to the third or seventh over a ii-V-I.",
        practiceTypeKey = "SEVENTH_ARPEGGIO",
        progressionKey = "II_V_I_MAJOR",
        stringSetKey = "STRINGS_1234",
        inversionKey = "AUTO",
        positionStart = 5,
        bpmStart = 55,
        bpmTarget = 70,
        subdivisionKey = "EIGHTH",
        beatsPerChord = 4,
        targetRounds = 3,
        accent = Color(0xFFB274F2)
    ),
    GuidedPracticeLesson(
        id = "harmony_06_circle_major",
        number = "13A",
        kind = GuidedLessonKind.HARMONY,
        chapterKey = SEVENTH_CIRCLE_CHAPTER_KEY,
        titleVi = "Vòng quãng 4 trưởng",
        titleEn = "Major circle of fourths",
        objectiveVi = "Chạy Cmaj7-Fmaj7-Bm7♭5-Em7-Am7-Dm7-G7 và nghe G7 giải quyết về Cmaj7.",
        objectiveEn = "Run Cmaj7-Fmaj7-Bm7♭5-Em7-Am7-Dm7-G7 and hear G7 resolve to Cmaj7.",
        theoryVi = "Đây là bảy hợp âm 7 thuận âm của C trưởng, sắp xếp theo quãng 4 để luyện chức năng và dẫn giọng.",
        theoryEn = "These are the seven diatonic seventh chords of C major, ordered by fourths for function and voice-leading.",
        applicationVi = "Dùng vòng này để phân tích hòa âm, luyện guide tones và chuyển hợp âm mượt trong Jazz/Pop.",
        applicationEn = "Use this cycle for harmonic analysis, guide-tone practice and smooth jazz/pop changes.",
        practiceTypeKey = "SEVENTH_ARPEGGIO",
        keyName = "C",
        progressionKey = "CIRCLE_FOURTHS_MAJOR",
        stringSetKey = "STRINGS_2345",
        inversionKey = "AUTO",
        positionStart = 5,
        bpmStart = 55,
        bpmTarget = 70,
        subdivisionKey = "EIGHTH",
        beatsPerChord = 4,
        targetRounds = 2,
        accent = Color(0xFF6D91E8)
    ),
    GuidedPracticeLesson(
        id = "harmony_06_em7_e7_comparison",
        number = "13B",
        kind = GuidedLessonKind.HARMONY,
        chapterKey = SEVENTH_CIRCLE_CHAPTER_KEY,
        titleVi = "Em7 và E7 khác nhau thế nào",
        titleEn = "How Em7 and E7 differ",
        objectiveVi = "So sánh Em7→Am7 với E7→Am7 và nghe nốt G nâng thành G# tạo lực hút về A.",
        objectiveEn = "Compare Em7→Am7 with E7→Am7 and hear G rise to G#, creating a pull toward A.",
        theoryVi = "Em7 thuộc A thứ tự nhiên; E7 mượn G# từ A thứ hòa thanh để trở thành dominant V7.",
        theoryEn = "Em7 belongs to A natural minor; E7 borrows G# from A harmonic minor to become the dominant V7.",
        applicationVi = "Nghe và nhìn trực tiếp hai cách giải quyết để hiểu vì sao E7→Am mạnh hơn Em7→Am.",
        applicationEn = "Hear and see both resolutions to understand why E7→Am is stronger than Em7→Am.",
        practiceTypeKey = "SEVENTH_ARPEGGIO",
        keyName = "A",
        progressionKey = "MINOR_DOMINANT_COMPARISON",
        stringSetKey = "STRINGS_2345",
        inversionKey = "AUTO",
        positionStart = 5,
        bpmStart = 50,
        bpmTarget = 60,
        subdivisionKey = "QUARTER",
        beatsPerChord = 4,
        targetRounds = 3,
        accent = Color(0xFFF2B94B)
    ),
    GuidedPracticeLesson(
        id = "harmony_06_circle_minor",
        number = "13C",
        kind = GuidedLessonKind.HARMONY,
        chapterKey = SEVENTH_CIRCLE_CHAPTER_KEY,
        titleVi = "Vòng quãng 4 thứ",
        titleEn = "Minor circle of fourths",
        objectiveVi = "Tổng hợp Arpeggio 7, dẫn giọng và thứ hòa thanh qua Am7-Dm7-G7-Cmaj7-Fmaj7-Bm7♭5-E7.",
        objectiveEn = "Combine seventh-chord arpeggios, voice-leading and harmonic minor through Am7-Dm7-G7-Cmaj7-Fmaj7-Bm7♭5-E7.",
        theoryVi = "E7 dùng G# từ thứ hòa thanh để tạo lực hút mạnh về Am.",
        theoryEn = "E7 uses G# from harmonic minor to pull strongly back to Am.",
        applicationVi = "Ứng dụng vào Jazz turnaround, nhạc xưa và chuyển tông.",
        applicationEn = "Apply it to jazz turnarounds, standards and key movement.",
        practiceTypeKey = "SEVENTH_ARPEGGIO",
        keyName = "A",
        progressionKey = "CIRCLE_FOURTHS_MINOR",
        stringSetKey = "STRINGS_2345",
        inversionKey = "AUTO",
        positionStart = 5,
        bpmStart = 55,
        bpmTarget = 75,
        subdivisionKey = "EIGHTH",
        beatsPerChord = 4,
        targetRounds = 2,
        accent = Color(0xFFFF8A50)
    )
)

private val harmonyNoteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

private fun harmonyProgressions(): List<HarmonyProgression> {
    val majorI = HarmonyDegreeSpec(1, "I", HarmonyChordQuality.MAJOR, HarmonyChordQuality.MAJ7)
    val majorIi = HarmonyDegreeSpec(2, "ii", HarmonyChordQuality.MINOR, HarmonyChordQuality.MIN7)
    val majorIv = HarmonyDegreeSpec(4, "IV", HarmonyChordQuality.MAJOR, HarmonyChordQuality.MAJ7)
    val majorV = HarmonyDegreeSpec(5, "V", HarmonyChordQuality.MAJOR, HarmonyChordQuality.DOM7)
    val majorVi = HarmonyDegreeSpec(6, "vi", HarmonyChordQuality.MINOR, HarmonyChordQuality.MIN7)
    // The diatonic circle of fourths additionally uses iii and vii° in major.
    val majorIii = HarmonyDegreeSpec(3, "iii", HarmonyChordQuality.MINOR, HarmonyChordQuality.MIN7)
    val majorVii = HarmonyDegreeSpec(7, "vii°", HarmonyChordQuality.DIMINISHED, HarmonyChordQuality.HALF_DIM)

    val minorI = HarmonyDegreeSpec(1, "i", HarmonyChordQuality.MINOR, HarmonyChordQuality.MIN_MAJ7)
    // In the natural-minor / dominant-V7 circle, tonic i7 is m7 (e.g. Am7),
    // unlike the minor-major-seventh tonic used by the existing minor cadences.
    val minorCircleI = HarmonyDegreeSpec(1, "i", HarmonyChordQuality.MINOR, HarmonyChordQuality.MIN7)
    val minorComparisonI = HarmonyDegreeSpec(1, "i7", HarmonyChordQuality.MINOR, HarmonyChordQuality.MIN7)
    val minorNaturalV = HarmonyDegreeSpec(5, "v7", HarmonyChordQuality.MINOR, HarmonyChordQuality.MIN7)
    val minorDominantV = HarmonyDegreeSpec(5, "V7", HarmonyChordQuality.MAJOR, HarmonyChordQuality.DOM7)
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
            key = "DIATONIC_TRIADS_MAJOR",
            titleVi = "7 Triad trong giọng Trưởng",
            titleEn = "Major-key diatonic triads",
            descriptionVi = "Theo bậc âm giai I-ii-iii-IV-V-vi-vii°: C-Dm-Em-F-G-Am-Bdim trong tông C.",
            descriptionEn = "Scale order I-ii-iii-IV-V-vi-vii°: C-Dm-Em-F-G-Am-Bdim in C major.",
            tonality = HarmonyTonality.MAJOR,
            degrees = listOf(majorI, majorIi, majorIii, majorIv, majorV, majorVi, majorVii)
        ),
        HarmonyProgression(
            key = "CIRCLE_FOURTHS_MAJOR",
            titleVi = "Quãng 4 · Trưởng",
            titleEn = "Circle of 4ths · Major",
            descriptionVi = "Vòng 1–4–7–3–6–2–5: C–F–Bdim–Em–Am–Dm–G (tông C). Kết vòng về I.",
            descriptionEn = "Diatonic 1–4–7–3–6–2–5: C–F–Bdim–Em–Am–Dm–G in C; resolves to I.",
            tonality = HarmonyTonality.MAJOR,
            degrees = listOf(majorI, majorIv, majorVii, majorIii, majorVi, majorIi, majorV)
        ),
        HarmonyProgression(
            key = "MINOR_DOMINANT_COMPARISON",
            titleVi = "So sánh Em7 và E7",
            titleEn = "Compare Em7 and E7",
            descriptionVi = "A thứ: Em7→Am7 (thứ tự nhiên) so với E7→Am7 (thứ hòa thanh, G# dẫn về A).",
            descriptionEn = "A minor: compare Em7→Am7 (natural minor) with E7→Am7 (harmonic minor, G# leading to A).",
            tonality = HarmonyTonality.MINOR,
            degrees = listOf(minorNaturalV, minorComparisonI, minorDominantV, minorComparisonI)
        ),
        HarmonyProgression(
            key = "CIRCLE_FOURTHS_MINOR",
            titleVi = "Quãng 4 · Thứ",
            titleEn = "Circle of 4ths · Minor",
            descriptionVi = "Tông Am: Triad Am–Dm–G–C–F–Bdim–E; arpeggio 7 Am7–Dm7–G7–Cmaj7–Fmaj7–Bm7♭5–E7.",
            descriptionEn = "A minor: triads Am–Dm–G–C–F–Bdim–E; 7ths Am7–Dm7–G7–Cmaj7–Fmaj7–Bm7♭5–E7.",
            tonality = HarmonyTonality.MINOR,
            degrees = listOf(minorCircleI, minorIv, minorVii, minorIii, minorVi, minorIi, minorV)
        ),
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
    HarmonyPracticeType.NINTH_ARPEGGIO -> listOf(
        HarmonyStringSet("STRINGS_12345", listOf(5, 4, 3, 2, 1), "Dây 1-2-3-4-5", "Strings 1-2-3-4-5"),
        HarmonyStringSet("STRINGS_23456", listOf(6, 5, 4, 3, 2), "Dây 2-3-4-5-6", "Strings 2-3-4-5-6")
    )
    HarmonyPracticeType.ELEVENTH_ARPEGGIO -> listOf(
        HarmonyStringSet("STRINGS_123456", listOf(6, 5, 4, 3, 2, 1), "6 dây · 6 nốt", "6 strings · 6 notes")
    )
    HarmonyPracticeType.THIRTEENTH_ARPEGGIO -> listOf(
        // Seven successive pitches on six guitar strings: two notes must be played on one string.
        HarmonyStringSet("STRINGS_123456_7", listOf(6, 5, 4, 3, 2, 1), "6 dây · 7 nốt", "6 strings · 7 notes")
    )
}

private fun harmonyInversionModes(type: HarmonyPracticeType): List<HarmonyInversionMode> =
    HarmonyInversionMode.values().filter { mode ->
        when {
            mode == HarmonyInversionMode.AUTO -> true
            mode == HarmonyInversionMode.ALL_TRIAD_INVERSIONS -> type == HarmonyPracticeType.TRIAD
            mode.inversionIndex < 0 -> false
            else -> mode.inversionIndex < type.noteCount
        }
    }

private fun harmonyRoleLabel(interval: Int): String = when (interval) {
    0 -> "R"
    3 -> "♭3"
    4 -> "3"
    6 -> "♭5"
    7 -> "5"
    8 -> "♯5"
    9 -> "𝄫7"
    10 -> "♭7"
    11 -> "7"
    13 -> "♭9"
    14 -> "9"
    15 -> "♯9"
    16 -> "♭11"
    17 -> "11"
    18 -> "♯11"
    20 -> "♭13"
    21 -> "13"
    22 -> "♯13"
    else -> interval.toString()
}

/**
 * Extensions follow the parent key rather than always assuming natural 9/11/13.
 * Examples: Fmaj7 in C gets #11 (B); Bm7b5 in C gets b9 (C).
 * E7 in A minor gets b9 (F) and b13 (C), as in harmonic-minor dominant usage.
 * The familiar blues dominants use natural 9, 11, and 13.
 */
private fun harmonyExtensionIntervals(
    progression: HarmonyProgression,
    degreeIndex: Int,
    noteCount: Int
): List<Int> {
    if (noteCount <= 4) return emptyList()
    if (progression.key == "TWELVE_BAR_BLUES") {
        return listOf(14, 17, 21).take(noteCount - 4)
    }
    val rootScaleSemitone = progression.tonality.degreeSemitones[degreeIndex]
    return listOf(1, 3, 5).take(noteCount - 4).map { diatonicStep ->
        val nextDegree = (degreeIndex + diatonicStep) % 7
        val targetScaleSemitone = progression.tonality.degreeSemitones[nextDegree]
        12 + normalizedPitchClass(targetScaleSemitone - rootScaleSemitone)
    }
}

private fun harmonyExtendedChordSuffix(
    quality: HarmonyChordQuality,
    extensionIntervals: List<Int>
): String {
    if (extensionIntervals.isEmpty()) return quality.suffix
    val labels = extensionIntervals.map(::harmonyRoleLabel)
    val allNatural = labels == listOf("9", "11", "13").take(labels.size)
    if (allNatural) {
        val suffix = when (quality) {
            HarmonyChordQuality.MAJ7 -> "maj"
            HarmonyChordQuality.MIN7 -> "m"
            HarmonyChordQuality.DOM7 -> ""
            HarmonyChordQuality.MIN_MAJ7 -> "mMaj"
            else -> null
        }
        if (suffix != null) {
            val highestExtension = listOf(9, 11, 13)[labels.lastIndex]
            return "$suffix$highestExtension"
        }
    }
    // An explicit alteration list avoids calling e.g. E7(b9,b13) an E13 chord.
    return quality.suffix + labels.joinToString(prefix = "(", postfix = ")")
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
        val extensions = harmonyExtensionIntervals(progression, degreeIndex, practiceType.noteCount)
        val rootName = harmonyNoteNames[rootPitchClass]
        HarmonyChord(
            index = index,
            roman = degreeSpec.roman,
            rootName = rootName,
            rootPitchClass = rootPitchClass,
            quality = quality,
            symbol = rootName + harmonyExtendedChordSuffix(quality, extensions),
            intervals = quality.intervals + extensions
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

/**
 * A 9th/11th/13th is a melodic arpeggio, not a 5/6/7-string simultaneous grip.
 * Notes are sounded in ascending pitch on adjacent strings. A 13th (7 notes)
 * uses two consecutive frets on one of the six strings; no note is discarded.
 * Bounded beam search prevents factorial permutations and keeps changes responsive.
 */
private fun buildHarmonyExtendedVoicing(
    chord: HarmonyChord,
    stringSet: HarmonyStringSet,
    fretRange: IntRange,
    inversionMode: HarmonyInversionMode,
    previous: HarmonyVoicing?
): HarmonyVoicing? {
    val strings = stringSet.strings.sortedDescending()
    val toneCount = chord.intervals.size
    if (toneCount !in 5..7 || toneCount !in strings.size..(strings.size + 1)) return null

    val stringLayouts: List<List<Int>> = if (toneCount == strings.size) {
        listOf(strings)
    } else {
        strings.indices.map { repeatAt ->
            strings.flatMapIndexed { index, value ->
                if (index == repeatAt) listOf(value, value) else listOf(value)
            }
        }
    }
    val desiredInversions = if (inversionMode == HarmonyInversionMode.AUTO) {
        chord.intervals.indices.toList()
    } else {
        listOf(inversionMode.inversionIndex.coerceIn(0, chord.intervals.lastIndex))
    }
    val ranges = listOf(
        fretRange,
        maxOf(0, fretRange.first - 2)..minOf(20, fretRange.last + 2),
        maxOf(0, fretRange.first - 4)..minOf(20, fretRange.last + 4),
        0..20
    ).distinct()
    val centerFret = (fretRange.first + fretRange.last) / 2f

    data class PartialPath(val notes: List<HarmonyFretNote>, val cost: Float)

    for (range in ranges) {
        val voicings = mutableListOf<HarmonyVoicing>()
        for (inversionIndex in desiredInversions) {
            val rotated = chord.intervals.drop(inversionIndex) + chord.intervals.take(inversionIndex)
            for (layout in stringLayouts) {
                var paths = listOf(PartialPath(emptyList(), 0f))
                for (toneIndex in rotated.indices) {
                    val stringNumber = layout[toneIndex]
                    val interval = rotated[toneIndex]
                    val pitchClass = normalizedPitchClass(chord.rootPitchClass + interval)
                    val fretOptions = harmonyFretsForPitchClass(stringNumber, pitchClass, range)
                    val openMidi = guitarOpenMidiByString.getValue(stringNumber)
                    val expanded = mutableListOf<PartialPath>()
                    for (path in paths) {
                        for (fret in fretOptions) {
                            val midi = openMidi + fret
                            val prior = path.notes.lastOrNull()
                            if (prior != null && midi <= prior.midi) continue
                            val note = HarmonyFretNote(
                                stringNumber = stringNumber,
                                fret = fret,
                                midi = midi,
                                pitchClass = pitchClass,
                                noteName = harmonyNoteNames[pitchClass],
                                interval = interval,
                                roleLabel = harmonyRoleLabel(interval)
                            )
                            val leap = if (prior == null) 0 else midi - prior.midi
                            val newCost = path.cost +
                                    kotlin.math.abs(fret - centerFret) * 0.75f +
                                    (if (prior == null) 0f else kotlin.math.abs(leap - 4) * 0.35f) +
                                    (if (leap > 12) (leap - 12) * 5f else 0f)
                            expanded += PartialPath(path.notes + note, newCost)
                        }
                    }
                    paths = expanded.sortedBy { it.cost }.take(24)
                    if (paths.isEmpty()) break
                }
                for (path in paths.take(3)) {
                    if (path.notes.size != toneCount) continue
                    val frets = path.notes.map { it.fret }
                    voicings += HarmonyVoicing(
                        chord = chord,
                        notes = path.notes,
                        inversionIndex = inversionIndex,
                        fretSpan = frets.maxOrNull()!! - frets.minOrNull()!!,
                        averageFret = frets.average().toFloat()
                    )
                }
            }
        }
        if (voicings.isNotEmpty()) {
            return voicings.minByOrNull { candidate ->
                harmonyVoicingScore(candidate, previous, fretRange, inversionMode)
            }
        }
    }
    return null
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
    val playingStrings = if (orderedIntervals.size == strings.size + 1) {
        strings.dropLast(1) + listOf(strings.last(), strings.last())
    } else strings
    val notes = playingStrings.mapIndexed { index, stringNumber ->
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
        val previousByString = previous.notes.groupBy { it.stringNumber }
        candidate.notes.forEach { note ->
            val prior = previousByString[note.stringNumber]
                ?.minByOrNull { kotlin.math.abs(it.midi - note.midi) }
            if (prior != null) {
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
    val pool = if (inversionMode == HarmonyInversionMode.AUTO) {
        candidates
    } else {
        // For an explicitly requested inversion, keep expanding the search range
        // until that exact bass note is available. Do not silently substitute a
        // different inversion merely because it appears in a smaller fret window.
        candidates.filter { it.inversionIndex == inversionMode.inversionIndex }
            .ifEmpty { return null }
    }
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

    fun resolveChord(
        chord: HarmonyChord,
        mode: HarmonyInversionMode,
        prior: HarmonyVoicing?
    ): HarmonyVoicing {
        if (chord.intervals.size > 4) {
            val extended = buildHarmonyExtendedVoicing(
                chord = chord,
                stringSet = stringSet,
                fretRange = fretRange,
                inversionMode = mode,
                previous = prior
            )
            if (extended != null) return extended
        }

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
                previous = prior,
                fretRange = fretRange,
                inversionMode = mode
            )
            if (selected != null) break
        }

        return selected ?: fallbackHarmonyVoicing(
            chord = chord,
            stringSet = stringSet,
            fretRange = fretRange,
            inversionMode = mode
        )
    }

    // Educational map used by "7 Triad trong giọng trưởng": each scale-degree
    // triad is played as root position, first inversion and second inversion.
    // This mirrors the common conservatory/fretboard chart approach while still
    // reusing the normal player, rhythm engine and note highlighting.
    if (inversionMode == HarmonyInversionMode.ALL_TRIAD_INVERSIONS) {
        val triadModes = listOf(
            HarmonyInversionMode.ROOT,
            HarmonyInversionMode.FIRST,
            HarmonyInversionMode.SECOND
        )
        chords.forEach { chord ->
            triadModes.forEach { mode ->
                val resolved = resolveChord(chord, mode, previous)
                result += resolved
                previous = resolved
            }
        }
        return result
    }

    chords.forEach { chord ->
        val resolved = resolveChord(chord, inversionMode, previous)
        result += resolved
        previous = resolved
    }

    return result
}

private fun buildHarmonyPlaybackSequence(
    voicings: List<HarmonyVoicing>,
    direction: HarmonyRunDirection,
    subdivision: PracticeSubdivision,
    beatsPerChord: Int,
    pickingPattern: PracticePickingPattern
): List<HarmonyPlaybackStep> = voicings.flatMapIndexed { chordIndex, voicing ->
    val ascending = voicing.notes.sortedBy { it.midi }
    val ordered = when (direction) {
        HarmonyRunDirection.ASCENDING -> ascending
        HarmonyRunDirection.DESCENDING -> ascending.asReversed()
        HarmonyRunDirection.ALTERNATING -> if (chordIndex % 2 == 0) ascending else ascending.asReversed()
    }
    if (ordered.isEmpty()) return@flatMapIndexed emptyList()

    // The harmonic rhythm is measured in beats, not in the number of chord tones.
    // If a 9th/11th/13th contains more tones than the chosen grid can hold, the
    // engine automatically increases the density (e.g. 1/4 -> 1/8) so no tone
    // is silently omitted while every chord still lasts the selected beat count.
    val safeBeats = beatsPerChord.coerceIn(1, 8)
    val minimumStepsPerBeat = kotlin.math.ceil(ordered.size / safeBeats.toFloat())
        .toInt()
        .coerceAtLeast(1)
    val effectiveStepsPerBeat = maxOf(subdivision.stepsPerBeat, minimumStepsPerBeat)
    val totalSlots = safeBeats * effectiveStepsPerBeat

    (0 until totalSlots).map { slotIndex ->
        val noteIndex = slotIndex % ordered.size
        val note = ordered[noteIndex]
        val movingUp = ordered.size < 2 || ordered.last().midi >= ordered.first().midi
        val pickSymbol = when (pickingPattern) {
            PracticePickingPattern.ALTERNATE -> if (slotIndex % 2 == 0) "↓" else "↑"
            PracticePickingPattern.DOWN_ONLY -> "↓"
            PracticePickingPattern.UP_ONLY -> "↑"
            PracticePickingPattern.SWEEP -> if (movingUp) "↓" else "↑"
            PracticePickingPattern.LEGATO -> when {
                noteIndex == 0 -> "↓"
                ordered[noteIndex].midi >= ordered[noteIndex - 1].midi -> "H"
                else -> "P"
            }
        }
        HarmonyPlaybackStep(
            chordIndex = chordIndex,
            noteIndex = noteIndex,
            note = note,
            voicing = voicing,
            slotIndex = slotIndex,
            beatNumber = slotIndex / effectiveStepsPerBeat + 1,
            subdivisionIndex = slotIndex % effectiveStepsPerBeat,
            stepsPerBeat = effectiveStepsPerBeat,
            pickSymbol = pickSymbol
        )
    }
}

private fun fingerPickSymbol(
    index: Int,
    step: FingerTrainingStep?,
    previous: FingerTrainingStep?,
    pattern: PracticePickingPattern
): String = when (pattern) {
    PracticePickingPattern.ALTERNATE -> if (index % 2 == 0) "↓" else "↑"
    PracticePickingPattern.DOWN_ONLY -> "↓"
    PracticePickingPattern.UP_ONLY -> "↑"
    PracticePickingPattern.SWEEP -> when {
        step == null || previous == null -> "↓"
        step.stringNumber < previous.stringNumber -> "↓"
        step.stringNumber > previous.stringNumber -> "↑"
        else -> if (index % 2 == 0) "↓" else "↑"
    }
    PracticePickingPattern.LEGATO -> when {
        step == null -> "-"
        index == 0 || previous?.stringNumber != step.stringNumber -> "↓"
        step.technique.contains("Pull", ignoreCase = true) -> "P"
        step.technique.contains("Hammer", ignoreCase = true) -> "H"
        else -> "H"
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


private data class HandShapeTonicOption(
    val key: String,
    val label: String,
    val pitchClass: Int
)

/*
 * Use one conventional spelling for each pitch class. D#/G#/A# are migrated to
 * Eb/Ab/Bb so modal and scale names avoid unnecessary double accidentals.
 */
private val handShapeTonicOptions = listOf(
    HandShapeTonicOption("C", "C", 0),
    HandShapeTonicOption("C#", "C♯", 1),
    HandShapeTonicOption("D", "D", 2),
    HandShapeTonicOption("Eb", "E♭", 3),
    HandShapeTonicOption("E", "E", 4),
    HandShapeTonicOption("F", "F", 5),
    HandShapeTonicOption("F#", "F♯", 6),
    HandShapeTonicOption("G", "G", 7),
    HandShapeTonicOption("Ab", "A♭", 8),
    HandShapeTonicOption("A", "A", 9),
    HandShapeTonicOption("Bb", "B♭", 10),
    HandShapeTonicOption("B", "B", 11)
)

private val legacyHandShapeTonicAliases = mapOf(
    "D#" to "Eb",
    "G#" to "Ab",
    "A#" to "Bb"
)

private fun normalizeHandShapeTonicKey(value: String?): String {
    val migrated = legacyHandShapeTonicAliases[value] ?: value
    return migrated?.takeIf { key -> handShapeTonicOptions.any { it.key == key } } ?: "C"
}

private fun handShapeTonicLabel(tonicKey: String): String =
    handShapeTonicOptions.firstOrNull { it.key == normalizeHandShapeTonicKey(tonicKey) }?.label ?: "C"

private fun tonicPitchClass(tonicName: String): Int =
    handShapeTonicOptions.firstOrNull { it.key == normalizeHandShapeTonicKey(tonicName) }?.pitchClass ?: 0

private val naturalPitchClassByLetter = mapOf(
    'C' to 0,
    'D' to 2,
    'E' to 4,
    'F' to 5,
    'G' to 7,
    'A' to 9,
    'B' to 11
)

private val diatonicLetterOrder = listOf('C', 'D', 'E', 'F', 'G', 'A', 'B')

private fun tonicLetterAndAccidental(tonicKey: String): Pair<Char, Int> {
    val normalized = normalizeHandShapeTonicKey(tonicKey)
    val letter = normalized.firstOrNull()?.uppercaseChar() ?: 'C'
    val accidental = when {
        normalized.contains('#') -> 1
        normalized.contains('b') -> -1
        else -> 0
    }
    return letter to accidental
}

private fun accidentalText(offset: Int): String = when (offset) {
    -2 -> "♭♭"
    -1 -> "♭"
    0 -> ""
    1 -> "♯"
    2 -> "𝄪"
    else -> if (offset < 0) "♭".repeat(-offset) else "♯".repeat(offset)
}

private fun signedPitchDifference(target: Int, natural: Int): Int {
    var difference = normalizedPitchClass(target - natural)
    if (difference > 6) difference -= 12
    return difference
}

private fun spellHandShapeScaleNote(
    tonicKey: String,
    degreeNumber: Int,
    semitoneInterval: Int
): String {
    val normalizedTonic = normalizeHandShapeTonicKey(tonicKey)
    val tonicPitch = tonicPitchClass(normalizedTonic)
    val (tonicLetter, _) = tonicLetterAndAccidental(normalizedTonic)
    val tonicLetterIndex = diatonicLetterOrder.indexOf(tonicLetter).coerceAtLeast(0)
    val targetLetter = diatonicLetterOrder[(tonicLetterIndex + degreeNumber - 1) % 7]
    val targetPitch = normalizedPitchClass(tonicPitch + semitoneInterval)
    val naturalPitch = naturalPitchClassByLetter.getValue(targetLetter)
    return "$targetLetter${accidentalText(signedPitchDifference(targetPitch, naturalPitch))}"
}

private val guitarOpenMidiByString = mapOf(
    6 to 40,
    5 to 45,
    4 to 50,
    3 to 55,
    2 to 59,
    1 to 64
)


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
    val useMajorFingering = scaleType.usesMajorCagedFingering

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
                    noteName = spellHandShapeScaleNote(
                        tonicKey = tonicName,
                        degreeNumber = scaleType.degreeNumberForInterval(interval),
                        semitoneInterval = interval
                    ),
                    intervalFromRoot = interval,
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

private fun handShapeScaleGroupTitle(groupKey: String, lang: String): String = when (groupKey) {
    "MODES" -> fingerUiText(lang, "7 mode của âm giai trưởng", "Seven modes of the major scale")
    "MINOR_SCALES" -> fingerUiText(lang, "Âm giai thứ nền tảng", "Core minor scales")
    "BASIC_PENTATONIC" -> fingerUiText(lang, "Ngũ cung cơ bản", "Core pentatonic scales")
    "MODAL_PENTATONIC" -> fingerUiText(lang, "Ngũ cung dominant / điệu thức", "Dominant / modal pentatonics")
    "BLUES" -> fingerUiText(lang, "Âm giai Blues", "Blues scales")
    "MELODIC_MINOR_MODES" -> fingerUiText(lang, "Mode của Melodic Minor", "Modes of melodic minor")
    "HARMONIC_MINOR_MODES" -> fingerUiText(lang, "Mode của Harmonic Minor", "Modes of harmonic minor")
    "SYMMETRIC" -> fingerUiText(lang, "Đối xứng và chromatic", "Symmetrical and chromatic")
    "BEBOP" -> fingerUiText(lang, "Âm giai Bebop", "Bebop scales")
    "SPECIAL" -> fingerUiText(lang, "Âm giai đặc biệt", "Special scales")
    "WORLD_PENTATONIC" -> fingerUiText(lang, "Ngũ cung truyền thống – dạng tham chiếu", "Traditional pentatonics – reference forms")
    else -> fingerUiText(lang, "Âm giai", "Scales")
}

private fun handShapeScaleLevelTitle(levelKey: String, lang: String): String = when (levelKey) {
    "ADVANCED" -> fingerUiText(lang, "Nâng cao", "Advanced")
    "TRADITIONAL" -> fingerUiText(lang, "Truyền thống", "Traditional")
    else -> fingerUiText(lang, "Cơ bản", "Core")
}

private fun handShapeScaleGroupsForLevel(levelKey: String): List<String> = when (levelKey) {
    "ADVANCED" -> listOf(
        "MELODIC_MINOR_MODES",
        "HARMONIC_MINOR_MODES",
        "SYMMETRIC",
        "BEBOP",
        "SPECIAL"
    )
    "TRADITIONAL" -> listOf("WORLD_PENTATONIC")
    else -> listOf(
        "MODES",
        "MINOR_SCALES",
        "BASIC_PENTATONIC",
        "MODAL_PENTATONIC",
        "BLUES"
    )
}

private fun handShapeScaleNoteNames(
    tonicName: String,
    scaleType: HandShapeScaleType
): List<String> = scaleType.intervals.mapIndexed { index, interval ->
    spellHandShapeScaleNote(
        tonicKey = tonicName,
        degreeNumber = scaleType.degreeLabels[index].filter(Char::isDigit).toIntOrNull() ?: 1,
        semitoneInterval = interval
    )
}

private fun handShapeScalePreviewMidi(
    tonicName: String,
    scaleType: HandShapeScaleType
): List<Int> {
    val rootMidi = 48 + tonicPitchClass(tonicName)
    val ascending = scaleType.intervals.map { interval -> rootMidi + interval } + (rootMidi + 12)
    return ascending + ascending.dropLast(1).asReversed()
}

private fun dominantSeventhChordMidi(tonicName: String): List<Int> {
    val rootMidi = 48 + tonicPitchClass(tonicName)
    return listOf(rootMidi, rootMidi + 4, rootMidi + 7, rootMidi + 10)
}

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
    val guidedLessons = remember(tunaGreen) { guidedPracticeLessons(tunaGreen) }
    var page by rememberSaveable(instrument, initialPage) { mutableStateOf(initialPage.name) }
    var selectedHandShapeKey by rememberSaveable(instrument) { mutableStateOf(CagedShape.C.key) }
    var selectedGuidedLessonId by rememberSaveable(instrument) { mutableStateOf("") }

    val selectedHandShape = cagedShapes().firstOrNull { it.key == selectedHandShapeKey }
        ?: CagedShape.C
    val selectedGuidedLesson = guidedLessons.firstOrNull { it.id == selectedGuidedLessonId }
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
                onOpenCurriculum = {
                    page = "GUIDED_CURRICULUM"
                },
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

        page == "GUIDED_CURRICULUM" -> {
            GuidedCurriculumScreen(
                instrument = instrument,
                currentLang = currentLang,
                bgColor = bgColor,
                surfaceColor = surfaceColor,
                textColor = textColor,
                tunaGreen = tunaGreen,
                lessons = guidedLessons,
                onLessonSelected = { lesson ->
                    selectedGuidedLessonId = lesson.id
                    page = if (lesson.kind == GuidedLessonKind.FINGER) {
                        "GUIDED_FINGER_PLAYER"
                    } else {
                        "GUIDED_HARMONY_PLAYER"
                    }
                },
                onClose = {
                    page = FingerTrainingLandingPage.ENTRY.name
                }
            )
        }

        page == "GUIDED_FINGER_PLAYER" && selectedGuidedLesson != null -> {
            FingerTrainingPlayerScreen(
                routes = routes,
                initialRouteKey = selectedGuidedLesson.routeKey ?: initialExerciseKey,
                instrument = instrument,
                currentLang = currentLang,
                bgColor = bgColor,
                surfaceColor = surfaceColor,
                textColor = textColor,
                guidedLesson = selectedGuidedLesson,
                onOpenHandShapes = {},
                onClose = { page = "GUIDED_CURRICULUM" }
            )
        }

        page == "GUIDED_HARMONY_PLAYER" && selectedGuidedLesson != null -> {
            HarmonyProgressionPlayerScreen(
                currentLang = currentLang,
                bgColor = bgColor,
                surfaceColor = surfaceColor,
                textColor = textColor,
                tunaGreen = tunaGreen,
                guidedLesson = selectedGuidedLesson,
                chapterLessons = guidedLessons.filter { lesson ->
                    selectedGuidedLesson.chapterKey != null &&
                            lesson.chapterKey == selectedGuidedLesson.chapterKey
                },
                onGuidedLessonSelected = { lesson ->
                    selectedGuidedLessonId = lesson.id
                },
                onClose = { page = "GUIDED_CURRICULUM" }
            )
        }

        page == "HARMONY_PLAYER" -> {
            HarmonyProgressionPlayerScreen(
                currentLang = currentLang,
                bgColor = bgColor,
                surfaceColor = surfaceColor,
                textColor = textColor,
                tunaGreen = tunaGreen,
                guidedLesson = null,
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
                guidedLesson = null,
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
    guidedLesson: GuidedPracticeLesson? = null,
    onOpenHandShapes: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val effectiveInstrument = if (instrument == "Ukulele") "Ukulele" else "Guitar"
    val isVietnamese = currentLang.lowercase(Locale.ROOT).startsWith("vi")

    var selectedRouteKey by rememberSaveable(effectiveInstrument, guidedLesson?.id) {
        mutableStateOf(guidedLesson?.routeKey ?: initialRouteKey)
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

    var directionKey by rememberSaveable(route.key, guidedLesson?.id) {
        mutableStateOf(guidedLesson?.fingerDirectionKey ?: "FORWARD")
    }
    var startFret by rememberSaveable(route.key, guidedLesson?.id) {
        mutableIntStateOf(guidedLesson?.fingerStartFret ?: 1)
    }
    var fretViewKey by rememberSaveable(route.key, guidedLesson?.id) { mutableStateOf("FULL") }
    var bpm by rememberSaveable(route.key, guidedLesson?.id) {
        mutableFloatStateOf(guidedLesson?.bpmStart?.toFloat() ?: route.recommendedBpm)
    }
    var pickingPatternKey by rememberSaveable(route.key, guidedLesson?.id) {
        mutableStateOf(guidedLesson?.pickingPatternKey ?: PracticePickingPattern.ALTERNATE.key)
    }
    var subdivisionKey by rememberSaveable(route.key, guidedLesson?.id) {
        mutableStateOf(guidedLesson?.subdivisionKey ?: PracticeSubdivision.QUARTER.key)
    }
    var speedTrainerEnabled by rememberSaveable(route.key, guidedLesson?.id) {
        mutableStateOf(guidedLesson != null)
    }
    var soundEnabled by rememberSaveable(route.key) { mutableStateOf(true) }
    var repeatEnabled by rememberSaveable(route.key) { mutableStateOf(true) }
    var isPlaying by rememberSaveable(route.key) { mutableStateOf(false) }
    var countdownValue by rememberSaveable(route.key) { mutableIntStateOf(0) }
    var hasPlaybackStarted by rememberSaveable(route.key) { mutableStateOf(false) }
    var currentIndex by rememberSaveable(route.key) { mutableIntStateOf(0) }
    var completedRounds by rememberSaveable(route.key, guidedLesson?.id) { mutableIntStateOf(0) }
    var roundsAtCurrentBpm by rememberSaveable(route.key, guidedLesson?.id) { mutableIntStateOf(0) }
    var lessonCompleted by rememberSaveable(guidedLesson?.id) {
        mutableStateOf(guidedLesson?.let { it.id in GuidedPracticeProgressStore.completedIds(context) } ?: false)
    }

    val pickingPattern = PracticePickingPattern.values().firstOrNull { it.key == pickingPatternKey }
        ?: PracticePickingPattern.ALTERNATE
    val subdivision = PracticeSubdivision.values().firstOrNull { it.key == subdivisionKey }
        ?: PracticeSubdivision.QUARTER
    val targetBpm = guidedLesson?.bpmTarget ?: (route.recommendedBpm + 20f).roundToInt().coerceAtMost(160)

    val sequence = remember(effectiveInstrument, route.key, directionKey, startFret) {
        buildFingerSequence(effectiveInstrument, route.key, directionKey, startFret)
    }

    val currentStep = sequence.getOrNull(currentIndex)
    val previousStep = sequence.getOrNull(currentIndex - 1)
    val currentPickSymbol = fingerPickSymbol(currentIndex, currentStep, previousStep, pickingPattern)
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
        roundsAtCurrentBpm = 0
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

    LaunchedEffect(isPlaying, bpm, repeatEnabled, sequence, subdivision.key) {
        if (!isPlaying || sequence.isEmpty()) return@LaunchedEffect

        while (isPlaying) {
            val intervalMs = (60_000f / bpm.coerceIn(40f, 160f) / subdivision.stepsPerBeat.toFloat())
                .toLong()
                .coerceAtLeast(35L)
            delay(intervalMs)

            if (currentIndex < sequence.lastIndex) {
                currentIndex++
            } else {
                val nextRound = completedRounds + 1
                val nextLevelRound = roundsAtCurrentBpm + 1
                completedRounds = nextRound
                roundsAtCurrentBpm = nextLevelRound
                if (speedTrainerEnabled && nextLevelRound >= 3 && bpm < targetBpm) {
                    bpm = (bpm + 5f).coerceAtMost(targetBpm.toFloat())
                    roundsAtCurrentBpm = 0
                }
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

    LaunchedEffect(roundsAtCurrentBpm, bpm, guidedLesson?.id) {
        val lesson = guidedLesson ?: return@LaunchedEffect
        if (!lessonCompleted && roundsAtCurrentBpm >= lesson.targetRounds && bpm >= lesson.bpmTarget) {
            GuidedPracticeProgressStore.markCompleted(context, lesson.id, bpm.roundToInt())
            lessonCompleted = true
            isPlaying = false
            countdownValue = 0
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
    val pickingOptions = PracticePickingPattern.values().map { it.key to it.title(currentLang) }
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
                            text = guidedLesson?.title(currentLang) ?: route.title(currentLang),
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
                            text = if (guidedLesson != null) {
                                "${fingerUiText(currentLang, "Lộ trình", "Guided")} · ${routePatternLabel(route.key, directionKey)} · ${subdivision.shortLabel}"
                            } else {
                                "${fingerUiText(currentLang, "Luyện ngón", "Finger training")} · ${routePatternLabel(route.key, directionKey)} · ${subdivision.shortLabel}"
                            },
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

                if (guidedLesson != null) {
                    GuidedLessonStrip(
                        lesson = guidedLesson,
                        currentLang = currentLang,
                        completedRounds = roundsAtCurrentBpm,
                        bpm = bpm.roundToInt(),
                        lessonCompleted = lessonCompleted,
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        compact = veryCompactHeight
                    )
                    Spacer(Modifier.height(panelGap))
                } else if (effectiveInstrument == "Guitar") {
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
                                            "Dây ${step.stringNumber} · Ngăn ${step.fret} · Ngón ${step.finger} · $currentPickSymbol"
                                        } else {
                                            "String ${step.stringNumber} · Fret ${step.fret} · Finger ${step.finger} · $currentPickSymbol"
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
                                .weight(if (guidedLesson != null) 1.95f else 2.05f),
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
                                if (guidedLesson == null) {
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
                                }
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
                                    label = fingerUiText(currentLang, "Tay gảy", "Picking"),
                                    selectedKey = pickingPattern.key,
                                    selectedLabel = pickingPattern.title(currentLang),
                                    options = pickingOptions,
                                    accent = playAccent,
                                    surfaceColor = bgColor.copy(alpha = 0.45f),
                                    textColor = textColor,
                                    compact = veryCompactHeight,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    onSelected = {
                                        isPlaying = false
                                        countdownValue = 0
                                        pickingPatternKey = it
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
                                if (guidedLesson == null) {
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
                                            "${midiDisplayName(step.midi)} · $currentPickSymbol · ${fingerUiText(currentLang, "Vòng", "Round")} ${completedRounds + 1}"
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
                                .weight(1.08f),
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
                                HorizontalDivider(color = borderColor)
                                HandShapeSwitchRow(
                                    icon = Icons.Default.Add,
                                    label = fingerUiText(
                                        currentLang,
                                        "Tăng tốc +5/3 vòng → $targetBpm",
                                        "Speed +5/3 rounds → $targetBpm"
                                    ),
                                    checked = speedTrainerEnabled,
                                    accent = playAccent,
                                    textColor = textColor,
                                    compact = veryCompactHeight,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    onCheckedChange = { speedTrainerEnabled = it }
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
                                    onClick = {
                                        bpm = (bpm - 5f).coerceAtLeast(40f)
                                        roundsAtCurrentBpm = 0
                                    },
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
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val values = PracticeSubdivision.values()
                                            val next = (values.indexOf(subdivision) + 1) % values.size
                                            subdivisionKey = values[next].key
                                            roundsAtCurrentBpm = 0
                                            currentIndex = 0
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${bpm.roundToInt()} BPM",
                                        color = textColor,
                                        fontSize = if (veryCompactHeight) 9.5.sp else 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "${subdivision.shortLabel} · ${fingerUiText(currentLang, "chạm để đổi", "tap to change")}",
                                        color = uiAccent,
                                        fontSize = if (veryCompactHeight) 6.7.sp else 7.4.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        bpm = (bpm + 5f).coerceAtMost(160f)
                                        roundsAtCurrentBpm = 0
                                    },
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
    val characteristicAccentPrimary = Color(0xFF20C7A5)
    val characteristicAccentSecondary = Color(0xFFFFA24C)
    val currentAccent = Color(0xFF35A7FF)
    val scaleNoteColor = Color(0xFFD0D4D8)
    val playAccent = Color(0xFFF2B94B)
    val borderColor = textColor.copy(alpha = 0.10f)
    val muted = textColor.copy(alpha = 0.58f)

    val handShapePrefs = remember(context) {
        context.getSharedPreferences(
            "tuner_tools_hand_shape_preferences",
            android.content.Context.MODE_PRIVATE
        )
    }
    val previewScope = rememberCoroutineScope()
    var previewJob by remember { mutableStateOf<Job?>(null) }
    var showScaleInfo by rememberSaveable(initialShape.key) { mutableStateOf(false) }

    var tonicKey by rememberSaveable(initialShape.key) {
        mutableStateOf(
            normalizeHandShapeTonicKey(
                handShapePrefs.getString("tonic_key", "C")
            )
        )
    }
    var scaleTypeKey by rememberSaveable(initialShape.key) {
        mutableStateOf(
            handShapePrefs.getString("scale_type_key", HandShapeScaleType.MAJOR.key)
                ?.takeIf { saved -> handShapeScaleTypes().any { it.key == saved } }
                ?: HandShapeScaleType.MAJOR.key
        )
    }
    var shapeKey by rememberSaveable(initialShape.key) {
        mutableStateOf(
            handShapePrefs.getString("shape_key", initialShape.key)
                ?.takeIf { saved -> cagedShapes().any { it.key == saved } }
                ?: initialShape.key
        )
    }
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
    val selectedTonicLabel = remember(tonicKey) { handShapeTonicLabel(tonicKey) }
    val selectedScaleNoteNames = remember(tonicKey, selectedScaleType) {
        handShapeScaleNoteNames(tonicKey, selectedScaleType)
    }
    val characteristicIntervals = selectedScaleType.characteristicIntervals.take(2)
    val primaryCharacteristicInterval = characteristicIntervals.getOrNull(0)
    val secondaryCharacteristicInterval = characteristicIntervals.getOrNull(1)
    val primaryCharacteristicDegree = primaryCharacteristicInterval
        ?.let(selectedScaleType::degreeLabelForInterval)
        .orEmpty()
    val secondaryCharacteristicDegree = secondaryCharacteristicInterval
        ?.let(selectedScaleType::degreeLabelForInterval)
        .orEmpty()
    val characteristicNoteDescriptions = characteristicIntervals.mapNotNull { interval ->
        val index = selectedScaleType.intervals.indexOf(interval)
        val degree = selectedScaleType.degreeLabelForInterval(interval)
        val noteName = selectedScaleNoteNames.getOrNull(index)
        if (degree.isBlank() || noteName.isNullOrBlank()) null else "$noteName ($degree)"
    }
    val hasCharacteristicTones = characteristicIntervals.isNotEmpty()
    val supportsDominantContext = selectedScaleType.supportsDominantContext
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

    fun stopPreview() {
        previewJob?.cancel()
        previewJob = null
    }

    fun playScalePreview(withDominantContext: Boolean) {
        stopPreview()
        isPlaying = false
        countdownValue = 0
        hasPlaybackStarted = false
        previewJob = previewScope.launch {
            if (withDominantContext && supportsDominantContext) {
                runCatching {
                    CoreAudioPlayer.playChord(
                        dominantSeventhChordMidi(tonicKey),
                        effectiveInstrument
                    )
                }
                delay(850L)
            }

            handShapeScalePreviewMidi(tonicKey, selectedScaleType).forEach { midi ->
                runCatching {
                    CoreAudioPlayer.playChord(listOf(midi), effectiveInstrument)
                }
                delay(330L)
            }

            if (withDominantContext && supportsDominantContext) {
                delay(180L)
                runCatching {
                    CoreAudioPlayer.playChord(
                        dominantSeventhChordMidi(tonicKey),
                        effectiveInstrument
                    )
                }
            }
        }
    }

    fun resetSession() {
        stopPreview()
        isPlaying = false
        countdownValue = 0
        hasPlaybackStarted = false
        currentIndex = 0
        completedRounds = 0
    }

    fun stopAndClose() {
        stopPreview()
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
            previewJob?.cancel()
            if (activity != null && !activity.isChangingConfigurations && previousOrientation != null) {
                activity.requestedOrientation = previousOrientation
            }
        }
    }

    LaunchedEffect(tonicKey, scaleTypeKey, shapeKey) {
        handShapePrefs.edit()
            .putString("tonic_key", tonicKey)
            .putString("scale_type_key", scaleTypeKey)
            .putString("shape_key", shapeKey)
            .apply()
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

    val tonicOptions = handShapeTonicOptions.map { it.key to it.label }
    val scaleOptions = handShapeScaleTypes()
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
                            resetSession()
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
                                    text = "$selectedTonicLabel ${selectedScaleType.title(currentLang)}",
                                    color = textColor,
                                    fontSize = if (veryCompactHeight) 9.5.sp else 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { showScaleInfo = true },
                                    modifier = Modifier.size(if (veryCompactHeight) 20.dp else 23.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = fingerUiText(
                                            currentLang,
                                            "Thông tin âm giai",
                                            "Scale information"
                                        ),
                                        tint = uiAccent,
                                        modifier = Modifier.size(if (veryCompactHeight) 14.dp else 16.dp)
                                    )
                                }
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

                            Text(
                                text = "${selectedScaleType.formula}  •  ${selectedScaleNoteNames.joinToString(" – ")}",
                                color = when {
                                    secondaryCharacteristicInterval != null -> characteristicAccentSecondary
                                    primaryCharacteristicInterval != null -> characteristicAccentPrimary
                                    else -> muted
                                },
                                fontSize = if (veryCompactHeight) 7.sp else 8.2.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

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
                                characteristicAccentPrimary = characteristicAccentPrimary,
                                characteristicAccentSecondary = characteristicAccentSecondary,
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

                            Spacer(Modifier.height(3.dp))

                            HandShapeQuickSelector(
                                selectedShape = selectedShape,
                                shapes = cagedShapes(),
                                accent = uiAccent,
                                surfaceColor = bgColor.copy(alpha = 0.42f),
                                textColor = textColor,
                                compact = veryCompactHeight,
                                onSelected = { selected ->
                                    resetSession()
                                    shapeKey = selected.key
                                }
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
                                    selectedLabel = selectedTonicLabel,
                                    options = tonicOptions,
                                    accent = uiAccent,
                                    surfaceColor = bgColor.copy(alpha = 0.45f),
                                    textColor = textColor,
                                    compact = veryCompactHeight,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    onSelected = {
                                        resetSession()
                                        tonicKey = it
                                    }
                                )
                                HandShapeScaleDropdown(
                                    label = fingerUiText(currentLang, "Âm giai", "Scale"),
                                    selectedScale = selectedScaleType,
                                    options = scaleOptions,
                                    currentLang = currentLang,
                                    accent = uiAccent,
                                    surfaceColor = bgColor.copy(alpha = 0.45f),
                                    textColor = textColor,
                                    compact = veryCompactHeight,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    onSelected = {
                                        resetSession()
                                        scaleTypeKey = it.key
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
                                        resetSession()
                                        shapeKey = it
                                    }
                                )
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(if (hasCharacteristicTones) 0.96f else 0.72f),
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
                                        color = if (primaryCharacteristicInterval != null) {
                                            characteristicAccentPrimary
                                        } else {
                                            scaleNoteColor
                                        },
                                        label = if (primaryCharacteristicInterval != null) {
                                            fingerUiText(
                                                currentLang,
                                                "Bậc $primaryCharacteristicDegree",
                                                "Degree $primaryCharacteristicDegree"
                                            )
                                        } else {
                                            fingerUiText(currentLang, "Âm giai", "Scale")
                                        },
                                        textColor = textColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (hasCharacteristicTones) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (secondaryCharacteristicInterval != null) {
                                            HandShapeLegendEntry(
                                                color = characteristicAccentSecondary,
                                                label = fingerUiText(
                                                    currentLang,
                                                    "Bậc $secondaryCharacteristicDegree",
                                                    "Degree $secondaryCharacteristicDegree"
                                                ),
                                                textColor = textColor,
                                                modifier = Modifier.weight(1f)
                                            )
                                        } else {
                                            HandShapeLegendEntry(
                                                color = scaleNoteColor,
                                                label = fingerUiText(currentLang, "Nốt còn lại", "Other tones"),
                                                textColor = textColor,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (secondaryCharacteristicInterval != null) {
                                            HandShapeLegendEntry(
                                                color = scaleNoteColor,
                                                label = fingerUiText(currentLang, "Nốt còn lại", "Other tones"),
                                                textColor = textColor,
                                                modifier = Modifier.weight(1f)
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
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
                                        resetSession()
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
                                stopPreview()
                                isPlaying = false
                                countdownValue = 0
                                currentIndex = (currentIndex - 1).coerceAtLeast(0)
                            }
                        )

                        Button(
                            onClick = {
                                stopPreview()
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
                                stopPreview()
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

    if (showScaleInfo) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showScaleInfo = false },
            containerColor = surfaceColor,
            titleContentColor = textColor,
            textContentColor = textColor,
            title = {
                Text(
                    text = "$selectedTonicLabel ${selectedScaleType.title(currentLang)}",
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = bgColor.copy(alpha = 0.58f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            uiAccent.copy(alpha = 0.30f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = selectedScaleType.formula,
                                color = when {
                                    secondaryCharacteristicInterval != null -> characteristicAccentSecondary
                                    primaryCharacteristicInterval != null -> characteristicAccentPrimary
                                    else -> uiAccent
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = selectedScaleNoteNames.joinToString(" – "),
                                color = textColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (characteristicNoteDescriptions.isNotEmpty()) {
                                Text(
                                    text = fingerUiText(
                                        currentLang,
                                        "Nốt đặc trưng: ${characteristicNoteDescriptions.joinToString(" · ")}",
                                        "Characteristic tones: ${characteristicNoteDescriptions.joinToString(" · ")}"
                                    ),
                                    color = textColor.copy(alpha = 0.68f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Text(
                        text = selectedScaleType.summary(currentLang),
                        color = textColor,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = selectedScaleType.usage(currentLang),
                        color = textColor.copy(alpha = 0.72f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    if (hasCharacteristicTones) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HandShapeLegendEntry(
                                color = characteristicAccentPrimary,
                                label = fingerUiText(
                                    currentLang,
                                    "Bậc $primaryCharacteristicDegree đặc trưng",
                                    "Characteristic $primaryCharacteristicDegree"
                                ),
                                textColor = textColor,
                                modifier = Modifier.weight(1f)
                            )
                            if (secondaryCharacteristicInterval != null) {
                                HandShapeLegendEntry(
                                    color = characteristicAccentSecondary,
                                    label = fingerUiText(
                                        currentLang,
                                        "Bậc $secondaryCharacteristicDegree đặc trưng",
                                        "Characteristic $secondaryCharacteristicDegree"
                                    ),
                                    textColor = textColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            showScaleInfo = false
                            playScalePreview(withDominantContext = false)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = uiAccent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = fingerUiText(currentLang, "Nghe âm giai", "Hear the scale"),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    if (supportsDominantContext) {
                        Button(
                            onClick = {
                                showScaleInfo = false
                                playScalePreview(withDominantContext = true)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = characteristicAccentSecondary,
                                contentColor = Color(0xFF1C1D1F)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                text = fingerUiText(
                                    currentLang,
                                    "Nghe trên hợp âm ${selectedTonicLabel}7",
                                    "Hear it over ${selectedTonicLabel}7"
                                ),
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showScaleInfo = false }
                ) {
                    Text(
                        text = fingerUiText(currentLang, "Đóng", "Close"),
                        color = uiAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
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
    guidedLesson: GuidedPracticeLesson? = null,
    chapterLessons: List<GuidedPracticeLesson> = emptyList(),
    onGuidedLessonSelected: (GuidedPracticeLesson) -> Unit = {},
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val isVietnamese = currentLang.lowercase(Locale.ROOT).startsWith("vi")
    val progressions = remember { harmonyProgressions() }
    val activeChapter = guidedPracticeChapter(guidedLesson?.chapterKey)
    val activeChapterLessons = if (activeChapter != null) {
        chapterLessons.filter { it.chapterKey == activeChapter.key }
    } else {
        emptyList()
    }

    var practiceTypeKey by rememberSaveable(guidedLesson?.id) {
        mutableStateOf(guidedLesson?.practiceTypeKey ?: HarmonyPracticeType.TRIAD.key)
    }
    var keyName by rememberSaveable(guidedLesson?.id) { mutableStateOf(guidedLesson?.keyName ?: "C") }
    var progressionKey by rememberSaveable(guidedLesson?.id) {
        mutableStateOf(guidedLesson?.progressionKey ?: "II_V_I_MAJOR")
    }
    var stringSetKey by rememberSaveable(guidedLesson?.id) {
        mutableStateOf(guidedLesson?.stringSetKey ?: "STRINGS_123")
    }
    var inversionKey by rememberSaveable(guidedLesson?.id) {
        mutableStateOf(guidedLesson?.inversionKey ?: HarmonyInversionMode.AUTO.key)
    }
    var directionKey by rememberSaveable(guidedLesson?.id) {
        mutableStateOf(guidedLesson?.harmonyDirectionKey ?: HarmonyRunDirection.ASCENDING.key)
    }
    var positionStart by rememberSaveable(guidedLesson?.id) {
        mutableIntStateOf(guidedLesson?.positionStart ?: 5)
    }
    var bpm by rememberSaveable(guidedLesson?.id) {
        mutableFloatStateOf(guidedLesson?.bpmStart?.toFloat() ?: 60f)
    }
    var subdivisionKey by rememberSaveable(guidedLesson?.id) {
        mutableStateOf(guidedLesson?.subdivisionKey ?: PracticeSubdivision.EIGHTH.key)
    }
    var beatsPerChord by rememberSaveable(guidedLesson?.id) {
        mutableIntStateOf(guidedLesson?.beatsPerChord ?: 4)
    }
    var pickingPatternKey by rememberSaveable(guidedLesson?.id) {
        mutableStateOf(guidedLesson?.pickingPatternKey ?: PracticePickingPattern.ALTERNATE.key)
    }
    var speedTrainerEnabled by rememberSaveable(guidedLesson?.id) { mutableStateOf(guidedLesson != null) }
    var soundEnabled by rememberSaveable { mutableStateOf(true) }
    var repeatEnabled by rememberSaveable { mutableStateOf(true) }
    var lookAheadEnabled by rememberSaveable { mutableStateOf(true) }
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var countdownValue by rememberSaveable { mutableIntStateOf(0) }
    var hasPlaybackStarted by rememberSaveable { mutableStateOf(false) }
    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    var completedRounds by rememberSaveable(guidedLesson?.id) { mutableIntStateOf(0) }
    var roundsAtCurrentBpm by rememberSaveable(guidedLesson?.id) { mutableIntStateOf(0) }
    var lessonCompleted by rememberSaveable(guidedLesson?.id) {
        mutableStateOf(guidedLesson?.let { it.id in GuidedPracticeProgressStore.completedIds(context) } ?: false)
    }

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
    val subdivision = PracticeSubdivision.values().firstOrNull { it.key == subdivisionKey }
        ?: PracticeSubdivision.EIGHTH
    val pickingPattern = PracticePickingPattern.values().firstOrNull { it.key == pickingPatternKey }
        ?: PracticePickingPattern.ALTERNATE
    val targetBpm = guidedLesson?.bpmTarget ?: 100

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
    val sequence = remember(
        voicings, direction.key, subdivision.key, beatsPerChord, pickingPattern.key
    ) {
        buildHarmonyPlaybackSequence(
            voicings = voicings,
            direction = direction,
            subdivision = subdivision,
            beatsPerChord = beatsPerChord,
            pickingPattern = pickingPattern
        )
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
    val focusedVoicingFrets = if (inversionMode == HarmonyInversionMode.ALL_TRIAD_INVERSIONS) {
        buildList {
            currentVoicing?.notes?.forEach { add(it.fret) }
            nextVoicing?.notes?.forEach { add(it.fret) }
        }
    } else {
        allVoicingFrets
    }
    val focusMin = focusedVoicingFrets.minOrNull() ?: positionStart
    val focusMax = focusedVoicingFrets.maxOrNull() ?: (positionStart + 4)
    val visibleStartFret = if (inversionMode == HarmonyInversionMode.ALL_TRIAD_INVERSIONS) {
        maxOf(0, focusMin - 1)
    } else {
        minOf(positionStart, focusMin).coerceAtLeast(0)
    }
    val visibleEndFret = if (inversionMode == HarmonyInversionMode.ALL_TRIAD_INVERSIONS) {
        minOf(20, maxOf(focusMax + 1, visibleStartFret + 4))
    } else {
        maxOf(positionStart + 4, focusMax).coerceAtMost(20)
    }
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
        roundsAtCurrentBpm = 0
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

    LaunchedEffect(isPlaying, bpm, repeatEnabled, sequence, speedTrainerEnabled) {
        if (!isPlaying || sequence.isEmpty()) return@LaunchedEffect
        while (isPlaying) {
            val activeStep = sequence.getOrNull(currentIndex)
            val stepsPerBeat = activeStep?.stepsPerBeat?.coerceAtLeast(1) ?: subdivision.stepsPerBeat
            val intervalMs = (60_000f / bpm.coerceIn(40f, 180f) / stepsPerBeat.toFloat())
                .toLong()
                .coerceAtLeast(35L)
            delay(intervalMs)
            if (currentIndex < sequence.lastIndex) {
                currentIndex++
            } else {
                val nextRound = completedRounds + 1
                val nextLevelRound = roundsAtCurrentBpm + 1
                completedRounds = nextRound
                roundsAtCurrentBpm = nextLevelRound
                if (speedTrainerEnabled && nextLevelRound >= 3 && bpm < targetBpm) {
                    bpm = (bpm + 5f).coerceAtMost(targetBpm.toFloat())
                    roundsAtCurrentBpm = 0
                }
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

    LaunchedEffect(roundsAtCurrentBpm, bpm, guidedLesson?.id) {
        val lesson = guidedLesson ?: return@LaunchedEffect
        if (!lessonCompleted && roundsAtCurrentBpm >= lesson.targetRounds && bpm >= lesson.bpmTarget) {
            GuidedPracticeProgressStore.markCompleted(context, lesson.id, bpm.roundToInt())
            lessonCompleted = true
            isPlaying = false
            countdownValue = 0
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
    // A fixed summary height clipped the chord flow and the current-note title on
    // phones with larger text/font scales. Treat this as a minimum only; let the
    // contents determine the actual measured height.
    val summaryMinHeight = if (veryCompactHeight) 68.dp else 80.dp
    val bottomControlHeight = if (veryCompactHeight) 49.dp else 56.dp
    val panelGap = if (veryCompactHeight) 3.dp else 5.dp
    val panelShape = RoundedCornerShape(if (compactWidth) 13.dp else 16.dp)

    val practiceTypeOptions = HarmonyPracticeType.values().map { it.key to it.title(currentLang) }
    val keyOptions = harmonyNoteNames.map { it to it }
    val progressionOptions = progressions.map { it.key to it.title(currentLang) }
    val stringOptions = stringSetOptions.map { it.key to it.title(currentLang) }
    val inversionDropdownOptions = inversionOptions.map { it.key to it.title(currentLang) }
    val directionOptions = HarmonyRunDirection.values().map { it.key to it.title(currentLang) }
    val subdivisionOptions = PracticeSubdivision.values().map { it.key to it.title(currentLang) }
    val pickingOptions = PracticePickingPattern.values().map { it.key to it.title(currentLang) }
    val beatOptions = listOf(1, 2, 4, 8).map { beats ->
        beats.toString() to fingerUiText(currentLang, "$beats phách / hợp âm", "$beats beats / chord")
    }
    val positionOptions = listOf(1, 3, 5, 7, 9).map { start ->
        start.toString() to fingerUiText(
            currentLang,
            "Ngăn $start-${start + 4}",
            "Frets $start-${start + 4}"
        )
    }
    val chordFlow = chords.joinToString("  →  ") { chord -> "${chord.roman} ${chord.symbol}" }
    val inversionText = currentVoicing?.let { voicing ->
        if (voicing.inversionIndex == 0) {
            fingerUiText(currentLang, "Thế gốc", "Root")
        } else {
            fingerUiText(currentLang, "Đảo ${voicing.inversionIndex}", "Inv. ${voicing.inversionIndex}")
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
                            text = guidedLesson?.title(currentLang)
                                ?: fingerUiText(currentLang, "Triad & Arpeggio", "Triads & Arpeggios"),
                            color = textColor,
                            fontSize = if (veryCompactHeight) 16.sp else 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (guidedLesson != null) {
                                "${fingerUiText(currentLang, "Lộ trình", "Guided")} · $keyName · ${progression.title(currentLang)}"
                            } else {
                                "$keyName ${progression.tonality.title(currentLang)} · ${progression.title(currentLang)}"
                            },
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

                if (guidedLesson != null && activeChapter != null && activeChapterLessons.size > 1) {
                    GuidedChapterPartSelector(
                        chapter = activeChapter,
                        currentLesson = guidedLesson,
                        lessons = activeChapterLessons,
                        currentLang = currentLang,
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        compact = veryCompactHeight,
                        onLessonSelected = { lesson ->
                            if (lesson.id != guidedLesson.id) {
                                resetSession()
                                onGuidedLessonSelected(lesson)
                            }
                        }
                    )
                }

                if (guidedLesson != null) {
                    GuidedLessonStrip(
                        lesson = guidedLesson,
                        currentLang = currentLang,
                        completedRounds = roundsAtCurrentBpm,
                        bpm = bpm.roundToInt(),
                        lessonCompleted = lessonCompleted,
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        compact = veryCompactHeight
                    )
                }

                // Let the summary wrap to its actual content height instead of
                // cutting the live pitch/degree and the horizontal chord flow.
                // The fretboard and settings share the remaining weighted area.
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = summaryMinHeight),
                    shape = panelShape,
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 9.dp, vertical = 7.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                    lineHeight = if (veryCompactHeight) 18.sp else 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = currentStep?.let { step ->
                                        "${midiDisplayName(step.note.midi)} · ${step.note.roleLabel} · ${step.pickSymbol} · ${fingerUiText(currentLang, "Phách", "Beat")} ${step.beatNumber} · $inversionText"
                                    } ?: progression.description(currentLang),
                                    color = currentAccent,
                                    fontSize = if (veryCompactHeight) 9.sp else 10.5.sp,
                                    lineHeight = if (veryCompactHeight) 13.sp else 15.sp,
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
                        // Seven-chord circles are longer than the summary width. Let users
                        // scroll the full progression instead of cutting the final chords.
                        Text(
                            text = chordFlow,
                            color = muted,
                            fontSize = if (veryCompactHeight) 9.sp else 10.sp,
                            lineHeight = if (veryCompactHeight) 13.sp else 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        )
                        // A weighted spacer inside a hard-height card previously
                        // made this line fall outside the visible card on some phones.
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
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // For 13th there are seven color-coded degrees; scroll this
                                // small legend rather than shrinking the fretboard itself.
                                Row(
                                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HarmonyLegendItem("R", rootAccent, textColor)
                                    HarmonyLegendItem("3", thirdAccent, textColor)
                                    HarmonyLegendItem("5", chordToneAccent, textColor)
                                    if (practiceType.noteCount >= 4) HarmonyLegendItem("7", seventhAccent, textColor)
                                    if (practiceType.noteCount >= 5) HarmonyLegendItem("9", Color(0xFF48C9D8), textColor)
                                    if (practiceType.noteCount >= 6) HarmonyLegendItem("11", Color(0xFFA78BFA), textColor)
                                    if (practiceType.noteCount >= 7) HarmonyLegendItem("13", Color(0xFFF08FB2), textColor)
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

                            if (guidedLesson == null) {
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
                            } else {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = selectorHeight),
                                    shape = RoundedCornerShape(11.dp),
                                    colors = CardDefaults.cardColors(containerColor = guidedLesson.accent.copy(alpha = 0.09f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, guidedLesson.accent.copy(alpha = 0.28f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = fingerUiText(currentLang, "CẤU HÌNH BÀI HỌC", "LESSON SETUP"),
                                            color = guidedLesson.accent,
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = "$keyName · ${progression.title(currentLang)} · ${practiceType.title(currentLang)}",
                                            color = textColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${stringSet.title(currentLang)} · ${inversionMode.title(currentLang)} · ${fingerUiText(currentLang, "Ngăn", "Frets")} $positionStart-${positionStart + 4}",
                                            color = textColor.copy(alpha = 0.56f),
                                            fontSize = 7.8.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
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
                                label = fingerUiText(currentLang, "Giá trị nốt", "Subdivision"),
                                selectedKey = subdivision.key,
                                selectedLabel = subdivision.title(currentLang),
                                options = subdivisionOptions,
                                accent = currentAccent,
                                surfaceColor = bgColor.copy(alpha = 0.50f),
                                textColor = textColor,
                                modifier = Modifier.fillMaxWidth().height(selectorHeight),
                                onSelected = { subdivisionKey = it }
                            )
                            CompactDropdownSelector(
                                label = fingerUiText(currentLang, "Thời lượng", "Chord duration"),
                                selectedKey = beatsPerChord.toString(),
                                selectedLabel = beatOptions.firstOrNull { it.first == beatsPerChord.toString() }?.second
                                    ?: "$beatsPerChord",
                                options = beatOptions,
                                accent = rootAccent,
                                surfaceColor = bgColor.copy(alpha = 0.50f),
                                textColor = textColor,
                                modifier = Modifier.fillMaxWidth().height(selectorHeight),
                                onSelected = { beatsPerChord = it.toIntOrNull()?.coerceIn(1, 8) ?: 4 }
                            )
                            CompactDropdownSelector(
                                label = fingerUiText(currentLang, "Tay gảy", "Picking"),
                                selectedKey = pickingPattern.key,
                                selectedLabel = pickingPattern.title(currentLang),
                                options = pickingOptions,
                                accent = thirdAccent,
                                surfaceColor = bgColor.copy(alpha = 0.50f),
                                textColor = textColor,
                                modifier = Modifier.fillMaxWidth().height(selectorHeight),
                                onSelected = { pickingPatternKey = it }
                            )
                            if (guidedLesson == null) {
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
                            }

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
                                        onClick = {
                                            bpm = (bpm - 5f).coerceAtLeast(40f)
                                            roundsAtCurrentBpm = 0
                                        },
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
                                        onClick = {
                                            bpm = (bpm + 5f).coerceAtMost(180f)
                                            roundsAtCurrentBpm = 0
                                        },
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
                            HarmonySideToggleRow(
                                icon = Icons.Default.Add,
                                label = fingerUiText(
                                    currentLang,
                                    "Tăng tốc +5/3 vòng → $targetBpm",
                                    "Speed +5/3 rounds → $targetBpm"
                                ),
                                selected = speedTrainerEnabled,
                                accent = currentAccent,
                                textColor = textColor,
                                backgroundColor = bgColor.copy(alpha = 0.50f),
                                onClick = { speedTrainerEnabled = !speedTrainerEnabled }
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
            note.interval in 9..11 -> seventhAccent
            note.interval in 12..15 -> Color(0xFF48C9D8) // ninth, flat ninth or sharp ninth
            note.interval in 16..19 -> Color(0xFFA78BFA) // eleventh / sharp eleventh
            note.interval >= 20 -> Color(0xFFF08FB2)    // thirteenth / flat thirteenth
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
private fun HandShapeScaleDropdown(
    label: String,
    selectedScale: HandShapeScaleType,
    options: List<HandShapeScaleType>,
    currentLang: String,
    accent: Color,
    surfaceColor: Color,
    textColor: Color,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onSelected: (HandShapeScaleType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var levelKey by remember(selectedScale.key) { mutableStateOf(selectedScale.levelKey) }
    val shape = RoundedCornerShape(if (compact) 8.dp else 10.dp)
    val levelOrder = listOf("BASIC", "ADVANCED", "TRADITIONAL")
    val menuWidth = minOf(LocalConfiguration.current.screenWidthDp - 24, 340).dp

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(surfaceColor)
                .border(1.dp, textColor.copy(alpha = 0.10f), shape)
                .clickable {
                    levelKey = selectedScale.levelKey
                    expanded = true
                }
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
                    text = selectedScale.title(currentLang),
                    color = textColor,
                    fontSize = if (compact) 8.6.sp else 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = selectedScale.formula,
                    color = accent.copy(alpha = 0.88f),
                    fontSize = if (compact) 6.3.sp else 7.2.sp,
                    fontWeight = FontWeight.SemiBold,
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
            modifier = Modifier
                .width(menuWidth)
                .background(surfaceColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                levelOrder.forEach { optionLevel ->
                    val selectedLevel = optionLevel == levelKey
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable { levelKey = optionLevel },
                        shape = RoundedCornerShape(9.dp),
                        color = if (selectedLevel) accent.copy(alpha = 0.20f) else surfaceColor,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedLevel) accent else textColor.copy(alpha = 0.12f)
                        )
                    ) {
                        Text(
                            text = handShapeScaleLevelTitle(optionLevel, currentLang),
                            color = if (selectedLevel) accent else textColor.copy(alpha = 0.72f),
                            fontSize = 9.5.sp,
                            fontWeight = if (selectedLevel) FontWeight.ExtraBold else FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            handShapeScaleGroupsForLevel(levelKey).forEach { groupKey ->
                val groupItems = options.filter {
                    it.levelKey == levelKey && it.groupKey == groupKey
                }
                if (groupItems.isNotEmpty()) {
                    HorizontalDivider(
                        color = textColor.copy(alpha = 0.07f),
                        modifier = Modifier.padding(top = 1.dp)
                    )
                    Text(
                        text = handShapeScaleGroupTitle(groupKey, currentLang),
                        color = accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(
                            start = 12.dp,
                            end = 12.dp,
                            top = 8.dp,
                            bottom = 3.dp
                        )
                    )
                    groupItems.forEach { option ->
                        val selected = option == selectedScale
                        androidx.compose.material3.DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = option.title(currentLang),
                                        color = if (selected) accent else textColor,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = option.formula,
                                        color = if (selected) {
                                            accent.copy(alpha = 0.78f)
                                        } else {
                                            textColor.copy(alpha = 0.54f)
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            onClick = {
                                expanded = false
                                onSelected(option)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HandShapeQuickSelector(
    selectedShape: CagedShape,
    shapes: List<CagedShape>,
    accent: Color,
    surfaceColor: Color,
    textColor: Color,
    compact: Boolean,
    onSelected: (CagedShape) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 25.dp else 29.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 3.dp)
    ) {
        shapes.forEach { shape ->
            val selected = shape == selectedShape
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(if (compact) 7.dp else 9.dp))
                    .background(
                        if (selected) accent.copy(alpha = 0.18f) else surfaceColor
                    )
                    .border(
                        width = 1.dp,
                        color = if (selected) accent.copy(alpha = 0.72f) else textColor.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(if (compact) 7.dp else 9.dp)
                    )
                    .clickable { onSelected(shape) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = shape.key,
                    color = if (selected) accent else textColor.copy(alpha = 0.72f),
                    fontSize = if (compact) 8.sp else 9.5.sp,
                    fontWeight = if (selected) FontWeight.Black else FontWeight.Bold
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
    characteristicAccentPrimary: Color,
    characteristicAccentSecondary: Color,
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

                val characteristicIndex = pattern.scaleType.characteristicIntervals
                    .take(2)
                    .indexOf(note.intervalFromRoot)
                val baseColor = when {
                    note.isRoot -> rootAccent
                    characteristicIndex == 0 -> characteristicAccentPrimary
                    characteristicIndex == 1 -> characteristicAccentSecondary
                    else -> scaleNoteColor
                }
                val isHighlightedScaleRole = baseColor != scaleNoteColor

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
                        if (isHighlightedScaleRole) {
                            drawCircle(
                                color = baseColor,
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
                        if (isHighlightedScaleRole) {
                            drawCircle(
                                color = baseColor.copy(alpha = 0.90f),
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
                        isCurrent || isHighlightedScaleRole || isCompleted -> android.graphics.Color.WHITE
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

@Composable
private fun GuidedCurriculumScreen(
    instrument: String,
    currentLang: String,
    bgColor: Color,
    surfaceColor: Color,
    textColor: Color,
    tunaGreen: Color,
    lessons: List<GuidedPracticeLesson>,
    onLessonSelected: (GuidedPracticeLesson) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val isVietnamese = currentLang.lowercase(Locale.ROOT).startsWith("vi")
    var completedIds by remember { mutableStateOf(GuidedPracticeProgressStore.completedIds(context)) }

    LaunchedEffect(lessons) {
        completedIds = GuidedPracticeProgressStore.completedIds(context)
    }

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

    val enabledLessons = lessons.filter { lesson ->
        when (lesson.kind) {
            GuidedLessonKind.FINGER -> instrument != "Piano"
            GuidedLessonKind.HARMONY -> instrument == "Guitar"
        }
    }
    val totalUnitCount = guidedCurriculumUnitCount(enabledLessons)
    val completedCount = guidedCurriculumCompletedCount(enabledLessons, completedIds)
    val progress = if (totalUnitCount <= 0) 0f else completedCount.toFloat() / totalUnitCount.toFloat()
    val harmonyAccent = Color(0xFFE85D86)
    val borderColor = textColor.copy(alpha = 0.10f)

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = bgColor) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp)
            ) {
                FingerSectionTopBar(
                    title = if (isVietnamese) "Lộ trình học có hướng dẫn" else "Guided learning path",
                    subtitle = if (isVietnamese) "Nghe · Hiểu · Chơi · Áp dụng" else "Listen · Understand · Play · Apply",
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    onBack = onClose
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isVietnamese) "Tiến độ của bạn" else "Your progress",
                                    color = textColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = if (isVietnamese) {
                                        "$completedCount/$totalUnitCount bài hoàn thành · ${enabledLessons.size} phần luyện · BPM tốt nhất được lưu theo từng phần"
                                    } else {
                                        "$completedCount/$totalUnitCount lessons completed · ${enabledLessons.size} practice parts · best BPM saved per part"
                                    },
                                    color = textColor.copy(alpha = 0.58f),
                                    fontSize = 9.5.sp,
                                    maxLines = 2
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = tunaGreen.copy(alpha = 0.13f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, tunaGreen.copy(alpha = 0.30f))
                            ) {
                                Text(
                                    text = "${(progress * 100f).roundToInt()}%",
                                    color = tunaGreen,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(7.dp))
                        LinearProgressIndicator(
                            progress = progress.coerceIn(0f, 1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(50)),
                            color = tunaGreen,
                            trackColor = textColor.copy(alpha = 0.08f)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    lessons.forEachIndexed { index, lesson ->
                        val chapter = guidedPracticeChapter(lesson.chapterKey)
                        val previousChapterKey = lessons.getOrNull(index - 1)?.chapterKey

                        if (chapter != null) {
                            // Ngoài lộ trình chỉ hiển thị một thẻ Chương 13.
                            // Danh sách 13A/13B/13C được chuyển vào bên trong màn luyện.
                            if (chapter.key != previousChapterKey) {
                                val chapterLessons = lessons.filter { it.chapterKey == chapter.key }
                                item(key = "chapter_${chapter.key}") {
                                    GuidedCurriculumChapterCard(
                                        chapter = chapter,
                                        lessons = chapterLessons,
                                        currentLang = currentLang,
                                        completedIds = completedIds,
                                        surfaceColor = surfaceColor,
                                        textColor = textColor,
                                        enabled = instrument == "Guitar",
                                        onLessonSelected = onLessonSelected
                                    )
                                }
                            }
                        } else {
                            item(key = lesson.id) {
                                val enabled = when (lesson.kind) {
                                    GuidedLessonKind.FINGER -> instrument != "Piano"
                                    GuidedLessonKind.HARMONY -> instrument == "Guitar"
                                }
                                GuidedCurriculumLessonCard(
                                    lesson = lesson,
                                    currentLang = currentLang,
                                    surfaceColor = surfaceColor,
                                    textColor = textColor,
                                    accent = if (lesson.kind == GuidedLessonKind.HARMONY) {
                                        harmonyAccent
                                    } else {
                                        lesson.accent
                                    },
                                    completed = lesson.id in completedIds,
                                    bestBpm = GuidedPracticeProgressStore.bestBpm(context, lesson.id),
                                    enabled = enabled,
                                    onClick = { onLessonSelected(lesson) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuidedCurriculumChapterCard(
    chapter: GuidedPracticeChapter,
    lessons: List<GuidedPracticeLesson>,
    currentLang: String,
    completedIds: Set<String>,
    surfaceColor: Color,
    textColor: Color,
    enabled: Boolean,
    onLessonSelected: (GuidedPracticeLesson) -> Unit
) {
    if (lessons.isEmpty()) return

    val isVietnamese = currentLang.lowercase(Locale.ROOT).startsWith("vi")
    val completedParts = lessons.count { it.id in completedIds }
    val allCompleted = completedParts == lessons.size
    val progress = completedParts.toFloat() / lessons.size.toFloat()
    val nextLesson = lessons.firstOrNull { it.id !in completedIds } ?: lessons.first()
    val shape = RoundedCornerShape(16.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onLessonSelected(nextLesson) },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            chapter.accent.copy(alpha = if (allCompleted) 0.65f else 0.40f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            chapter.accent.copy(alpha = 0.15f),
                            chapter.secondaryAccent.copy(alpha = 0.065f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 11.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = chapter.accent.copy(alpha = 0.18f),
                modifier = Modifier.size(45.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (allCompleted) "✓" else chapter.number,
                        color = chapter.accent,
                        fontSize = if (allCompleted) 18.sp else 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.title(currentLang),
                    color = if (enabled) textColor else textColor.copy(alpha = 0.38f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = chapter.description(currentLang),
                    color = textColor.copy(alpha = if (enabled) 0.56f else 0.28f),
                    fontSize = 8.8.sp,
                    lineHeight = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(50)),
                    color = chapter.accent,
                    trackColor = textColor.copy(alpha = 0.08f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (allCompleted) {
                        if (isVietnamese) "Đã hoàn thành 3/3 · Nhấn để luyện lại" else "Completed 3/3 · Tap to practise again"
                    } else {
                        if (isVietnamese) {
                            "Tiếp tục ${nextLesson.number} · $completedParts/${lessons.size} phần hoàn thành"
                        } else {
                            "Continue ${nextLesson.number} · $completedParts/${lessons.size} parts completed"
                        }
                    },
                    color = chapter.accent.copy(alpha = if (enabled) 0.92f else 0.32f),
                    fontSize = 8.2.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = chapter.secondaryAccent.copy(alpha = 0.13f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        chapter.secondaryAccent.copy(alpha = 0.30f)
                    )
                ) {
                    Text(
                        text = "$completedParts/${lessons.size}",
                        color = chapter.secondaryAccent,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(5.dp))
                Icon(
                    imageVector = if (enabled) Icons.Default.ArrowForward else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (enabled) chapter.accent else textColor.copy(alpha = 0.25f),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
private fun GuidedCurriculumLessonCard(
    lesson: GuidedPracticeLesson,
    currentLang: String,
    surfaceColor: Color,
    textColor: Color,
    accent: Color,
    completed: Boolean,
    bestBpm: Int,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val isVietnamese = currentLang.lowercase(Locale.ROOT).startsWith("vi")
    val shape = RoundedCornerShape(15.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (completed) accent.copy(alpha = 0.62f) else accent.copy(alpha = 0.24f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(accent.copy(alpha = if (enabled) 0.10f else 0.025f), Color.Transparent)
                    )
                )
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accent.copy(alpha = if (enabled) 0.17f else 0.06f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (completed) "✓" else lesson.number,
                        color = if (enabled) accent else textColor.copy(alpha = 0.30f),
                        fontWeight = FontWeight.Black,
                        fontSize = if (completed) 18.sp else 11.sp
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = lesson.title(currentLang),
                        color = textColor.copy(alpha = if (enabled) 1f else 0.42f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (lesson.kind == GuidedLessonKind.FINGER) {
                            if (isVietnamese) "KỸ THUẬT" else "TECHNIQUE"
                        } else {
                            if (isVietnamese) "HÒA ÂM" else "HARMONY"
                        },
                        color = accent,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (enabled) lesson.objective(currentLang) else {
                        if (isVietnamese) "Nội dung này hiện chỉ hỗ trợ đúng nhạc cụ phù hợp." else "This lesson requires the supported instrument."
                    },
                    color = textColor.copy(alpha = if (enabled) 0.58f else 0.30f),
                    fontSize = 9.4.sp,
                    lineHeight = 12.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isVietnamese) {
                            "${lesson.targetRounds} vòng · ${lesson.bpmStart}→${lesson.bpmTarget} BPM"
                        } else {
                            "${lesson.targetRounds} rounds · ${lesson.bpmStart}→${lesson.bpmTarget} BPM"
                        },
                        color = accent.copy(alpha = if (enabled) 0.90f else 0.35f),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (bestBpm > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isVietnamese) "Tốt nhất $bestBpm" else "Best $bestBpm",
                            color = textColor.copy(alpha = 0.50f),
                            fontSize = 8.2.sp
                        )
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = if (enabled) Icons.Default.ArrowForward else Icons.Default.Lock,
                contentDescription = null,
                tint = if (enabled) accent else textColor.copy(alpha = 0.25f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun GuidedChapterPartSelector(
    chapter: GuidedPracticeChapter,
    currentLesson: GuidedPracticeLesson,
    lessons: List<GuidedPracticeLesson>,
    currentLang: String,
    surfaceColor: Color,
    textColor: Color,
    compact: Boolean,
    onLessonSelected: (GuidedPracticeLesson) -> Unit
) {
    if (lessons.size <= 1) return

    val context = LocalContext.current
    val isVietnamese = currentLang.lowercase(Locale.ROOT).startsWith("vi")
    val completedIds = GuidedPracticeProgressStore.completedIds(context)
    val completedParts = lessons.count { it.id in completedIds }
    var expanded by rememberSaveable(currentLesson.id) { mutableStateOf(false) }
    val shape = RoundedCornerShape(if (compact) 11.dp else 13.dp)

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                // Use the content's measured height: the default Material text
                // line-height previously clipped the second line of the selector.
                // heightIn keeps it compact while allowing larger font scales.
                .heightIn(min = if (compact) 48.dp else 54.dp)
                .clip(shape)
                .clickable { expanded = true },
            shape = shape,
            color = surfaceColor,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                currentLesson.accent.copy(alpha = 0.34f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = currentLesson.accent.copy(alpha = 0.17f),
                    modifier = Modifier.size(if (compact) 31.dp else 35.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = currentLesson.number,
                            color = currentLesson.accent,
                            fontSize = if (compact) 8.5.sp else 9.5.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (isVietnamese) {
                            "Chương ${chapter.number} · Chọn phần học"
                        } else {
                            "Chapter ${chapter.number} · Choose a part"
                        },
                        color = textColor.copy(alpha = 0.48f),
                        fontSize = if (compact) 8.sp else 9.sp,
                        lineHeight = if (compact) 11.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = currentLesson.title(currentLang),
                        color = textColor,
                        fontSize = if (compact) 10.5.sp else 12.sp,
                        lineHeight = if (compact) 14.sp else 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "$completedParts/${lessons.size}",
                    color = chapter.secondaryAccent,
                    fontSize = if (compact) 8.sp else 9.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.width(5.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (isVietnamese) "Chọn phần học" else "Choose chapter part",
                    tint = currentLesson.accent,
                    modifier = Modifier.size(if (compact) 17.dp else 20.dp)
                )
            }
        }

        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(if (compact) 286.dp else 320.dp)
                .background(surfaceColor)
        ) {
            lessons.forEach { lesson ->
                val completed = lesson.id in completedIds
                val selected = lesson.id == currentLesson.id
                val bestBpm = GuidedPracticeProgressStore.bestBpm(context, lesson.id)

                androidx.compose.material3.DropdownMenuItem(
                    modifier = Modifier.heightIn(min = if (compact) 58.dp else 62.dp),
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "${lesson.number} · ${lesson.title(currentLang)}",
                                color = if (selected) lesson.accent else textColor,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = buildString {
                                    append("${lesson.targetRounds} ")
                                    append(if (isVietnamese) "vòng" else "rounds")
                                    append(" · ${lesson.bpmStart}→${lesson.bpmTarget} BPM")
                                    if (bestBpm > 0) {
                                        append(" · ")
                                        append(if (isVietnamese) "Tốt nhất" else "Best")
                                        append(" $bestBpm")
                                    }
                                },
                                color = lesson.accent.copy(alpha = 0.82f),
                                fontSize = 8.5.sp,
                                lineHeight = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    trailingIcon = {
                        Text(
                            text = when {
                                completed -> "✓"
                                selected -> "●"
                                else -> "○"
                            },
                            color = lesson.accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    },
                    onClick = {
                        expanded = false
                        onLessonSelected(lesson)
                    }
                )
            }
        }
    }
}

@Composable
private fun GuidedLessonStrip(
    lesson: GuidedPracticeLesson,
    currentLang: String,
    completedRounds: Int,
    bpm: Int,
    lessonCompleted: Boolean,
    surfaceColor: Color,
    textColor: Color,
    compact: Boolean
) {
    val isVietnamese = currentLang.lowercase(Locale.ROOT).startsWith("vi")
    val target = lesson.targetRounds.coerceAtLeast(1)
    val progress = if (lessonCompleted) 1f else {
        (completedRounds.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    }
    val stageTitle: String
    val detail: String
    when {
        lessonCompleted -> {
            stageTitle = if (isVietnamese) "3 · ÁP DỤNG" else "3 · APPLY"
            detail = lesson.application(currentLang)
        }
        completedRounds <= 0 -> {
            stageTitle = if (isVietnamese) "1 · NGHE & HIỂU" else "1 · LISTEN & UNDERSTAND"
            detail = lesson.theory(currentLang)
        }
        else -> {
            stageTitle = if (isVietnamese) "2 · CHƠI THEO NHỊP" else "2 · PLAY IN TIME"
            detail = lesson.objective(currentLang)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 61.dp else 68.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, lesson.accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 9.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${lesson.number} · ${lesson.title(currentLang)}",
                    color = textColor,
                    fontSize = if (compact) 10.5.sp else 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stageTitle,
                    color = lesson.accent,
                    fontSize = if (compact) 7.2.sp else 8.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Text(
                text = detail,
                color = textColor.copy(alpha = 0.58f),
                fontSize = if (compact) 7.5.sp else 8.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(50)),
                    color = lesson.accent,
                    trackColor = textColor.copy(alpha = 0.08f)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = if (lessonCompleted) {
                        if (isVietnamese) "Hoàn thành" else "Completed"
                    } else {
                        "$completedRounds/$target · $bpm/${lesson.bpmTarget} BPM"
                    },
                    color = if (lessonCompleted) lesson.accent else textColor.copy(alpha = 0.58f),
                    fontSize = if (compact) 7.3.sp else 8.2.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun GuidedRoadmapPreview(
    progress: Float,
    accent: Color,
    secondaryAccent: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas

        val points = listOf(
            Offset(size.width * 0.18f, size.height * 0.72f),
            Offset(size.width * 0.39f, size.height * 0.34f),
            Offset(size.width * 0.64f, size.height * 0.62f),
            Offset(size.width * 0.83f, size.height * 0.24f)
        )
        val pathProgress = progress.coerceIn(0f, 1f)
        val completedNodes = (pathProgress * points.size).toInt().coerceIn(0, points.size)
        val trackWidth = size.minDimension * 0.055f

        points.zipWithNext().forEachIndexed { index, (from, to) ->
            drawLine(
                color = textColor.copy(alpha = 0.16f),
                start = from,
                end = to,
                strokeWidth = trackWidth,
                cap = StrokeCap.Round
            )
            if (index < completedNodes - 1) {
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(accent, secondaryAccent),
                        start = from,
                        end = to
                    ),
                    start = from,
                    end = to,
                    strokeWidth = trackWidth,
                    cap = StrokeCap.Round
                )
            }
        }

        points.forEachIndexed { index, point ->
            val active = index < completedNodes
            val current = index == completedNodes && completedNodes < points.size
            val radius = size.minDimension * if (current) 0.105f else 0.085f
            if (current) {
                drawCircle(
                    color = accent.copy(alpha = 0.18f),
                    radius = radius * 1.75f,
                    center = point
                )
            }
            drawCircle(
                color = when {
                    active -> if (index % 2 == 0) accent else secondaryAccent
                    current -> accent
                    else -> textColor.copy(alpha = 0.18f)
                },
                radius = radius,
                center = point
            )
            drawCircle(
                color = textColor.copy(alpha = if (active || current) 0.88f else 0.28f),
                radius = radius * 0.34f,
                center = point
            )
        }
    }
}

@Composable
private fun GuidedProgressRing(
    progress: Float,
    accent: Color,
    textColor: Color,
    completedCount: Int,
    totalCount: Int,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val safeProgress = progress.coerceIn(0f, 1f)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.095f
            drawCircle(
                color = textColor.copy(alpha = 0.10f),
                style = Stroke(width = stroke)
            )
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = safeProgress * 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(safeProgress * 100f).roundToInt()}%",
                color = accent,
                fontSize = if (compact) 8.5.sp else 9.5.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Text(
                text = "$completedCount/$totalCount",
                color = textColor.copy(alpha = 0.52f),
                fontSize = if (compact) 6.5.sp else 7.2.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun GuidedLearningEntryCard(
    currentLang: String,
    surfaceColor: Color,
    textColor: Color,
    accent: Color,
    completedCount: Int,
    totalCount: Int,
    compact: Boolean,
    onClick: () -> Unit
) {
    val isVietnamese = currentLang.lowercase(Locale.ROOT).startsWith("vi")
    val secondaryAccent = Color(0xFFFF8A50)
    val progress = if (totalCount <= 0) 0f else completedCount.toFloat() / totalCount.toFloat()
    val shape = RoundedCornerShape(if (compact) 16.dp else 19.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 92.dp else 104.dp)
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.48f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            accent.copy(alpha = 0.20f),
                            secondaryAccent.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = if (compact) 9.dp else 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(if (compact) 58.dp else 66.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(if (compact) 13.dp else 15.dp))
                    .background(textColor.copy(alpha = 0.035f))
                    .border(
                        1.dp,
                        accent.copy(alpha = 0.20f),
                        RoundedCornerShape(if (compact) 13.dp else 15.dp)
                    )
                    .padding(7.dp)
            ) {
                GuidedRoadmapPreview(
                    progress = progress,
                    accent = accent,
                    secondaryAccent = secondaryAccent,
                    textColor = textColor,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.width(if (compact) 9.dp else 11.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = accent.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            accent.copy(alpha = 0.28f)
                        )
                    ) {
                        Text(
                            text = if (isVietnamese) "LỘ TRÌNH $totalCount BÀI" else "$totalCount-LESSON PATH",
                            color = accent,
                            fontSize = if (compact) 7.2.sp else 7.8.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (completedCount > 0) {
                            if (isVietnamese) "Tiếp tục" else "Continue"
                        } else {
                            if (isVietnamese) "Bắt đầu" else "Start"
                        },
                        color = secondaryAccent,
                        fontSize = if (compact) 7.5.sp else 8.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(3.dp))

                Text(
                    text = if (isVietnamese) {
                        "Học từng bước, có mục tiêu"
                    } else {
                        "Learn step by step with clear goals"
                    },
                    color = textColor,
                    fontSize = if (compact) 13.2.sp else 14.7.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = if (isVietnamese) {
                        "Kỹ thuật ngón  →  Hòa âm  →  Ứng dụng"
                    } else {
                        "Finger technique  →  Harmony  →  Application"
                    },
                    color = textColor.copy(alpha = 0.60f),
                    fontSize = if (compact) 8.1.sp else 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isVietnamese) {
                            "Nghe  •  Hiểu  •  Chơi  •  Áp dụng"
                        } else {
                            "Listen  •  Understand  •  Play  •  Apply"
                        },
                        color = accent,
                        fontSize = if (compact) 7.4.sp else 8.2.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = secondaryAccent,
                        modifier = Modifier.size(if (compact) 15.dp else 17.dp)
                    )
                }
            }

            Spacer(Modifier.width(if (compact) 7.dp else 9.dp))

            GuidedProgressRing(
                progress = progress,
                accent = accent,
                textColor = textColor,
                completedCount = completedCount,
                totalCount = totalCount,
                compact = compact,
                modifier = Modifier.size(if (compact) 47.dp else 54.dp)
            )
        }
    }
}

@Composable
private fun FreePracticeSectionHeader(
    currentLang: String,
    textColor: Color,
    accent: Color,
    compact: Boolean
) {
    val isVietnamese = currentLang.lowercase(Locale.ROOT).startsWith("vi")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = if (compact) 1.dp else 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 7.dp else 8.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(Modifier.width(6.dp))
        Column {
            Text(
                text = if (isVietnamese) "LUYỆN TỰ DO" else "FREE PRACTICE",
                color = textColor.copy(alpha = 0.72f),
                fontSize = if (compact) 8.3.sp else 9.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = if (isVietnamese) {
                    "Tự chọn nội dung, tông, vị trí và tốc độ"
                } else {
                    "Choose content, key, position and tempo"
                },
                color = textColor.copy(alpha = 0.42f),
                fontSize = if (compact) 7.2.sp else 7.8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = textColor.copy(alpha = 0.08f)
        )
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
    onOpenCurriculum: () -> Unit,
    onOpenFingerTraining: () -> Unit,
    onOpenHarmonyPractice: () -> Unit,
    onOpenPitchRecognition: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
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
    val harmonyProgressionCount = remember { harmonyProgressions().size }
    val curriculumAccent = Color(0xFFF2B94B)
    val harmonyAccent = Color(0xFFE85D86)
    val pitchAccent = Color(0xFF6D91E8)
    val panelShape = RoundedCornerShape(if (compactHeight) 13.dp else 16.dp)
    // Ba khu vực luyện tự do được giữ gọn; lộ trình hướng dẫn nằm ở thẻ riêng phía trên.
    val entryCardHeight = if (compactHeight) 86.dp else 98.dp
    val guidedLessons = remember(tunaGreen) { guidedPracticeLessons(tunaGreen) }
    val enabledGuidedLessons = guidedLessons.filter { lesson ->
        when (lesson.kind) {
            GuidedLessonKind.FINGER -> instrument != "Piano"
            GuidedLessonKind.HARMONY -> instrument == "Guitar"
        }
    }
    val completedGuidedIds = GuidedPracticeProgressStore.completedIds(context)
    val guidedUnitCount = guidedCurriculumUnitCount(enabledGuidedLessons)
    val completedGuidedCount = guidedCurriculumCompletedCount(enabledGuidedLessons, completedGuidedIds)

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = bgColor) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = if (compactHeight) 12.dp else 14.dp)
                    .verticalScroll(rememberScrollState())
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
                            text = if (isVietnamese) "Chọn cách học phù hợp" else "Choose how you want to learn",
                            color = textColor,
                            fontSize = if (compactHeight) 13.sp else 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (isVietnamese) {
                                "Đi theo $guidedUnitCount bài có mục tiêu hoặc tự thiết lập buổi luyện."
                            } else {
                                "Follow $guidedUnitCount goal-based lessons or build your own practice session."
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

                GuidedLearningEntryCard(
                    currentLang = currentLang,
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    accent = curriculumAccent,
                    completedCount = completedGuidedCount,
                    totalCount = guidedUnitCount,
                    compact = compactHeight,
                    onClick = onOpenCurriculum
                )

                Spacer(Modifier.height(if (compactHeight) 5.dp else 7.dp))

                FreePracticeSectionHeader(
                    currentLang = currentLang,
                    textColor = textColor,
                    accent = tunaGreen,
                    compact = compactHeight
                )

                Spacer(Modifier.height(if (compactHeight) 3.dp else 4.dp))

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
                        metaLeft = if (isVietnamese) "$harmonyProgressionCount vòng · 12 tông"
                        else "$harmonyProgressionCount loops · 12 keys",
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
                                "Người mới: mở Lộ trình học; ứng dụng sẽ tự chọn bài, nhịp và BPM phù hợp."
                            } else {
                                "Với Piano, bắt đầu ở Bước 1: Chơi đúng nốt."
                            }
                        } else {
                            if (fingerEnabled) {
                                "New players: open the guided path; the app selects the lesson, rhythm and BPM."
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
    // Horizontal six-string guitar fretboard in the second menu card.
    // The saddle/bridge is on the LEFT (9 o'clock); higher frets are on
    // the left, nearer the bridge, and lower frets on the right.
    // Low E (6) is at the top and high e (1) at the bottom.
    // Real ii-V-I triad notes within frets 5-9:
    //   Dm: G7=D, B6=F, e5=A
    //    G: G7=D, B8=G, e7=B
    //    C: G9=E, B8=G, e8=C
    var activeChord by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(850L)
            activeChord = (activeChord + 1) % 3
        }
    }

    val transition = rememberInfiniteTransition(label = "miniHarmonyPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "miniHarmonyPulseValue"
    )
    val density = LocalDensity.current
    val chordFontSize = with(density) { 7.sp.toPx() }
    val noteFontSize = with(density) { 5.5.sp.toPx() }
    val baseLineWidth = with(density) { 0.65.dp.toPx() }
    val chordLabels = listOf("Dm", "G", "C")
    // Triple(string number, fret, triad role: root=0, third=1, fifth=2)
    val chordNotes = listOf(
        listOf(Triple(3, 7, 0), Triple(2, 6, 1), Triple(1, 5, 2)),
        listOf(Triple(3, 7, 2), Triple(2, 8, 0), Triple(1, 7, 1)),
        listOf(Triple(3, 9, 1), Triple(2, 8, 2), Triple(1, 8, 0))
    )

    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas

        val left = size.width * 0.14f
        val right = size.width * 0.94f
        val top = size.height * 0.29f
        val bottom = size.height * 0.89f
        val stringSpacing = (bottom - top) / 5f
        val fretSpacing = (right - left) / 5f
        fun stringY(stringNumber: Int): Float = top + (6 - stringNumber) * stringSpacing
        // Fret 9 is on the LEFT, nearer the bridge; fret 5 is on the RIGHT.
        fun fretX(fret: Int): Float = right - (fret - 5 + 0.5f) * fretSpacing

        drawRoundRect(
            color = Color(0xFF211B1B).copy(alpha = 0.82f),
            topLeft = Offset(left, top - stringSpacing * 0.34f),
            size = androidx.compose.ui.geometry.Size(
                right - left,
                (bottom - top) + stringSpacing * 0.68f
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                with(density) { 3.dp.toPx() },
                with(density) { 3.dp.toPx() }
            )
        )

        // Five fret cells. The vertical bars are the fret wires.
        for (fretLine in 0..5) {
            val x = left + fretLine * fretSpacing
            drawLine(
                color = textColor.copy(alpha = 0.31f),
                start = Offset(x, top - stringSpacing * 0.34f),
                end = Offset(x, bottom + stringSpacing * 0.34f),
                strokeWidth = baseLineWidth,
                cap = StrokeCap.Round
            )
        }

        // All six strings are HORIZONTAL; bass strings are thicker/copper.
        for (stringNumber in 6 downTo 1) {
            val y = stringY(stringNumber)
            drawLine(
                color = if (stringNumber >= 4) {
                    Color(0xFFD2A15F).copy(alpha = 0.86f)
                } else {
                    textColor.copy(alpha = 0.70f)
                },
                start = Offset(left - size.width * 0.045f, y),
                end = Offset(right, y),
                strokeWidth = baseLineWidth * (1f + (stringNumber - 1) * 0.16f),
                cap = StrokeCap.Round
            )
        }

        // Gold saddle at the LEFT edge makes the 9-o'clock orientation clear.
        val bridgeX = left - size.width * 0.045f
        drawLine(
            color = Color(0xFFE2B46B),
            start = Offset(bridgeX, top - stringSpacing * 0.42f),
            end = Offset(bridgeX, bottom + stringSpacing * 0.42f),
            strokeWidth = baseLineWidth * 3.0f,
            cap = StrokeCap.Round
        )

        val chordPaint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textSize = chordFontSize
        }
        // Labels stay horizontal (unrotated) above the fretboard.
        chordLabels.forEachIndexed { index, name ->
            val x = left + (right - left) * ((index + 0.5f) / 3f)
            chordPaint.color = (
                    if (index == activeChord) accent else textColor.copy(alpha = 0.60f)
                    ).toArgb()
            drawContext.canvas.nativeCanvas.drawText(name, x, top * 0.64f, chordPaint)
        }

        val notes = chordNotes[activeChord]
        notes.zipWithNext().forEach { (from, to) ->
            drawLine(
                color = accent.copy(alpha = 0.42f),
                start = Offset(fretX(from.second), stringY(from.first)),
                end = Offset(fretX(to.second), stringY(to.first)),
                strokeWidth = baseLineWidth * 1.7f,
                cap = StrokeCap.Round
            )
        }

        val notePaint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textSize = noteFontSize
            color = android.graphics.Color.WHITE
        }
        val radius = minOf(
            stringSpacing * 0.40f,
            fretSpacing * 0.36f,
            with(density) { 4.2.dp.toPx() }
        ).coerceAtLeast(with(density) { 1.7.dp.toPx() })
        notes.forEachIndexed { index, note ->
            val center = Offset(fretX(note.second), stringY(note.first))
            val color = when (note.third) {
                0 -> rootAccent
                1 -> Color(0xFFFF8A50)
                else -> Color(0xFF6D91E8)
            }
            val highlight = index == activeChord
            if (highlight) {
                drawCircle(color.copy(alpha = 0.22f), radius * 1.35f * pulse, center)
            }
            drawCircle(color, radius * if (highlight) pulse else 1f, center)
            val label = when (note.third) { 0 -> "R"; 1 -> "3"; else -> "5" }
            val textY = center.y - (notePaint.ascent() + notePaint.descent()) / 2f
            drawContext.canvas.nativeCanvas.drawText(label, center.x, textY, notePaint)
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