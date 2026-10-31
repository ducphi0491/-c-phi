package com.docflowoffline

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.io.StringReader
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.PDFTextStripperByArea
import java.io.OutputStream
import kotlin.math.min
import kotlin.math.roundToInt

// ================ OfficeOoxml.kt ================
/**
 * Conservative OOXML reader + basic editor. This is NOT LibreOffice/Collabora.
 * The original ZIP's non-edited entries are copied byte-for-byte after inflation.
 * Saving is restricted to plain DOCX paragraphs and selected XLSX cells.
 * This class has no Android dependencies so its document logic can be unit-tested on a JVM.
 */
object OfficeOoxml {
    private const val W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
    private const val S = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val A = "http://schemas.openxmlformats.org/drawingml/2006/main"
    private const val P = "http://schemas.openxmlformats.org/presentationml/2006/main"
    private const val R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val PKG_R = "http://schemas.openxmlformats.org/package/2006/relationships"
    private const val CT = "http://schemas.openxmlformats.org/package/2006/content-types"
    private const val XML = "http://www.w3.org/XML/1998/namespace"

    // Android's javax.xml.XMLConstants stub does not expose the Java 7+
    // ACCESS_EXTERNAL_* constants on every API/toolchain. Use the standard
    // JAXP property URIs directly and keep runCatching around setAttribute()
    // because some Android XML implementations do not support these properties.
    private const val ACCESS_EXTERNAL_DTD_PROPERTY =
        "http://javax.xml.XMLConstants/property/accessExternalDTD"
    private const val ACCESS_EXTERNAL_SCHEMA_PROPERTY =
        "http://javax.xml.XMLConstants/property/accessExternalSchema"
    private const val ACCESS_EXTERNAL_STYLESHEET_PROPERTY =
        "http://javax.xml.XMLConstants/property/accessExternalStylesheet"

    private const val MAX_ENTRY = 24 * 1024 * 1024
    private const val MAX_TOTAL = 96 * 1024 * 1024
    private const val MAX_ENTRIES = 2500

    data class Word(val paragraphs: List<String>, val editable: Boolean, val reason: String?)

    /** A compact rich-text model used by the offline Word editor. Offsets are UTF-16 string offsets. */
    data class WordSpan(
        val start: Int, val end: Int,
        val bold: Boolean = false, val italic: Boolean = false, val underline: Boolean = false,
        val fontRgb: String? = null, val highlightRgb: String? = null,
        val fontSizeSp: Int? = null
    )
    data class WordParagraphFormat(
        val start: Int, val end: Int, val alignment: String = "left"
    )
    data class RichWord(
        val text: String,
        val spans: List<WordSpan>,
        val paragraphs: List<WordParagraphFormat>,
        val editable: Boolean,
        val reason: String?
    )
    // cells contain editable source values (formulas retain their leading '=');
    // displayCells hold cached formula results for a more natural spreadsheet grid.
    data class Sheet(
        val name: String,
        val partPath: String,
        val cells: Map<String, String>,
        val displayCells: Map<String, String> = emptyMap(),
        val maxRow: Int = 1,
        val maxCol: Int = 1,
        val truncated: Boolean = false,
        val appearances: Map<String, CellAppearance> = emptyMap(),
        val merges: List<MergeRange> = emptyList()
    )
    data class Workbook(val sheets: List<Sheet>)
    data class SlideParagraph(
        val text: String,
        val editable: Boolean,
        val reason: String? = null
    )
    data class Slide(val number: Int, val partPath: String, val paragraphs: List<SlideParagraph>)
    data class SlideChange(val partPath: String, val index: Int, val text: String)
    data class CellChange(val sheetPath: String, val address: String, val value: String, val cachedValue: String? = null)

    data class MergeRange(val startRow: Int, val startCol: Int, val endRow: Int, val endCol: Int) {
        init {
            require(startRow >= 1 && startCol >= 1 && endRow >= startRow && endCol >= startCol)
        }
        val ref: String get() = "${cellAddress(startRow, startCol)}:${cellAddress(endRow, endCol)}"
        fun contains(row: Int, col: Int): Boolean = row in startRow..endRow && col in startCol..endCol
        fun contains(address: String): Boolean = addressParts(address)?.let { contains(it.first, it.second) } == true
        fun overlaps(other: MergeRange): Boolean =
            startRow <= other.endRow && endRow >= other.startRow &&
                startCol <= other.endCol && endCol >= other.startCol
    }
    data class MergeChange(val sheetPath: String, val range: MergeRange, val merged: Boolean)

    /** Visual properties supported by the lightweight editor; original OOXML styling is otherwise preserved. */
    data class CellAppearance(
        val bold: Boolean = false, val italic: Boolean = false, val underline: Boolean = false,
        val alignment: String = "general", val fillRgb: String? = null,
        val fontRgb: String? = null, val percent: Boolean = false
    )
    /** Null = keep the original property; an empty RGB string clears the explicit color/fill. */
    data class StyleChange(
        val sheetPath: String, val address: String,
        val bold: Boolean? = null, val italic: Boolean? = null, val underline: Boolean? = null,
        val alignment: String? = null, val fillRgb: String? = null,
        val fontRgb: String? = null, val numFmtId: Int? = null
    )

    fun readWordRich(data: ByteArray): RichWord {
        val parts = unzip(data)
        val document = parse(parts["word/document.xml"] ?: error("Không tìm thấy nội dung Word."))
        val body = document.getElementsByTagNameNS(W, "body").item(0) as? Element
            ?: error("Cấu trúc Word không hợp lệ.")
        val paragraphNodes = body.getElementsByTagNameNS(W, "p")
        val text = StringBuilder()
        val spans = mutableListOf<WordSpan>()
        val formats = mutableListOf<WordParagraphFormat>()

        for (paragraphIndex in 0 until paragraphNodes.length) {
            if (paragraphIndex > 0) text.append('\n')
            val paragraph = paragraphNodes.item(paragraphIndex) as Element
            val paragraphStart = text.length
            val pPr = paragraph.childElements(W, "pPr").firstOrNull()
            val alignment = pPr?.getElementsByTagNameNS(W, "jc")?.item(0)
                ?.let { it as? Element }
                ?.getAttributeNS(W, "val")
                ?.lowercase(Locale.ROOT)
                ?.let {
                    when (it) {
                        "center" -> "center"
                        "right", "end" -> "right"
                        "both", "distribute", "thaiDistribute", "justify" -> "justify"
                        else -> "left"
                    }
                } ?: "left"

            val runs = paragraph.getElementsByTagNameNS(W, "r")
            for (runIndex in 0 until runs.length) {
                val run = runs.item(runIndex) as Element
                val runStart = text.length
                for (childIndex in 0 until run.childNodes.length) {
                    val child = run.childNodes.item(childIndex) as? Element ?: continue
                    if (child.namespaceURI != W) continue
                    when (child.localName) {
                        "t", "delText", "instrText" -> text.append(child.textContent.orEmpty())
                        "tab" -> text.append('\t')
                        "br", "cr" -> text.append('\n')
                    }
                }
                val runEnd = text.length
                if (runEnd <= runStart) continue
                val rPr = run.childElements(W, "rPr").firstOrNull()
                val bold = wordOnOff(rPr, "b") || wordOnOff(rPr, "bCs")
                val italic = wordOnOff(rPr, "i") || wordOnOff(rPr, "iCs")
                val underlineNode = rPr?.getElementsByTagNameNS(W, "u")?.item(0) as? Element
                val underline = underlineNode != null && underlineNode.getAttributeNS(W, "val")
                    .lowercase(Locale.ROOT).let { it.isBlank() || it !in setOf("none", "false", "0", "off") }
                val fontRgb = (rPr?.getElementsByTagNameNS(W, "color")?.item(0) as? Element)
                    ?.getAttributeNS(W, "val")?.uppercase(Locale.ROOT)
                    ?.takeIf { it.matches(Regex("[0-9A-F]{6}")) }
                val highlightName = (rPr?.getElementsByTagNameNS(W, "highlight")?.item(0) as? Element)
                    ?.getAttributeNS(W, "val")
                val shadingRgb = (rPr?.getElementsByTagNameNS(W, "shd")?.item(0) as? Element)
                    ?.getAttributeNS(W, "fill")?.uppercase(Locale.ROOT)
                    ?.takeIf { it.matches(Regex("[0-9A-F]{6}")) }
                val highlightRgb = shadingRgb ?: highlightName?.let(::wordHighlightRgb)
                val halfPoints = (rPr?.getElementsByTagNameNS(W, "sz")?.item(0) as? Element)
                    ?.getAttributeNS(W, "val")?.toIntOrNull()
                val fontSizeSp = halfPoints?.let { (it / 2f).roundToInt().coerceIn(6, 96) }
                if (bold || italic || underline || fontRgb != null || highlightRgb != null || fontSizeSp != null) {
                    spans += WordSpan(runStart, runEnd, bold, italic, underline, fontRgb, highlightRgb, fontSizeSp)
                }
            }
            val paragraphEnd = text.length
            formats += WordParagraphFormat(paragraphStart, paragraphEnd, alignment)
        }

        val unsupported = listOf(
            "tbl", "drawing", "pict", "object", "sdt", "hyperlink", "fldChar", "fldSimple", "instrText",
            "ins", "del", "moveFrom", "moveTo", "moveFromRangeStart", "moveFromRangeEnd",
            "moveToRangeStart", "moveToRangeEnd", "customXml", "smartTag", "subDoc", "altChunk",
            // A soft/page break cannot be represented by the current paragraph-only editor without
            // silently changing it into a new paragraph. Keep such documents read-only.
            "br", "cr", "lastRenderedPageBreak",
            // Structured references/ranges would be invalidated when text offsets change.
            "bookmarkStart", "bookmarkEnd", "commentRangeStart", "commentRangeEnd", "commentReference",
            "footnoteReference", "endnoteReference", "oMath", "oMathPara",
            // Native numbering changes paragraph semantics; special text nodes are not represented
            // by the current plain-text model. Run properties are checked separately inside rPr.
            "numPr", "sym", "noBreakHyphen", "softHyphen", "ptab"
        )
        val complexPart = unsupported.firstOrNull { document.getElementsByTagNameNS(W, it).length > 0 }
        val unsafeProperty = if (complexPart == null) unsupportedWordRunProperty(document) else null
        val paragraphSectionBreak = (0 until paragraphNodes.length).any { index ->
            val paragraph = paragraphNodes.item(index) as? Element
            paragraph?.childElements(W, "pPr")?.firstOrNull()
                ?.childElements(W, "sectPr")?.isNotEmpty() == true
        }
        val editable = complexPart == null && unsafeProperty == null && !paragraphSectionBreak &&
            paragraphNodes.length <= 500 && text.length <= 1_000_000
        val reason = when {
            complexPart != null -> "Tệp có thành phần $complexPart. Bạn vẫn có thể bôi chọn và sao chép, nhưng chỉnh sửa đầy đủ cần bộ máy Office chuyên dụng."
            unsafeProperty != null -> "Tệp có định dạng chữ $unsafeProperty chưa thể lưu nguyên vẹn. Bạn vẫn có thể bôi chọn và sao chép ở chế độ chỉ đọc."
            paragraphSectionBreak -> "Tệp có ngắt section nằm trong đoạn; giữ chỉ đọc để không làm thay đổi phân trang hoặc header/footer."
            paragraphNodes.length > 500 -> "Tệp có quá nhiều đoạn cho bộ chỉnh sửa Word offline."
            text.length > 1_000_000 -> "Nội dung Word vượt quá giới hạn chỉnh sửa an toàn trên điện thoại."
            else -> null
        }
        return RichWord(text.toString(), spans, formats, editable, reason)
    }

    /** Detect supported tags whose detailed value would still be flattened by the lightweight model. */
    private fun unsupportedWordRunProperty(document: Document): String? {
        val rPrNodes = document.getElementsByTagNameNS(W, "rPr")
        for (index in 0 until rPrNodes.length) {
            val rPr = rPrNodes.item(index) as? Element ?: continue
            for (childIndex in 0 until rPr.childNodes.length) {
                val child = rPr.childNodes.item(childIndex) as? Element ?: continue
                if (child.namespaceURI != W) continue
                val value = child.getAttributeNS(W, "val").lowercase(Locale.ROOT)
                when (child.localName) {
                    "b", "i" -> if (value in setOf("false", "0", "off", "none")) return "${child.localName}=off"
                    "bCs", "iCs" -> return child.localName
                    "u" -> if (value.isNotBlank() && value !in setOf("single", "true", "1", "on")) return "u=$value"
                    "color" -> if (child.hasAttributeNS(W, "themeColor") ||
                        child.hasAttributeNS(W, "themeTint") || child.hasAttributeNS(W, "themeShade")) return "color-theme"
                    "shd" -> {
                        val pattern = value
                        if (pattern.isNotBlank() && pattern !in setOf("clear", "solid")) return "shd=$pattern"
                    }
                    "highlight", "sz", "szCs" -> Unit
                    else -> return child.localName ?: child.tagName
                }
            }
            val size = (rPr.getElementsByTagNameNS(W, "sz").item(0) as? Element)
                ?.getAttributeNS(W, "val")?.toIntOrNull()
            val complexSize = (rPr.getElementsByTagNameNS(W, "szCs").item(0) as? Element)
                ?.getAttributeNS(W, "val")?.toIntOrNull()
            if (size != null && complexSize != null && size != complexSize) return "szCs"
        }
        return null
    }

    fun writeWordRich(original: ByteArray, edited: RichWord): ByteArray {
        require(edited.text.length <= 1_000_000) { "Nội dung Word vượt quá giới hạn 1.000.000 ký tự." }
        val previous = readWordRich(original)
        require(previous.editable) { previous.reason ?: "Tài liệu này chỉ hỗ trợ xem và sao chép." }
        val zip = unzip(original).toMutableMap()
        val xml = parse(zip.getValue("word/document.xml"))
        val body = xml.getElementsByTagNameNS(W, "body").item(0) as Element
        val directParagraphs = (0 until body.childNodes.length).mapNotNull { body.childNodes.item(it) as? Element }
            .filter { it.namespaceURI == W && it.localName == "p" }
        val section = (0 until body.childNodes.length).mapNotNull { body.childNodes.item(it) as? Element }
            .firstOrNull { it.namespaceURI == W && it.localName == "sectPr" }

        val paragraphRanges = wordParagraphRanges(edited.text)
        val originalTexts = wordParagraphRanges(previous.text).map { range ->
            previous.text.substring(range.first, range.second)
        }
        val editedTexts = paragraphRanges.map { range -> edited.text.substring(range.first, range.second) }
        val sourceByEditedParagraph = alignWordParagraphs(originalTexts, editedTexts)
        val sourceProperties = directParagraphs.map { paragraph ->
            paragraph.childElements(W, "pPr").firstOrNull()?.cloneNode(true) as? Element
        }

        // Rebuild only direct body paragraphs, but map each edited paragraph back to its original
        // paragraph before cloning pPr. This prevents inserting one line from shifting Heading,
        // spacing, indentation or section-related paragraph properties onto every later paragraph.
        directParagraphs.forEach(body::removeChild)
        paragraphRanges.forEachIndexed { index, range ->
            val paragraph = xml.createElementNS(W, "w:p")
            val mappedSource = sourceByEditedParagraph.getOrNull(index)
            var pPr = mappedSource?.let { sourceProperties.getOrNull(it)?.cloneNode(true) as? Element }
            if (pPr != null) paragraph.appendChild(pPr)

            val format = edited.paragraphs.lastOrNull { it.start <= range.first && it.end >= range.first }
            val alignment = format?.alignment?.lowercase(Locale.ROOT).orEmpty()
            pPr?.childElements(W, "jc")?.forEach(pPr::removeChild)
            if (alignment in setOf("center", "right", "justify")) {
                val properties = pPr ?: xml.createElementNS(W, "w:pPr").also {
                    paragraph.appendChild(it)
                    pPr = it
                }
                val jc = xml.createElementNS(W, "w:jc")
                jc.setAttributeNS(W, "w:val", if (alignment == "justify") "both" else alignment)
                properties.appendChild(jc)
            }

            val paragraphStart = range.first
            val paragraphEnd = range.second
            if (paragraphStart == paragraphEnd) {
                paragraph.appendChild(createWordRun(xml, "", null))
            } else {
                val boundaries = sortedSetOf(paragraphStart, paragraphEnd)
                edited.spans.forEach { span ->
                    if (span.end > paragraphStart && span.start < paragraphEnd) {
                        boundaries += span.start.coerceIn(paragraphStart, paragraphEnd)
                        boundaries += span.end.coerceIn(paragraphStart, paragraphEnd)
                    }
                }
                val points = boundaries.toList()
                for (pointIndex in 0 until points.lastIndex) {
                    val a = points[pointIndex]
                    val b = points[pointIndex + 1]
                    if (b <= a) continue
                    val active = edited.spans.filter { it.start < b && it.end > a }
                    val merged = WordSpan(
                        a, b,
                        bold = active.any { it.bold },
                        italic = active.any { it.italic },
                        underline = active.any { it.underline },
                        fontRgb = active.lastOrNull { it.fontRgb != null }?.fontRgb,
                        highlightRgb = active.lastOrNull { it.highlightRgb != null }?.highlightRgb,
                        fontSizeSp = active.lastOrNull { it.fontSizeSp != null }?.fontSizeSp
                    )
                    paragraph.appendChild(createWordRun(xml, edited.text.substring(a, b), merged))
                }
            }
            if (section != null) body.insertBefore(paragraph, section) else body.appendChild(paragraph)
        }
        zip["word/document.xml"] = serialize(xml)
        return zipEntries(zip)
    }

    /**
     * Minimum-edit alignment between original and edited paragraph lists.
     * Exact matches anchor the sequence; replacements keep their source paragraph properties;
     * inserted paragraphs return null and therefore use Word's default paragraph style.
     */
    private fun alignWordParagraphs(original: List<String>, edited: List<String>): List<Int?> {
        val rows = original.size
        val columns = edited.size
        val costs = Array(rows + 1) { IntArray(columns + 1) }
        for (i in 1..rows) costs[i][0] = i * 2
        for (j in 1..columns) costs[0][j] = j * 2
        for (i in 1..rows) {
            for (j in 1..columns) {
                val diagonal = costs[i - 1][j - 1] + if (original[i - 1] == edited[j - 1]) 0 else 3
                val deleted = costs[i - 1][j] + 2
                val inserted = costs[i][j - 1] + 2
                costs[i][j] = minOf(diagonal, deleted, inserted)
            }
        }
        val mapping = MutableList<Int?>(columns) { null }
        var i = rows
        var j = columns
        while (i > 0 || j > 0) {
            val exactDiagonal = i > 0 && j > 0 && original[i - 1] == edited[j - 1] &&
                costs[i][j] == costs[i - 1][j - 1]
            when {
                exactDiagonal -> {
                    mapping[j - 1] = i - 1
                    i--; j--
                }
                j > 0 && costs[i][j] == costs[i][j - 1] + 2 -> j-- // inserted paragraph
                i > 0 && costs[i][j] == costs[i - 1][j] + 2 -> i-- // deleted paragraph
                i > 0 && j > 0 -> {
                    mapping[j - 1] = i - 1 // modified/replaced paragraph
                    i--; j--
                }
                j > 0 -> j--
                else -> i--
            }
        }
        return mapping
    }

