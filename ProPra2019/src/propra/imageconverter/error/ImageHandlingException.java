package propra.imageconverter.error;

public class ImageHandlingException extends Exception {
	
	private int errorCode;
	
	private static final long serialVersionUID = 1L;
	
	public ImageHandlingException(String msg, ErrorCodes errorCode) {
		//super(msg + " Error code: " + errorCode.getErrorCode());
		this.errorCode = errorCode.getErrorCode();
		System.err.println(msg + " Error code: " + errorCode.getErrorCode());
		this.printStackTrace();
		System.out.println(" +++ ImageConverter was shut down with errors +++");
	}
	
	public Integer getErrorCode() {
        return errorCode;
    }

}
