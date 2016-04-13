// vim: syntax=java noexpandtab:
package ca.dioo.java.motqueser;

import java.net.Socket;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.PushbackInputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Iterator;

import ca.dioo.java.libmotqueser.XmlFactory;
import ca.dioo.java.libmotqueser.MessageFactory;
import ca.dioo.java.libmotqueser.Message;
import ca.dioo.java.libmotqueser.ClientMessage;
import ca.dioo.java.libmotqueser.ControlMessage;
import ca.dioo.java.libmotqueser.ErrorMessage;
import ca.dioo.java.libmotqueser.BaseServerMessage;
import ca.dioo.java.libmotqueser.ServerMessage;
import ca.dioo.java.libmotqueser.NotificationMessage;
import ca.dioo.java.libmotqueser.MalformedMessageException;

class NotificationThread extends Thread {
	private BufferedOutputStream os;
	private BufferedInputStream is;
	private PushbackInputStream pis;
	private PrintWriter wtr;
	private BufferedReader rdr;

	private ServerSocket mServSock;
	private Queue<Socket> mSockQueue;
	private ItemQueueListener mIql;


	public NotificationThread() {
		mServSock = new ServerSocket(Config.getNotificationPort());
		mSockQueue = new ConcurrentLinkedQueue<Socket>();
		mIql = new ItemQueueListener();
		ItemQueue.setNewEventListener(mIql);
	}


	public class ItemQueueListener implements ItemQueue.OnNewEventListener {
		ItemQueueListener() {
		}

		public void onNewEvent(ItemWithId it) {
			for (Iterator<Socket> i = mSockQueue.iterator(); i.hasNext(); ) {
				Socket sock = i.next();
				BufferedOutputStream os = new BufferedOutputStream(sock.getOutputStream());
				PrintWriter wtr = new PrintWriter(os, true);

				NotificationMessage nm = new NotificationMessage();
				try {
					nm.setItem(new BaseServerMessage.Item(it.getId(), it.getImgSize(), it.getVidSize(), it.getVidLen()));
				} catch (IOException e) {
					throw new Error("problem determining media size", e);
				}
				wtr.println(nm.getXmlString());

				wtr.flush();
				os.flush();
			}
		}
	}


	try {
		pis = new PushbackInputStream(sock.getInputStream());
		is = new BufferedInputStream(pis);
		wtr = new PrintWriter(os, true);
		rdr = new BufferedReader(new InputStreamReader(is));
	} catch (IOException e) {
		throw new Error("Error creating " + this.getClass().getName(), e);
	}


	public void run() {
		try {
			END: while (true) {
				sockCol.add(servSock.accept());
			}
			Utils.debugPrintln(2, "Closing thread");
		} catch (IOException e) {
			System.err.println(ca.dioo.java.commons.Utils.getPrettyStackTrace(e));
			System.err.println("Exception in thread \n" + e.getMessage());
		} finally {
			try {
				servSock.close();
			} catch (IOException e) {
				//Pass
			}
		}
	}
}
