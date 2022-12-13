package it.finanze.sanita.fse2.ms.gtwindexer.exceptions;

public class NoRecordFoundException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4420700371354323215L;

	/**
	 * Message constructor.
	 * 
	 * @param msg	Message to be shown.
	 */
	public NoRecordFoundException(final String msg) {
		super(msg);
	}
	
	/**
	 * Complete constructor.
	 * 
	 * @param msg	Message to be shown.
	 * @param e		Exception to be shown.
	 */
	public NoRecordFoundException(final String msg, final Exception e) {
		super(msg, e);
	}
	
	/**
	 * Exception constructor.
	 * 
	 * @param e	Exception to be shown.
	 */
	public NoRecordFoundException(final Exception e) {
		super(e);
	}
	
}