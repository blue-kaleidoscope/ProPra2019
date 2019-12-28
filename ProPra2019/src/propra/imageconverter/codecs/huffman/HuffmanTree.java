package propra.imageconverter.codecs.huffman;

public class HuffmanTree {

	public enum NODE_TYPE {
		INNER_NODE, LEAF
	}

	private Element root;
	private Element currentElement;

	public class Element {
		private Element leftChild;
		private Element rightChild;
		private NODE_TYPE bitCode;
		private byte data;

		private Element(byte data) {
			this.leftChild = null;
			this.rightChild = null;
			this.bitCode = NODE_TYPE.LEAF;
			this.data = data;

		}

		private Element() {
			this.leftChild = null;
			this.rightChild = null;
			this.bitCode = NODE_TYPE.INNER_NODE;
		}

		@Override
		public String toString() {
			return ("Type: " + bitCode + " Data: " + data);
		}

		public NODE_TYPE getType() {
			return bitCode;
		}

		public byte getData() {
			return data;
		}
	}

	public HuffmanTree() {
		root = null;
		currentElement = root;
	}

	public Element getRoot() {
		return root;
	}

	public boolean addNode() {
		if (root == null) {
			root = new Element();
			return true;
		} else {
			return addElement(root, null, new Element());
		}
	}

	public boolean addLeaf(byte data) {
		return addElement(root, null, new Element(data));
	}

	private boolean addElement(Element root, Element previousElem, Element newElem) {
		boolean wasAdded = false;
		if (root != null) {
			if (root.bitCode == NODE_TYPE.INNER_NODE) {
				wasAdded = addElement(root.leftChild, root, newElem);
				if (!wasAdded) {
					wasAdded = addElement(root.rightChild, root, newElem);
				}
			}
		} else {
			wasAdded = true;
			if (previousElem.leftChild == null) {
				previousElem.leftChild = newElem;
			} else {
				previousElem.rightChild = newElem;
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
	public Element traverse(boolean direction) {
		if (currentElement == null) {
			/*
			 * This method was called for the very first time. Therefor, the traversing
			 * starts from the root. Now the traversing starts from the root again.
			 */
			currentElement = root;
		}

		if (currentElement.bitCode == NODE_TYPE.LEAF) {
			/*
			 * The last time this method was called a leaf was given to the caller. Now the
			 * traversing starts from the root again.
			 */
			currentElement = root;
		}

		if (direction) {
			currentElement = currentElement.rightChild;
		} else {
			currentElement = currentElement.leftChild;
		}

		return currentElement;
	}
}
