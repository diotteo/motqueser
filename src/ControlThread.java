package ca.dioo.java.SurveillanceServer;

import java.net.*;
import java.io.*;

import ca.dioo.java.MonitorLib.ControlMessage;
import ca.dioo.java.MonitorLib.XmlFactory;
import ca.dioo.java.MonitorLib.MessageFactory;
import ca.dioo.java.MonitorLib.MalformedMessageException;
import ca.dioo.java.MonitorLib.Utils;

class ControlThread extends Thread {
	private static final Class THIS_CLASS = ControlThread.class;

	private Socket sock;

	public ControlThread(Socket sock) {
		this.sock = sock;
	}

	public void run() {
		try (
			PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		) {
			try {
				ca.dioo.java.MonitorLib.Message m = MessageFactory.parse(XmlFactory.newXmlParser(in));

				if (!(m instanceof ControlMessage)) {
					System.out.println("Bogus message, discarding: Message is not a ControlMessage");
				} else {
					ControlMessage cm = (ControlMessage)m;
					System.out.print("Control Message:");
					for (ControlMessage.Item it: cm) {
						System.out.print(" Item: " + it.getId());
						for (ControlMessage.Media med: it) {
							System.out.print(" Media: " + med.getPath());
							MessageProvider.addMessage(it.getId() + " " + med.getPath());
						}
					}
					System.out.print("\n");
				}
			} catch (MalformedMessageException e) {
				System.out.println(Utils.getPrettyStackTrace(e));
				System.out.println("Bogus message, discarding: " + e.getMessage());
			}

			if (false) {
				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					MessageProvider.addMessage(inputLine);
				}
			}

			System.out.println("Closing thread");
			sock.close();
		} catch (IOException e) {
			System.err.println("Exception in thread \n" + e.getMessage());
		}
	}
}
