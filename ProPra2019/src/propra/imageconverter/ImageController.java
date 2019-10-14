package propra.imageconverter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
	public ImageController(String inputPath, String outputPath) throws ImageHandlingException {
		
		String inputExtension = ImageHelper.getFileExtension(inputPath);
		String outputExtension = ImageHelper.getFileExtension(outputPath);
		
		// Let's check whether the desired conversion is valid
		if(inputExtension.equals("tga")) {
			inputImage = new ImageTGA(inputPath);
		} else if (inputExtension.equals("propra")) {
			inputImage = new ImagePropra(inputPath);
		} else {
			throw new ImageHandlingException("Unknown input file format: " + inputExtension, 
					ErrorCodes.INVALID_FILEFORMAT);
		}
		
		if(outputExtension.equals("tga")) {
			outputImage = new ImageTGA();			
		} else if (outputExtension.equals("propra")) {
			outputImage = new ImagePropra();			
		} else {
			throw new ImageHandlingException("Unknown output file format: " + outputExtension, 
					ErrorCodes.INVALID_FILEFORMAT);
		}
		outputImage.setPath(outputPath);
	}
	
	/**
	 * To start the conversion process from the input image of this controller to the output image.
	 * @throws ImageHandlingException An exception is thrown when the conversion cannot be performed.
	 */
	public void convert() throws ImageHandlingException {
		String inputExtension = inputImage.getExtension();
		String outputExtension = outputImage.getExtension();
		byte[] inputDatasegment = inputImage.getDataSegment();
		byte[] outputDatasegment;
		
		if (!inputExtension.equals(outputExtension)) {
			// Either tga>propra or propra>tga
			outputDatasegment = new byte[inputDatasegment.length];
			
			
			// Change the order of the pixels of input image.
			// propra: GBR --> tga: BGR			 
			for (int i = 0; i < inputDatasegment.length; i = i + 3) {
				outputDatasegment[i] = inputDatasegment[i + 1];
				outputDatasegment[i + 1] = inputDatasegment[i];
				outputDatasegment[i + 2] = inputDatasegment[i + 2];
			}
		} else {
			// tga>tga or propra>propra
			outputDatasegment = inputDatasegment;
		}
		outputImage.setImage(inputImage.getWidth(), inputImage.getHeight(), outputDatasegment);
		
		/*
		 * Create output file and write into the file.
		 */
		File outputFile = new File(outputImage.getPath());
		String errorMessage;
		try (FileOutputStream stream = new FileOutputStream(outputFile.getPath())) {
			stream.write(outputImage.getData());
		} catch (FileNotFoundException e) {
			errorMessage = "Error writing output file. Invalid path or user does not have enough rights.";
			throw new ImageHandlingException(errorMessage, ErrorCodes.IO_ERROR);
		} catch (IOException e) {
			errorMessage = "Error writing output file. Invalid path or user does not have enough rights.";
			throw new ImageHandlingException(errorMessage, ErrorCodes.IO_ERROR);
		}		
		System.out.println("Input image successfully converted.");
		System.out.println(inputImage.getPath() + " --> " + outputImage.getPath());
	}
	
	
}
