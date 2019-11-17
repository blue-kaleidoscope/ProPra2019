package propra.imageconverter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Scanner;

/**
 * A helper class which contains methods supporting the handling of files,
 * image data and encoding/decoding of base-n files.
 * 
 * @author Oliver Eckstein
 *
 */
public class ConverterHelper {
	
	private static final String BASE32_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUV";

	/**
	 * Converts a file into a base-n representation text file.
	 * @param inputFile the file to be converted.
	 * @param encode <code>true</code> when the file gets encoded. <code>false</code> when the file gets decoded.
	 * @param base32 <code>true</code> when the file should be treated with BASE32 encoding.
	 * @param alphabet the encoding alphabet when <code>base32</code> is false and a file should get encoded.
	 * @throws ImageHandlingException when the file could not get encoded.
	 */
	public static void baseCoder(File inputFile, boolean encode, boolean base32, String alphabet)
			throws ImageHandlingException {
		BufferedInputStream buffI = null;
		FileOutputStream oStream = null;
		int buffersize = 0;
		File outputFile = null;
		if (encode) {
			// File gets encoded			
			if (base32) {				
				outputFile = new File(inputFile.getPath() + ".base-32");
				alphabet = BASE32_ALPHABET;
			} else {				
				outputFile = new File(inputFile.getPath() + ".base-n");
			}
			buffersize = 1024 * (int) (Math.log(alphabet.length()) / Math.log(2));
		} else {
			// File gets decoded			
			if (base32) {				
				outputFile = new File(inputFile.getPath().replace(".base-32", ""));
				alphabet = BASE32_ALPHABET;
			} else {
				// Decoded with base-n
				outputFile = new File(inputFile.getPath().replace(".base-n", ""));
				// Let's get the alphabet from the file
				try (Scanner fileReader = new Scanner(inputFile)) {
					alphabet = fileReader.nextLine();
					fileReader.close();
				} catch (FileNotFoundException e) {
					throw new ImageHandlingException("Could not read from input file:" + inputFile.getPath(),
							ErrorCodes.INVALID_FILEPATH);
				}
				
			}
			buffersize = 1024 * getDecodeBufferSizeMultiplier(alphabet);
		}
		int bytesRead;

		// Let's prepare everything to read from the source file and write into the target file
		try {
			buffI = new BufferedInputStream(new FileInputStream(inputFile), buffersize);
			oStream = new FileOutputStream(outputFile);
		} catch (FileNotFoundException e) {
			throw new ImageHandlingException("Error while accessing the input or output file!",
					ErrorCodes.INVALID_FILE);
		}

		try {
			byte[] inputDatasegment = new byte[buffersize];
			byte[] outputDatasegment;

			if (encode && !base32) {
				// Write the alphabet at the beginning of the file
				oStream.write(alphabet.getBytes());
				oStream.write('\n');
			} else if(!encode && !base32) {
				//We must skip the line containing the alphabet
				buffI.skip(alphabet.length() + 1);
			}

			while ((bytesRead = buffI.read(inputDatasegment)) != -1) {
				if (encode) {
					outputDatasegment = encodeBaseN(inputDatasegment, bytesRead, alphabet);
				} else {
					outputDatasegment = decodeBaseN(inputDatasegment, bytesRead, alphabet);
				}
				oStream.write(outputDatasegment);
			}
			buffI.close();
			oStream.close();
		} catch (IOException e) {
			throw new ImageHandlingException("Error writing output file.", ErrorCodes.IO_ERROR);
		}
	}