    private fun wordOnOff(parent: Element?, localName: String): Boolean {
        val node = parent?.getElementsByTagNameNS(W, localName)?.item(0) as? Element ?: return false
        val value = node.getAttributeNS(W, "val").lowercase(Locale.ROOT)
        return value.isBlank() || value !in setOf("false", "0", "off", "none")
    }

    private fun wordHighlightRgb(name: String): String? = when (name.lowercase(Locale.ROOT)) {
        "yellow" -> "FFF59D"; "green" -> "A5D6A7"; "cyan" -> "80DEEA"; "magenta" -> "F48FB1"
        "blue" -> "90CAF9"; "red" -> "EF9A9A"; "darkblue" -> "1565C0"; "darkcyan" -> "00838F"
        "darkgreen" -> "2E7D32"; "darkmagenta" -> "AD1457"; "darkred" -> "C62828"
        "darkyellow" -> "F9A825"; "darkgray" -> "616161"; "lightgray" -> "E0E0E0"
        "black" -> "000000"; "white" -> "FFFFFF"; else -> null
    }

    private fun wordParagraphRanges(text: String): List<Pair<Int, Int>> {
        val ranges = mutableListOf<Pair<Int, Int>>()
        var start = 0
        for (index in text.indices) {
            if (text[index] == '\n') {
                ranges += start to index
                start = index + 1
            }
        }
        ranges += start to text.length
        return ranges
    }

    private fun createWordRun(doc: Document, value: String, span: WordSpan?): Element {
        val run = doc.createElementNS(W, "w:r")
        if (span != null && (span.bold || span.italic || span.underline || span.fontRgb != null ||
                    span.highlightRgb != null || span.fontSizeSp != null)) {
            val rPr = doc.createElementNS(W, "w:rPr")
            if (span.bold) rPr.appendChild(doc.createElementNS(W, "w:b"))
            if (span.italic) rPr.appendChild(doc.createElementNS(W, "w:i"))
            if (span.underline) {
                val u = doc.createElementNS(W, "w:u")
                u.setAttributeNS(W, "w:val", "single")
                rPr.appendChild(u)
            }
            span.fontRgb?.replace("#", "")?.uppercase(Locale.ROOT)
                ?.takeIf { it.matches(Regex("[0-9A-F]{6}")) }?.let { rgb ->
                    val color = doc.createElementNS(W, "w:color")
                    color.setAttributeNS(W, "w:val", rgb)
                    rPr.appendChild(color)
                }
            span.highlightRgb?.replace("#", "")?.uppercase(Locale.ROOT)
                ?.takeIf { it.matches(Regex("[0-9A-F]{6}")) }?.let { rgb ->
                    val shd = doc.createElementNS(W, "w:shd")
                    shd.setAttributeNS(W, "w:val", "clear")
                    shd.setAttributeNS(W, "w:color", "auto")
                    shd.setAttributeNS(W, "w:fill", rgb)
                    rPr.appendChild(shd)
                }
            span.fontSizeSp?.coerceIn(6, 96)?.let { size ->
                val halfPoints = (size * 2).toString()
                val sz = doc.createElementNS(W, "w:sz").also { it.setAttributeNS(W, "w:val", halfPoints) }
                val szCs = doc.createElementNS(W, "w:szCs").also { it.setAttributeNS(W, "w:val", halfPoints) }
                rPr.appendChild(sz); rPr.appendChild(szCs)
            }
            run.appendChild(rPr)
        }
        if (value.isEmpty()) {
            val t = doc.createElementNS(W, "w:t")
            t.setAttributeNS(XML, "xml:space", "preserve")
            t.textContent = ""
            run.appendChild(t)
            return run
        }
        val pieces = value.split('\t')
        pieces.forEachIndexed { index, piece ->
            if (piece.isNotEmpty() || pieces.size == 1) {
                val t = doc.createElementNS(W, "w:t")
                t.setAttributeNS(XML, "xml:space", "preserve")
                t.textContent = piece
                run.appendChild(t)
            }
            if (index < pieces.lastIndex) run.appendChild(doc.createElementNS(W, "w:tab"))
        }
        return run
    }

    fun readWord(data: ByteArray): Word {
        val parts = unzip(data)
        val document = parse(parts["word/document.xml"] ?: error("Không tìm thấy nội dung Word."))
        val body = document.getElementsByTagNameNS(W, "body").item(0) as? Element
            ?: error("Cấu trúc Word không hợp lệ.")
        val paragraphs = body.getElementsByTagNameNS(W, "p")
        val texts = (0 until paragraphs.length).map { index ->
            val node = paragraphs.item(index) as Element
            node.getElementsByTagNameNS(W, "t").asSequence().joinToString("") { it.textContent ?: "" }
        }
        val unsupported = listOf("tbl", "drawing", "pict", "object", "sdt", "hyperlink", "fldChar", "instrText", "ins", "del", "br", "tab", "altChunk")
        val complexPart = unsupported.firstOrNull { document.getElementsByTagNameNS(W, it).length > 0 }
        val editable = complexPart == null && paragraphs.length <= 400
        val reason = when {
            complexPart != null -> "Có thành phần $complexPart. Chỉ cho xem trước để tránh làm hỏng bố cục; cần bộ máy Collabora để chỉnh sửa đầy đủ."
            !editable -> "Tệp có quá nhiều đoạn cho bộ chỉnh sửa đơn giản."
            else -> null
        }
        return Word(texts, editable, reason)
    }

    fun writeWord(original: ByteArray, newParagraphs: List<String>): ByteArray {
        require(newParagraphs.size <= 400) { "Tối đa 400 đoạn ở chế độ chỉnh sửa cơ bản." }
        val previous = readWord(original)
        require(previous.editable) { previous.reason ?: "Tài liệu này chỉ hỗ trợ xem trước." }
        val zip = unzip(original).toMutableMap()
        val xml = parse(zip.getValue("word/document.xml"))
        val body = xml.getElementsByTagNameNS(W, "body").item(0) as Element
        val paragraphs = body.getElementsByTagNameNS(W, "p").asSequence().map { it as Element }.toList()
        for (index in paragraphs.indices) {
            if (index >= newParagraphs.size) {
                body.removeChild(paragraphs[index])
            } else {
                writeParagraph(xml, paragraphs[index], newParagraphs[index])
            }
        }
        for (index in paragraphs.size until newParagraphs.size) {
            val paragraph = xml.createElementNS(W, "w:p")
            writeParagraph(xml, paragraph, newParagraphs[index])
            val sectPr = body.getElementsByTagNameNS(W, "sectPr").item(0)
            if (sectPr != null && sectPr.parentNode == body) body.insertBefore(paragraph, sectPr)
            else body.appendChild(paragraph)
        }
        zip["word/document.xml"] = serialize(xml)
        return zipEntries(zip)
    }

    private fun writeParagraph(doc: Document, paragraph: Element, value: String) {
        val textNodes = paragraph.getElementsByTagNameNS(W, "t")
        if (textNodes.length == 0) {
            val run = doc.createElementNS(W, "w:r")
            val text = doc.createElementNS(W, "w:t")
            text.setAttributeNS(XML, "xml:space", "preserve")
            text.textContent = value
            run.appendChild(text)
            paragraph.appendChild(run)
        } else {
            for (i in 0 until textNodes.length) {
                val text = textNodes.item(i) as Element
                text.textContent = if (i == 0) value else ""
                if (i == 0) text.setAttributeNS(XML, "xml:space", "preserve")
            }
        }
    }

    fun readWorkbook(data: ByteArray): Workbook {
        val zip = unzip(data)
        val wb = parse(zip["xl/workbook.xml"] ?: error("Không phải file XLSX hợp lệ."))
        val rels = zip["xl/_rels/workbook.xml.rels"]?.let(::parse)
        val relation = mutableMapOf<String, String>()
        rels?.getElementsByTagNameNS(PKG_R, "Relationship")?.asSequence()?.forEach {
            val e = it as Element
            val target = e.getAttribute("Target")
            if (!target.startsWith("http://") && !target.startsWith("https://")) {
                relation[e.getAttribute("Id")] = normalizePartPath("xl", target)
            }
        }
        val strings = zip["xl/sharedStrings.xml"]?.let { sheetStrings(parse(it)) } ?: emptyList()
        val styleTable = zip["xl/styles.xml"]?.let(::parse)?.let(::readStyleTable).orEmpty()
        val sheets = wb.getElementsByTagNameNS(S, "sheet").asSequence().mapIndexedNotNull { i, node ->
            val element = node as Element
            val path = relation[element.getAttributeNS(R, "id")] ?: "xl/worksheets/sheet${i + 1}.xml"
            val sheetBytes = zip[path] ?: return@mapIndexedNotNull null
            val sheetDocument = parse(sheetBytes)
            val loaded = loadCells(sheetDocument, strings, styleTable)
            Sheet(
                element.getAttribute("name").ifBlank { "Sheet ${i + 1}" }, path,
                loaded.cells, loaded.displayCells, loaded.maxRow, loaded.maxCol, loaded.truncated,
                loaded.appearances, readMergeRanges(sheetDocument)
            )
        }.toList()
        require(sheets.isNotEmpty()) { "Không tìm thấy worksheet để hiển thị." }
        return Workbook(sheets)
    }


    private fun readMergeRanges(document: Document): List<MergeRange> =
        document.getElementsByTagNameNS(S, "mergeCell").asSequence().mapNotNull { node ->
            val ref = (node as? Element)?.getAttribute("ref").orEmpty()
            parseMergeRange(ref)
        }.distinctBy { it.ref }.take(2000).toList()

    fun addressParts(address: String): Pair<Int, Int>? {
        val normalized = address.trim().uppercase(Locale.ROOT).replace("$", "")
        if (!normalized.matches(Regex("[A-Z]{1,3}[1-9][0-9]{0,5}"))) return null
        val row = normalized.dropWhile(Char::isLetter).toIntOrNull() ?: return null
        val col = colNumber(normalized.takeWhile(Char::isLetter))
        return if (col > 0) row to col else null
    }

    fun normalizedMergeRange(first: String, second: String): MergeRange? {
        val a = addressParts(first) ?: return null
        val b = addressParts(second) ?: return null
        return MergeRange(minOf(a.first, b.first), minOf(a.second, b.second),
            maxOf(a.first, b.first), maxOf(a.second, b.second))
    }

    fun parseMergeRange(reference: String): MergeRange? {
        val sections = reference.trim().split(':')
        if (sections.isEmpty() || sections.size > 2) return null
        return normalizedMergeRange(sections.first(), sections.getOrElse(1) { sections.first() })
    }

    private fun sheetStrings(document: Document): List<String> {
        return document.getElementsByTagNameNS(S, "si").asSequence().map { node ->
            (node as Element).getElementsByTagNameNS(S, "t").asSequence().joinToString("") { it.textContent ?: "" }
        }.toList()
    }

    // Keep bounded RAM use on mobile, and tell the UI when a workbook is only partially loaded.
    private const val PREVIEW_MAX_ROWS = 100_000
    private const val PREVIEW_MAX_COLUMNS = 256
    private const val PREVIEW_MAX_NONEMPTY_CELLS = 120_000

    private data class LoadedCells(
        val cells: Map<String, String>, val displayCells: Map<String, String>,
        val maxRow: Int, val maxCol: Int, val truncated: Boolean,
        val appearances: Map<String, CellAppearance>
    )

    private fun loadCells(document: Document, strings: List<String>, styleTable: List<CellAppearance>): LoadedCells {
        val cells = LinkedHashMap<String, String>()
        val display = HashMap<String, String>()
        val appearances = HashMap<String, CellAppearance>()
        val nodes = document.getElementsByTagNameNS(S, "c")
        var maxRow = 1
        var maxCol = 1
        var truncated = false
        for (i in 0 until nodes.length) {
            val cell = nodes.item(i) as Element
            val address = cell.getAttribute("r").uppercase(Locale.ROOT)
            val row = address.dropWhile(Char::isLetter).toIntOrNull() ?: continue
            val col = colNumber(address.takeWhile(Char::isLetter))
            if (row !in 1..PREVIEW_MAX_ROWS || col !in 1..PREVIEW_MAX_COLUMNS) {
                truncated = true
                continue
            }
            maxRow = maxOf(maxRow, row)
            maxCol = maxOf(maxCol, col)
            val appearance = styleTable.getOrNull(cell.getAttribute("s").toIntOrNull() ?: 0)
            if (appearance != null && appearance != CellAppearance() && appearances.size < PREVIEW_MAX_NONEMPTY_CELLS) {
                appearances[address] = appearance
            }
            val value = cellValue(cell, strings)
            if (value.isEmpty()) continue
            if (cells.size >= PREVIEW_MAX_NONEMPTY_CELLS) {
                truncated = true
                continue
            }
            cells[address] = value
            if (value.startsWith("=")) {
                val cached = cell.getElementsByTagNameNS(S, "v").item(0)?.textContent.orEmpty()
                if (cached.isNotBlank()) display[address] = cached
            }
        }
        return LoadedCells(cells, display, maxRow, maxCol, truncated, appearances)
    }

    private fun cellValue(cell: Element, strings: List<String>): String {
        val formula = cell.getElementsByTagNameNS(S, "f").item(0)?.textContent
        if (!formula.isNullOrBlank()) return "=$formula"
        val rawValue = cell.getElementsByTagNameNS(S, "v").item(0)?.textContent.orEmpty()
        return when (cell.getAttribute("t")) {
            "s" -> rawValue.toIntOrNull()?.let(strings::getOrNull) ?: rawValue
            "inlineStr" -> cell.getElementsByTagNameNS(S, "t").asSequence().joinToString("") { it.textContent.orEmpty() }
            "b" -> if (rawValue == "1") "TRUE" else "FALSE"
            else -> rawValue
        }
    }


    /** Resolve fonts/fills for cells which have a direct style index. Styles are never flattened on save. */
    private fun readStyleTable(styles: Document): List<CellAppearance> {
        val root = styles.documentElement
        val fonts = (root.getElementsByTagNameNS(S, "fonts").item(0) as? Element)
            ?.childElements(S, "font").orEmpty()
        val fills = (root.getElementsByTagNameNS(S, "fills").item(0) as? Element)
            ?.childElements(S, "fill").orEmpty()
        val xfs = (root.getElementsByTagNameNS(S, "cellXfs").item(0) as? Element)
            ?.childElements(S, "xf").orEmpty()
        return xfs.map { xf ->
            val font = fonts.getOrNull(xf.getAttribute("fontId").toIntOrNull() ?: 0)
            val fill = fills.getOrNull(xf.getAttribute("fillId").toIntOrNull() ?: 0)
            val alignment = (xf.getElementsByTagNameNS(S, "alignment").item(0) as? Element)
                ?.getAttribute("horizontal").orEmpty().ifBlank { "general" }
            val rgb = (fill?.getElementsByTagNameNS(S, "fgColor")?.item(0) as? Element)
                ?.getAttribute("rgb")?.takeIf { it.length == 8 || it.length == 6 }?.takeLast(6)
            val fontRgb = (font?.getElementsByTagNameNS(S, "color")?.item(0) as? Element)
                ?.getAttribute("rgb")?.takeIf { it.length == 8 || it.length == 6 }?.takeLast(6)
            val numFmt = xf.getAttribute("numFmtId").toIntOrNull() ?: 0
            CellAppearance(
                bold = (font?.getElementsByTagNameNS(S, "b")?.length ?: 0) > 0,
                italic = (font?.getElementsByTagNameNS(S, "i")?.length ?: 0) > 0,
                underline = (font?.getElementsByTagNameNS(S, "u")?.length ?: 0) > 0,
                alignment = alignment, fillRgb = rgb, fontRgb = fontRgb,
                percent = numFmt == 9 || numFmt == 10
            )
        }
    }

    fun writeWorkbook(
        original: ByteArray,
        edits: List<CellChange>,
        styles: List<StyleChange> = emptyList(),
        merges: List<MergeChange> = emptyList()
    ): ByteArray {
        require(edits.size + styles.size + merges.size <= 5000) {
            "Tối đa 5000 thao tác ô/gộp cho mỗi lần lưu ở phiên bản thử nghiệm."
        }
        if (edits.isEmpty() && styles.isEmpty() && merges.isEmpty()) return original.copyOf()
        val validPaths = readWorkbook(original).sheets.map { it.partPath }.toSet()
        val zip = unzip(original).toMutableMap()
        val grouped = edits.groupBy { it.sheetPath }
        val styled = styles.groupBy { it.sheetPath }
        val merged = merges.groupBy { it.sheetPath }
        val stylesheet = if (styles.isNotEmpty()) {
            val bytes = zip["xl/styles.xml"] ?: error("Tệp XLSX không có styles.xml để ghi định dạng.")
            parse(bytes)
        } else null
        (grouped.keys + styled.keys + merged.keys).forEach { path ->
            require(path in validPaths) { "Worksheet không thuộc tài liệu: $path" }
            val doc = parse(zip.getValue(path))
            grouped[path].orEmpty().forEach { change ->
                updateCell(doc, change.address, change.value, change.cachedValue)
            }
            styled[path].orEmpty().forEach { change ->
                updateCellStyle(doc, stylesheet ?: error("Thiếu style."), change)
            }
            if (merged[path].orEmpty().isNotEmpty()) applyMergeChanges(doc, merged.getValue(path))
            zip[path] = serialize(doc)
        }
        if (stylesheet != null) zip["xl/styles.xml"] = serialize(stylesheet)
        // A different calculation result might be required when opening with Excel/LibreOffice.
        zip["xl/workbook.xml"]?.let { old ->
            val doc = parse(old)
            val root = doc.documentElement
            val calcPr = doc.getElementsByTagNameNS(S, "calcPr").item(0) as? Element
                ?: doc.createElementNS(S, "calcPr").also(root::appendChild)
            calcPr.setAttribute("fullCalcOnLoad", "1")
            calcPr.setAttribute("forceFullCalc", "1")
            zip["xl/workbook.xml"] = serialize(doc)
        }
        // When formulas are edited, the old calcChain is no longer trustworthy.
        if (edits.any { it.value.trim().startsWith("=") } && zip.remove("xl/calcChain.xml") != null) {
            zip["xl/_rels/workbook.xml.rels"]?.let { old ->
                val rels = parse(old)
                removeWhere(rels.documentElement, "Relationship", PKG_R) {
                    (it as Element).getAttribute("Type").endsWith("/calcChain")
                }
                zip["xl/_rels/workbook.xml.rels"] = serialize(rels)
            }
            zip["[Content_Types].xml"]?.let { old ->
                val ct = parse(old)
                removeWhere(ct.documentElement, "Override", CT) {
                    (it as Element).getAttribute("PartName") == "/xl/calcChain.xml"
                }
                zip["[Content_Types].xml"] = serialize(ct)
            }
        }
        return zipEntries(zip)
    }

