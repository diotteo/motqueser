// vim: syntax=java noexpandtab:
package ca.dioo.java.motqueser;

public class ItemNotFoundException extends Exception {
	public ItemNotFoundException(String message) {
		super(message);
	}

	public ItemNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
