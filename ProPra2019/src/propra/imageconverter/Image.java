package propra.imageconverter;

/**
 * This class describes an image which can be handled by the ImageConverter.
 * @author Oliver Eckstein
 *
 */
public abstract class Image {
	protected int headerLength;
	protected byte[] imageData; // Header data and image data segment
	protected int width;
	protected int height;
	protected byte compressionType;	
	protected byte bitsPerPixel;
	protected String filePath;
	protected String fileExtension;
	
	/**
	 * Creates a new <code>Image</code> for an existing image file according to the <code>filePath</code>.
	 * Do not call this constructor for not yet existing image files (such as output image files before a
	 * conversion took place).
	 * @param filePath the path to an existing image file.
	 * @throws ImageHandlingException an exception is thrown when this <code>Image</code> could not be created out of
	 * the file.
	 */
	public Image (String filePath) throws ImageHandlingException {
		if (filePath == null || filePath.equals("")) {			
			throw new ImageHandlingException("Invalid file path for input or output file.", 
					ErrorCodes.INVALID_FILEPATH);			
		}
		this.filePath = filePath;		
		this.imageData = ImageHelper.getBytesOfFile(filePath);		
		setProperties();
		checkHeader();		
		System.out.println("Image header information successfully checked: " + filePath);
	}
	
	/**
	 * Creates a new <code>Image</code> for a not yet existing image file.
	 * Use this constructor for not yet existing image files (such as output image files before
	 * a conversion took place).
	 */
	public Image() {
		setProperties();
		createHeader();
	}
	
	/**
	 * Set the image data segment for this <code>Image</code>.
	 * @param width the image's width.
	 * @param height the image's height.
	 * @param dataSegment the image's data segment.
	 */
	public void setImage(int width, int height, byte[] dataSegment) throws ImageHandlingException {
		this.width = width;
		this.height = height;
		setWidthInHeader();
		setHeightInHeader();
		// Copy the old header information into a new array
		// Copy the new image data segment into the new array
		byte[] tmp = new byte[headerLength + dataSegment.length];
		System.arraycopy(imageData, 0, tmp, 0, headerLength);
		System.arraycopy(dataSegment, 0, tmp, headerLength, dataSegment.length);
		imageData = tmp;
	}
	
	/**
	 * To get the data of this <code>Image</code>.
	 * @return the header information and the image data segment.
	 */
	public byte[] getData() {		
		return imageData;
	}
	
	/**
	 * To get the image data segment of this <code>Image</code>.
	 * @return the image data segment.
	 */
	public byte[] getDataSegment() {
		byte[] tmp = new byte[imageData.length - headerLength];
		System.arraycopy(imageData, headerLength, tmp, 
				0, imageData.length - headerLength);
		return tmp;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public String getExtension() {
		return fileExtension;
	}
	
	public String getPath() {
		return filePath;
	}
	
	public void setPath(String filePath) {
		this.filePath = filePath;
	}
	
	/**
	 * To set the width into the header of this <code>Image</code>.
	 * @throws ImageHandlingException when width cannot be set.
	 */
	protected abstract void setWidthInHeader() throws ImageHandlingException;
	
	/**
	 * To set the height into the header of this <code>Image</code>.
	 * @throws ImageHandlingException when height cannot be set.
	 */
	protected abstract void setHeightInHeader() throws ImageHandlingException;
	
	/**
	 * To define properties which are unique for this type of <code>Image</code>.
	 */
	protected abstract void setProperties();
	
	/**
	 * To check whether the header of this <code>Image</code> is valid according to its properties.
	 * @throws ImageHandlingException an exception is thrown when the header information does not fit
	 * to this type of <code>Image</code>.
	 */
	protected abstract void checkHeader() throws ImageHandlingException;	
	
	/**
	 * To create a new header of this <code>Image</code> based on the type of the <code>Image</code>.
	 */
	protected abstract void createHeader();	
}
