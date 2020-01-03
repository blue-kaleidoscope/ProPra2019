package propra.imageconverter.codecs.base;

import java.util.ArrayList;
import java.util.List;

import propra.imageconverter.codecs.Encoder;
import propra.imageconverter.error.ImageConverterErrorCode;
import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.util.Util;

/**
 * A <code>BaseEncoder</code> encodes files using a base-n algorithm. It
 * supports base-2, base-4, base-8, base-16, base-32 and base-64 codec.
 * 
 * @author Oliver Eckstein
 *
 */
public class BaseEncoder extends Encoder {

	/**
	 * The standard base-32 alphabet.
	 */
	private static final String BASE32_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUV";

	/**
	 * This <code>BaseEncoder</code>'s encoding alphabet.
	 */
	private String encodingAlphabet;

	/**
	 * Of how many bits consists one output byte. Example with base-32: 5 bits form
	 * one output byte.
	 */
	private int outputByteLength;

	/**
	 * Of how many bytes consists one package which will get encoded.
	 */
	int inputPackageByteCount;

	/**
	 * The remaining bytes if this <code>BaseEncoder</code> received less bytes to
	 * encode than the size of <code>inputPackageByteCount</code>.
	 */
	private List<Byte> remainingBytes;

	/**
	 * Creates a new <code>BaseEncoder</code.
	 * 
	 * @param encodingAlphabet the encoding alphabet.
	 * @throws ImageHandlingException when the given alphabet is not a valid
	 *                                base-nencoding/decoding alphabet.
	 */
	public BaseEncoder(String encodingAlphabet) throws ImageHandlingException {
		super();
		checkAlphabet(encodingAlphabet);
		this.encodingAlphabet = encodingAlphabet;
		outputByteLength = (int) (Math.log(encodingAlphabet.length()) / Math.log(2));
		inputPackageByteCount = Util.lcm(8, outputByteLength) / 8;
		remainingBytes = new ArrayList<Byte>();

	}

	/**
	 * Creates a new base-32 encoder <code>BaseEncoder</code>.
	 * 
	 * @throws ImageHandlingException when an error occurred during decoding.
	 */
	public BaseEncoder() throws ImageHandlingException {
		this(BASE32_ALPHABET);
	}

	@Override
	public byte[] encode(byte[] inputData) throws ImageHandlingException {
		encodedData.clear();
		List<Byte> inputAsList = Util.byteArrayToList(inputData);

		// Collect byte by byte and start encoding when enough bytes are added to
		// 'remainingBytes'
		for (Byte currentByte : inputAsList) {
			if (currentByte != null) {
				remainingBytes.add(currentByte);
				encodingState = EncodingState.WAITING_FOR_DATA;
			}

			if (remainingBytes.size() == inputPackageByteCount) {
				encodedData.addAll(encodePackage(remainingBytes, inputPackageByteCount));
				remainingBytes.clear();
				encodingState = EncodingState.FINISHED;
			}
		}

		return Util.byteListToArray(encodedData);
	}

	private List<Byte> encodePackage(List<Byte> inputData, int inputPackageByteCount) {
		byte[] alphaBytes = encodingAlphabet.getBytes();
		// byteBuffer will contain all bits from this input byte package
		// the bits will be extracted in order to perform the encoding
		long byteBuffer = 0;
		int outputPackageByteCount = inputPackageByteCount * 8 / outputByteLength;
		if ((inputPackageByteCount * 8) % outputByteLength != 0) {
			// When an input byte package is not complete (i.e. at the end of a file to be
			// encoded)
			outputPackageByteCount++;
		}

		List<Byte> output = new ArrayList<Byte>();
		for (int i = 0; i < inputData.size(); i++) {
			byteBuffer <<= 8;
			byteBuffer |= inputData.get(i) & 0xFF;
		}
		// byteBuffer now contains all bits from the current byte package
		// Now we extract the bits byte wise and encode them

		// Removes the current byte from the byteBuffer
		byte removalMask = 0;
		for (int i = 0; i < outputByteLength; i++) {
			removalMask <<= 1;
			removalMask |= 0x1;
		}

		int shiftCount = inputPackageByteCount * 8 - outputByteLength;
		byte outputBits = 0;
		for (int i = 0; i < outputPackageByteCount; i++) {
			if (shiftCount > 0) {
				outputBits = (byte) ((byteBuffer >> shiftCount) & removalMask);
			} else {
				byte lastBitsExtractionMask = 0;
				for (int j = 0; j < outputByteLength + shiftCount; j++) {
					lastBitsExtractionMask <<= 1;
					lastBitsExtractionMask |= 0x1;
				}
				outputBits = (byte) ((lastBitsExtractionMask & byteBuffer) << -shiftCount);
				// With the above line it is ensured that the last (output) bits will be filled
				// with zeros as stated in the specification.

			}
			output.add(alphaBytes[outputBits]);
			shiftCount -= outputByteLength;
		}

		return output;
	}

	/**
	 * To check if a given alphabet is a valid base-n encoding/decoding alphabet.
	 * 
	 * @param alphabet the alphabet potentially used for encoding/decoding.
	 * @throws ImageHandlingException when the given alphabet is not a valid base-n
	 *                                encoding/decoding alphabet.
	 */
	public static void checkAlphabet(String alphabet) throws ImageHandlingException {
		checkLength(alphabet);
		checkForDuplicates(alphabet);
	}

	private static void checkLength(String alphabet) throws ImageHandlingException {
		int length = alphabet.length();
		switch (length) {
		case 2:
			break;
		case 4:
			break;
		case 8:
			break;
		case 16:
			break;
		case 32:
			break;
		case 64:
			break;
		default:
			throw new ImageHandlingException("Invalid encoding alphabet. Allowed length: 2, 4, 8, 16, 32, 64.",
					ImageConverterErrorCode.INVALID_USER_INPUT);

		}
	}

	private static void checkForDuplicates(String alphabet) throws ImageHandlingException {
		char[] alphabetInChars = alphabet.toCharArray();
		for (int i = 0; i < alphabetInChars.length; i++) {
			for (int j = i + 1; j < alphabetInChars.length; j++) {
				if (alphabetInChars[i] == alphabetInChars[j]) {
					throw new ImageHandlingException(
							"Invalid encoding alphabet. Must not contain duplicate characters.",
							ImageConverterErrorCode.INVALID_USER_INPUT);
				}
			}
		}
	}

	/**
	 * Returns the last base-n encoded bytes of this <code>BaseEncoder</code>.
	 */
	@Override
	public byte[] flush() throws ImageHandlingException {
		encodedData.clear();
		if (encodingState == EncodingState.WAITING_FOR_DATA) {
			if (remainingBytes.size() > 0) {
				int lastInputPackageLength = remainingBytes.size() % inputPackageByteCount;
				encodedData.addAll(encodePackage(remainingBytes, lastInputPackageLength));
				encodingState = EncodingState.FINISHED;
				remainingBytes.clear();
			}
		}

		return Util.byteListToArray(encodedData);
	}

	@Override
	public void prepareEncoding(byte[] inputData) throws ImageHandlingException {
		// Nothing to do here...
	}

	@Override
	public void reset() {
		remainingBytes.clear();
		encodedData.clear();
	}
}
