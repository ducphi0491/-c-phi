package ducphi.tunnerinstrument

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.StatFs
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream
import java.util.zip.ZipInputStream

// ==========================================
// 1. DATA MODELS
// ==========================================
data class Song(
    val id: String, val title: String, val originalKey: String, val rhythm: String,
    val chordList: List<String>, val lyrics: List<String>, val favoriteCount: Int,
    val viewCount: Int, val commentCount: Int, val ratingStars: Float, val ratingCount: Int,
    val genre: List<String>, val authorId: String, val authorName: String,
    val singerNames: List<String>, val youtubeUrl: String, val createdAt: String,
    val updatedAt: String, val tac_gia: String, val ca_si: String, val tong: String,
    val dieu: String, val bpm: Int, val the_loai: String, val slug: String, val image_count: Int,
    val zingMp3Id: String = "",
    val hasLyrics: Boolean = false
)

data class SearchResultItem(
    val title: String, val singer: String, val composer: String, val url: String,
    val zingMp3Url: String, val videoUrl: String, val chords: List<String>,
    val slug: String = "", val imageCount: Int = 0,
    val hasLyricsCache: Boolean = false, val hasAudioCache: Boolean = false,
    val zingMp3Id: String = ""
)

data class SongDetails(
    val title: String = "", val singer: String = "", val composer: String = "",
    val baseTone: String = "C", val rhythm: String = "", val bpm: String = "",
    val lyrics: String = "", val videoUrl: String = "", val error: String? = null,
    val songId: String = "", val likes: Int = 0, val isLikedByDevice: Boolean = false,
    val slug: String = "", val imageCount: Int = 0,
    val zingMp3Id: String = ""
)

data class ChatMessage(val sender: String, val text: String, val timestamp: String)

data class SupportTicket(
    val id: String, val type: String, val deviceId: String, val subject: String,
    val message: String, val chatHistory: List<ChatMessage>, val status: Int,
    val userRead: Int, val date: String,
    // Gmail cua nguoi dung neu da dang nhap. De trong thi giao dien Admin dung deviceId.
    val userEmail: String = ""
)

data class AdminAccount(val id: String, val email: String, val role: String, val createdAt: String)

data class DbBundleInstallResult(
    val success: Boolean,
    val message: String = "",
    val songCount: Long = 0L
)

// ==========================================
// 2. TWO-DATABASE INSTALLER
// Kept in this file as requested. No extra .kt file is required.
// ==========================================
private object TunerDbBundleInstaller {
    const val MAIN_DB_NAME = "tuner_songs_v2.db"
    const val LYRICS_DB_NAME = "tuner_lyrics_v2.db"

    private const val DATA_RELEASE = 2
    private const val MAIN_SCHEMA_VERSION = 4
    private const val LYRICS_SCHEMA_VERSION = 1

    private const val MAIN_ASSET = "databases/tuner_songs.zip"
    private const val LYRICS_ASSET = "databases/tuner_lyrics.zip"

    private const val PREF_NAME = "tuner_database_bundle"
    private const val KEY_INSTALLED_RELEASE = "installed_release"
    // Tu 2.1.9 tro di: luu bang chung da chuyen doi/kiem tra, khong thay DB lan nua.
    private const val KEY_COMPAT_VALIDATED_219 = "db_compat_validated_219"
    private const val KEY_LAST_READY_APP_VERSION = "db_last_ready_app_version"

    private const val LEGACY_MAIN_DB_NAME = "tuner_songs.db"
    private const val LEGACY_LYRICS_DB_NAME = "tuner_lyrics.db"

    suspend fun ensureInstalled(context: Context): DbBundleInstallResult = withContext(Dispatchers.IO) {
        ensureInstalledBlocking(context.applicationContext)
    }

    @Synchronized
    private fun ensureInstalledBlocking(context: Context): DbBundleInstallResult {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val mainDb = context.getDatabasePath(MAIN_DB_NAME)
        val lyricsDb = context.getDatabasePath(LYRICS_DB_NAME)
        val installedRelease = prefs.getInt(KEY_INSTALLED_RELEASE, 0)
        val validatedFor219 = prefs.getBoolean(KEY_COMPAT_VALIDATED_219, false)
        val databaseDir = mainDb.parentFile
            ?: return DbBundleInstallResult(false, "Cannot locate database directory")
        databaseDir.mkdirs()

        // From Tuner Tools 2.1.8 to 2.1.9: reuse the existing V2 pair.
        // Installed app VERSION_NAME is not the prior installed version.
        // Therefore we use database identity and SQLite schema, never an inferred
        // old version, to decide whether a database is ready.
        if (mainDb.exists() && lyricsDb.exists()) {
            if (validatedFor219 &&
                mainDb.length() > 1_000_000L && lyricsDb.length() > 1_000_000L
            ) {
                return DbBundleInstallResult(true, "Database ready (delta sync)")
            }
            return try {
                // Verify the actual V2 schema once when upgrading from 2.1.8.
                // Count mismatches or embedded lyrics should not force a reinstall;
                // 2.1.8 could already have locally edited or partially synced data.
                val count = verifyMainDatabase(mainDb, strictContents = false)
                verifyLyricsDatabase(lyricsDb, count, strictCounts = false)
                require(markDatabasesReady(prefs)) {
                    "Cannot persist database compatibility state"
                }
                DbBundleInstallResult(true, "Database ready (delta sync)", count)
            } catch (e: Exception) {
                Log.e("TunerDbInstaller", "Existing V2 database is not compatible", e)
                DbBundleInstallResult(
                    false,
                    "Database V2 chua tuong thich. Da giu nguyen tat ca file .db; khong tu dong thay the."
                )
            }
        }

        // Do not wipe a partially present pair, even if the legacy marker is missing.
        // A real user's 2.1.8 database can exist without a reliable release marker.
        if (mainDb.exists() || lyricsDb.exists()) {
            return DbBundleInstallResult(
                false,
                "Thieu mot file database V2. Du lieu con lai duoc giu nguyen de phuc hoi an toan."
            )
        }

        // 2.1.2 and older are not auto-migrated by this release. The user has
        // confirmed that those versions require uninstall/reinstall. Never silently
        // replace a legacy database just because a version marker is absent.
        if (context.getDatabasePath(LEGACY_MAIN_DB_NAME).exists() ||
            context.getDatabasePath(LEGACY_LYRICS_DB_NAME).exists()
        ) {
            return DbBundleInstallResult(
                false,
                "Phat hien database phien ban cu. Can cai moi ung dung; khong tu dong xoa du lieu cu."
            )
        }

        // If the V2 installer previously completed but files disappeared, stop
        // instead of automatically replacing a user's database.
        if (installedRelease > 0 || validatedFor219) {
            return DbBundleInstallResult(
                false,
                "Database da duoc cai truoc day nhung hien khong con. Khong tu dong thay the."
            )
        }

        // Fresh installs only: inflate the complete seed database pair once.
        // All subsequent application versions keep the pair and use Sync V2.
        val tempMain = File(databaseDir, "$MAIN_DB_NAME.installing")
        val tempLyrics = File(databaseDir, "$LYRICS_DB_NAME.installing")
        deleteSqliteFile(tempMain)
        deleteSqliteFile(tempLyrics)
        try {
            checkAvailableStorage(databaseDir)
            extractZipAsset(context, MAIN_ASSET, tempMain)
            extractZipAsset(context, LYRICS_ASSET, tempLyrics)
            val seedCount = verifyMainDatabase(tempMain)
            verifyLyricsDatabase(tempLyrics, seedCount)
            setDatabaseVersion(tempMain, MAIN_SCHEMA_VERSION)
            setDatabaseVersion(tempLyrics, LYRICS_SCHEMA_VERSION)

            moveDatabaseFile(tempMain, mainDb)
            moveDatabaseFile(tempLyrics, lyricsDb)
            val finalCount = verifyMainDatabase(mainDb)
            verifyLyricsDatabase(lyricsDb, finalCount)

            // A fresh seed starts a new delta synchronization history.
            require(
                context.getSharedPreferences("tuner_song_sync_v2", Context.MODE_PRIVATE)
                    .edit().clear().commit()
            ) { "Cannot reset fresh-install sync state" }
            require(markDatabasesReady(prefs)) { "Cannot save seed state" }

            return DbBundleInstallResult(true, "Seed installed (delta sync ready)", finalCount)
        } catch (e: Exception) {
            Log.e("TunerDbInstaller", "Fresh seed installation failed", e)
            deleteSqliteFile(tempMain)
            deleteSqliteFile(tempLyrics)
            // Both paths were absent before this first install; any files created
            // in this try block are incomplete seed files, not existing user data.
            deleteSqliteFile(mainDb)
            deleteSqliteFile(lyricsDb)
            return DbBundleInstallResult(false, e.message ?: "Cannot install initial database")
        }
    }

    private fun markDatabasesReady(prefs: android.content.SharedPreferences): Boolean {
        return prefs.edit()
            .putInt(KEY_INSTALLED_RELEASE, DATA_RELEASE)
            .putBoolean(KEY_COMPAT_VALIDATED_219, true)
            .putString(KEY_LAST_READY_APP_VERSION, BuildConfig.VERSION_NAME)
            .commit()
    }

    private fun checkAvailableStorage(databaseDir: File) {
        val available = StatFs(databaseDir.absolutePath).availableBytes
        val required = 140L * 1024L * 1024L
        require(available >= required) {
            "Please free at least 140 MB to update song data safely"
        }
    }

    private fun extractZipAsset(context: Context, assetPath: String, outputFile: File) {
        outputFile.parentFile?.mkdirs()
        context.assets.open(assetPath).use { assetInput ->
            ZipInputStream(BufferedInputStream(assetInput)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null && entry.isDirectory) {
                    entry = zip.nextEntry
                }
                require(entry != null) { "ZIP does not contain a database: $assetPath" }

                FileOutputStream(outputFile).use { output ->
                    zip.copyTo(output)
                    output.fd.sync()
                }
                zip.closeEntry()
            }
        }

