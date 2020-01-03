package propra.imageconverter.codecs.rle;

import java.util.ArrayList;
import java.util.List;

import propra.imageconverter.codecs.Decoder;
import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.util.Util;

/**
 * An <code>RLEDecoder</code> decodes images which were encoded using the
 * run-length-encoding algorithm.
 * 
 * @author Oliver Eckstein
 *
 */
public class RLEDecoder extends Decoder {

	/**
	 * In case during decoding the data, more bytes were necessary than were given
	 * in the last decode()-call. This <code>List</code> contains the bytes which
	 * will be used in the next decode()-call.
	 */
	private List<Byte> remainingBytes;

	/**
	 * The number of equal or unequal pixels following after a header byte. 
	 */
	private int pixelCount;
	
	/**
	 * The number of already processed bytes after the last header byte.
	 */
	private int processedBytes;
	
	/**
	 * Indicates whether equal or unequal pixels follow after the last header byte.
	 */
	private boolean equalPixel;

	/**
	 * Creates a new <code>RLEDecoder</code>.
	 * 
	 * @param maxCountBytesToDecode the maximum number of bytes this
	 *                              <code>RLEDecoder</code> should be decoding.
	 */
	public RLEDecoder(long maxCountBytesToDecode) {
		super(maxCountBytesToDecode);
		remainingBytes = new ArrayList<Byte>();
		pixelCount = 0;
		equalPixel = false;
	}

	@Override
	public byte[] decode(byte[] inputData) throws ImageHandlingException {
		List<Byte> inputAsList = Util.byteArrayToList(inputData);
		decodedData.clear();
		// Ignore data which exceeds maxCountBytesToDecode
		if (alreadyDecodedBytes < maxCountBytesToDecode) {
			// Retrieve byte by byte
			for (Byte currentByte : inputAsList) {
				// The first byte MUST be a header byte
				if (decodingState == DecodingState.WAITING_FOR_HEADER_DATA) {
					// Check whether it indicates equal or unequal pixels and the pixel count
					pixelCount = getPixelCount(currentByte);
					processedBytes = 0;
					equalPixel = equalPixels(currentByte);
					decodingState = DecodingState.WAITING_FOR_DECODING_DATA;
					remainingBytes.clear();
				} else {					
					remainingBytes.add(currentByte);
					if (equalPixel) {												
						if (++processedBytes == 3) {
							decodingState = DecodingState.WAITING_FOR_HEADER_DATA;
							for (int i = 0; i < pixelCount; i++) {
								alreadyDecodedBytes += remainingBytes.size();
								decodedData.addAll(remainingBytes);
							}
						}
					} else {						
						if (++processedBytes == pixelCount * 3) {
							decodingState = DecodingState.WAITING_FOR_HEADER_DATA;
							decodedData.addAll(remainingBytes);
							alreadyDecodedBytes += remainingBytes.size();
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
