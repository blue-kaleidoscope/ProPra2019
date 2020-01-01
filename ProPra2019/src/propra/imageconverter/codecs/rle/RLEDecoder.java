package propra.imageconverter.codecs.rle;

import java.util.ArrayList;
import java.util.List;

import propra.imageconverter.codecs.Decoder;
import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.util.Util;

public class RLEDecoder extends Decoder {

	/**
	 * In case during decoding the data, more bytes were necessary than were given
	 * in the last decode()-call. This <code>List</code> contains the bytes which
	 * will be used in the next decode()-call.
	 */
	private List<Byte> remainingBytes;

	private int pixelCount;
	private int processedBytes;
	private boolean equalPixel;

	public RLEDecoder(long byteCount) {
		super(byteCount);
		remainingBytes = new ArrayList<Byte>();
		pixelCount = 0;
		equalPixel = false;
	}

	@Override
	public byte[] decode(byte[] inputData) throws ImageHandlingException {
		List<Byte> inputAsList = Util.byteArrayToList(inputData);
		decodedData.clear();
		if(alreadyDecodedBytes < byteCount) {
			for (Byte currentByte : inputAsList) {
				if (decodingState == DECODING_STATES.WAITING_FOR_HEADER_DATA) {
					pixelCount = getPixelCount(currentByte);
					processedBytes = 0;
					equalPixel = equalPixels(currentByte);
					decodingState = DECODING_STATES.WAITING_FOR_DECODING_DATA;					
					remainingBytes.clear();
				} else {
					if (equalPixel) {
						remainingBytes.add(currentByte);
						alreadyDecodedBytes++;
						if (++processedBytes == 3) {
							decodingState = DECODING_STATES.WAITING_FOR_HEADER_DATA;
							for (int i = 0; i < pixelCount; i++) {
								decodedData.addAll(remainingBytes);							
							}
						}
					} else {
						remainingBytes.add(currentByte);
						if(++processedBytes == pixelCount * 3) {
							decodingState = DECODING_STATES.WAITING_FOR_HEADER_DATA;
							decodedData.addAll(remainingBytes);
						}
					}
				}
			}
		}		

		return Util.byteListToArray(decodedData);
	}

	private int getPixelCount(byte controlByte) {
		return (controlByte & 0x7F) + 1;
	}

	private boolean equalPixels(byte controlByte) {
		return (controlByte & 0x80) == 0x80;
	}

	@Override
	public byte[] flush() throws ImageHandlingException {
		// Nothing to do here
		return null;
	}

}
