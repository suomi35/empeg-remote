package com.chasinglemons.empeg.connect

import com.chasinglemons.empeg.model.Empeg
import com.chasinglemons.empeg.preferences.EmpegPreferences
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import timber.log.Timber
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException

/**
 * Sends a broadcast UDP packet on the wifi network so empegs can announce themselves.
 */
class Discoverer(
    private val preferences: EmpegPreferences
) {

    /**
     * Closes [socket] when the calling coroutine is cancelled so a blocked
     * [DatagramSocket.receive] does not outlive the search.
     */
    suspend fun discoverCancellable(): List<Empeg> {
        // Bind to the well-known discovery port so the simulator's periodic
        // "name=... port=..." beacons (sent to port 8300) are heard even when
        // our own broadcast probe is silently dropped by a dual-homed host.
        val socket = DatagramSocket(DISCOVERY_PORT)
        currentCoroutineContext().job.invokeOnCompletion { socket.close() }
        return try {
            socket.broadcast = true
            socket.soTimeout = (preferences.discoveryTimeout * 1000)
            sendDiscoveryRequest(socket)
            val servers = ArrayList<Empeg>()
            listenForResponses(socket, servers)
            currentCoroutineContext().ensureActive()
            servers
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            Timber.e(e, "Could not complete discovery request")
            emptyList()
        } finally {
            socket.close()
        }
    }

    /**
     * Sends "?" to 255.255.255.255 *and* to the directed broadcast of every
     * up local interface (e.g. 192.168.1.255). Hosts with more than one
     * interface on the same network (VPN + Wi-Fi, USB ethernet alongside
     * Wi-Fi, ...) are known to silently drop one form or the other, so
     * sending both keeps discovery working. A failure on one target is
     * logged and never stops the remaining sends.
     */
    private fun sendDiscoveryRequest(socket: DatagramSocket) {
        val data = "?"
        Timber.d("Sending discovery request")
        for (target in probeTargets()) {
            try {
                socket.send(
                    DatagramPacket(data.toByteArray(), data.length, target, DISCOVERY_PORT)
                )
            } catch (e: Exception) {
                Timber.w("Probe to $target failed: ${e.message}")
            }
        }
    }

    /** 255.255.255.255 plus the directed broadcast of every up, non-loopback interface. */
    private fun probeTargets(): List<InetAddress> {
        val targets = LinkedHashSet<InetAddress>()
        targets += InetAddress.getByName("255.255.255.255")
        try {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.interfaceAddresses }
                .mapNotNull { it.broadcast }
                .forEach { targets += it }
        } catch (e: Exception) {
            Timber.w("probeTargets() could not enumerate interfaces: ${e.message}")
        }
        Timber.d("probeTargets() $targets")
        return targets.toList()
    }

    private fun listenForResponses(socket: DatagramSocket, servers: ArrayList<Empeg>) {
        val start = System.currentTimeMillis()
        val buf = ByteArray(1024)

        try {
            while (true) {
                val packet = DatagramPacket(buf, buf.size)
                socket.receive(packet)
                val s = String(packet.data, 0, packet.length)
                Timber.d("Packet received after ${System.currentTimeMillis() - start}ms")
                if (s == "?") continue
                val address = (packet.socketAddress as? InetSocketAddress)?.address
                val server = parseResponse(s, address)
                // Beacons and multi-target probes can surface the same player
                // several times per search; show each one only once.
                if (server != null && servers.none { it.ip == server.ip }) servers.add(server)
            }
        } catch (_: SocketTimeoutException) {
            Timber.d("Discovery receive timed out")
        }
    }

    companion object {
        private const val DISCOVERY_PORT = 8300

        internal fun parseResponse(response: String, address: InetAddress?): Empeg? {
            val host = address?.hostAddress ?: return null
            val fields = response
                .split(Regex("\\s+"))
                .mapNotNull { field ->
                    val parts = field.split('=', limit = 2)
                    if (parts.size == 2 && parts[0].isNotEmpty()) parts[0] to parts[1] else null
                }
                .toMap()
            val name = (fields["name"] ?: "")
                .substringBefore('\n')
                .substringBefore('\r')
                .trim()
            if (name.isEmpty()) {
                Timber.w("Ignoring discovery response without name=: $response")
                return null
            }
            // The simulator replies "name=... port=<httpPort>" so a sim on a
            // non-80 port yields a working "host:port" entry. A real player
            // answers only "name=..." and stays a bare host (port 80 default).
            val port = fields["port"]?.toIntOrNull()?.takeIf { it in 1..65535 }
            val ip = if (port != null && port != DEFAULT_PORT) "$host:$port" else host
            Timber.d("Discovered server $name@$ip")
            return Empeg(name, ip)
        }

        private const val DEFAULT_PORT = 80
    }
}
