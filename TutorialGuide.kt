package ducphi.tunnerinstrument

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// ============================================================================
// TUNERTOOLS VIDEO GUIDES - SERVER CATALOG + 15 WEBVTT SUBTITLE LANGUAGES
// Uses only Android VideoView / standard Compose, no new Gradle dependencies.
// ============================================================================
private data class TunerGuideVideo(
    val id: String,
    val category: String,
    val title: String,
    val description: String,
    val videoUrl: String,
    val subtitleUrl: String?,
    val subtitleLanguage: String?,
    val subtitleFallback: Boolean,
    val thumbnailUrl: String?
)

private data class TunerVttCue(val startMs: Long, val endMs: Long, val text: String)

// Android TTS voiceover uses the same WebVTT cues as the 15 subtitle languages.
// Speech stays optional, and an unavailable language NEVER falls back to a wrong voice.
private enum class TunerGuideVoiceStatus { INITIALIZING, READY, MISSING_LANGUAGE, ERROR, SUBTITLE_MISMATCH }

private fun tunerGuideVoiceLocale(code: String): java.util.Locale = java.util.Locale.forLanguageTag(
    when (tunerGuideLanguage(code)) {
        "vi" -> "vi-VN"
        "en" -> "en-US"
        "es" -> "es-ES"
        "fr" -> "fr-FR"
        "de" -> "de-DE"
        "ru" -> "ru-RU"
        "ja" -> "ja-JP"
        "zh" -> "zh-CN"
        "th" -> "th-TH"
        "lo" -> "lo-LA"
        "km" -> "km-KH"
        "ko" -> "ko-KR"
        "pt" -> "pt-BR"
        "sw" -> "sw-KE"
        "ar" -> "ar-SA"
        else -> "en-US"
    }
)

private fun tunerGuideVoiceLanguageName(code: String): String = when (tunerGuideLanguage(code)) {
    "vi" -> "Tiếng Việt"; "en" -> "English"; "es" -> "Español"
    "fr" -> "Français"; "de" -> "Deutsch"; "ru" -> "Русский"
    "ja" -> "日本語"; "zh" -> "中文"; "th" -> "ภาษาไทย"
    "lo" -> "ພາສາລາວ"; "km" -> "ភាសាខ្មែរ"; "ko" -> "한국어"
    "pt" -> "Português"; "sw" -> "Kiswahili"; "ar" -> "العربية"
    else -> "English"
}

/**
 * Only accept installed voices for the requested language. A TTS service may claim
 * support for a Locale but silently select its English default; verify the voice.
 * Prefer an offline voice where both local/network variants are installed.
 */
private fun tunerGuideSelectVoice(engine: TextToSpeech, languageCode: String): Boolean {
    val code = tunerGuideLanguage(languageCode)
    val preferred = tunerGuideVoiceLocale(code)
    val languageOnly = java.util.Locale.forLanguageTag(code)
    val candidates = listOf(preferred, languageOnly).distinctBy { it.toLanguageTag() }
    val availableVoices = runCatching { engine.voices.orEmpty() }
        .getOrDefault(emptySet())
        .filter { voice ->
            voice.locale.language.equals(code, ignoreCase = true) &&
                    TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in voice.features.orEmpty()
        }
        .sortedWith(compareBy<android.speech.tts.Voice> { it.isNetworkConnectionRequired }
            .thenByDescending { it.locale.country.equals(preferred.country, ignoreCase = true) }
            .thenByDescending { it.quality })

    for (candidate in candidates) {
        val supported = runCatching { engine.isLanguageAvailable(candidate) }
            .getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        if (supported < TextToSpeech.LANG_AVAILABLE) continue
        val selected = runCatching { engine.setLanguage(candidate) }
            .getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        if (selected < TextToSpeech.LANG_AVAILABLE) continue

        val currentVoice = runCatching { engine.voice }.getOrNull()
        if (currentVoice != null &&
            currentVoice.locale.language.equals(code, ignoreCase = true) &&
            TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in currentVoice.features.orEmpty()
        ) return true

        // Some engines pick a fallback language on setLanguage(). Override it only
        // if they expose a real, installed voice in the requested language.
        for (matchingVoice in availableVoices) {
            if (runCatching { engine.setVoice(matchingVoice) }.getOrDefault(TextToSpeech.ERROR) == TextToSpeech.SUCCESS &&
                runCatching { engine.voice?.locale?.language }.getOrNull().equals(code, ignoreCase = true)
            ) return true
        }
    }

    // A service might expose voices even when the regional Locale is unavailable.
    for (matchingVoice in availableVoices) {
        if (runCatching { engine.setVoice(matchingVoice) }.getOrDefault(TextToSpeech.ERROR) == TextToSpeech.SUCCESS &&
            runCatching { engine.voice?.locale?.language }.getOrNull().equals(code, ignoreCase = true)
        ) return true
    }
    return false
}

private fun tunerGuideVoiceHint(code: String, status: TunerGuideVoiceStatus): String {
    val name = tunerGuideVoiceLanguageName(code)
    val vietnamese = tunerGuideLanguage(code) == "vi"
    return when (status) {
        TunerGuideVoiceStatus.INITIALIZING ->
            if (vietnamese) "Đang chuẩn bị giọng đọc tiếng Việt..." else "Preparing $name voice..."
        TunerGuideVoiceStatus.MISSING_LANGUAGE ->
            if (vietnamese) "Điện thoại chưa có giọng đọc tiếng Việt. Hãy cài dữ liệu giọng nói trong Cài đặt → Chuyển văn bản thành giọng nói."
            else "$name voice is not installed or supported. Open Android Text-to-speech settings to check voice data."
        TunerGuideVoiceStatus.SUBTITLE_MISMATCH ->
            if (vietnamese) "Video này chưa có phụ đề tiếng Việt phù hợp để đọc. Bạn vẫn có thể xem video và phụ đề dự phòng."
            else "Matching $name subtitles are unavailable. The video and fallback captions still work."
        TunerGuideVoiceStatus.ERROR ->
            if (vietnamese) "Không thể khởi động dịch vụ đọc văn bản. Hãy kiểm tra cài đặt giọng nói Android."
            else "Text-to-speech could not start. Please check Android TTS settings."
        TunerGuideVoiceStatus.READY -> ""
    }
}