        require(outputFile.exists() && outputFile.length() > 1_000_000L) {
            "Extracted database is invalid: $assetPath"
        }
    }

    private fun verifyMainDatabase(file: File, strictContents: Boolean = true): Long {
        require(file.exists()) { "Metadata database not found" }
        val db = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        try {
            quickCheck(db)
            val columns = getTableColumns(db, "songs")
            require("id" in columns) { "Metadata database is missing id" }
            require("title" in columns) { "Metadata database is missing title" }
            require("lyrics" in columns) { "Metadata database is missing lyrics" }
            require("hasLyrics" in columns) { "Metadata database is missing hasLyrics" }

            val count = queryLong(db, "SELECT COUNT(*) FROM songs")
            require(count > 0L) { "Metadata database has no songs" }

            val nonEmptyLyrics = queryLong(
                db,
                "SELECT COUNT(*) FROM songs WHERE TRIM(COALESCE(lyrics, '')) <> ''"
            )
            if (strictContents) {
                require(nonEmptyLyrics == 0L) {
                    "Metadata database still contains $nonEmptyLyrics full lyrics rows"
                }
            } else if (nonEmptyLyrics > 0L) {
                Log.w("TunerDbInstaller", "Existing V2 has $nonEmptyLyrics inline lyrics; preserving data")
            }
            return count
        } finally {
            db.close()
        }
    }

    private fun verifyLyricsDatabase(file: File, expectedSongCount: Long, strictCounts: Boolean = true) {
        require(file.exists()) { "Lyrics database not found" }
        val db = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        try {
            quickCheck(db)
            val columns = getTableColumns(db, "lyrics")
            require("song_id" in columns) { "Lyrics database is missing song_id" }
            require("lyrics_z" in columns) { "Lyrics database is missing lyrics_z" }
            require("raw_bytes" in columns) { "Lyrics database is missing raw_bytes" }

            val count = queryLong(db, "SELECT COUNT(*) FROM lyrics")
            if (strictCounts) {
                require(count == expectedSongCount) {
                    "Lyrics row count ($count) differs from song count ($expectedSongCount)"
                }
            } else if (count != expectedSongCount) {
                Log.w("TunerDbInstaller", "V2 row count mismatch ($count vs $expectedSongCount); preserving existing databases")
            }

            db.rawQuery(
                "SELECT lyrics_z FROM lyrics WHERE raw_bytes > 0 LIMIT 1",
                null
            ).use { cursor ->
                require(cursor.moveToFirst()) { "Lyrics database has no sample row" }
                val restored = inflateUtf8(cursor.getBlob(0))
                require(restored.isNotBlank()) { "Cannot decompress sample lyrics" }
            }
        } finally {
            db.close()
        }
    }

    private fun quickCheck(db: SQLiteDatabase) {
        db.rawQuery("PRAGMA quick_check", null).use { cursor ->
            require(cursor.moveToFirst()) { "SQLite quick_check returned no result" }
            require(cursor.getString(0).equals("ok", ignoreCase = true)) {
                "SQLite quick_check failed"
            }
        }
    }

    private fun getTableColumns(db: SQLiteDatabase, tableName: String): Set<String> {
        val result = mutableSetOf<String>()
        db.rawQuery("PRAGMA table_info($tableName)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                result.add(cursor.getString(nameIndex) ?: "")
            }
        }
        return result
    }

    private fun queryLong(db: SQLiteDatabase, sql: String): Long {
        var result = 0L
        db.rawQuery(sql, null).use { cursor ->
            require(cursor.moveToFirst()) { "Query returned no result" }
            result = cursor.getLong(0)
        }
        return result
    }

    private fun setDatabaseVersion(file: File, version: Int) {
        val db = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
        try {
            db.version = version
        } finally {
            db.close()
        }
    }

    private fun moveDatabaseFile(source: File, target: File) {
        target.parentFile?.mkdirs()
        if (source.renameTo(target)) return

        source.inputStream().use { input ->
            FileOutputStream(target).use { output ->
                input.copyTo(output)
                output.fd.sync()
            }
        }
        require(target.exists() && target.length() > 0L) { "Cannot move database file" }
        source.delete()
    }

    private fun inflateUtf8(data: ByteArray): String {
        if (data.isEmpty()) return ""
        return InflaterInputStream(ByteArrayInputStream(data))
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
    }

    private fun deleteSqliteFile(file: File) {
        try {
            val deleted = SQLiteDatabase.deleteDatabase(file)
            if (!deleted && file.exists()) {
                file.delete()
                File("${file.absolutePath}-wal").delete()
                File("${file.absolutePath}-shm").delete()
                File("${file.absolutePath}-journal").delete()
            }
        } catch (_: Exception) {
            file.delete()
            File("${file.absolutePath}-wal").delete()
            File("${file.absolutePath}-shm").delete()
            File("${file.absolutePath}-journal").delete()
        }
    }
}

// ==========================================
// 3. DATABASE HELPERS
// ==========================================
class TunerDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, TunerDbBundleInstaller.MAIN_DB_NAME, null, 4) {

    init {
        setWriteAheadLoggingEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        // The prebuilt DB is installed before this helper is created.
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future metadata schema upgrades can be added here.
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        try {
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_songs_title_search ON songs(title COLLATE NOCASE)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_songs_slug_search ON songs(slug COLLATE NOCASE)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_songs_singer_search ON songs(singerNames COLLATE NOCASE)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_songs_composer_search ON songs(authorName COLLATE NOCASE)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_songs_popularity ON songs(viewCount DESC, updatedAt DESC)")
        } catch (e: Exception) {
            Log.e("DatabaseIndex", "Cannot create song indexes", e)
        }
    }
}

class LyricsDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, TunerDbBundleInstaller.LYRICS_DB_NAME, null, 1) {

    init {
        setWriteAheadLoggingEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS lyrics (
                song_id TEXT PRIMARY KEY NOT NULL,
                lyrics_z BLOB NOT NULL,
                raw_bytes INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future lyrics schema upgrades can be added here.
    }
}

class CaSiDatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, "ca_si.db", null, 1) {
    private val dbPath = context.getDatabasePath("ca_si.db").path

    init {
        if (!File(dbPath).exists()) {
            copyDatabase()
        }
    }

    private fun copyDatabase() {
        File(dbPath).parentFile?.mkdirs()
        try {
            context.assets.open("databases/ca_si.zip").use { inputStream ->
                ZipInputStream(inputStream).use { zip ->
                    val entry = zip.nextEntry
                    if (entry != null) {
                        FileOutputStream(dbPath).use { output -> zip.copyTo(output) }
                    }
                }
            }
        } catch (e: Exception) {
            context.assets.open("databases/ca_si.db").use { input ->
                FileOutputStream(dbPath).use { output -> input.copyTo(output) }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {}
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
}

// ==========================================
// 4. MAIN OFFLINE REPOSITORY
// ==========================================
class SongRepository(context: Context) {
    companion object {
        private const val SYNC_PREF_NAME = "tuner_song_sync_v2"
        private const val KEY_BOOTSTRAP_DONE = "bootstrap_done"
        private const val KEY_BOOTSTRAP_TIME = "bootstrap_time"
        private const val KEY_BOOTSTRAP_ID = "bootstrap_id"
        private const val KEY_BOOTSTRAP_SNAPSHOT = "bootstrap_snapshot_cursor"
        private const val KEY_DELTA_CURSOR = "delta_cursor"
        private const val SYNC_PAGE_SIZE = 100
        private val songSyncMutex = Mutex()

        suspend fun prepareDatabases(context: Context): DbBundleInstallResult {
            return TunerDbBundleInstaller.ensureInstalled(context.applicationContext)
        }
    }

    private val appContext = context.applicationContext
    private val syncPrefs = appContext.getSharedPreferences(SYNC_PREF_NAME, Context.MODE_PRIVATE)
    private val dbHelper = TunerDatabaseHelper(appContext)
    private val lyricsDbHelper = LyricsDatabaseHelper(appContext)
    private val caSiDbHelper = CaSiDatabaseHelper(appContext)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val obfuscatedKey = intArrayOf(
        12, 45, 54, 61, 42, 25, 40, 40, 7, 11, 61, 59, 42, 61, 44, 19,
        61, 33, 7, 106, 104, 106, 110, 7, 24, 8, 48, 49, 20, 45, 59, 121
    )

    private fun getRealApiUrl(): String = "https://tunertools.top/api_songs.php"

    private fun stringColumn(cursor: Cursor, name: String, defaultValue: String = ""): String {
        val index = cursor.getColumnIndex(name)
        return if (index >= 0 && !cursor.isNull(index)) cursor.getString(index) ?: defaultValue else defaultValue
    }

    private fun intColumn(cursor: Cursor, name: String): Int {
        val index = cursor.getColumnIndex(name)
        return if (index >= 0 && !cursor.isNull(index)) cursor.getInt(index) else 0
    }

    private fun floatColumn(cursor: Cursor, name: String): Float {
        val index = cursor.getColumnIndex(name)
        return if (index >= 0 && !cursor.isNull(index)) cursor.getFloat(index) else 0f
    }

    private fun compressLyrics(text: String): ByteArray {
        if (text.isEmpty()) return ByteArray(0)
        val output = ByteArrayOutputStream()
        DeflaterOutputStream(output, Deflater(Deflater.BEST_COMPRESSION)).use { stream ->
            stream.write(text.toByteArray(Charsets.UTF_8))
        }
        return output.toByteArray()
    }

    private fun decompressLyrics(data: ByteArray): String {
        if (data.isEmpty()) return ""
        return InflaterInputStream(ByteArrayInputStream(data))
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
    }

    private fun readLyricsBySongIdBlocking(songId: String): String {
        if (songId.isBlank()) return ""
        return try {
            lyricsDbHelper.readableDatabase.rawQuery(
                "SELECT lyrics_z FROM lyrics WHERE song_id = ? LIMIT 1",
                arrayOf(songId)
            ).use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) {
                    decompressLyrics(cursor.getBlob(0))
                } else {
                    ""
                }
            }
        } catch (e: Exception) {
            Log.e("LyricsDatabase", "Cannot read lyrics for $songId", e)
            ""
        }
    }

    private fun saveLyricsBlocking(songId: String, lyrics: String): Boolean {
        if (songId.isBlank()) return false
        return try {
            val values = ContentValues().apply {
                put("song_id", songId)
                put("lyrics_z", compressLyrics(lyrics))
                put("raw_bytes", lyrics.toByteArray(Charsets.UTF_8).size)
            }
            lyricsDbHelper.writableDatabase.insertWithOnConflict(
                "lyrics",
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            ) != -1L
        } catch (e: Exception) {
            Log.e("LyricsDatabase", "Cannot save lyrics for $songId", e)
            false
        }
    }

    suspend fun getLyricsBySongId(songId: String): String = withContext(Dispatchers.IO) {
        readLyricsBySongIdBlocking(songId)
    }

    private fun mapCursorToSong(cursor: Cursor, includeLyrics: Boolean = false): Song {
        val id = stringColumn(cursor, "id")
        val chordStr = stringColumn(cursor, "chordList")
        val genreStr = stringColumn(cursor, "genre")
        val legacyLyrics = stringColumn(cursor, "lyrics")
        val hasLyricsIndex = cursor.getColumnIndex("hasLyrics")
        val hasLyrics = if (hasLyricsIndex >= 0 && !cursor.isNull(hasLyricsIndex)) {
            cursor.getInt(hasLyricsIndex) == 1
        } else {
            legacyLyrics.isNotBlank()
        }

        val lyricsText = if (includeLyrics) {
            readLyricsBySongIdBlocking(id).ifBlank { legacyLyrics }
        } else {
            ""
        }

        return Song(
            id = id,
            title = stringColumn(cursor, "title", "Chua co ten"),
            originalKey = stringColumn(cursor, "originalKey"),
            rhythm = stringColumn(cursor, "rhythm"),
            chordList = if (chordStr.isNotBlank()) chordStr.split(",") else emptyList(),
            lyrics = if (lyricsText.isNotBlank()) lyricsText.split("\n") else emptyList(),
            favoriteCount = intColumn(cursor, "favoriteCount"),
            viewCount = intColumn(cursor, "viewCount"),
            commentCount = intColumn(cursor, "commentCount"),
            ratingStars = floatColumn(cursor, "ratingStars"),
            ratingCount = intColumn(cursor, "ratingCount"),
            genre = if (genreStr.isNotBlank()) genreStr.split(",") else emptyList(),
            authorId = stringColumn(cursor, "authorId"),
            authorName = stringColumn(cursor, "authorName"),
            singerNames = stringColumn(cursor, "singerNames").split(",").filter { it.isNotBlank() },
            youtubeUrl = stringColumn(cursor, "youtubeUrl"),
            createdAt = stringColumn(cursor, "createdAt"),
            updatedAt = stringColumn(cursor, "updatedAt"),
            tac_gia = stringColumn(cursor, "authorName"),
            ca_si = stringColumn(cursor, "singerNames"),
            tong = stringColumn(cursor, "originalKey"),
            dieu = stringColumn(cursor, "rhythm"),
            bpm = intColumn(cursor, "bpm"),
            the_loai = genreStr,
            slug = stringColumn(cursor, "slug"),
            image_count = intColumn(cursor, "image_count"),
            zingMp3Id = stringColumn(cursor, "id_zing_mp3"),
            hasLyrics = hasLyrics
        )
    }

    private fun songToDetails(song: Song, isLikedByDevice: Boolean): SongDetails {
        return SongDetails(
            title = song.title,
            singer = song.ca_si,
            composer = song.tac_gia,
            baseTone = song.tong,
            rhythm = song.dieu.ifBlank { song.rhythm },
            bpm = if (song.bpm > 0) song.bpm.toString() else "",
            lyrics = song.lyrics.joinToString("\n"),
            videoUrl = song.youtubeUrl,
            songId = song.id,
            likes = song.favoriteCount,
            isLikedByDevice = isLikedByDevice,
            slug = song.slug,
            imageCount = song.image_count,
            zingMp3Id = song.zingMp3Id
        )
    }

    suspend fun getSongDetailsLocal(
        songId: String,
        songTitle: String = "",
        singer: String = "",
        isLikedByDevice: Boolean = false
    ): SongDetails? {
        val song = getSongByExactIdOrTitle(songId, songTitle, singer, includeLyrics = true)
        return song?.let { songToDetails(it, isLikedByDevice) }
    }

    suspend fun getSongDetailsLocal(
        song: Song,
        isLikedByDevice: Boolean = false
    ): SongDetails = withContext(Dispatchers.IO) {
        val fullLyrics = if (song.lyrics.isNotEmpty()) {
            song.lyrics.joinToString("\n")
        } else {
            readLyricsBySongIdBlocking(song.id)
        }
        songToDetails(song.copy(lyrics = if (fullLyrics.isBlank()) emptyList() else fullLyrics.split("\n")), isLikedByDevice)
    }

    suspend fun getCustomSongLocalByTitle(title: String, singer: String = ""): Song? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val query = if (singer.isNotBlank()) {
            "SELECT * FROM songs WHERE title = ? COLLATE NOCASE AND singerNames = ? COLLATE NOCASE AND id GLOB 'local_*' LIMIT 1"
        } else {
            "SELECT * FROM songs WHERE title = ? COLLATE NOCASE AND id GLOB 'local_*' LIMIT 1"
        }
        val args = if (singer.isNotBlank()) arrayOf(title.trim(), singer.trim()) else arrayOf(title.trim())
        db.rawQuery(query, args).use { cursor ->
            if (cursor.moveToFirst()) mapCursorToSong(cursor, includeLyrics = true) else null
        }
    }

