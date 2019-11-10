package propra.imageconverter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This class describes an image which can be handled by the ImageConverter.
 * @author Oliver Eckstein
 *
 */
public abstract class Image {
	protected int headerLength;
	protected int[] header;
	protected int width;
	protected int height;
	protected byte compressionType;	
	protected byte bitsPerPixel;	
	protected String fileExtension;
	protected File file;
	protected boolean imageType;
	
	public static final boolean INPUT_IMAGE = true;
	public static final boolean OUTPUT_IMAGE = false;
	
	protected int headerHeight;
	protected int headerWidth;
	protected int headerCompression;
	protected int headerBitsPerPixel;
	
	/**
	 * Creates a new <code>Image</code> for an existing image file according to the <code>filePath</code>.
	 * Do not call this constructor for not yet existing image files (such as output image files before a
	 * conversion took place).
	 * @param filePath the path to an existing image file.
	 * @throws ImageHandlingException an exception is thrown when this <code>Image</code> could not be created out of
	 * the file.
	 */
	public Image (File file, boolean imageType) throws ImageHandlingException {
		if (file == null) {			
			throw new ImageHandlingException("Invalid file given for input or output file.", 
					ErrorCodes.IO_ERROR);			
		}			
		setProperties();
		this.file = file;
		
		if (imageType == INPUT_IMAGE) {
			this.header = ImageHelper.getHeaderFromFile(file, headerLength);
			this.imageType = INPUT_IMAGE;
			checkHeader();		
			System.out.println("Input image header information successfully checked: " + file.getPath());	
		} else {
			this.imageType = OUTPUT_IMAGE;
			createHeader();	
			System.out.println("Output image successfully created: " + file.getPath());
		}
		
	}
	
	/**
	 * To get the header of this <code>Image</code>.
	 * @return the header information.
	 */
	public int[] getHeader() {		
		return header;
	}
	
	public String getPath() {
		return this.file.getPath();
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
	
	public File getFile() {
		return this.file;
	}
	
	public int getHeaderLength() {
		return this.headerLength;
	}
	
	public long getDataSegmentLength() {
		return (this.file.length() - headerLength);
	}
	
	public void prepareConversion(Image inputImage) throws ImageHandlingException {
		if (imageType != OUTPUT_IMAGE) {
			throw new ImageHandlingException(
					"Method should not be called for input images. Only output images possible.", ErrorCodes.IO_ERROR);
		}
		width = inputImage.getWidth();
		height = inputImage.getHeight();
		
		header[headerWidth] = (byte) width;
		header[headerWidth + 1] = (byte) (width >> 8);
		
		header[headerHeight] = (byte) height;
		header[headerHeight + 1] = (byte) (height >> 8);
	}
	
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
	protected void createHeader() {
		header = new int[headerLength];
		header[headerCompression] = compressionType;
		header[headerBitsPerPixel] = bitsPerPixel;
	}
	
}
