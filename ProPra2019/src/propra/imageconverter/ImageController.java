package propra.imageconverter;

import java.io.File;
/**
 * This class controls the conversion process using an input and an output image.
 * @author Oliver Eckstein
 *
 */
public class ImageController {
	private Image inputImage;
	private Image outputImage;
	
	/**
	 * Creates a new <code>ImageController</code>.
	 * @param inputPath the path to the original image.
	 * @param outputPath the path for the target image.
	 * @throws ImageHandlingException An exception is thrown when the conversion cannot be performed.
	 */
	public ImageController(File inputFile, File outputFile) throws ImageHandlingException {
		
		String inputExtension = ImageHelper.getFileExtension(inputFile.getPath());
		String outputExtension = ImageHelper.getFileExtension(outputFile.getPath());
		
		// Let's check whether the desired conversion is valid
		if(inputExtension.equals("tga")) {
			inputImage = new ImageTGA(inputFile, Image.INPUT_IMAGE);
		} else if (inputExtension.equals("propra")) {
			inputImage = new ImagePropra(inputFile, Image.INPUT_IMAGE);
		} else {
			throw new ImageHandlingException("Unknown input file format: " + inputExtension, 
					ErrorCodes.INVALID_FILEFORMAT);
		}
		
		if(outputExtension.equals("tga")) {
			outputImage = new ImageTGA(outputFile, Image.OUTPUT_IMAGE);			
		} else if (outputExtension.equals("propra")) {
			outputImage = new ImagePropra(outputFile, Image.OUTPUT_IMAGE);			
		} else {
			throw new ImageHandlingException("Unknown output file format: " + outputExtension, 
					ErrorCodes.INVALID_FILEFORMAT);
		}
		outputImage.prepareConversion(inputImage);
	}
	
	/**
	 * To start the conversion process from the input image of this controller to the output image.
	 * @throws ImageHandlingException An exception is thrown when the conversion cannot be performed.
	 */
	public void convert() throws ImageHandlingException {
		if (!inputImage.getExtension().equals(outputImage.getExtension())) {
			// Either tga>propra or propra>tga
			ImageHelper.convert(inputImage, outputImage, false);
		} else {
			// tga>tga or propra>propra TODO
			//outputDatasegment = inputDatasegment;
		}
		System.out.println("Input image successfully converted.");
		System.out.println(inputImage.getPath() + " --> " + outputImage.getPath());
	}
	
	
}
