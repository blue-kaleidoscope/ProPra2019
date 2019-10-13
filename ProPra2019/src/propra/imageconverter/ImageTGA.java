package propra.imageconverter;

public class ImageTGA extends Image {
	private byte origin;
	
	public ImageTGA(String filepath) throws ImageHandlingException {
		super(filepath);
	}
	
	public ImageTGA() {
		super();
	}

	@Override
	protected void checkHeader() throws ImageHandlingException {		

		// Check if compression is valid.
		String hexTmp;
		hexTmp = String.format("%02x", imageData[2]);
		if (compressionType != Integer.parseInt(hexTmp, 16)) {			
			throw new ImageHandlingException("Invalid compression of source file.", 
					ErrorCodes.INVALID_HEADERDATA);
		}

		// Get source image dimensions from header.
		hexTmp = String.format("%02x", imageData[13]) + String.format("%02x", imageData[12]);
		width = Integer.parseInt(hexTmp, 16);
		hexTmp = String.format("%02x", imageData[15]) + String.format("%02x", imageData[14]);
		height = Integer.parseInt(hexTmp, 16);

		// Check if one dimension is zero.
		String errorMessage;
		if (width <= 0 || height <= 0) {
			errorMessage = "Source file corrupt. Invalid image dimensions.";
			throw new ImageHandlingException(errorMessage, ErrorCodes.INVALID_HEADERDATA);
		}

		// Check if actual image data length fits to dimensions given in the header.
		if (imageData.length - headerLength != height * width * 3) {
			errorMessage = "Source file corrupt. Image data length does not fit to header information.";
			throw new ImageHandlingException(errorMessage, ErrorCodes.INVALID_HEADERDATA);
		}

	}

	@Override
	protected void setProperties() {
		headerLength = 18;
		compressionType = 2; // uncompressed RGB
		bitsPerPixel = 24; // 24 bits per pixel
		origin = 32; // origin top-left
		fileExtension = "tga";
	}

	@Override
	protected void setWidthInHeader() {		
		byte[] widthInBytes = ImageHelper.hexStringToByteArray(
				Integer.toHexString(width));
		imageData[12] = widthInBytes[1];
		imageData[13] = widthInBytes[0];
	}

	@Override
	protected void setHeightInHeader() {		
		byte[] widthInBytes = ImageHelper.hexStringToByteArray(
				Integer.toHexString(height));
		imageData[14] = widthInBytes[1];
		imageData[15] = widthInBytes[0];		
	}
	
	@Override
	protected void createHeader() {
		imageData = new byte[headerLength];
		imageData[2] = compressionType; // uncompressed RGB
		imageData[16] = bitsPerPixel;
		imageData[17] = origin;
	}
}
