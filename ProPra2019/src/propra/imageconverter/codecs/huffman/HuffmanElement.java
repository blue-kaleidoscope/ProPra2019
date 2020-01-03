package propra.imageconverter.codecs.huffman;

import java.util.ArrayList;
import java.util.List;

import propra.imageconverter.codecs.huffman.HuffmanTree.NodeType;

/**
 * A <code>HuffmanElement</code> is either an inner node or a leaf of a Huffman
 * encoding tree. If it is a leaf it holds the data which can be used for
 * encoding/decoding data using the Huffman algorithm.
 * 
 * @author Oliver Eckstein
 *
 */
public class HuffmanElement implements Comparable<HuffmanElement> {
	private HuffmanElement leftChild;
	private HuffmanElement rightChild;
	private NodeType nodeType;

	/**
	 * The leaf data which can be used for encoding/decoding data using the Huffman
	 * algorithm.
	 */
	private Byte data;

	/**
	 * The frequency of the data if this <code>HuffmanElement</code> is a leaf.
	 */
	private int frequency;

	/**
	 * The traversal code of this <code>HuffmanElement</code> which can be used to
	 * create an encoding table once a Huffman tree which holds this
	 * <code>HuffmanElement</code> was built.
	 */
	private String code;

	/**
	 * To create a new empty <code>HuffmanElement</code> which will be set as an
	 * inner node.
	 */
	public HuffmanElement() {
		leftChild = null;
		rightChild = null;
		this.nodeType = NodeType.INNER_NODE;
		code = null;
		data = null;
	}

	/**
	 * To create a new <code>HuffmanElement</code> which will be set as a leaf.
	 * 
	 * @param data this leaf's data which can be used for encoding/decoding data
	 *             using the Huffman algorithm.
	 */
	public HuffmanElement(byte data) {
		this();
		this.nodeType = NodeType.LEAF;
		this.data = data;
	}

	/**
	 * To create a new empty <code>HuffmanElement</code> which will be set as an
	 * inner node.
	 * 
	 * @param frequency this inner node's frequency.
	 */
	public HuffmanElement(int frequency) {
		this();
		this.frequency = frequency;
	}

	/**
	 * To create a new <code>HuffmanElement</code> which will be set as a leaf.
	 * 
	 * @param frequency this leaf's frequency.
	 * @param data      this leaf's data which can be used for encoding/decoding
	 *                  data using the Huffman algorithm.
	 */
	public HuffmanElement(int frequency, byte data) {
		this(data);
		this.frequency = frequency;
	}

	@Override
	public String toString() {
		return ("Type: " + nodeType + " Data: " + data);
	}

	public NodeType getType() {
		return nodeType;
	}

	public byte getData() {
		return data;
	}

	public void setData(byte data) {
		this.data = data;
	}

	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}

	public int getFrequency() {
		return frequency;
	}

	public HuffmanElement getLeftChild() {
		return leftChild;
	}

	public void setLeftChild(HuffmanElement element) {
		leftChild = element;
	}

	public HuffmanElement getRightChild() {
		return rightChild;
	}

	public void setRightChild(HuffmanElement element) {
		rightChild = element;
	}

	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * To get the traversal code of this <code>HuffmanElement</code>.
	 * 
	 * @return the traversal code.
	 */
	public List<Character> getCode() {
		List<Character> output = new ArrayList<Character>();
		for (int i = 0; i < code.length(); i++) {
			output.add((code.charAt(i) == '0' ? '0' : '1'));
		}
		return output;
	}

	@Override
	public int compareTo(HuffmanElement o) {
		return this.frequency - o.frequency;
	}
}
