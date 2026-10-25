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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.input.pointer.pointerInput
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
        descriptionVi = "Bài 13 gồm 3 phần: 13A Trưởng → 13B Em7/E7 → 13C Thứ",
        descriptionEn = "Lesson 13 has 3 parts: 13A Major → 13B Em7/E7 → 13C Minor",
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

// The 13 original core lessons are always shown in numeric order across the
// five roadmap stages. The 13A/13B/13C parts form ONE progress unit (lesson 13).
// Do not use the separate, unnumbered free-practice activities to calculate progress.
private fun guidedRoadmapStageForLesson(lesson: GuidedPracticeLesson): Int {
    val lessonNumber = lesson.number.takeWhile { it.isDigit() }.toIntOrNull()
    return when (lessonNumber) {
        in 1..4 -> 0
        in 5..6 -> 1
        in 7..8 -> 2
        in 9..11 -> 3
        else -> 4  // Lesson 12 and all three parts of lesson 13.
    }
}

// Theory curriculum uses separate, stable IDs and preferences. The existing
// guided lessons (01-13, including 13A/B/C) are intentionally unchanged.
private data class TheoryLabel(val vi: String, val en: String) {
    fun text(lang: String): String = if (lang.lowercase(Locale.ROOT).startsWith("vi")) vi else en
}
private fun tl(vi: String, en: String) = TheoryLabel(vi, en)

private data class TheoryQuestion(
    val prompt: TheoryLabel,
    val choices: List<TheoryLabel>,
    val correctIndex: Int,
    val explanation: TheoryLabel
)

private fun theoryQuestion(
    vi: String, en: String, viOptions: List<String>, enOptions: List<String>,
    correct: Int, explanationVi: String, explanationEn: String
): TheoryQuestion {
    require(viOptions.size == 4 && enOptions.size == 4 && correct in 0..3)
    return TheoryQuestion(tl(vi, en), viOptions.zip(enOptions).map { (a, b) -> tl(a, b) },
        correct, tl(explanationVi, explanationEn))
}

private data class TheoryTopic(
    val id: String,
    val stage: Int,
    val title: TheoryLabel,
    val subtitle: TheoryLabel,
    val keyIdea: TheoryLabel,
    val details: List<TheoryLabel>,
    val exampleCaption: TheoryLabel,
    val audioGroups: List<List<Int>>,
    val questions: List<TheoryQuestion>
)

private fun theoryTopics(): List<TheoryTopic> = listOf(
    TheoryTopic("T01_TUNING", 0, tl("Sáu dây đàn và tên nốt", "Six strings and note names"),
        tl("E – A – D – G – B – E", "E – A – D – G – B – E"),
        tl("Dây 6 là E trầm; dây 1 là E cao.", "String 6 is low E; string 1 is high E."),
        listOf(tl("Từ dây 6 đến dây 1: E–A–D–G–B–E (lên dây chuẩn).", "From string 6 to string 1: E–A–D–G–B–E (standard tuning)."),
               tl("Giữa G và B là quãng 3 trưởng; các cặp dây kề còn lại cách nhau quãng 4 đúng.", "G to B is a major third; the other adjacent strings are a perfect fourth apart.")),
        tl("Nghe sáu dây buông từ trầm đến cao", "Hear six open strings, low to high"),
        listOf(listOf(40), listOf(45), listOf(50), listOf(55), listOf(59), listOf(64)),
        listOf(theoryQuestion("Dây 6 trong lên dây chuẩn là nốt nào?", "Which note is standard guitar string 6?",
                listOf("E trầm", "A", "D", "E cao"), listOf("Low E", "A", "D", "High E"), 0,
                "Dây 6 dày nhất phát E2, MIDI 40.", "The thickest sixth string plays E2, MIDI 40."),
               theoryQuestion("Hai dây G và B cách nhau quãng nào?", "What is the interval between the G and B strings?",
                listOf("Quãng 2 trưởng", "Quãng 3 trưởng", "Quãng 4 đúng", "Quãng 5 đúng"),
                listOf("Major second", "Major third", "Perfect fourth", "Perfect fifth"), 1,
                "G lên B là 4 bán cung, tức quãng 3 trưởng.", "G to B spans four semitones, a major third."))),
    TheoryTopic("T02_DURATION", 0, tl("Trường độ và dấu lặng", "Note lengths and rests"),
        tl("Nốt đen · móc đơn · móc kép", "Quarter · eighth · sixteenth"),
        tl("Trong cùng tốc độ: nốt đen bằng 2 nốt móc đơn.", "At the same tempo, a quarter note equals two eighth notes."),
        listOf(tl("Trong nhịp có đơn vị phách là nốt đen: nốt trắng = 2 phách, nốt đen = 1, móc đơn = 1/2, móc kép = 1/4.",
                  "With a quarter-note beat: half note = 2 beats, quarter = 1, eighth = 1/2, sixteenth = 1/4."),
               tl("Dấu lặng có trường độ tương ứng nhưng không phát ra âm.", "A rest has the matching duration but produces silence.")),
        tl("Nghe 4 phách đều (ví dụ minh họa)", "Hear four steady pulses (illustration)"),
        listOf(listOf(64), listOf(64), listOf(64), listOf(64)),
        listOf(theoryQuestion("Một nốt đen bằng bao nhiêu móc đơn?", "How many eighth notes equal a quarter note?",
                listOf("1", "2", "3", "4"), listOf("1", "2", "3", "4"), 1,
                "Hai móc đơn có tổng thời lượng bằng một nốt đen.", "Two eighth notes add up to one quarter note."),
               theoryQuestion("Dấu lặng đen dài bao nhiêu?", "How long is a quarter rest?",
                listOf("Nửa phách", "Một phách", "Hai phách", "Bốn phách"),
                listOf("Half beat", "One beat", "Two beats", "Four beats"), 1,
                "Trong nhịp lấy nốt đen làm phách, dấu lặng đen bằng một phách im lặng.",
                "With a quarter-note beat, a quarter rest is one silent beat."))),
    TheoryTopic("T03_METER", 0, tl("Số chỉ nhịp và phách mạnh", "Time signatures and accents"),
        tl("2/4 · 3/4 · 4/4 · 6/8", "2/4 · 3/4 · 4/4 · 6/8"),
        tl("3/4 thường có ba phách đen; 6/8 thường cảm thành hai phách chấm dôi.",
           "3/4 usually has three quarter beats; 6/8 is commonly felt in two dotted-quarter beats."),
        listOf(tl("4/4 thường đếm 1–2–3–4. Nhấn phách 1 để định hướng ô nhịp.",
                  "4/4 is commonly counted 1–2–3–4. Accent beat 1 to orient each bar."),
               tl("6/8 có sáu móc đơn trong một ô; thường nhóm 3+3, không đồng nghĩa với sáu phách đen.",
                  "6/8 has six eighth notes per bar, commonly grouped 3+3, not six quarter-note beats.")),
        tl("Nghe nhóm ba nốt minh họa nhịp 3/4", "Three-note example suggesting 3/4"),
        listOf(listOf(69), listOf(64), listOf(64)),
        listOf(theoryQuestion("Nhịp 3/4 có tổng trường độ bằng bao nhiêu nốt đen?", "How many quarter notes fit in a 3/4 bar?",
                listOf("2", "3", "4", "6"), listOf("2", "3", "4", "6"), 1,
                "Tử số 3 và mẫu số 4 chỉ ba đơn vị nốt đen mỗi ô nhịp.",
                "Three over four means three quarter-note units per bar."),
               theoryQuestion("Nhịp 6/8 thường được nhóm như thế nào?", "How is 6/8 commonly grouped?",
                listOf("2+2+2", "3+3", "4+2", "1+5"), listOf("2+2+2", "3+3", "4+2", "1+5"), 1,
                "Trong nhịp kép, sáu móc đơn thường thành hai nhóm ba.",
                "In compound duple meter, six eighth notes usually form two groups of three."))),
    TheoryTopic("T04_SEMITONES", 1, tl("Cung và nửa cung", "Whole steps and semitones"),
        tl("Nửa cung = 1 ngăn đàn", "Semitone = one fret"),
        tl("Trên cùng dây, tiến một ngăn tăng một bán cung.", "On one string, moving one fret raises the pitch one semitone."),
        listOf(tl("E–F và B–C liền nhau một nửa cung, không có phím đen xen giữa trên piano.",
                  "E–F and B–C are semitone pairs without an intervening black piano key."),
               tl("C–D là một cung (hai bán cung). Các dấu ♯/♭ thay đổi cao độ từng bán cung.",
                  "C–D is a whole step (two semitones). Sharps and flats change pitches by semitones.")),
        tl("Nghe C → C♯ → D trên cùng một quãng", "Hear C → C♯ → D in one register"),
        listOf(listOf(60), listOf(61), listOf(62)),
        listOf(theoryQuestion("Trên cùng dây, tăng 1 ngăn bằng bao nhiêu bán cung?", "How many semitones is one guitar fret?",
                listOf("0", "1", "2", "12"), listOf("0", "1", "2", "12"), 1,
                "Một ngăn trên cùng dây bằng một bán cung.", "One fret on the same string equals one semitone."),
               theoryQuestion("C đến D gồm bao nhiêu bán cung?", "How many semitones from C to D?",
                listOf("1", "2", "3", "4"), listOf("1", "2", "3", "4"), 1,
                "C–C♯–D là hai bước nửa cung.", "C–C♯–D spans two semitone steps."))),
    TheoryTopic("T05_INTERVALS", 1, tl("Quãng âm cơ bản", "Basic musical intervals"),
        tl("Quãng 3 trưởng = 4 · quãng 5 đúng = 7 bán cung", "Major third = 4 · perfect fifth = 7 semitones"),
        tl("Quãng mô tả khoảng cách hai cao độ, không phụ thuộc vào dây đàn.",
           "An interval is the pitch distance between two notes, regardless of strings."),
        listOf(tl("C–E là quãng 3 trưởng; C–E♭ là quãng 3 thứ (3 bán cung).", "C–E is a major third; C–E♭ is a minor third (3 semitones)."),
               tl("C–G là quãng 5 đúng, nền tảng của nhiều hợp âm và power chord.",
                  "C–G is a perfect fifth, foundational to many chords and power chords.")),
        tl("Nghe C → E → G", "Hear C → E → G"),
        listOf(listOf(60), listOf(64), listOf(67)),
        listOf(theoryQuestion("Quãng 3 thứ bằng mấy bán cung?", "How many semitones make a minor third?",
                listOf("2", "3", "4", "5"), listOf("2", "3", "4", "5"), 1,
                "Ví dụ C đến E♭ là 3 bán cung.", "For example, C to E♭ spans three semitones."),
               theoryQuestion("C–G là quãng gì?", "Which interval is C–G?",
                listOf("Quãng 4 đúng", "Quãng 5 đúng", "Quãng 6 trưởng", "Quãng 7 trưởng"),
                listOf("Perfect fourth", "Perfect fifth", "Major sixth", "Major seventh"), 1,
                "C–G cách 7 bán cung: quãng 5 đúng.", "C–G spans seven semitones: a perfect fifth."))),
    TheoryTopic("T06_PENTATONIC", 1, tl("Ngũ cung trưởng và thứ", "Major and minor pentatonic"),
        tl("Trưởng: 1–2–3–5–6 · Thứ: 1–♭3–4–5–♭7", "Major: 1–2–3–5–6 · Minor: 1–♭3–4–5–♭7"),
        tl("Ngũ cung chỉ có năm bậc âm khác nhau trong một quãng tám.",
           "A pentatonic scale has five distinct scale degrees per octave."),
        listOf(tl("C ngũ cung trưởng: C–D–E–G–A; A ngũ cung thứ: A–C–D–E–G.",
                  "C major pentatonic: C–D–E–G–A; A minor pentatonic: A–C–D–E–G."),
               tl("Cùng tập nốt có thể có nốt chủ khác nhau: điều này không tự động làm hai âm giai cùng giọng.",
                  "The same notes can have different tonics; sharing notes does not mean sharing a tonal center.")),
        tl("Nghe C ngũ cung trưởng", "Hear C major pentatonic"),
        listOf(listOf(60), listOf(62), listOf(64), listOf(67), listOf(69)),
        listOf(theoryQuestion("Công thức ngũ cung trưởng là gì?", "What is the major pentatonic formula?",
                listOf("1–2–3–5–6", "1–♭3–4–5–♭7", "1–2–3–4–5", "1–3–5–7–9"),
                listOf("1–2–3–5–6", "1–♭3–4–5–♭7", "1–2–3–4–5", "1–3–5–7–9"), 0,
                "Ngũ cung trưởng bỏ bậc 4 và 7 của âm giai trưởng.",
                "Major pentatonic omits degrees 4 and 7 of the major scale."),
               theoryQuestion("A ngũ cung thứ có nốt nào?", "Which note belongs to A minor pentatonic?",
                listOf("F♯", "G", "B", "D♯"), listOf("F♯", "G", "B", "D♯"), 1,
                "A–C–D–E–G là năm nốt của A ngũ cung thứ.",
                "A–C–D–E–G are the five notes of A minor pentatonic."))),
    TheoryTopic("T07_TRIAD", 2, tl("Hợp âm ba: 1–3–5", "Triads: 1–3–5"),
        tl("C trưởng = C–E–G", "C major = C–E–G"),
        tl("Triad là ba lớp cao độ gồm nốt gốc, bậc ba và bậc năm.",
           "A triad consists of a root, third, and fifth pitch class."),
        listOf(tl("Hợp âm trưởng: 1–3–5; hợp âm thứ: 1–♭3–5.",
                  "Major triad: 1–3–5; minor triad: 1–♭3–5."),
               tl("Học vị trí các nốt 1, 3, 5 trên cần đàn trước khi luyện đảo hợp âm.",
                  "Locate scale degrees 1, 3 and 5 before practising inversions.")),
        tl("Nghe hợp âm C trưởng", "Hear a C major triad"),
        listOf(listOf(60,64,67)),
        listOf(theoryQuestion("Hợp âm C trưởng gồm nốt nào?", "Which notes form C major?",
                listOf("C–E♭–G", "C–E–G", "C–F–G", "C–E–A"),
                listOf("C–E♭–G", "C–E–G", "C–F–G", "C–E–A"), 1,
                "C–E–G là ba nốt 1–3–5 của C trưởng.", "C–E–G represent degrees 1–3–5 of C major."),
               theoryQuestion("Trong triad, bậc 5 của C là nốt gì?", "What is the fifth of a C triad?",
                listOf("D", "E", "G", "B"), listOf("D", "E", "G", "B"), 2,
                "Từ C lên G là quãng 5 đúng.", "C to G is a perfect fifth."))),
    TheoryTopic("T08_TRIAD_TYPES", 2, tl("Trưởng, thứ, tăng và giảm", "Major, minor, augmented, diminished"),
        tl("1–3–5 · 1–♭3–5 · 1–3–♯5 · 1–♭3–♭5", "1–3–5 · 1–♭3–5 · 1–3–♯5 · 1–♭3–♭5"),
        tl("Bậc 3 và bậc 5 xác định chất lượng hợp âm ba.",
           "The third and fifth determine the triad quality."),
        listOf(tl("C giảm: C–E♭–G♭; C tăng: C–E–G♯.",
                  "C diminished: C–E♭–G♭; C augmented: C–E–G♯."),
               tl("Phân biệt triad giảm với hợp âm 7 giảm: số lượng lớp cao độ khác nhau.",
                  "A diminished triad is not the same as a diminished-seventh chord.")),
        tl("Nghe C giảm", "Hear C diminished"),
        listOf(listOf(60,63,66)),
        listOf(theoryQuestion("Cấu trúc triad giảm là gì?", "What is the diminished-triad formula?",
                listOf("1–3–5", "1–♭3–5", "1–♭3–♭5", "1–3–♯5"),
                listOf("1–3–5", "1–♭3–5", "1–♭3–♭5", "1–3–♯5"), 2,
                "Giảm bậc 3 và 5 so với hợp âm trưởng tạo triad giảm.",
                "Lowering the third and fifth produces a diminished triad."),
               theoryQuestion("C tăng chứa nốt nào?", "Which note belongs to C augmented?",
                listOf("G", "G♭", "G♯", "B♭"), listOf("G", "G♭", "G♯", "B♭"), 2,
                "C tăng gồm C–E–G♯: bậc 5 được nâng nửa cung.",
                "C augmented is C–E–G♯: the fifth is raised one semitone."))),
    TheoryTopic("T09_INVERSIONS", 2, tl("Thế đảo hợp âm", "Chord inversions"),
        tl("Gốc ở bass · bậc 3 ở bass · bậc 5 ở bass", "Root in bass · third in bass · fifth in bass"),
        tl("Thế đảo đổi nốt bass, không thay đổi bộ ba nốt của hợp âm.",
           "Inversions change the bass note, not the chord's pitch-class collection."),
        listOf(tl("C/E là C trưởng đảo 1, với E ở bass; C/G là đảo 2, với G ở bass.",
                  "C/E is C major first inversion with E in the bass; C/G is second inversion."),
               tl("Đảo hợp âm giúp giảm chuyển động tay và tạo đường bass mượt.",
                  "Inversions reduce hand movement and support smoother bass lines.")),
        tl("Nghe C/E (E–G–C)", "Hear C/E (E–G–C)"),
        listOf(listOf(52,55,60)),
        listOf(theoryQuestion("C/E có nốt nào ở bass?", "Which note is the bass of C/E?",
                listOf("C", "D", "E", "G"), listOf("C", "D", "E", "G"), 2,
                "Dấu gạch chéo cho biết bass E, hợp âm vẫn là C trưởng.",
                "The slash specifies E as bass while the chord is still C major."),
               theoryQuestion("C/G là thế đảo thứ mấy của C trưởng?", "Which inversion is C/G?",
                listOf("Thế gốc", "Đảo 1", "Đảo 2", "Đảo 3"),
                listOf("Root position", "First inversion", "Second inversion", "Third inversion"), 2,
                "C/G có bậc 5 (G) ở bass, nên là đảo 2.",
                "C/G puts the fifth (G) in the bass, giving second inversion."))),
    TheoryTopic("T10_DIATONIC", 3, tl("Hợp âm trong giọng và bậc La Mã", "Diatonic chords and Roman numerals"),
        tl("I – ii – iii – IV – V – vi – vii°", "I – ii – iii – IV – V – vi – vii°"),
        tl("Chữ hoa biểu thị hợp âm trưởng; chữ thường là hợp âm thứ.",
           "Uppercase numerals indicate major triads; lowercase indicates minor triads."),
        listOf(tl("Giọng C trưởng: C–Dm–Em–F–G–Am–Bdim.",
                  "In C major: C–Dm–Em–F–G–Am–Bdim."),
               tl("Lưu số bậc để chuyển tông: I–V–vi–IV ở C là C–G–Am–F, ở G là G–D–Em–C.",
                  "Roman numerals enable transposition: I–V–vi–IV is C–G–Am–F in C, or G–D–Em–C in G.")),
        tl("Nghe I → IV → V → I ở C", "Hear I → IV → V → I in C"),
        listOf(listOf(60,64,67),listOf(65,69,72),listOf(67,71,74),listOf(60,64,67)),
        listOf(theoryQuestion("Hợp âm ii trong C trưởng là gì?", "What is chord ii in C major?",
                listOf("D", "Dm", "Em", "G"), listOf("D", "Dm", "Em", "G"), 1,
                "Bậc ii của C trưởng là D–F–A, hợp âm Dm.",
                "Degree ii of C major is D–F–A, D minor."),
               theoryQuestion("Bậc V trong C trưởng là hợp âm nào?", "Which chord is V in C major?",
                listOf("F", "Am", "G", "Bdim"), listOf("F", "Am", "G", "Bdim"), 2,
                "Bậc V được xây từ G–B–D, tức G trưởng.",
                "Scale degree V builds G–B–D, a G major chord."))),
    TheoryTopic("T11_FUNCTIONS", 3, tl("Tonic, Pre-dominant, Dominant", "Tonic, pre-dominant, dominant"),
        tl("I → IV/ii → V → I", "I → IV/ii → V → I"),
        tl("Dominant thường dẫn mạnh về Tonic nhờ nốt dẫn và bậc 7 của V7.",
           "Dominant often resolves strongly to tonic through leading tones and the seventh of V7."),
        listOf(tl("Tonic (I, vi) tạo cảm giác về nhà; Pre-dominant (ii, IV) thường chuẩn bị cho Dominant.",
                  "Tonic (I, vi) feels at rest; pre-dominant (ii, IV) often prepares the dominant."),
               tl("Trong C trưởng, G7 → C là V7 → I; B có xu hướng về C, F về E.",
                  "In C major, G7 → C is V7 → I; B tends to C and F tends to E.")),
        tl("Nghe G7 → C", "Hear G7 → C"),
        listOf(listOf(55,59,62,65), listOf(60,64,67)),
        listOf(theoryQuestion("Trong C trưởng, G7 có chức năng nào?", "What is G7's function in C major?",
                listOf("Tonic", "Dominant", "Pre-dominant", "Neapolitan"),
                listOf("Tonic", "Dominant", "Pre-dominant", "Neapolitan"), 1,
                "G7 là V7, tạo lực hút về C (I).", "G7 is V7 and pulls toward C (I)."),
               theoryQuestion("Nhóm nào thường chuẩn bị cho Dominant?", "Which group typically prepares the dominant?",
                listOf("ii và IV", "I và vi", "V và vii°", "Chỉ có I"),
                listOf("ii and IV", "I and vi", "V and vii°", "Only I"), 0,
                "ii và IV thường có chức năng pre-dominant trong hòa âm chức năng cổ điển.",
                "ii and IV commonly serve pre-dominant function in tonal harmony."))),
    TheoryTopic("T12_II_V_I", 3, tl("Vòng ii–V–I và dẫn giọng", "ii–V–I and voice leading"),
        tl("Dm7 → G7 → Cmaj7", "Dm7 → G7 → Cmaj7"),
        tl("Chuyển hợp âm mượt là di chuyển các bè ngắn và giữ nốt chung khi phù hợp.",
           "Smooth changes use small voice movements and common tones when appropriate."),
        listOf(tl("Trong C: Dm7 = D–F–A–C; G7 = G–B–D–F; Cmaj7 = C–E–G–B.",
                  "In C: Dm7 = D–F–A–C; G7 = G–B–D–F; Cmaj7 = C–E–G–B."),
               tl("Guide tones là bậc 3 và 7 của hợp âm 7, thường quyết định hướng dẫn giọng.",
                  "Guide tones are the third and seventh of a seventh chord and are crucial to voice leading.")),
        tl("Nghe Dm7 → G7 → Cmaj7", "Hear Dm7 → G7 → Cmaj7"),
        listOf(listOf(50,53,57,60), listOf(55,59,62,65), listOf(60,64,67,71)),
        listOf(theoryQuestion("Vòng ii–V–I trong C trưởng là gì?", "Which is ii–V–I in C major?",
                listOf("Dm7–G7–Cmaj7", "Em7–A7–Dmaj7", "C–F–G", "Am–Dm–G"),
                listOf("Dm7–G7–Cmaj7", "Em7–A7–Dmaj7", "C–F–G", "Am–Dm–G"), 0,
                "ii là Dm7, V là G7 và I là Cmaj7 trong C trưởng.",
                "In C major, ii is Dm7, V is G7 and I is Cmaj7."),
               theoryQuestion("Guide tones của G7 gồm cặp nào?", "Which pair are G7's guide tones?",
                listOf("G và D", "A và C", "B và F", "G và B"),
                listOf("G and D", "A and C", "B and F", "G and B"), 2,
                "B là bậc 3, F là bậc ♭7 của G7.", "B is G7's third and F its flat seventh."))),
    TheoryTopic("T13_SEVENTHS", 4, tl("Hợp âm 7 và Arpeggio", "Seventh chords and arpeggios"),
        tl("maj7 · m7 · 7", "maj7 · m7 · 7"),
        tl("Hợp âm 7 thêm bậc 7 vào triad; arpeggio chơi các nốt hợp âm lần lượt.",
           "A seventh chord adds a seventh to a triad; an arpeggio plays its notes in sequence."),
        listOf(tl("Cmaj7: C–E–G–B; C7: C–E–G–B♭; Cm7: C–E♭–G–B♭.",
                  "Cmaj7: C–E–G–B; C7: C–E–G–B♭; Cm7: C–E♭–G–B♭."),
               tl("Bậc 3 và 7 là nốt dẫn quan trọng khi nối các hợp âm 7.",
                  "The third and seventh are important guide tones between seventh chords.")),
        tl("Nghe C7 rồi các nốt arpeggio C–E–G–B♭", "Hear C7 followed by its arpeggio"),
        listOf(listOf(60,64,67,70), listOf(60), listOf(64), listOf(67), listOf(70)),
        listOf(theoryQuestion("Cmaj7 khác C7 ở bậc nào?", "Which degree differs between Cmaj7 and C7?",
                listOf("Bậc 3", "Bậc 5", "Bậc 7", "Nốt gốc"),
                listOf("Third", "Fifth", "Seventh", "Root"), 2,
                "Cmaj7 có B (7); C7 có B♭ (♭7).", "Cmaj7 contains B (7); C7 contains B♭ (♭7)."),
               theoryQuestion("Arpeggio là gì?", "What is an arpeggio?",
                listOf("Chơi từng nốt của hợp âm", "Chơi cả âm giai chromatic", "Chỉ chơi nốt bass", "Đổi nhịp"),
                listOf("Playing chord tones in sequence", "Playing chromatic scale", "Playing only bass", "Changing meter"), 0,
                "Arpeggio tách các nốt hợp âm để chơi nối tiếp thay vì cùng lúc.",
                "An arpeggio plays chord tones one after another rather than together."))),
    TheoryTopic("T14_MODES_BLUES", 4, tl("Mode và âm giai Blues", "Modes and blues scales"),
        tl("Dorian ♮6 · Lydian ♯4 · Mixolydian ♭7 · Blues ♭5", "Dorian ♮6 · Lydian ♯4 · Mixolydian ♭7 · Blues ♭5"),
        tl("Một số mode khác nhau ở bậc đặc trưng; Blues có blue note riêng.",
           "Modes have distinctive degrees; blues scales feature blue notes."),
        listOf(tl("C Dorian: C–D–E♭–F–G–A–B♭; C Mixolydian: C–D–E–F–G–A–B♭.",
                  "C Dorian: C–D–E♭–F–G–A–B♭; C Mixolydian: C–D–E–F–G–A–B♭."),
               tl("C Blues thứ: C–E♭–F–G♭–G–B♭, với G♭ là ♭5 (blue note).",
                  "C minor blues: C–E♭–F–G♭–G–B♭, with G♭ as the ♭5 blue note.")),
        tl("Nghe C Blues thứ", "Hear C minor blues"),
        listOf(listOf(60),listOf(63),listOf(65),listOf(66),listOf(67),listOf(70)),
        listOf(theoryQuestion("Bậc đặc trưng của Lydian là gì?", "Which degree characterizes Lydian?",
                listOf("♭2", "♯4", "♭5", "♭7"), listOf("♭2", "♯4", "♭5", "♭7"), 1,
                "Lydian có bậc ♯4 so với Ionian.", "Lydian has a raised fourth compared to Ionian."),
               theoryQuestion("Blue note thêm vào ngũ cung thứ thường là bậc nào?", "Which blue note is added to minor pentatonic?",
                listOf("2", "3", "♭5", "7"), listOf("2", "3", "♭5", "7"), 2,
                "Blues thứ 6 nốt có thêm ♭5 so với ngũ cung thứ.",
                "The six-note minor blues scale adds ♭5 to minor pentatonic."))),
    TheoryTopic("T15_PASSING", 4, tl("Passing Chord và hợp âm chuyển", "Passing chords and approach harmony"),
        tl("Cmaj7 → C♯dim7 → Dm7", "Cmaj7 → C♯dim7 → Dm7"),
        tl("Hợp âm chuyển tạo hướng đi tới hợp âm đích, không chỉ để thêm nốt lạ.",
           "An approach chord creates motion toward the target, not random extra harmony."),
        listOf(tl("C♯dim7 giữa Cmaj7 và Dm7 tạo bass C–C♯–D.",
                  "C♯dim7 between Cmaj7 and Dm7 creates bass C–C♯–D."),
               tl("A7 → Dm là dominant phụ V7/ii trong giọng C; người học phải nghe giải quyết.",
                  "A7 → Dm is secondary dominant V7/ii in C; listen for resolution.")),
        tl("Nghe Cmaj7 → C♯dim7 → Dm7", "Hear Cmaj7 → C♯dim7 → Dm7"),
        listOf(listOf(48,52,55,59), listOf(49,52,55,58), listOf(50,53,57,60)),
        listOf(theoryQuestion("Hợp âm giữa trong Cmaj7–C♯dim7–Dm7 là gì?", "Which is the approach chord in Cmaj7–C♯dim7–Dm7?",
                listOf("Cmaj7", "C♯dim7", "Dm7", "Không có"),
                listOf("Cmaj7", "C♯dim7", "Dm7", "None"), 1,
                "C♯dim7 nằm giữa và đẩy bass C → C♯ → D.",
                "C♯dim7 sits between and moves bass C → C♯ → D."),
               theoryQuestion("A7 → Dm thường là loại gì trong giọng C?", "What is A7 → Dm in the key of C?",
                listOf("Tonic", "Dominant phụ V7/ii", "Hợp âm IV", "Hợp âm giảm"),
                listOf("Tonic", "Secondary dominant V7/ii", "IV chord", "Diminished chord"), 1,
                "A7 là V7 của Dm (ii), nên gọi là V7/ii.",
                "A7 is V7 of Dm (ii), making it V7/ii."))),
    TheoryTopic("T16_CHORD_MELODY", 4, tl("Chord Melody và nốt bè trên", "Chord melody and top voice"),
        tl("Giai điệu + hợp âm + dẫn giọng", "Melody + chords + voice leading"),
        tl("Chord Melody ghép nốt giai điệu với voicing phù hợp; không nhất thiết mỗi nốt đều có hợp âm.",
           "Chord melody harmonizes melody notes using chord voicings; not every melody note needs a full chord."),
        listOf(tl("Nốt melody thường nằm ở bè trên cùng của thế hợp âm, nhưng có thể xen nốt đơn.",
                  "The melody often sits in the top voice, but single notes can be interleaved."),
               tl("Ưu tiên thế bấm chơi được và dẫn giọng mượt, trước khi thêm passing chord.",
                  "Favor playable voicings and smooth voice leading before adding approach chords.")),
        tl("Nghe hai voicing có melody ở bè trên", "Hear two voicings with melody as top note"),
        listOf(listOf(48,55,59,64), listOf(50,57,60,65)),
        listOf(theoryQuestion("Trong Chord Melody, nốt giai điệu thường ở đâu?", "Where is the melody usually placed in chord melody?",
                listOf("Bè trên cùng", "Luôn ở bass", "Luôn ở dây 6", "Không có giai điệu"),
                listOf("Top voice", "Always bass", "Always string 6", "There is no melody"), 0,
                "Để giai điệu nghe rõ, nó thường nằm ở bè cao nhất.",
                "The melody is commonly voiced on top for clarity."),
               theoryQuestion("Chord Melody có cần gảy hợp âm ở mọi nốt không?", "Must every melody note be harmonized with a full chord?",
                listOf("Có, luôn luôn", "Không, có thể xen nốt đơn", "Chỉ dùng bass", "Chỉ dùng arpeggio"),
                listOf("Yes, always", "No, single notes can occur", "Only bass", "Only arpeggios"), 1,
                "Người chơi thường đan xen nốt đơn, double-stop và hợp âm đầy đủ.",
                "Players commonly interleave single notes, double-stops and full chords.")))
)

// A separate, original 18-lesson music-theory textbook.
// It does not change the existing 13 guided practice lessons or the legacy 16 theory topics.
private data class TheoryFoundationChapter(
    val title: TheoryLabel,
    val subtitle: TheoryLabel,
    val topicIds: List<String>
)

private fun theoryFoundationChapters() = listOf(
    TheoryFoundationChapter(tl("Khuông nhạc", "Staff notation"),
        tl("Dòng kẻ, khóa nhạc và cách đọc nốt", "Lines, clefs and reading notes"),
        listOf("F01_STAFF", "F02_CLEFS", "F03_STAFF_NOTES", "F04_LEDGER")),
    TheoryFoundationChapter(tl("Trường độ", "Rhythm and duration"),
        tl("Giá trị nốt, dấu lặng, nhịp và tempo", "Note values, rests, meter and tempo"),
        listOf("F05_VALUES", "F06_RESTS", "F07_DOTS_TIES", "F08_METER", "F09_TEMPO")),
    TheoryFoundationChapter(tl("Cao độ", "Pitch and intervals"),
        tl("Cung, dấu hóa và quãng", "Steps, accidentals and intervals"),
        listOf("F10_STEPS", "F11_ACCIDENTALS", "F12_INTERVALS")),
    TheoryFoundationChapter(tl("Hợp âm", "Chords"),
        tl("Trưởng, thứ, hợp âm 7 và sơ đồ guitar", "Major, minor, seventh chords and diagrams"),
        listOf("F13_MAJOR", "F14_MINOR", "F15_CHORD7", "F16_CHORD_DIAGRAM")),
    TheoryFoundationChapter(tl("Đọc TAB", "Guitar tablature"),
        tl("Vị trí trên cần đàn và ký hiệu TAB", "Fretboard positions and TAB symbols"),
        listOf("F17_FRETBOARD", "F18_TAB"))
)

