package propra.imageconverter.codecs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import propra.imageconverter.error.ImageConverterErrorCode;
import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.util.Util;

/**
 * An <code>RGBEncoder</code> encodes a pixel given as a 3-tuple in that way that the
 * first and second byte of the pixel get swapped.
 * @author Oliver Eckstein
 *
 */
public class RGBEncoder extends Encoder {
	
	/**
	 * In case during encoding (changing the byte order) more bytes were
	 * necessary than were given in the last encode()-call. This
	 * <code>byte[]</code>-array contains the bytes which will be used in the next
	 * encode()-call.
	 */
	private List<Byte> remainingBytes;
	
	/**
	 * To create a new <code>RGBEncoder</code>.
	 */
	public RGBEncoder() {
		super();
		remainingBytes = new ArrayList<Byte>();
	}
	/**
	 * To change the order of the given image data from RGB to GRB or vice versa.
	 * @return the bytes in the order <code>1-0-2-4-3-5-...-(n-1)-(n-2)-n</code>.
	 */
	@Override
	public byte[] encode(byte[] inputData) throws ImageHandlingException{		
		List<Byte> inputAsList = Util.byteArrayToList(inputData);				
		encodedData.clear();
		
		for(Byte currentByte : inputAsList) {
			encodingState = EncodingState.WAITING_FOR_DATA;
			if(currentByte != null) {
				remainingBytes.add(currentByte);
				if(remainingBytes.size() == 3) {
					Collections.swap(remainingBytes, 0, 1);
					encodedData.addAll(remainingBytes);
					remainingBytes.clear();
					encodingState = EncodingState.FINISHED;
				}
			}
		}
		return Util.byteListToArray(encodedData);
	}
	
	@Override
	public byte[] flush() throws ImageHandlingException {
		if(encodingState == EncodingState.WAITING_FOR_DATA) {
			// This exception is only thrown when the encoder waits for 1 or 2 bytes.
			throw new ImageHandlingException(
					"Image data segment corrupt.",
					ImageConverterErrorCode.INVALID_DATASEGMENT);
		}
		return null;
	}
	@Override
	public void prepareEncoding(byte[] inputData) throws ImageHandlingException {
		// Nothing to do here...		
	}
	
	@Override
	public void reset() {
		encodedData.clear();
		remainingBytes.clear();
		encodingState = EncodingState.FINISHED;
	}
}
