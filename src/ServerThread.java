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

import ca.dioo.java.libmotqueser.XmlFactory;
import ca.dioo.java.libmotqueser.MessageFactory;
import ca.dioo.java.libmotqueser.Message;
import ca.dioo.java.libmotqueser.ClientMessage;
import ca.dioo.java.libmotqueser.ControlMessage;
import ca.dioo.java.libmotqueser.ErrorMessage;
import ca.dioo.java.libmotqueser.ServerMessage;
import ca.dioo.java.libmotqueser.MalformedMessageException;

class ServerThread extends Thread {
	private Socket sock;
	private BufferedOutputStream os;
	private BufferedInputStream is;
	private PushbackInputStream pis;
	private PrintWriter wtr;
	private BufferedReader rdr;

	public ServerThread(Socket socket) {
		sock = socket;

		try {
			os = new BufferedOutputStream(sock.getOutputStream());
			pis = new PushbackInputStream(sock.getInputStream());
			is = new BufferedInputStream(pis);
			wtr = new PrintWriter(os, true);
			rdr = new BufferedReader(new InputStreamReader(is));
		} catch (IOException e) {
			throw new Error("Error creating " + this.getClass().getName(), e);
		}
	}


	public void run() {
		try {
			int b;
			END: while (!sock.isClosed() && (b = pis.read()) > -1) {
				pis.unread(b);
				Message m = MessageFactory.parse(XmlFactory.newXmlParser(rdr));

				if (m instanceof ClientMessage) {
					processClientMessage((ClientMessage) m);

				} else if (m instanceof ControlMessage) {
					if (!sock.getInetAddress().getHostAddress().equals("127.0.0.1")) {
						String msg = "Access forbidden";
						ErrorMessage em = new ErrorMessage(msg);
						wtr.println(em.getXmlString());
						System.err.println(msg);

					} else {
						System.out.println("control connection detected");
						processControlMessage((ControlMessage) m);
					}
				} else {
					System.err.println("Bogus message type received, closing connection: Message is a " + m.getClass().getName());
					break END;
				}
			}
			Utils.debugPrintln(2, "Closing thread");

		} catch (MalformedMessageException e) {
			System.err.println(ca.dioo.java.commons.Utils.getPrettyStackTrace(e));
			System.err.println("Bogus message, discarding: " + e.getMessage());
		} catch (IOException e) {
			System.err.println(ca.dioo.java.commons.Utils.getPrettyStackTrace(e));
			System.err.println("Exception in thread \n" + e.getMessage());
		} finally {
			try {
				sock.close();
			} catch (IOException e) {
				//Pass
			}
		}
	}


	private void processControlMessage(ControlMessage cm) throws IOException {
		int id = -1;

		try {
			StringBuffer sb = new StringBuffer();
			sb.append("Control Message:");
			for (ControlMessage.Item cmit: cm) {
				sb.append(" Item: " + cmit.getId());

				Item it = Utils.getItemFromString(cmit.getId());

				if (ItemQueue.isSnoozed()) {
					Utils.debugPrintln(1, "Snoozed, deleting media files for " + cmit.getId());

					//FIXME: delete media? maybe add a configuration parameter to control this?
					Utils.deleteByItem(it);
				} else {
					ItemQueue.add(it);
				}
			}
			System.out.println(sb.toString());
			//FIXME: fine-grained response
			wtr.println("success");

		//FIXME: fine-grained logging if not response
		} catch (IllegalArgumentException e) {
			String errMsg = "Forbidden add request for ID " + id;
			ErrorMessage em = new ErrorMessage(errMsg);
			System.err.println(errMsg);
			wtr.println(em.getXmlString());
		}
	}


	private void processClientMessage(ClientMessage cm)
			throws UnsupportedOperationException, IOException {

		Utils.debugPrintln(3, "Client Message:" + cm);

		ServerMessage sm = new ServerMessage(cm.getVersion());
		sm.buildAsResponse(cm);
		ServerMessage.Response resp = sm.getResponse();

		if (resp instanceof ServerMessage.ItemListResponse) {
			processItemListRequest((ServerMessage.ItemListResponse) resp, sm);

		} else if (resp instanceof ServerMessage.ItemResponse) {
			processItemRequest((ServerMessage.ItemResponse) resp, sm);

		} else if (resp instanceof ServerMessage.ItemDeletionResponse) {
			processItemDeletionRequest((ServerMessage.ItemDeletionResponse) resp, sm);

		} else if (resp instanceof ServerMessage.ItemPreservationResponse) {
			processItemPreservationRequest((ServerMessage.ItemPreservationResponse) resp, sm);

		} else if (resp instanceof ServerMessage.SnoozeResponse) {
			processSnoozeRequest((ServerMessage.SnoozeResponse) resp, sm);

		} else if (resp instanceof ServerMessage.UnsnoozeResponse) {
			processUnsnoozeRequest((ServerMessage.UnsnoozeResponse) resp, sm);

		} else if (resp instanceof ServerMessage.ConfigResponse) {
			processConfigRequest((ServerMessage.ConfigResponse) resp, sm);

		} else {
			throw new UnsupportedOperationException("Unimplemented response: " + resp.getClass().getName());
		}
		Utils.debugPrintln(3, "Server Message: " + sm);
	}


