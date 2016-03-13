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

public class Utils {
	public static int dbgLvl = 0;
	public static Runtime rt = Runtime.getRuntime();

	public static void debugPrintln(int minLvl, String msg) {
		if (dbgLvl >= minLvl) {
			System.out.println(msg);
		}
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


	public static boolean deleteById(int itemId) {
		boolean wasDeleted = false;

		try {
			DirectoryStream<Path> ds = Files.newDirectoryStream(Config.getMediaDir(),
					Config.getDeletePrefix() + itemId + Config.getDeleteSuffix());

			debugPrintln(3, "dir = " + Config.getMediaDir() + " glob = " + Config.getDeletePrefix() + itemId + Config.getDeleteSuffix());
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


	public static Path getVideoPathFromId(int itemId) throws IOException {
		Path itemPath = null;

		try {
			DirectoryStream<Path> ds = Files.newDirectoryStream(Config.getMediaDir(),
					itemId + "-*.avi");

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


	public static Path getImagePathFromId(int itemId) throws IOException {
		Path itemPath = null;

		try {
			DirectoryStream<Path> ds = Files.newDirectoryStream(Config.getMediaDir(), itemId + "-*.jpg");

			ArrayList<Path> al = new ArrayList<Path>();
			for (Path p: ds) {
				al.add(p);
				debugPrintln(2, p.toString());
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
