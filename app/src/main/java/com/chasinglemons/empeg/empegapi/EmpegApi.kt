package com.chasinglemons.empeg.empegapi

/**
 * The Empeg's HTTP interface, extracted so it can be faked in tests and
 * emulated by a local player simulator.
 *
 * Grammar (confirmed against empeg web lite 0.95, see fixtures/ghostwheel):
 * - Browse:        GET /?FID=<tagfid>&EXT=.xml
 * - Buttons:       GET /?NODATA&BUTTONRAW=<Button>        (press)
 *                  GET /?NODATA&BUTTONRAW=<Button>.R      (release)
 * - Item commands: GET /?NODATA&SERIAL=%23<fid>[+|!|-]    (play/append/insert/enqueue)
 * - Legacy notify: GET /proc/empeg_notify?button=<command>
 * - VFD screen:    GET /proc/empeg_screen.gif (256x64 GIF; 404 when disabled)
 */
interface EmpegApi {

    /**
     * Fetch and parse the playlist with the given tag FID.
     *
     * @return the parsed playlist, or null when the player returned an empty
     * body (observed for unknown FIDs: HTTP 200, empty body).
     */
    suspend fun fetchPlaylist(fid: String = ROOT_FID): EmpegPlaylist?

    /** Send a button press, e.g. "Top", "KnobLeft". */
    suspend fun pressButton(button: String)

    /** Send a button release for a previously pressed button. */
    suspend fun releaseButton(button: String)

    /**
     * Send a raw SERIAL command, e.g. "#2cf0" (play), "#2cf0+" (append),
     * "#2cf0!" (insert) or "#2cf0-" (enqueue).
     */
    suspend fun sendSerial(command: String)

    /**
     * Legacy button endpoint used by earlier versions of the app.
     * @param command the raw command value, e.g. "Top".
     */
    suspend fun sendNotifyButton(command: String)

    /**
     * URL of the live VFD screen image (128x32 PNG, 256x64 on some players).
     * The endpoint returns 404 on players where hijack screen capture is
     * disabled, so callers must handle load failures gracefully.
     */
    fun screenUrl(): String

    companion object {
        const val ROOT_FID = "101"

        /** Screen image on the player; weblite polls exactly this path. */
        const val SCREEN_PATH = "/proc/empeg_screen.png"

        /**
         * Screen URL for a configured player, which is either a bare host
         * (`192.168.1.56`, the real-player case: port 80) or `host:port` as
         * set up by discovery of a simulator or manually by the user.
         */
        fun screenUrlFor(hostSpec: String): String = "http://$hostSpec$SCREEN_PATH"
    }
}
