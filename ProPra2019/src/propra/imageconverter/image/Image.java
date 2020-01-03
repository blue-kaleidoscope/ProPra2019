package propra.imageconverter.image;

import java.io.File;

import propra.imageconverter.error.ImageConverterErrorCode;
import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.util.FileHandler;
import propra.imageconverter.util.Util;
import propra.imageconverter.util.arguments.CompressionFormat;

/**
 * An <code>Image</code> describes an image file which can be handled by the
 * <code>ImageConverter</code>.
 * 
 * @author Oliver Eckstein
 *
 */
public abstract class Image {
	protected int headerLength;
	protected int[] header;
	protected int width;
	protected int height;
	protected int compressionDescriptionInHeader;
	protected int bitsPerPixel;
	protected String fileExtension;
	protected FileHandler fileHandler;
	protected CompressionFormat compressionFormat;

	protected int headerIndexHeight;
	protected int headerIndexWidth;
	protected int headerIndexCompression;
	protected int headerIndexBitsPerPixel;

	/**
	 * Creates a new <code>Image</code> for an existing image file This constructor
	 * should not be called for not yet existing image files (such as output image
	 * files before a conversion took place).
	 * 
	 * @param fileHandler this <code>Image</code>'s file handler which reads data
	 *                    from the image file.
	 * @throws ImageHandlingException when the given file handler is
	 *                                <code>null</code>.
	 */
	public Image(FileHandler fileHandler) throws ImageHandlingException {
		if (fileHandler == null) {
			throw new ImageHandlingException("Invalid input file. Cannot create Image.",
					ImageConverterErrorCode.IO_ERROR);
		}
		this.fileHandler = fileHandler;
		setProperties();

		byte[] header = fileHandler.readNBytes(headerLength);
		this.header = Util.byteArrayToIntArray(header);
		checkHeader();
	}

	/**
	 * Creates a new <code>Image</code> for a not yet existing image file This
	 * constructor should be called for not yet existing image files (such as output
	 * image files before a conversion took place).
	 * 
	 * @param fileHandler       this <code>Image</code>'s file handler which writes
	 *                          data into the image file.
	 * @param compressionFormat this <code>Image</code>'s compression format.
	 * @throws ImageHandlingException when the given file handler is
	 *                                <code>null</code> or the given compression
	 *                                format was set to <code>AUTO</code> which is
	 *                                invalid for output images.
	 */
	public Image(FileHandler fileHandler, CompressionFormat compressionFormat) throws ImageHandlingException {
		if (fileHandler == null) {
			throw new ImageHandlingException("Invalid output file. Cannot create Image.",
					ImageConverterErrorCode.IO_ERROR);
		}

		if (compressionFormat == CompressionFormat.AUTO) {
			throw new ImageHandlingException("'auto' is not allowed as an output compression format.",
					ImageConverterErrorCode.UNEXPECTED_ERROR);
		}

		this.fileHandler = fileHandler;
		this.compressionFormat = compressionFormat;
		setProperties();
		createHeader();
	}

	public byte[] getHeader() {
		byte[] byteHeader = new byte[header.length];
		for (int i = 0; i < byteHeader.length; i++) {
			byteHeader[i] = (byte) header[i];
		}
		return byteHeader;
	}

	public String getPath() {
		return fileHandler.getFilePath();
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public String getExtension() {
		return fileExtension;
	}

	public File getFile() {
		return fileHandler.getFile();
	}

	/**
	 * To get the length of this <code>Image</code>'s data segment.
	 * 
	 * @return the length of the data segment.
	 */
	public abstract long getImageDataLength();

	/**
	 * To get the length of this <code>Image</code>'s uncompressed data segment.
	 * 
	 * @return the length of the uncompressed data segment.
	 */
	public long getUncompressedImageDataLength() {
		return width * height * 3;
	}

	public CompressionFormat getCompressionMode() {
		return compressionFormat;
	}

	/**
	 * To set the dimensions for this image.
	 * 
	 * @param width  the image's width.
	 * @param height the image's height.
	 */
	public void setDimensions(int width, int height) {
		header[headerIndexWidth] = (byte) width;
		header[headerIndexWidth + 1] = (byte) (width >> 8);

		header[headerIndexHeight] = (byte) height;
		header[headerIndexHeight + 1] = (byte) (height >> 8);
	}

	/**
	 * Call this method when file content of the output file needs to be prepared
	 * after an image conversion took place.
	 * 
	 * @throws ImageHandlingException when the conversion could not be finalized.
	 */
	public abstract void finalizeConversion() throws ImageHandlingException;

	/**
	 * To define properties which are unique for this <code>Image</code>.
	 */
	protected abstract void setProperties();

	/**
	 * To check whether the header of this <code>Image</code> is valid according to
	 * its properties.
	 * 
	 * @throws ImageHandlingException an exception is thrown when the header
	 *                                information does not fit to this type of
	 *                                <code>Image</code>.
	 */
	protected void checkHeader() throws ImageHandlingException {

		if (header[headerIndexBitsPerPixel] != 24) {
			throw new ImageHandlingException(
					"Source file corrupt. Invalid bit depth. This program currently only allows 24 bit images.",
					ImageConverterErrorCode.INVALID_HEADERDATA);
		}

		// Get source image dimensions from header.
		width = (header[headerIndexWidth + 1] << 8) + header[headerIndexWidth];
		height = (header[headerIndexHeight + 1] << 8) + header[headerIndexHeight];

		// Check if one dimension is zero.
		if (width <= 0 || height <= 0) {
			throw new ImageHandlingException("Source file corrupt. Invalid image dimensions.",
					ImageConverterErrorCode.INVALID_HEADERDATA);
		}
	}

	/**
	 * To create a new header of this <code>Image</code> based on the type of the
	 * <code>Image</code>.
	 */
	protected void createHeader() {
		header = new int[headerLength];
		header[headerIndexCompression] = (byte) compressionDescriptionInHeader;
		header[headerIndexBitsPerPixel] = bitsPerPixel;
	}

}
