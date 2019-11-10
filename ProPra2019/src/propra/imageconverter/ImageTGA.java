package propra.imageconverter;

import java.io.File;

public class ImageTGA extends Image {
	private byte origin;
	
	private int headerOrigin;
	
	/**
	 * Creates a new <code>ImageTGA</code> for an existing tga image file according to the <code>filePath</code>.
	 * Do not call this constructor for not yet existing image files (such as output image files before a
	 * conversion took place).
	 * @param filePath the path to an existing tga image file.
	 * @throws ImageHandlingException an exception is thrown when this <code>ImageTGA</code> could not be created out of
	 * the file.
	 */
	public ImageTGA(File file, boolean imageType) throws ImageHandlingException {
		super(file, imageType);
	}	

	@Override
	protected void setProperties() {
		headerLength = 18;
		compressionType = 2; // uncompressed RGB
		bitsPerPixel = 24;
		origin = 32; // origin top-left
		fileExtension = "tga";
		
		headerWidth = 12;
		headerHeight = 14;
		headerCompression = 2;
		headerBitsPerPixel = 16;
		headerOrigin = 17;
	}
	
	@Override
	protected void checkHeader() throws ImageHandlingException {		

		// Check if compression type is valid.
		if (compressionType != header[headerCompression]) {			
			throw new ImageHandlingException("Invalid compression of source file.", 
					ErrorCodes.INVALID_HEADERDATA);
		}

		// Get source image dimensions from header.		
		width = (header[headerWidth + 1] << 8) + header[headerWidth];		
		height = (header[headerHeight + 1] << 8) + header[headerHeight];

		// Check if one dimension is zero.
		String errorMessage;
		if (width <= 0 || height <= 0) {
			errorMessage = "Source file corrupt. Invalid image dimensions.";
			throw new ImageHandlingException(errorMessage, ErrorCodes.INVALID_HEADERDATA);
		}

		// Check if actual image data length fits to dimensions given in the header.
		if (file.length() - headerLength != height * width * 3) {
			errorMessage = "Source file corrupt. Image data length does not fit to header information.";
			throw new ImageHandlingException(errorMessage, ErrorCodes.INVALID_HEADERDATA);
		}

	}
	
	@Override
	protected void createHeader() {
		super.createHeader();
		header[headerOrigin] = origin;
	}
}
