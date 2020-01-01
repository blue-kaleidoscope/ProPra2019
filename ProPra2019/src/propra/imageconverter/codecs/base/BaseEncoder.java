package propra.imageconverter.codecs.base;

import java.util.ArrayList;
import java.util.List;

import propra.imageconverter.codecs.Encoder;
import propra.imageconverter.error.ErrorCodes;
import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.util.Util;

public class BaseEncoder extends Encoder {

	private static final String BASE32_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUV";
	private String alphabet;

	/**
	 * How many bits form one output byte. Example: Base-32 5 bits form one output
	 * byte.
	 */
	private int outputByteLength;
	int inputPackageByteCount;

	private List<Byte> remainingBytes;

	public BaseEncoder(String alphabet) throws ImageHandlingException {
		super();
		checkAlphabet(alphabet);
		this.alphabet = alphabet;
		outputByteLength = (int) (Math.log(alphabet.length()) / Math.log(2));
		inputPackageByteCount = Util.lcm(8, outputByteLength) / 8;
		remainingBytes = new ArrayList<Byte>();

	}

	public BaseEncoder() {
		super();
		this.alphabet = BASE32_ALPHABET;
		outputByteLength = 5;
		inputPackageByteCount = 5;
		remainingBytes = new ArrayList<Byte>();
	}

	@Override
	public byte[] encode(byte[] inputData) throws ImageHandlingException {
		encodedData.clear();
		List<Byte> inputAsList = Util.byteArrayToList(inputData);
		
		for (Byte currentByte : inputAsList) {
			if (currentByte != null) {
				remainingBytes.add(currentByte);
				encodingState = ENCODING_STATES.WAITING_FOR_DATA;
			}

			if (remainingBytes.size() == inputPackageByteCount) {
				encodedData.addAll(encodePackage(remainingBytes, inputPackageByteCount));
				remainingBytes.clear();
				encodingState = ENCODING_STATES.FINISHED;
			}
		}

		return Util.byteListToArray(encodedData);
	}

	private List<Byte> encodePackage(List<Byte> inputData, int inputPackageByteCount) {
		byte[] alphaBytes = alphabet.getBytes();
		long byteBuffer = 0;
		int outputPackageByteCount = inputPackageByteCount * 8 / outputByteLength;
		if ((inputPackageByteCount * 8) % outputByteLength != 0) {
			outputPackageByteCount++;
		}

		List<Byte> output = new ArrayList<Byte>();
		for (int i = 0; i < inputData.size(); i++) {
			byteBuffer <<= 8;
			byteBuffer |= inputData.get(i) & 0xFF;
		}

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

	public static void main(String[] args) throws ImageHandlingException {
		BaseEncoder b32 = new BaseEncoder("abcdefgh");
		String fooba = "foo";
		//String r = "ar";
		byte[] output1 = b32.encode(fooba.getBytes());
		System.out.println(new String(output1));
		//byte[] output2 = b32.encode(r.getBytes());		
		output1 = b32.flush();
		System.out.println(new String(output1));
	}

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
					ErrorCodes.INVALID_USER_INPUT);

		}
	}

	private static void checkForDuplicates(String alphabet) throws ImageHandlingException {
		char[] alphabetInChars = alphabet.toCharArray();
		for (int i = 0; i < alphabetInChars.length; i++) {
			for (int j = i + 1; j < alphabetInChars.length; j++) {
				if (alphabetInChars[i] == alphabetInChars[j]) {
					throw new ImageHandlingException(
							"Invalid encoding alphabet. Must not contain duplicate characters.",
							ErrorCodes.INVALID_USER_INPUT);
				}
			}
		}
	}

	@Override
	public byte[] flush() throws ImageHandlingException {
		encodedData.clear();
		if (encodingState == ENCODING_STATES.WAITING_FOR_DATA) {
			if (remainingBytes.size() > 0) {
				int lastInputPackageLength = remainingBytes.size() % inputPackageByteCount;
				encodedData.addAll(encodePackage(remainingBytes, lastInputPackageLength));
				encodingState = ENCODING_STATES.FINISHED;
				remainingBytes.clear();
			}
		}

		return Util.byteListToArray(encodedData);
	}

	@Override
	public void prepareEncoding(byte[] inputData) throws ImageHandlingException {
		//Nothing to do here...		
	}

	@Override
	public void reset() {
		remainingBytes.clear();
		encodedData.clear();
	}
}
