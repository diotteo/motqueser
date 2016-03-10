package ca.dioo.java.motqueser;

import java.net.Socket;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import ca.dioo.java.libmotqueser.Message;
import ca.dioo.java.libmotqueser.ControlMessage;
import ca.dioo.java.libmotqueser.ClientMessage;
import ca.dioo.java.libmotqueser.ErrorMessage;
import ca.dioo.java.libmotqueser.XmlFactory;
import ca.dioo.java.libmotqueser.MessageFactory;
import ca.dioo.java.libmotqueser.MalformedMessageException;


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

				if (m instanceof ClientMessage) {
					Utils.debugPrintln(2, "ClientMessage received in ControlThread, redirecting");
					ServerThread.processClientMessage((ClientMessage)m, null, in, null, out);
				} else if (!(m instanceof ControlMessage)) {
					System.err.println("Bogus message, discarding: Message is not a ControlMessage");
				} else {
					processControlMessage((ControlMessage)m, in, out);
				}
			} catch (MalformedMessageException e) {
				System.err.println(ca.dioo.java.commons.Utils.getPrettyStackTrace(e));
				System.err.println("Bogus message, discarding: " + e.getMessage());
			} catch (IllegalArgumentException e) {
				ErrorMessage em = new ErrorMessage();
				em.setErrorMessage("Something went wrong with control_message: " + e.getMessage());
				out.println(em.getXmlString());
			}

			Utils.debugPrintln(2, "Closing thread");
			sock.close();
		} catch (IOException e) {
			System.err.println("Exception in thread \n" + e.getMessage());
		}
	}


	public static void processControlMessage(ControlMessage cm, BufferedReader in, PrintWriter out) {
		StringBuffer sb = new StringBuffer();
		sb.append("Control Message:");
		for (ControlMessage.Item it: cm) {
			sb.append(" Item: " + it.getId());
			if (ItemQueue.isSnoozed()) {
				Utils.debugPrintln(2, "Snoozed, deleting media files for " + it.getId());

				//FIXME: delete media? maybe add a configuration parameter to control this?
				Utils.deleteById(it.getId());
			} else {
				try {
					ItemQueue.add(new Item(it.getId()));
				} catch (IOException e) {
					throw new Error("IOException occured: " + e.getMessage(), e);
				}
			}
		}
		System.out.println(sb.toString());
		//FIXME: fine-grained response
		out.println("success");
	}
}