private fun tunerGuideSpeechRate(text: String, remainingMs: Long, code: String): Float {
    val words = text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }.coerceAtLeast(1)
    // Chinese/Japanese/Thai/Lao/Khmer don't reliably separate spoken words with spaces.
    val ideographicScript = tunerGuideLanguage(code) in setOf("zh", "ja", "th", "lo", "km", "ko")
    val letters = text.count { it.isLetterOrDigit() }
    val estimatedWords = if (ideographicScript) maxOf(words.toFloat(), letters / 2.5f) else words.toFloat()
    val seconds = (remainingMs / 1000f).coerceAtLeast(0.65f)
    return (estimatedWords / (3.6f * seconds)).coerceIn(0.95f, 1.55f)
}

private const val TUNER_GUIDE_HOST = "tunertools.top"
// This is the public address that returned api_version=2 and total=1 on the owner's IIS.
// The physical directory C:/inetpub/wwwroot/tunertools/app-guides/ must NOT be
// confused with https://tunertools.top/app-guides/ (HTTP 404 on this setup).
private const val TUNER_GUIDE_API_URL = "https://tunertools.top/tunertools/app-guides/api_guides.php"

private fun tunerGuideLanguage(raw: String): String {
    val lang = raw.lowercase(java.util.Locale.ROOT).substringBefore('-').substringBefore('_')
    return if (lang in setOf("vi", "en", "es", "fr", "de", "ru", "ja", "zh", "th", "lo", "km", "ko", "pt", "sw", "ar")) lang else "en"
}

private fun tunerGuideSafeMediaUrl(raw: String): String? {
    val url = runCatching { URL(raw) }.getOrNull() ?: return null
    if (url.protocol != "https" || !url.host.equals(TUNER_GUIDE_HOST, ignoreCase = true)) return null
    // PHP emits video/VTT links relative to its actual public app-guides folder.
    // Both /tunertools/app-guides/ and /app-guides/ are valid IIS mappings.
    val safePrefix = listOf("/tunertools/app-guides/", "/app-guides/")
    if (safePrefix.none { url.path.startsWith(it) }) return null
    if (url.userInfo != null || url.port !in listOf(-1, 443)) return null
    return url.toString()
}

private fun tunerGuideReadText(urlString: String, maxBytes: Int): String {
    val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 8500
        readTimeout = 10000
        instanceFollowRedirects = false
        setRequestProperty("Accept", "application/json, text/vtt, text/plain, */*")
        setRequestProperty("Accept-Charset", "utf-8")
    }
    try {
        if (connection.responseCode !in 200..299) {
            throw java.io.IOException("HTTP ${connection.responseCode}")
        }
        val reportedSize = connection.contentLength
        if (reportedSize > maxBytes) throw java.io.IOException("File exceeds size limit")
        return connection.inputStream.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            var size = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                size += count
                if (size > maxBytes) throw java.io.IOException("File exceeds size limit")
                output.write(buffer, 0, count)
            }
            output.toString("UTF-8")
        }
    } finally {
        connection.disconnect()
    }
}

private fun tunerGuideParseList(jsonText: String): List<TunerGuideVideo> {
    val guides = org.json.JSONObject(jsonText).optJSONArray("guides")
        ?: throw java.io.IOException("Unexpected guide API format")
    val items = ArrayList<TunerGuideVideo>()
    for (index in 0 until guides.length()) {
        val item = guides.optJSONObject(index) ?: continue
        val category = item.optString("category")
        if (category !in setOf("songs", "chords", "tuner", "metronome", "training", "documents", "settings")) continue
        val videoUrl = tunerGuideSafeMediaUrl(item.optString("video_url")) ?: continue
        val subtitleUrl = if (item.isNull("subtitle_url")) null
        else tunerGuideSafeMediaUrl(item.optString("subtitle_url"))
        val thumbnailUrl = if (item.isNull("thumbnail_url")) null
        else tunerGuideSafeMediaUrl(item.optString("thumbnail_url"))
        items.add(
            TunerGuideVideo(
                id = item.optString("id", "$category/$index"),
                category = category,
                title = item.optString("title", "Video ${index + 1}"),
                description = item.optString("description", ""),
                videoUrl = videoUrl,
                subtitleUrl = subtitleUrl,
                subtitleLanguage = if (item.isNull("subtitle_language")) null else item.optString("subtitle_language"),
                subtitleFallback = item.optBoolean("subtitle_fallback", false),
                thumbnailUrl = thumbnailUrl
            )
        )
    }
    return items
}

private suspend fun tunerGuideFetchCatalog(lang: String): List<TunerGuideVideo> =
    withContext(Dispatchers.IO) {
        val url = "$TUNER_GUIDE_API_URL?lang=${URLEncoder.encode(tunerGuideLanguage(lang), "UTF-8")}&category=all"
        try {
            val result = tunerGuideParseList(tunerGuideReadText(url, 2 * 1024 * 1024))
            android.util.Log.i("TunerGuide", "Catalog loaded: ${result.size} videos; source=$url")
            result
        } catch (cancel: kotlinx.coroutines.CancellationException) {
            throw cancel
        } catch (error: Exception) {
            android.util.Log.e("TunerGuide", "Cannot fetch tutorial catalog: $url", error)
            throw java.io.IOException("${error.message ?: "Network error"}\n$url", error)
        }
    }

