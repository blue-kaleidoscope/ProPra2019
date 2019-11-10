package propra.imageconverter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A helper class which contains methods supporting the handling of files and
 * image data.
 * 
 * @author Oliver Eckstein
 *
 */
public class ImageHelper {

	/**
	 * Calculates the check sum of image data based on the PROPRA file specification
	 * V1.0.
	 * 
	 * @param TODO params
	 * @return the check sum.
	 */
	public static byte[] getCheckSum(Image image, int offset) {
		BufferedInputStream buffI = null;
		File file = image.getFile();
		// int bufferSize = 8 * 1024;
		//int bufferSize = 65513;
		int bufferSize = 9 * 1024;
		byte[] buffArray = new byte[bufferSize];
		int bytesRead = 0;
		int bytesInTotal = 0;

		int x = 65513;
		int a_i = 0; // initial sum
		int b_i = 1; // initial b_0

		try {
			buffI = new BufferedInputStream(new FileInputStream(file), bufferSize);
			buffI.skip(offset);
			while ((bytesRead = buffI.read(buffArray)) != -1) {
				if(image.getExtension().equals("tga")) {
					buffArray = convertRGB(buffArray);
				}				
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
		byte[] checkSumArray = new byte[4];
		checkSumArray[0] = (byte) checkSum;
		checkSumArray[1] = (byte) (checkSum >> 8);
		checkSumArray[2] = (byte) (checkSum >> 16);
		checkSumArray[3] = (byte) (checkSum >> 24);

		return checkSumArray;
	}

	/**
	 * To extract the file extension of a given file path. TODO use file filter
	 * 
	 * @param filePath the file path.
	 * @return the extension of the file path in lower case letters.
	 * @throws ImageHandlingException when file path does not contain a file
	 *                                extension.
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
	 * TODO update doc here To extract the bytes of a file.
	 * 
	 * @param filePath file path to the file.
	 * @return the bytes of the file.
	 * @throws ImageHandlingException is thrown when source file could not be read
	 *                                or returned byte array would be empty.
	 */
	public static int[] getHeaderFromFile(File file, int headerLength) throws ImageHandlingException {

		byte[] tmp = new byte[headerLength];
		BufferedInputStream buffI = null;

		try {
			buffI = new BufferedInputStream(new FileInputStream(file));

			if (buffI.read(tmp, 0, headerLength) == -1) {
				buffI.close();
				throw new ImageHandlingException("Error heading header.", ErrorCodes.INVALID_HEADERDATA);
			} else {
				buffI.close();
			}

		} catch (IOException e) {
			throw new ImageHandlingException("Error reading source file.", ErrorCodes.IO_ERROR);
		}

		int[] header = new int[headerLength];
		for (int i = 0; i < headerLength; i++) {
			header[i] = tmp[i] & 0xFF;
		}
		return header;
	}

	/**
	 * To start the conversion process from the input image of this controller to
	 * the output image.
	 * 
	 * @throws ImageHandlingException An exception is thrown when the conversion
	 *                                cannot be performed.
	 */
	public static void convert(Image inputImage, Image outputImage, boolean compress) throws ImageHandlingException {
		BufferedInputStream buffI = null;
		FileOutputStream oStream = null;
		// int buffersize = 9 * 1024;
		int buffersize = inputImage.getWidth() * 3;
		int bytesRead = 0;

		try {
			buffI = new BufferedInputStream(new FileInputStream(inputImage.getFile()), buffersize);
			oStream = new FileOutputStream(outputImage.getFile());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int[] header = outputImage.getHeader();
		byte[] byteHeader = new byte[header.length];
		for (int i = 0; i < byteHeader.length; i++) {
			byteHeader[i] = (byte) header[i];
		}

		try {
			oStream.write(byteHeader);
			byte[] inputDatasegment = new byte[buffersize];
			buffI.skip(inputImage.getHeaderLength());
			while ((bytesRead = buffI.read(inputDatasegment)) != -1) {
				byte[] outputDatasegment = new byte[bytesRead];
				if (compress) {
					outputDatasegment = compress(inputDatasegment, outputDatasegment, buffersize);
				} else {
					outputDatasegment = convertRGB(inputDatasegment);
				}
				oStream.write(outputDatasegment);
			}
			buffI.close();
			oStream.close();
		} catch (IOException e) {
			throw new ImageHandlingException("Error writing output file.", ErrorCodes.IO_ERROR);
		}
	}

	private static byte[] compress(byte[] inputDatasegment, byte[] outputDatasegment, int len) {
		int indexOutput = 0;
		int equalPixels = 0;
		int unequalPixels = 0;

		// Compressed
		for (int i = 0; i < inputDatasegment.length - 6; i = i + 3) {
			if (((i + 1) % len == 0 && i > 0) || equalPixels == 127 || unequalPixels == 128) {
				if (equalPixels > 0) {
					outputDatasegment[indexOutput++] = (byte) (0x80 + equalPixels);
					outputDatasegment[indexOutput++] = inputDatasegment[i];
					outputDatasegment[indexOutput++] = inputDatasegment[i + 1];
					outputDatasegment[indexOutput++] = inputDatasegment[i + 2];
				} else {
					outputDatasegment[indexOutput++] = (byte) (0x7F + unequalPixels - 1);
					for (int j = 0; j < unequalPixels; j++) {
						outputDatasegment[indexOutput++] = inputDatasegment[i - 3 * unequalPixels];
						outputDatasegment[indexOutput++] = inputDatasegment[i - 3 * unequalPixels + 1];
						outputDatasegment[indexOutput++] = inputDatasegment[i - 3 * unequalPixels + 2];
						unequalPixels--;
					}
				}
			} else {
				if (inputDatasegment[i] == inputDatasegment[i + 4] && inputDatasegment[i + 1] == inputDatasegment[i + 5]
						&& inputDatasegment[i + 2] == inputDatasegment[i + 6]) {
					equalPixels++;
					if (unequalPixels > 0) {
						outputDatasegment[indexOutput++] = (byte) (0x7F + unequalPixels - 1);
						for (int j = 0; j < unequalPixels; j++) {
							outputDatasegment[indexOutput++] = inputDatasegment[i - 3 * unequalPixels];
							outputDatasegment[indexOutput++] = inputDatasegment[i - 3 * unequalPixels + 1];
							outputDatasegment[indexOutput++] = inputDatasegment[i - 3 * unequalPixels + 2];
							unequalPixels--;
						}
						unequalPixels = 0; // TODO notwendig? Und oben evtl. auch (nicht)?
					} else {
						if (equalPixels == 0) {
							unequalPixels++;
						} else {
							outputDatasegment[indexOutput++] = (byte) (0x80 + equalPixels);
							outputDatasegment[indexOutput++] = inputDatasegment[i];
							outputDatasegment[indexOutput++] = inputDatasegment[i + 1];
							outputDatasegment[indexOutput++] = inputDatasegment[i + 2];
							equalPixels = 0;
						}
					}
				}
			}
		}
		return outputDatasegment;
	}

	private static byte[] convertRGB(byte[] inputDatasegment) {
		// Change the order of the pixels of input image.
		// propra: GBR --> tga: BGR
		byte[] outputDatasegment = new byte[inputDatasegment.length];
		for (int i = 0; i < inputDatasegment.length; i = i + 3) {
			outputDatasegment[i] = inputDatasegment[i + 1];
			outputDatasegment[i + 1] = inputDatasegment[i];
			outputDatasegment[i + 2] = inputDatasegment[i + 2];
		}
		return outputDatasegment;
	}
}
