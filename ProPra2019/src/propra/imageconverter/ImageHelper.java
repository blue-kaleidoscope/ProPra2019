package propra.imageconverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * A helper class which contains methods supporting the handling of files and image data.
 * @author Oliver Eckstein
 *
 */
public class ImageHelper {
	
	/**
	 * Converts a hexadecimal string into a byte array.
	 * @param s the hexadecimal string.
	 * @return the byte array.
	 */
	public static byte[] hexStringToByteArray(String s) {    
		if (s == null) {
			throw new IllegalArgumentException();			
		}
		
		if (s.length() % 2 != 0) {
			// Trailing zero must be added
			String tmp = "0";
			tmp += s;
			s = tmp;
		}
		
		int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
	
	/**
	 * Calculates the check sum of image data based on the PROPRA file specification V1.0.
	 * @param data the image data.
	 * @return the check sum.
	 */
	public static String getCheckSum(byte[] data) {
		if (data == null) {
			throw new IllegalArgumentException();			
		}
		
		int x = 65513;
		int a_i = 0; // initial sum
		int b_i = 1; // initial b_0

		for (int i = 0; i < data.length; i++) {
			a_i += (i + 1) + Byte.toUnsignedInt(data[i]);
			a_i %= x;
			b_i = (b_i % x + a_i) % x;
		}
		return Integer.toHexString(a_i * (int) Math.pow(2, 16) + b_i);
	}
	
	/**
	 * To extract the file extension of a given file path.
	 * @param filePath the file path.
	 * @return the extension of the file path in lower case letters.
	 * @throws ImageHandlingException when file path does not contain a file extension.
	 */
	public static String getFileExtension(String filePath) throws ImageHandlingException {
		String fileExtension = null;		

		/*
		 * If fileName does not contain "." or starts with "." then it is not a valid
		 * file.
		 */
		if (filePath.contains(".") && filePath.lastIndexOf(".") != 0) {
			fileExtension = filePath.substring(filePath.lastIndexOf(".") + 1);
		}
		if (fileExtension == null) {			
			throw new ImageHandlingException("Could not read file extension. Invalid file path: " + filePath,
					ErrorCodes.INVALID_FILEPATH);
		}
		return fileExtension.toLowerCase();
	}
	
	/**
	 * Returns image dimensions of an image based on the image header data.
	 * @param headerData an array which contains either the header data or the whole image data including
	 * the header data.
	 * @param fileExtension the file extension of the file containing the image data. Must either be "propra" or "tga".
	 * @return the image data as an array with length <code>2</code> whereas the first entry contains the width and the
	 * second entry contains the height of the image. 
	 * @throws ImageHandlingException if the file format is neither "propra" nor "tga" or when <code>headerData</code> is empty.
	 */
	public static int[] getImageDimensions(byte[] headerData, String fileExtension) throws ImageHandlingException {
		String errorMessage;
		if (headerData == null || headerData.length == 0 || fileExtension == null || fileExtension.equals("")) {
			errorMessage = "Could not get image dimensions. Invalid input data.";
			throw new ImageHandlingException(errorMessage, ErrorCodes.INVALID_FILEPATH);
		}
		
		int arrayOffset = 0; // To use the same code for both file formats.
		int[] imageDimensions = new int[2];
		String hexTmp;
		
		if (fileExtension.equals("propra")) {			
			arrayOffset = 10;
		} else if (fileExtension.equals("tga")) {			
			arrayOffset = 12;
		} else {			
			errorMessage = "Unknown output file format.";
			throw new ImageHandlingException(errorMessage, ErrorCodes.INVALID_FILEFORMAT);
		}
		
		// Get source image dimensions from header.		 
		hexTmp = String.format("%02x", headerData[arrayOffset + 1]) + 
				String.format("%02x", headerData[arrayOffset]);
		imageDimensions[0] = Integer.parseInt(hexTmp, 16); // width
		hexTmp = String.format("%02x", headerData[arrayOffset + 3]) + 
				String.format("%02x", headerData[arrayOffset + 2]);
		imageDimensions[1] = Integer.parseInt(hexTmp, 16); // height
		
		return imageDimensions;
	}
	
	/**
	 * To extract the bytes of a file.
	 * @param filePath file path to the file.
	 * @return the bytes of the file.
	 * @throws ImageHandlingException is thrown when source file could not be read or returned byte
	 * array would be empty.
	 */
	public static byte[] getBytesOfFile(String filePath) throws ImageHandlingException {
		
		String errorMessage;
		File inputFile = new File(filePath);
		byte[] inputImageData = null;
		try {
			inputImageData = Files.readAllBytes(inputFile.toPath());
		} catch (IOException e) {
			errorMessage = "Error reading source file.";
			throw new ImageHandlingException(errorMessage, ErrorCodes.IO_ERROR);
		}
		
		if (inputImageData == null || inputImageData.length == 0) {
			errorMessage = "Input file is empty.";
			throw new ImageHandlingException(errorMessage, ErrorCodes.INVALID_FILE);
		}
		return inputImageData;
	}
}