    private fun removeWhere(root: Element, name: String, namespace: String, condition: (Node) -> Boolean) {
        val nodes = root.getElementsByTagNameNS(namespace, name).asSequence().toList()
        nodes.filter(condition).forEach { it.parentNode.removeChild(it) }
    }

    /** XLSX stores function arguments separated by commas regardless of phone locale.
     * Preserve semicolons inside quoted text, sheet names and Excel array literals.
     */
    private fun ooxmlFormula(formula: String): String {
        if (';' !in formula || '{' in formula || '}' in formula) return formula
        val out = StringBuilder(formula.length)
        var doubleQuoted = false
        var singleQuoted = false
        var i = 0
        while (i < formula.length) {
            val ch = formula[i]
            if (ch == '"' && !singleQuoted) {
                if (doubleQuoted && i + 1 < formula.length && formula[i + 1] == '"') {
                    out.append("\"\""); i += 2; continue
                }
                doubleQuoted = !doubleQuoted
            } else if (ch == '\'' && !doubleQuoted) {
                if (singleQuoted && i + 1 < formula.length && formula[i + 1] == '\'') {
                    out.append("''"); i += 2; continue
                }
                singleQuoted = !singleQuoted
            }
            out.append(if (ch == ';' && !doubleQuoted && !singleQuoted) ',' else ch)
            i++
        }
        return out.toString()
    }

    private fun updateCell(document: Document, address: String, text: String, cachedValue: String? = null) {
        val normalized = address.trim().uppercase(Locale.ROOT)
        require(normalized.matches(Regex("[A-Z]{1,3}[1-9][0-9]{0,5}"))) { "Địa chỉ ô không hợp lệ: $address" }
        val cell = locateCell(document, normalized)
        // Keep attributes such as style 's'; remove only old value/formula contents.
        val children = (0 until cell.childNodes.length).map { cell.childNodes.item(it) }
        children.filter { it.localName in setOf("f", "v", "is") && it.namespaceURI == S }
            .forEach(cell::removeChild)
        val cleaned = text.trim()
        when {
            cleaned.startsWith("=") && cleaned.length > 1 -> {
                cell.removeAttribute("t")
                cell.appendChild(document.createElementNS(S, "f").also { it.textContent = ooxmlFormula(cleaned.substring(1)) })
                if (cachedValue != null) {
                    cell.appendChild(document.createElementNS(S, "v").also { it.textContent = cachedValue })
                }
            }
            cleaned.matches(Regex("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[Ee][+-]?[0-9]+)?")) -> {
                cell.removeAttribute("t")
                cell.appendChild(document.createElementNS(S, "v").also { it.textContent = cleaned })
            }
            else -> {
                cell.setAttribute("t", "inlineStr")
                val inline = document.createElementNS(S, "is")
                val t = document.createElementNS(S, "t")
                t.setAttributeNS(XML, "xml:space", "preserve")
                t.textContent = text
                inline.appendChild(t)
                cell.appendChild(inline)
            }
        }
    }


    /** Creates only the target cell; all unrelated rows, formulas and styles remain unchanged. */
    private fun locateCell(document: Document, normalized: String): Element {
        require(normalized.matches(Regex("[A-Z]{1,3}[1-9][0-9]{0,5}"))) { "Địa chỉ ô không hợp lệ: $normalized" }
        val rowNo = normalized.dropWhile(Char::isLetter).toInt()
        val column = colNumber(normalized.takeWhile(Char::isLetter))
        require(rowNo in 1..PREVIEW_MAX_ROWS && column in 1..PREVIEW_MAX_COLUMNS) {
            "Chế độ cơ bản chỉ chỉnh sửa A1:IV100000."
        }
        val root = document.documentElement
        val sheetData = root.getElementsByTagNameNS(S, "sheetData").item(0) as? Element
            ?: document.createElementNS(S, "sheetData").also(root::appendChild)
        val row = sheetData.childElements(S, "row").firstOrNull { it.getAttribute("r").toIntOrNull() == rowNo }
            ?: document.createElementNS(S, "row").also { newRow ->
                newRow.setAttribute("r", rowNo.toString())
                val next = sheetData.childElements(S, "row").firstOrNull {
                    (it.getAttribute("r").toIntOrNull() ?: Int.MAX_VALUE) > rowNo
                }
                sheetData.insertBefore(newRow, next)
            }
        return row.childElements(S, "c").firstOrNull { it.getAttribute("r").equals(normalized, true) }
            ?: document.createElementNS(S, "c").also { newCell ->
                newCell.setAttribute("r", normalized)
                val next = row.childElements(S, "c").firstOrNull {
                    colNumber(it.getAttribute("r").takeWhile(Char::isLetter)) > column
                }
                row.insertBefore(newCell, next)
            }
    }

    /** Clone the original xf/font so other formatting (borders, size, formats) remains intact. */
    private fun updateCellStyle(sheet: Document, styles: Document, change: StyleChange) {
        val cell = locateCell(sheet, change.address.uppercase(Locale.ROOT))
        val root = styles.documentElement
        val fonts = root.getElementsByTagNameNS(S, "fonts").item(0) as? Element
            ?: error("Không tìm thấy danh sách font trong styles.xml")
        val fills = root.getElementsByTagNameNS(S, "fills").item(0) as? Element
            ?: error("Không tìm thấy danh sách fill trong styles.xml")
        val xfs = root.getElementsByTagNameNS(S, "cellXfs").item(0) as? Element
            ?: error("Không tìm thấy danh sách cellXfs trong styles.xml")
        val xfArray = xfs.childElements(S, "xf")
        val originalXf = xfArray.getOrNull(cell.getAttribute("s").toIntOrNull() ?: 0)
            ?: xfArray.firstOrNull() ?: error("styles.xml không có cellXfs")
        val newXf = originalXf.cloneNode(true) as Element
        if (change.bold != null || change.italic != null || change.underline != null || change.fontRgb != null) {
            val allFonts = fonts.childElements(S, "font")
            val originalFont = allFonts.getOrNull(originalXf.getAttribute("fontId").toIntOrNull() ?: 0)
                ?: allFonts.firstOrNull() ?: error("styles.xml không có font")
            val font = originalFont.cloneNode(true) as Element
            fun toggle(name: String, value: Boolean?) {
                if (value == null) return
                font.childElements(S, name).forEach(font::removeChild)
                if (value) font.appendChild(styles.createElementNS(S, name))
            }
            toggle("b", change.bold)
            toggle("i", change.italic)
            toggle("u", change.underline)
            if (change.fontRgb != null) {
                font.childElements(S, "color").forEach(font::removeChild)
                if (change.fontRgb.isNotBlank()) {
                    val rgb = change.fontRgb.uppercase(Locale.ROOT)
                    require(rgb.matches(Regex("[0-9A-F]{6}"))) { "Màu chữ RGB không hợp lệ." }
                    font.appendChild(styles.createElementNS(S, "color").also { it.setAttribute("rgb", "FF$rgb") })
                }
            }
            val fontIndex = appendOrReuseStyleElement(fonts, "font", font)
            newXf.setAttribute("fontId", fontIndex.toString())
            newXf.setAttribute("applyFont", "1")
        }
        if (change.fillRgb != null) {
            if (change.fillRgb.isBlank()) {
                newXf.setAttribute("fillId", "0")
            } else {
                val rgb = change.fillRgb.uppercase(Locale.ROOT)
                require(rgb.matches(Regex("[0-9A-F]{6}"))) { "Màu RGB không hợp lệ." }
                val newFill = styles.createElementNS(S, "fill")
                val pattern = styles.createElementNS(S, "patternFill")
                pattern.setAttribute("patternType", "solid")
                pattern.appendChild(styles.createElementNS(S, "fgColor").also { it.setAttribute("rgb", "FF$rgb") })
                pattern.appendChild(styles.createElementNS(S, "bgColor").also { it.setAttribute("indexed", "64") })
                newFill.appendChild(pattern)
                val fillIndex = appendOrReuseStyleElement(fills, "fill", newFill)
                newXf.setAttribute("fillId", fillIndex.toString())
            }
            newXf.setAttribute("applyFill", "1")
        }
        if (change.alignment != null) {
            val align = (newXf.childElements(S, "alignment").firstOrNull()
                ?: styles.createElementNS(S, "alignment").also(newXf::appendChild))
            align.setAttribute("horizontal", change.alignment)
            newXf.setAttribute("applyAlignment", "1")
        }
        if (change.numFmtId != null) {
            newXf.setAttribute("numFmtId", change.numFmtId.toString())
            newXf.setAttribute("applyNumberFormat", "1")
        }
        val xfIndex = appendOrReuseStyleElement(xfs, "xf", newXf)
        cell.setAttribute("s", xfIndex.toString())
    }

    /** Reuse an identical font/fill/xf instead of creating thousands of duplicate styles. */
    private fun appendOrReuseStyleElement(container: Element, localName: String, candidate: Element): Int {
        val existing = container.childElements(S, localName)
        val signature = styleElementSignature(candidate)
        val found = existing.indexOfFirst { styleElementSignature(it) == signature }
        if (found >= 0) return found
        val index = existing.size
        container.appendChild(candidate)
        container.setAttribute("count", (index + 1).toString())
        return index
    }

    private fun styleElementSignature(element: Element): String = buildString {
        append('{').append(element.namespaceURI.orEmpty()).append('}').append(element.localName ?: element.tagName)
        val attributes = (0 until element.attributes.length).map { element.attributes.item(it) }
            .sortedWith(compareBy<Node> { it.namespaceURI.orEmpty() }.thenBy { it.localName ?: it.nodeName })
        attributes.forEach { attribute ->
            append('|').append(attribute.namespaceURI.orEmpty()).append(':')
                .append(attribute.localName ?: attribute.nodeName).append('=').append(attribute.nodeValue.orEmpty())
        }
        (0 until element.childNodes.length).mapNotNull { element.childNodes.item(it) as? Element }
            .forEach { child -> append('[').append(styleElementSignature(child)).append(']') }
        val directText = (0 until element.childNodes.length).map { element.childNodes.item(it) }
            .filter { it.nodeType == Node.TEXT_NODE || it.nodeType == Node.CDATA_SECTION_NODE }
            .joinToString("") { it.nodeValue.orEmpty() }.trim()
        if (directText.isNotEmpty()) append("#").append(directText)
    }

    private fun applyMergeChanges(document: Document, changes: List<MergeChange>) {
        val root = document.documentElement
        val existing = LinkedHashMap<String, MergeRange>()
        document.getElementsByTagNameNS(S, "mergeCell").asSequence().forEach { node ->
            val range = parseMergeRange((node as? Element)?.getAttribute("ref").orEmpty())
            if (range != null) existing[range.ref] = range
        }
        changes.forEach { change ->
            if (change.merged) existing[change.range.ref] = change.range else existing.remove(change.range.ref)
        }
        val ranges = existing.values.sortedWith(compareBy<MergeRange> { it.startRow }.thenBy { it.startCol })
        for (i in ranges.indices) for (j in i + 1 until ranges.size) {
            require(!ranges[i].overlaps(ranges[j])) {
                "Hai vùng gộp bị chồng nhau: ${ranges[i].ref} và ${ranges[j].ref}"
            }
        }
        val old = document.getElementsByTagNameNS(S, "mergeCells").item(0) as? Element
        if (ranges.isEmpty()) {
            if (old != null) old.parentNode?.removeChild(old)
            return
        }
        val container = old ?: document.createElementNS(S, "mergeCells").also { mergeCells ->
            val laterNames = setOf(
                "phoneticPr", "conditionalFormatting", "dataValidations", "hyperlinks",
                "printOptions", "pageMargins", "pageSetup", "headerFooter", "rowBreaks",
                "colBreaks", "customProperties", "cellWatches", "ignoredErrors", "smartTags",
                "drawing", "legacyDrawing", "legacyDrawingHF", "picture", "oleObjects",
                "controls", "webPublishItems", "tableParts", "extLst"
            )
            val before = (0 until root.childNodes.length).mapNotNull { root.childNodes.item(it) as? Element }
                .firstOrNull { it.localName in laterNames }
            root.insertBefore(mergeCells, before)
        }
        while (container.hasChildNodes()) container.removeChild(container.firstChild)
        container.setAttribute("count", ranges.size.toString())
        ranges.forEach { range ->
            container.appendChild(document.createElementNS(S, "mergeCell").also { it.setAttribute("ref", range.ref) })
        }
    }

    fun readPresentation(original: ByteArray): List<Slide> {
        val zip = unzip(original)
        val paths = presentationSlidePaths(zip)
        return paths.mapIndexed { index, path ->
            val doc = parse(zip.getValue(path))
            val paragraphs = doc.getElementsByTagNameNS(A, "p").asSequence().map { item ->
                val element = item as Element
                val textNodes = element.getElementsByTagNameNS(A, "t")
                val text = textNodes.asSequence().joinToString("") { it.textContent.orEmpty() }
                val simple = textNodes.length == 1 &&
                    element.getElementsByTagNameNS(A, "br").length == 0 &&
                    element.getElementsByTagNameNS(A, "fld").length == 0
                SlideParagraph(
                    text = text,
                    editable = simple,
                    reason = if (simple) null else "Đoạn có nhiều kiểu chữ, trường hoặc xuống dòng; giữ chỉ đọc để không làm mất định dạng."
                )
            }.toList()
            Slide(index + 1, path, paragraphs)
        }
    }

    /** Resolve the actual presentation order from p:sldIdLst and presentation relationships. */
    private fun presentationSlidePaths(zip: Map<String, ByteArray>): List<String> {
        val fallback = zip.keys.filter { it.matches(Regex("ppt/slides/slide[0-9]+\\.xml")) }
            .sortedBy { it.substringAfterLast("/slide").substringBefore(".xml").toInt() }
        val presentationBytes = zip["ppt/presentation.xml"] ?: return fallback
        val relationshipBytes = zip["ppt/_rels/presentation.xml.rels"] ?: return fallback
        return runCatching {
            val relationships = parse(relationshipBytes)
            val targets = relationships.getElementsByTagNameNS(PKG_R, "Relationship").asSequence()
                .mapNotNull { it as? Element }
                .filter { it.getAttribute("TargetMode").isBlank() || !it.getAttribute("TargetMode").equals("External", true) }
                .filter { it.getAttribute("Type").endsWith("/slide") }
                .associate { it.getAttribute("Id") to it.getAttribute("Target") }
            val presentation = parse(presentationBytes)
            val ordered = presentation.getElementsByTagNameNS(P, "sldId").asSequence()
                .mapNotNull { it as? Element }
                .mapNotNull { slideId ->
                    val relId = slideId.getAttributeNS(R, "id").ifBlank { slideId.getAttribute("r:id") }
                    targets[relId]?.let { resolvePackageTarget("ppt/presentation.xml", it) }
                }
                .filter { it in zip }
                .distinct()
                .toList()
            if (ordered.isEmpty()) fallback else ordered + fallback.filterNot(ordered::contains)
        }.getOrDefault(fallback)
    }

    private fun resolvePackageTarget(basePart: String, rawTarget: String): String {
        val target = runCatching { java.net.URI(rawTarget).path }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: rawTarget
        val combined = if (target.startsWith('/')) target.removePrefix("/")
        else basePart.substringBeforeLast('/', "") + "/" + target
        val stack = java.util.ArrayDeque<String>()
        combined.split('/').forEach { segment ->
            when (segment) {
                "", "." -> Unit
                ".." -> if (stack.isNotEmpty()) stack.removeLast() else error("Quan hệ PPTX đi ra ngoài gói tệp.")
                else -> stack.addLast(segment)
            }
        }
        return stack.joinToString("/")
    }

    /** Edits existing slide text in-place; it cannot create shapes, alter geometry or add slides. */
    fun writePresentation(original: ByteArray, edits: List<SlideChange>): ByteArray {
        if (edits.isEmpty()) return original.copyOf()
        val parts = unzip(original).toMutableMap()
        val valid = readPresentation(original).map { it.partPath }.toSet()
        edits.groupBy { it.partPath }.forEach { (path, changes) ->
            require(path in valid) { "Slide không nằm trong tệp PPTX." }
            val xml = parse(parts.getValue(path))
            val paragraphs = xml.getElementsByTagNameNS(A, "p").asSequence().map { it as Element }.toList()
            changes.forEach { change ->
                require(change.index in paragraphs.indices) { "Không tìm thấy đoạn chữ trên slide." }
                val paragraph = paragraphs[change.index]
                val tNodes = paragraph.getElementsByTagNameNS(A, "t")
                require(tNodes.length == 1 && paragraph.getElementsByTagNameNS(A, "br").length == 0 &&
                    paragraph.getElementsByTagNameNS(A, "fld").length == 0) {
                    "Đoạn PowerPoint có nhiều kiểu chữ hoặc trường đặc biệt; không ghi để tránh mất định dạng."
                }
                tNodes.item(0).textContent = change.text
            }
            parts[path] = serialize(xml)
        }
        return zipEntries(parts)
    }

