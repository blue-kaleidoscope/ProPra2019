package propra.imageconverter.image;

import propra.imageconverter.error.ImageConverterErrorCode;
import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.util.ChecksumCalculator;
import propra.imageconverter.util.FileHandler;
import propra.imageconverter.util.arguments.CompressionFormat;

/**
 * An <code>ImagePropra</code> describes a *.propra image file which can be
 * handled by the <code>ImageConverter</code>. It follows the specification V3.0
 * of the PROPRA-specification from Fernuniversitaet in Hagen.
 * 
 * @author Oliver Eckstein
 *
 */
public class ImagePropra extends Image {
	private final String PROPRA_IDENTIFIER = "ProPraWS19";

	/**
	 * Creates a new <code>ImagePropra</code> for an existing *.propra image file
	 * This constructor should not be called for not yet
	 * existing image files (such as output image files before a conversion took
	 * place).
	 * 
	 * @param fileHandler this <code>ImagePropra</code>'s file handler which reads data from the image file.
	 * @throws ImageHandlingException when the given file handler is <code>null</code>.
	 */
	public ImagePropra(FileHandler fileHandler) throws ImageHandlingException {
		super(fileHandler);
	}

	/**
	 * Creates a new <code>ImagePropra</code> for a not yet existing image file
	 * This constructor should be called for not yet existing
	 * image files (such as output image files before a conversion took place).
	 * 
	 * @param fileHandler this <code>ImagePropra</code>'s file handler which writes data into the image file.
	 * @param compressionFormat this <code>ImagePropra</code>'s compression format.
	 * @throws ImageHandlingException when the given file handler is <code>null</code> or the given compression format
	 * was set to <code>AUTO</code> which is invalid for output images.
	 */
	public ImagePropra(FileHandler fileHandler, CompressionFormat compressionMode) throws ImageHandlingException {
		super(fileHandler, compressionMode);
	}

	@Override
	protected void setProperties() {
		headerLength = 28;
		bitsPerPixel = 24;
		fileExtension = "propra";
		if (compressionFormat == CompressionFormat.UNCOMPRESSED) {
			compressionDescriptionInHeader = 0;
		} else if (compressionFormat == CompressionFormat.RLE) {
			compressionDescriptionInHeader = 1;
		} else if (compressionFormat == CompressionFormat.HUFFMAN) {
			compressionDescriptionInHeader = 2;
		}

		headerIndexWidth = 10;
		headerIndexHeight = 12;
		headerIndexBitsPerPixel = 14;
		headerIndexCompression = 15;
	}

	@Override
	protected void checkHeader() throws ImageHandlingException {
		super.checkHeader();

		// Get compression type of this input image from header
		compressionDescriptionInHeader = header[headerIndexCompression];

		// Check if compression type is valid.
		if (compressionDescriptionInHeader == 0) {
			compressionFormat = CompressionFormat.UNCOMPRESSED;
		} else if (compressionDescriptionInHeader == 1) {
			compressionFormat = CompressionFormat.RLE;
		} else if (compressionDescriptionInHeader == 2) {
			compressionFormat = CompressionFormat.HUFFMAN;
		} else {
			throw new ImageHandlingException("Invalid compression of source file.",
					ImageConverterErrorCode.INVALID_HEADERDATA);
		}

		// Check if actual image data length fits to dimensions given in the header.
		if (fileHandler.getFile().length() - headerLength < height * width * 3
				&& compressionFormat == CompressionFormat.UNCOMPRESSED) {
			throw new ImageHandlingException(
					"Source file corrupt. Image data length does not fit to header information.",
					ImageConverterErrorCode.INVALID_HEADERDATA);
		}

		/*
		 * Check if length of data segment from header and image dimensions from header
		 * are valid.
		 */
		// Get the size of the data segment
		long dataLength = header[16] + (header[17] << 8) + (header[18] << 16) + (header[19] << 24) + (header[20] << 32)
				+ (header[21] << 40) + (header[22] << 48) + (header[23] << 56);
		// Compare the size of the data segment with the image dimensions
		if (dataLength != width * height * 3 && this.compressionFormat == CompressionFormat.UNCOMPRESSED) {
			throw new ImageHandlingException("Source file corrupt. Invalid image size information in header.",
					ImageConverterErrorCode.INVALID_HEADERDATA);
		}

		/*
		 * Check if length of data segment from header and actual length of data segment
		 * are equal.
		 */
		if (dataLength != fileHandler.getFile().length() - headerLength) {
			throw new ImageHandlingException("Source file corrupt. Invalid image data length information in header.",
					ImageConverterErrorCode.INVALID_HEADERDATA);
		}

		/*
		 * Check for the "ProPraWS19" header entry.
		 */
		byte[] proPraBytes = PROPRA_IDENTIFIER.getBytes();
		for (int i = 0; i < PROPRA_IDENTIFIER.length(); i++) {
			if (header[i] != Byte.toUnsignedInt(proPraBytes[i])) {
				throw new ImageHandlingException("Source file is not a valid *.propra file. Header corrupt.",
						ImageConverterErrorCode.INVALID_HEADERDATA);
			}
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
						ImageConverterErrorCode.INVALID_CHECKSUM);
			}
		}

	}

	@Override
	protected void createHeader() {
		super.createHeader();
		byte[] proPraBytes = PROPRA_IDENTIFIER.getBytes();
		for (int i = 0; i < PROPRA_IDENTIFIER.length(); i++) {
			header[i] = Byte.toUnsignedInt(proPraBytes[i]);
		}
	}

	@Override
	public void finalizeConversion() throws ImageHandlingException {

		/*
		 * Write the length of the data segment into the header (little-endian).
		 */
		long sizeOfDataSegment = 0;
		if (compressionFormat == CompressionFormat.UNCOMPRESSED) {
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

		// Write the header into the output file
		fileHandler.writeDataRandomlyIntoFile(getHeader(), 0);
	}

	@Override
	public long getImageDataLength() {
		if (compressionFormat == CompressionFormat.UNCOMPRESSED) {
			return width * height * 3;
		} else {
			return fileHandler.getFile().length() - headerLength;
		}
	}
}
