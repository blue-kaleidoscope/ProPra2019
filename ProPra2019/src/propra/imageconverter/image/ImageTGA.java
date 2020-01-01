package propra.imageconverter.image;

import propra.imageconverter.error.ErrorCodes;
import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.util.FileHandler;
import propra.imageconverter.util.arguments.CompressionFormat;

public class ImageTGA extends Image {
	private byte origin;

	private int headerOrigin;

	/**
	 * Creates a new <code>ImageTGA</code> for an existing tga image file according
	 * to the <code>filePath</code>. Do not call this constructor for not yet
	 * existing image files (such as output image files before a conversion took
	 * place).
	 * 
	 * @param filePath the path to an existing tga image file.
	 * @throws ImageHandlingException an exception is thrown when this
	 *                                <code>ImageTGA</code> could not be created out
	 *                                of the file.
	 */
	public ImageTGA(FileHandler fileHandler) throws ImageHandlingException {
		super(fileHandler);
	}

	public ImageTGA(FileHandler fileHandler, CompressionFormat compressionMode) throws ImageHandlingException {
		super(fileHandler, compressionMode);
	}

	@Override
	protected void setProperties() {
		headerLength = 18;
		bitsPerPixel = 24;
		origin = 32; // origin top-left
		fileExtension = "tga";
		if(compressionFormat == CompressionFormat.UNCOMPRESSED) {
			compressionDescriptionInHeader = 2;
		} else if(compressionFormat == CompressionFormat.RLE) {
			compressionDescriptionInHeader = 10;
		}
		
		headerWidth = 12;
		headerHeight = 14;
		headerCompression = 2;
		headerBitsPerPixel = 16;
		headerOrigin = 17;
	}

	@Override
	protected void createHeader() {
		super.createHeader();
		header[headerOrigin] = origin;
	}

	@Override
	protected void checkHeader() throws ImageHandlingException {
		super.checkHeader();
		
		// Get compression type of this input image from header
		compressionDescriptionInHeader = header[headerCompression];

		// Check if compression type is valid.
		if (compressionDescriptionInHeader == 2) {
			compressionFormat = CompressionFormat.UNCOMPRESSED;
		} else if (compressionDescriptionInHeader == 10) {
			compressionFormat = CompressionFormat.RLE;
		} else {
			throw new ImageHandlingException("Invalid compression of source file.", ErrorCodes.INVALID_HEADERDATA);
		}

		// Check if actual image data length fits to dimensions given in the header.
		if (fileHandler.getFile().length() - headerLength < height * width * 3 && compressionFormat == CompressionFormat.UNCOMPRESSED) {
			throw new ImageHandlingException(
					"Source file corrupt. Image data length does not fit to header information.",
					ErrorCodes.INVALID_HEADERDATA);
		}
	}

	@Override
	public void finalizeConversion() throws ImageHandlingException {
		// Nothing to do here for TGA images.		
	}

	@Override
	public long getImageDataLength() {
		return height * width * 3;
	}
}
