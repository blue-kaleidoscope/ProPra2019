package propra.imageconverter.util;

import propra.imageconverter.error.ImageHandlingException;

/**
 * A <code>ChecksumCalculator</code> calculates the check sum for PROPRA image files.
 * @author Oliver Eckstein
 *
 */
public class ChecksumCalculator {
	
	private final int X = 65513;
	private int a_i;
	private int b_i;
	private long bytesInTotal = 0;
	private FileHandler fileHandler;

	/**
	 * To create a new <code>ChecksumCalculator</code>.
	 * @param fileHandler the file handler which reads the PROPRA image's image data segment.
	 */
	public ChecksumCalculator(FileHandler fileHandler) {
		a_i = 0;
		b_i = 1;
		this.fileHandler = fileHandler;
	}
	
	/**
	 * Calculates the check sum of image data based on the PROPRA file specification
	 * V3.0.
	 * 
	 * @param inputData the data of which this checksum shall be calculated
	 * @throws ImageHandlingException when an error occurred handling the image file
	 */
	public byte[] getCheckSum(int headerLength) throws ImageHandlingException {
		
		fileHandler.createFile();
		fileHandler.openInputStream();
		fileHandler.skipNBytes(headerLength);
		
		byte[] inputData;
		while ((inputData = fileHandler.readData()) != null) {
			calculateChecksum(inputData);
		}
		
		fileHandler.closeFile();		
		
		int checkSum = a_i * (int) Math.pow(2, 16) + b_i;
		byte[] checkSumArray = new byte[4];
		checkSumArray[0] = (byte) checkSum;
		checkSumArray[1] = (byte) (checkSum >> 8);
		checkSumArray[2] = (byte) (checkSum >> 16);
		checkSumArray[3] = (byte) (checkSum >> 24);

		return checkSumArray;
	}

	private void calculateChecksum(byte[] inputData) {

		for (int i = 0; i < inputData.length; i++) {
			a_i += (i + bytesInTotal + 1) + Byte.toUnsignedInt(inputData[i]);
			a_i %= X;
			b_i = (b_i % X + a_i) % X;
		}
		bytesInTotal += inputData.length;

	}


}
