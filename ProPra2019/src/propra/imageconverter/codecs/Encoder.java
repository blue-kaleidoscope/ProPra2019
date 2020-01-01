package propra.imageconverter.codecs;

import java.util.ArrayList;
import java.util.List;

import propra.imageconverter.error.ImageHandlingException;

public abstract class Encoder {
	/**
	 * To define in which state this encoder is.
	 */
	protected ENCODING_STATES encodingState;

	/**
	 * The encoded data of this encoding run.
	 */
	protected List<Byte> encodedData;

	/**
	 * States this encoder can have. Either it is waiting for data or for data which
	 * should be decoded.
	 * 
	 * @author Oliver Eckstein
	 *
	 */
	protected enum ENCODING_STATES {
		PREPARING, WAITING_FOR_DATA, WRITING_HEADER_DATA, ENCODING, FINISHED
	}

	/**
	 * To create a new <code>Encoder</code>.
	 */
	public Encoder() {
		encodingState = ENCODING_STATES.FINISHED;
		encodedData = new ArrayList<Byte>();
	}

	/**
	 * Prepares this <code>Encoder</code> so that it can perform the actual encoding
	 * process. It needs to be called with the exact same data as the method
	 * <code>encode(byte[] inputData)</code> will be called with later on.
	 * 
	 * @param inputData the data to be encoded.
	 * @throws ImageHandlingException when an error occurred during preparing the
	 *                                encoding.
	 */
	public abstract void prepareEncoding(byte[] inputData) throws ImageHandlingException;

	/**
	 * To encode data.
	 * 
	 * @param inputData the data to be encoded
	 * @return the encoded data
	 * @throws ImageHandlingException when an error occurred during encoding
	 */
	public abstract byte[] encode(byte[] inputData) throws ImageHandlingException;

	public abstract byte[] flush() throws ImageHandlingException;

}