    fun getRecentSongsOffline(): Flow<List<Song>> = flow {
        val songs = mutableListOf<Song>()
        dbHelper.readableDatabase.rawQuery(
            "SELECT * FROM songs ORDER BY updatedAt DESC LIMIT 50",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) songs.add(mapCursorToSong(cursor))
        }
        emit(songs)
    }.flowOn(Dispatchers.IO)

    private fun getFamousSongsCrossDb(limit: Int, currentLang: String): List<Song> {
        val caSiDb = caSiDbHelper.readableDatabase
        val tunerDb = dbHelper.readableDatabase
        val qtTarget = if (currentLang == "vi") {
            (limit * 0.10).toInt().coerceAtLeast(if (limit >= 10) 1 else 0)
        } else {
            limit
        }
        val vnTarget = limit - qtTarget

        fun getSongsByKhuVuc(isVietNam: Boolean, targetCount: Int): List<Song> {
            val sql = if (isVietNam) {
                "SELECT ten_ca_si, danh_sach_bai_hat FROM danh_sach_ca_si WHERE (khu_vuc = 'viet_nam' OR khu_vuc IS NULL OR khu_vuc = '') AND danh_sach_bai_hat IS NOT NULL AND danh_sach_bai_hat != ''"
            } else {
                "SELECT ten_ca_si, danh_sach_bai_hat FROM danh_sach_ca_si WHERE (khu_vuc != 'viet_nam' AND khu_vuc IS NOT NULL AND khu_vuc != '') AND danh_sach_bai_hat IS NOT NULL AND danh_sach_bai_hat != ''"
            }

            val queries = mutableListOf<Pair<String, String>>()
            caSiDb.rawQuery(sql, null).use { cursorCaSi ->
                while (cursorCaSi.moveToNext()) {
                    val artistName = cursorCaSi.getString(0) ?: ""
                    val songsStr = cursorCaSi.getString(1) ?: ""
                    songsStr.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { title ->
                        queries.add(Pair(title, artistName))
                    }
                }
            }
            queries.shuffle()

            val matchedSongs = mutableListOf<Song>()
            for ((title, artistName) in queries) {
                if (matchedSongs.size >= targetCount) break
                tunerDb.rawQuery(
                    "SELECT * FROM songs WHERE title = ? AND singerNames LIKE ? LIMIT 1",
                    arrayOf(title, "%$artistName%")
                ).use { songCursor ->
                    if (songCursor.moveToFirst()) {
                        val song = mapCursorToSong(songCursor)
                        if (matchedSongs.none { it.id == song.id }) matchedSongs.add(song)
                    }
                }
            }
            return matchedSongs
        }

        val qtSongs = if (qtTarget > 0) getSongsByKhuVuc(false, qtTarget) else emptyList()
        val vnSongs = if (vnTarget > 0) getSongsByKhuVuc(true, vnTarget) else emptyList()
        val extraNeeded = limit - (vnSongs.size + qtSongs.size)
        val finalVnSongs = if (extraNeeded > 0) {
            val additional = getSongsByKhuVuc(true, vnTarget + extraNeeded)
            (vnSongs + additional).distinctBy { it.id }
        } else {
            vnSongs
        }

        return (finalVnSongs + qtSongs).distinctBy { it.id }.shuffled()
    }

    fun getSuggestedKaraokeSongs(currentLang: String): Flow<List<Song>> = flow {
        emit(getFamousSongsCrossDb(12, currentLang))
    }.flowOn(Dispatchers.IO)

    fun getTop50PopularSongs(currentLang: String): Flow<List<Song>> = flow {
        emit(getFamousSongsCrossDb(50, currentLang))
    }.flowOn(Dispatchers.IO)

    private fun removeAccents(str: String): String {
        val normalized = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD)
        val pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(normalized).replaceAll("").replace("\u0111", "d").replace("\u0110", "D").lowercase()
    }

    fun searchOfflineSongs(
        query: String,
        limit: Int = 15,
        broadSearch: Boolean = false
    ): Flow<List<Song>> = flow {
        val rawQuery = query.trim()
        if (rawQuery.isBlank()) {
            emit(emptyList())
            return@flow
        }

        val cleanQuery = rawQuery
            .removePrefix("ca s\u0129:")
            .removePrefix("ca si:")
            .removePrefix("Ca s\u0129:")
            .removePrefix("Ca si:")
            .trim()
            .replace(Regex("\\s+"), " ")

        if (cleanQuery.isBlank()) {
            emit(emptyList())
            return@flow
        }

        val noAccentQuery = removeAccents(cleanQuery)
        val containsText = "%$cleanQuery%"
        val containsNoAccent = "%$noAccentQuery%"
        val slugQuery = "%${noAccentQuery.replace(" ", "-")}%"
        val limitClause = if (limit <= 0) "" else "LIMIT 50"

        val sql = """
            SELECT *
            FROM songs
            WHERE (
                title LIKE ? COLLATE NOCASE OR
                slug LIKE ? COLLATE NOCASE OR
                singerNames LIKE ? COLLATE NOCASE OR
                singerNames LIKE ? COLLATE NOCASE OR
                authorName LIKE ? COLLATE NOCASE
            )
              AND (id NOT GLOB 'local_*' OR title NOT IN (SELECT title FROM songs WHERE id NOT GLOB 'local_*'))
            ORDER BY viewCount DESC, updatedAt DESC
            $limitClause
        """.trimIndent()

        var songs = mutableListOf<Song>()
        dbHelper.readableDatabase.rawQuery(
            sql,
            arrayOf(containsText, slugQuery, containsText, containsNoAccent, containsText)
        ).use { cursor ->
            while (cursor.moveToNext()) songs.add(mapCursorToSong(cursor))
        }

        if (songs.isNotEmpty()) {
            val exactSlug = noAccentQuery.replace(" ", "-")
            songs.sortWith(compareBy<Song> { song ->
                when {
                    song.ca_si.equals(cleanQuery, true) || song.ca_si.equals(noAccentQuery, true) -> 0
                    song.title.equals(cleanQuery, true) || song.slug.equals(exactSlug, true) -> 1
                    song.title.startsWith(cleanQuery, true) || song.slug.startsWith(exactSlug, true) -> 2
                    song.ca_si.contains(cleanQuery, true) || song.ca_si.contains(noAccentQuery, true) -> 3
                    else -> 4
                }
            }.thenByDescending { it.viewCount })
            if (limit > 0) songs = songs.take(limit).toMutableList()
        }

        // Full-text lyrics fallback was removed because lyrics are now compressed BLOB data.
        emit(songs)
    }.flowOn(Dispatchers.IO)

    // vi: giu NGUYEN logic trending truoc khi them bo loc quoc te.
    // Cac ngon ngu khac: loc ca si quoc te TRUOC khi xep hang viewCount.
    // Khong doi tim kiem, Top 50, goi y bai hat hoac schema SQLite.
    fun getTrendingSongsOffline(currentLang: String): Flow<List<Song>> = flow {
        val isVietnamese = currentLang.trim()
            .substringBefore('-').substringBefore('_')
            .equals("vi", ignoreCase = true)

        if (isVietnamese) {
            // Khop 100% voi truy van va fallback cua ban truoc:
            // Top 15 luot xem toan DB; neu tat ca viewCount=0 thi lay goi y cu.
            val originalTrending = mutableListOf<Song>()
            dbHelper.readableDatabase.rawQuery(
                "SELECT * FROM songs ORDER BY viewCount DESC LIMIT 15",
                null
            ).use { cursor ->
                while (cursor.moveToNext()) originalTrending.add(mapCursorToSong(cursor))
            }
            if (originalTrending.isEmpty() || originalTrending.all { it.viewCount == 0 }) {
                emit(getFamousSongsCrossDb(15, currentLang))
            } else {
                emit(originalTrending)
            }
            return@flow
        }

        val requiredRegion = 2 // Quoc te; chi dung cho ngon ngu khac vi.
        val maxSongs = 15
        val songs = mutableListOf<Song>()

        try {
            val accentsRegex = Regex("\\p{M}+")
            val separatorsRegex = Regex("[^\\p{L}\\p{N}]+")

            // Cung chuan hoa ca danh sach nghe si va cot singerNames.
            // So sanh theo ranh gioi tu, tranh khop nham cac ten ngan.
            fun normalizedArtist(value: String): String {
                return java.text.Normalizer.normalize(
                    value.trim().lowercase(java.util.Locale.ROOT),
                    java.text.Normalizer.Form.NFD
                ).replace('đ', 'd')
                    .replace(accentsRegex, "")
                    .replace(separatorsRegex, " ")
                    .trim()
            }

            // Du lieu khu_vuc da co trong ca_si.db, khong can sua database.
            // Neu ten nghe si xuat hien o ca hai khu vuc, danh dau khong ro rang (3).
            val regionsByArtist = HashMap<String, Int>()
            caSiDbHelper.readableDatabase.rawQuery(
                "SELECT ten_ca_si, khu_vuc FROM danh_sach_ca_si " +
                        "WHERE TRIM(COALESCE(ten_ca_si, '')) <> ''",
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val artist = normalizedArtist(cursor.getString(0) ?: "")
                    if (artist.isBlank()) continue
                    val area = (cursor.getString(1) ?: "").trim()
                    val region = if (area.isBlank() || area.equals("viet_nam", ignoreCase = true)) 1 else 2
                    regionsByArtist[artist] = (regionsByArtist[artist] ?: 0) or region
                }
            }

            // Index theo tu dau tien, de khong phai duyet tat ca nghe si cho moi bai.
            val artistsByFirstWord = HashMap<String, MutableList<Pair<String, Int>>>()
            for ((artist, region) in regionsByArtist) {
                artistsByFirstWord.getOrPut(artist.substringBefore(' ')) {
                    mutableListOf()
                }.add(artist to region)
            }
            val singerRegionCache = HashMap<String, Int>()

            fun regionForSinger(rawSinger: String): Int {
                val singer = normalizedArtist(rawSinger)
                if (singer.isBlank()) return 0
                singerRegionCache[singer]?.let { return it }

                // Uu tien ten chinh xac, sau do moi xu ly nhieu nghe si/feat.
                val exact = regionsByArtist[singer]
                if (exact != null) {
                    singerRegionCache[singer] = exact
                    return exact
                }

                val padded = " $singer "
                var region = 0
                for (word in singer.split(' ').distinct()) {
                    for ((artist, artistRegion) in artistsByFirstWord[word].orEmpty()) {
                        // Ten qua ngan chi duoc tinh khi trung toan bo, tranh nham lan.
                        if (artist.length < 3) continue
                        if (padded.contains(" $artist ")) {
                            region = region or artistRegion
                            if (region == 3) break
                        }
                    }
                    if (region == 3) break
                }
                singerRegionCache[singer] = region
                return region
            }

            // Quet theo viewCount tu cao xuong thap, CHI lay 15 bai DUNG khu vuc.
            // Khong LIMIT 15 truoc khi loc: top 15 toan DB co the deu la nhac Viet.
            if (regionsByArtist.isNotEmpty()) {
                dbHelper.readableDatabase.rawQuery(
                    "SELECT * FROM songs ORDER BY viewCount DESC, updatedAt DESC, id ASC",
                    null
                ).use { cursor ->
                    val singerIndex = cursor.getColumnIndexOrThrow("singerNames")
                    while (songs.size < maxSongs && cursor.moveToNext()) {
                        val singer = if (cursor.isNull(singerIndex)) "" else cursor.getString(singerIndex)
                        if (regionForSinger(singer ?: "") == requiredRegion) {
                            songs.add(mapCursorToSong(cursor))
                        }
                    }
                }
            }
            Log.d("SongRepository", "Trending lang=$currentLang, artists=${regionsByArtist.size}, songs=${songs.size}")
        } catch (error: Exception) {
            // Khong fallback sang danh sach chua loc: se hien sai nhac cho nguoi quoc te.
            Log.e("SongRepository", "Cannot load region-filtered trending songs", error)
        }

        // Van giu thu tu viewCount, ke ca khi luot xem bang 0.
        // Khong chen ngau nhien nhac Viet vao danh sach quoc te khi thieu bai.
        emit(songs)
    }.flowOn(Dispatchers.IO)

    data class ArtistOffline(val name: String, val totalSongs: Int, val youtubeUrl: String)

    fun getArtistsFromCaSiDb(
        theLoai: String? = null,
        theHe: String? = null,
        currentLang: String
    ): Flow<List<ArtistOffline>> = flow {
        val caSiDb = caSiDbHelper.readableDatabase
        val tunerDb = dbHelper.readableDatabase
        val queryParams = mutableListOf<String>()
        val khuVucCond = if (currentLang == "vi") {
            "(khu_vuc = 'viet_nam' OR khu_vuc IS NULL OR khu_vuc = '')"
        } else {
            "(khu_vuc != 'viet_nam' AND khu_vuc IS NOT NULL AND khu_vuc != '')"
        }

        var sqlCaSi = "SELECT DISTINCT ten_ca_si, avatar_url FROM danh_sach_ca_si WHERE $khuVucCond"
        if (theLoai != null && theHe != null) {
            sqlCaSi += " AND the_loai = ? AND the_he = ?"
            queryParams.add(theLoai)
            queryParams.add(theHe)
            sqlCaSi += " ORDER BY RANDOM()"
        } else {
            sqlCaSi += " ORDER BY RANDOM() LIMIT 15"
        }

        val targetArtists = mutableSetOf<String>()
        val artistDataList = mutableListOf<Pair<String, String>>()
        caSiDb.rawQuery(sqlCaSi, queryParams.toTypedArray()).use { cursorCaSi ->
            while (cursorCaSi.moveToNext()) {
                val name = cursorCaSi.getString(0) ?: ""
                if (name.isNotBlank()) {
                    targetArtists.add(name.lowercase())
                    artistDataList.add(Pair(name, cursorCaSi.getString(1) ?: ""))
                }
            }
        }

        val songCountMap = mutableMapOf<String, Int>()
        tunerDb.rawQuery("SELECT singerNames FROM songs", null).use { cursorTuner ->
            while (cursorTuner.moveToNext()) {
                val dbSingerString = (cursorTuner.getString(0) ?: "").lowercase()
                for (targetName in targetArtists) {
                    if (dbSingerString.contains(targetName)) {
                        songCountMap[targetName] = songCountMap.getOrDefault(targetName, 0) + 1
                    }
                }
            }
        }

        val resultList = artistDataList.map { (name, avatarUrl) ->
            ArtistOffline(name, songCountMap.getOrDefault(name.lowercase(), 0), avatarUrl)
        }.shuffled()
        emit(resultList)
    }.flowOn(Dispatchers.IO)

    fun getSongsByIdsLocal(likedIds: List<String>): List<SearchResultItem> {
        if (likedIds.isEmpty()) return emptyList()
        val list = mutableListOf<SearchResultItem>()
        try {
            val db = dbHelper.readableDatabase
            likedIds.chunked(500).forEach { chunk ->
                val placeholders = chunk.joinToString(",") { "?" }
                db.rawQuery(
                    "SELECT * FROM songs WHERE id IN ($placeholders) ORDER BY title ASC",
                    chunk.toTypedArray()
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        val chordStr = stringColumn(cursor, "chordList")
                        list.add(
                            SearchResultItem(
                                title = stringColumn(cursor, "title", "Khong ten"),
                                singer = stringColumn(cursor, "singerNames", "Dang cap nhat"),
                                composer = stringColumn(cursor, "authorName", "Dang cap nhat"),
                                url = stringColumn(cursor, "id"),
                                zingMp3Url = "",
                                videoUrl = stringColumn(cursor, "youtubeUrl"),
                                chords = if (chordStr.isNotBlank()) chordStr.split(",") else emptyList(),
                                slug = stringColumn(cursor, "slug"),
                                imageCount = intColumn(cursor, "image_count"),
                                zingMp3Id = stringColumn(cursor, "id_zing_mp3")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SongRepository", "Cannot read favorite songs", e)
        }
        return list
    }

    fun getLastUpdateTimestamp(): String {
        var lastUpdate = "2000-01-01 00:00:00"
        dbHelper.readableDatabase.rawQuery(
            "SELECT MAX(updatedAt) FROM songs WHERE id NOT GLOB 'local_*'",
            null
        ).use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                cursor.getString(0)?.takeIf { it.isNotBlank() }?.let { lastUpdate = it }
            }
        }
        return lastUpdate
    }

    private data class SyncBatchResult(
        var upserted: Int = 0,
        var deleted: Int = 0
    ) {
        val total: Int get() = upserted + deleted
        operator fun plusAssign(other: SyncBatchResult) {
            upserted += other.upserted
            deleted += other.deleted
        }
    }

    private class SyncV2UnavailableException(message: String) : IOException(message)
    private class SyncHttpException(val statusCode: Int, message: String) : IOException(message)

    private fun JSONObject.stringOrEmpty(name: String): String {
        return if (!has(name) || isNull(name)) "" else optString(name, "")
    }

    private fun fetchJson(url: String, readTimeoutSeconds: Long = 60L): JSONObject {
        val requestClient = client.newBuilder()
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .build()

        requestClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
            val rawBody = response.body?.string().orEmpty()
            val json = try {
                if (rawBody.isBlank()) JSONObject() else JSONObject(rawBody)
            } catch (e: Exception) {
                if (response.isSuccessful) {
                    throw SyncV2UnavailableException("Máy chủ chưa hỗ trợ JSON Sync V2")
                }
                throw SyncHttpException(response.code, "Phản hồi máy chủ không hợp lệ")
            }

            val apiCode = json.optString("code", "")
            if (response.code == 409 && apiCode == "SYNC_SCHEMA_MISSING") {
                throw SyncV2UnavailableException("Máy chủ chưa chạy ALTER_song_sync_v2.sql")
            }
            if (!response.isSuccessful) {
                throw SyncHttpException(response.code, json.optString("error", "HTTP ${response.code}"))
            }
            return json
        }
    }

    private suspend fun legacyCheckHasUpdate(): Boolean = withContext(Dispatchers.IO) {
        try {
            val lastUpdate = getLastUpdateTimestamp()
            val apiUrl = "${getRealApiUrl()}?action=check_update&last_update=${URLEncoder.encode(lastUpdate, "UTF-8")}" 
            client.newCall(Request.Builder().url(apiUrl).build()).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val body = response.body?.string() ?: "{}"
                JSONObject(body).optBoolean("has_update", false)
            }
        } catch (e: Exception) {
            Log.e("SongRepository", "Cannot check legacy updates", e)
            false
        }
    }

    suspend fun checkHasUpdate(): Boolean = withContext(Dispatchers.IO) {
        // Bản đầu tiên dùng Sync V2 phải bootstrap một lần từ DB hiện tại.
        if (!syncPrefs.getBoolean(KEY_BOOTSTRAP_DONE, false)) {
            return@withContext true
        }

        try {
            val cursor = syncPrefs.getLong(KEY_DELTA_CURSOR, 0L).coerceAtLeast(0L)
            val apiUrl = "${getRealApiUrl()}?action=check_update_v2&after=$cursor"
            val json = fetchJson(apiUrl, 20L)
            if (!json.has("has_update")) {
                throw SyncV2UnavailableException("Action check_update_v2 chưa có trên máy chủ")
            }
            json.optBoolean("has_update", false)
        } catch (e: SyncV2UnavailableException) {
            Log.w("SongRepository", "Sync V2 unavailable; checking legacy API", e)
            legacyCheckHasUpdate()
        } catch (e: Exception) {
            Log.e("SongRepository", "Cannot check updates", e)
            false
        }
    }

    suspend fun syncNewSongsOnline(): String = songSyncMutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val result = syncUsingV2()
                return@withContext if (result.total == 0) {
                    "Dữ liệu đã là phiên bản mới nhất."
                } else {
                    "Đã cập nhật ${result.upserted} bài và xóa ${result.deleted} bài cũ."
                }
            } catch (e: SyncV2UnavailableException) {
                // Cho phép phát hành PHP trước/sau Android mà không làm app cũ hoặc app mới bị ngắt.
                Log.w("SongRepository", "Falling back to legacy song sync", e)
                return@withContext syncLegacySongsOnline()
            } catch (e: SyncHttpException) {
                Log.e("SongRepository", "Song Sync V2 server error", e)
                return@withContext "Lỗi máy chủ: ${e.statusCode}"
            } catch (e: Exception) {
                Log.e("SongRepository", "Song synchronization failed", e)
                return@withContext "Lỗi mạng: Không thể đồng bộ dữ liệu"
            }
        }
    }

    private fun syncUsingV2(): SyncBatchResult {
        val total = SyncBatchResult()
        if (!syncPrefs.getBoolean(KEY_BOOTSTRAP_DONE, false)) {
            total += runBootstrapV2()
        }
        total += runDeltaSyncV2()
        return total
    }

    private fun runBootstrapV2(): SyncBatchResult {
        val total = SyncBatchResult()
        var pageGuard = 0

        while (!syncPrefs.getBoolean(KEY_BOOTSTRAP_DONE, false)) {
            pageGuard++
            if (pageGuard > 100_000) throw IOException("Bootstrap vượt quá số trang an toàn")

            val hasSavedProgress = syncPrefs.contains(KEY_BOOTSTRAP_TIME)
            val lastUpdate = if (hasSavedProgress) {
                syncPrefs.getString(KEY_BOOTSTRAP_TIME, "2000-01-01 00:00:00")
                    ?: "2000-01-01 00:00:00"
            } else {
                getLastUpdateTimestamp()
            }
            val lastId = syncPrefs.getString(KEY_BOOTSTRAP_ID, "").orEmpty()
            val hasSnapshot = syncPrefs.contains(KEY_BOOTSTRAP_SNAPSHOT)
            val snapshotCursor = if (hasSnapshot) {
                syncPrefs.getLong(KEY_BOOTSTRAP_SNAPSHOT, 0L).coerceAtLeast(0L)
            } else {
                null
            }

            val url = buildString {
                append(getRealApiUrl())
                append("?action=sync_bootstrap_v2")
                append("&last_update=")
                append(URLEncoder.encode(lastUpdate, "UTF-8"))
                append("&last_id=")
                append(URLEncoder.encode(lastId, "UTF-8"))
                append("&limit=")
                append(SYNC_PAGE_SIZE)
                if (snapshotCursor != null) {
                    append("&snapshot_cursor=")
                    append(snapshotCursor)
                }
            }

            val json = fetchJson(url, 90L)
            if (!json.has("items") || !json.has("snapshot_cursor")) {
                throw SyncV2UnavailableException("Action sync_bootstrap_v2 chưa có trên máy chủ")
            }

            val items = json.optJSONArray("items") ?: JSONArray()
            val pageResult = applySyncItems(items, deltaMode = false)
            total += pageResult

            val nextUpdate = json.optString("next_updated_at", lastUpdate)
            val nextId = json.optString("next_id", lastId)
            val returnedSnapshot = json.optLong("snapshot_cursor", snapshotCursor ?: 0L)
            val hasMore = json.optBoolean("has_more", false)

            if (hasMore && nextUpdate == lastUpdate && nextId == lastId) {
                throw IOException("Máy chủ không dịch chuyển cursor bootstrap")
            }

            val editor = syncPrefs.edit()
                .putString(KEY_BOOTSTRAP_TIME, nextUpdate)
                .putString(KEY_BOOTSTRAP_ID, nextId)
                .putLong(KEY_BOOTSTRAP_SNAPSHOT, returnedSnapshot)

            if (hasMore) {
                require(editor.commit()) { "Không lưu được tiến độ bootstrap" }
            } else {
                require(
                    editor
                        // Replay toàn bộ change log kể từ lúc cài migration để nhận cả các sự kiện delete
                        // đã phát sinh trước khi thiết bị bootstrap. Upsert lặp lại là an toàn.
                        .putLong(KEY_DELTA_CURSOR, 0L)
                        .putBoolean(KEY_BOOTSTRAP_DONE, true)
                        .remove(KEY_BOOTSTRAP_TIME)
                        .remove(KEY_BOOTSTRAP_ID)
                        .remove(KEY_BOOTSTRAP_SNAPSHOT)
                        .commit()
                ) { "Không lưu được cursor Sync V2" }
            }
        }

        return total
    }

    private fun runDeltaSyncV2(): SyncBatchResult {
        val total = SyncBatchResult()
        var cursor = syncPrefs.getLong(KEY_DELTA_CURSOR, 0L).coerceAtLeast(0L)
        var pageGuard = 0

        while (true) {
            pageGuard++
            if (pageGuard > 100_000) throw IOException("Delta sync vượt quá số trang an toàn")

            val url = "${getRealApiUrl()}?action=sync_v2&after=$cursor&limit=$SYNC_PAGE_SIZE"
            val json = fetchJson(url, 90L)
            if (!json.has("changes") || !json.has("next_cursor")) {
                throw SyncV2UnavailableException("Action sync_v2 chưa có trên máy chủ")
            }

            val changes = json.optJSONArray("changes") ?: JSONArray()
            val pageResult = applySyncItems(changes, deltaMode = true)
            total += pageResult

            val nextCursor = json.optLong("next_cursor", cursor).coerceAtLeast(cursor)
            val hasMore = json.optBoolean("has_more", false)

            if (hasMore && nextCursor <= cursor) {
                throw IOException("Máy chủ không dịch chuyển delta cursor")
            }

            // Chỉ nâng cursor sau khi cả metadata và lyrics của trang đã được ghi xong.
            require(syncPrefs.edit().putLong(KEY_DELTA_CURSOR, nextCursor).commit()) {
                "Không lưu được delta cursor"
            }
            cursor = nextCursor

            if (!hasMore) break
        }

        return total
    }

    private fun applySyncItems(items: JSONArray, deltaMode: Boolean): SyncBatchResult {
        if (items.length() == 0) return SyncBatchResult()

        val mainDb = dbHelper.writableDatabase
        val lyricsDb = lyricsDbHelper.writableDatabase
        val result = SyncBatchResult()

        val mainSql = """
            INSERT OR REPLACE INTO songs (
                id, title, originalKey, rhythm, chordList, lyrics, favoriteCount,
                viewCount, commentCount, ratingStars, ratingCount, genre,
                authorName, singerNames, youtubeUrl, createdAt, updatedAt,
                bpm, slug, image_count, id_zing_mp3, hasLyrics
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        val lyricsSql = "INSERT OR REPLACE INTO lyrics (song_id, lyrics_z, raw_bytes) VALUES (?, ?, ?)"
        val mainStatement = mainDb.compileStatement(mainSql)
        val lyricsStatement = lyricsDb.compileStatement(lyricsSql)

        var mainTransactionOpen = false
        var lyricsTransactionOpen = false
        try {
            mainDb.beginTransaction()
            mainTransactionOpen = true
            lyricsDb.beginTransaction()
            lyricsTransactionOpen = true

            for (index in 0 until items.length()) {
                if (deltaMode) {
                    val change = items.optJSONObject(index) ?: continue
                    val operation = change.optString("operation", "upsert")
                    val songId = change.optString("song_id", "")

                    if (operation == "delete") {
                        if (songId.isNotBlank() && !songId.startsWith("local_")) {
                            val deletedMain = mainDb.delete("songs", "id = ?", arrayOf(songId))
                            val deletedLyrics = lyricsDb.delete("lyrics", "song_id = ?", arrayOf(songId))
                            if (deletedMain > 0 || deletedLyrics > 0) result.deleted++
                        }
                    } else {
                        val song = change.optJSONObject("song") ?: continue
                        if (upsertSongFromJson(song, mainStatement, lyricsStatement)) result.upserted++
                    }
                } else {
                    val song = items.optJSONObject(index) ?: continue
                    if (upsertSongFromJson(song, mainStatement, lyricsStatement)) result.upserted++
                }
            }

            lyricsDb.setTransactionSuccessful()
            mainDb.setTransactionSuccessful()
        } finally {
            if (lyricsTransactionOpen) {
                try { lyricsDb.endTransaction() } catch (_: Exception) {}
            }
            if (mainTransactionOpen) {
                try { mainDb.endTransaction() } catch (_: Exception) {}
            }
            mainStatement.close()
            lyricsStatement.close()
        }

        return result
    }

    private fun upsertSongFromJson(
        songData: JSONObject,
        mainStatement: android.database.sqlite.SQLiteStatement,
        lyricsStatement: android.database.sqlite.SQLiteStatement
    ): Boolean {
        val songId = songData.stringOrEmpty("id")
        if (songId.isBlank() || songId.startsWith("local_")) return false

        val lyricsText = songData.stringOrEmpty("lyrics")
        val compressed = compressLyrics(lyricsText)

        lyricsStatement.clearBindings()
        lyricsStatement.bindString(1, songId)
        lyricsStatement.bindBlob(2, compressed)
        lyricsStatement.bindLong(3, lyricsText.toByteArray(Charsets.UTF_8).size.toLong())
        lyricsStatement.executeInsert()

        mainStatement.clearBindings()
        mainStatement.bindString(1, songId)
        mainStatement.bindString(2, songData.stringOrEmpty("title"))
        mainStatement.bindString(3, songData.stringOrEmpty("originalKey"))
        mainStatement.bindString(4, songData.stringOrEmpty("rhythm"))
        mainStatement.bindString(5, songData.stringOrEmpty("chordList"))
        mainStatement.bindString(6, "")
        mainStatement.bindLong(7, songData.optLong("favoriteCount", 0L))
        mainStatement.bindLong(8, songData.optLong("viewCount", 0L))
        mainStatement.bindLong(9, songData.optLong("commentCount", 0L))
        mainStatement.bindDouble(10, songData.optDouble("ratingStars", 0.0))
        mainStatement.bindLong(11, songData.optLong("ratingCount", 0L))
        mainStatement.bindString(12, songData.stringOrEmpty("genre"))
        mainStatement.bindString(13, songData.stringOrEmpty("authorName"))
        mainStatement.bindString(14, songData.stringOrEmpty("singerNames"))
        mainStatement.bindString(15, songData.stringOrEmpty("youtubeUrl"))
        mainStatement.bindString(16, songData.stringOrEmpty("createdAt"))
        mainStatement.bindString(17, songData.stringOrEmpty("updatedAt"))
        mainStatement.bindLong(18, songData.optLong("bpm", 0L))
        mainStatement.bindString(19, songData.stringOrEmpty("slug"))
        mainStatement.bindLong(20, songData.optLong("image_count", 0L))
        mainStatement.bindString(21, songData.stringOrEmpty("id_zing_mp3"))
        mainStatement.bindLong(22, if (lyricsText.isNotBlank()) 1L else 0L)
        mainStatement.executeInsert()
        return true
    }

    private fun syncLegacySongsOnline(): String {
        try {
            val lastUpdate = getLastUpdateTimestamp()
            val apiUrl = "${getRealApiUrl()}?action=sync&last_update=${URLEncoder.encode(lastUpdate, "UTF-8")}" 
            val syncClient = client.newBuilder().readTimeout(90, TimeUnit.SECONDS).build()

            syncClient.newCall(Request.Builder().url(apiUrl).build()).execute().use { response ->
                if (!response.isSuccessful) return "Lỗi máy chủ: ${response.code}"

                val inputStream = response.body?.byteStream() ?: return "Dữ liệu trống"
                val mainDb = dbHelper.writableDatabase
                val lyricsDb = lyricsDbHelper.writableDatabase

                val mainSql = """
                    INSERT OR REPLACE INTO songs (
                        id, title, originalKey, rhythm, chordList, lyrics, favoriteCount,
                        viewCount, commentCount, ratingStars, ratingCount, genre,
                        authorName, singerNames, youtubeUrl, createdAt, updatedAt,
                        bpm, slug, image_count, id_zing_mp3, hasLyrics
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                val lyricsSql = "INSERT OR REPLACE INTO lyrics (song_id, lyrics_z, raw_bytes) VALUES (?, ?, ?)"
                val mainStatement = mainDb.compileStatement(mainSql)
                val lyricsStatement = lyricsDb.compileStatement(lyricsSql)

                var lyricsTransactionOpen = false
                mainDb.beginTransaction()
                lyricsDb.beginTransaction()
                lyricsTransactionOpen = true
                try {
                    val reader = android.util.JsonReader(java.io.InputStreamReader(inputStream, Charsets.UTF_8))
                    var count = 0
                    reader.beginArray()
                    while (reader.hasNext()) {
                        val songData = HashMap<String, String>()
                        reader.beginObject()
                        while (reader.hasNext()) {
                            val key = reader.nextName()
                            when (reader.peek()) {
                                android.util.JsonToken.NULL -> {
                                    reader.nextNull()
                                    songData[key] = ""
                                }
                                android.util.JsonToken.BOOLEAN -> songData[key] = reader.nextBoolean().toString()
                                android.util.JsonToken.STRING,
                                android.util.JsonToken.NUMBER -> songData[key] = reader.nextString()
                                else -> reader.skipValue()
                            }
                        }
                        reader.endObject()

                        val songId = songData["id"] ?: ""
                        if (songId.isBlank() || songId.startsWith("local_")) continue
                        val lyricsText = songData["lyrics"] ?: ""
                        val compressed = compressLyrics(lyricsText)

                        lyricsStatement.clearBindings()
                        lyricsStatement.bindString(1, songId)
                        lyricsStatement.bindBlob(2, compressed)
                        lyricsStatement.bindLong(3, lyricsText.toByteArray(Charsets.UTF_8).size.toLong())
                        lyricsStatement.executeInsert()

                        mainStatement.clearBindings()
                        mainStatement.bindString(1, songId)
                        mainStatement.bindString(2, songData["title"] ?: "")
                        mainStatement.bindString(3, songData["originalKey"] ?: "")
                        mainStatement.bindString(4, songData["rhythm"] ?: "")
                        mainStatement.bindString(5, songData["chordList"] ?: "")
                        mainStatement.bindString(6, "")
                        mainStatement.bindLong(7, (songData["favoriteCount"] ?: "0").toLongOrNull() ?: 0L)
                        mainStatement.bindLong(8, (songData["viewCount"] ?: "0").toLongOrNull() ?: 0L)
                        mainStatement.bindLong(9, (songData["commentCount"] ?: "0").toLongOrNull() ?: 0L)
                        mainStatement.bindDouble(10, (songData["ratingStars"] ?: "0.0").toDoubleOrNull() ?: 0.0)
                        mainStatement.bindLong(11, (songData["ratingCount"] ?: "0").toLongOrNull() ?: 0L)
                        mainStatement.bindString(12, songData["genre"] ?: "")
                        mainStatement.bindString(13, songData["authorName"] ?: "")
                        mainStatement.bindString(14, songData["singerNames"] ?: "")
                        mainStatement.bindString(15, songData["youtubeUrl"] ?: "")
                        mainStatement.bindString(16, songData["createdAt"] ?: "")
                        mainStatement.bindString(17, songData["updatedAt"] ?: "")
                        mainStatement.bindLong(18, (songData["bpm"] ?: "0").toLongOrNull() ?: 0L)
                        mainStatement.bindString(19, songData["slug"] ?: "")
                        mainStatement.bindLong(20, (songData["image_count"] ?: "0").toLongOrNull() ?: 0L)
                        mainStatement.bindString(21, songData["id_zing_mp3"] ?: "")
                        mainStatement.bindLong(22, if (lyricsText.isNotBlank()) 1L else 0L)
                        mainStatement.executeInsert()
                        count++
                    }
                    reader.endArray()
                    reader.close()

                    lyricsDb.setTransactionSuccessful()
                    lyricsDb.endTransaction()
                    lyricsTransactionOpen = false
                    mainDb.setTransactionSuccessful()
                    return "Đã cập nhật thành công $count bài hát mới!"
                } finally {
                    if (lyricsTransactionOpen) {
                        try { lyricsDb.endTransaction() } catch (_: Exception) {}
                    }
                    mainDb.endTransaction()
                    mainStatement.close()
                    lyricsStatement.close()
                }
            }
        } catch (e: Exception) {
            Log.e("SongRepository", "Legacy song synchronization failed", e)
            return "Lỗi mạng: Không thể đồng bộ dữ liệu"
        }
    }

    suspend fun deleteSongOffline(songId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val deletedMain = dbHelper.writableDatabase.delete("songs", "id = ?", arrayOf(songId))
            lyricsDbHelper.writableDatabase.delete("lyrics", "song_id = ?", arrayOf(songId))
            deletedMain > 0
        } catch (e: Exception) {
            Log.e("SongRepository", "Cannot delete song $songId", e)
            false
        }
    }

    suspend fun deleteCustomSongLocal(title: String, singer: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val mainDb = dbHelper.writableDatabase
            val ids = mutableListOf<String>()
            mainDb.rawQuery(
                "SELECT id FROM songs WHERE title = ? COLLATE NOCASE AND singerNames = ? COLLATE NOCASE AND id GLOB 'local_*'",
                arrayOf(title.trim(), singer.trim())
            ).use { cursor ->
                while (cursor.moveToNext()) ids.add(cursor.getString(0) ?: "")
            }

            val deleted = mainDb.delete(
                "songs",
                "title = ? COLLATE NOCASE AND singerNames = ? COLLATE NOCASE AND id GLOB 'local_*'",
                arrayOf(title.trim(), singer.trim())
            )
            val lyricsDb = lyricsDbHelper.writableDatabase
            ids.forEach { id -> lyricsDb.delete("lyrics", "song_id = ?", arrayOf(id)) }
            deleted > 0
        } catch (e: Exception) {
            Log.e("SongRepository", "Cannot delete local chord version", e)
            false
        }
    }

    suspend fun updateZingIdOffline(songId: String, zingId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply { put("id_zing_mp3", zingId) }
            dbHelper.writableDatabase.update("songs", values, "id = ?", arrayOf(songId)) > 0
        } catch (e: Exception) {
            Log.e("SongRepository", "Cannot update Zing ID", e)
            false
        }
    }

    suspend fun incrementViewCountLocal(songId: String) = withContext(Dispatchers.IO) {
        try {
            dbHelper.writableDatabase.execSQL(
                "UPDATE songs SET viewCount = viewCount + 1 WHERE id = ?",
                arrayOf(songId)
            )
        } catch (e: Exception) {
            Log.e("SongRepository", "Cannot increment view count", e)
        }
    }

    suspend fun insertCustomSongLocal(song: SongDetails): Boolean = withContext(Dispatchers.IO) {
        val newId = if (song.songId.startsWith("local_")) {
            song.songId
        } else {
            val fingerprint = "${song.title.trim().lowercase()}|${song.singer.trim().lowercase()}"
            val hash = MessageDigest.getInstance("SHA-256")
                .digest(fingerprint.toByteArray(Charsets.UTF_8))
                .take(10)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            "local_$hash"
        }
        try {
            if (!saveLyricsBlocking(newId, song.lyrics)) return@withContext false
            val now = java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                java.util.Locale.getDefault()
            ).format(java.util.Date())

            val values = ContentValues().apply {
                put("id", newId)
                put("title", song.title)
                put("originalKey", song.baseTone)
                put("rhythm", song.rhythm)
                put("chordList", "")
                put("lyrics", "")
                put("favoriteCount", song.likes)
                put("viewCount", 0)
                put("commentCount", 0)
                put("ratingStars", 0f)
                put("ratingCount", 0)
                put("genre", "")
                put("authorName", song.composer)
                put("singerNames", song.singer)
                put("youtubeUrl", song.videoUrl)
                put("createdAt", now)
                put("updatedAt", now)
                put("bpm", song.bpm.toIntOrNull() ?: 0)
                put("slug", removeAccents(song.title).replace(Regex("\\s+"), "-"))
                put("image_count", song.imageCount)
                put("id_zing_mp3", song.zingMp3Id)
                put("hasLyrics", if (song.lyrics.isNotBlank()) 1 else 0)
            }

            val rowId = dbHelper.writableDatabase.insertWithOnConflict(
                "songs",
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
            if (rowId == -1L) {
                lyricsDbHelper.writableDatabase.delete("lyrics", "song_id = ?", arrayOf(newId))
                false
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e("SongRepository", "Cannot save local chord version", e)
            try { lyricsDbHelper.writableDatabase.delete("lyrics", "song_id = ?", arrayOf(newId)) } catch (_: Exception) {}
            false
        }
    }

    suspend fun getSongByExactIdOrTitle(
        songId: String,
        songTitle: String,
        singer: String = "",
        includeLyrics: Boolean = true
    ): Song? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase

        fun find(sql: String, args: Array<String>): Song? {
            var result: Song? = null
            db.rawQuery(sql, args).use { cursor ->
                if (cursor.moveToFirst()) {
                    result = mapCursorToSong(cursor, includeLyrics)
                }
            }
            return result
        }

        var song: Song? = null
        if (songId.isNotBlank()) {
            song = find("SELECT * FROM songs WHERE id = ? LIMIT 1", arrayOf(songId))
        }
        if (song == null && songId.isNotBlank()) {
            song = find("SELECT * FROM songs WHERE youtubeUrl LIKE ? LIMIT 1", arrayOf("%$songId%"))
        }
        if (song == null && songId.isNotBlank()) {
            song = find("SELECT * FROM songs WHERE id_zing_mp3 = ? LIMIT 1", arrayOf(songId))
        }
        if (song == null && songTitle.isNotBlank()) {
            song = if (singer.isNotBlank()) {
                find(
                    "SELECT * FROM songs WHERE title = ? COLLATE NOCASE AND singerNames LIKE ? LIMIT 1",
                    arrayOf(songTitle.trim(), "%${singer.trim()}%")
                )
            } else {
                find(
                    "SELECT * FROM songs WHERE title = ? COLLATE NOCASE LIMIT 1",
                    arrayOf(songTitle.trim())
                )
            }
        }
        if (song == null && songTitle.isNotBlank()) {
            song = if (singer.isNotBlank()) {
                find(
                    "SELECT * FROM songs WHERE title LIKE ? AND singerNames LIKE ? LIMIT 1",
                    arrayOf("%${songTitle.trim()}%", "%${singer.trim()}%")
                )
            } else {
                find(
                    "SELECT * FROM songs WHERE title LIKE ? LIMIT 1",
                    arrayOf("%${songTitle.trim()}%")
                )
            }
        }
        song
    }
}

