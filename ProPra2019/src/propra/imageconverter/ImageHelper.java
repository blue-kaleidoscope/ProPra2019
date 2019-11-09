package propra.imageconverter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * A helper class which contains methods supporting the handling of files and image data.
 * @author Oliver Eckstein
 *
 */
public class ImageHelper {
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		System.out.println(getCheckSum(new FileInputStream(new File("origin\\test_01_uncompressed.tga")).readAllBytes()));
	}
	
	/**
	 * Calculates the check sum of image data based on the PROPRA file specification V1.0.
	 * @param TODO params
	 * @return the check sum.
	 */
	public static byte[] getCheckSum(File file, int offset) {		
		BufferedInputStream buffI = null;
		//int bufferSize = 8 * 1024;
		int bufferSize = 65513;
		byte[] buffArray = new byte[bufferSize];
		int bytesRead = 0;
		int bytesInTotal = 0;
		
		int x = 65513;
		int a_i = 0; // initial sum
		int b_i = 1; // initial b_0
		
		try {
			buffI = new BufferedInputStream(
					new FileInputStream(file), bufferSize);
			buffI.skip(offset);
			while((bytesRead = buffI.read(buffArray)) != -1) {
				for (int i = 0; i < bytesRead; i++) {
					a_i += (i + bytesInTotal + 1) + Byte.toUnsignedInt(buffArray[i]);
					a_i %= x;
					b_i = (b_i % x + a_i) % x;
				}
				bytesInTotal += bytesRead;
			}
			buffI.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();	
		}
		
		int checkSum = a_i * (int) Math.pow(2, 16) + b_i;
		String tmp = Integer.toHexString(checkSum);
		byte[] tmpArray = tmp.getBytes();
		byte[] checkSumArray = new byte[4];
		checkSumArray[0] = (byte) checkSum;
		checkSumArray[1] = (byte) (checkSum >> 8);
		checkSumArray[2] = (byte) (checkSum >> 16);
		checkSumArray[3] = (byte) (checkSum >> 24);
		
		return checkSumArray;
	}	
	
	/**
	 * Calculates the check sum of image data based on the PROPRA file specification V1.0.
	 * @param data the image data.
	 * @return the check sum.
	 */
	public static String getCheckSum(byte[] data) {
		byte[] tmp = new byte[data.length - 18];
		System.arraycopy(data, 18, tmp, 
				0, data.length - 18);
		
		if (tmp == null) {
			throw new IllegalArgumentException();			
		}
		
		int x = 65513;
		int a_i = 0; // initial sum
		int b_i = 1; // initial b_0

		for (int i = 0; i < tmp.length; i++) {
			a_i += (i + 1) + Byte.toUnsignedInt(tmp[i]);
			a_i %= x;
			b_i = (b_i % x + a_i) % x;
		}
		return Integer.toHexString(a_i * (int) Math.pow(2, 16) + b_i);
	}
	
	/**
	 * To extract the file extension of a given file path.
	 * TODO use file filter
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
	
	/** TODO update doc here
	 * To extract the bytes of a file.
	 * @param filePath file path to the file.
	 * @return the bytes of the file.
	 * @throws ImageHandlingException is thrown when source file could not be read or returned byte
	 * array would be empty.
	 */
	public static int[] getHeaderFromFile(File file, int headerLength) throws ImageHandlingException {
			
		byte[] tmp = new byte[headerLength];
		BufferedInputStream buffI = null;	
		
		try {
			buffI = new BufferedInputStream(
					new FileInputStream(file));			
			
			if(buffI.read(tmp, 0, headerLength) == -1) {				
				buffI.close();
				throw new ImageHandlingException("Error heading header.", ErrorCodes.INVALID_HEADERDATA);
			} else {
				buffI.close();
			}			
			
			} catch (IOException e) {				
				throw new ImageHandlingException("Error reading source file.", ErrorCodes.IO_ERROR);
			}
		
		int[] header = new int[headerLength];
		for(int i = 0; i < headerLength; i++) {
			header[i] = tmp[i] & 0xFF;
		}
		return header;
	}
}
