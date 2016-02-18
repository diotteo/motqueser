package ca.dioo.java.SurveillanceServer;

import java.net.*;
import java.io.*;

import ca.dioo.java.MonitorLib.Utils;
import ca.dioo.java.MonitorLib.XmlFactory;
import ca.dioo.java.MonitorLib.MessageFactory;
import ca.dioo.java.MonitorLib.ClientMessage;
import ca.dioo.java.MonitorLib.MalformedMessageException;

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
			try {
				ca.dioo.java.MonitorLib.Message m = MessageFactory.parse(XmlFactory.newXmlParser(in));

				if (!(m instanceof ClientMessage)) {
					System.out.println("Bogus message, discarding: Message is not a ClientMessage");
				} else {
					ClientMessage cm = (ClientMessage)m;
					System.out.print("Client Message:");
					for (ClientMessage.Action a: cm) {
						System.out.print(" Action: " + a.getActionType());
					}
					System.out.print("\n");

					//TODO: build a ServerMessage and send it to the client
				}
			} catch (MalformedMessageException e) {
				System.out.println(Utils.getPrettyStackTrace(e));
				System.out.println("Bogus message, discarding: " + e.getMessage());
			}

			if (false) {
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
			}
			System.out.println("Closing thread");
			sock.close();
		} catch (IOException e) {
			System.err.println("Exception in thread \n" + e.getMessage());
		}
	}
}
