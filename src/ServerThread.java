package ca.dioo.java.SurveillanceServer;

import java.net.*;
import java.io.*;

import ca.dioo.java.MonitorLib.XmlFactory;
import ca.dioo.java.MonitorLib.MessageFactory;
import ca.dioo.java.MonitorLib.Message;
import ca.dioo.java.MonitorLib.ClientMessage;
import ca.dioo.java.MonitorLib.ServerMessage;
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
				Message m = MessageFactory.parse(XmlFactory.newXmlParser(in));

				if (!(m instanceof ClientMessage)) {
					System.err.println("Bogus message, discarding: Message is not a ClientMessage");
				} else {
					ClientMessage cm = (ClientMessage)m;
					Utils.debugPrintln(1, "Client Message:" + cm);

					ServerMessage sm = new ServerMessage(cm.getVersion());
					sm.buildAsResponse(cm);
					ServerMessage.SubMessage sub = sm.getSubMessage();
					if (sub instanceof ServerMessage.ItemList) {
						ServerMessage.ItemList il = (ServerMessage.ItemList)sub;

						ItemQueue.ItemBundle ib = ItemQueue.getItems(il.getPrevId());
						if (ib != null) {
							for (Item it: ib) {
								il.add(new ServerMessage.Item(it.getId()));
							}
						}
						out.println(sm.getXmlString());
					} else {
						System.err.println("Unimplement response: " + sub.getClass().getName());
					}
					Utils.debugPrintln(1, "Server Message: " + sm);
				}
			} catch (MalformedMessageException e) {
				System.err.println(ca.dioo.java.MonitorLib.Utils.getPrettyStackTrace(e));
				System.err.println("Bogus message, discarding: " + e.getMessage());
			}

			Utils.debugPrintln(2, "Closing thread");
			sock.close();
		} catch (IOException e) {
			System.err.println("Exception in thread \n" + e.getMessage());
		}
	}
}
