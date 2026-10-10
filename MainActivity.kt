@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package duc_phi.music

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.DocumentsContract
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Folder
import android.media.RingtoneManager
import android.provider.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileInputStream
import java.io.OutputStream
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.ArrayDeque
import kotlin.math.max
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.widthIn

import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

// ==========================================
// 1. HỆ THỐNG MÀU SẮC (SÁNG / TỐI)
// ==========================================
internal data class AppColorPalette(
    val background: Color,
    val backgroundSoft: Color,
    val card: Color,
    val cardRaised: Color,
    val border: Color,
    val borderSoft: Color,
    val purple: Color,
    val purpleBright: Color,
    val blue: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val red: Color,
    val isDark: Boolean
)

internal val DarkPalette = AppColorPalette(
    background = Color(0xFF020812),
    backgroundSoft = Color(0xFF06111E),
    card = Color(0xFF071421),
    cardRaised = Color(0xFF0C1A2A),
    border = Color(0xFF1C2C3F),
    borderSoft = Color(0xFF132337),
    purple = Color(0xFF8D35FF),
    purpleBright = Color(0xFFA44CFF),
    blue = Color(0xFF2F8BFF),
    textPrimary = Color(0xFFF5F7FC),
    textSecondary = Color(0xFF9AA5B8),
    textMuted = Color(0xFF6F7D93),
    red = Color(0xFFFF4F57),
    isDark = true
)

internal val LightPalette = AppColorPalette(
    background = Color(0xFFF4F7FC),
    backgroundSoft = Color(0xFFE8EEF8),
    card = Color(0xFFFFFFFF),
    cardRaised = Color(0xFFF0F4FA),
    border = Color(0xFFD3DFEE),
    borderSoft = Color(0xFFE2EAF4),
    purple = Color(0xFF7929E8),
    purpleBright = Color(0xFF8D35FF),
    blue = Color(0xFF1E75E8),
    textPrimary = Color(0xFF111827),
    textSecondary = Color(0xFF4B5563),
    textMuted = Color(0xFF9CA3AF),
    red = Color(0xFFEF4444),
    isDark = false
)

internal val LocalPalette = compositionLocalOf { DarkPalette }

// ĐỐI TƯỢNG NÀY PHẢI ĐƯỢC GIỮ LẠI CHO EQUALIZER_PANEL HOẠT ĐỘNG
internal object PlayerPalette {
    val Background = Color(0xFF020812)
    val BackgroundSoft = Color(0xFF06111E)
    val Card = Color(0xFF071421)
    val CardRaised = Color(0xFF0C1A2A)
    val Border = Color(0xFF1C2C3F)
    val BorderSoft = Color(0xFF132337)
    val Purple = Color(0xFF8D35FF)
    val PurpleBright = Color(0xFFA44CFF)
    val Blue = Color(0xFF2F8BFF)
    val TextPrimary = Color(0xFFF5F7FC)
    val TextSecondary = Color(0xFF9AA5B8)
    val TextMuted = Color(0xFF6F7D93)
    val Red = Color(0xFFFF4F57)
}

// ==========================================
// 2. HỆ THỐNG 15 NGÔN NGỮ QUỐC TẾ VÀ CHUỖI DỊCH
// ==========================================
internal data class LanguageInfo(
    val code: String,
    val name: String,
    val nativeName: String
)

internal val SupportedLanguages = listOf(
    LanguageInfo("vi", "Tiếng Việt", "Tiếng Việt"),
    LanguageInfo("en", "English", "English"),
    LanguageInfo("zh", "Chinese", "简体中文"),
    LanguageInfo("ja", "Japanese", "日本語"),
    LanguageInfo("ko", "Korean", "한국어"),
    LanguageInfo("es", "Spanish", "Español"),
    LanguageInfo("fr", "French", "Français"),
    LanguageInfo("de", "German", "Deutsch"),
    LanguageInfo("ru", "Russian", "Русский"),
    LanguageInfo("pt", "Portuguese", "Português"),
    LanguageInfo("id", "Indonesian", "Bahasa Indonesia"),
    LanguageInfo("hi", "Hindi", "हिन्दी"),
    LanguageInfo("ar", "Arabic", "العربية"),
    LanguageInfo("th", "Thai", "ไทย"),
    LanguageInfo("it", "Italian", "Italiano")
)

internal class AppStrings(val lang: String) {
    fun searchPlaceholder() = when (lang) {
        "en" -> "Search online songs..."
        "zh" -> "在线搜索歌曲..."
        "ja" -> "オンラインで曲を検索..."
        "ko" -> "온라인 노래 검색..."
        "es" -> "Buscar canciones online..."
        "fr" -> "Rechercher des chansons en ligne..."
        "de" -> "Online-Lieder suchen..."
        "ru" -> "Поиск музыки онлайн..."
        "pt" -> "Pesquisar músicas online..."
        "id" -> "Cari lagu online..."
        "hi" -> "ऑनलाइन गाने खोजें..."
        "ar" -> "البحث عن الأغاني عبر الإنترنت..."
        "th" -> "ค้นหาเพลงออนไลน์..."
        "it" -> "Cerca canzoni online..."
        else -> "Tìm bài hát Online..."
    }

    fun settings() = when (lang) {
        "en" -> "Settings"
        "zh" -> "设置"
        "ja" -> "設定"
        "ko" -> "설정"
        "es" -> "Ajustes"
        "fr" -> "Paramètres"
        "de" -> "Einstellungen"
        "ru" -> "Настройки"
        "pt" -> "Configurações"
        "id" -> "Pengaturan"
        "hi" -> "सेटिंग्स"
        "ar" -> "الإعدادات"
        "th" -> "การตั้งค่า"
        "it" -> "Impostazioni"
        else -> "Cài đặt"
    }

    fun language() = when (lang) {
        "en" -> "Language"
        "zh" -> "语言"
        "ja" -> "言語"
        "ko" -> "언어"
        "es" -> "Idioma"
        "fr" -> "Langue"
        "de" -> "Sprache"
        "ru" -> "Язык"
        "pt" -> "Idioma"
        "id" -> "Bahasa"
        "hi" -> "भाषा"
        "ar" -> "اللغة"
        "th" -> "ภาษา"
        "it" -> "Lingua"
        else -> "Ngôn ngữ"
    }

    fun themeMode() = when (lang) {
        "en" -> "Theme (Dark/Light)"
        "zh" -> "深色/浅色模式"
        "ja" -> "ダーク/ライトモード"
        "ko" -> "다크/라이트 모드"
        "es" -> "Modo Oscuro/Claro"
        "fr" -> "Mode Sombre/Clair"
        "de" -> "Dunkel/Hell-Modus"
        "ru" -> "Темная/Светлая тема"
        "pt" -> "Modo Escuro/Claro"
        "id" -> "Mode Gelap/Terang"
        "hi" -> "डार्क/लाइट मोड"
        "ar" -> "الوضع الداكن/الفاتح"
        "th" -> "โหมดมืด/สว่าง"
        "it" -> "Modalità Scuro/Chiaro"
        else -> "Chế độ Sáng/Tối"
    }

    fun privacyPolicy() = when (lang) {
        "en" -> "Privacy Policy"
        "zh" -> "隐私政策"
        "ja" -> "プライバシーポリシー"
        "ko" -> "개인정보 처리방침"
        "es" -> "Política de privacidad"
        "fr" -> "Politique de confidentialité"
        "de" -> "Datenschutzrichtlinie"
        "ru" -> "Политика конфиденциальности"
        "pt" -> "Política de Privacidade"
        "id" -> "Kebijakan Privasi"
        "hi" -> "गोपनीयता नीति"
        "ar" -> "سياسة الخصوصية"
        "th" -> "นโยบายความเป็นส่วนตัว"
        "it" -> "Informativa sulla privacy"
        else -> "Chính sách bảo mật"
    }

    fun premiumUpgrade() = when (lang) {
        "en" -> "Upgrade to Premium"
        "zh" -> "升级至尊享版"
        "ja" -> "プレミアムにアップグレード"
        "ko" -> "프리미엄으로 업그레이드"
        "es" -> "Mejorar a Premium"
        "fr" -> "Passer à Premium"
        "de" -> "Auf Premium upgraden"
        "ru" -> "Перейти на Premium"
        "pt" -> "Atualizar para Premium"
        "id" -> "Tingkatkan ke Premium"
        "hi" -> "प्रीमियम में अपग्रेड करें"
        "ar" -> "الترقية إلى بريميوم"
        "th" -> "อัปเกรดเป็นพรีเมียม"
        "it" -> "Passa a Premium"
        else -> "Nâng cấp Premium"
    }

    fun selectAll() = when (lang) {
        "en" -> "All"
        "zh" -> "全选"
        "ja" -> "すべて"
        "ko" -> "전체"
        "es" -> "Todo"
        "fr" -> "Tout"
        "de" -> "Alle"
        "ru" -> "Все"
        "pt" -> "Tudo"
        "id" -> "Semua"
        "hi" -> "सभी"
        "ar" -> "الكل"
        "th" -> "ทั้งหมด"
        "it" -> "Tutto"
        else -> "Tất cả"
    }

    fun playCount(count: Int) = when (lang) {
        "en" -> "Play ($count)"
        "zh" -> "播放 ($count)"
        "ja" -> "再生 ($count)"
        "ko" -> "재생 ($count)"
        "es" -> "Reproducir ($count)"
        "fr" -> "Lire ($count)"
        "de" -> "Abspielen ($count)"
        "ru" -> "Слушать ($count)"
        "pt" -> "Tocar ($count)"
        "id" -> "Putar ($count)"
        "hi" -> "चलाएं ($count)"
        "ar" -> "تشغيل ($count)"
        "th" -> "เล่น ($count)"
        "it" -> "Riproduci ($count)"
        else -> "Phát ($count)"
    }

    fun cancel() = when (lang) {
        "en" -> "Cancel"
        "zh" -> "取消"
        "ja" -> "キャンセル"
        "ko" -> "취소"
        "es" -> "Cancelar"
        "fr" -> "Annuler"
        "de" -> "Abbrechen"
        "ru" -> "Отмена"
        "pt" -> "Cancelar"
        "id" -> "Batal"
        "hi" -> "रद्द करें"
        "ar" -> "إلغاء"
        "th" -> "ยกเลิก"
        "it" -> "Annulla"
        else -> "Hủy"
    }

    fun save() = when (lang) {
        "en" -> "Save"
        "zh" -> "保存"
        "ja" -> "保存"
        "ko" -> "저장"
        "es" -> "Guardar"
        "fr" -> "Enregistrer"
        "de" -> "Speichern"
        "ru" -> "Сохранить"
        "pt" -> "Salvar"
        "id" -> "Simpan"
        "hi" -> "सहेजें"
        "ar" -> "حفظ"
        "th" -> "บันทึก"
        "it" -> "Salva"
        else -> "Lưu"
    }

    fun download() = when (lang) {
        "en" -> "Download"
        "zh" -> "下载"
        "ja" -> "ダウンロード"
        "ko" -> "다운로드"
        "es" -> "Descargar"
        "fr" -> "Télécharger"
        "de" -> "Herunterladen"
        "ru" -> "Скачать"
        "pt" -> "Baixar"
        "id" -> "Unduh"
        "hi" -> "डाउनलोड"
        "ar" -> "تنزيل"
        "th" -> "ดาวน์โหลด"
        "it" -> "Scarica"
        else -> "Tải xuống"
    }

    fun editInfo() = when (lang) {
        "en" -> "Edit Info"
        "zh" -> "编辑信息"
        "ja" -> "情報を編集"
        "ko" -> "정보 수정"
        "es" -> "Editar información"
        "fr" -> "Modifier les infos"
        "de" -> "Info bearbeiten"
        "ru" -> "Изменить инфо"
        "pt" -> "Editar informações"
        "id" -> "Edit Info"
        "hi" -> "जानकारी संपादित करें"
        "ar" -> "تعديل المعلومات"
        "th" -> "แก้ไขข้อมูล"
        "it" -> "Modifica info"
        else -> "Sửa thông tin"
    }

    fun songTitle() = when (lang) {
        "en" -> "Song Title"
        "zh" -> "歌曲名称"
        "ja" -> "曲名"
        "ko" -> "노래 제목"
        "es" -> "Título de la canción"
        "fr" -> "Titre de la chanson"
        "de" -> "Songtitel"
        "ru" -> "Название песни"
        "pt" -> "Título da música"
        "id" -> "Judul Lagu"
        "hi" -> "गीत का शीर्षक"
        "ar" -> "عنوان الأغنية"
        "th" -> "ชื่อเพลง"
        "it" -> "Titolo del brano"
        else -> "Tên bài hát"
    }

    fun artist() = when (lang) {
        "en" -> "Artist"
        "zh" -> "艺术家"
        "ja" -> "アーティスト"
        "ko" -> "아티스트"
        "es" -> "Artista"
        "fr" -> "Artiste"
        "de" -> "Künstler"
        "ru" -> "Исполнитель"
        "pt" -> "Artista"
        "id" -> "Artis"
        "hi" -> "कलाकार"
        "ar" -> "الفنان"
        "th" -> "ศิลปิน"
        "it" -> "Artista"
        else -> "Nghệ sĩ"
    }

    fun cutRingtone() = when (lang) {
        "en" -> "Ringtone Cutter"
        "zh" -> "铃声裁剪"
        "ja" -> "着信音カッター"
        "ko" -> "벨소리 자르기"
        "es" -> "Cortador de tonos"
        "fr" -> "Coupeur de sonnerie"
        "de" -> "Klingelton-Cutter"
        "ru" -> "Обрезать рингтон"
        "pt" -> "Cortador de Toques"
        "id" -> "Pemotong Nada Dering"
        "hi" -> "रिंगटोन कटर"
        "ar" -> "قاطع النغمات"
        "th" -> "ตัดเสียงเรียกเข้า"
        "it" -> "Taglia suoneria"
        else -> "Cắt nhạc chuông"
    }

    fun saveCut() = when (lang) {
        "en" -> "Save Clip"
        "zh" -> "保存片段"
        "ja" -> "クリップを保存"
        "ko" -> "클립 저장"
        "es" -> "Guardar clip"
        "fr" -> "Enregistrer le clip"
        "de" -> "Clip speichern"
        "ru" -> "Сохранить отрывок"
        "pt" -> "Salvar clipe"
        "id" -> "Simpan Klip"
        "hi" -> "क्लिप सहेजें"
        "ar" -> "حفظ المقطع"
        "th" -> "บันทึกคลิป"
        "it" -> "Salva clip"
        else -> "Lưu đoạn cắt"
    }

    fun cutting() = when (lang) {
        "en" -> "Cutting..."
        "zh" -> "裁剪中..."
        "ja" -> "処理中..."
        "ko" -> "자르는 중..."
        "es" -> "Cortando..."
        "fr" -> "Découpage..."
        "de" -> "Schneiden..."
        "ru" -> "Обрезка..."
        "pt" -> "Cortando..."
        "id" -> "Memotong..."
        "hi" -> "काट रहा है..."
        "ar" -> "جارٍ القص..."
        "th" -> "กำลังตัด..."
        "it" -> "Taglio in corso..."
        else -> "Đang cắt..."
    }

    fun downloadTitle() = when (lang) {
        "en" -> "Download this song?"
        "zh" -> "下载这首歌？"
        "ja" -> "この曲をダウンロードしますか？"
        "ko" -> "이 노래를 다운로드하시겠습니까?"
        "es" -> "¿Descargar esta canción?"
        "fr" -> "Télécharger cette chanson ?"
        "de" -> "Dieses Lied herunterladen?"
        "ru" -> "Скачать эту песню?"
        "pt" -> "Baixar esta música?"
        "id" -> "Unduh lagu ini?"
        "hi" -> "क्या यह गीत डाउनलोड करें?"
        "ar" -> "تنزيل هذه الأغنية؟"
        "th" -> "ดาวน์โหลดเพลงนี้หรือไม่?"
        "it" -> "Scaricare questa canzone?"
        else -> "Tải bài hát này?"
    }

    fun downloadDesc(songTitle: String) = when (lang) {
        "en" -> "Do you want to download '$songTitle' for offline listening?"
        "zh" -> "您想下载 '$songTitle' 以供离线收听吗？"
        "ja" -> "オフラインで聴くために '$songTitle' をダウンロードしますか？"
        "ko" -> "오프라인 감상을 위해 '$songTitle'을(를) 다운로드하시겠습니까?"
        "es" -> "¿Quieres descargar '$songTitle' para escuchar sin conexión?"
        "fr" -> "Voulez-vous télécharger '$songTitle' pour une écoute hors ligne ?"
        "de" -> "Möchten Sie '$songTitle' zum Offline-Hören herunterladen?"
        "ru" -> "Хотите скачать '$songTitle' для прослушивания офлайн?"
        "pt" -> "Deseja baixar '$songTitle' para ouvir offline?"
        "id" -> "Apakah Anda ingin mengunduh '$songTitle' untuk mendengarkan offline?"
        "hi" -> "क्या आप ऑफ़लाइन सुनने के लिए '$songTitle' डाउनलोड करना चाहते हैं?"
        "ar" -> "هل تريد تنزيل '$songTitle' للاستماع دون اتصال؟"
        "th" -> "คุณต้องการดาวน์โหลด '$songTitle' เพื่อฟังแบบออฟไลน์หรือไม่?"
        "it" -> "Vuoi scaricare '$songTitle' per l'ascolto offline?"
        else -> "Bạn có muốn tải bài hát '$songTitle' về máy để nghe Offline không?"
    }

    fun cutFromTo(start: String, end: String) = when (lang) {
        "en" -> "From $start to $end"
        "zh" -> "从 $start 到 $end"
        "ja" -> "$start から $end まで"
        "ko" -> "$start 부터 $end 까지"
        "es" -> "De $start a $end"
        "fr" -> "De $start à $end"
        "de" -> "Von $start bis $end"
        "ru" -> "От $start до $end"
        "pt" -> "De $start a $end"
        "id" -> "Dari $start ke $end"
        "hi" -> "$start से $end तक"
        "ar" -> "من $start إلى $end"
        "th" -> "จาก $start ถึง $end"
        "it" -> "Da $start a $end"
        else -> "Lấy từ $start đến $end"
    }

    fun totalDuration(duration: String) = when (lang) {
        "en" -> "Total duration: $duration"
        "zh" -> "总时长：$duration"
        "ja" -> "合計時間：$duration"
        "ko" -> "총 시간: $duration"
        "es" -> "Duración total: $duration"
        "fr" -> "Durée totale : $duration"
        "de" -> "Gesamtdauer: $duration"
        "ru" -> "Общая длительность: $duration"
        "pt" -> "Duração total: $duration"
        "id" -> "Total durasi: $duration"
        "hi" -> "कुल अवधि: $duration"
        "ar" -> "المدة الإجمالية: $duration"
        "th" -> "ระยะเวลารวม: $duration"
        "it" -> "Durata totale: $duration"
        else -> "Tổng thời lượng: $duration"
    }

    fun savedAt() = when (lang) {
        "en" -> "Saved at:"
        "zh" -> "保存于："
        "ja" -> "保存先："
        "ko" -> "저장 위치:"
        "es" -> "Guardado en:"
        "fr" -> "Enregistré sous :"
        "de" -> "Gespeichert unter:"
        "ru" -> "Сохранено в:"
        "pt" -> "Salvo em:"
        "id" -> "Disimpan di:"
        "hi" -> "यहाँ सहेजा गया:"
        "ar" -> "تم الحفظ في:"
        "th" -> "บันทึกไว้ที่:"
        "it" -> "Salvato in:"
        else -> "Đã lưu tại:"
    }

    fun downloadSuccess() = when (lang) {
        "en" -> "Successfully downloaded to Music folder!"
        "zh" -> "成功下载到音乐文件夹！"
        "ja" -> "ミュージックフォルダに保存しました！"
        "ko" -> "Music 폴더에 성공적으로 저장되었습니다!"
        "es" -> "¡Descargado en la carpeta de Música!"
        "fr" -> "Téléchargé avec succès dans Musique !"
        "de" -> "Erfolgreich in den Musikordner heruntergeladen!"
        "ru" -> "Успешно скачано в папку Музыка!"
        "pt" -> "Baixado com sucesso para a pasta de Música!"
        "id" -> "Berhasil diunduh ke folder Musik!"
        "hi" -> "संगीत फ़ोल्डर में सफलतापूर्वक डाउनलोड किया गया!"
        "ar" -> "تم التنزيل بنجاح إلى مجلد الموسيقى!"
        "th" -> "ดาวน์โหลดลงในโฟลเดอร์เพลงสำเร็จ!"
        "it" -> "Scaricato con successo nella cartella Musica!"
        else -> "Đã tải thành công vào thư mục Music!"
    }

    fun notOnlineFile() = when (lang) {
        "en" -> "This song is not an online file."
        "zh" -> "这首歌不是在线文件。"
        "ja" -> "この曲はオンラインファイルではありません。"
        "ko" -> "이 노래는 온라인 파일이 아닙니다."
        "es" -> "Esta canción no es un archivo en línea."
        "fr" -> "Cette chanson n'est pas un fichier en ligne."
        "de" -> "Dieses Lied ist keine Online-Datei."
        "ru" -> "Эта песня не является онлайн-файлом."
        "pt" -> "Esta música não é um arquivo online."
        "id" -> "Lagu ini bukan file online."
        "hi" -> "यह गीत एक ऑनलाइन फ़ाइल नहीं है।"
        "ar" -> "هذه الأغنية ليست ملفًا عبر الإنترنت."
        "th" -> "เพลงนี้ไม่ใช่ไฟล์ออนไลน์"
        "it" -> "Questa canzone non è un file online."
        else -> "Bài hát này không phải là tệp trực tuyến."
    }

    fun downloadError() = when (lang) {
        "en" -> "Download error: "
        "zh" -> "下载错误: "
        "ja" -> "ダウンロードエラー: "
        "ko" -> "다운로드 오류: "
        "es" -> "Error de descarga: "
        "fr" -> "Erreur de téléchargement : "
        "de" -> "Download-Fehler: "
        "ru" -> "Ошибка скачивания: "
        "pt" -> "Erro ao baixar: "
        "id" -> "Kesalahan unduh: "
        "hi" -> "डाउनलोड त्रुटि: "
        "ar" -> "خطأ في التنزيل: "
        "th" -> "ข้อผิดพลาดในการดาวน์โหลด: "
        "it" -> "Errore di download: "
        else -> "Lỗi khi tải: "
    }

    fun saveSuccess() = when (lang) {
        "en" -> "Saved successfully!"
        "zh" -> "保存成功！"
        "ja" -> "正常に保存されました！"
        "ko" -> "성공적으로 저장되었습니다!"
        "es" -> "¡Guardado con éxito!"
        "fr" -> "Enregistré avec succès!"
        "de" -> "Erfolgreich gespeichert!"
        "ru" -> "Успешно сохранено!"
        "pt" -> "Salvo com sucesso!"
        "id" -> "Berhasil disimpan!"
        "hi" -> "सफलतापूर्वक सहेजा गया!"
        "ar" -> "تم الحفظ بنجاح!"
        "th" -> "บันทึกสำเร็จ!"
        "it" -> "Salvato con successo!"
        else -> "Đã lưu thành công!"
    }

    fun cutError() = when (lang) {
        "en" -> "Cut failed. Check storage permissions."
        "zh" -> "裁剪失败。请检查存储权限。"
        "ja" -> "カット失敗。ストレージの権限を確認してください。"
        "ko" -> "자르기 실패. 저장소 권한을 확인하세요."
        "es" -> "Error al cortar. Revisa los permisos."
        "fr" -> "Échec de la coupe. Vérifiez les autorisations."
        "de" -> "Schneiden fehlgeschlagen. Berechtigungen prüfen."
        "ru" -> "Ошибка обрезки. Проверьте разрешения."
        "pt" -> "Falha ao cortar. Verifique as permissões."
        "id" -> "Gagal memotong. Periksa izin penyimpanan."
        "hi" -> "कट विफल। स्टोरेज अनुमतियां जांचें।"
        "ar" -> "فشل القص. تحقق من أذونات التخزين."
        "th" -> "การตัดล้มเหลว ตรวจสอบสิทธิ์ที่เก็บข้อมูล"
        "it" -> "Taglio fallito. Controlla i permessi."
        else -> "Cắt nhạc thất bại. Vui lòng kiểm tra quyền lưu trữ."
    }

    fun voicePrompt() = when (lang) {
        "en" -> "Listening... What song do you want to find?"
        "zh" -> "倾听中... 您想找什么歌？"
        "ja" -> "聞いています... どの曲を探しますか？"
        "ko" -> "듣는 중... 어떤 노래를 찾으시나요?"
        "es" -> "Escuchando... ¿Qué canción quieres encontrar?"
        "fr" -> "Écoute... Quelle chanson voulez-vous trouver ?"
        "de" -> "Ich höre... Welches Lied suchst du?"
        "ru" -> "Слушаю... Какую песню вы ищете?"
        "pt" -> "Ouvindo... Que música você quer encontrar?"
        "id" -> "Mendengarkan... Lagu apa yang ingin Anda cari?"
        "hi" -> "सुन रहा हूँ... आप कौन सा गाना खोजना चाहते हैं?"
        "ar" -> "أستمع... ما الأغنية التي تريد العثور عليها؟"
        "th" -> "กำลังฟัง... คุณต้องการหาเพลงอะไร?"
        "it" -> "In ascolto... Che canzone vuoi trovare?"
        else -> "Đang nghe... Bạn muốn tìm bài gì?"
    }

