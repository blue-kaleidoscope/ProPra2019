package propra.imageconverter.codecs.rle;

import java.util.ArrayList;
import java.util.List;

import propra.imageconverter.codecs.Encoder;
import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.util.Util;

public class RLEEncoder extends Encoder {

	/**
	 * Length of one image line as byte count. This field gets used when this
	 * <code>Encoder</code> is used for compressing images. Example: If an image is
	 * 9 pixel wide and each pixel consists of three bytes then this field must be
	 * set with 9 * 3 = 27.
	 */
	private int lineLength;

	/**
	 * Creates a new <code>RLEEncoder</code> to encode images using the run length
	 * encoding algorithm.
	 * 
	 * @param lineLength length of one image line as byte count.Example: If an image
	 *                   is 9 pixel wide and each pixel consists of three bytes then
	 *                   this field must be set with 9 * 3 = 27.
	 */
	public RLEEncoder(int lineLength) {
		super();
		this.lineLength = lineLength;
	}

	/**
	 * Performs run-length-encoding on the given data and returns the run-length-encoded data.
	 */
	@Override	
	public byte[] encode(byte[] inputData) throws ImageHandlingException {

		int equalPixels = 0;
		int unequalPixels = 0;

		List<Byte> outputDatasegment = new ArrayList<Byte>();

		for (int i = 0; i < inputData.length; i += 3) {
			if (i == inputData.length - 3) {
				// Last pixel reached
				if (equalPixels > 0) {
					outputDatasegment.add((byte) (0x80 + equalPixels));
					// Before current pixel there was a streak of equal pixels which gets written
					outputDatasegment.add(inputData[i]);
					outputDatasegment.add(inputData[i + 1]);
					outputDatasegment.add(inputData[i + 2]);

				} else {
					// Raw pixels get written
					outputDatasegment.add((byte) unequalPixels);
					int arrayIndexUnequalPixel = unequalPixels;
					for (int j = 0; j <= unequalPixels; j++) {
						outputDatasegment.add(inputData[i - 3 * arrayIndexUnequalPixel]);
						outputDatasegment.add(inputData[i - 3 * arrayIndexUnequalPixel + 1]);
						outputDatasegment.add(inputData[i - 3 * arrayIndexUnequalPixel + 2]);
						arrayIndexUnequalPixel--;
					}
				}
			} else {
				if (((i + 1) % lineLength == 0 && i > 0) || equalPixels == 127 || unequalPixels == 127) {
					// End of image line reached or maximum length of possible 7 bit pixel counter
					// reached
					if (equalPixels > 0) {
						// Equal pixels count gets written
						outputDatasegment.add((byte) (0x80 + equalPixels));
						outputDatasegment.add(inputData[i]);
						outputDatasegment.add(inputData[i + 1]);
						outputDatasegment.add(inputData[i + 2]);
						equalPixels = 0;
					} else {
						// Raw pixels get written
						outputDatasegment.add((byte) unequalPixels);
						int arrayIndexUnequalPixel = unequalPixels;
						for (int j = 0; j <= unequalPixels; j++) {
							outputDatasegment.add(inputData[i - 3 * arrayIndexUnequalPixel]);
							outputDatasegment.add(inputData[i - 3 * arrayIndexUnequalPixel + 1]);
							outputDatasegment.add(inputData[i - 3 * arrayIndexUnequalPixel + 2]);
							arrayIndexUnequalPixel--;
						}
						unequalPixels = 0;
					}
				} else {
					if (inputData[i] == inputData[i + 3] && inputData[i + 1] == inputData[i + 4]
							&& inputData[i + 2] == inputData[i + 5]) {
						// Current and next pixel are equal
						equalPixels++;
						if (unequalPixels > 0) {
							// Before current pixel there were unequal pixels --> They get written as raw
							// pixels
							outputDatasegment.add((byte) (unequalPixels - 1));
							int arrayIndexUnequalPixel = unequalPixels;
							for (int j = 0; j < unequalPixels; j++) {
								outputDatasegment.add(inputData[i - 3 * arrayIndexUnequalPixel]);
								outputDatasegment.add(inputData[i - 3 * arrayIndexUnequalPixel + 1]);
								outputDatasegment.add(inputData[i - 3 * arrayIndexUnequalPixel + 2]);
								arrayIndexUnequalPixel--;
							}
							unequalPixels = 0;
						}
					} else {
						// Current and next pixel are unequal
						if (equalPixels > 0) {
							// Before current pixel there was a streak of equal pixels which gets written
							outputDatasegment.add((byte) (0x80 + equalPixels));
							outputDatasegment.add(inputData[i]);
							outputDatasegment.add(inputData[i + 1]);
							outputDatasegment.add(inputData[i + 2]);
							equalPixels = 0;
						} else {
							unequalPixels++;
						}
					}
				}
			}
		}
		return Util.byteListToArray(outputDatasegment);
	}

	@Override
	public byte[] flush() throws ImageHandlingException {
		// TODO Auto-generated method stub
		return null;
	}
}
