package propra.imageconverter.codecs.base;

import java.util.ArrayList;
import java.util.List;

import propra.imageconverter.codecs.Decoder;
import propra.imageconverter.error.ImageConverterErrorCode;
import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.util.Util;

/**
 * A <code>BaseDecoder</code> decodes files which were encoded using a base-n
 * algorithm. It supports base-2, base-4, base-8, base-16, base-32 and base-64
 * codec.
 * 
 * @author Oliver Eckstein
 *
 */
public class BaseDecoder extends Decoder {

	/**
	 * The standard base-32 alphabet.
	 */
	private static final String BASE32_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUV";

	/**
	 * This <code>BaseDecoder</code>'s decoding alphabet.
	 */
	private String decodingAlphabet;

	/**
	 * Of how many bits consists one input byte.
	 */
	private int inputByteLength;

	/**
	 * Of how many bytes consists one decoded package.
	 */
	private int outputPackageByteCount;

	/**
	 * Of how many bytes consists one encoded package.
	 */
	private int inputPackageByteCount;

	/**
	 * The remaining bytes if this <code>BaseDecoder</code> received less bytes to
	 * decode than the size of <code>inputPackageByteCount</code>.
	 */
	private List<Byte> remainingBytes;

	/**
	 * Creates a new <code>BaseDecoder</code>.
	 * 
	 * @param decodingAlphabet the decoding alphabet.
	 * @throws ImageHandlingException when the given alphabet is not a valid
	 *                                base-nencoding/decoding alphabet.
	 */
	public BaseDecoder(String decodingAlphabet) throws ImageHandlingException {
		super();
		BaseEncoder.checkAlphabet(decodingAlphabet);
		this.decodingAlphabet = decodingAlphabet;
		remainingBytes = new ArrayList<Byte>();
		inputByteLength = (int) (Math.log(decodingAlphabet.length()) / Math.log(2));
		outputPackageByteCount = Util.lcm(8, inputByteLength) / 8;
		inputPackageByteCount = outputPackageByteCount * 8 / inputByteLength;
	}

	/**
	 * Creates a new base-32 decoder <code>BaseDecoder</code>.
	 * 
	 * @throws ImageHandlingException when an error occurred during decoding.
	 */
	public BaseDecoder() throws ImageHandlingException {
		this(BASE32_ALPHABET);
	}

	@Override
	public byte[] decode(byte[] inputData) throws ImageHandlingException {
		decodedData.clear();
		List<Byte> inputAsList = Util.byteArrayToList(inputData);

		// Collect byte by byte and start decoding when enough bytes are added to
		// 'remainingBytes'
		for (Byte currentByte : inputAsList) {
			if (currentByte != null) {
				remainingBytes.add(currentByte);
				decodingState = DecodingState.WAITING_FOR_DECODING_DATA;
			}

			if (remainingBytes.size() == inputPackageByteCount) {
				decodedData.addAll(decodePackage(remainingBytes, false));
				remainingBytes.clear();
				decodingState = DecodingState.FINISHED;
			}
		}

		return Util.byteListToArray(decodedData);
	}

	private List<Byte> decodePackage(List<Byte> inputData, boolean flush) throws ImageHandlingException {
		List<Byte> output = new ArrayList<Byte>();

		int remainingOutputBytes = this.outputPackageByteCount;
		int remainingInputBytes = inputPackageByteCount;
		int shiftCount = 8 * outputPackageByteCount - 8;
		if (flush) {
			remainingInputBytes = inputData.size() % inputPackageByteCount;
			remainingOutputBytes = remainingInputBytes * inputByteLength / 8;
			shiftCount = remainingInputBytes * outputPackageByteCount - inputPackageByteCount;
		}
		// byteBuffer will contain all bits from this input byte package
		// the bits will be extracted in order to perform the decoding
		long byteBuffer = 0;

		// First the buffer gets filled
		for (int i = 0; i < remainingInputBytes; i++) {
			byteBuffer <<= inputByteLength;
			// Get one byte from the whole input byte package, decode and buffer it
			int alphabetIndex = decodingAlphabet.indexOf(inputData.get(i));
			if (alphabetIndex < 0) {
				throw new ImageHandlingException(
						"Invalid character found in the input byte stream which is not part of the given decoding alphabet.",
						ImageConverterErrorCode.INVALID_USER_INPUT);
			}
			byteBuffer |= decodingAlphabet.indexOf(inputData.get(i));
		}

		// byteBuffer now contains all bits from the current byte package
		// Now we extract the bits byte wise and transfer them to the output bytes
		for (int i = 0; i < remainingOutputBytes; i++) {
			if (shiftCount >= 0) {
				output.add((byte) ((byteBuffer >> shiftCount) & 0xFF));
			}
			shiftCount -= 8;
		}
		return output;
	}

	/**
	 * Returns the last base-n decoded bytes of this <code>BaseDecoder</code>.
	 */
	@Override
	public byte[] flush() throws ImageHandlingException {
		decodedData.clear();
		if (decodingState == DecodingState.WAITING_FOR_DECODING_DATA) {
			if (remainingBytes.size() > 0) {
				decodedData.addAll(decodePackage(remainingBytes, true));
				decodingState = DecodingState.FINISHED;
				remainingBytes.clear();
			}
		}
		return Util.byteListToArray(decodedData);
	}
}
