package com.thevoid.mypcrc.comm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

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

	private void getKnownHosts(Set<String> hosts) {

		hosts.clear();
		SharedPreferences preferences = app.getSharedPreferences("settings",
				Context.MODE_PRIVATE);
		hosts.addAll(Arrays.asList(preferences.getString("knownHosts",
				"[192.168.0.26]").split("\\[,\\]")));
	}

	private void setKnownHosts(Set<String> hosts) {

		SharedPreferences preferences = app.getSharedPreferences("settings",
				Context.MODE_PRIVATE);
		Editor edit = preferences.edit();
		edit.putString("knownHosts", Arrays.toString(hosts.toArray()));
		edit.commit();
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

		if (isWifiConnected()) { // TODO: !
			return;
		}

		if (socket != null) {
			disconnect();
		}

		if (socket == null) {
			HashSet<String> hosts = new HashSet<String>();
			getKnownHosts(hosts);

			for (String host : hosts) {
				InetSocketAddress address = new InetSocketAddress(host, 10101);
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
					socket.connect(address, 100);
					return;
				} catch (IllegalArgumentException e) {
					disconnect();
					e.printStackTrace();
				} catch (IOException e) {
					disconnect();
					e.printStackTrace();
				}
			}

			WifiManager manager = (WifiManager) app
					.getSystemService(Context.WIFI_SERVICE);
			DhcpInfo info = manager.getDhcpInfo();

			int host = info.ipAddress & info.netmask;
			int net = Integer.reverse(~info.netmask);
			for (--net; net > 0; net--) {
				int ip = host | Integer.reverse(net);
				if (ip != info.ipAddress) {
					InetSocketAddress address = new InetSocketAddress(
							Formatter.formatIpAddress(ip), 10101);
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
						socket.connect(address, 100);
						hosts.add(Formatter.formatIpAddress(ip));
						setKnownHosts(hosts);
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
