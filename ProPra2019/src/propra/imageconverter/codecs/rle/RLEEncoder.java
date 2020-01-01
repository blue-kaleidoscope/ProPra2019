package propra.imageconverter.codecs.rle;

import java.util.ArrayList;
import java.util.List;

import propra.imageconverter.codecs.Encoder;
import propra.imageconverter.error.ErrorCodes;
import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.util.Util;

public class RLEEncoder extends Encoder {

	private List<Byte> currentLine;
	private List<Byte> inputData;
	private final int MAX_PIXEL_STREAK = 128;
	private int lineLength;

	public RLEEncoder(int lineLength) throws ImageHandlingException {
		this.lineLength = lineLength * 3;
		currentLine = new ArrayList<Byte>();
	}

	@Override
	public byte[] encode(byte[] inputData) throws ImageHandlingException {
		this.inputData = Util.byteArrayToList(inputData);
		encodedData.clear();
		getNextLine();
		while (encodingState == ENCODING_STATES.ENCODING) {
			int equalPixels = 0;
			List<Byte> nextPixel;
			List<Byte> currentPixel = getNextPixel();
			List<Byte> unequalPixels = new ArrayList<Byte>();

			while ((nextPixel = getNextPixel()) != null) {
				if (equals(currentPixel, nextPixel)) {
					if (unequalPixels.size() > 0) {
						encodedData.add(getControlByte(unequalPixels.size() / 3, false));
						encodedData.addAll(unequalPixels);
						unequalPixels.clear();
					}
					equalPixels++;
				} else {
					if (equalPixels > 0) {
						encodedData.add(getControlByte(equalPixels, true));
						encodedData.addAll(currentPixel);
						equalPixels = 0;
					} else {
						unequalPixels.addAll(currentPixel);
					}
				}

				if (equalPixels == MAX_PIXEL_STREAK) {
					encodedData.add(getControlByte(MAX_PIXEL_STREAK - 1, true));
					encodedData.addAll(currentPixel);
					equalPixels = 0;
				}

				if (unequalPixels.size() / 3 == MAX_PIXEL_STREAK) {
					encodedData.add(getControlByte(MAX_PIXEL_STREAK, false));
					encodedData.addAll(unequalPixels);
					unequalPixels.clear();
				}

				currentPixel = nextPixel;
			}

			if (equalPixels == 0) {
				unequalPixels.addAll(currentPixel);
			}

			if (equalPixels > 0) {
				encodedData.add(getControlByte(equalPixels, true));
				encodedData.addAll(currentPixel);
			} else if (unequalPixels.size() > 0) {
				encodedData.add(getControlByte(unequalPixels.size() / 3, false));
				encodedData.addAll(unequalPixels);
			}

			currentLine.clear();
			getNextLine();
		}

		return Util.byteListToArray(encodedData);
	}

	@Override
	public byte[] flush() throws ImageHandlingException {
		if (currentLine.size() != 0) {
			throw new ImageHandlingException(
					"Not enough bytes were given in order to perform RLE encoding. Missing number of bytes: "
							+ (lineLength - currentLine.size()),
					ErrorCodes.COMPRESSION_ERROR);
		}
		return null;
	}

	private void getNextLine() {
		encodingState = ENCODING_STATES.WAITING_FOR_DATA;
		int removeIndex = 0;
		for (Byte currentByte : inputData) {
			removeIndex++;
			currentLine.add(currentByte);
			if (currentLine.size() == lineLength) {
				encodingState = ENCODING_STATES.ENCODING;
				break;
			}
		}
		inputData.subList(0, removeIndex).clear();
	}

	private List<Byte> getNextPixel() throws ImageHandlingException {
		if (currentLine.size() >= 3) {
			List<Byte> output = new ArrayList<Byte>();
			for (int i = 0; i < 3; i++) {
				output.add(currentLine.get(i));
			}
			currentLine.subList(0, 3).clear();
			return output;
		}

		if (currentLine.size() > 0) {
			throw new ImageHandlingException("An error occured during RLE encoding.", ErrorCodes.COMPRESSION_ERROR);
		}

		return null;
	}

	private boolean equals(List<Byte> pixel1, List<Byte> pixel2) throws ImageHandlingException {
		if (pixel1.size() != 3 || pixel2.size() != 3) {
			throw new ImageHandlingException("An error occured during RLE encoding. Could not compare two pixels.",
					ErrorCodes.COMPRESSION_ERROR);
		}

		boolean pixelsEqual = true;
		for (int i = 0; i < 3; i++) {
			if (pixel1.get(i) != pixel2.get(i)) {
				pixelsEqual = false;
				break;
			}
		}
		return pixelsEqual;
	}

	private byte getControlByte(int pixelCount, boolean equalPixels) {
		if (!equalPixels)
			pixelCount--;
		return (byte) (equalPixels == true ? 0x80 + pixelCount : pixelCount);
	}

	/**
	 * This <code>RLEEncoder</code> does not need any preparation. The method
	 * <code>encode(byte[] inputData)</code> can directly be called.
	 */
	@Override
	public void prepareEncoding(byte[] inputData) throws ImageHandlingException {
		// Nothing to do here
	}

	@Override
	public void reset() {
		encodedData.clear();
		currentLine.clear();
	}
}
