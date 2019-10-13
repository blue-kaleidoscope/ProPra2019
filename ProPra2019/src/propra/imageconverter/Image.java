package propra.imageconverter;

public abstract class Image {
	protected int headerLength;	
	protected byte[] imageData;
	protected int width;
	protected int height;
	protected byte compressionType;	
	protected byte bitsPerPixel;
	protected String filePath;
	protected String fileExtension;
	
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
	
	public Image() {
		setProperties();
		createHeader();
	}
	
	public void setImage(int width, int height, byte[] dataSegment) {
		this.width = width;
		this.height = height;
		setWidthInHeader();
		setHeightInHeader();
		byte[] tmp = new byte[headerLength + dataSegment.length];
		System.arraycopy(imageData, 0, tmp, 0, headerLength);
		System.arraycopy(dataSegment, 0, tmp, headerLength, dataSegment.length);
		imageData = tmp;
	}
	
	public byte[] getData() {		
		return imageData;
	}
	
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
	
	protected abstract void setWidthInHeader();
	protected abstract void setHeightInHeader();
	protected abstract void setProperties();
	protected abstract void checkHeader() throws ImageHandlingException;	
	protected abstract void createHeader();	
}
