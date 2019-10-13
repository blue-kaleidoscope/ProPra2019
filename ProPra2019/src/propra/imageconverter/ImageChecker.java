package propra.imageconverter;

/**
 * Perform checking operations on image files to confirm whether the image
 * files are valid for further operations or not.
 * @author Oliver Eckstein
 *
 */
public class ImageChecker {	
	
	/**
	 * To check whether an image conversion from <code>inputPath</code> to <code>outputPath</code> is valid.
	 * Only allowed conversions are from <code>tga</code> to <code>propra</code> or vice versa.
	 * @param inputPath the file path to the original image.
	 * @param outputPath the file path of the desired output.
	 * @return <code>true</code> if the desired conversion can be performed.<br><code>false</code> if the input file is
	 * invalid in terms of
	 * <p><ul>
	 * <li>its file path
	 * <li>its file extension
	 * <li>its header data
	 * <li>its data segment.
	 * </ul>
	 * This method also returns <code>false</code> when the output path is invalid or the output extension is unknown.
	 * @throws ImageHandlingException when conversion cannot be performed.
	 */
	public boolean isValid(String inputPath, String outputPath) throws ImageHandlingException {
		byte[] inputImageData = checkFilepath(inputPath, outputPath);
		return checkImageHeader(inputPath, inputImageData);
	}
	
	/**
	 * To check either whether the given file path of the original image is valid or the
	 * output file is valid.
	 * @param inputPath the file path to the original image.
	 * @param outputPath the desired output file path.
	 * @return a byte array containing the data of the original image including the header data and the
	 * data segment.
	 * @throws ImageHandlingException when input or output path are invalid or the conversion type is invalid.
	 * Only allowed conversions are from <code>tga</code> to <code>propra</code> or vice versa.
	 */
	private byte[] checkFilepath(String inputPath, String outputPath) throws ImageHandlingException {
		String errorMessage;
		if (inputPath == null || outputPath == null || inputPath.equals("") || outputPath.equals("")) {
			errorMessage = "Invalid file path for input or output file.";
			throw new ImageHandlingException(errorMessage, ErrorCodes.INVALID_FILEPATH);			
		}
		
		String inputExtension = ImageHelper.getFileExtension(inputPath);
		String outputExtension = ImageHelper.getFileExtension(outputPath);
		
		/*
		 * Check if input or output file format is known. Either *.tga or *.propra
		 */
		if (!inputExtension.equals("tga") && !inputExtension.equals("propra")) {
			errorMessage = "Unknown input file format: " + inputExtension + ".";
			throw new ImageHandlingException(errorMessage, ErrorCodes.INVALID_FILEFORMAT);			
		}

		if (!outputExtension.equals("tga") && !outputExtension.equals("propra")) {
			errorMessage = "Unknown output file format: " + outputExtension + ".";
			throw new ImageHandlingException(errorMessage, ErrorCodes.INVALID_FILEFORMAT);	
		}

		byte[] inputImageData = ImageHelper.getBytesOfFile(inputPath);
		
		System.out.println("Input and output file format successfully checked. " + inputExtension + " --> "
				+ outputExtension);
		return inputImageData;
	}
	
