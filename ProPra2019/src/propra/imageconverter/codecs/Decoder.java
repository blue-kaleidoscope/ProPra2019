package propra.imageconverter.codecs;

import java.util.ArrayList;
import java.util.List;

import propra.imageconverter.error.ImageHandlingException;

/**
 * A <code>Decoder</code> decodes images and files based on the implemented
 * algorithm of this <code>Decoder</code>.
 * 
 * @author Oliver Eckstein
 *
 */
public abstract class Decoder {

	/**
	 * To define in which state this decoder is.
	 */
	protected DecodingState decodingState;

	/**
	 * The decoded data of this decoding pass.
	 */
	protected List<Byte> decodedData;

	/**
	 * The number of bytes which have already been decoded by this
	 * <code>Decoder</code>.
	 */
	protected long alreadyDecodedBytes;

	/**
	 * The maximum number of bytes this <code>Decoder</code> should be decoding. It
	 * ignores further data as soon as this threshold has been reached.
	 */
	protected long maxCountBytesToDecode;

	/**
	 * States this decoder can have.
	 * 
	 * @author Oliver Eckstein
	 *
	 */
	public enum DecodingState {
		WAITING_FOR_HEADER_DATA, WAITING_FOR_DECODING_DATA, FINISHED
	}

	/**
	 * Creates a new <code>Decoder</code> and lets it wait for header data.
	 */
	public Decoder() {
		decodingState = DecodingState.WAITING_FOR_HEADER_DATA;
		decodedData = new ArrayList<Byte>();
		alreadyDecodedBytes = 0;
	}

	/**
	 * Creates a new <code>Decoder</code> and lets it wait for header data.
	 * 
	 * @param maxCountBytesToDecode the maximum number of bytes this
	 *                              <code>Decoder</code> should be decoding.
	 */
	public Decoder(long maxCountBytesToDecode) {
		this();
		this.maxCountBytesToDecode = maxCountBytesToDecode;
	}

	/**
	 * To decode encoded data using this <code>Decoder</code>'s algorithm.
	 * 
	 * @param inputData the encoded data to be decoded.
	 * @return the decoded data of this decoding pass.
	 * @throws ImageHandlingException when an error occurred during decoding
	 */
	public abstract byte[] decode(byte[] inputData) throws ImageHandlingException;

	/**
	 * Lets this <code>Decoder</code> return the remaining data to be decoded. This
	 * method should be called after the last decode-pass has been executed to
	 * ensure that all data gets decoded.
	 * 
	 * @return the remaining data to be decoded.
	 * @throws ImageHandlingException when an error occurred during decoding
	 */
	public abstract byte[] flush() throws ImageHandlingException;

}