    fun voiceNotSupported() = when (lang) {
        "en" -> "Device does not support voice recognition"
        "zh" -> "设备不支持语音识别"
        "ja" -> "デバイスが音声認識をサポートしていません"
        "ko" -> "기기가 음성 인식을 지원하지 않습니다"
        "es" -> "El dispositivo no soporta reconocimiento de voz"
        "fr" -> "L'appareil ne prend pas en charge la reconnaissance vocale"
        "de" -> "Gerät unterstützt keine Spracherkennung"
        "ru" -> "Устройство не поддерживает распознавание голоса"
        "pt" -> "O dispositivo não suporta reconhecimento de voz"
        "id" -> "Perangkat tidak mendukung pengenalan suara"
        "hi" -> "डिवाइस ध्वनि पहचान का समर्थन नहीं करता है"
        "ar" -> "الجهاز لا يدعم التعرف على الصوت"
        "th" -> "อุปกรณ์ไม่รองรับการจดจำเสียง"
        "it" -> "Il dispositivo non supporta il riconoscimento vocale"
        else -> "Thiết bị không hỗ trợ nhận diện giọng nói"
    }

    fun premiumComingSoon() = when (lang) {
        "en" -> "Thank you! Payment feature coming soon."
        "zh" -> "谢谢！支付功能即将推出。"
        "ja" -> "ありがとうございます！決済機能は近日公開予定です。"
        "ko" -> "감사합니다! 결제 기능이 곧 출시됩니다."
        "es" -> "¡Gracias! La función de pago estará disponible pronto."
        "fr" -> "Merci ! La fonction de paiement sera bientôt disponible."
        "de" -> "Danke! Zahlungsfunktion kommt bald."
        "ru" -> "Спасибо! Функция оплаты скоро появится."
        "pt" -> "Obrigado! Recurso de pagamento em breve."
        "id" -> "Terima kasih! Fitur pembayaran segera hadir."
        "hi" -> "धन्यवाद! भुगतान सुविधा जल्द ही आ रही है।"
        "ar" -> "شكراً لك! ميزة الدفع قريباً."
        "th" -> "ขอบคุณ! ระบบชำระเงินกำลังจะมาเร็วๆ นี้"
        "it" -> "Grazie! La funzione di pagamento arriverà presto."
        else -> "Cảm ơn bạn! Tính năng thanh toán sẽ sớm ra mắt."
    }

    fun voiceLocale() = when (lang) {
        "en" -> "en-US"; "zh" -> "zh-CN"; "ja" -> "ja-JP"; "ko" -> "ko-KR"; "es" -> "es-ES";
        "fr" -> "fr-FR"; "de" -> "de-DE"; "ru" -> "ru-RU"; "pt" -> "pt-BR"; "id" -> "id-ID";
        "hi" -> "hi-IN"; "ar" -> "ar-SA"; "th" -> "th-TH"; "it" -> "it-IT"; else -> "vi-VN"
    }

    fun gotIt() = when (lang) {
        "en" -> "Got it"
        "zh" -> "明白了"
        "ja" -> "了解"
        "ko" -> "확인"
        "es" -> "Entendido"
        "fr" -> "Compris"
        "de" -> "Verstanden"
        "ru" -> "Понятно"
        "pt" -> "Entendi"
        "id" -> "Mengerti"
        "hi" -> "समझ गया"
        "ar" -> "فهمت"
        "th" -> "เข้าใจแล้ว"
        "it" -> "Capito"
        else -> "Đã hiểu"
    }

    fun premiumDesc() = when (lang) {
        "en" -> "Unlock all premium features:\n• 10-band advanced equalizer\n• Unlimited 320kbps lossless downloads\n• Ad-free ultimate music experience"
        "zh" -> "解锁所有高级功能：\n• 10频段高级均衡器\n• 无限下载320kbps无损音乐\n• 无广告终极音乐体验"
        "ja" -> "すべてのプレミアム機能のロックを解除:\n• 10バンド高度なイコライザー\n• 320kbpsロスレスダウンロード無制限\n• 広告なしの究極の音楽体験"
        "ko" -> "모든 프리미엄 기능 잠금 해제:\n• 10밴드 고급 이퀄라이저\n• 320kbps 무손실 다운로드 무제한\n• 광고 없는 완벽한 음악 감상"
        "es" -> "Desbloquea funciones premium:\n• Ecualizador avanzado de 10 bandas\n• Descargas ilimitadas a 320kbps\n• Experiencia musical sin anuncios"
        "fr" -> "Débloquez les fonctionnalités premium :\n• Égaliseur avancé 10 bandes\n• Téléchargements illimités 320kbps\n• Expérience musicale sans publicité"
        "de" -> "Premium-Funktionen freischalten:\n• 10-Band-Equalizer\n• Unbegrenzte 320kbps Downloads\n• Werbefreies Musikerlebnis"
        "ru" -> "Разблокируйте премиум:\n• 10-полосный эквалайзер\n• Безлимитные загрузки 320kbps\n• Музыка без рекламы"
        "pt" -> "Desbloquear recursos premium:\n• Equalizador avançado de 10 bandas\n• Downloads ilimitados 320kbps\n• Experiência sem anúncios"
        "id" -> "Buka fitur premium:\n• Ekualiser 10-band tingkat lanjut\n• Unduhan lossless 320kbps tanpa batas\n• Pengalaman musik tanpa iklan"
        "hi" -> "प्रीमियम सुविधाएँ अनलॉक करें:\n• 10-बैंड उन्नत इक्वलाइज़र\n• असीमित 320kbps डाउनलोड\n• विज्ञापन-मुक्त संगीत अनुभव"
        "ar" -> "افتح الميزات المميزة:\n• معادل صوت متقدم بـ 10 نطاقات\n• تنزيلات غير محدودة 320 كيلوبايت/ثانية\n• تجربة موسيقى بدون إعلانات"
        "th" -> "ปลดล็อกฟีเจอร์พรีเมียม:\n• อีควอไลเซอร์ 10 แบนด์ขั้นสูง\n• ดาวน์โหลดไม่จำกัด 320kbps\n• ประสบการณ์ดนตรีไร้โฆษณา"
        "it" -> "Sblocca le funzioni premium:\n• Equalizzatore avanzato a 10 bande\n• Download illimitati a 320kbps\n• Esperienza senza pubblicità"
        else -> "Mở khóa toàn bộ tính năng cao cấp:\n• Equalizer chuyên sâu 10 băng tần\n• Tải nhạc Lossless 320kbps không giới hạn\n• Không quảng cáo trải nghiệm âm nhạc tối đa"
    }

    fun upgradeNow() = when (lang) {
        "en" -> "Upgrade Now"
        "zh" -> "立即升级"
        "ja" -> "今すぐアップグレード"
        "ko" -> "지금 업그레이드"
        "es" -> "Mejorar ahora"
        "fr" -> "Mettre à niveau"
        "de" -> "Jetzt upgraden"
        "ru" -> "Обновить сейчас"
        "pt" -> "Atualize agora"
        "id" -> "Tingkatkan Sekarang"
        "hi" -> "अभी अपग्रेड करें"
        "ar" -> "الترقية الآن"
        "th" -> "อัปเกรดทันที"
        "it" -> "Aggiorna ora"
        else -> "Nâng cấp ngay"
    }

    fun privacyText1() = when(lang) {
        "en" -> "1. Data Collection:\nThe app only requests storage access to scan existing audio files on your device for playback, ID3 tag editing, and ringtone cutting."
        "zh" -> "1. 数据收集:\n应用程序仅请求存储访问权限以扫描您设备上的现有音频文件，以便进行播放、ID3标签编辑和铃声裁剪。"
        "ja" -> "1. データ収集:\nアプリは、デバイス上のオーディオファイルをスキャンして再生、ID3タグの編集、着信音のカットを行うためにのみストレージアクセスを要求します。"
        "ko" -> "1. 데이터 수집:\n앱은 기기의 오디오 파일을 스캔하여 재생, ID3 태그 편집 및 벨소리 자르기를 위해서만 저장소 접근 권한을 요청합니다."
        "es" -> "1. Recolección de datos:\nLa aplicación solo solicita acceso para escanear archivos de audio para reproducción, edición y corte de tonos."
        "fr" -> "1. Collecte de données:\nL'application demande l'accès au stockage uniquement pour scanner les fichiers audio à des fins de lecture et d'édition."
        "de" -> "1. Datenerhebung:\nDie App fordert Speicherzugriff nur zum Scannen von Audiodateien für Wiedergabe und Bearbeitung an."
        "ru" -> "1. Сбор данных:\nПриложение запрашивает доступ к хранилищу только для сканирования аудиофайлов для воспроизведения и редактирования."
        "pt" -> "1. Coleta de Dados:\nO app solicita acesso ao armazenamento apenas para escanear arquivos de áudio para reprodução e edição."
        "id" -> "1. Pengumpulan Data:\nAplikasi hanya meminta akses penyimpanan untuk memindai file audio untuk pemutaran dan pengeditan."
        "hi" -> "1. डेटा संग्रह:\nऐप केवल प्लेबैक, संपादन और रिंगटोन काटने के लिए आपके डिवाइस पर ऑडियो फ़ाइलों को स्कैन करने के लिए स्टोरेज तक पहुंच का अनुरोध करता है।"
        "ar" -> "1. جمع البيانات:\nيطلب التطبيق الوصول إلى التخزين فقط لمسح الملفات الصوتية للتشغيل والتحرير وقطع النغمات."
        "th" -> "1. การรวบรวมข้อมูล:\nแอปขอสิทธิ์เข้าถึงที่เก็บข้อมูลเพื่อสแกนไฟล์เสียงสำหรับการเล่น การแก้ไข และการตัดเสียงเรียกเข้าเท่านั้น"
        "it" -> "1. Raccolta dati:\nL'app richiede l'accesso alla memoria solo per scansionare i file audio per la riproduzione e la modifica."
        else -> "1. Thu thập dữ liệu:\nỨng dụng chỉ yêu cầu quyền truy cập bộ nhớ nhằm mục đích quét tệp âm thanh có sẵn trên thiết bị của bạn để phát nhạc và hỗ trợ chỉnh sửa ID3 tag / cắt nhạc chuông."
    }

    fun privacyText2() = when(lang) {
        "en" -> "2. Privacy & Security:\nWe commit to NOT sending or storing any of your personal music files on external servers. All processing happens entirely locally on your device."
        "zh" -> "2. 隐私与安全:\n我们承诺不向外部服务器发送或存储您的任何个人音乐文件。所有处理均完全在您的设备上本地完成。"
        "ja" -> "2. プライバシーとセキュリティ:\n個人の音楽ファイルを外部サーバーに送信または保存しないことをお約束します。すべての処理はデバイス上で完全にローカルに行われます。"
        "ko" -> "2. 개인정보 및 보안:\n당사는 개인 음악 파일을 외부 서버로 전송하거나 저장하지 않을 것을 약속합니다. 모든 처리는 기기에서 로컬로 진행됩니다."
        "es" -> "2. Privacidad y Seguridad:\nNos comprometemos a NO enviar ni almacenar sus archivos de música en servidores externos."
        "fr" -> "2. Confidentialité et sécurité:\nNous nous engageons à NE PAS envoyer ni stocker vos fichiers musicaux sur des serveurs externes."
        "de" -> "2. Datenschutz & Sicherheit:\nWir senden oder speichern Ihre Musikdateien NICHT auf externen Servern."
        "ru" -> "2. Конфиденциальность и безопасность:\nМы НЕ отправляем и НЕ храним ваши музыкальные файлы на внешних серверах."
        "pt" -> "2. Privacidade e Segurança:\nNós nos comprometemos a NÃO enviar ou armazenar seus arquivos de música em servidores externos."
        "id" -> "2. Privasi & Keamanan:\nKami berkomitmen untuk TIDAK mengirim atau menyimpan file musik Anda di server eksternal."
        "hi" -> "2. गोपनीयता और सुरक्षा:\nहम आपकी कोई भी संगीत फ़ाइल बाहरी सर्वर पर नहीं भेजने या सहेजने के लिए प्रतिबद्ध हैं। सभी प्रोसेसिंग आपके डिवाइस पर स्थानीय रूप से होती है।"
        "ar" -> "2. الخصوصية والأمان:\nنحن نلتزم بعدم إرسال أو تخزين أي من ملفات الموسيقى الخاصة بك على خوادم خارجية."
        "th" -> "2. ความเป็นส่วนตัวและความปลอดภัย:\nเราขอสัญญาว่าจะไม่ส่งหรือจัดเก็บไฟล์เพลงส่วนตัวของคุณไปยังเซิร์ฟเวอร์ภายนอก การประมวลผลทั้งหมดเกิดขึ้นบนอุปกรณ์ของคุณเท่านั้น"
        "it" -> "2. Privacy e sicurezza:\nCi impegniamo a NON inviare o memorizzare i tuoi file musicali su server esterni."
        else -> "2. Quyền riêng tư & Bảo mật:\nChúng tôi cam kết KHÔNG gửi hoặc lưu trữ bất kỳ tệp nhạc cá nhân nào của bạn lên máy chủ bên ngoài. Mọi tác vụ xử lý đều diễn ra hoàn toàn cục bộ trên thiết bị của bạn."
    }

    fun mergeAudio() = when (lang) {
        "en" -> "Merge Audio"
        "zh" -> "合并音频"
        "ja" -> "オーディオを結合"
        "ko" -> "오디오 병합"
        "es" -> "Fusionar audio"
        "fr" -> "Fusionner l'audio"
        "de" -> "Audio zusammenführen"
        "ru" -> "Объединить аудио"
        "pt" -> "Mesclar Áudio"
        "id" -> "Gabungkan Audio"
        "hi" -> "ऑडियो मर्ज करें"
        "ar" -> "دمج الصوت"
        "th" -> "รวมเสียง"
        "it" -> "Unisci audio"
        else -> "Ghép nhạc"
    }

    fun merging() = when (lang) {
        "en" -> "Merging..."
        "zh" -> "合并中..."
        "ja" -> "結合中..."
        "ko" -> "병합 중..."
        "es" -> "Fusionando..."
        "fr" -> "Fusion..."
        "de" -> "Zusammenführen..."
        "ru" -> "Объединение..."
        "pt" -> "Mesclando..."
        "id" -> "Menggabungkan..."
        "hi" -> "मर्ज हो रहा है..."
        "ar" -> "جارٍ الدمج..."
        "th" -> "กำลังรวม..."
        "it" -> "Unione..."
        else -> "Đang ghép..."
    }

    fun selectToMerge() = when (lang) {
        "en" -> "Select 2nd song to merge"
        "zh" -> "选择要合并的第二首歌曲"
        "ja" -> "結合する2曲目を選択"
        "ko" -> "병합할 두 번째 노래 선택"
        "es" -> "Seleccionar 2da canción para fusionar"
        "fr" -> "Sélectionner la 2ème chanson"
        "de" -> "2. Lied zum Zusammenführen wählen"
        "ru" -> "Выберите 2-ю песню"
        "pt" -> "Selecione a 2ª música"
        "id" -> "Pilih lagu ke-2"
        "hi" -> "मर्ज करने के लिए दूसरा गाना चुनें"
        "ar" -> "حدد الأغنية الثانية للدمج"
        "th" -> "เลือกเพลงที่ 2 เพื่อรวม"
        "it" -> "Seleziona il secondo brano"
        else -> "Chọn bài hát thứ 2 để ghép cùng"
    }

    fun deleteSong() = when (lang) {
        "en" -> "Delete"
        "zh" -> "删除"
        "ja" -> "削除"
        "ko" -> "삭제"
        "es" -> "Eliminar"
        "fr" -> "Supprimer"
        "de" -> "Löschen"
        "ru" -> "Удалить"
        "pt" -> "Excluir"
        "id" -> "Hapus"
        "hi" -> "हटाएं"
        "ar" -> "حذف"
        "th" -> "ลบ"
        "it" -> "Elimina"
        else -> "Xóa bài hát"
    }

    fun deleteSuccess() = when (lang) {
        "en" -> "Deleted successfully!"
        "zh" -> "删除成功！"
        "ja" -> "正常に削除されました！"
        "ko" -> "성공적으로 삭제되었습니다!"
        "es" -> "¡Eliminado con éxito!"
        "fr" -> "Supprimé avec succès !"
        "de" -> "Erfolgreich gelöscht!"
        "ru" -> "Успешно удалено!"
        "pt" -> "Excluído com sucesso!"
        "id" -> "Berhasil dihapus!"
        "hi" -> "सफलतापूर्वक हटाया गया!"
        "ar" -> "تم الحذف بنجاح!"
        "th" -> "ลบสำเร็จ!"
        "it" -> "Eliminato con successo!"
        else -> "Đã xóa tệp vật lý!"
    }

    fun setAsRingtone() = when (lang) {
        "en" -> "Set as Ringtone"
        "zh" -> "设为铃声"
        "ja" -> "着信音に設定"
        "ko" -> "벨소리로 설정"
        "es" -> "Establecer como tono"
        "fr" -> "Définir comme sonnerie"
        "de" -> "Als Klingelton"
        "ru" -> "На звонок"
        "pt" -> "Definir como Toque"
        "id" -> "Jadikan Nada Dering"
        "hi" -> "रिंगटोन के रूप में सेट करें"
        "ar" -> "تعيين كنغمة رنين"
        "th" -> "ตั้งเป็นเสียงเรียกเข้า"
        "it" -> "Imposta come suoneria"
        else -> "Đặt làm nhạc chuông"
    }

    fun setRingtoneSuccess() = when (lang) {
        "en" -> "Ringtone set successfully!"
        "zh" -> "铃声设置成功！"
        "ja" -> "着信音が設定されました！"
        "ko" -> "벨소리가 설정되었습니다!"
        "es" -> "¡Tono configurado con éxito!"
        "fr" -> "Sonnerie définie avec succès !"
        "de" -> "Klingelton erfolgreich eingestellt!"
        "ru" -> "Рингтон успешно установлен!"
        "pt" -> "Toque configurado com sucesso!"
        "id" -> "Nada dering berhasil diatur!"
        "hi" -> "रिंगटोन सफलतापूर्वक सेट की गई!"
        "ar" -> "تم تعيين نغمة الرنين بنجاح!"
        "th" -> "ตั้งเสียงเรียกเข้าสำเร็จ!"
        "it" -> "Suoneria impostata con successo!"
        else -> "Cài nhạc chuông thành công!"
    }

    fun requireWriteSettings() = when (lang) {
        "en" -> "Please grant permission to modify system settings."
        "zh" -> "请授予修改系统设置的权限。"
        "ja" -> "システム設定を変更する権限を付与してください。"
        "ko" -> "시스템 설정 수정 권한을 허용해주세요."
        "es" -> "Por favor, concede permiso para modificar los ajustes."
        "fr" -> "Veuillez accorder l'autorisation de modifier les paramètres."
        "de" -> "Bitte berechtigen, Systemeinstellungen zu ändern."
        "ru" -> "Пожалуйста, предоставьте разрешение на изменение настроек."
        "pt" -> "Por favor, conceda permissão para modificar as configurações."
        "id" -> "Tolong berikan izin untuk mengubah pengaturan sistem."
        "hi" -> "कृपया सिस्टम सेटिंग्स को संशोधित करने की अनुमति दें।"
        "ar" -> "يرجى منح الإذن لتعديل إعدادات النظام."
        "th" -> "โปรดให้สิทธิ์ในการแก้ไขการตั้งค่าระบบ"
        "it" -> "Si prega di concedere il permesso per modificare le impostazioni."
        else -> "Vui lòng cấp quyền sửa đổi hệ thống để cài nhạc chuông."
    }

    fun selectRingtoneType() = when (lang) {
        "en" -> "Select Ringtone Type"
        "zh" -> "选择铃声类型"
        "ja" -> "着信音の種類を選択"
        "ko" -> "벨소리 유형 선택"
        "es" -> "Seleccionar tipo de tono"
        "fr" -> "Sélectionner le type de sonnerie"
        "de" -> "Klingelton-Typ auswählen"
        "ru" -> "Выберите тип рингтона"
        "pt" -> "Selecione o tipo de toque"
        "id" -> "Pilih Jenis Nada Dering"
        "hi" -> "रिंगटोन का प्रकार चुनें"
        "ar" -> "حدد نوع نغمة الرنين"
        "th" -> "เลือกประเภทเสียงเรียกเข้า"
        "it" -> "Seleziona tipo di suoneria"
        else -> "Chọn loại nhạc chuông"
    }

    fun phoneRingtone() = when (lang) {
        "en" -> "Phone Ringtone"
        "zh" -> "电话铃声"
        "ja" -> "電話の着信音"
        "ko" -> "전화 벨소리"
        "es" -> "Tono de llamada"
        "fr" -> "Sonnerie de téléphone"
        "de" -> "Telefon-Klingelton"
        "ru" -> "Мелодия звонка"
        "pt" -> "Toque de Chamada"
        "id" -> "Nada Dering Panggilan"
        "hi" -> "फ़ोन रिंगटोन"
        "ar" -> "نغمة رنين الهاتف"
        "th" -> "เสียงเรียกเข้าโทรศัพท์"
        "it" -> "Suoneria del telefono"
        else -> "Nhạc chuông cuộc gọi"
    }

    fun notificationRingtone() = when (lang) {
        "en" -> "Notification Ringtone"
        "zh" -> "通知铃声"
        "ja" -> "通知音"
        "ko" -> "알림 벨소리"
        "es" -> "Tono de notificación"
        "fr" -> "Sonnerie de notification"
        "de" -> "Benachrichtigungston"
        "ru" -> "Мелодия уведомления"
        "pt" -> "Toque de Notificação"
        "id" -> "Nada Dering Notifikasi"
        "hi" -> "अधिसूचना रिंगटोन"
        "ar" -> "نغمة الإشعارات"
        "th" -> "เสียงแจ้งเตือน"
        "it" -> "Suoneria di notifica"
        else -> "Nhạc chuông tin nhắn"
    }

    fun restoreDefaultSuccess() = when (lang) {
        "en" -> "Restored to default ringtone!"
        "zh" -> "已恢复默认铃声！"
        "ja" -> "デフォルトの着信音に復元しました！"
        "ko" -> "기본 벨소리로 복원되었습니다!"
        "es" -> "¡Restaurado al tono por defecto!"
        "fr" -> "Restauré à la sonnerie par défaut !"
        "de" -> "Auf Standard-Klingelton zurückgesetzt!"
        "ru" -> "Восстановлен стандартный рингтон!"
        "pt" -> "Restaurado para o toque padrão!"
        "id" -> "Dikembalikan ke nada dering default!"
        "hi" -> "डिफ़ॉल्ट रिंगटोन पर बहाल किया गया!"
        "ar" -> "تمت استعادة نغمة الرنين الافتراضية!"
        "th" -> "คืนค่าเป็นเสียงเรียกเข้าเริ่มต้นแล้ว!"
        "it" -> "Ripristinato alla suoneria predefinita!"
        else -> "Đã khôi phục nhạc chuông gốc!"
    }

    fun googleDriveFolder() = when (lang) {
        "vi" -> "Thư mục Google Drive"
        else -> "Google Drive folder"
    }

    fun chooseGoogleDriveFolder() = when (lang) {
        "vi" -> "Chọn trên Drive"
        else -> "Choose from Drive"
    }

    fun driveFolderPath() = when (lang) {
        "vi" -> "Đường dẫn thư mục đã chọn"
        else -> "Selected folder path"
    }

    fun driveFolderHint() = when (lang) {
        "vi" -> "Chưa chọn thư mục Google Drive"
        else -> "No Google Drive folder selected"
    }

    fun scanningDrive() = when (lang) {
        "vi" -> "Đang quét Google Drive..."
        else -> "Scanning Google Drive..."
    }

    fun noAudioFolders() = when (lang) {
        "vi" -> "Chưa tìm thấy thư mục âm thanh. Bạn có thể chọn một thư mục trên Google Drive ở phía trên."
        else -> "No audio folders found. You can choose a Google Drive folder above."
    }

    fun removeFromList() = when (lang) {
        "vi" -> "Xóa khỏi danh sách"
        else -> "Remove from list"
    }

    fun removeFromListSuccess() = when (lang) {
        "vi" -> "Đã xóa khỏi danh sách phát"
        else -> "Removed from playlist"
    }

    fun selectFolder() = when (lang) {
        "en" -> "Select Folder"
        "zh" -> "选择文件夹"
        "ja" -> "フォルダを選択"
        "ko" -> "폴더 선택"
        "es" -> "Seleccionar carpeta"
        "fr" -> "Sélectionner un dossier"
        "de" -> "Ordner auswählen"
        "ru" -> "Выбрать папку"
        "pt" -> "Selecionar pasta"
        "id" -> "Pilih folder"
        "hi" -> "फ़ोल्डर चुनें"
        "ar" -> "حدد المجلد"
        "th" -> "เลือกโฟลเดอร์"
        "it" -> "Seleziona cartella"
        else -> "Chọn thư mục"
    }

