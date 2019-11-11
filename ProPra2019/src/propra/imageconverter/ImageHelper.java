package propra.imageconverter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * A helper class which contains methods supporting the handling of files and
 * image data.
 * 
 * @author Oliver Eckstein
 *
 */
public class ImageHelper {

	private static int tmpIndex = 0;

	/**
	 * Calculates the check sum of image data based on the PROPRA file specification
	 * V1.0.
	 * 
	 * @param TODO params
	 * @return the check sum.
	 */
	public static byte[] getCheckSum(File file, int offset) {
		BufferedInputStream buffI = null;
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
	public static void convertTgaPropra(Image inputImage, Image outputImage) throws ImageHandlingException {
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
			buffI.skip(inputImage.getHeader().length);
			while ((bytesRead = buffI.read(inputDatasegment)) != -1) {
				byte[] outputDatasegment = new byte[bytesRead];
				if (outputImage.getCompressionMode() == Image.UNCOMPRESSED) {
					outputDatasegment = convertRGB(inputDatasegment);
				} else {
					outputDatasegment = compressRLE(inputDatasegment, bytesRead);
				}
				oStream.write(outputDatasegment);
			}
			buffI.close();
			oStream.close();
		} catch (IOException e) {
			throw new ImageHandlingException("Error writing output file.", ErrorCodes.IO_ERROR);
		}
	}

	public static byte[] compressRLE(byte[] inputDatasegment, int len) {
		int indexOutput = 0;
		int equalPixels = 0;
		int unequalPixels = 0;

		/*
		 * In the worst case 'maxNeeded' entries in the output data array are needed in
		 * case every pixel of 'inputDatasegment' is different.
		 */
		byte[] outputDatasegment = new byte[2 * inputDatasegment.length];
		// inputDatasegment = convertRGB(inputDatasegment);

		for (int i = 0; i < inputDatasegment.length; i = i + 3) {
			if (tmpIndex > 208777) {
				System.out.print("");
			}

			if (i == inputDatasegment.length - 3) {
				// Last pixel reached
				if (equalPixels > 0) {
					// Before current pixel there was a streak of equal pixels which gets written
					outputDatasegment[indexOutput++] = (byte) (0x80 + equalPixels);
					outputDatasegment[indexOutput++] = inputDatasegment[i];
					outputDatasegment[indexOutput++] = inputDatasegment[i + 1];
					outputDatasegment[indexOutput++] = inputDatasegment[i + 2];
					tmpIndex += 4;
				} else {
					// Raw pixels get written
					outputDatasegment[indexOutput++] = (byte) unequalPixels;
					tmpIndex++;
					int arrayIndexUnequalPixel = unequalPixels;
					for (int j = 0; j <= unequalPixels; j++) {
						outputDatasegment[indexOutput++] = inputDatasegment[i - 3 * arrayIndexUnequalPixel];
						outputDatasegment[indexOutput++] = inputDatasegment[i - 3 * arrayIndexUnequalPixel + 1];
						outputDatasegment[indexOutput++] = inputDatasegment[i - 3 * arrayIndexUnequalPixel + 2];
						tmpIndex += 3;
						arrayIndexUnequalPixel--;
					}
				}
			} else {
				// Still some pixels left
				if (((i + 1) % len == 0 && i > 0) || equalPixels == 127 || unequalPixels == 127) {
					// End of image line reached or maximum length of possible 7 bit pixel counter
					// reached
					if (equalPixels > 0) {
						// Equal pixels count gets written
						outputDatasegment[indexOutput++] = (byte) (0x80 + equalPixels);
						outputDatasegment[indexOutput++] = inputDatasegment[i];
						outputDatasegment[indexOutput++] = inputDatasegment[i + 1];
						outputDatasegment[indexOutput++] = inputDatasegment[i + 2];
						tmpIndex += 4;
						equalPixels = 0;
					} else {
						// Raw pixels get written
						outputDatasegment[indexOutput++] = (byte) unequalPixels;
						tmpIndex++;
						int arrayIndexUnequalPixel = unequalPixels;
						for (int j = 0; j <= unequalPixels; j++) {
							outputDatasegment[indexOutput++] = inputDatasegment[i - 3 * arrayIndexUnequalPixel];
							outputDatasegment[indexOutput++] = inputDatasegment[i - 3 * arrayIndexUnequalPixel + 1];
							outputDatasegment[indexOutput++] = inputDatasegment[i - 3 * arrayIndexUnequalPixel + 2];
							tmpIndex += 3;
							arrayIndexUnequalPixel--;
						}
						unequalPixels = 0;
					}
				} else {
					if (inputDatasegment[i] == inputDatasegment[i + 3]
							&& inputDatasegment[i + 1] == inputDatasegment[i + 4]
							&& inputDatasegment[i + 2] == inputDatasegment[i + 5]) {
						// Current and next pixel are equal
						equalPixels++;
						if (unequalPixels > 0) {
							// Before current pixel there were unequal pixels --> They get written as raw
							// pixels
							outputDatasegment[indexOutput++] = (byte) (unequalPixels - 1);
							tmpIndex++;
							int arrayIndexUnequalPixel = unequalPixels;
							for (int j = 0; j < unequalPixels; j++) {
								outputDatasegment[indexOutput++] = inputDatasegment[i - 3 * arrayIndexUnequalPixel];
								outputDatasegment[indexOutput++] = inputDatasegment[i - 3 * arrayIndexUnequalPixel + 1];
								outputDatasegment[indexOutput++] = inputDatasegment[i - 3 * arrayIndexUnequalPixel + 2];
								tmpIndex += 3;
								arrayIndexUnequalPixel--;
							}
							unequalPixels = 0;
						}
					} else {
						// Current and next pixel are unequal
						if (equalPixels > 0) {
							// Before current pixel there was a streak of equal pixels which gets written
							outputDatasegment[indexOutput++] = (byte) (0x80 + equalPixels);
							outputDatasegment[indexOutput++] = inputDatasegment[i];
							outputDatasegment[indexOutput++] = inputDatasegment[i + 1];
							outputDatasegment[indexOutput++] = inputDatasegment[i + 2];
							tmpIndex += 4;
							equalPixels = 0;
						} else {
							unequalPixels++;
						}
					}

				}
			}

		}

		/*
		 * Let's get rid of the entries in outputDatasegment which were reserved but not
		 * needed when the worst case mentioned above did not happen.
		 */
		if (indexOutput < inputDatasegment.length * 2) {
			byte[] tmp = new byte[indexOutput];
			for (int i = 0; i < tmp.length; i++) {
				tmp[i] = outputDatasegment[i];
			}
			outputDatasegment = tmp;
		}
		return outputDatasegment;
	}

	public static byte[] decompressRLE(byte[] inputDatasegment) {
		return null;
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

	public static void writeHeaderIntoFile(Image image) {
		RandomAccessFile file = null;
		try {
			file = new RandomAccessFile(image.getFile(), "rw");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int[] header = image.getHeader();
		try {
			for (int i = 0; i < header.length; i++) {
				file.seek(i);
				file.write((byte) header[i] & 0xFF);
			}
			file.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