    /** A strict document size guard prevents huge or malicious ZIP inflation. */
    private fun unzip(source: ByteArray): LinkedHashMap<String, ByteArray> {
        require(source.size <= MAX_TOTAL) { "Tệp vượt quá 96 MB." }
        val parts = linkedMapOf<String, ByteArray>()
        var totalSize = 0L
        ZipInputStream(ByteArrayInputStream(source)).use { stream ->
            while (true) {
                val entry = stream.nextEntry ?: break
                if (!entry.isDirectory) {
                    require(totalEntries(parts) <= MAX_ENTRIES) { "Tệp chứa quá nhiều thành phần." }
                    val bytes = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val read = stream.read(buffer)
                        if (read < 0) break
                        bytes.write(buffer, 0, read)
                        totalSize += read
                        require(bytes.size() <= MAX_ENTRY && totalSize <= MAX_TOTAL) {
                            "Tệp Office quá lớn hoặc có dấu hiệu ZIP bomb."
                        }
                    }
                    val name = entry.name
                    require(!name.startsWith('/') && !name.contains("..")) { "Đường dẫn không an toàn trong tệp Office." }
                    parts[name] = bytes.toByteArray()
                }
                stream.closeEntry()
            }
        }
        return LinkedHashMap(parts)
    }

    private fun totalEntries(entries: Map<String, ByteArray>): Int = entries.size + 1

    private fun zipEntries(entries: Map<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun parse(bytes: ByteArray): Document {
        val f = DocumentBuilderFactory.newInstance()
        f.isNamespaceAware = true
        runCatching { f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        runCatching { f.setFeature("http://xml.org/sax/features/external-general-entities", false) }
        runCatching { f.setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        runCatching { f.setAttribute(ACCESS_EXTERNAL_DTD_PROPERTY, "") }
        runCatching { f.setAttribute(ACCESS_EXTERNAL_SCHEMA_PROPERTY, "") }
        f.isExpandEntityReferences = false
        val builder = f.newDocumentBuilder()
        builder.setEntityResolver { _, _ -> org.xml.sax.InputSource(StringReader("")) }
        return builder.parse(ByteArrayInputStream(bytes))
    }

    private fun serialize(document: Document): ByteArray {
        val factory = TransformerFactory.newInstance()
        try { factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) } catch (_: Exception) { }
        runCatching { factory.setAttribute(ACCESS_EXTERNAL_DTD_PROPERTY, "") }
        runCatching { factory.setAttribute(ACCESS_EXTERNAL_STYLESHEET_PROPERTY, "") }
        val transformer = factory.newTransformer()
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        transformer.setOutputProperty(OutputKeys.INDENT, "no")
        val out = ByteArrayOutputStream()
        transformer.transform(DOMSource(document), StreamResult(out))
        return out.toByteArray()
    }

    private fun normalizePartPath(base: String, target: String): String {
        val sections = mutableListOf<String>()
        val raw = if (target.startsWith('/')) target.removePrefix("/") else "$base/$target"
        raw.replace('\\', '/').split('/').forEach {
            when (it) {
                "", "." -> Unit
                ".." -> if (sections.isNotEmpty()) sections.removeAt(sections.lastIndex) else error("Đường dẫn ngoài gói OOXML")
                else -> sections += it
            }
        }
        return sections.joinToString("/")
    }

    private fun org.w3c.dom.NodeList.asSequence(): Sequence<Node> = sequence {
        for (i in 0 until length) yield(item(i))
    }
    private fun Element.childElements(ns: String, local: String): List<Element> =
        (0 until childNodes.length).mapNotNull { childNodes.item(it) as? Element }
            .filter { it.namespaceURI == ns && it.localName == local }

    private fun colNumber(letters: String): Int {
        if (letters.isEmpty()) return -1
        var number = 0
        for (char in letters.uppercase(Locale.ROOT)) {
            if (char !in 'A'..'Z') return -1
            number = number * 26 + (char - 'A') + 1
        }
        return number
    }

    fun cellAddress(row: Int, column: Int): String {
        require(row >= 1 && column in 1..16384)
        var c = column
        val label = StringBuilder()
        while (c > 0) {
            c--
            label.append(('A'.code + c % 26).toChar())
            c /= 26
        }
        return label.reverse().toString() + row
    }
}


// ================ PdfTools.kt ================
object PdfTools {
    private fun ready(context: Context) { PDFBoxResourceLoader.init(context.applicationContext) }

    fun merge(context: Context, inputFiles: List<Uri>, output: OutputStream) {
        require(inputFiles.size >= 2) { "Chọn tối thiểu hai tệp PDF để ghép." }
        ready(context)
        PDDocument().use { target ->
            val merger = PDFMergerUtility()
            inputFiles.forEach { uri ->
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    PDDocument.load(stream).use { document -> merger.appendDocument(target, document) }
                } ?: throw IOException("Không mở được PDF: $uri")
            }
            target.save(output)
        }
    }

    /** 1-based page range e.g. "1-3,5,8-9" */
    fun extract(context: Context, source: Uri, pages: String, output: OutputStream) {
        ready(context)
        context.contentResolver.openInputStream(source)?.use { stream ->
            PDDocument.load(stream).use { document ->
                val indices = parsePageRange(pages, document.numberOfPages)
                PDDocument().use { result ->
                    indices.forEach { page -> result.importPage(document.getPage(page - 1)) }
                    result.save(output)
                }
            }
        } ?: throw IOException("Không mở được tệp PDF nguồn.")
    }

    fun parsePageRange(spec: String, total: Int): List<Int> {
        require(total > 0)
        val unique = linkedSetOf<Int>()
        spec.split(',').forEach { part ->
            val token = part.trim()
            require(token.matches(Regex("[1-9][0-9]*(?:\\s*-\\s*[1-9][0-9]*)?"))) { "Khoảng trang không hợp lệ: $token" }
            val numbers = token.split('-').map { it.trim().toInt() }
            val start = numbers.first()
            val end = numbers.last()
            require(start <= end && start in 1..total && end in 1..total) { "Trang phải thuộc khoảng 1..$total" }
            require(end - start <= 2000) { "Khoảng trang quá dài." }
            for (index in start..end) unique.add(index)
        }
        require(unique.isNotEmpty()) { "Chưa chọn trang." }
        return unique.toList()
    }

    fun renderPage(context: Context, uri: Uri, pageIndex: Int, maxWidth: Int = 1280): Pair<Bitmap, Int> {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: throw IOException("Không mở được PDF.")
        descriptor.use { fd ->
            PdfRenderer(fd).use { renderer ->
                require(pageIndex in 0 until renderer.pageCount) { "Trang không hợp lệ." }
                renderer.openPage(pageIndex).use { page ->
                    val scale = min(maxWidth.toFloat() / page.width, 3f).coerceAtLeast(0.2f)
                    val width = (page.width * scale).toInt().coerceAtLeast(1)
                    val height = (page.height * scale).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    return bitmap to renderer.pageCount
                }
            }
        }
    }

    fun pagePng(context: Context, uri: Uri, pageIndex: Int, output: OutputStream) {
        val (bitmap, _) = renderPage(context, uri, pageIndex, 1900)
        try {
            require(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Không thể ghi PNG." }
        } finally { bitmap.recycle() }
    }

    data class Area(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        fun normalized(): Area {
            val l = minOf(left, right).coerceIn(0f, 1f)
            val r = maxOf(left, right).coerceIn(0f, 1f)
            val t = minOf(top, bottom).coerceIn(0f, 1f)
            val b = maxOf(top, bottom).coerceIn(0f, 1f)
            return Area(l, t, r, b)
        }
        val width: Float get() = (right - left).coerceAtLeast(0f)
        val height: Float get() = (bottom - top).coerceAtLeast(0f)
    }
    enum class Markup { HIGHLIGHT, UNDERLINE, STRIKEOUT }
    data class SearchHit(val pageIndex: Int, val excerpt: String)

    fun extractTextInArea(context: Context, uri: Uri, pageIndex: Int, area: Area): String {
        ready(context)
        val normalized = area.normalized()
        require(normalized.width > 0.002f && normalized.height > 0.002f) { "Vùng chọn quá nhỏ." }
        context.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input).use { document ->
                require(pageIndex in 0 until document.numberOfPages) { "Trang PDF không hợp lệ." }
                val page = document.getPage(pageIndex)
                val box = page.cropBox
                val rotation = ((page.rotation % 360) + 360) % 360
                val visualWidth = if (rotation == 90 || rotation == 270) box.height else box.width
                val visualHeight = if (rotation == 90 || rotation == 270) box.width else box.height
                val region = RectF(
                    normalized.left * visualWidth,
                    normalized.top * visualHeight,
                    normalized.right * visualWidth,
                    normalized.bottom * visualHeight
                )
                val stripper = PDFTextStripperByArea().apply {
                    sortByPosition = true
                    addRegion("selection", region)
                    extractRegions(page)
                }
                return stripper.getTextForRegion("selection").trim()
            }
        }
        throw IOException("Không mở được PDF để trích xuất văn bản.")
    }

    fun searchText(context: Context, uri: Uri, query: String, maxHits: Int = 50): List<SearchHit> {
        val key = query.trim()
        require(key.isNotEmpty()) { "Hãy nhập nội dung cần tìm." }
        ready(context)
        context.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input).use { document ->
                val hits = mutableListOf<SearchHit>()
                val pageLimit = minOf(document.numberOfPages, 500)
                for (pageIndex in 0 until pageLimit) {
                    val stripper = PDFTextStripper().apply {
                        sortByPosition = true
                        startPage = pageIndex + 1
                        endPage = pageIndex + 1
                    }
                    val pageText = stripper.getText(document).replace(Regex("\\s+"), " ").trim()
                    var from = 0
                    while (from < pageText.length && hits.size < maxHits) {
                        val found = pageText.indexOf(key, from, ignoreCase = true)
                        if (found < 0) break
                        val excerptStart = (found - 45).coerceAtLeast(0)
                        val excerptEnd = (found + key.length + 65).coerceAtMost(pageText.length)
                        hits += SearchHit(pageIndex, pageText.substring(excerptStart, excerptEnd))
                        from = found + key.length.coerceAtLeast(1)
                    }
                    if (hits.size >= maxHits) break
                }
                return hits
            }
        }
        throw IOException("Không mở được PDF để tìm kiếm.")
    }

    fun addMarkup(
        context: Context, uri: Uri, pageIndex: Int, area: Area,
        markup: Markup, selectedText: String?, output: OutputStream
    ) {
        ready(context)
        val normalized = area.normalized()
        require(normalized.width > 0.002f && normalized.height > 0.002f) { "Vùng đánh dấu quá nhỏ." }
        context.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input).use { document ->
                require(pageIndex in 0 until document.numberOfPages) { "Trang PDF không hợp lệ." }
                val page = document.getPage(pageIndex)
                val box = page.cropBox
                val rotation = ((page.rotation % 360) + 360) % 360
                val rect = normalizedAreaToPdfRectangle(normalized, box, rotation)
                val subtype = when (markup) {
                    Markup.HIGHLIGHT -> PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT
                    Markup.UNDERLINE -> PDAnnotationTextMarkup.SUB_TYPE_UNDERLINE
                    Markup.STRIKEOUT -> PDAnnotationTextMarkup.SUB_TYPE_STRIKEOUT
                }
                val annotation = PDAnnotationTextMarkup(subtype)
                annotation.setRectangle(rect)
                annotation.setQuadPoints(floatArrayOf(
                    rect.lowerLeftX, rect.upperRightY,
                    rect.upperRightX, rect.upperRightY,
                    rect.lowerLeftX, rect.lowerLeftY,
                    rect.upperRightX, rect.lowerLeftY
                ))
                val color = when (markup) {
                    Markup.HIGHLIGHT -> floatArrayOf(1f, 0.90f, 0.10f)
                    Markup.UNDERLINE -> floatArrayOf(0.05f, 0.45f, 0.95f)
                    Markup.STRIKEOUT -> floatArrayOf(0.90f, 0.12f, 0.12f)
                }
                annotation.setColor(PDColor(color, PDDeviceRGB.INSTANCE))
                annotation.setConstantOpacity(if (markup == Markup.HIGHLIGHT) 0.35f else 0.95f)
                annotation.setContents(selectedText?.take(500).orEmpty())
                annotation.setPrinted(true)
                page.annotations.add(annotation)
                runCatching { annotation.constructAppearances(document) }
                document.save(output)
            }
            return
        }
        throw IOException("Không mở được PDF để đánh dấu.")
    }

    fun rotatePage(context: Context, uri: Uri, pageIndex: Int, degrees: Int, output: OutputStream) {
        require(degrees % 90 == 0) { "Góc xoay phải là bội số của 90°." }
        ready(context)
        context.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input).use { document ->
                require(pageIndex in 0 until document.numberOfPages) { "Trang PDF không hợp lệ." }
                val page = document.getPage(pageIndex)
                val next = (((page.rotation + degrees) % 360) + 360) % 360
                page.rotation = next
                document.save(output)
            }
            return
        }
        throw IOException("Không mở được PDF để xoay trang.")
    }

    fun deletePage(context: Context, uri: Uri, pageIndex: Int, output: OutputStream) {
        ready(context)
        context.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input).use { document ->
                require(document.numberOfPages > 1) { "PDF chỉ có một trang nên không thể xóa." }
                require(pageIndex in 0 until document.numberOfPages) { "Trang PDF không hợp lệ." }
                document.removePage(pageIndex)
                document.save(output)
            }
            return
        }
        throw IOException("Không mở được PDF để xóa trang.")
    }

    private fun normalizedAreaToPdfRectangle(area: Area, box: PDRectangle, rotation: Int): PDRectangle {
        fun point(u: Float, v: Float): Pair<Float, Float> {
            val x: Float
            val y: Float
            when (rotation) {
                90 -> {
                    x = box.lowerLeftX + v * box.width
                    y = box.lowerLeftY + u * box.height
                }
                180 -> {
                    x = box.lowerLeftX + (1f - u) * box.width
                    y = box.lowerLeftY + v * box.height
                }
                270 -> {
                    x = box.lowerLeftX + (1f - v) * box.width
                    y = box.lowerLeftY + (1f - u) * box.height
                }
                else -> {
                    x = box.lowerLeftX + u * box.width
                    y = box.lowerLeftY + (1f - v) * box.height
                }
            }
            return x to y
        }
        val points = listOf(
            point(area.left, area.top), point(area.right, area.top),
            point(area.left, area.bottom), point(area.right, area.bottom)
        )
        val minX = points.minOf { it.first }
        val maxX = points.maxOf { it.first }
        val minY = points.minOf { it.second }
        val maxY = points.maxOf { it.second }
        return PDRectangle(minX, minY, (maxX - minX).coerceAtLeast(1f), (maxY - minY).coerceAtLeast(1f))
    }

    fun imagesToPdf(context: Context, uris: List<Uri>, output: OutputStream) {
        require(uris.isNotEmpty()) { "Chưa chọn hình ảnh." }
        val pdf = PdfDocument()
        try {
            val pageWidth = 595
            val pageHeight = 842
            val padding = 28f
            uris.forEachIndexed { index, uri ->
                val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                    val buffer = ByteArrayOutputStream()
                    val chunk = ByteArray(8192)
                    while (true) {
                        val n = stream.read(chunk)
                        if (n < 0) break
                        require(buffer.size() + n <= 24 * 1024 * 1024) { "Ảnh quá lớn." }
                        buffer.write(chunk, 0, n)
                    }
                    buffer.toByteArray()
                } ?: throw IOException("Không đọc được hình ảnh: $uri")
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Định dạng ảnh không hợp lệ." }
                var sample = 1
                while (bounds.outWidth / sample > 2400 || bounds.outHeight / sample > 2400) sample *= 2
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    ?: throw IOException("Không giải mã được hình ảnh.")
                try {
                    val page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create())
                    page.canvas.drawColor(Color.WHITE)
                    val xScale = (pageWidth - padding * 2) / bitmap.width
                    val yScale = (pageHeight - padding * 2) / bitmap.height
                    val ratio = min(xScale, yScale)
                    val width = bitmap.width * ratio
                    val height = bitmap.height * ratio
                    page.canvas.drawBitmap(bitmap, null,
                        android.graphics.RectF((pageWidth - width) / 2f, (pageHeight - height) / 2f,
                            (pageWidth + width) / 2f, (pageHeight + height) / 2f),
                        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                    pdf.finishPage(page)
                } finally { bitmap.recycle() }
            }
            pdf.writeTo(output)
        } finally {
            pdf.close()
        }
    }
}

/** Simple text-to-PDF fallback; it does not preserve Word/Excel page layout. */
object TextPdfExporter {
    fun create(text: String, output: OutputStream, title: String = "Tài liệu") {
        val document = PdfDocument()
        try {
            val width = 595
            val height = 842
            val margin = 42f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 40, 56)
                textSize = 12f
                typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
            }
            val lineHeight = 19f
            var pageNumber = 0
            var y = 0f
            var page: PdfDocument.Page? = null
            fun nextPage() {
                if (page != null) document.finishPage(page!!)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())
                page!!.canvas.drawColor(Color.WHITE)
                y = margin
                paint.textSize = 9f
                paint.color = Color.GRAY
                page!!.canvas.drawText("$title  |  $pageNumber", margin, y, paint)
                paint.textSize = 12f
                paint.color = Color.rgb(30, 40, 56)
                y += 32f
            }
            nextPage()
            for (line in text.replace("\r\n", "\n").split('\n')) {
                val fragments = wrap(line, paint, width - 2 * margin)
                for (fragment in fragments) {
                    if (y > height - margin) nextPage()
                    page!!.canvas.drawText(fragment, margin, y, paint)
                    y += lineHeight
                }
            }
            if (page != null) document.finishPage(page!!)
            document.writeTo(output)
        } finally {
            document.close()
        }
    }

    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isEmpty()) return listOf("")
        val lines = mutableListOf<String>()
        var rest = text.replace('\t', ' ')
        while (rest.isNotEmpty()) {
            val fit = paint.breakText(rest, true, maxWidth, null).coerceAtLeast(1)
            if (fit >= rest.length) {
                lines.add(rest)
                break
            }
            val end = rest.lastIndexOf(' ', fit).takeIf { it > 0 } ?: fit
            lines.add(rest.substring(0, end).trimEnd())
            rest = rest.substring(end).trimStart()
        }
        return lines
    }
}


// ================ CsvEngine.kt ================
/** RFC 4180-style quoted fields; supports commas, escaped quotes and CRLF/newlines. */
object CsvEngine {
    fun parse(input: String, maxRows: Int = 5000): List<List<String>> {
        val result = mutableListOf<List<String>>()
        var record = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            when {
                inQuotes && ch == '"' && i + 1 < input.length && input[i + 1] == '"' -> {
                    field.append('"')
                    i++
                }
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    record.add(field.toString())
                    field.setLength(0)
                }
                (ch == '\r' || ch == '\n') && !inQuotes -> {
                    record.add(field.toString())
                    result.add(record)
                    if (result.size >= maxRows) error("CSV quá lớn cho chế độ chỉnh sửa cơ bản.")
                    record = mutableListOf()
                    field.setLength(0)
                    if (ch == '\r' && i + 1 < input.length && input[i + 1] == '\n') i++
                }
                else -> field.append(ch)
            }
            i++
        }
        if (inQuotes) error("CSV không hợp lệ: thiếu dấu nháy đóng.")
        if (field.isNotEmpty() || record.isNotEmpty()) {
            record.add(field.toString())
            result.add(record)
        }
        return result
    }

    fun write(rows: List<List<String>>): String = rows.joinToString("\r\n") { row ->
        row.joinToString(",") { cell ->
            if (cell.any { it == ',' || it == '\n' || it == '\r' || it == '"' })
                "\"" + cell.replace("\"", "\"\"") + "\""
            else cell
        }
    }
}

