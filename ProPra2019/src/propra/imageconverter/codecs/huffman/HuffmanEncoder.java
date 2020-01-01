package propra.imageconverter.codecs.huffman;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import propra.imageconverter.codecs.Encoder;
import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.util.Util;

public class HuffmanEncoder extends Encoder {

	private Map<Byte, Integer> byteFrequency;	
	private PriorityQueue<HuffmanElement> minHeap;
	private HuffmanTree tree;
	private List<Character> currentByteAsChar;

	public HuffmanEncoder() {
		super();
		encodingState = ENCODING_STATES.PREPARING;
		byteFrequency = new HashMap<Byte, Integer>();
		minHeap = new PriorityQueue<HuffmanElement>();
		tree = new HuffmanTree();
		currentByteAsChar = new ArrayList<Character>();
	}

	@Override
	public void prepareEncoding(byte[] inputData) {
		if(encodingState == ENCODING_STATES.PREPARING) {
			calculateFrequencies(inputData);	
		}		
	}

	@Override
	public byte[] encode(byte[] inputData) throws ImageHandlingException {
		encodedData.clear();
		if (encodingState == ENCODING_STATES.PREPARING) {
			finalizePreparation();
		}
		
		if (encodingState == ENCODING_STATES.WRITING_HEADER_DATA) {
			List<Character> treeCode = tree.getPreOrderTreeCode();
			for(Character currentChar : treeCode) {
				// Convert the tree code into a byte representation and adds it to the encoded data list
				bufferEncodedData(currentChar);
			}
			encodingState = ENCODING_STATES.ENCODING;
		}
		
		if(encodingState == ENCODING_STATES.ENCODING) {				
			HashMap<Byte, String> codeTable = tree.getCodeTable();
			for(Byte currentByte : inputData) {
				char[] currentCode = codeTable.get(currentByte).toCharArray();
				for(Character currentChar : currentCode) {
					bufferEncodedData(currentChar);
				}
			}
		}		
		return Util.byteListToArray(encodedData);
	}

	@Override
	public byte[] flush() throws ImageHandlingException {
		encodedData.clear();
		if(currentByteAsChar.size() > 0) {
			// Fill padding zero bits
			int listSize = currentByteAsChar.size();
			for(int i = 0; i < 8 - listSize; i++) {
				currentByteAsChar.add('0');
			}
			encodedData.add(Util.charListToByte(currentByteAsChar));
			return Util.byteListToArray(encodedData);
		}
		return null;
	}

	private void finalizePreparation() throws ImageHandlingException {
		populateMinHeap();
		createTree();
		encodingState = ENCODING_STATES.WRITING_HEADER_DATA;
	}

	private void calculateFrequencies(byte[] inputData) {
		for (Byte currentByte : inputData) {
			if (byteFrequency.containsKey(currentByte)) {
				byteFrequency.put(currentByte, byteFrequency.get(currentByte) + 1);
			} else {
				byteFrequency.put(currentByte, 1);
			}
		}
	}

	private void populateMinHeap() {
		for (Map.Entry<Byte, Integer> currentEntry : byteFrequency.entrySet()) {
			minHeap.add(new HuffmanElement(currentEntry.getValue(), currentEntry.getKey()));
		}
	}

	private void createTree() throws ImageHandlingException {

		while (minHeap.size() > 1) {
			HuffmanElement node1 = minHeap.poll();
			HuffmanElement node2 = minHeap.poll();

			HuffmanElement rootOfNode1AndNode2 = new HuffmanElement(node1.getFrequency() + node2.getFrequency());
			rootOfNode1AndNode2.setLeftChild(node1);
			rootOfNode1AndNode2.setRightChild(node2);

			tree.SetRoot(rootOfNode1AndNode2);
			minHeap.add(rootOfNode1AndNode2);
		}
		tree.createCodeTable();
	}
	
	/**
	 * This method receives a char, buffers it and when it has received 8 chars
	 * they will get converted into a byte and added to the encoded
	 * data of this <code>Encoder</code>.
	 * @param currentChar the char to be added to the encoded data
	 * @throws ImageHandlingException 
	 */
	private void bufferEncodedData(char currentChar) throws ImageHandlingException {
		if(currentByteAsChar.size() < 8) {
			currentByteAsChar.add(currentChar);
		} else {
			encodedData.add(Util.charListToByte(currentByteAsChar));
			currentByteAsChar.clear();
			currentByteAsChar.add(currentChar);
		}
	}

	/**
	 * Resets this <code>HuffmanEncoder</code> in that way that a so far created Huffman tree
	 * is kept and data which refers to that tree can be encoded again. 
	 * If a new Huffman tree should get created, a new <code>HuffmanEncoder</code>
	 * must be used.
	 */
	@Override
	public void reset() {
		encodedData.clear();
		encodingState = ENCODING_STATES.WRITING_HEADER_DATA;		
		currentByteAsChar.clear();
	}
}
