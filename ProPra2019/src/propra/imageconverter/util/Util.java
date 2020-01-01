package propra.imageconverter.util;

import java.util.ArrayList;
import java.util.List;

import propra.imageconverter.error.ErrorCodes;
import propra.imageconverter.error.ImageHandlingException;

public class Util {

	public static byte[] byteListToArray(List<Byte> bytes) {
		byte[] byteArray = new byte[bytes.size()];
		for (int i = 0; i < bytes.size(); i++) {
			byte theByte = bytes.get(i);
			byteArray[i] = theByte;
		}

		return byteArray;
	}

	/*
	 * private static byte reverseBits(byte input) { byte reversedInput = 0;
	 * 
	 * for(int i = 0; i < 8; i++) { reversedInput = (byte) ((reversedInput << 1) |
	 * (input & 1)); input >>= 1; } return reversedInput; }
	 * 
	 * public static byte[] reverseBitsOfBytes(byte[] input) { byte[] output = new
	 * byte[input.length]; for(int i = 0; i < input.length; i ++) { output[i] =
	 * reverseBits(input[i]); } return output; }
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
	
	public static List<Character> byteToCharList(byte input) {
		List<Character> output = new ArrayList<Character>();
		char[] inputAsChar = byteToCharArray(input);
		for(Character currentChar : inputAsChar) {
			output.add(currentChar);
		}
		return output;
	}
	
	public static byte charListToByte(List<Character> input) throws ImageHandlingException {
		return charArrayToByte(charListToArray(input));
	}

	public static byte charArrayToByte(char[] input) throws ImageHandlingException {
		if(input.length > 8) {
			throw new ImageHandlingException(
					"Cannot cast char-array with more than 8 entries to byte. ",
					ErrorCodes.INVALID_USER_INPUT);
		}
		byte output = 0x0;
		for (int i = 0; i < input.length; i++) {
			output <<= 1;
			if (input[i] == '1') {
				output |= 0x1;
			}
		}
		return output;
	}

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

	public static byte getNextByte(char[] input, int offset) throws ImageHandlingException {
		byte output = 0x0;
		if (offset < input.length - 8 && offset >= 0) {
			char[] nextByte = new char[8];
			for (int i = 0; i < 8; i++) {
				nextByte[i] = input[offset + i];
			}
			output = charArrayToByte(nextByte);
		}
		return output;
	}
	
	public static byte getNextByte(List<Character> input) throws ImageHandlingException {
		int byteLength = 8;
		if(input.size() < 8) {
			byteLength = input.size();
		}
		
		char[] inputAsChar = new char[8];
		for(int i = 0; i < byteLength; i++) {
			inputAsChar[i] = input.remove(i);
		}
		
		return charArrayToByte(inputAsChar);
	}
	
	public static int[] byteArrayToIntArray(byte[] input) {
		int[] output = new int[input.length];
		for(int i = 0; i < input.length; i ++) {
			output[i] = Byte.toUnsignedInt(input[i]);
		}
		
		return output;
	}
	
	public static List<Byte> byteArrayToList(byte[] input) {
		List<Byte> output = new ArrayList<Byte>();
		for(int i = 0; i < input.length; i++) {
			output.add(input[i]);
		}
		return output;
	}
	
	public static char[] charListToArray(List<Character> input) {
		char[] output = new char[input.size()];
		for(int i = 0; i < input.size(); i++) {
			output[i] = input.get(i);			
		}
		return output;
	}
	
	public static List<Byte> charListToByteList(List<Character> input) throws ImageHandlingException {
		List<Byte> output = new ArrayList<Byte>();
		int outputByteCount = input.size() / 8;
		if(input.size() % 8 != 0) {
			outputByteCount++;
		}
		for(int i = 0; i < outputByteCount; i++) {			
			char[] currentByteAsChar = new char[8]; 
			for(int j = 0; j < 8; j++) {
				if(input.size() > 0) {
					currentByteAsChar[j] = input.remove(0);	
				} else {
					// Bit stream has finished. Fill up with padding bits.
					currentByteAsChar[j] = '0';
				}				
			}			
			output.add(Util.charArrayToByte(currentByteAsChar));
		}
		
		return output;
	}
	
	/**
	 * To calculate the greatest common divider of two numbers.
	 * 
	 * @param number1
	 * @param number2
	 * @return the gcd of the two numbers.
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
	 * @return the lcd of two numbers.
	 */
	public static int lcm(int number1, int number2) {
		if (number1 == 0 || number2 == 0)
			return 0;
		else {
			int gcd = gcd(number1, number2);
			return Math.abs(number1 * number2) / gcd;
		}
	}

	public static void main(String[] args) throws ImageHandlingException {
		byte[] input = { 0x3 };
		char[] inChars = byteArrayToCharArray(input);
		System.out.println(inChars);
		System.out.println(charArrayToByte(inChars));
		/*
		 * char[] first = {'1', '1'}; char[] second= {'0','0', '0', '0', '0', '0'};
		 * System.out.println(charArrayToByte(mergeCharArrays(first, second)));
		 */
	}
}