// ================ PDF -> Excel (layout-aware offline conversion) ================
/**
 * PDF pages are drawings, not spreadsheets. This converter reconstructs editable
 * XLSX cells from vector table borders + positioned text when they exist.
 * Each page also has an ORIGINAL image reference sheet for visual checking.
 * Scan-only pages become an embedded image, NOT fake editable text: real OCR
 * would require an OCR model which is not included in this build.
 */
object PdfExcelConverter {
    private const val MAX_PAGES = 80
    private const val MAX_INPUT_BYTES = 75L * 1024 * 1024
    private const val MAX_GLYPHS = 90_000
    private const val MAX_CELLS = 95_000

    fun convert(context: Context, uri: Uri, output: OutputStream) {
        PDFBoxResourceLoader.init(context.applicationContext)
        val temporary = java.io.File.createTempFile("docflow_pdf_to_excel_", ".pdf", context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                temporary.outputStream().buffered().use { out ->
                    val buffer = ByteArray(64 * 1024)
                    var copied = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        copied += n
                        if (copied > MAX_INPUT_BYTES) throw IOException("PDF quá lớn để chuyển offline (tối đa 75 MB).")
                        out.write(buffer, 0, n)
                    }
                }
            } ?: throw IOException("Không mở được PDF nguồn.")
            java.io.FileInputStream(temporary).use { source ->
                PDDocument.load(source).use { pdf ->
                    val pages = pdf.numberOfPages
                    require(pages in 1..MAX_PAGES) { "Hỗ trợ PDF từ 1 đến $MAX_PAGES trang trong một lần." }
                    val result = ArrayList<OfflineXlsxWriter.SheetData>(pages * 2)
                    var total = 0
                    for (number in 1..pages) {
                        val page = pdf.getPage(number - 1)
                        val width = page.cropBox.width.coerceAtLeast(50f)
                        val height = page.cropBox.height.coerceAtLeast(50f)
                        val extractor = object : com.tom_roush.pdfbox.text.PDFTextStripper() {
                            val glyphs = ArrayList<PdfPageGrid.Glyph>()
                            init { sortByPosition = true; startPage = number; endPage = number }
                            override fun writeString(text: String, textPositions: MutableList<com.tom_roush.pdfbox.text.TextPosition>) {
                                for (pos in textPositions) {
                                    val value = pos.unicode ?: continue
                                    if (value.isBlank()) continue
                                    if (glyphs.size >= MAX_GLYPHS) throw IOException("Trang $number chứa quá nhiều ký tự.")
                                    glyphs.add(PdfPageGrid.Glyph(
                                        value, pos.xDirAdj, pos.yDirAdj,
                                        pos.widthDirAdj.coerceAtLeast(0.1f), pos.heightDir.coerceAtLeast(1f),
                                        pos.fontSizeInPt.coerceIn(6f, 34f),
                                        pos.font?.name?.contains("Bold", true) == true
                                    ))
                                }
                            }
                        }
                        extractor.getText(pdf)
                        val graphics = if ((page.rotation % 360 + 360) % 360 == 0) {
                            runCatching { PdfVectorGrid(page, height).apply { processPage(page) } }.getOrNull()
                        } else null
                        val hasText = extractor.glyphs.size >= 2
                        val image = renderReference(temporary, number - 1)
                        if (hasText) {
                            val grid = PdfPageGrid.analyze(
                                extractor.glyphs, graphics?.lines.orEmpty(), graphics?.fills.orEmpty(),
                                width, height
                            )
                            total += grid.rows.sumOf { it.size }
                            require(total <= MAX_CELLS) { "Vượt $MAX_CELLS ô. Hãy chia PDF thành tệp nhỏ hơn." }
                            result.add(grid.copy(name = "Trang $number"))
                            result.add(OfflineXlsxWriter.SheetData(
                                name = "Gốc $number",
                                rows = listOf(mapOf(0 to "Ảnh đối chiếu PDF gốc (không sửa chữ trong ảnh).")),
                                referenceJpeg = image, pageWidthPt = width, pageHeightPt = height
                            ))
                        } else {
                            // A scan does NOT magically become editable text without OCR.
                            result.add(OfflineXlsxWriter.SheetData(
                                name = "Trang $number (ảnh)",
                                rows = listOf(mapOf(0 to "PDF scan: chưa có OCR, nội dung là ảnh tham chiếu.")),
                                referenceJpeg = image, pageWidthPt = width, pageHeightPt = height
                            ))
                        }
                    }
                    OfflineXlsxWriter.write(result, output)
                }
            }
        } finally {
            temporary.delete()
        }
    }

    private fun renderReference(file: java.io.File, index: Int): ByteArray {
        val fd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
        android.graphics.pdf.PdfRenderer(fd).use { renderer ->
            renderer.openPage(index).use { page ->
                val scale = minOf(1.8f, 1400f / maxOf(page.width, page.height).toFloat())
                val w = (page.width * scale).toInt().coerceAtLeast(1)
                val h = (page.height * scale).toInt().coerceAtLeast(1)
                val bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
                try {
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    return ByteArrayOutputStream().use { out ->
                        check(bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out))
                        out.toByteArray()
                    }
                } finally { bitmap.recycle() }
            }
        }
    }
}

/** PDF vector graphic callback signatures follow PdfBox-Android, not desktop PDFBox. */
internal class PdfVectorGrid(
    page: com.tom_roush.pdfbox.pdmodel.PDPage, private val pageHeight: Float
) : com.tom_roush.pdfbox.contentstream.PDFGraphicsStreamEngine(page) {
    data class Line(val x1: Float, val y1: Float, val x2: Float, val y2: Float)
    data class Fill(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val rgb: String)
    val lines = mutableListOf<Line>()
    val fills = mutableListOf<Fill>()
    private val path = ArrayList<Line>()
    private var current = android.graphics.PointF()
    private var first = android.graphics.PointF()

    private fun segment(x1: Float, y1: Float, x2: Float, y2: Float) {
        if (path.size >= 6000) return
        if (kotlin.math.abs(x1 - x2) < 0.65f || kotlin.math.abs(y1 - y2) < 0.65f) {
            path.add(Line(x1, pageHeight - y1, x2, pageHeight - y2))
        }
    }
    override fun appendRectangle(p0: android.graphics.PointF, p1: android.graphics.PointF,
                                 p2: android.graphics.PointF, p3: android.graphics.PointF) {
        segment(p0.x,p0.y,p1.x,p1.y);segment(p1.x,p1.y,p2.x,p2.y)
        segment(p2.x,p2.y,p3.x,p3.y);segment(p3.x,p3.y,p0.x,p0.y)
        first = android.graphics.PointF(p0.x,p0.y); current = android.graphics.PointF(p0.x,p0.y)
    }
    override fun moveTo(x: Float, y: Float) { current = android.graphics.PointF(x,y); first = android.graphics.PointF(x,y) }
    override fun lineTo(x: Float, y: Float) {
        segment(current.x,current.y,x,y); current = android.graphics.PointF(x,y)
    }
    override fun curveTo(x1: Float,y1: Float,x2: Float,y2: Float,x3: Float,y3: Float) {
        current = android.graphics.PointF(x3,y3) // Curves are not spreadsheet grid lines.
    }
    override fun getCurrentPoint(): android.graphics.PointF = current
    override fun closePath() { segment(current.x,current.y,first.x,first.y); current = first }
    override fun clip(windingRule: android.graphics.Path.FillType) = Unit
    override fun endPath() { path.clear() }
    override fun strokePath() { if (lines.size < 10000) lines.addAll(path.take(10000-lines.size)); path.clear() }
    override fun fillPath(windingRule: android.graphics.Path.FillType) {
        // A rectangular filled area can be used for Excel background color.
        if (path.size == 4 && fills.size < 1000) {
            val points = path.flatMap { listOf(it.x1 to it.y1, it.x2 to it.y2) }
            val left = points.minOf { it.first }; val right = points.maxOf { it.first }
            val top = points.minOf { it.second }; val bottom = points.maxOf { it.second }
            if (right-left > 6f && bottom-top > 5f) {
                val color = runCatching { getGraphicsState().nonStrokingColor.toRGB() }.getOrNull()
                if (color != null) {
                    val rgb = "%06X".format(color and 0xFFFFFF)
                    if (rgb != "FFFFFF") fills.add(Fill(left,top,right,bottom,rgb))
                }
            }
        }
        path.clear()
    }
    override fun fillAndStrokePath(windingRule: android.graphics.Path.FillType) {
        val saved = path.toList(); fillPath(windingRule)
        if (lines.size < 10000) lines.addAll(saved.take(10000-lines.size))
    }
    override fun drawImage(pdImage: com.tom_roush.pdfbox.pdmodel.graphics.image.PDImage) = Unit
    override fun shadingFill(shadingName: com.tom_roush.pdfbox.cos.COSName) = Unit
}

/** Pure data/geometry core: runnable with JVM unit tests, no Android dependency. */
internal object PdfPageGrid {
    data class Glyph(val text: String, val x: Float, val y: Float, val w: Float, val h: Float,
                     val fontSize: Float = 11f, val bold: Boolean = false)

    private fun quantize(values: List<Float>, tolerance: Float): List<Float> {
        if (values.isEmpty()) return emptyList()
        val sorted = values.filter { it.isFinite() }.sorted()
        if (sorted.isEmpty()) return emptyList()
        val result = ArrayList<Float>()
        var start = sorted.first(); var sum = start; var size = 1
        for (v in sorted.drop(1)) {
            if (v - start <= tolerance) { sum += v; size++ }
            else { result.add(sum / size); start = v; sum = v; size = 1 }
        }
        result.add(sum / size)
        return result
    }
    private fun bracket(v: Float, edges: List<Float>): Int =
        (edges.binarySearch(v).let { if (it >= 0) it.coerceAtMost(edges.lastIndex - 1) else -it - 2 })
            .coerceIn(0, edges.size - 2)

    private data class TextLine(val y: Float, val h: Float, val chars: MutableList<Glyph>)
    private fun textLines(glyphs: List<Glyph>): List<TextLine> {
        val lines = ArrayList<TextLine>()
        for (g in glyphs.sortedWith(compareBy<Glyph> { it.y }.thenBy { it.x })) {
            val line = lines.lastOrNull()
            if (line != null && kotlin.math.abs(g.y-line.y) < maxOf(2.5f, minOf(4f,g.h*.4f))) {
                line.chars.add(g)
            } else lines.add(TextLine(g.y,g.h, mutableListOf(g)))
        }
        return lines
    }
    fun analyze(glyphs: List<Glyph>, sourceLines: List<PdfVectorGrid.Line>, fills: List<PdfVectorGrid.Fill>,
                width: Float, height: Float): OfflineXlsxWriter.SheetData {
        val lines = textLines(glyphs)
        val vertical = sourceLines.filter { kotlin.math.abs(it.x1-it.x2) < 1.1f && kotlin.math.abs(it.y2-it.y1) > 8f }
        val horizontal = sourceLines.filter { kotlin.math.abs(it.y1-it.y2) < 1.1f && kotlin.math.abs(it.x2-it.x1) > 15f }
        val grid = vertical.size >= 3 && horizontal.size >= 3
        if (!grid) {
            // Preserve v0.7 table extraction for unruled PDFs. Avoid making up borders.
            val rows = PdfTableLayout.extract(glyphs.map { PdfTableLayout.Glyph(it.text,it.x,it.y,it.w,it.h) })
            return OfflineXlsxWriter.SheetData("Trang", rows)
        }
        val tableXs = quantize(vertical.map { it.x1 }.filter { it in 0f..width }, 2f)
        val tableYs = quantize(horizontal.map { it.y1 }.filter { it in 0f..height }, 2f)
        if (tableXs.size < 2 || tableYs.size < 2 || tableXs.size > 55 || tableYs.size > 550) {
            val rows = PdfTableLayout.extract(glyphs.map { PdfTableLayout.Glyph(it.text,it.x,it.y,it.w,it.h) })
            return OfflineXlsxWriter.SheetData("Trang", rows)
        }
        val xs = (listOf(0f) + tableXs + listOf(width)).sorted().let { quantize(it,1.5f) }
        val ysBase = (listOf(0f) + tableYs + listOf(height)).sorted().let { quantize(it,1.5f) }
        // Add missing row boundaries for text outside ruled tables, e.g. headings/footers.
        val moreY = ArrayList<Float>()
        for (i in 0 until lines.size-1) {
            val a = lines[i]; val b = lines[i+1]
            if (b.y-a.y > 9f && bracket(a.y-a.h/2,ysBase) == bracket(b.y-b.h/2,ysBase)) {
                val between = (a.y+b.y)/2f
                val section = bracket(between,ysBase)
                val hasRuledRows = tableYs.any { it > ysBase[section]+1.5f && it < ysBase[section+1]-1.5f }
                if (!hasRuledRows) moreY.add(between)
            }
        }
        val ys = quantize((ysBase+moreY).sorted(), 1.4f)
        val rowCount = ys.size-1; val colCount = xs.size-1
        if (rowCount <= 0 || colCount <= 0 || rowCount > 560) throw IOException("Bảng quá phức tạp để dựng thành Excel.")
        if (rowCount * colCount > 12_000) {
            // Avoid quadratic line intersection work on huge drawings; preserve the
            // original appearance in the companion screenshot sheet instead.
            return OfflineXlsxWriter.SheetData("Trang", PdfTableLayout.extract(
                glyphs.map { PdfTableLayout.Glyph(it.text,it.x,it.y,it.w,it.h) }
            ))
        }
        val values = ArrayList<MutableMap<Int,String>>(rowCount)
        repeat(rowCount) { values.add(LinkedHashMap()) }
        val styles = LinkedHashMap<Pair<Int,Int>, OfflineXlsxWriter.CellStyle>()
        val merged = ArrayList<OfflineXlsxWriter.Merge>()
        fun verticalHere(x: Float, top: Float, bottom: Float): Boolean = vertical.any {
            kotlin.math.abs(it.x1-x) < 2.2f &&
                maxOf(it.y1,it.y2) >= bottom-2.2f && minOf(it.y1,it.y2) <= top+2.2f
        }
        fun horizontalHere(y: Float, left: Float, right: Float): Boolean = horizontal.any {
            kotlin.math.abs(it.y1-y) < 2.2f &&
                maxOf(it.x1,it.x2) >= right-2.2f && minOf(it.x1,it.x2) <= left+2.2f
        }
        // Pre-create ruled blank cells so the visible table grid survives export.
        for (r in 0 until rowCount) for (c in 0 until colCount) {
            val l=xs[c]; val rr=xs[c+1]; val t=ys[r]; val b=ys[r+1]
            val borders = OfflineXlsxWriter.Borders(
                left=verticalHere(l,t,b), right=verticalHere(rr,t,b),
                top=horizontalHere(t,l,rr), bottom=horizontalHere(b,l,rr)
            )
            val fill = fills.lastOrNull { it.x1 <= l+1 && it.x2 >= rr-1 && it.y1 <= t+1 && it.y2 >= b-1 }?.rgb
            if (borders != OfflineXlsxWriter.Borders() || fill != null) {
                styles[r to c] = OfflineXlsxWriter.CellStyle(borders=borders, fillRgb=fill)
            }
        }
        for (line in lines) {
            val r = bracket(line.y-line.h/2,ys)
            val row = line.chars.sortedBy { it.x }
            val outsideTable = line.y < tableYs.first() - 2f || line.y > tableYs.last() + 2f
            // Do not cut a report title into multiple cells simply because it crosses
            // an imaginary column boundary outside the actual ruled table.
            val groups = if (outsideTable) {
                sortedMapOf(bracket(row.first().x, xs) to row)
            } else row.groupBy { bracket(it.x+it.w/2,xs) }.toSortedMap()
            for ((c, chars) in groups) {
                val sorted = chars.sortedBy { it.x }
                val b = StringBuilder()
                var lastEnd = 0f
                for (g in sorted) {
                    val space = g.x-lastEnd
                    if (b.isNotEmpty() && space > maxOf(2.6f,g.h*.27f)) b.append(' ')
                    b.append(g.text)
                    lastEnd = maxOf(lastEnd,g.x+g.w)
                }
                val text = b.toString().trim().take(32700)
                if (text.isEmpty()) continue
                val prev = values[r][c]
                values[r][c] = if (prev.isNullOrBlank()) text else "$prev\n$text"
                val center = sorted.first().x+(sorted.last().x+sorted.last().w-sorted.first().x)/2f
                val align = if (kotlin.math.abs(center-(xs[c]+xs[c+1])/2f) < (xs[c+1]-xs[c])*.13f) "center" else "left"
                val current = styles[r to c] ?: OfflineXlsxWriter.CellStyle()
                styles[r to c] = current.copy(
                    bold = sorted.any { it.bold }, fontSize = sorted.map { it.fontSize }.average().toInt().coerceIn(8,22),
                    alignment = align
                )
            }
        }
        // A title or other unruled row can span columns. Only merge if no text in other columns.
        for (r in 0 until rowCount) {
            val occupied = values[r].keys.sorted()
            if (occupied.size != 1 || colCount < 3) continue
            val c=occupied[0]
            val t=ys[r]; val b=ys[r+1]
            var last=c
            while (last < colCount-1 && !verticalHere(xs[last+1],t,b) && !values[r].containsKey(last+1)) last++
            if (last>c && (b-t)<150f) merged.add(OfflineXlsxWriter.Merge(r,c,r,last))
        }
        return OfflineXlsxWriter.SheetData(
            name="Trang", rows=values, colWidthsPt=(0 until colCount).map { xs[it+1]-xs[it] },
            rowHeightsPt=(0 until rowCount).map { ys[it+1]-ys[it] },
            merges=merged, cellStyles=styles
        )
    }
}

