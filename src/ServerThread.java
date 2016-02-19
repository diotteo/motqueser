package ca.dioo.java.SurveillanceServer;

import java.net.*;
import java.io.*;

import ca.dioo.java.MonitorLib.Utils;
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
					System.out.println("Bogus message, discarding: Message is not a ClientMessage");
				} else {
					ClientMessage cm = (ClientMessage)m;
					System.out.println("Client Message:" + cm);

					//TODO: build a ServerMessage and send it to the client
					ServerMessage sm = new ServerMessage(cm.getVersion());
					sm.buildAsResponse(cm);
					ServerMessage.SubMessage sub = sm.getSubMessage();
					if (sub instanceof ServerMessage.ItemList) {
						ServerMessage.ItemList il = (ServerMessage.ItemList)sub;

						ItemQueue.ItemBundle ib = ItemQueue.getItems(il.getPrevId());
						if (ib == null) {
							//out.println("No messages!");
						} else {
							for (Item it: ib) {
								//out.println("message: " + it);
								il.add(new ServerMessage.Item(it.getId()));
							}
							//out.println("last_id:" + ib.getLastId());
						}
						out.println(sm.getXmlString());
					} else {
						System.out.println("Unimplement response: " + sub.getClass().getName());
					}
					System.out.println("Server Message: " + sm);
				}
			} catch (MalformedMessageException e) {
				System.out.println(Utils.getPrettyStackTrace(e));
				System.out.println("Bogus message, discarding: " + e.getMessage());
			}

			System.out.println("Closing thread");
			sock.close();
		} catch (IOException e) {
			System.err.println("Exception in thread \n" + e.getMessage());
		}
	}
}
