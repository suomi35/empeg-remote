package com.chasinglemons.empeg.discovery

import com.chasinglemons.empeg.model.Empeg
import com.chasinglemons.empeg.preferences.EmpegPreferences
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import timber.log.Timber

/**
 * Code for dealing with Empeg server discovery.
 * This class tries to send a broadcast UDP packet over your wifi network to
 * discover the empeg service.
 */
class Discoverer internal constructor(
    private val mReceiver: DiscoveryReceiver,
    private val preferences: EmpegPreferences
) :
    Thread() {

    internal interface DiscoveryReceiver {
        /**
         * Process the list of discovered servers. This is always called once after
         * a short timeout.
         *
         * @param servers
         * list of discovered servers, null on error
         */
        fun addAnnouncedServers(servers: ArrayList<Empeg>?)
    }

    override fun run() {
        Timber.d(">>> run()")
        var servers: ArrayList<Empeg>
        try {
            val socket = DatagramSocket(DISCOVERY_PORT)
            socket.broadcast = true
            socket.soTimeout = (preferences.discoveryTimeout * 1000)

            sendDiscoveryRequest(socket)
            servers = listenForResponses(socket)
            socket.close()
        } catch (e: IOException) {
            servers = ArrayList()
            Timber.e(">>> Could not send discovery request: ${e.message}")
        }
        mReceiver.addAnnouncedServers(servers)
    }

    /**
     * Send a broadcast UDP packet containing a request for empegs to
     * announce themselves.
     *
     * The probe goes to 255.255.255.255 *and* to the directed broadcast
     * address of every up local interface (e.g. 192.168.1.255). Hosts with
     * more than one interface on the same network (VPN + Wi-Fi, USB
     * ethernet alongside Wi-Fi, ...) are known to silently drop one form or
     * the other, so sending both keeps discovery working. A failure on one
     * target is logged and never stops the remaining sends.
     */
    private fun sendDiscoveryRequest(socket: DatagramSocket) {
        val data = "?"
        val bytes = data.toByteArray()
        for (target in probeTargets()) {
            try {
                socket.send(DatagramPacket(bytes, bytes.size, target, DISCOVERY_PORT))
                Timber.d(">>> sendDiscoveryRequest() sent '$data' to $target")
            } catch (e: IOException) {
                Timber.w(">>> sendDiscoveryRequest() probe to $target failed: ${e.message}")
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
        } catch (e: IOException) {
            Timber.w(">>> probeTargets() could not enumerate interfaces: ${e.message}")
        }
        Timber.d(">>> probeTargets() $targets")
        return targets.toList()
    }

    /**
     * Listen on socket for responses, timing out after TIMEOUT_MS
     *
     * @param socket
     * socket on which the announcement request was sent
     * @return list of discovered servers, never null
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun listenForResponses(socket: DatagramSocket): ArrayList<Empeg> {
        val start = System.currentTimeMillis()
        val buf = ByteArray(1024)
        val servers: ArrayList<Empeg> = ArrayList()

        // Loop and try to receive responses until the timeout elapses. We'll get
        // back the packet we just sent out, which isn't terribly helpful, but we'll
        // discard it in parseResponse because the cmd is wrong.
        try {
            while (true) {
                var server: Empeg? = null
                val packet = DatagramPacket(buf, buf.size)
                socket.receive(packet)
                val s = String(packet.data, 0, packet.length)
                Timber.d(">>> Packet received after ${(System.currentTimeMillis() - start)})")
                if (s != "?") {
                    server = parseResponse(
                        s, (packet
                            .socketAddress as InetSocketAddress).address
                    )
                }
                // Beacons and multi-target probes can surface the same player
                // several times per search; show each one only once.
                if (server != null && servers.none { it.ip == server.ip }) servers.add(server)
            }
        } catch (e: SocketTimeoutException) {
            Timber.e(">>> Receive timed out: ${e.message}")
        }
        return servers
    }

    @Throws(IOException::class)
    private fun parseResponse(response: String, address: InetAddress): Empeg? {
        Timber.d(">>> response = $response")
        // hijack answers with space separated key=value fields, e.g.
        // "name=EmpegCar" (real player) or "name=EmpegSim port=8099" (simulator).
        val server = DiscoveryResponse.parse(response, address.hostAddress ?: return null)
        Timber.d(">>> Discovered server ${server?.name}@${server?.ip}")
        return server
    }

    companion object {
        private const val DISCOVERY_PORT = 8300
    }
}