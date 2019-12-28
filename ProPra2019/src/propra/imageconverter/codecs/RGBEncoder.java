package propra.imageconverter.codecs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import propra.imageconverter.error.ErrorCodes;
import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.util.Util;

public class RGBEncoder extends Encoder {
	
	/**
	 * In case during encoding (changing the byte order) more bytes were
	 * necessary than were given in the last encode()-call. This
	 * <code>byte[]</code>-array contains the bytes which will be used in the next
	 * encode()-call.
	 */
	private List<Byte> remainingBytes;
	
	public RGBEncoder() {
		super();
		remainingBytes = new ArrayList<Byte>();
	}
	/**
	 * To change the order of the given image data from RGB to GRB or vice versa.
	 * @return the bytes in the order 1-0-2-4-3-5-...-(n-1)-(n-2)-n
	 */
	@Override
	public byte[] encode(byte[] inputData) throws ImageHandlingException{		
		List<Byte> inputAsList = Util.byteArrayToList(inputData);
		List<Byte> output = new ArrayList<Byte>();		
		
		for(Byte currentByte : inputAsList) {
			encodingState = ENCODING_STATES.WAITING_FOR_DATA;
			if(currentByte != null) {
				remainingBytes.add(currentByte);
				if(remainingBytes.size() == 3) {
					Collections.swap(remainingBytes, 0, 1);
					output.addAll(remainingBytes);
					remainingBytes.clear();
					encodingState = ENCODING_STATES.FINISHED;
				}
			}
		}
		return Util.byteListToArray(output);
	}
	
	@Override
	public byte[] flush() throws ImageHandlingException {
		if(encodingState == ENCODING_STATES.WAITING_FOR_DATA) {
			throw new ImageHandlingException(
					"Image data segment corrupt.",
					ErrorCodes.INVALID_DATASEGMENT);
		}
		return null;
	}
}