	/**
	 * Decodes a file with the base-n algorithm. Supports BASE2, BASE4, BASE8, BASE16, BASE32 and BASE64.
	 * @param input the bytes from the file to be converted.
	 * @param length <code>input</code> will be used from <code>0</code> to <code>length</code>.
	 * @param alphabet the decoding alphabet
	 * @return the decoded bytes in base-n representation
	 */
	private static byte[] decodeBaseN(byte[] input, int length, String alphabet) {
		int inputPackages; // Number of input byte packages
		int difference = input.length - length;
		int inputByteLength = (int) (Math.log(alphabet.length()) / Math.log(2));
		int outputPackageByteCount = lcm(8, inputByteLength) / 8;
		int inputPackageByteCount = outputPackageByteCount * 8 / inputByteLength;
		int lastInputPackageLength = (input.length - difference) % inputPackageByteCount;
		int outputByteCount;

		if (lastInputPackageLength != 0) {
			inputPackages = (input.length - difference) / inputPackageByteCount + 1;
			outputByteCount = (inputPackages - 1) * outputPackageByteCount 
					+ (int) (lastInputPackageLength * inputByteLength / 8);
		} else {
			inputPackages = (input.length - difference) / inputPackageByteCount;
			outputByteCount = inputPackages * outputPackageByteCount;
		}

		byte[] output = new byte[outputByteCount];
		int remainingInputBytes;
		int remainingOutputBytes;

		for (int inputPackage = 0; inputPackage < inputPackages; inputPackage++) {
			if (inputPackage == inputPackages - 1 && lastInputPackageLength != 0) {
				remainingInputBytes = lastInputPackageLength;
				remainingOutputBytes = (int) (remainingInputBytes * inputByteLength / 8) + 1;
			} else {
				remainingInputBytes = inputPackageByteCount;
				remainingOutputBytes = (int) (remainingInputBytes * inputByteLength / 8);
			}
			
			// Get one input byte package
			long byteBuffer = 0;
			for (int i = inputPackage * inputPackageByteCount; i < inputPackage * inputPackageByteCount
					+ remainingInputBytes; i++) {
				byteBuffer <<= inputByteLength;
				// Get one byte from the whole input byte package, decode and buffer it
				byteBuffer |= alphabet.indexOf(input[i]);
			}

			// byteBuffer now contains all bits from the current byte package
			// Now we extract the bits byte wise and transfer them to the output bytes
			int shiftCount = remainingInputBytes * outputPackageByteCount - inputPackageByteCount;
			for (int i = inputPackage * outputPackageByteCount; i < inputPackage * outputPackageByteCount
					+ remainingOutputBytes; i++) {
				if(shiftCount >= 0) {
					output[i] = (byte) (byteBuffer >> shiftCount);
				}				
				shiftCount -= inputPackageByteCount;
			}
		}
		return output;
	}

	/**
	 * Encodes a file with the base-n algorithm. Supports BASE2, BASE4, BASE8, BASE16, BASE32 and BASE64.
	 * @param input the bytes from the file to be converted.
	 * @param length <code>input</code> will be used from <code>0</code> to <code>length</code>.
	 * @param alphabet the decoding alphabet. The length of this alphabet must be between 2^1 and 2^6
	 * @return the decoded bytes in base-n representation
	 */
	private static byte[] encodeBaseN(byte[] input, int length, String alphabet) {

		byte[] alphaBytes = alphabet.getBytes();
		int inputPackages;
		int difference = input.length - length;
		int outputByteLength = (int) (Math.log(alphabet.length()) / Math.log(2));
		int inputPackageByteCount = lcm(8, outputByteLength) / 8;
		int outputPackageByteCount = inputPackageByteCount * 8 / outputByteLength;

		int lastInputPackageLength = (input.length - difference) % inputPackageByteCount;
		int outputByteCount;

		if (lastInputPackageLength != 0) {
			inputPackages = (input.length - difference) / inputPackageByteCount + 1;
			outputByteCount = (int) ((input.length - difference) / inputPackageByteCount) * 8
					+ (int) (lastInputPackageLength * 8 / outputByteLength) + 1;
		} else {
			inputPackages = (input.length - difference) / inputPackageByteCount;
			outputByteCount = (int) ((input.length - difference) / inputPackageByteCount) * 8;
		}

		byte[] output = new byte[outputByteCount];
		int remainingInputBytes;
		int remainingOutputBytes;

		long byteBuffer;

		for (int inputPackage = 0; inputPackage < inputPackages; inputPackage++) {
			if (inputPackage == inputPackages - 1 && lastInputPackageLength != 0) {
				remainingInputBytes = lastInputPackageLength;
				remainingOutputBytes = (int) (remainingInputBytes * 8 / outputByteLength) + 1;
			} else {
				remainingInputBytes = inputPackageByteCount;
				remainingOutputBytes = (int) (remainingInputBytes * 8 / outputByteLength);
			}
			// Get one input byte package
			byteBuffer = 0;
			for (int i = inputPackage * inputPackageByteCount; i < inputPackage * inputPackageByteCount
					+ remainingInputBytes; i++) {
				byteBuffer <<= 8;
				// Get one byte from the input byte package and but it on the buffer
				byteBuffer |= input[i] & 0xFF;
			}

			// byteBuffer now contains all bits from the current input byte package
			// Now we extract the bits byte wise, encode them and transfer them to the
			// output bytes
			int shiftCount = remainingInputBytes * 8 - outputByteLength;
			byte removalMask = 0;
			// Since an output byte can be shorter than 8 bit (i.e. in base32 it is 5 bit long)
			// we must remove all bits which do not belong to the output byte by using 'removelMask'
			for (int i = 0; i < outputByteLength; i++) {
				removalMask <<= 1;
				removalMask |= 0x1;
			}
			
			byte outputBits = 0;
			for (int i = inputPackage * outputPackageByteCount; i < inputPackage * outputPackageByteCount
					+ remainingOutputBytes; i++) {
				if (shiftCount > 0) {
					outputBits = (byte) ((byteBuffer >> shiftCount) & removalMask);
				} else {
					byte lastBitsExtractionMask = 0;
					for (int j = 0; j < outputByteLength + shiftCount; j++) {
						lastBitsExtractionMask <<= 1;
						lastBitsExtractionMask |= 0x1;
					}
					// tmp is used to extract the last bits which will be used for the last
					// input byte
					outputBits = (byte) ((lastBitsExtractionMask & byteBuffer) << -shiftCount);
					// With the above line it is ensured that the last (output) bits will be filled
					// with zeros as stated in the specification.

				}
				output[i] = alphaBytes[outputBits];
				shiftCount -= outputByteLength;
			}
		}
		return output;
	}

