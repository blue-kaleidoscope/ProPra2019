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
		if(compressionMode == UNCOMPRESSED) {
			compressionType = 2;
		} else {
			compressionType = 10;
		}
		origin = 32; // origin top-left
		fileExtension = "tga";

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

		// Check if compression type is valid.
		if (!(compressionType == 2 || compressionType == 10)) {
			throw new ImageHandlingException("Invalid compression of source file.", ErrorCodes.INVALID_HEADERDATA);
		}
	}

	@Override
	protected void finalizeConversion() {
		// Nothing to do here...

	}
}
