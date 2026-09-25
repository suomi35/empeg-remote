package com.chasinglemons.empeg.discovery

import com.chasinglemons.empeg.model.Empeg

/**
 * Parses the UDP discovery reply from an emplayer/hijack player.
 *
 * A real player answers a "?" probe on port 8300 with a single
 * `name=<player name>` field. The simulator also reports the HTTP `port` it
 * is listening on, so discovery of a simulator running on a non-80 port sets
 * up a working "host:port" player entry instead of an unreachable bare IP.
 *
 * Example replies:
 * - `name=EmpegCar`
 * - `name=EmpegSim port=8099`
 */
object DiscoveryResponse {

    fun parse(response: String, hostAddress: String): Empeg? {
        val text = response.trim()
        if (text.isEmpty() || text == "?") return null

        val fields = text.split(Regex("\\s+"))
            .mapNotNull { field ->
                val parts = field.split('=', limit = 2)
                if (parts.size == 2 && parts[0].isNotEmpty()) parts[0] to parts[1] else null
            }
            .toMap()

        val name = fields["name"]?.takeIf { it.isNotBlank() } ?: text
        val port = fields["port"]?.toIntOrNull()?.takeIf { it in 1..65535 }
        // Port 80 is the default, so keep the entry as a bare host there in the
        // same way the app has always stored discovered players.
        val ip = if (port != null && port != DEFAULT_PORT) "$hostAddress:$port" else hostAddress
        return Empeg(name, ip)
    }

    private const val DEFAULT_PORT = 80
}