    fun mediaFiles(count: Int) = when (lang) {
        "en" -> "$count media files"
        "zh" -> "$count 个媒体文件"
        "ja" -> "$count メディアファイル"
        "ko" -> "$count 개의 미디어 파일"
        "es" -> "$count archivos multimedia"
        "fr" -> "$count fichiers multimédias"
        "de" -> "$count Mediendateien"
        "ru" -> "$count медиафайлов"
        "pt" -> "$count arquivos de mídia"
        "id" -> "$count file media"
        "hi" -> "$count मीडिया फ़ाइलें"
        "ar" -> "$count ملفات وسائط"
        "th" -> "$count ไฟล์มีเดีย"
        "it" -> "$count file multimediali"
        else -> "$count tệp phương tiện"
    }

    fun logout(email: String) = when (lang) {
        "en" -> "Logout ($email)"
        "zh" -> "登出 ($email)"
        "ja" -> "ログアウト ($email)"
        "ko" -> "로그아웃 ($email)"
        "es" -> "Cerrar sesión ($email)"
        "fr" -> "Se déconnecter ($email)"
        "de" -> "Abmelden ($email)"
        "ru" -> "Выйти ($email)"
        "pt" -> "Sair ($email)"
        "id" -> "Keluar ($email)"
        "hi" -> "लॉग आउट ($email)"
        "ar" -> "تسجيل الخروج ($email)"
        "th" -> "ออกจากระบบ ($email)"
        "it" -> "Disconnetti ($email)"
        else -> "Đăng xuất ($email)"
    }

    fun login() = when (lang) {
        "en" -> "Login"
        "zh" -> "登录"
        "ja" -> "ログイン"
        "ko" -> "로그인"
        "es" -> "Iniciar sesión"
        "fr" -> "Connexion"
        "de" -> "Anmelden"
        "ru" -> "Войти"
        "pt" -> "Entrar"
        "id" -> "Masuk"
        "hi" -> "लॉगिन करें"
        "ar" -> "تسجيل الدخول"
        "th" -> "เข้าสู่ระบบ"
        "it" -> "Accedi"
        else -> "Đăng nhập"
    }

    fun admin() = when (lang) {
        "en" -> "Admin Dashboard"
        "zh" -> "管理面板"
        "ja" -> "管理ダッシュボード"
        "ko" -> "관리자 대시보드"
        "es" -> "Panel de administración"
        "fr" -> "Tableau de bord d'administration"
        "de" -> "Admin-Dashboard"
        "ru" -> "Панель администратора"
        "pt" -> "Painel de Administração"
        "id" -> "Dasbor Admin"
        "hi" -> "एडमिन डैशबोर्ड"
        "ar" -> "لوحة تحكم المسؤول"
        "th" -> "แดชบอร์ดผู้ดูแลระบบ"
        "it" -> "Pannello di amministrazione"
        else -> "Quản lý Hỗ trợ (Admin)"
    }
}

internal val LocalStrings = compositionLocalOf { AppStrings("vi") }

internal enum class SongSource {
    LOCAL_MEDIASTORE,
    SAF_CLOUD,
    ONLINE_HTTP,
    YOUTUBE,
    APP_RESOURCE
}

internal data class SongItem(
    val id: String,
    val title: String,
    val artist: String,
    val uri: Uri,
    val durationLabel: String,
    val artworkRes: Int = R.drawable.album_cover,
    val artworkUrl: String? = null,
    val source: SongSource = SongSource.LOCAL_MEDIASTORE,
    val mimeType: String? = null
)

private val SUPPORTED_AUDIO_EXTENSIONS = setOf(
    "mp3", "m4a", "m4b", "aac", "adts", "flac", "wav", "wave",
    "ogg", "oga", "opus", "amr", "3gp", "mp4", "webm", "mka",
    "mkv", "ac3", "ec3", "ac4", "ts", "mpeg", "mpg"
)

private fun extensionOf(fileName: String): String =
    fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)

private fun removeSupportedMediaExtension(fileName: String): String {
    val extension = extensionOf(fileName)
    return if (extension in SUPPORTED_AUDIO_EXTENSIONS) {
        fileName.substringBeforeLast('.').trim()
    } else {
        fileName.trim()
    }
}

private fun isSupportedAudioFile(displayName: String, mimeType: String?): Boolean {
    val normalizedMime = mimeType.orEmpty().lowercase(Locale.ROOT)
    if (normalizedMime.startsWith("audio/")) return true

    // Một số tệp âm thanh được đóng trong container video như MP4/WebM/MKV.
    // Khi nhà cung cấp trả MIME chưa chính xác, phần mở rộng vẫn được dùng để nhận diện.
    return extensionOf(displayName) in SUPPORTED_AUDIO_EXTENSIONS
}

private fun parseTitleAndArtist(displayName: String, fallbackArtist: String): Pair<String, String> {
    val baseName = removeSupportedMediaExtension(displayName).ifBlank { "Bài hát không tên" }
    return if (baseName.contains(" - ")) {
        val parts = baseName.split(" - ", limit = 2)
        parts[0].trim().ifBlank { "Bài hát không tên" } to
                parts[1].trim().ifBlank { fallbackArtist }
    } else {
        baseName to fallbackArtist
    }
}

private fun inferSongSource(id: String, uri: Uri): SongSource {
    val uriText = uri.toString()
    return when {
        id.startsWith("yt_") || id.startsWith("vid_") || uriText.contains("youtu", ignoreCase = true) -> SongSource.YOUTUBE
        uriText.startsWith("http://") || uriText.startsWith("https://") -> SongSource.ONLINE_HTTP
        uriText.startsWith("android.resource://") -> SongSource.APP_RESOURCE
        uriText.startsWith("content://") && uriText.contains("document", ignoreCase = true) -> SongSource.SAF_CLOUD
        else -> SongSource.LOCAL_MEDIASTORE
    }
}

private data class DocumentTreeAudioScanResult(
    val rootName: String,
    val folders: Map<String, List<SongItem>>,
    val totalFiles: Int
)

private data class PendingDocumentFolder(
    val documentId: String,
    val relativePath: String
)

private fun queryDocumentDisplayName(context: Context, documentUri: Uri): String? {
    return try {
        context.contentResolver.query(
            documentUri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    } catch (_: Exception) {
        null
    }
}

private fun scanDocumentTreeAudio(context: Context, treeUri: Uri): DocumentTreeAudioScanResult {
    val resolver = context.contentResolver
    val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
    val rootDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocumentId)
    val rootName = queryDocumentDisplayName(context, rootDocumentUri)
        ?.takeIf { it.isNotBlank() }
        ?: "Google Drive"

    val result = linkedMapOf<String, MutableList<SongItem>>()
    val pendingFolders = ArrayDeque<PendingDocumentFolder>()
    val visitedFolderIds = hashSetOf<String>()
    pendingFolders.add(PendingDocumentFolder(rootDocumentId, rootName))

    val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE
    )

    var totalFiles = 0

    while (pendingFolders.isNotEmpty()) {
        val current = pendingFolders.removeFirst()
        if (!visitedFolderIds.add(current.documentId)) continue

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            current.documentId
        )

        resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val documentIdColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val displayNameColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeTypeColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)

            while (cursor.moveToNext()) {
                if (documentIdColumn < 0) continue

                val documentId = cursor.getString(documentIdColumn) ?: continue
                val displayName = if (displayNameColumn >= 0) {
                    cursor.getString(displayNameColumn).orEmpty()
                } else {
                    ""
                }
                val mimeType = if (mimeTypeColumn >= 0) {
                    cursor.getString(mimeTypeColumn)
                } else {
                    null
                }

                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    val childFolderName = displayName.ifBlank { "Thư mục" }
                    pendingFolders.add(
                        PendingDocumentFolder(
                            documentId = documentId,
                            relativePath = "${current.relativePath}/$childFolderName"
                        )
                    )
                    continue
                }

                if (!isSupportedAudioFile(displayName, mimeType)) continue

                val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                val (title, artist) = parseTitleAndArtist(displayName, "Google Drive")
                val folderKey = "Google Drive / ${current.relativePath}"
                val normalizedMime = mimeType?.takeIf {
                    it.startsWith("audio/", ignoreCase = true) ||
                            it.startsWith("video/", ignoreCase = true)
                }

                result.getOrPut(folderKey) { mutableListOf() }.add(
                    SongItem(
                        id = documentUri.toString(),
                        title = title,
                        artist = artist,
                        uri = documentUri,
                        durationLabel = "--:--",
                        artworkRes = R.drawable.album_cover,
                        artworkUrl = null,
                        source = SongSource.SAF_CLOUD,
                        mimeType = normalizedMime
                    )
                )
                totalFiles++
            }
        }
    }

    val individualFolders = result.mapValues { (_, songs) -> songs.toList() }
    val allSongs = individualFolders.values
        .flatten()
        .distinctBy { it.id }

    val outputFolders = linkedMapOf<String, List<SongItem>>()
    if (allSongs.isNotEmpty()) {
        outputFolders["Google Drive / $rootName (Tất cả)"] = allSongs
    }
    outputFolders.putAll(individualFolders)

    return DocumentTreeAudioScanResult(
        rootName = rootName,
        folders = outputFolders,
        totalFiles = totalFiles
    )
}

private fun scanLocalAudioFolders(context: Context): Map<String, List<SongItem>> {
    val resolver = context.contentResolver
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

    val projection = mutableListOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.MIME_TYPE
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(MediaStore.Audio.Media.RELATIVE_PATH)
        } else {
            @Suppress("DEPRECATION")
            add(MediaStore.Audio.Media.DATA)
        }
    }.toTypedArray()

    val foldersMap = linkedMapOf<String, MutableList<SongItem>>()

    resolver.query(
        collection,
        projection,
        null,
        null,
        "${MediaStore.Audio.Media.DATE_ADDED} DESC"
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
        val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
        val durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
        val displayNameColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
        val mimeTypeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
        val pathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
        } else {
            @Suppress("DEPRECATION")
            cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
        }

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val duration = if (durationColumn >= 0) cursor.getLong(durationColumn) else 0L
            val displayName = if (displayNameColumn >= 0) cursor.getString(displayNameColumn).orEmpty() else ""
            val mimeType = if (mimeTypeColumn >= 0) cursor.getString(mimeTypeColumn) else null

            if (!isSupportedAudioFile(displayName, mimeType)) continue

            val folderName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val relativePath = if (pathColumn >= 0) cursor.getString(pathColumn).orEmpty() else ""
                relativePath.trim('/').ifBlank { "Nhạc trên máy" }
            } else {
                @Suppress("DEPRECATION")
                val absolutePath = if (pathColumn >= 0) cursor.getString(pathColumn).orEmpty() else ""
                File(absolutePath).parentFile?.name ?: "Nhạc trên máy"
            }

            val rawDatabaseTitle = if (titleColumn >= 0) cursor.getString(titleColumn) else null
            val rawDatabaseArtist = if (artistColumn >= 0) cursor.getString(artistColumn) else null
            val parsed = parseTitleAndArtist(
                rawDatabaseTitle?.takeIf { it.isNotBlank() } ?: displayName,
                rawDatabaseArtist?.takeIf { it.isNotBlank() && it != "<unknown>" }
                    ?: "Nghệ sĩ chưa biết"
            )

            val contentUri = ContentUris.withAppendedId(collection, id)
            val songId = contentUri.toString()
            val customTitle = getCustomTitle(context, songId, parsed.first)
            val customArtist = getCustomArtist(context, songId, parsed.second)

            foldersMap.getOrPut(folderName) { mutableListOf() }.add(
                SongItem(
                    id = songId,
                    title = customTitle,
                    artist = customArtist,
                    uri = contentUri,
                    durationLabel = if (duration > 0L) formatMillis(duration) else "--:--",
                    artworkRes = R.drawable.album_cover,
                    artworkUrl = null,
                    source = SongSource.LOCAL_MEDIASTORE,
                    mimeType = mimeType
                )
            )
        }
    }

    return foldersMap.mapValues { (_, songs) -> songs.toList() }
}

