package propra.imageconverter;

public class ImagePropra extends Image {
	private final String HEADER_TEXT = "ProPraWS19";

	/**
	 * Creates a new <code>ImagePropra</code> for an existing propra image file according to the <code>filePath</code>.
	 * Do not call this constructor for not yet existing image files (such as output image files before a
	 * conversion took place).
	 * @param filePath the path to an existing propra image file.
	 * @throws ImageHandlingException an exception is thrown when this <code>ImagePropra</code> could not be created out of
	 * the file.
	 */
	public ImagePropra(String filePath) throws ImageHandlingException {
		super(filePath);
	}
	
	/**
	 * Creates a new <code>ImagePropra</code> for a not yet existing propra image file.
	 * Use this constructor for not yet existing propra image files (such as output propra image files before
	 * a conversion took place).
	 */
	public ImagePropra() {
		super();
	}

	@Override
	protected void setProperties() {
		headerLength = 28;
		compressionType = 0; // uncompressed
		bitsPerPixel = 24;
		fileExtension = "propra";
	}

	@Override
	protected void checkHeader() throws ImageHandlingException {
		
		// Check if compression type is valid.
		String hexTmp;
		hexTmp = String.format("%02x", imageData[15]);
		if (compressionType != Integer.parseInt(hexTmp, 16)) {
			throw new ImageHandlingException("Invalid compression of source file.", ErrorCodes.INVALID_HEADERDATA);
		}

		// Get source image dimensions from header.
		hexTmp = String.format("%02x", imageData[11]) + String.format("%02x", imageData[10]);
		width = Integer.parseInt(hexTmp, 16);
		hexTmp = String.format("%02x", imageData[13]) + String.format("%02x", imageData[12]);
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

		/*
		 * Check if length of data segment from header and image dimensions from header
		 * are valid.
		 */
		// Get the size of the data segment
		hexTmp = "";
		for (int i = 0; i < 8; i++) {
			hexTmp += String.format("%02x", imageData[23 - i]);
		}

		// Compare the size of the data segment with the image dimensions
		int dataLength;
		dataLength = Integer.parseInt(hexTmp, 16);
		if (dataLength != width * height * 3) {
			errorMessage = "Source file corrupt. Invalid image size information in header.";
			throw new ImageHandlingException(errorMessage, ErrorCodes.INVALID_HEADERDATA);
		}

		/*
		 * Check if length of data segment from header and actual length of data segment
		 * are equal.
		 */
		if (dataLength != imageData.length - headerLength) {
			errorMessage = "Source file corrupt. Invalid image data length information in header.";
			throw new ImageHandlingException(errorMessage, ErrorCodes.INVALID_HEADERDATA);
		}

		/*
		 * Check for valid checksum.
		 */
		// Get the check sum from header
		hexTmp = "";
		for (int i = 0; i < 4; i++) {
			hexTmp += String.format("%02x", imageData[27 - i]);
		}

		// Compare the actual checksum with the checksum from the header
		if (!hexTmp.equals(ImageHelper.getCheckSum(this.getDataSegment()))) {
			errorMessage = "Source file corrupt. Invalid check sum.";
			throw new ImageHandlingException(errorMessage, ErrorCodes.INVALID_CHECKSUM);
		}
	}

	@Override
	protected void setWidthInHeader() throws ImageHandlingException {
		byte[] widthInBytes = ImageHelper.hexStringToByteArray(Integer.toHexString(width));
		try {
			imageData[10] = widthInBytes[1];
			imageData[11] = widthInBytes[0];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ImageHandlingException("Invalid header data.", 
					ErrorCodes.INVALID_HEADERDATA);
		}
		
		
	}

	@Override
	protected void setHeightInHeader() throws ImageHandlingException {
		byte[] widthInBytes = ImageHelper.hexStringToByteArray(Integer.toHexString(height));
		try {
			imageData[12] = widthInBytes[1];
			imageData[13] = widthInBytes[0];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ImageHandlingException("Invalid header data.", 
					ErrorCodes.INVALID_HEADERDATA);
		}		
	}

	@Override
	protected void createHeader() {
		imageData = new byte[headerLength];
		byte[] proPraBytes = HEADER_TEXT.getBytes();
		for (int i = 0; i < HEADER_TEXT.length(); i++) {
			imageData[i] = proPraBytes[i];
		}

		imageData[14] = bitsPerPixel;
		imageData[15] = compressionType;
	}

	@Override
	public void setImage(int width, int height, byte[] dataSegment) throws ImageHandlingException {
		super.setImage(width, height, dataSegment);

		/*
		 * Write the length of the data segment into the header (little-endian).
		 */
		String dataSegmentString = Integer.toHexString(imageData.length - headerLength);
		byte[] dataSegmentBytes = ImageHelper.hexStringToByteArray(dataSegmentString);
		for (int i = 0; i < dataSegmentBytes.length; i++) {
			imageData[16 + i] = dataSegmentBytes[dataSegmentBytes.length - 1 - i];
		}

		/*
		 * Write check sum into the header (little-endian).
		 */
		String checkSum = ImageHelper.getCheckSum(this.getDataSegment());
		byte[] checkSumBytes = ImageHelper.hexStringToByteArray(checkSum);
		for (int i = 0; i < checkSumBytes.length; i++) {
			imageData[24 + i] = checkSumBytes[checkSumBytes.length - 1 - i];
		}

	}

}
