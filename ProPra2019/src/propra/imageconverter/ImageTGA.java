package propra.imageconverter;

public class ImageTGA extends Image {
	private byte origin;
	
	/**
	 * Creates a new <code>ImageTGA</code> for an existing tga image file according to the <code>filePath</code>.
	 * Do not call this constructor for not yet existing image files (such as output image files before a
	 * conversion took place).
	 * @param filePath the path to an existing tga image file.
	 * @throws ImageHandlingException an exception is thrown when this <code>ImageTGA</code> could not be created out of
	 * the file.
	 */
	public ImageTGA(String filepath) throws ImageHandlingException {
		super(filepath);
	}
	
	/**
	 * Creates a new <code>ImageTGA</code> for a not yet existing tga image file.
	 * Use this constructor for not yet existing tga image files (such as output tga image files before
	 * a conversion took place).
	 */
	public ImageTGA() {
		super();
	}

	

	@Override
	protected void setProperties() {
		headerLength = 18;
		compressionType = 2; // uncompressed RGB
		bitsPerPixel = 24;
		origin = 32; // origin top-left
		fileExtension = "tga";
	}
	
	@Override
	protected void checkHeader() throws ImageHandlingException {		

		// Check if compression type is valid.
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
	protected void setWidthInHeader() throws ImageHandlingException {		
		byte[] widthInBytes = ImageHelper.hexStringToByteArray(
				Integer.toHexString(width));
		try {
			imageData[12] = widthInBytes[1];
			imageData[13] = widthInBytes[0];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ImageHandlingException("Invalid header data.", 
					ErrorCodes.INVALID_HEADERDATA);
		}
		
	}

	@Override
	protected void setHeightInHeader() throws ImageHandlingException {		
		byte[] widthInBytes = ImageHelper.hexStringToByteArray(
				Integer.toHexString(height));
		try {
			imageData[14] = widthInBytes[1];
			imageData[15] = widthInBytes[0];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ImageHandlingException("Invalid header data.", 
					ErrorCodes.INVALID_HEADERDATA);
		}
		
				
	}
	
	@Override
	protected void createHeader() {
		imageData = new byte[headerLength];
		imageData[2] = compressionType; // uncompressed RGB
		imageData[16] = bitsPerPixel;
		imageData[17] = origin;
	}
}
