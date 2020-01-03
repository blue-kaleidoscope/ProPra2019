package propra.imageconverter.codecs.huffman;

import propra.imageconverter.codecs.Decoder;
import propra.imageconverter.error.ImageConverterErrorCode;
import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.util.Util;

/**
 * A <code>HuffmanDecoder</code> decodes data bytewise using the Huffman algorithm.
 * 
 * @author Oliver Eckstein
 *
 */
public class HuffmanDecoder extends Decoder {

	/**
	 * A binary tree which is used to decompress data encoded via the Huffman
	 * compression algorithm.
	 */
	private HuffmanTree tree;

	/**
	 * In case during creating the tree or during decoding the data, more bits were
	 * necessary than were given in the last decode()-call. This
	 * <code>char[]</code>-array contains the bits which will be used in the next
	 * decode()-call.
	 */
	private char[] remainingBits;

	/**
	 * The final number of bits which describe the Huffman tree.
	 */
	private int globalTreeLength;

	/**
	 * An index which selects the current bit from the data to be decoded.
	 */
	private int globalBitSelector;

	/**
	 * Creates a new <code>HuffmanDecoder</code>.
	 * 
	 * @param maxCountBytesToDecode the maximum number of bytes this
	 *                              <code>HuffmanDecoder</code> should be decoding.
	 */
	public HuffmanDecoder(long maxCountBytesToDecode) {
		super(maxCountBytesToDecode);
		tree = new HuffmanTree();
		remainingBits = null;
		globalBitSelector = 0;
		globalTreeLength = 0;
	}

	/**
	 * @return the decoded data which was encoded using Huffman compression.<br>
	 *         Returns <code>null</code> when decoder did not start decoding data
	 *         yet but is still setting up the Huffman tree structure.
	 */
	@Override
	public byte[] decode(byte[] inputData) throws ImageHandlingException {
		int inputDataLengthInBit = inputData.length * 8;
		globalBitSelector = 0;
		globalTreeLength = 0;
		decodedData.clear();
		while (globalBitSelector < inputDataLengthInBit) {
			if (decodingState == DecodingState.WAITING_FOR_HEADER_DATA) {
				try {
					globalTreeLength = buildTree(inputData);
					globalBitSelector = globalTreeLength;
				} catch (NullPointerException npe) {
					throw new ImageHandlingException("Invalid Huffman tree data given!", ImageConverterErrorCode.INVALID_HEADERDATA);
				}
			} else if (decodingState == DecodingState.WAITING_FOR_DECODING_DATA) {
				tree.createCodeTable();
				int bytesToSkip = globalTreeLength / 8; // These bytes of inputData were used to build up the tree. They
														// must be skipped.
				int bitsToSkip = globalTreeLength % 8;  // These bits were the last ones in inputData to build up the
														// tree. They must also be skipped.
				char[] imageDataInBits;
				if (bytesToSkip != 0 && bitsToSkip != 0) {
					byte[] imageData = new byte[inputData.length - bytesToSkip];
					System.arraycopy(inputData, bytesToSkip, imageData, 0, imageData.length);
					imageDataInBits = Util.byteArrayToCharArray(imageData);
				} else {
					imageDataInBits = Util.byteArrayToCharArray(inputData);
				}

				/*
				 * imageDataInBits contains the last bits to build up the tree and all bits
				 * which reference the image data. The next loop starts not at position '0' but
				 * at bitsToSkip otherwise the last bits of the tree data would be used for the
				 * decoding of image data.
				 */
				for (int i = bitsToSkip; i < imageDataInBits.length; i++) {
					// imageDataInBits is now used to reconstruct the image pixel data
					decodeBitForDecompression(imageDataInBits[i] == '1');
				}
				globalBitSelector = inputDataLengthInBit; // All remaining bits from inputData have been "used" which
															// were not used to build the tree.
			}
		}

		if (decodedData.size() > 0) {
			return Util.byteListToArray(decodedData);
		} else {
			return null;
		}
	}

