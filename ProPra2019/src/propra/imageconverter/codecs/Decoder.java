package propra.imageconverter.codecs;

import java.util.ArrayList;
import java.util.List;

import propra.imageconverter.error.ImageHandlingException;

public abstract class Decoder {
	/**
	 * To define in which state this decoder is.
	 */
	
	protected DECODING_STATES decodingState;
	/**
	 * The decoded data of this decoding run.
	 */
	protected List<Byte> decodedData;
	
	/**
	 * States this decoder can have. Either it is waiting for header data or for data which should be decoded.
	 * @author Oliver Eckstein
	 *
	 */
	public enum DECODING_STATES {
		WAITING_FOR_HEADER_DATA,
		WAITING_FOR_DECODING_DATA,
		FINISHED
	}
	
	/**
	 * To create a new <code>Decoder</code>.
	 */
	public Decoder() {
		decodingState = DECODING_STATES.WAITING_FOR_HEADER_DATA;
		decodedData = new ArrayList<Byte>();
	}
	
	/**
	 * To decode encoded data.
	 * @param inputData the encoded data
	 * @return the decoded data
	 * @throws ImageHandlingException when an error occured during decoding
	 */
	public abstract byte[] decode(byte[] inputData) throws ImageHandlingException;
	
	public abstract byte[] flush() throws ImageHandlingException;

}