/** Writes editable OOXML cells, merged ranges, borders, font/fill and original PDF page snapshots. */
internal object OfflineXlsxWriter {
    data class Borders(val left:Boolean=false, val right:Boolean=false, val top:Boolean=false, val bottom:Boolean=false)
    data class CellStyle(val bold:Boolean=false, val fontSize:Int=11, val fillRgb:String?=null,
                         val alignment:String="left", val borders:Borders=Borders())
    data class Merge(val r1:Int, val c1:Int, val r2:Int, val c2:Int)
    data class SheetData(
        val name:String, val rows:List<Map<Int,String>>,
        val colWidthsPt:List<Float> = emptyList(), val rowHeightsPt:List<Float> = emptyList(),
        val merges:List<Merge> = emptyList(), val cellStyles:Map<Pair<Int,Int>,CellStyle> = emptyMap(),
        val referenceJpeg:ByteArray? = null, val pageWidthPt:Float = 595f, val pageHeightPt:Float = 842f
    )
    private const val M = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val PKG = "http://schemas.openxmlformats.org/package/2006/relationships"
    private data class Font(val bold:Boolean, val size:Int)
    private class Styles {
        val fonts = LinkedHashMap<Font,Int>().apply { put(Font(false,11),0) }
        val fills = LinkedHashMap<String,Int>().apply { put("",0) }
        val borders = LinkedHashMap<Borders,Int>().apply { put(Borders(),0) }
        val variants = LinkedHashMap<CellStyle,Int>().apply { put(CellStyle(),0) }
        fun id(style:CellStyle):Int {
            if (style == CellStyle()) return 0
            if (variants.size >= 512) return 0 // avoid workbook style explosion
            fonts.getOrPut(Font(style.bold,style.fontSize)) { fonts.size }
            if (!style.fillRgb.isNullOrEmpty()) fills.getOrPut(style.fillRgb.uppercase(java.util.Locale.ROOT)) { fills.size+1 }
            borders.getOrPut(style.borders) { borders.size }
            return variants.getOrPut(style) { variants.size }
        }
        fun xml():String = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><styleSheet xmlns=\"$M\">")
            append("<fonts count=\"${fonts.size}\">")
            fonts.forEach { (font,_) ->
                append("<font>"); if(font.bold) append("<b/>")
                append("<sz val=\"${font.size}\"/><name val=\"Calibri\"/></font>")
            }
            append("</fonts><fills count=\"${fills.size+1}\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill>")
            fills.entries.filter { it.key.isNotEmpty() }.forEach { (rgb,_) ->
                append("<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF$rgb\"/><bgColor indexed=\"64\"/></patternFill></fill>")
            }
            append("</fills><borders count=\"${borders.size}\">")
            borders.forEach { (border,_) ->
                append("<border>")
                for ((n,on) in listOf("left" to border.left,"right" to border.right,"top" to border.top,"bottom" to border.bottom)) {
                    if(on) append("<$n style=\"thin\"><color rgb=\"FF7A8592\"/></$n>") else append("<$n/>")
                }
                append("<diagonal/></border>")
            }
            append("</borders><cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>")
            append("<cellXfs count=\"${variants.size}\">")
            variants.forEach { (style,_) ->
                val fid = fonts.getValue(Font(style.bold,style.fontSize))
                val fillId=style.fillRgb?.uppercase(java.util.Locale.ROOT)?.let { fills[it] } ?: 0
                val bid=borders.getValue(style.borders)
                append("<xf numFmtId=\"0\" fontId=\"$fid\" fillId=\"$fillId\" borderId=\"$bid\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"${style.alignment}\" vertical=\"center\" wrapText=\"1\"/></xf>")
            }
            append("</cellXfs></styleSheet>")
        }
    }
    fun write(sheets:List<SheetData>, output:OutputStream) {
        require(sheets.isNotEmpty()) { "Không có trang xuất." }
        val styles=Styles()
        sheets.forEach { it.cellStyles.values.forEach(styles::id) }
        ZipOutputStream(output).use { zip ->
            fun part(name:String, bytes:ByteArray) {
                zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry()
            }
            fun xmlPart(name:String, s:String)=part(name,s.toByteArray(Charsets.UTF_8))
            val images=sheets.mapIndexedNotNull { i, s -> if(s.referenceJpeg!=null) i else null }
            xmlPart("[Content_Types].xml",buildString {
                append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/>")
                if(images.isNotEmpty()) append("<Default Extension=\"jpg\" ContentType=\"image/jpeg\"/>")
                append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>")
                sheets.indices.forEach { append("<Override PartName=\"/xl/worksheets/sheet${it+1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>") }
                images.forEach { append("<Override PartName=\"/xl/drawings/drawing${it+1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.drawing+xml\"/>") }
                append("</Types>")
            })
            xmlPart("_rels/.rels", "<?xml version=\"1.0\"?><Relationships xmlns=\"$PKG\"><Relationship Id=\"rId1\" Type=\"$REL/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>")
            xmlPart("xl/workbook.xml", buildString {
                append("<?xml version=\"1.0\"?><workbook xmlns=\"$M\" xmlns:r=\"$REL\"><sheets>")
                sheets.forEachIndexed { i,s -> append("<sheet name=\"${escape(s.name.take(31))}\" sheetId=\"${i+1}\" r:id=\"rId${i+1}\"/>") }
                append("</sheets></workbook>")
            })
            xmlPart("xl/_rels/workbook.xml.rels",buildString {
                append("<?xml version=\"1.0\"?><Relationships xmlns=\"$PKG\">")
                sheets.indices.forEach { append("<Relationship Id=\"rId${it+1}\" Type=\"$REL/worksheet\" Target=\"worksheets/sheet${it+1}.xml\"/>") }
                append("<Relationship Id=\"rId${sheets.size+1}\" Type=\"$REL/styles\" Target=\"styles.xml\"/></Relationships>")
            })
            xmlPart("xl/styles.xml",styles.xml())
            sheets.forEachIndexed { i,sheet ->
                xmlPart("xl/worksheets/sheet${i+1}.xml",buildSheet(sheet,styles))
                if(sheet.referenceJpeg!=null) {
                    xmlPart("xl/worksheets/_rels/sheet${i+1}.xml.rels", "<?xml version=\"1.0\"?><Relationships xmlns=\"$PKG\"><Relationship Id=\"rId1\" Type=\"$REL/drawing\" Target=\"../drawings/drawing${i+1}.xml\"/></Relationships>")
                    xmlPart("xl/drawings/drawing${i+1}.xml",imageDrawing(sheet))
                    xmlPart("xl/drawings/_rels/drawing${i+1}.xml.rels", "<?xml version=\"1.0\"?><Relationships xmlns=\"$PKG\"><Relationship Id=\"rId1\" Type=\"$REL/image\" Target=\"../media/page${i+1}.jpg\"/></Relationships>")
                    part("xl/media/page${i+1}.jpg",sheet.referenceJpeg)
                }
            }
        }
    }
    private fun buildSheet(sheet:SheetData, styles:Styles):String = buildString {
        val maxRow = maxOf(1,sheet.rows.size,sheet.rowHeightsPt.size)
        val maxCol = maxOf(1,sheet.colWidthsPt.size,(sheet.rows.flatMap { it.keys }.maxOrNull() ?: 0)+1)
        require(maxRow<=10000 && maxCol<=256) { "Kích thước bảng vượt khả năng xử lý offline." }
        append("<?xml version=\"1.0\"?><worksheet xmlns=\"$M\" xmlns:r=\"$REL\">")
        append("<dimension ref=\"A1:${column(maxCol-1)}$maxRow\"/><sheetViews><sheetView workbookViewId=\"0\"/></sheetViews><sheetFormatPr defaultRowHeight=\"15\"/>")
        append("<cols>")
        (0 until maxCol).forEach { c ->
            val w = sheet.colWidthsPt.getOrNull(c)?.let { (it * (96.0/72.0)/7.0).coerceIn(2.0,120.0) }
                ?: 14.0
            append("<col min=\"${c+1}\" max=\"${c+1}\" width=\"${"%.2f".format(java.util.Locale.US,w)}\" customWidth=\"1\"/>")
        }
        append("</cols><sheetData>")
        for (r in 0 until maxRow) {
            val height=sheet.rowHeightsPt.getOrNull(r)?.coerceIn(10f,320f)
            append("<row r=\"${r+1}\"")
            if(height!=null) append(" ht=\"${"%.2f".format(java.util.Locale.US,height)}\" customHeight=\"1\"")
            append(">")
            val row=sheet.rows.getOrNull(r).orEmpty()
            val keys=(row.keys + sheet.cellStyles.keys.filter { it.first==r }.map { it.second }).distinct().sorted()
            for (c in keys) {
                if (c !in 0 until maxCol) continue
                val text=row[c]
                val styleId=styles.id(sheet.cellStyles[r to c]?:CellStyle())
                append("<c r=\"${column(c)}${r+1}\"")
                if (styleId != 0) append(" s=\"$styleId\"")
                if(text==null) { append("/>"); continue }
                require(text.length<=32767) { "Nội dung ô Excel vượt giới hạn 32.767 ký tự." }
                val number=numericValue(text.trim())
                if(number!=null) append("><v>$number</v></c>")
                else append(" t=\"inlineStr\"><is><t xml:space=\"preserve\">${escape(text)}</t></is></c>")
            }
            append("</row>")
        }
        append("</sheetData>")
        if(sheet.merges.isNotEmpty()) {
            append("<mergeCells count=\"${sheet.merges.size}\">")
            sheet.merges.forEach { m -> append("<mergeCell ref=\"${column(m.c1)}${m.r1+1}:${column(m.c2)}${m.r2+1}\"/>") }
            append("</mergeCells>")
        }
        if(sheet.referenceJpeg!=null) append("<drawing r:id=\"rId1\"/>")
        append("<pageMargins left=\"0.25\" right=\"0.25\" top=\"0.25\" bottom=\"0.25\" header=\"0\" footer=\"0\"/></worksheet>")
    }
    private fun imageDrawing(sheet:SheetData):String {
        val cx=(sheet.pageWidthPt*12700f).toLong().coerceAtLeast(12700L)
        val cy=(sheet.pageHeightPt*12700f).toLong().coerceAtLeast(12700L)
        return """<?xml version="1.0"?><xdr:wsDr xmlns:xdr="http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="$REL"><xdr:oneCellAnchor><xdr:from><xdr:col>0</xdr:col><xdr:colOff>0</xdr:colOff><xdr:row>0</xdr:row><xdr:rowOff>0</xdr:rowOff></xdr:from><xdr:ext cx="$cx" cy="$cy"/><xdr:pic><xdr:nvPicPr><xdr:cNvPr id="2" name="Trang PDF goc"/><xdr:cNvPicPr/></xdr:nvPicPr><xdr:blipFill><a:blip r:embed="rId1"/><a:stretch><a:fillRect/></a:stretch></xdr:blipFill><xdr:spPr><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></xdr:spPr></xdr:pic><xdr:clientData/></xdr:oneCellAnchor></xdr:wsDr>"""
    }
    private fun column(col:Int):String {
        var v=col+1; val s=StringBuilder()
        while(v>0) { v--; s.append(('A'.code+v%26).toChar());v/=26 }
        return s.reverse().toString()
    }
    private fun numericValue(s:String):String? {
        if(s.isBlank() || s.length>17 || s.startsWith('+') || (s.length>1 && s[0]=='0' && s[1].isDigit())) return null
        val normalized = when {
            s.matches(Regex("-?(?:0|[1-9][0-9]{0,12})")) -> s
            s.matches(Regex("-?(?:0|[1-9][0-9]{0,12})[.,][0-9]{1,2}")) -> s.replace(',','.')
            s.matches(Regex("-?[1-9][0-9]{0,2}(?:\\.[0-9]{3})+,[0-9]{1,2}")) -> s.replace(".","").replace(',','.')
            else -> return null
        }
        return normalized.takeIf { it.toDoubleOrNull()?.isFinite()==true }
    }
    private fun escape(s:String):String = buildString(s.length+32) {
        for(c in s) when(c) {
            '&'->append("&amp;")
            '<'->append("&lt;")
            '>'->append("&gt;")
            '"'->append("&quot;")
            '\''->append("&apos;")
            else -> if(c=='\t'||c=='\n'||c=='\r'||c.code>=32) append(c)
        }
    }
}


/** Render/export the lightweight spreadsheet model without a server. */
internal object SpreadsheetExporter {
    data class SheetSnapshot(
        val documentName: String,
        val sheetName: String,
        val rows: Int,
        val columns: Int,
        val values: Map<String, String>,
        val appearances: Map<String, OfficeOoxml.CellAppearance>,
        val merges: List<OfficeOoxml.MergeRange> = emptyList()
    )

    private const val PDF_WIDTH = 842
    private const val PDF_HEIGHT = 595
    private const val PDF_MARGIN = 28f
    private const val PDF_INDEX_WIDTH = 38f
    private const val PDF_COLUMN_WIDTH = 112f
    private const val PDF_ROW_HEIGHT = 25f
    private const val PDF_TITLE_HEIGHT = 42f
    private const val PDF_HEADER_HEIGHT = 28f

    fun writePdf(snapshot: SheetSnapshot, output: OutputStream) {
        val rows = snapshot.rows.coerceIn(1, 2000)
        val columns = snapshot.columns.coerceIn(1, 100)
        val columnsPerPage = kotlin.math.floor(
            (PDF_WIDTH - PDF_MARGIN * 2 - PDF_INDEX_WIDTH) / PDF_COLUMN_WIDTH
        ).toInt().coerceAtLeast(1)
        val rowsPerPage = kotlin.math.floor(
            (PDF_HEIGHT - PDF_MARGIN * 2 - PDF_TITLE_HEIGHT - PDF_HEADER_HEIGHT) / PDF_ROW_HEIGHT
        ).toInt().coerceAtLeast(1)
        val horizontalPages = kotlin.math.ceil(columns / columnsPerPage.toDouble()).toInt()
        val verticalPages = kotlin.math.ceil(rows / rowsPerPage.toDouble()).toInt()
        require(horizontalPages * verticalPages <= 160) {
            "Bảng quá lớn để xuất PDF trên điện thoại. Hãy lọc dữ liệu hoặc tách sheet."
        }
        val document = PdfDocument()
        try {
            var pageNumber = 0
            for (rowBlock in 0 until verticalPages) {
                for (columnBlock in 0 until horizontalPages) {
                    pageNumber++
                    val page = document.startPage(
                        PdfDocument.PageInfo.Builder(PDF_WIDTH, PDF_HEIGHT, pageNumber).create()
                    )
                    drawPdfPage(snapshot, page.canvas, rowBlock, columnBlock, rowsPerPage, columnsPerPage)
                    document.finishPage(page)
                }
            }
            document.writeTo(output)
        } finally {
            document.close()
        }
    }

