package propra.imageconverter.error;

/**
 * Error codes when using ImageConverter.
 * @author Oliver Eckstein
 *
 */
public enum ErrorCodes {
	INVALID_FILEPATH(1),
	INVALID_FILEFORMAT(2),
	INVALID_HEADERDATA(3),
	INVALID_DATASEGMENT(4),
	INVALID_CHECKSUM(5),
	IO_ERROR(6),
	INVALID_FILE(7),
	INVALID_COMPRESSION(8),
	INVALID_USER_INPUT(9),
	INVALID_ARGUMENT(10);
	
	
	private int errorCode;
	
	ErrorCodes(int errorCode) {
		this.errorCode = errorCode;		
	}
	
	public int getErrorCode() {
		return errorCode;
	}
}
