package propra.imageconverter.codecs.base;

import java.util.ArrayList;
import java.util.List;

import propra.imageconverter.codecs.Decoder;
import propra.imageconverter.error.ErrorCodes;
import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.util.Util;

public class BaseDecoder extends Decoder {

	private static final String BASE32_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUV";
	private String alphabet;
	private int inputByteLength;
	private int outputPackageByteCount;
	private int inputPackageByteCount;
	private List<Byte> remainingBytes;

	public BaseDecoder(String alphabet) throws ImageHandlingException {
		super();
		BaseEncoder.checkAlphabet(alphabet);
		this.alphabet = alphabet;
		remainingBytes = new ArrayList<Byte>();
		inputByteLength = (int) (Math.log(alphabet.length()) / Math.log(2));
		outputPackageByteCount = Util.lcm(8, inputByteLength) / 8;
		inputPackageByteCount = outputPackageByteCount * 8 / inputByteLength;
	}

	public BaseDecoder() {
		super();
		this.alphabet = BASE32_ALPHABET;
		remainingBytes = new ArrayList<Byte>();
		inputByteLength = 5;
		outputPackageByteCount = 5;
		inputPackageByteCount = outputPackageByteCount * 8 / inputByteLength;
	}

	@Override
	public byte[] decode(byte[] inputData) throws ImageHandlingException {
		decodedData.clear();
		List<Byte> inputAsList = Util.byteArrayToList(inputData);

		for (Byte currentByte : inputAsList) {
			if (currentByte != null) {
				remainingBytes.add(currentByte);
				decodingState = DECODING_STATES.WAITING_FOR_DECODING_DATA;
			}

			if (remainingBytes.size() == inputPackageByteCount) {
				decodedData.addAll(decodePackage(remainingBytes, false));
				remainingBytes.clear();
				decodingState = DECODING_STATES.FINISHED;
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
		long byteBuffer = 0;

		for (int i = 0; i < remainingInputBytes; i++) {
			byteBuffer <<= inputByteLength;
			// Get one byte from the whole input byte package, decode and buffer it
			int alphabetIndex = alphabet.indexOf(inputData.get(i));
			if (alphabetIndex < 0) {
				throw new ImageHandlingException("Invalid character found in the input byte stream which is not part of the given decoding alphabet.",
						ErrorCodes.INVALID_USER_INPUT);
			}
			byteBuffer |= alphabet.indexOf(inputData.get(i));
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

	@Override
	public byte[] flush() throws ImageHandlingException {
		decodedData.clear();
		if (decodingState == DECODING_STATES.WAITING_FOR_DECODING_DATA) {
			if (remainingBytes.size() > 0) {
				decodedData.addAll(decodePackage(remainingBytes, true));
				decodingState = DECODING_STATES.FINISHED;
				remainingBytes.clear();
			}
		}
		return Util.byteListToArray(decodedData);
	}
}
