package com.chasinglemons.empeg.discovery

import com.chasinglemons.empeg.model.Empeg
import com.chasinglemons.empeg.preferences.EmpegPreferences
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
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
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun sendDiscoveryRequest(socket: DatagramSocket) {
        val data = "?"
        Timber.d(">>> sendDiscoveryRequest() Sending data $data")
        val packet = DatagramPacket(
            data.toByteArray(), data.length,
            InetAddress.getByName("255.255.255.255"),
            DISCOVERY_PORT
        )
        socket.send(packet)
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
                if (server != null) servers.add(server)
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