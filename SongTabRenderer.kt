package ducphi.tunnerinstrument

import org.json.JSONObject

/**
 * AlphaTab WebView renderer. HTML/JS for score, TAB, technique drawing,
 * fretboard, playhead overlay and pinch zoom live here.
 * The AndroidInterface bridge lives in SongTabPlay.kt.
 * Future bar-by-bar fretboard guidance can extend this renderer without
 * adding thousands of JS lines to the Compose screen.
 */

// Hàm cung cấp HTML nhúng JS alphaTab
internal fun getAlphaTabHtml(
    isDarkInitial: Boolean,
    initialSoundFontMode: String,
    initialSoundFontUrl: String,
    currentLang: String,
    initialShowNoteNames: Boolean,
    initialMetronomeEnabled: Boolean,
    initialCursorColor: String,
    initialActiveRegionColor: String,
    initialTabColor: String,
    initialNoteColor: String,
    initialFretboardColor: String,
    initialPanelColor: String,
    initialCursorWidth: Float,
    initialRegionOpacity: Float,
    initialScoreZoomPercent: Int
): String {
    val quotedInitialSoundFontMode = JSONObject.quote(initialSoundFontMode)
    val quotedInitialSoundFontUrl = JSONObject.quote(initialSoundFontUrl)
    val quotedStandardSoundFontUrl = JSONObject.quote(STANDARD_SOUNDFONT_URL)
    val quotedInitialCursorColor = JSONObject.quote(initialCursorColor)
    val quotedInitialActiveRegionColor = JSONObject.quote(initialActiveRegionColor)
    val quotedInitialTabColor = JSONObject.quote(initialTabColor)
    val quotedInitialNoteColor = JSONObject.quote(initialNoteColor)
    val quotedInitialFretboardColor = JSONObject.quote(initialFretboardColor)
    val quotedInitialPanelColor = JSONObject.quote(initialPanelColor)
    val quotedSoundFontStartFailed = JSONObject.quote(
        soundFontText(
            "tab_sound_alpha_start_failed", currentLang,
            "AlphaTab không thể bắt đầu nạp SoundFont.",
            "AlphaTab could not start loading the SoundFont."
        )
    )
    val quotedSoundFontLoadFailedPrefix = JSONObject.quote(
        soundFontText(
            "tab_sound_load_failed_prefix", currentLang,
            "Không thể nạp SoundFont: ",
            "Could not load SoundFont: "
        )
    )
    val fretboardTitle = JSONObject.quote(soundFontText(
        "tab_fretboard_title", currentLang, "Cần đàn", "Fretboard"
    ))
    val fretboardIdle = JSONObject.quote(soundFontText(
        "tab_fretboard_idle", currentLang,
        "Nhấn Play để nốt chạy theo TAB", "Press Play to follow the notes"
    ))
    val fretboardActive = JSONObject.quote(soundFontText(
        "tab_fretboard_active", currentLang, "Nốt đang phát", "Playing notes"
    ))
    val fretboardPreview = JSONObject.quote(soundFontText(
        "tab_fretboard_preview", currentLang, "Xem trước nốt đầu", "First-note preview"
    ))
    val fretboardUnsupported = JSONObject.quote(soundFontText(
        "tab_fretboard_unsupported", currentLang,
        "Track này không có vị trí TAB trên dây đàn.",
        "This track does not contain fretted-string TAB notes."
    ))
    val fretboardRest = JSONObject.quote(soundFontText(
        "tab_fretboard_rest", currentLang, "Nốt nghỉ", "Rest"
    ))
    val fretboardClose = JSONObject.quote(soundFontText(
        "tab_fretboard_close", currentLang, "Đóng cần đàn", "Close fretboard"
    ))
    val fretboardString = JSONObject.quote(soundFontText(
        "tab_fretboard_string", currentLang, "Dây", "String"
    ))
    val fretboardFret = JSONObject.quote(soundFontText(
        "tab_fretboard_fret", currentLang, "Ngăn", "Fret"
    ))
    val fretboardChord = JSONObject.quote(soundFontText(
        "tab_fretboard_chord", currentLang, "Hợp âm", "Chord"
    ))
    val fretboardNote = JSONObject.quote(soundFontText(
        "tab_fretboard_note", currentLang, "Nốt", "Note"
    ))
    val fretboardNotes = JSONObject.quote(soundFontText(
        "tab_fretboard_notes", currentLang, "nốt", "notes"
    ))

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <script src="https://cdn.jsdelivr.net/npm/@coderline/alphatab@1.8.4/dist/alphaTab.js"></script>
            <style>
                html, body {
                    margin: 0; padding: 0;
                    width: 100%; height: 100%;
                    min-width: 100%; min-height: 100%;
                    background: transparent;
                    overflow: hidden;
                }

                body { position: relative; }

                .wrapper {
                    position: fixed;
                    top: 0; left: 0;
                    width: 100vw; height: 100vh;
                    min-height: 100vh;
                    box-sizing: border-box;
                    overflow-y: auto;
                    overflow-x: hidden;
                    -webkit-overflow-scrolling: touch;
                }

                /* True screen-sized playhead, NOT a pseudo-element clipped by AlphaTab.
                   It is placed above the scrolling score and below the fretboard panel. */
                #tt-playhead-overlay {
                    display: none;
                    position: fixed;
                    z-index: 2147482000;
                    box-sizing: border-box;
                    pointer-events: none;
                    user-select: none;
                    -webkit-user-select: none;
                    margin: 0;
                    padding: 0;
                    transform: translateX(-50%);
                    border-radius: 3px;
                    background: #DC2626;
                }
                #tt-playhead-overlay-cap {
                    position: absolute;
                    box-sizing: border-box;
                    left: 50%; top: -5px;
                    height: 8px;
                    transform: translateX(-50%);
                    border-radius: 5px;
                    background: inherit;
                }
                body.pdf-export-mode #tt-playhead-overlay {
                    display: none !important;
                }
                @media print {
                    #tt-playhead-overlay { display: none !important; }
                }

                #alphatab-container {
                    position: relative;
                    width: 100%;
                    box-sizing: border-box;
                    margin-top: -10px;
                    padding-bottom: 60vh;
                    filter: none !important;
                }

                /*
                 * Chế độ S+T+F dùng bố cục chia đôi ổn định:
                 *   khuông/TAB ở trên -> cần đàn ngay bên dưới -> nút Play ở đáy.
                 * Bảng cần đàn không đè lên bản nhạc và không thay đổi vị trí theo nốt.
                 */
                #tt-fretboard-panel {
                    display: none;
                    position: absolute;
                    top: 0;
                    left: 0;
                    right: auto;
                    bottom: auto;
                    z-index: 2147483000;
                    width: 100%;
                    height: var(--tt-fretboard-height, 260px);
                    box-sizing: border-box;
                    flex-direction: column;
                    padding: 0 8px;
                    overflow: hidden;
                    background: var(--tt-panel-bg, #f4f6fb);
                    color: var(--tt-panel-text, #101828);
                    border-top: 2px solid var(--tt-cursor-color, #8b5cf6);
                    box-shadow: 0 -3px 16px rgba(16, 24, 40, .18);
                    font-family: Arial, sans-serif;
                    opacity: 0;
                    visibility: hidden;
                    pointer-events: none;
                    transform: translateZ(0);
                    -webkit-transform: translateZ(0);
                    isolation: isolate;
                }
                body.tt-fretboard-open #tt-fretboard-panel {
                    display: flex !important;
                    opacity: 1 !important;
                    visibility: visible !important;
                    pointer-events: auto;
                }
                body.tt-fretboard-open #scroll-wrapper {
                    border-bottom: 1px solid rgba(139, 92, 246, .34);
                }
                body.tt-fretboard-open #alphatab-container {
                    min-height: 100% !important;
                    /* Đủ khoảng trống để hệ khuông cuối vẫn cuộn lên trọn vẹn,
                       không bị mép trên của cần đàn che mất. */
                    padding-bottom: var(--tt-score-bottom-reserve, 300px) !important;
                }
                .tt-fretboard-header {
                    height: 34px;
                    min-height: 34px;
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    gap: 6px;
                    padding: 2px 1px 0;
                }
                .tt-fretboard-header-main {
                    min-width: 0;
                    flex: 1;
                    display: flex;
                    align-items: center;
                    overflow: hidden;
                }
                .tt-fretboard-header-actions {
                    flex-shrink: 0;
                    display: flex;
                    align-items: center;
                    gap: 6px;
                }
                #tt-fretboard-title {
                    flex-shrink: 0;
                    font-size: 13px;
                    line-height: 16px;
                    font-weight: 800;
                    color: #101828;
                }
                #tt-fretboard-track {
                    margin-left: 6px;
                    font-size: 9.5px;
                    line-height: 13px;
                    font-weight: 600;
                    color: #667085;
                    max-width: 45vw;
                    display: inline-block;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                    vertical-align: middle;
                }
                /* Tên hợp âm được vẽ trực tiếp trong SVG, ngay phía trên
                   cụm vị trí bấm. Badge ở header chỉ được giữ lại để tương thích DOM. */
                #tt-fretboard-chord,
                #tt-fretboard-chord.is-visible {
                    display: none !important;
                }
                #tt-fretboard-info {
                    width: 27px;
                    min-width: 27px;
                    height: 27px;
                    min-height: 27px;
                    padding: 0;
                    border: 1.5px solid #7c3aed;
                    background: rgba(124,58,237,.08);
                    color: #6d28d9;
                    border-radius: 50%;
                    font-family: Georgia, serif;
                    font-size: 17px;
                    line-height: 23px;
                    font-weight: 800;
                    cursor: pointer;
                }
                body.tt-tab-dark #tt-fretboard-info {
                    background: rgba(167,139,250,.12);
                    border-color: #a78bfa;
                    color: #ddd6fe;
                }
                #tt-fretboard-close {
                    width: 27px;
                    min-width: 27px;
                    height: 27px;
                    min-height: 27px;
                    padding: 0;
                    border: 1px solid #cbd5e1;
                    background: #ffffff;
                    color: #344054;
                    border-radius: 50%;
                    font-size: 15px;
                    line-height: 25px;
                    font-weight: 700;
                }
                #tt-fretboard-stage {
                    flex: 1;
                    min-height: 112px;
                    width: 100%;
                    border-radius: 11px;
                    overflow: hidden;
                    background: var(--tt-fretboard-color, #f8fafc);
                    box-shadow: inset 0 0 0 1px var(--tt-board-border, #cbd5e1), 0 2px 10px rgba(16,24,40,.12);
                }
                #tt-fretboard-svg {
                    display: block;
                    width: 100%;
                    height: 100%;
                }
                #tt-fretboard-status {
                    flex-shrink: 0;
                    height: 23px;
                    line-height: 23px;
                    text-align: center;
                    font-size: 10.5px;
                    font-weight: 800;
                    color: #344054;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                }
                /* Vùng này dành đúng chỗ cho thanh Play/Pause Compose cố định ở đáy. */
                #tt-fretboard-controls-space {
                    flex-shrink: 0;
                    height: var(--tt-fretboard-controls-height, 66px);
                }
                body.tt-tab-dark #tt-fretboard-panel {
                    background: var(--tt-panel-bg, #0f131c);
                    color: var(--tt-panel-text, #f9fafb);
                    border-color: var(--tt-cursor-color, #8b5cf6);
                    box-shadow: 0 -3px 16px rgba(0, 0, 0, .58);
                }
                body.tt-tab-dark #tt-fretboard-stage {
                    background: var(--tt-fretboard-color, #171b24);
                    box-shadow: inset 0 0 0 1px var(--tt-board-border, #344054), 0 2px 10px rgba(0,0,0,.38);
                }
                body.tt-tab-dark #tt-fretboard-title {
                    color: #ffffff;
                }
                body.tt-tab-dark #tt-fretboard-track {
                    color: #98a2b3;
                }
                body.tt-tab-dark #tt-fretboard-status {
                    color: #f2f4f7;
                }
                body.tt-tab-dark #tt-fretboard-close {
                    background: #252b37;
                    color: #f9fafb;
                    border-color: #475467;
                }
                body.pdf-export-mode #tt-fretboard-panel { display: none !important; }
                body.tt-fretboard-side #tt-fretboard-panel {
                    border-top: none;
                    border-left: 2px solid #8b5cf6;
                    border-radius: 14px 0 0 14px;
                }
                body.tt-fretboard-side #tt-fretboard-track { max-width: 12vw; }
                body.tt-fretboard-side #tt-fretboard-stage { min-height: 160px; }

                /* Ban TAB ngan van cho phep cuon xuong sau bai de xem banner.
                   Khong ap dung khi xuat PDF de tranh trang giay trang. */
                body:not(.pdf-export-mode) #alphatab-container {
                    min-height: calc(100vh + 150px);
                }

                /* Không tạo một GPU layer khổng lồ cho toàn bộ bài ở chế độ tối. */
                body.tt-tab-dark:not(.pdf-export-mode) #alphatab-container svg:not(.tunertools-note-name-overlay) {
                    /* Màu khuông/TAB được cấu hình bằng RenderingResources của alphaTab. */
                    filter: none !important;
                }

                body.tt-tab-dark:not(.pdf-export-mode) .tunertools-note-name {
                    fill: #b8c2cd;
                    stroke: rgba(0, 0, 0, 0.92);
                }

                body.tt-tab-dark:not(.pdf-export-mode) .tunertools-note-name.is-active {
                    fill: #d0b9ff;
                }

                body.pdf-export-mode #alphatab-container svg {
                    filter: none !important;
                }

                /*
                 * Một số file tải từ kho SQLite không có metadata title bên trong GP.
                 * Khối này chỉ xuất hiện khi alphaTab không tự vẽ được tiêu đề. Nó vẫn
                 * nằm trong vùng bản nhạc, ngay trên khuông đầu tiên, không nằm ở toolbar.
                 */
                #tunertools-score-title-fallback {
                    display: none;
                    width: 100%;
                    box-sizing: border-box;
                    margin: 0;
                    padding: 4px 14px 3px;
                    overflow: hidden;
                    white-space: nowrap;
                    text-align: center;
                    text-overflow: clip;
                    font-family: Arial, sans-serif;
                    font-weight: 700;
                    line-height: 1.15;
                    color: #202124;
                    transform-origin: center center;
                }

                body.pdf-export-mode #tunertools-score-title-fallback {
                    color: #000000 !important;
                    width: 190mm !important;
                    max-width: 190mm !important;
                }

                .tunertools-note-name-overlay {
                    position: absolute;
                    left: 0;
                    top: 0;
                    overflow: visible;
                    pointer-events: none;
                    z-index: 4;
                }

                .tunertools-note-name {
                    font-family: Arial, sans-serif;
                    font-size: 8.5px;
                    font-weight: 600;
                    text-anchor: middle;
                    fill: #5f6368;
                    paint-order: stroke fill;
                    stroke: rgba(255, 255, 255, 0.90);
                    stroke-width: 1.1px;
                    stroke-linejoin: round;
                }

                .tunertools-note-name.is-active {
                    fill: #5e35b1;
                    font-weight: 700;
                }

                body.pdf-export-mode .tunertools-note-name {
                    fill: #424242;
                    stroke: #ffffff;
                }

                /* Bố cục tạm thời dùng riêng cho WebView PrintDocumentAdapter. */
                html.pdf-export-mode,
                body.pdf-export-mode {
                    width: 190mm !important;
                    min-width: 190mm !important;
                    height: auto !important;
                    min-height: 0 !important;
                    margin: 0 !important;
                    padding: 0 !important;
                    overflow: visible !important;
                    background: #ffffff !important;
                    color-scheme: light !important;
                }

                body.pdf-export-mode .wrapper {
                    position: static !important;
                    width: 190mm !important;
                    min-width: 190mm !important;
                    height: auto !important;
                    min-height: 0 !important;
                    margin: 0 !important;
                    padding: 0 !important;
                    overflow: visible !important;
                    background: #ffffff !important;
                }

                body.pdf-export-mode #alphatab-container {
                    width: 190mm !important;
                    min-width: 190mm !important;
                    margin: 0 !important;
                    padding: 0 !important;
                    filter: none !important;
                    background: #ffffff !important;
                }

                body.pdf-export-mode .at-cursor-bar,
                body.pdf-export-mode .at-cursor-beat {
                    display: none !important;
                }

                @media print {
                    html, body,
                    .wrapper,
                    #alphatab-container {
                        overflow: visible !important;
                        background: #ffffff !important;
                    }

                    #alphatab-container {
                        filter: none !important;
                        padding-bottom: 0 !important;
                    }

                    #alphatab-container svg {
                        filter: none !important;
                    }

                    .at-cursor-bar,
                    .at-cursor-beat {
                        display: none !important;
                    }
                }
            </style>
            <!-- Live colors/thickness go AFTER the base CSS to win equal-specificity rules. -->
            <style id="dynamic-theme-style"></style>
        </head>
        <body>
            <div class="wrapper" id="scroll-wrapper">
                <div id="tunertools-score-title-fallback" aria-hidden="true"></div>
                <div id="alphatab-container"></div>
            </div>
            <!-- Fixed outside alphaTab's own cursor/score layers so it cannot be
                 clipped by a 1px native cursor or a transformed score SVG. -->
            <div id="tt-playhead-overlay" aria-hidden="true">
                <div id="tt-playhead-overlay-cap"></div>
            </div>
            <section id="tt-fretboard-panel" aria-live="off">
                <div class="tt-fretboard-header">
                    <div class="tt-fretboard-header-main">
                        <span id="tt-fretboard-title"></span><span id="tt-fretboard-track"></span>
                    </div>
                    <div class="tt-fretboard-header-actions">
                        <span id="tt-fretboard-chord" aria-live="polite"></span>
                        <button type="button" id="tt-fretboard-info" onclick="openSongTechniqueInfo()" aria-label="Kỹ thuật và ký hiệu có trong bài" title="Kỹ thuật và ký hiệu có trong bài">i</button>
                        <button type="button" id="tt-fretboard-close" onclick="closeFretboardPanel()"></button>
                    </div>
                </div>
                <div id="tt-fretboard-stage">
                    <svg id="tt-fretboard-svg" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 390 158" role="img"></svg>
                </div>
                <div id="tt-fretboard-status"></div>
                <div id="tt-fretboard-controls-space" aria-hidden="true"></div>
            </section>
            <script>
                var currentThemeIsDark = $isDarkInitial;
                var currentVisualPreferences = {
                    cursorColor: $quotedInitialCursorColor,
                    activeRegionColor: $quotedInitialActiveRegionColor,
                    tabColor: $quotedInitialTabColor,
                    noteColor: $quotedInitialNoteColor,
                    fretboardColor: $quotedInitialFretboardColor,
                    panelColor: $quotedInitialPanelColor,
                    cursorWidth: $initialCursorWidth,
                    regionOpacity: $initialRegionOpacity
                };
                var visualResourceRenderTimer = null;
                var isPdfExportMode = false;
                var pdfExportWaitingForRender = false;
                var pdfExportOriginalState = null;
                var pdfExportReadyTimer = null;
                var pdfExportFallbackTimer = null;
                var alphaTabInitRetryTimer = null;
                var lastAndroidViewportWidth = 0;
                var lastAndroidViewportHeight = 0;
                var lastSyncedCssViewportWidth = 0;
                var fretboardEnabled = false;
                // Zoom phan tram cua toan bo khuong nhac + TAB (khong CSS scale).
                // -1: chua tuy chinh: S+T+F = 82%, che do khac = 100%.
                var userScoreZoomPercent = $initialScoreZoomPercent;
                var scoreZoomRenderTimer = 0;
                var scoreZoomScrollAnchor = null;
                function scoreZoomScaleForMode(fretboardMode) {
                    if (userScoreZoomPercent >= 0) {
                        return Math.max(.5, Math.min(1.5, userScoreZoomPercent / 100));
                    }
                    return fretboardMode ? .82 : 1.0;
                }
                function setScoreZoomPercent(percent) {
                    var parsed = Number(percent);
                    if (!Number.isFinite(parsed)) return false;
                    var next = parsed < 0 ? -1
                        : Math.max(50, Math.min(150, Math.round(parsed)));
                    userScoreZoomPercent = next;
                    if (!api || !api.settings || !api.settings.display ||
                        !api.score || isPdfExportMode) return true;
                    clearTimeout(scoreZoomRenderTimer);
                    var targetScale = scoreZoomScaleForMode(fretboardEnabled);
                    if (Math.abs(Number(api.settings.display.scale) - targetScale) < .0005) {
                        return true;
                    }
                    // Nhom thao tac slider thanh 1 lan render de khong gian doan nhac.
                    scoreZoomRenderTimer = setTimeout(function() {
                        if (!api || !api.score || isPdfExportMode) return;
                        var wrapper = document.getElementById('scroll-wrapper');
                        var nativeCursor = document.querySelector('.at-cursor-beat');
                        var cursorTop = null;
                        if (wrapper && nativeCursor) {
                            var rect = nativeCursor.getBoundingClientRect();
                            var containerRect = wrapper.getBoundingClientRect();
                            var relativeTop = rect.top - containerRect.top;
                            if (relativeTop >= 0 && relativeTop <= wrapper.clientHeight) {
                                cursorTop = relativeTop;
                            }
                        }
                        scoreZoomScrollAnchor = wrapper ? {
                            scrollRatio: wrapper.scrollTop /
                                Math.max(1, wrapper.scrollHeight - wrapper.clientHeight),
                            cursorTop: cursorTop
                        } : null;
                        try {
                            api.settings.display.scale = scoreZoomScaleForMode(fretboardEnabled);
                            api.updateSettings();
                            api.render();
                            scheduleVisiblePlayheadThickness();
                        } catch (error) {
                            scoreZoomScrollAnchor = null;
                            console.warn('Unable to zoom score notation', error);
                        }
                    }, 120);
                    return true;
                }
                // Pinch 2 ngon CHI tren khuong nhac/TAB. 1 ngon van cuon binh thuong.
                // Trong khi pinch chi preview so %, thuc hien render sau khi tha tay
                // de DOM AlphaTab khong bi thay giua cac su kien touch cua Android WebView.
                var scorePinch = {
                    active: false, startDistance: 0, startPercent: 100, previewPercent: 100
                };
                function scorePinchDistance(touches) {
                    if (!touches || touches.length < 2) return 0;
                    var dx = touches[0].clientX - touches[1].clientX;
                    var dy = touches[0].clientY - touches[1].clientY;
                    return Math.sqrt(dx * dx + dy * dy);
                }
                function reportScorePinch(phase, percent) {
                    try {
                        if (window.AndroidInterface &&
                            typeof window.AndroidInterface.onScoreZoomGesture === 'function') {
                            window.AndroidInterface.onScoreZoomGesture(phase, percent);
                        }
                    } catch (ignored) {}
                }
                function startScorePinch(event) {
                    if (isPdfExportMode || isScoreLoading || !api || !api.score ||
                        !event.touches || event.touches.length < 2) return;
                    var distance = scorePinchDistance(event.touches);
                    if (distance < 12) return;
                    if (!scorePinch.active) {
                        scorePinch.active = true;
                        scorePinch.startDistance = distance;
                        scorePinch.startPercent = Math.round(
                            scoreZoomScaleForMode(fretboardEnabled) * 100
                        );
                        scorePinch.previewPercent = scorePinch.startPercent;
                        clearTimeout(scrollTimeout);
                        isUserScrolling = false;
                        reportScorePinch('begin', scorePinch.previewPercent);
                    }
                    if (event.cancelable) event.preventDefault();
                }
                function moveScorePinch(event) {
                    if (!event.touches || event.touches.length < 2) return;
                    if (!scorePinch.active) startScorePinch(event);
                    if (!scorePinch.active) return;
                    if (event.cancelable) event.preventDefault();
                    var distance = scorePinchDistance(event.touches);
                    var zoom = Math.max(50, Math.min(150,
                        Math.round((scorePinch.startPercent *
                            distance / scorePinch.startDistance) / 5) * 5
                    ));
                    if (zoom !== scorePinch.previewPercent) {
                        scorePinch.previewPercent = zoom;
                        reportScorePinch('change', zoom);
                    }
                }
                function finishScorePinch(event) {
                    if (!scorePinch.active ||
                        (event.touches && event.touches.length >= 2)) return;
                    if (event.cancelable) event.preventDefault();
                    var zoom = scorePinch.previewPercent;
                    scorePinch.active = false;
                    isUserScrolling = false;
                    clearTimeout(scrollTimeout);
                    // Reflow layout 1 lan sau khi tha ngon, khong scale ca WebView.
                    setScoreZoomPercent(zoom);
                    reportScorePinch('end', zoom);
                }

                function restoreZoomScrollAnchor() {
                    var saved = scoreZoomScrollAnchor;
                    if (!saved || isPdfExportMode) return;
                    scoreZoomScrollAnchor = null;
                    requestAnimationFrame(function() {
                        var wrapper = document.getElementById('scroll-wrapper');
                        if (!wrapper) return;
                        var target = Math.max(0, wrapper.scrollHeight - wrapper.clientHeight) *
                            saved.scrollRatio;
                        var cursor = document.querySelector('.at-cursor-beat');
                        if (saved.cursorTop !== null && cursor) {
                            var newTop = cursor.getBoundingClientRect().top -
                                wrapper.getBoundingClientRect().top;
                            if (Number.isFinite(newTop)) {
                                target = wrapper.scrollTop + newTop - saved.cursorTop;
                            }
                        }
                        wrapper.scrollTop = Math.max(0, Math.min(
                            target, Math.max(0, wrapper.scrollHeight - wrapper.clientHeight)
                        ));
                        if (fretboardEnabled && lastKnownPlayerState === 1) {
                            keepScoreCursorNearFretboard(false);
                        }
                        scheduleVisiblePlayheadThickness();
                    });
                }
                var fretboardLatestBeat = null;
                // Giữ cửa sổ ngăn đàn ổn định. Chỉ chuyển theo nhóm 4 ngăn khi
                // bài thực sự đi ra ngoài phạm vi đang hiển thị.
                var fretboardWindowStart = 1;
                var fretboardLabels = {
                    title: $fretboardTitle,
                    idle: $fretboardIdle,
                    active: $fretboardActive,
                    preview: $fretboardPreview,
                    unsupported: $fretboardUnsupported,
                    rest: $fretboardRest,
                    close: $fretboardClose,
                    string: $fretboardString,
                    fret: $fretboardFret,
                    chord: $fretboardChord,
                    note: $fretboardNote,
                    notes: $fretboardNotes
                };

                function syncWebViewViewportBox(androidWidth, androidHeight) {
                    var root = document.documentElement;
                    var body = document.body;
                    var wrapper = document.getElementById('scroll-wrapper');
                    if (!root || !body || !wrapper || isPdfExportMode) {
                        return {
                            width: wrapper ? wrapper.clientWidth : 0,
                            height: wrapper ? wrapper.clientHeight : 0
                        };
                    }

                    var dpr = Math.max(1, Number(window.devicePixelRatio) || 1);
                    var physicalWidth = Math.max(0, Number(androidWidth) || 0);
                    var physicalHeight = Math.max(0, Number(androidHeight) || 0);
                    var cssWidth = Math.round(Number(window.innerWidth) || 0);
                    var cssHeight = Math.round(Number(window.innerHeight) || 0);

                    if (cssWidth <= 1 && physicalWidth > 0) cssWidth = Math.round(physicalWidth / dpr);
                    if (cssHeight <= 1 && physicalHeight > 0) cssHeight = Math.round(physicalHeight / dpr);
                    if (cssWidth <= 1) cssWidth = Math.round(Number(root.clientWidth) || 0);
                    if (cssHeight <= 1) cssHeight = Math.round(Number(root.clientHeight) || 0);
                    if (cssWidth <= 1 || cssHeight <= 1) {
                        return { width: wrapper.clientWidth || 0, height: wrapper.clientHeight || 0 };
                    }

                    root.style.width = cssWidth + 'px';
                    root.style.height = cssHeight + 'px';
                    root.style.minHeight = cssHeight + 'px';
                    body.style.width = cssWidth + 'px';
                    body.style.height = cssHeight + 'px';
                    body.style.minHeight = cssHeight + 'px';
                    wrapper.style.position = 'fixed';
                    wrapper.style.top = '0px';
                    wrapper.style.left = '0px';
                    wrapper.style.right = 'auto';
                    wrapper.style.bottom = 'auto';

                    var sideBySide = fretboardEnabled && cssWidth >= cssHeight * 1.28;
                    var boardWidth = sideBySide
                        ? Math.min(Math.round(cssWidth * .47), Math.max(230, cssWidth - 300))
                        : cssWidth;
                    var boardHeight = 0;
                    var controlsHeight = cssHeight < 560 ? 60 : 66;

                    if (fretboardEnabled && !sideBySide) {
                        // Portrait: gần một nửa chiều cao cho cần đàn. Vùng score vẫn đủ
                        // một hệ khuông/TAB, nhưng con trỏ nằm sát khu vực cần đàn.
                        var minimumScoreHeight = Math.min(
                            285,
                            Math.max(195, Math.round(cssHeight * .47))
                        );
                        var maximumBoardHeight = Math.max(0, cssHeight - minimumScoreHeight);
                        var desiredBoardHeight = Math.min(
                            330,
                            Math.max(248, Math.round(cssHeight * .47))
                        );
                        boardHeight = Math.max(0, Math.min(desiredBoardHeight, maximumBoardHeight));
                    }

                    var panel = document.getElementById('tt-fretboard-panel');
                    body.style.setProperty('--tt-fretboard-height', boardHeight + 'px');
                    body.style.setProperty('--tt-fretboard-width', boardWidth + 'px');
                    body.style.setProperty(
                        '--tt-fretboard-controls-height',
                        controlsHeight + 'px'
                    );
                    body.classList.toggle('tt-fretboard-open', !!fretboardEnabled);
                    body.classList.toggle('tt-fretboard-side', !!sideBySide);

                    var scoreWidth = sideBySide ? cssWidth - boardWidth : cssWidth;
                    var scoreHeight = sideBySide ? cssHeight : cssHeight - boardHeight;
                    var scoreBottomReserve = fretboardEnabled
                        ? Math.min(440, Math.max(230, Math.round(scoreHeight * .52)))
                        : 0;
                    body.style.setProperty(
                        '--tt-score-bottom-reserve',
                        scoreBottomReserve + 'px'
                    );
                    wrapper.style.width = Math.max(1, scoreWidth) + 'px';
                    wrapper.style.height = Math.max(1, scoreHeight) + 'px';
                    wrapper.style.minHeight = wrapper.style.height;

                    // Không dùng position:fixed cho panel vì một số Android WebView
                    // đặt sai tọa độ sau khi Compose đổi chiều cao. Luôn gắn panel vào
                    // đúng cạnh của viewport thật.
                    if (panel) {
                        if (!fretboardEnabled) {
                            panel.style.display = 'none';
                            panel.style.visibility = 'hidden';
                            panel.style.opacity = '0';
                            panel.style.pointerEvents = 'none';
                        } else if (sideBySide) {
                            panel.style.display = 'flex';
                            panel.style.visibility = 'visible';
                            panel.style.opacity = '1';
                            panel.style.pointerEvents = 'auto';
                            panel.style.left = (cssWidth - boardWidth) + 'px';
                            panel.style.top = '0px';
                            panel.style.width = boardWidth + 'px';
                            panel.style.height = cssHeight + 'px';
                        } else {
                            panel.style.display = 'flex';
                            panel.style.visibility = 'visible';
                            panel.style.opacity = '1';
                            panel.style.pointerEvents = 'auto';
                            panel.style.left = '0px';
                            panel.style.top = Math.max(0, scoreHeight) + 'px';
                            panel.style.width = cssWidth + 'px';
                            panel.style.height = boardHeight + 'px';
                        }
                    }

                    return {
                        width: wrapper.clientWidth || scoreWidth,
                        height: wrapper.clientHeight || scoreHeight
                    };
                }

                function onAndroidViewportChanged(width, height) {
                    width = Math.max(0, Math.round(Number(width) || 0));
                    height = Math.max(0, Math.round(Number(height) || 0));
                    if (width > 0) lastAndroidViewportWidth = width;
                    if (height > 0) lastAndroidViewportHeight = height;

                    var previousWidth = lastSyncedCssViewportWidth;
                    var viewport = syncWebViewViewportBox(width, height);
                    if (viewport.width <= 1 || viewport.height <= 1) return;
                    lastSyncedCssViewportWidth = viewport.width;

                    if (!isApiReady || !api) {
                        initAlphaTab(0);
                    } else if (
                        previousWidth > 0 &&
                        previousWidth !== viewport.width &&
                        api.score &&
                        !isPdfExportMode
                    ) {
                        tabBannerLastScoreSvg = null;
                        window.dispatchEvent(new Event('resize'));
                    }
                    // Re-evaluate when Compose inserts/removes the ad and changes
                    // WebView height. The two thresholds prevent visibility loops.
                    if (isApiReady) {
                        requestAnimationFrame(updateTabBannerAtScroll);
                        if (fretboardEnabled) requestAnimationFrame(scheduleFretboardRefresh);
                    }
                }

                // Vi tri banner dua vao khuong nhac SVG cuoi cung, KHONG dua vao
                // padding-bottom 60vh hoac min-height cua #alphatab-container.
                // Hai khoang trong CSS do khong phai la noi dung bai hat.
                var tabBannerVisible = false;
                var tabBannerManuallyScrolled = false;
                var tabBannerPreviousScrollTop = 0;
                var tabBannerLastScoreSvg = null;

                function notifyTabBannerVisibility(visible) {
                    visible = !!visible;
                    if (visible === tabBannerVisible) return;
                    tabBannerVisible = visible;
                    if (window.AndroidInterface &&
                        typeof window.AndroidInterface.onTabBottomBannerVisibilityChanged === 'function') {
                        window.AndroidInterface.onTabBottomBannerVisibilityChanged(visible);
                    }
                }

                function resetTabBannerForScore() {
                    tabBannerManuallyScrolled = false;
                    tabBannerPreviousScrollTop = 0;
                    tabBannerLastScoreSvg = null;
                    notifyTabBannerVisibility(false);
                }

                function findLastScoreSvg(score) {
                    if (tabBannerLastScoreSvg && score.contains(tabBannerLastScoreSvg)) {
                        return tabBannerLastScoreSvg;
                    }
                    // AlphaTab ve cac he khuong thanh SVG. Loai tru overlay ten not
                    // va cac SVG dieu khien nho khong phai khuong nhac.
                    var svgList = score.querySelectorAll('svg:not(.tunertools-note-name-overlay)');
                    for (var i = svgList.length - 1; i >= 0; i--) {
                        var rect = svgList[i].getBoundingClientRect();
                        if (rect.width >= 60 && rect.height >= 20) {
                            tabBannerLastScoreSvg = svgList[i];
                            return tabBannerLastScoreSvg;
                        }
                    }
                    return null;
                }

                function updateTabBannerAtScroll() {
                    var wrapper = document.getElementById('scroll-wrapper');
                    var score = document.getElementById('alphatab-container');
                    if (!wrapper || !score || !api || !api.score ||
                        isScoreLoading || isPdfExportMode || fretboardEnabled) {
                        notifyTabBannerVisibility(false);
                        return;
                    }

                    var lastSvg = findLastScoreSvg(score);
                    if (!lastSvg) {
                        notifyTabBannerVisibility(false);
                        return;
                    }

                    var viewportTop = wrapper.getBoundingClientRect().top;
                    var visibleBottom = wrapper.scrollTop + wrapper.clientHeight;
                    var songEnd = lastSvg.getBoundingClientRect().bottom -
                        viewportTop + wrapper.scrollTop;

                    // Khi phan cuoi khuong nhac da nam trong khung nhin, banner
                    // xuat hien. Khi banner lam WebView thap xuong, giu nguong
                    // an cach xa hon de tranh nhap nhay do resize.
                    var threshold = tabBannerVisible ? songEnd - 200 : songEnd - 18;
                    var maxScrollTop = Math.max(0, wrapper.scrollHeight - wrapper.clientHeight);
                    var minimumScrollTop = Math.min(56, Math.max(16, maxScrollTop - 16));
                    notifyTabBannerVisibility(
                        tabBannerManuallyScrolled &&
                        wrapper.scrollTop >= minimumScrollTop &&
                        visibleBottom >= threshold
                    );
                }

                function normalizeVisualHex(value, fallback) {
                    var text = String(value || '').trim();
                    if (/^#[0-9a-f]{6}$/i.test(text)) return text.toUpperCase();
                    if (/^#[0-9a-f]{3}$/i.test(text)) {
                        return ('#' + text.charAt(1) + text.charAt(1) +
                            text.charAt(2) + text.charAt(2) +
                            text.charAt(3) + text.charAt(3)).toUpperCase();
                    }
                    return fallback;
                }

                function visualRgb(hex) {
                    var normalized = normalizeVisualHex(hex, '#000000');
                    return {
                        r: parseInt(normalized.substring(1, 3), 16),
                        g: parseInt(normalized.substring(3, 5), 16),
                        b: parseInt(normalized.substring(5, 7), 16)
                    };
                }

                function visualRgba(hex, alpha) {
                    var rgb = visualRgb(hex);
                    return 'rgba(' + rgb.r + ',' + rgb.g + ',' + rgb.b + ',' +
                        Math.max(0, Math.min(1, Number(alpha))) + ')';
                }

                function visualLuminance(hex) {
                    var rgb = visualRgb(hex);
                    function channel(value) {
                        value = value / 255;
                        return value <= 0.03928
                            ? value / 12.92
                            : Math.pow((value + 0.055) / 1.055, 2.4);
                    }
                    return 0.2126 * channel(rgb.r) +
                        0.7152 * channel(rgb.g) +
                        0.0722 * channel(rgb.b);
                }

                function visualContrast(firstHex, secondHex) {
                    var first = visualLuminance(firstHex);
                    var second = visualLuminance(secondHex);
                    var lighter = Math.max(first, second);
                    var darker = Math.min(first, second);
                    return (lighter + 0.05) / (darker + 0.05);
                }

                function visualMix(firstHex, secondHex, ratio) {
                    var first = visualRgb(firstHex);
                    var second = visualRgb(secondHex);
                    var amount = Math.max(0, Math.min(1, Number(ratio)));
                    function toHex(value) {
                        var text = Math.round(value).toString(16).toUpperCase();
                        return text.length < 2 ? '0' + text : text;
                    }
                    return '#' +
                        toHex(first.r + (second.r - first.r) * amount) +
                        toHex(first.g + (second.g - first.g) * amount) +
                        toHex(first.b + (second.b - first.b) * amount);
                }

                function visualTextColor(backgroundHex) {
                    return visualLuminance(backgroundHex) > 0.179 ? '#111827' : '#FFFFFF';
                }

                function scheduleVisualResourceRender() {
                    clearTimeout(visualResourceRenderTimer);
                    visualResourceRenderTimer = setTimeout(function() {
                        if (!api || !api.score) return;
                        var wrapper = document.getElementById('scroll-wrapper');
                        var oldScrollTop = wrapper ? wrapper.scrollTop : 0;
                        var restoreManualPosition = lastKnownPlayerState !== 1;
                        try {
                            api.render();
                            if (restoreManualPosition && wrapper) {
                                setTimeout(function() {
                                    wrapper.scrollTop = oldScrollTop;
                                }, 90);
                            }
                        } catch (_) {}
                    }, 45);
                }

                var lastAlphaTabGlyphColor = null;
                function applyAlphaTabVisualResources(shouldRender) {
                    if (!api || !api.settings || !api.settings.display) return false;

                    var glyphColor = isPdfExportMode
                        ? '#111111'
                        : normalizeVisualHex(currentVisualPreferences.tabColor, '#111827');
                    // Changing fretboard/panel/cursor colors must not re-render the score.
                    var glyphPaletteKey = glyphColor + '|' +
                        (currentThemeIsDark ? 'dark' : 'light') + '|' +
                        (isPdfExportMode ? 'pdf' : 'screen');
                    if (glyphPaletteKey === lastAlphaTabGlyphColor) return true;
                    var secondaryGlyph = visualRgba(
                        glyphColor,
                        isPdfExportMode ? 0.58 : (currentThemeIsDark ? 0.62 : 0.56)
                    );
                    var staffLine = visualRgba(
                        glyphColor,
                        isPdfExportMode ? 0.48 : (currentThemeIsDark ? 0.46 : 0.40)
                    );

                    try {
                        if (typeof api.settings.fillFromJson === 'function') {
                            api.settings.fillFromJson({
                                display: {
                                    resources: {
                                        mainGlyphColor: glyphColor,
                                        secondaryGlyphColor: secondaryGlyph,
                                        staffLineColor: staffLine,
                                        barSeparatorColor: glyphColor,
                                        barNumberColor: glyphColor,
                                        scoreInfoColor: glyphColor
                                    }
                                }
                            });
                        } else {
                            return false;
                        }
                        api.updateSettings();
                        lastAlphaTabGlyphColor = glyphPaletteKey;
                        if (shouldRender) scheduleVisualResourceRender();
                        return true;
                    } catch (_) {
                        return false;
                    }
                }

                function applyVisualPreferences(
                    isDarkTheme,
                    cursorColor,
                    activeRegionColor,
                    tabColor,
                    noteColor,
                    fretboardColor,
                    panelColor,
                    cursorWidth,
                    regionOpacity
                ) {
                    currentThemeIsDark = !!isDarkTheme;
                    currentVisualPreferences.cursorColor = normalizeVisualHex(
                        cursorColor,
                        currentThemeIsDark ? '#C084FC' : '#7C3AED'
                    );
                    currentVisualPreferences.activeRegionColor = normalizeVisualHex(
                        activeRegionColor,
                        currentThemeIsDark ? '#7C3AED' : '#8B5CF6'
                    );
                    currentVisualPreferences.tabColor = normalizeVisualHex(
                        tabColor,
                        currentThemeIsDark ? '#F8FAFC' : '#111827'
                    );
                    currentVisualPreferences.noteColor = normalizeVisualHex(
                        noteColor,
                        currentThemeIsDark ? '#FFD43B' : '#FBBF24'
                    );
                    currentVisualPreferences.fretboardColor = normalizeVisualHex(
                        fretboardColor,
                        currentThemeIsDark ? '#171B24' : '#F8FAFC'
                    );
                    currentVisualPreferences.panelColor = normalizeVisualHex(
                        panelColor,
                        currentThemeIsDark ? '#0F131C' : '#F4F6FB'
                    );
                    var numericCursorWidth = Number(cursorWidth);
                    currentVisualPreferences.cursorWidth = isFinite(numericCursorWidth)
                        ? Math.max(0, Math.min(24, Math.round(numericCursorWidth))) : 6;
                    var numericRegionOpacity = Number(regionOpacity);
                    currentVisualPreferences.regionOpacity = isFinite(numericRegionOpacity)
                        ? Math.max(0, Math.min(.85, numericRegionOpacity)) : .28;

                    var container = document.getElementById('alphatab-container');
                    if (container) container.style.filter = 'none';
                    document.body.classList.toggle(
                        'tt-tab-dark',
                        currentThemeIsDark && !isPdfExportMode
                    );

                    var rootStyle = document.documentElement.style;
                    var panelBackground = currentVisualPreferences.panelColor;
                    // Panel and fretboard colors are independent of the light/dark app theme.
                    // Readability follows the ACTUAL selected background, even in light mode.
                    var panelIsLight = visualLuminance(panelBackground) > .179;
                    var panelText = panelIsLight ? '#101828' : '#F9FAFB';
                    var panelMuted = visualMix(panelBackground, panelText, .70);
                    var boardColor = currentVisualPreferences.fretboardColor;
                    var boardIsLight = visualLuminance(boardColor) > .179;
                    var boardBorder = visualMix(
                        boardColor,
                        boardIsLight ? '#475569' : '#CBD5E1',
                        boardIsLight ? 0.50 : 0.42
                    );
                    rootStyle.setProperty('--tt-panel-bg', panelBackground);
                    rootStyle.setProperty('--tt-panel-text', panelText);
                    rootStyle.setProperty('--tt-panel-muted', panelMuted);
                    rootStyle.setProperty('--tt-cursor-color', currentVisualPreferences.cursorColor);
                    rootStyle.setProperty('--tt-fretboard-color', boardColor);
                    rootStyle.setProperty('--tt-board-border', boardBorder);

                    var effectiveTabColor = isPdfExportMode
                        ? '#111111'
                        : currentVisualPreferences.tabColor;
                    var fallbackTitleElement = document.getElementById(
                        'tunertools-score-title-fallback'
                    );
                    if (fallbackTitleElement) {
                        fallbackTitleElement.style.color = effectiveTabColor;
                    }

                    // Beat cursor is an opaque narrow line; only the bar region uses alpha.
                    var regionAlpha = currentVisualPreferences.regionOpacity;
                    var regionInk = currentVisualPreferences.activeRegionColor;
                    var cursorInk = currentVisualPreferences.cursorColor;
                    var scoreBackgroundInk = currentThemeIsDark ? '#080A10' : '#F5F6FA';
                    var contrastEdgeInk = visualContrast(cursorInk, scoreBackgroundInk) < 2.8
                        ? (currentThemeIsDark ? '#FFFFFF' : '#121826')
                        : (currentThemeIsDark ? '#0B0F19' : '#FFFFFF');
                    // AlphaTab's moving cursor is a positioning anchor ONLY.
                    // Draw the actual visible cursor as an independent fixed div,
                    // so WebView/score scale, stacking and overflow cannot collapse it.
                    var cursorLineWidth = Math.round(currentVisualPreferences.cursorWidth);
                    var regionHidden = regionAlpha <= 0;
                    var regionEdgeInk = visualMix(
                        regionInk, currentThemeIsDark ? '#FFFFFF' : '#101828',
                        currentThemeIsDark ? .45 : .44
                    );
                    var lowTabContrast = visualContrast(
                        effectiveTabColor, scoreBackgroundInk) < 2.2;
                    var tabOutlineInk = currentThemeIsDark ? '#FFFFFF' : '#111827';
                    var style = document.getElementById('dynamic-theme-style');
                    if (style) {
                        style.innerHTML =
                            '.at-cursor-bar {' +
                                'background:' + (regionHidden ? 'transparent'
                                    : visualRgba(regionInk, regionAlpha)) + ' !important;' +
                                'border:0 !important;' +
                                'border-radius:9px !important;' +
                                'box-shadow:' + (regionHidden ? 'none'
                                    : 'inset 0 0 0 1.5px ' + visualRgba(regionEdgeInk, .44) +
                                      ',0 0 0 1px ' + visualRgba(regionEdgeInk, .22)) +
                                    ' !important;' +
                            '}' +
                            'html body .at-cursor-beat {' +
                                'opacity:0 !important;' +
                                'width:1px !important;min-width:1px !important;' +
                                'padding:0 !important;' +
                                'background:transparent !important;' +
                                'border:0 !important;' +
                                'box-shadow:none !important;' +
                                'pointer-events:none !important;' +
                            '}' +
                            // Prevent ghost lines from earlier CSS implementations.
                            'html body .at-cursor-beat::before,' +
                            'html body .at-cursor-beat::after {' +
                                'content:none !important;display:none !important;' +
                            '}' +
                            // Panel labels always contrast with chosen white/black background.
                            '#tt-fretboard-panel #tt-fretboard-title,' +
                            '#tt-fretboard-panel #tt-fretboard-status {' +
                                'color:var(--tt-panel-text) !important;' +
                            '}' +
                            '#tt-fretboard-panel #tt-fretboard-track {' +
                                'color:var(--tt-panel-muted) !important;' +
                            '}' +
                            '#tt-fretboard-panel #tt-fretboard-close {' +
                                'color:var(--tt-panel-text) !important;' +
                                'background:' + visualMix(panelBackground,
                                    panelIsLight ? '#FFFFFF' : '#000000', .15) + ' !important;' +
                                'border-color:' + panelMuted + ' !important;' +
                            '}' +
                            'body:not(.pdf-export-mode) .tunertools-note-name {' +
                                'fill:' + currentVisualPreferences.tabColor + ' !important;' +
                                'stroke:' + (currentThemeIsDark ? '#0B0F1A' : '#FFFFFF') +
                                    ' !important;' +
                            '}' +
                            'body:not(.pdf-export-mode) .tunertools-note-name.is-active {' +
                                'fill:' + cursorInk + ' !important;' +
                            '}' +
                            // If the user intentionally selects white TAB on a white score
                            // or black TAB on a black score, keep the chosen ink and add
                            // just a one-pixel contrast edge rather than changing its color.
                            'body:not(.pdf-export-mode) #alphatab-container svg {' +
                                'filter:' + (lowTabContrast
                                    ? 'drop-shadow(0 0 0.75px ' + tabOutlineInk + ')'
                                    : 'none') + ' !important;' +
                            '}';
                    }

                    if (fretboardEnabled) scheduleFretboardRefresh();
                    applyAlphaTabVisualResources(true);
                    scheduleVisiblePlayheadThickness();
                    return true;
                }

                // A REAL DIV above AlphaTab: width in actual WebView CSS pixels.
                // AlphaTab retains full control of the underlying beat position.
                // No ::before pseudo-elements, 1px visible lines, scale compensation,
                // or reliance on the cursor's clipping/stacking context.
                var ttPlayheadFrame = 0;
                var ttPlayheadOnceFrame = 0;
                var ttPlayheadLastLayout = '';
                function syncVisiblePlayheadThickness() {
                    var overlay = document.getElementById('tt-playhead-overlay');
                    var cap = document.getElementById('tt-playhead-overlay-cap');
                    if (!overlay || !cap) return;
                    var width = Math.max(0, Math.min(24,
                        Math.round(Number(currentVisualPreferences.cursorWidth) || 0)));
                    if (width === 0 || isPdfExportMode) {
                        overlay.style.display = 'none';
                        ttPlayheadLastLayout = '';
                        return;
                    }
                    var wrapper = document.getElementById('scroll-wrapper');
                    if (!wrapper) { overlay.style.display = 'none'; return; }
                    var wrapperRect = wrapper.getBoundingClientRect();
                    var bottomLimit = Math.min(wrapperRect.bottom, window.innerHeight);
                    var panel = document.getElementById('tt-fretboard-panel');
                    if (fretboardEnabled && panel) {
                        bottomLimit = Math.min(bottomLimit, panel.getBoundingClientRect().top);
                    }
                    var candidate = null;
                    var candidates = document.querySelectorAll('.at-cursor-beat');
                    for (var i = 0; i < candidates.length; i++) {
                        var r = candidates[i].getBoundingClientRect();
                        if (r.width <= 0 || r.height <= 0 || !isFinite(r.left) ||
                            !isFinite(r.top)) continue;
                        if (r.right < wrapperRect.left || r.left > wrapperRect.right ||
                            r.bottom <= wrapperRect.top || r.top >= bottomLimit) continue;
                        candidate = r;
                        break;
                    }
                    if (!candidate) {
                        overlay.style.display = 'none';
                        ttPlayheadLastLayout = '';
                        return;
                    }
                    var top = Math.max(0, wrapperRect.top, candidate.top);
                    var bottom = Math.min(window.innerHeight, bottomLimit, candidate.bottom);
                    // Some AlphaTab variants return only a very small beat rectangle:
                    // extend to the associated measure, preserving the same x-position.
                    if (bottom - top < 12) {
                        var barCursor = document.querySelector('.at-cursor-bar');
                        if (barCursor) {
                            var bar = barCursor.getBoundingClientRect();
                            top = Math.max(0, wrapperRect.top, Math.min(top, bar.top));
                            bottom = Math.min(window.innerHeight, bottomLimit,
                                Math.max(bottom, bar.bottom));
                        }
                    }
                    if (bottom - top < 5) {
                        overlay.style.display = 'none';
                        ttPlayheadLastLayout = '';
                        return;
                    }
                    var left = candidate.left + candidate.width / 2;
                    if (left < wrapperRect.left || left > wrapperRect.right) {
                        overlay.style.display = 'none';
                        ttPlayheadLastLayout = '';
                        return;
                    }
                    // A dark/bright contrasting edge keeps white, red or black visible
                    // without making the selected user's color translucent.
                    var color = currentVisualPreferences.cursorColor;
                    var scoreBg = currentThemeIsDark ? '#080A10' : '#F5F6FA';
                    var edge = visualContrast(color, scoreBg) < 2.8
                        ? (currentThemeIsDark ? '#FFFFFF' : '#101828')
                        : (currentThemeIsDark ? '#080A10' : '#FFFFFF');
                    var key = [Math.round(left * 10) / 10, Math.round(top * 10) / 10,
                        Math.round((bottom - top) * 10) / 10, width, color, edge].join('|');
                    if (key !== ttPlayheadLastLayout) {
                        ttPlayheadLastLayout = key;
                        overlay.style.left = left + 'px';
                        overlay.style.top = top + 'px';
                        overlay.style.width = width + 'px';
                        overlay.style.height = (bottom - top) + 'px';
                        overlay.style.backgroundColor = color;
                        overlay.style.border = width >= 3 ? '1px solid ' + edge : '0';
                        overlay.style.boxShadow = '0 0 0 1px ' + color +
                            ', 0 0 7px ' + visualRgba(color, .70);
                        cap.style.width = Math.max(12, width + 6) + 'px';
                        cap.style.border = '1px solid ' + edge;
                    }
                    overlay.style.display = 'block';
                }

                function ttTrackPlayheadFrame() {
                    ttPlayheadFrame = 0;
                    syncVisiblePlayheadThickness();
                    if (lastKnownPlayerState === 1 && !isPdfExportMode &&
                        typeof requestAnimationFrame === 'function') {
                        ttPlayheadFrame = requestAnimationFrame(ttTrackPlayheadFrame);
                    }
                }

                function scheduleVisiblePlayheadThickness() {
                    if (typeof requestAnimationFrame !== 'function') {
                        syncVisiblePlayheadThickness();
                        return;
                    }
                    if (!ttPlayheadOnceFrame) {
                        ttPlayheadOnceFrame = requestAnimationFrame(function() {
                            ttPlayheadOnceFrame = 0;
                            syncVisiblePlayheadThickness();
                        });
                    }
                    if (lastKnownPlayerState === 1 && !ttPlayheadFrame && !isPdfExportMode) {
                        ttPlayheadFrame = requestAnimationFrame(ttTrackPlayheadFrame);
                    }
                }

                function stopVisiblePlayheadTracking() {
                    if (ttPlayheadFrame && typeof cancelAnimationFrame === 'function') {
                        cancelAnimationFrame(ttPlayheadFrame);
                    }
                    ttPlayheadFrame = 0;
                    scheduleVisiblePlayheadThickness();
                }

                // Score auto-scroll and manual scroll move the native cursor's
                // screen coordinates, even if AlphaTab doesn't emit a new beat.
                var ttScoreScroller = document.getElementById('scroll-wrapper');
                if (ttScoreScroller) {
                    ttScoreScroller.addEventListener('scroll',
                        scheduleVisiblePlayheadThickness, { passive: true });
                }
                window.addEventListener('resize', scheduleVisiblePlayheadThickness);

                // Giữ API cũ để các luồng xuất PDF/khôi phục giao diện tiếp tục hoạt động.
                function toggleTheme(isDarkTheme) {
                    return applyVisualPreferences(
                        isDarkTheme,
                        currentVisualPreferences.cursorColor,
                        currentVisualPreferences.activeRegionColor,
                        currentVisualPreferences.tabColor,
                        currentVisualPreferences.noteColor,
                        currentVisualPreferences.fretboardColor,
                        currentVisualPreferences.panelColor,
                        currentVisualPreferences.cursorWidth,
                        currentVisualPreferences.regionOpacity
                    );
                }

                // Áp dụng đúng lựa chọn đã lưu ngay từ frame đầu tiên của WebView.
                applyVisualPreferences(
                    $isDarkInitial,
                    $quotedInitialCursorColor,
                    $quotedInitialActiveRegionColor,
                    $quotedInitialTabColor,
                    $quotedInitialNoteColor,
                    $quotedInitialFretboardColor,
                    $quotedInitialPanelColor,
                    $initialCursorWidth,
                    $initialRegionOpacity
                );
                syncWebViewViewportBox(0, 0);

                var api = null;
                // Âm click được tạo bởi alphaTab, dùng clock của chính bài đang phát.
                // 0: tắt, 0.75: bật. Không bật count-in trước bài.
                var metronomeEnabled = $initialMetronomeEnabled;
                function setMetronomeEnabled(enabled) {
                    metronomeEnabled = !!enabled;
                    if (api) {
                        api.metronomeVolume = metronomeEnabled ? 0.75 : 0;
                        api.countInVolume = 0;
                    }
                    return true;
                }
                var currentTracks = [];
                var selectedTrackForPlayback = 0;
                var isApiReady = false;
                var pendingSoundFontMode = $quotedInitialSoundFontMode;
                var pendingSoundFontTemporaryFallback = false;
                var activeSoundFontMode = null;
                var isSoundFontLoading = true;
                var soundFontStartFailedText = $quotedSoundFontStartFailed;
                var soundFontLoadFailedPrefix = $quotedSoundFontLoadFailedPrefix;
                var standardSoundFontUrl = $quotedStandardSoundFontUrl;
                var isScoreLoading = false;
                var lastKnownPlayerState = 0;
                var autoPlayAfterSoundFontReady = false;
                var autoFallbackInProgress = false;
                var soundFontCompatibilityCheckInProgress = false;
                var soundFontCompatibilityCheckToken = 0;
                var soundFontCompatibilityScoreGeneration = 0;
                var soundFontCompatibilityCache = {};
                var noteNamesVisible = $initialShowNoteNames;
                var noteNameOverlayId = 'tunertools-note-name-overlay';
                var lastPlayedNoteNameBeatId = '';
                var defaultSystemPaddingBottom = 10;
                var defaultLastSystemPaddingBottom = 5;
                var noteNameSystemPaddingBottom = 24;
                var noteNameLastSystemPaddingBottom = 24;

                // Bản sao BPM gốc để mọi lần chỉnh đều tính từ dữ liệu ban đầu,
                // tránh sai số cộng dồn khi người dùng đổi BPM nhiều lần.
                var originalTempoEntries = [];
                var originalBaseBpm = 120;
                var currentDisplayedBpm = 120;

                // Tiêu đề bài hát được đo theo chiều rộng thật của WebView.
                // Font được ghi lại bằng Settings.fillFromJson để alphaTab chuyển chuỗi CSS
                // thành đúng kiểu Font nội bộ trước khi render lại.
                var currentScoreTitle = 'Bài nhạc';
                var scoreTitleFallbackId = 'tunertools-score-title-fallback';
                // Tên lấy từ danh sách Android được ưu tiên nếu metadata Guitar Pro
                // trống, bị lỗi bảng mã hoặc bị mất dấu so với tên sạch trong danh sách.
                var pendingScoreTitle = '';
                var scrollToTopAfterNextRender = false;
                var currentTitleFontSize = 18;
                var lastResponsiveTitleFont = '';
                var titleResizeTimer = null;
                var titlePostRenderTimer = null;
                var isCorrectingRenderedTitle = false;

                function getTitleViewportMetrics() {
                    var wrapper = document.getElementById('scroll-wrapper');
                    var viewportWidth = wrapper && wrapper.clientWidth > 0
                        ? wrapper.clientWidth
                        : (window.innerWidth || document.documentElement.clientWidth || 320);

                    // Giữ một khoảng an toàn ở cả hai cạnh màn hình.
                    var horizontalSafeArea = viewportWidth <= 340 ? 24 : 32;
                    return {
                        wrapper: wrapper,
                        viewportWidth: viewportWidth,
                        maxTitleWidth: Math.max(88, viewportWidth - horizontalSafeArea)
                    };
                }

                function measureTitleWidth(title, fontSize) {
                    try {
                        var canvas = document.createElement('canvas');
                        var context = canvas.getContext('2d');
                        if (!context) return NaN;
                        context.font = 'bold ' + fontSize + 'px Arial, sans-serif';
                        return context.measureText(String(title || '')).width;
                    } catch (_) {
                        return NaN;
                    }
                }

                function calculateResponsiveTitleFont(title) {
                    var metrics = getTitleViewportMetrics();
                    var safeTitle = String(title || 'Bài nhạc').replace(/\s+/g, ' ').trim();

                    // Màn hình dọc dùng cỡ tối đa nhỏ hơn; màn hình ngang vẫn có thể hiển thị lớn.
                    var maxFontSize = metrics.viewportWidth >= 600
                        ? 26
                        : (metrics.viewportWidth >= 400 ? 22 : 20);
                    var minFontSize = metrics.viewportWidth <= 320 ? 7.5 : 8;
                    var targetWidth = metrics.maxTitleWidth * 0.94;

                    var measuredAtMax = measureTitleWidth(safeTitle, maxFontSize);
                    if (!Number.isFinite(measuredAtMax) || measuredAtMax <= 0) {
                        // Dự phòng cho WebView rất cũ không đo được canvas text.
                        var estimated = targetWidth / Math.max(1, safeTitle.length * 0.58);
                        return Math.max(
                            minFontSize,
                            Math.min(maxFontSize, Math.floor(estimated * 4) / 4)
                        );
                    }

                    if (measuredAtMax <= targetWidth) {
                        return maxFontSize;
                    }

                    // Tìm nhị phân theo bước 0.25px để tên dài co vừa chính xác hơn.
                    var low = minFontSize;
                    var high = maxFontSize;
                    var best = minFontSize;
                    for (var i = 0; i < 14; i++) {
                        var middle = (low + high) / 2;
                        var measured = measureTitleWidth(safeTitle, middle);
                        if (Number.isFinite(measured) && measured <= targetWidth) {
                            best = middle;
                            low = middle;
                        } else {
                            high = middle;
                        }
                    }

                    return Math.max(
                        minFontSize,
                        Math.min(maxFontSize, Math.floor(best * 4) / 4)
                    );
                }

                function createAlphaTabFont(cssFont) {
                    try {
                        if (
                            typeof alphaTab !== 'undefined' &&
                            alphaTab.model &&
                            alphaTab.model.Font &&
                            typeof alphaTab.model.Font.fromJson === 'function'
                        ) {
                            return alphaTab.model.Font.fromJson(cssFont);
                        }
                    } catch (_) {
                        // Settings.fillFromJson phía dưới là nhánh chính.
                    }
                    return null;
                }

                function ensureScoreTitleSettingVisible() {
                    if (!api || !api.settings || !api.settings.notation) return false;

                    try {
                        if (typeof api.settings.fillFromJson === 'function') {
                            api.settings.fillFromJson({
                                notation: {
                                    elements: { scoreTitle: true }
                                }
                            });
                            return true;
                        }
                    } catch (_) {
                        // Tiếp tục sang nhánh tương thích bên dưới.
                    }

                    try {
                        if (
                            api.settings.notation.elements &&
                            typeof api.settings.notation.elements.set === 'function' &&
                            typeof alphaTab !== 'undefined' &&
                            alphaTab.NotationElement
                        ) {
                            api.settings.notation.elements.set(
                                alphaTab.NotationElement.ScoreTitle,
                                true
                            );
                            return true;
                        }
                    } catch (_) {
                        // Không chặn việc render nếu bản alphaTab cũ không có Map.set.
                    }

                    return false;
                }

                function updateAlphaTabHeaderSettings(titleFont) {
                    if (!api || !api.settings || !api.settings.display) return false;

                    // Luôn bật lại ScoreTitle để việc đổi chế độ TAB/tiết tấu không làm mất
                    // tiêu đề ở phần đầu bản nhạc.
                    ensureScoreTitleSettingVisible();

                    // Đây là cách an toàn cho bản Web: chuỗi CSS được parser của alphaTab
                    // chuyển thành Font nội bộ, thay vì gán thẳng String vào thuộc tính Font.
                    try {
                        if (typeof api.settings.fillFromJson === 'function') {
                            api.settings.fillFromJson({
                                display: {
                                    padding: [12, 10, 12, 24],
                                    resources: {
                                        titleFont: titleFont,
                                        subTitleFont: '14px Arial, sans-serif',
                                        wordsFont: '12px Arial, sans-serif',
                                        copyrightFont: 'bold 10px Arial, sans-serif'
                                    }
                                },
                                notation: {
                                    elements: { scoreTitle: true }
                                }
                            });
                            return true;
                        }
                    } catch (_) {
                        // Tiếp tục sang nhánh tương thích bên dưới.
                    }

                    var resources = api.settings.display.resources;
                    if (!resources) return false;

                    var parsedTitleFont = createAlphaTabFont(titleFont);
                    var parsedSubTitleFont = createAlphaTabFont('14px Arial, sans-serif');
                    var parsedWordsFont = createAlphaTabFont('12px Arial, sans-serif');
                    var parsedCopyrightFont = createAlphaTabFont('bold 10px Arial, sans-serif');

                    resources.titleFont = parsedTitleFont || titleFont;
                    resources.subTitleFont = parsedSubTitleFont || resources.subTitleFont;
                    resources.wordsFont = parsedWordsFont || resources.wordsFont;
                    resources.copyrightFont = parsedCopyrightFont || resources.copyrightFont;
                    api.settings.display.padding = [12, 10, 12, 24];
                    return true;
                }

                // Tránh hiển thị lỗi giải mã ký tự như "V?ng Tr?ng Kh�c".
                // Dấu ? NẰM GIỮA chữ và ký tự thay thế U+FFFD là dấu hiệu hỏng dữ liệu,
                // khác với dấu hỏi thông thường ở cuối một câu/tên bài hát.
                function isUsableScoreTitle(raw) {
                    var title = String(raw || '').replace(/\s+/g, ' ').trim();
                    if (!title || title.indexOf('\uFFFD') >= 0) return false;
                    if (/[\u0000-\u0008\u000B\u000C\u000E-\u001F]/.test(title)) return false;
                    if (/[^\s?]\?[^\s?]/.test(title)) return false;
                    if (/(?:Ã[\u0080-\u00BF]|Â[\u0080-\u00BF]|á[º»])/.test(title)) return false;
                    if (/^\?+$/.test(title) || /^(?:không tên|bài nhạc|bản nhạc|untitled|n\/a)$/i.test(title)) return false;
                    return true;
                }

                function unaccentedTitleKey(raw) {
                    var title = String(raw || '');
                    if (typeof title.normalize === 'function') {
                        title = title.normalize('NFD').replace(/[\u0300-\u036f]/g, '');
                    }
                    return title.replace(/đ/g, 'd').replace(/Đ/g, 'D')
                        .toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim();
                }

                function chooseReadableScoreTitle(titleFromFile, fallbackTitle) {
                    var fileIsClean = isUsableScoreTitle(titleFromFile);
                    var fallbackIsClean = isUsableScoreTitle(fallbackTitle);
                    if (!fileIsClean) return fallbackIsClean ? fallbackTitle : 'Bản nhạc';
                    if (!fallbackIsClean) return titleFromFile;

                    // Ví dụ file chứa "Bien Nho" nhưng tên API là "Biển Nhớ":
                    // hai tên tương đương, giữ bản có dấu thay vì bản ASCII.
                    if (/^[\x00-\x7F]*$/.test(titleFromFile) &&
                        /[^\x00-\x7F]/.test(fallbackTitle) &&
                        unaccentedTitleKey(titleFromFile) === unaccentedTitleKey(fallbackTitle)) {
                        return fallbackTitle;
                    }
                    return titleFromFile;
                }

                function applyResponsiveSongHeader(score, renderAfterUpdate, forcedFontSize) {
                    if (!api || !api.settings || !score) return false;

                    var titleFromFile = String(score.title || '').replace(/\s+/g, ' ').trim();
                    var fallbackTitle = String(pendingScoreTitle || '').replace(/\s+/g, ' ').trim();
                    currentScoreTitle = chooseReadableScoreTitle(titleFromFile, fallbackTitle);

                    var scoreTitleChanged = false;

                    // Thay cả metadata hỏng (không chỉ trường hợp trống) trước khi
                    // alphaTab render để tiêu đề hiện đúng trong khuông và file PDF.
                    if (titleFromFile !== currentScoreTitle) {
                        try {
                            score.title = currentScoreTitle;
                            scoreTitleChanged = true;
                        } catch (_) {
                            // Nếu model không cho sửa, DOM fallback sẽ dùng currentScoreTitle.
                        }
                    }

                    ensureScoreTitleSettingVisible();
                    var calculatedSize = Number.isFinite(Number(forcedFontSize))
                        ? Number(forcedFontSize)
                        : calculateResponsiveTitleFont(currentScoreTitle);
                    var metrics = getTitleViewportMetrics();
                    var minimumSize = metrics.viewportWidth <= 320 ? 7.5 : 8;
                    var maximumSize = metrics.viewportWidth >= 600
                        ? 26
                        : (metrics.viewportWidth >= 400 ? 22 : 20);
                    var fontSize = Math.max(
                        minimumSize,
                        Math.min(maximumSize, Math.floor(calculatedSize * 4) / 4)
                    );
                    var titleFont = 'bold ' + fontSize + 'px Arial, sans-serif';
                    var fontChanged = lastResponsiveTitleFont !== titleFont;

                    if (fontChanged) {
                        if (!updateAlphaTabHeaderSettings(titleFont)) return false;
                        currentTitleFontSize = fontSize;
                        lastResponsiveTitleFont = titleFont;
                    } else {
                        // Dù font không đổi, title vừa chèn vẫn cần buộc alphaTab nhận settings mới.
                        currentTitleFontSize = fontSize;
                    }

                    if (fontChanged || scoreTitleChanged) {
                        api.updateSettings();
                    }

                    if (renderAfterUpdate && api.score) {
                        api.render();
                    }
                    return fontChanged || scoreTitleChanged;
                }

                function normalizeRenderedText(value) {
                    return String(value || '').replace(/\s+/g, ' ').trim();
                }

                function hideFallbackScoreTitle() {
                    var element = document.getElementById(scoreTitleFallbackId);
                    if (element) {
                        element.style.display = 'none';
                        element.style.transform = '';
                        element.textContent = '';
                    }

                    var container = document.getElementById('alphatab-container');
                    if (container && !isPdfExportMode) {
                        container.style.marginTop = '-10px';
                    }
                }

                function showFallbackScoreTitle() {
                    var title = normalizeRenderedText(currentScoreTitle);
                    if (!title || title === 'Bài nhạc') {
                        hideFallbackScoreTitle();
                        return;
                    }

                    var element = document.getElementById(scoreTitleFallbackId);
                    if (!element) return;

                    element.textContent = title;
                    element.style.display = 'block';
                    element.style.fontSize = currentTitleFontSize + 'px';
                    element.style.color = isPdfExportMode
                        ? '#202124'
                        : normalizeVisualHex(
                            currentVisualPreferences.tabColor,
                            currentThemeIsDark ? '#F8FAFC' : '#111827'
                        );
                    element.style.transform = '';

                    var container = document.getElementById('alphatab-container');
                    if (container) {
                        // Bỏ margin âm để khuông đầu tiên không đè lên tiêu đề dự phòng.
                        container.style.marginTop = isPdfExportMode ? '0px' : '-2px';
                    }

                    // Phòng tên quá dài trên màn hình nhỏ: co ngang nhưng không cắt chữ.
                    requestAnimationFrame(function() {
                        if (!element || element.style.display === 'none') return;
                        var availableWidth = Math.max(88, element.clientWidth - 28);
                        var contentWidth = element.scrollWidth;
                        if (contentWidth > availableWidth && contentWidth > 0) {
                            var scaleX = Math.max(0.55, Math.min(1, availableWidth / contentWidth));
                            element.style.transform = 'scaleX(' + scaleX + ')';
                        }
                    });
                }

                function findRenderedTitleElement() {
                    var expectedTitle = normalizeRenderedText(currentScoreTitle);
                    if (!expectedTitle) return null;

                    var textNodes = document.querySelectorAll('#alphatab-container svg text');
                    for (var i = 0; i < textNodes.length; i++) {
                        if (normalizeRenderedText(textNodes[i].textContent) === expectedTitle) {
                            return textNodes[i];
                        }
                    }
                    return null;
                }

                function verifyRenderedTitleFits() {
                    clearTimeout(titlePostRenderTimer);
                    titlePostRenderTimer = setTimeout(function() {
                        if (!api || !api.score || isCorrectingRenderedTitle) return;

                        var titleElement = findRenderedTitleElement();
                        if (!titleElement) {
                            showFallbackScoreTitle();
                            return;
                        }

                        hideFallbackScoreTitle();

                        var metrics = getTitleViewportMetrics();
                        var wrapper = metrics.wrapper;
                        if (!wrapper) return;

                        var titleRect = titleElement.getBoundingClientRect();
                        var wrapperRect = wrapper.getBoundingClientRect();
                        var leftLimit = wrapperRect.left + 8;
                        var rightLimit = wrapperRect.right - 8;
                        var isOverflowing =
                            titleRect.width > metrics.maxTitleWidth ||
                            titleRect.left < leftLimit ||
                            titleRect.right > rightLimit;

                        var minimumSize = metrics.viewportWidth <= 320 ? 7.5 : 8;
                        if (!isOverflowing) return;

                        // Với tên đặc biệt dài, khi đã chạm cỡ chữ tối thiểu thì nén ngang
                        // riêng dòng tiêu đề để vẫn hiển thị đủ, không cắt ký tự hay xuống dòng.
                        if (currentTitleFontSize <= minimumSize + 0.01) {
                            var finalScale = Math.min(
                                1,
                                (metrics.maxTitleWidth / Math.max(1, titleRect.width)) * 0.96
                            );
                            try {
                                var naturalSvgLength = titleElement.getComputedTextLength();
                                if (Number.isFinite(naturalSvgLength) && naturalSvgLength > 0) {
                                    titleElement.setAttribute(
                                        'textLength',
                                        String(naturalSvgLength * finalScale)
                                    );
                                    titleElement.setAttribute('lengthAdjust', 'spacingAndGlyphs');
                                } else {
                                    throw new Error('Không đọc được chiều dài SVG');
                                }
                            } catch (_) {
                                titleElement.style.transformBox = 'fill-box';
                                titleElement.style.transformOrigin = 'center center';
                                titleElement.style.transform = 'scaleX(' + finalScale + ')';
                            }
                            return;
                        }

                        // Đo trực tiếp phần SVG đã render để bù sai số font/devicePixelRatio.
                        var widthRatio = metrics.maxTitleWidth / Math.max(1, titleRect.width);
                        var nextSize = Math.floor(
                            Math.max(minimumSize, currentTitleFontSize * widthRatio * 0.92) * 4
                        ) / 4;
                        if (nextSize >= currentTitleFontSize) {
                            nextSize = Math.max(minimumSize, currentTitleFontSize - 0.5);
                        }
                        if (Math.abs(nextSize - currentTitleFontSize) < 0.24) return;

                        isCorrectingRenderedTitle = true;
                        applyResponsiveSongHeader(api.score, true, nextSize);
                        setTimeout(function() {
                            isCorrectingRenderedTitle = false;
                            verifyRenderedTitleFits();
                        }, 420);
                    }, 80);
                }

                var isUserScrolling = false;
                var scrollTimeout = null;
                
                function ensureInitialTempoAutomation(score) {
                    if (!score || !score.masterBars || score.masterBars.length === 0) return;

                    var firstMasterBar = score.masterBars[0];
                    if (!firstMasterBar.tempoAutomations) {
                        firstMasterBar.tempoAutomations = [];
                    }

                    if (firstMasterBar.tempoAutomations.length === 0) {
                        var initialValue = Number(score.tempo);
                        if (!Number.isFinite(initialValue) || initialValue <= 0) {
                            initialValue = 120;
                        }

                        var automation = null;
                        try {
                            if (
                                typeof alphaTab !== 'undefined' &&
                                alphaTab.model &&
                                alphaTab.model.Automation
                            ) {
                                automation = new alphaTab.model.Automation();
                            }
                        } catch (_) {
                            automation = null;
                        }

                        // Dạng object này vẫn chứa đầy đủ các thuộc tính mà renderer/MIDI cần.
                        if (!automation) {
                            automation = {};
                        }
                        automation.type = 0;
                        automation.isLinear = false;
                        automation.value = initialValue;
                        automation.ratioPosition = 0;
                        automation.text = '';
                        automation.isVisible = true;
                        firstMasterBar.tempoAutomations.push(automation);
                    } else {
                        // Yêu cầu của màn hình này là luôn nhìn thấy BPM đầu bài.
                        firstMasterBar.tempoAutomations[0].isVisible = true;
                    }
                }

                function captureOriginalTempos(score) {
                    originalTempoEntries = [];
                    ensureInitialTempoAutomation(score);

                    if (score && score.masterBars) {
                        for (var barIndex = 0; barIndex < score.masterBars.length; barIndex++) {
                            var masterBar = score.masterBars[barIndex];
                            var automations = masterBar.tempoAutomations || [];
                            for (var automationIndex = 0; automationIndex < automations.length; automationIndex++) {
                                var automation = automations[automationIndex];
                                var value = Number(automation.value);
                                if (Number.isFinite(value) && value > 0) {
                                    originalTempoEntries.push({
                                        automation: automation,
                                        value: value
                                    });
                                }
                            }
                        }
                    }

                    originalBaseBpm = Number(score && score.tempo);
                    if (!Number.isFinite(originalBaseBpm) || originalBaseBpm <= 0) {
                        originalBaseBpm = originalTempoEntries.length > 0
                            ? originalTempoEntries[0].value
                            : 120;
                    }

                    currentDisplayedBpm = Math.round(originalBaseBpm);
                    AndroidInterface.onTempoChanged(currentDisplayedBpm);
                }

                function setScoreBpm(targetBpm) {
                    try {
                        targetBpm = Math.round(Number(targetBpm));
                        if (!Number.isFinite(targetBpm) || targetBpm < 20 || targetBpm > 400) {
                            AndroidInterface.onTempoChangeFailed('BPM phải nằm trong khoảng 20 đến 400.');
                            return false;
                        }
                        if (!api || !api.score || originalTempoEntries.length === 0) {
                            AndroidInterface.onTempoChangeFailed('Bản nhạc chưa sẵn sàng để đổi BPM.');
                            return false;
                        }

                        var ratio = targetBpm / originalBaseBpm;
                        for (var i = 0; i < originalTempoEntries.length; i++) {
                            var entry = originalTempoEntries[i];
                            entry.automation.value = Math.max(
                                1,
                                Math.round(entry.value * ratio)
                            );
                        }

                        // Đảm bảo BPM đầu bài chính xác tuyệt đối với số người dùng nhập.
                        if (
                            api.score.masterBars &&
                            api.score.masterBars.length > 0 &&
                            api.score.masterBars[0].tempoAutomations &&
                            api.score.masterBars[0].tempoAutomations.length > 0
                        ) {
                            api.score.masterBars[0].tempoAutomations[0].value = targetBpm;
                            api.score.masterBars[0].tempoAutomations[0].isVisible = true;
                        }

                        // BPM đã được ghi thẳng vào data model nên giữ playbackSpeed = 100%.
                        // Re-render cập nhật con số tempo trên khuông nhạc;
                        // loadMidiForScore tạo lại MIDI để tốc độ nghe khớp với con số đó.
                        api.stop();
                        api.playbackSpeed = 1.0;
                        api.render();
                        if (typeof api.loadMidiForScore === 'function') {
                            api.loadMidiForScore();
                        }

                        currentDisplayedBpm = targetBpm;
                        AndroidInterface.onTempoChanged(targetBpm);
                        return true;
                    } catch (error) {
                        AndroidInterface.onTempoChangeFailed(
                            'Không thể đổi BPM: ' + (error && error.message ? error.message : error)
                        );
                        return false;
                    }
                }


                function clearNoteNameOverlay() {
                    var existing = document.getElementById(noteNameOverlayId);
                    if (existing && existing.parentNode) {
                        existing.parentNode.removeChild(existing);
                    }
                }

                function noteNameBeatId(beat) {
                    if (!beat) return '';

                    var beatId = Number(beat.id);
                    if (Number.isFinite(beatId)) {
                        return String(Math.round(beatId));
                    }

                    var displayStart = Number(beat.absoluteDisplayStart);
                    var beatIndex = Number(beat.index);
                    if (Number.isFinite(displayStart)) {
                        return 's' + String(Math.round(displayStart)) +
                            'i' + String(Number.isFinite(beatIndex) ? Math.round(beatIndex) : 0);
                    }
                    return '';
                }

                function selectedTrackForNoteNames() {
                    if (!currentTracks || currentTracks.length === 0) return null;
                    var safeIndex = Math.max(
                        0,
                        Math.min(selectedTrackForPlayback, currentTracks.length - 1)
                    );
                    return currentTracks[safeIndex] || null;
                }

                function trackOfBeatForNoteNames(beat) {
                    try {
                        return beat && beat.voice && beat.voice.bar && beat.voice.bar.staff
                            ? beat.voice.bar.staff.track
                            : null;
                    } catch (_) {
                        return null;
                    }
                }

                function beatBelongsToSelectedTrack(beat, selectedTrack) {
                    if (!beat || !selectedTrack) return true;
                    var beatTrack = trackOfBeatForNoteNames(beat);
                    if (!beatTrack) return true;
                    if (beatTrack === selectedTrack) return true;

                    var beatTrackIndex = Number(beatTrack.index);
                    var selectedTrackIndex = Number(selectedTrack.index);
                    return Number.isFinite(beatTrackIndex) &&
                        Number.isFinite(selectedTrackIndex) &&
                        beatTrackIndex === selectedTrackIndex;
                }

                function beatUsesFlatNoteNames(beat) {
                    try {
                        var bar = beat && beat.voice ? beat.voice.bar : null;
                        var masterBar = bar ? bar.masterBar : null;
                        var keySignature = Number(masterBar ? masterBar.keySignature : 0);
                        return Number.isFinite(keySignature) && keySignature < 0;
                    } catch (_) {
                        return false;
                    }
                }

                function notePitchForName(note) {
                    if (!note || note.isVisible === false || note.isDead === true || note.isPercussion === true) {
                        return null;
                    }

                    var pitch = NaN;
                    try {
                        if (typeof note.calculateRealValue === 'function') {
                            pitch = Number(note.calculateRealValue(true, true));
                        }
                    } catch (_) {
                        pitch = NaN;
                    }

                    if (!Number.isFinite(pitch)) pitch = Number(note.realValue);
                    if (!Number.isFinite(pitch)) pitch = Number(note.displayValue);
                    if (!Number.isFinite(pitch)) return null;
                    return Math.round(pitch);
                }

                function noteKeyForName(note, noteIndex, pitch) {
                    var noteId = Number(note ? note.id : NaN);
                    if (Number.isFinite(noteId)) {
                        return 'id' + String(Math.round(noteId));
                    }

                    var stringNumber = Number(note ? note.string : NaN);
                    return 's' + (Number.isFinite(stringNumber) ? String(Math.round(stringNumber)) : 'x') +
                        'p' + String(Math.round(pitch)) +
                        'i' + String(noteIndex);
                }

                function collectBeatNoteNameEntries(beat) {
                    if (!beat || beat.isRest === true || beat.isEmpty === true || !beat.notes) {
                        return [];
                    }

                    var useFlats = beatUsesFlatNoteNames(beat);
                    var sharpNames = ['C', 'C\u266f', 'D', 'D\u266f', 'E', 'F', 'F\u266f', 'G', 'G\u266f', 'A', 'A\u266f', 'B'];
                    var flatNames = ['C', 'D\u266d', 'D', 'E\u266d', 'E', 'F', 'G\u266d', 'G', 'A\u266d', 'A', 'B\u266d', 'B'];
                    var selectedNames = useFlats ? flatNames : sharpNames;
                    var entries = [];

                    // Giữ từng nốt vật lý của hợp âm, kể cả khi nhiều dây tạo ra
                    // cùng một tên nốt ở các quãng tám khác nhau. Ví dụ hợp âm 6 dây
                    // E-B-E-G#-B-E phải hiển thị đủ 6 tên, không rút gọn còn E-G#-B.
                    for (var noteIndex = 0; noteIndex < beat.notes.length; noteIndex++) {
                        var note = beat.notes[noteIndex];
                        var pitch = notePitchForName(note);
                        if (!Number.isFinite(pitch)) continue;

                        var pitchClass = ((pitch % 12) + 12) % 12;
                        var stringNumber = Number(note ? note.string : NaN);
                        entries.push({
                            pitch: pitch,
                            name: selectedNames[pitchClass],
                            stringNumber: Number.isFinite(stringNumber) ? stringNumber : NaN,
                            noteIndex: noteIndex,
                            key: noteKeyForName(note, noteIndex, pitch)
                        });
                    }

                    // Đọc từ âm thấp lên âm cao; nếu hai nốt đồng âm thì giữ thứ tự dây.
                    entries.sort(function(left, right) {
                        if (left.pitch !== right.pitch) return left.pitch - right.pitch;
                        if (
                            Number.isFinite(left.stringNumber) &&
                            Number.isFinite(right.stringNumber) &&
                            left.stringNumber !== right.stringNumber
                        ) {
                            return right.stringNumber - left.stringNumber;
                        }
                        return left.noteIndex - right.noteIndex;
                    });
                    return entries;
                }

                function updateActiveNoteNameBeat(beat) {
                    lastPlayedNoteNameBeatId = noteNameBeatId(beat);
                    var labels = document.querySelectorAll('.tunertools-note-name');
                    var searchToken = lastPlayedNoteNameBeatId
                        ? '|' + lastPlayedNoteNameBeatId + '|'
                        : '';

                    for (var labelIndex = 0; labelIndex < labels.length; labelIndex++) {
                        var label = labels[labelIndex];
                        var beatIds = label.getAttribute('data-beat-ids') || '';
                        var isActive = searchToken && beatIds.indexOf(searchToken) >= 0;
                        if (isActive) {
                            label.classList.add('is-active');
                        } else {
                            label.classList.remove('is-active');
                        }
                    }
                }

                function renderNoteNameOverlay() {
                    clearNoteNameOverlay();
                    if (!noteNamesVisible || !api || !api.score) return;

                    var lookup = api.boundsLookup ||
                        (api.renderer ? api.renderer.boundsLookup : null);
                    if (!lookup || !lookup.staffSystems || lookup.staffSystems.length === 0) return;

                    var container = document.getElementById('alphatab-container');
                    if (!container) return;

                    var selectedTrack = selectedTrackForNoteNames();
                    var svgNamespace = 'http://www.w3.org/2000/svg';
                    var overlay = document.createElementNS(svgNamespace, 'svg');
                    overlay.setAttribute('id', noteNameOverlayId);
                    overlay.setAttribute('class', 'tunertools-note-name-overlay');
                    overlay.setAttribute('aria-hidden', 'true');

                    var maximumRight = Math.max(container.clientWidth || 0, container.scrollWidth || 0);
                    var maximumBottom = Math.max(container.clientHeight || 0, container.scrollHeight || 0);
                    var labelsCreated = 0;

                    for (var systemIndex = 0; systemIndex < lookup.staffSystems.length; systemIndex++) {
                        var systemBounds = lookup.staffSystems[systemIndex];
                        if (!systemBounds) continue;

                        var visualBounds = systemBounds.visualBounds || systemBounds.realBounds;
                        var realBounds = systemBounds.realBounds || visualBounds;
                        if (!visualBounds || !realBounds) continue;

                        maximumRight = Math.max(
                            maximumRight,
                            Number(realBounds.x || 0) + Number(realBounds.w || 0)
                        );
                        maximumBottom = Math.max(
                            maximumBottom,
                            Number(realBounds.y || 0) + Number(realBounds.h || 0)
                        );

                        var visualBottom = Number(visualBounds.y || 0) + Number(visualBounds.h || 0);
                        var labelBaseline = visualBottom + 12;
                        maximumBottom = Math.max(maximumBottom, labelBaseline + 4);

                        var groupsByX = {};
                        // Cùng một beat có thể xuất hiện trong bounds của cả khuông nhạc
                        // và TAB. Chỉ lấy một lần để không nhân đôi tên nốt.
                        var processedBeatIds = {};
                        var masterBarBoundsList = systemBounds.bars || [];
                        for (var masterIndex = 0; masterIndex < masterBarBoundsList.length; masterIndex++) {
                            var masterBarBounds = masterBarBoundsList[masterIndex];
                            var barBoundsList = masterBarBounds && masterBarBounds.bars
                                ? masterBarBounds.bars
                                : [];

                            for (var barIndex = 0; barIndex < barBoundsList.length; barIndex++) {
                                var barBounds = barBoundsList[barIndex];
                                var beatBoundsList = barBounds && barBounds.beats
                                    ? barBounds.beats
                                    : [];

                                for (var beatIndex = 0; beatIndex < beatBoundsList.length; beatIndex++) {
                                    var beatBounds = beatBoundsList[beatIndex];
                                    var beat = beatBounds ? beatBounds.beat : null;
                                    if (!beatBelongsToSelectedTrack(beat, selectedTrack)) continue;

                                    var beatId = noteNameBeatId(beat);
                                    if (
                                        beatId &&
                                        Object.prototype.hasOwnProperty.call(processedBeatIds, beatId)
                                    ) {
                                        continue;
                                    }

                                    var entries = collectBeatNoteNameEntries(beat);
                                    if (entries.length === 0) continue;
                                    if (beatId) processedBeatIds[beatId] = true;

                                    var x = Number(beatBounds.onNotesX);
                                    if (!Number.isFinite(x)) {
                                        var beatVisualBounds = beatBounds.visualBounds || beatBounds.realBounds;
                                        if (!beatVisualBounds) continue;
                                        x = Number(beatVisualBounds.x || 0) +
                                            Number(beatVisualBounds.w || 0) / 2;
                                    }

                                    var roundedX = Math.round(x * 2) / 2;
                                    var groupKey = String(roundedX);
                                    var group = groupsByX[groupKey];
                                    if (!group) {
                                        group = {
                                            x: roundedX,
                                            entries: [],
                                            entryKeys: {},
                                            beatIds: {}
                                        };
                                        groupsByX[groupKey] = group;
                                    }

                                    for (var entryIndex = 0; entryIndex < entries.length; entryIndex++) {
                                        var entry = entries[entryIndex];
                                        var entryKey = (beatId || ('x' + groupKey)) + '|' + entry.key;
                                        if (Object.prototype.hasOwnProperty.call(group.entryKeys, entryKey)) {
                                            continue;
                                        }
                                        group.entryKeys[entryKey] = true;
                                        group.entries.push(entry);
                                    }

                                    if (beatId) group.beatIds[beatId] = true;
                                }
                            }
                        }

                        var groups = [];
                        for (var groupProperty in groupsByX) {
                            if (Object.prototype.hasOwnProperty.call(groupsByX, groupProperty)) {
                                groups.push(groupsByX[groupProperty]);
                            }
                        }
                        groups.sort(function(left, right) { return left.x - right.x; });

                        for (var groupIndex = 0; groupIndex < groups.length; groupIndex++) {
                            var currentGroup = groups[groupIndex];
                            var groupEntries = (currentGroup.entries || []).slice();
                            groupEntries.sort(function(left, right) {
                                if (left.pitch !== right.pitch) return left.pitch - right.pitch;
                                if (
                                    Number.isFinite(left.stringNumber) &&
                                    Number.isFinite(right.stringNumber) &&
                                    left.stringNumber !== right.stringNumber
                                ) {
                                    return right.stringNumber - left.stringNumber;
                                }
                                return left.noteIndex - right.noteIndex;
                            });

                            var nameParts = [];
                            for (var nameIndex = 0; nameIndex < groupEntries.length; nameIndex++) {
                                nameParts.push(groupEntries[nameIndex].name);
                            }
                            if (nameParts.length === 0) continue;

                            // Không loại tên trùng: mỗi dây/nốt của hợp âm được thể hiện riêng.
                            var labelText = nameParts.join('\u00b7');
                            var leftBoundary = groupIndex > 0
                                ? (groups[groupIndex - 1].x + currentGroup.x) / 2
                                : Number(realBounds.x || 0);
                            var rightBoundary = groupIndex < groups.length - 1
                                ? (currentGroup.x + groups[groupIndex + 1].x) / 2
                                : Number(realBounds.x || 0) + Number(realBounds.w || 0);
                            var availableWidth = Math.max(7, rightBoundary - leftBoundary - 2);
                            var baseFontSize = nameParts.length >= 5 ? 7.8 : 8.5;
                            var minimumFontSize = nameParts.length >= 5 ? 5.6 : 6.2;
                            var estimatedWidth = Math.max(1, labelText.length * baseFontSize * 0.57);
                            var fontSize = estimatedWidth > availableWidth
                                ? Math.max(minimumFontSize, baseFontSize * availableWidth / estimatedWidth)
                                : baseFontSize;

                            var textNode = document.createElementNS(svgNamespace, 'text');
                            textNode.setAttribute('class', 'tunertools-note-name');
                            textNode.setAttribute('x', String(currentGroup.x));
                            textNode.setAttribute('y', String(labelBaseline));
                            textNode.style.fontSize = String(fontSize) + 'px';
                            textNode.textContent = labelText;

                            var beatIdParts = [];
                            for (var beatIdKey in currentGroup.beatIds) {
                                if (Object.prototype.hasOwnProperty.call(currentGroup.beatIds, beatIdKey)) {
                                    beatIdParts.push(beatIdKey);
                                }
                            }
                            textNode.setAttribute(
                                'data-beat-ids',
                                '|' + beatIdParts.join('|') + '|'
                            );

                            var adjustedEstimatedWidth = Math.max(
                                1,
                                labelText.length * fontSize * 0.57
                            );
                            if (adjustedEstimatedWidth > availableWidth && availableWidth > 4) {
                                textNode.setAttribute('textLength', String(availableWidth));
                                textNode.setAttribute('lengthAdjust', 'spacingAndGlyphs');
                            }

                            overlay.appendChild(textNode);
                            labelsCreated++;
                        }
                    }

                    if (labelsCreated === 0) return;

                    var overlayWidth = Math.max(1, Math.ceil(maximumRight));
                    var overlayHeight = Math.max(1, Math.ceil(maximumBottom));
                    overlay.setAttribute('width', String(overlayWidth));
                    overlay.setAttribute('height', String(overlayHeight));
                    overlay.setAttribute('viewBox', '0 0 ' + overlayWidth + ' ' + overlayHeight);
                    overlay.style.width = String(overlayWidth) + 'px';
                    overlay.style.height = String(overlayHeight) + 'px';
                    container.appendChild(overlay);
                    updateActiveNoteNameBeat(
                        lastPlayedNoteNameBeatId ? { id: Number(lastPlayedNoteNameBeatId) } : null
                    );
                }

                function setNoteNamesVisible(visible) {
                    if (isPdfExportMode) return false;

                    var nextVisible = !!visible;
                    if (nextVisible === noteNamesVisible) {
                        if (nextVisible) renderNoteNameOverlay();
                        else clearNoteNameOverlay();
                        return true;
                    }

                    var previousVisible = noteNamesVisible;
                    noteNamesVisible = nextVisible;
                    if (!noteNamesVisible) {
                        clearNoteNameOverlay();
                        lastPlayedNoteNameBeatId = '';
                    }

                    if (!api || !api.settings || !api.settings.display) return true;

                    try {
                        var wrapper = document.getElementById('scroll-wrapper');
                        var previousScrollTop = wrapper ? wrapper.scrollTop : 0;
                        var targetSystemPadding = noteNamesVisible
                            ? noteNameSystemPaddingBottom
                            : defaultSystemPaddingBottom;
                        var targetLastSystemPadding = noteNamesVisible
                            ? noteNameLastSystemPaddingBottom
                            : defaultLastSystemPaddingBottom;

                        if (typeof api.settings.fillFromJson === 'function') {
                            api.settings.fillFromJson({
                                display: {
                                    systemPaddingBottom: targetSystemPadding,
                                    lastSystemPaddingBottom: targetLastSystemPadding
                                }
                            });
                        } else {
                            api.settings.display.systemPaddingBottom = targetSystemPadding;
                            api.settings.display.lastSystemPaddingBottom = targetLastSystemPadding;
                        }

                        api.updateSettings();
                        if (api.score) {
                            applySelectedTrackSolo(true);
                        }

                        setTimeout(function() {
                            var currentWrapper = document.getElementById('scroll-wrapper');
                            if (currentWrapper) {
                                var maxScroll = Math.max(
                                    0,
                                    currentWrapper.scrollHeight - currentWrapper.clientHeight
                                );
                                currentWrapper.scrollTop = Math.min(previousScrollTop, maxScroll);
                            }
                        }, 320);
                        return true;
                    } catch (error) {
                        noteNamesVisible = previousVisible;
                        console.warn('Could not change note-name display', error);
                        return false;
                    }
                }

                // Bản đồ cần đàn đọc số dây/ngăn và cao độ từ chính alphaTab Beat/Note.
                // Note.string = 1 là dây dưới cùng (E trầm của guitar 6 dây),
                // trong khi Staff.tuning[0] là dây trên cùng của TAB.
                function fretboardStaffForTrack() {
                    var track = selectedTrackForNoteNames();
                    if (!track || !track.staves) return null;
                    for (var i = 0; i < track.staves.length; i++) {
                        var staff = track.staves[i];
                        if (staff && staff.isStringed !== false && staff.tuning &&
                            staff.tuning.length >= 4 && staff.tuning.length <= 12) {
                            return staff;
                        }
                    }
                    return null;
                }

                function firstFrettedBeatForPreview() {
                    var staff = fretboardStaffForTrack();
                    if (!staff || !staff.bars) return null;
                    for (var b = 0; b < staff.bars.length; b++) {
                        var bar = staff.bars[b];
                        if (!bar || !bar.voices) continue;
                        for (var v = 0; v < bar.voices.length; v++) {
                            var voice = bar.voices[v];
                            if (!voice || !voice.beats) continue;
                            for (var k = 0; k < voice.beats.length; k++) {
                                var beat = voice.beats[k];
                                if (!beat || !beat.notes) continue;
                                for (var n = 0; n < beat.notes.length; n++) {
                                    if (Number(beat.notes[n].string) >= 1 &&
                                        Number(beat.notes[n].fret) >= 0) return beat;
                                }
                            }
                        }
                    }
                    return null;
                }

                function fretboardNoteLabel(pitch, usesFlats) {
                    var sharp = ['C', 'C♯', 'D', 'D♯', 'E', 'F', 'F♯', 'G', 'G♯', 'A', 'A♯', 'B'];
                    var flat = ['C', 'D♭', 'D', 'E♭', 'E', 'F', 'G♭', 'G', 'A♭', 'A', 'B♭', 'B'];
                    if (!Number.isFinite(pitch)) return '•';
                    return (usesFlats ? flat : sharp)[((Math.round(pitch) % 12) + 12) % 12];
                }

                function fretboardPitchClass(pitch) {
                    if (!Number.isFinite(pitch)) return null;
                    return ((Math.round(pitch) % 12) + 12) % 12;
                }

                function fretboardIsArpeggioBeat(beat) {
                    if (!beat) return false;
                    var raw = beat.brushType;
                    var numeric = Number(raw);
                    if (numeric === 3 || numeric === 4) return true;
                    var label = String(raw == null ? '' : raw).toLowerCase();
                    return label.indexOf('arpeggio') >= 0;
                }

                function fretboardExplicitChordName(beat) {
                    if (!beat) return '';
                    try {
                        if (beat.chord && beat.chord.name) {
                            return String(beat.chord.name).trim();
                        }
                    } catch (ignored) {}
                    var chordId = beat.chordId == null ? '' : String(beat.chordId).trim();
                    // chordId đôi khi là ID nội bộ. Chỉ dùng khi trông giống tên hợp âm.
                    return /^[A-Ga-g][#b♯♭]?/.test(chordId) ? chordId : '';
                }

                function fretboardChordRootPitchClass(chordName) {
                    var match = /^\s*([A-Ga-g])([#b♯♭]?)/.exec(String(chordName || ''));
                    if (!match) return null;
                    var natural = { C: 0, D: 2, E: 4, F: 5, G: 7, A: 9, B: 11 };
                    var pitchClass = natural[match[1].toUpperCase()];
                    if (match[2] === '#' || match[2] === '♯') pitchClass += 1;
                    if (match[2] === 'b' || match[2] === '♭') pitchClass -= 1;
                    return (pitchClass + 12) % 12;
                }

                function fretboardSamePitchClassSet(actual, expected) {
                    if (actual.length !== expected.length) return false;
                    for (var i = 0; i < actual.length; i++) {
                        if (actual[i] !== expected[i]) return false;
                    }
                    return true;
                }

                function fretboardInferChord(highlighted, usesFlats) {
                    if (!highlighted || highlighted.length < 3) return null;

                    var pitchClassMap = {};
                    var pitchClasses = [];
                    var bass = null;
                    for (var i = 0; i < highlighted.length; i++) {
                        var point = highlighted[i];
                        var pitchClass = point.pitchClass;
                        if (pitchClass == null) continue;
                        if (!Object.prototype.hasOwnProperty.call(pitchClassMap, pitchClass)) {
                            pitchClassMap[pitchClass] = true;
                            pitchClasses.push(pitchClass);
                        }
                        if (!bass || point.pitch < bass.pitch) bass = point;
                    }
                    pitchClasses.sort(function(a, b) { return a - b; });
                    if (pitchClasses.length < 3) return null;

                    // Mẫu chính xác để tránh đoán hợp âm từ hai nốt hoặc cụm nốt mơ hồ.
                    var patterns = [
                        { intervals: [0, 4, 7, 11], suffix: 'maj7' },
                        { intervals: [0, 4, 7, 10], suffix: '7' },
                        { intervals: [0, 3, 7, 10], suffix: 'm7' },
                        { intervals: [0, 3, 7, 11], suffix: 'mMaj7' },
                        { intervals: [0, 3, 6, 10], suffix: 'm7♭5' },
                        { intervals: [0, 3, 6, 9], suffix: 'dim7' },
                        { intervals: [0, 4, 7, 9], suffix: '6' },
                        { intervals: [0, 3, 7, 9], suffix: 'm6' },
                        { intervals: [0, 2, 4, 7], suffix: 'add9' },
                        { intervals: [0, 4, 7], suffix: '' },
                        { intervals: [0, 3, 7], suffix: 'm' },
                        { intervals: [0, 3, 6], suffix: 'dim' },
                        { intervals: [0, 4, 8], suffix: 'aug' },
                        { intervals: [0, 2, 7], suffix: 'sus2' },
                        { intervals: [0, 5, 7], suffix: 'sus4' }
                    ];

                    var bassPitchClass = bass ? bass.pitchClass : null;
                    var roots = [];
                    if (bassPitchClass != null) roots.push(bassPitchClass);
                    for (var rootCandidate = 0; rootCandidate < 12; rootCandidate++) {
                        if (rootCandidate !== bassPitchClass) roots.push(rootCandidate);
                    }

                    for (var r = 0; r < roots.length; r++) {
                        var root = roots[r];
                        var intervals = pitchClasses.map(function(value) {
                            return (value - root + 12) % 12;
                        }).sort(function(a, b) { return a - b; });

                        for (var p = 0; p < patterns.length; p++) {
                            if (!fretboardSamePitchClassSet(intervals, patterns[p].intervals)) continue;
                            var rootName = fretboardNoteLabel(root, usesFlats);
                            var chordName = rootName + patterns[p].suffix;
                            if (bassPitchClass != null && bassPitchClass !== root) {
                                chordName += '/' + fretboardNoteLabel(bassPitchClass, usesFlats);
                            }
                            return {
                                name: chordName,
                                rootPitchClass: root,
                                inferred: true
                            };
                        }
                    }
                    return null;
                }

                function fretboardChordForBeat(beat, highlighted, usesFlats, isPreview) {
                    // Nốt tỉa/sequential chỉ có một nốt tại beat hiện tại: không hiện hợp âm.
                    // Arpeggio được loại trừ ngay cả khi file lưu nhiều nốt trong cùng beat.
                    if (isPreview || !beat || !highlighted || highlighted.length < 2 ||
                        fretboardIsArpeggioBeat(beat)) return null;

                    var explicitName = fretboardExplicitChordName(beat);
                    if (explicitName) {
                        return {
                            name: explicitName,
                            rootPitchClass: fretboardChordRootPitchClass(explicitName),
                            inferred: false
                        };
                    }
                    return fretboardInferChord(highlighted, usesFlats);
                }

                function fretboardSvgElement(parent, tag, attrs, label) {
                    var element = document.createElementNS('http://www.w3.org/2000/svg', tag);
                    for (var key in attrs) {
                        if (Object.prototype.hasOwnProperty.call(attrs, key)) {
                            element.setAttribute(key, String(attrs[key]));
                        }
                    }
                    if (label !== undefined) element.textContent = String(label);
                    parent.appendChild(element);
                    return element;
                }


                function fretboardCollectionItems(collection) {
                    if (!collection) return [];
                    if (Array.isArray(collection)) return collection;
                    var result = [];
                    var length = Number(collection.length);
                    if (Number.isFinite(length)) {
                        for (var i = 0; i < length; i++) result.push(collection[i]);
                        return result;
                    }
                    var count = Number(collection.count);
                    if (Number.isFinite(count)) {
                        for (var j = 0; j < count; j++) {
                            result.push(typeof collection.get === 'function'
                                ? collection.get(j)
                                : collection[j]);
                        }
                    }
                    return result;
                }

                function fretboardEnumNumber(value) {
                    var numeric = Number(value);
                    return Number.isFinite(numeric) ? Math.round(numeric) : 0;
                }

                function fretboardEnumText(value) {
                    return String(value == null ? '' : value).toLowerCase();
                }

                function fretboardTechniqueNoteRef(note) {
                    if (!note) return null;
                    var fret = Math.round(Number(note.fret));
                    var stringNumber = Math.round(Number(note.string));
                    if (!Number.isFinite(fret) || !Number.isFinite(stringNumber) ||
                        fret < 0 || stringNumber < 1) return null;
                    return { fret: fret, stringNumber: stringNumber };
                }

                function fretboardBendAmountLabel(points) {
                    var list = fretboardCollectionItems(points);
                    var maximum = 0;
                    for (var i = 0; i < list.length; i++) {
                        var value = Math.abs(Number(list[i] && list[i].value));
                        if (Number.isFinite(value)) maximum = Math.max(maximum, value);
                    }
                    var quarterSteps = Math.round(maximum);
                    if (quarterSteps <= 0) return '';
                    if (quarterSteps === 1) return '1/4';
                    if (quarterSteps === 2) return '1/2';
                    if (quarterSteps === 3) return '3/4';
                    if (quarterSteps === 4) return 'Full';
                    if (quarterSteps === 5) return '1 1/4';
                    if (quarterSteps === 6) return '1 1/2';
                    if (quarterSteps === 7) return '1 3/4';
                    if (quarterSteps % 4 === 0) return String(quarterSteps / 4) + ' tones';
                    return String(quarterSteps) + '/4';
                }

                function fretboardBendTechnique(note) {
                    if (!note) return null;
                    var type = fretboardEnumNumber(note.bendType);
                    var rawText = fretboardEnumText(note.bendType);
                    var points = fretboardCollectionItems(note.bendPoints);
                    if (type === 0 && points.length > 0) type = 1;
                    if (type === 0 && rawText.indexOf('bend') < 0 &&
                        rawText.indexOf('release') < 0 && rawText.indexOf('hold') < 0) {
                        return null;
                    }

                    if (type === 0) {
                        if (rawText.indexOf('prebendbend') >= 0) type = 7;
                        else if (rawText.indexOf('prebendrelease') >= 0) type = 8;
                        else if (rawText.indexOf('prebend') >= 0) type = 6;
                        else if (rawText.indexOf('bendrelease') >= 0) type = 4;
                        else if (rawText.indexOf('release') >= 0) type = 3;
                        else if (rawText.indexOf('hold') >= 0) type = 5;
                        else type = 2;
                    }

                    var label = 'Bend';
                    var shape = 'up';
                    if (type === 3) {
                        label = 'Release';
                        shape = 'down';
                    } else if (type === 4) {
                        label = 'Bend/Release';
                        shape = 'updown';
                    } else if (type === 5) {
                        label = 'Hold';
                        shape = 'hold';
                    } else if (type === 6) {
                        label = 'Pre-bend';
                        shape = 'preup';
                    } else if (type === 7) {
                        label = 'Pre-bend +';
                        shape = 'preup';
                    } else if (type === 8) {
                        label = 'Pre-bend/Release';
                        shape = 'updown';
                    }

                    var amount = fretboardBendAmountLabel(points);
                    if (amount && type !== 3 && type !== 5) label += ' ' + amount;
                    var bendStyle = fretboardEnumNumber(note.bendStyle);
                    var bendStyleText = fretboardEnumText(note.bendStyle);
                    if (bendStyle === 1 || bendStyleText.indexOf('gradual') >= 0) {
                        label += ' gradual';
                    } else if (bendStyle === 2 || bendStyleText.indexOf('fast') >= 0) {
                        label += ' fast';
                    }
                    return { label: label, shape: shape, type: type, style: bendStyle };
                }

                function fretboardSlideTechniques(note) {
                    if (!note) return [];
                    var result = [];
                    var inType = fretboardEnumNumber(note.slideInType);
                    var outType = fretboardEnumNumber(note.slideOutType);
                    var inText = fretboardEnumText(note.slideInType);
                    var outText = fretboardEnumText(note.slideOutType);
                    var origin = fretboardTechniqueNoteRef(note.slideOrigin);
                    var target = fretboardTechniqueNoteRef(note.slideTarget);

                    if (inType === 0) {
                        if (inText.indexOf('frombelow') >= 0) inType = 1;
                        else if (inText.indexOf('fromabove') >= 0) inType = 2;
                    }
                    if (outType === 0) {
                        if (outText.indexOf('pickslidedown') >= 0) outType = 5;
                        else if (outText.indexOf('pickslideup') >= 0) outType = 6;
                        else if (outText.indexOf('outup') >= 0) outType = 3;
                        else if (outText.indexOf('outdown') >= 0) outType = 4;
                        else if (outText.indexOf('shift') >= 0) outType = 1;
                        else if (outText.indexOf('legato') >= 0) outType = 2;
                    }

                    if (origin) {
                        result.push({ phase: 'in-target', label: 'Slide', origin: origin });
                    } else if (inType === 1 || inType === 2) {
                        result.push({
                            phase: 'in-free',
                            label: 'Slide',
                            direction: inType === 1 ? 'below' : 'above'
                        });
                    }

                    if (outType === 1 || outType === 2) {
                        result.push({ phase: 'out-target', label: 'Slide', target: target });
                    } else if (outType === 3 || outType === 4) {
                        result.push({
                            phase: 'out-free',
                            label: 'Slide',
                            direction: outType === 3 ? 'up' : 'down'
                        });
                    } else if (outType === 5 || outType === 6) {
                        result.push({
                            phase: 'out-free',
                            label: 'Pick slide',
                            direction: outType === 6 ? 'up' : 'down',
                            pick: true
                        });
                    }
                    return result;
                }


                function fretboardTechniqueRefFromValue(value) {
                    return fretboardTechniqueNoteRef(value);
                }

                function fretboardTechniqueHasValue(value) {
                    var numeric = Number(value);
                    if (Number.isFinite(numeric)) return Math.round(numeric) !== 0;
                    var text = fretboardEnumText(value);
                    return text !== '' && text !== 'none' && text !== '0' && text !== 'false';
                }

                function fretboardHarmonicTechnique(note) {
                    if (!note) return null;
                    var type = fretboardEnumNumber(note.harmonicType);
                    var text = fretboardEnumText(note.harmonicType);
                    if (!note.isHarmonic && type === 0 && text.indexOf('harmonic') < 0) return null;
                    var labels = {
                        1: 'N.H.',
                        2: 'A.H.',
                        3: 'P.H.',
                        4: 'T.H.',
                        5: 'S.H.',
                        6: 'F.B.'
                    };
                    var label = labels[type] || 'Harm.';
                    if (text.indexOf('natural') >= 0) label = 'N.H.';
                    else if (text.indexOf('artificial') >= 0) label = 'A.H.';
                    else if (text.indexOf('pinch') >= 0) label = 'P.H.';
                    else if (text.indexOf('tap') >= 0) label = 'T.H.';
                    else if (text.indexOf('semi') >= 0) label = 'S.H.';
                    else if (text.indexOf('feedback') >= 0) label = 'F.B.';
                    return { label: label, type: type };
                }

                function fretboardVibratoTechnique(value) {
                    var type = fretboardEnumNumber(value);
                    var text = fretboardEnumText(value);
                    if (type === 0 && text.indexOf('vibrato') < 0 &&
                        text.indexOf('slight') < 0 && text.indexOf('wide') < 0) return null;
                    var wide = type === 2 || text.indexOf('wide') >= 0;
                    return { label: wide ? 'Wide vib.' : 'Vibrato', wide: wide };
                }

                function fretboardAccentTechnique(note) {
                    if (!note) return null;
                    var type = fretboardEnumNumber(note.accentuated);
                    var text = fretboardEnumText(note.accentuated);
                    if (type === 0 && text.indexOf('normal') < 0 &&
                        text.indexOf('heavy') < 0 && text.indexOf('tenuto') < 0) return null;
                    if (type === 2 || text.indexOf('heavy') >= 0) {
                        return { label: 'Heavy accent', symbol: '>>' };
                    }
                    if (type === 3 || text.indexOf('tenuto') >= 0) {
                        return { label: 'Tenuto', symbol: '—' };
                    }
                    return { label: 'Accent', symbol: '>' };
                }

                function fretboardOrnamentTechnique(note) {
                    if (!note) return null;
                    var type = fretboardEnumNumber(note.ornament);
                    var text = fretboardEnumText(note.ornament);
                    if (type === 0 && text === '') return null;
                    if (type === 1 || text.indexOf('invertedturn') >= 0) return { label: 'Inv. turn', symbol: '↶' };
                    if (type === 2 || text === 'turn' || text.indexOf('turn') >= 0) return { label: 'Turn', symbol: '↷' };
                    if (type === 3 || text.indexOf('uppermordent') >= 0) return { label: 'Mordent +', symbol: 'M+' };
                    if (type === 4 || text.indexOf('lowermordent') >= 0) return { label: 'Mordent -', symbol: 'M-' };
                    return null;
                }

                function fretboardFingerLabel(value, rightHand) {
                    var number = Number(value);
                    if (!Number.isFinite(number) || number < 0 || number > 4) return '';
                    number = Math.round(number);
                    if (rightHand) {
                        return ['Rp', 'Ri', 'Rm', 'Ra', 'Rc'][number] || '';
                    }
                    return number === 0 ? 'LT' : 'L' + number;
                }

                function fretboardHammerPullTechnique(note) {
                    if (!note) return null;
                    var origin = fretboardTechniqueRefFromValue(note.hammerPullOrigin);
                    var target = fretboardTechniqueRefFromValue(note.hammerPullDestination);
                    var current = fretboardTechniqueNoteRef(note);
                    if (!origin && !target && note.isHammerPullOrigin !== true) return null;
                    var destination = target || current;
                    var source = origin || current;
                    var label = 'H/P';
                    if (source && destination && Number.isFinite(source.fret) && Number.isFinite(destination.fret)) {
                        label = destination.fret >= source.fret ? 'H' : 'P';
                    }
                    return {
                        label: label,
                        origin: origin,
                        target: target,
                        phase: target || note.isHammerPullOrigin === true ? 'out' : 'in'
                    };
                }

                function fretboardSlurTechnique(note) {
                    if (!note) return null;
                    var origin = fretboardTechniqueRefFromValue(
                        note.slurOrigin || note.effectSlurOrigin
                    );
                    var target = fretboardTechniqueRefFromValue(
                        note.slurDestination || note.effectSlurDestination
                    );
                    var starts = note.isSlurOrigin === true || note.isEffectSlurOrigin === true;
                    var ends = note.isSlurDestination === true ||
                        note.isEffectSlurDestination === true || note.hasEffectSlur === true;
                    if (!origin && !target && !starts && !ends) return null;
                    return {
                        label: 'Legato',
                        origin: origin,
                        target: target,
                        phase: target || starts ? 'out' : 'in'
                    };
                }

                function fretboardTieTechnique(note) {
                    if (!note) return null;
                    var origin = fretboardTechniqueRefFromValue(note.tieOrigin);
                    var target = fretboardTechniqueRefFromValue(note.tieDestination);
                    if (!origin && !target && note.isTieOrigin !== true && note.isTieDestination !== true) return null;
                    return {
                        label: 'Tie',
                        origin: origin,
                        target: target,
                        phase: target || note.isTieOrigin === true ? 'out' : 'in'
                    };
                }

                function fretboardTrillTechnique(note) {
                    if (!note || note.isTrill !== true) return null;
                    var targetFret = Math.round(Number(note.trillFret));
                    if (!Number.isFinite(targetFret) || targetFret < 0) {
                        var trillValue = Number(note.trillValue);
                        var currentValue = Number(note.realValue);
                        if (Number.isFinite(trillValue) && Number.isFinite(currentValue)) {
                            targetFret = Math.max(0, Math.round(Number(note.fret) + trillValue - currentValue));
                        }
                    }
                    return {
                        label: Number.isFinite(targetFret) ? 'tr ' + targetFret : 'tr',
                        targetFret: Number.isFinite(targetFret) ? targetFret : null
                    };
                }

                function fretboardThemeInk(darkInk, lightInk) {
                    return currentThemeIsDark ? darkInk : lightInk;
                }

                function fretboardNoteEffects(note) {
                    note = note || {};
                    var harmonic = fretboardHarmonicTechnique(note);
                    var vibrato = fretboardVibratoTechnique(note.vibrato);
                    var hammerPull = fretboardHammerPullTechnique(note);
                    var slur = hammerPull ? null : fretboardSlurTechnique(note);
                    var tie = fretboardTieTechnique(note);
                    var trill = fretboardTrillTechnique(note);
                    var accent = fretboardAccentTechnique(note);
                    var ornament = fretboardOrnamentTechnique(note);
                    var leftFinger = fretboardFingerLabel(note.leftHandFinger, false);
                    var rightFinger = fretboardFingerLabel(note.rightHandFinger, true);
                    var tags = [];
                    if (harmonic) tags.push({ label: harmonic.label, ink: fretboardThemeInk('#67e8f9', '#0e7490') });
                    if (note.isLeftHandTapped === true) tags.push({ label: 'T', ink: fretboardThemeInk('#f472b6', '#be185d') });
                    if (ornament) tags.push({ label: ornament.label, ink: fretboardThemeInk('#c084fc', '#7e22ce') });
                    if (leftFinger) tags.push({ label: leftFinger, ink: fretboardThemeInk('#a5b4fc', '#4338ca') });
                    if (rightFinger) tags.push({ label: rightFinger, ink: fretboardThemeInk('#a5b4fc', '#4338ca') });
                    if (note.isContinuedBend === true) tags.push({ label: 'Bend cont.', ink: fretboardThemeInk('#fb923c', '#d9480f') });
                    return {
                        dead: note.isDead === true,
                        ghost: note.isGhost === true,
                        staccato: note.isStaccato === true,
                        palmMute: note.isPalmMute === true,
                        letRing: note.isLetRing === true,
                        tapped: note.isLeftHandTapped === true,
                        harmonic: harmonic,
                        vibrato: vibrato,
                        hammerPull: hammerPull,
                        slur: slur,
                        tie: tie,
                        trill: trill,
                        accent: accent,
                        ornament: ornament,
                        tags: tags
                    };
                }

                function fretboardWhammyTechnique(beat) {
                    if (!beat) return null;
                    var type = fretboardEnumNumber(beat.whammyBarType);
                    var text = fretboardEnumText(beat.whammyBarType);
                    var points = fretboardCollectionItems(beat.whammyBarPoints);
                    var has = beat.hasWhammyBar === true || beat.isContinuedWhammy === true ||
                        type !== 0 || points.length > 0 || text.indexOf('whammy') >= 0 ||
                        text.indexOf('dive') >= 0 || text.indexOf('dip') >= 0;
                    if (!has) return null;
                    var labels = {
                        1: 'Whammy',
                        2: 'Dive',
                        3: 'Dip',
                        4: 'Whammy hold',
                        5: 'Pre-dive',
                        6: 'Pre-dive/Dive'
                    };
                    var label = labels[type] || 'Whammy';
                    if (beat.isContinuedWhammy === true && type === 0) label = 'Whammy cont.';
                    if (text.indexOf('predivedive') >= 0) label = 'Pre-dive/Dive';
                    else if (text.indexOf('predive') >= 0) label = 'Pre-dive';
                    else if (text.indexOf('dive') >= 0) label = 'Dive';
                    else if (text.indexOf('dip') >= 0) label = 'Dip';
                    else if (text.indexOf('hold') >= 0) label = 'Whammy hold';
                    var amount = fretboardBendAmountLabel(points);
                    if (amount && label.indexOf('hold') < 0) label += ' ' + amount;
                    return { label: label, type: type, points: points };
                }

                function fretboardTremoloTechnique(beat) {
                    if (!beat) return null;
                    var effect = beat.tremoloPicking || null;
                    var rawMarks = Number(effect && effect.marks);
                    var marks = Math.round(rawMarks);
                    var style = fretboardEnumNumber(effect && effect.style);
                    var legacy = fretboardEnumNumber(beat.tremoloSpeed);
                    var has = beat.isTremolo === true || effect != null || legacy !== 0;
                    if (!has) return null;
                    if (!Number.isFinite(rawMarks) || marks < 0) {
                        marks = legacy > 0 ? Math.min(5, legacy) : 3;
                    }
                    marks = Math.max(0, Math.min(5, marks));
                    var buzz = style === 1 || fretboardEnumText(effect && effect.style).indexOf('buzz') >= 0;
                    return {
                        label: buzz ? 'Buzz roll' : (marks > 0 ? 'TR x' + marks : 'TR'),
                        marks: marks,
                        buzz: buzz
                    };
                }

                function fretboardBrushTechnique(beat) {
                    if (!beat) return null;
                    var type = fretboardEnumNumber(beat.brushType);
                    var text = fretboardEnumText(beat.brushType);
                    if (type === 0 && text === '') return null;
                    if (type === 1 || text.indexOf('brushup') >= 0) return { label: 'Strum ↑', direction: 'up', arpeggio: false };
                    if (type === 2 || text.indexOf('brushdown') >= 0) return { label: 'Strum ↓', direction: 'down', arpeggio: false };
                    if (type === 3 || text.indexOf('arpeggioup') >= 0) return { label: 'Arp. ↑', direction: 'up', arpeggio: true };
                    if (type === 4 || text.indexOf('arpeggiodown') >= 0) return { label: 'Arp. ↓', direction: 'down', arpeggio: true };
                    return null;
                }

                function fretboardRasgueadoLabel(value) {
                    var type = fretboardEnumNumber(value);
                    var labels = [
                        '', 'Rasg. ii', 'Rasg. mi', 'Rasg. mii 3', 'Rasg. mii A',
                        'Rasg. pmp 3', 'Rasg. pmp A', 'Rasg. pei 3', 'Rasg. pei A',
                        'Rasg. pai 3', 'Rasg. pai A', 'Rasg. ami 3', 'Rasg. ami A',
                        'Rasg. ppp', 'Rasg. amii', 'Rasg. amip', 'Rasg. eami',
                        'Rasg. eamii', 'Rasg. peami'
                    ];
                    if (type > 0 && type < labels.length) return labels[type];
                    var text = fretboardEnumText(value);
                    return text && text !== 'none' && text !== '0' ? 'Rasgueado' : '';
                }

                function fretboardBeatTechniques(beat) {
                    var result = {
                        labels: [],
                        whammy: null,
                        tremolo: null,
                        brush: null,
                        barre: null
                    };
                    if (!beat) return result;
                    var seen = {};
                    function add(label, ink) {
                        if (!label || seen[label]) return;
                        seen[label] = true;
                        result.labels.push({ label: label, ink: ink || fretboardThemeInk('#cbd5e1', '#475569') });
                    }

                    result.whammy = fretboardWhammyTechnique(beat);
                    result.tremolo = fretboardTremoloTechnique(beat);
                    result.brush = fretboardBrushTechnique(beat);
                    if (result.whammy) add(result.whammy.label, fretboardThemeInk('#2dd4bf', '#0f766e'));
                    if (result.tremolo) add(result.tremolo.label, fretboardThemeInk('#f472b6', '#be185d'));
                    if (result.brush) add(result.brush.label, fretboardThemeInk('#a78bfa', '#6d28d9'));

                    var pickType = fretboardEnumNumber(beat.pickStroke);
                    var pickText = fretboardEnumText(beat.pickStroke);
                    if (pickType === 1 || pickText.indexOf('up') >= 0) add('Pick ↑', fretboardThemeInk('#60a5fa', '#1d4ed8'));
                    else if (pickType === 2 || pickText.indexOf('down') >= 0) add('Pick ↓', fretboardThemeInk('#60a5fa', '#1d4ed8'));

                    var beatVibrato = fretboardVibratoTechnique(beat.vibrato);
                    if (beatVibrato) add(beatVibrato.label, fretboardThemeInk('#22d3ee', '#0e7490'));
                    if (beat.isPalmMute === true) add('P.M.', fretboardThemeInk('#fbbf24', '#b45309'));
                    if (beat.isLetRing === true) add('Let ring', fretboardThemeInk('#86efac', '#047857'));

                    var graceType = fretboardEnumNumber(beat.graceType);
                    var graceText = fretboardEnumText(beat.graceType);
                    if (graceType === 1 || graceText.indexOf('onbeat') >= 0) add('Grace on beat', fretboardThemeInk('#c4b5fd', '#6d28d9'));
                    else if (graceType === 2 || graceText.indexOf('before') >= 0) add('Grace', fretboardThemeInk('#c4b5fd', '#6d28d9'));
                    else if (graceType === 3 || graceText.indexOf('bend') >= 0) add('Bend grace', fretboardThemeInk('#fb923c', '#d9480f'));

                    if (beat.slap === true) add('Slap', fretboardThemeInk('#38bdf8', '#0277b5'));
                    if (beat.pop === true) add('Pop', fretboardThemeInk('#38bdf8', '#0277b5'));
                    if (beat.tap === true) add('Tap', fretboardThemeInk('#f472b6', '#be185d'));
                    if (beat.deadSlapped === true) add('Dead slap', fretboardThemeInk('#f87171', '#b91c1c'));

                    var golpe = fretboardEnumNumber(beat.golpe);
                    var golpeText = fretboardEnumText(beat.golpe);
                    if (golpe === 1 || golpeText.indexOf('thumb') >= 0) add('Golpe T', fretboardThemeInk('#fb7185', '#be123c'));
                    else if (golpe === 2 || golpeText.indexOf('finger') >= 0) add('Golpe F', fretboardThemeInk('#fb7185', '#be123c'));

                    var wah = fretboardEnumNumber(beat.wahPedal);
                    var wahText = fretboardEnumText(beat.wahPedal);
                    if (wah === 1 || wahText.indexOf('open') >= 0) add('Wah open', fretboardThemeInk('#4ade80', '#15803d'));
                    else if (wah === 2 || wahText.indexOf('closed') >= 0) add('Wah closed', fretboardThemeInk('#4ade80', '#15803d'));

                    var fade = fretboardEnumNumber(beat.fade);
                    var fadeText = fretboardEnumText(beat.fade);
                    if (fade === 1 || fadeText.indexOf('fadein') >= 0) add('Fade in', fretboardThemeInk('#93c5fd', '#1d4ed8'));
                    else if (fade === 2 || fadeText.indexOf('fadeout') >= 0) add('Fade out', fretboardThemeInk('#93c5fd', '#1d4ed8'));
                    else if (fade === 3 || fadeText.indexOf('swell') >= 0) add('Swell', fretboardThemeInk('#93c5fd', '#1d4ed8'));

                    var crescendo = fretboardEnumNumber(beat.crescendo);
                    var crescendoText = fretboardEnumText(beat.crescendo);
                    if (crescendo === 1 || crescendoText.indexOf('crescendo') >= 0) add('Cresc.', fretboardThemeInk('#f0abfc', '#a21caf'));
                    else if (crescendo === 2 || crescendoText.indexOf('decrescendo') >= 0) add('Dim.', fretboardThemeInk('#f0abfc', '#a21caf'));

                    var rasgueado = fretboardRasgueadoLabel(beat.rasgueado);
                    if (rasgueado || beat.hasRasgueado === true) add(rasgueado || 'Rasgueado', fretboardThemeInk('#f9a8d4', '#be185d'));
                    if (beat.isEffectSlurOrigin === true || beat.isEffectSlurDestination === true ||
                        beat.isLegatoOrigin === true || beat.isLegatoDestination === true) {
                        add('Legato', fretboardThemeInk('#34d399', '#047857'));
                    }
                    if (beat.fermata) add('Fermata', fretboardThemeInk('#fde68a', '#b45309'));

                    if (beat.isBarre === true || Number(beat.barreFret) > 0 ||
                        fretboardTechniqueHasValue(beat.barreShape)) {
                        var barreFret = Math.max(0, Math.round(Number(beat.barreFret) || 0));
                        var barreShape = fretboardEnumNumber(beat.barreShape);
                        var half = barreShape === 2 || fretboardEnumText(beat.barreShape).indexOf('half') >= 0;
                        result.barre = { fret: barreFret, half: half };
                        add((half ? 'Half barre ' : 'Barre ') + barreFret, fretboardThemeInk('#c084fc', '#7e22ce'));
                    }
                    return result;
                }


                // Scan only the current track. Nothing is reported unless its actual GP
                // model contains the technique. Cache per track to avoid repeated work.
                var songTechniqueCatalogCache = {};
                function collectSongTechniqueIds() {
                    var track = currentTracks && currentTracks[selectedTrackForPlayback];
                    if (!track) return [];
                    var found = Object.create(null);
                    function add(code) { found[code] = true; }
                    function active(value) { return fretboardTechniqueHasValue(value); }

                    var masterBars = fretboardCollectionItems(api && api.score && api.score.masterBars);
                    for (var mbi = 0; mbi < masterBars.length; mbi++) {
                        var master = masterBars[mbi];
                        if (!master) continue;
                        if (Number(master.timeSignatureNumerator) > 0 && Number(master.timeSignatureDenominator) > 0) add('time_signature');
                        if (master.keySignature != null && Number(master.keySignature) !== 0 &&
                            Number.isFinite(Number(master.keySignature))) add('key_signature');
                        if (fretboardCollectionItems(master.tempoAutomations).length > 0) add('tempo');
                        if (master.isRepeatStart === true || Number(master.repeatCount) > 0 ||
                            master.isRepeatEnd === true || active(master.alternateEndings)) add('repeat');
                    }

                    var staves = fretboardCollectionItems(track.staves);
                    for (var si = 0; si < staves.length; si++) {
                        var bars = fretboardCollectionItems(staves[si] && staves[si].bars);
                        for (var bi = 0; bi < bars.length; bi++) {
                            var voices = fretboardCollectionItems(bars[bi] && bars[bi].voices);
                            for (var vi = 0; vi < voices.length; vi++) {
                                var beats = fretboardCollectionItems(voices[vi] && voices[vi].beats);
                                for (var ti = 0; ti < beats.length; ti++) {
                                    var beat = beats[ti];
                                    if (!beat) continue;
                                    if (beat.isRest === true) add('rest');
                                    if (Number(beat.tupletNumerator) > 1) add('tuplet');
                                    if (beat.fermata) add('fermata');
                                    if (beat.hasChord === true || beat.chord || active(beat.chordId)) add('chord_symbol');
                                    if (beat.isPalmMute === true) add('palm_mute');
                                    if (beat.isLetRing === true) add('let_ring');
                                    if (fretboardWhammyTechnique(beat)) add('whammy');
                                    var trem = fretboardTremoloTechnique(beat);
                                    if (trem) add(trem.buzz ? 'buzz' : 'tremolo');
                                    var brush = fretboardBrushTechnique(beat);
                                    if (brush) add(brush.arpeggio ? 'arpeggio' : 'strum');
                                    if (active(beat.pickStroke)) add('pick_stroke');
                                    if (fretboardVibratoTechnique(beat.vibrato)) {
                                        add(fretboardVibratoTechnique(beat.vibrato).wide ? 'wide_vibrato' : 'vibrato');
                                    }
                                    if (active(beat.graceType)) add('grace');
                                    if (beat.slap === true) add('slap');
                                    if (beat.pop === true) add('pop');
                                    if (beat.tap === true) add('tapping');
                                    if (beat.deadSlapped === true) add('dead_slap');
                                    if (active(beat.rasgueado) || beat.hasRasgueado === true) add('rasgueado');
                                    if (active(beat.golpe)) add('golpe');
                                    if (active(beat.wahPedal)) add('wah');
                                    if (active(beat.fade)) add('fade');
                                    if (active(beat.crescendo)) add('crescendo');
                                    if (beat.isBarre === true || Number(beat.barreFret) > 0 || active(beat.barreShape)) add('barre');
                                    if (beat.isEffectSlurOrigin === true || beat.isEffectSlurDestination === true ||
                                        beat.isLegatoOrigin === true || beat.isLegatoDestination === true) add('legato');
                                    var notes = fretboardCollectionItems(beat.notes);
                                    for (var ni = 0; ni < notes.length; ni++) {
                                        var note = notes[ni];
                                        if (!note || note.isVisible === false) continue;
                                        if (fretboardBendTechnique(note) || note.isContinuedBend === true) add('bend');
                                        var slides = fretboardSlideTechniques(note);
                                        for (var sli = 0; sli < slides.length; sli++) add(slides[sli].pick ? 'pick_slide' : 'slide');
                                        var hp = fretboardHammerPullTechnique(note);
                                        if (hp) {
                                            if (hp.label === 'H') add('hammer');
                                            else if (hp.label === 'P') add('pull');
                                            else { add('hammer'); add('pull'); }
                                        }
                                        if (fretboardSlurTechnique(note)) add('legato');
                                        if (fretboardTieTechnique(note)) add('tie');
                                        if (fretboardTrillTechnique(note)) add('trill');
                                        if (fretboardHarmonicTechnique(note)) add('harmonic');
                                        var vib = fretboardVibratoTechnique(note.vibrato);
                                        if (vib) add(vib.wide ? 'wide_vibrato' : 'vibrato');
                                        if (note.isDead === true) add('dead');
                                        if (note.isGhost === true) add('ghost');
                                        if (note.isStaccato === true) add('staccato');
                                        if (note.isPalmMute === true) add('palm_mute');
                                        if (note.isLetRing === true) add('let_ring');
                                        if (note.isLeftHandTapped === true) add('tapping');
                                        var accent = fretboardAccentTechnique(note);
                                        if (accent) add(accent.label === 'Heavy accent' ? 'heavy_accent' :
                                            accent.label === 'Tenuto' ? 'tenuto' : 'accent');
                                        var ornament = fretboardOrnamentTechnique(note);
                                        if (ornament) add(ornament.label.indexOf('mordent') >= 0 ||
                                            ornament.label.indexOf('Mordent') >= 0 ? 'mordent' : 'turn');
                                        if (fretboardFingerLabel(note.leftHandFinger, false) ||
                                            fretboardFingerLabel(note.rightHandFinger, true)) add('fingering');
                                        // DynamicValue 0 is PPP (not "None"); retain it.
                                        if (note.dynamics !== null && note.dynamics !== undefined) add('dynamics');
                                    }
                                }
                            }
                        }
                    }
                    return Object.keys(found);
                }

                function reportSongTechniqueCatalog() {
                    var trackIndex = Number(selectedTrackForPlayback) || 0;
                    try {
                        if (!Object.prototype.hasOwnProperty.call(songTechniqueCatalogCache, trackIndex)) {
                            songTechniqueCatalogCache[trackIndex] = collectSongTechniqueIds();
                        }
                        AndroidInterface.onSongTechniqueCatalog(
                            trackIndex,
                            JSON.stringify(songTechniqueCatalogCache[trackIndex])
                        );
                    } catch (error) {
                        AndroidInterface.onSongTechniqueCatalogFailed();
                    }
                }
                function openSongTechniqueInfo() {
                    if (window.AndroidInterface &&
                        typeof AndroidInterface.onSongTechniqueInfoRequested === 'function') {
                        AndroidInterface.onSongTechniqueInfoRequested();
                    }
                }

                var fretboardRefreshTimer = null;

                function scheduleFretboardRefresh() {
                    if (!fretboardEnabled || isPdfExportMode) return;
                    try { refreshFretboard(); } catch (error) {
                        console.warn('Immediate fretboard render failed', error);
                    }
                    requestAnimationFrame(function() {
                        try { refreshFretboard(); } catch (error) {
                            console.warn('Animation-frame fretboard render failed', error);
                        }
                    });
                    clearTimeout(fretboardRefreshTimer);
                    fretboardRefreshTimer = setTimeout(function() {
                        try { refreshFretboard(); } catch (error) {
                            console.warn('Delayed fretboard render failed', error);
                        }
                    }, 120);
                }

                function fretboardWindowForHighlights(highlighted) {
                    var frets = highlighted.filter(function(note) {
                        return note.fret > 0;
                    }).map(function(note) {
                        return note.fret;
                    });

                    if (frets.length === 0) return fretboardWindowStart;

                    var minimumFret = Math.min.apply(null, frets);
                    var maximumFret = Math.max.apply(null, frets);
                    var currentLastFret = fretboardWindowStart + 11;

                    // The current played note always has priority. A remote slide target
                    // is clamped to the board edge instead of moving the current note away.
                    if (maximumFret > currentLastFret) {
                        fretboardWindowStart = 1 +
                            Math.ceil((maximumFret - 12) / 4) * 4;
                    } else if (minimumFret < fretboardWindowStart) {
                        fretboardWindowStart = minimumFret <= 4
                            ? 1
                            : 1 + Math.floor((minimumFret - 1) / 4) * 4;
                    }

                    fretboardWindowStart = Math.max(1, Math.min(25, fretboardWindowStart));
                    return fretboardWindowStart;
                }

                function keepScoreCursorNearFretboard(force) {
                    if (!fretboardEnabled || isPdfExportMode || isUserScrolling) return;
                    var wrapper = document.getElementById('scroll-wrapper');
                    if (!wrapper || wrapper.clientHeight <= 1) return;

                    // at-cursor-bar thường bao trọn cả khuông + TAB của ô nhịp;
                    // at-cursor-beat là vạch nốt hiện tại. Dùng hợp của cả hai để
                    // đảm bảo phần dưới khuông không nằm sau mép cần đàn.
                    var cursors = [
                        document.querySelector('.at-cursor-bar'),
                        document.querySelector('.at-cursor-beat')
                    ];
                    var activeTop = Infinity;
                    var activeBottom = -Infinity;
                    var hasCursor = false;
                    for (var i = 0; i < cursors.length; i++) {
                        var cursor = cursors[i];
                        if (!cursor) continue;
                        var rect = cursor.getBoundingClientRect();
                        if (!Number.isFinite(rect.top) || !Number.isFinite(rect.bottom) ||
                            rect.width <= 0 || rect.height <= 0) continue;
                        activeTop = Math.min(activeTop, rect.top);
                        activeBottom = Math.max(activeBottom, rect.bottom);
                        hasCursor = true;
                    }
                    if (!hasCursor) return;

                    var wrapperRect = wrapper.getBoundingClientRect();
                    var viewportHeight = wrapper.clientHeight;
                    var relativeTop = activeTop - wrapperRect.top;
                    var relativeBottom = activeBottom - wrapperRect.top;
                    var activeHeight = Math.max(1, relativeBottom - relativeTop);
                    var topMargin = Math.max(12, viewportHeight * .045);
                    var bottomMargin = Math.max(28, viewportHeight * .085);
                    var safeBottom = viewportHeight - bottomMargin;
                    var safeHeight = Math.max(1, safeBottom - topMargin);
                    var activeCenter = (relativeTop + relativeBottom) * .5;

                    // Đặt toàn bộ hệ khuông ở nửa trên của vùng score để mắt chỉ cần
                    // di chuyển một quãng ngắn xuống cần đàn, nhưng vẫn thấy trọn TAB.
                    var targetRelativeTop = topMargin;
                    if (activeHeight < safeHeight) {
                        targetRelativeTop += Math.min(
                            (safeHeight - activeHeight) * .22,
                            viewportHeight * .16
                        );
                    }

                    var shouldMove = force ||
                        relativeTop < topMargin ||
                        relativeBottom > safeBottom ||
                        activeCenter > viewportHeight * .58;
                    if (!shouldMove) return;

                    var absoluteTop = wrapper.scrollTop + relativeTop;
                    var maximumScrollTop = Math.max(0, wrapper.scrollHeight - wrapper.clientHeight);
                    var targetTop = Math.max(
                        0,
                        Math.min(maximumScrollTop, absoluteTop - targetRelativeTop)
                    );
                    if (Math.abs(wrapper.scrollTop - targetTop) > 1) {
                        // Không dùng smooth ở chế độ cần đàn: tại BPM cao, nhiều lần
                        // animation chồng nhau làm TAB trôi sau nốt đang phát.
                        wrapper.scrollTo({ top: targetTop, behavior: 'auto' });
                    }
                }

                function refreshFretboard() {
                    if (!fretboardEnabled || isPdfExportMode) return;
                    var panel = document.getElementById('tt-fretboard-panel');
                    var svg = document.getElementById('tt-fretboard-svg');
                    var status = document.getElementById('tt-fretboard-status');
                    var header = document.getElementById('tt-fretboard-title');
                    var trackName = document.getElementById('tt-fretboard-track');
                    var chordBadge = document.getElementById('tt-fretboard-chord');
                    var close = document.getElementById('tt-fretboard-close');
                    if (!panel || !svg || !status || !header || !trackName ||
                        !chordBadge || !close) return;

                    panel.style.display = 'flex';
                    panel.style.visibility = 'visible';
                    panel.style.opacity = '1';
                    panel.style.pointerEvents = 'auto';
                    header.textContent = fretboardLabels.title;
                    close.textContent = '×';
                    close.setAttribute('aria-label', fretboardLabels.close);
                    chordBadge.textContent = '';
                    chordBadge.classList.remove('is-visible');

                    while (svg.firstChild) svg.removeChild(svg.firstChild);
                    var staff = fretboardStaffForTrack();
                    var count = staff && staff.tuning ? staff.tuning.length : 0;
                    var track = selectedTrackForNoteNames();
                    if (!staff || count < 4) {
                        trackName.textContent = track && track.name ? '• ' + track.name : '';
                        status.textContent = track ? fretboardLabels.unsupported : fretboardLabels.idle;
                        return;
                    }

                    var currentBeat = fretboardLatestBeat;
                    if (currentBeat && !beatBelongsToSelectedTrack(currentBeat, track)) {
                        currentBeat = null;
                    }
                    var isPreview = !currentBeat;
                    if (!currentBeat) currentBeat = firstFrettedBeatForPreview();
                    var usesFlats = currentBeat ? beatUsesFlatNoteNames(currentBeat) : false;
                    var highlighted = [];

                    if (currentBeat && currentBeat.notes) {
                        for (var i = 0; i < currentBeat.notes.length; i++) {
                            var note = currentBeat.notes[i];
                            var number = Math.round(Number(note.string));
                            var fret = Math.round(Number(note.fret));
                            var effects = fretboardNoteEffects(note);
                            if (effects.dead && (!Number.isFinite(fret) || fret < 0)) fret = 0;
                            if (note.isVisible === false ||
                                !Number.isFinite(number) || !Number.isFinite(fret) ||
                                number < 1 || number > count || fret < 0 || fret > 36) continue;
                            var row = count - number;
                            var pitch = effects.dead ? NaN : notePitchForName(note);
                            if (!effects.dead && !Number.isFinite(pitch)) {
                                pitch = Number(staff.tuning[row]) + fret + Number(staff.capo || 0);
                            }
                            highlighted.push({
                                row: row,
                                stringNumber: number,
                                fret: fret,
                                pitch: pitch,
                                pitchClass: effects.dead ? null : fretboardPitchClass(pitch),
                                name: effects.dead ? '×' : fretboardNoteLabel(pitch, usesFlats),
                                note: note,
                                effects: effects,
                                bend: fretboardBendTechnique(note),
                                slides: fretboardSlideTechniques(note)
                            });
                        }
                    }

                    var chordCandidates = highlighted.filter(function(point) {
                        return !(point.effects && point.effects.dead);
                    });
                    var chordInfo = fretboardChordForBeat(
                        currentBeat, chordCandidates, usesFlats, isPreview
                    );
                    var beatTechniques = fretboardBeatTechniques(currentBeat);
                    function ensureBeatTechnique(label, ink) {
                        if (!label) return;
                        for (var ebt = 0; ebt < beatTechniques.labels.length; ebt++) {
                            if (beatTechniques.labels[ebt].label === label) return;
                        }
                        beatTechniques.labels.push({ label: label, ink: ink });
                    }
                    for (var hfx = 0; hfx < highlighted.length; hfx++) {
                        var noteEffects = highlighted[hfx].effects || {};
                        if (noteEffects.palmMute) ensureBeatTechnique('P.M.', fretboardThemeInk('#fbbf24', '#b45309'));
                        if (noteEffects.letRing) ensureBeatTechnique('Let ring', fretboardThemeInk('#86efac', '#047857'));
                    }
                    // Không hiển thị badge "Hợp âm: ..." ở header. Tên hợp âm
                    // sẽ nằm ngay phía trên cụm nốt trong cần đàn.

                    var firstFret = fretboardWindowForHighlights(highlighted);
                    var visibleFrets = 12;
                    var lastFret = firstFret + visibleFrets - 1;
                    var trackParts = [];
                    if (track && track.name) trackParts.push(track.name);
                    trackParts.push(fretboardLabels.fret + ' ' + firstFret + '–' + lastFret);
                    trackName.textContent = '• ' + trackParts.join(' • ');

                    var viewW = 410;
                    // More vertical room prevents adjacent string labels and note circles
                    // from touching. The open-string column is also separated from labels.
                    var viewH = Math.max(220, 27 * count + 58);
                    var labelCenterX = 17;
                    var openStringX = 50;
                    var boardStartX = 64;
                    var boardEndX = 401;
                    var boardTop = 34;
                    var boardBottom = viewH - 27;
                    var topY = 55;
                    var bottomY = viewH - 43;
                    var fretWidth = (boardEndX - boardStartX) / visibleFrets;
                    var centerY = (topY + bottomY) * .5;

                    // Màu cần đàn và nốt được lấy trực tiếp từ bảng tùy chỉnh.
                    // Độ sáng của chính mặt cần đàn quyết định màu phím, dây và chữ,
                    // nên người dùng vẫn có thể dùng cần đàn tối trong theme sáng hoặc ngược lại.
                    var boardInk = normalizeVisualHex(
                        currentVisualPreferences.fretboardColor,
                        currentThemeIsDark ? '#171B24' : '#F8FAFC'
                    );
                    var boardIsLight = visualLuminance(boardInk) > .179;
                    var isDarkBoardTheme = !boardIsLight;
                    var boardContrastInk = boardIsLight ? '#1F2937' : '#F8FAFC';
                    var boardBorderInk = visualMix(
                        boardInk,
                        boardIsLight ? '#475569' : '#CBD5E1',
                        boardIsLight ? .52 : .44
                    );
                    var labelInk = boardContrastInk;
                    var secondaryInk = visualMix(
                        boardInk,
                        boardContrastInk,
                        boardIsLight ? .72 : .68
                    );
                    var fretInk = visualMix(
                        boardInk,
                        boardContrastInk,
                        boardIsLight ? .44 : .40
                    );
                    var stringInk = visualMix(
                        boardInk,
                        boardContrastInk,
                        boardIsLight ? .62 : .58
                    );
                    var markerInk = visualMix(
                        boardInk,
                        boardContrastInk,
                        boardIsLight ? .50 : .46
                    );
                    var activeNoteInk = normalizeVisualHex(
                        currentVisualPreferences.noteColor,
                        currentThemeIsDark ? '#FFD43B' : '#FBBF24'
                    );
                    // Dây đang đánh dùng cùng họ màu với nốt nhưng được tăng tương phản
                    // khi người dùng chọn nền cần đàn sáng gần với màu nốt.
                    var activeInk = activeNoteInk;
                    if (visualContrast(activeInk, boardInk) < 2.35) {
                        activeInk = visualMix(
                            activeInk,
                            boardIsLight ? '#3F2A00' : '#FFFFFF',
                            boardIsLight ? .48 : .30
                        );
                    }
                    var activeTextInk = visualTextColor(activeNoteInk);
                    var previewInk = visualMix(boardInk, boardContrastInk, .22);
                    var noteInk = isPreview ? previewInk : activeNoteInk;
                    var slideInk = isDarkBoardTheme ? '#38BDF8' : '#0277B5';
                    var bendInk = isDarkBoardTheme ? '#FB923C' : '#D9480F';
                    var legatoInk = isDarkBoardTheme ? '#34D399' : '#047857';
                    var trillInk = isDarkBoardTheme ? '#F472B6' : '#BE185D';
                    var vibratoInk = isDarkBoardTheme ? '#22D3EE' : '#0E7490';
                    var whammyInk = isDarkBoardTheme ? '#2DD4BF' : '#0F766E';
                    var rhythmInk = isDarkBoardTheme ? '#A78BFA' : '#6D28D9';
                    var labelBubbleInk = visualMix(
                        boardInk,
                        boardIsLight ? '#FFFFFF' : '#000000',
                        boardIsLight ? .56 : .30
                    );
                    var labelBubbleBorderInk = boardBorderInk;
                    var stringSheenInk = boardContrastInk;
                    var stringSheenOpacity = boardIsLight ? .14 : .30;
                    var nutInk = visualMix(boardInk, boardContrastInk, .78);
                    var techniquePillInk = boardIsLight ? '#FFFFFF' : '#0B1220';
                    var techniquePillOpacity = .95;
                    var noteOutlineInk = visualTextColor(activeNoteInk);
                    var ghostInk = isDarkBoardTheme ? '#FDE68A' : '#B45309';
                    var deadNoteFillInk = boardIsLight ? '#FFFFFF' : '#111827';
                    var accentInk = boardContrastInk;
                    var slurInk = isDarkBoardTheme ? '#A5B4FC' : '#4F46E5';
                    var tieInk = visualMix(boardInk, boardContrastInk, .62);
                    var cellShadeInk = boardContrastInk;
                    var cellShadeOpacity = boardIsLight ? .026 : .018;
                    var activeRowMap = {};
                    for (var a = 0; a < highlighted.length; a++) {
                        activeRowMap[highlighted[a].row] = true;
                    }

                    svg.setAttribute('viewBox', '0 0 ' + viewW + ' ' + viewH);
                    svg.setAttribute(
                        'aria-label',
                        fretboardLabels.title + ', ' + count + ' strings, ' +
                        fretboardLabels.fret + ' ' + firstFret + ' to ' + lastFret
                    );

                    var defs = fretboardSvgElement(svg, 'defs', {});
                    var glowFilter = fretboardSvgElement(defs, 'filter', {
                        id: 'tt-note-glow', x: '-80%', y: '-80%', width: '260%', height: '260%'
                    });
                    fretboardSvgElement(glowFilter, 'feGaussianBlur', {
                        stdDeviation: 3.6, result: 'blur'
                    });
                    var glowMerge = fretboardSvgElement(glowFilter, 'feMerge', {});
                    fretboardSvgElement(glowMerge, 'feMergeNode', { 'in': 'blur' });
                    fretboardSvgElement(glowMerge, 'feMergeNode', { 'in': 'SourceGraphic' });


                    var slideArrow = fretboardSvgElement(defs, 'marker', {
                        id: 'tt-slide-arrow', markerWidth: 8, markerHeight: 8,
                        refX: 7, refY: 4, orient: 'auto', markerUnits: 'userSpaceOnUse'
                    });
                    fretboardSvgElement(slideArrow, 'path', {
                        d: 'M 0 0 L 8 4 L 0 8 z', fill: slideInk
                    });
                    var bendArrow = fretboardSvgElement(defs, 'marker', {
                        id: 'tt-bend-arrow', markerWidth: 8, markerHeight: 8,
                        refX: 7, refY: 4, orient: 'auto', markerUnits: 'userSpaceOnUse'
                    });
                    fretboardSvgElement(bendArrow, 'path', {
                        d: 'M 0 0 L 8 4 L 0 8 z', fill: bendInk
                    });
                    var legatoArrow = fretboardSvgElement(defs, 'marker', {
                        id: 'tt-legato-arrow', markerWidth: 7, markerHeight: 7,
                        refX: 6, refY: 3.5, orient: 'auto', markerUnits: 'userSpaceOnUse'
                    });
                    fretboardSvgElement(legatoArrow, 'path', {
                        d: 'M 0 0 L 7 3.5 L 0 7 z', fill: legatoInk
                    });
                    var whammyArrow = fretboardSvgElement(defs, 'marker', {
                        id: 'tt-whammy-arrow', markerWidth: 7, markerHeight: 7,
                        refX: 6, refY: 3.5, orient: 'auto', markerUnits: 'userSpaceOnUse'
                    });
                    fretboardSvgElement(whammyArrow, 'path', {
                        d: 'M 0 0 L 7 3.5 L 0 7 z', fill: whammyInk
                    });

                    fretboardSvgElement(svg, 'rect', {
                        x: 39, y: boardTop, width: boardEndX - 39,
                        height: boardBottom - boardTop, rx: 8,
                        fill: boardInk,
                        stroke: boardBorderInk,
                        'stroke-width': 1.35
                    });

                    // Phân khoang rất nhẹ; nền không còn vân gỗ cạnh tranh với nốt.
                    for (var cell = 0; cell < visibleFrets; cell++) {
                        if (cell % 2 === 1) {
                            fretboardSvgElement(svg, 'rect', {
                                x: boardStartX + cell * fretWidth,
                                y: boardTop + 1,
                                width: fretWidth,
                                height: boardBottom - boardTop - 2,
                                fill: cellShadeInk,
                                opacity: cellShadeOpacity
                            });
                        }
                    }

                    // Không tô cả khoang ngăn. Chỉ dây thực sự phát sáng để
                    // người học nhận biết nhanh mà không bị quá nhiều màu cạnh tranh.

                    // Dấu mốc 3, 5, 7, 9, 12... chỉ hỗ trợ định vị.
                    var standardMarkers = [3, 5, 7, 9, 12, 15, 17, 19, 21, 24];
                    for (var m = 0; m < standardMarkers.length; m++) {
                        var markerFret = standardMarkers[m];
                        if (markerFret < firstFret || markerFret > lastFret) continue;
                        var markerX = boardStartX +
                            (markerFret - firstFret + .5) * fretWidth;
                        if (markerFret % 12 === 0) {
                            fretboardSvgElement(svg, 'circle', {
                                cx: markerX, cy: topY + (bottomY - topY) * .34,
                                r: 3.2, fill: markerInk, opacity: .55
                            });
                            fretboardSvgElement(svg, 'circle', {
                                cx: markerX, cy: topY + (bottomY - topY) * .66,
                                r: 3.2, fill: markerInk, opacity: .55
                            });
                        } else {
                            fretboardSvgElement(svg, 'circle', {
                                cx: markerX, cy: centerY,
                                r: 3.3, fill: markerInk, opacity: .55
                            });
                        }
                    }

                    var stringStartX = boardStartX - 25;
                    for (var j = 0; j < count; j++) {
                        var y = topY + (bottomY - topY) *
                            (count === 1 ? 0 : j / (count - 1));
                        var tuningPitch = Number(staff.tuning[j]);
                        var rowIsActive = !isPreview && activeRowMap[j] === true;

                        // Tuning labels are fixed reference labels. They never light up,
                        // so an open-string note cannot be mistaken for the label itself.
                        fretboardSvgElement(svg, 'circle', {
                            cx: labelCenterX, cy: y, r: 8.8,
                            fill: labelBubbleInk, stroke: labelBubbleBorderInk, 'stroke-width': 1.2
                        });
                        fretboardSvgElement(svg, 'text', {
                            x: labelCenterX, y: y + 3.3, 'font-size': 9.2,
                            'font-weight': 900, 'text-anchor': 'middle', fill: labelInk
                        }, fretboardNoteLabel(tuningPitch, false));

                        if (rowIsActive) {
                            fretboardSvgElement(svg, 'line', {
                                x1: stringStartX, y1: y,
                                x2: boardEndX, y2: y,
                                stroke: activeInk,
                                'stroke-width': Math.min(6.0, 4.0 + j * .22),
                                opacity: .28,
                                filter: 'url(#tt-note-glow)'
                            });
                        }
                        fretboardSvgElement(svg, 'line', {
                            x1: stringStartX, y1: y,
                            x2: boardEndX, y2: y,
                            stroke: rowIsActive ? activeInk : stringInk,
                            'stroke-width': rowIsActive
                                ? Math.min(3.2, 2.25 + j * .16)
                                : Math.min(2.35, 1.0 + j * .24),
                            opacity: rowIsActive ? 1 : .72
                        });
                        if (!rowIsActive) {
                            fretboardSvgElement(svg, 'line', {
                                x1: stringStartX, y1: y - .42,
                                x2: boardEndX, y2: y - .42,
                                stroke: stringSheenInk, 'stroke-width': .42,
                                opacity: stringSheenOpacity
                            });
                        }
                    }

                    for (var f = 0; f <= visibleFrets; f++) {
                        var fx = boardStartX + f * fretWidth;
                        fretboardSvgElement(svg, 'line', {
                            x1: fx, y1: boardTop + 1,
                            x2: fx, y2: boardBottom - 1,
                            stroke: f === 0 && firstFret === 1 ? nutInk : fretInk,
                            'stroke-width': f === 0 && firstFret === 1 ? 4.2 : 1.35,
                            opacity: f === 0 && firstFret === 1 ? 1 : .9
                        });

                        if (f < visibleFrets) {
                            var fretNumber = firstFret + f;
                            var numberX = fx + fretWidth * .5;
                            fretboardSvgElement(svg, 'text', {
                                x: numberX, y: viewH - 10,
                                'font-size': visibleFrets > 12 ? 8.5 : 10,
                                'text-anchor': 'middle', 'font-weight': 700,
                                fill: secondaryInk
                            }, fretNumber);
                        }
                    }

                    function fretboardXForFret(fret) {
                        if (fret === 0) return openStringX;
                        var rawX = boardStartX + (fret - firstFret + .5) * fretWidth;
                        return Math.max(boardStartX + 2, Math.min(boardEndX - 2, rawX));
                    }

                    function fretboardYForString(stringNumber) {
                        var row = count - Number(stringNumber);
                        if (!Number.isFinite(row) || row < 0 || row >= count) return null;
                        return topY + (bottomY - topY) *
                            (count === 1 ? 0 : row / (count - 1));
                    }

                    var techniqueLabelBoxes = [];
                    function drawTechniquePill(label, centerX, desiredY, ink) {
                        if (!label) return;
                        var width = Math.min(96, Math.max(30, 12 + label.length * 5.2));
                        var height = 15;
                        var x = Math.max(39, Math.min(viewW - width - 4, centerX - width * .5));
                        var minimumY = boardTop + 2;
                        var baseY = Math.max(minimumY, Math.min(boardBottom - height - 2, desiredY));
                        var y = baseY;
                        var offsets = [0, -16, 16, -32, 32, -48, 48, -64, 64];
                        for (var attempt = 0; attempt < offsets.length; attempt++) {
                            y = Math.max(
                                minimumY,
                                Math.min(boardBottom - height - 2, baseY + offsets[attempt])
                            );
                            var overlaps = false;
                            for (var b = 0; b < techniqueLabelBoxes.length; b++) {
                                var box = techniqueLabelBoxes[b];
                                if (!(x + width + 2 < box.x || x > box.x + box.w + 2 ||
                                    y + height + 2 < box.y || y > box.y + box.h + 2)) {
                                    overlaps = true;
                                    break;
                                }
                            }
                            if (!overlaps) break;
                        }
                        techniqueLabelBoxes.push({ x: x, y: y, w: width, h: height });
                        fretboardSvgElement(svg, 'rect', {
                            x: x, y: y, width: width, height: height, rx: 6.5,
                            fill: techniquePillInk, stroke: ink, 'stroke-width': 1.1,
                            opacity: techniquePillOpacity
                        });
                        fretboardSvgElement(svg, 'text', {
                            x: x + width * .5, y: y + 10.6,
                            'text-anchor': 'middle', 'font-size': 8.2,
                            'font-weight': 900, fill: ink
                        }, label);
                    }

                    function drawHorizontalWave(x1, x2, y, amplitude, cycles, ink, width) {
                        var startX = Math.min(x1, x2);
                        var endX = Math.max(x1, x2);
                        if (endX - startX < 8) endX = startX + 8;
                        var segments = Math.max(4, Math.round(cycles * 2));
                        var segmentWidth = (endX - startX) / segments;
                        var path = 'M ' + startX + ' ' + y;
                        for (var waveIndex = 0; waveIndex < segments; waveIndex++) {
                            var controlX = startX + segmentWidth * (waveIndex + .5);
                            var endSegmentX = startX + segmentWidth * (waveIndex + 1);
                            var controlY = y + (waveIndex % 2 === 0 ? -amplitude : amplitude);
                            path += ' Q ' + controlX + ' ' + controlY + ' ' + endSegmentX + ' ' + y;
                        }
                        fretboardSvgElement(svg, 'path', {
                            d: path, fill: 'none', stroke: ink,
                            'stroke-width': width || 1.8,
                            'stroke-linecap': 'round', 'stroke-linejoin': 'round'
                        });
                    }

                    function drawConnectionTechnique(effect, pointX, pointY, pointRadius, ink) {
                        if (!effect) return;
                        var source = effect.phase === 'in' ? effect.origin : null;
                        var target = effect.phase === 'out' ? effect.target : null;
                        var sx = source ? fretboardXForFret(source.fret) : pointX;
                        var sy = source ? fretboardYForString(source.stringNumber) : pointY;
                        var ex = target ? fretboardXForFret(target.fret) : pointX;
                        var ey = target ? fretboardYForString(target.stringNumber) : pointY;
                        if (sy == null) sy = pointY;
                        if (ey == null) ey = pointY;
                        if (!source && effect.phase === 'in') sx = pointX - fretWidth * 1.15;
                        if (!target && effect.phase === 'out') ex = pointX + fretWidth * 1.15;
                        if (Math.abs(ex - sx) < 5 && Math.abs(ey - sy) < 5) ex = sx + fretWidth * 1.15;
                        var direction = ex >= sx ? 1 : -1;
                        if (!source) sx += direction * pointRadius * .68;
                        if (!target) ex -= direction * 3;
                        var controlX = (sx + ex) * .5;
                        var controlY = Math.min(sy, ey) - 16;
                        fretboardSvgElement(svg, 'path', {
                            d: 'M ' + sx + ' ' + sy + ' Q ' + controlX + ' ' + controlY + ' ' + ex + ' ' + ey,
                            fill: 'none', stroke: ink, 'stroke-width': 2,
                            'stroke-linecap': 'round', 'stroke-linejoin': 'round',
                            'marker-end': effect.label === 'Tie' ? '' : 'url(#tt-legato-arrow)'
                        });
                        drawTechniquePill(effect.label, controlX, controlY - 12, ink);
                    }

                    var topBadgeBoxes = [];
                    function reserveTopBadgeBox(box) {
                        if (box) topBadgeBoxes.push(box);
                    }
                    function drawTopTechniqueBadge(item) {
                        if (!item || !item.label) return;
                        var label = String(item.label);
                        var ink = item.ink || secondaryInk;
                        var width = Math.min(78, Math.max(26, 10 + label.length * 4.7));
                        var height = 13;
                        var rows = [2, 17, boardTop + 3, boardTop + 18];
                        var chosen = null;
                        for (var rowIndex = 0; rowIndex < rows.length && !chosen; rowIndex++) {
                            for (var scanX = 3; scanX <= viewW - width - 3; scanX += 3) {
                                var candidate = { x: scanX, y: rows[rowIndex], w: width, h: height };
                                var blocked = false;
                                for (var tb = 0; tb < topBadgeBoxes.length; tb++) {
                                    var occupied = topBadgeBoxes[tb];
                                    if (!(candidate.x + candidate.w + 2 < occupied.x ||
                                        candidate.x > occupied.x + occupied.w + 2 ||
                                        candidate.y + candidate.h + 1 < occupied.y ||
                                        candidate.y > occupied.y + occupied.h + 1)) {
                                        blocked = true;
                                        break;
                                    }
                                }
                                if (!blocked) {
                                    chosen = candidate;
                                    break;
                                }
                            }
                        }
                        if (!chosen) chosen = { x: 3, y: boardTop + 3, w: width, h: height };
                        reserveTopBadgeBox(chosen);
                        fretboardSvgElement(svg, 'rect', {
                            x: chosen.x, y: chosen.y, width: chosen.w, height: chosen.h,
                            rx: 6, fill: techniquePillInk, stroke: ink,
                            'stroke-width': .95, opacity: techniquePillOpacity
                        });
                        fretboardSvgElement(svg, 'text', {
                            x: chosen.x + chosen.w * .5,
                            y: chosen.y + 9.2,
                            'text-anchor': 'middle', 'font-size': 7.2,
                            'font-weight': 900, fill: ink
                        }, label);
                    }

                    if (beatTechniques.barre && beatTechniques.barre.fret >= firstFret &&
                        beatTechniques.barre.fret <= lastFret) {
                        var barreX = fretboardXForFret(beatTechniques.barre.fret);
                        var barreY1 = topY - 8;
                        var barreY2 = beatTechniques.barre.half
                            ? topY + (bottomY - topY) * .52
                            : bottomY + 8;
                        fretboardSvgElement(svg, 'rect', {
                            x: barreX - 5.5, y: barreY1,
                            width: 11, height: Math.max(12, barreY2 - barreY1), rx: 5.5,
                            fill: fretboardThemeInk('#c084fc', '#7e22ce'), opacity: .20,
                            stroke: fretboardThemeInk('#c084fc', '#7e22ce'), 'stroke-width': 1.5
                        });
                    }

                    if (beatTechniques.brush && highlighted.length > 1) {
                        var brushRows = highlighted.map(function(point) { return point.row; });
                        var brushMinRow = Math.min.apply(null, brushRows);
                        var brushMaxRow = Math.max.apply(null, brushRows);
                        var brushTopY = topY + (bottomY - topY) *
                            (count === 1 ? 0 : brushMinRow / (count - 1));
                        var brushBottomY = topY + (bottomY - topY) *
                            (count === 1 ? 0 : brushMaxRow / (count - 1));
                        var brushX = boardEndX - 7;
                        var brushSymbol = beatTechniques.brush.direction === 'up' ? '↑' : '↓';
                        fretboardSvgElement(svg, 'line', {
                            x1: brushX, y1: brushTopY, x2: brushX, y2: brushBottomY,
                            stroke: rhythmInk, 'stroke-width': 2.2,
                            'stroke-dasharray': beatTechniques.brush.arpeggio ? '3 2' : '',
                            opacity: .95
                        });
                        fretboardSvgElement(svg, 'text', {
                            x: brushX - 1,
                            y: beatTechniques.brush.direction === 'up' ? brushTopY - 4 : brushBottomY + 10,
                            'text-anchor': 'middle', 'font-size': 15,
                            'font-weight': 900, fill: rhythmInk
                        }, brushSymbol);
                    }

                    var chordMinX = Infinity;
                    var chordMaxX = -Infinity;
                    var renderedPoints = [];
                    for (var k = 0; k < highlighted.length; k++) {
                        var point = highlighted[k];
                        if (point.fret > 0 &&
                            (point.fret < firstFret || point.fret > lastFret)) continue;

                        var px = fretboardXForFret(point.fret);
                        var py = topY + (bottomY - topY) *
                            (count === 1 ? 0 : point.row / (count - 1));
                        var radius = Math.min(12.4, Math.max(9.4, fretWidth * .38));
                        chordMinX = Math.min(chordMinX, px);
                        chordMaxX = Math.max(chordMaxX, px);
                        renderedPoints.push({
                            point: point, px: px, py: py, radius: radius
                        });
                    }

                    if (beatTechniques.whammy && renderedPoints.length > 0) {
                        var whammyStartX = Math.max(boardStartX + 4, chordMinX - 8);
                        var whammyEndX = Math.min(boardEndX - 4, Math.max(chordMaxX + 28, whammyStartX + 48));
                        var whammyBaseY = boardTop + 10;
                        var whammyPath = '';
                        var whammyType = beatTechniques.whammy.type;
                        if (whammyType === 3) {
                            whammyPath = 'M ' + whammyStartX + ' ' + whammyBaseY +
                                ' Q ' + ((whammyStartX + whammyEndX) * .5) + ' ' + (whammyBaseY + 15) +
                                ' ' + whammyEndX + ' ' + whammyBaseY;
                        } else if (whammyType === 4) {
                            whammyPath = 'M ' + whammyStartX + ' ' + (whammyBaseY + 7) +
                                ' L ' + whammyEndX + ' ' + (whammyBaseY + 7);
                        } else if (whammyType === 5) {
                            whammyPath = 'M ' + whammyStartX + ' ' + (whammyBaseY + 13) +
                                ' Q ' + ((whammyStartX + whammyEndX) * .5) + ' ' + whammyBaseY +
                                ' ' + whammyEndX + ' ' + whammyBaseY;
                        } else if (whammyType === 6) {
                            whammyPath = 'M ' + whammyStartX + ' ' + (whammyBaseY + 13) +
                                ' Q ' + ((whammyStartX + whammyEndX) * .45) + ' ' + (whammyBaseY + 2) +
                                ' ' + whammyEndX + ' ' + (whammyBaseY + 16);
                        } else {
                            whammyPath = 'M ' + whammyStartX + ' ' + whammyBaseY +
                                ' Q ' + ((whammyStartX + whammyEndX) * .5) + ' ' + (whammyBaseY + 2) +
                                ' ' + whammyEndX + ' ' + (whammyBaseY + 15);
                        }
                        fretboardSvgElement(svg, 'path', {
                            d: whammyPath, fill: 'none', stroke: whammyInk,
                            'stroke-width': 2.2, 'stroke-linecap': 'round',
                            'stroke-linejoin': 'round',
                            'marker-end': whammyType === 4 ? '' : 'url(#tt-whammy-arrow)'
                        });
                    }

                    // Technique paths are drawn first. Note heads are drawn afterward,
                    // so every line appears to start behind the fingertip marker.
                    for (var t = 0; t < renderedPoints.length; t++) {
                        var rendered = renderedPoints[t];
                        var techniquePoint = rendered.point;
                        var tx0 = rendered.px;
                        var ty0 = rendered.py;
                        var tr = rendered.radius;
                        var slides = techniquePoint.slides || [];

                        for (var sl = 0; sl < slides.length; sl++) {
                            var slide = slides[sl];
                            var sx = tx0;
                            var sy = ty0;
                            var ex = tx0;
                            var ey = ty0;
                            var dash = slide.pick ? '4 3' : '';

                            if (slide.phase === 'out-target' && slide.target) {
                                ex = fretboardXForFret(slide.target.fret);
                                var targetY = fretboardYForString(slide.target.stringNumber);
                                ey = targetY == null ? ty0 : targetY;
                                var outDirection = ex >= tx0 ? 1 : -1;
                                sx = tx0 + outDirection * tr * .72;
                                ex -= outDirection * 5;
                            } else if (slide.phase === 'in-target' && slide.origin) {
                                sx = fretboardXForFret(slide.origin.fret);
                                var originY = fretboardYForString(slide.origin.stringNumber);
                                sy = originY == null ? ty0 : originY;
                                var inDirection = tx0 >= sx ? 1 : -1;
                                sx += inDirection * 5;
                                ex = tx0 - inDirection * tr * .72;
                                ey = ty0;
                            } else if (slide.phase === 'in-free') {
                                sx = tx0 - 29;
                                sy = ty0 + (slide.direction === 'below' ? 15 : -15);
                                ex = tx0 - tr * .72;
                                ey = ty0;
                            } else {
                                sx = tx0 + tr * .72;
                                sy = ty0;
                                ex = tx0 + 31;
                                ey = ty0 + (slide.direction === 'down' ? 15 : -15);
                            }

                            var midX = (sx + ex) * .5;
                            var midY = (sy + ey) * .5;
                            fretboardSvgElement(svg, 'path', {
                                d: 'M ' + sx + ' ' + sy +
                                    ' Q ' + midX + ' ' + (midY - 4) + ' ' + ex + ' ' + ey,
                                fill: 'none', stroke: slideInk, 'stroke-width': 2.25,
                                'stroke-linecap': 'round', 'stroke-linejoin': 'round',
                                'stroke-dasharray': dash, 'marker-end': 'url(#tt-slide-arrow)'
                            });
                            drawTechniquePill(
                                slide.label,
                                midX,
                                Math.min(sy, ey) - 20,
                                slideInk
                            );
                        }

                        var bend = techniquePoint.bend;
                        if (bend) {
                            var bendPath = '';
                            var bendLabelX = tx0 + 21;
                            var bendLabelY = ty0 - 36;
                            if (bend.shape === 'down') {
                                bendPath = 'M ' + (tx0 + 4) + ' ' + (ty0 - 31) +
                                    ' C ' + (tx0 + 16) + ' ' + (ty0 - 29) + ', ' +
                                    (tx0 + 22) + ' ' + (ty0 - 16) + ', ' +
                                    (tx0 + 23) + ' ' + (ty0 - tr * .72);
                            } else if (bend.shape === 'updown') {
                                bendPath = 'M ' + (tx0 + 3) + ' ' + (ty0 - tr * .72) +
                                    ' C ' + (tx0 + 10) + ' ' + (ty0 - 33) + ', ' +
                                    (tx0 + 20) + ' ' + (ty0 - 34) + ', ' +
                                    (tx0 + 25) + ' ' + (ty0 - 23) +
                                    ' C ' + (tx0 + 29) + ' ' + (ty0 - 14) + ', ' +
                                    (tx0 + 31) + ' ' + (ty0 - 11) + ', ' +
                                    (tx0 + 34) + ' ' + (ty0 - tr * .5);
                                bendLabelX = tx0 + 18;
                                bendLabelY = ty0 - 50;
                            } else if (bend.shape === 'hold') {
                                bendPath = 'M ' + (tx0 + tr * .72) + ' ' + (ty0 - 12) +
                                    ' L ' + (tx0 + 34) + ' ' + (ty0 - 12);
                                bendLabelY = ty0 - 34;
                            } else if (bend.shape === 'preup') {
                                bendPath = 'M ' + tx0 + ' ' + (ty0 - tr * .72) +
                                    ' C ' + (tx0 + 1) + ' ' + (ty0 - 19) + ', ' +
                                    (tx0 + 7) + ' ' + (ty0 - 26) + ', ' +
                                    (tx0 + 13) + ' ' + (ty0 - 33);
                                bendLabelX = tx0 + 8;
                                bendLabelY = ty0 - 50;
                            } else {
                                bendPath = 'M ' + (tx0 + 3) + ' ' + (ty0 - tr * .72) +
                                    ' C ' + (tx0 + 8) + ' ' + (ty0 - 20) + ', ' +
                                    (tx0 + 16) + ' ' + (ty0 - 27) + ', ' +
                                    (tx0 + 24) + ' ' + (ty0 - 33);
                            }
                            fretboardSvgElement(svg, 'path', {
                                d: bendPath, fill: 'none', stroke: bendInk,
                                'stroke-width': 2.35, 'stroke-linecap': 'round',
                                'stroke-linejoin': 'round',
                                'marker-end': bend.shape === 'hold' ? '' : 'url(#tt-bend-arrow)'
                            });
                            drawTechniquePill(bend.label, bendLabelX, bendLabelY, bendInk);
                        }

                        var pointEffects = techniquePoint.effects || {};
                        drawConnectionTechnique(pointEffects.hammerPull, tx0, ty0, tr, legatoInk);
                        drawConnectionTechnique(pointEffects.slur, tx0, ty0, tr, slurInk);
                        drawConnectionTechnique(pointEffects.tie, tx0, ty0, tr, tieInk);

                        if (pointEffects.trill) {
                            var trillTargetX = pointEffects.trill.targetFret == null
                                ? tx0 + fretWidth * 1.15
                                : fretboardXForFret(pointEffects.trill.targetFret);
                            if (Math.abs(trillTargetX - tx0) < 8) trillTargetX = tx0 + fretWidth;
                            var trillY = ty0 - tr - 8;
                            drawHorizontalWave(
                                tx0 + tr * .55,
                                trillTargetX,
                                trillY,
                                3.2,
                                4,
                                trillInk,
                                1.9
                            );
                            drawTechniquePill(
                                pointEffects.trill.label,
                                (tx0 + trillTargetX) * .5,
                                trillY - 19,
                                trillInk
                            );
                        }

                        if (pointEffects.vibrato) {
                            var vibratoY = ty0 - tr - 5;
                            drawHorizontalWave(
                                tx0 - tr * .75,
                                tx0 + tr * .9,
                                vibratoY,
                                pointEffects.vibrato.wide ? 4 : 2.5,
                                pointEffects.vibrato.wide ? 3 : 4,
                                vibratoInk,
                                1.8
                            );
                            drawTechniquePill(
                                pointEffects.vibrato.label,
                                tx0,
                                vibratoY - 19,
                                vibratoInk
                            );
                        }
                    }

                    for (var n = 0; n < renderedPoints.length; n++) {
                        var renderedNote = renderedPoints[n];
                        var notePoint = renderedNote.point;
                        var noteX = renderedNote.px;
                        var noteY = renderedNote.py;
                        var noteRadius = renderedNote.radius;

                        var effects = notePoint.effects || {};
                        var noteFill = effects.dead ? deadNoteFillInk : noteInk;
                        var noteStroke = effects.ghost ? ghostInk : noteOutlineInk;
                        var noteOpacity = effects.ghost ? .68 : 1;

                        fretboardSvgElement(svg, 'circle', {
                            cx: noteX, cy: noteY, r: noteRadius + 6.2,
                            fill: noteFill,
                            opacity: isPreview ? .14 : (effects.ghost ? .16 : .28),
                            filter: 'url(#tt-note-glow)'
                        });

                        if (effects.harmonic) {
                            var diamond = [
                                noteX + ',' + (noteY - noteRadius),
                                (noteX + noteRadius) + ',' + noteY,
                                noteX + ',' + (noteY + noteRadius),
                                (noteX - noteRadius) + ',' + noteY
                            ].join(' ');
                            fretboardSvgElement(svg, 'polygon', {
                                points: diamond, fill: noteFill,
                                stroke: noteStroke, 'stroke-width': 2.15,
                                opacity: noteOpacity
                            });
                        } else {
                            fretboardSvgElement(svg, 'circle', {
                                cx: noteX, cy: noteY, r: noteRadius,
                                fill: noteFill,
                                stroke: effects.dead ? activeInk : noteStroke,
                                'stroke-width': 2.15,
                                'stroke-dasharray': effects.ghost ? '3 2' : '',
                                opacity: noteOpacity
                            });
                        }

                        fretboardSvgElement(svg, 'text', {
                            x: noteX, y: noteY + 3.4, 'text-anchor': 'middle',
                            'font-size': effects.dead ? 13 : (noteRadius < 11 ? 8.2 : 9.3),
                            'font-weight': 900,
                            fill: effects.dead ? activeInk : activeTextInk
                        }, effects.dead ? '×' : notePoint.name);

                        if (effects.ghost) {
                            fretboardSvgElement(svg, 'text', {
                                x: noteX - noteRadius - 3, y: noteY + 3.2,
                                'text-anchor': 'middle', 'font-size': 12,
                                'font-weight': 800, fill: ghostInk
                            }, '(');
                            fretboardSvgElement(svg, 'text', {
                                x: noteX + noteRadius + 3, y: noteY + 3.2,
                                'text-anchor': 'middle', 'font-size': 12,
                                'font-weight': 800, fill: ghostInk
                            }, ')');
                        }

                        if (effects.staccato) {
                            fretboardSvgElement(svg, 'circle', {
                                cx: noteX, cy: noteY + noteRadius + 5,
                                r: 1.8, fill: accentInk
                            });
                        }
                        if (effects.accent) {
                            fretboardSvgElement(svg, 'text', {
                                x: noteX, y: noteY - noteRadius - 5,
                                'text-anchor': 'middle', 'font-size': 10,
                                'font-weight': 900, fill: accentInk
                            }, effects.accent.symbol);
                        }

                        if (beatTechniques.tremolo) {
                            var tremolo = beatTechniques.tremolo;
                            if (tremolo.buzz) {
                                fretboardSvgElement(svg, 'text', {
                                    x: noteX + noteRadius + 5,
                                    y: noteY - noteRadius + 2,
                                    'font-size': 10, 'font-weight': 900,
                                    fill: trillInk
                                }, 'Z');
                            } else if (tremolo.marks > 0) {
                                var tremoloBaseX = noteX - Math.min(8, tremolo.marks * 2.2);
                                var tremoloY = noteY - noteRadius - 4;
                                for (var tm = 0; tm < tremolo.marks; tm++) {
                                    var markX = tremoloBaseX + tm * 4;
                                    fretboardSvgElement(svg, 'line', {
                                        x1: markX, y1: tremoloY + 3,
                                        x2: markX + 5, y2: tremoloY - 3,
                                        stroke: trillInk, 'stroke-width': 1.8,
                                        'stroke-linecap': 'round'
                                    });
                                }
                            } else {
                                fretboardSvgElement(svg, 'text', {
                                    x: noteX + noteRadius + 4,
                                    y: noteY - noteRadius + 1,
                                    'font-size': 8.5, 'font-weight': 900,
                                    fill: trillInk
                                }, 'TR');
                            }
                        }

                        var effectTags = effects.tags || [];
                        for (var tagIndex = 0; tagIndex < effectTags.length; tagIndex++) {
                            var tag = effectTags[tagIndex];
                            var tagY = tagIndex % 2 === 0
                                ? noteY + noteRadius + 8 + Math.floor(tagIndex / 2) * 17
                                : noteY - noteRadius - 25 - Math.floor(tagIndex / 2) * 17;
                            drawTechniquePill(tag.label, noteX, tagY, tag.ink);
                        }
                    }

                    // Chỉ hiển thị tên (ví dụ: Am), đặt ngay phía trên cụm vị trí bấm.
                    // Không thêm tiền tố "Hợp âm" và không lặp tên ở dòng trạng thái.
                    if (chordInfo && chordInfo.name &&
                        Number.isFinite(chordMinX) && Number.isFinite(chordMaxX)) {
                        var chordText = String(chordInfo.name);
                        var chordCenterX = (chordMinX + chordMaxX) * .5;
                        var chordLabelWidth = Math.min(
                            102,
                            Math.max(36, 19 + chordText.length * 8.4)
                        );
                        var chordLabelX = Math.max(
                            4,
                            Math.min(viewW - chordLabelWidth - 5,
                                chordCenterX - chordLabelWidth * .5)
                        );
                        var chordPointerX = Math.max(
                            chordLabelX + 9,
                            Math.min(chordLabelX + chordLabelWidth - 9, chordCenterX)
                        );
                        var chordBox = {
                            x: chordLabelX, y: 3,
                            w: chordLabelWidth, h: 25
                        };
                        reserveTopBadgeBox(chordBox);
                        var chordBadgeInk = normalizeVisualHex(
                            currentVisualPreferences.cursorColor,
                            currentThemeIsDark ? '#C084FC' : '#7C3AED'
                        );
                        var chordBadgeBorder = visualMix(
                            chordBadgeInk,
                            visualTextColor(chordBadgeInk),
                            .42
                        );
                        fretboardSvgElement(svg, 'line', {
                            x1: chordPointerX, y1: 28,
                            x2: Math.max(39, Math.min(boardEndX, chordCenterX)), y2: boardTop - 2,
                            stroke: chordBadgeBorder, 'stroke-width': 1.4, opacity: .9
                        });
                        fretboardSvgElement(svg, 'rect', {
                            x: chordLabelX, y: 3,
                            width: chordLabelWidth, height: 25, rx: 12.5,
                            fill: chordBadgeInk, stroke: chordBadgeBorder,
                            'stroke-width': 1.2
                        });
                        fretboardSvgElement(svg, 'text', {
                            x: chordLabelX + chordLabelWidth * .5,
                            y: 20, 'text-anchor': 'middle',
                            'font-size': 13, 'font-weight': 900,
                            fill: visualTextColor(chordBadgeInk)
                        }, chordText);
                    }

                    for (var badgeIndex = 0; badgeIndex < beatTechniques.labels.length; badgeIndex++) {
                        drawTopTechniqueBadge(beatTechniques.labels[badgeIndex]);
                    }

                    if (!currentBeat) {
                        status.textContent = fretboardLabels.idle;
                    } else if (highlighted.length === 0) {
                        status.textContent = fretboardLabels.rest;
                    } else if (isPreview) {
                        status.textContent = fretboardLabels.preview;
                    } else if (chordInfo && chordInfo.name) {
                        // Tên hợp âm đã hiển thị trong SVG phía trên thế bấm.
                        status.textContent = '';
                    } else if (highlighted.length === 1) {
                        var single = highlighted[0];
                        var singleEffects = single.effects || {};
                        var singleLabel = singleEffects.dead
                            ? 'Dead note'
                            : (singleEffects.ghost ? 'Ghost ' + single.name : fretboardLabels.note + ' ' + single.name);
                        status.textContent = singleLabel + ' • ' +
                            fretboardLabels.string + ' ' + single.stringNumber + ' • ' +
                            fretboardLabels.fret + ' ' + single.fret;
                    } else {
                        // Nhiều nốt nhưng đang arpeggio hoặc không đủ chắc chắn để đặt tên hợp âm.
                        status.textContent = fretboardLabels.active + ' • ' +
                            highlighted.length + ' ' + fretboardLabels.notes;
                    }
                }

                function updateFretboardForBeat(beat) {
                    // playedBeatChanged emits Beat directly, but accept a wrapped event as
                    // a defensive fallback for older/custom alphaTab builds.
                    if (beat && !beat.notes && beat.beat) beat = beat.beat;
                    if (beat && !beatBelongsToSelectedTrack(beat, selectedTrackForNoteNames())) return;
                    fretboardLatestBeat = beat || null;
                    if (fretboardEnabled) {
                        scheduleFretboardRefresh();
                        requestAnimationFrame(function() {
                            keepScoreCursorNearFretboard(false);
                        });
                    }
                }

                function closeFretboardPanel() {
                    if (setStaveDisplayMode('scoretab') && window.AndroidInterface &&
                        typeof window.AndroidInterface.onFretboardClosed === 'function') {
                        window.AndroidInterface.onFretboardClosed();
                    }
                }

                function setStaveDisplayMode(mode) {
                    if (!api || !api.score || isPdfExportMode) return false;
                    clearTimeout(scoreZoomRenderTimer);
                    scoreZoomScrollAnchor = null;

                    var normalizedMode = String(mode || 'scoretab').toLowerCase();
                    if (
                        normalizedMode !== 'scoretab' &&
                        normalizedMode !== 'scoretab_fretboard' &&
                        normalizedMode !== 'tab' &&
                        normalizedMode !== 'score'
                    ) {
                        normalizedMode = 'scoretab';
                    }

                    // TAB-only dùng từng ký hiệu trường độ riêng ở phía dưới khuông.
                    // Phần thân nốt sẽ được rút ngắn bằng custom renderer bên dưới,
                    // nhờ đó không còn đường dọc chạy xuyên qua dây và số ngăn đàn.
                    // Ở chế độ có khuông nhạc, dùng Automatic để tránh lặp ký hiệu tiết tấu.
                    var tabRhythmMode = normalizedMode === 'tab'
                        ? 'ShowWithBars'
                        : 'Automatic';
                    var tabRhythmHeight = 25;
                    var nextFretboardEnabled = normalizedMode === 'scoretab_fretboard';
                    var staveMode = nextFretboardEnabled ? 'scoretab' : normalizedMode;
                    var previousFretboardEnabled = fretboardEnabled;

                    try {
                        var wrapper = document.getElementById('scroll-wrapper');
                        var previousScrollTop = wrapper ? wrapper.scrollTop : 0;

                        if (
                            api.settings &&
                            typeof api.settings.fillFromJson === 'function'
                        ) {
                            api.settings.fillFromJson({
                                display: {
                                    staveProfile: staveMode,
                                    scale: scoreZoomScaleForMode(nextFretboardEnabled),
                                    systemPaddingBottom: noteNamesVisible
                                        ? noteNameSystemPaddingBottom
                                        : defaultSystemPaddingBottom,
                                    lastSystemPaddingBottom: noteNamesVisible
                                        ? noteNameLastSystemPaddingBottom
                                        : defaultLastSystemPaddingBottom
                                },
                                notation: {
                                    rhythmMode: tabRhythmMode,
                                    rhythmHeight: tabRhythmHeight,
                                    elements: { scoreTitle: true }
                                }
                            });
                        } else if (
                            api.settings &&
                            api.settings.display &&
                            typeof alphaTab !== 'undefined' &&
                            alphaTab.StaveProfile
                        ) {
                            api.settings.display.staveProfile =
                                staveMode === 'tab'
                                    ? alphaTab.StaveProfile.Tab
                                    : staveMode === 'score'
                                        ? alphaTab.StaveProfile.Score
                                        : alphaTab.StaveProfile.ScoreTab;
                            api.settings.display.scale = scoreZoomScaleForMode(nextFretboardEnabled);
                            api.settings.display.systemPaddingBottom = noteNamesVisible
                                ? noteNameSystemPaddingBottom
                                : defaultSystemPaddingBottom;
                            api.settings.display.lastSystemPaddingBottom = noteNamesVisible
                                ? noteNameLastSystemPaddingBottom
                                : defaultLastSystemPaddingBottom;

                            if (api.settings.notation) {
                                ensureScoreTitleSettingVisible();
                                if (alphaTab.TabRhythmMode) {
                                    api.settings.notation.rhythmMode =
                                        normalizedMode === 'tab'
                                            ? alphaTab.TabRhythmMode.ShowWithBars
                                            : alphaTab.TabRhythmMode.Automatic;
                                } else {
                                    api.settings.notation.rhythmMode = tabRhythmMode;
                                }
                                api.settings.notation.rhythmHeight = tabRhythmHeight;
                            }
                        } else {
                            return false;
                        }

                        if (nextFretboardEnabled && !previousFretboardEnabled) {
                            fretboardWindowStart = 1;
                        }
                        fretboardEnabled = nextFretboardEnabled;
                        syncWebViewViewportBox(
                            lastAndroidViewportWidth,
                            lastAndroidViewportHeight
                        );
                        api.updateSettings();
                        applySelectedTrackSolo(true);
                        if (fretboardEnabled) {
                            notifyTabBannerVisibility(false);
                            scheduleFretboardRefresh();
                            setTimeout(function() {
                                keepScoreCursorNearFretboard(true);
                            }, 360);
                        } else {
                            notifyTabBannerVisibility(false);
                        }

                        // Sau khi render lại, giữ vị trí cuộn gần nhất để người dùng
                        // không bị đưa đột ngột về đầu bản nhạc.
                        setTimeout(function() {
                            var currentWrapper = document.getElementById('scroll-wrapper');
                            if (currentWrapper) {
                                var maxScroll = Math.max(
                                    0,
                                    currentWrapper.scrollHeight - currentWrapper.clientHeight
                                );
                                currentWrapper.scrollTop = Math.min(previousScrollTop, maxScroll);
                            }
                        }, 300);

                        return true;
                    } catch (error) {
                        fretboardEnabled = previousFretboardEnabled;
                        syncWebViewViewportBox(
                            lastAndroidViewportWidth,
                            lastAndroidViewportHeight
                        );
                        console.warn('Could not change stave display mode', error);
                        return false;
                    }
                }

                function resetSoundFontCompatibilityState() {
                    soundFontCompatibilityCheckToken++;
                    soundFontCompatibilityCache = {};
                    if (soundFontCompatibilityCheckInProgress) {
                        soundFontCompatibilityCheckInProgress = false;
                        AndroidInterface.onSoundFontCompatibilityCheckChanged(false);
                    }
                }

                function applySelectedTrackSolo(shouldRender) {
                    if (!api || !currentTracks || currentTracks.length === 0) return;

                    var safeIndex = Math.max(
                        0,
                        Math.min(selectedTrackForPlayback, currentTracks.length - 1)
                    );
                    selectedTrackForPlayback = safeIndex;

                    var selectedTrack = currentTracks[safeIndex];
                    api.changeTrackSolo(currentTracks, false);
                    if (shouldRender) {
                        api.renderTracks([selectedTrack]);
                    }
                    api.changeTrackSolo([selectedTrack], true);
                    if (fretboardEnabled) scheduleFretboardRefresh();
                }

                function selectedTrackNoteRange() {
                    if (!currentTracks || currentTracks.length === 0) return null;
                    var track = currentTracks[selectedTrackForPlayback];
                    if (!track || !track.staves) return null;

                    // Track bi mute hoac volume bang 0 se im lang voi moi SoundFont. Khong coi
                    // day la loi tuong thich de tranh chuyen cau hinh sai nguyen nhan.
                    var playbackInfo = track.playbackInfo;
                    if (
                        playbackInfo &&
                        (playbackInfo.isMute === true || Number(playbackInfo.volume) <= 0)
                    ) {
                        return null;
                    }

                    var firstTick = Number.POSITIVE_INFINITY;
                    var lastTick = 0;
                    var firstDuration = 0;
                    var noteCount = 0;

                    for (var staffIndex = 0; staffIndex < track.staves.length; staffIndex++) {
                        var staff = track.staves[staffIndex];
                        if (!staff || !staff.bars) continue;

                        for (var barIndex = 0; barIndex < staff.bars.length; barIndex++) {
                            var bar = staff.bars[barIndex];
                            if (!bar || !bar.voices) continue;

                            for (var voiceIndex = 0; voiceIndex < bar.voices.length; voiceIndex++) {
                                var voice = bar.voices[voiceIndex];
                                if (!voice || !voice.beats) continue;

                                for (var beatIndex = 0; beatIndex < voice.beats.length; beatIndex++) {
                                    var beat = voice.beats[beatIndex];
                                    if (!beat || !beat.notes || beat.notes.length === 0) continue;

                                    var tick = Number(beat.absolutePlaybackStart);
                                    if (!Number.isFinite(tick)) continue;
                                    var duration = Number(beat.playbackDuration);
                                    if (!Number.isFinite(duration) || duration <= 0) duration = 960;

                                    if (tick < firstTick) {
                                        firstTick = tick;
                                        firstDuration = duration;
                                    }
                                    lastTick = Math.max(lastTick, tick + duration);
                                    noteCount += beat.notes.length;
                                }
                            }
                        }
                    }

                    if (!Number.isFinite(firstTick) || noteCount <= 0) return null;

                    // Bat dau ngay truoc not dau tien de khong nham doan nghi dau bai la loi.
                    var startTick = Math.max(0, firstTick - Math.max(60, firstDuration * 0.10));
                    var minimumSpan = Math.max(3840, firstDuration * 8);
                    var maximumEnd = startTick + 15360;
                    var endTick = Math.min(Math.max(lastTick + 960, startTick + minimumSpan), maximumEnd);
                    if (!Number.isFinite(endTick) || endTick <= startTick) {
                        endTick = startTick + 7680;
                    }

                    return {
                        startTick: startTick,
                        endTick: endTick,
                        noteCount: noteCount
                    };
                }

                function containsAudibleSamples(samples) {
                    if (!samples || samples.length === 0) return false;

                    var peak = 0;
                    var energy = 0;
                    var count = 0;
                    for (var i = 0; i < samples.length; i++) {
                        var value = Number(samples[i]);
                        if (!Number.isFinite(value)) continue;
                        var absolute = Math.abs(value);
                        if (absolute > peak) peak = absolute;
                        energy += value * value;
                        count++;
                    }

                    if (count === 0) return false;
                    var rms = Math.sqrt(energy / count);
                    // Preset bi thieu tra ve mau bang 0. Nguong rat thap de khong loai nham not pianissimo.
                    return peak >= 0.00001 || rms >= 0.000001;
                }

                async function verifySelectedSoundFontProducesAudio() {
                    if (!api || !api.score || activeSoundFontMode === 'standard') return true;
                    if (typeof api.exportAudio !== 'function') return true;

                    var noteRange = selectedTrackNoteRange();
                    // Track khong co not la van de noi dung/track, khong phai loi SoundFont.
                    if (!noteRange) return true;

                    var cacheKey = [
                        soundFontCompatibilityScoreGeneration,
                        selectedTrackForPlayback,
                        activeSoundFontMode
                    ].join('|');
                    if (Object.prototype.hasOwnProperty.call(soundFontCompatibilityCache, cacheKey)) {
                        return soundFontCompatibilityCache[cacheKey] === true;
                    }

                    var checkToken = ++soundFontCompatibilityCheckToken;
                    soundFontCompatibilityCheckInProgress = true;
                    AndroidInterface.onSoundFontCompatibilityCheckChanged(true);
                    var exporter = null;

                    try {
                        api.stop();
                        var options = new alphaTab.synth.AudioExportOptions();
                        options.sampleRate = 22050;
                        options.masterVolume = 1.0;
                        options.metronomeVolume = 0.0;
                        options.useSyncPoints = false;
                        options.playbackRange = {
                            startTick: noteRange.startTick,
                            endTick: noteRange.endTick
                        };

                        if (options.trackVolume && typeof options.trackVolume.set === 'function') {
                            for (var trackIndex = 0; trackIndex < currentTracks.length; trackIndex++) {
                                options.trackVolume.set(
                                    trackIndex,
                                    trackIndex === selectedTrackForPlayback ? 1.0 : 0.0
                                );
                            }
                        }

                        exporter = await api.exportAudio(options);
                        var audible = false;
                        for (var chunkIndex = 0; chunkIndex < 20; chunkIndex++) {
                            if (checkToken !== soundFontCompatibilityCheckToken) return true;
                            var chunk = await exporter.render(400);
                            if (!chunk) break;
                            if (containsAudibleSamples(chunk.samples)) {
                                audible = true;
                                break;
                            }
                            if (
                                Number.isFinite(Number(chunk.currentTick)) &&
                                Number(chunk.currentTick) >= noteRange.endTick
                            ) {
                                break;
                            }
                        }

                        if (checkToken === soundFontCompatibilityCheckToken) {
                            soundFontCompatibilityCache[cacheKey] = audible;
                        }
                        return audible;
                    } catch (error) {
                        // Neu WebView/alphaTab khong ho tro exportAudio, khong duoc fallback nham.
                        console.warn('SoundFont compatibility check skipped', error);
                        soundFontCompatibilityCache[cacheKey] = true;
                        return true;
                    } finally {
                        if (exporter && typeof exporter.destroy === 'function') {
                            try { exporter.destroy(); } catch (_) {}
                        }
                        if (checkToken === soundFontCompatibilityCheckToken) {
                            soundFontCompatibilityCheckInProgress = false;
                            AndroidInterface.onSoundFontCompatibilityCheckChanged(false);
                        }
                    }
                }

                function autoFallbackToStandard() {
                    if (
                        !api ||
                        activeSoundFontMode === 'standard' ||
                        autoFallbackInProgress
                    ) {
                        return false;
                    }

                    var incompatibleMode = activeSoundFontMode || 'standard';
                    autoFallbackInProgress = true;
                    AndroidInterface.onSoundFontAutoFallback(incompatibleMode);
                    var started = switchSoundFontMode(
                        'standard',
                        standardSoundFontUrl,
                        true,
                        true
                    );
                    if (!started) {
                        autoFallbackInProgress = false;
                    }
                    return started;
                }

                function signalA4PdfLayoutReady() {
                    if (!isPdfExportMode || !pdfExportWaitingForRender) return;

                    pdfExportWaitingForRender = false;
                    clearTimeout(pdfExportReadyTimer);
                    clearTimeout(pdfExportFallbackTimer);
                    setTimeout(function() {
                        AndroidInterface.onPdfLayoutReady();
                    }, 80);
                }

                function prepareA4PdfExport() {
                    clearTimeout(scoreZoomRenderTimer);
                    scoreZoomScrollAnchor = null;
                    try {
                        if (!api || !api.score) {
                            AndroidInterface.onPdfExportFailed('Bản nhạc chưa sẵn sàng để xuất PDF.');
                            return;
                        }
                        if (isPdfExportMode) return;

                        var wrapper = document.getElementById('scroll-wrapper');
                        pdfExportOriginalState = {
                            scale: Number(api.settings.display.scale),
                            stretchForce: Number(api.settings.display.stretchForce),
                            enableLazyLoading: typeof api.settings.core.enableLazyLoading === 'boolean'
                                ? api.settings.core.enableLazyLoading
                                : true,
                            scrollTop: wrapper ? wrapper.scrollTop : 0
                        };

                        api.stop();
                        notifyTabBannerVisibility(false);
                        isPdfExportMode = true;
                        pdfExportWaitingForRender = true;
                        document.documentElement.classList.add('pdf-export-mode');
                        document.body.classList.add('pdf-export-mode');
                        toggleTheme(currentThemeIsDark);

                        // Cùng các thông số alphaTab dùng cho bản in: render toàn bộ,
                        // scale 0.8, stretchForce 0.8 và chiều rộng vùng nội dung A4.
                        api.settings.core.enableLazyLoading = false;
                        api.settings.display.scale = 0.8;
                        api.settings.display.stretchForce = 0.8;

                        // Tính lại tiêu đề theo chiều rộng 190 mm rồi render track đang chọn.
                        lastResponsiveTitleFont = '';
                        applyResponsiveSongHeader(api.score, false);
                        applySelectedTrackSolo(true);

                        clearTimeout(pdfExportFallbackTimer);
                        pdfExportFallbackTimer = setTimeout(function() {
                            signalA4PdfLayoutReady();
                        }, 7000);
                    } catch (error) {
                        restoreAfterA4PdfExport();
                        AndroidInterface.onPdfExportFailed(
                            'Không thể chuẩn bị PDF: ' +
                            (error && error.message ? error.message : error)
                        );
                    }
                }

                function restoreAfterA4PdfExport() {
                    clearTimeout(pdfExportReadyTimer);
                    clearTimeout(pdfExportFallbackTimer);
                    pdfExportWaitingForRender = false;

                    if (!isPdfExportMode) return;
                    isPdfExportMode = false;
                    document.documentElement.classList.remove('pdf-export-mode');
                    document.body.classList.remove('pdf-export-mode');
                    syncWebViewViewportBox(
                        lastAndroidViewportWidth,
                        lastAndroidViewportHeight
                    );

                    try {
                        if (api && api.score && pdfExportOriginalState) {
                            api.settings.core.enableLazyLoading =
                                pdfExportOriginalState.enableLazyLoading;
                            api.settings.display.scale = Number.isFinite(pdfExportOriginalState.scale)
                                ? pdfExportOriginalState.scale
                                : 1.0;
                            api.settings.display.stretchForce =
                                Number.isFinite(pdfExportOriginalState.stretchForce)
                                    ? pdfExportOriginalState.stretchForce
                                    : 1.0;

                            lastResponsiveTitleFont = '';
                            applyResponsiveSongHeader(api.score, false);
                            applySelectedTrackSolo(true);

                            var oldScrollTop = pdfExportOriginalState.scrollTop || 0;
                            setTimeout(function() {
                                var wrapper = document.getElementById('scroll-wrapper');
                                if (wrapper) wrapper.scrollTop = oldScrollTop;
                            }, 350);
                        }
                    } catch (_) {
                        // Không chặn giao diện nếu thiết bị hủy hộp thoại in giữa chừng.
                    } finally {
                        toggleTheme(currentThemeIsDark);
                        pdfExportOriginalState = null;
                    }
                }

                function switchSoundFontMode(
                    mode,
                    soundFontUrl,
                    temporaryFallback,
                    autoPlayAfterLoad
                ) {
                    if (!api || !soundFontUrl) return false;

                    try {
                        api.stop();
                        resetSoundFontCompatibilityState();
                        pendingSoundFontMode = String(mode || 'standard');
                        pendingSoundFontTemporaryFallback = !!temporaryFallback;
                        autoPlayAfterSoundFontReady = !!autoPlayAfterLoad;
                        isSoundFontLoading = true;

                        // append=false da thay the toan bo SoundFont cu. Khong reset truoc de
                        // tranh tao khoang thoi gian player khong con preset nao.
                        var started = api.loadSoundFont(soundFontUrl, false);
                        if (!started) {
                            var failedMode = pendingSoundFontMode || 'standard';
                            pendingSoundFontMode = null;
                            pendingSoundFontTemporaryFallback = false;
                            autoPlayAfterSoundFontReady = false;
                            isSoundFontLoading = false;
                            AndroidInterface.onSoundFontLoadFailed(
                                failedMode,
                                soundFontStartFailedText
                            );
                            return false;
                        }

                        return true;
                    } catch (error) {
                        var failedMode = pendingSoundFontMode || String(mode || 'standard');
                        pendingSoundFontMode = null;
                        pendingSoundFontTemporaryFallback = false;
                        autoPlayAfterSoundFontReady = false;
                        isSoundFontLoading = false;
                        AndroidInterface.onSoundFontLoadFailed(
                            failedMode,
                            soundFontLoadFailedPrefix +
                                (error && error.message ? error.message : error)
                        );
                        return false;
                    }
                }

                // alphaTab mặc định nối thân trường độ từ số ngăn đàn xuống beam,
                // vì vậy đường dọc đi xuyên qua khuông TAB. TunerTools giữ cách tính
                // trường độ của alphaTab nhưng chỉ vẽ đoạn thân ngắn ngay phía trên beam.
                // Custom renderer chạy ở main thread nên initAlphaTab đặt useWorkers=false.
                var detachedTabRhythmFactoryPatched = false;

                function installDetachedTabRhythmPatch() {
                    if (detachedTabRhythmFactoryPatched) return true;

                    try {
                        if (
                            typeof alphaTab === 'undefined' ||
                            !alphaTab.Environment ||
                            !alphaTab.Environment.defaultRenderers
                        ) {
                            return false;
                        }

                        var factories = alphaTab.Environment.defaultRenderers;
                        var tabFactory = null;
                        for (var factoryIndex = 0; factoryIndex < factories.length; factoryIndex++) {
                            var candidate = factories[factoryIndex];
                            if (
                                candidate &&
                                candidate.staffId === 'tab' &&
                                typeof candidate.create === 'function'
                            ) {
                                tabFactory = candidate;
                                break;
                            }
                        }

                        if (!tabFactory) return false;
                        if (tabFactory.__tunerToolsDetachedRhythmPatched === true) {
                            detachedTabRhythmFactoryPatched = true;
                            return true;
                        }

                        var originalCreateTabRenderer = tabFactory.create;
                        tabFactory.create = function(renderer, bar) {
                            var tabRenderer = originalCreateTabRenderer.call(this, renderer, bar);

                            if (
                                tabRenderer &&
                                tabRenderer.__tunerToolsThinRhythmBeam !== true
                            ) {
                                try {
                                    // Beam mặc định của alphaTab khá dày khi thu nhỏ trên màn hình
                                    // điện thoại. Chỉ giảm độ dày các thanh ngang của TAB; số lượng
                                    // một, hai hoặc ba thanh vẫn giữ nguyên để đọc đúng trường độ.
                                    Object.defineProperty(tabRenderer, 'beamThickness', {
                                        configurable: true,
                                        get: function() {
                                            var sourceThickness =
                                                this.smuflMetrics &&
                                                Number.isFinite(Number(this.smuflMetrics.beamThickness))
                                                    ? Number(this.smuflMetrics.beamThickness)
                                                    : 3;

                                            return Math.max(
                                                0.85,
                                                Math.min(1.15, sourceThickness * 0.34)
                                            );
                                        }
                                    });
                                    tabRenderer.__tunerToolsThinRhythmBeam = true;
                                } catch (_) {
                                    // Nếu phiên bản alphaTab không cho ghi đè getter thì vẫn dùng
                                    // renderer mặc định, không làm gián đoạn việc tải bản nhạc.
                                }
                            }

                            if (
                                tabRenderer &&
                                tabRenderer.__tunerToolsDetachedRhythmRenderer !== true
                            ) {
                                tabRenderer.paintBeamingStem = function(
                                    beat,
                                    cy,
                                    x,
                                    topY,
                                    bottomY,
                                    canvas
                                ) {
                                    if (!canvas || typeof canvas.fillRect !== 'function') return;

                                    var stemTop = Number(topY);
                                    var stemBottom = Number(bottomY);
                                    if (!Number.isFinite(stemTop) || !Number.isFinite(stemBottom)) return;
                                    if (stemBottom < stemTop) {
                                        var swapY = stemTop;
                                        stemTop = stemBottom;
                                        stemBottom = swapY;
                                    }

                                    var availableHeight = stemBottom - stemTop;
                                    if (availableHeight <= 0) return;

                                    var configuredRhythmHeight = 25;
                                    if (
                                        this.settings &&
                                        this.settings.notation &&
                                        Number.isFinite(Number(this.settings.notation.rhythmHeight))
                                    ) {
                                        configuredRhythmHeight = Number(
                                            this.settings.notation.rhythmHeight
                                        );
                                    }

                                    // Chỉ giữ 7-12 px cuối của thân nốt. Đoạn này nằm hoàn toàn
                                    // trong vùng trường độ dưới khuông, không chạm dây hoặc số TAB.
                                    var preferredStemLength = Math.max(
                                        7,
                                        Math.min(12, configuredRhythmHeight * 0.42)
                                    );
                                    var detachedStemLength = Math.min(
                                        availableHeight,
                                        preferredStemLength
                                    );
                                    var detachedStemTop = stemBottom - detachedStemLength;
                                    var stemThickness = this.smuflMetrics &&
                                        Number.isFinite(Number(this.smuflMetrics.stemThickness))
                                        ? Number(this.smuflMetrics.stemThickness)
                                        : 1;

                                    canvas.fillRect(
                                        Number(x),
                                        detachedStemTop,
                                        stemThickness,
                                        detachedStemLength
                                    );
                                };
                                tabRenderer.__tunerToolsDetachedRhythmRenderer = true;
                            }
                            return tabRenderer;
                        };

                        tabFactory.__tunerToolsDetachedRhythmPatched = true;
                        detachedTabRhythmFactoryPatched = true;
                        return true;
                    } catch (error) {
                        console.warn('Could not install detached TAB rhythm renderer', error);
                        return false;
                    }
                }

                function initAlphaTab(attempt) {
                    attempt = Number(attempt) || 0;
                    if (isApiReady || api) return;

                    var wrapper = document.getElementById('scroll-wrapper');
                    var container = document.getElementById('alphatab-container');
                    var viewport = syncWebViewViewportBox(
                        lastAndroidViewportWidth,
                        lastAndroidViewportHeight
                    );

                    if (
                        !wrapper ||
                        !container ||
                        typeof alphaTab === 'undefined' ||
                        viewport.width <= 1 ||
                        viewport.height <= 1
                    ) {
                        clearTimeout(alphaTabInitRetryTimer);
                        alphaTabInitRetryTimer = setTimeout(function() {
                            initAlphaTab(attempt + 1);
                        }, attempt < 20 ? 100 : 250);
                        return;
                    }

                    clearTimeout(alphaTabInitRetryTimer);
                    installDetachedTabRhythmPatch();
                    
                    // Touch listener phai non-passive de chan native page-pinch,
                    // chi xu ly khi co >= 2 ngon trong vung TAB (khong phai can dan).
                    wrapper.addEventListener('touchstart', startScorePinch,
                        { passive: false });
                    wrapper.addEventListener('touchmove', moveScorePinch,
                        { passive: false });
                    wrapper.addEventListener('touchend', finishScorePinch,
                        { passive: false });
                    wrapper.addEventListener('touchcancel', finishScorePinch,
                        { passive: false });

                    wrapper.addEventListener('touchstart', function(event) {
                        if (scorePinch.active ||
                            (event.touches && event.touches.length >= 2)) return;
                        isUserScrolling = true;
                        clearTimeout(scrollTimeout);
                    }, { passive: true });

                    wrapper.addEventListener('touchmove', function() {
                        if (scorePinch.active) return;
                        // WebView co the phat scroll sau touchmove (inertia/fling).
                        // Ghi nhan thao tac thuc, khong phu thuoc event scroll dau tien.
                        if (wrapper.scrollTop > 0) tabBannerManuallyScrolled = true;
                    }, { passive: true });

                    wrapper.addEventListener('touchend', function() {
                        scrollTimeout = setTimeout(function() { isUserScrolling = false; }, 3000);
                    }, { passive: true });
                    
                    wrapper.addEventListener('wheel', function() {
                        isUserScrolling = true;
                        clearTimeout(scrollTimeout);
                        scrollTimeout = setTimeout(function() { isUserScrolling = false; }, 3000);
                    }, { passive: true });

                    wrapper.addEventListener('scroll', function() {
                        var newScrollTop = Math.max(0, wrapper.scrollTop);
                        // AlphaTab tu cuon theo not dang phat khong duoc tinh
                        // la nguoi dung chu dong cuon den cuoi bai.
                        if (isUserScrolling &&
                            newScrollTop > tabBannerPreviousScrollTop + 0.5) {
                            tabBannerManuallyScrolled = true;
                        }
                        tabBannerPreviousScrollTop = newScrollTop;
                        updateTabBannerAtScroll();
                    }, { passive: true });

                    api = new alphaTab.AlphaTabApi(document.getElementById('alphatab-container'), {
                        // Tắt render worker để custom TabBarRenderer ở trên được áp dụng.
                        core: { engine: 'svg', useWorkers: false, enableLazyLoading: false },
                        display: {
                            layoutMode: 'page',
                            staveProfile: 'scoretab',
                            scale: scoreZoomScaleForMode(false),
                            stretchForce: 1.0,
                            systemPaddingBottom: noteNamesVisible
                                ? noteNameSystemPaddingBottom
                                : defaultSystemPaddingBottom,
                            lastSystemPaddingBottom: noteNamesVisible
                                ? noteNameLastSystemPaddingBottom
                                : defaultLastSystemPaddingBottom,
                            padding: [12, 10, 12, 24],
                            resources: {
                                titleFont: 'bold 18px Arial, sans-serif',
                                subTitleFont: '14px Arial, sans-serif',
                                wordsFont: '12px Arial, sans-serif',
                                copyrightFont: 'bold 10px Arial, sans-serif'
                            }
                        },
                        // Automatic không vẽ thêm tiết tấu khi khuông nhạc đang hiển thị.
                        // Khi người dùng chọn TAB, setStaveDisplayMode() chuyển sang
                        // ShowWithBars và custom renderer chỉ giữ ký hiệu ở dưới khuông.
                        notation: {
                            rhythmMode: 'Automatic',
                            rhythmHeight: 25,
                            elements: { scoreTitle: true }
                        },
                        player: {
                            enablePlayer: true,
                            enableCursor: true,
                            soundFont: $quotedInitialSoundFontUrl,
                            scrollElement: wrapper,
                            // Không để alphaTab tự nhảy tới khuông đầu khi vừa tạo cursor.
                            // Cuộn theo khuông lúc phát được xử lý bởi playerPositionChanged.
                            scrollMode: 'Off'
                        }
                    });
                    // Đồng bộ màu RenderingResources trước khi score đầu tiên được render.
                    applyAlphaTabVisualResources(false);
                    setMetronomeEnabled(metronomeEnabled);

                    api.soundFontLoad.on(function(e) {
                        AndroidInterface.onSoundFontLoadProgress(
                            Number(e && e.loaded ? e.loaded : 0),
                            Number(e && e.total ? e.total : 0)
                        );
                    });
                    api.soundFontLoaded.on(function() {
                        isSoundFontLoading = false;
                        activeSoundFontMode = pendingSoundFontMode ||
                            activeSoundFontMode ||
                            'standard';
                        var wasTemporaryFallback = pendingSoundFontTemporaryFallback;
                        pendingSoundFontMode = null;
                        pendingSoundFontTemporaryFallback = false;
                        AndroidInterface.onSoundFontLoaded(
                            activeSoundFontMode,
                            wasTemporaryFallback
                        );

                        // Doi SoundFont trong luc score dang mo phai tao lai MIDI truoc khi Play.
                        if (api && api.score && typeof api.loadMidiForScore === 'function') {
                            api.loadMidiForScore();
                        }
                    });
                    api.playerReady.on(function() {
                        AndroidInterface.onPlayerReady();
                        if (autoPlayAfterSoundFontReady) {
                            autoPlayAfterSoundFontReady = false;
                            autoFallbackInProgress = false;
                            setTimeout(function() {
                                if (api && api.isReadyForPlayback) {
                                    api.play();
                                }
                            }, 50);
                        }
                    });
                    api.playerStateChanged.on(function(e) {
                        lastKnownPlayerState = Number(e && e.state ? e.state : 0);
                        if (e && e.stopped === true) {
                            updateActiveNoteNameBeat(null);
                            updateFretboardForBeat(null);
                        }
                        if (lastKnownPlayerState === 1) {
                            scheduleVisiblePlayheadThickness();
                        } else {
                            stopVisiblePlayheadTracking();
                        }
                        AndroidInterface.onPlayerStateChanged(lastKnownPlayerState);
                    });
                    if (api.playedBeatChanged && typeof api.playedBeatChanged.on === 'function') {
                        api.playedBeatChanged.on(function(beat) {
                            updateActiveNoteNameBeat(beat);
                            updateFretboardForBeat(beat);
                            // Playhead may appear only after the first playback beat.
                            scheduleVisiblePlayheadThickness();
                        });
                    }
                    api.midiLoaded.on(function() {
                        // loadMidiForScore() tạo lại MIDI khi đổi BPM; áp lại track đang solo.
                        applySelectedTrackSolo(false);
                    });

                    api.playerPositionChanged.on(function(e) {
                        // Sự kiện này cũng phát sinh khi MIDI/cursor vừa khởi tạo.
                        // Chỉ tự cuộn khi người dùng đã thật sự bấm Play.
                        if (isUserScrolling || lastKnownPlayerState !== 1) return;

                        if (fretboardEnabled) {
                            keepScoreCursorNearFretboard(false);
                            return;
                        }

                        var cursor = document.querySelector('.at-cursor-beat');
                        if (cursor) {
                            var rect = cursor.getBoundingClientRect();
                            var wrapperRect = wrapper.getBoundingClientRect();
                            var relativeTop = rect.top - wrapperRect.top;
                            var wrapperHeight = wrapper.clientHeight;

                            if (relativeTop > wrapperHeight * .60 ||
                                relativeTop < wrapperHeight * .10) {
                                var absoluteTop = wrapper.scrollTop + relativeTop;
                                wrapper.scrollTo({
                                    top: Math.max(0, absoluteTop - wrapperHeight * .30),
                                    behavior: 'smooth'
                                });
                            }
                        }
                    });

                    api.error.on(function(e) {
                        var message = 'Lỗi AlphaTab: ' +
                            (e && e.message ? e.message : e);

                        if (isScoreLoading) {
                            isScoreLoading = false;
                            AndroidInterface.onScoreLoaded("[]", message);
                        } else if (isSoundFontLoading || pendingSoundFontMode) {
                            var failedMode = pendingSoundFontMode ||
                                activeSoundFontMode ||
                                'standard';
                            pendingSoundFontMode = null;
                            pendingSoundFontTemporaryFallback = false;
                            autoPlayAfterSoundFontReady = false;
                            autoFallbackInProgress = false;
                            isSoundFontLoading = false;
                            AndroidInterface.onSoundFontLoadFailed(failedMode, message);
                        } else {
                            AndroidInterface.onScoreLoaded("[]", message);
                        }
                    });

                    // Kiểm tra lại đúng kích thước SVG sau khi alphaTab đã gắn kết quả
                    // vào DOM. Nếu font thực tế vẫn rộng hơn màn hình thì tự co thêm.
                    var completedRenderEvent = api.postRenderFinished || api.renderFinished;
                    if (completedRenderEvent && typeof completedRenderEvent.on === 'function') {
                        completedRenderEvent.on(function() {
                            restoreZoomScrollAnchor();
                            scheduleVisiblePlayheadThickness();
                            verifyRenderedTitleFits();
                            renderNoteNameOverlay();
                            if (fretboardEnabled) {
                                scheduleFretboardRefresh();
                                setTimeout(function() {
                                    keepScoreCursorNearFretboard(false);
                                }, 70);
                            }

                            // Mỗi lần mở bài mới phải quay về đúng đầu bản nhạc để người dùng
                            // nhìn thấy tiêu đề. Không áp dụng khi chỉ đổi track hoặc đổi chế độ.
                            if (scrollToTopAfterNextRender) {
                                scrollToTopAfterNextRender = false;
                                var renderedWrapper = document.getElementById('scroll-wrapper');
                                if (renderedWrapper) renderedWrapper.scrollTop = 0;
                            }

                            // Sau khi alphaTab ve lai, SVG cuoi co the da bi thay the.
                            tabBannerLastScoreSvg = null;
                            setTimeout(updateTabBannerAtScroll, 50);

                            if (pdfExportWaitingForRender) {
                                // Đợi tiêu đề và các SVG ổn định; nếu alphaTab render lại,
                                // timer được đặt lại để PDF không bị thiếu trang hoặc cắt chữ.
                                clearTimeout(pdfExportReadyTimer);
                                pdfExportReadyTimer = setTimeout(function() {
                                    signalA4PdfLayoutReady();
                                }, 700);
                            }
                        });
                    }

                    api.scoreLoaded.on(function(score) {
                        isScoreLoading = false;
                        soundFontCompatibilityScoreGeneration++;
                        resetSoundFontCompatibilityState();
                        autoFallbackInProgress = false;
                        autoPlayAfterSoundFontReady = false;
                        currentTracks = score.tracks;
                        songTechniqueCatalogCache = {};
                        fretboardLatestBeat = null;
                        fretboardWindowStart = 1;
                        lastPlayedNoteNameBeatId = '';
                        clearNoteNameOverlay();

                        // Sửa metadata trống/hỏng bảng mã ngay trước lần render đầu.
                        applyResponsiveSongHeader(score, false);
                        captureOriginalTempos(score);

                        var tracksData = [];
                        for(var i=0; i<score.tracks.length; i++) {
                            tracksData.push({ index: i, name: score.tracks[i].name || ('Track ' + (i+1)) });
                        }
                        
                        if(currentTracks.length > 0) {
                            selectedTrackForPlayback = 0;
                            applySelectedTrackSolo(true);
                        }
                        if (fretboardEnabled) scheduleFretboardRefresh();
                        
                        var title = String(currentScoreTitle || score.title || 'Bản nhạc').trim();
                        AndroidInterface.onScoreLoaded(JSON.stringify(tracksData), title);

                        // Một số bản alphaTab không tạo header nếu file gốc không có metadata.
                        // Kiểm tra lại sau render; nếu vẫn thiếu thì hiển thị title dự phòng
                        // ngay trong vùng bản nhạc, phía trên khuông đầu tiên.
                        setTimeout(function() {
                            verifyRenderedTitleFits();
                        }, 180);
                    });
                    
                    // Khi xoay màn hình hoặc WebView thay đổi chiều rộng, tính lại font
                    // để tiêu đề vẫn vừa cả ở chế độ dọc lẫn ngang.
                    window.addEventListener('resize', function() {
                        clearTimeout(titleResizeTimer);
                        titleResizeTimer = setTimeout(function() {
                            if (!api || !api.score || isPdfExportMode) return;
                            applyResponsiveSongHeader(api.score, true);
                        }, 180);
                    });

                    isApiReady = true;
                    lastSyncedCssViewportWidth = wrapper.clientWidth || lastSyncedCssViewportWidth;
                    AndroidInterface.onTabReady();
                }
                
                async function loadScoreFromUrl(fileUrl, fallbackTitle) {
                    if (!isApiReady || !api) {
                        setTimeout(function() {
                            loadScoreFromUrl(fileUrl, fallbackTitle);
                        }, 500);
                        return;
                    }

                    try {
                        pendingScoreTitle = String(fallbackTitle || '')
                            .replace(/\s+/g, ' ')
                            .trim();
                        hideFallbackScoreTitle();
                        resetTabBannerForScore();
                        fretboardLatestBeat = null;
                        fretboardWindowStart = 1;
                        currentTracks = [];
                        if (fretboardEnabled) scheduleFretboardRefresh();
                        scrollToTopAfterNextRender = true;

                        // Xóa vị trí cuộn của bài trước ngay khi bắt đầu tải. Sau lần render
                        // đầu tiên sẽ đặt lại thêm một lần để chắc chắn tiêu đề không bị khuất.
                        var wrapper = document.getElementById('scroll-wrapper');
                        if (wrapper) wrapper.scrollTop = 0;

                        isScoreLoading = true;
                        resetSoundFontCompatibilityState();
                        autoFallbackInProgress = false;
                        autoPlayAfterSoundFontReady = false;
                        const res = await fetch(fileUrl);
                        if (!res.ok) throw new Error("Mã phản hồi HTTP " + res.status);
                        const buffer = await res.arrayBuffer();
                        api.load(new Uint8Array(buffer));
                    } catch (err) {
                        isScoreLoading = false;
                        AndroidInterface.onScoreLoaded("[]", "Lỗi Fetch JS: " + err.message);
                    }
                }
                
                function selectTrackSolo(index) {
                    if (!currentTracks || index < 0 || index >= currentTracks.length) return;
                    selectedTrackForPlayback = index;
                    fretboardLatestBeat = null;
                    fretboardWindowStart = 1;
                    lastPlayedNoteNameBeatId = '';
                    clearNoteNameOverlay();
                    resetSoundFontCompatibilityState();
                    applySelectedTrackSolo(true);
                }

                async function playPause() {
                    if (!api) return false;

                    if (lastKnownPlayerState === 1) {
                        api.pause();
                        return true;
                    }
                    if (
                        isSoundFontLoading ||
                        soundFontCompatibilityCheckInProgress ||
                        !api.isReadyForPlayback
                    ) {
                        return false;
                    }

                    if (activeSoundFontMode && activeSoundFontMode !== 'standard') {
                        var compatible = await verifySelectedSoundFontProducesAudio();
                        if (!compatible) {
                            autoFallbackToStandard();
                            return true;
                        }
                    }

                    return api.play();
                }
                function stopPlayer() {
                    resetSoundFontCompatibilityState();
                    if (api) api.stop();
                    updateFretboardForBeat(null);
                }

                window.addEventListener('load', function() {
                    syncWebViewViewportBox(
                        lastAndroidViewportWidth,
                        lastAndroidViewportHeight
                    );
                    initAlphaTab(0);
                });
                if (document.readyState === 'complete') {
                    setTimeout(function() { initAlphaTab(0); }, 0);
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}
