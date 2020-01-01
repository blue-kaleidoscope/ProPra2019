package propra.imageconverter.codecs.huffman;

import java.util.ArrayList;
import java.util.List;

import propra.imageconverter.codecs.huffman.HuffmanTree.NODE_TYPE;

public class HuffmanElement implements Comparable<HuffmanElement> {
	private HuffmanElement leftChild;
	private HuffmanElement rightChild;
	private NODE_TYPE nodeType;
	private byte data;
	private int frequency;
	private String code;

	public HuffmanElement() {
		leftChild = null;
		rightChild = null;
		this.nodeType = NODE_TYPE.INNER_NODE;
		code = null;
	}
	
	public HuffmanElement(byte data) {
		this();
		this.nodeType = NODE_TYPE.LEAF;
		this.data = data;
	}	
	
	public HuffmanElement(int frequency) {
		this();
		this.frequency = frequency;
	}
	
	public HuffmanElement(int frequency, byte data) {
		this(data);
		this.frequency = frequency;
	}

	@Override
	public String toString() {
		return ("Type: " + nodeType + " Data: " + data);
	}

	public NODE_TYPE getType() {
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
	
	public List<Character> getCode() {
		List<Character> output = new ArrayList<Character>();
		for(int i = 0; i < code.length(); i++) {
			output.add((code.charAt(i) == '0' ? '0' : '1')); 
		}
		return output;
	}

	@Override
	public int compareTo(HuffmanElement o) {
		return this.frequency - o.frequency;
	}
}
