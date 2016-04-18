// vim: syntax=java noexpandtab:
package ca.dioo.java.motqueser;

import java.net.Socket;
import java.net.SocketException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.io.File;
import java.io.IOException;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
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
	private static int notificationPort = -1;

	private BufferedOutputStream os;
	private PrintWriter wtr;

	private ServerSocket mServSock;
	private Queue<SocketWrapper> mSockQueue;
	private ItemQueueListener mIql;


	static class SocketWrapper {
		private Socket sock;
		private BufferedOutputStream bos;
		private PrintWriter wtr;

		SocketWrapper(Socket sock) throws IOException {
			this.sock = sock;
			bos = new BufferedOutputStream(sock.getOutputStream());
			wtr = new PrintWriter(bos, true);
		}

		PrintWriter getWriter() {
			return wtr;
		}

		OutputStream getOutputStream() {
			return bos;
		}
	}


	public static int getPort() {
		return notificationPort;
	}


	public NotificationThread(int port) {
		try {
			mServSock = new ServerSocket(port);
		} catch (IOException e) {
			throw new Error("Error opening ServerSocket: " + e.getMessage(), e);
		}
		NotificationThread.notificationPort = mServSock.getLocalPort();

		mSockQueue = new ConcurrentLinkedQueue<SocketWrapper>();
		mIql = new ItemQueueListener();
		ItemQueue.setQueueChangeListener(mIql);
	}


	public class ItemQueueListener implements ItemQueue.OnQueueChangeListener {
		ItemQueueListener() {
		}

		public void onNewItem(ItemQueue.ItemWithId it) {
			for (Iterator<SocketWrapper> i = mSockQueue.iterator(); i.hasNext(); ) {
				SocketWrapper sock = i.next();
				NotificationMessage nm = new NotificationMessage();
				try {
					nm.setItem(new NotificationMessage.NewItem(
							new BaseServerMessage.Item(it.getId(), it.getEventId(), it.getImgSize(), it.getVidSize(), it.getVidLen())));
				} catch (IOException e) {
					throw new Error("problem determining media size", e);
				}

				synchronized (sock) {
					try {
						PrintWriter wtr = sock.getWriter();
						wtr.println(nm.getXmlString());
						wtr.flush();
						sock.getOutputStream().flush();
					} catch (SocketException e) {
Utils.debugPrintln(3, "Dropping problematic connection");
						i.remove();
					} catch (IOException e) {
						throw new Error("Error manipulating output stream in " + this.getClass().getName() + ": " + e.getMessage(), e);
					}
				}
			}
		}


		public void onItemRemoved(int itemId) {
			for (Iterator<SocketWrapper> i = mSockQueue.iterator(); i.hasNext(); ) {
				SocketWrapper sock = i.next();
				NotificationMessage nm = new NotificationMessage();
				nm.setItem(new NotificationMessage.RemovedItem(itemId));

				synchronized (sock) {
					try {
						PrintWriter wtr = sock.getWriter();
						wtr.println(nm.getXmlString());
						wtr.flush();
						sock.getOutputStream().flush();
					} catch (SocketException e) {
Utils.debugPrintln(3, "Dropping problematic connection");
						i.remove();
					} catch (IOException e) {
						throw new Error("Error manipulating output stream in " + this.getClass().getName() + ": " + e.getMessage(), e);
					}
				}
			}
		}
	}


	public void run() {
		try {
			END: while (true) {
				mSockQueue.add(new SocketWrapper(mServSock.accept()));
			}
		} catch (IOException e) {
			System.err.println(ca.dioo.java.commons.Utils.getPrettyStackTrace(e));
			System.err.println("Exception in thread \n" + e.getMessage());
		} finally {
			Utils.debugPrintln(2, "Closing thread");
			try {
				mServSock.close();
			} catch (IOException e) {
				//Pass
			}
		}
	}
}
