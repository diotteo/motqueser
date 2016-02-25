package ca.dioo.java.SurveillanceServer;

import java.net.Socket;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import ca.dioo.java.MonitorLib.Message;
import ca.dioo.java.MonitorLib.ControlMessage;
import ca.dioo.java.MonitorLib.XmlFactory;
import ca.dioo.java.MonitorLib.MessageFactory;
import ca.dioo.java.MonitorLib.MalformedMessageException;


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
				Message m = MessageFactory.parse(XmlFactory.newXmlParser(in));

				if (!(m instanceof ControlMessage)) {
					System.err.println("Bogus message, discarding: Message is not a ControlMessage");
				} else {
					StringBuffer sb = new StringBuffer();
					ControlMessage cm = (ControlMessage)m;
					sb.append("Control Message:");
					for (ControlMessage.Item it: cm) {
						sb.append(" Item: " + it.getId());
						for (ControlMessage.Media med: it) {
							sb.append(" Media: " + med.getPath());
							ItemQueue.add(new Item(med.getPath(), it.getId()));
						}
					}
					System.out.println(sb.toString());
					//FIXME: fine-grained response
					out.println("success");
				}
			} catch (MalformedMessageException e) {
				System.err.println(ca.dioo.java.commons.Utils.getPrettyStackTrace(e));
				System.err.println("Bogus message, discarding: " + e.getMessage());
			} catch (IllegalArgumentException e) {
				//FIXME: fine-grained response
				out.println("failure: " + e.getMessage());
			}

			Utils.debugPrintln(2, "Closing thread");
			sock.close();
		} catch (IOException e) {
			System.err.println("Exception in thread \n" + e.getMessage());
		}
	}
}
