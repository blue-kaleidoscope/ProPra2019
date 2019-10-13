package propra.imageconverter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * To convert image data from an input to an output format.
 * @author Oliver Eckstein
 *
 */
public class ImageWriter {
	
	/**
	 * Converts image data from an original image to a target image based on the file formats of
	 * the original and the target image.
	 * Only allowed conversions are from <code>tga</code> to <code>propra</code> or vice versa.
	 * If input and output format are equal then the file content simply gets copied from the input to the
	 * output file.
	 * @param inputPath the file path of the original image.
	 * @param outputPath the file path of the target image.
	 * @return <code>true</code> if conversion was successful else <code>false</code>.
	 * @throws ImageHandlingException when conversion failed.
	 */
	public boolean writeData(String inputPath, String outputPath) throws ImageHandlingException {

		int width;
		int height;
		byte[] outputImage;
		int dataLength;		
		// The following offsets are used to save some code lines later...
		int dimensionRangeOffset = 0;
		int dimensionCopyOffset = 0;
		int sourcePixelOffset = 0;
		int targetPixelOffset = 0;
		boolean writeSuccessful = false;
		String errorMessage;

		if (inputPath == null || outputPath == null || inputPath.equals("") || outputPath.equals("")) {
			errorMessage = "Invalid file path for input or output file.";
			throw new ImageHandlingException(errorMessage, ErrorCodes.INVALID_FILEPATH);			
		}

		String inputExtension = ImageHelper.getFileExtension(inputPath);
		String outputExtension = ImageHelper.getFileExtension(outputPath);
		byte[] inputImageData = ImageHelper.getBytesOfFile(inputPath);

		// Get source image dimensions from header.
		int[] imageDimensions = ImageHelper.getImageDimensions(inputImageData, inputExtension);
		width = imageDimensions[0];
		height = imageDimensions[1];
		dataLength = width * height * 3;		

		if (inputExtension.equals("propra") && outputExtension.equals("tga")) {
			// From propra to tga
			dimensionRangeOffset = 12;
			dimensionCopyOffset = -2;
			sourcePixelOffset = 28;
			targetPixelOffset = 18;
			
			/*
			 * Set up header data of the output image.
			 */			
			outputImage = new byte[18 + dataLength];
			outputImage[2] = 2; // uncompressed RGB
			outputImage[16] = 24; // 24 bits per pixel
			outputImage[17] = 32; // origin top-left
		

		} else if (inputExtension.equals("tga") && outputExtension.equals("propra")) {
			// From tga to propra
			dimensionRangeOffset = 10;
			dimensionCopyOffset = 2;
			sourcePixelOffset = 18;
			targetPixelOffset = 28;
			
			/*
			 * Set up header data of the output image. Let's begin with the "ProPraWS19"
			 * string.
			 */
			outputImage = new byte[28 + dataLength];
			String proPra = "ProPraWS19";
			byte[] proPraBytes = proPra.getBytes();
			for (int i = 0; i < proPra.length(); i++) {
				outputImage[i] = proPraBytes[i];
			}

			outputImage[14] = 24; // 24 bits per pixel
			outputImage[15] = 0; // uncompressed

			/*
			 * Write the length of the data segment into the header (little-endian).
			 */
			String dataSegment = Integer.toHexString(dataLength);
			byte[] dataSegmentBytes = ImageHelper.hexStringToByteArray(dataSegment);
			for (int i = 0; i < dataSegmentBytes.length; i++) {
				outputImage[16 + i] = dataSegmentBytes[dataSegmentBytes.length - 1 - i];
			}			
		} else {
			/*
			 * Input and output format are the same.
			 * Therefore we will simply copy the input data to the output file.
			 */
			outputImage = inputImageData;
		}
		

		/*
		 * Transfer the image dimension header information.
		 */
		for (int i = dimensionRangeOffset; i < dimensionRangeOffset + 4; i++) {
			outputImage[i] = inputImageData[i + dimensionCopyOffset];
		}

		/*
		 * Write pixel data into outputImage. From PROPRA: GBR --> TGA: BGR
		 */
		for (int i = 0; i < dataLength; i = i + 3) {
			outputImage[targetPixelOffset + i] = inputImageData[sourcePixelOffset + i + 1];
			outputImage[targetPixelOffset + i + 1] = inputImageData[sourcePixelOffset + i];
			outputImage[targetPixelOffset + i + 2] = inputImageData[sourcePixelOffset + i + 2];
		}
		
		/*
		 * Write check sum into the header (little-endian). First let's copy the image data
		 * segment into a new array and calculate the check sum with it.
		 */
		if (inputExtension.equals("tga") && outputExtension.equals("propra")) {			
			byte[] imageData = new byte[dataLength];
			System.arraycopy(outputImage, 28, imageData, 0, dataLength);
			String checkSum = ImageHelper.getCheckSum(imageData);
			byte[] checkSumBytes = ImageHelper.hexStringToByteArray(checkSum);
			for (int i = 0; i < checkSumBytes.length; i++) {
				outputImage[24 + i] = checkSumBytes[checkSumBytes.length - 1 - i];
			}
		}

		/*
		 * Create output file and write into the file.
		 */
		File outputFile = new File(outputPath);

		try (FileOutputStream stream = new FileOutputStream(outputFile.getPath())) {
			stream.write(outputImage);
		} catch (FileNotFoundException e) {
			errorMessage = "Error writing output file. Invalid path or user does not have enough rights.";
			throw new ImageHandlingException(errorMessage, ErrorCodes.IO_ERROR);
		} catch (IOException e) {
			errorMessage = "Error writing output file. Invalid path or user does not have enough rights.";
			throw new ImageHandlingException(errorMessage, ErrorCodes.IO_ERROR);
		}
		writeSuccessful = true;
		System.out.println("Input image successfully converted.");
		System.out.println(inputPath + " --> " + outputPath);
		return writeSuccessful;		
	}
}
