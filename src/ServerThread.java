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

import ca.dioo.java.libmotqueser.XmlFactory;
import ca.dioo.java.libmotqueser.MessageFactory;
import ca.dioo.java.libmotqueser.Message;
import ca.dioo.java.libmotqueser.ClientMessage;
import ca.dioo.java.libmotqueser.ServerMessage;
import ca.dioo.java.libmotqueser.ErrorMessage;
import ca.dioo.java.libmotqueser.MalformedMessageException;

class ServerThread extends Thread {
	private static final Class THIS_CLASS = ServerThread.class;

	private Socket sock;

	public ServerThread(Socket sock) {
		this.sock = sock;
	}

	public void run() {
		try (
			BufferedOutputStream bos = new BufferedOutputStream(sock.getOutputStream());
			PrintWriter out = new PrintWriter(bos, true);
			BufferedInputStream bis = new BufferedInputStream(sock.getInputStream());
			BufferedReader in = new BufferedReader(new InputStreamReader(bis));
		) {
			try {
				Message m = MessageFactory.parse(XmlFactory.newXmlParser(in));

				if (!(m instanceof ClientMessage)) {
					System.err.println("Bogus message, discarding: Message is not a ClientMessage");
				} else {
					processClientMessage((ClientMessage)m, bis, in, bos, out);
				}
			} catch (MalformedMessageException e) {
				System.err.println(ca.dioo.java.commons.Utils.getPrettyStackTrace(e));
				System.err.println("Bogus message, discarding: " + e.getMessage());
			}

			Utils.debugPrintln(2, "Closing thread");
			sock.close();
		} catch (IOException e) {
			System.err.println("Exception in thread \n" + e.getMessage());
		}
	}


	public static void processClientMessage(ClientMessage cm,
			BufferedInputStream bis,
			BufferedReader in,
			BufferedOutputStream bos,
			PrintWriter out) {
		Utils.debugPrintln(1, "Client Message:" + cm);

		ServerMessage sm = new ServerMessage(cm.getVersion());
		sm.buildAsResponse(cm);
		ServerMessage.Response resp = sm.getResponse();
		if (resp instanceof ServerMessage.ItemListResponse) {
			ServerMessage.ItemListResponse il = (ServerMessage.ItemListResponse)resp;

			ItemQueue.ItemBundle ib = ItemQueue.getItems(il.getPrevId());
			if (ib != null) {
				for (Item it: ib) {
					il.add(new ServerMessage.Item(it.getId()));
				}
			}
			out.println(sm.getXmlString());
		} else if (resp instanceof ServerMessage.ItemResponse) {
			ServerMessage.ItemResponse it = (ServerMessage.ItemResponse)resp;

			Path mediaPath;
			try {
				switch (it.getMediaType()) {
				case VID:
					mediaPath = Utils.getVideoPathFromId(it.getId());
					break;
				case IMG:
					mediaPath = Utils.getImagePathFromId(it.getId());
					break;
				default:
					throw new Error("Unknown media type: " + it.getMediaType());
				}

				if (mediaPath == null) {
					String errMsg = "No file matching filter for id " + it.getId();
					System.err.println(errMsg);

					ErrorMessage em = new ErrorMessage(errMsg);
					out.println(em.getXmlString());
				} else {
					File mediaFile = new File(mediaPath.toString());
					if (mediaFile.length() > Integer.MAX_VALUE) {
						throw new Error("File " + mediaPath.toString() + " is WAY too large");
					} else {
						int fileLen = (int)mediaFile.length();
						byte[] fileContent = new byte[fileLen];
System.out.println("file \"" + mediaPath.toString() + "\" is " + fileLen + " bytes long");
						int len = (new FileInputStream(mediaFile)).read(fileContent);

						it.setMediaSize(len);
						out.println(sm.getXmlString());
						out.flush();
						bos.write(new byte[]{(byte)0xEE, (byte)0x00, (byte)0xFF}, 0, 3);
						bos.write(fileContent, 0, fileContent.length);
						bos.flush();
						bos.close();
					}
				}
			} catch (IOException e) {
				System.err.println("Error matching ID:" + e.getMessage());
			}
		} else if (resp instanceof ServerMessage.ItemDeletionResponse) {
			ServerMessage.ItemDeletionResponse idr = (ServerMessage.ItemDeletionResponse)resp;
			boolean canRemove = ItemQueue.remove(idr.getId());
			if (!canRemove) {
				String errMsg = "Disallowed deletion request for ID " + idr.getId();
				ErrorMessage em = new ErrorMessage(errMsg);
				System.err.println(errMsg);
				out.println(em.getXmlString());

			} else {
				System.err.println("Deleting item id " + idr.getId());
				Utils.deleteById(idr.getId());
				out.println(sm.getXmlString());
			}

		} else if (resp instanceof ServerMessage.ItemPreservationResponse) {
			ServerMessage.ItemPreservationResponse ipr = (ServerMessage.ItemPreservationResponse)resp;

			boolean itemExists = ItemQueue.keep(ipr.getId());
			if (!itemExists) {
				String errMsg = "Disallowed preservation request for ID " + ipr.getId();
				ErrorMessage em = new ErrorMessage(errMsg);
				System.err.println(errMsg);
				out.println(em.getXmlString());

			} else {
				System.err.println("Keeping item id " + ipr.getId());
				out.println(sm.getXmlString());
			}

		} else if (resp instanceof ServerMessage.SnoozeResponse) {
			ServerMessage.SnoozeResponse sr = (ServerMessage.SnoozeResponse)resp;

			int interval = sr.getSnoozeInterval();
			ItemQueue.snoozeFor(interval);
			out.println(sm.getXmlString());

		} else if (resp instanceof ServerMessage.UnsnoozeResponse) {
			ServerMessage.UnsnoozeResponse sr = (ServerMessage.UnsnoozeResponse)resp;
			ItemQueue.unsnooze();

		} else {
			System.err.println("Unimplemented response: " + resp.getClass().getName());
		}
		Utils.debugPrintln(1, "Server Message: " + sm);
	}
}
