package propra.imageconverter;

import java.io.File;

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
	public ImageTGA(File file) throws ImageHandlingException {
		super(file);
	}

	public ImageTGA(File file, int compressionMode) throws ImageHandlingException {
		super(file, compressionMode);
	}

	@Override
	protected void setProperties() {
		headerLength = 18;
		bitsPerPixel = 24;
		origin = 32; // origin top-left
		fileExtension = "tga";
		if(compressionMode == UNCOMPRESSED) {
			compressionType = 2;
		} else {
			compressionType = 10;
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
		compressionType = header[headerCompression];

		// Check if compression type is valid.
		if (compressionType == 2) {
			compressionMode = UNCOMPRESSED;
		} else if (compressionType == 10) {
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

		

	}

	@Override
	protected void finalizeConversion() throws ImageHandlingException {
		// Nothing to do here for TGA images.		
	}
}
