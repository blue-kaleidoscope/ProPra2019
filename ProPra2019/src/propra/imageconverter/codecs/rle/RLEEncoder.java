package propra.imageconverter.codecs.rle;

import java.util.ArrayList;
import java.util.List;

import propra.imageconverter.codecs.Encoder;
import propra.imageconverter.error.ImageConverterErrorCode;
import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.util.Util;

/**
 * An <code>RLEEncoder</code> encodes uncompressed images using the
 * run-length-encoding algorithm. This encoder collects the image's data line by
 * line an performs the compression line by line.
 * 
 * @author Oliver Eckstein
 *
 */
public class RLEEncoder extends Encoder {

	/**
	 * The current image's line to be encoded.
	 */
	private List<Byte> currentLine;

	/**
	 * The data to be encoded.
	 */
	private List<Byte> inputData;

	/**
	 * The maximum number of pixels which can be encoded after a header byte.
	 */
	private final int MAX_PIXEL_STREAK = 128;

	/**
	 * The image's width which gets encoded.
	 */
	private int lineLength;

	/**
	 * Creates a new <code>RLEEncoder</code>.
	 * 
	 * @param lineLength the image's width which gets encoded.
	 */
	public RLEEncoder(int lineLength) {
		this.lineLength = lineLength * 3;
		currentLine = new ArrayList<Byte>();
	}

	@Override
	public byte[] encode(byte[] inputData) throws ImageHandlingException {
		this.inputData = Util.byteArrayToList(inputData);
		encodedData.clear();
		// Let's start with buffering the next line
		getNextLine();
		while (encodingState == EncodingState.ENCODING) {
			int equalPixels = 0;
			List<Byte> nextPixel;
			List<Byte> currentPixel = getNextPixel();
			List<Byte> unequalPixels = new ArrayList<Byte>();

			while ((nextPixel = getNextPixel()) != null) {
				// Scan the current line pixel by pixel
				if (equals(currentPixel, nextPixel)) {
					if (unequalPixels.size() > 0) {
						// After a streak of unequal pixels, at least two equal pixels were detected
						// Therefore the unequal pixels get written
						encodedData.add(getControlByte(unequalPixels.size() / 3, false));
						encodedData.addAll(unequalPixels);
						unequalPixels.clear();
					}
					equalPixels++;
				} else {
					if (equalPixels > 0) {
						// After a streak of equal pixels, at least two unequal pixels were detected
						// Therefore the equal pixels get written
						encodedData.add(getControlByte(equalPixels, true));
						encodedData.addAll(currentPixel);
						equalPixels = 0;
					} else {
						unequalPixels.addAll(currentPixel);
					}
				}

				if (equalPixels == MAX_PIXEL_STREAK) {
					// 128 equal pixels get written
					encodedData.add(getControlByte(MAX_PIXEL_STREAK - 1, true));
					encodedData.addAll(currentPixel);
					equalPixels = 0;
				}

				if (unequalPixels.size() / 3 == MAX_PIXEL_STREAK) {
					// 128 unequal pixels get written
					encodedData.add(getControlByte(MAX_PIXEL_STREAK, false));
					encodedData.addAll(unequalPixels);
					unequalPixels.clear();
				}

				currentPixel = nextPixel;
			}

			if (equalPixels == 0) {
				// After a streak of equal pixels, there is only one more pixel at the end of
				// this line which gets written.
				// This pixel is unequal from the preceding pixels.
				unequalPixels.addAll(currentPixel);
			}

			// The last pixels of this line get written
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
					ImageConverterErrorCode.COMPRESSION_ERROR);
		}
		return null;
	}

	/**
	 * Buffers the next line of the image into the encoder. Changes the encoder's
	 * state to <code>ENCODING</code> when enough data was given to the encoder to
	 * buffer a full line.
	 */
	private void getNextLine() {
		encodingState = EncodingState.WAITING_FOR_DATA;
		int removeIndex = 0;
		for (Byte currentByte : inputData) {
			removeIndex++;
			currentLine.add(currentByte);
			if (currentLine.size() == lineLength) {
				encodingState = EncodingState.ENCODING;
				break;
			}
		}
		inputData.subList(0, removeIndex).clear();
	}

	/**
	 * Returns the next pixel of the current line.
	 * 
	 * @return the next pixel
	 * @throws ImageHandlingException when not enough bytes were given to encode
	 *                                this line.
	 */
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
			throw new ImageHandlingException(
					"An error occured during RLE encoding. Not enough bytes were given in order to perform RLE encoding.",
					ImageConverterErrorCode.COMPRESSION_ERROR);
		}

		return null;
	}

	/**
	 * Compares to pixels whether they are equal or not.
	 * 
	 * @param pixel1 the first pixel.
	 * @param pixel2 the second pixel
	 * @return <code>true</code> when both pixels are equal, <code>false</code>
	 *         otherwise.
	 * @throws ImageHandlingException when the given pixels contained less or more
	 *                                than 3 bytes.
	 */
	private boolean equals(List<Byte> pixel1, List<Byte> pixel2) throws ImageHandlingException {
		if (pixel1.size() != 3 || pixel2.size() != 3) {
			throw new ImageHandlingException("An error occured during RLE encoding. Could not compare two pixels.",
					ImageConverterErrorCode.COMPRESSION_ERROR);
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

	/**
	 * Creates a new RLE header byte.
	 * 
	 * @param pixelCount  the number of pixels following after the header byte.
	 * @param equalPixels <code>true</code> when the pixels after the header byte
	 *                    are equal, <code>false</code> otherwise.
	 * @return the header byte.
	 */
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
