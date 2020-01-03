package propra.imageconverter.codecs;

import java.util.ArrayList;
import java.util.List;

import propra.imageconverter.error.ImageHandlingException;

/**
 * An <code>Encoder</code> encodes images and files based on the implemented
 * algorithm of this <code>Encoder</code>.
 * 
 * @author Oliver Eckstein
 *
 */
public abstract class Encoder {

	/**
	 * To define in which state this encoder is.
	 */
	protected EncodingState encodingState;

	/**
	 * The encoded data of this encoding run.
	 */
	protected List<Byte> encodedData;

	/**
	 * States this encoder can have.
	 * 
	 * @author Oliver Eckstein
	 *
	 */
	protected enum EncodingState {
		PREPARING, WAITING_FOR_DATA, WRITING_HEADER_DATA, ENCODING, FINISHED
	}

	/**
	 * Creates a new <code>Encoder</code> and lets it wait for data to be encoded.
	 */
	public Encoder() {
		encodingState = EncodingState.FINISHED;
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
	 * To encode data using this <code>Encoder</code>'s algorithm.
	 * 
	 * @param inputData the data to be encoded
	 * @return the encoded data
	 * @throws ImageHandlingException when an error occurred during encoding
	 */
	public abstract byte[] encode(byte[] inputData) throws ImageHandlingException;

	/**
	 * Lets this <code>Encoder</code> return the remaining data to be encoded. This
	 * method should be called after the last encode-pass has been executed to
	 * ensure that all data gets encoded.
	 * 
	 * @return the remaining data to be encoded.
	 * @throws ImageHandlingException when an error occurred during encoding
	 */
	public abstract byte[] flush() throws ImageHandlingException;

	/**
	 * To reset this <code>Encoder</code> and prepare it for a new conversion. After
	 * calling this method this <code>Encoder</code> is in the same state as it was
	 * after creating it.
	 */
	public abstract void reset();

}