	/**
	 * To check the validity of the header data of the original image.
	 * @param imagePath the file path of the original image.
	 * @param imageData a byte representation of the original image file including the header data and the
	 * data segment.
	 * @return <code>true</code> if header data is correct else <code>false</code>.
	 * @throws ImageHandlingException if the header data or the given file path is invalid.
	 */
	private boolean checkImageHeader(String imagePath, byte[] imageData) throws ImageHandlingException {
		String errorMessage;
		
		if (imagePath == null || imagePath.equals("")) {
			errorMessage = "Invalid file path.";
			throw new ImageHandlingException(errorMessage, ErrorCodes.INVALID_FILEPATH);	
		}
		
		if (imageData == null) {
			errorMessage = "Given file is empty.";
			throw new ImageHandlingException(errorMessage, ErrorCodes.INVALID_HEADERDATA);	
		}
		
		String fileExtension = ImageHelper.getFileExtension(imagePath);
		int width = 0; // Width of the image according to the header
		int height = 0; // Height of the image according to the header
		int compressionType = -1; // Compression type according to the header
		int headerLength = 0; // Length of the header according to the file extension
		int dataLength;	// Length of the data segment according to the header	
		String hexTmp; // A temporary string for HEX to INT conversions
		boolean headerValid = false; // Flag to define whether header of image data is valid		
		
		if (fileExtension.equals("propra")) {
			
			headerLength = 28;
			
			// Get the compression type
			hexTmp = String.format("%02x", imageData[15]);
			compressionType = Integer.parseInt(hexTmp, 16);
		} else if (fileExtension.equals("tga")) {
			
			headerLength = 18;
			
			// Get the compression type
			hexTmp = String.format("%02x", imageData[2]);
			compressionType = Integer.parseInt(hexTmp, 16);
		}
		
		 // Get source image dimensions from header.		
		int[] imageDimensions = ImageHelper.getImageDimensions(imageData, fileExtension);
		width = imageDimensions[0];
		height = imageDimensions[1];
		
		// Perform deeper checks on a PROPRA source file
		if (fileExtension.equals("propra")) {

			headerLength = 28;

			/*
			 * Check if length of data segment from header and image dimensions from header
			 * are valid.
			 */
			// Get the size of the data segment
			hexTmp = "";
			for (int i = 0; i < 8; i++) {
				hexTmp += String.format("%02x", imageData[23 - i]);
			}
			
			// Compare the size of the data segment with the image dimensions
			dataLength = Integer.parseInt(hexTmp, 16);
			if (dataLength != width * height * 3) {
				errorMessage = "Source file corrupt. Invalid image size information in header.";
				throw new ImageHandlingException(errorMessage, ErrorCodes.INVALID_HEADERDATA);	
			}

			/*
			 * Check if length of data segment from header and actual length of data
			 * segment are equal.
			 */
			if (dataLength != imageData.length - headerLength) {
				errorMessage = "Source file corrupt. Invalid image data length information in header.";
				throw new ImageHandlingException(errorMessage, ErrorCodes.INVALID_HEADERDATA);	
			}

			/*
			 * Check for valid checksum.
			 */
			// Get the check sum from header
			hexTmp = "";
			for (int i = 0; i < 4; i++) {
				hexTmp += String.format("%02x", imageData[27 - i]);
			}

			// Get the raw image data excluding the header
			byte[] rawImageData = new byte[dataLength];			
			try {
				// Copy the image data after the header into a new array
				System.arraycopy(imageData, 28, rawImageData, 0, dataLength);
				/*
				 * The arraycopy function will fail with an ArrayIndexOutOfBoundsException
				 * if dataLength (from header) is smaller than the actual image data segment size.
				 */
			} catch (ArrayIndexOutOfBoundsException e) {
				errorMessage = "Source file corrupt. Invalid size of image data segment.";
				throw new ImageHandlingException(errorMessage, ErrorCodes.INVALID_DATASEGMENT);	
			}
			
			// Compare the actual checksum with the checksum from the header
			if (!hexTmp.equals(ImageHelper.getCheckSum(rawImageData))) {
				errorMessage = "Source file corrupt. Invalid check sum.";
				throw new ImageHandlingException(errorMessage, ErrorCodes.INVALID_CHECKSUM);	
			}
		}

		/*
		 * The following tests are valid for both file types.
		 */

		// Check if one dimension is zero.
		if (width <= 0 || height <= 0) {
			errorMessage = "Source file corrupt. Invalid image dimensions.";
			throw new ImageHandlingException(errorMessage, ErrorCodes.INVALID_HEADERDATA);
		}

		// Check if compression is valid.
		if ((fileExtension.equals("propra") && compressionType != 0) ||
				(fileExtension.equals("tga") && compressionType != 2)) {
			errorMessage = "Invalid compression of source file.";
			throw new ImageHandlingException(errorMessage, ErrorCodes.INVALID_HEADERDATA);			
			
		}

		// Check if actual image data length fits to dimensions given in the header.
		if (imageData.length - headerLength != height * width * 3) {
			errorMessage = "Source file corrupt. Image data length does not fit to header information.";
			throw new ImageHandlingException(errorMessage, ErrorCodes.INVALID_HEADERDATA);
		}
		System.out.println("Image header information successfully checked: " + imagePath);
		headerValid = true;
		return headerValid;
	}
}
