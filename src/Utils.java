package ca.dioo.java.SurveillanceServer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.DirectoryStream;

public class Utils {
	public static int dbgLvl = 0;

	public static void debugPrintln(int minLvl, String msg) {
		if (dbgLvl >= minLvl) {
			System.out.println(msg);
		}
	}


	public static Path getPathFromId(int itemId) throws IOException {
		Path itemPath = null;
		Path mediaDir = null;

		try {
			String dirStr = (new BufferedReader(new FileReader("dir.conf"))).readLine();
			mediaDir = FileSystems.getDefault().getPath(dirStr);
			DirectoryStream<Path> ds = Files.newDirectoryStream(mediaDir, itemId + "-*.avi");

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
}

