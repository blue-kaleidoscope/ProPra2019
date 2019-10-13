package propra.imageconverter;

public class ImageHandlingException extends Exception {
	
	private int errorCode;
	
	private static final long serialVersionUID = 1L;
	
	public ImageHandlingException(String msg, ErrorCodes errorCode) {
		super(msg + " Error code: " + errorCode.getErrorCode());
		this.errorCode = errorCode.getErrorCode();
	}
	
	public Integer getErrorCode() {
        return errorCode;
    }

}
