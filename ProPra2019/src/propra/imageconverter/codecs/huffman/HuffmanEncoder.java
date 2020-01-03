package propra.imageconverter.codecs.huffman;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import propra.imageconverter.codecs.Encoder;
import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.util.Util;

/**
 * A <code>HuffmanEncoder</code> encodes data using the Huffman encoding
 * algorithm bytewise.
 * 
 * @author Oliver Eckstein
 *
 */
public class HuffmanEncoder extends Encoder {

	/**
	 * The frequency of each image's byte.
	 */
	private Map<Byte, Integer> byteFrequency;

	/**
	 * A minimum heap which is used for creating the Huffman tree.
	 */
	private PriorityQueue<HuffmanElement> minHeap;

	private HuffmanTree tree;

	/**
	 * Holds a char representation of the current byte to be encoded.
	 */
	private List<Character> currentByteAsChar;

	/**
	 * Creates a new <code>HuffmanEncoder</code>.
	 */
	public HuffmanEncoder() {
		super();
		encodingState = EncodingState.PREPARING;
		byteFrequency = new HashMap<Byte, Integer>();
		minHeap = new PriorityQueue<HuffmanElement>();
		tree = new HuffmanTree();
		currentByteAsChar = new ArrayList<Character>();
	}

	@Override
	public void prepareEncoding(byte[] inputData) {
		if (encodingState == EncodingState.PREPARING) {
			calculateFrequencies(inputData);
		}
	}

	@Override
	public byte[] encode(byte[] inputData) throws ImageHandlingException {
		encodedData.clear();
		if (encodingState == EncodingState.PREPARING) {
			finalizePreparation();
		}

		if (encodingState == EncodingState.WRITING_HEADER_DATA) {
			List<Character> treeCode = tree.getPreOrderTreeCode();
			for (Character currentChar : treeCode) {
				// Convert the tree code into a byte representation and adds it to the encoded
				// data list
				bufferEncodedData(currentChar);
			}
			encodingState = EncodingState.ENCODING;
		}

		if (encodingState == EncodingState.ENCODING) {
			// The encoder is ready for encoding data
			HashMap<Byte, String> codeTable = tree.getCodeTable();
			// Encode byte by byte of the input data using the code table of the Huffman
			// tree
			for (Byte currentByte : inputData) {
				char[] currentCode = codeTable.get(currentByte).toCharArray();
				for (Character currentChar : currentCode) {
					bufferEncodedData(currentChar);
				}
			}
		}
		return Util.byteListToArray(encodedData);
	}

	@Override
	public byte[] flush() throws ImageHandlingException {
		encodedData.clear();
		if (currentByteAsChar.size() > 0) {
			// Fill padding zero bits
			int listSize = currentByteAsChar.size();
			for (int i = 0; i < 8 - listSize; i++) {
				currentByteAsChar.add('0');
			}
			encodedData.add(Util.charListToByte(currentByteAsChar));
			return Util.byteListToArray(encodedData);
		}
		return null;
	}

	/**
	 * Finalizes the preparation steps so that this <code>HuffmanEncoder</code> is
	 * ready for encoding data.
	 * 
	 * @throws ImageHandlingException when an error occurred during finalizing the
	 *                                preparation.
	 */
	private void finalizePreparation() throws ImageHandlingException {
		populateMinHeap();
		createTree();
		encodingState = EncodingState.WRITING_HEADER_DATA;
	}

	/**
	 * Calculates the frequencies of the bytes to be encoded.
	 * 
	 * @param inputData the bytes to be encoded.
	 */
	private void calculateFrequencies(byte[] inputData) {
		for (Byte currentByte : inputData) {
			if (byteFrequency.containsKey(currentByte)) {
				// Byte already exists, increase the frequency by one
				byteFrequency.put(currentByte, byteFrequency.get(currentByte) + 1);
			} else {
				// A new byte was spotted
				byteFrequency.put(currentByte, 1);
			}
		}
	}

	private void populateMinHeap() {
		for (Map.Entry<Byte, Integer> currentEntry : byteFrequency.entrySet()) {
			minHeap.add(new HuffmanElement(currentEntry.getValue(), currentEntry.getKey()));
		}
		// minHeap now contains all nodes which are technically each one Huffman tree
		// consisting of one element
	}

	private void createTree() throws ImageHandlingException {

		while (minHeap.size() > 1) {
			// When minHeap has the size of 1 it only contains one element which is the root
			// of the Huffman tree which can be used for encoding.

			// Get the first two entries and remove them from the minimum heap
			HuffmanElement node1 = minHeap.poll();
			HuffmanElement node2 = minHeap.poll();

			// Create a new node which references the two nodes
			HuffmanElement rootOfNode1AndNode2 = new HuffmanElement(node1.getFrequency() + node2.getFrequency());
			rootOfNode1AndNode2.setLeftChild(node1);
			rootOfNode1AndNode2.setRightChild(node2);

			tree.SetRoot(rootOfNode1AndNode2);
			// Add the newly created node to the minimum heap again
			// The heap now consists of one element less than at the beginning of the
			// while-loop
			minHeap.add(rootOfNode1AndNode2);
		}
		// Huffman tree was created. The traversal code table can now be created.
		tree.createCodeTable();
	}

	/**
	 * This method receives a char, buffers it and when it has received 8 chars they
	 * will get converted into a byte and added to the encoded data of this
	 * <code>Encoder</code>.
	 * 
	 * @param currentChar the char to be added to the encoded data
	 * @throws ImageHandlingException
	 */
	private void bufferEncodedData(char currentChar) throws ImageHandlingException {
		if (currentByteAsChar.size() < 8) {
			currentByteAsChar.add(currentChar);
		} else {
			encodedData.add(Util.charListToByte(currentByteAsChar));
			currentByteAsChar.clear();
			currentByteAsChar.add(currentChar);
		}
	}

	/**
	 * Resets this <code>HuffmanEncoder</code> in that way that a so far created
	 * Huffman tree is kept and data which refers to that tree can be encoded again.
	 * If a new Huffman tree should get created, a new <code>HuffmanEncoder</code>
	 * must be used.
	 */
	@Override
	public void reset() {
		encodedData.clear();
		encodingState = EncodingState.WRITING_HEADER_DATA;
		currentByteAsChar.clear();
	}
}