fun getAndLogSHA1(context: android.content.Context) {
    try {
        val pm = context.packageManager
        val packageName = context.packageName
        val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            pm.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES).signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNATURES).signatures
        }

        signatures?.forEach { signature ->
            val md = java.security.MessageDigest.getInstance("SHA-1")
            md.update(signature.toByteArray())
            val sha1 = md.digest().joinToString(":") { String.format("%02X", it) }

            android.util.Log.d("AUTH_DEBUG", "SHA-1 thực tế: $sha1")
        }
    } catch (e: Exception) {
        android.util.Log.e("AUTH_DEBUG", "Lỗi lấy SHA-1: ${e.message}")
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Gọi hàm in mã SHA-1 ra màn hình và Logcat
        getAndLogSHA1(this)

        // Khởi tạo SDK Quảng cáo AdMob
        MobileAds.initialize(this) {}

        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("music_app_prefs", Context.MODE_PRIVATE) }

            var currentLang by remember {
                mutableStateOf(
                    prefs.getString("app_language", null) ?: run {
                        val sysLang = java.util.Locale.getDefault().language
                        if (SupportedLanguages.any { it.code == sysLang }) sysLang else "vi"
                    }
                )
            }
            // 0: System, 1: Light, 2: Dark
            var themeMode by remember { mutableIntStateOf(prefs.getInt("app_theme_mode", 2)) }

            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                1 -> false
                2 -> true
                else -> systemDark
            }

            val appPalette = if (isDark) DarkPalette else LightPalette
            val appStrings = remember(currentLang) { AppStrings(currentLang) }

            CompositionLocalProvider(
                LocalPalette provides appPalette,
                LocalStrings provides appStrings
            ) {
                MusicEqTheme(isDark = isDark) {
                    MusicPlayerApp(
                        currentLang = currentLang,
                        themeMode = themeMode,
                        onLanguageChange = { newLang ->
                            currentLang = newLang
                            prefs.edit().putString("app_language", newLang).apply()
                        },
                        onThemeChange = { newMode ->
                            themeMode = newMode
                            prefs.edit().putInt("app_theme_mode", newMode).apply()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicEqTheme(isDark: Boolean, content: @Composable () -> Unit) {
    val palette = LocalPalette.current
    val colors = if (isDark) {
        darkColorScheme(
            primary = palette.purple,
            secondary = palette.blue,
            background = palette.background,
            surface = palette.card,
            onPrimary = Color.White,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
        )
    } else {
        lightColorScheme(
            primary = palette.purple,
            secondary = palette.blue,
            background = palette.background,
            surface = palette.card,
            onPrimary = Color.White,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
private fun MusicPlayerApp(
    currentLang: String,
    themeMode: Int,
    onLanguageChange: (String) -> Unit,
    onThemeChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val palette = LocalPalette.current
    val strings = LocalStrings.current
    val coroutineScope = rememberCoroutineScope()
    val musicPrefs = remember(context) {
        context.getSharedPreferences("music_app_prefs", Context.MODE_PRIVATE)
    }

    // --- CODE XIN QUYỀN VÀ GỌI BÁO THỨC ---
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                scheduleDailyReminder(context)
            } else {
                Toast.makeText(context, "Phải cấp quyền thì máy mới báo thức được!", Toast.LENGTH_LONG).show()
            }
        }

        LaunchedEffect(Unit) {
            val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                scheduleDailyReminder(context)
            }
        }
    } else {
        LaunchedEffect(Unit) {
            scheduleDailyReminder(context)
        }
    }
    // --- KẾT THÚC CODE XIN QUYỀN ---

    val gso = remember {
        com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
        )
            .requestIdToken("886650248212-iuvj3vueej3u8eu6jb0mpsbifmmq7dsk.apps.googleusercontent.com")
            .requestEmail()
            .requestProfile()
            .build()
    }
    val googleSignInClient = remember { com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso) }
    var currentUserEmail by remember {
        val savedEmail = context.getSharedPreferences("music_app_prefs", Context.MODE_PRIVATE).getString("logged_in_email", null)
        mutableStateOf(com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)?.email ?: savedEmail)
    }

    val googleAuthLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            val email = account?.email
            if (!email.isNullOrBlank()) {
                currentUserEmail = email
                val prefs = context.getSharedPreferences("music_app_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("logged_in_email", email).apply()
                Toast.makeText(context, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: com.google.android.gms.common.api.ApiException) {
            Toast.makeText(context, "Lỗi Google Sign-In (${e.statusCode}): ${e.message}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Lỗi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
    val songs = remember {
        mutableStateListOf<SongItem>().apply {
            val savedList = loadPlaylist(context)
            if (savedList != null && savedList.isNotEmpty()) {
                addAll(savedList)
            } else {
                addAll(createDemoSongs(context))
            }
        }
    }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()
            setAudioAttributes(audioAttributes, true)
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(1L) }
    var audioSessionId by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var favorite by remember { mutableStateOf(true) }

    var shuffleEnabled by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(Player.REPEAT_MODE_OFF) }
    var showEqualizer by remember { mutableStateOf(true) }

    var secretClickCount by remember { mutableIntStateOf(0) }
    var showTvIcon by remember { mutableStateOf(false) }

    var showAudioSheet by remember { mutableStateOf(false) }
    var scannedFolders by remember { mutableStateOf<Map<String, List<SongItem>>>(emptyMap()) }
    var driveFolderUriText by remember {
        mutableStateOf(musicPrefs.getString("google_drive_tree_uri", "").orEmpty())
    }
    var driveFolderName by remember {
        mutableStateOf(musicPrefs.getString("google_drive_tree_name", "").orEmpty())
    }
    var isScanningDrive by remember { mutableStateOf(false) }
    var lastScannedDriveUri by remember { mutableStateOf<String?>(null) }

    var pendingUpdateSong by remember { mutableStateOf<Triple<Int, String, String>?>(null) }
    var songToCut by remember { mutableStateOf<SongItem?>(null) }
    var songToMerge by remember { mutableStateOf<SongItem?>(null) }
    var songToDelete by remember { mutableStateOf<SongItem?>(null) }

    // Quản lý hộp thoại Cài đặt
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAdminDialog by remember { mutableStateOf(false) }

    val scanSelectedDriveFolder: (Uri, Boolean) -> Unit = { treeUri, persistSelection ->
        val uriText = treeUri.toString()
        lastScannedDriveUri = uriText
        showAudioSheet = true
        isScanningDrive = true
        errorMessage = null

        if (persistSelection) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // Một số DocumentsProvider chỉ cấp quyền trong phiên hiện tại.
                // Ứng dụng vẫn tiếp tục quét và phát nhạc trong phiên này.
            }
            driveFolderUriText = uriText
            musicPrefs.edit().putString("google_drive_tree_uri", uriText).apply()
        }

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val scanResult = scanDocumentTreeAudio(context, treeUri)
                withContext(Dispatchers.Main) {
                    val nonCloudFolders = scannedFolders
                        .mapValues { (_, list) -> list.filter { it.source != SongSource.SAF_CLOUD } }
                        .filterValues { it.isNotEmpty() }

                    scannedFolders = linkedMapOf<String, List<SongItem>>().apply {
                        putAll(nonCloudFolders)
                        putAll(scanResult.folders)
                    }

                    driveFolderName = scanResult.rootName
                    driveFolderUriText = uriText
                    musicPrefs.edit()
                        .putString("google_drive_tree_uri", uriText)
                        .putString("google_drive_tree_name", scanResult.rootName)
                        .apply()

                    isScanningDrive = false
                    errorMessage = if (scanResult.totalFiles == 0) {
                        "Không tìm thấy tệp âm thanh được hỗ trợ trong thư mục Drive đã chọn."
                    } else {
                        null
                    }
                }
            } catch (e: SecurityException) {
                withContext(Dispatchers.Main) {
                    isScanningDrive = false
                    lastScannedDriveUri = null
                    errorMessage = "Không thể đọc thư mục Google Drive. Hãy chọn lại thư mục và cấp quyền truy cập."
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isScanningDrive = false
                    lastScannedDriveUri = null
                    errorMessage = "Lỗi khi quét Google Drive: ${e.localizedMessage ?: "Không xác định"}"
                }
            }
        }
    }

    val driveFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            scanSelectedDriveFolder(treeUri, true)
        }
    }

    val chooseDriveFolder: () -> Unit = {
        val initialUri = driveFolderUriText
            .takeIf { it.isNotBlank() }
            ?.let { Uri.parse(it) }
        driveFolderLauncher.launch(initialUri)
    }

    LaunchedEffect(showAudioSheet, driveFolderUriText) {
        if (
            showAudioSheet &&
            driveFolderUriText.isNotBlank() &&
            lastScannedDriveUri != driveFolderUriText
        ) {
            scanSelectedDriveFolder(Uri.parse(driveFolderUriText), false)
        }
    }

    // Tự động nhận diện ngôn ngữ máy ở lần mở app đầu tiên
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("music_app_prefs", Context.MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("is_first_run_lang", true)

        if (isFirstRun) {
            val sysLang = java.util.Locale.getDefault().language
            val isSupported = SupportedLanguages.any { it.code == sysLang }

            if (isSupported) {
                // Ngôn ngữ máy có trong list -> Tự động lưu và áp dụng
                onLanguageChange(sysLang)
            } else {
                // Ngôn ngữ máy KHÔNG có trong list -> Bật hộp thoại cho người dùng chọn
                showLanguageDialog = true
            }

            // Đánh dấu đã xử lý xong lần mở đầu tiên
            prefs.edit().putBoolean("is_first_run_lang", false).apply()
        }
    }

    // Quản lý kiểm tra cập nhật ứng dụng tự động từ Google Play
    val appUpdateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            Toast.makeText(context, "Cập nhật ứng dụng bị hủy.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val appUpdateManager = com.google.android.play.core.appupdate.AppUpdateManagerFactory.create(context)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == com.google.android.play.core.install.model.UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE)
            ) {
                // Yêu cầu cập nhật bắt buộc nếu có bản mới trên Play Store
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE,
                    context as Activity,
                    101 // Request Code
                )
            }
        }.addOnFailureListener {
            // Không thao tác nếu có lỗi gọi API Play Store (vd: máy ko có CH Play)
        }
    }

    // Gộp mỗi thiết bị/tài khoản thành 1 dòng duy nhất và đếm số lần vào app trong ngày
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var isAppInForeground by remember { mutableStateOf(true) }

    // 1. Theo dõi trạng thái Foreground/Background
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_START || event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isAppInForeground = true
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE || event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                isAppInForeground = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var isSessionCounted by remember { mutableStateOf(false) }

    // 2. Cập nhật dữ liệu thời gian lên Firebase
    LaunchedEffect(currentUserEmail) {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "Unknown"
        val deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        val osVersion = "Android ${Build.VERSION.RELEASE}"

        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val dateStr = formatter.format(java.util.Date())

        val timeFormatter = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        val accessTimeStr = timeFormatter.format(java.util.Date()) // Chốt giờ truy cập 1 lần khi mở app

        val userNodeKey = androidId.replace(Regex("[^a-zA-Z0-9_]"), "_")

        try {
            val database = com.google.firebase.database.FirebaseDatabase.getInstance(
                "https://melowave-f8056-default-rtdb.asia-southeast1.firebasedatabase.app"
            ).getReference("usage_stats").child(dateStr).child(userNodeKey)

            var initialDurationMs = 0L
            var openCount = 1L

            // Dùng coroutine suspend để BẮT BUỘC CHỜ lấy xong dữ liệu cũ mới đi tiếp
            kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->
                database.get().addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        initialDurationMs = (snapshot.child("durationMs").value as? Number)?.toLong() ?: 0L
                        val dbOpenCount = (snapshot.child("openCount").value as? Number)?.toLong() ?: 0L
                        openCount = if (isSessionCounted) dbOpenCount else dbOpenCount + 1L
                    } else {
                        openCount = if (isSessionCounted) 0L else 1L
                    }
                    isSessionCounted = true
                    continuation.resumeWith(Result.success(Unit))
                }.addOnFailureListener {
                    continuation.resumeWith(Result.success(Unit)) // Lỗi mạng vẫn cho qua để tính từ 0
                }
            }

            var currentSessionAccumulatedMs = 0L
            var lastTickTime = System.currentTimeMillis()

            while (true) {
                if (isAppInForeground) {
                    // Đang ở Foreground -> Cộng dồn thời gian
                    val now = System.currentTimeMillis()
                    currentSessionAccumulatedMs += (now - lastTickTime)
                    lastTickTime = now

                    val totalDurationMs = initialDurationMs + currentSessionAccumulatedMs
                    val totalSec = totalDurationMs / 1000L
                    val durationText = if (totalSec >= 60) "${totalSec / 60}m ${totalSec % 60}s" else "${totalSec}s"
                    val activeUser = currentUserEmail ?: "Khách (${androidId.take(6).uppercase()})"

                    val stats = mapOf(
                        "userId" to activeUser,
                        "deviceModel" to deviceModel,
                        "osVersion" to osVersion,
                        "deviceId" to androidId,
                        "accessTime" to accessTimeStr, // Giữ nguyên giờ của phiên đăng nhập này
                        "openCount" to openCount, // Lưu đúng số lần mở app đã cộng thêm
                        "durationString" to durationText,
                        "durationMs" to totalDurationMs
                    )

                    database.setValue(stats)
                } else {
                    // Đang ở Background -> Tạm ngưng, kéo mốc thời gian lastTickTime tới hiện tại để không cộng thời gian ẩn
                    lastTickTime = System.currentTimeMillis()
                }

                kotlinx.coroutines.delay(5000L) // Chu kỳ cập nhật mỗi 5 giây
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val updatePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingUpdateSong?.let { (index, newTitle, newArtist) ->
                val song = songs[index]
                try {
                    val values = ContentValues().apply {
                        put(MediaStore.Audio.Media.TITLE, newTitle)
                        put(MediaStore.Audio.Media.ARTIST, newArtist)
                        put(MediaStore.Audio.Media.DISPLAY_NAME, "$newTitle.mp3")
                    }
                    context.contentResolver.update(song.uri, values, null, null)
                    saveCustomSongInfo(context, song.id, newTitle, newArtist)

                    val updatedSong = song.copy(title = newTitle, artist = newArtist)
                    songs[index] = updatedSong
                    if (index < player.mediaItemCount) {
                        player.replaceMediaItem(index, updatedSong.toMediaItem())
                    }
                    savePlaylist(context, songs.toList())
                } catch (e: Exception) {
                    errorMessage = "Lỗi khi cập nhật sau khi cấp quyền: ${e.message}"
                }
            }
        } else {
            errorMessage = "Cập nhật bị từ chối bởi người dùng."
        }
        pendingUpdateSong = null
    }

    val removeSongFromPlaylist: (SongItem) -> Unit = { song ->
        val removedIndex = songs.indexOfFirst { it.id == song.id }
        if (removedIndex >= 0) {
            songs.removeAt(removedIndex)

            if (removedIndex < player.mediaItemCount) {
                runCatching { player.removeMediaItem(removedIndex) }
            }

            currentIndex = when {
                songs.isEmpty() -> 0
                removedIndex <= currentIndex -> (currentIndex - 1).coerceAtLeast(0)
                else -> currentIndex.coerceAtMost(songs.lastIndex)
            }

            if (songs.isEmpty()) {
                player.stop()
                player.clearMediaItems()
                isPlaying = false
                positionMs = 0L
                durationMs = 1L
            }

            savePlaylist(context, songs.toList())
        }
    }

    val deletePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            songToDelete?.let { song ->
                removeSongFromPlaylist(song)
                Toast.makeText(context, strings.deleteSuccess(), Toast.LENGTH_SHORT).show()
            }
        }
        songToDelete = null
    }

    val performDelete: (SongItem) -> Unit = deleteSong@{ song ->
        // Tệp Google Drive, URL online và tài nguyên demo chỉ bị gỡ khỏi playlist.
        // Không gọi deleteDocument để tránh xóa nhầm tệp thật trên đám mây.
        if (song.source != SongSource.LOCAL_MEDIASTORE) {
            removeSongFromPlaylist(song)
            Toast.makeText(context, strings.removeFromListSuccess(), Toast.LENGTH_SHORT).show()
            return@deleteSong
        }

        try {
            val isDeleted = context.contentResolver.delete(song.uri, null, null) > 0

            if (isDeleted) {
                removeSongFromPlaylist(song)
                Toast.makeText(context, strings.deleteSuccess(), Toast.LENGTH_SHORT).show()
            } else {
                errorMessage = "Không thể xóa tệp khỏi thiết bị."
            }
        } catch (e: SecurityException) {
            songToDelete = song
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intentSender = MediaStore.createDeleteRequest(
                        context.contentResolver,
                        listOf(song.uri)
                    ).intentSender
                    deletePermissionLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    val recoverableSecurityException = e as? RecoverableSecurityException
                    if (recoverableSecurityException != null) {
                        val intentSender = recoverableSecurityException.userAction.actionIntent.intentSender
                        deletePermissionLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                    } else {
                        errorMessage = "Thiếu quyền xóa tệp."
                    }
                } else {
                    errorMessage = "Thiếu quyền xóa tệp."
                }
            } catch (ex: Exception) {
                errorMessage = "Lỗi hệ thống khi xóa: ${ex.message}"
            }
        } catch (e: Exception) {
            errorMessage = "Không thể xóa: ${e.localizedMessage ?: "Không xác định"}"
        }
    }

    val playerListener = remember(player) {
        object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) {
                isPlaying = value
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isLoading = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                if (playbackState == Player.STATE_READY) {
                    errorMessage = null

                    val readyIndex = player.currentMediaItemIndex
                    val readyDuration = player.duration
                    if (
                        readyIndex in songs.indices &&
                        readyDuration != C.TIME_UNSET &&
                        readyDuration > 0L &&
                        songs[readyIndex].durationLabel == "--:--"
                    ) {
                        songs[readyIndex] = songs[readyIndex].copy(
                            durationLabel = formatMillis(readyDuration)
                        )
                        savePlaylist(context, songs.toList())
                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentIndex = player.currentMediaItemIndex.coerceAtLeast(0)
            }

            override fun onPlayerError(error: PlaybackException) {
                isLoading = false
                isPlaying = false

                val sourceName = songs.getOrNull(currentIndex)?.let { song ->
                    when (song.source) {
                        SongSource.SAF_CLOUD -> "Google Drive"
                        SongSource.ONLINE_HTTP -> "Internet"
                        SongSource.YOUTUBE -> "YouTube"
                        SongSource.APP_RESOURCE -> "tài nguyên ứng dụng"
                        SongSource.LOCAL_MEDIASTORE -> "bộ nhớ thiết bị"
                    }
                } ?: "nguồn phát"

                val detail = error.cause?.localizedMessage
                    ?: error.localizedMessage
                    ?: "Không xác định được nguyên nhân"

                val failedIndex = player.currentMediaItemIndex
                val nextIndex = failedIndex + 1
                val canTryNext = failedIndex >= 0 && nextIndex < player.mediaItemCount

                errorMessage = "Không thể phát tệp từ $sourceName. " +
                        "Mã lỗi: ${error.errorCodeName}. Chi tiết: $detail" +
                        if (canTryNext) " Ứng dụng sẽ thử bài tiếp theo." else ""

                if (canTryNext) {
                    coroutineScope.launch {
                        delay(900L)
                        if (player.currentMediaItemIndex == failedIndex) {
                            currentIndex = nextIndex
                            player.seekTo(nextIndex, 0L)
                            player.prepare()
                            player.play()
                        }
                    }
                }
            }

            override fun onAudioSessionIdChanged(newAudioSessionId: Int) {
                if (newAudioSessionId > 0 && newAudioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    audioSessionId = newAudioSessionId
                }
            }
        }
    }

    DisposableEffect(player) {
        player.addListener(playerListener)
        onDispose {
            player.removeListener(playerListener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        if (player.mediaItemCount == 0) {
            player.setMediaItems(songs.map { it.toMediaItem() })
            player.prepare()
        }
    }

    // Đã gộp luồng cập nhật thời gian xuống bên dưới để tránh xung đột với YouTube

    val safeIndex = currentIndex.coerceIn(0, max(0, songs.lastIndex))
    val currentSong = songs.getOrNull(safeIndex) ?: SongItem("default", "Unknown", "Unknown", Uri.EMPTY, "0:00")

    LaunchedEffect(currentSong.id) {
        if (currentSong.artworkUrl == null && currentSong.id.startsWith("content://")) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val query = "${currentSong.title} ${currentSong.artist}".trim()
                    val encodedQuery = URLEncoder.encode(query, "UTF-8")
                    val url = "https://itunes.apple.com/search?term=$encodedQuery&media=music&entity=song&limit=1"
                    val response = URL(url).readText()
                    val results = JSONObject(response).getJSONArray("results")

                    if (results.length() > 0) {
                        val rawArtwork = results.getJSONObject(0).optString("artworkUrl100", "")
                        val hdArtworkUrl = rawArtwork.replace("100x100bb", "600x600bb").takeIf { it.isNotBlank() }

                        if (hdArtworkUrl != null) {
                            withContext(Dispatchers.Main) {
                                songs[safeIndex] = currentSong.copy(artworkUrl = hdArtworkUrl)
                                savePlaylist(context, songs.toList()) // Cập nhật lại list sau khi load ảnh
                            }
                        }
                    }
                } catch (e: Exception) {
                }
            }
        }
    }

    // BẮT ĐẦU: Tự động load ảnh cho toàn bộ danh sách khi chuyển qua tab List
    LaunchedEffect(showEqualizer) {
        if (!showEqualizer) { // Khi showEqualizer = false tức là đang hiển thị List
            coroutineScope.launch(Dispatchers.IO) {
                var isUpdated = false
                // Chỉ tải ảnh nền cho tối đa 30 bài local đầu tiên để tránh hàng nghìn
                // request mạng khi người dùng chọn một thư mục Drive lớn.
                for (i in songs.indices.take(30)) {
                    val song = songs[i]
                    if (
                        song.source == SongSource.LOCAL_MEDIASTORE &&
                        song.artworkUrl == null &&
                        song.id.startsWith("content://")
                    ) {
                        try {
                            val query = "${song.title} ${song.artist}".trim()
                            val encodedQuery = URLEncoder.encode(query, "UTF-8")
                            val url = "https://itunes.apple.com/search?term=$encodedQuery&media=music&entity=song&limit=1"
                            val response = URL(url).readText()
                            val results = JSONObject(response).getJSONArray("results")

                            if (results.length() > 0) {
                                val rawArtwork = results.getJSONObject(0).optString("artworkUrl100", "")
                                val hdArtworkUrl = rawArtwork.replace("100x100bb", "600x600bb").takeIf { it.isNotBlank() }

                                if (hdArtworkUrl != null) {
                                    withContext(Dispatchers.Main) {
                                        if (i < songs.size && songs[i].id == song.id) {
                                            songs[i] = song.copy(artworkUrl = hdArtworkUrl)
                                            isUpdated = true
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                        }
                    }
                }

                // Nếu có bất kỳ bài hát nào được tải ảnh mới, tiến hành lưu lại vào bộ nhớ
                if (isUpdated) {
                    withContext(Dispatchers.Main) {
                        savePlaylist(context, songs.toList())
                    }
                }
            }
        }
    }
    // KẾT THÚC

    var isVideoSearchMode by remember { mutableStateOf(false) }
    var isYoutubeMode by remember { mutableStateOf(false) }
    var youtubeSearchQuery by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<android.webkit.WebView?>(null) }
    var fullScreenView by remember { mutableStateOf<android.view.View?>(null) }
    var fullScreenCallback by remember { mutableStateOf<android.webkit.WebChromeClient.CustomViewCallback?>(null) }

    var isSearching by remember { mutableStateOf(false) }

    val searchOnlineMusic: (String) -> Unit = { query ->
        if (query.isNotBlank()) {
            isSearching = true
            errorMessage = null
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val encodedQuery = URLEncoder.encode(query, "UTF-8")
                    val onlineSongs = mutableListOf<SongItem>()

                    if (isVideoSearchMode || isYoutubeMode) {
                        // GỌI API PYTHON ĐỂ TÌM KIẾM MEDIA
                        val searchUrl = "https://cut.tunertools.top/api/v1/search?query=$encodedQuery&limit=25"
                        val response = URL(searchUrl).readText()
                        val jsonResponse = JSONObject(response)

                        if (jsonResponse.optString("status") == "success") {
                            val dataArray = jsonResponse.getJSONArray("data")
                            for (i in 0 until dataArray.length()) {
                                val item = dataArray.getJSONObject(i)
                                val videoIdWithPrefix = item.getString("id")
                                val videoId = videoIdWithPrefix.removePrefix("vid_") // Bóc tách id thật

                                val durationSec = item.optLong("duration", 0L)
                                val formattedDuration = if (durationSec > 0) {
                                    String.format(Locale.getDefault(), "%d:%02d", durationSec / 60, durationSec % 60)
                                } else {
                                    "Media"
                                }

                                onlineSongs.add(
                                    SongItem(
                                        id = videoIdWithPrefix,
                                        title = item.getString("title"),
                                        artist = item.getString("artist"),
                                        // Dùng link rút gọn youtu.be để tránh lộ từ khóa domain đầy đủ
                                        uri = Uri.parse("https://youtu.be/$videoId"),
                                        durationLabel = formattedDuration,
                                        artworkRes = R.drawable.album_cover,
                                        artworkUrl = item.getString("artworkUrl"),
                                        source = SongSource.YOUTUBE,
                                        mimeType = null
                                    )
                                )
                            }
                        }
                    } else {
                        // TÌM KIẾM ITUNES MP3 NHƯ BÌNH THƯỜNG
                        val url = "https://itunes.apple.com/search?term=$encodedQuery&media=music&entity=song&limit=25"
                        val response = URL(url).readText()
                        val results = JSONObject(response).getJSONArray("results")

                        for (i in 0 until results.length()) {
                            val track = results.getJSONObject(i)
                            val previewUrl = track.optString("previewUrl", "")
                            val rawArtwork = track.optString("artworkUrl100", "")
                            val hdArtworkUrl = rawArtwork.replace("100x100bb", "600x600bb").takeIf { it.isNotBlank() }

                            if (previewUrl.isNotBlank()) {
                                // Ép sử dụng HTTPS để tránh lỗi "Source error" do Android chặn kết nối HTTP
                                val safePreviewUrl = previewUrl.replace("http://", "https://")
                                onlineSongs.add(
                                    SongItem(
                                        id = track.optString("trackId", "id_$i"),
                                        title = track.optString("trackName", "Unknown"),
                                        artist = track.optString("artistName", "Unknown Artist"),
                                        uri = Uri.parse(safePreviewUrl),
                                        durationLabel = formatMillis(track.optLong("trackTimeMillis", 30000L)),
                                        artworkRes = R.drawable.album_cover,
                                        artworkUrl = hdArtworkUrl,
                                        source = SongSource.ONLINE_HTTP,
                                        mimeType = "audio/mp4"
                                    )
                                )
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        isSearching = false
                        if (onlineSongs.isNotEmpty()) {
                            songs.clear()
                            songs.addAll(onlineSongs)

                            if (isVideoSearchMode || isYoutubeMode) {
                                // NẾU LÀ YOUTUBE: Xóa playlist của ExoPlayer để tránh nó cố tải link web gây lỗi Source Error
                                player.clearMediaItems()
                                if (player.isPlaying) player.pause()
                            } else {
                                // NẾU LÀ NHẠC MP3: Nạp vào ExoPlayer và phát bình thường
                                player.setMediaItems(songs.map { it.toMediaItem() })
                                player.prepare()
                                player.play()
                            }

                            errorMessage = null
                        } else {
                            errorMessage = "Không tìm thấy kết quả nào cho '$query'."
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isSearching = false
                        errorMessage = "Lỗi kết nối mạng! Vui lòng kiểm tra lại Internet."
                    }
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val isGranted = permissions.values.any { it }
        showAudioSheet = true

        if (isGranted) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val localFolders = scanLocalAudioFolders(context)
                    withContext(Dispatchers.Main) {
                        val cloudFolders = scannedFolders
                            .mapValues { (_, list) -> list.filter { it.source == SongSource.SAF_CLOUD } }
                            .filterValues { it.isNotEmpty() }

                        scannedFolders = linkedMapOf<String, List<SongItem>>().apply {
                            putAll(localFolders)
                            putAll(cloudFolders)
                        }

                        errorMessage = if (scannedFolders.isEmpty()) {
                            "Không tìm thấy tệp âm thanh trên thiết bị. Bạn vẫn có thể chọn Google Drive."
                        } else {
                            null
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Lỗi khi quét nhạc trên thiết bị: ${e.localizedMessage ?: "Không xác định"}"
                    }
                }
            }
        } else {
            // Không đóng hộp thoại: người dùng vẫn có thể chọn Google Drive bằng SAF.
            errorMessage = "Chưa cấp quyền đọc nhạc trên thiết bị. Bạn vẫn có thể chọn Google Drive."
        }
    }

    val requestMusicScan: () -> Unit = {
        showAudioSheet = true

        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(android.Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        permissionLauncher.launch(permissionsToRequest)
    }
// BỔ SUNG: Đồng bộ tiến trình Video YouTube và ExoPlayer ra chung một mối
    LaunchedEffect(isYoutubeMode, webViewRef, player) {
        while (true) {
            if (isYoutubeMode && webViewRef != null) {
                // TRẠNG THÁI 1: ĐANG XEM YOUTUBE
                webViewRef?.evaluateJavascript("(function() { var v = document.getElementsByTagName('video')[0]; return v ? (v.currentTime + '|' + v.duration + '|' + (v.paused ? 'true' : 'false')) : ''; })();") { result ->
                    val cleanResult = result?.replace("\"", "") ?: ""
                    if (cleanResult.isNotBlank() && cleanResult.contains("|")) {
                        val parts = cleanResult.split("|")
                        val currentSec = parts.getOrNull(0)?.toFloatOrNull()
                        val durSec = parts.getOrNull(1)?.toFloatOrNull()
                        val paused = parts.getOrNull(2)?.toBooleanStrictOrNull() ?: false

                        if (currentSec != null && currentSec >= 0) positionMs = (currentSec * 1000).toLong()
                        if (durSec != null && durSec > 0) durationMs = (durSec * 1000).toLong()

                        // BẮT BUỘC tắt MP3 khi đang ở mode YouTube để tránh phát nhạc đè lên nhau
                        if (player.isPlaying) player.pause()
                        isPlaying = !paused
                    }
                }
                kotlinx.coroutines.delay(500)
            } else {
                // TRẠNG THÁI 2: ĐANG NGHE MP3 BÌNH THƯỜNG
                positionMs = player.currentPosition.coerceAtLeast(0L)
                val detectedDuration = player.duration
                durationMs = if (detectedDuration == C.TIME_UNSET || detectedDuration <= 0L) 1L else detectedDuration

                val detectedSessionId = player.audioSessionId
                if (
                    detectedSessionId > 0 &&
                    detectedSessionId != C.AUDIO_SESSION_ID_UNSET &&
                    detectedSessionId != audioSessionId
                ) {
                    audioSessionId = detectedSessionId
                }
                kotlinx.coroutines.delay(if (player.isPlaying) 250L else 600L)
            }
        }
    }

    val togglePlayback: () -> Unit = {
        errorMessage = null
        if (isYoutubeMode && webViewRef != null) {
            val js = """
                var v = document.getElementsByTagName('video')[0]; 
                if(v) { 
                    if(v.paused) { 
                        v.dataset.userPaused = 'false'; 
                        v.play(); 
                    } else { 
                        v.dataset.userPaused = 'true'; 
                        v.pause(); 
                    } 
                }
            """.trimIndent()
            webViewRef?.evaluateJavascript(js, null)
            Unit
        } else {
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0L)
            }
            if (player.isPlaying) player.pause() else player.play()
            Unit
        }
    }

    val handleSeek: (Long) -> Unit = { position ->
        if (isYoutubeMode && webViewRef != null) {
            webViewRef?.evaluateJavascript("var v = document.getElementsByTagName('video')[0]; if(v) v.currentTime = ${position / 1000.0};", null)
            positionMs = position
        } else {
            player.seekTo(position)
        }
    }

    val previousTrack = {
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0L)
        }
        player.play()
    }

    val nextTrack = {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else if (songs.isNotEmpty()) {
            player.seekTo(0, 0L)
        }
        player.play()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        palette.background,
                        palette.backgroundSoft,
                        palette.background,
                    ),
                ),
            ),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
        ) { scaffoldPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
                    .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 0.dp), // Ép sát lề dưới cùng để tăng thêm không gian
                verticalArrangement = Arrangement.spacedBy(4.dp), // Thu hẹp khoảng cách giữa các khối để nút Play không bị đẩy lên
            ) {
                PlayerTopBar(
                    onSearch = { query ->
                        searchOnlineMusic(query)
                        showEqualizer = false // Luôn xổ ra danh sách kết quả (List)

                        if (isVideoSearchMode || isYoutubeMode) {
                            // Tắt giao diện web lớn nhưng GIỮ icon TV sáng để phát video khi click list
                            isVideoSearchMode = false
                            isYoutubeMode = true
                        } else {
                            isVideoSearchMode = false
                            isYoutubeMode = false
                        }
                    },
                    onYoutubeSearch = { query ->
                        // Đã bỏ qua
                    },
                    isVideoSearchMode = isYoutubeMode, // Nút icon TV sẽ sáng khi bật chế độ YouTube
                    onToggleVideoSearchMode = {
                        // 1. Chỉ bật/tắt chế độ tìm kiếm YouTube (không mở giao diện Web lớn nữa)
                        isYoutubeMode = !isYoutubeMode

                        // 2. Ép tắt giao diện Web lớn vĩnh viễn
                        isVideoSearchMode = false

                        // 3. Dừng nhạc MP3 nền nếu người dùng chuyển sang chế độ YouTube
                        if (isYoutubeMode && player.isPlaying) {
                            player.pause()
                        }
                    },
                    onSettingsClick = { showSettingsDialog = true },
                    showTvIcon = showTvIcon
                )

                if (isVideoSearchMode) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                    ) {
                        var lastSearchedQuery by remember { mutableStateOf("") }

                        AndroidView(
                            factory = { ctx ->
                                android.webkit.WebView(ctx).apply {
                                    layoutParams = android.view.ViewGroup.LayoutParams(
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    setBackgroundColor(android.graphics.Color.BLACK)
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        mediaPlaybackRequiresUserGesture = false
                                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                        userAgentString = "Mozilla/5.0"
                                    }
                                    webChromeClient = object : android.webkit.WebChromeClient() {
                                        override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
                                            super.onShowCustomView(view, callback)
                                            if (view != null && callback != null) {
                                                fullScreenView = view
                                                fullScreenCallback = callback
                                            }
                                        }
                                        override fun onHideCustomView() {
                                            super.onHideCustomView()
                                            fullScreenView = null
                                            fullScreenCallback = null
                                        }
                                    }
                                    webViewClient = object : android.webkit.WebViewClient() {
                                        override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?): Boolean = false

                                        override fun doUpdateVisitedHistory(view: android.webkit.WebView?, url: String?, isReload: Boolean) {
                                            super.doUpdateVisitedHistory(view, url, isReload)
                                            // Khi người dùng bấm vào một video cụ thể
                                            if (url != null && url.contains("watch?v=")) {
                                                view?.evaluateJavascript("var v = document.getElementsByTagName('video')[0]; if(v) v.pause();", null)
                                                youtubeSearchQuery = url
                                                isYoutubeMode = true
                                                isVideoSearchMode = false
                                                if (player.isPlaying) player.pause() // Ép tắt MP3 ngay lập tức
                                            }
                                        }

                                        override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            val js = """
                                                javascript:(function() {
                                                    var style = document.createElement('style');
                                                    style.id = 'custom-yt-style';
                                                    style.innerHTML = `
                                                        body, html { width: 100% !important; height: 100% !important; background-color: black !important; margin: 0 !important; padding: 0 !important; overflow: hidden !important; }
                                                        
                                                        /* Ẩn siêu bạo lực mọi loại Logo, Avatar, Overlay của YouTube bằng CSS */
                                                        .ytp-chrome-top, .ytp-watermark, .ytp-youtube-button, .ytp-title-channel, 
                                                        .ytp-title-text, .ytp-title, .ytp-title-channel-logo, .ytp-gradient-top, 
                                                        .ytp-paid-content-overlay, .ytm-paid-content-overlay, .ytp-ad-overlay-container, 
                                                        .ytm-custom-ad-banner, .annotation, .ytp-ce-element, .ytp-impression-link,
                                                        .ytp-button.ytp-share-button, .ytp-button.ytp-watch-later-button, .branding-img,
                                                        .ytp-pause-overlay-container, .ytp-pause-overlay, .ytp-related-video-overlay,
                                                        .ytp-menuitem, .ytp-panel, .ytp-error, .ytp-spinner { 
                                                            display: none !important; opacity: 0 !important; visibility: hidden !important; pointer-events: none !important; width: 0 !important; height: 0 !important; 
                                                        }
                                                        
                                                        .html5-video-player { position: absolute !important; top: 0 !important; left: 0 !important; width: 100% !important; height: 100% !important; z-index: 9999 !important; background: black !important; }
                                                        
                                                        /* Ẩn Controls Bar khi xem cửa sổ nhỏ */
                                                        body:not(.fullscreen) .ytp-chrome-bottom, 
                                                        body:not(.fullscreen) .ytp-gradient-bottom { display: none !important; opacity: 0 !important; visibility: hidden !important; }
                                                        body:not(.fullscreen) video { object-fit: cover !important; width: 100% !important; height: 100% !important; position: absolute !important; top: 0 !important; left: 0 !important; z-index: 9999 !important; pointer-events: none !important; }
                                                        
                                                        /* Bật Controls khi vào chế độ Fullscreen */
                                                        body.fullscreen video { object-fit: contain !important; width: 100% !important; height: 100% !important; position: absolute !important; top: 0 !important; left: 0 !important; z-index: 1 !important; pointer-events: auto !important; }
                                                    `;
                                                    document.head.appendChild(style);
                                                    
                                                    Object.defineProperty(document, 'visibilityState', {value: 'visible', writable: true});
                                                    Object.defineProperty(document, 'hidden', {value: false, writable: true});
                                                    
                                                    var videoSetup = false;

                                                    setInterval(function() {
                                                        var v = document.querySelector('video');
                                                        
                                                        // XÓA XÁC DOM (Xóa vĩnh viễn các thẻ rác khỏi bộ nhớ trình duyệt để chống YouTube ghi đè CSS)
                                                        var garbage = document.querySelectorAll('.ytp-chrome-top, .ytp-watermark, .ytp-pause-overlay-container, .ytp-related-video-overlay, .ytp-impression-link, .ytp-gradient-top');
                                                        garbage.forEach(function(el) { el.remove(); });

                                                        if (!v) return;
                                                        
                                                        if (!videoSetup) {
                                                            v.addEventListener('pause', function() {
                                                                if (document.body.classList.contains('fullscreen')) v.dataset.userPaused = 'true';
                                                            });
                                                            v.addEventListener('play', function() {
                                                                if (document.body.classList.contains('fullscreen')) v.dataset.userPaused = 'false';
                                                            });
                                                            videoSetup = true;
                                                        }

                                                        if (v.paused && v.dataset.userPaused !== 'true') {
                                                            var largePlayBtn = document.querySelector('.ytp-large-play-button');
                                                            var smallPlayBtn = document.querySelector('.ytp-play-button');
                                                            if (largePlayBtn && largePlayBtn.style.display !== 'none') largePlayBtn.click();
                                                            else if (smallPlayBtn) smallPlayBtn.click();
                                                            var p = v.play();
                                                            if (p !== undefined) p.catch(e => {});
                                                        }
                                                        
                                                        var skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytm-autonav-promo-close-button');
                                                        if (skipBtn) skipBtn.click(); 
                                                        else {
                                                            var adShowing = document.querySelector('.ad-showing');
                                                            if (adShowing) { v.playbackRate = 16.0; v.muted = true; } 
                                                            else { if (v.playbackRate === 16.0) { v.playbackRate = 1.0; v.muted = false; } }
                                                        }
                                                    }, 500);
                                                })();
                                            """.trimIndent()
                                            view?.evaluateJavascript(js, null)
                                        }
                                    }
                                    webViewRef = this

                                    // Nạp mặc định trang chủ YouTube Mobile khi vừa bật
                                    loadUrl("https://m.youtube.com", mutableMapOf("Referer" to "http://ducphi.atwebpages.com/"))
                                }
                            },
                            update = { webView ->
                                if (youtubeSearchQuery.isNotBlank() && youtubeSearchQuery != lastSearchedQuery) {
                                    lastSearchedQuery = youtubeSearchQuery
                                    val targetUrl = if (youtubeSearchQuery.startsWith("http")) {
                                        youtubeSearchQuery
                                    } else {
                                        "https://m.youtube.com/results?search_query=${java.net.URLEncoder.encode(youtubeSearchQuery, "UTF-8")}"
                                    }
                                    webView.loadUrl(targetUrl, mutableMapOf("Referer" to "http://ducphi.atwebpages.com/"))
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    NowPlayingSection(
                        song = currentSong,
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        favorite = favorite,
                        shuffleEnabled = shuffleEnabled,
                        repeatMode = repeatMode,
                        showEqualizer = showEqualizer,
                        isYoutubeMode = isYoutubeMode, // Liên kết lại biến trạng thái để hiển thị AlbumArtwork
                        youtubeSearchQuery = youtubeSearchQuery,
                        onWebViewCreated = { webViewRef = it },
                        onEnterFullScreen = { view, callback ->
                            fullScreenView = view
                            fullScreenCallback = callback
                        },
                        onExitFullScreen = {
                            fullScreenView = null
                            fullScreenCallback = null
                        },
                        onFavorite = { favorite = !favorite },
                        onShuffle = {
                            shuffleEnabled = !shuffleEnabled
                            player.shuffleModeEnabled = shuffleEnabled
                        },
                        onRepeat = {
                            val newMode = when (repeatMode) {
                                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                else -> Player.REPEAT_MODE_OFF
                            }
                            repeatMode = newMode
                            player.repeatMode = newMode
                        },
                        onPrevious = previousTrack,
                        onPlayPause = togglePlayback,
                        onNext = nextTrack,
                        onSeek = handleSeek,
                        onToggleEqualizer = { showEqualizer = !showEqualizer },
                        onOpenFolder = requestMusicScan,
                        onArtworkClick = {
                            secretClickCount++
                            if (secretClickCount >= 7) {
                                showTvIcon = true
                            }
                        }
                    )

                    // Hiển thị sóng âm nhấp nháy dưới nút play CHỈ KHI đang tải/tìm kiếm
                    if (isSearching) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            WaveLoadingIndicator(modifier = Modifier.height(32.dp))
                        }
                    } else if (errorMessage != null) {
                        ErrorBanner(message = errorMessage.orEmpty())
                    }

                    if (showEqualizer) {
                        EqualizerPanel(
                            audioSessionId = audioSessionId,
                            onImportAudio = requestMusicScan,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(max = 280.dp) // GIỚI HẠN CỨNG: Ép Equalizer nhỏ lại, không cho phép phình to đẩy bố cục
                        )
                    } else {
                        SongListPanel(
                            songs = songs,
                            currentSongId = currentSong.id,
                            isSearching = isSearching, // TRUYỀN BIẾN NÀY VÀO ĐỂ LIST BIẾT TRẠNG THÁI
                            onSongSelected = { index ->
                                if (index !in songs.indices) return@SongListPanel

                                currentIndex = index
                                val selectedSong = songs[index]
                                val isDirectYoutubeItem =
                                    selectedSong.source == SongSource.YOUTUBE ||
                                            selectedSong.id.startsWith("yt_") ||
                                            selectedSong.id.startsWith("vid_")

                                if (isVideoSearchMode || isYoutubeMode || isDirectYoutubeItem) {
                                    if (player.isPlaying) player.pause()

                                    isYoutubeMode = true
                                    isVideoSearchMode = false

                                    if (isDirectYoutubeItem) {
                                        youtubeSearchQuery = selectedSong.uri.toString()
                                        errorMessage = null
                                    } else {
                                        errorMessage = null
                                        coroutineScope.launch(Dispatchers.IO) {
                                            try {
                                                val query = "${selectedSong.title} ${selectedSong.artist}".trim()
                                                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                                                val searchUrl = "https://cut.tunertools.top/api/v1/search?query=$encodedQuery&limit=1"
                                                val response = URL(searchUrl).readText()
                                                val jsonResponse = JSONObject(response)

                                                withContext(Dispatchers.Main) {
                                                    if (jsonResponse.optString("status") == "success") {
                                                        val dataArray = jsonResponse.getJSONArray("data")
                                                        if (dataArray.length() > 0) {
                                                            val item = dataArray.getJSONObject(0)
                                                            val videoId = item.getString("id").removePrefix("vid_")
                                                            youtubeSearchQuery = "https://youtu.be/$videoId"
                                                            errorMessage = null
                                                        } else {
                                                            youtubeSearchQuery = "https://m.youtube.com/results?search_query=$encodedQuery"
                                                            errorMessage = "Không tìm thấy MV cho bài hát này."
                                                        }
                                                    } else {
                                                        youtubeSearchQuery = "https://m.youtube.com/results?search_query=$encodedQuery"
                                                        errorMessage = "Lỗi xử lý API lấy video."
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    youtubeSearchQuery = selectedSong.title
                                                    errorMessage = "Lỗi kết nối API."
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    errorMessage = null
                                    webViewRef?.evaluateJavascript(
                                        "document.querySelector('video')?.pause();",
                                        null
                                    )

                                    if (selectedSong.uri.toString().contains("youtu", ignoreCase = true)) {
                                        errorMessage = "Vui lòng bật icon TV (chế độ Video) để phát bài này!"
                                    } else {
                                        val playerListIsDifferent =
                                            player.mediaItemCount != songs.size ||
                                                    index >= player.mediaItemCount ||
                                                    player.getMediaItemAt(index).mediaId != selectedSong.id

                                        if (playerListIsDifferent) {
                                            player.setMediaItems(songs.map { it.toMediaItem() })
                                            player.prepare()
                                        }

                                        player.seekTo(index, 0L)
                                        player.play()
                                    }
                                }
                            },
                            onSongEdit = { index, newTitle, newArtist ->
                                if (index in songs.indices) {
                                    val song = songs[index]
                                    saveCustomSongInfo(context, song.id, newTitle, newArtist)

                                    // Với Google Drive/URL online, chỉ sửa tên hiển thị trong ứng dụng,
                                    // không ghi ngược vào tệp đám mây.
                                    if (song.source != SongSource.LOCAL_MEDIASTORE) {
                                        val updatedSong = song.copy(title = newTitle, artist = newArtist)
                                        songs[index] = updatedSong
                                        if (index < player.mediaItemCount) {
                                            player.replaceMediaItem(index, updatedSong.toMediaItem())
                                        }
                                        savePlaylist(context, songs.toList())
                                        return@SongListPanel
                                    }

                                    try {
                                        val values = ContentValues().apply {
                                            put(MediaStore.Audio.Media.TITLE, newTitle)
                                            put(MediaStore.Audio.Media.ARTIST, newArtist)
                                            put(MediaStore.Audio.Media.DISPLAY_NAME, "$newTitle.mp3")
                                        }

                                        context.contentResolver.update(song.uri, values, null, null)

                                        val updatedSong = song.copy(title = newTitle, artist = newArtist)
                                        songs[index] = updatedSong
                                        if (index < player.mediaItemCount) {
                                            player.replaceMediaItem(index, updatedSong.toMediaItem())
                                        }
                                        savePlaylist(context, songs.toList())
                                    } catch (e: Exception) {
                                        pendingUpdateSong = Triple(index, newTitle, newArtist)
                                        try {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                                val intentSender = MediaStore.createWriteRequest(
                                                    context.contentResolver,
                                                    listOf(song.uri)
                                                ).intentSender
                                                updatePermissionLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                                            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                                                val recoverableSecurityException = e as? RecoverableSecurityException ?: throw e
                                                val intentSender = recoverableSecurityException.userAction.actionIntent.intentSender
                                                updatePermissionLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                                            } else {
                                                errorMessage = "Thiếu quyền ghi file."
                                            }
                                        } catch (ex: Exception) {
                                            errorMessage = "Lỗi hệ thống: ${ex.message}"
                                        }
                                    }
                                }
                            },
                            onSongCut = { song ->
                                songToCut = song
                            },
                            onSongMerge = { song ->
                                songToMerge = song
                            },
                            onSongDelete = performDelete,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Khóa cứng không gian Banner vừa đúng chuẩn 50dp, chặn đứng việc quảng cáo phình to đẩy UI lên trên
                BannerAdView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp) // 50dp Banner + 4dp Padding
                        .padding(top = 4.dp)
                )
            }
        }

        if (showAudioSheet) {
            AudioSelectionSheet(
                foldersMap = scannedFolders,
                driveFolderUri = driveFolderUriText,
                driveFolderName = driveFolderName,
                isScanningDrive = isScanningDrive,
                onChooseDriveFolder = chooseDriveFolder,
                onDismiss = { showAudioSheet = false },
                onConfirm = { selectedList ->
                    if (selectedList.isNotEmpty()) {
                        // Chuyển dứt khoát khỏi chế độ YouTube trước khi phát Local/Drive.
                        isYoutubeMode = false
                        isVideoSearchMode = false
                        youtubeSearchQuery = ""
                        webViewRef?.evaluateJavascript(
                            "document.querySelector('video')?.pause();",
                            null
                        )

                        songs.clear()
                        songs.addAll(selectedList)
                        savePlaylist(context, selectedList)

                        currentIndex = 0
                        errorMessage = null
                        isLoading = true
                        player.stop()
                        player.clearMediaItems()
                        player.setMediaItems(selectedList.map { it.toMediaItem() })
                        player.prepare()
                        player.play()

                        // Chuyển sang hiển thị List bài hát thay vì Equalizer.
                        // Không quét ảnh bìa hàng loạt cho Google Drive để playlist lớn vẫn tải nhanh.
                        showEqualizer = false
                    }
                    showAudioSheet = false
                }
            )
        }

        songToCut?.let { song ->
            SongCutterDialog(
                song = song,
                onDismiss = { songToCut = null },
                onCutConfirm = { _, _ ->
                    Toast.makeText(context, strings.saveSuccess(), Toast.LENGTH_SHORT).show()
                    requestMusicScan()
                }
            )
        }

        songToMerge?.let { baseSong ->
            AudioMergeDialog(
                baseSong = baseSong,
                availableSongs = songs,
                onDismiss = { songToMerge = null },
                onMergeSuccess = {
                    songToMerge = null
                    Toast.makeText(context, strings.saveSuccess(), Toast.LENGTH_SHORT).show()
                    requestMusicScan()
                }
            )
        }

        // HỘP THOẠI CÀI ĐẶT
        if (showSettingsDialog) {
            SettingsDialog(
                onDismiss = { showSettingsDialog = false },
                onOpenLanguage = {
                    showSettingsDialog = false
                    showLanguageDialog = true
                },
                onOpenTheme = {
                    showSettingsDialog = false
                    showThemeDialog = true
                },
                onOpenPrivacy = {
                    showSettingsDialog = false
                    showPrivacyDialog = true
                },
                onOpenLogin = {
                    showSettingsDialog = false
                    if (currentUserEmail != null) {
                        googleSignInClient.signOut().addOnCompleteListener {
                            currentUserEmail = null
                            context.getSharedPreferences("music_app_prefs", Context.MODE_PRIVATE)
                                .edit().remove("logged_in_email").apply()
                            Toast.makeText(context, "Đã đăng xuất tài khoản", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        googleAuthLauncher.launch(googleSignInClient.signInIntent)
                    }
                },
                currentUserEmail = currentUserEmail,
                onOpenAdmin = {
                    showSettingsDialog = false
                    showAdminDialog = true
                }
            )
        }

        // HỘP THOẠI ADMIN
        if (showAdminDialog) {
            AdminDashboardDialog(onDismiss = { showAdminDialog = false })
        }

        // HỘP THOẠI 15 NGÔN NGỮ
        if (showLanguageDialog) {
            LanguageSelectionDialog(
                currentLang = currentLang,
                onLanguageSelect = { newLang ->
                    onLanguageChange(newLang)
                    showLanguageDialog = false
                },
                onDismiss = { showLanguageDialog = false }
            )
        }

        // HỘP THOẠI CHẾ ĐỘ SÁNG / TỐI
        if (showThemeDialog) {
            ThemeSelectionDialog(
                currentMode = themeMode,
                onThemeSelect = { newMode ->
                    onThemeChange(newMode)
                    showThemeDialog = false
                },
                onDismiss = { showThemeDialog = false }
            )
        }

        // HỘP THOẠI CHÍNH SÁCH BẢO MẬT
        if (showPrivacyDialog) {
            PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
        }


        // VIEW FULLSCREEN TRỰC TIẾP TRÊN ROOT (BỎ DIALOG ĐỂ TRÁNH LỖI TAI THỎ)
        if (fullScreenView != null) {
            val activity = LocalContext.current as? Activity

            // Chặn phím Back vật lý để thoát Fullscreen
            androidx.activity.compose.BackHandler {
                fullScreenCallback?.onCustomViewHidden()
            }

            // Tự động ẩn thanh trạng thái và điều hướng khi phóng to
            DisposableEffect(Unit) {
                val window = activity?.window
                if (window != null) {
                    val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                    insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                }
                onDispose {
                    if (window != null) {
                        val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                        insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        android.widget.FrameLayout(ctx).apply {
                            setBackgroundColor(android.graphics.Color.BLACK)
                            (fullScreenView?.parent as? android.view.ViewGroup)?.removeView(fullScreenView)
                            addView(fullScreenView, android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = { fullScreenCallback?.onCustomViewHidden() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 48.dp, end = 24.dp) // Căn lề an toàn tránh đè vào camera
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(androidx.compose.material.icons.Icons.Rounded.Close, contentDescription = "Thoát Fullscreen", tint = Color.White)
                }
            }
        }

    }
}

@Composable
private fun PlayerTopBar(
    onSearch: (String) -> Unit,
    onYoutubeSearch: (String) -> Unit,
    isVideoSearchMode: Boolean,
    onToggleVideoSearchMode: () -> Unit,
    onSettingsClick: () -> Unit,
    showTvIcon: Boolean
) {
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val palette = LocalPalette.current
    val strings = LocalStrings.current

    // THÊM: Khai báo trình điều khiển bàn phím ảo
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    val executeSearch = { query: String ->
        if (query.isNotBlank()) {
            // Ẩn bàn phím ngay sau khi bấm tìm kiếm
            keyboardController?.hide()
            // Chỉ nạp danh sách nhạc bên dưới, chờ người dùng click chọn bài
            onSearch(query)
        }
    }

    val voiceSearchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val spokenText = matches[0]
                searchQuery = spokenText
                executeSearch(spokenText)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(14.dp),
            color = palette.cardRaised,
            border = BorderStroke(1.dp, palette.borderSoft)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Tìm kiếm",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(18.dp)
                )

                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = TextStyle(
                        color = palette.textPrimary,
                        fontSize = 14.sp
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(if (palette.isDark) Color.White else Color.Black),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { executeSearch(searchQuery) }
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = strings.searchPlaceholder(),
                                    color = palette.textMuted,
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Xóa từ khóa",
                            tint = palette.textSecondary
                        )
                    }
                }

                if (showTvIcon) {
                    IconButton(
                        onClick = onToggleVideoSearchMode,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Rounded.Tv,
                            contentDescription = "Chế độ Video YouTube",
                            tint = if (isVideoSearchMode) palette.purpleBright else palette.textSecondary
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, strings.voiceLocale())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, strings.voicePrompt())
                        }
                        try {
                            voiceSearchLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, strings.voiceNotSupported(), Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = "Tìm kiếm bằng giọng nói",
                        tint = palette.textSecondary
                    )
                }
            }
        }

        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = strings.settings(),
                tint = palette.textPrimary,
            )
        }
    }
}

