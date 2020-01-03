package propra.imageconverter.error;

/**
 * Error codes which might be produced when using the <code>ImageConverter</code>.
 * @author Oliver Eckstein
 *
 */
public enum ImageConverterErrorCode {
	INVALID_FILEPATH(1),
	INVALID_FILEFORMAT(2),
	INVALID_HEADERDATA(3),
	INVALID_DATASEGMENT(4),
	INVALID_CHECKSUM(5),
	IO_ERROR(6),
	INVALID_FILE(7),
	COMPRESSION_ERROR(8),
	INVALID_USER_INPUT(9),
	UNEXPECTED_ERROR(10);
	
	
	private int errorCode;
	
	ImageConverterErrorCode(int errorCode) {
		this.errorCode = errorCode;		
	}
	
	public int getErrorCode() {
		return errorCode;
	}
}
