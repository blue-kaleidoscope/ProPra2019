package propra.imageconverter.image;

import propra.imageconverter.error.ImageConverterErrorCode;
import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.util.FileHandler;
import propra.imageconverter.util.arguments.CompressionFormat;

/**
 * An <code>ImageTGA</code> describes a *.tga image file which can be handled by
 * the <code>ImageConverter</code>. It follows a subset of the official
 * specification which can be found on Wikipedia.
 * 
 * @author Oliver Eckstein
 *
 */
public class ImageTGA extends Image {
	private int origin;
	private int headerIndexOrigin;

	/**
	 * Creates a new <code>ImageTGA</code> for an existing *.tga image file
	 * This constructor should not be called for not yet
	 * existing image files (such as output image files before a conversion took
	 * place).
	 * 
	 * @param fileHandler this <code>ImageTGA</code>'s file handler which reads data from the image file.
	 * @throws ImageHandlingException when the given file handler is <code>null</code>.
	 */
	public ImageTGA(FileHandler fileHandler) throws ImageHandlingException {
		super(fileHandler);
	}

	/**
	 * Creates a new <code>ImageTGA</code> for a not yet existing image file
	 * This constructor should be called for not yet existing
	 * image files (such as output image files before a conversion took place).
	 * 
	 * @param fileHandler this <code>ImageTGA</code>'s file handler which writes data into the image file.
	 * @param compressionFormat this <code>ImageTGA</code>'s compression format.
	 * @throws ImageHandlingException when the given file handler is <code>null</code> or the given compression format
	 * was set to <code>AUTO</code> which is invalid for output images.
	 */
	public ImageTGA(FileHandler fileHandler, CompressionFormat compressionMode) throws ImageHandlingException {
		super(fileHandler, compressionMode);
	}

	@Override
	protected void setProperties() {
		headerLength = 18;
		bitsPerPixel = 24;
		origin = 32; // origin top-left
		fileExtension = "tga";
		if (compressionFormat == CompressionFormat.UNCOMPRESSED) {
			compressionDescriptionInHeader = 2;
		} else if (compressionFormat == CompressionFormat.RLE) {
			compressionDescriptionInHeader = 10;
		}

		headerIndexWidth = 12;
		headerIndexHeight = 14;
		headerIndexCompression = 2;
		headerIndexBitsPerPixel = 16;
		headerIndexOrigin = 17;
	}

	@Override
	protected void createHeader() {
		super.createHeader();
		header[headerIndexOrigin] = origin;
	}

	@Override
	protected void checkHeader() throws ImageHandlingException {
		super.checkHeader();

		// Get compression type of this input image from header
		compressionDescriptionInHeader = header[headerIndexCompression];

		// Check if compression type is valid.
		if (compressionDescriptionInHeader == 2) {
			compressionFormat = CompressionFormat.UNCOMPRESSED;
		} else if (compressionDescriptionInHeader == 10) {
			compressionFormat = CompressionFormat.RLE;
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
	}

	@Override
	public void finalizeConversion() throws ImageHandlingException {
		// Nothing to do here for TGA images.
	}

	@Override
	public long getImageDataLength() {
		return fileHandler.getFile().length() - headerLength;
	}
}