	private void processItemListRequest(ServerMessage.ItemListResponse il, ServerMessage sm) {
		ItemQueue.ItemBundle ib = ItemQueue.getItems(il.getPrevId());
		if (ib != null) {
			for (ItemQueue.ItemWithId it: ib) {
				try {
					il.add(new ServerMessage.Item(it.getId(), it.getImgSize(), it.getVidSize(), it.getVidLen()));
				} catch (IOException e) {
					throw new Error("problem determining media size", e);
				}
			}
		}
		wtr.println(sm.getXmlString());
	}


	private void processItemRequest(ServerMessage.ItemResponse smit, ServerMessage sm)
			throws UnsupportedOperationException, IOException {

		Path mediaPath = null;
		try {
			switch (smit.getMediaType()) {
			case VID:
				{
					Item it = ItemQueue.get(smit.getId());
					mediaPath = it.getVidPath();
				}
				break;
			case IMG:
				{
					Item it = ItemQueue.get(smit.getId());
					mediaPath = it.getImgPath();
				}
				break;
			default:
				throw new UnsupportedOperationException("Unknown media type: " + smit.getMediaType());
			}

			File mediaFile = new File(mediaPath.toString());

			//FIXME: should improve this eventually
			if (mediaFile.length() > Integer.MAX_VALUE) {
				throw new UnsupportedOperationException("File " + mediaPath.toString() + " is WAY too large");
			} else {
				int fileLen = (int) mediaFile.length();
				byte[] fileContent = new byte[fileLen];
				Utils.debugPrintln(3, "file \"" + mediaPath.toString() + "\" is " + fileLen + " bytes long");
				int len = (new FileInputStream(mediaFile)).read(fileContent);

				smit.setMediaSize(len);
				wtr.println(sm.getXmlString());
				wtr.flush();
				os.write(new byte[]{(byte)0xEE, (byte)0x00, (byte)0xFF}, 0, 3);
				os.write(fileContent, 0, fileContent.length);
				os.flush();
			}
		} catch (ItemNotFoundException e) {
			String errMsg = "No file matching filter for id " + smit.getId();
			System.err.println(errMsg);

			ErrorMessage em = new ErrorMessage(errMsg);
			wtr.println(em.getXmlString());
		}
	}


	private void processItemDeletionRequest(ServerMessage.ItemDeletionResponse idr, ServerMessage sm) {
		boolean canRemove = ItemQueue.remove(idr.getId());
		if (!canRemove) {
			String errMsg = "Disallowed deletion request for ID " + idr.getId();
			ErrorMessage em = new ErrorMessage(errMsg);
			System.err.println(errMsg);
			wtr.println(em.getXmlString());

		} else {
			try {
				System.out.println("Deleting item id " + idr.getId());
				Utils.deleteByItem(ItemQueue.get(idr.getId()));
				wtr.println(sm.getXmlString());
			} catch (ItemNotFoundException e) {
				throw new Error("Error", e);
			}
		}
	}


	private void processItemPreservationRequest(ServerMessage.ItemPreservationResponse ipr, ServerMessage sm) {
		boolean itemExists = ItemQueue.keep(ipr.getId());
		if (!itemExists) {
			String errMsg = "Disallowed preservation request for ID " + ipr.getId();
			ErrorMessage em = new ErrorMessage(errMsg);
			System.err.println(errMsg);
			wtr.println(em.getXmlString());

		} else {
			System.out.println("Keeping item id " + ipr.getId());
			wtr.println(sm.getXmlString());
		}
	}


	private void processSnoozeRequest(ServerMessage.SnoozeResponse sr, ServerMessage sm) {
		int interval = sr.getSnoozeInterval();

		System.out.println("Snoozing for " + (interval / 60) + " minutes");
		ItemQueue.snoozeFor(interval);
		wtr.println(sm.getXmlString());
	}


	private void processUnsnoozeRequest(ServerMessage.UnsnoozeResponse ur, ServerMessage sm) {
		System.out.println("Unsnoozing");
		ItemQueue.unsnooze();
		wtr.println(sm.getXmlString());
	}


	private void processConfigRequest(ServerMessage.ConfigResponse cr, ServerMessage sm) {
		cr.add("notification_port", Integer.toString(NotificationThread.getPort()));
		wtr.println(sm.getXmlString());
	}
}