// ==========================================
object SongScraper {

    private val API_URL: String
        get() {
            return "https://tunertools.top/api_songs.php"
        }

    private val obfuscatedKey = intArrayOf(
        12, 45, 54, 61, 42, 25, 40, 40, 7, 11, 61, 59, 42, 61, 44, 19,
        61, 33, 7, 106, 104, 106, 110, 7, 24, 8, 48, 49, 20, 45, 59, 121
    )

    private fun getSecretKey(): String {
        val sb = StringBuilder()
        for (i in obfuscatedKey.indices) sb.append((obfuscatedKey[i] xor 88).toChar())
        return sb.toString()
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun applySecurityHeaders(connection: HttpURLConnection, action: String) {
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val signature = md5(timestamp + getSecretKey() + action)
        connection.setRequestProperty("User-Agent", "TunerApp-Android-v1.02")
        connection.setRequestProperty("X-App-Timestamp", timestamp)
        connection.setRequestProperty("X-App-Signature", signature)
    }
    private fun applySecurityHeadersOkHttp(builder: okhttp3.Request.Builder, action: String) {
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val signature = md5(timestamp + getSecretKey() + action)
        builder.header("User-Agent", "TunerApp-Android-v1.02")
        builder.header("X-App-Timestamp", timestamp)
        builder.header("X-App-Signature", signature)
    }

    fun getLyricsSample(lyrics: String): String {
        if (lyrics.isBlank()) return ""
        val lines = lyrics.split("\n")
        for (line in lines) {
            val clean = line.replace(Regex("\\[.*?\\]"), "").replace(Regex("[.,?!]"), "").trim()
            val words = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.size >= 4) {
                return words.take(6).joinToString(" ")
            }
        }
        return ""
    }

