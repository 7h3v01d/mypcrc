package com.thevoid.mypcrc.comm;

import java.io.IOException;
import java.net.Socket;

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
		}

		return instance;
	}

	public void connect() {

		if (socket != null) {
			disconnect();
		}

		if (socket == null) {

		}
	}

	public void disconnect() {

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

	public void send(byte[] data) {

	}
}