@Composable
private fun NowPlayingSection(
    song: SongItem,
    isPlaying: Boolean,
    isLoading: Boolean,
    positionMs: Long,
    durationMs: Long,
    favorite: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    showEqualizer: Boolean,
    isYoutubeMode: Boolean,
    youtubeSearchQuery: String,
    onWebViewCreated: (android.webkit.WebView) -> Unit,
    onEnterFullScreen: (android.view.View, android.webkit.WebChromeClient.CustomViewCallback) -> Unit,
    onExitFullScreen: () -> Unit,
    onFavorite: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleEqualizer: () -> Unit,
    onOpenFolder: () -> Unit,
    onArtworkClick: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (this.maxWidth < 390.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlbumArtwork(
                        song = song,
                        favorite = favorite,
                        onFavorite = onFavorite,
                        isYoutubeMode = isYoutubeMode,
                        youtubeSearchQuery = youtubeSearchQuery,
                        onWebViewCreated = onWebViewCreated,
                        onEnterFullScreen = onEnterFullScreen,
                        onExitFullScreen = onExitFullScreen,
                        modifier = Modifier.size(124.dp),
                        onArtworkClick = onArtworkClick,
                    )
                    TrackMetadata(
                        song = song,
                        showEqualizer = showEqualizer,
                        onToggleEqualizer = onToggleEqualizer,
                        onOpenFolder = onOpenFolder,
                        modifier = Modifier.weight(1f),
                    )
                }
                ProgressAndControls(
                    isPlaying = isPlaying,
                    isLoading = isLoading,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    shuffleEnabled = shuffleEnabled,
                    repeatMode = repeatMode,
                    onShuffle = onShuffle,
                    onRepeat = onRepeat,
                    onPrevious = onPrevious,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onSeek = onSeek,
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumArtwork(
                    song = song,
                    favorite = favorite,
                    onFavorite = onFavorite,
                    isYoutubeMode = isYoutubeMode,
                    youtubeSearchQuery = youtubeSearchQuery,
                    onWebViewCreated = onWebViewCreated,
                    onEnterFullScreen = onEnterFullScreen,
                    onExitFullScreen = onExitFullScreen,
                    modifier = Modifier.size(142.dp),
                    onArtworkClick = onArtworkClick,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TrackMetadata(
                        song = song,
                        showEqualizer = showEqualizer,
                        onToggleEqualizer = onToggleEqualizer,
                        onOpenFolder = onOpenFolder
                    )
                    ProgressAndControls(
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        shuffleEnabled = shuffleEnabled,
                        repeatMode = repeatMode,
                        onShuffle = onShuffle,
                        onRepeat = onRepeat,
                        onPrevious = onPrevious,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        onSeek = onSeek,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumArtwork(
    song: SongItem,
    favorite: Boolean,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    isYoutubeMode: Boolean = false,
    youtubeSearchQuery: String = "",
    onWebViewCreated: (android.webkit.WebView) -> Unit = {},
    onEnterFullScreen: (android.view.View, android.webkit.WebChromeClient.CustomViewCallback) -> Unit = {_,_->},
    onExitFullScreen: () -> Unit = {},
    onArtworkClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val palette = LocalPalette.current

    var isFullscreen by remember { mutableStateOf(false) }
    var lastSearchedQuery by remember { mutableStateOf("") }

    val webView = remember {
        android.webkit.WebView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.BLACK)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            // Đã gỡ bỏ setOnTouchListener cứng ở đây để điều khiển linh hoạt bên dưới

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                userAgentString = "Mozilla/5.0"
            }

            webChromeClient = object : android.webkit.WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                    android.util.Log.d("WEBVIEW_JS", "Console: ${consoleMessage?.message()}")
                    return super.onConsoleMessage(consoleMessage)
                }
            }
            webViewClient = object : android.webkit.WebViewClient() {
                override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?): Boolean = false

                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val js = """
                        javascript:(function() {
                            var style = document.createElement('style');
                            style.id = 'custom-yt-style';
                            style.innerHTML = `
                                body, html { width: 100% !important; height: 100% !important; background-color: black !important; margin: 0 !important; padding: 0 !important; overflow: hidden !important; }
                                
                                /* Ẩn TRIỆT ĐỂ toàn bộ Logo Youtube, Avatar, Tiêu đề, Nút Share, Watermark */
                                .ytp-chrome-top, .ytp-watermark, .ytp-youtube-button, .ytp-title-channel, 
                                .ytp-title-text, .ytp-title, .ytp-title-channel-logo, .ytp-gradient-top, 
                                .ytp-paid-content-overlay, .ytm-paid-content-overlay, .ytp-ad-overlay-container, 
                                .ytm-custom-ad-banner, .annotation, .ytp-ce-element, .ytp-impression-link,
                                .ytp-button.ytp-share-button, .ytp-button.ytp-watch-later-button, .branding-img { 
                                    display: none !important; opacity: 0 !important; visibility: hidden !important; pointer-events: none !important; 
                                }
                                
                                .html5-video-player { position: absolute !important; top: 0 !important; left: 0 !important; width: 100% !important; height: 100% !important; z-index: 9999 !important; background: black !important; }
                                
                                /* Khi ở chế độ Nhỏ mặc định (Ẩn thanh tiến trình & Pause Overlay) */
                                body:not(.fullscreen) .ytp-chrome-bottom, 
                                body:not(.fullscreen) .ytp-gradient-bottom, 
                                body:not(.fullscreen) .ytp-pause-overlay { display: none !important; opacity: 0 !important; visibility: hidden !important; }
                                body:not(.fullscreen) video { object-fit: cover !important; width: 100% !important; height: 100% !important; position: absolute !important; top: 0 !important; left: 0 !important; z-index: 9999 !important; pointer-events: none !important; }
                                
                                /* Khi ở chế độ Fullscreen (Cho phép vuốt Controls) */
                                body.fullscreen video { object-fit: contain !important; width: 100% !important; height: 100% !important; position: absolute !important; top: 0 !important; left: 0 !important; z-index: 1 !important; pointer-events: auto !important; }
                            `;
                            document.head.appendChild(style);
                            
                            Object.defineProperty(document, 'visibilityState', {value: 'visible', writable: true});
                            Object.defineProperty(document, 'hidden', {value: false, writable: true});
                            
                            var videoSetup = false;

                            setInterval(function() {
                                var v = document.querySelector('video');
                                if (!v) return;
                                
                                // Nếu người dùng tự Play/Pause trên thanh công cụ của Youtube trong màn hình lớn thì phải ghi nhận lại để không bị code tự động cưỡng chế play đè lên
                                if (!videoSetup) {
                                    v.addEventListener('pause', function() {
                                        if (document.body.classList.contains('fullscreen')) v.dataset.userPaused = 'true';
                                    });
                                    v.addEventListener('play', function() {
                                        if (document.body.classList.contains('fullscreen')) v.dataset.userPaused = 'false';
                                    });
                                    videoSetup = true;
                                }

                                if (v.paused && v.dataset.userPaused !== 'true') {
                                    var largePlayBtn = document.querySelector('.ytp-large-play-button');
                                    var smallPlayBtn = document.querySelector('.ytp-play-button');
                                    if (largePlayBtn && largePlayBtn.style.display !== 'none') largePlayBtn.click();
                                    else if (smallPlayBtn) smallPlayBtn.click();
                                    var p = v.play();
                                    if (p !== undefined) p.catch(e => {});
                                }
                                
                                var skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytm-autonav-promo-close-button');
                                if (skipBtn) skipBtn.click(); 
                                else {
                                    var adShowing = document.querySelector('.ad-showing');
                                    if (adShowing) { v.playbackRate = 16.0; v.muted = true; } 
                                    else { if (v.playbackRate === 16.0) { v.playbackRate = 1.0; v.muted = false; } }
                                }
                            }, 500);
                        })();
                    """.trimIndent()
                    view?.evaluateJavascript(js, null)
                }
            }
            onWebViewCreated(this)
        }
    }

    // Đẩy tín hiệu Class Fullscreen vào JS và cấu hình Sự Kiện Chạm
    LaunchedEffect(isFullscreen) {
        if (isFullscreen) {
            webView.evaluateJavascript("document.body.classList.add('fullscreen');", null)
            // Mở khóa sự kiện chạm khi phóng to để có thể vuốt tiến trình YouTube
            webView.setOnTouchListener(null)
        } else {
            webView.evaluateJavascript("document.body.classList.remove('fullscreen');", null)
            // Khóa chặn chạm khi thu nhỏ để không bị ấn nhầm làm khựng video
            webView.setOnTouchListener { _, _ -> true }
        }
    }

    LaunchedEffect(youtubeSearchQuery, isYoutubeMode) {
        if (isYoutubeMode && youtubeSearchQuery.isNotBlank() && youtubeSearchQuery != lastSearchedQuery) {
            lastSearchedQuery = youtubeSearchQuery

            // Xử lý bắt ID cho cả link watch?v= VÀ youtu.be để chuyển đổi sang trình phát embed sạch
            val videoIdMatch = Regex("(?<=v=|v\\/|vi=|vi\\/|youtu.be\\/)[a-zA-Z0-9_-]{11}").find(youtubeSearchQuery)

            val targetUrl = if (videoIdMatch != null) {
                val videoId = videoIdMatch.value
                // Bổ sung tham số modestbranding=1 để xóa Logo Youtube
                "https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1&controls=1&showinfo=0&rel=0&modestbranding=1&enablejsapi=1"
            } else if (youtubeSearchQuery.startsWith("http")) {
                youtubeSearchQuery
            } else {
                "https://m.youtube.com/results?search_query=${java.net.URLEncoder.encode(youtubeSearchQuery, "UTF-8")}"
            }
            webView.loadUrl(targetUrl, mutableMapOf("Referer" to "http://ducphi.atwebpages.com/"))
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null, // Tắt hiệu ứng nhấn để người dùng không nhận ra
                onClick = onArtworkClick
            )
    ) {
        if (isYoutubeMode) {
            if (!isFullscreen) {
                AndroidView(
                    factory = { ctx ->
                        android.widget.FrameLayout(ctx).apply {
                            (webView.parent as? android.view.ViewGroup)?.removeView(webView)
                            addView(webView, android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clickable {
                        isFullscreen = true
                        // Truyền thẳng WebView lên lớp Root bao phủ toàn màn hình
                        onEnterFullScreen(webView, object : android.webkit.WebChromeClient.CustomViewCallback {
                            override fun onCustomViewHidden() {
                                isFullscreen = false
                                onExitFullScreen()
                            }
                        })
                    },
                shape = CircleShape,
                color = palette.card.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, palette.border),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Rounded.Fullscreen,
                        contentDescription = "Phóng to toàn màn hình",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(context)
                    .data(song.artworkUrl ?: song.artworkRes).crossfade(true)
                    .error(song.artworkRes).placeholder(song.artworkRes).build(),
                contentDescription = "Ảnh bìa bài hát",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (!isFullscreen) {
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).size(28.dp).clickable(onClick = onFavorite),
                shape = CircleShape,
                color = palette.card.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, palette.border),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Rounded.Favorite,
                        contentDescription = if (favorite) "Bỏ yêu thích" else "Yêu thích",
                        tint = if (favorite) palette.red else palette.textMuted,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackMetadata(
    song: SongItem,
    showEqualizer: Boolean,
    onToggleEqualizer: () -> Unit,
    onOpenFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDownloadDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val palette = LocalPalette.current
    val strings = LocalStrings.current

    val isOnlineSong = song.uri.toString().startsWith("http://") || song.uri.toString().startsWith("https://")

    Column(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = song.title,
                color = palette.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
            Text(
                text = song.artist,
                color = palette.textSecondary,
                fontSize = 15.sp,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = palette.purple.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, palette.purple.copy(alpha = 0.3f)),
            ) {
                Text(
                    text = song.formatBadge(),
                    color = palette.purpleBright,
                    fontSize = 11.sp, // Thu nhỏ text một chút để cân bằng với icon
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp), // Thu hẹp khoảng cách giữa các icon
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isOnlineSong) {
                    Surface(
                        modifier = Modifier
                            .size(38.dp) // Giảm kích thước vòng tròn chứa icon
                            .clickable { showDownloadDialog = true },
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, palette.border),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDownload,
                                contentDescription = "Tải bài hát online",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(20.dp) // Giảm kích thước icon bên trong
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .size(38.dp) // Giảm kích thước vòng tròn chứa icon
                        .clickable(onClick = onOpenFolder),
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, palette.border),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = "Mở thư mục nhạc",
                            tint = palette.textSecondary,
                            modifier = Modifier.size(20.dp) // Giảm kích thước icon bên trong
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .size(38.dp) // Giảm kích thước vòng tròn chứa icon
                        .clickable(onClick = onToggleEqualizer),
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, palette.border),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (showEqualizer) Icons.Rounded.FormatListBulleted else Icons.Rounded.GraphicEq,
                            contentDescription = if (showEqualizer) "Mở danh sách" else "Mở Equalizer",
                            tint = palette.purpleBright,
                            modifier = Modifier.size(20.dp) // Giảm kích thước icon bên trong
                        )
                    }
                }
            }
        }
    }

    if (showDownloadDialog) {
        val context = LocalContext.current
        Dialog(onDismissRequest = { showDownloadDialog = false }) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.card,
                border = BorderStroke(1.dp, palette.border)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = strings.downloadTitle(),
                        color = palette.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.downloadDesc(song.title),
                        color = palette.textSecondary,
                        fontSize = 14.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showDownloadDialog = false }) {
                            Text(strings.cancel(), color = palette.textSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showDownloadDialog = false
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val urlStr = song.uri.toString()
                                        if (urlStr.startsWith("http://") || urlStr.startsWith("https://")) {
                                            // Format lại tên file để có thêm tên ca sĩ
                                            val cleanTitle = song.title.replace(Regex("[\\\\/:*?\"<>|]"), "").trim()
                                            val cleanArtist = song.artist.replace(Regex("[\\\\/:*?\"<>|]"), "").trim()
                                            val fileName = "$cleanTitle - $cleanArtist.mp3"

                                            val values = ContentValues().apply {
                                                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                                                put(MediaStore.Audio.Media.TITLE, song.title)
                                                put(MediaStore.Audio.Media.ARTIST, song.artist)
                                                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                                                put(MediaStore.Audio.Media.IS_MUSIC, true)
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                    put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC)
                                                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                                                }
                                            }

                                            val resolver = context.contentResolver
                                            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                                            } else {
                                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                                            }

                                            val downloadedUri = resolver.insert(collection, values)
                                            if (downloadedUri != null) {
                                                URL(urlStr).openStream().use { input ->
                                                    resolver.openOutputStream(downloadedUri)?.use { output ->
                                                        input.copyTo(output)
                                                    }
                                                }

                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                    values.clear()
                                                    values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                                                    resolver.update(downloadedUri, values, null, null)
                                                }

                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, strings.downloadSuccess(), Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, strings.notOnlineFile(), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "${strings.downloadError()}${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.purple)
                        ) {
                            Text(strings.download(), color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressAndControls(
    isPlaying: Boolean,
    isLoading: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    val palette = LocalPalette.current
    val coroutineScope = rememberCoroutineScope()

    // Khai báo quản lý trạng thái kéo thanh trượt
    var isDragging by remember { mutableStateOf(false) }
    var draggedPosition by remember { mutableFloatStateOf(0f) }
    var seekJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val safeDuration = max(durationMs, 1L)
    val sliderPosition = if (isDragging) draggedPosition else positionMs.toFloat()

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = sliderPosition.coerceIn(0f, safeDuration.toFloat()),
            onValueChange = { newPos ->
                isDragging = true
                draggedPosition = newPos
                // FIX LỖI KÉO KHÔNG MƯỢT: Hủy bộ đếm thời gian cũ nếu người dùng vẫn đang kéo liên tục
                seekJob?.cancel()
            },
            onValueChangeFinished = {
                onSeek(draggedPosition.toLong())
                // FIX LỖI GIẬT LÙI: Chờ Youtube/ExoPlayer cập nhật xong vị trí mới buông cờ Dragging
                seekJob = coroutineScope.launch {
                    delay(1500) // Đóng băng thanh trượt 1.5 giây để chờ player thực sự load xong
                    isDragging = false
                }
            },
            valueRange = 0f..safeDuration.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = palette.purple,
                activeTrackColor = palette.purple,
                inactiveTrackColor = palette.borderSoft,
            ),
            modifier = Modifier.height(28.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatMillis(sliderPosition.toLong()),
                color = palette.textSecondary,
                fontSize = 12.sp,
            )
            Text(
                text = formatMillis(safeDuration),
                color = palette.textSecondary,
                fontSize = 12.sp,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SmallPlaybackButton(
                icon = Icons.Rounded.Shuffle,
                description = "Phát ngẫu nhiên",
                active = shuffleEnabled,
                onClick = onShuffle,
            )
            SmallPlaybackButton(
                icon = Icons.Rounded.SkipPrevious,
                description = "Bài trước",
                onClick = onPrevious,
            )
            MainPlayButton(
                isPlaying = isPlaying,
                isLoading = isLoading,
                onClick = onPlayPause,
            )
            SmallPlaybackButton(
                icon = Icons.Rounded.SkipNext,
                description = "Bài tiếp theo",
                onClick = onNext,
            )
            SmallPlaybackButton(
                icon = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                description = if (repeatMode == Player.REPEAT_MODE_ONE) "Lặp 1 bài" else "Lặp tất cả",
                active = repeatMode != Player.REPEAT_MODE_OFF,
                onClick = onRepeat,
            )
        }
    }
}
@Composable
private fun SmallPlaybackButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    active: Boolean = false,
) {
    val palette = LocalPalette.current
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(38.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (active) palette.purpleBright else palette.textSecondary,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun MainPlayButton(
    isPlaying: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    Box(
        modifier = Modifier
            .size(66.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(palette.purple, palette.blue),
                ),
                shape = CircleShape,
            )
            .padding(2.dp)
            .background(palette.background, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) "Tạm dừng" else "Phát",
            tint = if (isLoading && !isPlaying) palette.textSecondary else (if (palette.isDark) Color.White else palette.textPrimary),
            modifier = Modifier.size(38.dp),
        )
    }
}

