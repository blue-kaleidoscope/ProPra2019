package propra.imageconverter.codecs.huffman;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import propra.imageconverter.error.ErrorCodes;
import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.util.Util;

public class HuffmanTree {

	public enum NODE_TYPE {
		INNER_NODE, LEAF
	}

	private HuffmanElement root;
	private HuffmanElement currentElement;
	private HashMap<Byte, String> codeTable;
	private List<Character> preOrderTreeCode;

	public HuffmanTree() {
		root = null;
		currentElement = null;
		codeTable = null;
		preOrderTreeCode = null;
	}

	public HuffmanElement getRoot() {
		return root;
	}
	
	public List<Character> getPreOrderTreeCode() {
		return preOrderTreeCode;
	}
	
	public HashMap<Byte, String> getCodeTable() {
		return codeTable;
	}

	public void SetRoot(HuffmanElement newRoot) {
		this.root = newRoot;
	}

	public boolean addNode() {
		if (root == null) {
			root = new HuffmanElement();
			return true;
		} else {
			return addElement(root, null, new HuffmanElement());
		}
	}

	public void createCodeTable() throws ImageHandlingException {
		if (root != null) {
			codeTable = new HashMap<Byte, String>();
			preOrderTreeCode = new ArrayList<Character>();
			preOrderTreeCode.add('0'); // Adding the root explicitely to the Huffman tree code
			getCode(root.getLeftChild(), "0");
			getCode(root.getRightChild(), "1");
		} else {
			throw new ImageHandlingException("Huffman-Tree not built yet. Cannot create code table.",
					ErrorCodes.INVALID_COMPRESSION);
		}
	}

	private void getCode(HuffmanElement currentElement, String code) {
		currentElement.setCode(code);
		if(currentElement.getType() == NODE_TYPE.INNER_NODE) {
			preOrderTreeCode.add('0');
			getCode(currentElement.getLeftChild(), code + '0');
			getCode(currentElement.getRightChild(), code + '1');
		} else {			
			codeTable.put(currentElement.getData(), code);
			preOrderTreeCode.add('1');
			preOrderTreeCode.addAll(Util.byteToCharList(currentElement.getData()));
		}
	}

	public boolean addLeaf(byte data) {
		return addElement(root, null, new HuffmanElement(data));
	}

	private boolean addElement(HuffmanElement root, HuffmanElement previousElem, HuffmanElement newElem) {
		boolean wasAdded = false;
		if (root != null) {
			if (root.getType() == NODE_TYPE.INNER_NODE) {
				wasAdded = addElement(root.getLeftChild(), root, newElem);
				if (!wasAdded) {
					wasAdded = addElement(root.getRightChild(), root, newElem);
				}
			}
		} else {
			wasAdded = true;
			if (previousElem.getLeftChild() == null) {
				previousElem.setLeftChild(newElem);
			} else {
				previousElem.setRightChild(newElem);
			}
		}
		return wasAdded;
	}

	/**
	 * 
	 * @param direction <code>false</code> when traversing right. <code>true</code>
	 *                  when traversing left.
	 * @return the <code>Element</code> after traversing.
	 */
	public HuffmanElement traverse(boolean direction) {
		if (currentElement == null) {
			/*
			 * This method was called for the very first time. Therefor, the traversing
			 * starts from the root. Now the traversing starts from the root again.
			 */
			currentElement = root;
		}

		if (currentElement.getType() == NODE_TYPE.LEAF) {
			/*
			 * The last time this method was called a leaf was given to the caller. Now the
			 * traversing starts from the root again.
			 */
			currentElement = root;
		}

		if (direction) {
			currentElement = currentElement.getRightChild();
		} else {
			currentElement = currentElement.getLeftChild();
		}

		return currentElement;
	}
}
