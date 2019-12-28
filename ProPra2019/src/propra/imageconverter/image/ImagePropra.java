package propra.imageconverter.image;

import propra.imageconverter.error.ErrorCodes;
import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.util.ChecksumCalculator;
import propra.imageconverter.util.FileHandler;

public class ImagePropra extends Image {
	private final String HEADER_TEXT = "ProPraWS19";

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
	public ImagePropra(FileHandler fileHandler) throws ImageHandlingException {
		super(fileHandler);
	}

	public ImagePropra(FileHandler fileHandler, CompressionType compressionMode) throws ImageHandlingException {
		super(fileHandler, compressionMode);
	}

	@Override
	protected void setProperties() {
		headerLength = 28;
		bitsPerPixel = 24;
		fileExtension = "propra";
		if(compressionMode == CompressionType.UNCOMPRESSED) {
			compressionDescriptionInHeader = 0;
		} else if(compressionMode == CompressionType.RLE) {
			compressionDescriptionInHeader = 1;
		} else if(compressionMode == CompressionType.HUFFMAN) {
			compressionDescriptionInHeader = 2;
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
		compressionDescriptionInHeader = header[headerCompression];

		// Check if compression type is valid.
		if (compressionDescriptionInHeader == 0) {
			compressionMode = CompressionType.UNCOMPRESSED;
		} else if (compressionDescriptionInHeader == 1) {
			compressionMode = CompressionType.RLE;			
		}  else if (compressionDescriptionInHeader == 2) {
			compressionMode = CompressionType.HUFFMAN;
		} else {
			throw new ImageHandlingException("Invalid compression of source file.", ErrorCodes.INVALID_HEADERDATA);
		}

		// Check if actual image data length fits to dimensions given in the header.
		if (fileHandler.getFile().length() - headerLength < height * width * 3 && compressionMode == CompressionType.UNCOMPRESSED) {
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
		if (dataLength != width * height * 3 && this.compressionMode == CompressionType.UNCOMPRESSED) {
			throw new ImageHandlingException("Source file corrupt. Invalid image size information in header.",
					ErrorCodes.INVALID_HEADERDATA);
		}

		/*
		 * Check if length of data segment from header and actual length of data segment
		 * are equal.
		 */
		if (dataLength != fileHandler.getFile().length() - headerLength) {
			throw new ImageHandlingException("Source file corrupt. Invalid image data length information in header.",
					ErrorCodes.INVALID_HEADERDATA);
		}

		/*
		 * Check for valid checksum.
		 */
		// Compare the actual checksum with the checksum from the header
		ChecksumCalculator checksumCalc = new ChecksumCalculator(new FileHandler(this.getPath()));
		byte[] checkSum = checksumCalc.getCheckSum(headerLength);
		for (int i = 0; i < 4; i++) {
			if (Byte.toUnsignedInt((checkSum[i])) != header[24 + i]) {
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
		if(compressionMode == CompressionType.UNCOMPRESSED) {
			sizeOfDataSegment = width * height * 3;
		} else {
			sizeOfDataSegment = fileHandler.getFile().length() - headerLength;
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
		ChecksumCalculator checksumCalc = new ChecksumCalculator(new FileHandler(this.getPath()));
		byte[] checkSum = checksumCalc.getCheckSum(headerLength);
		for (int i = 0; i < checkSum.length; i++) {
			header[24 + i] = checkSum[i];
		}

		fileHandler.writeDataIntoFile(checkSum, 24);
	}

	@Override
	public long getImageDataLength() {
		if(compressionMode == CompressionType.UNCOMPRESSED) {
			return width * height * 3;
		} else {
			return fileHandler.getFile().length() - headerLength;
		}
	}
}
