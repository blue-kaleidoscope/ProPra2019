package propra.imageconverter;

import java.io.File;
/**
 * This class controls the conversion process using an input and an output image.
 * @author Oliver Eckstein
 *
 */
public class ConversionController {
	private Image inputImage;
	private Image outputImage;
	
	/**
	 * Creates a new <code>ImageController</code>.
	 * @param inputPath the path to the original image.
	 * @param outputPath the path for the target image.
	 * @throws ImageHandlingException An exception is thrown when the conversion cannot be performed.
	 */
	public ConversionController(File inputFile, File outputFile, String compression) throws ImageHandlingException {
		
		String inputExtension = ConverterHelper.getFileExtension(inputFile.getPath());
		String outputExtension = ConverterHelper.getFileExtension(outputFile.getPath());
		
		if(!(compression.equals("rle") || compression.equals("uncompressed"))) {
			throw new ImageHandlingException("Unknown compression type: " + compression, 
					ErrorCodes.INVALID_COMPRESSION);
		}
		
		// Let's check whether the desired conversion is valid
		if(inputExtension.equals("tga")) {
			inputImage = new ImageTGA(inputFile);
		} else if (inputExtension.equals("propra")) {
			inputImage = new ImagePropra(inputFile);
		} else {
			throw new ImageHandlingException("Unknown input file format: " + inputExtension, 
					ErrorCodes.INVALID_FILEFORMAT);
		}
		
		int compressionMode;
		if (compression.equals("rle")) {
			compressionMode = Image.RLE;
		} else {
			compressionMode = Image.UNCOMPRESSED;
		}
		 
		
		if(outputExtension.equals("tga")) {
			outputImage = new ImageTGA(outputFile, compressionMode);			
		} else if (outputExtension.equals("propra")) {
			outputImage = new ImagePropra(outputFile, compressionMode);			
		} else {
			throw new ImageHandlingException("Unknown output file format: " + outputExtension, 
					ErrorCodes.INVALID_FILEFORMAT);
		}
		outputImage.setDimensions(inputImage);
	}
	
	/**
	 * To start the conversion process from the input image of this controller to the output image.
	 * @throws ImageHandlingException An exception is thrown when the conversion cannot be performed.
	 */
	public void convert() throws ImageHandlingException {		
		
		if(inputImage.getCompressionMode() == Image.UNCOMPRESSED && outputImage.getCompressionMode() == Image.RLE) {
			ConverterHelper.convertAndCompress(inputImage, outputImage, true);
		}
		
		if(inputImage.getCompressionMode() == Image.RLE && outputImage.getCompressionMode() == Image.RLE) {
			if(inputImage.getExtension().equals(outputImage.getExtension())) {
				ConverterHelper.copyImage(inputImage, outputImage);
			} else {
				ConverterHelper.handleCompressedImages(inputImage, outputImage, true, false);
			}
		}
		
		if(inputImage.getCompressionMode() == Image.RLE && outputImage.getCompressionMode() == Image.UNCOMPRESSED) {			
			ConverterHelper.handleCompressedImages(inputImage, outputImage, 
					!inputImage.getExtension().equals(outputImage.getExtension()), true);
		}
		
		if(inputImage.getCompressionMode() == Image.UNCOMPRESSED && outputImage.getCompressionMode() == Image.UNCOMPRESSED) {
			if(inputImage.getExtension().equals(outputImage.getExtension())) {
				ConverterHelper.copyImage(inputImage, outputImage);
			} else {
				ConverterHelper.convertAndCompress(inputImage, outputImage, false);
			}
		}	
		
		outputImage.finalizeConversion();
		System.out.println("Input image successfully converted.");
		System.out.println(inputImage.getPath() + " --> " + outputImage.getPath());
	}
	
	
}