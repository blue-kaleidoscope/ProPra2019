package propra.imageconverter.error;

/**
 * An <code>ImageHandlingException</code> extends the class
 * <code>Exception</code> and is thrown when an error occurred during the use of
 * the <code>ImageConverter</code>.
 * 
 * @author Oliver Eckstein
 *
 */
public class ImageHandlingException extends Exception {

	private int errorCode;

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new <code>ImageHandlingException</code>.
	 * 
	 * @param msg the exception message
	 * @param errorCode the error code
	 */
	public ImageHandlingException(String msg, ImageConverterErrorCode errorCode) {
		this.errorCode = errorCode.getErrorCode();
		System.err.println(msg + " Error code: " + errorCode.getErrorCode());
		this.printStackTrace();
		System.out.println(" +++ ImageConverter was shut down with errors +++");
	}

	public Integer getErrorCode() {
		return errorCode;
	}

}
