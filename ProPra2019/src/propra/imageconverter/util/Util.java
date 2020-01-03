package propra.imageconverter.util;

import java.util.ArrayList;
import java.util.List;

import propra.imageconverter.error.ImageConverterErrorCode;
import propra.imageconverter.error.ImageHandlingException;

/**
 * This helper class provides some basic static data operation methods.
 * 
 * @author Oliver Eckstein
 *
 */
public class Util {

	/**
	 * To convert a list of bytes to an array of bytes.
	 * 
	 * @param bytes the list of bytes
	 * @return the array of bytes
	 */
	public static byte[] byteListToArray(List<Byte> bytes) {
		byte[] byteArray = new byte[bytes.size()];
		for (int i = 0; i < bytes.size(); i++) {
			byte theByte = bytes.get(i);
			byteArray[i] = theByte;
		}

		return byteArray;
	}

	/**
	 * To convert a byte array to a char array representation.
	 * 
	 * @param input the byte array
	 * @return a bit representation of the byte array
	 */
	public static char[] byteArrayToCharArray(byte[] input) {
		char[] inputAsCharArray = new char[8 * input.length];
		for (int j = 0; j < input.length; j++) {
			for (int i = 0; i < 8; i++) {
				byte currentBit = (byte) ((input[j] >> (7 - i)) & 0x1);
				if (currentBit == 0) {
					inputAsCharArray[j * 8 + i] = '0';
				} else {
					inputAsCharArray[j * 8 + i] = '1';
				}
			}
		}
		return inputAsCharArray;
	}

	/**
	 * To convert a byte to a char array representation.
	 * 
	 * @param input the byte
	 * @return a bit representation of the byte
	 */
	public static char[] byteToCharArray(byte input) {
		char[] inputAsCharArray = new char[8];

		for (int i = 0; i < 8; i++) {
			byte currentBit = (byte) ((input >> (7 - i)) & 0x1);
			if (currentBit == 0) {
				inputAsCharArray[i] = '0';
			} else {
				inputAsCharArray[i] = '1';
			}
		}

		return inputAsCharArray;
	}

	/**
	 * To convert a byte to a list of Characters.
	 * 
	 * @param input the byte
	 * @return a bit representation of the byte
	 */
	public static List<Character> byteToCharList(byte input) {
		List<Character> output = new ArrayList<Character>();
		char[] inputAsChar = byteToCharArray(input);
		for (Character currentChar : inputAsChar) {
			output.add(currentChar);
		}
		return output;
	}

	/**
	 * To convert a list of characters into a byte.
	 * 
	 * @param input the list of characters
	 * @return
	 * @throws ImageHandlingException
	 */
	public static byte charListToByte(List<Character> input) throws ImageHandlingException {
		return charArrayToByte(charListToArray(input));
	}

	/**
	 * To convert a char array with the length of 8 containing only the characters
	 * '0' or '1' to a byte.
	 * 
	 * @param input the byte represented as a char-array
	 * @return the byte
	 * @throws ImageHandlingException when the char-array contained more than 8
	 *                                entries or a character different from '0' or
	 *                                '1'.
	 */
	public static byte charArrayToByte(char[] input) throws ImageHandlingException {
		if (input.length > 8) {
			throw new ImageHandlingException("Cannot cast char-array with more than 8 entries to byte. ",
					ImageConverterErrorCode.UNEXPECTED_ERROR);
		}
		byte output = 0x0;
		for (int i = 0; i < input.length; i++) {
			if (input[i] != '0' && input[i] != '1') {
				throw new ImageHandlingException("Cannot convert the given char-Array into a byte representation.",
						ImageConverterErrorCode.UNEXPECTED_ERROR);
			}
			output <<= 1;
			if (input[i] == '1') {
				output |= 0x1;
			}
		}
		return output;
	}

	/**
	 * Merges to char-arrays into one char-array
	 * 
	 * @param first  the first char-array
	 * @param second the second char-array
	 * @return the merged char-array
	 */
	public static char[] mergeCharArrays(char[] first, char[] second) {
		char[] output = new char[first.length + second.length];
		for (int i = 0; i < first.length; i++) {
			output[i] = first[i];
		}

		for (int i = 0; i < second.length; i++) {
			output[first.length + i] = second[i];
		}

		return output;
	}

	/**
	 * Returns the next byte of a given char-array containing only the characters
	 * '0' or '1'.
	 * 
	 * @param input  the char-array
	 * @param offset the offset from where the next up to 8 characters should be
	 *               used to create a byte representation
	 * @return the next byte from the offset's position.
	 * @throws ImageHandlingException when the char-array contanied a character
	 *                                different from '0' or '1' or when the given
	 *                                offset was smaller than 0 or greater than the
	 *                                length of the given char-array.
	 */
	public static byte getNextByte(char[] input, int offset) throws ImageHandlingException {
		byte output = 0x0;
		if (offset < input.length - 8 && offset >= 0) {
			char[] nextByte = new char[8];
			for (int i = 0; i < 8; i++) {
				nextByte[i] = input[offset + i];
			}
			output = charArrayToByte(nextByte);
		} else {
			throw new ImageHandlingException(
					"The given offset must be greater than 0 and smaller then the array's length.",
					ImageConverterErrorCode.UNEXPECTED_ERROR);
		}
		return output;
	}

	/**
	 * Transforms a byte-array to an int-array.
	 * @param input the byte-array
	 * @return the int-array
	 */
	public static int[] byteArrayToIntArray(byte[] input) {
		int[] output = new int[input.length];
		for (int i = 0; i < input.length; i++) {
			output[i] = Byte.toUnsignedInt(input[i]);
		}

		return output;
	}

	/**
	 * To convert an array of bytes to a list of bytes.
	 * 
	 * @param bytes the array of bytes
	 * @return the list of bytes
	 */
	public static List<Byte> byteArrayToList(byte[] input) {
		List<Byte> output = new ArrayList<Byte>();
		for (int i = 0; i < input.length; i++) {
			output.add(input[i]);
		}
		return output;
	}

	/**
	 * To convert a list of Characters to a char-array.
	 * 
	 * @param bytes the list of Characters
	 * @return the char-array
	 */
	public static char[] charListToArray(List<Character> input) {
		char[] output = new char[input.size()];
		for (int i = 0; i < input.size(); i++) {
			output[i] = input.get(i);
		}
		return output;
	}

	/**
	 * To calculate the greatest common divider of two numbers.
	 * 
	 * @param number1
	 * @param number2
	 * @return the greatest common divider of the two numbers.
	 */
	private static int gcd(int number1, int number2) {
		if (number1 == 0 || number2 == 0) {
			return number1 + number2;
		} else {
			int absNumber1 = Math.abs(number1);
			int absNumber2 = Math.abs(number2);
			int biggerValue = Math.max(absNumber1, absNumber2);
			int smallerValue = Math.min(absNumber1, absNumber2);
			return gcd(biggerValue % smallerValue, smallerValue);
		}
	}

	/**
	 * To calculate the least common multiplier of two numbers.
	 * 
	 * @param number1
	 * @param number2
	 * @return the least common multiplier of two numbers.
	 */
	public static int lcm(int number1, int number2) {
		if (number1 == 0 || number2 == 0)
			return 0;
		else {
			int gcd = gcd(number1, number2);
			return Math.abs(number1 * number2) / gcd;
		}
	}
}