    suspend fun getZingMp3Karaoke(zingId: String, title: String, singer: String, firstSentence: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val client = okhttp3.OkHttpClient.Builder()
                // Tăng thời gian chờ lên 120s (2 phút) để Máy chủ Python có đủ thời gian lùng sục bài hát khớp lời
                .connectTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val formBody = okhttp3.FormBody.Builder()
                .add("zing_id", zingId)
                .add("title", title)
                .add("singer", singer)
                .add("first_sentence", firstSentence)
                .build()

            val request = okhttp3.Request.Builder()
                .url("https://api.tunertools.top/api/search_zing_mp3")
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                return@withContext org.json.JSONObject(responseBody)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun updateZingIdOnline(songId: String, zingId: String): Boolean = withContext(Dispatchers.IO) {
        if (songId.isBlank() || zingId.isBlank()) return@withContext false
        try {
            val action = "update_zing_id"
            val url = URL("$API_URL?action=$action")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            applySecurityHeaders(connection, action)

            val postData = "id=${URLEncoder.encode(songId, "UTF-8")}&zing_id=${URLEncoder.encode(zingId, "UTF-8")}"
            connection.outputStream.write(postData.toByteArray(Charsets.UTF_8))

            return@withContext connection.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    suspend fun getZingAcapellaSync(zingId: String, title: String, singer: String, firstSentence: String, mode: String, onProgressUpdate: ((Float, String) -> Unit)? = null): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val formBody = okhttp3.FormBody.Builder()
                .add("zing_id", zingId)
                .add("title", title)
                .add("singer", singer)
                .add("first_sentence", firstSentence)
                .add("mode", mode) // Truyền tham số Mode
                .build()

            val request = okhttp3.Request.Builder()
                .url("https://api.tunertools.top/api/zing_acapella_sync")
                .post(formBody)
                .build()

            // 1. Gửi yêu cầu khởi tạo tiến trình lên máy chủ (Nhận diện Cache hoặc nhận Task ID ngay lập tức)
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val responseBody = response.body?.string() ?: ""
            val jsonObj = org.json.JSONObject(responseBody)

            // 2. Nếu máy chủ đã có sẵn Cache, trả về dữ liệu luôn
            if (jsonObj.has("karaoke_data")) {
                return@withContext jsonObj
            }

            // 3. Nếu máy chủ trả về mã Task ID, tiến hành vòng lặp "Hỏi thăm" (Polling)
            val taskId = jsonObj.optString("task_id", "")
            if (taskId.isNotEmpty()) {
                while (true) {
                    // Ngủ 2 giây để thanh Progress trơn tru hơn
                    kotlinx.coroutines.delay(2000)

                    val checkReq = okhttp3.Request.Builder()
                        .url("https://api.tunertools.top/api/check_status/$taskId")
                        .get()
                        .build()

                    val checkRes = client.newCall(checkReq).execute()
                    if (checkRes.isSuccessful) {
                        val statusBody = checkRes.body?.string() ?: ""
                        val statusObj = org.json.JSONObject(statusBody)
                        val status = statusObj.optString("status")

                        if (status == "completed") {
                            return@withContext statusObj.optJSONObject("data")
                        } else if (status == "error") {
                            return@withContext null
                        } else {
                            // Cập nhật tiến trình về UI
                            val currentProgress = statusObj.optDouble("progress", -1.0).toFloat()
                            val message = statusObj.optString("message", "AI đang phân tích...")
                            if (currentProgress >= 0f) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    onProgressUpdate?.invoke(currentProgress / 100f, message)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun searchSongsList(query: String): List<SearchResultItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SearchResultItem>()
        if (query.isBlank()) return@withContext list
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val targetUrl = "$API_URL?action=search&q=$encodedQuery"
            val url = URL(targetUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000; connection.readTimeout = 10000; connection.requestMethod = "GET"
            applySecurityHeaders(connection, "search")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val chordsList = mutableListOf<String>()
                    val chordsArray = obj.optJSONArray("chords")
                    if (chordsArray != null) { for (j in 0 until chordsArray.length()) chordsList.add(chordsArray.getString(j)) }
                    list.add(SearchResultItem(
                        title = obj.optString("title", "Không tên"), singer = obj.optString("singer", "Đang cập nhật"), composer = obj.optString("composer", "Đang cập nhật"),
                        url = obj.optString("songId", ""), zingMp3Url = obj.optString("zingMp3Url", obj.optString("zing_mp3_url", "")), videoUrl = obj.optString("videoUrl", ""), chords = chordsList,
                        slug = obj.optString("slug", ""), imageCount = obj.optInt("imageCount", obj.optInt("image_count", 0))
                    ))
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return@withContext list
    }

    suspend fun deleteServerCachePython(videoId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val formBody = okhttp3.FormBody.Builder()
                .add("video_id", videoId)
                .build()

            val request = okhttp3.Request.Builder()
                .url("https://api.tunertools.top/api/delete_cache")
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()
            return@withContext response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    suspend fun getAvailableCachedSongs(): List<SearchResultItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SearchResultItem>()
        try {
            val connection = URL("https://api.tunertools.top/api/get_cached_songs").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            applySecurityHeaders(connection, "get_cached_songs")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(jsonString)
                val songsArray = jsonObject.optJSONArray("songs")
                if (songsArray != null) {
                    for (i in 0 until songsArray.length()) {
                        val obj = songsArray.getJSONObject(i)
                        val chordsList = mutableListOf<String>()
                        val chordsArray = obj.optJSONArray("chords")
                        if (chordsArray != null) {
                            for (j in 0 until chordsArray.length()) {
                                chordsList.add(chordsArray.getString(j))
                            }
                        }

                        list.add(SearchResultItem(
                            title = obj.optString("title"),
                            singer = obj.optString("singer"),
                            composer = "",
                            url = obj.optString("id"),
                            zingMp3Url = "",
                            videoUrl = obj.optString("youtube_url", ""),
                            chords = chordsList,
                            hasLyricsCache = obj.optBoolean("has_lyrics_cache", false),
                            hasAudioCache = obj.optBoolean("has_audio_cache", false)
                        ))
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return@withContext list
    }
    suspend fun getFavoriteIdsOnline(email: String): List<String>? = withContext(Dispatchers.IO) {
        val ids = mutableListOf<String>()
        try {
            val encodedEmail = URLEncoder.encode(email, "UTF-8")
            val url = URL("$API_URL?action=get_favorite_ids&email=$encodedEmail")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000; connection.readTimeout = 10000; connection.requestMethod = "GET"
            applySecurityHeaders(connection, "get_favorite_ids")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    ids.add(jsonArray.getString(i))
                }
                return@withContext ids // Trả về danh sách khi thành công
            } else {
                return@withContext null // Lỗi phản hồi từ server
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null // QUAN TRỌNG: Trả về null khi mất kết nối mạng
        }
    }

    suspend fun getFavoriteSongs(email: String): List<SearchResultItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SearchResultItem>()
        try {
            val encodedEmail = URLEncoder.encode(email, "UTF-8")
            val url = URL("$API_URL?action=favorites&email=$encodedEmail")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000; connection.readTimeout = 10000; connection.requestMethod = "GET"
            applySecurityHeaders(connection, "favorites")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val chordsList = mutableListOf<String>()
                    val chordsArray = obj.optJSONArray("chords")
                    if (chordsArray != null) { for (j in 0 until chordsArray.length()) chordsList.add(chordsArray.getString(j)) }
                    list.add(SearchResultItem(
                        title = obj.optString("title", "Không tên"), singer = obj.optString("singer", "Đang cập nhật"), composer = obj.optString("composer", "Đang cập nhật"),
                        url = obj.optString("songId", ""), zingMp3Url = obj.optString("zingMp3Url", obj.optString("zing_mp3_url", "")), videoUrl = obj.optString("videoUrl", ""), chords = chordsList,
                        slug = obj.optString("slug", ""), imageCount = obj.optInt("imageCount", obj.optInt("image_count", 0))
                    ))
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return@withContext list
    }

    suspend fun fetchSongDetails(songId: String, email: String): SongDetails = withContext(Dispatchers.IO) {
        try {
            val encodedId = URLEncoder.encode(songId, "UTF-8"); val encodedEmail = URLEncoder.encode(email, "UTF-8")
            val url = URL("$API_URL?action=detail&id=$encodedId&email=$encodedEmail")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000; connection.readTimeout = 10000; connection.requestMethod = "GET"
            applySecurityHeaders(connection, "detail")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(jsonString)
                if (obj.has("error")) return@withContext SongDetails(error = obj.optString("error"))

                return@withContext SongDetails(
                    title = obj.optString("title", ""), singer = obj.optString("singer", "Đang cập nhật"), composer = obj.optString("composer", "Đang cập nhật"),
                    baseTone = obj.optString("baseTone", "C"), rhythm = obj.optString("rhythm", ""), bpm = obj.optString("bpm", ""),
                    videoUrl = obj.optString("videoUrl", ""), lyrics = obj.optString("lyrics", ""), songId = obj.optString("songId", ""),
                    likes = obj.optInt("likes", 0), isLikedByDevice = obj.optBoolean("isLikedByDevice", false),
                    slug = obj.optString("slug", ""), imageCount = obj.optInt("imageCount", obj.optInt("image_count", 0))
                )
            }
        } catch (e: Exception) { e.printStackTrace() }
        return@withContext SongDetails(error = "Không thể kết nối máy chủ.")
    }

    suspend fun updateSongWithImages(context: Context, song: SongDetails, keptOldIndices: List<Int>, newImageUris: List<android.net.Uri>): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val multipartBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("id", song.songId)
                .addFormDataPart("title", song.title)
                .addFormDataPart("singer", song.singer)
                .addFormDataPart("composer", song.composer)
                .addFormDataPart("baseTone", song.baseTone)
                .addFormDataPart("rhythm", song.rhythm)
                .addFormDataPart("bpm", song.bpm)
                .addFormDataPart("videoUrl", song.videoUrl)
                .addFormDataPart("lyrics", song.lyrics)
                .addFormDataPart("kept_old_indices", keptOldIndices.joinToString(","))

            // Đóng gói các file ảnh MỚI được chọn để gửi lên PHP
            for (i in newImageUris.indices) {
                val uri = newImageUris[i]
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null) {
                    val requestBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                    multipartBuilder.addFormDataPart("sheet_images[]", "new_image_$i.jpg", requestBody)
                }
            }

            val requestBuilder = okhttp3.Request.Builder()
                .url("$API_URL?action=update")
                .post(multipartBuilder.build())
            applySecurityHeadersOkHttp(requestBuilder, "update")

            val response = client.newCall(requestBuilder.build()).execute()
            return@withContext response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    suspend fun addSongWithImages(context: Context, song: SongDetails, imageUris: List<android.net.Uri>, userRole: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val multipartBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("title", song.title)
                .addFormDataPart("singer", song.singer)
                .addFormDataPart("composer", song.composer)
                .addFormDataPart("baseTone", song.baseTone)
                .addFormDataPart("rhythm", song.rhythm)
                .addFormDataPart("bpm", song.bpm)
                .addFormDataPart("videoUrl", song.videoUrl)
                .addFormDataPart("lyrics", song.lyrics)
                .addFormDataPart("role", userRole)

            // Đóng gói tất cả file ảnh để gửi lên PHP
            for (i in imageUris.indices) {
                val uri = imageUris[i]
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null) {
                    val requestBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                    multipartBuilder.addFormDataPart("sheet_images[]", "image_$i.jpg", requestBody)
                }
            }

            val requestBuilder = okhttp3.Request.Builder()
                .url("$API_URL?action=add")
                .post(multipartBuilder.build())
            applySecurityHeadersOkHttp(requestBuilder, "add")

            val response = client.newCall(requestBuilder.build()).execute()
            return@withContext response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }


    suspend fun toggleLike(songId: String, email: String, isLiking: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$API_URL?action=toggle_like&id=${URLEncoder.encode(songId, "UTF-8")}&email=${URLEncoder.encode(email, "UTF-8")}&is_liking=$isLiking")
            val connection = url.openConnection() as HttpURLConnection; connection.requestMethod = "GET"
            applySecurityHeaders(connection, "toggle_like")
            return@withContext connection.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) { return@withContext false }
    }

    suspend fun deleteSong(songId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$API_URL?action=delete"); val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"; connection.doOutput = true; connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            applySecurityHeaders(connection, "delete")
            connection.outputStream.write("id=${URLEncoder.encode(songId, "UTF-8")}".toByteArray(Charsets.UTF_8))
            return@withContext connection.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) { return@withContext false }
    }

    suspend fun translateText(text: String, targetLang: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank() || targetLang.isBlank()) return@withContext ""
        try {
            val chordMap = mutableListOf<String>()
            var textWithMarkers = text
            Regex("\\[.*?\\]").findAll(text).forEachIndexed { index, matchResult ->
                chordMap.add(matchResult.value)
                textWithMarkers = textWithMarkers.replaceFirst(matchResult.value, " <m$index> ")
            }
            val url = URL("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLang&dt=t")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"; connection.doOutput = true; connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.outputStream.write("q=${URLEncoder.encode(textWithMarkers, "UTF-8")}".toByteArray(Charsets.UTF_8))

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val parts = JSONArray(connection.inputStream.bufferedReader().use { it.readText() }).getJSONArray(0)
                var translatedText = ""
                for (i in 0 until parts.length()) translatedText += parts.getJSONArray(i).getString(0)
                chordMap.forEachIndexed { index, originalChord -> translatedText = translatedText.replace(Regex("<\\s*m\\s*$index\\s*>"), originalChord) }
                return@withContext translatedText.replace("  ", " ").trim()
            }
        } catch (e: Exception) { e.printStackTrace() }
        return@withContext "[⚠️ Lỗi kết nối]"
    }

    suspend fun getUserRole(email: String): String = withContext(Dispatchers.IO) {
        if (email.isBlank()) return@withContext "user"
        try {
            val url = URL("$API_URL?action=check_role&email=${URLEncoder.encode(email, "UTF-8")}")
            val connection = url.openConnection() as HttpURLConnection; connection.requestMethod = "GET"
            applySecurityHeaders(connection, "check_role")
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                return@withContext JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).optString("role", "user")
            }
        } catch (e: Exception) { e.printStackTrace() }
        return@withContext "user"
    }