    private fun drawPdfPage(
        snapshot: SheetSnapshot,
        canvas: android.graphics.Canvas,
        rowBlock: Int,
        columnBlock: Int,
        rowsPerPage: Int,
        columnsPerPage: Int
    ) {
        canvas.drawColor(Color.WHITE)
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(24, 42, 67); textSize = 18f; typeface = Typeface.DEFAULT_BOLD
        }
        val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(98, 111, 132); textSize = 10f }
        val normal = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 38, 50); textSize = 10.5f }
        val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(205, 213, 224); strokeWidth = 0.8f; style = Paint.Style.STROKE }
        val headerFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(235, 241, 248); style = Paint.Style.FILL }
        canvas.drawText(snapshot.documentName.take(72), PDF_MARGIN, PDF_MARGIN + 18f, title)
        canvas.drawText(
            "${snapshot.sheetName} · Trang dữ liệu ${rowBlock + 1}/${kotlin.math.ceil(snapshot.rows / rowsPerPage.toDouble()).toInt()} · Khối cột ${columnBlock + 1}",
            PDF_MARGIN, PDF_MARGIN + 34f, sub
        )
        val originX = PDF_MARGIN
        val originY = PDF_MARGIN + PDF_TITLE_HEIGHT
        canvas.drawRect(originX, originY, originX + PDF_INDEX_WIDTH, originY + PDF_HEADER_HEIGHT, headerFill)
        canvas.drawRect(originX, originY, originX + PDF_INDEX_WIDTH, originY + PDF_HEADER_HEIGHT, grid)
        val firstColumn = columnBlock * columnsPerPage + 1
        val lastColumn = minOf(snapshot.columns, firstColumn + columnsPerPage - 1)
        for (col in firstColumn..lastColumn) {
            val local = col - firstColumn
            val left = originX + PDF_INDEX_WIDTH + local * PDF_COLUMN_WIDTH
            canvas.drawRect(left, originY, left + PDF_COLUMN_WIDTH, originY + PDF_HEADER_HEIGHT, headerFill)
            canvas.drawRect(left, originY, left + PDF_COLUMN_WIDTH, originY + PDF_HEADER_HEIGHT, grid)
            val label = OfficeOoxml.cellAddress(1, col).dropLast(1)
            normal.typeface = Typeface.DEFAULT_BOLD
            normal.color = Color.rgb(55, 69, 91)
            drawCentered(canvas, label, left, originY, PDF_COLUMN_WIDTH, PDF_HEADER_HEIGHT, normal)
        }
        val firstRow = rowBlock * rowsPerPage + 1
        val lastRow = minOf(snapshot.rows, firstRow + rowsPerPage - 1)
        for (row in firstRow..lastRow) {
            val localRow = row - firstRow
            val top = originY + PDF_HEADER_HEIGHT + localRow * PDF_ROW_HEIGHT
            canvas.drawRect(originX, top, originX + PDF_INDEX_WIDTH, top + PDF_ROW_HEIGHT, headerFill)
            canvas.drawRect(originX, top, originX + PDF_INDEX_WIDTH, top + PDF_ROW_HEIGHT, grid)
            normal.typeface = Typeface.DEFAULT
            normal.color = Color.rgb(78, 91, 111)
            drawCentered(canvas, row.toString(), originX, top, PDF_INDEX_WIDTH, PDF_ROW_HEIGHT, normal)
            var col = firstColumn
            while (col <= lastColumn) {
                val merge = snapshot.merges.firstOrNull { it.startRow == row && it.endRow == row && col in it.startCol..it.endCol }
                if (merge != null && col != merge.startCol) { col++; continue }
                val spanEnd = if (merge != null) minOf(merge.endCol, lastColumn) else col
                val span = spanEnd - col + 1
                val left = originX + PDF_INDEX_WIDTH + (col - firstColumn) * PDF_COLUMN_WIDTH
                val width = PDF_COLUMN_WIDTH * span
                val address = OfficeOoxml.cellAddress(row, if (merge != null) merge.startCol else col)
                val appearance = snapshot.appearances[address] ?: OfficeOoxml.CellAppearance()
                appearance.fillRgb?.let { rgb -> parseRgb(rgb)?.let { color ->
                    Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }.also {
                        canvas.drawRect(left, top, left + width, top + PDF_ROW_HEIGHT, it)
                    }
                } }
                canvas.drawRect(left, top, left + width, top + PDF_ROW_HEIGHT, grid)
                val text = snapshot.values[address].orEmpty().replace('\n', ' ')
                if (text.isNotBlank()) {
                    normal.typeface = if (appearance.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    normal.isUnderlineText = appearance.underline
                    normal.textSkewX = if (appearance.italic) -0.18f else 0f
                    normal.color = appearance.fontRgb?.let(::parseRgb) ?: Color.rgb(25, 31, 40)
                    drawClippedText(canvas, text, left, top, width, PDF_ROW_HEIGHT, normal, appearance.alignment)
                    normal.isUnderlineText = false; normal.textSkewX = 0f
                }
                col = spanEnd + 1
            }
        }
    }

    fun writePng(snapshot: SheetSnapshot, output: OutputStream) {
        val cellWidth = 138
        val rowHeight = 42
        val indexWidth = 54
        val titleHeight = 58
        val maxSide = 8192
        val columns = minOf(snapshot.columns.coerceAtLeast(1), (maxSide - indexWidth) / cellWidth)
        val rows = minOf(snapshot.rows.coerceAtLeast(1), (maxSide - titleHeight - rowHeight) / rowHeight)
        val width = indexWidth + columns * cellWidth
        val height = titleHeight + rowHeight + rows * rowHeight
        val bitmap = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(24, 42, 67); textSize = 22f; typeface = Typeface.DEFAULT_BOLD }
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 38, 50); textSize = 16f }
        val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(205, 213, 224); strokeWidth = 1f; style = Paint.Style.STROKE }
        val header = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(235, 241, 248); style = Paint.Style.FILL }
        canvas.drawText("${snapshot.documentName} · ${snapshot.sheetName}".take(90), 16f, 35f, title)
        canvas.drawRect(0f, titleHeight.toFloat(), indexWidth.toFloat(), (titleHeight + rowHeight).toFloat(), header)
        canvas.drawRect(0f, titleHeight.toFloat(), indexWidth.toFloat(), (titleHeight + rowHeight).toFloat(), grid)
        for (col in 1..columns) {
            val left = indexWidth + (col - 1) * cellWidth
            canvas.drawRect(left.toFloat(), titleHeight.toFloat(), (left + cellWidth).toFloat(), (titleHeight + rowHeight).toFloat(), header)
            canvas.drawRect(left.toFloat(), titleHeight.toFloat(), (left + cellWidth).toFloat(), (titleHeight + rowHeight).toFloat(), grid)
            text.typeface = Typeface.DEFAULT_BOLD; text.color = Color.rgb(55, 69, 91)
            drawCentered(canvas, OfficeOoxml.cellAddress(1, col).dropLast(1), left.toFloat(), titleHeight.toFloat(), cellWidth.toFloat(), rowHeight.toFloat(), text)
        }
        for (row in 1..rows) {
            val top = titleHeight + rowHeight + (row - 1) * rowHeight
            canvas.drawRect(0f, top.toFloat(), indexWidth.toFloat(), (top + rowHeight).toFloat(), header)
            canvas.drawRect(0f, top.toFloat(), indexWidth.toFloat(), (top + rowHeight).toFloat(), grid)
            text.typeface = Typeface.DEFAULT; text.color = Color.rgb(78, 91, 111)
            drawCentered(canvas, row.toString(), 0f, top.toFloat(), indexWidth.toFloat(), rowHeight.toFloat(), text)
            var col = 1
            while (col <= columns) {
                val merge = snapshot.merges.firstOrNull { it.startRow == row && it.endRow == row && col in it.startCol..it.endCol }
                if (merge != null && col != merge.startCol) { col++; continue }
                val spanEnd = if (merge != null) minOf(merge.endCol, columns) else col
                val span = spanEnd - col + 1
                val left = indexWidth + (col - 1) * cellWidth
                val widthCell = cellWidth * span
                val address = OfficeOoxml.cellAddress(row, if (merge != null) merge.startCol else col)
                val appearance = snapshot.appearances[address] ?: OfficeOoxml.CellAppearance()
                appearance.fillRgb?.let { rgb -> parseRgb(rgb)?.let { color ->
                    Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }.also {
                        canvas.drawRect(left.toFloat(), top.toFloat(), (left + widthCell).toFloat(), (top + rowHeight).toFloat(), it)
                    }
                } }
                canvas.drawRect(left.toFloat(), top.toFloat(), (left + widthCell).toFloat(), (top + rowHeight).toFloat(), grid)
                val value = snapshot.values[address].orEmpty().replace('\n', ' ')
                if (value.isNotBlank()) {
                    text.typeface = if (appearance.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    text.isUnderlineText = appearance.underline
                    text.textSkewX = if (appearance.italic) -0.18f else 0f
                    text.color = appearance.fontRgb?.let(::parseRgb) ?: Color.rgb(25, 31, 40)
                    drawClippedText(canvas, value, left.toFloat(), top.toFloat(), widthCell.toFloat(), rowHeight.toFloat(), text, appearance.alignment)
                    text.isUnderlineText = false; text.textSkewX = 0f
                }
                col = spanEnd + 1
            }
        }
        try {
            require(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Không thể mã hóa ảnh PNG." }
        } finally {
            bitmap.recycle()
        }
    }

    fun writeCombinedWorkbook(snapshots: List<SheetSnapshot>, output: OutputStream) {
        require(snapshots.isNotEmpty()) { "Không có worksheet để hợp nhất." }
        val rows = mutableListOf<Map<Int, String>>()
        val styles = mutableMapOf<Pair<Int, Int>, OfflineXlsxWriter.CellStyle>()
        val merges = mutableListOf<OfflineXlsxWriter.Merge>()
        var maxColumns = 1
        snapshots.forEach { sheet ->
            val titleRow = rows.size
            rows += mapOf(0 to sheet.sheetName)
            styles[titleRow to 0] = OfflineXlsxWriter.CellStyle(
                bold = true, fontSize = 13, fillRgb = "D9EAD3", alignment = "left",
                borders = OfflineXlsxWriter.Borders(true, true, true, true)
            )
            val columns = sheet.columns.coerceIn(1, 80)
            maxColumns = maxOf(maxColumns, columns)
            if (columns > 1) merges += OfflineXlsxWriter.Merge(titleRow, 0, titleRow, columns - 1)
            val rowOffset = rows.size
            for (row in 1..sheet.rows.coerceIn(1, 5000)) {
                val data = linkedMapOf<Int, String>()
                for (col in 1..columns) {
                    val address = OfficeOoxml.cellAddress(row, col)
                    val value = sheet.values[address].orEmpty()
                    if (value.isNotEmpty()) data[col - 1] = value
                    val appearance = sheet.appearances[address]
                    if (appearance != null && appearance != OfficeOoxml.CellAppearance()) {
                        styles[(rowOffset + row - 1) to (col - 1)] = OfflineXlsxWriter.CellStyle(
                            bold = appearance.bold, fillRgb = appearance.fillRgb,
                            alignment = appearance.alignment.takeIf { it in setOf("left", "center", "right") } ?: "left"
                        )
                    }
                }
                rows += data
            }
            rows += emptyMap()
        }
        OfflineXlsxWriter.write(
            listOf(
                OfflineXlsxWriter.SheetData(
                    name = "Hop nhat",
                    rows = rows,
                    colWidthsPt = List(maxColumns) { 92f },
                    merges = merges,
                    cellStyles = styles
                )
            ), output
        )
    }

    fun csv(snapshot: SheetSnapshot, separator: Char = ','): String {
        val rows = snapshot.rows.coerceIn(1, 10000)
        val cols = snapshot.columns.coerceIn(1, 256)
        return buildString {
            for (row in 1..rows) {
                if (row > 1) append("\r\n")
                for (col in 1..cols) {
                    if (col > 1) append(separator)
                    val value = snapshot.values[OfficeOoxml.cellAddress(row, col)].orEmpty()
                    if (separator == '\t') append(value.replace("\t", " ").replace("\r", " ").replace("\n", " "))
                    else {
                        append('"'); append(value.replace("\"", "\"\"")); append('"')
                    }
                }
            }
        }
    }

    private fun parseRgb(rgb: String): Int? = runCatching {
        Color.parseColor("#${rgb.takeLast(6)}")
    }.getOrNull()

    private fun drawCentered(canvas: android.graphics.Canvas, value: String, left: Float, top: Float, width: Float, height: Float, paint: Paint) {
        val baseline = top + height / 2f - (paint.ascent() + paint.descent()) / 2f
        canvas.drawText(value, left + (width - paint.measureText(value)) / 2f, baseline, paint)
    }

    private fun drawClippedText(
        canvas: android.graphics.Canvas, value: String, left: Float, top: Float,
        width: Float, height: Float, paint: Paint, alignment: String
    ) {
        val padding = 6f
        val baseline = top + height / 2f - (paint.ascent() + paint.descent()) / 2f
        val measured = paint.measureText(value)
        val x = when (alignment) {
            "right" -> left + width - measured - padding
            "center" -> left + (width - measured) / 2f
            else -> left + padding
        }
        canvas.save()
        canvas.clipRect(left + 1f, top + 1f, left + width - 1f, top + height - 1f)
        canvas.drawText(value, x, baseline, paint)
        canvas.restore()
    }
}

/** Password-protected DocFlow backup. This is a DocFlow .dfx container, not native Excel encryption. */
internal object DocFlowCrypto {
    private val MAGIC_V1 = byteArrayOf('D'.code.toByte(), 'F'.code.toByte(), 'X'.code.toByte(), '1'.code.toByte())
    private val MAGIC_V2 = byteArrayOf('D'.code.toByte(), 'F'.code.toByte(), 'X'.code.toByte(), '2'.code.toByte())
    private const val LEGACY_ITERATIONS = 120_000
    private const val CURRENT_ITERATIONS = 210_000
    private const val CURRENT_KEY_BITS = 256

    data class Decrypted(val filename: String, val bytes: ByteArray)

    fun encrypt(bytes: ByteArray, password: String, filename: String): ByteArray {
        require(password.length >= 10) { "Mật khẩu cần ít nhất 10 ký tự." }
        require(bytes.size <= 96 * 1024 * 1024) { "Tệp vượt quá 96 MB." }
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val iv = ByteArray(12).also(SecureRandom()::nextBytes)
        val nameBytes = filename.take(180).toByteArray(Charsets.UTF_8)
        val plain = ByteArrayOutputStream().also { raw ->
            DataOutputStream(raw).use { data ->
                data.writeInt(nameBytes.size); data.write(nameBytes); data.writeInt(bytes.size); data.write(bytes)
            }
        }.toByteArray()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            key(password, salt, CURRENT_ITERATIONS, CURRENT_KEY_BITS),
            GCMParameterSpec(128, iv)
        )
        val encrypted = cipher.doFinal(plain)
        return ByteArrayOutputStream().also { raw ->
            DataOutputStream(raw).use { data ->
                data.write(MAGIC_V2)
                data.writeInt(CURRENT_ITERATIONS)
                data.writeInt(CURRENT_KEY_BITS)
                data.writeInt(salt.size); data.write(salt)
                data.writeInt(iv.size); data.write(iv)
                data.writeInt(encrypted.size); data.write(encrypted)
            }
        }.toByteArray()
    }

    fun decrypt(container: ByteArray, password: String): Decrypted {
        require(container.size >= 4 + 16 + 12 + 4 + 16) { "Bản sao mã hóa không hợp lệ." }
        try {
            DataInputStream(ByteArrayInputStream(container)).use { data ->
                val magic = ByteArray(4).also(data::readFully)
                val parameters = when {
                    magic.contentEquals(MAGIC_V1) -> CryptoParameters(
                        LEGACY_ITERATIONS,
                        CURRENT_KEY_BITS,
                        ByteArray(16).also(data::readFully),
                        ByteArray(12).also(data::readFully)
                    )
                    magic.contentEquals(MAGIC_V2) -> {
                        val iterations = data.readInt()
                        val keyBits = data.readInt()
                        val saltLength = data.readInt()
                        require(iterations in 100_000..2_000_000) { "Số vòng bảo vệ .dfx không hợp lệ." }
                        require(keyBits == 128 || keyBits == 192 || keyBits == 256) { "Độ dài khóa .dfx không hợp lệ." }
                        require(saltLength in 16..64) { "Salt .dfx không hợp lệ." }
                        val salt = ByteArray(saltLength).also(data::readFully)
                        val ivLength = data.readInt()
                        require(ivLength in 12..32) { "IV .dfx không hợp lệ." }
                        val iv = ByteArray(ivLength).also(data::readFully)
                        CryptoParameters(iterations, keyBits, salt, iv)
                    }
                    else -> throw IllegalArgumentException("Đây không phải bản sao DocFlow .dfx.")
                }
                val length = data.readInt()
                require(length in 16..(96 * 1024 * 1024 + 4096) && length == data.available()) { "Dữ liệu mã hóa bị hỏng." }
                val encrypted = ByteArray(length).also(data::readFully)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    key(password, parameters.salt, parameters.iterations, parameters.keyBits),
                    GCMParameterSpec(128, parameters.iv)
                )
                val plain = cipher.doFinal(encrypted)
                DataInputStream(ByteArrayInputStream(plain)).use { decoded ->
                    val nameLength = decoded.readInt()
                    require(nameLength in 1..512) { "Tên tệp trong bản sao không hợp lệ." }
                    val name = ByteArray(nameLength).also(decoded::readFully).toString(Charsets.UTF_8)
                    val size = decoded.readInt()
                    require(size in 0..(96 * 1024 * 1024) && size == decoded.available()) { "Nội dung bản sao bị hỏng." }
                    return Decrypted(name, ByteArray(size).also(decoded::readFully))
                }
            }
        } catch (e: javax.crypto.AEADBadTagException) {
            throw IllegalArgumentException("Sai mật khẩu hoặc bản sao đã bị thay đổi.")
        }
    }

    private data class CryptoParameters(
        val iterations: Int,
        val keyBits: Int,
        val salt: ByteArray,
        val iv: ByteArray
    )

    private fun key(password: String, salt: ByteArray, iterations: Int, keyBits: Int): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, keyBits)
        return try {
            SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}

// ================ existing unruled table parsing ================
internal object PdfTableLayout {
    data class Glyph(val text: String, val x: Float, val y: Float, val width: Float, val height: Float)
    private data class Cluster(val glyphs: MutableList<Glyph>, var y: Float, var height: Float)
    private data class CellFragment(val x: Float, val text: String)

    fun extract(glyphs: List<Glyph>): List<Map<Int, String>> {
        if (glyphs.isEmpty()) return emptyList()
        val sorted = glyphs.filter { it.text.isNotBlank() && it.x.isFinite() && it.y.isFinite() }
            .sortedWith(compareBy<Glyph> { it.y }.thenBy { it.x })
        if (sorted.isEmpty()) return emptyList()
        val lines = ArrayList<Cluster>()
        for (g in sorted) {
            val last = lines.lastOrNull()
            val tolerance = if (last == null) 0f else minOf(3.7f, maxOf(2.1f, last.height * 0.36f))
            if (last != null && kotlin.math.abs(last.y - g.y) <= tolerance) {
                last.glyphs.add(g)
                val n = last.glyphs.size
                last.y = (last.y * (n - 1) + g.y) / n
                last.height = maxOf(last.height, g.height)
            } else {
                lines.add(Cluster(mutableListOf(g), g.y, g.height))
            }
        }
        val rows: List<List<CellFragment>> = lines.map { line ->
            val chars = line.glyphs.sortedBy { it.x }
            val fragments = ArrayList<CellFragment>()
            var startX = chars.first().x
            var endX = startX
            val current = StringBuilder()
            val gapLimit = maxOf(9f, minOf(19f, line.height * 0.90f))
            fun addFragment() {
                val s = current.toString().trim().replace(Regex("[\\t ]+"), " ")
                if (s.isNotEmpty()) fragments.add(CellFragment(startX, s))
                current.setLength(0)
            }
            for (g in chars) {
                val gap = g.x - endX
                if (current.isNotEmpty() && gap > gapLimit) {
                    addFragment()
                    startX = g.x
                } else if (current.isNotEmpty() && gap > 2.6f && !current.last().isWhitespace()) {
                    // Keep normal word spaces inside a cell; split only on wide visual gaps.
                    current.append(' ')
                }
                current.append(g.text)
                endX = maxOf(endX, g.x + g.width)
            }
            addFragment()
            fragments
        }.filter { it.isNotEmpty() }
        if (rows.isEmpty()) return emptyList()

        // Use the most typical multi-column line as a positional template.
        // A PDF has no real column metadata; on irregular pages, one-column text is safer.
        val possible = rows.filter { it.size in 2..35 }
        val anchors: List<Float> = if (possible.isEmpty()) listOf(rows.first().first().x) else {
            val commonSize = possible.groupingBy { it.size }.eachCount()
                .maxWithOrNull(compareBy<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })!!.key
            val representative = possible.first { it.size == commonSize }
            representative.map { it.x }
        }
        return rows.map { fragments ->
            val line = LinkedHashMap<Int, String>()
            var prevCol = -1
            for (cell in fragments) {
                var col = anchors.indices.minByOrNull { kotlin.math.abs(anchors[it] - cell.x) } ?: 0
                col = maxOf(col, prevCol + 1)
                if (col >= 256) throw IOException("Trang PDF có quá nhiều cột để xuất Excel.")
                line[col] = cell.text
                prevCol = col
            }
            line
        }
    }
}



// ================ PDF -> Word (editable text / layout-aware tables / scan images) ================
/**
 * Converts locally on Android with the PDFBox already included in DocFlow.
 * PDF text is reconstructed as editable Word paragraphs and, for simple ruled
 * forms, Word tables. Scan-only pages are embedded as pictures, not fake OCR.
 * PDF does not carry Word's paragraph/table semantics, so fidelity is best-effort.
 */
object PdfWordConverter {
    private const val MAX_PAGES = 80
    private const val MAX_BYTES = 75L * 1024 * 1024
    private const val MAX_GLYPHS_PER_PAGE = 90_000
    private const val MAX_TOTAL_GLYPHS = 260_000