@Composable
private fun ErrorBanner(message: String) {
    val palette = LocalPalette.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = palette.red.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, palette.red.copy(alpha = 0.35f)),
    ) {
        Text(
            text = message,
            color = palette.red,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun AudioSelectionSheet(
    foldersMap: Map<String, List<SongItem>>,
    driveFolderUri: String,
    driveFolderName: String,
    isScanningDrive: Boolean,
    onChooseDriveFolder: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (List<SongItem>) -> Unit
) {
    val palette = LocalPalette.current
    val strings = LocalStrings.current
    var currentFolder by remember { mutableStateOf<String?>(null) }
    val selectedSongs = remember { mutableStateListOf<SongItem>() }
    val selectedFolders = remember { mutableStateListOf<String>() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(18.dp),
            color = palette.backgroundSoft,
            border = BorderStroke(1.dp, palette.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (currentFolder == null) {
                    val selectedSongCount = selectedFolders
                        .flatMap { folderName -> foldersMap[folderName].orEmpty() }
                        .distinctBy { it.id }
                        .size

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.selectFolder(),
                            color = palette.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedSongCount > 0) {
                                TextButton(
                                    onClick = {
                                        val allSelectedSongs = selectedFolders
                                            .flatMap { foldersMap[it] ?: emptyList() }
                                            .distinctBy { it.id }
                                        onConfirm(allSelectedSongs)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = palette.purpleBright,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = strings.playCount(selectedSongCount),
                                        color = palette.purpleBright,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Rounded.Close, "Đóng", tint = palette.textSecondary)
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = palette.card,
                        border = BorderStroke(1.dp, palette.borderSoft)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudDownload,
                                    contentDescription = null,
                                    tint = palette.purpleBright,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.googleDriveFolder(),
                                        color = palette.textPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (driveFolderName.isNotBlank()) {
                                        Text(
                                            text = driveFolderName,
                                            color = palette.textSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Text(
                                text = strings.driveFolderPath(),
                                color = palette.textSecondary,
                                fontSize = 12.sp
                            )

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = palette.backgroundSoft,
                                border = BorderStroke(1.dp, palette.borderSoft)
                            ) {
                                BasicTextField(
                                    value = driveFolderUri,
                                    onValueChange = {},
                                    readOnly = true,
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = palette.textPrimary,
                                        fontSize = 12.sp
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 11.dp),
                                    decorationBox = { innerTextField ->
                                        Box(contentAlignment = Alignment.CenterStart) {
                                            if (driveFolderUri.isBlank()) {
                                                Text(
                                                    text = strings.driveFolderHint(),
                                                    color = palette.textMuted,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )
                            }

                            Button(
                                onClick = onChooseDriveFolder,
                                enabled = !isScanningDrive,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.purple,
                                    disabledContainerColor = palette.cardRaised
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isScanningDrive) {
                                        strings.scanningDrive()
                                    } else {
                                        strings.chooseGoogleDriveFolder()
                                    },
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(palette.borderSoft)
                    )

                    if (foldersMap.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isScanningDrive) strings.scanningDrive() else strings.noAudioFolders(),
                                color = palette.textSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(foldersMap.keys.toList()) { folderName ->
                                val folderSongs = foldersMap[folderName].orEmpty()
                                val songCount = folderSongs.size
                                val isSelected = selectedFolders.contains(folderName)
                                val isCloudFolder = folderSongs.any { it.source == SongSource.SAF_CLOUD }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isSelected) selectedFolders.remove(folderName)
                                            else selectedFolders.add(folderName)
                                        }
                                        .padding(vertical = 10.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = palette.purple,
                                            uncheckedColor = palette.border
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))

                                    Icon(
                                        imageVector = if (isCloudFolder) {
                                            Icons.Rounded.CloudDownload
                                        } else {
                                            Icons.Rounded.Folder
                                        },
                                        contentDescription = null,
                                        tint = palette.purpleBright,
                                        modifier = Modifier.size(27.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = folderName,
                                            color = palette.textPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = strings.mediaFiles(songCount),
                                            color = palette.textSecondary,
                                            fontSize = 12.sp
                                        )
                                    }

                                    IconButton(onClick = { currentFolder = folderName }) {
                                        Icon(
                                            Icons.Rounded.ChevronRight,
                                            contentDescription = "Xem thư mục",
                                            tint = palette.textMuted
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(palette.borderSoft.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }
                } else {
                    val availableSongs = foldersMap[currentFolder].orEmpty()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { currentFolder = null }
                                .padding(end = 8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.ChevronRight,
                                "Quay lại",
                                tint = palette.textPrimary,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer(rotationZ = 180f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentFolder.orEmpty(),
                                color = palette.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 190.dp)
                            )
                        }

                        TextButton(
                            onClick = {
                                onConfirm(selectedSongs.distinctBy { it.id })
                            },
                            enabled = selectedSongs.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = if (selectedSongs.isNotEmpty()) {
                                    palette.purpleBright
                                } else {
                                    palette.textMuted
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.playCount(selectedSongs.size),
                                color = if (selectedSongs.isNotEmpty()) {
                                    palette.purpleBright
                                } else {
                                    palette.textMuted
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selectedSongs.containsAll(availableSongs)) {
                                    selectedSongs.removeAll(availableSongs)
                                } else {
                                    selectedSongs.addAll(
                                        availableSongs.filter { !selectedSongs.contains(it) }
                                    )
                                }
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        Checkbox(
                            checked = availableSongs.isNotEmpty() &&
                                    selectedSongs.containsAll(availableSongs),
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = palette.purple,
                                uncheckedColor = palette.border
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            strings.selectAll(),
                            color = palette.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(palette.borderSoft)
                    )

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(availableSongs, key = { it.uri.toString() }) { song ->
                            val isSelected = selectedSongs.contains(song)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isSelected) selectedSongs.remove(song)
                                        else selectedSongs.add(song)
                                    }
                                    .padding(vertical = 11.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = palette.purple,
                                        uncheckedColor = palette.border
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        color = palette.textPrimary,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${song.artist} • ${song.durationLabel}",
                                        color = palette.textSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (song.source == SongSource.SAF_CLOUD) {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudDownload,
                                        contentDescription = "Google Drive",
                                        tint = palette.textMuted,
                                        modifier = Modifier.size(18.dp)
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

private fun createDemoSongs(context: Context): List<SongItem> {
    val uri = try {
        Uri.parse("android.resource://${context.packageName}/raw/chill_with_you_demo")
    } catch (e: Exception) {
        Uri.EMPTY
    }
    return listOf(
        SongItem(
            id = "demo-1",
            title = "Chill With You (Demo Offline)",
            artist = "Lofi Star",
            uri = uri,
            durationLabel = "4:21",
            source = SongSource.APP_RESOURCE,
            mimeType = "audio/mpeg"
        )
    )
}
private fun savePlaylist(context: Context, songs: List<SongItem>) {
    val prefs = context.getSharedPreferences("music_app_prefs", Context.MODE_PRIVATE)
    val jsonArray = JSONArray()
    for (song in songs) {
        val jsonObject = JSONObject().apply {
            put("id", song.id)
            put("title", song.title)
            put("artist", song.artist)
            put("uri", song.uri.toString())
            put("durationLabel", song.durationLabel)
            put("artworkUrl", song.artworkUrl ?: "")
            put("source", song.source.name)
            put("mimeType", song.mimeType ?: "")
        }
        jsonArray.put(jsonObject)
    }
    prefs.edit().putString("saved_playlist", jsonArray.toString()).apply()
}

private fun loadPlaylist(context: Context): List<SongItem>? {
    val prefs = context.getSharedPreferences("music_app_prefs", Context.MODE_PRIVATE)
    val jsonString = prefs.getString("saved_playlist", null) ?: return null
    return try {
        val jsonArray = JSONArray(jsonString)
        val list = mutableListOf<SongItem>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val artworkUrl = obj.optString("artworkUrl", "")

            var rawTitle = obj.getString("title")
            var rawArtist = obj.getString("artist")

            // Xử lý tự động tách Tên Bài Hát và Ca Sĩ cho những file đã lưu sẵn trong máy bị dính liền
            if (rawTitle.contains(" - ")) {
                val parts = rawTitle.split(" - ", limit = 2)
                rawTitle = parts[0].trim()
                rawArtist = parts[1].trim()
            }

            rawTitle = removeSupportedMediaExtension(rawTitle)
            rawArtist = removeSupportedMediaExtension(rawArtist)

            if (rawArtist.isBlank() || rawArtist.contains("unknown", ignoreCase = true)) {
                rawArtist = "Nghệ sĩ chưa biết"
            }

            val id = obj.getString("id")
            val uri = Uri.parse(obj.getString("uri"))
            val source = runCatching {
                SongSource.valueOf(obj.optString("source"))
            }.getOrElse {
                inferSongSource(id, uri)
            }
            val mimeType = obj.optString("mimeType", "").takeIf { it.isNotBlank() }

            list.add(
                SongItem(
                    id = id,
                    title = rawTitle,
                    artist = rawArtist,
                    uri = uri,
                    durationLabel = obj.optString("durationLabel", "--:--"),
                    artworkRes = R.drawable.album_cover,
                    artworkUrl = if (artworkUrl.isNotEmpty()) artworkUrl else null,
                    source = source,
                    mimeType = mimeType
                )
            )
        }
        if (list.isNotEmpty()) list else null
    } catch (e: Exception) {
        null
    }
}
private fun SongItem.toMediaItem(): MediaItem {
    val builder = MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)

    mimeType?.takeIf { it.isNotBlank() }?.let { builder.setMimeType(it) }

    return builder
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setArtworkUri(artworkUrl?.let { Uri.parse(it) })
                .build(),
        )
        .build()
}

private fun SongItem.formatBadge(): String {
    if (source == SongSource.SAF_CLOUD) {
        val extension = extensionOf(uri.lastPathSegment.orEmpty())
        return if (extension.isNotBlank()) "DRIVE ${extension.uppercase(Locale.ROOT)}" else "DRIVE AUDIO"
    }

    val extension = extensionOf(uri.lastPathSegment.orEmpty())
    if (extension.isNotBlank()) return extension.uppercase(Locale.ROOT)

    return mimeType
        ?.substringAfter('/', "AUDIO")
        ?.substringBefore(';')
        ?.uppercase(Locale.ROOT)
        ?.take(16)
        ?: "AUDIO"
}

private fun formatMillis(milliseconds: Long): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0L) / 1000L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

@Composable
private fun SongListPanel(
    songs: List<SongItem>,
    currentSongId: String,
    isSearching: Boolean, // NHẬN THÊM BIẾN NÀY
    onSongSelected: (Int) -> Unit,
    onSongEdit: (Int, String, String) -> Unit,
    onSongCut: (SongItem) -> Unit,
    onSongMerge: (SongItem) -> Unit,
    onSongDelete: (SongItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalPalette.current
    val strings = LocalStrings.current
    var songToEdit by remember { mutableStateOf<Pair<Int, SongItem>?>(null) }
    var songToSetRingtone by remember { mutableStateOf<SongItem?>(null) }

    var currentPhoneRingtone by remember { mutableStateOf<Uri?>(null) }
    var currentNotificationRingtone by remember { mutableStateOf<Uri?>(null) }

    val refreshRingtones = {
        try {
            currentPhoneRingtone = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
            currentNotificationRingtone = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(Unit) {
        refreshRingtones()
    }

    val writeSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(context)) {
            Toast.makeText(context, "Đã cấp quyền, vui lòng chọn lại bài hát để cài nhạc chuông", Toast.LENGTH_LONG).show()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(18.dp),
        color = palette.card.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, palette.border),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    val isPlaying = song.id == currentSongId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSongSelected(index) }
                            .background(if (isPlaying) palette.purple.copy(alpha = 0.15f) else Color.Transparent)
                            .padding(vertical = 8.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.cardRaised),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(song.artworkUrl ?: song.artworkRes)
                                    .crossfade(true)
                                    .error(song.artworkRes)
                                    .placeholder(song.artworkRes)
                                    .build(),
                                contentDescription = "Ảnh bìa",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // CHỈ HIỆN SÓNG ÂM TRÊN ẢNH KHI: Bài này đang chọn VÀ đã load xong (không còn searching)
                            if (isPlaying && !isSearching) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.45f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    WaveLoadingIndicator(modifier = Modifier.height(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                color = if (isPlaying) palette.purpleBright else palette.textPrimary,
                                fontSize = 15.sp,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                            Text(
                                text = "${song.artist} • ${song.durationLabel}",
                                color = palette.textSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                        }

                        val isRingtone = currentPhoneRingtone?.toString() == song.uri.toString() || currentNotificationRingtone?.toString() == song.uri.toString()
                        if (isRingtone) {
                            IconButton(
                                onClick = { songToSetRingtone = song },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = "Nhạc chuông hiện tại",
                                    tint = palette.purpleBright,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Box {
                            var expanded by remember { mutableStateOf(false) }

                            IconButton(
                                onClick = { expanded = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "Tùy chọn",
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(palette.cardRaised)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = strings.editInfo(),
                                            color = palette.textPrimary,
                                            fontSize = 14.sp
                                        )
                                    },
                                    onClick = {
                                        expanded = false
                                        songToEdit = index to song
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = null,
                                            tint = palette.textSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )
                                if (song.source == SongSource.LOCAL_MEDIASTORE) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = strings.setAsRingtone(),
                                                color = palette.textPrimary,
                                                fontSize = 14.sp
                                            )
                                        },
                                        onClick = {
                                            expanded = false
                                            songToSetRingtone = song
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Rounded.NotificationsActive,
                                                contentDescription = null,
                                                tint = palette.textSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = strings.cutRingtone(),
                                                color = palette.textPrimary,
                                                fontSize = 14.sp
                                            )
                                        },
                                        onClick = {
                                            expanded = false
                                            onSongCut(song)
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Rounded.ContentCut,
                                                contentDescription = null,
                                                tint = palette.textSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = strings.mergeAudio(),
                                                color = palette.textPrimary,
                                                fontSize = 14.sp
                                            )
                                        },
                                        onClick = {
                                            expanded = false
                                            onSongMerge(song)
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Rounded.PlaylistAdd,
                                                contentDescription = null,
                                                tint = palette.textSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (song.source == SongSource.LOCAL_MEDIASTORE) {
                                                strings.deleteSong()
                                            } else {
                                                strings.removeFromList()
                                            },
                                            color = palette.red,
                                            fontSize = 14.sp
                                        )
                                    },
                                    onClick = {
                                        expanded = false
                                        onSongDelete(song)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = null,
                                            tint = palette.red,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            songToSetRingtone?.let { song ->
                Dialog(onDismissRequest = { songToSetRingtone = null }) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = palette.card,
                        border = BorderStroke(1.dp, palette.border)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = strings.selectRingtoneType(),
                                color = palette.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            val applyRingtone = { type: Int ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    if (Settings.System.canWrite(context)) {
                                        RingtoneManager.setActualDefaultRingtoneUri(context, type, song.uri)
                                        Toast.makeText(context, strings.setRingtoneSuccess(), Toast.LENGTH_SHORT).show()
                                        refreshRingtones()
                                    } else {
                                        Toast.makeText(context, strings.requireWriteSettings(), Toast.LENGTH_LONG).show()
                                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        writeSettingsLauncher.launch(intent)
                                    }
                                } else {
                                    RingtoneManager.setActualDefaultRingtoneUri(context, type, song.uri)
                                    Toast.makeText(context, strings.setRingtoneSuccess(), Toast.LENGTH_SHORT).show()
                                    refreshRingtones()
                                }
                                songToSetRingtone = null
                            }

                            val restoreRingtone = { type: Int ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
                                    Toast.makeText(context, strings.requireWriteSettings(), Toast.LENGTH_LONG).show()
                                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    writeSettingsLauncher.launch(intent)
                                } else {
                                    try {
                                        // Sử dụng RingtoneManager để quét và lấy âm thanh mặc định số 0 (Original System Ringtone)
                                        val rm = RingtoneManager(context)
                                        rm.setType(type)
                                        val cursor = rm.cursor
                                        val defaultUri = if (cursor != null && cursor.count > 0) rm.getRingtoneUri(0) else null
                                        RingtoneManager.setActualDefaultRingtoneUri(context, type, defaultUri)

                                        Toast.makeText(context, strings.restoreDefaultSuccess(), Toast.LENGTH_SHORT).show()
                                        refreshRingtones()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    songToSetRingtone = null
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { applyRingtone(RingtoneManager.TYPE_RINGTONE) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = palette.purpleBright)
                                ) {
                                    Text(strings.phoneRingtone(), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                IconButton(
                                    onClick = { restoreRingtone(RingtoneManager.TYPE_RINGTONE) },
                                    modifier = Modifier.background(palette.cardRaised, CircleShape)
                                ) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Khôi phục gốc", tint = palette.red)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { applyRingtone(RingtoneManager.TYPE_NOTIFICATION) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = palette.blue)
                                ) {
                                    Text(strings.notificationRingtone(), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                IconButton(
                                    onClick = { restoreRingtone(RingtoneManager.TYPE_NOTIFICATION) },
                                    modifier = Modifier.background(palette.cardRaised, CircleShape)
                                ) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Khôi phục gốc", tint = palette.red)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { songToSetRingtone = null }) {
                                    Text(strings.cancel(), color = palette.textSecondary)
                                }
                            }
                        }
                    }
                }
            }

            songToEdit?.let { (index, song) ->
                var editTitle by remember { mutableStateOf(song.title) }
                var editArtist by remember { mutableStateOf(song.artist) }

                Dialog(onDismissRequest = { songToEdit = null }) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = palette.card,
                        border = BorderStroke(1.dp, palette.border)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = strings.editInfo(),
                                color = palette.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Column {
                                Text(strings.songTitle(), color = palette.textSecondary, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = palette.backgroundSoft,
                                    border = BorderStroke(1.dp, palette.borderSoft),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    BasicTextField(
                                        value = editTitle,
                                        onValueChange = { editTitle = it },
                                        textStyle = TextStyle(color = palette.textPrimary, fontSize = 14.sp),
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            Column {
                                Text(strings.artist(), color = palette.textSecondary, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = palette.backgroundSoft,
                                    border = BorderStroke(1.dp, palette.borderSoft),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    BasicTextField(
                                        value = editArtist,
                                        onValueChange = { editArtist = it },
                                        textStyle = TextStyle(color = palette.textPrimary, fontSize = 14.sp),
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { songToEdit = null }) {
                                    Text(strings.cancel(), color = palette.textSecondary)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(onClick = {
                                    onSongEdit(index, editTitle, editArtist)
                                    songToEdit = null
                                }) {
                                    Text(strings.save(), color = palette.purpleBright, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun saveCustomSongInfo(context: Context, id: String, title: String, artist: String) {
    val prefs = context.getSharedPreferences("music_app_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString("${id}_title", title).putString("${id}_artist", artist).apply()
}

private fun getCustomTitle(context: Context, id: String, defaultTitle: String): String {
    val prefs = context.getSharedPreferences("music_app_prefs", Context.MODE_PRIVATE)
    return prefs.getString("${id}_title", defaultTitle) ?: defaultTitle
}

private fun getCustomArtist(context: Context, id: String, defaultArtist: String): String {
    val prefs = context.getSharedPreferences("music_app_prefs", Context.MODE_PRIVATE)
    return prefs.getString("${id}_artist", defaultArtist) ?: defaultArtist
}

@Composable
private fun SongCutterDialog(
    song: SongItem,
    onDismiss: () -> Unit,
    onCutConfirm: (startTimeMs: Float, endTimeMs: Float) -> Unit
) {
    val context = LocalContext.current
    val palette = LocalPalette.current
    val strings = LocalStrings.current

    val durationParts = song.durationLabel.split(":")
    val totalDurationSec = if (durationParts.size == 2) {
        durationParts[0].toLongOrNull()?.times(60)?.plus(durationParts[1].toLongOrNull() ?: 0) ?: 0L
    } else {
        1L
    }
    val maxDurationMs = (totalDurationSec * 1000f).coerceAtLeast(1000f)

    var sliderPosition by remember { mutableStateOf(0f..maxDurationMs) }
    var isCutting by remember { mutableStateOf(false) }

    val previewPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(song.uri))
            prepare()
        }
    }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    DisposableEffect(previewPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isPreviewPlaying = isPlaying
            }
        }
        previewPlayer.addListener(listener)
        onDispose {
            previewPlayer.removeListener(listener)
            previewPlayer.release()
        }
    }

    LaunchedEffect(isPreviewPlaying, sliderPosition) {
        while (isPreviewPlaying) {
            if (previewPlayer.currentPosition >= sliderPosition.endInclusive.toLong()) {
                previewPlayer.pause()
                previewPlayer.seekTo(sliderPosition.start.toLong())
            }
            delay(100L)
        }
    }

    Dialog(onDismissRequest = {
        previewPlayer.pause()
        onDismiss()
    }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = palette.card,
            border = BorderStroke(1.dp, palette.borderSoft),
            shadowElevation = 16.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // HÀNG 1: Tiêu đề + Nút Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.cutRingtone(),
                            color = palette.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = song.title,
                            color = palette.purpleBright,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = {
                            previewPlayer.pause()
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Đóng",
                            tint = palette.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // HÀNG 2: Nút Play to, căn giữa
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            if (isPreviewPlaying) {
                                previewPlayer.pause()
                            } else {
                                previewPlayer.seekTo(sliderPosition.start.toLong())
                                previewPlayer.play()
                            }
                        },
                        modifier = Modifier
                            .size(76.dp)
                            .background(palette.purpleBright, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPreviewPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Nghe thử",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // HÀNG 3: Thanh trượt RangeSlider chuyên nghiệp
                Column {
                    RangeSlider(
                        value = sliderPosition,
                        onValueChange = {
                            sliderPosition = it
                            previewPlayer.seekTo(it.start.toLong())
                        },
                        valueRange = 0f..maxDurationMs,
                        colors = SliderDefaults.colors(
                            thumbColor = palette.purpleBright,
                            activeTrackColor = palette.purpleBright,
                            inactiveTrackColor = palette.borderSoft
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatMillis(sliderPosition.start.toLong()),
                            color = palette.textSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = formatMillis(sliderPosition.endInclusive.toLong()),
                            color = palette.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // HÀNG 4: Block hiển thị thời lượng cắt
                Surface(
                    color = palette.backgroundSoft.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = strings.cutFromTo(
                                formatMillis(sliderPosition.start.toLong()),
                                formatMillis(sliderPosition.endInclusive.toLong())
                            ),
                            color = palette.purpleBright,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.totalDuration(
                                formatMillis((sliderPosition.endInclusive - sliderPosition.start).toLong())
                            ),
                            color = palette.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // HÀNG 5: Block hiển thị đường dẫn lưu
                val cleanTitle = song.title.replace(Regex("[\\\\/:*?\"<>|]"), "")
                val cutFileName = "${cleanTitle}_cut.mp3"
                val expectedPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    "Thư mục Music/Cuts/$cutFileName"
                } else {
                    "Thư mục Music/$cutFileName"
                }

                Surface(
                    color = palette.backgroundSoft.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = strings.savedAt(),
                            color = palette.textSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = expectedPath,
                            color = palette.textPrimary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // HÀNG 6: Nút Lưu (Đã xóa nút Hủy)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            isCutting = true
                            previewPlayer.pause()

                            val path = cutAudioFile(
                                context = context,
                                sourceUri = song.uri,
                                title = song.title,
                                startTimeMs = sliderPosition.start.toLong(),
                                endTimeMs = sliderPosition.endInclusive.toLong(),
                                totalDurationMs = maxDurationMs.toLong()
                            )

                            if (path != null) {
                                onCutConfirm(sliderPosition.start, sliderPosition.endInclusive)
                            } else {
                                Toast.makeText(context, strings.cutError(), Toast.LENGTH_SHORT).show()
                            }
                            isCutting = false
                        },
                        enabled = !isCutting,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.purpleBright,
                            contentColor = Color.White,
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = if (isCutting) strings.cutting() else strings.saveCut(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

private fun cutAudioFile(
    context: Context,
    sourceUri: Uri,
    title: String,
    startTimeMs: Long,
    endTimeMs: Long,
    totalDurationMs: Long
): String? {
    try {
        val cleanTitle = title.replace(Regex("[\\\\/:*?\"<>|]"), "")
        val cutFileName = "${cleanTitle}_cut.mp3"

        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, cutFileName)
            put(MediaStore.Audio.Media.TITLE, "$title (Cut)")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            put(MediaStore.Audio.Media.IS_RINGTONE, true)
            put(MediaStore.Audio.Media.IS_MUSIC, true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/Cuts")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val audioCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val newUri = resolver.insert(audioCollection, values) ?: throw Exception("Không thể tạo Uri mới qua MediaStore")

        resolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
            FileInputStream(pfd.fileDescriptor).use { fis ->
                resolver.openOutputStream(newUri)?.use { fos: OutputStream ->
                    val totalSize = fis.channel.size()
                    val durationMs = if (endTimeMs > startTimeMs) endTimeMs - startTimeMs else 1L
                    val safeTotal = if (totalDurationMs > 0) totalDurationMs else durationMs

                    val startByte = ((startTimeMs.toFloat() / safeTotal) * totalSize).toLong()
                    val lengthToCopy = ((durationMs.toFloat() / safeTotal) * totalSize).toLong()

                    fis.channel.position(startByte)

                    val buffer = ByteArray(8192)
                    var bytesCopied = 0L
                    var read: Int

                    while (fis.read(buffer).also { read = it } != -1) {
                        if (bytesCopied + read > lengthToCopy) {
                            fos.write(buffer, 0, (lengthToCopy - bytesCopied).toInt())
                            break
                        }
                        fos.write(buffer, 0, read)
                        bytesCopied += read
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Audio.Media.IS_PENDING, 0)
            resolver.update(newUri, values, null, null)
        }

        val displayPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "Thư mục Music/Cuts/$cutFileName"
        } else {
            "Thư mục Music/$cutFileName (Bộ nhớ máy)"
        }

        return displayPath

    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

// ==========================================
// 3. DIALOGS: CÀI ĐẶT, NGÔN NGỮ, THEME, BẢO MẬT, PREMIUM
// ==========================================

@Composable
private fun SettingsDialog(
    onDismiss: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenLogin: () -> Unit,
    onOpenAdmin: () -> Unit,
    currentUserEmail: String? = null
) {
    val palette = LocalPalette.current
    val strings = LocalStrings.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.card,
            border = BorderStroke(1.dp, palette.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.settings(),
                        color = palette.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Đóng",
                            tint = palette.textSecondary
                        )
                    }
                }

                SettingsItem(
                    icon = Icons.Rounded.Language,
                    text = strings.language(),
                    onClick = onOpenLanguage
                )
                SettingsItem(
                    icon = Icons.Rounded.DarkMode,
                    text = strings.themeMode(),
                    onClick = onOpenTheme
                )
                SettingsItem(
                    icon = Icons.Rounded.Security,
                    text = strings.privacyPolicy(),
                    onClick = onOpenPrivacy
                )
                SettingsItem(
                    icon = androidx.compose.material.icons.Icons.Rounded.AccountCircle,
                    text = if (currentUserEmail != null) strings.logout(currentUserEmail) else strings.login(),
                    onClick = onOpenLogin
                )
                if (currentUserEmail.equals("ducphi0491@gmail.com", ignoreCase = true)) {
                    SettingsItem(
                        icon = androidx.compose.material.icons.Icons.Rounded.AdminPanelSettings,
                        text = strings.admin(),
                        onClick = onOpenAdmin
                    )
                }

                // Hiển thị phiên bản ứng dụng
                val context = LocalContext.current
                val versionName = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                } catch (e: Exception) {
                    "0.0"
                }

                Text(
                    text = "Version $versionName",
                    color = palette.textMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    isPremium: Boolean = false
) {
    val palette = LocalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = if (isPremium) palette.purpleBright else palette.textSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            color = if (isPremium) palette.purpleBright else palette.textPrimary,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = palette.textMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun LanguageSelectionDialog(
    currentLang: String,
    onLanguageSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalPalette.current
    val strings = LocalStrings.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f),
            shape = RoundedCornerShape(18.dp),
            color = palette.card,
            border = BorderStroke(1.dp, palette.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.language(),
                        color = palette.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Đóng",
                            tint = palette.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(SupportedLanguages) { langItem ->
                        val isSelected = langItem.code == currentLang
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onLanguageSelect(langItem.code) }
                                .background(if (isSelected) palette.purple.copy(alpha = 0.15f) else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = langItem.nativeName,
                                    color = if (isSelected) palette.purpleBright else palette.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = langItem.name,
                                    color = palette.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Đã chọn",
                                    tint = palette.purpleBright,
                                    modifier = Modifier.size(20.dp)
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
private fun ThemeSelectionDialog(
    currentMode: Int, // 0: System, 1: Light, 2: Dark
    onThemeSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalPalette.current
    val strings = LocalStrings.current

    val themeOptions = listOf(
        Triple(2, "Chế độ Tối (Dark)", "Tối ưu pin và dịu mắt khi nghe nhạc ban đêm"),
        Triple(1, "Chế độ Sáng (Light)", "Giao diện thanh lịch, rõ nét dưới ánh sáng mạnh"),
        Triple(0, "Theo hệ thống (System)", "Tự động đổi màu theo thiết lập của thiết bị")
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.card,
            border = BorderStroke(1.dp, palette.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = strings.themeMode(),
                    color = palette.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                themeOptions.forEach { (mode, title, desc) ->
                    val isSelected = mode == currentMode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onThemeSelect(mode) }
                            .background(if (isSelected) palette.purple.copy(alpha = 0.12f) else Color.Transparent)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) palette.purpleBright else palette.textMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                color = if (isSelected) palette.purpleBright else palette.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Text(
                                text = desc,
                                color = palette.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancel(), color = palette.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    val palette = LocalPalette.current
    val strings = LocalStrings.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = palette.card,
            border = BorderStroke(1.dp, palette.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.privacyPolicy(),
                        color = palette.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Đóng",
                            tint = palette.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings.privacyText1(),
                        color = palette.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = strings.privacyText2(),
                        color = palette.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.purple),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(strings.gotIt(), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
private fun startMergingWithTransformer(
    context: Context,
    song1: SongItem, startMs1: Long, endMs1: Long,
    song2: SongItem, startMs2: Long, endMs2: Long,
    onSuccess: (String) -> Unit,
    onError: (Exception) -> Unit
) {
    val cleanTitle1 = song1.title.replace(Regex("[\\\\/:*?\"<>|]"), "")
    val finalFileName = "${cleanTitle1}_merged.m4a" // Media3 xuất file audio chuẩn m4a

    // Tạo file tạm trong bộ nhớ đệm (cache) để Transformer ghi dữ liệu
    val outputFile = File(context.cacheDir, finalFileName)
    if (outputFile.exists()) outputFile.delete()

    // Cấu hình đoạn cắt của Bài 1
    val item1 = MediaItem.Builder()
        .setUri(song1.uri)
        .setClippingConfiguration(
            MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startMs1)
                .setEndPositionMs(endMs1)
                .build()
        ).build()
    val editedItem1 = EditedMediaItem.Builder(item1).build()

    // Cấu hình đoạn cắt của Bài 2
    val item2 = MediaItem.Builder()
        .setUri(song2.uri)
        .setClippingConfiguration(
            MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startMs2)
                .setEndPositionMs(endMs2)
                .build()
        ).build()
    val editedItem2 = EditedMediaItem.Builder(item2).build()

    // Đưa 2 bài hát vào một chuỗi nối tiếp nhau
    val sequence = EditedMediaItemSequence(editedItem1, editedItem2)
    val composition = Composition.Builder(listOf(sequence)).build()

    val transformer = Transformer.Builder(context)
        .addListener(object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                // Quá trình ghép hoàn tất, di chuyển file từ Cache ra thư mục Music
                try {
                    val values = ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, finalFileName)
                        put(MediaStore.Audio.Media.TITLE, "${song1.title} + ${song2.title}")
                        put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                        put(MediaStore.Audio.Media.IS_MUSIC, true)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/Merges")
                            put(MediaStore.Audio.Media.IS_PENDING, 1)
                        }
                    }

                    val resolver = context.contentResolver
                    val audioCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    } else {
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }

                    val newUri = resolver.insert(audioCollection, values)
                    if (newUri != null) {
                        resolver.openOutputStream(newUri)?.use { out ->
                            outputFile.inputStream().use { input ->
                                input.copyTo(out)
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            values.clear()
                            values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                            resolver.update(newUri, values, null, null)
                            onSuccess("Thư mục Music/Merges/$finalFileName")
                        } else {
                            onSuccess("Thư mục Music/$finalFileName")
                        }
                    } else {
                        onError(Exception("Không thể tạo dữ liệu trên MediaStore"))
                    }
                } catch (e: Exception) {
                    onError(e)
                } finally {
                    outputFile.delete() // Luôn dọn dẹp file rác trong cache
                }
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException
            ) {
                onError(exportException)
                outputFile.delete() // Dọn dẹp nếu có lỗi
            }
        })
        .build()

    // Bắt đầu quá trình xuất file
    transformer.start(composition, outputFile.absolutePath)
}

@Composable
private fun AudioMergeDialog(
    baseSong: SongItem,
    availableSongs: List<SongItem>,
    onDismiss: () -> Unit,
    onMergeSuccess: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalPalette.current
    val strings = LocalStrings.current

    var step by remember { mutableIntStateOf(1) }
    var selectedSong by remember { mutableStateOf<SongItem?>(null) }
    var isMerging by remember { mutableStateOf(false) }

    // Quản lý thời lượng và slider bài 1
    var maxMs1 by remember { mutableFloatStateOf(1000f) }
    var slider1 by remember { mutableStateOf(0f..1000f) }

    // Quản lý thời lượng và slider bài 2
    var maxMs2 by remember { mutableFloatStateOf(1000f) }
    var slider2 by remember { mutableStateOf(0f..1000f) }

    // Trình phát nhạc để nghe thử đoạn ghép
    val previewPlayer = remember {
        ExoPlayer.Builder(context).build()
    }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    // Quản lý tiến trình kết quả
    var previewPositionMs by remember { mutableLongStateOf(0L) }
    var previewTotalMs by remember { mutableLongStateOf(1L) }

    // Quản lý thao tác kéo Slider kết quả
    var isDraggingPreview by remember { mutableStateOf(false) }
    var draggedPreviewMs by remember { mutableFloatStateOf(0f) }

    DisposableEffect(previewPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isPreviewPlaying = isPlaying
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    isPreviewPlaying = false
                    previewPositionMs = previewTotalMs // Đặt max khi chạy xong
                }
            }
        }
        previewPlayer.addListener(listener)
        onDispose {
            previewPlayer.removeListener(listener)
            previewPlayer.release()
        }
    }

    LaunchedEffect(step, selectedSong) {
        if (step == 2 && selectedSong != null) {
            val parts1 = baseSong.durationLabel.split(":")
            val sec1 = if (parts1.size == 2) (parts1[0].toLongOrNull() ?: 0) * 60 + (parts1[1].toLongOrNull() ?: 0) else 1L
            maxMs1 = (sec1 * 1000f).coerceAtLeast(1000f)
            slider1 = 0f..maxMs1

            val parts2 = selectedSong!!.durationLabel.split(":")
            val sec2 = if (parts2.size == 2) (parts2[0].toLongOrNull() ?: 0) * 60 + (parts2[1].toLongOrNull() ?: 0) else 1L
            maxMs2 = (sec2 * 1000f).coerceAtLeast(1000f)
            slider2 = 0f..maxMs2
        }
    }

    // Tính toán thời gian khi nghe thử
    LaunchedEffect(isPreviewPlaying, slider1, slider2, step) {
        while (true) {
            val dur1 = (slider1.endInclusive - slider1.start).toLong()
            val dur2 = (slider2.endInclusive - slider2.start).toLong()
            previewTotalMs = (dur1 + dur2).coerceAtLeast(1L)

            if (isPreviewPlaying && !isDraggingPreview) {
                val idx = previewPlayer.currentMediaItemIndex
                val pos = previewPlayer.currentPosition

                if (idx == 0) {
                    previewPositionMs = pos.coerceIn(0L, dur1)
                } else if (idx == 1) {
                    previewPositionMs = (dur1 + pos).coerceIn(0L, previewTotalMs)
                }
            }
            kotlinx.coroutines.delay(50L) // Cập nhật mượt mà
        }
    }

    // Hàm khởi tạo Player cho Preview
    val preparePreviewPlayer = {
        selectedSong?.let { song2 ->
            previewPlayer.clearMediaItems()

            val item1 = MediaItem.Builder()
                .setUri(baseSong.uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(slider1.start.toLong())
                        .setEndPositionMs(slider1.endInclusive.toLong())
                        .build()
                ).build()

            val item2 = MediaItem.Builder()
                .setUri(song2.uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(slider2.start.toLong())
                        .setEndPositionMs(slider2.endInclusive.toLong())
                        .build()
                ).build()

            previewPlayer.addMediaItem(item1)
            previewPlayer.addMediaItem(item2)
            previewPlayer.repeatMode = Player.REPEAT_MODE_OFF
            previewPlayer.prepare()
        }
    }

    Dialog(onDismissRequest = {
        previewPlayer.pause()
        onDismiss()
    }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(22.dp),
            color = palette.card,
            border = BorderStroke(1.dp, palette.border)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // CẬP NHẬT: Thêm nút Close vào góc trên cùng bên phải
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.mergeAudio(),
                        color = palette.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = {
                            previewPlayer.pause()
                            onDismiss()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Đóng",
                            tint = palette.textSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (step == 1) {
                    Text(text = "1. ${baseSong.title}", color = palette.purpleBright, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = strings.selectToMerge(), color = palette.textSecondary, fontSize = 13.sp)

                    LazyColumn(modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .padding(vertical = 8.dp)
                    ) {
                        items(availableSongs) { song ->
                            if (song.id != baseSong.id) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedSong = song }
                                        .background(if (selectedSong == song) palette.purple.copy(alpha = 0.2f) else Color.Transparent)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = song.title,
                                        color = if (selectedSong == song) palette.purpleBright else palette.textPrimary,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            previewPlayer.pause()
                            onDismiss()
                        }) {
                            Text(strings.cancel(), color = palette.textSecondary)
                        }
                        Button(
                            onClick = { step = 2 },
                            enabled = selectedSong != null,
                            colors = ButtonDefaults.buttonColors(containerColor = palette.purpleBright)
                        ) {
                            Text(if (strings.lang == "vi") "Tiếp tục" else "Next")
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Slider Bài 1
                        Text(text = "1. ${baseSong.title}", color = palette.purpleBright, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        RangeSlider(
                            value = slider1,
                            onValueChange = {
                                slider1 = it
                                previewPositionMs = 0L // Reset preview khi thay đổi mốc
                                previewPlayer.pause()
                                previewPlayer.clearMediaItems() // Force rebuild
                            },
                            valueRange = 0f..maxMs1,
                            colors = SliderDefaults.colors(thumbColor = palette.purpleBright, activeTrackColor = palette.purpleBright)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatMillis(slider1.start.toLong()), color = palette.textSecondary, fontSize = 12.sp)
                            Text(formatMillis(slider1.endInclusive.toLong()), color = palette.textSecondary, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Slider Bài 2
                        Text(text = "2. ${selectedSong?.title}", color = palette.blue, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        RangeSlider(
                            value = slider2,
                            onValueChange = {
                                slider2 = it
                                previewPositionMs = 0L // Reset preview khi thay đổi mốc
                                previewPlayer.pause()
                                previewPlayer.clearMediaItems() // Force rebuild
                            },
                            valueRange = 0f..maxMs2,
                            colors = SliderDefaults.colors(thumbColor = palette.blue, activeTrackColor = palette.blue)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatMillis(slider2.start.toLong()), color = palette.textSecondary, fontSize = 12.sp)
                            Text(formatMillis(slider2.endInclusive.toLong()), color = palette.textSecondary, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Giao diện Slider hiển thị tiến trình tổng kết quả có phân chia màu
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (strings.lang == "vi") "Kết quả (Kéo để tua)" else "Result (Drag to seek)",
                                color = palette.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            val currentSliderValue = if (isDraggingPreview) draggedPreviewMs else previewPositionMs.toFloat()
                            val dur1 = (slider1.endInclusive - slider1.start)
                            val total = previewTotalMs.coerceAtLeast(1L).toFloat()
                            val fraction = (dur1 / total).coerceIn(0f, 1f)

                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {

                                // Canvas vẽ dải Track ảo với 2 màu phân biệt và dấu mốc
                                androidx.compose.foundation.Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp) // Căn chỉnh vừa vặn với Track thực tế của Slider
                                        .height(24.dp)
                                ) {
                                    val y = size.height / 2
                                    val trackHeight = 4.dp.toPx()
                                    val corner = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2, trackHeight / 2)

                                    // Vẽ dải màu bài 1 (Tím)
                                    drawRoundRect(
                                        color = palette.purpleBright.copy(alpha = 0.5f),
                                        topLeft = androidx.compose.ui.geometry.Offset(0f, y - trackHeight / 2),
                                        size = androidx.compose.ui.geometry.Size(size.width * fraction, trackHeight),
                                        cornerRadius = corner
                                    )
                                    // Vẽ dải màu bài 2 (Xanh)
                                    drawRoundRect(
                                        color = palette.blue.copy(alpha = 0.5f),
                                        topLeft = androidx.compose.ui.geometry.Offset(size.width * fraction, y - trackHeight / 2),
                                        size = androidx.compose.ui.geometry.Size(size.width * (1f - fraction), trackHeight),
                                        cornerRadius = corner
                                    )

                                    // Dấu chấm phân chia mốc giao tiếp giữa bài 1 và bài 2
                                    drawCircle(
                                        color = palette.textPrimary,
                                        radius = 3.5.dp.toPx(),
                                        center = androidx.compose.ui.geometry.Offset(size.width * fraction, y)
                                    )
                                }

                                Slider(
                                    value = currentSliderValue.coerceIn(0f, previewTotalMs.toFloat()),
                                    onValueChange = {
                                        isDraggingPreview = true
                                        draggedPreviewMs = it
                                    },
                                    onValueChangeFinished = {
                                        isDraggingPreview = false
                                        val targetMs = draggedPreviewMs.toLong()
                                        val dur1Long = (slider1.endInclusive - slider1.start).toLong()

                                        // Đảm bảo player đã được nạp tệp nếu kéo thanh trượt trước khi bấm nút Play
                                        if (previewPlayer.mediaItemCount == 0) {
                                            preparePreviewPlayer()
                                        }

                                        // Logic nhảy giây đúng bài hát (Bài 1 hay Bài 2)
                                        if (targetMs <= dur1Long) {
                                            previewPlayer.seekTo(0, targetMs)
                                        } else {
                                            previewPlayer.seekTo(1, targetMs - dur1Long)
                                        }
                                        previewPositionMs = targetMs
                                    },
                                    valueRange = 0f..previewTotalMs.toFloat(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = palette.textPrimary,
                                        activeTrackColor = palette.textPrimary, // Thanh đã chạy sẽ có màu sáng đè lên Canvas
                                        inactiveTrackColor = Color.Transparent // Ẩn track mặc định để thấy nền Canvas 2 màu bên dưới
                                    ),
                                    modifier = Modifier.height(24.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(formatMillis(currentSliderValue.toLong()), color = palette.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(formatMillis(previewTotalMs), color = palette.textSecondary, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Nút Play / Nghe thử kết quả ghép
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (isPreviewPlaying) {
                                        previewPlayer.pause()
                                    } else {
                                        if (previewPlayer.mediaItemCount == 0) {
                                            preparePreviewPlayer()
                                        }

                                        if (previewPositionMs >= previewTotalMs) {
                                            // Nếu đã phát xong mà bấm lại thì cho nghe lại từ đầu
                                            previewPositionMs = 0L
                                            previewPlayer.seekTo(0, 0L)
                                        }

                                        previewPlayer.playWhenReady = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = palette.purple),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPreviewPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isPreviewPlaying) (if (strings.lang == "vi") "Tạm dừng" else "Pause") else (if (strings.lang == "vi") "Nghe thử kết quả" else "Preview Mix"),
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Hiển thị đường dẫn lưu dự kiến
                    val cleanTitle1 = baseSong.title.replace(Regex("[\\\\/:*?\"<>|]"), "")
                    val finalFileName = "${cleanTitle1}_merged.m4a"
                    val expectedPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        "Thư mục Music/Merges/$finalFileName"
                    } else {
                        "Thư mục Music/$finalFileName"
                    }

                    Surface(
                        color = palette.backgroundSoft.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (strings.lang == "vi") "Lưu tại:" else "Saved at:",
                                color = palette.textSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = expectedPath,
                                color = palette.textPrimary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = {
                            previewPlayer.pause()
                            step = 1
                        }) {
                            Text(if (strings.lang == "vi") "Quay lại" else "Back", color = palette.textSecondary)
                        }
                        Button(
                            onClick = {
                                if (selectedSong != null) {
                                    isMerging = true
                                    previewPlayer.pause()

                                    // Bắt đầu gọi Transformer
                                    startMergingWithTransformer(
                                        context = context,
                                        song1 = baseSong, startMs1 = slider1.start.toLong(), endMs1 = slider1.endInclusive.toLong(),
                                        song2 = selectedSong!!, startMs2 = slider2.start.toLong(), endMs2 = slider2.endInclusive.toLong(),
                                        onSuccess = { savedPath ->
                                            (context as android.app.Activity).runOnUiThread {
                                                isMerging = false
                                                onMergeSuccess() // Đóng Dialog và hiển thị Toast thành công
                                            }
                                        },
                                        onError = { error ->
                                            (context as android.app.Activity).runOnUiThread {
                                                isMerging = false
                                                android.widget.Toast.makeText(context, "Lỗi khi ghép: ${error.message}", android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    )
                                }
                            },
                            enabled = !isMerging,
                            colors = ButtonDefaults.buttonColors(containerColor = palette.purpleBright)
                        ) {
                            Text(if (isMerging) strings.merging() else strings.save())
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun AdminDashboardDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val palette = LocalPalette.current
    val todayDateStr = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    }
    var selectedDateStr by remember { mutableStateOf(todayDateStr) }

    var allDatabaseStats by remember { mutableStateOf<Map<String, Map<String, Map<String, Any>>>>(emptyMap()) }
    var selectedTab by remember { mutableStateOf("Theo Ngày") }
    var selectedMonthIndex by remember { mutableIntStateOf(0) }

    // Đọc toàn bộ dữ liệu từ nhánh usage_stats trên Firebase
    DisposableEffect(Unit) {
        val databaseRef = try {
            com.google.firebase.database.FirebaseDatabase.getInstance(
                "https://melowave-f8056-default-rtdb.asia-southeast1.firebasedatabase.app"
            ).getReference("usage_stats")
        } catch (e: Exception) {
            null
        }

        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val fullMap = mutableMapOf<String, Map<String, Map<String, Any>>>()
                for (dateSnapshot in snapshot.children) {
                    val dateKey = dateSnapshot.key ?: continue
                    val usersInDate = mutableMapOf<String, Map<String, Any>>()
                    for (userSnapshot in dateSnapshot.children) {
                        val userKey = userSnapshot.key ?: continue
                        val rawData = userSnapshot.value as? Map<String, Any>
                        if (rawData != null) {
                            usersInDate[userKey] = rawData
                        }
                    }
                    fullMap[dateKey] = usersInDate
                }
                allDatabaseStats = fullMap
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        }

        databaseRef?.addValueEventListener(listener)

        onDispose {
            databaseRef?.removeEventListener(listener)
        }
    }

    // Khóa gộp chung ưu tiên Model máy để xử lý triệt để cả dữ liệu cũ thiếu deviceId
    val getGroupKey = { item: Map<String, Any> ->
        val model = item["deviceModel"]?.toString()?.takeIf { it.isNotBlank() }
        val deviceId = item["deviceId"]?.toString()?.takeIf { it.isNotBlank() }
        val userId = item["userId"]?.toString()?.takeIf { it.isNotBlank() }
        model ?: deviceId ?: userId ?: "Unknown"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = palette.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.ChevronRight, "Back", tint = palette.textPrimary, modifier = Modifier.size(30.dp))
                    }
                    Text("Quản lý Hỗ trợ (Admin)", color = palette.purpleBright, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }

                // Tabs Chuyển đổi
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Theo Ngày", "Theo Tháng", "So Sánh Tháng").forEach { tab ->
                        val isSelected = tab == selectedTab
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { selectedTab = tab }
                                .padding(8.dp)
                        ) {
                            Text(
                                tab,
                                color = if (isSelected) palette.purpleBright else palette.textMuted,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .height(3.dp)
                                        .width(70.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(palette.purpleBright)
                                )
                            }
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.borderSoft))

                when (selectedTab) {
                    "Theo Ngày" -> {
                        val todayUsersMap = allDatabaseStats[selectedDateStr] ?: emptyMap()
                        val availableDates = allDatabaseStats.keys.sortedDescending()
                        var showDatePicker by remember { mutableStateOf(false) }

                        // Gộp dữ liệu theo Group Key
                        val groupedSessions = mutableMapOf<String, MutableMap<String, Any>>()
                        todayUsersMap.values.forEach { item ->
                            val groupKey = getGroupKey(item)

                            if (groupedSessions.containsKey(groupKey)) {
                                val existing = groupedSessions[groupKey]!!
                                val currentDur = (existing["durationMs"] as? Number)?.toLong() ?: 0L
                                val newDur = (item["durationMs"] as? Number)?.toLong() ?: 0L
                                existing["durationMs"] = currentDur + newDur

                                val currentOpen = (existing["openCount"] as? Number)?.toLong() ?: 0L
                                val newOpen = (item["openCount"] as? Number)?.toLong() ?: 0L
                                existing["openCount"] = currentOpen + newOpen

                                // Ưu tiên lưu tên Email nếu có
                                val currentUserId = existing["userId"]?.toString() ?: ""
                                val newUserId = item["userId"]?.toString() ?: ""
                                if (currentUserId.startsWith("Khách") && !newUserId.startsWith("Khách")) {
                                    existing["userId"] = newUserId
                                }

                                // Cập nhật thời gian truy cập gần nhất
                                val currentAccess = existing["accessTime"]?.toString() ?: ""
                                val newAccess = item["accessTime"]?.toString() ?: ""
                                if (newAccess > currentAccess) {
                                    existing["accessTime"] = newAccess
                                }

                                val totalSec = (currentDur + newDur) / 1000L
                                existing["durationString"] = if (totalSec >= 60) "${totalSec / 60}m ${totalSec % 60}s" else "${totalSec}s"
                            } else {
                                groupedSessions[groupKey] = item.toMutableMap()
                            }
                        }

                        val userSessions = groupedSessions.values.toList().sortedByDescending { (it["durationMs"] as? Number)?.toLong() ?: 0L }
                        val totalUserCount = userSessions.size
                        var totalMs = 0L
                        userSessions.forEach { item ->
                            totalMs += (item["durationMs"] as? Number)?.toLong() ?: 0L
                        }
                        val totalSec = totalMs / 1000L
                        val totalDurationFormatted = if (totalSec >= 60) "${totalSec / 60}m ${totalSec % 60}s" else "${totalSec}s"

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Dữ liệu ngày: $selectedDateStr", color = palette.purpleBright, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                            IconButton(
                                onClick = { showDatePicker = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Rounded.DateRange, contentDescription = "Chọn ngày", tint = palette.purpleBright, modifier = Modifier.size(20.dp))
                            }
                        }

                        if (showDatePicker) {
                            @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
                            val datePickerState = androidx.compose.material3.rememberDatePickerState()

                            androidx.compose.material3.DatePickerDialog(
                                onDismissRequest = { showDatePicker = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        // Format UTC Millis từ DatePicker sang yyyy-MM-dd
                                        datePickerState.selectedDateMillis?.let { millis ->
                                            val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                            format.timeZone = java.util.TimeZone.getTimeZone("UTC")
                                            selectedDateStr = format.format(java.util.Date(millis))
                                        }
                                        showDatePicker = false
                                    }) {
                                        Text("Chọn", color = palette.purpleBright, fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDatePicker = false }) {
                                        Text("Hủy", color = palette.textSecondary)
                                    }
                                }
                            ) {
                                androidx.compose.material3.DatePicker(state = datePickerState)
                            }
                        }
                        val statusText = if (selectedDateStr == todayDateStr) "● Trực tiếp" else "● Đã lưu"
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            AdminStatCard(title = "Thiết bị online", value = totalUserCount.toString(), percent = statusText, modifier = Modifier.weight(1f))
                            AdminStatCard(title = "Tổng thời gian", value = totalDurationFormatted, percent = statusText, modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            itemsIndexed(userSessions) { index, item ->
                                val name = item["userId"]?.toString() ?: "Người dùng"
                                val model = item["deviceModel"]?.toString() ?: ""
                                val timeIn = item["accessTime"]?.toString() ?: "00:00:00"
                                val openCount = (item["openCount"] as? Number)?.toLong() ?: 1L
                                val duration = item["durationString"]?.toString() ?: "0s"
                                val fullInfo = if (model.isNotEmpty()) "$name ($model)" else name
                                AdminUserRow(index + 1, fullInfo, timeIn, openCount, duration)
                            }
                        }
                    }

                    "Theo Tháng" -> {
                        val monthGroups = allDatabaseStats.keys.map { it.substring(0, 7) }.distinct().sortedDescending()
                        val activeMonth = monthGroups.getOrNull(selectedMonthIndex) ?: todayDateStr.substring(0, 7)
                        val displayMonth = activeMonth.split("-").let { "Tháng ${it[1]}/${it[0]}" }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Bộ chuyển tháng
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { if (selectedMonthIndex < monthGroups.lastIndex) selectedMonthIndex++ },
                                    enabled = selectedMonthIndex < monthGroups.lastIndex
                                ) {
                                    Icon(Icons.Rounded.ChevronRight, contentDescription = "Prev", tint = if (selectedMonthIndex < monthGroups.lastIndex) palette.purpleBright else palette.textMuted, modifier = Modifier.size(24.dp))
                                }
                                Text(displayMonth, color = palette.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                IconButton(
                                    onClick = { if (selectedMonthIndex > 0) selectedMonthIndex-- },
                                    enabled = selectedMonthIndex > 0
                                ) {
                                    Icon(Icons.Rounded.ChevronRight, contentDescription = "Next", tint = if (selectedMonthIndex > 0) palette.purpleBright else palette.textMuted, modifier = Modifier.size(24.dp))
                                }
                            }

                            // Chú thích
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(10.dp).background(palette.cardRaised, RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Số lượt truy cập", color = palette.textSecondary, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(16.dp))
                                Box(modifier = Modifier.size(10.dp).background(palette.purpleBright, CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Thời gian sử dụng", color = palette.textSecondary, fontSize = 12.sp)
                            }

                            val datesInMonth = allDatabaseStats.filterKeys { it.startsWith(activeMonth) }.toSortedMap()
                            val chartDays = datesInMonth.map { (date, users) ->
                                val dayLabel = date.substring(8, 10) + "/" + date.substring(5, 7)
                                val visits = users.values.sumOf { (it["openCount"] as? Number)?.toLong() ?: 1L }
                                val durationMs = users.values.sumOf { (it["durationMs"] as? Number)?.toLong() ?: 0L }
                                val sec = durationMs / 1000L
                                val durLabel = if (sec >= 3600) "${sec / 3600}h" else if (sec >= 60) "${sec / 60}m" else "${sec}s"
                                Triple(dayLabel, visits, durationMs to durLabel)
                            }

                            // Biểu đồ Cột + Đường (Tím)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = palette.card,
                                border = BorderStroke(1.dp, palette.borderSoft),
                                modifier = Modifier.fillMaxWidth().height(260.dp)
                            ) {
                                if (chartDays.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Chưa có dữ liệu trong tháng này", color = palette.textMuted, fontSize = 13.sp)
                                    }
                                } else {
                                    MonthlyCombinedChart(chartDays)
                                }
                            }

                            // Bảng Xếp Hạng
                            Text("🏆 Bảng xếp hạng ($displayMonth)", color = palette.purpleBright, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                            val rankingMap = mutableMapOf<String, Long>()
                            val deviceToUserMap = mutableMapOf<String, String>()

                            datesInMonth.values.forEach { usersMap ->
                                usersMap.values.forEach { user ->
                                    val groupKey = getGroupKey(user)
                                    val uid = user["userId"]?.toString() ?: "Người dùng"
                                    val ms = (user["durationMs"] as? Number)?.toLong() ?: 0L

                                    rankingMap[groupKey] = (rankingMap[groupKey] ?: 0L) + ms

                                    // Ưu tiên hiển thị bằng tên đăng nhập Email
                                    val currentBestUid = deviceToUserMap[groupKey] ?: ""
                                    if (currentBestUid.isEmpty() || (currentBestUid.startsWith("Khách") && !uid.startsWith("Khách"))) {
                                        deviceToUserMap[groupKey] = uid
                                    }
                                }
                            }

                            val rankedList = rankingMap.toList().map { (groupKey, ms) ->
                                (deviceToUserMap[groupKey] ?: "Người dùng") to ms
                            }.sortedByDescending { it.second }

                            rankedList.take(5).forEachIndexed { index, (user, totalMs) ->
                                val sec = totalMs / 1000L
                                val durText = if (sec >= 3600) "${sec / 3600}h ${(sec % 3600) / 60}m ${sec % 60}s" else if (sec >= 60) "${sec / 60}m ${sec % 60}s" else "${sec}s"
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = palette.card,
                                    border = BorderStroke(1.dp, palette.borderSoft),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("#${index + 1}", color = if (index == 0) Color(0xFFFFB300) else if (index == 1) Color(0xFFC0C0C0) else Color(0xFFCD7F32), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(user, color = palette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(durText, color = palette.purpleBright, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    "So Sánh Tháng" -> {
                        val allMonths = allDatabaseStats.keys.map { it.substring(0, 7) }.distinct().sorted()

                        val monthComparisons = allMonths.map { month ->
                            val dates = allDatabaseStats.filterKeys { it.startsWith(month) }

                            val uniqueDevices = mutableSetOf<String>()
                            var totalVisits = 0L
                            var totalDurationMs = 0L

                            dates.values.forEach { dayUsers ->
                                dayUsers.values.forEach { item ->
                                    val groupKey = getGroupKey(item)
                                    uniqueDevices.add(groupKey)
                                    totalVisits += (item["openCount"] as? Number)?.toLong() ?: 1L
                                    totalDurationMs += (item["durationMs"] as? Number)?.toLong() ?: 0L
                                }
                            }

                            val totalHours = totalDurationMs / (1000f * 3600f)
                            val parts = month.split("-")
                            val label = "${parts[1]}/${parts[0]}"
                            MonthCompareData(label, uniqueDevices.size, totalVisits, totalHours, totalDurationMs)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("📊 Biểu đồ so sánh chỉ số qua các tháng", color = palette.purpleBright, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                            // Legend chú thích 3 màu
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).background(palette.blue, RoundedCornerShape(2.dp)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Người dùng", color = palette.textSecondary, fontSize = 12.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFFFB300), RoundedCornerShape(2.dp)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Lượt dùng", color = palette.textSecondary, fontSize = 12.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).background(palette.purpleBright, RoundedCornerShape(2.dp)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Thời gian (giờ)", color = palette.textSecondary, fontSize = 12.sp)
                                }
                            }

                            // Khung Biểu Đồ So Sánh
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = palette.card,
                                border = BorderStroke(1.dp, palette.borderSoft),
                                modifier = Modifier.fillMaxWidth().height(260.dp)
                            ) {
                                if (monthComparisons.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Chưa có dữ liệu so sánh", color = palette.textMuted, fontSize = 13.sp)
                                    }
                                } else {
                                    MultiMonthBarChart(monthComparisons)
                                }
                            }

                            Text("📋 Chi tiết từng tháng", color = palette.purpleBright, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                            monthComparisons.forEach { data ->
                                val sec = data.rawDurationMs / 1000L
                                val durText = if (sec >= 3600) "${sec / 3600}h ${(sec % 3600) / 60}m" else "${sec / 60}m"
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = palette.card,
                                    border = BorderStroke(1.dp, palette.borderSoft),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(data.monthLabel, color = palette.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Rounded.AccountCircle, contentDescription = null, tint = palette.blue, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("${data.users} thiết bị", color = palette.blue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Rounded.Repeat, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("${data.visits} lượt sử dụng", color = Color(0xFFFFB300), fontSize = 13.sp)
                                            }
                                        }

                                        Text("⏱ $durText", color = palette.purpleBright, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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

// Data class cho tab so sánh tháng
data class MonthCompareData(
    val monthLabel: String,
    val users: Int,
    val visits: Long,
    val hours: Float,
    val rawDurationMs: Long
)

// Canvas vẽ Biểu đồ kết hợp Bar + Line Chart cho tab Theo Tháng
@Composable
fun MonthlyCombinedChart(data: List<Triple<String, Long, Pair<Long, String>>>) {
    val palette = LocalPalette.current
    val maxVisits = data.maxOfOrNull { it.second }?.coerceAtLeast(1L)?.toFloat() ?: 1f
    val maxDurationMs = data.maxOfOrNull { it.third.first }?.coerceAtLeast(1L)?.toFloat() ?: 1f

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val width = size.width
        val height = size.height - 40.dp.toPx()
        val barWidth = 26.dp.toPx()
        val spacing = width / data.size

        val points = mutableListOf<androidx.compose.ui.geometry.Offset>()

        data.forEachIndexed { i, (dayLabel, visits, durPair) ->
            val x = spacing * i + (spacing / 2)

            val barHeight = (visits / maxVisits) * height * 0.85f
            drawRoundRect(
                color = palette.cardRaised,
                topLeft = androidx.compose.ui.geometry.Offset(x - barWidth / 2, height - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )

            val pointY = height - ((durPair.first / maxDurationMs) * height * 0.75f)
            points.add(androidx.compose.ui.geometry.Offset(x, pointY))
        }

        for (i in 0 until points.size - 1) {
            drawLine(
                color = palette.purpleBright,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 2.5.dp.toPx()
            )
        }

        points.forEach { pt ->
            drawCircle(color = palette.purpleBright, radius = 4.dp.toPx(), center = pt)
        }
    }
}

// Canvas vẽ Biểu đồ So Sánh 3 Cột
@Composable
fun MultiMonthBarChart(data: List<MonthCompareData>) {
    val palette = LocalPalette.current
    val maxVal = data.maxOfOrNull { maxOf(it.users.toFloat(), it.visits.toFloat(), it.hours) }?.coerceAtLeast(1f) ?: 1f

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val width = size.width
        val height = size.height - 30.dp.toPx()
        val groupSpacing = width / data.size
        val barW = 12.dp.toPx()

        data.forEachIndexed { i, item ->
            val groupCenter = groupSpacing * i + (groupSpacing / 2)

            // 1. Cột Người Dùng (Xanh dương)
            val h1 = (item.users / maxVal) * height * 0.9f
            drawRoundRect(
                color = palette.blue,
                topLeft = androidx.compose.ui.geometry.Offset(groupCenter - barW * 1.6f, height - h1),
                size = androidx.compose.ui.geometry.Size(barW, h1),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
            )

            // 2. Cột Lượt Dùng (Vàng cam)
            val h2 = (item.visits / maxVal) * height * 0.9f
            drawRoundRect(
                color = Color(0xFFFFB300),
                topLeft = androidx.compose.ui.geometry.Offset(groupCenter - barW / 2, height - h2),
                size = androidx.compose.ui.geometry.Size(barW, h2),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
            )

            // 3. Cột Thời Gian (Tím)
            val h3 = (item.hours / maxVal) * height * 0.9f
            drawRoundRect(
                color = palette.purpleBright,
                topLeft = androidx.compose.ui.geometry.Offset(groupCenter + barW * 0.6f, height - h3),
                size = androidx.compose.ui.geometry.Size(barW, h3),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
            )
        }
    }
}

@Composable
fun AdminStatCard(title: String, value: String, percent: String, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = palette.card,
        border = BorderStroke(1.dp, palette.borderSoft)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = palette.textSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = palette.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(percent, color = palette.purpleBright, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AdminUserRow(index: Int, name: String, timeIn: String, openCount: Long, duration: String) {
    val palette = LocalPalette.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.card,
        border = BorderStroke(1.dp, palette.borderSoft),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("#$index", color = Color(0xFFFFB300), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = palette.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("Gần nhất: $timeIn • Vào app: ${openCount} lần", color = palette.textSecondary, fontSize = 12.sp)
            }
            Text(duration, color = palette.purpleBright, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}
@Composable
private fun WaveLoadingIndicator(modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val infiniteTransition = rememberInfiniteTransition(label = "wave_transition")

    val animations = List(5) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 400,
                    delayMillis = index * 80,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "wave_anim_$index"
        )
    }

    Row(
        modifier = modifier, // Sử dụng modifier linh hoạt truyền từ bên ngoài vào
        horizontalArrangement = Arrangement.spacedBy(4.dp), // Thu hẹp khoảng cách cho thanh mảnh hơn
        verticalAlignment = Alignment.CenterVertically
    ) {
        animations.forEach { anim ->
            Box(
                modifier = Modifier
                    .width(4.dp) // Thanh sóng âm thon gọn hơn để nằm đẹp trong cover ảnh
                    .fillMaxHeight(anim.value)
                    .clip(RoundedCornerShape(50))
                    .background(palette.purpleBright)
            )
        }
    }
}

@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                // Lưu ý: Đây là App ID Test của Google để code không bị lỗi khóa tài khoản
                // Khi phát hành lên CH Play thực tế, bạn cần đổi ID này sang ID AdMob Banner thực tế của bạn
                adUnitId = "ca-app-pub-3940256099942544/6300978111"
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}