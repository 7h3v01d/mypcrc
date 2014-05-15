package com.thevoid.mypcrc.comm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

public class Connection {

	private static Connection instance;

	private Socket socket;

	private Connection() {
		socket = null;
	}

	public static synchronized Connection getInstance() {

		if (instance == null) {
			instance = new Connection();
			instance.connect();
			instance.authenticate();
		}

		return instance;
	}

	public synchronized void connect() {

		if (socket != null) {
			disconnect();
		}

		if (socket == null) {
			WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			DhcpInfo info = manager.getDhcpInfo();

			int host = info.ipAddress & info.netmask;
			int net = Integer.reverse(~info.netmask);
			for (--net; net > 0; net--) {
				int ip = host | Integer.reverse(net);
				if (ip != info.ipAddress) {
					InetSocketAddress address = new InetSocketAddress(
							Formatter.formatIpAddress(ip), 10101);
					socket = new Socket();
					socket.setSoTimeout(100);
					socket.setSoLinger(false, 0);
					socket.setKeepAlive(true);
					socket.setReuseAddress(false);
					try {
						socket.connect(address, 100);
						break;
					} catch (IllegalArgumentException e) {
						disconnect();
						e.printStackTrace();
					} catch (IOException e) {
						disconnect();
						e.printStackTrace();
					}
				}
			}
		}
	}

	public synchronized void disconnect() {

		if (socket != null) {
			if (socket.isConnected()) {
				if (!socket.isClosed()) {
					try {
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			socket = null;
		}
	}

	public synchronized boolean isConnected() {
		return socket != null && !socket.isClosed() && socket.isConnected();
	}

	public synchronized void send(byte[] data) {

		if (!isConnected()) {
			connect();
			authenticate();
		}

		if (isConnected()) {
			try {
				ByteArrayOutputStream output = (ByteArrayOutputStream) socket
						.getOutputStream();
				output.write(data, 0, data.length);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
				e.printStackTrace();
			} catch (IndexOutOfBoundsException e) {
				e.printStackTrace();
			}
		}
	}

	private synchronized void authenticate() {
		send("a93abd1ee31d66e5e161c2a27e3f75a7\r\n".getBytes());
	}
}