    fun convert(context: Context, uri: Uri, output: OutputStream) {
        PDFBoxResourceLoader.init(context.applicationContext)
        val local = java.io.File.createTempFile("docflow_pdf_word_", ".pdf", context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                local.outputStream().buffered().use { target ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_BYTES) throw IOException("PDF vượt giới hạn chuyển đổi offline 75 MB.")
                        target.write(buffer, 0, read)
                    }
                }
            } ?: throw IOException("Không đọc được PDF nguồn.")
            java.io.FileInputStream(local).use { input ->
                PDDocument.load(input).use { pdf ->
                    require(pdf.numberOfPages in 1..MAX_PAGES) {
                        "Hỗ trợ 1–$MAX_PAGES trang PDF cho mỗi lần chuyển đổi."
                    }
                    val pages = ArrayList<OfflineDocxWriter.Page>(pdf.numberOfPages)
                    var allGlyphs = 0
                    for (index in 0 until pdf.numberOfPages) {
                        val number = index + 1
                        val pdfPage = pdf.getPage(index)
                        val angle = ((pdfPage.rotation % 360) + 360) % 360
                        val pageWidth = pdfPage.cropBox.width.coerceAtLeast(50f)
                        val pageHeight = pdfPage.cropBox.height.coerceAtLeast(50f)
                        val glyphs = ArrayList<OfflineDocxWriter.Glyph>()
                        val reader = object : com.tom_roush.pdfbox.text.PDFTextStripper() {
                            init {
                                sortByPosition = true
                                startPage = number
                                endPage = number
                            }
                            override fun writeString(
                                text: String,
                                textPositions: MutableList<com.tom_roush.pdfbox.text.TextPosition>
                            ) {
                                for (pos in textPositions) {
                                    val content = pos.unicode ?: continue
                                    if (content.isBlank()) continue
                                    if (glyphs.size >= MAX_GLYPHS_PER_PAGE) {
                                        throw IOException("Trang $number có quá nhiều ký tự.")
                                    }
                                    val x = pos.xDirAdj
                                    val y = pos.yDirAdj
                                    if (!x.isFinite() || !y.isFinite()) continue
                                    val fontName = pos.font?.name.orEmpty()
                                    glyphs.add(OfflineDocxWriter.Glyph(
                                        text = content,
                                        x = x,
                                        y = y,
                                        width = pos.widthDirAdj.coerceAtLeast(0.1f),
                                        height = pos.heightDir.coerceAtLeast(1f),
                                        fontSize = pos.fontSizeInPt.coerceIn(5f, 48f),
                                        bold = fontName.contains("Bold", true) || fontName.contains("Black", true),
                                        italic = fontName.contains("Italic", true) || fontName.contains("Oblique", true)
                                    ))
                                }
                            }
                        }
                        reader.getText(pdf)
                        allGlyphs += glyphs.size
                        if (allGlyphs > MAX_TOTAL_GLYPHS) {
                            throw IOException("PDF có quá nhiều chữ để chuyển trên thiết bị. Hãy tách tệp nhỏ hơn.")
                        }
                        // A page without selectable text needs an image fallback. This is
                        // not OCR, and Word editing is only possible for real text pages.
                        if (glyphs.sumOf { it.text.length } < 8) {
                            val image = renderScanPage(local, index)
                            val w = if (angle == 90 || angle == 270) pageHeight else pageWidth
                            val h = if (angle == 90 || angle == 270) pageWidth else pageHeight
                            pages.add(OfflineDocxWriter.Page(w, h, emptyList(), image, null))
                            continue
                        }
                        var table: OfflineXlsxWriter.SheetData? = null
                        // Reuse v0.8's vector-ruled form detection to rebuild Word tables.
                        if (angle == 0) {
                            val vector = runCatching {
                                PdfVectorGrid(pdfPage, pageHeight).apply { processPage(pdfPage) }
                            }.getOrNull()
                            if (vector != null && vector.lines.size >= 6) {
                                val grid = runCatching {
                                    PdfPageGrid.analyze(
                                        glyphs.map {
                                            PdfPageGrid.Glyph(it.text, it.x, it.y, it.width, it.height,
                                                it.fontSize, it.bold)
                                        }, vector.lines, vector.fills, pageWidth, pageHeight
                                    )
                                }.getOrNull()
                                if (grid != null && grid.colWidthsPt.size in 2..24 &&
                                    grid.rowHeightsPt.size in 1..140 &&
                                    grid.colWidthsPt.size * grid.rowHeightsPt.size <= 2400) {
                                    table = grid
                                }
                            }
                        }
                        val w = if (angle == 90 || angle == 270) pageHeight else pageWidth
                        val h = if (angle == 90 || angle == 270) pageWidth else pageHeight
                        pages.add(OfflineDocxWriter.Page(w, h, glyphs, null, table))
                    }
                    OfflineDocxWriter.write(pages, output)
                }
            }
        } finally {
            local.delete()
        }
    }

    private fun renderScanPage(pdf: java.io.File, index: Int): ByteArray {
        val descriptor = android.os.ParcelFileDescriptor.open(
            pdf, android.os.ParcelFileDescriptor.MODE_READ_ONLY
        )
        android.graphics.pdf.PdfRenderer(descriptor).use { renderer ->
            renderer.openPage(index).use { page ->
                val factor = minOf(1.8f, 1700f / maxOf(page.width, page.height).toFloat())
                val width = (page.width * factor).toInt().coerceAtLeast(1)
                val height = (page.height * factor).toInt().coerceAtLeast(1)
                val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                try {
                    image.eraseColor(Color.WHITE)
                    page.render(image, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    return ByteArrayOutputStream().use { bytes ->
                        if (!image.compress(Bitmap.CompressFormat.JPEG, 84, bytes)) {
                            throw IOException("Không thể xuất ảnh trang ${index + 1}.")
                        }
                        bytes.toByteArray()
                    }
                } finally { image.recycle() }
            }
        }
    }
}

/** DOCX OOXML writer: no remote service, template, paid library or extra dependency. */
internal object OfflineDocxWriter {
    data class Glyph(
        val text: String, val x: Float, val y: Float, val width: Float, val height: Float,
        val fontSize: Float, val bold: Boolean = false, val italic: Boolean = false
    )
    data class Page(
        val widthPt: Float, val heightPt: Float, val glyphs: List<Glyph>,
        val scanJpeg: ByteArray? = null, val table: OfflineXlsxWriter.SheetData? = null
    )
    private data class TextRun(val text: String, val size: Float, val bold: Boolean, val italic: Boolean)
    private data class Part(val x: Float, val runs: List<TextRun>)
    private data class Line(val y: Float, val height: Float, val parts: List<Part>)
    private const val WORD = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
    private const val MAIN_REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val PACK_REL = "http://schemas.openxmlformats.org/package/2006/relationships"
    private const val TYPES = "http://schemas.openxmlformats.org/package/2006/content-types"
    private const val EMU_PER_PT = 12700L
    private const val PAGE_MARGIN_PT = 12f

    private fun escape(text: String): String {
        val safe = text.filter { ch -> (ch >= ' ' || ch == '\t' || ch == '\n') && ch != '\uFFFE' && ch != '\uFFFF' }
        return safe.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&apos;")
    }
    private fun ptTwip(value: Float): Int = (value.coerceAtLeast(0f) * 20f).toInt()
    private fun docxText(text: String): String = "<w:t xml:space=\"preserve\">${escape(text)}</w:t>"

    private fun fontRun(run: TextRun): String = buildString {
        if (run.text.isEmpty()) return@buildString
        val half = (run.size.coerceIn(5f, 48f) * 2f).toInt().coerceIn(10, 96)
        append("<w:r><w:rPr><w:rFonts w:ascii=\"Arial\" w:hAnsi=\"Arial\"/>")
        append("<w:sz w:val=\"$half\"/>")
        if (run.bold) append("<w:b/>")
        if (run.italic) append("<w:i/>")
        append("</w:rPr>")
        append(docxText(run.text))
        append("</w:r>")
    }

    private fun toLines(glyphs: List<Glyph>): List<Line> {
        if (glyphs.isEmpty()) return emptyList()
        val rows = ArrayList<MutableList<Glyph>>()
        val ordered = glyphs.sortedWith(compareBy<Glyph> { it.y }.thenBy { it.x })
        var baseline = Float.NaN
        for (g in ordered) {
            if (rows.isEmpty() || kotlin.math.abs(g.y - baseline) > maxOf(2.7f, g.fontSize * .36f)) {
                rows.add(ArrayList())
                baseline = g.y
            }
            rows.last().add(g)
        }
        return rows.map { row ->
            val sorted = row.sortedBy { it.x }
            val parts = ArrayList<Part>()
            var chars = ArrayList<Glyph>()
            fun flush() {
                if (chars.isEmpty()) return
                val runs = ArrayList<TextRun>()
                val accumulator = StringBuilder()
                var lastEnd = Float.NaN
                var size = chars.first().fontSize
                var bold = chars.first().bold
                var italic = chars.first().italic
                fun flushRun() {
                    if (accumulator.isNotEmpty()) {
                        runs.add(TextRun(accumulator.toString(), size, bold, italic))
                        accumulator.setLength(0)
                    }
                }
                for (glyph in chars) {
                    val gap = glyph.x - lastEnd
                    if (lastEnd.isFinite() && gap > maxOf(1.5f, glyph.fontSize * .20f)) {
                        accumulator.append(' ')
                    }
                    if (glyph.bold != bold || glyph.italic != italic || kotlin.math.abs(glyph.fontSize - size) >= 1.5f) {
                        flushRun()
                        size = glyph.fontSize
                        bold = glyph.bold
                        italic = glyph.italic
                    }
                    accumulator.append(glyph.text)
                    lastEnd = maxOf(glyph.x + glyph.width, if (lastEnd.isFinite()) lastEnd else 0f)
                }
                flushRun()
                parts.add(Part(chars.first().x, runs))
                chars = ArrayList()
            }
            var previousEnd = Float.NaN
            for (glyph in sorted) {
                val gap = glyph.x - previousEnd
                if (previousEnd.isFinite() && gap > maxOf(16f, glyph.fontSize * 1.65f)) flush()
                chars.add(glyph)
                previousEnd = maxOf(glyph.x + glyph.width,
                    if (previousEnd.isFinite()) previousEnd else 0f)
            }
            flush()
            Line(row.minOf { it.y }, row.maxOf { it.height.coerceAtLeast(it.fontSize) }, parts)
        }
    }

    /** Paragraph per PDF visual line, with nearest Word indents/tabs and vertical spacing. */
    private fun textPage(page: Page): String = buildString {
        val lines = toLines(page.glyphs)
        var bottom = PAGE_MARGIN_PT
        for (line in lines) {
            if (line.parts.isEmpty()) continue
            val top = (line.y - line.height * 0.85f).coerceAtLeast(0f)
            val lineHeight = maxOf(line.height * 1.12f, 9f)
            val before = (top - bottom).coerceIn(0f, 120f)
            val firstX = line.parts.first().x
            val indent = (firstX - PAGE_MARGIN_PT).coerceIn(0f, page.widthPt * .8f)
            append("<w:p><w:pPr>")
            append("<w:ind w:left=\"${ptTwip(indent)}\"/>")
            append("<w:spacing w:before=\"${ptTwip(before)}\" w:after=\"0\" w:line=\"${ptTwip(lineHeight)}\" w:lineRule=\"exact\"/>")
            if (line.parts.size > 1) {
                append("<w:tabs>")
                for (part in line.parts.drop(1)) {
                    val pos = ptTwip((part.x - PAGE_MARGIN_PT).coerceAtLeast(1f))
                    append("<w:tab w:val=\"left\" w:pos=\"$pos\"/>")
                }
                append("</w:tabs>")
            }
            append("</w:pPr>")
            line.parts.forEachIndexed { index, part ->
                if (index > 0) append("<w:r><w:tab/></w:r>")
                part.runs.forEach { append(fontRun(it)) }
            }
            append("</w:p>")
            bottom = top + lineHeight
        }
    }

    /** Gridded vector PDF reports become editable Word tables (not screenshots). */
    private fun tablePage(page: Page, sheet: OfflineXlsxWriter.SheetData): String = buildString {
        val cols = sheet.colWidthsPt.size
        if (cols <= 0) { append(textPage(page)); return@buildString }
        val usableWidth = (page.widthPt - PAGE_MARGIN_PT * 2).coerceAtLeast(80f)
        val originalWidth = sheet.colWidthsPt.sum().coerceAtLeast(1f)
        val ratio = minOf(1f, usableWidth / originalWidth)
        val colTwips = sheet.colWidthsPt.map { ptTwip((it * ratio).coerceAtLeast(2f)) }
        append("<w:tbl><w:tblPr><w:tblW w:type=\"dxa\" w:w=\"${colTwips.sum()}\"/>")
        append("<w:tblLayout w:type=\"fixed\"/><w:tblCellMar><w:top w:w=\"12\" w:type=\"dxa\"/><w:left w:w=\"30\" w:type=\"dxa\"/><w:bottom w:w=\"12\" w:type=\"dxa\"/><w:right w:w=\"30\" w:type=\"dxa\"/></w:tblCellMar></w:tblPr>")
        append("<w:tblGrid>")
        colTwips.forEach { append("<w:gridCol w:w=\"$it\"/>") }
        append("</w:tblGrid>")
        val maxRows = minOf(sheet.rowHeightsPt.size, sheet.rows.size)
        for (r in 0 until maxRows) {
            append("<w:tr><w:trPr>")
            val rowHeight = sheet.rowHeightsPt[r] * ratio
            if (rowHeight in 3f..250f) append("<w:trHeight w:val=\"${ptTwip(rowHeight)}\" w:hRule=\"atLeast\"/>")
            append("</w:trPr>")
            var c = 0
            while (c < cols) {
                val merge = sheet.merges.firstOrNull { it.r1 == r && it.r2 == r && it.c1 == c }
                val span = if (merge != null) (merge.c2 - merge.c1 + 1).coerceIn(1, cols - c) else 1
                val style = sheet.cellStyles[r to c]
                val width = colTwips.subList(c, c + span).sum()
                append("<w:tc><w:tcPr><w:tcW w:w=\"$width\" w:type=\"dxa\"/>")
                if (span > 1) append("<w:gridSpan w:val=\"$span\"/>")
                style?.fillRgb?.uppercase(Locale.ROOT)?.takeIf { it.matches(Regex("[0-9A-F]{6}")) }?.let { color ->
                    append("<w:shd w:val=\"clear\" w:fill=\"$color\"/>")
                }
                val borders = style?.borders
                if (borders != null && borders != OfflineXlsxWriter.Borders()) {
                    append("<w:tcBorders>")
                    for ((side, enabled) in listOf("top" to borders.top, "left" to borders.left,
                        "bottom" to borders.bottom, "right" to borders.right)) {
                        if (enabled) append("<w:$side w:val=\"single\" w:sz=\"4\" w:color=\"7A8592\"/>")
                    }
                    append("</w:tcBorders>")
                }
                append("</w:tcPr><w:p><w:pPr>")
                when (style?.alignment) {
                    "center" -> append("<w:jc w:val=\"center\"/>")
                    "right" -> append("<w:jc w:val=\"right\"/>")
                }
                append("<w:spacing w:after=\"0\"/></w:pPr>")
                val cellText = sheet.rows[r][c].orEmpty()
                if (cellText.isNotEmpty()) append(fontRun(TextRun(cellText, style?.fontSize?.toFloat() ?: 10f,
                    style?.bold ?: false, false)))
                append("</w:p></w:tc>")
                c += span
            }
            append("</w:tr>")
        }
        append("</w:tbl>")
    }

    private fun pictureXml(index: Int, page: Page): String {
        // A full-height inline picture can push the following page break to an
        // extra empty page in Word/LibreOffice. Keep space for the paragraph mark.
        val maxW = (page.widthPt - PAGE_MARGIN_PT * 2 - 8f).coerceAtLeast(50f)
        val maxH = (page.heightPt - PAGE_MARGIN_PT * 2 - 42f).coerceAtLeast(50f)
        val factor = minOf(maxW / page.widthPt, maxH / page.heightPt, 1f)
        val widthEmu = (page.widthPt * factor * EMU_PER_PT).toLong()
        val heightEmu = (page.heightPt * factor * EMU_PER_PT).toLong()
        return """<w:p><w:pPr><w:spacing w:before="0" w:after="0"/></w:pPr><w:r><w:drawing>
          <wp:inline distT="0" distB="0" distL="0" distR="0">
            <wp:extent cx="$widthEmu" cy="$heightEmu"/><wp:docPr id="$index" name="PDF scan page $index" descr="PDF goc dang anh, chua co OCR"/>
            <a:graphic><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">
              <pic:pic><pic:nvPicPr><pic:cNvPr id="0" name="scan-$index.jpg"/><pic:cNvPicPr/></pic:nvPicPr>
                <pic:blipFill><a:blip r:embed="rId$index"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>
                <pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="$widthEmu" cy="$heightEmu"/></a:xfrm>
                  <a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr>
              </pic:pic>
            </a:graphicData></a:graphic>
          </wp:inline></w:drawing></w:r></w:p>""".trimIndent()
    }

    fun write(pages: List<Page>, output: OutputStream) {
        require(pages.isNotEmpty()) { "Không có trang PDF để xuất Word." }
        val first = pages.first()
        val docXml = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
            append("<w:document xmlns:w=\"$WORD\" xmlns:r=\"$MAIN_REL\" ")
            append("xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\" ")
            append("xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" ")
            append("xmlns:pic=\"http://schemas.openxmlformats.org/drawingml/2006/picture\"><w:body>")
            for ((i, page) in pages.withIndex()) {
                if (i > 0) append("<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>")
                when {
                    page.scanJpeg != null -> append(pictureXml(i + 1, page))
                    page.table != null -> append(tablePage(page, page.table))
                    else -> append(textPage(page))
                }
            }
            val pageW = ptTwip(first.widthPt.coerceIn(100f, 1440f))
            val pageH = ptTwip(first.heightPt.coerceIn(100f, 1440f))
            append("<w:sectPr><w:pgSz w:w=\"$pageW\" w:h=\"$pageH\"")
            if (pageW > pageH) append(" w:orient=\"landscape\"")
            append("/><w:pgMar w:top=\"240\" w:right=\"240\" w:bottom=\"240\" w:left=\"240\" w:header=\"120\" w:footer=\"120\" w:gutter=\"0\"/></w:sectPr>")
            append("</w:body></w:document>")
        }
        ZipOutputStream(output).use { zip ->
            fun part(path: String, data: ByteArray) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(data)
                zip.closeEntry()
            }
            fun xml(path: String, data: String) = part(path, data.toByteArray(Charsets.UTF_8))
            xml("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8"?>
                <Types xmlns="$TYPES">
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="jpg" ContentType="image/jpeg"/>
                  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                </Types>""".trimIndent())
            xml("_rels/.rels", """<?xml version="1.0" encoding="UTF-8"?>
                <Relationships xmlns="$PACK_REL"><Relationship Id="rId1" Type="$MAIN_REL/officeDocument" Target="word/document.xml"/></Relationships>""".trimIndent())
            xml("word/document.xml", docXml)
            val images = pages.withIndex().filter { it.value.scanJpeg != null }
            xml("word/_rels/document.xml.rels", buildString {
                append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                append("<Relationships xmlns=\"$PACK_REL\">")
                for ((index, _) in images) {
                    append("<Relationship Id=\"rId${index+1}\" Type=\"$MAIN_REL/image\" Target=\"media/scan-${index+1}.jpg\"/>")
                }
                append("</Relationships>")
            })
            for ((index, page) in images) {
                part("word/media/scan-${index+1}.jpg", page.scanJpeg!!)
            }
        }
    }
}
