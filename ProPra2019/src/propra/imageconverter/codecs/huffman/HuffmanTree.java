package propra.imageconverter.codecs.huffman;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import propra.imageconverter.error.ImageConverterErrorCode;
import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.util.Util;

/**
 * A <code>HuffmanTree</code> represents a binary tree of nodes which can be
 * used for encoding/decoding data using the Huffman algorithm.
 * 
 * @author Oliver Eckstein
 *
 */
public class HuffmanTree {

	/**
	 * This <code>HuffmanTree</code>'s elements can either be an inner node or a
	 * leaf.
	 * 
	 * @author Oliver Eckstein
	 *
	 */
	public enum NodeType {
		INNER_NODE, LEAF
	}

	private HuffmanElement root;
	private HuffmanElement currentElement;

	/**
	 * Holds the traversal codes for this <code>HuffmanTree</code>'s encoded data.
	 */
	private HashMap<Byte, String> codeTable;

	/**
	 * Holds a binary description of this <code>HuffmanTree</code> stored in a list
	 * of characters which contains a series of '0' or '1' characters.
	 */
	private List<Character> preOrderTreeCode;

	/**
	 * To create a new <code>HuffmanTree</code>.
	 */
	public HuffmanTree() {
		root = null;
		currentElement = null;
		codeTable = null;
		preOrderTreeCode = null;
	}

	public HuffmanElement getRoot() {
		return root;
	}

	/**
	 * To get this <code>HuffmanTree</code>'s binary description stored in a list of
	 * characters which contains a series of '0' or '1' characters.
	 * 
	 * @return the tree's binary representation.
	 */
	public List<Character> getPreOrderTreeCode() {
		return preOrderTreeCode;
	}

	/**
	 * To get this <code>HuffmanTree</code>'s traversal codes for the encoded data.
	 * 
	 * @return the traversal codes.
	 */
	public HashMap<Byte, String> getCodeTable() {
		return codeTable;
	}

	public void SetRoot(HuffmanElement newRoot) {
		this.root = newRoot;
	}

	/**
	 * Adds a new empty inner node to this <code>HuffmanTree</code>.
	 * 
	 * @return <code>true</code> if the inner node was added, <code>false</code>
	 *         otherwise which means this <code>HuffmanTree</code> is satured with
	 *         leafs so that no new inner node can be added.
	 */
	public boolean addNode() {
		if (root == null) {
			root = new HuffmanElement();
			return true;
		} else {
			return addElement(root, null, new HuffmanElement());
		}
	}

	/**
	 * Creates the traversal code table of this <code>HuffmanTree</code>. This
	 * method should not be called before the to be encoded data was forwarded to
	 * this tree which means this <code>HuffmanTree</code> must be finished before a
	 * valid traversal code table can be created.
	 * 
	 * @throws ImageHandlingException when this <code>HuffmanTree</code> was not
	 *                                created yet.
	 */
	public void createCodeTable() throws ImageHandlingException {
		if (root != null) {
			codeTable = new HashMap<Byte, String>();
			preOrderTreeCode = new ArrayList<Character>();
			preOrderTreeCode.add('0'); // Adding the root explicitely to the Huffman tree code
			// And now traversing the tree in pre-order
			determineTraversalCode(root.getLeftChild(), "0");
			determineTraversalCode(root.getRightChild(), "1");
		} else {
			throw new ImageHandlingException("Huffman-Tree not built yet. Cannot create code table.",
					ImageConverterErrorCode.COMPRESSION_ERROR);
		}
	}

	/**
	 * To determine the traversal code for an element and store it into the
	 * traversal code table. Additionally add the respective bit code of this
	 * element to the binary representation of this <code>HuffmanTree</code>.
	 * 
	 * @param currentElement the element for which the traversal code should be
	 *                       determined.
	 * @param code           the traversal code of the element.
	 */
	private void determineTraversalCode(HuffmanElement currentElement, String code) {
		currentElement.setCode(code);
		if (currentElement.getType() == NodeType.INNER_NODE) {
			preOrderTreeCode.add('0');
			determineTraversalCode(currentElement.getLeftChild(), code + '0');
			determineTraversalCode(currentElement.getRightChild(), code + '1');
		} else {
			codeTable.put(currentElement.getData(), code);
			preOrderTreeCode.add('1');
			preOrderTreeCode.addAll(Util.byteToCharList(currentElement.getData()));
		}
	}

	/**
	 * Adds a leaf to this <code>HuffmanTree</code>.
	 * 
	 * @param data the leaf's data.
	 * @return <code>true</code> when the leaf could be added, <code>false</code>
	 *         otherwise.
	 */
	public boolean addLeaf(byte data) {
		return addElement(root, null, new HuffmanElement(data));
	}

	/**
	 * To add an element to this <code>HuffmanTree</code>.
	 * 
	 * @param root       this <code>HuffmanTree</code>'s root.
	 * @param parentElem the parent element of the new element.
	 * @param newElem    the new element to be added to this
	 *                   <code>HuffmanTree</code>.
	 * @return <code>true</code> when the element could be added, <code>false</code>
	 *         otherwise.
	 */
	private boolean addElement(HuffmanElement root, HuffmanElement parentElem, HuffmanElement newElem) {
		boolean wasAdded = false;
		if (root != null) {
			if (root.getType() == NodeType.INNER_NODE) {
				// Recursively traverse the tree until a "free spot" below an inner node is
				// available.
				wasAdded = addElement(root.getLeftChild(), root, newElem);
				if (!wasAdded) {
					wasAdded = addElement(root.getRightChild(), root, newElem);
				}
			}
		} else {
			wasAdded = true;
			if (parentElem.getLeftChild() == null) {
				parentElem.setLeftChild(newElem);
			} else {
				parentElem.setRightChild(newElem);
			}
		}
		return wasAdded;
	}

	/**
	 * Traverses this <code>HuffmanTree</code> step-by-step.
	 * 
	 * @param direction <code>false</code> when traversing one step right.
	 *                  <code>true</code> when traversing one step left.
	 * @return the <code>Element</code> after traversing.
	 */
	public HuffmanElement traverse(boolean direction) {
		if (currentElement == null) {
			/*
			 * This method was called for the very first time. Therefore, the traversing
			 * starts from the root.
			 */
			currentElement = root;
		}

		if (currentElement.getType() == NodeType.LEAF) {
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
