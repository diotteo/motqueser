// vim: syntax=java noexpandtab:
package ca.dioo.java.motqueser;

import java.io.File;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.DirectoryStream;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.text.ParseException;

public class Utils {
	public static final int MAX_EVENT_LEN = 600;
	public static int dbgLvl = 0;
	public static Runtime rt = Runtime.getRuntime();

	public static void debugPrintln(int minLvl, String msg) {
		if (dbgLvl >= minLvl) {
			System.out.println(msg);
		}
	}


	public static String findEventByIndex(int idx) {
		String eventStr = null;

		try {
			String glob = idx + "-*.avi";
			DirectoryStream<Path> ds = Files.newDirectoryStream(Config.getMediaDir(),
					idx + "-*.avi");

			Path eventPath = null;
			for (Path p: ds) {
				if (eventPath != null) {
					throw new Error("Too many videos match " + glob);
				} else {
					eventPath = p;
				}
			}
			if (eventPath == null) {
				throw new Error("No match for " + glob);
			}
			String s = eventPath.getFileName().toString();
			eventStr = s.substring(0, s.indexOf("."));
		} catch (IOException|StringIndexOutOfBoundsException e) {
			throw new Error("Error matching file:" + e.getMessage(), e);
		}
		return eventStr;
	}


	public static long getTimestampFromString(String id) throws ParseException {
		String a[] = id.split("-");
		return new SimpleDateFormat("yyyyMMddHHmmss").parse(a[1]).getTime() / 1000;
	}


	public static Item getItemFromString(String id) throws IOException {
		//FIXME: motion-specific rule
		int idx = -1;
		long timestamp = -1;
		try {
			String a[] = id.split("-");
			idx = Integer.parseInt(a[0]);
			timestamp = getTimestampFromString(id);
		} catch (ParseException|NumberFormatException e) {
			throw new Error("something went wrong parsing ControlMessage id", e);
		}
		Item it = new Item(idx, timestamp);

		return it;
	}


	public static String getVidLen(Path vidPath) {
		String vidLen = null;

		try {
			Process p = rt.exec("avprobe " + vidPath.toString());
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));

			String s;
			while (null != (s = br.readLine())) {
				if (s.contains("Duration")) {
					String[] a = s.split("\\s|\\.");
					if (a.length > 3) {
						vidLen = a[3];
					}
					break;
				}
			}
		} catch (IOException e) {
			throw new Error(e.getMessage(), e);
		}

		return vidLen;
	}


	public static boolean deleteByItem(Item it) {
		boolean wasDeleted = false;

		try {
			String glob = Config.getDeletePrefix();
			glob += getGlobFromItem(it, null) + Config.getDeleteSuffix();
			DirectoryStream<Path> ds = Files.newDirectoryStream(Config.getMediaDir(), glob);

			debugPrintln(3, "dir = " + Config.getMediaDir() + " glob = " + glob);
			for (Path p: ds) {
				debugPrintln(2, p.toString());

				p.toFile().delete();
				wasDeleted = true;
			}
		} catch (IOException e) {
			throw new Error("Error matching file:" + e.getMessage(), e);
		}

		return wasDeleted;
	}


	private static String getGlobFromItem(Item it, String ext) {
		String glob = it.getEventId() + "*";
		if (ext != null) {
			glob += "." + ext;
		}

		if (it.getIdx() < 10) {
			glob = "0" + glob;
		}

		return glob;
	}


	public static Path getVideoPathFromItem(Item it) throws IOException {
		Path itemPath = null;

		try {
			String glob = getGlobFromItem(it, "avi");
			DirectoryStream<Path> ds = Files.newDirectoryStream(Config.getMediaDir(),
					glob);

			for (Path p: ds) {
				if (itemPath != null) {
					throw new IOException("More than one file matches filter, aborting.");
				}
				itemPath = p;
				debugPrintln(2, p.toString());
			}
		} catch (IOException e) {
			throw new IOException("Error matching file:" + e.getMessage(), e);
		}

		return itemPath;
	}


	public static Path getImagePathFromItem(Item it) throws IOException {
		Path itemPath = null;

		try {
			String glob = getGlobFromItem(it, "jpg");
			DirectoryStream<Path> ds = Files.newDirectoryStream(Config.getMediaDir(), glob);

			long ts = it.getTimestamp();
			ArrayList<Path> al = new ArrayList<Path>();
			for (Path p: ds) {
				try {
					long imgTs = getTimestampFromString(p.getFileName().toString());
					if (imgTs >= ts && imgTs < ts + MAX_EVENT_LEN) {
						al.add(p);
						debugPrintln(2, p.toString());
					}
				} catch (ParseException e) {
					//Pass
				}
			}

			if (al.size() > 0) {
				itemPath = al.get((al.size() - 1) / 2);
			}
		} catch (IOException e) {
			throw new IOException("Error matching file:" + e.getMessage(), e);
		}

		return itemPath;
	}
}