private fun theoryFoundationTopics(): List<TheoryTopic> {
    fun f(id: String, chapter: Int, vi: String, en: String,
          subtitleVi: String, subtitleEn: String,
          ideaVi: String, ideaEn: String,
          details: List<TheoryLabel>,
          exampleVi: String, exampleEn: String,
          audio: List<List<Int>> = emptyList()): TheoryTopic =
        TheoryTopic(id, chapter, tl(vi, en), tl(subtitleVi, subtitleEn),
            tl(ideaVi, ideaEn), details, tl(exampleVi, exampleEn), audio, emptyList())
    return listOf(
        f("F01_STAFF", 0, "Khuông nhạc có 5 dòng", "The five-line staff",
            "Dòng, khe và chiều cao của nốt", "Lines, spaces and note placement",
            "Khuông nhạc có năm dòng kẻ song song, tạo bốn khe. Nốt được đặt trên dòng hoặc trong khe; càng cao trên khuông, cao độ thường càng cao.",
            "A staff consists of five lines and four spaces. Notes sit on lines or spaces; higher positions normally indicate higher pitches.",
            listOf(tl("Đếm dòng và khe từ dưới lên. Mỗi bước sang dòng/khe liền kề tiến một tên nốt tự nhiên.",
                "Count lines and spaces from the bottom. Each adjacent position advances one letter name."),
                tl("Khuông nhạc tự nó chưa xác định tên nốt: phải biết khóa nhạc đặt ở đầu khuông.",
                "The staff alone does not identify pitches; the clef at the start is required."),
                tl("Trên guitar thường đọc khuông khóa Sol; bản nhạc guitar được viết cao hơn âm thực tế một quãng tám.",
                "Guitar usually reads treble clef; guitar notation typically sounds an octave below written pitch.")),
            "Đọc các nốt trên năm dòng khóa Sol", "Read the five treble-clef lines", listOf(listOf(64), listOf(67), listOf(71), listOf(74), listOf(77))),
        f("F02_CLEFS", 0, "Khóa Sol và khóa Fa", "Treble and bass clefs",
            "Khóa nhạc xác định tên nốt", "A clef names the staff pitches",
            "Khóa Sol (𝄞) xác định G4 trên dòng thứ hai từ dưới lên; khóa Fa (𝄢) xác định F3 trên dòng thứ tư từ dưới lên.",
            "Treble clef identifies G4 on the second line; bass clef identifies F3 on the fourth line.",
            listOf(tl("Trên khóa Sol, năm dòng từ dưới lên là E–G–B–D–F.",
                "On treble clef, the five lines from bottom to top are E–G–B–D–F."),
                tl("Trên khóa Fa, năm dòng từ dưới lên là G–B–D–F–A.",
                "On bass clef, the five lines from bottom to top are G–B–D–F–A."),
                tl("Không nên nhầm chữ E trên khóa Sol với E ở dây trầm: tên nốt có thể giống nhưng khác quãng tám.",
                "Do not confuse a written E with low guitar E: matching letters may belong to different octaves.")),
            "So sánh năm dòng của hai khóa", "Compare the five lines in both clefs", listOf(listOf(64),listOf(67),listOf(71),listOf(74),listOf(77))),
        f("F03_STAFF_NOTES", 0, "Nốt trên dòng và trong khe", "Notes on lines and spaces",
            "Ghi nhớ vị trí, không chỉ đọc chữ", "Read staff positions, not only letters",
            "Với khóa Sol, năm dòng là E–G–B–D–F và bốn khe là F–A–C–E, đều tính từ dưới lên.",
            "In treble clef, the lines spell E–G–B–D–F and the spaces F–A–C–E, from bottom to top.",
            listOf(tl("Nốt ở khe nằm chính giữa hai dòng liền nhau; tên nốt liên tiếp không bỏ qua chữ cái.",
                "A space note sits between adjacent lines; letter names advance without skipping."),
                tl("Đọc chậm từ E4 lên F5 rồi đi xuống giúp tạo phản xạ nhận nốt.",
                "Reading slowly from E4 up to F5 and back builds recognition."),
                tl("Cùng vị trí dòng/khe có thể đổi cao độ nếu có dấu hóa hoặc hóa biểu.",
                "A staff position can be altered by an accidental or key signature.")),
            "Các dòng và khe khóa Sol", "Treble clef lines and spaces", listOf(listOf(64),listOf(65),listOf(67),listOf(69),listOf(71),listOf(72),listOf(74),listOf(76),listOf(77))),
        f("F04_LEDGER", 0, "Dòng phụ và quãng tám", "Ledger lines and octaves",
            "Đọc nốt nằm ngoài khuông", "Notes outside the staff",
            "Dòng phụ là các nét ngắn mở rộng khuông nhạc để ghi những nốt quá cao hoặc quá thấp.",
            "Ledger lines extend the staff for pitches above or below its five lines.",
            listOf(tl("C4 (Đô giữa) nằm trên một dòng phụ ngay dưới khuông khóa Sol.",
                "Middle C (C4) sits on one ledger line below the treble staff."),
                tl("Một quãng tám là 12 bán cung và lặp lại tên nốt ở âm vực cao hơn hoặc thấp hơn.",
                "An octave spans 12 semitones and repeats the same note letter at a different register."),
                tl("Trên guitar, hãy tách rõ quãng tám và số ngăn; không suy ra cao độ chỉ từ tên nốt.",
                "On guitar, distinguish octave from fret number; note letters alone are not enough.")),
            "C4, E4, G4 và C5 trên khuông", "C4, E4, G4 and C5 on the staff", listOf(listOf(60),listOf(64),listOf(67),listOf(72))),
        f("F05_VALUES", 1, "Hình nốt và độ dài", "Note symbols and values",
            "Nốt tròn, trắng, đen và móc", "Whole, half, quarter and flagged notes",
            "Nếu lấy nốt đen làm một phách, nốt tròn bằng 4 phách, nốt trắng 2, nốt đen 1, móc đơn 1/2 và móc kép 1/4 phách.",
            "With a quarter-note beat, whole = 4 beats, half = 2, quarter = 1, eighth = 1/2, sixteenth = 1/4.",
            listOf(tl("Nốt tròn không có thân; nốt trắng có thân với đầu rỗng; nốt đen có đầu tô kín.",
                "Whole notes have no stem; half notes have hollow heads and stems; quarter notes have filled heads."),
                tl("Móc đơn có một lá cờ (hoặc một thanh nối), móc kép có hai.",
                "Eighth notes use one flag or beam; sixteenth notes use two."),
                tl("Tỉ lệ độ dài luôn giữ nguyên; số phách cụ thể còn tùy đơn vị phách của bài.",
                "Value ratios stay constant; counted beats depend on the selected beat unit.")),
            "So sánh độ dài nốt trong 4/4", "Compare note values in 4/4", listOf(listOf(64),listOf(64),listOf(64),listOf(64))),
        f("F06_RESTS", 1, "Dấu lặng và khoảng nghỉ", "Rests and silence",
            "Im lặng cũng có trường độ", "Silence also has duration",
            "Dấu lặng cho biết người chơi phải ngừng phát âm trong một khoảng thời gian xác định, nhưng nhịp vẫn tiếp tục.",
            "A rest indicates silence for a measured duration while the pulse continues.",
            listOf(tl("Dấu lặng tròn, trắng, đen, móc đơn và móc kép tương ứng các giá trị trường độ của nốt.",
                "Whole, half, quarter, eighth and sixteenth rests match the corresponding note durations."),
                tl("Trong một ô 4/4, hai phách chơi và hai phách nghỉ vẫn cộng đủ bốn phách.",
                "In a 4/4 bar, two beats played and two beats resting still total four beats."),
                tl("Không tăng tốc để lấp khoảng im lặng; tiếp tục đếm hoặc theo metronome.",
                "Do not rush through silence: keep counting or follow the metronome.")),
            "Nghe nốt xen kẽ khoảng lặng", "Hear notes with spaces between", listOf(listOf(64), emptyList(), listOf(64), emptyList())),
        f("F07_DOTS_TIES", 1, "Chấm dôi, nối và luyến", "Dots, ties and slurs",
            "Ba ký hiệu có chức năng khác nhau", "Three signs with different meanings",
            "Chấm dôi kéo dài nốt thêm một nửa giá trị gốc; dấu nối gộp thời lượng hai nốt cùng cao độ; dấu luyến diễn tả cách nối các nốt khác cao độ.",
            "A dot adds half the original duration; a tie combines equal-pitch note values; a slur joins different pitches in phrasing.",
            listOf(tl("Nốt đen chấm dôi = 1,5 phách đen; nốt trắng chấm dôi = 3 phách đen.",
                "A dotted quarter lasts 1.5 quarter beats; a dotted half lasts three."),
                tl("Hai nốt đen cùng cao độ được nối bằng dấu nối sẽ phát một lần, ngân 2 phách.",
                "Two tied quarter notes of the same pitch sound once for two beats."),
                tl("Với slur trên guitar, có thể dùng hammer-on hoặc pull-off khi phù hợp ngữ cảnh biểu diễn.",
                "On guitar, a slur may indicate hammer-on or pull-off where musically appropriate.")),
            "Đối chiếu trường độ thường và chấm dôi", "Compare plain and dotted durations", listOf(listOf(64),listOf(64))),
        f("F08_METER", 1, "Số chỉ nhịp và phách mạnh", "Time signatures and accents",
            "2/4, 3/4, 4/4 và 6/8", "2/4, 3/4, 4/4 and 6/8",
            "Số trên cho biết lượng đơn vị nốt trong một ô nhịp, số dưới chỉ loại nốt làm đơn vị; cách cảm nhận phách còn phụ thuộc nhịp đơn hay nhịp kép.",
            "The top number counts note-value units per bar, the bottom chooses the unit; felt beats also depend on simple versus compound meter.",
            listOf(tl("3/4 thường cảm nhận ba phách đen, mạnh ở phách 1.",
                "3/4 commonly has three quarter-note beats, with beat 1 accented."),
                tl("6/8 có sáu móc đơn thường nhóm 3 + 3; thường cảm nhận hai phách chấm dôi, không giống 3/4.",
                "6/8 has six eighths grouped 3 + 3, often heard as two dotted-quarter beats rather than 3/4."),
                tl("Tập đếm và gõ phách mạnh trước khi dùng guitar.",
                "Count and tap the main beats before adding the guitar.")),
            "Nghe nhóm 3/4 và 6/8", "Listen to 3/4 and 6/8 grouping", listOf(listOf(69),listOf(64),listOf(64))),
        f("F09_TEMPO", 1, "Nhịp độ (Tempo) và BPM", "Tempo and BPM",
            "BPM đo tốc độ nhịp", "BPM measures beat rate",
            "Tempo là mức nhanh/chậm của bản nhạc. BPM cho biết số phách trong một phút, với đơn vị phách cần được xác định.",
            "Tempo controls musical speed. BPM is beats per minute, provided the beat unit is defined.",
            listOf(tl("60 BPM tương ứng một phách mỗi giây; 120 BPM tương ứng nửa giây mỗi phách.",
                "At 60 BPM a beat lasts one second; at 120 BPM it lasts half a second."),
                tl("Trong 6/8, metronome có thể đánh vào phách móc đơn hoặc phách lớn chấm dôi; phải đọc đơn vị tempo.",
                "In 6/8 the click may mark eighths or dotted-quarter main beats; check the tempo unit."),
                tl("Khi luyện, tăng tốc dần khi đã giữ đúng nhịp, không cố chơi nhanh trước.",
                "Raise the tempo only after accuracy and steadiness are reliable.")),
            "So sánh nhịp đều và phách chia đôi", "Hear steady beats and subdivisions", listOf(listOf(64),listOf(64),listOf(64),listOf(64))),
        f("F10_STEPS", 2, "Cung và nửa cung", "Whole steps and semitones",
            "Cấu trúc khoảng cách trên piano và guitar", "Distances on piano and guitar",
            "Một bán cung là khoảng cách giữa hai phím piano liền kề hoặc hai ngăn liên tiếp trên cùng dây guitar. Một cung bằng hai bán cung.",
            "A semitone is the distance between adjacent piano keys or guitar frets on one string. A whole tone equals two semitones.",
            listOf(tl("E–F và B–C cách nhau một bán cung dù không có phím đen ở giữa.",
                "E–F and B–C are semitones even with no black key between them."),
                tl("C–D là một cung; C–C♯ là nửa cung.",
                "C–D is a whole tone; C–C♯ is a semitone."),
                tl("Bấm cùng dây từ ngăn 1 sang ngăn 3 nâng lên một cung.",
                "On the same string, moving from fret 1 to fret 3 raises the pitch a whole tone.")),
            "Chạm các cặp nốt và nghe độ xa", "Listen to the pitch distance", listOf(listOf(60),listOf(61))),
        f("F11_ACCIDENTALS", 2, "Dấu thăng, giáng và bình", "Sharps, flats and naturals",
            "♯ tăng, ♭ giảm, ♮ hủy dấu", "♯ raises, ♭ lowers, ♮ cancels",
            "Dấu thăng nâng cao độ lên một bán cung; dấu giáng hạ một bán cung; dấu bình khôi phục cao độ không bị biến đổi bởi dấu hóa.",
            "A sharp raises a note one semitone, a flat lowers it one, and a natural cancels its previous alteration.",
            listOf(tl("C♯ và D♭ có cùng cao độ trong hệ lên dây bình quân 12 bán cung, nhưng tên viết phụ thuộc nhạc lý.",
                "C♯ and D♭ sound alike in 12-tone equal temperament, but their spelling has different functions."),
                tl("Hóa biểu ở đầu khuông áp dụng trong toàn bài cho đến khi thay đổi; dấu hóa bất thường thường có hiệu lực trong ô nhịp theo quy ước ký âm.",
                "A key signature applies until changed; local accidentals usually apply within the measure under notation conventions."),
                tl("Dấu bình ♮ không phải dấu thăng hoặc giáng ngược chiều: nó trả về nốt tự nhiên.",
                "A natural sign restores the natural pitch, not an arbitrary transposition.")),
            "So sánh C, C♯ và D♭", "Compare C, C♯ and D♭", listOf(listOf(60),listOf(61),listOf(61))),
        f("F12_INTERVALS", 2, "Quãng âm và cách đếm", "Intervals and counting",
            "Đếm số tên nốt rồi đo bán cung", "Count letter names, then semitones",
            "Tên quãng được xác định bằng số bậc chữ cái tính cả hai đầu; tính chất (trưởng, thứ, đúng, tăng, giảm) phụ thuộc khoảng cách bán cung.",
            "Interval number counts staff letters inclusively; quality (major, minor, perfect, augmented, diminished) depends on semitones.",
            listOf(tl("C–E là quãng 3 trưởng, bốn bán cung; C–E♭ là quãng 3 thứ, ba bán cung.",
                "C–E is a major third of four semitones; C–E♭ is a minor third of three."),
                tl("C–G là quãng 5 đúng, bảy bán cung; C4–C5 là quãng 8 đúng, mười hai bán cung.",
                "C–G is a perfect fifth of seven semitones; C4–C5 a perfect octave of twelve."),
                tl("Hai nốt có cùng cao độ nghe được nhưng viết khác tên có thể tạo các loại quãng khác nhau về lý thuyết.",
                "Enharmonically equal pitches may represent different written intervals.")),
            "Nghe và so sánh quãng 3, 5, 8", "Hear thirds, fifths and octaves", listOf(listOf(60),listOf(64))),
        f("F13_MAJOR", 3, "Cấu tạo hợp âm trưởng", "Building major chords",
            "Bậc 1–3–5", "Degrees 1–3–5",
            "Hợp âm ba trưởng gồm nốt gốc, quãng 3 trưởng và quãng 5 đúng. Trong giọng C: C–E–G.",
            "A major triad contains root, major third and perfect fifth. In C: C–E–G.",
            listOf(tl("Từ nốt gốc lên bậc 3 là 4 bán cung; từ gốc lên bậc 5 là 7 bán cung.",
                "The third is four semitones above the root; the fifth is seven."),
                tl("Nốt bậc 3 trưởng giúp tạo tính chất trưởng; nốt gốc cho biết tên hợp âm.",
                "The major third determines the major quality; the root names the chord."),
                tl("Dùng sơ đồ bấm C, G hoặc F để nhận diện các nốt hợp âm trên dây đàn.",
                "Use C, G or F shapes to locate chord tones on guitar strings.")),
            "Nghe C trưởng C–E–G", "Hear C major: C–E–G", listOf(listOf(60,64,67))),
        f("F14_MINOR", 3, "Cấu tạo hợp âm thứ", "Building minor chords",
            "Bậc 1–♭3–5", "Degrees 1–♭3–5",
            "Hợp âm ba thứ dùng nốt gốc, quãng 3 thứ và quãng 5 đúng. C thứ là C–E♭–G, khác C trưởng ở nốt bậc 3.",
            "A minor triad uses root, minor third and perfect fifth. C minor is C–E♭–G, differing from C major at the third.",
            listOf(tl("Quãng 3 thứ cách gốc ba bán cung; quãng 5 đúng vẫn cách gốc bảy bán cung.",
                "The minor third lies three semitones above the root; the perfect fifth remains seven."),
                tl("Hãy so C–E–G và C–E♭–G để nghe sự khác biệt; không phải mọi nhạc trưởng đều vui hoặc nhạc thứ đều buồn.",
                "Compare C–E–G and C–E♭–G; major is not always happy and minor is not always sad."),
                tl("A thứ gồm A–C–E, là hợp âm cơ bản rất phổ biến trên guitar.",
                "A minor has A–C–E and is a common open guitar chord.")),
            "Nghe C trưởng và C thứ", "Hear C major and C minor", listOf(listOf(60,64,67), listOf(60,63,67))),
        f("F15_CHORD7", 3, "Hợp âm 7 và hợp âm thông dụng", "Seventh and common chords",
            "Triad cộng thêm bậc 7", "Add a seventh to a triad",
            "Hợp âm 7 có bốn nốt cơ bản: 1–3–5–7 (maj7), 1–3–5–♭7 (dominant 7), hoặc 1–♭3–5–♭7 (m7).",
            "Seventh chords add a fourth tone: 1–3–5–7 (maj7), 1–3–5–♭7 (dominant 7), or 1–♭3–5–♭7 (m7).",
            listOf(tl("Cmaj7 = C–E–G–B; C7 = C–E–G–B♭; Cm7 = C–E♭–G–B♭.",
                "Cmaj7 = C–E–G–B; C7 = C–E–G–B♭; Cm7 = C–E♭–G–B♭."),
                tl("Tên G7 không có nghĩa là Gmaj7: G7 là dominant 7 với nốt F, không phải F♯.",
                "G7 is not Gmaj7: dominant G7 has F rather than F♯."),
                tl("Các hợp âm C, F, G, Am, Dm, Em và G7 thường xuất hiện trong vòng đệm cơ bản giọng C.",
                "C, F, G, Am, Dm, Em and G7 are common in simple C-major accompaniment.")),
            "Nghe sự khác nhau của maj7, 7 và m7", "Compare major-7, dominant-7 and minor-7", listOf(listOf(60,64,67,71),listOf(60,64,67,70),listOf(60,63,67,70))),
        f("F16_CHORD_DIAGRAM", 3, "Đọc sơ đồ hợp âm guitar", "Reading guitar chord charts",
            "Dây, ngăn, dấu X/O và số ngón", "Strings, frets, X/O and finger numbers",
            "Trong sơ đồ hợp âm đứng, sáu đường dọc là sáu dây guitar; đường ngang là các ngăn. Chấm tròn thể hiện vị trí bấm.",
            "A vertical chord diagram has six strings and horizontal fret lines. Dots indicate fretted positions.",
            listOf(tl("O ở phía trên nghĩa là gảy dây buông; X nghĩa là không gảy dây đó.",
                "O above a string means play it open; X means mute or avoid that string."),
                tl("Các số 1–4 cạnh chấm thường là ngón trỏ, giữa, áp út và út, không phải số ngăn.",
                "Numbers 1–4 beside dots indicate fingers, not fret numbers."),
                tl("Sơ đồ C mở thường ghi X–3–2–0–1–0 từ dây 6 đến dây 1.",
                "Open C major is commonly written X–3–2–0–1–0 from strings 6 to 1.")),
            "Đối chiếu sơ đồ C, Am và G7", "Compare C, Am and G7 diagrams", listOf(listOf(48,52,55),listOf(45,48,52),listOf(43,47,50,53))),
        f("F17_FRETBOARD", 4, "Cần đàn, sáu dây và số ngăn", "Fretboard, six strings and frets",
            "Đọc dây 6→1 và vị trí bấm", "Read strings 6→1 and fret positions",
            "Trong lên dây chuẩn, sáu dây từ trầm đến cao là E2–A2–D3–G3–B3–E4. Mỗi lần tăng một ngăn trên cùng dây làm cao độ tăng một bán cung.",
            "In standard tuning, strings low-to-high are E2–A2–D3–G3–B3–E4. Each fret raises a string one semitone.",
            listOf(tl("Dây 6 là dây dày nhất; dây 1 là dây mảnh nhất. Hình TAB thường xếp dây 1 ở trên cùng.",
                "String 6 is thickest and string 1 is thinnest. TAB usually places string 1 at the top."),
                tl("Số 0 trên TAB là dây buông, số 1 là ngăn 1; số 12 thường tương ứng cao hơn dây buông một quãng tám.",
                "TAB 0 means open string and 1 means fret 1; fret 12 is usually an octave above the open string."),
                tl("Để đọc đúng âm, phải biết cả tên dây và số ngăn chứ không chỉ nhìn chữ trên TAB.",
                "To identify a pitch, use both the string and fret number.")),
            "Nghe vị trí dây 1 tại ngăn 0, 1, 3", "Hear string 1 at frets 0, 1, 3", listOf(listOf(64),listOf(65),listOf(67))),
        f("F18_TAB", 4, "Cách đọc TAB và ký hiệu kỹ thuật", "Reading TAB and technique signs",
            "Sáu dòng đại diện cho sáu dây", "Six lines represent six strings",
            "TAB ghi số ngăn trực tiếp trên sáu dòng: dòng trên cùng thường là dây 1 (e cao), dòng dưới cùng là dây 6 (E trầm).",
            "TAB writes fret numbers on six lines: the top line is usually string 1 (high e), and the bottom string 6 (low E).",
            listOf(tl("Đọc các số từ trái sang phải. Số trên cùng một cột ở nhiều dây có thể biểu thị nốt cùng lúc (hợp âm).",
                "Read numbers left to right. Vertically aligned numbers on multiple strings may be played together as a chord."),
                tl("Ký hiệu h thường là hammer-on, p là pull-off, / và \\ chỉ slide; b thường dùng cho bend.",
                "h commonly means hammer-on, p pull-off, / and \\ slide, and b bend."),
                tl("Không phải mọi TAB đều ghi chính xác trường độ; khi có khuông nhạc kèm theo, hãy đọc cả hai.",
                "Not all TAB provides complete rhythms; read accompanying notation when available.")),
            "Đọc e|--0--1--3-- tương ứng E–F–G", "Read e|--0--1--3-- as E–F–G", listOf(listOf(64),listOf(65),listOf(67)))
    )
}

private object TheoryLearningProgressStore {
    private const val PREF_NAME = "tuner_tools_theory_academy_v1"
    fun completedTopics(context: android.content.Context): Set<String> =
        context.getSharedPreferences(PREF_NAME, android.content.Context.MODE_PRIVATE)
            .getStringSet("read_topics", emptySet<String>())?.toSet().orEmpty()

    fun markRead(context: android.content.Context, id: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, android.content.Context.MODE_PRIVATE)
        val saved = prefs.getStringSet("read_topics", emptySet<String>()).orEmpty().toMutableSet()
        prefs.edit().putStringSet("read_topics", saved + id).apply()
    }

    fun bestScore(context: android.content.Context, stage: Int): Int =
        context.getSharedPreferences(PREF_NAME, android.content.Context.MODE_PRIVATE)
            .getInt("best_quiz_$stage", -1)

    fun recordQuiz(context: android.content.Context, stage: Int, score: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("best_quiz_$stage", maxOf(score, bestScore(context, stage)))
            .putInt("quiz_attempts_$stage", prefs.getInt("quiz_attempts_$stage", 0) + 1)
            .apply()
    }
}

private fun theoryQuizForStage(topics: List<TheoryTopic>, stage: Int): List<TheoryQuestion> {
    val inStage = topics.filter { it.stage == stage }
    // Cover each topic before drawing additional questions (especially stage 5).
    return (inStage.mapNotNull { it.questions.firstOrNull() } +
        inStage.flatMap { it.questions.drop(1) }).take(5)
}

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
    HAND_SHAPES,
    HARMONY
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
        descriptionVi = "Gốc trên dây 6, dây 3 và dây 1; vùng C trưởng mẫu nằm ở ngăn 5-8",
        descriptionEn = "Roots on strings 6, 3 and 1; the C-major reference box spans frets 5-8",
        rootString = 6,
        lowerFretOffsetFromRoot = 3,
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
        titleVi = "Ngũ cung dominant (có 4)",
        titleEn = "Dominant pentatonic (with 4)",
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
        // Keep key RITUSEN for saved preferences. This is the pentatonic-mode
        // convention used in the supplied reference app, NOT dominant 1-3-4-5-b7.
        titleVi = "Mixolydian ngũ cung (Ritusen / Yo)",
        titleEn = "Mixolydian pentatonic (Ritusen / Yo)",
        formula = "1 · 2 · 4 · 5 · 6",
        groupKey = "MODAL_PENTATONIC",
        summaryVi = "Dạng ngũ cung thức Ritusen/Yo: 1–2–4–5–6. Khi G là nốt chủ, tập nốt G–A–C–D–E trùng với C ngũ cung trưởng, nên khuôn C mở vẫn hợp lệ; G vẫn là nốt gốc (R). Không đồng nhất với ngũ cung dominant 1–3–4–5–♭7.",
        summaryEn = "Ritusen/Yo pentatonic mode: 1–2–4–5–6. With G as tonic, G–A–C–D–E shares the pitches of C major pentatonic, so its open C-reference shape is valid while G stays the root (R). This differs from dominant 1–3–4–5–b7.",
        usageVi = "Dùng màu sus/ngũ cung mở; trong G, chọn Thế C tham chiếu C trưởng sẽ thấy dây buông–ngăn 4, nhưng phải nhấn G làm nốt chủ.",
        usageEn = "Use for open/suspended pentatonic sounds. In G, the C-major reference shape occupies open strings through fret 4; keep G as the tonal centre.",
        degreeLabels = listOf("1", "2", "4", "5", "6"),
        intervals = listOf(0, 2, 5, 7, 9),
        usesMajorCagedFingering = true,
        characteristicIntervals = listOf(7),
        levelKey = "BASIC"
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

private enum class HandShapeVisualMode(
    val key: String,
    val titleVi: String,
    val titleEn: String
) {
    SCALE_CHARACTER(
        key = "SCALE_CHARACTER",
        titleVi = "Đặc trưng",
        titleEn = "Scale colour"
    ),
    HARMONIC_ROLE(
        key = "HARMONIC_ROLE",
        titleVi = "Hòa âm",
        titleEn = "Harmony role"
    );

    fun title(lang: String): String = fingerUiText(lang, titleVi, titleEn)
}

private enum class HandShapeHarmonyRole {
    CHORD_TONE,
    PENTATONIC_TONE,
    TENSION_TONE,
    HIGH_TENSION
}

private data class HandShapeHarmonyProfile(
    val chordIntervals: Set<Int>,
    val pentatonicIntervals: Set<Int>,
    val highTensionIntervals: Set<Int>
)

/**
 * Builds a practical tonic-harmony profile for the selected scale.
 *
 * The colour model follows the teaching approach shown in the user's reference:
 * - chord tones: the tonic triad (1, 3/♭3, 5/♭5/#5)
 * - pentatonic tones: the non-triad notes from the matching major/minor pentatonic
 * - tension tones: remaining scale colours
 * - high tension: a note one semitone above a chord tone, plus explicit blue notes
 *
 * This is intentionally a learning aid relative to the tonic triad, not a claim that
 * every advanced scale has only one possible chord-scale interpretation.
 */
private fun HandShapeScaleType.harmonyProfile(): HandShapeHarmonyProfile {
    val scaleIntervals = intervals.toSet()

    val third = when {
        4 in scaleIntervals -> 4
        3 in scaleIntervals -> 3
        else -> null
    }
    val fifth = when {
        7 in scaleIntervals -> 7
        6 in scaleIntervals -> 6
        8 in scaleIntervals -> 8
        else -> null
    }

    val chordIntervals = mutableSetOf(0).apply {
        if (third != null) add(third)
        if (fifth != null) add(fifth)
    }.intersect(scaleIntervals)

    val dominantFamilyKeys = setOf(
        "MIXOLYDIAN",
        "MIXOLYDIAN_PENTATONIC",
        "DOMINANT_PENTATONIC",
        "LYDIAN_DOMINANT",
        "PHRYGIAN_DOMINANT",
        "MIXOLYDIAN_FLAT_6",
        "ALTERED",
        "DIMINISHED_HALF_WHOLE",
        "DOMINANT_BEBOP"
    )

    val pentatonicTemplate = when {
        key in dominantFamilyKeys && third == 4 -> setOf(0, 2, 4, 7, 10)
        third == 4 -> setOf(0, 2, 4, 7, 9)
        third == 3 -> setOf(0, 3, 5, 7, 10)
        else -> scaleIntervals
    }

    val pentatonicIntervals = pentatonicTemplate
        .intersect(scaleIntervals)
        .minus(chordIntervals)

    val protectedSharpFourKeys = setOf(
        "LYDIAN",
        "LYDIAN_AUGMENTED",
        "LYDIAN_DOMINANT",
        "WHOLE_TONE"
    )

    val highTensionIntervals = scaleIntervals
        .minus(chordIntervals)
        .filterTo(linkedSetOf()) { interval ->
            val semitoneAboveChordTone = chordIntervals.any { chordInterval ->
                ((interval - chordInterval) % 12 + 12) % 12 == 1
            }
            val explicitBlueNote = when (key) {
                "MAJOR_BLUES" -> interval == 3
                "MINOR_BLUES" -> interval == 6
                else -> false
            }
            val exposedTritone = interval == 6 &&
                    interval !in pentatonicIntervals &&
                    key !in protectedSharpFourKeys

            explicitBlueNote || semitoneAboveChordTone || exposedTritone
        }

    return HandShapeHarmonyProfile(
        chordIntervals = chordIntervals,
        // A nominal pentatonic note can still clash with an altered tonic triad.
        // In that case the stronger harmonic warning takes priority.
        pentatonicIntervals = pentatonicIntervals.minus(highTensionIntervals),
        highTensionIntervals = highTensionIntervals
    )
}