private fun tunerGuideParseVttTime(raw: String): Long? {
    val parts = raw.trim().replace(',', '.').split(':')
    if (parts.size !in 2..3) return null
    val secs = parts.last().split('.')
    if (secs.size != 2 || secs[1].length !in 1..3) return null
    val sec = secs[0].toLongOrNull() ?: return null
    val milli = secs[1].padEnd(3, '0').toLongOrNull() ?: return null
    val min = parts[parts.size - 2].toLongOrNull() ?: return null
    val hour = if (parts.size == 3) parts[0].toLongOrNull() ?: return null else 0L
    if (min > 59 || sec > 59 || min < 0 || hour < 0) return null
    return ((hour * 60 + min) * 60 + sec) * 1000 + milli
}

private fun tunerGuideParseVtt(source: String): List<TunerVttCue> {
    val normalized = source.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n')
    if (!normalized.startsWith("WEBVTT")) throw java.io.IOException("Invalid WebVTT subtitle")
    val result = ArrayList<TunerVttCue>()
    for (block in normalized.split(Regex("\\n[ \\t]*\\n"))) {
        val lines = block.lines()
        val idx = lines.indexOfFirst { it.contains("-->") }
        if (idx < 0) continue // WEBVTT header, NOTE, STYLE, and REGION blocks
        val range = lines[idx].split("-->", limit = 2)
        if (range.size != 2) continue
        val start = tunerGuideParseVttTime(range[0]) ?: continue
        val end = tunerGuideParseVttTime(range[1].trim().substringBefore(' ')) ?: continue
        if (end <= start) continue
        val content = lines.drop(idx + 1).joinToString("\n").trim()
            .replace(Regex("<[^>]+>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
        if (content.isNotEmpty()) result.add(TunerVttCue(start, end, content))
    }
    return result.sortedBy { it.startMs }
}

private suspend fun tunerGuideFetchCaptions(url: String): List<TunerVttCue> =
    withContext(Dispatchers.IO) {
        val safe = tunerGuideSafeMediaUrl(url) ?: throw java.io.IOException("Untrusted subtitle URL")
        tunerGuideParseVtt(tunerGuideReadText(safe, 512 * 1024))
    }

private data class TunerGuideWords(
    val loading: String,
    val empty: String,
    val retry: String,
    val unavailable: String,
    val videoError: String,
    val videos: String
)

private fun tunerGuideWords(raw: String): TunerGuideWords = when (tunerGuideLanguage(raw)) {
    "vi" -> TunerGuideWords("Đang tải hướng dẫn...", "Chưa có video hướng dẫn trong mục này.", "Thử lại", "Chưa có phụ đề cho video này.", "Không thể phát video. Kiểm tra mạng hoặc đường dẫn MP4.", "video")
    "en" -> TunerGuideWords("Loading tutorials...", "No videos available in this category yet.", "Retry", "Subtitles are unavailable for this video.", "Cannot play video. Check your connection and MP4 URL.", "videos")
    "es" -> TunerGuideWords("Cargando guías...", "Aún no hay videos en esta categoría.", "Reintentar", "Subtítulos no disponibles.", "No se puede reproducir el video.", "videos")
    "fr" -> TunerGuideWords("Chargement des guides...", "Aucune vidéo dans cette catégorie.", "Réessayer", "Sous-titres indisponibles.", "Lecture vidéo impossible.", "vidéos")
    "de" -> TunerGuideWords("Anleitungen werden geladen...", "Noch keine Videos in dieser Kategorie.", "Erneut versuchen", "Untertitel nicht verfügbar.", "Video kann nicht abgespielt werden.", "Videos")
    "ru" -> TunerGuideWords("Загрузка инструкций...", "В этой категории пока нет видео.", "Повторить", "Субтитры недоступны.", "Не удалось воспроизвести видео.", "видео")
    "ja" -> TunerGuideWords("ガイドを読み込み中...", "このカテゴリにはまだ動画がありません。", "再試行", "字幕を利用できません。", "動画を再生できません。", "動画")
    "zh" -> TunerGuideWords("正在加载教程...", "此分类尚无教程视频。", "重试", "此视频暂无字幕。", "无法播放视频。", "视频")
    "th" -> TunerGuideWords("กำลังโหลดคู่มือ...", "หมวดนี้ยังไม่มีวิดีโอ", "ลองใหม่", "ไม่มีคำบรรยาย", "เล่นวิดีโอไม่ได้", "วิดีโอ")
    "lo" -> TunerGuideWords("ກຳລັງໂຫຼດຄູ່ມື...", "ຍັງບໍ່ມີວິດີໂອໃນໝວດນີ້", "ລອງໃໝ່", "ບໍ່ມີຄຳບັນຍາຍ", "ຫຼິ້ນວິດີໂອບໍ່ໄດ້", "ວິດີໂອ")
    "km" -> TunerGuideWords("កំពុងផ្ទុកសេចក្តីណែនាំ...", "មិនទាន់មានវីដេអូក្នុងប្រភេទនេះទេ។", "សាកល្បងម្តងទៀត", "មិនមានអក្សររត់។", "មិនអាចចាក់វីដេអូបាន។", "វីដេអូ")
    "ko" -> TunerGuideWords("사용 안내를 불러오는 중...", "이 분류에는 아직 동영상이 없습니다.", "다시 시도", "자막을 사용할 수 없습니다.", "동영상을 재생할 수 없습니다.", "동영상")
    "pt" -> TunerGuideWords("Carregando tutoriais...", "Ainda não há vídeos nesta categoria.", "Tentar novamente", "Legendas indisponíveis.", "Não é possível reproduzir o vídeo.", "vídeos")
    "sw" -> TunerGuideWords("Inapakia maelekezo...", "Bado hakuna video katika kundi hili.", "Jaribu tena", "Manukuu hayapatikani.", "Imeshindwa kucheza video.", "video")
    "ar" -> TunerGuideWords("جارٍ تحميل الدروس...", "لا توجد فيديوهات في هذا القسم بعد.", "إعادة المحاولة", "الترجمة النصية غير متوفرة.", "تعذّر تشغيل الفيديو.", "فيديو")
    else -> TunerGuideWords("Loading tutorials...", "No videos yet.", "Retry", "Subtitles unavailable.", "Cannot play video.", "videos")
}

private fun tunerGuideCategoryTitle(category: String, lang: String): String {
    val code = tunerGuideLanguage(lang)
    return when (category) {
        "songs" -> if (code == "en") "Songs" else CoreTranslator.getString("tab", code)
        "chords" -> CoreTranslator.getString("chorus", code)
        "tuner" -> CoreTranslator.getString("tune", code)
        "metronome" -> CoreTranslator.getString("tempo", code)
        "settings" -> CoreTranslator.getString("settings_menu", code)
        "training" -> when (code) {
            "vi" -> "Luyện tập"; "es" -> "Práctica"; "fr" -> "Entraînement"
            "de" -> "Übungen"; "ru" -> "Тренировка"; "ja" -> "練習"
            "zh" -> "练习"; "th" -> "ฝึกซ้อม"; "lo" -> "ຝຶກຊ້ອມ"
            "km" -> "ហ្វឹកហាត់"; "ko" -> "연습"; "pt" -> "Prática"
            "sw" -> "Mazoezi"; "ar" -> "التدريب"; else -> "Practice"
        }
        "documents" -> when (code) {
            "vi" -> "Tài liệu"; "es" -> "Documentos"; "fr" -> "Documents"
            "de" -> "Dokumente"; "ru" -> "Документы"; "ja" -> "資料"
            "zh" -> "文档"; "th" -> "เอกสาร"; "lo" -> "ເອກະສານ"
            "km" -> "ឯកសារ"; "ko" -> "자료"; "pt" -> "Documentos"
            "sw" -> "Nyaraka"; "ar" -> "المستندات"; else -> "Documents"
        }
        else -> category
    }
}

// Reuse the exact same icons as the application's bottom navigation menu.
// Documents is not a bottom navigation tab, so it retains its folder icon.
private fun tunerGuideCategoryIcon(category: String): ImageVector = when (category) {
    "songs" -> CustomIcons.MusicScore
    "chords" -> CustomIcons.Chord
    "tuner" -> CustomIcons.Tuner
    "metronome" -> CustomIcons.Metronome
    "training" -> Icons.Default.SportsEsports
    "documents" -> Icons.Default.FolderOpen
    else -> Icons.Default.Settings
}

@Composable
fun TunerVideoGuidesDialog(
    currentLang: String,
    isDarkMode: Boolean,
    tunaGreen: Color,
    onDismiss: () -> Unit
) {
    val foreground = if (isDarkMode) Color.White else Color(0xFF202124)
    val background = if (isDarkMode) Color.Black else Color(0xFFF5F5F7)
    val cardColor = if (isDarkMode) Color(0xFF11181C) else Color.White
    // Neutral monochrome icon surfaces that respect light/dark mode.
    val iconTileColor = if (isDarkMode) Color(0xFF252A2D) else Color(0xFFF0F1F2)
    val iconGlyphColor = if (isDarkMode) Color(0xFFE2E5E7) else Color(0xFF454C51)
    val secondary = foreground.copy(alpha = 0.61f)
    val dividerColor = foreground.copy(alpha = if (isDarkMode) 0.12f else 0.09f)
    val words = remember(currentLang) { tunerGuideWords(currentLang) }
    val groups = remember { listOf("songs", "chords", "tuner", "metronome", "training", "documents", "settings") }
    // Empty category = category selection screen. Opening a card shows only that
    // category's video list; the Back button returns here rather than closing.
    var activeCategory by rememberSaveable { mutableStateOf("") }
    var selectedVideo by remember { mutableStateOf<TunerGuideVideo?>(null) }
    var allGuides by remember { mutableStateOf<List<TunerGuideVideo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }

    val videoCounts = remember(allGuides) { allGuides.groupingBy { it.category }.eachCount() }
    val rootTitle = CoreTranslator.getString("guide", currentLang)

    fun goBack() {
        when {
            selectedVideo != null -> selectedVideo = null
            activeCategory.isNotBlank() -> activeCategory = ""
            else -> onDismiss()
        }
    }

    LaunchedEffect(currentLang, refresh) {
        isLoading = true
        loadError = null
        try {
            val updatedGuides = tunerGuideFetchCatalog(currentLang)
            allGuides = updatedGuides
            // Preserve the selected video's identity when its subtitle language changes.
            selectedVideo = selectedVideo?.let { playingGuide ->
                updatedGuides.firstOrNull { it.id == playingGuide.id }
            }
        } catch (cancel: kotlinx.coroutines.CancellationException) {
            throw cancel
        } catch (error: Exception) {
            loadError = error.message ?: "Network error"
            allGuides = emptyList()
        } finally {
            isLoading = false
        }
    }

    Dialog(
        onDismissRequest = { goBack() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = background) {
            val currentVideo = selectedVideo
            if (currentVideo != null) {
                TunerGuideVideoPlayer(
                    guide = currentVideo,
                    currentLang = currentLang,
                    isDarkMode = isDarkMode,
                    tunaGreen = tunaGreen,
                    onBack = { selectedVideo = null }
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 15.dp, vertical = 9.dp)
                ) {
                    // Shared header, with a true two-level navigation hierarchy.
                    Row(
                        modifier = Modifier.fillMaxWidth().height(49.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { goBack() }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = foreground)
                        }
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = if (activeCategory.isBlank()) rootTitle else tunerGuideCategoryTitle(activeCategory, currentLang),
                            color = foreground,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { refresh++ }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = words.retry, tint = foreground)
                        }
                    }

                    if (activeCategory.isBlank()) {
                        // Level one: seven full-width, vertically stacked feature cards.
                        Spacer(modifier = Modifier.height(9.dp))

                        if (isLoading) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(2.dp),
                                color = tunaGreen,
                                trackColor = tunaGreen.copy(alpha = 0.13f)
                            )
                            Spacer(modifier = Modifier.height(7.dp))
                        }
                        if (loadError != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = cardColor,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, dividerColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.WifiOff, contentDescription = null, tint = secondary, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(9.dp))
                                    Text(
                                        loadError.orEmpty(),
                                        color = secondary,
                                        fontSize = 10.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { refresh++ }) {
                                        Text(words.retry, color = tunaGreen, fontSize = 11.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(7.dp))
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            items(groups, key = { it }) { category ->
                                val count = videoCounts[category] ?: 0
                                Surface(
                                    onClick = { activeCategory = category },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = cardColor,
                                    shape = RoundedCornerShape(15.dp),
                                    border = BorderStroke(1.dp, dividerColor)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.size(45.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(iconTileColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = tunerGuideCategoryIcon(category),
                                                contentDescription = null,
                                                tint = iconGlyphColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(13.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                tunerGuideCategoryTitle(category, currentLang),
                                                color = foreground,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                "$count ${words.videos}",
                                                color = if (count > 0) tunaGreen else secondary,
                                                fontSize = 11.sp,
                                                maxLines = 1
                                            )
                                        }
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = secondary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Level two: video lessons belonging to the chosen feature.
                        val visibleGuides = allGuides.filter { it.category == activeCategory }
                        // Category name is already shown in the top app bar.
                        // Go straight to the video list; avoid repeating the icon/title row.
                        Spacer(modifier = Modifier.height(12.dp))
                        when {
                            isLoading -> Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = tunaGreen, modifier = Modifier.size(29.dp))
                                    Spacer(modifier = Modifier.height(11.dp))
                                    Text(words.loading, color = secondary, fontSize = 12.sp)
                                }
                            }
                            loadError != null -> Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 18.dp)
                                ) {
                                    Icon(Icons.Default.WifiOff, null, tint = secondary, modifier = Modifier.size(35.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        loadError.orEmpty(), color = secondary, fontSize = 12.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedButton(onClick = { refresh++ }) { Text(words.retry, color = foreground) }
                                }
                            }
                            visibleGuides.isEmpty() -> Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.PlayCircleOutline, null,
                                        tint = secondary.copy(alpha = 0.54f), modifier = Modifier.size(41.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        words.empty, color = secondary, fontSize = 12.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                            else -> LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(9.dp),
                                contentPadding = PaddingValues(bottom = 12.dp)
                            ) {
                                items(visibleGuides, key = { it.id }) { item ->
                                    Surface(
                                        onClick = { selectedVideo = item },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        color = cardColor,
                                        border = BorderStroke(1.dp, dividerColor)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier.size(width = 72.dp, height = 86.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFF20262B)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (item.thumbnailUrl != null) {
                                                    coil.compose.SubcomposeAsyncImage(
                                                        model = item.thumbnailUrl,
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop,
                                                        error = {
                                                            Icon(Icons.Default.PlayCircleOutline, null,
                                                                tint = Color.White, modifier = Modifier.size(31.dp))
                                                        }
                                                    )
                                                } else {
                                                    Icon(Icons.Default.PlayCircleOutline, null,
                                                        tint = Color.White, modifier = Modifier.size(31.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    item.title, color = foreground, fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold, maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (item.description.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(5.dp))
                                                    Text(
                                                        item.description, color = secondary, fontSize = 11.sp,
                                                        lineHeight = 15.sp, maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Subtitles, null,
                                                        tint = tunaGreen, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    Text(
                                                        item.subtitleLanguage?.uppercase(java.util.Locale.ROOT) ?: "—",
                                                        color = secondary, fontSize = 10.sp
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Icon(Icons.Default.ChevronRight, null,
                                                tint = secondary, modifier = Modifier.size(21.dp))
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
private fun TunerGuideVideoPlayer(
    guide: TunerGuideVideo,
    currentLang: String,
    isDarkMode: Boolean,
    tunaGreen: Color,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val foreground = if (isDarkMode) Color.White else Color(0xFF202124)
    val background = if (isDarkMode) Color.Black else Color(0xFFF5F5F7)
    val secondary = foreground.copy(alpha = 0.65f)
    val words = remember(currentLang) { tunerGuideWords(currentLang) }
    var ready by remember(guide.id) { mutableStateOf(false) }
    var buffering by remember(guide.id) { mutableStateOf(true) }
    var playing by remember(guide.id) { mutableStateOf(false) }
    var durationMs by remember(guide.id) { mutableIntStateOf(0) }
    var positionMs by remember(guide.id) { mutableIntStateOf(0) }
    var playerFailed by remember(guide.id) { mutableStateOf(false) }
    var cues by remember(guide.id, guide.subtitleUrl) { mutableStateOf<List<TunerVttCue>>(emptyList()) }
    var captionsEnabled by remember(guide.id) { mutableStateOf(true) }
    var captionFailed by remember(guide.id, guide.subtitleUrl) { mutableStateOf(false) }
    var captionsLoading by remember(guide.id, guide.subtitleUrl) { mutableStateOf(true) }
    var captionFailureDetail by remember(guide.id, guide.subtitleUrl) { mutableStateOf("") }
    var isScrubbing by remember(guide.id) { mutableStateOf(false) }
    var scrubFraction by remember(guide.id) { mutableFloatStateOf(0f) }

    // Voiceover uses the selected application's language (not device UI language,
    // unless the app is configured to follow the device). Keep subtitle visibility
    // and narration independent; never read fallback subtitles in the wrong voice.
    val narrationLanguage = tunerGuideLanguage(currentLang)
    val guideSubtitleLanguage = guide.subtitleLanguage?.let { tunerGuideLanguage(it) }
    val canNarrateLanguage = guide.subtitleUrl != null &&
            guideSubtitleLanguage == narrationLanguage && !guide.subtitleFallback
    var narrationEnabled by remember(guide.id, narrationLanguage) { mutableStateOf(false) }
    var voiceStatus by remember(guide.id, narrationLanguage, guide.subtitleUrl) {
        mutableStateOf(if (canNarrateLanguage) TunerGuideVoiceStatus.INITIALIZING
        else TunerGuideVoiceStatus.SUBTITLE_MISMATCH)
    }
    var narrationEngine by remember(guide.id, narrationLanguage) { mutableStateOf<TextToSpeech?>(null) }
    var narrationSeekGeneration by remember(guide.id, narrationLanguage) { mutableIntStateOf(0) }
    var narrationRetry by remember(guide.id, narrationLanguage) { mutableIntStateOf(0) }

    // A new TTS instance is required after switching language. The old instance
    // is stopped and shut down when the video, locale or retry generation changes.
    DisposableEffect(guide.id, narrationLanguage, canNarrateLanguage, narrationRetry) {
        var disposed = false
        var created: TextToSpeech? = null
        if (canNarrateLanguage) {
            voiceStatus = TunerGuideVoiceStatus.INITIALIZING
            created = runCatching {
                TextToSpeech(context.applicationContext) { status ->
                    Handler(Looper.getMainLooper()).post {
                        if (!disposed) {
                            val engine = created
                            if (status == TextToSpeech.SUCCESS && engine != null) {
                                val supported = runCatching {
                                    tunerGuideSelectVoice(engine, narrationLanguage)
                                }.getOrDefault(false)
                                voiceStatus = if (supported) {
                                    android.util.Log.i("TunerGuide", "TTS ready: $narrationLanguage, voice=${engine.voice?.name}")
                                    TunerGuideVoiceStatus.READY
                                } else {
                                    android.util.Log.w("TunerGuide", "No installed TTS voice: $narrationLanguage")
                                    TunerGuideVoiceStatus.MISSING_LANGUAGE
                                }
                            } else {
                                voiceStatus = TunerGuideVoiceStatus.ERROR
                            }
                        }
                    }
                }
            }.getOrNull()
            narrationEngine = created
            if (created == null) voiceStatus = TunerGuideVoiceStatus.ERROR
        } else {
            voiceStatus = TunerGuideVoiceStatus.SUBTITLE_MISMATCH
            narrationEnabled = false
            narrationEngine = null
        }
        onDispose {
            disposed = true
            runCatching { created?.stop() }
            runCatching { created?.shutdown() }
        }
    }

    val videoView = remember(guide.videoUrl) { android.widget.VideoView(context) }
    DisposableEffect(videoView, guide.videoUrl, lifecycle) {
        videoView.setOnPreparedListener { player ->
            ready = true
            buffering = false
            durationMs = player.duration.coerceAtLeast(0)
            // Avoid starting audio if the user has already backgrounded the app.
            if (lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                videoView.start()
                playing = true
            }
        }
        videoView.setOnCompletionListener {
            narrationEngine?.stop()
            playing = false
            buffering = false
            positionMs = durationMs
        }
        videoView.setOnErrorListener { _, what, extra ->
            narrationEngine?.stop()
            android.util.Log.w("TunerGuide", "VideoView failed: $what / $extra")
            playerFailed = true
            buffering = false
            playing = false
            true
        }
        videoView.setOnInfoListener { _, info, _ ->
            when (info) {
                android.media.MediaPlayer.MEDIA_INFO_BUFFERING_START -> buffering = true
                android.media.MediaPlayer.MEDIA_INFO_BUFFERING_END -> buffering = false
            }
            false
        }
        videoView.setVideoURI(Uri.parse(guide.videoUrl))
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_PAUSE) {
                narrationEngine?.stop()
                if (videoView.isPlaying) videoView.pause()
                playing = false
            }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose {
            narrationEngine?.stop()
            lifecycle.lifecycle.removeObserver(observer)
            videoView.stopPlayback()
        }
    }

    LaunchedEffect(videoView) {
        while (true) {
            if (ready && !playerFailed) {
                positionMs = runCatching { videoView.currentPosition }.getOrDefault(0).coerceAtLeast(0)
                if (durationMs <= 0) durationMs = runCatching { videoView.duration }.getOrDefault(0).coerceAtLeast(0)
                playing = runCatching { videoView.isPlaying }.getOrDefault(false)
            }
            kotlinx.coroutines.delay(175L)
        }
    }
    LaunchedEffect(guide.subtitleUrl) {
        captionFailed = false
        captionsLoading = true
        captionFailureDetail = ""
        cues = emptyList()
        try {
            val subtitleUrl = guide.subtitleUrl
            if (subtitleUrl == null) throw java.io.IOException("Missing subtitle URL from API")
            val loadedCues = tunerGuideFetchCaptions(subtitleUrl)
            if (loadedCues.isEmpty()) throw java.io.IOException("WebVTT has no timed cues")
            cues = loadedCues
            android.util.Log.i("TunerGuide", "Loaded ${loadedCues.size} caption cues from $subtitleUrl")
        } catch (cancel: kotlinx.coroutines.CancellationException) {
            throw cancel
        } catch (error: Exception) {
            captionFailed = true
            captionFailureDetail = error.message.orEmpty().take(140)
            android.util.Log.e("TunerGuide", "Cannot load guide subtitles: ${guide.subtitleUrl}", error)
        } finally {
            captionsLoading = false
        }
    }
    // Track the real playback clock, not the slider's visual position. Each cue
    // is spoken at most once during uninterrupted playback; a pause/seek resets it.
    LaunchedEffect(
        videoView, guide.id, narrationEngine, voiceStatus, narrationEnabled,
        cues, ready, playerFailed, buffering, isScrubbing, narrationSeekGeneration
    ) {
        val speaker = narrationEngine
        if (speaker == null || voiceStatus != TunerGuideVoiceStatus.READY ||
            !narrationEnabled || !canNarrateLanguage || cues.isEmpty() ||
            !ready || playerFailed || buffering || isScrubbing
        ) {
            speaker?.stop()
            return@LaunchedEffect
        }
        // Remote seekTo() may take a moment; do not speak the old cue in that gap.
        if (narrationSeekGeneration > 0) kotlinx.coroutines.delay(220L)
        var lastSpokenIndex = -1
        var lastPosition = -1L
        try {
            while (true) {
                val active = lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                        runCatching { videoView.isPlaying }.getOrDefault(false)
                if (!active) {
                    if (lastSpokenIndex >= 0) speaker.stop()
                    lastSpokenIndex = -1
                    lastPosition = -1L
                } else {
                    val now = runCatching { videoView.currentPosition.toLong() }
                        .getOrDefault(0L).coerceAtLeast(0L)
                    // Detect a seek even when the slider changes too quickly for a
                    // recomposition to observe the intermediate dragging state.
                    if (lastPosition >= 0L && (now < lastPosition - 450L || now > lastPosition + 1800L)) {
                        speaker.stop()
                        lastSpokenIndex = -1
                    }
                    val cueIndex = cues.indexOfFirst { now >= it.startMs && now < it.endMs }
                    if (cueIndex >= 0 && cueIndex != lastSpokenIndex) {
                        val cue = cues[cueIndex]
                        speaker.stop()
                        val remainingMs = cue.endMs - now
                        if (remainingMs >= 700L && cue.text.isNotBlank()) {
                            speaker.setSpeechRate(tunerGuideSpeechRate(cue.text, remainingMs, narrationLanguage))
                            val speakResult = speaker.speak(
                                cue.text,
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                "tuner-guide-${guide.id}-${cueIndex}-${narrationSeekGeneration}"
                            )
                            if (speakResult == TextToSpeech.ERROR) {
                                android.util.Log.w("TunerGuide", "TTS failed to speak cue $cueIndex")
                            }
                        }
                    }
                    lastSpokenIndex = cueIndex
                    lastPosition = now
                }
                kotlinx.coroutines.delay(120L)
            }
        } finally {
            speaker.stop()
        }
    }

    val currentCaption = if (!captionsEnabled) "" else
        cues.firstOrNull { positionMs.toLong() >= it.startMs && positionMs.toLong() < it.endMs }?.text.orEmpty()
    // Render captions in an Android TextView ABOVE the VideoView's SurfaceView.
    // A separate Compose overlay can end up behind the hardware video surface.
    val visibleCaption = when {
        !captionsEnabled -> ""
        captionsLoading -> if (tunerGuideLanguage(currentLang) == "vi") "Đang tải phụ đề..." else "Loading subtitles..."
        captionFailed -> words.unavailable
        else -> currentCaption
    }

    Column(modifier = Modifier.fillMaxSize().background(background).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = CoreTranslator.getString("close", currentLang), tint = foreground)
            }
            Spacer(modifier = Modifier.width(7.dp))
            Text(guide.title, color = foreground, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = { captionsEnabled = !captionsEnabled }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Subtitles, contentDescription = "Subtitles",
                    tint = if (captionsEnabled && !captionFailed) tunaGreen else secondary)
            }
            // Visible for all 15 languages. A missing voice gives a clear message
            // instead of silently playing a Vietnamese/English fallback voice.
            IconButton(
                onClick = {
                    if (voiceStatus == TunerGuideVoiceStatus.READY &&
                        cues.isNotEmpty() && !captionFailed && canNarrateLanguage
                    ) {
                        narrationEnabled = !narrationEnabled
                        if (!narrationEnabled) narrationEngine?.stop()
                    } else {
                        val hint = if (voiceStatus == TunerGuideVoiceStatus.READY) {
                            if (narrationLanguage == "vi") "Đang tải phụ đề để đọc."
                            else "Waiting for subtitle cues..."
                        } else {
                            tunerGuideVoiceHint(narrationLanguage, voiceStatus)
                        }
                        Toast.makeText(context, hint, Toast.LENGTH_LONG).show()
                        // Retry after installing a new system TTS voice without restarting the app.
                        if (voiceStatus == TunerGuideVoiceStatus.MISSING_LANGUAGE ||
                            voiceStatus == TunerGuideVoiceStatus.ERROR
                        ) narrationRetry++
                    }
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (narrationEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = if (narrationEnabled) {
                        if (narrationLanguage == "vi") "Tắt thuyết minh" else "Disable narration"
                    } else {
                        if (narrationLanguage == "vi") "Bật thuyết minh" else "Enable narration"
                    },
                    tint = if (narrationEnabled) tunaGreen else secondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        if (guide.description.isNotBlank()) {
            Text(guide.description, color = secondary, fontSize = 11.5.sp, lineHeight = 16.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            // A 9:16 frame fits the available width/height without BoxWithConstraints.
            // VideoView keeps the recording's actual aspect ratio with black letterboxing.
            Box(modifier = Modifier
                .aspectRatio(9f / 16f, matchHeightConstraintsFirst = true)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black),
                contentAlignment = Alignment.Center) {
                androidx.compose.ui.viewinterop.AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { viewContext ->
                        val density = viewContext.resources.displayMetrics.density
                        fun px(dp: Int): Int = (dp * density + 0.5f).toInt()
                        android.widget.FrameLayout(viewContext).apply {
                            setBackgroundColor(android.graphics.Color.BLACK)
                            clipChildren = false
                            // VideoView is a SurfaceView: keep its video surface below the caption layer.
                            videoView.setZOrderOnTop(false)
                            addView(videoView, android.widget.FrameLayout.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            ))
                            val captionView = android.widget.TextView(viewContext).apply {
                                tag = "tuner_guide_caption_overlay"
                                setTextColor(android.graphics.Color.WHITE)
                                textSize = 15f
                                typeface = android.graphics.Typeface.DEFAULT_BOLD
                                gravity = android.view.Gravity.CENTER
                                textDirection = android.view.View.TEXT_DIRECTION_FIRST_STRONG
                                maxLines = 4
                                setPadding(px(10), px(7), px(10), px(7))
                                // Use View.setBackground(Drawable) explicitly. The outer
                                // Compose `background` is an immutable Color and must not be reassigned.
                                setBackground(android.graphics.drawable.GradientDrawable().apply {
                                    setColor(android.graphics.Color.argb(224, 0, 0, 0))
                                    cornerRadius = px(9).toFloat()
                                })
                                visibility = android.view.View.GONE
                            }
                            addView(captionView, android.widget.FrameLayout.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                                android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
                            ).apply {
                                marginStart = px(9)
                                marginEnd = px(9)
                                bottomMargin = px(25)
                            })
                            captionView.bringToFront()
                        }
                    },
                    update = { root ->
                        root.findViewWithTag<android.widget.TextView>("tuner_guide_caption_overlay")?.apply {
                            if (text.toString() != visibleCaption) text = visibleCaption
                            visibility = if (visibleCaption.isBlank()) android.view.View.GONE else android.view.View.VISIBLE
                            bringToFront()
                        }
                    }
                )
                if (buffering && !playerFailed) {
                    CircularProgressIndicator(color = tunaGreen, modifier = Modifier.size(34.dp))
                }
                if (playerFailed) {
                    Text(words.videoError, color = Color.White, fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(18.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = {
                if (ready && !playerFailed) {
                    if (videoView.isPlaying) {
                        narrationEngine?.stop()
                        videoView.pause()
                    } else {
                        videoView.start()
                    }
                    playing = videoView.isPlaying
                }
            }, enabled = ready && !playerFailed, modifier = Modifier.size(46.dp)) {
                Icon(if (playing) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = if (ready) tunaGreen else secondary, modifier = Modifier.size(33.dp))
            }
            Slider(
                value = if (isScrubbing) scrubFraction else
                    if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
                onValueChange = {
                    if (!isScrubbing) narrationEngine?.stop()
                    isScrubbing = true
                    scrubFraction = it
                },
                onValueChangeFinished = {
                    narrationEngine?.stop()
                    narrationSeekGeneration++
                    if (durationMs > 0) {
                        val target = (durationMs * scrubFraction).toInt()
                        positionMs = target
                        videoView.seekTo(target)
                    }
                    isScrubbing = false
                },
                enabled = ready && durationMs > 0 && !playerFailed,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = tunaGreen, activeTrackColor = tunaGreen)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("${tunerGuideTime(positionMs)} / ${tunerGuideTime(durationMs)}",
                color = secondary, fontSize = 10.sp)
        }
        if (voiceStatus in setOf(
                TunerGuideVoiceStatus.MISSING_LANGUAGE,
                TunerGuideVoiceStatus.ERROR,
                TunerGuideVoiceStatus.SUBTITLE_MISMATCH
            )
        ) {
            Text(
                text = tunerGuideVoiceHint(narrationLanguage, voiceStatus),
                color = Color(0xFFFFB74D), fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 3.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        if (captionFailed || guide.subtitleFallback) {
            Text(
                text = if (captionFailed) listOf(words.unavailable, captionFailureDetail)
                    .filter { it.isNotBlank() }.joinToString(" · ")
                else "CC: ${guide.subtitleLanguage?.uppercase(java.util.Locale.ROOT) ?: "—"}",
                color = if (captionFailed) Color(0xFFFFB74D) else secondary,
                fontSize = 11.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            )
        }
    }
}

private fun tunerGuideTime(timeMs: Int): String {
    val seconds = timeMs.coerceAtLeast(0) / 1000
    return "%02d:%02d".format(java.util.Locale.ROOT, seconds / 60, seconds % 60)
}