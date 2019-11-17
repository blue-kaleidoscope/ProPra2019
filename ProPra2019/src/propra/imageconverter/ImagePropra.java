package propra.imageconverter;

import java.io.File;

public class ImagePropra extends Image {
	private final String HEADER_TEXT = "ProPraWS19";
	private long dataLength;

	/**
	 * Creates a new <code>ImagePropra</code> for an existing propra image file
	 * according to the <code>filePath</code>. Do not call this constructor for not
	 * yet existing image files (such as output image files before a conversion took
	 * place).
	 * 
	 * @param filePath the path to an existing propra image file.
	 * @throws ImageHandlingException an exception is thrown when this
	 *                                <code>ImagePropra</code> could not be created
	 *                                out of the file.
	 */
	public ImagePropra(File file) throws ImageHandlingException {
		super(file);
	}

	public ImagePropra(File file, int compressionMode) throws ImageHandlingException {
		super(file, compressionMode);
	}
	
	/**
	 * To set the length of the data segment this PROPRA image.
	 * @param dataLength the data segment's length
	 */
	public void setDataLength(long dataLength) {
		this.dataLength = dataLength;
	}

	@Override
	protected void setProperties() {
		headerLength = 28;
		bitsPerPixel = 24;
		fileExtension = "propra";
		if(compressionMode == UNCOMPRESSED) {
			compressionType = 0;
		} else {
			compressionType = 1;
		}

		headerWidth = 10;
		headerHeight = 12;
		headerBitsPerPixel = 14;
		headerCompression = 15;
	}

	@Override
	protected void checkHeader() throws ImageHandlingException {
		super.checkHeader();

		// Get compression type of this input image from header
		compressionType = header[headerCompression];

		// Check if compression type is valid.
		if (compressionType == 0) {
			compressionMode = UNCOMPRESSED;
		} else if (compressionType == 1) {
			compressionMode = RLE;
		} else {
			throw new ImageHandlingException("Invalid compression of source file.", ErrorCodes.INVALID_HEADERDATA);
		}

		// Check if actual image data length fits to dimensions given in the header.
		if (file.length() - headerLength != height * width * 3 && compressionMode == Image.UNCOMPRESSED) {
			throw new ImageHandlingException(
					"Source file corrupt. Image data length does not fit to header information.",
					ErrorCodes.INVALID_HEADERDATA);
		}

		/*
		 * Check if length of data segment from header and image dimensions from header
		 * are valid.
		 */
		// Get the size of the data segment
		long dataLength = header[16] + (header[17] << 8) + (header[18] << 16) + (header[19] << 24) + (header[20] << 32)
				+ (header[21] << 40) + (header[22] << 48) + (header[23] << 56);
		// Compare the size of the data segment with the image dimensions
		if (dataLength != width * height * 3 && this.compressionMode == Image.UNCOMPRESSED) {
			throw new ImageHandlingException("Source file corrupt. Invalid image size information in header.",
					ErrorCodes.INVALID_HEADERDATA);
		}

		/*
		 * Check if length of data segment from header and actual length of data segment
		 * are equal.
		 */
		if (dataLength != file.length() - headerLength) {
			throw new ImageHandlingException("Source file corrupt. Invalid image data length information in header.",
					ErrorCodes.INVALID_HEADERDATA);
		}

		/*
		 * Check for valid checksum.
		 */
		// Compare the actual checksum with the checksum from the header
		byte[] checkSum = ConverterHelper.getCheckSum(file, header.length);
		for (int i = 0; i < 4; i++) {
			if ((checkSum[i] & 0xFF) != header[24 + i]) {
				throw new ImageHandlingException("Source file corrupt. Invalid check sum.",
						ErrorCodes.INVALID_CHECKSUM);
			}
		}

	}

	@Override
	protected void createHeader() {
		super.createHeader();
		byte[] proPraBytes = HEADER_TEXT.getBytes();
		for (int i = 0; i < HEADER_TEXT.length(); i++) {
			header[i] = proPraBytes[i];
		}
	}

	@Override
	public void finalizeConversion() throws ImageHandlingException {

		/*
		 * Write the length of the data segment into the header (little-endian).
		 */
		long sizeOfDataSegment = 0; 
		if(compressionMode == Image.UNCOMPRESSED) {
			sizeOfDataSegment = width * height * 3;
		} else {
			sizeOfDataSegment = dataLength;
		}
		header[16] = (byte) sizeOfDataSegment;
		header[17] = (byte) (sizeOfDataSegment >> 8);
		header[18] = (byte) (sizeOfDataSegment >> 16);
		header[19] = (byte) (sizeOfDataSegment >> 24);
		header[20] = (byte) (sizeOfDataSegment >> 32);
		header[21] = (byte) (sizeOfDataSegment >> 40);
		header[22] = (byte) (sizeOfDataSegment >> 48);
		header[23] = (byte) (sizeOfDataSegment >> 56);

		/*
		 * Write check sum into the header (little-endian).
		 */
		byte[] checkSum = ConverterHelper.getCheckSum(file, header.length);
		for (int i = 0; i < checkSum.length; i++) {
			header[24 + i] = checkSum[i];
		}

		ConverterHelper.writeHeaderIntoFile(this);
	}
}