private fun HandShapeHarmonyProfile.roleForInterval(interval: Int): HandShapeHarmonyRole =
    when (interval) {
        in chordIntervals -> HandShapeHarmonyRole.CHORD_TONE
        in pentatonicIntervals -> HandShapeHarmonyRole.PENTATONIC_TONE
        in highTensionIntervals -> HandShapeHarmonyRole.HIGH_TENSION
        else -> HandShapeHarmonyRole.TENSION_TONE
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

private enum class HarmonyWorkspaceTab(val key: String) {
    PROGRESSION("PROGRESSION"),
    CHORD_MELODY("CHORD_MELODY")
}

private enum class HarmonyLibraryTab(val key: String) {
    PRESET("PRESET"),
    MY_LOOP("MY_LOOP")
}

private data class PassingChordSuggestion(
    val key: String,
    val chord: HarmonyChord,
    val typeVi: String,
    val typeEn: String,
    val explanationVi: String,
    val explanationEn: String,
    val bassLine: String,
    val difficultyVi: String,
    val difficultyEn: String
) {
    fun type(lang: String): String = fingerUiText(lang, typeVi, typeEn)
    fun explanation(lang: String): String = fingerUiText(lang, explanationVi, explanationEn)
    fun difficulty(lang: String): String = fingerUiText(lang, difficultyVi, difficultyEn)
}

private fun harmonyDegreeSpecFor(
    tonality: HarmonyTonality,
    degree: Int
): HarmonyDegreeSpec {
    val safeDegree = degree.coerceIn(1, 7)
    val majorRomans = listOf("I", "ii", "iii", "IV", "V", "vi", "vii°")
    val minorRomans = listOf("i", "iiø", "III", "iv", "V", "VI", "VII")
    val majorTriads = listOf(
        HarmonyChordQuality.MAJOR,
        HarmonyChordQuality.MINOR,
        HarmonyChordQuality.MINOR,
        HarmonyChordQuality.MAJOR,
        HarmonyChordQuality.MAJOR,
        HarmonyChordQuality.MINOR,
        HarmonyChordQuality.DIMINISHED
    )
    val majorSevenths = listOf(
        HarmonyChordQuality.MAJ7,
        HarmonyChordQuality.MIN7,
        HarmonyChordQuality.MIN7,
        HarmonyChordQuality.MAJ7,
        HarmonyChordQuality.DOM7,
        HarmonyChordQuality.MIN7,
        HarmonyChordQuality.HALF_DIM
    )
    // The minor template uses a major/dominant V so the cadence resolves clearly.
    val minorTriads = listOf(
        HarmonyChordQuality.MINOR,
        HarmonyChordQuality.DIMINISHED,
        HarmonyChordQuality.MAJOR,
        HarmonyChordQuality.MINOR,
        HarmonyChordQuality.MAJOR,
        HarmonyChordQuality.MAJOR,
        HarmonyChordQuality.MAJOR
    )
    val minorSevenths = listOf(
        HarmonyChordQuality.MIN7,
        HarmonyChordQuality.HALF_DIM,
        HarmonyChordQuality.MAJ7,
        HarmonyChordQuality.MIN7,
        HarmonyChordQuality.DOM7,
        HarmonyChordQuality.MAJ7,
        HarmonyChordQuality.DOM7
    )
    val index = safeDegree - 1
    return if (tonality == HarmonyTonality.MAJOR) {
        HarmonyDegreeSpec(
            degree = safeDegree,
            roman = majorRomans[index],
            triadQuality = majorTriads[index],
            seventhQuality = majorSevenths[index]
        )
    } else {
        HarmonyDegreeSpec(
            degree = safeDegree,
            roman = minorRomans[index],
            triadQuality = minorTriads[index],
            seventhQuality = minorSevenths[index]
        )
    }
}

private fun buildCustomHarmonyProgression(
    tonality: HarmonyTonality,
    degrees: List<Int>
): HarmonyProgression {
    val normalized = degrees.map { it.coerceIn(1, 7) }.ifEmpty { listOf(1, 5, 6, 4) }
    return HarmonyProgression(
        key = "CUSTOM_${tonality.name}_${normalized.joinToString("_")}",
        titleVi = "Vòng của tôi",
        titleEn = "My progression",
        descriptionVi = "Vòng tự tạo được lưu tự động theo bậc La Mã và có thể chuyển sang mọi tông.",
        descriptionEn = "A custom Roman-numeral loop that is saved automatically and transposes to every key.",
        tonality = tonality,
        degrees = normalized.map { harmonyDegreeSpecFor(tonality, it) }
    )
}

/** Playable, voice-led guitar voicings for one passing-chord demonstration.
 * Never synthesize an artificial 4-note root-position stack that doesn't match
 * the fingering shown on the current fretboard.
 */
private fun passingChordPreviewVoicings(
    fromChord: HarmonyChord,
    suggestion: PassingChordSuggestion,
    toChord: HarmonyChord,
    stringSet: HarmonyStringSet,
    practiceNoteCount: Int,
    fretRange: IntRange
): List<List<Int>> {
    val targetCount = if (practiceNoteCount <= 3) 3 else 4
    val previewSet = if (stringSet.strings.size == targetCount) stringSet else
        harmonyStringSets(if (targetCount == 3) HarmonyPracticeType.TRIAD else HarmonyPracticeType.SEVENTH_ARPEGGIO).first()
    fun compact(chord: HarmonyChord): HarmonyChord {
        val intervals = when {
            targetCount == 3 && chord.quality == HarmonyChordQuality.DOM7 -> listOf(0, 4, 10)
            targetCount == 3 && chord.quality == HarmonyChordQuality.DIM7 -> listOf(0, 3, 6)
            else -> chord.quality.intervals.take(targetCount)
        }
        return chord.copy(intervals = intervals)
    }
    val previewChords = listOf(fromChord, suggestion.chord, toChord).map(::compact)
    val voicings = buildHarmonyVoicings(
        chords = previewChords,
        stringSet = previewSet,
        fretRange = fretRange,
        inversionMode = HarmonyInversionMode.AUTO
    )
    return if (voicings.size == 3 && voicings.all { it.notes.isNotEmpty() }) {
        voicings.map { voicing -> voicing.notes.map { it.midi }.distinct().sorted() }
    } else {
        // Controlled fallback for unusual custom chords or fret constraints.
        previewChords.map { chord ->
            val rootMidi = 48 + chord.rootPitchClass
            chord.intervals.map { interval -> rootMidi + interval }
        }
    }
}

private fun makeExplicitHarmonyChord(
    rootPitchClass: Int,
    quality: HarmonyChordQuality,
    roman: String,
    index: Int = 0,
    displayRootName: String? = null
): HarmonyChord {
    val normalizedRoot = normalizedPitchClass(rootPitchClass)
    val rootName = displayRootName ?: harmonyNoteNames[normalizedRoot]
    return HarmonyChord(
        index = index,
        roman = roman,
        rootName = rootName,
        rootPitchClass = normalizedRoot,
        quality = quality,
        symbol = rootName + quality.suffix,
        intervals = quality.intervals
    )
}

private fun passingChordSuggestions(
    fromChord: HarmonyChord,
    toChord: HarmonyChord,
    gapIndex: Int
): List<PassingChordSuggestion> {
    val targetRoot = toChord.rootPitchClass
    val sourceRoot = fromChord.rootPitchClass
    val suggestions = mutableListOf<PassingChordSuggestion>()

    val secondaryRoot = normalizedPitchClass(targetRoot + 7)
    val secondary = makeExplicitHarmonyChord(
        rootPitchClass = secondaryRoot,
        quality = HarmonyChordQuality.DOM7,
        roman = "V7/${toChord.roman}",
        index = gapIndex
    )
    suggestions += PassingChordSuggestion(
        key = "SECONDARY_${gapIndex}_${secondary.rootPitchClass}",
        chord = secondary,
        typeVi = "Dominant phụ",
        typeEn = "Secondary dominant",
        explanationVi = "Dominant của hợp âm đích; bậc 3 và bậc 7 tạo lực hút rõ về ${toChord.symbol}.",
        explanationEn = "The dominant of the target chord; its 3rd and 7th pull clearly into ${toChord.symbol}.",
        bassLine = "${fromChord.rootName} → ${secondary.rootName} → ${toChord.rootName}",
        difficultyVi = "Dễ áp dụng",
        difficultyEn = "Easy to apply"
    )

    // A diminished leading-tone chord is built on the semitone BELOW the destination.
    // The old rule chose a semitone above the source (Dm -> D#dim7 -> G),
    // which did not resolve by semitone into G as the explanation claimed.
    val leadingToneRoot = normalizedPitchClass(targetRoot - 1)
    if (leadingToneRoot != sourceRoot) {
        val diminished = makeExplicitHarmonyChord(
            rootPitchClass = leadingToneRoot,
            quality = HarmonyChordQuality.DIM7,
            roman = "vii°7/${toChord.roman}",
            index = gapIndex,
            displayRootName = harmonyNoteNames[leadingToneRoot]
        )
        suggestions += PassingChordSuggestion(
            key = "LEADING_DIM_${gapIndex}_${diminished.rootPitchClass}",
            chord = diminished,
            typeVi = "Diminished dẫn về đích",
            typeEn = "Leading-tone diminished",
            explanationVi = "Hợp âm diminished có nốt gốc thấp hơn ${toChord.rootName} nửa cung; nốt dẫn tiến lên nốt gốc hợp âm đích.",
            explanationEn = "The diminished chord root lies a semitone below ${toChord.rootName}, resolving upward into the destination root.",
            bassLine = "${fromChord.rootName} → ${diminished.rootName} → ${toChord.rootName}",
            difficultyVi = "Trung bình",
            difficultyEn = "Intermediate"
        )
    }

    val tritoneRoot = normalizedPitchClass(targetRoot + 1)
    val tritone = makeExplicitHarmonyChord(
        rootPitchClass = tritoneRoot,
        quality = HarmonyChordQuality.DOM7,
        roman = "subV/${toChord.roman}",
        index = gapIndex,
        displayRootName = harmonyFlatNoteNames[tritoneRoot]
    )
    suggestions += PassingChordSuggestion(
        key = "TRITONE_${gapIndex}_${tritone.rootPitchClass}",
        chord = tritone,
        typeVi = "Thay thế tritone",
        typeEn = "Tritone substitution",
        explanationVi = "Dominant cách hợp âm đích nửa cung, phù hợp Jazz và tạo bass chromatic mạnh.",
        explanationEn = "A dominant a semitone above the target, useful in jazz with a strong chromatic bass line.",
        bassLine = "${fromChord.rootName} → ${tritone.rootName} → ${toChord.rootName}",
        difficultyVi = "Nâng cao",
        difficultyEn = "Advanced"
    )

    return suggestions.distinctBy { it.chord.symbol }
}

private fun insertPassingChords(
    baseChords: List<HarmonyChord>,
    appliedByGap: Map<Int, PassingChordSuggestion>,
    practiceNoteCount: Int
): List<HarmonyChord> {
    if (baseChords.isEmpty()) return emptyList()
    val result = mutableListOf<HarmonyChord>()
    baseChords.forEachIndexed { index, chord ->
        result += chord
        if (index < baseChords.lastIndex) {
            appliedByGap[index]?.let { suggestion ->
                val adaptedIntervals = when {
                    practiceNoteCount <= 3 && suggestion.chord.quality == HarmonyChordQuality.DOM7 ->
                        listOf(0, 4, 10) // compact dominant shell: root, 3rd, b7
                    practiceNoteCount <= 3 && suggestion.chord.quality == HarmonyChordQuality.DIM7 ->
                        listOf(0, 3, 6)
                    else -> suggestion.chord.intervals
                }
                result += suggestion.chord.copy(intervals = adaptedIntervals)
            }
        }
    }
    return result.mapIndexed { index, chord -> chord.copy(index = index) }
}

private fun buildChordMelodyVoicings(
    chords: List<HarmonyChord>,
    stringSet: HarmonyStringSet,
    fretRange: IntRange,
    melodyToneIndexes: List<Int>
): List<HarmonyVoicing> {
    val result = mutableListOf<HarmonyVoicing>()
    var previous: HarmonyVoicing? = null
    chords.forEachIndexed { chordIndex, chord ->
        val targetToneIndex = melodyToneIndexes.getOrElse(chordIndex) { 1 }
            .coerceIn(0, chord.intervals.lastIndex.coerceAtLeast(0))
        val targetPitchClass = normalizedPitchClass(
            chord.rootPitchClass + chord.intervals.getOrElse(targetToneIndex) { 0 }
        )
        val ranges = listOf(
            fretRange,
            maxOf(0, fretRange.first - 2)..minOf(20, fretRange.last + 2),
            maxOf(0, fretRange.first - 5)..minOf(20, fretRange.last + 5),
            0..20
        ).distinct()
        var selected: HarmonyVoicing? = null
        for (range in ranges) {
            val candidates = buildHarmonyVoicingCandidates(chord, stringSet, range)
                .filter { voicing ->
                    voicing.notes.maxByOrNull { it.midi }?.pitchClass == targetPitchClass
                }
            selected = selectHarmonyVoicing(
                candidates = candidates,
                previous = previous,
                fretRange = fretRange,
                inversionMode = HarmonyInversionMode.AUTO
            )
            if (selected != null) break
        }
        val resolved = selected ?: buildHarmonyVoicings(
            chords = listOf(chord),
            stringSet = stringSet,
            fretRange = fretRange,
            inversionMode = HarmonyInversionMode.AUTO
        ).firstOrNull()
        if (resolved != null) {
            result += resolved
            previous = resolved
        }
    }
    return result
}

private fun buildChordMelodyPlaybackSequence(
    voicings: List<HarmonyVoicing>,
    beatsPerChord: Int
): List<HarmonyPlaybackStep> {
    val safeBeats = beatsPerChord.coerceIn(1, 8)
    return voicings.flatMapIndexed { chordIndex, voicing ->
        val melodyNote = voicing.notes.maxByOrNull { it.midi }
            ?: return@flatMapIndexed emptyList()
        (1..safeBeats).map { beat ->
            HarmonyPlaybackStep(
                chordIndex = chordIndex,
                noteIndex = voicing.notes.indexOf(melodyNote).coerceAtLeast(0),
                note = melodyNote,
                voicing = voicing,
                slotIndex = beat - 1,
                beatNumber = beat,
                subdivisionIndex = 0,
                stepsPerBeat = 1,
                pickSymbol = if (beat == 1) "◆" else "·"
            )
        }
    }
}

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
private val harmonyFlatNoteNames = listOf("C", "D♭", "D", "E♭", "E", "F", "G♭", "G", "A♭", "A", "B♭", "B")

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
     * A, E và D cho phép dùng thế mở khi dây gốc là dây buông.
     * Dạng G có nốt gốc trên dây 6 ngăn 3, vùng ngăn 0-3 là thế G mở hợp lệ.
     * Chỉ dịch lên một quãng tám khi vùng ngăn thực sự nằm dưới nut.
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

/**
 * CAGED reference key is NOT always the tonal centre of a pentatonic mode.
 * Ritusen/Yo = 1-2-4-5-6; it is the fourth-degree rotation of the same
 * pitch collection as the major pentatonic whose tonic is a fourth above.
 * Example: G Ritusen = G A C D E = C major pentatonic; its C-shape is open.
 * Other scales keep their existing tonic-based CAGED behaviour unchanged.
 */
private fun handShapeReferenceTonicKey(
    tonicName: String,
    scaleType: HandShapeScaleType
): String {
    val tonic = normalizeHandShapeTonicKey(tonicName)
    if (scaleType != HandShapeScaleType.RITUSEN) return tonic
    val referencePc = normalizedPitchClass(tonicPitchClass(tonic) + 5)
    return handShapeTonicOptions.first { it.pitchClass == referencePc }.key
}

/**
 * Position learning is separate from scale identity. CAGED keeps its original
 * reference-chord positions; 3NPS uses the seven starting degrees on string 6.
 * The full-neck view is a reference view, not another audio exercise engine.
 */
private enum class HandPositionLearningMode { CAGED, THREE_NPS }

private fun isThreeNotesPerStringScale(scale: HandShapeScaleType): Boolean =
    scale.intervals.size == 7 &&
        scale.intervals.distinct().size == 7 &&
        scale.intervals.all { it in 0..11 } &&
        scale.intervals.zipWithNext().all { (a, b) -> a < b }

/**
 * Find 7 valid 3NPS starting positions along the lowest E string (frets 0..11).
 * Numbered left-to-right on the neck; the first one is not necessarily a root.
 */
private fun threeNpsStartingFrets(tonicName: String, scaleType: HandShapeScaleType): List<Int> {
    if (!isThreeNotesPerStringScale(scaleType)) return emptyList()
    val rootPc = tonicPitchClass(tonicName)
    return (0..11).filter { fret ->
        normalizedPitchClass(guitarOpenMidiByString.getValue(6) + fret - rootPc) in scaleType.intervals
    }
}

/**
 * Each selected position walks UP the scale, three successive pitches per
 * string, from low E to high E. Fretting follows the standard E A D G B E
 * tuning; crucially the B/G tuning break is not treated as a perfect fourth.
 * Open strings count as playable notes and don't require a left-hand finger.
 */
private fun buildThreeNotesPerStringPattern(
    tonicName: String,
    scaleType: HandShapeScaleType,
    positionIndex: Int
): HandShapePattern {
    require(isThreeNotesPerStringScale(scaleType)) { "3NPS requires seven distinct scale tones" }
    val startPositions = threeNpsStartingFrets(tonicName, scaleType)
    require(startPositions.size == 7) { "Expected seven 3NPS starting positions" }
    val tonicPc = tonicPitchClass(tonicName)
    val notes = mutableListOf<HandShapeNote>()
    var previousMidi = guitarOpenMidiByString.getValue(6) + startPositions[positionIndex.coerceIn(0, 6)] - 1
    for (stringNumber in 6 downTo 1) {
        val openMidi = guitarOpenMidiByString.getValue(stringNumber)
        val minFret = maxOf(0, previousMidi - openMidi + 1)
        val frets = (minFret..24).filter { fret ->
            normalizedPitchClass(openMidi + fret - tonicPc) in scaleType.intervals
        }.take(3)
        require(frets.size == 3) { "3NPS string $stringNumber exceeds fret 24" }
        val fretted = frets.filter { it > 0 }
        // Conventional suggested 3NPS fingering. These numbers guide the
        // learner; players can adapt them to hand size and technique.
        val recommendedFingers = when {
            fretted.size == 3 && fretted.last() - fretted.first() <= 2 -> listOf(1, 2, 3)
            fretted.size == 3 && fretted[1] - fretted[0] == 2 &&
                fretted[2] - fretted[1] == 1 -> listOf(1, 3, 4)
            fretted.size == 3 -> listOf(1, 2, 4)
            fretted.size == 2 -> listOf(1, if (fretted[1] - fretted[0] == 1) 2 else 3)
            else -> listOf(1)
        }
        for (fret in frets) {
            val midi = openMidi + fret
            val interval = normalizedPitchClass(midi - tonicPc)
            val finger = if (fret == 0) 0 else {
                recommendedFingers[fretted.indexOf(fret).coerceAtLeast(0)]
            }
            notes += HandShapeNote(
                stringNumber = stringNumber,
                fret = fret,
                finger = finger,
                midi = midi,
                noteName = spellHandShapeScaleNote(
                    tonicKey = tonicName,
                    degreeNumber = scaleType.degreeNumberForInterval(interval),
                    semitoneInterval = interval
                ),
                intervalFromRoot = interval,
                isRoot = interval == 0
            )
        }
        previousMidi = openMidi + frets.last()
    }
    return HandShapePattern(
        tonicName = tonicName,
        scaleType = scaleType,
        shape = CagedShape.C, // Not used to position 3NPS notes.
        startFret = notes.minOf { it.fret },
        endFret = notes.maxOf { it.fret },
        notes = notes
    )
}

/** All tones over the complete 24-fret guitar neck, regardless of CAGED shape. */
private fun buildFullNeckScalePattern(
    tonicName: String,
    scaleType: HandShapeScaleType
): HandShapePattern {
    val tonicPc = tonicPitchClass(tonicName)
    val notes = (6 downTo 1).flatMap { stringNumber ->
        val openMidi = guitarOpenMidiByString.getValue(stringNumber)
        (0..24).mapNotNull { fret ->
            val midi = openMidi + fret
            val interval = normalizedPitchClass(midi - tonicPc)
            if (interval !in scaleType.intervals) return@mapNotNull null
            HandShapeNote(
                stringNumber = stringNumber,
                fret = fret,
                finger = if (fret == 0) 0 else 1,
                midi = midi,
                noteName = spellHandShapeScaleNote(
                    tonicKey = tonicName,
                    degreeNumber = scaleType.degreeNumberForInterval(interval),
                    semitoneInterval = interval
                ),
                intervalFromRoot = interval,
                isRoot = interval == 0
            )
        }
    }
    return HandShapePattern(tonicName, scaleType, CagedShape.C, 0, 24, notes)
}

private fun handShapeScaleFretWindow(
    tonicName: String,
    scaleType: HandShapeScaleType,
    shape: CagedShape
): IntRange {
    val referenceKey = handShapeReferenceTonicKey(tonicName, scaleType)
    val referenceWindow = handShapeFretWindow(referenceKey, shape)
    // Reference app: G Ritusen + C shape shows open strings through fret 4.
    // Fret 4 is a convenient final empty column for the open C-shaped pattern.
    return if (scaleType == HandShapeScaleType.RITUSEN &&
        shape == CagedShape.C && referenceWindow.first == 0
    ) {
        0..maxOf(4, referenceWindow.last)
    } else {
        referenceWindow
    }
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
    // Location is determined from the reference major pentatonic when relevant;
    // note degrees, pitch classes, note names and root colours STILL use tonicName.
    val fretWindow = handShapeScaleFretWindow(tonicName, scaleType, shape)
    val startFret = fretWindow.first
    val endFret = fretWindow.last
    val notes = mutableListOf<HandShapeNote>()

    /*
     * Mỗi dạng CAGED có vùng ngăn riêng, không dùng chung một khung 5 ngăn:
     * C trưởng làm mốc: C 0-3, A 2-6, G 5-8, E 7-10, D 9-13.
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
    val theoryLessons = remember { theoryTopics() }
    var page by rememberSaveable(instrument, initialPage) { mutableStateOf(initialPage.name) }
    var selectedHandShapeKey by rememberSaveable(instrument) { mutableStateOf(CagedShape.C.key) }
    var selectedGuidedLessonId by rememberSaveable(instrument) { mutableStateOf("") }
    var returnToCurriculum by rememberSaveable(instrument) { mutableStateOf(false) }
    var theoryStartEntry by rememberSaveable(instrument) { mutableStateOf("LIBRARY") }
    var theoryStartStage by rememberSaveable(instrument) { mutableIntStateOf(0) }
    var theoryStartTopicId by rememberSaveable(instrument) { mutableStateOf("") }

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
                        returnToCurriculum = false
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
                onOpenHandShapes = {
                    returnToCurriculum = true
                    selectedHandShapeKey = CagedShape.C.key
                    page = "HAND_SHAPE_PLAYER"
                },
                onOpenHarmony = {
                    returnToCurriculum = true
                    page = "HARMONY_PLAYER"
                },
                onOpenTheoryLibrary = { stage ->
                    theoryStartStage = stage
                    theoryStartTopicId = ""
                    theoryStartEntry = "LIBRARY"
                    page = "THEORY_ACADEMY"
                },
                onOpenTheoryTopic = { id ->
                    theoryStartTopicId = id
                    theoryStartStage = theoryLessons.firstOrNull { it.id == id }?.stage ?: 0
                    theoryStartEntry = "TOPIC"
                    page = "THEORY_ACADEMY"
                },
                onOpenTheoryQuiz = { stage ->
                    theoryStartStage = stage
                    theoryStartTopicId = ""
                    theoryStartEntry = "QUIZ"
                    page = "THEORY_ACADEMY"
                },
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

        page == "THEORY_ACADEMY" -> {
            TheoryAcademyScreen(
                topics = theoryLessons,
                instrument = instrument,
                currentLang = currentLang,
                bgColor = bgColor,
                surfaceColor = surfaceColor,
                textColor = textColor,
                accent = tunaGreen,
                initialEntry = theoryStartEntry,
                initialStage = theoryStartStage,
                initialTopicId = theoryStartTopicId,
                onPractice = { theoryTopic ->
                    if (theoryTopic.id == "T06_PENTATONIC" || theoryTopic.id == "T14_MODES_BLUES") {
                        returnToCurriculum = true
                        selectedHandShapeKey = CagedShape.C.key
                        page = "HAND_SHAPE_PLAYER"
                    } else if (theoryTopic.id == "T15_PASSING" || theoryTopic.id == "T16_CHORD_MELODY") {
                        returnToCurriculum = true
                        page = "HARMONY_PLAYER"
                    } else {
                        val practiceLesson = guidedLessons.firstOrNull {
                            guidedRoadmapStageForLesson(it) == theoryTopic.stage
                        }
                        if (practiceLesson != null) {
                            selectedGuidedLessonId = practiceLesson.id
                            page = if (practiceLesson.kind == GuidedLessonKind.FINGER)
                                "GUIDED_FINGER_PLAYER" else "GUIDED_HARMONY_PLAYER"
                        }
                    }
                },
                onClose = { page = "GUIDED_CURRICULUM" }
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
                onOpenHarmony = {},
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
                onOpenExercises = {
                    returnToCurriculum = false
                    page = "EXERCISE_PLAYER"
                },
                onOpenHandShapes = {
                    selectedHandShapeKey = CagedShape.C.key
                    page = "HAND_SHAPE_PLAYER"
                },
                onClose = {
                    page = if (returnToCurriculum) "GUIDED_CURRICULUM" else FingerTrainingLandingPage.ENTRY.name
                    returnToCurriculum = false
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
                    returnToCurriculum = false
                    page = "EXERCISE_PLAYER"
                },
                onOpenHarmony = {
                    page = "HARMONY_PLAYER"
                },
                onClose = {
                    page = if (returnToCurriculum) "GUIDED_CURRICULUM" else FingerTrainingLandingPage.ENTRY.name
                    returnToCurriculum = false
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
                        returnToCurriculum = false
                        selectedHandShapeKey = CagedShape.C.key
                        page = "HAND_SHAPE_PLAYER"
                    }
                },
                onOpenHarmony = {
                    if (instrument == "Guitar") {
                        returnToCurriculum = false
                        page = "HARMONY_PLAYER"
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
                    harmonyAccent = Color(0xFFF2B94B),
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
    harmonyAccent: Color,
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
                fingerUiText(currentLang, "Thế tay", "Positions"),
                handShapeAccent
            ),
            Triple(
                FingerPracticeSection.HARMONY,
                fingerUiText(currentLang, "Hòa âm", "Harmony"),
                harmonyAccent
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
                        text = when (section) {
                            FingerPracticeSection.EXERCISES -> "1"
                            FingerPracticeSection.HAND_SHAPES -> "2"
                            FingerPracticeSection.HARMONY -> "3"
                            else -> "3"
                        },
                        color = accent,
                        fontSize = if (compact) 8.sp else 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(Modifier.width(if (compact) 5.dp else 7.dp))
                Text(
                    text = label,
                    color = if (selected) accent else textColor.copy(alpha = 0.68f),
                    fontSize = if (compact) 10.5.sp else 12.sp,
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
    onOpenHarmony: () -> Unit,
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
                        harmonyAccent = Color(0xFFF2B94B),
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        compact = veryCompactHeight,
                        onSelected = { section ->
                            when (section) {
                                FingerPracticeSection.HAND_SHAPES -> {
                                    isPlaying = false
                                    countdownValue = 0
                                    onOpenHandShapes()
                                }
                                FingerPracticeSection.HARMONY -> {
                                    isPlaying = false
                                    countdownValue = 0
                                    onOpenHarmony()
                                }
                                FingerPracticeSection.EXERCISES -> Unit
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
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Keep all settings visible on normal screens; only short
                                // devices scroll inside this panel, leaving the fretboard
                                // and playback controls fixed. Avoid unused-constraint lint.
                                val scrollSettings = configuration.screenHeightDp <
                                    (if (guidedLesson == null) 670 else 620)
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(if (veryCompactHeight) 4.dp else 6.dp)
                                        .then(if (scrollSettings) Modifier.verticalScroll(rememberScrollState()) else Modifier),
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
                                            modifier = if (scrollSettings) {
                                                Modifier.fillMaxWidth().height(42.dp)
                                            } else {
                                                Modifier.fillMaxWidth().weight(1f)
                                            },
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
                                        modifier = if (scrollSettings) {
                                            Modifier.fillMaxWidth().height(42.dp)
                                        } else {
                                            Modifier.fillMaxWidth().weight(1f)
                                        },
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
                                        modifier = if (scrollSettings) {
                                            Modifier.fillMaxWidth().height(42.dp)
                                        } else {
                                            Modifier.fillMaxWidth().weight(1f)
                                        },
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
                                        modifier = if (scrollSettings) {
                                            Modifier.fillMaxWidth().height(42.dp)
                                        } else {
                                            Modifier.fillMaxWidth().weight(1f)
                                        },
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
                                            modifier = if (scrollSettings) {
                                                Modifier.fillMaxWidth().height(42.dp)
                                            } else {
                                                Modifier.fillMaxWidth().weight(1f)
                                            },
                                            onSelected = { fretViewKey = it }
                                        )
                                    }
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
                            PracticeBpmStepper(
                                bpm = bpm.roundToInt(),
                                minBpm = 40,
                                maxBpm = 160,
                                accent = uiAccent,
                                textColor = textColor,
                                subdivisionLabel = subdivision.shortLabel,
                                onSubdivisionClick = {
                                    val values = PracticeSubdivision.values()
                                    val next = (values.indexOf(subdivision) + 1) % values.size
                                    subdivisionKey = values[next].key
                                    roundsAtCurrentBpm = 0
                                    currentIndex = 0
                                },
                                onDecrease = {
                                    bpm = (bpm - 5f).coerceAtLeast(40f)
                                    roundsAtCurrentBpm = 0
                                },
                                onIncrease = {
                                    bpm = (bpm + 5f).coerceAtMost(160f)
                                    roundsAtCurrentBpm = 0
                                }
                            )
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
    onOpenHarmony: () -> Unit,
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
    val chordToneColor = Color(0xFF2D7CFF)
    val pentatonicToneColor = Color(0xFF22C58B)
    val tensionToneColor = Color(0xFFF2B94B)
    val highTensionToneColor = Color(0xFFEF4D5A)
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
    var showHorizontalFretboard by rememberSaveable(initialShape.key) { mutableStateOf(false) }
    var showFullNeck by rememberSaveable(initialShape.key) { mutableStateOf(false) }
    var showThreeNpsHelp by rememberSaveable(initialShape.key) { mutableStateOf(false) }
    var learningModeKey by rememberSaveable(initialShape.key) {
        mutableStateOf(handShapePrefs.getString("learning_mode", "CAGED") ?: "CAGED")
    }
    var threeNpsPosition by rememberSaveable(initialShape.key) {
        mutableIntStateOf(handShapePrefs.getInt("three_nps_position", 0).coerceIn(0, 6))
    }
    var visualModeKey by rememberSaveable(initialShape.key) {
        mutableStateOf(
            handShapePrefs.getString(
                "visual_mode_key",
                HandShapeVisualMode.SCALE_CHARACTER.key
            )?.takeIf { saved -> HandShapeVisualMode.values().any { it.key == saved } }
                ?: HandShapeVisualMode.SCALE_CHARACTER.key
        )
    }

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
    var fretViewKey by rememberSaveable(initialShape.key) {
        mutableStateOf(
            if (handShapePrefs.getString("scale_type_key", "") == HandShapeScaleType.RITUSEN.key) {
                "SHAPE"
            } else {
                "FULL"
            }
        )
    }
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
    val selectedVisualMode = HandShapeVisualMode.values().firstOrNull {
        it.key == visualModeKey
    } ?: HandShapeVisualMode.SCALE_CHARACTER
    val selectedTonicLabel = remember(tonicKey) { handShapeTonicLabel(tonicKey) }
    val cagedReferenceKey = remember(tonicKey, selectedScaleType) {
        handShapeReferenceTonicKey(tonicKey, selectedScaleType)
    }
    val cagedReferenceLabel = handShapeTonicLabel(cagedReferenceKey)
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
    val threeNpsAvailable = isThreeNotesPerStringScale(selectedScaleType)
    val isThreeNps = learningModeKey == HandPositionLearningMode.THREE_NPS.name && threeNpsAvailable
    val pattern = remember(tonicKey, selectedScaleType, selectedShape, isThreeNps, threeNpsPosition) {
        if (isThreeNps) {
            buildThreeNotesPerStringPattern(tonicKey, selectedScaleType, threeNpsPosition)
        } else {
            buildHandShapePattern(tonicKey, selectedScaleType, selectedShape)
        }
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

    LaunchedEffect(tonicKey, scaleTypeKey, shapeKey, visualModeKey, learningModeKey, threeNpsPosition) {
        handShapePrefs.edit()
            .putString("tonic_key", tonicKey)
            .putString("scale_type_key", scaleTypeKey)
            .putString("shape_key", shapeKey)
            .putString("visual_mode_key", visualModeKey)
            .putString("learning_mode", learningModeKey)
            .putInt("three_nps_position", threeNpsPosition)
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
                            // A pentatonic-mode reference key may differ from the root.
                            // G Ritusen + C reference shape = open strings to fret 4.
                            text = if (isThreeNps) {
                                "$selectedTonicLabel · 3NPS ${threeNpsPosition + 1}/7 · ${if (isVietnamese) "ngăn" else "frets"} ${pattern.startFret}–${pattern.endFret}"
                            } else {
                                "$selectedTonicLabel · ${selectedShape.title(currentLang)}" +
                                    (if (cagedReferenceKey != tonicKey) " ($cagedReferenceLabel)" else "") +
                                    " · ${if (isVietnamese) "ngăn" else "frets"} ${pattern.startFret}–${pattern.endFret}"
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

                FingerPracticeSectionSelector(
                    selectedSection = FingerPracticeSection.HAND_SHAPES,
                    currentLang = currentLang,
                    exerciseAccent = Color(0xFF22C58B),
                    handShapeAccent = rootAccent,
                    harmonyAccent = Color(0xFFF2B94B),
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    compact = veryCompactHeight,
                    onSelected = { section ->
                        when (section) {
                            FingerPracticeSection.EXERCISES -> {
                                resetSession()
                                onOpenExercises()
                            }
                            FingerPracticeSection.HARMONY -> {
                                resetSession()
                                onOpenHarmony()
                            }
                            FingerPracticeSection.HAND_SHAPES -> Unit
                        }
                    }
                )

                Spacer(Modifier.height(panelGap))

                HandPositionModeSelector(
                    isThreeNps = isThreeNps,
                    isThreeNpsAvailable = threeNpsAvailable,
                    currentLang = currentLang,
                    accent = uiAccent,
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    compact = veryCompactHeight,
                    onCaged = {
                        resetSession()
                        learningModeKey = HandPositionLearningMode.CAGED.name
                    },
                    onThreeNps = {
                        if (threeNpsAvailable) {
                            resetSession()
                            learningModeKey = HandPositionLearningMode.THREE_NPS.name
                            fretViewKey = "SHAPE"
                        } else {
                            showThreeNpsHelp = true
                        }
                    },
                    onFullNeck = { showFullNeck = true }
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
                                    onClick = { showHorizontalFretboard = true },
                                    modifier = Modifier.size(if (veryCompactHeight) 23.dp else 27.dp)
                                ) {
                                    Text(
                                        text = "↔",
                                        color = uiAccent,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
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
                                visualMode = selectedVisualMode,
                                chordToneColor = chordToneColor,
                                pentatonicToneColor = pentatonicToneColor,
                                tensionToneColor = tensionToneColor,
                                highTensionToneColor = highTensionToneColor,
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

                            if (isThreeNps) {
                                ThreeNpsPositionSelector(
                                    selectedPosition = threeNpsPosition,
                                    accent = uiAccent,
                                    textColor = textColor,
                                    surfaceColor = bgColor.copy(alpha = 0.42f),
                                    compact = veryCompactHeight,
                                    onSelected = { position ->
                                        resetSession()
                                        threeNpsPosition = position
                                    }
                                )
                            } else {
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
                                        if (!isThreeNotesPerStringScale(it)) {
                                            learningModeKey = HandPositionLearningMode.CAGED.name
                                        }
                                        // Start Ritusen in its compact CAGED region, while
                                        // retaining the full-fretboard option for other scales.
                                        fretViewKey = if (it == HandShapeScaleType.RITUSEN) "SHAPE" else "FULL"
                                    }
                                )
                                HandShapeSideDropdown(
                                    label = if (isThreeNps) {
                                        fingerUiText(currentLang, "Vị trí 3NPS", "3NPS position")
                                    } else {
                                        fingerUiText(currentLang, "Thế tay", "Shape")
                                    },
                                    selectedKey = if (isThreeNps) threeNpsPosition.toString() else shapeKey,
                                    selectedLabel = if (isThreeNps) "${threeNpsPosition + 1} / 7" else selectedShape.title(currentLang),
                                    options = if (isThreeNps) {
                                        (0..6).map { position ->
                                            position.toString() to fingerUiText(
                                                currentLang,
                                                "Vị trí ${position + 1} · dây 6 ngăn ${threeNpsStartingFrets(tonicKey, selectedScaleType)[position]}",
                                                "Position ${position + 1} · low E fret ${threeNpsStartingFrets(tonicKey, selectedScaleType)[position]}"
                                            )
                                        }
                                    } else shapeOptions,
                                    accent = uiAccent,
                                    surfaceColor = bgColor.copy(alpha = 0.45f),
                                    textColor = textColor,
                                    compact = veryCompactHeight,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    onSelected = {
                                        resetSession()
                                        if (isThreeNps) threeNpsPosition = it.toIntOrNull()?.coerceIn(0, 6) ?: 0
                                        else shapeKey = it
                                    }
                                )
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(
                                    when {
                                        selectedVisualMode == HandShapeVisualMode.HARMONIC_ROLE -> 1.18f
                                        hasCharacteristicTones -> 1.12f
                                        else -> 0.92f
                                    }
                                ),
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
                                HandShapeVisualModeSelector(
                                    selectedMode = selectedVisualMode,
                                    currentLang = currentLang,
                                    accent = uiAccent,
                                    harmonyAccent = chordToneColor,
                                    surfaceColor = bgColor.copy(alpha = 0.42f),
                                    textColor = textColor,
                                    compact = veryCompactHeight,
                                    onSelected = { visualModeKey = it.key }
                                )

                                if (selectedVisualMode == HandShapeVisualMode.HARMONIC_ROLE) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        HandShapeLegendEntry(
                                            color = chordToneColor,
                                            label = fingerUiText(currentLang, "Nốt hợp âm", "Chord tones"),
                                            textColor = textColor,
                                            modifier = Modifier.weight(1f)
                                        )
                                        HandShapeLegendEntry(
                                            color = pentatonicToneColor,
                                            label = fingerUiText(currentLang, "Ngũ cung", "Pentatonic"),
                                            textColor = textColor,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        HandShapeLegendEntry(
                                            color = tensionToneColor,
                                            label = fingerUiText(currentLang, "Tension", "Tension"),
                                            textColor = textColor,
                                            modifier = Modifier.weight(1f)
                                        )
                                        HandShapeLegendEntry(
                                            color = highTensionToneColor,
                                            label = fingerUiText(currentLang, "Căng mạnh", "High tension"),
                                            textColor = textColor,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                } else {
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
                            PracticeBpmStepper(
                                bpm = bpm.roundToInt(),
                                minBpm = 40,
                                maxBpm = 160,
                                accent = uiAccent,
                                textColor = textColor,
                                onDecrease = { bpm = (bpm - 5f).coerceAtLeast(40f) },
                                onIncrease = { bpm = (bpm + 5f).coerceAtMost(160f) }
                            )
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
                    if (cagedReferenceKey != tonicKey) {
                        Text(
                            text = fingerUiText(
                                currentLang,
                                "Nốt chủ $selectedTonicLabel · Thế CAGED tham chiếu $cagedReferenceLabel ngũ cung trưởng. Nốt gốc không đổi khi chuyển thế tay.",
                                "Tonic $selectedTonicLabel · CAGED shapes refer to $cagedReferenceLabel major pentatonic. The tonic does not change when switching shapes."
                            ),
                            color = uiAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 17.sp
                        )
                    }

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

                    if (selectedVisualMode == HandShapeVisualMode.HARMONIC_ROLE) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = chordToneColor.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                chordToneColor.copy(alpha = 0.24f)
                            )
                        ) {
                            Text(
                                text = fingerUiText(
                                    currentLang,
                                    "Màu hòa âm được tính theo hợp âm ba nốt trên nốt chủ: xanh dương là nốt hợp âm; xanh lá là phần còn lại của ngũ cung phù hợp; vàng là tension màu sắc; đỏ là nốt căng mạnh, thường nên giải về nốt gần nhất. Trên A Aeolian/Am: A–C–E xanh dương, D–G xanh lá, B vàng và F đỏ.",
                                    "Harmony colours are calculated against the tonic triad: blue marks chord tones; green marks the remaining matching pentatonic tones; gold marks colour tensions; red marks strong tensions that commonly resolve to a nearby tone. In A Aeolian over Am: A–C–E are blue, D–G green, B gold and F red."
                                ),
                                color = textColor.copy(alpha = 0.78f),
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp)
                            )
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

    if (showThreeNpsHelp) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showThreeNpsHelp = false },
            title = { Text(fingerUiText(currentLang, "3NPS – ba nốt trên mỗi dây", "3NPS – three notes per string")) },
            text = {
                Text(
                    fingerUiText(
                        currentLang,
                        "Chế độ 3NPS dùng cho âm giai có đúng 7 cao độ khác nhau. Hãy chọn một trong 7 mode trưởng, Harmonic Minor hoặc Melodic Minor. Ngũ cung và Blues vẫn luyện với CAGED.",
                        "3NPS requires a seven-tone scale. Choose a major mode, harmonic minor or melodic minor. Pentatonic and blues scales stay in CAGED."
                    )
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showThreeNpsHelp = false }) {
                    Text(fingerUiText(currentLang, "Đã hiểu", "OK"))
                }
            }
        )
    }

    if (showFullNeck) {
        HandShapeFullNeckDialog(
            tonicKey = tonicKey,
            scaleType = selectedScaleType,
            selectedShape = selectedShape,
            currentLang = currentLang,
            background = bgColor,
            surfaceColor = surfaceColor,
            textColor = textColor,
            accent = uiAccent,
            onSelectShape = { selected ->
                resetSession()
                learningModeKey = HandPositionLearningMode.CAGED.name
                shapeKey = selected.key
            },
            onClose = { showFullNeck = false }
        )
    }

    if (showHorizontalFretboard) {
        if (isThreeNps) {
            ThreeNpsHorizontalReferenceDialog(
                tonicKey = tonicKey,
                scaleType = selectedScaleType,
                selectedPosition = threeNpsPosition,
                currentLang = currentLang,
                background = bgColor,
                surfaceColor = surfaceColor,
                textColor = textColor,
                accent = uiAccent,
                onSelectPosition = { position ->
                    resetSession()
                    threeNpsPosition = position
                },
                onClose = { showHorizontalFretboard = false }
            )
        } else {
            HandShapeHorizontalReferenceDialog(
                tonicKey = tonicKey,
                scaleType = selectedScaleType,
                selectedShape = selectedShape,
                currentLang = currentLang,
                background = bgColor,
                surfaceColor = surfaceColor,
                textColor = textColor,
                accent = uiAccent,
                onSelectShape = { newShape ->
                    resetSession()
                    shapeKey = newShape.key
                },
                onClose = { showHorizontalFretboard = false }
            )
        }
    }

    if (countdownValue > 0) {
        PracticeCountdownOverlay(
            value = countdownValue,
            accent = uiAccent
        )
    }
}


/**
 * Full-width, horizontal reference view. It is deliberately separate from the
 * existing vertical exercise canvas, so the practice / audio / BPM flow stays
 * unchanged. Each shape renders notes computed from the SAME pattern builder.
 */
@Composable
private fun HandShapeHorizontalReferenceDialog(
    tonicKey: String,
    scaleType: HandShapeScaleType,
    selectedShape: CagedShape,
    currentLang: String,
    background: Color,
    surfaceColor: Color,
    textColor: Color,
    accent: Color,
    onSelectShape: (CagedShape) -> Unit,
    onClose: () -> Unit
) {
    val isVi = currentLang.lowercase(Locale.ROOT).startsWith("vi")
    val referenceKey = remember(tonicKey, scaleType) {
        handShapeReferenceTonicKey(tonicKey, scaleType)
    }
    val patterns = remember(tonicKey, scaleType) {
        cagedShapes().associateWith { shape -> buildHandShapePattern(tonicKey, scaleType, shape) }
    }
    val pattern = patterns.getValue(selectedShape)
    val tonicLabel = handShapeTonicLabel(tonicKey)
    val referenceLabel = handShapeTonicLabel(referenceKey)
    val scaleNotes = remember(tonicKey, scaleType) {
        handShapeScaleNoteNames(tonicKey, scaleType).joinToString(" – ")
    }
    val configuration = LocalConfiguration.current
    val maxDialogHeight = (configuration.screenHeightDp * 0.84f).dp

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .heightIn(max = maxDialogHeight),
            shape = RoundedCornerShape(20.dp),
            color = background,
            border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$tonicLabel ${scaleType.title(currentLang)}",
                            color = textColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2
                        )
                        Text(
                            text = scaleType.formula,
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = if (isVi) "Đóng" else "Close", tint = textColor)
                    }
                }
                Text(
                    text = scaleNotes,
                    color = textColor.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (referenceKey != tonicKey) {
                        fingerUiText(
                            currentLang,
                            "Tông $tonicLabel · Tham chiếu $referenceLabel ngũ cung trưởng · Thế ${selectedShape.key} · Ngăn ${pattern.startFret}–${pattern.endFret}",
                            "Tonic $tonicLabel · $referenceLabel major-pentatonic reference · ${selectedShape.key} shape · Frets ${pattern.startFret}–${pattern.endFret}"
                        )
                    } else {
                        fingerUiText(
                            currentLang,
                            "Tông $tonicLabel · Thế ${selectedShape.key} · Ngăn ${pattern.startFret}–${pattern.endFret}",
                            "Tonic $tonicLabel · ${selectedShape.key} shape · Frets ${pattern.startFret}–${pattern.endFret}"
                        )
                    },
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp
                )
                Surface(
                    color = surfaceColor,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, textColor.copy(alpha = 0.15f)
                    )
                ) {
                    HandShapeHorizontalFretboardCanvas(
                        pattern = pattern,
                        textColor = textColor,
                        miniature = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(242.dp)
                            .padding(horizontal = 4.dp, vertical = 5.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("● R", color = Color(0xFFEF5559), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("● 5", color = Color(0xFF2AD8BB), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("● 4", color = Color(0xFF91D778), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (isVi) "● Bậc khác" else "● Other",
                        color = textColor.copy(alpha = 0.75f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = if (isVi) "Chọn thế CAGED" else "Choose CAGED shape",
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    cagedShapes().forEach { shape ->
                        val isSelected = shape == selectedShape
                        Surface(
                            modifier = Modifier
                                .width(98.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .clickable { onSelectShape(shape) },
                            color = if (isSelected) accent.copy(alpha = 0.16f) else surfaceColor,
                            shape = RoundedCornerShape(11.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) accent else textColor.copy(alpha = 0.16f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                HandShapeHorizontalFretboardCanvas(
                                    pattern = patterns.getValue(shape),
                                    textColor = textColor,
                                    miniature = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp)
                                )
                                Text(
                                    text = if (isVi) "Thế ${shape.key}" else "${shape.key} shape",
                                    color = if (isSelected) accent else textColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
                if (referenceKey != tonicKey) {
                    Text(
                        text = fingerUiText(
                            currentLang,
                            "Giữ $tonicLabel làm nốt chủ (R); $referenceLabel chỉ dùng để xác định vị trí khuôn CAGED. Thay thế tay không làm đổi âm giai.",
                            "Keep $tonicLabel as tonic (R). $referenceLabel only locates the CAGED shape; changing shapes does not change the scale."
                        ),
                        color = textColor.copy(alpha = 0.76f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

/** Horizontal 3NPS chart with all seven ascending scale positions. */
@Composable
private fun ThreeNpsHorizontalReferenceDialog(
    tonicKey: String,
    scaleType: HandShapeScaleType,
    selectedPosition: Int,
    currentLang: String,
    background: Color,
    surfaceColor: Color,
    textColor: Color,
    accent: Color,
    onSelectPosition: (Int) -> Unit,
    onClose: () -> Unit
) {
    val pattern = remember(tonicKey, scaleType, selectedPosition) {
        buildThreeNotesPerStringPattern(tonicKey, scaleType, selectedPosition)
    }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.84f).dp),
            color = background,
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.43f))
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${handShapeTonicLabel(tonicKey)} ${scaleType.title(currentLang)}",
                            color = textColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            maxLines = 2
                        )
                        Text(
                            fingerUiText(
                                currentLang,
                                "3NPS · Vị trí ${selectedPosition + 1}/7 · ngăn ${pattern.startFret}–${pattern.endFret}",
                                "3NPS · Position ${selectedPosition + 1}/7 · frets ${pattern.startFret}–${pattern.endFret}"
                            ),
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = textColor)
                    }
                }
                Text(
                    text = "${scaleType.formula}  ·  ${handShapeScaleNoteNames(tonicKey, scaleType).joinToString(" – ")}",
                    color = accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                )
                Surface(
                    color = surfaceColor,
                    shape = RoundedCornerShape(13.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.15f))
                ) {
                    HandShapeHorizontalFretboardCanvas(
                        pattern = pattern,
                        textColor = textColor,
                        miniature = false,
                        modifier = Modifier.fillMaxWidth().height(230.dp).padding(5.dp)
                    )
                }
                Text(
                    text = fingerUiText(
                        currentLang,
                        "Mỗi dây có đúng ba nốt âm giai; đi từ dây E trầm (6) đến E cao (1). Vòng tròn hiển thị bậc âm, không phải số ngón.",
                        "Each string contains three scale tones; play from low E (6) to high E (1). Circles show scale degrees, not finger numbers."
                    ),
                    color = textColor.copy(alpha = 0.75f), fontSize = 11.sp
                )
                Text(
                    fingerUiText(currentLang, "Chọn 7 vị trí 3NPS", "Select one of seven 3NPS positions"),
                    color = textColor, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp
                )
                ThreeNpsPositionSelector(
                    selectedPosition = selectedPosition,
                    accent = accent,
                    textColor = textColor,
                    surfaceColor = surfaceColor,
                    compact = false,
                    onSelected = onSelectPosition
                )
            }
        }
    }
}

/** Full 24-fret reference chart with the selected CAGED range highlighted. */
@Composable
private fun HandShapeFullNeckDialog(
    tonicKey: String,
    scaleType: HandShapeScaleType,
    selectedShape: CagedShape,
    currentLang: String,
    background: Color,
    surfaceColor: Color,
    textColor: Color,
    accent: Color,
    onSelectShape: (CagedShape) -> Unit,
    onClose: () -> Unit
) {
    val chartPattern = remember(tonicKey, scaleType) { buildFullNeckScalePattern(tonicKey, scaleType) }
    val selectedWindow = remember(tonicKey, scaleType, selectedShape) {
        handShapeScaleFretWindow(tonicKey, scaleType, selectedShape)
    }
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    LaunchedEffect(tonicKey, scaleType, selectedShape) {
        val targetPx = with(density) { (maxOf(0, selectedWindow.first - 2) * 40).dp.roundToPx() }
        scrollState.scrollTo(targetPx.coerceIn(0, scrollState.maxValue))
    }
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.87f).dp
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.98f).heightIn(max = maxHeight),
            color = background,
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.50f))
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${handShapeTonicLabel(tonicKey)} ${scaleType.title(currentLang)}",
                            color = textColor, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                            maxLines = 2
                        )
                        Text(
                            text = fingerUiText(
                                currentLang,
                                "Toàn cần đàn 0–24 · cuộn ngang để xem vị trí",
                                "Full neck 0–24 · scroll horizontally"
                            ),
                            color = textColor.copy(alpha = 0.68f), fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = fingerUiText(currentLang, "Đóng", "Close"), tint = textColor)
                    }
                }
                Text(
                    text = "${scaleType.formula}  ·  ${handShapeScaleNoteNames(tonicKey, scaleType).joinToString(" – ")}",
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = fingerUiText(
                        currentLang,
                        "Thế ${selectedShape.key} đang chọn · ngăn ${selectedWindow.first}–${selectedWindow.last}. Vùng sáng là phần thế tham chiếu, không thay đổi nốt chủ.",
                        "Selected ${selectedShape.key} shape · frets ${selectedWindow.first}–${selectedWindow.last}. Highlight indicates the reference position; tonic stays unchanged."
                    ),
                    color = textColor.copy(alpha = 0.83f),
                    fontSize = 11.sp
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .border(1.dp, textColor.copy(alpha = 0.14f), RoundedCornerShape(13.dp))
                        .horizontalScroll(scrollState)
                ) {
                    HandShapeHorizontalFretboardCanvas(
                        pattern = chartPattern,
                        textColor = textColor,
                        miniature = false,
                        highlightRange = selectedWindow,
                        highlightColor = accent,
                        modifier = Modifier.width(1040.dp).height(235.dp).padding(8.dp)
                    )
                }
                Text(
                    text = fingerUiText(
                        currentLang,
                        "● Đỏ: nốt gốc  ·  ● Xanh: bậc 5  ·  ● Các màu khác: bậc âm",
                        "● Red: tonic  ·  ● Teal: fifth  ·  ● Other colors: scale degrees"
                    ),
                    color = textColor.copy(alpha = 0.77f), fontSize = 10.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    cagedShapes().forEach { shape ->
                        val selected = shape == selectedShape
                        Surface(
                            modifier = Modifier.weight(1f).clickable { onSelectShape(shape) },
                            color = if (selected) accent.copy(alpha = 0.20f) else surfaceColor,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, if (selected) accent else textColor.copy(alpha = 0.14f)
                            )
                        ) {
                            Text(
                                text = shape.key,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = if (selected) accent else textColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Standard tuning: string 1 (high E) is displayed at the top, string 6 at the bottom. */
@Composable
private fun HandShapeHorizontalFretboardCanvas(
    pattern: HandShapePattern,
    textColor: Color,
    miniature: Boolean,
    modifier: Modifier = Modifier,
    highlightRange: IntRange? = null,
    highlightColor: Color = Color(0xFF22C58B)
) {
    val density = LocalDensity.current
    Canvas(modifier = modifier) {
        if (size.width <= 1f || size.height <= 1f) return@Canvas
        val isOpen = pattern.startFret == 0
        val left = with(density) { (if (miniature) 13 else 39).dp.toPx() }
        val right = size.width - with(density) { (if (miniature) 4 else 10).dp.toPx() }
        val top = with(density) { (if (miniature) 7 else 36).dp.toPx() }
        val bottom = size.height - with(density) { (if (miniature) 5 else 19).dp.toPx() }
        val usableWidth = (right - left).coerceAtLeast(1f)
        val usableHeight = (bottom - top).coerceAtLeast(1f)
        val stringGap = usableHeight / 5f
        val firstFret = maxOf(1, pattern.startFret)
        val fretCount = maxOf(1, pattern.endFret - firstFret + 1)
        val fretGap = usableWidth / fretCount.toFloat()
        val radius = minOf(
            stringGap * 0.40f,
            fretGap * 0.32f,
            with(density) { (if (miniature) 3.7f else 13f).dp.toPx() }
        ).coerceAtLeast(with(density) { (if (miniature) 1.8f else 5f).dp.toPx() })
        fun yForString(stringNumber: Int): Float = top + (stringNumber - 1) * stringGap
        fun xForFret(fret: Int): Float = if (isOpen && fret == 0) {
            left - with(density) { (if (miniature) 6 else 19).dp.toPx() }
        } else {
            left + (fret - firstFret + 0.5f) * fretGap
        }

        if (highlightRange != null && !miniature) {
            val clipStart = maxOf(firstFret, highlightRange.first)
            val clipEnd = minOf(pattern.endFret, highlightRange.last)
            if (clipStart <= clipEnd) {
                drawRect(
                    color = highlightColor.copy(alpha = 0.13f),
                    topLeft = Offset(left + (clipStart - firstFret) * fretGap, top),
                    size = androidx.compose.ui.geometry.Size(
                        (clipEnd - clipStart + 1) * fretGap,
                        usableHeight
                    )
                )
            }
        }
        for (stringNumber in 1..6) {
            drawLine(
                color = textColor.copy(alpha = 0.33f),
                start = Offset(left, yForString(stringNumber)),
                end = Offset(right, yForString(stringNumber)),
                strokeWidth = with(density) { (if (miniature) 0.5f else 1f).dp.toPx() }
            )
        }
        for (index in 0..fretCount) {
            val x = left + index * fretGap
            drawLine(
                color = if (index == 0 && isOpen) Color(0xFFE6E6E6) else textColor.copy(alpha = 0.26f),
                start = Offset(x, top),
                end = Offset(x, bottom),
                strokeWidth = with(density) {
                    (if (index == 0 && isOpen && !miniature) 3f else if (miniature) 0.7f else 1f).dp.toPx()
                }
            )
        }
        if (!miniature) {
            val fretPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = with(density) { 11.sp.toPx() }
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                color = textColor.copy(alpha = 0.70f).toArgb()
            }
            for (fret in firstFret..pattern.endFret) {
                drawContext.canvas.nativeCanvas.drawText(
                    fret.toString(),
                    xForFret(fret),
                    top - with(density) { 11.dp.toPx() },
                    fretPaint
                )
            }
        }
        val labelPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = with(density) { 11.sp.toPx() }
        }
        pattern.notes.forEach { note ->
            val cx = xForFret(note.fret)
            val cy = yForString(note.stringNumber)
            val degree = pattern.scaleType.degreeLabelForInterval(note.intervalFromRoot)
            val colour = when {
                note.isRoot -> Color(0xFFEF5559)
                degree == "5" -> Color(0xFF2AD8BB)
                degree == "4" -> Color(0xFF91D778)
                else -> Color(0xFF7388A4)
            }
            drawCircle(colour, radius, Offset(cx, cy))
            if (!miniature) {
                labelPaint.color = Color(0xFF0B1423).toArgb()
                val label = if (note.isRoot) "R" else degree
                val centeredY = cy - (labelPaint.ascent() + labelPaint.descent()) / 2f
                drawContext.canvas.nativeCanvas.drawText(label, cx, centeredY, labelPaint)
            }
        }
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
    onOpenExercises: () -> Unit = {},
    onOpenHandShapes: () -> Unit = {},
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val isVietnamese = currentLang.lowercase(Locale.ROOT).startsWith("vi")
    val progressions = remember { harmonyProgressions() }
    val activeChapter = guidedPracticeChapter(guidedLesson?.chapterKey)
    val activeChapterLessons = if (activeChapter != null) {
        chapterLessons.filter { it.chapterKey == activeChapter.key }
    } else {
        emptyList()
    }

    val harmonyPrefs = remember(context) {
        context.getSharedPreferences("tuner_tools_harmony_workspace_v2", android.content.Context.MODE_PRIVATE)
    }
    var workspaceKey by rememberSaveable(guidedLesson?.id) {
        mutableStateOf(
            if (guidedLesson == null) {
                harmonyPrefs.getString("workspace_tab", HarmonyWorkspaceTab.PROGRESSION.key)
                    ?: HarmonyWorkspaceTab.PROGRESSION.key
            } else {
                HarmonyWorkspaceTab.PROGRESSION.key
            }
        )
    }
    var libraryKey by rememberSaveable(guidedLesson?.id) {
        mutableStateOf(
            harmonyPrefs.getString("library_tab", HarmonyLibraryTab.PRESET.key)
                ?: HarmonyLibraryTab.PRESET.key
        )
    }
    var customTonalityKey by rememberSaveable {
        mutableStateOf(harmonyPrefs.getString("custom_tonality", HarmonyTonality.MAJOR.name) ?: HarmonyTonality.MAJOR.name)
    }
    var customDegreeCsv by rememberSaveable {
        mutableStateOf(harmonyPrefs.getString("custom_degrees", "1,5,6,4") ?: "1,5,6,4")
    }
    var melodyToneCsv by rememberSaveable {
        mutableStateOf(harmonyPrefs.getString("melody_tones", "2,1,2,1") ?: "2,1,2,1")
    }
    var selectedPassingGap by rememberSaveable { mutableIntStateOf(-1) }
    var appliedPassingByGap by remember {
        mutableStateOf<Map<Int, PassingChordSuggestion>>(emptyMap())
    }
    var passingPreviewJob by remember { mutableStateOf<Job?>(null) }
    // Audition audio is isolated from the regular lesson engine.
    fun stopPassingPreview() {
        passingPreviewJob?.cancel()
        passingPreviewJob = null
        CoreAudioPlayer.stopGuitarChordPreview()
    }
    DisposableEffect(Unit) {
        onDispose {
            passingPreviewJob?.cancel()
            CoreAudioPlayer.stopGuitarChordPreview()
        }
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

    val workspaceTab = HarmonyWorkspaceTab.values().firstOrNull { it.key == workspaceKey }
        ?: HarmonyWorkspaceTab.PROGRESSION
    val libraryTab = HarmonyLibraryTab.values().firstOrNull { it.key == libraryKey }
        ?: HarmonyLibraryTab.PRESET
    val customTonality = HarmonyTonality.values().firstOrNull { it.name == customTonalityKey }
        ?: HarmonyTonality.MAJOR
    val customDegrees = customDegreeCsv.split(',')
        .mapNotNull { it.trim().toIntOrNull() }
        .map { it.coerceIn(1, 7) }
        .let { values -> if (values.size >= 2) values.take(8) else listOf(1, 5, 6, 4) }

    val selectedPracticeType = HarmonyPracticeType.values().firstOrNull { it.key == practiceTypeKey }
        ?: HarmonyPracticeType.TRIAD
    // Chord Melody deliberately starts with compact three-note voicings. This
    // keeps the melody on top and avoids impossible 5-7 note grips on a phone-sized diagram.
    val practiceType = if (workspaceTab == HarmonyWorkspaceTab.CHORD_MELODY && guidedLesson == null) {
        HarmonyPracticeType.TRIAD
    } else {
        selectedPracticeType
    }
    val presetProgression = progressions.firstOrNull { it.key == progressionKey }
        ?: progressions.first()
    val progression = if (guidedLesson == null && libraryTab == HarmonyLibraryTab.MY_LOOP) {
        buildCustomHarmonyProgression(customTonality, customDegrees)
    } else {
        presetProgression
    }
    val stringSetOptions = remember(practiceType) { harmonyStringSets(practiceType) }
    val stringSet = stringSetOptions.firstOrNull { it.key == stringSetKey }
        ?: stringSetOptions.first()
    val inversionOptions = remember(practiceType) { harmonyInversionModes(practiceType) }
    val inversionMode = if (workspaceTab == HarmonyWorkspaceTab.CHORD_MELODY && guidedLesson == null) {
        HarmonyInversionMode.AUTO
    } else {
        inversionOptions.firstOrNull { it.key == inversionKey } ?: HarmonyInversionMode.AUTO
    }
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

    LaunchedEffect(workspaceKey, libraryKey, customTonalityKey, customDegreeCsv, melodyToneCsv) {
        if (guidedLesson == null) {
            harmonyPrefs.edit()
                .putString("workspace_tab", workspaceKey)
                .putString("library_tab", libraryKey)
                .putString("custom_tonality", customTonalityKey)
                .putString("custom_degrees", customDegreeCsv)
                .putString("melody_tones", melodyToneCsv)
                .apply()
        }
    }

    LaunchedEffect(keyName, progression.key) {
        appliedPassingByGap = emptyMap()
        selectedPassingGap = -1
    }

    val fretRange = positionStart..(positionStart + 4)
    val baseChords = remember(keyName, progression.key, practiceType.key) {
        buildHarmonyChords(keyName, progression, practiceType)
    }
    val chords = remember(baseChords, appliedPassingByGap, practiceType.noteCount) {
        insertPassingChords(
            baseChords = baseChords,
            appliedByGap = appliedPassingByGap,
            practiceNoteCount = practiceType.noteCount
        )
    }
    val parsedMelodyToneIndexes = melodyToneCsv.split(',')
        .mapNotNull { it.trim().toIntOrNull() }
    val melodyToneIndexes = chords.indices.map { index ->
        parsedMelodyToneIndexes.getOrNull(index)
            ?: when (index % 4) {
                0 -> 2
                1 -> 1
                2 -> 2
                else -> 0
            }
    }
    val voicings = remember(
        chords, stringSet.key, fretRange, inversionMode.key, workspaceTab.key, melodyToneIndexes
    ) {
        if (workspaceTab == HarmonyWorkspaceTab.CHORD_MELODY && guidedLesson == null) {
            buildChordMelodyVoicings(
                chords = chords,
                stringSet = stringSet,
                fretRange = fretRange,
                melodyToneIndexes = melodyToneIndexes
            )
        } else {
            buildHarmonyVoicings(
                chords = chords,
                stringSet = stringSet,
                fretRange = fretRange,
                inversionMode = inversionMode
            )
        }
    }
    val sequence = remember(
        voicings, direction.key, subdivision.key, beatsPerChord, pickingPattern.key, workspaceTab.key
    ) {
        if (workspaceTab == HarmonyWorkspaceTab.CHORD_MELODY && guidedLesson == null) {
            buildChordMelodyPlaybackSequence(
                voicings = voicings,
                beatsPerChord = beatsPerChord
            )
        } else {
            buildHarmonyPlaybackSequence(
                voicings = voicings,
                direction = direction,
                subdivision = subdivision,
                beatsPerChord = beatsPerChord,
                pickingPattern = pickingPattern
            )
        }
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
                    if (workspaceTab == HarmonyWorkspaceTab.CHORD_MELODY && guidedLesson == null) {
                        if (step.beatNumber == 1) {
                            // Sound the voicing once per chord, not an asynchronous
                            // 250 ms note cascade restarted on every beat.
                            CoreAudioPlayer.playStrumRoll(
                                step.voicing.notes.sortedBy { it.midi }.map { it.midi to 0.72f },
                                "Guitar",
                                stringGapMs = 18L
                            )
                        }
                    } else {
                        CoreAudioPlayer.playChord(listOf(step.note.midi), "Guitar")
                    }
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
    val customTonalityOptions = HarmonyTonality.values().map { it.name to it.title(currentLang) }
    val customDegreeOptions = (1..7).map { degree ->
        val spec = harmonyDegreeSpecFor(customTonality, degree)
        degree.toString() to "${spec.roman} · ${fingerUiText(currentLang, "Bậc", "Degree")} $degree"
    }
    val currentMelodyChord = chords.getOrNull(currentChordIndex)
    val currentMelodyToneIndex = melodyToneIndexes.getOrElse(currentChordIndex) { 0 }
        .coerceIn(0, currentMelodyChord?.intervals?.lastIndex ?: 0)
    val melodyToneOptions = currentMelodyChord?.intervals?.mapIndexed { index, interval ->
        val noteName = harmonyNoteNames[normalizedPitchClass(currentMelodyChord.rootPitchClass + interval)]
        index.toString() to "${harmonyRoleLabel(interval)} · $noteName"
    }.orEmpty()
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
                                ?: if (workspaceTab == HarmonyWorkspaceTab.CHORD_MELODY) {
                                    "Chord Melody"
                                } else {
                                    fingerUiText(currentLang, "Luyện hòa âm", "Harmony practice")
                                },
                            color = textColor,
                            fontSize = if (veryCompactHeight) 16.sp else 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (guidedLesson != null) {
                                "${fingerUiText(currentLang, "Lộ trình", "Guided")} · $keyName · ${progression.title(currentLang)}"
                            } else if (workspaceTab == HarmonyWorkspaceTab.CHORD_MELODY) {
                                "$keyName · ${progression.title(currentLang)} · ${fingerUiText(currentLang, "melody ở bè trên", "top-note melody")}"
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

                if (guidedLesson == null) {
                    FingerPracticeSectionSelector(
                        selectedSection = FingerPracticeSection.HARMONY,
                        currentLang = currentLang,
                        exerciseAccent = Color(0xFF22C58B),
                        handShapeAccent = Color(0xFFE85D86),
                        harmonyAccent = Color(0xFFF2B94B),
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        compact = veryCompactHeight,
                        onSelected = { section ->
                            when (section) {
                                FingerPracticeSection.EXERCISES -> {
                                    resetSession()
                                    onOpenExercises()
                                }
                                FingerPracticeSection.HAND_SHAPES -> {
                                    resetSession()
                                    onOpenHandShapes()
                                }
                                FingerPracticeSection.HARMONY -> Unit
                            }
                        }
                    )
                    HarmonyWorkspaceSelector(
                        selectedTab = workspaceTab,
                        currentLang = currentLang,
                        accent = uiAccent,
                        melodyAccent = currentAccent,
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        compact = veryCompactHeight,
                        onSelected = { selected ->
                            resetSession()
                            workspaceKey = selected.key
                        }
                    )
                    HarmonyLibrarySelector(
                        selectedTab = libraryTab,
                        currentLang = currentLang,
                        accent = if (workspaceTab == HarmonyWorkspaceTab.CHORD_MELODY) currentAccent else uiAccent,
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        compact = veryCompactHeight,
                        onSelected = { selected ->
                            resetSession()
                            libraryKey = selected.key
                        }
                    )
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
                        HarmonyChordFlowStrip(
                            baseChords = baseChords,
                            activeChords = chords,
                            appliedPassingByGap = appliedPassingByGap,
                            currentChordIndex = currentChordIndex,
                            workspaceTab = workspaceTab,
                            melodyToneIndexes = melodyToneIndexes,
                            currentLang = currentLang,
                            accent = uiAccent,
                            melodyAccent = currentAccent,
                            textColor = textColor,
                            compact = veryCompactHeight,
                            onGapSelected = { gap ->
                                if (workspaceTab == HarmonyWorkspaceTab.PROGRESSION) {
                                    isPlaying = false
                                    countdownValue = 0
                                    stopPassingPreview()
                                    selectedPassingGap = gap
                                }
                            },
                            onChordSelected = { chordIndex ->
                                isPlaying = false
                                countdownValue = 0
                                val targetIndex = sequence.indexOfFirst { it.chordIndex == chordIndex }
                                if (targetIndex >= 0) {
                                    currentIndex = targetIndex
                                    hasPlaybackStarted = true
                                }
                            }
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
                                melodyPitchClass = if (workspaceTab == HarmonyWorkspaceTab.CHORD_MELODY) {
                                    currentVoicing?.notes?.maxByOrNull { it.midi }?.pitchClass
                                } else null,
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
                                if (workspaceTab == HarmonyWorkspaceTab.CHORD_MELODY) {
                                    HarmonyLegendItem(
                                        fingerUiText(currentLang, "Melody", "Melody"),
                                        currentAccent,
                                        textColor
                                    )
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
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(start = 3.dp, bottom = 1.dp)
                            )

                            if (guidedLesson == null) {
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

                                if (libraryTab == HarmonyLibraryTab.PRESET) {
                                    CompactDropdownSelector(
                                        label = fingerUiText(currentLang, "Vòng mẫu", "Preset progression"),
                                        selectedKey = presetProgression.key,
                                        selectedLabel = presetProgression.title(currentLang),
                                        options = progressionOptions,
                                        accent = uiAccent,
                                        surfaceColor = bgColor.copy(alpha = 0.50f),
                                        textColor = textColor,
                                        modifier = Modifier.fillMaxWidth().height(selectorHeight),
                                        onSelected = { progressionKey = it }
                                    )
                                } else {
                                    CompactDropdownSelector(
                                        label = fingerUiText(currentLang, "Hệ", "Tonality"),
                                        selectedKey = customTonality.name,
                                        selectedLabel = customTonality.title(currentLang),
                                        options = customTonalityOptions,
                                        accent = uiAccent,
                                        surfaceColor = bgColor.copy(alpha = 0.50f),
                                        textColor = textColor,
                                        modifier = Modifier.fillMaxWidth().height(selectorHeight),
                                        onSelected = { customTonalityKey = it }
                                    )
                                    customDegrees.forEachIndexed { slotIndex, degree ->
                                        CompactDropdownSelector(
                                            label = fingerUiText(
                                                currentLang,
                                                "Hợp âm ${slotIndex + 1}",
                                                "Chord ${slotIndex + 1}"
                                            ),
                                            selectedKey = degree.toString(),
                                            selectedLabel = customDegreeOptions.firstOrNull {
                                                it.first == degree.toString()
                                            }?.second ?: degree.toString(),
                                            options = customDegreeOptions,
                                            accent = if (slotIndex % 2 == 0) uiAccent else rootAccent,
                                            surfaceColor = bgColor.copy(alpha = 0.50f),
                                            textColor = textColor,
                                            modifier = Modifier.fillMaxWidth().height(selectorHeight),
                                            onSelected = { selectedDegree ->
                                                val updated = customDegrees.toMutableList()
                                                updated[slotIndex] = selectedDegree.toIntOrNull()?.coerceIn(1, 7) ?: degree
                                                customDegreeCsv = updated.joinToString(",")
                                            }
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        androidx.compose.material3.OutlinedButton(
                                            onClick = {
                                                if (customDegrees.size < 8) {
                                                    customDegreeCsv = (customDegrees + 1).joinToString(",")
                                                }
                                            },
                                            enabled = customDegrees.size < 8,
                                            modifier = Modifier.weight(1f).height(34.dp),
                                            contentPadding = PaddingValues(horizontal = 3.dp, vertical = 0.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(2.dp))
                                            Text(
                                                text = fingerUiText(currentLang, "Thêm", "Add"),
                                                fontSize = 8.sp,
                                                maxLines = 1
                                            )
                                        }
                                        androidx.compose.material3.OutlinedButton(
                                            onClick = {
                                                if (customDegrees.size > 2) {
                                                    customDegreeCsv = customDegrees.dropLast(1).joinToString(",")
                                                }
                                            },
                                            enabled = customDegrees.size > 2,
                                            modifier = Modifier.weight(1f).height(34.dp),
                                            contentPadding = PaddingValues(horizontal = 3.dp, vertical = 0.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(2.dp))
                                            Text(
                                                text = fingerUiText(currentLang, "Bớt", "Remove"),
                                                fontSize = 8.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                if (workspaceTab == HarmonyWorkspaceTab.PROGRESSION) {
                                    CompactDropdownSelector(
                                        label = fingerUiText(currentLang, "Kiểu hợp âm", "Chord type"),
                                        selectedKey = selectedPracticeType.key,
                                        selectedLabel = selectedPracticeType.title(currentLang),
                                        options = practiceTypeOptions,
                                        accent = uiAccent,
                                        surfaceColor = bgColor.copy(alpha = 0.50f),
                                        textColor = textColor,
                                        modifier = Modifier.fillMaxWidth().height(selectorHeight),
                                        onSelected = { practiceTypeKey = it }
                                    )
                                } else {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().heightIn(min = selectorHeight),
                                        shape = RoundedCornerShape(11.dp),
                                        colors = CardDefaults.cardColors(containerColor = currentAccent.copy(alpha = 0.09f)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, currentAccent.copy(alpha = 0.32f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = fingerUiText(currentLang, "CHORD MELODY", "CHORD MELODY"),
                                                color = currentAccent,
                                                fontSize = 7.5.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                            Text(
                                                text = fingerUiText(
                                                    currentLang,
                                                    "Nốt melody luôn ở bè cao nhất của thế hợp âm 3 nốt.",
                                                    "The melody note stays on top of a compact three-note voicing."
                                                ),
                                                color = textColor.copy(alpha = 0.68f),
                                                fontSize = 8.sp,
                                                lineHeight = 11.sp
                                            )
                                        }
                                    }
                                    if (currentMelodyChord != null && melodyToneOptions.isNotEmpty()) {
                                        CompactDropdownSelector(
                                            label = fingerUiText(currentLang, "Nốt melody", "Melody note"),
                                            selectedKey = currentMelodyToneIndex.toString(),
                                            selectedLabel = melodyToneOptions.firstOrNull {
                                                it.first == currentMelodyToneIndex.toString()
                                            }?.second ?: currentMelodyChord.rootName,
                                            options = melodyToneOptions,
                                            accent = currentAccent,
                                            surfaceColor = bgColor.copy(alpha = 0.50f),
                                            textColor = textColor,
                                            modifier = Modifier.fillMaxWidth().height(selectorHeight),
                                            onSelected = { selectedIndex ->
                                                val updated = melodyToneIndexes.toMutableList()
                                                while (updated.size < chords.size) updated.add(0)
                                                updated[currentChordIndex.coerceIn(0, updated.lastIndex)] =
                                                    selectedIndex.toIntOrNull() ?: 0
                                                melodyToneCsv = updated.joinToString(",")
                                                resetSession()
                                            }
                                        )
                                    }
                                }

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
                                if (workspaceTab == HarmonyWorkspaceTab.PROGRESSION) {
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
                                }
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
                            if (workspaceTab == HarmonyWorkspaceTab.PROGRESSION || guidedLesson != null) {
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
                            }
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
                            if (workspaceTab == HarmonyWorkspaceTab.PROGRESSION || guidedLesson != null) {
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
                            }
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
                                PracticeBpmStepper(
                                    bpm = bpm.roundToInt(),
                                    minBpm = 40,
                                    maxBpm = 180,
                                    accent = uiAccent,
                                    textColor = textColor,
                                    onDecrease = {
                                        bpm = (bpm - 5f).coerceAtLeast(40f)
                                        roundsAtCurrentBpm = 0
                                    },
                                    onIncrease = {
                                        bpm = (bpm + 5f).coerceAtMost(180f)
                                        roundsAtCurrentBpm = 0
                                    }
                                )
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

    if (selectedPassingGap in 0 until baseChords.lastIndex) {
        val fromChord = baseChords[selectedPassingGap]
        val toChord = baseChords[selectedPassingGap + 1]
        PassingChordSuggestionPopup(
            fromChord = fromChord,
            toChord = toChord,
            gapIndex = selectedPassingGap,
            currentApplied = appliedPassingByGap[selectedPassingGap],
            currentLang = currentLang,
            previewBpm = bpm.roundToInt(),
            accent = uiAccent,
            surfaceColor = surfaceColor,
            textColor = textColor,
            onPreview = { suggestion ->
                // Exactly one audible demonstration at a time. The previous job is
                // fully cancelled before a new one can create SoundPool streams.
                isPlaying = false
                countdownValue = 0
                val previousJob = passingPreviewJob
                previousJob?.cancel()
                passingPreviewJob = scope.launch {
                    previousJob?.join()
                    try {
                        val voicingMidis = passingChordPreviewVoicings(
                            fromChord = fromChord,
                            suggestion = suggestion,
                            toChord = toChord,
                            stringSet = stringSet,
                            practiceNoteCount = practiceType.noteCount,
                            fretRange = fretRange
                        )
                        val beatMs = (60_000f / bpm.coerceIn(40f, 180f)).toLong()
                        val durations = listOf(2L * beatMs, beatMs, 2L * beatMs)
                        voicingMidis.take(3).forEachIndexed { index, midis ->
                            CoreAudioPlayer.playGuitarChordPreview(
                                midis = midis,
                                volume = 0.68f,
                                strumGapMs = 22L
                            )
                            delay(durations[index])
                        }
                    } finally {
                        CoreAudioPlayer.stopGuitarChordPreview()
                    }
                }
            },
            onApply = { suggestion ->
                stopPassingPreview()
                appliedPassingByGap = appliedPassingByGap + (selectedPassingGap to suggestion)
                selectedPassingGap = -1
                resetSession()
            },
            onRemove = {
                stopPassingPreview()
                appliedPassingByGap = appliedPassingByGap - selectedPassingGap
                selectedPassingGap = -1
                resetSession()
            },
            onDismiss = {
                stopPassingPreview()
                selectedPassingGap = -1
            }
        )
    }

    if (countdownValue > 0) {
        PracticeCountdownOverlay(value = countdownValue, accent = uiAccent)
    }
}


@Composable
private fun HarmonyWorkspaceSelector(
    selectedTab: HarmonyWorkspaceTab,
    currentLang: String,
    accent: Color,
    melodyAccent: Color,
    surfaceColor: Color,
    textColor: Color,
    compact: Boolean,
    onSelected: (HarmonyWorkspaceTab) -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 30.dp else 34.dp)
            .clip(shape)
            .background(surfaceColor)
            .border(1.dp, textColor.copy(alpha = 0.09f), shape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val entries = listOf(
            Triple(
                HarmonyWorkspaceTab.PROGRESSION,
                fingerUiText(currentLang, "Vòng hợp âm", "Chord progression"),
                accent
            ),
            Triple(
                HarmonyWorkspaceTab.CHORD_MELODY,
                "Chord Melody",
                melodyAccent
            )
        )
        entries.forEach { (tab, label, tabAccent) ->
            val selected = selectedTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) tabAccent.copy(alpha = 0.18f) else Color.Transparent)
                    .border(
                        if (selected) 1.dp else 0.dp,
                        if (selected) tabAccent.copy(alpha = 0.58f) else Color.Transparent,
                        RoundedCornerShape(9.dp)
                    )
                    .clickable { onSelected(tab) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) tabAccent else textColor.copy(alpha = 0.64f),
                    fontSize = if (compact) 11.sp else 12.sp,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HarmonyLibrarySelector(
    selectedTab: HarmonyLibraryTab,
    currentLang: String,
    accent: Color,
    surfaceColor: Color,
    textColor: Color,
    compact: Boolean,
    onSelected: (HarmonyLibraryTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 27.dp else 31.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        val entries = listOf(
            HarmonyLibraryTab.PRESET to fingerUiText(currentLang, "Vòng mẫu", "Presets"),
            HarmonyLibraryTab.MY_LOOP to fingerUiText(currentLang, "Vòng của tôi", "My progression")
        )
        entries.forEach { (tab, label) ->
            val selected = selectedTab == tab
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSelected(tab) },
                shape = RoundedCornerShape(10.dp),
                color = if (selected) accent.copy(alpha = 0.13f) else surfaceColor.copy(alpha = 0.74f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (selected) accent.copy(alpha = 0.50f) else textColor.copy(alpha = 0.08f)
                )
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        color = if (selected) accent else textColor.copy(alpha = 0.60f),
                        fontSize = if (compact) 10.5.sp else 11.5.sp,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun HarmonyChordFlowStrip(
    baseChords: List<HarmonyChord>,
    activeChords: List<HarmonyChord>,
    appliedPassingByGap: Map<Int, PassingChordSuggestion>,
    currentChordIndex: Int,
    workspaceTab: HarmonyWorkspaceTab,
    melodyToneIndexes: List<Int>,
    currentLang: String,
    accent: Color,
    melodyAccent: Color,
    textColor: Color,
    compact: Boolean,
    onGapSelected: (Int) -> Unit,
    onChordSelected: (Int) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        var flattenedIndex = 0
        baseChords.forEachIndexed { baseIndex, chord ->
            val chordIndex = flattenedIndex
            HarmonyChordFlowChip(
                chord = activeChords.getOrElse(chordIndex) { chord },
                selected = currentChordIndex == chordIndex,
                passing = false,
                melodyToneIndex = melodyToneIndexes.getOrElse(chordIndex) { 0 },
                workspaceTab = workspaceTab,
                currentLang = currentLang,
                accent = accent,
                melodyAccent = melodyAccent,
                textColor = textColor,
                compact = compact,
                onClick = { onChordSelected(chordIndex) }
            )
            flattenedIndex++

            if (baseIndex < baseChords.lastIndex) {
                val applied = appliedPassingByGap[baseIndex]
                Text(
                    text = "→",
                    color = textColor.copy(alpha = 0.38f),
                    fontSize = if (compact) 10.sp else 12.sp,
                    fontWeight = FontWeight.Bold
                )
                if (applied != null) {
                    val passingIndex = flattenedIndex
                    HarmonyChordFlowChip(
                        chord = activeChords.getOrElse(passingIndex) { applied.chord },
                        selected = currentChordIndex == passingIndex,
                        passing = true,
                        melodyToneIndex = melodyToneIndexes.getOrElse(passingIndex) { 1 },
                        workspaceTab = workspaceTab,
                        currentLang = currentLang,
                        accent = accent,
                        melodyAccent = melodyAccent,
                        textColor = textColor,
                        compact = compact,
                        onClick = { onGapSelected(baseIndex) }
                    )
                    flattenedIndex++
                    Text(
                        text = "→",
                        color = textColor.copy(alpha = 0.38f),
                        fontSize = if (compact) 10.sp else 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else if (workspaceTab == HarmonyWorkspaceTab.PROGRESSION) {
                    Box(
                        modifier = Modifier
                            .size(if (compact) 22.dp else 25.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.11f))
                            .border(1.dp, accent.copy(alpha = 0.45f), CircleShape)
                            .clickable { onGapSelected(baseIndex) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            color = accent,
                            fontSize = if (compact) 13.sp else 15.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        text = "→",
                        color = textColor.copy(alpha = 0.38f),
                        fontSize = if (compact) 10.sp else 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun HarmonyChordFlowChip(
    chord: HarmonyChord,
    selected: Boolean,
    passing: Boolean,
    melodyToneIndex: Int,
    workspaceTab: HarmonyWorkspaceTab,
    currentLang: String,
    accent: Color,
    melodyAccent: Color,
    textColor: Color,
    compact: Boolean,
    onClick: () -> Unit
) {
    val chipAccent = if (workspaceTab == HarmonyWorkspaceTab.CHORD_MELODY) melodyAccent else accent
    val safeToneIndex = melodyToneIndex.coerceIn(0, chord.intervals.lastIndex.coerceAtLeast(0))
    val melodyName = harmonyNoteNames[
        normalizedPitchClass(chord.rootPitchClass + chord.intervals.getOrElse(safeToneIndex) { 0 })
    ]
    Surface(
        modifier = Modifier
            .width(if (compact) 62.dp else 72.dp)
            .height(if (workspaceTab == HarmonyWorkspaceTab.CHORD_MELODY) {
                if (compact) 41.dp else 47.dp
            } else {
                if (compact) 34.dp else 39.dp
            })
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = when {
            selected -> chipAccent.copy(alpha = 0.20f)
            passing -> accent.copy(alpha = 0.09f)
            else -> Color.Transparent
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when {
                selected -> chipAccent.copy(alpha = 0.75f)
                passing -> accent.copy(alpha = 0.38f)
                else -> textColor.copy(alpha = 0.10f)
            }
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = chord.symbol,
                color = if (selected) chipAccent else textColor,
                fontSize = if (compact) 9.sp else 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = when {
                    workspaceTab == HarmonyWorkspaceTab.CHORD_MELODY ->
                        fingerUiText(currentLang, "Melody $melodyName", "Melody $melodyName")
                    passing -> "P · ${chord.roman}"
                    else -> chord.roman
                },
                color = when {
                    workspaceTab == HarmonyWorkspaceTab.CHORD_MELODY -> melodyAccent
                    passing -> accent
                    else -> textColor.copy(alpha = 0.52f)
                },
                fontSize = if (compact) 6.8.sp else 7.8.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PassingChordSuggestionPopup(
    fromChord: HarmonyChord,
    toChord: HarmonyChord,
    gapIndex: Int,
    currentApplied: PassingChordSuggestion?,
    currentLang: String,
    previewBpm: Int,
    accent: Color,
    surfaceColor: Color,
    textColor: Color,
    onPreview: (PassingChordSuggestion) -> Unit,
    onApply: (PassingChordSuggestion) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val suggestions = remember(fromChord, toChord, gapIndex) {
        passingChordSuggestions(fromChord, toChord, gapIndex)
    }
    val maxDialogHeight = (LocalConfiguration.current.screenHeightDp * 0.78f)
        .coerceIn(300f, 660f).dp
    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = maxDialogHeight)
                    .clickable { },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.42f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fingerUiText(currentLang, "Gợi ý hợp âm chuyển", "Passing-chord suggestions"),
                                color = textColor,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${fromChord.symbol}  →  ${toChord.symbol}",
                                color = accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = textColor)
                        }
                    }
                    Text(
                        text = fingerUiText(
                            currentLang,
                            "Nghe theo nhịp $previewBpm BPM: gốc 2 phách · chuyển 1 phách · đích 2 phách",
                            "At $previewBpm BPM: original 2 beats · passing 1 beat · target 2 beats"
                        ),
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.74f),
                        lineHeight = 14.sp
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 6.dp)
                    ) {
                        items(suggestions, key = { it.key }) { suggestion ->
                            val selected = currentApplied?.key == suggestion.key
                            Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) accent.copy(alpha = 0.13f)
                                else textColor.copy(alpha = 0.035f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selected) accent.copy(alpha = 0.65f) else textColor.copy(alpha = 0.09f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${fromChord.symbol} → ${suggestion.chord.symbol} → ${toChord.symbol}",
                                            color = textColor,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "${suggestion.type(currentLang)} · ${suggestion.difficulty(currentLang)}",
                                            color = accent,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (selected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = accent)
                                    }
                                }
                                Text(
                                    text = suggestion.explanation(currentLang),
                                    color = textColor.copy(alpha = 0.68f),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                                Text(
                                    text = "Bass: ${suggestion.bassLine}",
                                    color = textColor.copy(alpha = 0.52f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    androidx.compose.material3.OutlinedButton(
                                        onClick = { onPreview(suggestion) },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(fingerUiText(currentLang, "Nghe thử", "Preview"), fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = { onApply(suggestion) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Text(
                                            fingerUiText(currentLang, "Áp dụng", "Apply"),
                                            color = Color(0xFF17191C),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                        }
                    }
                    if (currentApplied != null) {
                        androidx.compose.material3.TextButton(
                            onClick = onRemove,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = fingerUiText(currentLang, "Xóa hợp âm chuyển", "Remove passing chord"),
                                color = Color(0xFFEF5C6C),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
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
    val keyword = when {
        label.startsWith("Tăng tốc") -> "Tăng tốc"
        label.startsWith("Speed") -> "Speed"
        else -> label
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.12f) else backgroundColor)
            .border(1.dp, if (selected) accent.copy(alpha = 0.4f) else textColor.copy(alpha = 0.12f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) accent else textColor.copy(alpha = 0.68f),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = keyword,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor.copy(alpha = 0.92f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        PracticeCompactToggle(selected, accent, textColor)
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
    melodyPitchClass: Int? = null,
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
            melodyPitchClass != null && note.pitchClass == melodyPitchClass -> currentAccent
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
    val configuration = LocalConfiguration.current
    val dialogMaxHeight = (configuration.screenHeightDp * 0.82f).dp
    val scaleListState = rememberLazyListState()

    // textColor sáng nghĩa là màn hình đang ở dark mode và ngược lại.
    // Dùng bảng màu riêng cho modal để không còn bị Material DropdownMenu
    // tự phủ nền xám, đồng thời vẫn bảo đảm độ tương phản ở cả hai theme.
    val isDarkUi = (textColor.red + textColor.green + textColor.blue) > 1.5f
    val pickerBackground = if (isDarkUi) Color(0xFF101317) else Color(0xFFF8FAFC)
    val pickerRaised = if (isDarkUi) Color(0xFF181D23) else Color(0xFFFFFFFF)
    val pickerText = if (isDarkUi) Color(0xFFF5F7FA) else Color(0xFF17202A)
    val pickerMuted = if (isDarkUi) Color(0xFFAEB7C2) else Color(0xFF647180)
    val pickerBorder = if (isDarkUi) Color(0xFF2A323C) else Color(0xFFDCE2E8)

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
                .padding(start = if (compact) 3.dp else 4.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(if (compact) 15.dp else 16.dp)
            )
            Text(
                text = practiceSettingKeyword(label),
                color = textColor.copy(alpha = 0.90f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = selectedScale.title(currentLang),
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }
    }

    LaunchedEffect(expanded, levelKey, selectedScale.key) {
        if (expanded) {
            var targetIndex = 0
            if (levelKey == selectedScale.levelKey) {
                var runningIndex = 0
                handShapeScaleGroupsForLevel(levelKey).forEach { groupKey ->
                    val groupItems = options.filter {
                        it.levelKey == levelKey && it.groupKey == groupKey
                    }
                    if (groupItems.isNotEmpty()) {
                        runningIndex += 1 // group header
                        val selectedIndex = groupItems.indexOf(selectedScale)
                        if (selectedIndex >= 0) {
                            targetIndex = (runningIndex + selectedIndex - 1).coerceAtLeast(0)
                            return@forEach
                        }
                        runningIndex += groupItems.size
                    }
                }
            }
            scaleListState.scrollToItem(targetIndex)
        }
    }

    if (expanded) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { expanded = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.93f)
                    .heightIn(max = dialogMaxHeight)
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(22.dp),
                color = pickerBackground,
                tonalElevation = 0.dp,
                shadowElevation = 18.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    accent.copy(alpha = if (isDarkUi) 0.42f else 0.30f)
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, top = 15.dp, end = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fingerUiText(currentLang, "Chọn âm giai", "Select scale"),
                                color = pickerText,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${selectedScale.title(currentLang)}  •  ${selectedScale.formula}",
                                color = pickerMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { expanded = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = fingerUiText(currentLang, "Đóng", "Close"),
                                tint = pickerMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        levelOrder.forEach { optionLevel ->
                            val selectedLevel = optionLevel == levelKey
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { levelKey = optionLevel },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedLevel) accent.copy(alpha = 0.18f) else pickerRaised,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (selectedLevel) 1.4.dp else 1.dp,
                                    if (selectedLevel) accent else pickerBorder
                                ),
                                tonalElevation = 0.dp
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = handShapeScaleLevelTitle(optionLevel, currentLang),
                                        color = if (selectedLevel) accent else pickerMuted,
                                        fontSize = 11.sp,
                                        fontWeight = if (selectedLevel) FontWeight.ExtraBold else FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = pickerBorder,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    LazyColumn(
                        state = scaleListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = 10.dp,
                            bottom = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        handShapeScaleGroupsForLevel(levelKey).forEach { groupKey ->
                            val groupItems = options.filter {
                                it.levelKey == levelKey && it.groupKey == groupKey
                            }
                            if (groupItems.isNotEmpty()) {
                                item(key = "header_${levelKey}_$groupKey") {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        color = accent.copy(alpha = if (isDarkUi) 0.10f else 0.08f),
                                        tonalElevation = 0.dp
                                    ) {
                                        Text(
                                            text = handShapeScaleGroupTitle(groupKey, currentLang),
                                            color = accent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }

                                items(
                                    items = groupItems,
                                    key = { option -> "${levelKey}_${groupKey}_${option.key}" }
                                ) { option ->
                                    val selected = option == selectedScale
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(13.dp))
                                            .clickable {
                                                expanded = false
                                                onSelected(option)
                                            },
                                        shape = RoundedCornerShape(13.dp),
                                        color = if (selected) accent.copy(alpha = 0.15f) else pickerRaised,
                                        border = androidx.compose.foundation.BorderStroke(
                                            if (selected) 1.3.dp else 1.dp,
                                            if (selected) accent.copy(alpha = 0.82f) else pickerBorder
                                        ),
                                        tonalElevation = 0.dp
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 13.dp, vertical = 11.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(if (selected) accent else pickerMuted.copy(alpha = 0.34f))
                                            )
                                            Spacer(Modifier.width(11.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = option.title(currentLang),
                                                    color = if (selected) accent else pickerText,
                                                    fontSize = 13.5.sp,
                                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = option.formula,
                                                    color = if (selected) accent.copy(alpha = 0.82f) else pickerMuted,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (selected) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = accent,
                                                    tonalElevation = 0.dp
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = if (isDarkUi) Color(0xFF06130F) else Color.White,
                                                        modifier = Modifier
                                                            .padding(5.dp)
                                                            .size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun HandPositionModeSelector(
    isThreeNps: Boolean,
    isThreeNpsAvailable: Boolean,
    currentLang: String,
    accent: Color,
    surfaceColor: Color,
    textColor: Color,
    compact: Boolean,
    onCaged: () -> Unit,
    onThreeNps: () -> Unit,
    onFullNeck: () -> Unit
) {
    val items = listOf(
        Triple("CAGED", !isThreeNps, onCaged),
        Triple("3NPS", isThreeNps, onThreeNps),
        Triple(fingerUiText(currentLang, "Toàn cần đàn", "Full neck"), false, onFullNeck)
    )
    Row(
        modifier = Modifier.fillMaxWidth().height(if (compact) 28.dp else 33.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        items.forEachIndexed { index, (title, selected, action) ->
            val isUnavailable = index == 1 && !isThreeNpsAvailable
            Box(
                modifier = Modifier
                    .weight(if (index == 2) 1.35f else 1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) accent.copy(alpha = 0.19f) else surfaceColor)
                    .border(
                        1.dp,
                        if (selected) accent.copy(alpha = 0.75f) else textColor.copy(alpha = 0.15f),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { action() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isUnavailable) "$title ·" else title,
                    fontSize = if (compact) 9.sp else 11.sp,
                    color = when {
                        selected -> accent
                        isUnavailable -> textColor.copy(alpha = 0.43f)
                        else -> textColor.copy(alpha = 0.87f)
                    },
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ThreeNpsPositionSelector(
    selectedPosition: Int,
    accent: Color,
    textColor: Color,
    surfaceColor: Color,
    compact: Boolean,
    onSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(if (compact) 25.dp else 29.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        (0..6).forEach { position ->
            val selected = selectedPosition == position
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (selected) accent.copy(alpha = 0.19f) else surfaceColor)
                    .border(1.dp, if (selected) accent else textColor.copy(alpha = 0.12f), RoundedCornerShape(7.dp))
                    .clickable { onSelected(position) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${position + 1}",
                    color = if (selected) accent else textColor.copy(alpha = 0.74f),
                    fontSize = if (compact) 10.sp else 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
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
                    fontSize = if (compact) 11.sp else 12.sp,
                    fontWeight = if (selected) FontWeight.Black else FontWeight.Bold
                )
            }
        }
    }
}

// Compact player controls: show readable keywords in the narrow side panel.
// The complete labels/options remain available in each selector's popup.
private fun practiceSettingKeyword(label: String): String {
    val low = label.lowercase(Locale.ROOT)
    return when {
        low.startsWith("bài tập") -> "Bài"
        low.startsWith("exercise") -> "Lesson"
        low.startsWith("hướng luyện") -> "Hướng"
        low == "direction" -> "Way"
        low.startsWith("tay gảy") -> "Gảy"
        low == "picking" -> "Pick"
        low.startsWith("vị trí tay") -> "Tay"
        low.startsWith("hand position") -> "Hand"
        low.startsWith("vị trí cần đàn") || low.startsWith("phạm vi cần đàn") -> "Ngăn"
        low == "fret range" -> "Frets"
        low.startsWith("thế tay") -> "Thế"
        low == "shape" -> "Shape"
        low.startsWith("tông") -> "Tông"
        low == "key" -> "Key"
        low == "âm giai" -> "Âm giai"
        low == "scale" -> "Scale"
        low.startsWith("vòng mẫu") -> "Vòng"
        low.startsWith("preset progression") -> "Preset"
        low.startsWith("kiểu hợp âm") -> "Kiểu"
        low.startsWith("chord type") -> "Type"
        low.startsWith("hợp âm ") -> "Hợp âm"
        low.startsWith("chord ") -> "Chord"
        low.startsWith("nốt melody") -> "Melody"
        low.startsWith("melody note") -> "Melody"
        low.startsWith("nhóm dây") -> "Dây"
        low.startsWith("string set") -> "Strings"
        low.startsWith("đảo hợp âm") -> "Đảo"
        low.startsWith("inversion") -> "Inv."
        low.startsWith("thời lượng") -> "Phách"
        low.startsWith("chord duration") -> "Beats"
        low.startsWith("vị trí") -> "Ngăn"
        low.startsWith("position") -> "Frets"
        else -> label
    }
}

private fun practiceSettingValue(label: String, key: String, fullValue: String): String {
    val low = label.lowercase(Locale.ROOT)
    return when {
        low.startsWith("bài tập") || low == "exercise" ->
            fullValue.trim().takeWhile { it.isDigit() }.ifBlank { fullValue }
        low.startsWith("hướng luyện") || low == "direction" -> when (key) {
            "FORWARD", "ASCENDING" -> "↑"
            "REVERSE", "DESCENDING" -> "↓"
            "BOTH", "UP_DOWN" -> "↕"
            else -> fullValue
        }
        low.startsWith("tay gảy") || low == "picking" -> when (key) {
            "ALTERNATE" -> "↓↑"
            "DOWN_ONLY" -> "↓"
            "UP_ONLY" -> "↑"
            "SWEEP" -> "Sweep"
            "LEGATO" -> "Legato"
            else -> fullValue
        }
        low.startsWith("vị trí tay") || low.startsWith("hand position") ||
                low.startsWith("vị trí cần đàn") || low.startsWith("phạm vi cần đàn") ||
                low == "fret range" || low == "position" ->
            fullValue.replace(Regex("^(Ngăn|Frets|Fret)\\s+", RegexOption.IGNORE_CASE), "")
        low.startsWith("thế tay") || low == "shape" ->
            fullValue.replace(Regex("^(Dạng|Shape)\\s+", RegexOption.IGNORE_CASE), "")
        else -> fullValue
    }
}

@Composable
private fun PracticeCompactToggle(
    checked: Boolean,
    accent: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 37.dp, height = 21.dp)
            .clip(CircleShape)
            .background(if (checked) accent else textColor.copy(alpha = 0.24f))
            .border(1.dp, if (checked) accent else textColor.copy(alpha = 0.30f), CircleShape)
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(15.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
private fun PracticeBpmStepper(
    bpm: Int,
    minBpm: Int,
    maxBpm: Int,
    accent: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    subdivisionLabel: String? = null,
    onSubdivisionClick: (() -> Unit)? = null,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        val decreaseEnabled = bpm > minBpm
        val increaseEnabled = bpm < maxBpm
        Box(
            modifier = Modifier
                .width(34.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = if (decreaseEnabled) 0.17f else 0.07f))
                .clickable(enabled = decreaseEnabled, onClick = onDecrease),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "−5 BPM",
                tint = accent.copy(alpha = if (decreaseEnabled) 1f else 0.35f),
                modifier = Modifier.size(20.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .then(if (onSubdivisionClick != null) Modifier.clickable(onClick = onSubdivisionClick) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$bpm BPM",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            if (subdivisionLabel != null) {
                Text(
                    text = "▾ $subdivisionLabel",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
        Box(
            modifier = Modifier
                .width(34.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = if (increaseEnabled) 0.17f else 0.07f))
                .clickable(enabled = increaseEnabled, onClick = onIncrease),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "+5 BPM",
                tint = accent.copy(alpha = if (increaseEnabled) 1f else 0.35f),
                modifier = Modifier.size(20.dp)
            )
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
    val shape = RoundedCornerShape(10.dp)
    val keyword = practiceSettingKeyword(label)
    val value = practiceSettingValue(label, selectedKey, selectedLabel)
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(surfaceColor)
                .border(1.dp, textColor.copy(alpha = 0.17f), shape)
                .clickable { expanded = true }
                // The arrow is a leading affordance, not a trailing occupant
                // of the value column.  Keeping it at the left frees space
                // for a complete keyword (especially "Hướng").
                .padding(start = 4.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = keyword,
                color = textColor.copy(alpha = 0.90f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = value,
                color = textColor,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(surfaceColor)
        ) {
            Text(
                text = label,
                color = accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
            options.forEach { (key, optionLabel) ->
                androidx.compose.material3.DropdownMenuItem(
                    text = {
                        Text(
                            text = optionLabel,
                            color = if (key == selectedKey) accent else textColor,
                            fontSize = 14.sp,
                            fontWeight = if (key == selectedKey) FontWeight.Bold else FontWeight.Medium,
                        )
                    },
                    trailingIcon = if (key == selectedKey) {
                        { Icon(Icons.Default.Check, null, tint = accent, modifier = Modifier.size(18.dp)) }
                    } else null,
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
private fun HandShapeVisualModeSelector(
    selectedMode: HandShapeVisualMode,
    currentLang: String,
    accent: Color,
    harmonyAccent: Color,
    surfaceColor: Color,
    textColor: Color,
    compact: Boolean,
    onSelected: (HandShapeVisualMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 22.dp else 25.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(surfaceColor)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        HandShapeVisualMode.values().forEach { mode ->
            val selected = mode == selectedMode
            val modeAccent = if (mode == HandShapeVisualMode.HARMONIC_ROLE) {
                harmonyAccent
            } else {
                accent
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        if (selected) modeAccent.copy(alpha = 0.20f) else Color.Transparent
                    )
                    .border(
                        width = if (selected) 1.dp else 0.dp,
                        color = if (selected) modeAccent.copy(alpha = 0.72f) else Color.Transparent,
                        shape = RoundedCornerShape(7.dp)
                    )
                    .clickable { onSelected(mode) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.title(currentLang),
                    color = if (selected) modeAccent else textColor.copy(alpha = 0.58f),
                    fontSize = if (compact) 9.5.sp else 10.5.sp,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
            fontSize = 10.sp,
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
    val shortLabel = when {
        label.startsWith("Tăng tốc") -> "Tăng tốc"
        label.startsWith("Speed") -> "Speed"
        else -> label
    }
    Row(
        modifier = modifier
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (checked) accent else textColor.copy(alpha = 0.70f),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = shortLabel,
            color = textColor.copy(alpha = 0.91f),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        PracticeCompactToggle(
            checked = checked,
            accent = accent,
            textColor = textColor
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
    visualMode: HandShapeVisualMode,
    chordToneColor: Color,
    pentatonicToneColor: Color,
    tensionToneColor: Color,
    highTensionToneColor: Color,
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
    val harmonyProfile = remember(pattern.scaleType, visualMode) {
        if (visualMode == HandShapeVisualMode.HARMONIC_ROLE) {
            pattern.scaleType.harmonyProfile()
        } else {
            null
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
                val harmonyRole = harmonyProfile?.roleForInterval(note.intervalFromRoot)
                val baseColor = when (visualMode) {
                    HandShapeVisualMode.HARMONIC_ROLE -> when (harmonyRole) {
                        HandShapeHarmonyRole.CHORD_TONE -> chordToneColor
                        HandShapeHarmonyRole.PENTATONIC_TONE -> pentatonicToneColor
                        HandShapeHarmonyRole.TENSION_TONE -> tensionToneColor
                        HandShapeHarmonyRole.HIGH_TENSION -> highTensionToneColor
                        null -> scaleNoteColor
                    }
                    HandShapeVisualMode.SCALE_CHARACTER -> when {
                        note.isRoot -> rootAccent
                        characteristicIndex == 0 -> characteristicAccentPrimary
                        characteristicIndex == 1 -> characteristicAccentSecondary
                        else -> scaleNoteColor
                    }
                }
                val isHighlightedScaleRole = visualMode == HandShapeVisualMode.HARMONIC_ROLE ||
                        baseColor != scaleNoteColor

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
                            color = if (visualMode == HandShapeVisualMode.HARMONIC_ROLE) {
                                baseColor.copy(alpha = 0.30f)
                            } else {
                                Color.Transparent
                            },
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

                if (
                    visualMode == HandShapeVisualMode.HARMONIC_ROLE &&
                    note.isRoot &&
                    !isCurrent &&
                    !isNext
                ) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.82f),
                        radius = if (isCompleted) normalRadius * 0.90f else normalRadius * 0.86f,
                        center = center,
                        style = Stroke(width = with(density) { 1.1.dp.toPx() })
                    )
                }

                val label = if (visualMode == HandShapeVisualMode.HARMONIC_ROLE) {
                    pattern.scaleType.degreeLabelForInterval(note.intervalFromRoot)
                        .ifBlank { note.noteName }
                } else {
                    if (note.finger == 0) "O" else note.finger.toString()
                }
                val compactLabelScale = when {
                    label.length >= 3 -> 0.72f
                    label.length == 2 -> 0.84f
                    else -> 1f
                }
                val notePaint = android.graphics.Paint().apply {
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    textSize = (if (isCurrent) noteTextSizePx * pulse else noteTextSizePx) * compactLabelScale
                    color = when {
                        isNext -> android.graphics.Color.WHITE
                        isCurrent || isCompleted -> android.graphics.Color.WHITE
                        visualMode == HandShapeVisualMode.HARMONIC_ROLE &&
                                harmonyRole == HandShapeHarmonyRole.TENSION_TONE ->
                            android.graphics.Color.rgb(45, 40, 28)
                        isHighlightedScaleRole -> android.graphics.Color.WHITE
                        else -> android.graphics.Color.rgb(38, 40, 43)
                    }
                }
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
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(surfaceColor)
                .border(1.dp, textColor.copy(alpha = 0.16f), shape)
                .clickable { expanded = true }
                .padding(start = 4.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = practiceSettingKeyword(label),
                color = textColor.copy(alpha = 0.90f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = practiceSettingValue(label, selectedKey, selectedLabel),
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(surfaceColor)
        ) {
            Text(
                text = label,
                color = accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
            )
            options.forEach { (key, optionLabel) ->
                androidx.compose.material3.DropdownMenuItem(
                    text = {
                        Text(
                            text = optionLabel,
                            color = if (key == selectedKey) accent else textColor,
                            fontSize = 13.5.sp,
                            fontWeight = if (key == selectedKey) FontWeight.ExtraBold else FontWeight.Medium,
                        )
                    },
                    trailingIcon = if (key == selectedKey) {
                        { Icon(Icons.Default.Check, null, tint = accent, modifier = Modifier.size(17.dp)) }
                    } else null,
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

// Original educational visualisations for Tuner Tools. These are generated from
// music theory intervals, not copied from the third-party UMI APK.
private data class TheoryVisualExample(
    val titleVi: String,
    val titleEn: String,
    val explanationVi: String,
    val explanationEn: String,
    val notes: List<Int>,
    val degrees: String,
    val layout: String = "PIANO",
    val chordNames: List<String> = emptyList(),
    val chordNotes: List<List<Int>> = emptyList(),
    val pulses: Int = 0,
    val groupSize: Int = 0,
    val focusString: Int = 0
) {
    fun title(lang: String) = fingerUiText(lang, titleVi, titleEn)
    fun explanation(lang: String) = fingerUiText(lang, explanationVi, explanationEn)
    fun playback(): List<List<Int>> = when {
        chordNotes.isNotEmpty() -> chordNotes
        layout == "CHORD" || layout == "VOICING" -> listOf(notes)
        else -> notes.map { listOf(it) }
    }
}

private fun theoryVisualExamples(id: String): List<TheoryVisualExample> {
    fun ex(vi: String, en: String, tipVi: String, tipEn: String,
           notes: List<Int>, degrees: String, layout: String = "PIANO",
           names: List<String> = emptyList(), groups: List<List<Int>> = emptyList(),
           pulses: Int = 0, groupSize: Int = 0, focusString: Int = 0) =
        TheoryVisualExample(vi, en, tipVi, tipEn, notes, degrees, layout, names, groups, pulses, groupSize, focusString)
    return when (id) {
        "F01_STAFF" -> listOf(
            ex("5 dòng khóa Sol", "Treble clef lines", "Từ dưới lên: E4, G4, B4, D5, F5.", "Bottom to top: E4, G4, B4, D5, F5.", listOf(64,67,71,74,77), "E · G · B · D · F", "STAFF"),
            ex("4 khe khóa Sol", "Treble clef spaces", "Các khe từ dưới lên: F4, A4, C5, E5.", "Spaces from bottom up: F4, A4, C5, E5.", listOf(65,69,72,76), "F · A · C · E", "STAFF")
        )
        "F02_CLEFS" -> listOf(
            ex("Khóa Sol (treble)", "Treble clef", "Dòng thứ hai ghi G4; dòng thấp nhất E4.", "The second line is G4; the bottom line is E4.", listOf(64,67,71,74,77), "E4 · G4 · B4 · D5 · F5", "STAFF"),
            ex("Khóa Fa (bass)", "Bass clef", "Dòng thứ tư là F3, dòng thấp nhất G2.", "The fourth line is F3, the bottom line is G2.", listOf(43,47,50,53,57), "G2 · B2 · D3 · F3 · A3", "STAFF_BASS")
        )
        "F03_STAFF_NOTES" -> listOf(
            ex("Nốt trên dòng", "Notes on lines", "Các đầu nốt chạm đúng năm đường kẻ.", "The noteheads sit exactly on the five lines.", listOf(64,67,71,74,77), "E · G · B · D · F", "STAFF"),
            ex("Nốt trong khe", "Notes in spaces", "Mỗi khe nằm giữa hai dòng kẻ.", "Each space lies between two lines.", listOf(65,69,72,76), "F · A · C · E", "STAFF")
        )
        "F04_LEDGER" -> listOf(
            ex("Đô giữa (C4)", "Middle C (C4)", "C4 có một dòng phụ ở dưới khuông khóa Sol.", "C4 sits on a ledger line below the treble staff.", listOf(60,64,67,72), "C4 · E4 · G4 · C5", "STAFF"),
            ex("Nốt ngoài khuông", "Notes outside the staff", "A3 cần nhiều dòng phụ hơn nốt trong khuông.", "A3 requires more ledger lines below the staff.", listOf(57,60,64,81), "A3 · C4 · E4 · A5", "STAFF")
        )
        "F05_VALUES" -> listOf(
            ex("Nốt tròn · 4 phách", "Whole note · 4 beats", "Một nốt tròn điền trọn một ô 4/4.", "One whole note fills a 4/4 bar.", listOf(64), "1 × 4 = 4 phách", "DURATION", pulses=1, groupSize=4),
            ex("Nốt trắng · 2 phách", "Half note · 2 beats", "Hai nốt trắng có tổng bốn phách đen.", "Two half notes add up to four quarter beats.", listOf(64,64), "2 × 2 = 4 phách", "DURATION", pulses=2, groupSize=2),
            ex("Nốt đen · 1 phách", "Quarter note · 1 beat", "Bốn nốt đen điền ô 4/4.", "Four quarter notes fill a 4/4 bar.", List(4){64}, "4 × 1 = 4 phách", "DURATION", pulses=4, groupSize=1),
            ex("Móc đơn · 1/2", "Eighth note · 1/2", "Tám móc đơn điền ô 4/4.", "Eight eighth notes fill a 4/4 bar.", List(8){64}, "8 × 1/2 = 4 phách", "DURATION", pulses=8, groupSize=1),
            ex("Móc kép · 1/4", "Sixteenth note · 1/4", "Mười sáu móc kép điền ô 4/4.", "Sixteen sixteenths fill a 4/4 bar.", List(16){64}, "16 × 1/4 = 4 phách", "DURATION", pulses=16, groupSize=1)
        )
        "F06_RESTS" -> listOf(
            ex("Chơi – nghỉ – chơi – nghỉ", "Sound – rest – sound – rest", "Bốn phách vẫn diễn ra đều, ngay cả khi không nghe tiếng đàn ở phách nghỉ.", "All four beats continue evenly, even though rests are silent.", listOf(64,64), "♪ · nghỉ · ♪ · nghỉ", "RESTS", groups=listOf(listOf(64),emptyList(),listOf(64),emptyList()), pulses=4,groupSize=1)
        )
        "F07_DOTS_TIES" -> listOf(
            ex("Nốt đen chấm dôi", "Dotted quarter", "1 + 1/2 = 1,5 phách; không được nhầm với hai nốt đen.", "1 + 1/2 = 1.5 beats, not two quarters.", listOf(64), "1 + ½ = 1½ phách", "DOT", pulses=1,groupSize=4),
            ex("Hai nốt đen nối", "Two tied quarters", "Một nốt duy nhất ngân hai phách; không gảy lại ở nét nối.", "One note sustains for two beats without another attack.", listOf(64), "1 + 1 = 2 phách", "TIE", pulses=2,groupSize=2)
        )
        "F08_METER" -> listOf(
            ex("2/4", "2/4", "Hai phách đen mỗi ô; phách đầu mạnh.", "Two quarter-note beats per bar; first beat accented.", listOf(69,64), "MẠNH · nhẹ", "METER", pulses=2,groupSize=2),
            ex("3/4", "3/4", "Ba phách đen mỗi ô, nhấn một.", "Three quarter beats per bar, accent one.", listOf(69,64,64), "MẠNH · 2 · 3", "METER", pulses=3,groupSize=3),
            ex("4/4", "4/4", "Bốn phách đen mỗi ô; có thể nhấn phụ ở phách ba.", "Four quarter beats per bar; beat three may have secondary accent.", listOf(69,64,67,64), "1 · 2 · 3 · 4", "METER", pulses=4,groupSize=4),
            ex("6/8", "6/8", "Sáu móc đơn nhóm 3 + 3, cảm nhận hai phách lớn.", "Six eighth notes in 3+3 groups, felt as two main beats.", listOf(69,64,64,67,64,64), "1 2 3 | 4 5 6", "METER", pulses=6,groupSize=3)
        )
        "F09_TEMPO" -> listOf(
            ex("60 BPM", "60 BPM", "Mỗi phách cách nhau một giây (đơn vị: nốt đen).", "One second between beats (quarter-note beat unit).", List(4){64}, "60 BPM · 1,0 s/phách", "TEMPO", pulses=60),
            ex("120 BPM", "120 BPM", "Mỗi phách cách nhau nửa giây (đơn vị: nốt đen).", "Half a second between beats (quarter-note unit).", List(4){64}, "120 BPM · 0,5 s/phách", "TEMPO", pulses=120)
        )
        "F10_STEPS" -> listOf(
            ex("C → C♯", "C → C♯", "1 bán cung = 1 ngăn guitar trên cùng dây.", "One semitone equals one fret on the same string.", listOf(60,61), "½ cung · 1 bán cung"),
            ex("C → D", "C → D", "1 cung = 2 bán cung = 2 ngăn guitar.", "One whole tone equals two semitones or two frets.", listOf(60,62), "1 cung · 2 bán cung"),
            ex("B → C", "B → C", "Hai nốt tự nhiên B và C liền kề, không có phím đen xen vào.", "Natural notes B and C are adjacent with no black key.", listOf(59,60), "½ cung · 1 bán cung")
        )
        "F11_ACCIDENTALS" -> listOf(
            ex("C · C♯ · D", "C · C♯ · D", "Dấu ♯ nâng C thành C♯.", "A sharp raises C to C♯.", listOf(60,61,62), "C → C♯ → D"),
            ex("D♭ = C♯ (cao độ)", "D♭ = C♯ (pitch)", "Hai cách viết có thể cùng âm trong hệ bình quân 12 bán cung.", "Both spellings can share a pitch in 12-tone equal temperament.", listOf(61,61), "D♭ = C♯"),
            ex("B · C", "B · C", "Giữa B và C không có phím đen; khoảng cách là bán cung.", "B and C have no black key between them; they are a semitone apart.", listOf(59,60), "B → C")
        )
        "F12_INTERVALS" -> listOf(
            ex("Quãng 3 thứ", "Minor third", "C–E♭: 3 bán cung, ba tên nốt C–D–E.", "C–E♭: 3 semitones, letter distance C–D–E.", listOf(60,63), "♭3 · 3 semitones"),
            ex("Quãng 3 trưởng", "Major third", "C–E: 4 bán cung.", "C–E: 4 semitones.", listOf(60,64), "3 · 4 semitones"),
            ex("Quãng 5 đúng", "Perfect fifth", "C–G: 7 bán cung.", "C–G: 7 semitones.", listOf(60,67), "5 · 7 semitones"),
            ex("Quãng tám", "Octave", "C4–C5: 12 bán cung.", "C4–C5: 12 semitones.", listOf(60,72), "8 · 12 semitones")
        )
        "F13_MAJOR" -> listOf(
            ex("C trưởng", "C major", "Nốt C–E–G cho âm thanh của C trưởng.", "C–E–G form the C major triad.", listOf(60,64,67), "1 · 3 · 5", "CHORD"),
            ex("G trưởng", "G major", "G–B–D, giữ quãng 3 trưởng và quãng 5 đúng.", "G–B–D have a major third and perfect fifth.", listOf(55,59,62), "1 · 3 · 5", "CHORD")
        )
        "F14_MINOR" -> listOf(
            ex("C thứ", "C minor", "C–E♭–G: bậc 3 bị hạ nửa cung so với C trưởng.", "C–E♭–G: third is a semitone below C major's third.", listOf(60,63,67), "1 · ♭3 · 5", "CHORD"),
            ex("A thứ", "A minor", "A–C–E: hợp âm Am dạng mở phổ biến.", "A–C–E: the common open Am chord.", listOf(57,60,64), "1 · ♭3 · 5", "CHORD")
        )
        "F15_CHORD7" -> listOf(
            ex("Cmaj7", "Cmaj7", "C–E–G–B: bậc 7 trưởng.", "C–E–G–B: major seventh.", listOf(60,64,67,71), "1 · 3 · 5 · 7", "CHORD"),
            ex("C7", "C7", "C–E–G–B♭: bậc 7 giáng, là dominant seventh.", "C–E–G–B♭: lowered seventh; dominant seventh.", listOf(60,64,67,70), "1 · 3 · 5 · ♭7", "CHORD"),
            ex("Cm7", "Cm7", "C–E♭–G–B♭: bậc 3 và 7 đều giáng.", "C–E♭–G–B♭: flat third and seventh.", listOf(60,63,67,70), "1 · ♭3 · 5 · ♭7", "CHORD")
        )
        "F16_CHORD_DIAGRAM" -> listOf(
            ex("C trưởng · X32010", "C major · X32010", "X: không gảy dây 6; 0 là dây buông.", "X means do not play string 6; 0 is open.", listOf(48,52,55), "C · X 3 2 0 1 0", "CHORD_DIAGRAM", focusString=1),
            ex("Am · X02210", "Am · X02210", "Dây 5 buông, ngón 2–3 bấm ngăn 2.", "Open fifth string, fingers 2–3 at fret 2.", listOf(45,48,52), "Am · X 0 2 2 1 0", "CHORD_DIAGRAM", focusString=2),
            ex("G7 · 320001", "G7 · 320001", "G7 chứa G–B–D–F, khác G trưởng ở F.", "G7 includes G–B–D–F, unlike G major.", listOf(43,47,50,53), "G7 · 3 2 0 0 0 1", "CHORD_DIAGRAM", focusString=3)
        )
        "F17_FRETBOARD" -> listOf(
            ex("Sáu dây buông", "Six open strings", "Từ dây 6 đến 1: E2–A2–D3–G3–B3–E4.", "From string 6 to 1: E2–A2–D3–G3–B3–E4.", listOf(40,45,50,55,59,64), "E2 · A2 · D3 · G3 · B3 · E4", "TUNING"),
            ex("Dây 1 · ngăn 0/1/3", "String 1 · fret 0/1/3", "E4–F4–G4 trên cùng một dây.", "E4–F4–G4 on the same string.", listOf(64,65,67), "e: 0 · 1 · 3", "TAB", focusString=1)
        )
        "F18_TAB" -> listOf(
            ex("Dây 1 · e|--0--1--3", "String 1 · e|--0--1--3", "Dây e cao, buông là E4; ngăn 1 F4; ngăn 3 G4.", "High e string: open E4, fret 1 F4, fret 3 G4.", listOf(64,65,67), "e |--0--1--3--", "TAB", focusString=1),
            ex("Dây 2 · B|--0--1--3", "String 2 · B|--0--1--3", "Dây B buông B3, ngăn 1 C4, ngăn 3 D4.", "B string: open B3, fret 1 C4, fret 3 D4.", listOf(59,60,62), "B |--0--1--3--", "TAB", focusString=2),
            ex("Dây 6 · E|--0--3--5", "String 6 · E|--0--3--5", "Dây E trầm buông E2, ngăn 3 G2, ngăn 5 A2.", "Low E string: open E2, fret 3 G2, fret 5 A2.", listOf(40,43,45), "E |--0--3--5--", "TAB", focusString=6)
        )

        "T01_TUNING" -> listOf(
            ex("Dây 6 · E2", "String 6 · E2", "Dây dày nhất; âm E trầm (82,4 Hz).", "Thickest string; low E (82.4 Hz).", listOf(40), "E2", "TUNING", focusString = 6),
            ex("Dây 5 · A2", "String 5 · A2", "A2 cao hơn E2 một quãng 4 đúng.", "A2 is a perfect fourth above E2.", listOf(45), "A2", "TUNING", focusString = 5),
            ex("Dây 4 · D3", "String 4 · D3", "D3 là dây số 4 khi lên dây chuẩn.", "D3 is the fourth string in standard tuning.", listOf(50), "D3", "TUNING", focusString = 4),
            ex("Dây 3 · G3", "String 3 · G3", "G3 nằm dưới dây B3 một quãng 3 trưởng.", "G3 is a major third below B3.", listOf(55), "G3", "TUNING", focusString = 3),
            ex("Dây 2 · B3", "String 2 · B3", "Khoảng G3–B3 là ngoại lệ trong cách lên dây chuẩn.", "G3–B3 is the exception in standard string intervals.", listOf(59), "B3", "TUNING", focusString = 2),
            ex("Dây 1 · E4", "String 1 · E4", "Dây mảnh nhất; E cao hơn E2 hai quãng tám.", "Thinnest string; two octaves above E2.", listOf(64), "E4", "TUNING", focusString = 1)
        )
        "T02_DURATION" -> listOf(
            ex("Nốt đen · 1 phách", "Quarter notes · 1 beat", "Bốn nốt đen chiếm bốn phách.", "Four quarter notes occupy four beats.", List(4) { 64 }, "1 + 1 + 1 + 1", "RHYTHM", pulses = 4, groupSize = 1),
            ex("Móc đơn · 1/2 phách", "Eighth notes · 1/2 beat", "Tám móc đơn có tổng trường độ bằng bốn nốt đen.", "Eight eighth notes equal four quarter notes.", List(8) { 64 }, "1/2 × 8", "RHYTHM", pulses = 8, groupSize = 2),
            ex("Móc kép · 1/4 phách", "Sixteenth notes · 1/4 beat", "Mười sáu móc kép tương đương bốn phách đen.", "Sixteen sixteenth notes fill four quarter-note beats.", List(16) { 64 }, "1/4 × 16", "RHYTHM", pulses = 16, groupSize = 4)
        )
        "T03_METER" -> listOf(
            ex("3/4 · ba phách", "3/4 · three beats", "Nhấn vào phách 1; đếm MỘT–hai–ba.", "Accent beat 1; count ONE–two–three.", listOf(69, 64, 64), "1 · 2 · 3", "METER", pulses = 3, groupSize = 3),
            ex("4/4 · bốn phách", "4/4 · four beats", "Phách 1 mạnh; phách 3 thường có trọng âm phụ.", "Beat 1 is strong; beat 3 often has a secondary accent.", listOf(69, 64, 67, 64), "1 · 2 · 3 · 4", "METER", pulses = 4, groupSize = 4),
            ex("6/8 · nhịp kép", "6/8 · compound duple", "Sáu móc đơn được nhóm 3 + 3, cảm nhận hai phách lớn.", "Six eighths group 3 + 3, felt as two main beats.", listOf(69,64,64,67,64,64), "1 2 3 | 4 5 6", "METER", pulses = 6, groupSize = 3)
        )
        "T04_SEMITONES" -> listOf(
            ex("C → C♯", "C → C♯", "Nửa cung = 1 phím đàn piano liền kề hoặc 1 ngăn guitar.", "A semitone is the next piano key or one guitar fret.", listOf(60,61), "1 semitone"),
            ex("C → D", "C → D", "Một cung bằng hai nửa cung, tức hai ngăn cùng dây.", "A whole step spans two semitones, or two guitar frets.", listOf(60,62), "2 semitones"),
            ex("B → C", "B → C", "B–C cũng là nửa cung dù không có phím đen xen giữa.", "B–C is a semitone even without a black key between.", listOf(59,60), "1 semitone")
        )
        "T05_INTERVALS" -> listOf(
            ex("Quãng 3 thứ", "Minor third", "C lên E♭ = 3 bán cung. Đây là màu của hợp âm thứ.", "C to E♭ = 3 semitones, the minor-third sound.", listOf(60,63), "♭3 · 3 semitones"),
            ex("Quãng 3 trưởng", "Major third", "C lên E = 4 bán cung; phân biệt hợp âm trưởng.", "C to E = 4 semitones; defines major chord quality.", listOf(60,64), "3 · 4 semitones"),
            ex("Quãng 5 đúng", "Perfect fifth", "C lên G = 7 bán cung; tạo âm sắc ổn định.", "C to G = 7 semitones; a stable consonance.", listOf(60,67), "5 · 7 semitones"),
            ex("Quãng 8", "Octave", "C4 lên C5 = 12 bán cung, cùng tên nốt ở âm vực khác.", "C4 to C5 = 12 semitones; same note name in another register.", listOf(60,72), "8 · 12 semitones")
        )
        "T06_PENTATONIC" -> listOf(
            ex("C ngũ cung trưởng", "C major pentatonic", "C, D, E, G, A — không có bậc 4 và 7.", "C, D, E, G, A — no fourth or seventh degrees.", listOf(60,62,64,67,69), "1 · 2 · 3 · 5 · 6", "FRETBOARD"),
            ex("A ngũ cung thứ", "A minor pentatonic", "A, C, D, E, G — cùng bộ nốt nhưng A là chủ âm.", "A, C, D, E, G — same note set with A as tonic.", listOf(57,60,62,64,67), "1 · ♭3 · 4 · 5 · ♭7", "FRETBOARD")
        )
        "T07_TRIAD" -> listOf(
            ex("C trưởng · C–E–G", "C major · C–E–G", "Bậc 1, 3 trưởng, 5 đúng tạo hợp âm trưởng.", "Root, major third and perfect fifth form a major triad.", listOf(60,64,67), "1 · 3 · 5", "CHORD"),
            ex("C thứ · C–E♭–G", "C minor · C–E♭–G", "Hạ bậc 3 từ E xuống E♭ chuyển chất trưởng sang thứ.", "Lowering E to E♭ changes major into minor.", listOf(60,63,67), "1 · ♭3 · 5", "CHORD")
        )
        "T08_TRIAD_TYPES" -> listOf(
            ex("C trưởng", "C major", "C–E–G: quãng 3 trưởng + quãng 3 thứ.", "C–E–G: major third then minor third.", listOf(60,64,67), "1 · 3 · 5", "CHORD"),
            ex("C thứ", "C minor", "C–E♭–G: quãng 3 thứ + quãng 3 trưởng.", "C–E♭–G: minor third then major third.", listOf(60,63,67), "1 · ♭3 · 5", "CHORD"),
            ex("C tăng", "C augmented", "C–E–G♯: hai quãng 3 trưởng chồng nhau.", "C–E–G♯: two stacked major thirds.", listOf(60,64,68), "1 · 3 · ♯5", "CHORD"),
            ex("C giảm", "C diminished", "C–E♭–G♭: hai quãng 3 thứ chồng nhau.", "C–E♭–G♭: two stacked minor thirds.", listOf(60,63,66), "1 · ♭3 · ♭5", "CHORD")
        )
        "T09_INVERSIONS" -> listOf(
            ex("Thế gốc · C/E/G", "Root position · C/E/G", "Bass C: các nốt xếp từ thấp lên là C–E–G.", "C in bass: notes from low to high C–E–G.", listOf(48,52,55), "C  →  E  →  G", "VOICING"),
            ex("Đảo 1 · E/G/C", "1st inversion · E/G/C", "Bass E: nốt bậc 3 được đưa xuống thấp nhất.", "E in bass: the third becomes the lowest voice.", listOf(52,55,60), "E  →  G  →  C", "VOICING"),
            ex("Đảo 2 · G/C/E", "2nd inversion · G/C/E", "Bass G: nốt bậc 5 nằm dưới cùng.", "G in bass: the fifth is the lowest voice.", listOf(55,60,64), "G  →  C  →  E", "VOICING")
        )
        "T10_DIATONIC" -> listOf(
            ex("I · C", "I · C", "Tonic: C–E–G.", "Tonic: C–E–G.", listOf(60,64,67), "I · major", "CHORD"),
            ex("ii · Dm", "ii · Dm", "Hợp âm thứ ở bậc 2: D–F–A.", "Minor triad on degree 2: D–F–A.", listOf(62,65,69), "ii · minor", "CHORD"),
            ex("iii · Em", "iii · Em", "Hợp âm thứ bậc 3: E–G–B.", "Minor triad on degree 3: E–G–B.", listOf(64,67,71), "iii · minor", "CHORD"),
            ex("IV · F", "IV · F", "Pre-dominant: F–A–C.", "Pre-dominant: F–A–C.", listOf(65,69,72), "IV · major", "CHORD"),
            ex("V · G", "V · G", "Dominant: G–B–D.", "Dominant: G–B–D.", listOf(67,71,74), "V · major", "CHORD"),
            ex("vi · Am", "vi · Am", "Hợp âm thứ bậc 6: A–C–E.", "Minor triad on degree 6: A–C–E.", listOf(69,72,76), "vi · minor", "CHORD"),
            ex("vii° · Bdim", "vii° · Bdim", "B–D–F tạo hợp âm giảm bậc 7.", "B–D–F forms the diminished leading-tone triad.", listOf(71,74,77), "vii° · diminished", "CHORD")
        )
        "T11_FUNCTIONS" -> listOf(
            ex("Tonic · I", "Tonic · I", "C trưởng tạo cảm giác ổn định và điểm về.", "C major is a stable tonal home.", listOf(60,64,67), "T · C", "PROGRESSION", listOf("C", "F", "G7", "C"), listOf(listOf(60,64,67),listOf(65,69,72),listOf(55,59,62,65),listOf(60,64,67))),
            ex("Pre-dominant · IV", "Pre-dominant · IV", "F trưởng chuẩn bị chuyển sang dominant G7.", "F major prepares the dominant G7.", listOf(65,69,72), "PD · F", "PROGRESSION", listOf("C", "F", "G7", "C"), listOf(listOf(60,64,67),listOf(65,69,72),listOf(55,59,62,65),listOf(60,64,67))),
            ex("Dominant · V7", "Dominant · V7", "G7 chứa B và F, muốn giải quyết về C.", "G7 has B and F, tending toward C.", listOf(55,59,62,65), "D · G7", "PROGRESSION", listOf("C", "F", "G7", "C"), listOf(listOf(60,64,67),listOf(65,69,72),listOf(55,59,62,65),listOf(60,64,67)))
        )
        "T12_II_V_I" -> listOf(
            ex("ii–V–I trong C", "ii–V–I in C", "Dm7 → G7 → Cmaj7; chú ý F chuyển xuống E và C chuyển xuống B.", "Dm7 → G7 → Cmaj7; notice F resolving to E and C to B.", listOf(50,53,57,60), "ii7 → V7 → Imaj7", "PROGRESSION", listOf("Dm7", "G7", "Cmaj7"), listOf(listOf(50,53,57,60),listOf(55,59,62,65),listOf(60,64,67,71)))
        )
        "T13_SEVENTHS" -> listOf(
            ex("Cmaj7", "Cmaj7", "Bậc 7 trưởng là B: C–E–G–B.", "Major seventh B: C–E–G–B.", listOf(60,64,67,71), "1 · 3 · 5 · 7", "CHORD"),
            ex("C7", "C7", "Bậc ♭7 là B♭: C–E–G–B♭.", "Flat seventh B♭: C–E–G–B♭.", listOf(60,64,67,70), "1 · 3 · 5 · ♭7", "CHORD"),
            ex("Cm7", "Cm7", "C–E♭–G–B♭; bậc 3 và 7 đều giáng.", "C–E♭–G–B♭; both the third and seventh are flat.", listOf(60,63,67,70), "1 · ♭3 · 5 · ♭7", "CHORD")
        )
        "T14_MODES_BLUES" -> listOf(
            ex("C Dorian", "C Dorian", "Nốt A (6) tạo màu Dorian khi so với Aeolian (A♭).", "A natural sixth distinguishes Dorian from Aeolian (A♭).", listOf(60,62,63,65,67,69,70), "1 · 2 · ♭3 · 4 · 5 · 6 · ♭7", "FRETBOARD"),
            ex("C Lydian", "C Lydian", "F♯ là bậc ♯4 đặc trưng Lydian.", "F♯ is Lydian's characteristic raised fourth.", listOf(60,62,64,66,67,69,71), "1 · 2 · 3 · ♯4 · 5 · 6 · 7", "FRETBOARD"),
            ex("C Mixolydian", "C Mixolydian", "B♭ (♭7) tạo màu dominant.", "B♭ (♭7) produces the dominant colour.", listOf(60,62,64,65,67,69,70), "1 · 2 · 3 · 4 · 5 · 6 · ♭7", "FRETBOARD"),
            ex("C Blues thứ", "C minor blues", "G♭ là blue note ♭5 thêm vào ngũ cung thứ.", "G♭ is the ♭5 blue note added to minor pentatonic.", listOf(60,63,65,66,67,70), "1 · ♭3 · 4 · ♭5 · 5 · ♭7", "FRETBOARD")
        )
        "T15_PASSING" -> listOf(
            ex("Diminished chuyển tiếp", "Diminished approach", "Cmaj7 → C♯dim7 → Dm7 tạo bass đi chromatic: C–C♯–D.", "Cmaj7 → C♯dim7 → Dm7: chromatic bass C–C♯–D.", listOf(48,52,55,59), "C → C♯ → D", "PROGRESSION", listOf("Cmaj7", "C♯dim7", "Dm7"), listOf(listOf(48,52,55,59),listOf(49,52,55,58),listOf(50,53,57,60))),
            ex("Dominant phụ", "Secondary dominant", "Cmaj7 → A7 → Dm7; A7 là V7/ii, dẫn tới Dm7.", "Cmaj7 → A7 → Dm7; A7 is V7/ii leading to Dm7.", listOf(48,52,55,59), "Imaj7 → V7/ii → ii7", "PROGRESSION", listOf("Cmaj7", "A7", "Dm7"), listOf(listOf(48,52,55,59),listOf(45,49,52,55),listOf(50,53,57,60)))
        )
        "T16_CHORD_MELODY" -> listOf(
            ex("Melody E trên Cmaj7", "Melody E on Cmaj7", "E là nốt cao nhất, nằm trên bè hợp âm C–G–B.", "E is the top note above chord voices C–G–B.", listOf(48,55,59,64), "Cmaj7 → E (melody)", "VOICING"),
            ex("Melody F trên Dm7", "Melody F on Dm7", "F là nốt giai điệu trên cùng của Dm7.", "F is the upper melody note of Dm7.", listOf(50,57,60,65), "Dm7 → F (melody)", "VOICING")
        )
        else -> emptyList()
    }
}

private fun theoryNoteName(midi: Int, flatFifth: Boolean = false): String = when (((midi % 12) + 12) % 12) {
    0 -> "C"; 1 -> "C♯"; 2 -> "D"; 3 -> "E♭"; 4 -> "E"; 5 -> "F"
    6 -> if (flatFifth) "G♭" else "F♯"; 7 -> "G"; 8 -> "G♯"; 9 -> "A"; 10 -> "B♭"; else -> "B"
}

private data class TheoryExtendedGuide(
    val vi: List<String>, val en: List<String>, val noteVi: String, val noteEn: String
)

private fun theoryExtendedGuide(id: String): TheoryExtendedGuide {
    fun guide(a: List<String>, b: List<String>, caution: String, cautionEn: String) =
        TheoryExtendedGuide(a, b, caution, cautionEn)
    return when (id) {
        "T01_TUNING" -> guide(
            listOf("Gảy từng dây buông từ dây 6 lên dây 1 và nói tên nốt.", "Chọn dây bất kỳ trên sơ đồ để nghe riêng cao độ.", "Đặt ngón vào ngăn 12 để so sánh với dây buông: cùng tên nốt, cao hơn một quãng tám."),
            listOf("Pluck open strings from 6 to 1 and name each pitch.", "Select an individual string in the diagram to hear it.", "Compare fret 12 to the open string: the same note an octave higher."),
            "Đừng nhầm thứ tự dây 1–6 với chiều trầm đến cao: dây 6 là dây E trầm.",
            "Do not confuse string numbers with low-to-high pitch order: string 6 is low E.")
        "T02_DURATION" -> guide(
            listOf("Đếm bốn phách đều: 1–2–3–4.", "Giữ nhịp và thay bốn nốt đen bằng tám móc đơn.", "Thử nghỉ đúng một phách mà không mất nhịp đếm."),
            listOf("Count four steady quarter beats.", "Keep the pulse and replace four quarters with eight eighths.", "Rest for exactly one beat without losing the count."),
            "Trường độ là thời gian tương đối: 1/8 nhanh gấp đôi 1/4 ở cùng BPM, không phải lúc nào cũng mang tốc độ cố định.",
            "Durations are relative: an eighth is half a quarter's duration at the same BPM, not a fixed speed.")
        "T03_METER" -> guide(
            listOf("Vỗ tay rõ phách 1 của ô 3/4.", "Chuyển sang 4/4 và nhận biết phách 3 nhấn nhẹ hơn phách 1.", "Với 6/8, đếm hai nhóm 1-2-3 | 4-5-6, nhấn 1 và 4."),
            listOf("Clap the downbeat in 3/4.", "Try 4/4 with a lighter secondary accent on beat 3.", "For 6/8, count 1-2-3 | 4-5-6, accenting 1 and 4."),
            "6/8 không đồng nghĩa với sáu phách nốt đen; đây thường là nhịp kép với hai phách chấm dôi.",
            "6/8 is not six quarter beats: it is commonly compound duple with two dotted-quarter beats.")
        "T04_SEMITONES" -> guide(
            listOf("Chơi C, C♯ rồi D để nghe các bước nửa cung.", "Trên một dây guitar, tăng hai ngăn để được một cung.", "Tìm hai cặp nửa cung tự nhiên: E–F và B–C."),
            listOf("Play C, C♯ then D to hear semitone steps.", "Move two frets along one string for a whole step.", "Find the natural semitone pairs E–F and B–C."),
            "Một nửa cung không nhất thiết cần dấu ♯ hoặc ♭: E–F là ví dụ.",
            "A semitone does not always need a sharp or flat: E–F is one example.")
        "T05_INTERVALS" -> guide(
            listOf("Nghe C→E♭ và C→E, nhận biết màu thứ và trưởng.", "Đếm bán cung trên bàn phím hoặc trên cùng một dây.", "Dùng C–G để kiểm chứng quãng 5 đúng (7 bán cung)."),
            listOf("Hear C→E♭ versus C→E, comparing minor and major colours.", "Count semitones on the keyboard or on one string.", "Verify C–G as a perfect fifth (7 semitones)."),
            "Tên quãng phụ thuộc cả chữ cái nốt và khoảng cách; số bán cung không đủ để viết đúng mọi quãng nâng cao.",
            "Intervals depend on spelling and semitone size; semitones alone do not name every advanced interval.")
        "T06_PENTATONIC" -> guide(
            listOf("So sánh hai tập nốt C ngũ cung trưởng và A ngũ cung thứ.", "Tìm nốt chủ C hoặc A trên cần đàn và kết mỗi câu nhạc về nốt ấy.", "Chạy năm nốt lên/xuống, rồi tạo câu ngắn thay vì chỉ chạy tuần tự."),
            listOf("Compare C major and A minor pentatonic pitch sets.", "Find tonic C or A on the fretboard and resolve phrases to it.", "Play five notes up/down, then make a short phrase."),
            "Cùng năm cao độ không có nghĩa cùng nốt chủ; cảm giác chủ âm phụ thuộc hòa âm và cách kết câu.",
            "Sharing five pitches does not mean sharing a tonic; harmony and phrase endings matter.")
        "T07_TRIAD" -> guide(
            listOf("Nghe C trưởng rồi C thứ và tìm nốt thay đổi duy nhất.", "Chơi riêng C–E–G trước khi gảy đồng thời.", "Chuyển E xuống E♭ để biến hợp âm trưởng thành thứ."),
            listOf("Hear C major and C minor; locate the single changed note.", "Arpeggiate C–E–G before sounding the chord together.", "Lower E to E♭ to switch major to minor."),
            "Triad chỉ có ba cao độ khác nhau, dù thế bấm guitar có thể lặp nốt ở nhiều dây.",
            "A triad has three pitch classes even if guitar voicings double notes.")
        "T08_TRIAD_TYPES" -> guide(
            listOf("So sánh 4 cấu trúc 1–3–5, 1–♭3–5, 1–3–♯5 và 1–♭3–♭5.", "Chạm từng thẻ để nhìn nốt thay đổi trên bàn phím.", "Nghe âm thanh trưởng, thứ, tăng và giảm rồi tự nhận biết."),
            listOf("Compare the four third/fifth formulas.", "Switch examples to see changes on the keyboard.", "Listen and distinguish major, minor, augmented, diminished."),
            "Hợp âm tăng không đồng nghĩa với hợp âm trưởng: bậc 5 của tăng được nâng nửa cung.",
            "An augmented chord is not major: its fifth is raised by a semitone.")
        "T09_INVERSIONS" -> guide(
            listOf("Chơi C–E–G rồi đưa C lên quãng tám để thành E–G–C.", "Đảo thêm lần nữa thành G–C–E.", "Nghe cả ba thế và tìm nốt bass ở vị trí thấp nhất."),
            listOf("Start C–E–G, move C up an octave for E–G–C.", "Invert again into G–C–E.", "Hear each voicing and identify its bass note."),
            "Thế đảo được gọi theo nốt bass, không theo nốt nằm cao nhất.",
            "Inversions are named by the bass note, not the highest note.")
        "T10_DIATONIC" -> guide(
            listOf("Từ C trưởng, xếp từng bậc cách một nốt để tạo 7 triad.", "Chọn từng hợp âm I–vii° và quan sát trưởng, thứ hoặc giảm.", "Tập vòng I–vi–ii–V ở tông C rồi chuyển tông."),
            listOf("Stack alternating notes from C major to make seven triads.", "Select chords I through vii° to inspect qualities.", "Practise I–vi–ii–V in C then transpose."),
            "Chữ số La Mã hoa biểu thị hợp âm trưởng; chữ thường chỉ hợp âm thứ, ° là giảm.",
            "Uppercase Roman numerals are major; lowercase are minor; ° means diminished.")
        "T11_FUNCTIONS" -> guide(
            listOf("Gảy C để cảm nhận điểm nghỉ Tonic.", "Gảy F rồi G7 để nghe chuyển động chuẩn bị và căng thẳng.", "Kết lại bằng C, nhận biết cảm giác giải quyết."),
            listOf("Play C as the stable tonic.", "Play F then G7 to hear preparation and tension.", "Return to C and hear the resolution."),
            "Các nhãn chức năng là mô hình theo ngữ cảnh hòa âm, không phải tính chất bất biến của mọi hợp âm.",
            "Harmonic functions depend on context; a chord does not have one fixed function everywhere.")
        "T12_II_V_I" -> guide(
            listOf("Nghe Dm7 → G7 → Cmaj7 liên tục để nhận ra chuyển động ii–V–I.", "Theo dõi F của G7 đi xuống E của Cmaj7 và B là nốt chung.", "Luyện chuyển giữa các thế bấm gần nhau trên guitar."),
            listOf("Hear Dm7 → G7 → Cmaj7 as a complete cadence.", "Trace F in G7 resolving to E in Cmaj7 and common tone B.", "Practise nearby guitar voicings with minimal motion."),
            "Guide tones là bậc 3 và 7 của hợp âm, không phải bất kỳ hai nốt cao nhất nào.",
            "Guide tones are chord thirds and sevenths, not simply the two highest notes.")
        "T13_SEVENTHS" -> guide(
            listOf("So sánh Cmaj7 và C7: chỉ thay B thành B♭.", "Nghe Cm7 để nhận ra bậc 3 thứ E♭.", "Rải từng nốt 1–3–5–7 hoặc 1–3–5–♭7 theo BPM."),
            listOf("Compare Cmaj7 and C7: B changes to B♭.", "Hear Cm7 and notice its minor third E♭.", "Arpeggiate the four chord tones in time."),
            "C7 là dominant seventh, không phải hợp âm Cmaj7; bậc 7 khác nhau một bán cung.",
            "C7 is a dominant seventh, not Cmaj7; their sevenths differ by one semitone.")
        "T14_MODES_BLUES" -> guide(
            listOf("Chơi C Dorian và tìm bậc 6 là A.", "Chuyển sang C Lydian để nghe ♯4 là F♯.", "Luyện C Blues thứ và nhấn blue note G♭ rồi giải về G."),
            listOf("Play C Dorian and locate degree 6 A.", "Switch to C Lydian to hear raised fourth F♯.", "Practise C minor blues: lean on G♭ then resolve to G."),
            "Blues không phải mode thứ tám của âm giai trưởng; nó là họ âm giai riêng.",
            "Blues is not an eighth major-scale mode; it is a separate scale family.")
        "T15_PASSING" -> guide(
            listOf("Nghe vòng Cmaj7 → Dm7 trước, rồi thêm C♯dim7 ở giữa.", "Theo dõi bass C–C♯–D di chuyển nửa cung.", "So sánh với A7 → Dm7 để nghe dominant phụ."),
            listOf("Hear Cmaj7 → Dm7 before inserting C♯dim7.", "Trace chromatic bass C–C♯–D.", "Compare A7 → Dm7 as a secondary dominant."),
            "Hợp âm chuyển là lựa chọn theo ngữ cảnh; không phải cứ chèn nhiều là hòa âm hay hơn.",
            "Passing chords are contextual; adding more is not automatically better harmony.")
        "T16_CHORD_MELODY" -> guide(
            listOf("Chọn nốt E làm melody trên Cmaj7.", "Chọn voicing có E là nốt cao nhất mà vẫn bấm được.", "Đổi sang Dm7 với melody F; luyện đường dẫn giọng E→F."),
            listOf("Select E as the melody above Cmaj7.", "Choose a playable voicing with E on top.", "Move to Dm7 with melody F and practise E→F voice leading."),
            "Không cần rải hợp âm dưới mọi nốt giai điệu: nốt đơn và double-stop cũng là một phần chord melody.",
            "Not every melody note needs a full chord; single notes and double-stops also belong in chord melody.")
        else -> guide(emptyList(), emptyList(), "", "")
    }
}

// Original Compose diagrams for the written music-theory lessons; no copied UMI assets.
@Composable
private fun TheoryTextbookStaff(notes: List<Int>, bass: Boolean, accent: Color, textColor: Color) {
    Box(Modifier.fillMaxWidth().height(200.dp)
        .clip(RoundedCornerShape(12.dp)).background(Color(0xFF172329))) {
        Text(if (bass) "𝄢" else "𝄞", color = Color(0xFFFFD188), fontSize = 48.sp,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp))
        Canvas(Modifier.fillMaxSize()) {
            val x0 = size.width * 0.22f
            val x1 = size.width * 0.97f
            val gap = 23.dp.toPx()
            val yBottom = size.height * 0.69f
            val lineColor = Color.White.copy(alpha = 0.62f)
            for (lineIndex in 0..4) {
                val y = yBottom - lineIndex * gap
                drawLine(lineColor, Offset(x0, y), Offset(x1, y), strokeWidth = 1.5.dp.toPx())
            }
            val baseIndex = if (bass) 2 * 7 + 4 else 4 * 7 + 2 // G2 or E4, lower line
            val naturalStep = intArrayOf(0,0,1,1,2,3,3,4,4,5,5,6)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.White.toArgb()
                textSize = 11.dp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
            }
            notes.take(10).forEachIndexed { i, midi ->
                val octave = midi / 12 - 1
                val pitch = ((midi % 12) + 12) % 12
                val step = octave * 7 + naturalStep[pitch] - baseIndex
                val y = yBottom - (step * gap / 2f)
                val x = x0 + (i + 1) * (x1 - x0) / (notes.size.coerceAtMost(10) + 1)
                if (step < 0 || step > 8) {
                    val begin = minOf(step, 0)
                    val end = maxOf(step, 8)
                    for (ledger in begin..end) {
                        if (ledger % 2 == 0 && (ledger < 0 || ledger > 8)) {
                            val ly = yBottom - ledger * gap / 2f
                            drawLine(lineColor, Offset(x - 12.dp.toPx(), ly),
                                Offset(x + 12.dp.toPx(), ly), strokeWidth = 1.5.dp.toPx())
                        }
                    }
                }
                drawOval(accent, topLeft = Offset(x - 7.dp.toPx(), y - 5.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(14.dp.toPx(),10.dp.toPx()))
                drawLine(accent, Offset(x + 7.dp.toPx(), y),
                    Offset(x + 7.dp.toPx(), y - 25.dp.toPx()), 1.7.dp.toPx())
                drawContext.canvas.nativeCanvas.drawText(
                    "${theoryNoteName(midi)}${octave}", x,
                    minOf(size.height - 7.dp.toPx(), y + 26.dp.toPx()), paint)
            }
        }
    }
}

@Composable
private fun TheoryTextbookNoteValues(noteCount: Int, accent: Color, textColor: Color, currentLang: String) {
    val count = noteCount.coerceIn(1, 16)
    Box(Modifier.fillMaxWidth().height(125.dp)
        .clip(RoundedCornerShape(12.dp)).background(Color(0xFF172329))) {
        Canvas(Modifier.fillMaxSize()) {
            val left = 15.dp.toPx()
            val right = size.width - left
            val usable = right - left
            val barY = size.height * 0.71f
            drawLine(Color.White.copy(alpha = 0.25f), Offset(left, barY), Offset(right, barY), 2.dp.toPx())
            for (beat in 0..4) {
                val x = left + usable * beat / 4
                drawLine(Color.White.copy(alpha=0.4f),Offset(x,barY-10.dp.toPx()),
                    Offset(x,barY+10.dp.toPx()),1.dp.toPx())
            }
            val width = usable / count
            for (i in 0 until count) {
                val x = left + (i + 0.5f) * width
                val noteY = size.height * 0.36f
                drawOval(if (count <= 2) Color(0xFF172329) else accent,
                    topLeft=Offset(x-5.7.dp.toPx(),noteY-4.7.dp.toPx()),
                    size=androidx.compose.ui.geometry.Size(11.4.dp.toPx(),9.4.dp.toPx()),
                    style=if (count<=2) Stroke(width=2.dp.toPx()) else androidx.compose.ui.graphics.drawscope.Fill)
                if (count > 1) {
                    drawLine(accent,Offset(x+6.dp.toPx(),noteY),
                        Offset(x+6.dp.toPx(),noteY-21.dp.toPx()),1.8.dp.toPx())
                }
                drawRect(accent.copy(alpha=0.35f),
                    topLeft=Offset(left+i*width+2.dp.toPx(),barY+14.dp.toPx()),
                    size=androidx.compose.ui.geometry.Size((width-4.dp.toPx()).coerceAtLeast(1f),8.dp.toPx()))
            }
            if(count >= 8) {
                val groupSize=if(count==16) 4 else 2
                for (group in 0 until count/groupSize) {
                    val first=group*groupSize
                    val last=first+groupSize-1
                    val startX=left+(first+0.5f)*width+6.dp.toPx()
                    val endX=left+(last+0.5f)*width+6.dp.toPx()
                    val y=size.height*.36f-21.dp.toPx()
                    drawLine(accent,Offset(startX,y),Offset(endX,y),3.dp.toPx())
                    if(count==16) drawLine(accent,Offset(startX,y+6.dp.toPx()),
                        Offset(endX,y+6.dp.toPx()),3.dp.toPx())
                }
            }
        }
        Text("4/4 · $count ${if(currentLang.lowercase(Locale.ROOT).startsWith("vi")) "nốt" else if(count==1) "note" else "notes"}",
            color = Color.White.copy(alpha=0.78f), fontSize=11.sp,
            modifier=Modifier.align(Alignment.TopEnd).padding(10.dp))
    }
}

@Composable
private fun TheoryTextbookTab(notes: List<Int>, stringNumber: Int, accent: Color, textColor: Color) {
    val opens = listOf(64,59,55,50,45,40)
    val selected = stringNumber.coerceIn(1,6)
    Box(Modifier.fillMaxWidth().height(197.dp)
        .clip(RoundedCornerShape(12.dp)).background(Color(0xFF172329))) {
        Canvas(Modifier.fillMaxSize()) {
            val left = size.width * 0.14f
            val right = size.width * 0.94f
            val top = size.height * 0.17f
            val lineGap = (size.height * 0.68f) / 5f
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                textAlign=android.graphics.Paint.Align.CENTER
                typeface=android.graphics.Typeface.create("monospace",android.graphics.Typeface.BOLD)
                textSize=14.dp.toPx()
            }
            val label = listOf("e","B","G","D","A","E")
            for (s in 0 until 6) {
                val y = top + s * lineGap
                drawLine(if(s+1==selected) accent else Color.White.copy(alpha=.32f),
                    Offset(left,y),Offset(right,y),1.6.dp.toPx())
                paint.color = Color.White.toArgb()
                drawContext.canvas.nativeCanvas.drawText(label[s],left-17.dp.toPx(),y+5.dp.toPx(),paint)
            }
            val base = opens[selected-1]
            val y = top+(selected-1)*lineGap
            notes.take(7).forEachIndexed { idx, midi ->
                val fret = midi-base
                if (fret in 0..24) {
                    val x = left+(idx+1)*(right-left)/(notes.size.coerceAtMost(7)+1)
                    drawCircle(Color(0xFF172329),12.dp.toPx(),Offset(x,y))
                    paint.color=accent.toArgb()
                    drawContext.canvas.nativeCanvas.drawText(fret.toString(),x,y+5.dp.toPx(),paint)
                }
            }
        }
    }
}

@Composable
private fun TheoryTextbookChordDiagram(shapeId: Int, accent: Color, textColor: Color) {
    // Frets listed from the thickest (sixth) to the thinnest (first) string.
    val frets = when(shapeId) {
        2 -> listOf(-1,0,2,2,1,0) // Am
        3 -> listOf(3,2,0,0,0,1) // G7
        else -> listOf(-1,3,2,0,1,0) // C
    }
    val chordName = when(shapeId) { 2 -> "Am"; 3 -> "G7"; else -> "C" }
    Box(Modifier.fillMaxWidth().height(224.dp)
        .clip(RoundedCornerShape(12.dp)).background(Color(0xFF172329))) {
        Text(chordName, color=accent, fontSize=19.sp,fontWeight=FontWeight.Bold,
            modifier=Modifier.align(Alignment.TopStart).padding(start=12.dp,top=8.dp))
        Canvas(Modifier.fillMaxSize()) {
            val left = size.width*0.17f
            val right = size.width*0.86f
            val top = size.height*0.22f
            val fretH = (size.height*0.65f)/4f
            val stringGap = (right-left)/5f
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                textAlign=android.graphics.Paint.Align.CENTER
                typeface=android.graphics.Typeface.DEFAULT_BOLD
                textSize=14.dp.toPx()
                color=textColor.toArgb()
            }
            for(s in 0..5) {
                val x=left+s*stringGap
                drawLine(Color.White.copy(alpha=.65f),Offset(x,top),Offset(x,top+4*fretH),1.6.dp.toPx())
                val f=frets[s]
                if(f==0 || f<0) {
                    paint.color=if(f==0) accent.toArgb() else Color.White.copy(alpha=.60f).toArgb()
                    drawContext.canvas.nativeCanvas.drawText(if(f==0) "O" else "X",x,top-10.dp.toPx(),paint)
                } else {
                    val y=top+(f-0.5f)*fretH
                    drawCircle(accent,10.dp.toPx(),Offset(x,y))
                }
            }
            for (fret in 0..4) {
                val y=top+fret*fretH
                drawLine(Color.White.copy(alpha=if(fret==0) .95f else .42f),
                    Offset(left,y),Offset(right,y),(if (fret == 0) 4f else 1.5f).dp.toPx())
            }
            paint.color=Color.White.copy(alpha=.65f).toArgb()
            drawContext.canvas.nativeCanvas.drawText("6",left,top+4*fretH+18.dp.toPx(),paint)
            drawContext.canvas.nativeCanvas.drawText("1",right,top+4*fretH+18.dp.toPx(),paint)
        }
    }
}

@Composable
private fun TheoryTextbookRestGrid(accent: Color, currentLang: String) {
    val isVi = currentLang.lowercase(Locale.ROOT).startsWith("vi")
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
        .background(Color(0xFF172329)).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (0 until 4).forEach { beat ->
            val plays = beat % 2 == 0
            Column(Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                .background(if (plays) accent.copy(alpha=.19f) else Color.White.copy(alpha=.05f))
                .padding(vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                Text("${beat+1}", color=Color.White.copy(alpha=.77f),fontSize=12.sp)
                Spacer(Modifier.height(6.dp))
                Text(if(plays) "♪" else "—", color=if(plays) accent else Color(0xFFFFC16A),
                    fontSize=28.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(if(plays) (if(isVi) "Chơi" else "Play") else (if(isVi) "Nghỉ" else "Rest"),
                    color=Color.White,fontSize=11.sp,fontWeight=FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TheoryTextbookDotOrTie(isTie: Boolean, accent: Color, currentLang: String) {
    Box(Modifier.fillMaxWidth().height(165.dp)
        .clip(RoundedCornerShape(12.dp)).background(Color(0xFF172329))) {
        Canvas(Modifier.fillMaxSize()) {
            val baseline = size.height*0.64f
            val space = 16.dp.toPx()
            for(i in 0..4) {
                val y=baseline-i*space
                drawLine(Color.White.copy(alpha=.45f),Offset(15.dp.toPx(),y),
                    Offset(size.width-15.dp.toPx(),y),1.3.dp.toPx())
            }
            val y=baseline-2*space
            val positions=if (isTie) listOf(size.width*.35f,size.width*.65f) else listOf(size.width*.48f)
            positions.forEach { x ->
                drawOval(accent, topLeft=Offset(x-9.dp.toPx(),y-5.dp.toPx()),
                    size=androidx.compose.ui.geometry.Size(18.dp.toPx(),10.dp.toPx()))
                drawLine(accent,Offset(x+9.dp.toPx(),y),
                    Offset(x+9.dp.toPx(),y-32.dp.toPx()),1.8.dp.toPx())
            }
            if (isTie) {
                drawArc(accent,startAngle=0f,sweepAngle=180f,useCenter=false,
                    topLeft=Offset(size.width*.35f,y+6.dp.toPx()),
                    size=androidx.compose.ui.geometry.Size(size.width*.30f,17.dp.toPx()),
                    style=Stroke(width=2.6.dp.toPx()))
            } else {
                drawCircle(accent,radius=2.6.dp.toPx(),
                    center=Offset(size.width*.48f+17.dp.toPx(),y-1.dp.toPx()))
            }
        }
        Text(if(isTie) {
            if(currentLang.startsWith("vi")) "Dấu nối: chơi 1 lần, ngân 2 phách"
            else "Tie: one attack, sustain 2 beats"
        } else {
            if(currentLang.startsWith("vi")) "Chấm dôi: 1 + ½ = 1½ phách"
            else "Dot: 1 + ½ = 1½ beats"
        }, color=Color.White,fontSize=12.sp,
            modifier=Modifier.align(Alignment.BottomCenter).padding(bottom=9.dp))
    }
}


@Composable
private fun TheoryVisualExplorer(
    topic: TheoryTopic,
    currentLang: String,
    surfaceColor: Color,
    textColor: Color,
    accent: Color,
    onPlay: (List<List<Int>>, Long) -> Unit
) {
    val examples = remember(topic.id) { theoryVisualExamples(topic.id) }
    if (examples.isEmpty()) return
    var selected by rememberSaveable(topic.id) { mutableIntStateOf(0) }
    val example = examples[selected.coerceIn(examples.indices)]
    val flatFive = "♭5" in example.degrees
    val warm = Color(0xFFFFBF55)
    val line = textColor.copy(alpha = 0.15f)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text(fingerUiText(currentLang, "HÌNH MINH HỌA NHẠC LÝ", "MUSIC THEORY ILLUSTRATION"),
                color = accent, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        }
        Surface(
            shape = RoundedCornerShape(15.dp),
            color = surfaceColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.36f))
        ) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    fingerUiText(currentLang, "Chọn ví dụ để thay đổi hình và nghe", "Choose an example to update the illustration and sound"),
                    color = textColor.copy(alpha = 0.68f), fontSize = 12.sp, lineHeight = 17.sp
                )
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    examples.forEachIndexed { index, item ->
                        val active = index == selected
                        Surface(
                            modifier = Modifier.clip(RoundedCornerShape(9.dp)).clickable { selected = index },
                            shape = RoundedCornerShape(9.dp),
                            color = if (active) accent.copy(alpha = 0.16f) else textColor.copy(alpha = 0.045f),
                            border = androidx.compose.foundation.BorderStroke(if (active) 1.4.dp else 1.dp,
                                if (active) accent else line)
                        ) {
                            Text(item.title(currentLang), color = if (active) accent else textColor,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp))
                        }
                    }
                }
                Text(example.degrees, color = warm, fontWeight = FontWeight.Bold,
                    fontSize = 13.sp, lineHeight = 18.sp)
                when (example.layout) {
                    "RESTS" -> TheoryTextbookRestGrid(accent=accent,currentLang=currentLang)
                    "DOT", "TIE" -> TheoryTextbookDotOrTie(
                        isTie=example.layout=="TIE",accent=accent,currentLang=currentLang)
                    "STAFF", "STAFF_BASS" -> TheoryTextbookStaff(
                        notes = example.notes, bass = example.layout == "STAFF_BASS",
                        accent = accent, textColor = textColor
                    )
                    "TAB" -> TheoryTextbookTab(
                        notes = example.notes, stringNumber = example.focusString,
                        accent = accent, textColor = textColor
                    )
                    "CHORD_DIAGRAM" -> TheoryTextbookChordDiagram(
                        shapeId = example.focusString, accent = accent, textColor = textColor
                    )
                    "DURATION" -> TheoryTextbookNoteValues(
                        noteCount = example.pulses, accent = accent, textColor = textColor,
                        currentLang = currentLang
                    )
                    "TEMPO" -> TheoryVisualBeatGrid(
                        count = 4, groupSize = 4, isMeter = true,
                        currentLang = currentLang, textColor = textColor, accent = accent
                    )
                    "TUNING", "FRETBOARD" -> TheoryVisualFretboard(
                        pitches = example.notes, layout = example.layout, focusString = example.focusString,
                        surfaceColor = surfaceColor, accent = accent, textColor = textColor,
                        flatFifth = flatFive,
                        onNote = { onPlay(listOf(listOf(it)), 480L) }
                    )
                    "RHYTHM", "METER" -> TheoryVisualBeatGrid(
                        count = example.pulses, groupSize = example.groupSize,
                        isMeter = example.layout == "METER", currentLang = currentLang,
                        textColor = textColor, accent = accent
                    )
                    "VOICING" -> TheoryVisualVoicing(
                        notes = example.notes, textColor = textColor, accent = accent, onNote = { onPlay(listOf(listOf(it)), 480L) }
                    )
                    "PROGRESSION" -> TheoryVisualProgression(
                        example = example, textColor = textColor, accent = accent,
                        onChord = { onPlay(listOf(it), 480L) }
                    )
                    else -> TheoryVisualPiano(
                        notes = example.notes, textColor = textColor, accent = accent,
                        flatFifth = flatFive
                    )
                }
                Text(example.explanation(currentLang), color = textColor.copy(alpha = 0.89f),
                    fontSize = 13.5.sp, lineHeight = 20.sp)
                if (example.layout == "PIANO" || example.layout == "CHORD" || example.layout == "FRETBOARD") {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        example.notes.forEachIndexed { i, midi ->
                            Surface(
                                modifier = Modifier.clip(RoundedCornerShape(9.dp)).clickable {
                                    onPlay(listOf(listOf(midi)), 480L)
                                },
                                shape = RoundedCornerShape(9.dp),
                                color = if (i == 0) accent.copy(alpha = 0.20f) else textColor.copy(alpha = 0.06f)
                            ) {
                                Text(
                                    theoryNoteName(midi, flatFive) +
                                        (if (example.notes.maxOrNull() != null &&
                                            example.notes.maxOrNull()!! - example.notes.minOrNull()!! >= 12)
                                            "${midi / 12 - 1}" else ""),
                                    color = if (i == 0) accent else textColor,
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp))
                            }
                        }
                    }
                }
                Surface(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .clickable {
                        val interval = when (example.layout) {
                            "RHYTHM" -> (600L / example.groupSize.coerceAtLeast(1))
                            "METER" -> if (example.pulses == 6) 300L else 600L
                            "TEMPO" -> 60000L / example.pulses.coerceAtLeast(1)
                            else -> 480L
                        }
                        onPlay(example.playback(), interval)
                    },
                    shape = RoundedCornerShape(10.dp), color = accent.copy(alpha = 0.15f)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = accent,
                            modifier = Modifier.size(21.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(fingerUiText(currentLang, "Nghe ví dụ đang chọn", "Hear selected example"),
                            color = accent, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Text(fingerUiText(currentLang,
                    "Hình minh họa được xây dựng riêng cho Tuner Tools. Chọn ví dụ để xem cách ghi và nghe âm thanh tương ứng.",
                    "Original Tuner Tools diagrams. Choose an example to inspect notation and hear the corresponding sound."),
                    color = textColor.copy(alpha = 0.45f), fontSize = 10.sp, lineHeight = 14.sp)
            }
        }
    }
}

@Composable
private fun TheoryVisualFretboard(
    pitches: List<Int>, layout: String, focusString: Int,
    surfaceColor: Color, accent: Color, textColor: Color,
    flatFifth: Boolean, onNote: (Int) -> Unit
) {
    val current = remember(pitches, layout, focusString) {
        val open = listOf(64, 59, 55, 50, 45, 40)
        buildList {
            for (stringIndex in 0..5) {
                for (fret in 0..5) {
                    val midi = open[stringIndex] + fret
                    if ((layout == "TUNING" && fret == 0) ||
                        (layout != "TUNING" && pitches.any { it % 12 == midi % 12 })) {
                        add(Triple(stringIndex, fret, midi))
                    }
                }
            }
        }
    }
    var selectedNote by remember(layout, focusString, pitches) { mutableStateOf<Int?>(null) }
    val rootPitch = pitches.firstOrNull()?.let { ((it % 12) + 12) % 12 }
    val info = if (selectedNote == null) {
        if (layout == "TUNING") "E  A  D  G  B  E" else "0  ·  1  ·  2  ·  3  ·  4  ·  5"
    } else "${theoryNoteName(selectedNote!!, flatFifth)} · MIDI $selectedNote"
    Box(Modifier.fillMaxWidth().height(205.dp)
        .clip(RoundedCornerShape(11.dp)).background(Color(0xFF192226))) {
        Canvas(
            modifier = Modifier.fillMaxSize().pointerInput(current, focusString) {
                detectTapGestures { tap ->
                    val left = size.width * 0.12f
                    val width = size.width * 0.85f
                    val step = width / 6f
                    val xCandidates = (0..5).map { left + (it + 0.5f) * step }
                    val stringGap = size.height * 0.66f / 5f
                    val yStart = size.height * 0.17f
                    val index = ((tap.y - yStart) / stringGap).roundToInt().coerceIn(0,5)
                    val fret = xCandidates.indices.minByOrNull { kotlin.math.abs(xCandidates[it] - tap.x) } ?: 0
                    val match = current.firstOrNull { it.first == index && it.second == fret }
                    if (match != null) { selectedNote = match.third; onNote(match.third) }
                }
            }
        ) {
            val x0 = size.width * 0.12f
            val width = size.width * 0.85f
            val step = width / 6f
            val y0 = size.height * 0.17f
            val yStep = size.height * 0.66f / 5f
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textSize = 10.dp.toPx()
            }
            for (s in 0..5) {
                val y = y0 + s * yStep
                drawLine(textColor.copy(alpha = 0.40f), Offset(x0, y), Offset(x0 + width, y),
                    strokeWidth = (1.5f + s * 0.45f).dp.toPx())
            }
            for (f in 0..5) {
                val x = x0 + (f+1) * step
                drawLine(if (f == 0) Color(0xFFF2D18C) else textColor.copy(alpha = 0.24f),
                    Offset(x, y0-5.dp.toPx()), Offset(x, y0+yStep*5+5.dp.toPx()),
                    strokeWidth = if (f == 0) 3.dp.toPx() else 1.dp.toPx())
                drawContext.canvas.nativeCanvas.drawText("${f+1}", x + step*0.5f,
                    y0-10.dp.toPx(), paint)
            }
            current.forEach { (stringIndex, fret, midi) ->
                val x = x0 + (fret+0.5f) * step
                val y = y0 + stringIndex * yStep
                val isFocus = layout == "TUNING" && stringIndex == focusString-1
                val isRoot = layout != "TUNING" && midi % 12 == rootPitch
                val fill = when { isFocus || isRoot -> Color(0xFFE8527F)
                    layout == "TUNING" -> Color(0xFF3E5054)
                    else -> accent }
                val radius = if (isFocus || isRoot) 12.dp.toPx() else 10.7.dp.toPx()
                drawCircle(fill, radius, Offset(x,y))
                paint.color = if (layout == "TUNING" && !isFocus) android.graphics.Color.LTGRAY else android.graphics.Color.WHITE
                paint.textSize = 8.7.dp.toPx()
                drawContext.canvas.nativeCanvas.drawText(theoryNoteName(midi, flatFifth), x, y+3.3.dp.toPx(), paint)
            }
        }
        Text(info, color = Color(0xFFB6D2CE), fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp))
    }
}

@Composable
private fun TheoryVisualPiano(notes: List<Int>, textColor: Color, accent: Color,
                              flatFifth: Boolean) {
    val highlighted = notes.map { ((it % 12) + 12) % 12 }.toSet()
    Canvas(Modifier.fillMaxWidth().height(140.dp)
        .clip(RoundedCornerShape(10.dp)).background(Color(0xFF171D21))) {
        val margin = 8.dp.toPx()
        val whiteWidth = (size.width - margin*2) / 7f
        val top = 9.dp.toPx()
        val height = size.height - 18.dp.toPx()
        val whitePc = listOf(0,2,4,5,7,9,11)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textSize = 12.dp.toPx()
        }
        whitePc.forEachIndexed { i, pitch ->
            val left = margin + i*whiteWidth + 1.dp.toPx()
            drawRoundRect(
                color = if (pitch in highlighted) accent.copy(alpha = 0.78f) else Color(0xFFEBF0F2),
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(whiteWidth - 2.dp.toPx(), height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
            )
            paint.color = android.graphics.Color.rgb(22,36,40)
            drawContext.canvas.nativeCanvas.drawText(theoryNoteName(60 + pitch, flatFifth),
                left+whiteWidth/2f, top+height-12.dp.toPx(), paint)
        }
        listOf(0 to 1,1 to 3,3 to 6,4 to 8,5 to 10).forEach { (after, pc) ->
            val x = margin+(after+1)*whiteWidth - whiteWidth*0.30f
            val w = whiteWidth*0.60f
            drawRoundRect(
                color = if (pc in highlighted) Color(0xFFFFBF55) else Color(0xFF243139),
                topLeft = Offset(x,top),
                size = androidx.compose.ui.geometry.Size(w,height*0.60f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
            )
            if (pc in highlighted) {
                paint.color = android.graphics.Color.rgb(21,30,33)
                paint.textSize = 9.3.dp.toPx()
                drawContext.canvas.nativeCanvas.drawText(theoryNoteName(60 + pc, flatFifth),
                    x+w/2, top+height*0.60f-10.dp.toPx(), paint)
            }
        }
    }
}

@Composable
private fun TheoryVisualBeatGrid(count: Int, groupSize: Int, isMeter: Boolean,
                                 currentLang: String, textColor: Color, accent: Color) {
    val actualCount = count.coerceIn(1,16)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .background(Color(0xFF192226)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            repeat(actualCount) { index ->
                val group = groupSize.coerceAtLeast(1)
                val accented = index % group == 0
                Box(Modifier.weight(1f).height(55.dp).clip(RoundedCornerShape(7.dp))
                    .background(if (accented) accent.copy(alpha = 0.27f) else Color(0xFF313C42)),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${index+1}", color = if (accented) Color(0xFF82EACB) else Color.White,
                            fontSize = if (actualCount > 8) 10.sp else 14.sp, fontWeight = FontWeight.Bold)
                        Box(Modifier.size(if (accented) 8.dp else 5.dp)
                            .background(if (accented) accent else Color.LightGray, CircleShape))
                    }
                }
            }
        }
        Text(if (isMeter) {
                if (actualCount == 6) "6/8   •   1-2-3 | 4-5-6" else "$actualCount/4   •   ${1..actualCount}"
            } else "${actualCount} ${if (actualCount == 4) "× ♩" else if (actualCount == 8) "× ♪" else "× ♬"}   = " +
                fingerUiText(currentLang, "4 phách đen", "4 quarter beats"),
            fontSize = 12.sp, color = Color(0xFFDAE8E5), fontWeight = FontWeight.SemiBold)
        Text(if (isMeter) fingerUiText(currentLang,
                "Ô màu xanh biểu thị phách nhấn", "Green blocks mark accented beats")
             else fingerUiText(currentLang,
                "Mỗi ô tương ứng một nốt", "Each block represents one note"),
            fontSize = 10.sp, color = Color(0xFF9DAFAA))
    }
}

@Composable
private fun TheoryVisualVoicing(notes: List<Int>, textColor: Color, accent: Color,
                                 onNote: (Int) -> Unit) {
    val ordered = notes.sorted()
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .background(Color(0xFF192226)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("TOP VOICE / MELODY", color = Color(0xFFFFBF55), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        ordered.reversed().forEachIndexed { i, note ->
            val highest = i == 0
            val lowest = i == ordered.lastIndex
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp))
                .background(if (highest) Color(0xFFFFBF55).copy(alpha = .18f)
                            else if (lowest) accent.copy(alpha = .18f) else Color.White.copy(alpha = .055f))
                .clickable { onNote(note) }.padding(horizontal = 13.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(11.dp).background(if (highest) Color(0xFFFFBF55)
                    else if (lowest) accent else Color.LightGray, CircleShape))
                Spacer(Modifier.width(12.dp))
                Text(theoryNoteName(note), color = Color.White, fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.weight(1f))
                Text(if (highest) "MELODY" else if (lowest) "BASS" else "VOICE",
                    color = if (highest) Color(0xFFFFBF55) else Color(0xFFAFCECA), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun TheoryVisualProgression(example: TheoryVisualExample, textColor: Color,
                                    accent: Color, onChord: (List<Int>) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
        .clip(RoundedCornerShape(12.dp)).background(Color(0xFF192226)).padding(9.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        example.chordNames.forEachIndexed { i, name ->
            if (i > 0) Text("→", color = Color(0xFFFFBF55), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Surface(modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable {
                example.chordNotes.getOrNull(i)?.let(onChord)
            }, shape = RoundedCornerShape(10.dp),
                color = if (i == example.chordNames.lastIndex) accent.copy(alpha = 0.22f)
                        else Color(0xFF304147),
                border = androidx.compose.foundation.BorderStroke(1.dp,
                    if (i == example.chordNames.lastIndex) accent.copy(alpha = 0.7f)
                    else Color(0xFF637677).copy(alpha = .45f))) {
                Column(Modifier.padding(horizontal = 11.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(name, color = Color.White, fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Text(example.chordNotes.getOrNull(i)?.joinToString(" · ") { theoryNoteName(it) }.orEmpty(),
                        color = Color(0xFFC5DDD7), fontSize = 10.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun TheoryLearningSteps(topicId: String, currentLang: String, surfaceColor: Color,
                                textColor: Color, accent: Color) {
    val guide = remember(topicId) { theoryExtendedGuide(topicId) }
    val vi = currentLang.lowercase(Locale.ROOT).startsWith("vi")
    val steps = if (vi) guide.vi else guide.en
    if (steps.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (vi) "THỬ NGAY · TỪNG BƯỚC" else "TRY IT · STEP BY STEP",
            color = accent, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        steps.forEachIndexed { i, content ->
            Row(Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(10.dp))
                .padding(11.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(accent.copy(alpha=.16f)),
                    contentAlignment = Alignment.Center) {
                    Text("${i+1}", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(11.dp))
                Text(content, color = textColor, fontSize = 13.sp, lineHeight = 19.sp,
                    modifier = Modifier.weight(1f))
            }
        }
        Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFFFBF55).copy(alpha=.09f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFBF55).copy(alpha=.32f))) {
            Column(Modifier.fillMaxWidth().padding(11.dp)) {
                Text(if (vi) "LƯU Ý NHẠC LÝ" else "THEORY NOTE", color = Color(0xFFFFBF55),
                    fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(if (vi) guide.noteVi else guide.noteEn,
                    color = textColor, fontSize = 12.5.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun TheoryAcademyScreen(
    topics: List<TheoryTopic>,
    instrument: String,
    currentLang: String,
    bgColor: Color,
    surfaceColor: Color,
    textColor: Color,
    accent: Color,
    initialEntry: String,
    initialStage: Int,
    initialTopicId: String,
    onPractice: (TheoryTopic) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val isVi = currentLang.lowercase(Locale.ROOT).startsWith("vi")
    val borderColor = textColor.copy(alpha = 0.14f)
    var stage by rememberSaveable(initialEntry, initialStage, initialTopicId) {
        mutableIntStateOf(initialStage.coerceIn(0, 4))
    }
    var mode by rememberSaveable(initialEntry, initialStage, initialTopicId) {
        mutableStateOf(when (initialEntry) { "TOPIC" -> "TOPIC"; "QUIZ" -> "QUIZ"; else -> "LIBRARY" })
    }
    var topicId by rememberSaveable(initialEntry, initialStage, initialTopicId) { mutableStateOf(initialTopicId) }
    var completed by remember { mutableStateOf(TheoryLearningProgressStore.completedTopics(context)) }
    var quizIndex by rememberSaveable(initialEntry, initialStage, initialTopicId) { mutableIntStateOf(0) }
    var selectedAnswer by rememberSaveable(initialEntry, initialStage, initialTopicId) { mutableIntStateOf(-1) }
    var quizScore by rememberSaveable(initialEntry, initialStage, initialTopicId) { mutableIntStateOf(0) }
    var quizFinished by rememberSaveable(initialEntry, initialStage, initialTopicId) { mutableStateOf(false) }
    val quiz = remember(stage, topics) { theoryQuizForStage(topics, stage) }
    val foundationChapters = remember { theoryFoundationChapters() }
    val foundationTopics = remember { theoryFoundationTopics() }
    val allTopics = foundationTopics + topics
    val chosenTopic = allTopics.firstOrNull { it.id == topicId }
    val foundationLesson = chosenTopic?.id?.startsWith("F") == true
    var expandedChapter by rememberSaveable(initialStage) {
        mutableIntStateOf(when(initialStage) { 1 -> 2; 2,3 -> 3; 4 -> 4; else -> 0 })
    }
    var expandedExtra by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var soundJob by remember { mutableStateOf<Job?>(null) }
    val instrumentForAudio = when {
        instrument.contains("Piano", ignoreCase = true) -> "Piano"
        instrument.contains("Ukulele", ignoreCase = true) -> "Ukulele"
        instrument.contains("Bass", ignoreCase = true) -> "Bass"
        else -> "Guitar"
    }

    fun stopExample() {
        soundJob?.cancel()
        soundJob = null
    }
    fun startQuiz(forStage: Int) {
        stopExample()
        stage = forStage.coerceIn(0, 4)
        quizIndex = 0
        selectedAnswer = -1
        quizScore = 0
        quizFinished = false
        mode = "QUIZ"
    }
    fun openTopic(id: String) {
        stopExample()
        val target = allTopics.firstOrNull { it.id == id } ?: return
        stage = target.stage
        topicId = id
        mode = "TOPIC"
    }
    fun goBack() {
        stopExample()
        if (mode != "LIBRARY") mode = "LIBRARY" else onClose()
    }
    fun playExample(groups: List<List<Int>>, noteIntervalMs: Long = 480L) {
        stopExample()
        if (groups.isEmpty()) return
        runCatching {
            CoreAudioPlayer.init()
            CoreAudioPlayer.loadInstrument(context, instrumentForAudio)
        }
        soundJob = scope.launch {
            groups.forEach { chord ->
                if (chord.size > 1) {
                    runCatching { CoreAudioPlayer.playSimultaneousNotes(chord.map { it to 0.72f }, instrumentForAudio) }
                    delay(1000L)
                } else if (chord.isNotEmpty()) {
                    runCatching { CoreAudioPlayer.playSingleNote(chord.first(), instrumentForAudio, 0.85f) }
                    delay(noteIntervalMs.coerceIn(100L, 1500L))
                } else {
                    // An empty group is an actual rest; preserve the silent beat.
                    delay(noteIntervalMs.coerceIn(100L, 1500L))
                }
            }
        }
    }

    DisposableEffect(activity) {
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            soundJob?.cancel()
            if (activity != null && previousOrientation != null) activity.requestedOrientation = previousOrientation
        }
    }
    BackHandler { goBack() }

    val tabLabels = if (isVi) listOf("Kỹ thuật", "Câu chạy", "Triad", "Hòa âm", "Ứng dụng")
        else listOf("Technique", "Phrasing", "Triads", "Harmony", "Apply")
    val stageTopics = topics.filter { it.stage == stage }
    val stageRead = stageTopics.count { it.id in completed }
    val bestScore = TheoryLearningProgressStore.bestScore(context, stage)

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = bgColor) {
            Column(
                modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                    .padding(horizontal = 12.dp)
            ) {
                FingerSectionTopBar(
                    title = when (mode) {
                        "TOPIC" -> if (isVi) "Bài học nhạc lý" else "Theory lesson"
                        "QUIZ" -> if (isVi) "Kiểm tra nhạc lý" else "Theory quiz"
                        else -> if (isVi) "Thư viện nhạc lý" else "Music theory library"
                    },
                    subtitle = if (isVi) "5 chương nhạc lý · 18 bài cơ bản · Không khóa" else "5 chapters · 18 foundation lessons · Unlocked",
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    onBack = { goBack() }
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    if (mode == "LIBRARY") {
                        item(key = "foundation_intro") {
                            Surface(shape = RoundedCornerShape(15.dp), color = surfaceColor,
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                    Text(if (isVi) "GIÁO TRÌNH NHẠC LÝ" else "MUSIC THEORY TEXTBOOK",
                                        color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(6.dp))
                                    Text(if (isVi) "Học để hiểu bản nhạc" else "Understand the music you play",
                                        color = textColor, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
                                    Spacer(Modifier.height(6.dp))
                                    Text(if (isVi) "5 chương · 18 bài đọc · Ví dụ bằng khuông nhạc, cần đàn và piano. Không có game hoặc điều kiện mở khóa."
                                        else "5 chapters · 18 lessons · Staff, guitar and piano examples. No games or locked lessons.",
                                        color = textColor.copy(alpha = .76f), fontSize = 13.sp, lineHeight = 19.sp)
                                    Spacer(Modifier.height(12.dp))
                                    val read = foundationTopics.count { it.id in completed }
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (isVi) "$read/18 bài đã đọc" else "$read/18 lessons read",
                                            modifier = Modifier.weight(1f), color = textColor,
                                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text("${((read / 18f)*100).roundToInt()}%", color = accent,
                                            fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.height(7.dp))
                                    LinearProgressIndicator(progress = read / 18f,
                                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(20.dp)),
                                        color = accent, trackColor = textColor.copy(alpha=.1f))
                                }
                            }
                        }
                        foundationChapters.forEachIndexed { chapterIndex, chapter ->
                            val lessons = foundationTopics.filter { it.id in chapter.topicIds }
                            val readCount = lessons.count { it.id in completed }
                            val expanded = expandedChapter == chapterIndex
                            item(key = "foundation_chapter_${chapterIndex}") {
                                Surface(shape = RoundedCornerShape(15.dp), color = surfaceColor,
                                    border = androidx.compose.foundation.BorderStroke(1.dp,
                                        if (expanded) accent.copy(alpha=.55f) else borderColor)) {
                                    Column(Modifier.fillMaxWidth()) {
                                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
                                            .clickable { expandedChapter = if (expanded) -1 else chapterIndex }
                                            .padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.size(47.dp).clip(RoundedCornerShape(13.dp))
                                                .background(accent.copy(alpha=.13f)), contentAlignment = Alignment.Center) {
                                                Text("${chapterIndex+1}", color=accent,
                                                    fontWeight=FontWeight.Black,fontSize=21.sp)
                                            }
                                            Spacer(Modifier.width(11.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(chapter.title.text(currentLang), color=textColor,
                                                    fontSize=18.sp,fontWeight=FontWeight.ExtraBold)
                                                Spacer(Modifier.height(2.dp))
                                                Text(chapter.subtitle.text(currentLang),
                                                    color=textColor.copy(alpha=.72f),fontSize=12.sp,
                                                    maxLines=2,overflow=TextOverflow.Ellipsis)
                                                Spacer(Modifier.height(7.dp))
                                                Text(if (isVi) "$readCount/${lessons.size} bài đã đọc"
                                                    else "$readCount/${lessons.size} read",
                                                    color=accent,fontSize=11.sp,fontWeight=FontWeight.Bold)
                                            }
                                            Icon(if (expanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowForward,
                                                contentDescription=null,tint=accent,modifier=Modifier.size(24.dp))
                                        }
                                        LinearProgressIndicator(
                                            progress=if(lessons.isEmpty()) 0f else readCount.toFloat()/lessons.size,
                                            modifier=Modifier.fillMaxWidth().padding(horizontal=14.dp)
                                                .height(4.dp).clip(RoundedCornerShape(8.dp)),
                                            color=accent, trackColor=textColor.copy(alpha=.10f))
                                        Spacer(Modifier.height(12.dp))
                                        if (expanded) {
                                            HorizontalDivider(color=borderColor)
                                            Column(Modifier.padding(10.dp),
                                                verticalArrangement=Arrangement.spacedBy(7.dp)) {
                                                lessons.forEachIndexed { i, lesson ->
                                                    TheoryAcademyTopicCard(
                                                        topic=lesson,number=i+1,currentLang=currentLang,
                                                        surfaceColor=bgColor,textColor=textColor,accent=accent,
                                                        completed=lesson.id in completed,
                                                        onClick={ openTopic(lesson.id) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item(key = "foundation_extra") {
                            Surface(shape = RoundedCornerShape(14.dp), color=surfaceColor,
                                border=androidx.compose.foundation.BorderStroke(1.dp,borderColor)) {
                                Column(Modifier.fillMaxWidth()) {
                                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                        .clickable { expandedExtra=!expandedExtra }.padding(14.dp),
                                        verticalAlignment=Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info,contentDescription=null,tint=accent,
                                            modifier=Modifier.size(23.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(if(isVi) "Tra cứu nhạc lý nâng cao" else "Advanced theory reference",
                                                color=textColor,fontSize=15.sp,fontWeight=FontWeight.Bold)
                                            Text(if(isVi) "Giữ nguyên 16 chủ đề trước đây" else "The previous 16 theory topics remain available",
                                                color=textColor.copy(alpha=.68f),fontSize=12.sp)
                                        }
                                        Icon(if(expandedExtra) Icons.Default.ArrowDropDown else Icons.Default.ArrowForward,
                                            contentDescription=null,tint=accent)
                                    }
                                    if(expandedExtra) {
                                        Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                                            topics.forEachIndexed { i, lesson ->
                                                TheoryAcademyTopicCard(topic=lesson, number=i+1,
                                                    currentLang=currentLang,surfaceColor=bgColor,
                                                    textColor=textColor,accent=accent,
                                                    completed=lesson.id in completed,
                                                    onClick={openTopic(lesson.id)})
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (mode == "TOPIC" && chosenTopic != null) {
                        item(key = "theory_topic_header") {
                            Surface(shape = RoundedCornerShape(14.dp), color = surfaceColor,
                                border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.45f))) {
                                Column(Modifier.fillMaxWidth().padding(15.dp)) {
                                    Text(if (foundationLesson) {
                                        if (isVi) "CHƯƠNG ${chosenTopic.stage + 1} · BÀI LÝ THUYẾT"
                                        else "CHAPTER ${chosenTopic.stage + 1} · THEORY LESSON"
                                    } else {
                                        if (isVi) "CHỦ ĐỀ NÂNG CAO · NHẠC LÝ" else "ADVANCED THEORY REFERENCE"
                                    },
                                        color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(7.dp))
                                    Text(chosenTopic.title.text(currentLang), color = textColor,
                                        fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                                    Spacer(Modifier.height(4.dp))
                                    Text(chosenTopic.subtitle.text(currentLang), color = accent,
                                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        item(key = "theory_visual") {
                            TheoryVisualExplorer(
                                topic = chosenTopic,
                                currentLang = currentLang,
                                surfaceColor = surfaceColor,
                                textColor = textColor,
                                accent = accent,
                                onPlay = { notes, interval -> playExample(notes, interval) }
                            )
                        }
                        item(key = "theory_concept") {
                            Column(Modifier.fillMaxWidth().padding(4.dp)) {
                                Text(if (isVi) "1. KIẾN THỨC CỐT LÕI" else "1. CORE CONCEPT",
                                    color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(7.dp))
                                Text(chosenTopic.keyIdea.text(currentLang), color = textColor,
                                    fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        chosenTopic.details.forEachIndexed { i, detail ->
                            item(key = "theory_fact_$i") {
                                Row(Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(11.dp))
                                    .padding(12.dp), verticalAlignment = Alignment.Top) {
                                    Text("${i + 1}.", color = accent, fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold, modifier = Modifier.width(26.dp))
                                    Text(detail.text(currentLang), color = textColor.copy(alpha = 0.88f),
                                        fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        item(key = "theory_detailed_steps") {
                            TheoryLearningSteps(
                                topicId = chosenTopic.id,
                                currentLang = currentLang,
                                surfaceColor = surfaceColor,
                                textColor = textColor,
                                accent = accent
                            )
                        }
                        item(key = "theory_hear") {
                            Surface(shape = RoundedCornerShape(12.dp), color = surfaceColor,
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)) {
                                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                    Text(if (isVi) "2. NGHE MINH HỌA" else "2. LISTEN TO AN EXAMPLE",
                                        color = accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Spacer(Modifier.height(6.dp))
                                    Text(chosenTopic.exampleCaption.text(currentLang), color = textColor,
                                        fontSize = 13.sp, lineHeight = 19.sp)
                                    Spacer(Modifier.height(11.dp))
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                            .clickable { playExample(chosenTopic.audioGroups) },
                                        color = accent.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center) {
                                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = accent)
                                            Spacer(Modifier.width(9.dp))
                                            Text(if (isVi) "Nghe mẫu" else "Play example", color = accent,
                                                fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        if (!foundationLesson) item(key = "theory_practice") {
                            Surface(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                                    .clickable { stopExample(); onPractice(chosenTopic) },
                                shape = RoundedCornerShape(11.dp), color = surfaceColor,
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                            ) {
                                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (isVi) "3. Luyện trên guitar / trong bài học" else "3. Practise in the guitar lessons",
                                        color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = accent)
                                }
                            }
                        }
                        item(key = "theory_read") {
                            Surface(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                                    .clickable {
                                        TheoryLearningProgressStore.markRead(context, chosenTopic.id)
                                        completed = completed + chosenTopic.id
                                    },
                                shape = RoundedCornerShape(11.dp),
                                color = if (chosenTopic.id in completed) accent.copy(alpha = 0.13f) else surfaceColor,
                                border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.45f))
                            ) {
                                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = accent)
                                    Spacer(Modifier.width(9.dp))
                                    Text(if (chosenTopic.id in completed) {
                                        if (isVi) "Đã đánh dấu đã học" else "Marked as studied"
                                    } else {
                                        if (isVi) "Đánh dấu đã học" else "Mark as studied"
                                    }, color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else if (mode == "QUIZ") {
                        if (!quizFinished && quizIndex in quiz.indices) {
                            val question = quiz[quizIndex]
                            item(key = "theory_question_header") {
                                Surface(shape = RoundedCornerShape(13.dp), color = surfaceColor,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)) {
                                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                        Text(if (isVi) "GIAI ĐOẠN ${stage + 1} · CÂU ${quizIndex + 1}/${quiz.size}"
                                            else "STAGE ${stage + 1} · QUESTION ${quizIndex + 1}/${quiz.size}",
                                            color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(9.dp))
                                        LinearProgressIndicator(
                                            progress = quizIndex.toFloat() / quiz.size.coerceAtLeast(1),
                                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(20)),
                                            color = accent, trackColor = textColor.copy(alpha = 0.12f)
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Text(question.prompt.text(currentLang), color = textColor,
                                            fontSize = 18.sp, lineHeight = 25.sp,
                                            fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                            question.choices.forEachIndexed { index, option ->
                                item(key = "theory_answer_${quizIndex}_$index") {
                                    val isCorrect = index == question.correctIndex
                                    val revealed = selectedAnswer >= 0
                                    val isSelected = index == selectedAnswer
                                    val highlightColor = if (revealed && isCorrect) Color(0xFF15B687)
                                        else if (revealed && isSelected) Color(0xFFE85D86) else accent
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                            .clickable(enabled = !revealed) {
                                                selectedAnswer = index
                                                if (isCorrect) quizScore += 1
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (revealed && (isCorrect || isSelected))
                                            highlightColor.copy(alpha = 0.13f) else surfaceColor,
                                        border = androidx.compose.foundation.BorderStroke(
                                            if (revealed && (isCorrect || isSelected)) 1.6.dp else 1.dp,
                                            if (revealed && (isCorrect || isSelected)) highlightColor else borderColor
                                        )
                                    ) {
                                        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text("${'A' + index}.", color = if (revealed && (isCorrect || isSelected)) highlightColor else accent,
                                                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                                modifier = Modifier.width(30.dp))
                                            Text(option.text(currentLang), color = textColor,
                                                fontSize = 14.sp, modifier = Modifier.weight(1f))
                                            if (revealed && isCorrect) Icon(Icons.Default.Check,
                                                contentDescription = null, tint = Color(0xFF15B687),
                                                modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                            if (selectedAnswer >= 0) {
                                item(key = "theory_explanation") {
                                    Surface(shape = RoundedCornerShape(12.dp), color = accent.copy(alpha = 0.08f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.24f))) {
                                        Column(Modifier.fillMaxWidth().padding(13.dp)) {
                                            Text(if (isVi) "Giải thích" else "Explanation", color = accent,
                                                fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Spacer(Modifier.height(6.dp))
                                            Text(question.explanation.text(currentLang), color = textColor,
                                                fontSize = 13.sp, lineHeight = 19.sp)
                                        }
                                    }
                                }
                                item(key = "theory_next_question") {
                                    Button(
                                        onClick = {
                                            if (quizIndex == quiz.lastIndex) {
                                                TheoryLearningProgressStore.recordQuiz(context, stage, quizScore)
                                                quizFinished = true
                                            } else {
                                                quizIndex += 1
                                                selectedAnswer = -1
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                                        shape = RoundedCornerShape(11.dp)
                                    ) {
                                        Text(if (quizIndex == quiz.lastIndex) {
                                            if (isVi) "Xem kết quả" else "See results"
                                        } else {
                                            if (isVi) "Câu tiếp theo" else "Next question"
                                        }, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            item(key = "theory_quiz_result") {
                                Surface(shape = RoundedCornerShape(16.dp), color = surfaceColor,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.48f))) {
                                    Column(Modifier.fillMaxWidth().padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(if (isVi) "ĐÃ HOÀN THÀNH KIỂM TRA" else "QUIZ COMPLETED",
                                            color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(9.dp))
                                        Text("$quizScore/${quiz.size}", fontSize = 38.sp,
                                            fontWeight = FontWeight.Black, color = textColor)
                                        Text(if (isVi) "Điểm đã lưu riêng, không ảnh hưởng 13 bài chính"
                                             else "Score saved separately from the 13 core lessons",
                                            color = textColor.copy(alpha = 0.72f), fontSize = 12.sp,
                                            textAlign = TextAlign.Center)
                                        Spacer(Modifier.height(15.dp))
                                        Button(onClick = { startQuiz(stage) },
                                            colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                                            Text(if (isVi) "Làm lại 5 câu" else "Try the five questions again")
                                        }
                                        Spacer(Modifier.height(5.dp))
                                        Surface(modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                            .clickable { mode = "LIBRARY" }, color = surfaceColor) {
                                            Text(if (isVi) "Về thư viện nhạc lý" else "Back to theory library",
                                                color = accent, modifier = Modifier.padding(12.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TheoryAcademyTopicCard(
    topic: TheoryTopic,
    number: Int,
    currentLang: String,
    surfaceColor: Color,
    textColor: Color,
    accent: Color,
    completed: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp), color = surfaceColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (completed) accent.copy(alpha = 0.50f)
            else textColor.copy(alpha = 0.13f))
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(9.dp), color = accent.copy(alpha = 0.13f)) {
                Text("${number.toString().padStart(2, '0')}", color = accent, fontWeight = FontWeight.Bold,
                    fontSize = 12.sp, modifier = Modifier.padding(horizontal = 9.dp, vertical = 11.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(topic.title.text(currentLang), color = textColor, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(topic.subtitle.text(currentLang), color = textColor.copy(alpha = 0.68f),
                    fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Icon(if (completed) Icons.Default.Check else Icons.Default.ArrowForward,
                contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
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
    onOpenHandShapes: () -> Unit,
    onOpenHarmony: () -> Unit,
    onOpenTheoryLibrary: (Int) -> Unit,
    onOpenTheoryTopic: (String) -> Unit,
    onOpenTheoryQuiz: (Int) -> Unit,
    onLessonSelected: (GuidedPracticeLesson) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val isVietnamese = currentLang.lowercase(Locale.ROOT).startsWith("vi")
    val roadmapPrefs = remember(context) {
        context.getSharedPreferences("tuner_tools_guided_practice_v1", android.content.Context.MODE_PRIVATE)
    }
    var selectedStage by rememberSaveable {
        mutableIntStateOf(roadmapPrefs.getInt("selected_stage", 0).coerceIn(0, 4))
    }
    var completedIds by remember { mutableStateOf(GuidedPracticeProgressStore.completedIds(context)) }
    val lessonListState = rememberLazyListState()

    LaunchedEffect(lessons) {
        completedIds = GuidedPracticeProgressStore.completedIds(context)
    }
    LaunchedEffect(selectedStage) {
        lessonListState.scrollToItem(0)
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
    val progress = if (totalUnitCount == 0) 0f else completedCount.toFloat() / totalUnitCount
    val harmonyAccent = Color(0xFFE85D86)
    val borderColor = textColor.copy(alpha = 0.12f)

    // The five stages are consecutive GROUPS of the existing 13 guided units:
    // 01-04 / 05-06 / 07-08 / 09-11 / 12-13 (lesson 13 has parts A/B/C).
    // Other scale/fretboard/harmony activities are explicitly unnumbered extras.
    val stageLabels = if (isVietnamese) {
        listOf("Kỹ thuật", "Câu chạy", "Triad", "Hòa âm", "Ứng dụng")
    } else {
        listOf("Technique", "Phrasing", "Triads", "Harmony", "Apply")
    }
    val stageLessonRanges = listOf("01–04", "05–06", "07–08", "09–11", "12–13")
    val stageTitles = if (isVietnamese) {
        listOf(
            "Kỹ thuật ngón căn bản",
            "Phát triển câu chạy và legato",
            "Cấu tạo Triad và thế đảo",
            "Vòng hợp âm và dẫn giọng",
            "Hợp âm 7, Arpeggio và ứng dụng"
        )
    } else {
        listOf(
            "Foundational finger technique",
            "Note sequences and legato",
            "Triads and chord inversions",
            "Chord progressions and voice leading",
            "Seventh chords, arpeggios and application"
        )
    }
    val stageDescriptions = if (isVietnamese) {
        listOf(
            "Bài 01–04: Phối hợp bốn ngón, chuyển dây, độc lập ngón và Spider.",
            "Bài 05–06: Chuỗi ba nốt, hammer-on và pull-off; khám phá thêm ngũ cung.",
            "Bài 07–08: Nhận biết nốt hợp âm 1–3–5 và ba thế đảo của Triad.",
            "Bài 09–11: Hợp âm trong giọng, ii–V–I và voice-leading vòng Pop.",
            "Bài 12–13: Hợp âm 7, guide tones và vòng quãng 4; thực hành hòa âm mở rộng."
        )
    } else {
        listOf(
            "Lessons 01–04: Four fingers, string crossing, independence and Spider.",
            "Lessons 05–06: Three-note sequences and legato; optional pentatonic practice.",
            "Lessons 07–08: Chord tones 1–3–5 and three triad inversions.",
            "Lessons 09–11: Diatonic chords, ii–V–I and pop voice leading.",
            "Lessons 12–13: Seventh chords, guide tones and the circle of fourths."
        )
    }
    val stageLessons = lessons.filter { guidedRoadmapStageForLesson(it) == selectedStage }
    val stageTheoryTopics = remember(selectedStage) { theoryTopics().filter { it.stage == selectedStage } }
    val theoryReadTopics = remember { TheoryLearningProgressStore.completedTopics(context) }
    val theoryBestScore = TheoryLearningProgressStore.bestScore(context, selectedStage)
    val stageUnitTotal = guidedCurriculumUnitCount(stageLessons)
    val stageUnitCompleted = guidedCurriculumCompletedCount(stageLessons, completedIds)
    val stageProgress = if (stageUnitTotal == 0) 0f else stageUnitCompleted.toFloat() / stageUnitTotal
    val stageEnabled = stageLessons.any { lesson ->
        when (lesson.kind) {
            GuidedLessonKind.FINGER -> instrument != "Piano"
            GuidedLessonKind.HARMONY -> instrument == "Guitar"
        }
    }
    val nextLesson = stageLessons.firstOrNull { it.id !in completedIds } ?: stageLessons.firstOrNull()
    val stageAccent = if (selectedStage >= 2) harmonyAccent else tunaGreen

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
                    subtitle = if (isVietnamese) "13 bài theo thứ tự · Chia thành 5 giai đoạn" else "13 core lessons · 5 ordered stages",
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    onBack = onClose
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (isVietnamese) "Tiến độ 13 bài chính" else "13 core lessons: progress",
                                    color = textColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (isVietnamese) "$completedCount/$totalUnitCount bài hoàn thành · Bài 13 gồm 3 phần"
                                    else "$completedCount/$totalUnitCount lessons done · Lesson 13 has 3 parts",
                                    color = textColor.copy(alpha = 0.68f),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                "${(progress * 100).roundToInt()}%",
                                color = tunaGreen,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(Modifier.height(7.dp))
                        LinearProgressIndicator(
                            progress = progress.coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)),
                            color = tunaGreen,
                            trackColor = textColor.copy(alpha = 0.10f)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stageLabels.forEachIndexed { index, label ->
                        val chosen = selectedStage == index
                        val stageColor = if (index >= 2) harmonyAccent else tunaGreen
                        val stagePartLessons = lessons.filter { guidedRoadmapStageForLesson(it) == index }
                        val stageDone = stagePartLessons.isNotEmpty() &&
                            guidedCurriculumCompletedCount(stagePartLessons, completedIds) ==
                            guidedCurriculumUnitCount(stagePartLessons)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 72.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .clickable {
                                    selectedStage = index
                                    roadmapPrefs.edit().putInt("selected_stage", index).apply()
                                },
                            shape = RoundedCornerShape(11.dp),
                            color = if (chosen) stageColor.copy(alpha = 0.17f) else surfaceColor,
                            border = androidx.compose.foundation.BorderStroke(
                                if (chosen) 1.5.dp else 1.dp,
                                if (chosen) stageColor else borderColor
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    stageLessonRanges[index],
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (chosen || stageDone) stageColor else textColor.copy(alpha = 0.76f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    label,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (chosen) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = textColor.copy(alpha = if (chosen) 1f else 0.72f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(9.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = surfaceColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, stageAccent.copy(alpha = 0.42f))
                ) {
                    Column(modifier = Modifier.padding(11.dp)) {
                        Text(
                            if (isVietnamese) "GIAI ĐOẠN ${selectedStage + 1}/5  ·  BÀI ${stageLessonRanges[selectedStage]}"
                            else "STAGE ${selectedStage + 1}/5  ·  LESSONS ${stageLessonRanges[selectedStage]}",
                            color = stageAccent,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(stageTitles[selectedStage], color = textColor, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            stageDescriptions[selectedStage],
                            color = textColor.copy(alpha = 0.75f),
                            fontSize = 11.5.sp,
                            lineHeight = 15.5.sp
                        )
                        Spacer(Modifier.height(7.dp))
                        if (stageUnitTotal > 0) {
                            Text(
                                if (isVietnamese) "$stageUnitCompleted/$stageUnitTotal bài đã hoàn thành"
                                else "$stageUnitCompleted/$stageUnitTotal lessons complete",
                                color = textColor.copy(alpha = 0.68f),
                                fontSize = 10.5.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = stageProgress.coerceIn(0f, 1f),
                                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(50)),
                                color = stageAccent,
                                trackColor = textColor.copy(alpha = 0.10f)
                            )
                        }
                        if (stageEnabled) {
                            Spacer(Modifier.height(7.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { nextLesson?.let(onLessonSelected) },
                                color = stageAccent.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        if (isVietnamese) {
                                            if (stageUnitCompleted == stageUnitTotal) "Ôn lại · Bài ${nextLesson?.number.orEmpty()}"
                                            else "Tiếp tục · Bài ${nextLesson?.number.orEmpty()}"
                                        } else {
                                            if (stageUnitCompleted == stageUnitTotal) "Review · Lesson ${nextLesson?.number.orEmpty()}"
                                            else "Continue · Lesson ${nextLesson?.number.orEmpty()}"
                                        },
                                        color = stageAccent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = stageAccent, modifier = Modifier.size(19.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = lessonListState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    item(key = "stage_${selectedStage}_theory_heading") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(if (isVietnamese) "CHỦ ĐỀ NHẠC LÝ LIÊN QUAN" else "RELATED THEORY TOPICS",
                                    color = stageAccent, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    if (isVietnamese) "${stageTheoryTopics.count { it.id in theoryReadTopics }}/${stageTheoryTopics.size} chuyên đề · Giáo trình cơ bản có 18 bài riêng"
                                    else "${stageTheoryTopics.count { it.id in theoryReadTopics }}/${stageTheoryTopics.size} topics · 18 foundation lessons in the textbook",
                                    color = textColor.copy(alpha = 0.72f), fontSize = 10.5.sp
                                )
                            }
                            Surface(
                                modifier = Modifier.clip(RoundedCornerShape(9.dp))
                                    .clickable { onOpenTheoryLibrary(selectedStage) },
                                shape = RoundedCornerShape(9.dp), color = stageAccent.copy(alpha = 0.13f)
                            ) {
                                Text(if (isVietnamese) "Giáo trình" else "Textbook",
                                    color = stageAccent, fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp))
                            }
                        }
                    }
                    stageTheoryTopics.forEachIndexed { index, topic ->
                        item(key = "guided_theory_${topic.id}") {
                            TheoryAcademyTopicCard(
                                topic = topic, number = index + 1, currentLang = currentLang,
                                surfaceColor = surfaceColor, textColor = textColor,
                                accent = stageAccent, completed = topic.id in theoryReadTopics,
                                onClick = { onOpenTheoryTopic(topic.id) }
                            )
                        }
                    }
                    item(key = "guided_theory_quiz_${selectedStage}") {
                        Surface(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .clickable { onOpenTheoryQuiz(selectedStage) },
                            shape = RoundedCornerShape(12.dp),
                            color = stageAccent.copy(alpha = 0.13f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, stageAccent.copy(alpha = 0.42f))
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = stageAccent,
                                    modifier = Modifier.size(21.dp))
                                Spacer(Modifier.width(9.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(if (isVietnamese) "Tự ôn tập · 5 câu (tùy chọn)" else "Optional review · 5 questions",
                                        color = textColor, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                    Text(if (isVietnamese) "Điểm tốt nhất: ${if (theoryBestScore < 0) "—" else "$theoryBestScore/5"} · Không khóa bài"
                                         else "Best: ${if (theoryBestScore < 0) "—" else "$theoryBestScore/5"} · No locks",
                                        color = textColor.copy(alpha = 0.72f), fontSize = 10.5.sp)
                                }
                                Icon(Icons.Default.ArrowForward, contentDescription = null,
                                    tint = stageAccent, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    if (stageUnitTotal > 0) {
                        item(key = "stage_${selectedStage}_heading") {
                            Text(
                                if (isVietnamese) "BÀI LUYỆN CHÍNH · THUỘC 13 BÀI" else "CORE PRACTICE · PART OF 13 LESSONS",
                                color = textColor.copy(alpha = 0.84f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 3.dp, top = 4.dp)
                            )
                        }
                    }
                    stageLessons.forEachIndexed { index, lesson ->
                        val chapter = guidedPracticeChapter(lesson.chapterKey)
                        val previousChapterKey = stageLessons.getOrNull(index - 1)?.chapterKey
                        if (chapter != null) {
                            if (chapter.key != previousChapterKey) {
                                val chapterLessons = stageLessons.filter { it.chapterKey == chapter.key }
                                item(key = "stage_${selectedStage}_chapter_${chapter.key}") {
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
                                val enabled = if (lesson.kind == GuidedLessonKind.FINGER) instrument != "Piano"
                                    else instrument == "Guitar"
                                GuidedCurriculumLessonCard(
                                    lesson = lesson,
                                    currentLang = currentLang,
                                    surfaceColor = surfaceColor,
                                    textColor = textColor,
                                    accent = if (lesson.kind == GuidedLessonKind.HARMONY) harmonyAccent else lesson.accent,
                                    completed = lesson.id in completedIds,
                                    bestBpm = GuidedPracticeProgressStore.bestBpm(context, lesson.id),
                                    enabled = enabled,
                                    onClick = { onLessonSelected(lesson) }
                                )
                            }
                        }
                    }
                    if (selectedStage == 1 || selectedStage == 2 || selectedStage == 4) {
                        item(key = "stage_${selectedStage}_extras_heading") {
                            Text(
                                if (isVietnamese) "THỰC HÀNH MỞ RỘNG · KHÔNG TÍNH TRONG 13 BÀI"
                                else "OPTIONAL PRACTICE · OUTSIDE THE 13 CORE LESSONS",
                                color = textColor.copy(alpha = 0.72f),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 3.dp, top = 12.dp, bottom = 2.dp)
                            )
                        }
                    }
                    if (selectedStage == 1 || selectedStage == 2) {
                        val topics = if (selectedStage == 1) {
                            if (isVietnamese) listOf(
                                "Nhận diện nốt chủ" to "Chọn một tông, tìm các nốt gốc trên sáu dây.",
                                "Ngũ cung trưởng và thứ" to "So sánh bậc âm, nghe và chạy các nốt.",
                                "Năm thế CAGED" to "Chuyển qua C – A – G – E – D trên cùng bộ nốt."
                            ) else listOf(
                                "Find root notes" to "Choose a key and locate its roots on six strings.",
                                "Major and minor pentatonic" to "Compare scale degrees and practise the notes.",
                                "Five CAGED shapes" to "Move through C – A – G – E – D with the same note set."
                            )
                        } else {
                            if (isVietnamese) listOf(
                                "CAGED và vị trí thế tay" to "Xem các khuôn nốt và vị trí trên từng vùng cần đàn.",
                                "3NPS và 7 mode" to "Luyện ba nốt trên mỗi dây với âm giai 7 nốt.",
                                "Toàn cần đàn" to "Quan sát nốt và các vùng thế nối tiếp nhau."
                            ) else listOf(
                                "CAGED and hand positions" to "Explore chord shapes and fretboard positions.",
                                "3NPS and seven modes" to "Practise three notes per string with seven-note scales.",
                                "Full neck" to "Explore connected positions along the fretboard."
                            )
                        }
                        topics.forEachIndexed { index, topic ->
                            item(key = "stage_${selectedStage}_topic_$index") {
                                GuidedRoadmapTopicCard(
                                    number = "↗",
                                    title = topic.first,
                                    description = topic.second,
                                    accent = tunaGreen,
                                    surfaceColor = surfaceColor,
                                    textColor = textColor,
                                    enabled = instrument == "Guitar",
                                    onClick = onOpenHandShapes
                                )
                            }
                        }
                    }
                    if (selectedStage == 4) {
                        val practiceTopics = if (isVietnamese) {
                            listOf(
                                "Passing Chord" to "Trong Vòng hợp âm, nhấn + giữa hai hợp âm để nghe và chọn hợp âm chuyển.",
                                "Chord Melody" to "Trong Hòa âm, chọn Chord Melody để ghép giai điệu vào thế hợp âm."
                            )
                        } else {
                            listOf(
                                "Passing chords" to "In Progressions, tap + between chords to preview and insert a passing chord.",
                                "Chord melody" to "In Harmony, select Chord Melody to combine melody with a chord voicing."
                            )
                        }
                        practiceTopics.forEachIndexed { index, topic ->
                            item(key = "stage_4_topic_$index") {
                                GuidedRoadmapTopicCard(
                                    number = "↗",
                                    title = topic.first,
                                    description = topic.second,
                                    accent = harmonyAccent,
                                    surfaceColor = surfaceColor,
                                    textColor = textColor,
                                    enabled = instrument == "Guitar",
                                    onClick = onOpenHarmony
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
private fun GuidedRoadmapTopicCard(
    number: String,
    title: String,
    description: String,
    accent: Color,
    surfaceColor: Color,
    textColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = surfaceColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.30f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(9.dp), color = accent.copy(alpha = 0.13f)) {
                Text(number, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = textColor.copy(alpha = if (enabled) 1f else 0.45f),
                    fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text(description, color = textColor.copy(alpha = if (enabled) 0.70f else 0.35f),
                    fontSize = 10.5.sp, lineHeight = 14.sp)
            }
            Spacer(Modifier.width(8.dp))
            Icon(if (enabled) Icons.Default.ArrowForward else Icons.Default.Lock,
                contentDescription = null, tint = accent.copy(alpha = if (enabled) 1f else 0.35f),
                modifier = Modifier.size(18.dp))
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
                modifier = Modifier.padding(start = 4.dp, end = 9.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (isVietnamese) "Chọn phần học" else "Choose chapter part",
                    tint = currentLesson.accent,
                    modifier = Modifier.size(if (compact) 17.dp else 20.dp)
                )
                Spacer(Modifier.width(3.dp))
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
                        title = if (isVietnamese) "Hòa âm & Chord Melody" else "Harmony & Chord Melody",
                        description = if (isVietnamese) {
                            "Luyện vòng mẫu hoặc vòng riêng, chèn Passing Chord và đặt nốt melody ở bè trên."
                        } else {
                            "Practice presets or custom loops, insert passing chords and keep melody on top."
                        },
                        metaLeft = if (isVietnamese) "$harmonyProgressionCount vòng · 12 tông"
                        else "$harmonyProgressionCount loops · 12 keys",
                        metaRight = if (isVietnamese) "Dẫn giọng" else "Voice leading",
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