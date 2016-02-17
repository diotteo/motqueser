package ca.dioo.java.SurveillanceServer;

import java.net.*;
import java.io.*;

class ServerThread extends Thread {
	private static final Class THIS_CLASS = ServerThread.class;

	private Socket sock;

	public ServerThread(Socket sock) {
		this.sock = sock;
	}

	public void run() {
		try (
			PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		) {
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				//System.out.println(inputLine);

				if (inputLine.equals("close")) {
					System.out.println("Close command received");
					break;
				} else {
					long ts = -1;
					try {
						ts = new Long(inputLine);
					} catch (NumberFormatException e) {
						System.out.println("Wrong ts: " + inputLine);
					}

					MessageProvider.MessageBundle mb = MessageProvider.getMessages(ts);
					if (mb == null) {
						out.println("No messages!");
					} else {
						for (String s: mb.getMessages()) {
							out.println(s);
						}
						out.println("ts:" + mb.getTimestamp());
					}
					break;
				}
			}
			System.out.println("Closing thread");
			sock.close();
		} catch (IOException e) {
			System.err.println("Exception in thread \n" + e.getMessage());
		}
	}
}
