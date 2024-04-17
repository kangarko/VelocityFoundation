package org.mineacademy.vfo.exception;

/**
 * Thrown when a command has invalid argument
 */
public final class InvalidCommandArgException extends CommandException {

	private static final long serialVersionUID = 1L;

	public InvalidCommandArgException() {
	}

	public InvalidCommandArgException(String message) {
		super(message);
	}
}