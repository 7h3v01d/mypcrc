package com.thevoid.mypcrc.comm;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.app.Application;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class Connection {

	private static Connection instance;

	private Application app;
	private Socket socket;

	private Connection(Application application) {

		app = application;
		socket = null;
	}

	public static synchronized Connection getInstance(Application application) {

		if (instance == null) {
			instance = new Connection(application);
		}

		return instance;
	}

	public boolean isWifiConnected() {

		WifiManager manager = (WifiManager) app
				.getSystemService(Context.WIFI_SERVICE);
		if (manager.isWifiEnabled() == false) {
			return false;
		}

		WifiInfo info = manager.getConnectionInfo();
		if (info == null) {
			return false;
		}

		return true;
	}

	public synchronized void connect() {

		if (!isWifiConnected()) {
			return;
		}

		if (socket != null) {
			disconnect();
		}

		if (socket == null) {
			InetSocketAddress address = new InetSocketAddress("192.168.0.26",
					10101);
			socket = new Socket();
			try {
				socket.setSoTimeout(100);
				socket.setSoLinger(false, 0);
				socket.setKeepAlive(true);
				socket.setReuseAddress(false);
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				socket.connect(address, 0);
				return;
			} catch (IllegalArgumentException e) {
				disconnect();
				e.printStackTrace();
			} catch (IOException e) {
				disconnect();
				e.printStackTrace();
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

	public synchronized void send(String data) {
		send(data.getBytes());
	}

	public synchronized void send(byte[] data) {

		if (!isConnected()) {
			connect();

			if (isConnected()) {
				authenticate();
			}
		}

		if (isConnected()) {
			try {
				BufferedOutputStream output = new BufferedOutputStream(
						socket.getOutputStream(), 64);
				output.write(data, 0, data.length);
				output.flush();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
				e.printStackTrace();
			} catch (IndexOutOfBoundsException e) {
				e.printStackTrace();
			}
		}
	}

	public synchronized void authenticate() {
		send("AUTH a93abd1ee31d66e5e161c2a27e3f75a7\r\n");
	}
}
