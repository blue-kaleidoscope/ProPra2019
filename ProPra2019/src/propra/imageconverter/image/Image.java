package propra.imageconverter.image;

import java.io.File;

import propra.imageconverter.error.ErrorCodes;
import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.util.FileHandler;
import propra.imageconverter.util.Util;
import propra.imageconverter.util.arguments.CompressionFormat;

/**
 * This class describes an image which can be handled by the ImageConverter.
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
	protected byte bitsPerPixel;
	protected String fileExtension;
	protected FileHandler fileHandler;
	protected CompressionFormat compressionFormat;
	
	protected int headerHeight;
	protected int headerWidth;
	protected int headerCompression;
	protected int headerBitsPerPixel;

	/**
	 * Creates a new <code>Image</code> for an existing image file according to the
	 * <code>filePath</code>. Do not call this constructor for not yet existing
	 * image files (such as output image files before a conversion took place).
	 * 
	 * @param filePath the path to an existing image file.
	 * @throws ImageHandlingException an exception is thrown when this
	 *                                <code>Image</code> could not be created out of
	 *                                the file.
	 */
	public Image(FileHandler fileHandler) throws ImageHandlingException {
		if (fileHandler == null) {
			throw new ImageHandlingException(
					"Invalid input file. Cannot create Image.", ErrorCodes.IO_ERROR);
		}
		this.fileHandler = fileHandler;		
		setProperties();
		
		byte[] header = fileHandler.readData(headerLength);
		this.header = Util.byteArrayToIntArray(header);
		checkHeader();		
	}

	public Image(FileHandler fileHandler, CompressionFormat compressionFormat) throws ImageHandlingException {
		if (fileHandler == null) {
			throw new ImageHandlingException(
					"Invalid output file. Cannot create Image.", ErrorCodes.IO_ERROR);
		}
		
		if(compressionFormat == CompressionFormat.AUTO) {
			throw new ImageHandlingException(
					"'auto' is not allowed as an output compression format.", ErrorCodes.UNEXPECTED_ERROR); 
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
	
	public abstract long getImageDataLength();
	
	public long getUncompressedImageDataLength() {
		return width * height * 3;
	}

	public CompressionFormat getCompressionMode() {
		return compressionFormat;
	}

	public void setDimensions(Image inputImage) throws ImageHandlingException {
		width = inputImage.getWidth();
		height = inputImage.getHeight();

		header[headerWidth] = (byte) width;
		header[headerWidth + 1] = (byte) (width >> 8);

		header[headerHeight] = (byte) height;
		header[headerHeight + 1] = (byte) (height >> 8);
	}

	/**
	 * Call this method when file content needs to be prepared after an image conversion took place.
	 * @throws ImageHandlingException when conversion could not be finalized.
	 */
	public abstract void finalizeConversion() throws ImageHandlingException;

	/**
	 * To define properties which are unique for this type of <code>Image</code>.
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
		
		if(header[headerBitsPerPixel] != 24) {
			throw new ImageHandlingException("Source file corrupt. Invalid bit depth. This program currently only allows 24 bit images.",
					ErrorCodes.INVALID_HEADERDATA);
		}

		// Get source image dimensions from header.
		width = (header[headerWidth + 1] << 8) + header[headerWidth];
		height = (header[headerHeight + 1] << 8) + header[headerHeight];

		// Check if one dimension is zero.
		if (width <= 0 || height <= 0) {
			throw new ImageHandlingException("Source file corrupt. Invalid image dimensions.",
					ErrorCodes.INVALID_HEADERDATA);
		}
	}

	/**
	 * To create a new header of this <code>Image</code> based on the type of the
	 * <code>Image</code>.
	 */
	protected void createHeader() {
		header = new int[headerLength];
		header[headerCompression] = (byte) compressionDescriptionInHeader;
		header[headerBitsPerPixel] = bitsPerPixel;
	}

}