	/**
	 * Calculates the check sum of image data based on the PROPRA file specification
	 * V2.0.
	 * 
	 * @param file the file of which the check sum gets calculated.
	 * @param offset offset for skipping the beginning of a file.
	 * @return the check sum.
	 * @throws ImageHandlingException when <code>file</code> could not get accessed.
	 */
	public static byte[] getCheckSum(File file, int offset) throws ImageHandlingException {
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
			throw new ImageHandlingException("Error while accessing the output file!",
					ErrorCodes.IO_ERROR);
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
	 * To extract the file extension of a given file path.
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
	 * To extract the header of a given file.
	 * @param filePath file path to the file.
	 * @param headerLength the length of the file's header.
	 * @return the bytes of the header.
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
				throw new ImageHandlingException("File does not contain any data.", ErrorCodes.INVALID_HEADERDATA);
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
	 * To convert a 'TGA' file to a 'PROPRA' file or vice versa.
	 * @param inputImage the input image.
	 * @param outputImage the output image.
	 * @param compress <code>true</code> when the file should get compressed using run length encoding.
	 * @return the number of written bytes.
	 * @throws ImageHandlingException an exception is thrown when the conversion could not be executed. 
	 */
	public static long convertTgaPropra(Image inputImage, Image outputImage, boolean compress)
			throws ImageHandlingException {
		BufferedInputStream buffI = null;
		FileOutputStream oStream = null;
		int buffersize = inputImage.getWidth() * 3;
		int bytesRead = 0;
		long bytesWritten = 0;

		try {
			buffI = new BufferedInputStream(new FileInputStream(inputImage.getFile()), buffersize);
			oStream = new FileOutputStream(outputImage.getFile());
		} catch (FileNotFoundException e) {
			throw new ImageHandlingException("Error reading source file.", ErrorCodes.IO_ERROR);
		}

		byte[] byteHeader = outputImage.getByteHeader();

		try {
			oStream.write(byteHeader);
			byte[] inputDatasegment = new byte[buffersize];
			buffI.skip(inputImage.getHeader().length);
			while ((bytesRead = buffI.read(inputDatasegment)) != -1) {
				byte[] outputDatasegment = new byte[bytesRead];
				if (compress) {
					outputDatasegment = compressRLE(inputDatasegment, bytesRead, true);
				} else {
					outputDatasegment = convertRGB(inputDatasegment);
				}
				oStream.write(outputDatasegment);
				bytesWritten += outputDatasegment.length;
			}
			if (compress && outputImage instanceof ImagePropra) {
				// When the compression to a propra file was successful the length of the data segment must be written
				// into the file.
				ImagePropra tmp = (ImagePropra) outputImage;
				tmp.setDataLength(bytesWritten);
				tmp = null;
			}
			buffI.close();
			oStream.close();
		} catch (IOException e) {
			throw new ImageHandlingException("Error writing output file.", ErrorCodes.IO_ERROR);
		}
		return bytesWritten;
	}

	/**
	 * To compress data using run length encoding.
	 * @param inputDatasegment the images's data segment.
	 * @param len the length of a line of pixel of this image
	 * @param rgbConversion <code>true</code> when the pixel order should be changed from BGR to GBR
	 * @return the compressed data
	 */
	private static byte[] compressRLE(byte[] inputDatasegment, int len, boolean rgbConversion) {
		int indexOutput = 0;
		int equalPixels = 0;
		int unequalPixels = 0;

		/*
		 * In the worst case 'maxSize' entries in the output data array are needed in
		 * case the pixels are ordered i.e.:
		 * unequal-equal-equal-unequal-equal-equal-unequal
		 */
		int maxSize = (int) inputDatasegment.length / 3 + inputDatasegment.length + inputDatasegment.length % 3;
		byte[] outputDatasegment = new byte[maxSize];

		for (int i = 0; i < inputDatasegment.length; i += 3) {
			if (i == inputDatasegment.length - 3) {
				// Last pixel reached
				if (equalPixels > 0) {
					outputDatasegment[indexOutput++] = (byte) (0x80 + equalPixels);
					// Before current pixel there was a streak of equal pixels which gets written
					outputDatasegment[indexOutput++] = inputDatasegment[i + 1];
					outputDatasegment[indexOutput++] = inputDatasegment[i];
					outputDatasegment[indexOutput++] = inputDatasegment[i + 2];

				} else {
					// Raw pixels get written
					outputDatasegment[indexOutput++] = (byte) unequalPixels;
					int arrayIndexUnequalPixel = unequalPixels;
					for (int j = 0; j <= unequalPixels; j++) {
						outputDatasegment[indexOutput++] = inputDatasegment[i - 3 * arrayIndexUnequalPixel + 1];
						outputDatasegment[indexOutput++] = inputDatasegment[i - 3 * arrayIndexUnequalPixel];
						outputDatasegment[indexOutput++] = inputDatasegment[i - 3 * arrayIndexUnequalPixel + 2];
						arrayIndexUnequalPixel--;
					}
				}
			} else {
				if (((i + 1) % len == 0 && i > 0) || equalPixels == 127 || unequalPixels == 127) {
					// End of image line reached or maximum length of possible 7 bit pixel counter
					// reached
					if (equalPixels > 0) {
						// Equal pixels count gets written
						outputDatasegment[indexOutput++] = (byte) (0x80 + equalPixels);
						outputDatasegment[indexOutput++] = inputDatasegment[i + 1];
						outputDatasegment[indexOutput++] = inputDatasegment[i];
						outputDatasegment[indexOutput++] = inputDatasegment[i + 2];
						equalPixels = 0;
					} else {
						// Raw pixels get written
						outputDatasegment[indexOutput++] = (byte) unequalPixels;
						int arrayIndexUnequalPixel = unequalPixels;
						for (int j = 0; j <= unequalPixels; j++) {
							outputDatasegment[indexOutput++] = inputDatasegment[i - 3 * arrayIndexUnequalPixel + 1];
							outputDatasegment[indexOutput++] = inputDatasegment[i - 3 * arrayIndexUnequalPixel];
							outputDatasegment[indexOutput++] = inputDatasegment[i - 3 * arrayIndexUnequalPixel + 2];

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
							int arrayIndexUnequalPixel = unequalPixels;
							for (int j = 0; j < unequalPixels; j++) {
								outputDatasegment[indexOutput++] = inputDatasegment[i - 3 * arrayIndexUnequalPixel + 1];
								outputDatasegment[indexOutput++] = inputDatasegment[i - 3 * arrayIndexUnequalPixel];
								outputDatasegment[indexOutput++] = inputDatasegment[i - 3 * arrayIndexUnequalPixel + 2];
								arrayIndexUnequalPixel--;
							}
							unequalPixels = 0;
						}
					} else {
						// Current and next pixel are unequal
						if (equalPixels > 0) {
							// Before current pixel there was a streak of equal pixels which gets written
							outputDatasegment[indexOutput++] = (byte) (0x80 + equalPixels);
							outputDatasegment[indexOutput++] = inputDatasegment[i + 1];
							outputDatasegment[indexOutput++] = inputDatasegment[i];
							outputDatasegment[indexOutput++] = inputDatasegment[i + 2];
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

	/**
	 * To decompress an image which was compressed using run length encoding.
	 * @param inputImage the image which should get decompressed.
	 * @param outputImage the output image to which the decompressed data should be written.
	 * @param rgbConversion <code>true</code> when the pixel order should be changed from BGR to GBR
	 * @throws ImageHandlingException when <code>inputImage</code> or <code>outputImage</code> could not be accessed.
	 */
	public static void decompressRLE(Image inputImage, Image outputImage, boolean rgbConversion) throws ImageHandlingException {

		BufferedInputStream buffI = null;
		FileOutputStream oStream = null;
		int buffersize = 9 * 1024;

		byte[] outputDatasegment = new byte[outputImage.getWidth() * 3];
		byte controlByte = 0;
		int pixelCount = 0;
		int restByteCount = 0;
		int outputIndex = 0;
		boolean equalPixels = false;

		try {
			buffI = new BufferedInputStream(new FileInputStream(inputImage.getFile()), buffersize);
			oStream = new FileOutputStream(outputImage.getFile());
		} catch (FileNotFoundException e) {
			throw new ImageHandlingException("Error reading source file.", ErrorCodes.IO_ERROR);
		}

		try {
			oStream.write(outputImage.getByteHeader());
			byte[] inputDatasegment = new byte[buffersize];
			buffI.skip(inputImage.getHeader().length);
			int inputIndex;
			int timesWritten = 0;

			byte tmpByte1 = 0;
			byte tmpByte2 = 0;
			while (buffI.read(inputDatasegment) != -1) {
				inputIndex = 0;
				while (inputIndex < inputDatasegment.length) {
					if (restByteCount > 0) {

						// From the previous array "inputDatasegment" there are still bytes to write
						if (equalPixels) {

							// 1 to 3 pixels from the previous array "inputDatasegment" need to be written
							for (int i = 0; i < pixelCount; i++) {
								// Those pixels will get written pixelCount times
								if (restByteCount == 3) {
									// The three bytes are at the very beginning of the new array
									for (int k = 0; k < 3; k++) {
										outputDatasegment[outputIndex++] = inputDatasegment[k];
									}
								} else if (restByteCount == 2) {
									// Two bytes are taken from the previous array the third is from the new one
									outputDatasegment[outputIndex++] = tmpByte1;
									outputDatasegment[outputIndex++] = tmpByte2;
									outputDatasegment[outputIndex++] = inputDatasegment[0];
								} else if (restByteCount == 1) {
									// One byte is taken from the previous array, the 2nd and 3rd are from the new
									// one
									outputDatasegment[outputIndex++] = tmpByte1;
									outputDatasegment[outputIndex++] = inputDatasegment[0];
									outputDatasegment[outputIndex++] = inputDatasegment[1];
								}
							}
							if (restByteCount == 3) {
								inputIndex += 3;
							} else if (restByteCount == 2) {
								inputIndex += 1;
							} else if (restByteCount == 1) {
								inputIndex += 2;
							}
							restByteCount = 0;
						} else {
							// Up to restByteCount bytes of raw pixels need to get written
							while (restByteCount > 0) {
								outputDatasegment[outputIndex++] = inputDatasegment[inputIndex++];
								restByteCount--;
							}
						}
					} else {
						// Let's determine whether we have to write a series of equal or raw pixels
						controlByte = inputDatasegment[inputIndex];
						pixelCount = (controlByte & 0x7F) + 1;
						equalPixels = (controlByte & 0x80) == 0x80;
						restByteCount = pixelCount * 3;

						if (equalPixels) {
							if (inputIndex + 3 < inputDatasegment.length) {
								// Series of equal pixels gets written
								for (int j = 0; j < pixelCount; j++) {
									// j times the same pixel gets written

									// Writing the pixel data bytewise into the outputDatasegment
									for (int k = 1; k < 4; k++) {
										outputDatasegment[outputIndex++] = inputDatasegment[inputIndex + k];
									}

								}
								// All equal pixels were written
								pixelCount = 0;
								restByteCount = 0;
							} else if (inputIndex + 3 == inputDatasegment.length) {
								// The following two entries are pixels then the inputDatasegment ends
								tmpByte1 = inputDatasegment[inputIndex + 1];
								tmpByte2 = inputDatasegment[inputIndex + 2];
								restByteCount = 2;
							} else if (inputIndex + 2 == inputDatasegment.length) {
								// The following entry is a pixel then the inputDatasegment ends
								tmpByte1 = inputDatasegment[inputIndex + 1];
								restByteCount = 1;
							} else if (inputIndex + 1 == inputDatasegment.length) {
								// The current entry is a control byte then the inputDatasegment ends
								restByteCount = 3;
							}
							inputIndex += 4;
						} else {
							// A set of raw pixels gets written
							for (int j = 1; j <= pixelCount * 3; j++) {
								// Writing the raw pixels bytewise
								if (inputIndex + j < inputDatasegment.length) {
									outputDatasegment[outputIndex++] = inputDatasegment[inputIndex + j];
									restByteCount--;
								}
							}
							inputIndex += pixelCount * 3 + 1;
						}
					}

					// Write a line of the output image to the file
					if (outputIndex == outputDatasegment.length) {
						if (rgbConversion) {
							outputDatasegment = convertRGB(outputDatasegment);
						}
						oStream.write(outputDatasegment);
						outputDatasegment = new byte[outputImage.getWidth() * 3];
						outputIndex = 0;
						timesWritten++;
						if (timesWritten == outputImage.getHeight()) {
							// Let's get out of here. Decompression is done.
							return;
						}
					}
				}
			}
			buffI.close();
			oStream.close();
		} catch (IOException e) {
			throw new ImageHandlingException("Error reading from inputImage or writing into outputImage.", ErrorCodes.IO_ERROR);
		}
	}

	/**
	 * To change the order of pixels of an image. GBR > BGR
	 * @param inputDatasegment the data segment of an image.
	 * @return the pixels in the new order.
	 */
	private static byte[] convertRGB(byte[] inputDatasegment) {
		byte[] outputDatasegment = new byte[inputDatasegment.length];
		for (int i = 0; i < inputDatasegment.length; i = i + 3) {
			outputDatasegment[i] = inputDatasegment[i + 1];
			outputDatasegment[i + 1] = inputDatasegment[i];
			outputDatasegment[i + 2] = inputDatasegment[i + 2];
		}
		return outputDatasegment;
	}

	/**
	 * To write the header of an image into an already existing file.
	 * @param image the image.
	 */
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

	/**
	 * To copy the data segment of an input image to an output image.
	 * @param inputImage the input image.
	 * @param outputImage the output image.
	 * @param rgbConversion <code>true</code> when the order of the pixels must be changed i.e. from TGA > PROPRA conversion
	 */
	public static void copyImage(Image inputImage, Image outputImage, boolean rgbConversion) {
		BufferedInputStream buffI = null;
		FileOutputStream oStream = null;
		int buffersize = inputImage.getWidth() * 3;

		try {
			buffI = new BufferedInputStream(new FileInputStream(inputImage.getFile()), buffersize);
			oStream = new FileOutputStream(outputImage.getFile());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		byte[] byteHeader = outputImage.getByteHeader();

		try {
			oStream.write(byteHeader);
			byte[] inputDatasegment = new byte[buffersize];
			int bytesRead = 0;
			buffI.skip(inputImage.getHeader().length);
			while ((bytesRead = buffI.read(inputDatasegment)) != -1) {
				if (bytesRead < buffersize) {
					byte[] tmp = new byte[bytesRead];
					for (int i = 0; i < bytesRead; i++) {
						tmp[i] = inputDatasegment[i];
					}
					inputDatasegment = tmp;
				}
				if (rgbConversion) {
					convertRGB(inputDatasegment);
				}
				oStream.write(inputDatasegment);
			}
		} catch (IOException e) {
			
		}
	}

	/**
	 * To calculate the greated common divider of two numbers.
	 * @param number1
	 * @param number2
	 * @return the gcd of the two numbers.
	 */
	private static int gcd(int number1, int number2) {
		if (number1 == 0 || number2 == 0) {
			return number1 + number2;
		} else {
			int absNumber1 = Math.abs(number1);
			int absNumber2 = Math.abs(number2);
			int biggerValue = Math.max(absNumber1, absNumber2);
			int smallerValue = Math.min(absNumber1, absNumber2);
			return gcd(biggerValue % smallerValue, smallerValue);
		}
	}

	/**
	 * To calculate the least common multiplier of two numbers.
	 * @param number1
	 * @param number2
	 * @return the lcd of two numbers.
	 */
	private static int lcm(int number1, int number2) {
		if (number1 == 0 || number2 == 0)
			return 0;
		else {
			int gcd = gcd(number1, number2);
			return Math.abs(number1 * number2) / gcd;
		}
	}
	
	/**
	 * To get the multiplier of a buffer size when decoding base-n files.
	 * Useful when trying to use buffer sizes which are a multiple of 1024. 
	 * @param alphabet the decoding alphabet
	 * @return the mutliplier
	 */
	private static int getDecodeBufferSizeMultiplier(String alphabet) {
		int outputByteLength = (int) (Math.log(alphabet.length()) / Math.log(2));
		int outputPackageByteCount = lcm(8, outputByteLength) / 8;
		int inputPackageByteCount = outputPackageByteCount * 8 / outputByteLength;
		
		return inputPackageByteCount;
	}
}