	/**
	 * Reconstructs the Huffman tree out of the given bit code.
	 * @param inputData a bit representation of the Huffman tree
	 * @return the number of bits used in inputData to build the tree
	 * @throws ImageHandlingException when the given bit code does not represent a valid Huffman tree.
	 */
	private int buildTree(byte[] inputData) throws ImageHandlingException {
		int bitSelector = 0;

		/*
		 * The bit representation of the given inputData.
		 */
		char[] bitRepresentation = Util.byteArrayToCharArray(inputData);
		int numberOfNextBits = 1;
		byte nextByte = 0x0;

		if (remainingBits != null) {
			// In the last decode()-call not all necessary bits to form a byte (leaf data)
			// were in 'inputData'.
			// These now get retrieved and combined with the remaining bits from the last
			// decode()-call.
			char[] newBits = new char[8 - remainingBits.length];
			try {
				System.arraycopy(Util.byteToCharArray(inputData[0]), 0, newBits, 0, 8 - remainingBits.length);
			} catch (IndexOutOfBoundsException e) {
				throw new ImageHandlingException("Invalid Huffman tree data given!", ImageConverterErrorCode.INVALID_HEADERDATA);
			}
			nextByte = Util.charArrayToByte(Util.mergeCharArrays(remainingBits, newBits));
			decodeBitsForTreeBuild(nextByte, true);
			bitSelector += 8 - remainingBits.length;
			remainingBits = null;
			// Now the usual decoding continues
		}

		while (numberOfNextBits > 0 && bitSelector < bitRepresentation.length) {
			if (numberOfNextBits == 1) {
				// Get the next bit of the BitSet. 1: Leaf, 0: Inner node.
				if (bitRepresentation[bitSelector] == '1') {
					nextByte = 0x1;
				} else {
					nextByte = 0x0;
				}
				numberOfNextBits = decodeBitsForTreeBuild(nextByte, false);
				bitSelector++;
			} else {
				// A leaf will be added with its data
				if (bitSelector + 8 <= bitRepresentation.length) {
					// The leaf will be added now
					nextByte = Util.getNextByte(bitRepresentation, bitSelector);
					numberOfNextBits = decodeBitsForTreeBuild(nextByte, true);
					if (numberOfNextBits > 0) {
						bitSelector += 8;
					}
				} else {
					// Not enough data was passed to add a new leaf.
					// The next time this method will be called, the leaf will finally be added.
					remainingBits = new char[bitRepresentation.length - bitSelector];
					System.arraycopy(bitRepresentation, bitSelector, remainingBits, 0,
							bitRepresentation.length - bitSelector);
					bitSelector += remainingBits.length;
					return bitSelector;
				}
			}
		}
		// bitSelector must be reduced by one because the current bit
		// bitSelector is pointing to is already part of the data to be decoded!
		return bitSelector - 1;
	}

	/**
	 * Decodes the current bit of the bit representation of a Huffman tree.
	 * It communicates to the caller whether in the next step decoding step a leaf
	 * including its data will be added to this <code>HuffmanDecoder</code>'s Huffman tree
	 * or whether a new inner node was added.
	 * @param treeData the next bit to be decoded to build this <code>HuffmanDecoder</code>'s Huffman tree.
	 * @param addLeaf <code>true</code> when <code>treeData</code> is a new leaf's data, <code>false</code> otherwise.
	 * @return the number of bits the decoder needs the next time this method gets called. It returns <code>0</code> when
	 * this <code>HuffmanDecoder</code>'s Huffman tree was finalized.
	 */
	private int decodeBitsForTreeBuild(byte treeData, boolean addLeaf) {
		boolean wasAdded = false;
		if (!addLeaf) {
			if (treeData == 0) {
				// Add a new inner node
				wasAdded = tree.addNode();
			} else if (treeData == 1) {
				// Ask for next 8 bits which represent the leaf's data.
				return 8;
			}
		} else {
			wasAdded = tree.addLeaf(treeData);
		}

		if (wasAdded) {
			// Ask for next 1 bit.
			return 1;
		} else {
			// No new element could be added to the tree.
			// Tree build is finished. No more bits needed.
			decodingState = DecodingState.WAITING_FOR_DECODING_DATA;
			return 0;
		}
	}

	/**
	 * Traverses this <code>HuffmanDecoder</code>'s Huffman tree based on the given bit code
	 * to decompress Huffman algorithm encoded data.
	 * This method adds the decompressed data to this <code>HuffmanDecoder</code>'s list of
	 * decoded data.
	 * @param bitCode the bit code to decompress the data.
	 */
	private void decodeBitForDecompression(boolean bitCode) {
		HuffmanElement nextElement = tree.traverse(bitCode);
		if (nextElement.getType() == HuffmanTree.NodeType.LEAF && alreadyDecodedBytes < maxCountBytesToDecode) {
			alreadyDecodedBytes++;
			decodedData.add(nextElement.getData());
		}
	}

	@Override
	public byte[] flush() throws ImageHandlingException {
		// Nothing to do here
		return null;
	}
}
