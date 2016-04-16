package ca.dioo.java.motqueser;

import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

public class Version {
	public static final String VERSION;
	static {
		Properties p = new Properties();
		try {
			p.load(Version.class.getResourceAsStream("/ca/dioo/java/motqueser/version.properties"));
		} catch (IOException e) {
			throw new Error(e.getMessage(), e);
		}
		VERSION = p.getProperty("vcs_version");
		if (VERSION == null) {
			throw new Error("vcs_version is undefined!");
		}
	}

	private Version() { }
}
