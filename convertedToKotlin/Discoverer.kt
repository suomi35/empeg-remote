package com.chasinglemons.empeg

import android.content.Context
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import android.preference.PreferenceManager
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException

/**
 * Code for dealing with Empeg server discovery.
 * This class tries to send a broadcast UDP packet over your wifi network to
 * discover the empeg service.
 */
class Discoverer internal constructor(
    private val mWifi: WifiManager,
    private val mReceiver: DiscoveryReceiver,
    var mContext: Context
) :
    Thread() {
    var config: SharedPreferences? = null

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
        config = PreferenceManager.getDefaultSharedPreferences(mContext)
        var servers: ArrayList<Empeg>? = null
        try {
            val socket = DatagramSocket(DISCOVERY_PORT)
            socket.broadcast = true
            socket.soTimeout = (config.getInt("discoveryTimeout", 2) * 1000)

            sendDiscoveryRequest(socket)
            servers = listenForResponses(socket)
            socket.close()
        } catch (e: IOException) {
            servers = ArrayList() // use an empty one
            //Log.e("DISCOVERER_run()", "Could not send discovery request",e);
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

        //Log.d("sendDiscoveryRequest()", "Sending data " + data);
        val packet = DatagramPacket(
            data.toByteArray(), data.length,
            broadcastAddress, DISCOVERY_PORT
        )
        socket.send(packet)
    }

    @get:Throws(IOException::class)
    private val broadcastAddress: InetAddress?
        /**
         * Calculate the broadcast IP we need to send the packet along. If we send it
         * to 255.255.255.255, it never gets sent. I guess this has something to do
         * with the mobile network not wanting to do broadcast.
         */
        get() {
            val dhcp = mWifi.dhcpInfo
                ?: //Log.d("InetAddress getBroadcastAddress()", "Could not get dhcp info");
                return null

            val broadcast = (dhcp.ipAddress and dhcp.netmask) or dhcp.netmask.inv()
            val quads = ByteArray(4)
            for (k in 0..3) quads[k] = ((broadcast shr k * 8) and 0xFF).toByte()
            return InetAddress.getByAddress(quads)
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
        //    long start = System.currentTimeMillis();
        val buf = ByteArray(1024)
        val servers = ArrayList<Empeg>()

        // Loop and try to receive responses until the timeout elapses. We'll get
        // back the packet we just sent out, which isn't terribly helpful, but we'll
        // discard it in parseResponse because the cmd is wrong.
        try {
            while (true) {
                var server: Empeg? = null
                val packet = DatagramPacket(buf, buf.size)
                socket.receive(packet)
                val s = String(packet.data, 0, packet.length)
                /*        //Log.d("DISCOVERY", "Packet received after "
            + (System.currentTimeMillis() - start) + " " + s);*/
                if (s != "?") {
                    server = parseResponse(
                        s, (packet
                            .socketAddress as InetSocketAddress).address
                    )
                }
                if (server != null) servers.add(server)
            }
        } catch (e: SocketTimeoutException) {
            //Log.d("DISCOVERY", "Receive timed out: "+e);
        }
        return servers
    }

    @Throws(IOException::class)
    private fun parseResponse(response: String, address: InetAddress): Empeg {
        //Log.i("DISCOVERER","response = "+response);

        val empegName =
            response.split("name=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val server = Empeg(empegName[1], address.hostAddress)

        //    //Log.d("DISCOVERY", "Discovered server "empegName[1]+"@"+address.getHostAddress());
        return server
    } /*  public static void main(String[] args) {
    new Discoverer(null, null).start();
    while (true) {
    }
  }*/


    companion object {
        private const val DISCOVERY_PORT = 8300
    }
}