    suspend fun getAdmins(): List<AdminAccount> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AdminAccount>()
        try {
            val url = URL("$API_URL?action=get_admins")
            val connection = url.openConnection() as HttpURLConnection; connection.requestMethod = "GET"
            applySecurityHeaders(connection, "get_admins")
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonArray = JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(AdminAccount(obj.optString("id"), obj.optString("email"), obj.optString("role"), obj.optString("created_at")))
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return@withContext list
    }

    suspend fun addAdmin(email: String, role: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val action = "add_admin"
            val url = URL("$API_URL?action=$action")
            val connection = url.openConnection() as HttpURLConnection; connection.requestMethod = "POST"; connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            applySecurityHeaders(connection, action)
            connection.outputStream.write("email=${URLEncoder.encode(email, "UTF-8")}&role=${URLEncoder.encode(role, "UTF-8")}".toByteArray(Charsets.UTF_8))
            return@withContext connection.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) { return@withContext false }
    }

    suspend fun updateAdminRole(id: String, role: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val action = "update_admin"
            val url = URL("$API_URL?action=$action")
            val connection = url.openConnection() as HttpURLConnection; connection.requestMethod = "POST"; connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            applySecurityHeaders(connection, action)
            connection.outputStream.write("id=${URLEncoder.encode(id, "UTF-8")}&role=${URLEncoder.encode(role, "UTF-8")}".toByteArray(Charsets.UTF_8))
            return@withContext connection.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) { return@withContext false }
    }

    suspend fun submitReport(songId: String, deviceId: String, reason: String, note: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val action = "report"; val url = URL("$API_URL?action=$action")
            val connection = url.openConnection() as HttpURLConnection; connection.requestMethod = "POST"; connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            applySecurityHeaders(connection, action)
            val postData = "song_id=${URLEncoder.encode(songId, "UTF-8")}&device_id=${URLEncoder.encode(deviceId, "UTF-8")}&reason=${URLEncoder.encode(reason, "UTF-8")}&note=${URLEncoder.encode(note, "UTF-8")}"
            connection.outputStream.write(postData.toByteArray(Charsets.UTF_8))
            return@withContext connection.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) { return@withContext false }
    }

    suspend fun submitContactSupport(deviceId: String, subject: String, message: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val action = "contact_support"; val url = URL("$API_URL?action=$action")
            val connection = url.openConnection() as HttpURLConnection; connection.requestMethod = "POST"; connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            applySecurityHeaders(connection, action)
            val postData = "device_id=${URLEncoder.encode(deviceId, "UTF-8")}&subject=${URLEncoder.encode(subject, "UTF-8")}&message=${URLEncoder.encode(message, "UTF-8")}"
            connection.outputStream.write(postData.toByteArray(Charsets.UTF_8))
            return@withContext connection.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) { return@withContext false }
    }

    suspend fun getSupportData(deviceId: String, isAdmin: Boolean): Pair<Int, List<SupportTicket>> = withContext(Dispatchers.IO) {
        val resultList = mutableListOf<SupportTicket>()
        var unreadCount = 0
        try {
            val action = "get_support_data"
            val url = URL("$API_URL?action=$action&device_id=${URLEncoder.encode(deviceId, "UTF-8")}&is_admin=$isAdmin")
            val connection = url.openConnection() as HttpURLConnection; connection.requestMethod = "GET"; connection.useCaches = false
            applySecurityHeaders(connection, action)

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonObject = JSONObject(connection.inputStream.bufferedReader().readText())
                unreadCount = jsonObject.optInt("unread_count", 0)
                val ticketsArray = jsonObject.optJSONArray("tickets")
                if (ticketsArray != null) {
                    for (i in 0 until ticketsArray.length()) {
                        val obj = ticketsArray.getJSONObject(i)
                        val chatHistoryList = mutableListOf<ChatMessage>()
                        val chatHistoryStr = obj.optString("chat_history", "")
                        if (chatHistoryStr.isNotEmpty() && chatHistoryStr != "null") {
                            val arr = JSONArray(chatHistoryStr)
                            for (j in 0 until arr.length()) {
                                val msgObj = arr.getJSONObject(j)
                                chatHistoryList.add(ChatMessage(msgObj.optString("sender"), msgObj.optString("text"), msgObj.optString("timestamp")))
                            }
                        } else {
                            chatHistoryList.add(ChatMessage("user", obj.optString("message"), obj.optString("created_at")))
                            val adminReply = obj.optString("admin_reply")
                            if (adminReply.isNotEmpty() && adminReply != "null") { chatHistoryList.add(ChatMessage("admin", adminReply, "")) }
                        }
                        resultList.add(SupportTicket(
                            id = obj.optString("id"), type = obj.optString("type"), deviceId = obj.optString("device_id"),
                            subject = obj.optString("subject"), message = obj.optString("message"),
                            chatHistory = chatHistoryList, status = obj.optInt("status", 0), userRead = obj.optInt("user_read", 0),
                            date = obj.optString("created_at"), userEmail = obj.optString("user_email", "")
                        ))
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return@withContext Pair(unreadCount, resultList)
    }

    suspend fun replyTicket(type: String, id: String, senderRole: String, replyText: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val action = "reply_ticket"; val url = URL("$API_URL?action=$action")
            val connection = url.openConnection() as HttpURLConnection; connection.requestMethod = "POST"; connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            applySecurityHeaders(connection, action)
            val postData = "type=${URLEncoder.encode(type, "UTF-8")}&id=${URLEncoder.encode(id, "UTF-8")}&sender=${URLEncoder.encode(senderRole, "UTF-8")}&reply=${URLEncoder.encode(replyText, "UTF-8")}"
            connection.outputStream.write(postData.toByteArray(Charsets.UTF_8))
            return@withContext connection.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) { e.printStackTrace(); return@withContext false }
    }

    suspend fun markUserRead(type: String, id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val action = "mark_user_read"; val url = URL("$API_URL?action=$action")
            val connection = url.openConnection() as HttpURLConnection; connection.requestMethod = "POST"; connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            applySecurityHeaders(connection, action)
            connection.outputStream.write("type=${URLEncoder.encode(type, "UTF-8")}&id=${URLEncoder.encode(id, "UTF-8")}".toByteArray(Charsets.UTF_8))
            return@withContext connection.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) { return@withContext false }
    }

    suspend fun getPendingSongs(): List<SearchResultItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SearchResultItem>()
        try {
            val url = URL("$API_URL?action=get_pending_songs")
            val connection = url.openConnection() as HttpURLConnection; connection.requestMethod = "GET"
            applySecurityHeaders(connection, "get_pending_songs")
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonArray = JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(SearchResultItem(
                        title = obj.optString("title"), singer = obj.optString("singer"), composer = obj.optString("composer"),
                        url = obj.optString("songId"), zingMp3Url = "", videoUrl = "", chords = emptyList(),
                        slug = obj.optString("slug"), imageCount = obj.optInt("imageCount")
                    ))
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return@withContext list
    }

    suspend fun approveSong(songId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val action = "approve_song"; val url = URL("$API_URL?action=$action")
            val connection = url.openConnection() as HttpURLConnection; connection.requestMethod = "POST"; connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            applySecurityHeaders(connection, action)
            connection.outputStream.write("id=${URLEncoder.encode(songId, "UTF-8")}".toByteArray(Charsets.UTF_8))
            return@withContext connection.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) { return@withContext false }
    }

    // --- HÀM ĐĂNG THÔNG BÁO TỪ APP (ADMIN) ---
    suspend fun postSystemNotification(message: String, sendPush: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        try {
            val action = "post_notification"
            val url = URL("$API_URL?action=$action")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            applySecurityHeaders(connection, action)

            // Ghép thêm biến send_push để gửi lên Server PHP
            val postData = "message=${URLEncoder.encode(message, "UTF-8")}&send_push=$sendPush"
            connection.outputStream.write(postData.toByteArray(Charsets.UTF_8))

            return@withContext connection.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}