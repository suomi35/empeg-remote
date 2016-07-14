package com.chasinglemons.empeg;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

/**
 * Code for dealing with Empeg server discovery. 
 * This class tries to send a broadcast UDP packet over your wifi network to
 * discover the empeg service.
 */

public class Discoverer extends Thread {
	private static final int DISCOVERY_PORT = 8300;
	private DiscoveryReceiver mReceiver;
	private WifiManager mWifi;
	SharedPreferences config;
	Context mContext;

	interface DiscoveryReceiver {
		/**
		 * Process the list of discovered servers. This is always called once after
		 * a short timeout.
		 * 
		 * @param servers
		 *          list of discovered servers, null on error
		 */
		void addAnnouncedServers(ArrayList<Empeg> servers);
	}

	Discoverer(WifiManager wifi, DiscoveryReceiver receiver, Context context) {
		mWifi = wifi;
		mReceiver = receiver;
		mContext = context;
	}

	@Override
	public void run() {
		config = PreferenceManager.getDefaultSharedPreferences(mContext);
		ArrayList<Empeg> servers = null;
		try {
			DatagramSocket socket = new DatagramSocket(DISCOVERY_PORT);
			socket.setBroadcast(true);
			socket.setSoTimeout((config.getInt("discoveryTimeout",2)*1000));

			sendDiscoveryRequest(socket);
			servers = listenForResponses(socket);
			socket.close();
		} catch (IOException e) {
			servers = new ArrayList<Empeg>();  // use an empty one
			//Log.e("DISCOVERER_run()", "Could not send discovery request",e);
		}
		mReceiver.addAnnouncedServers(servers);
	}

	/**
	 * Send a broadcast UDP packet containing a request for empegs to
	 * announce themselves.
	 * 
	 * @throws IOException
	 */
	private void sendDiscoveryRequest(DatagramSocket socket) throws IOException {
		String data = "?";
		//Log.d("sendDiscoveryRequest()", "Sending data " + data);

		DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(),
				getBroadcastAddress(), DISCOVERY_PORT);
		socket.send(packet);
	}

	/**
	 * Calculate the broadcast IP we need to send the packet along. If we send it
	 * to 255.255.255.255, it never gets sent. I guess this has something to do
	 * with the mobile network not wanting to do broadcast.
	 */
	private InetAddress getBroadcastAddress() throws IOException {
		DhcpInfo dhcp = mWifi.getDhcpInfo();
		if (dhcp == null) {
			//Log.d("InetAddress getBroadcastAddress()", "Could not get dhcp info");
			return null;
		}

		int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
		byte[] quads = new byte[4];
		for (int k = 0; k < 4; k++)
			quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
		return InetAddress.getByAddress(quads);
	}

	/**
	 * Listen on socket for responses, timing out after TIMEOUT_MS
	 * 
	 * @param socket
	 *          socket on which the announcement request was sent
	 * @return list of discovered servers, never null
	 * @throws IOException
	 */
	private ArrayList<Empeg> listenForResponses(DatagramSocket socket)
			throws IOException {
		//    long start = System.currentTimeMillis();
		byte[] buf = new byte[1024];
		ArrayList<Empeg> servers = new ArrayList<Empeg>();

		// Loop and try to receive responses until the timeout elapses. We'll get
		// back the packet we just sent out, which isn't terribly helpful, but we'll
		// discard it in parseResponse because the cmd is wrong.
		try {
			while (true) {
				Empeg server = null;
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				String s = new String(packet.getData(), 0, packet.getLength());
				/*        //Log.d("DISCOVERY", "Packet received after "
            + (System.currentTimeMillis() - start) + " " + s);*/
				if (!s.equals("?")) {
					server = parseResponse(s, ((InetSocketAddress) packet
							.getSocketAddress()).getAddress());
				}
				if (server != null)
					servers.add(server);
			}
		} catch (SocketTimeoutException e) {
			//Log.d("DISCOVERY", "Receive timed out: "+e);
		}
		return servers;
	}

	private Empeg parseResponse(String response, InetAddress address)
			throws IOException {

		//Log.i("DISCOVERER","response = "+response);
		String[] empegName = response.split("name=");
		Empeg server = new Empeg(empegName[1],address.getHostAddress());
		//    //Log.d("DISCOVERY", "Discovered server "empegName[1]+"@"+address.getHostAddress());

		return server;
	}


	/*  public static void main(String[] args) {
    new Discoverer(null, null).start();
    while (true) {
    }
  }*/
}