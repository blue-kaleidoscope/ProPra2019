package propra.imageconverter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageController {
	private Image inputImage;
	private Image outputImage;
	
	public ImageController(String inputPath, String outputPath) throws ImageHandlingException {
		
		String inputExtension = ImageHelper.getFileExtension(inputPath);
		String outputExtension = ImageHelper.getFileExtension(outputPath);
		
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
	
	public void convert() throws ImageHandlingException {
		String inputExtension = inputImage.getExtension();
		String outputExtension = outputImage.getExtension();
		byte[] inputDatasegment = inputImage.getDataSegment();
		byte[] outputDatasegment;
		
		if (!inputExtension.equals(outputExtension)) {			
			outputDatasegment = new byte[inputDatasegment.length];
			
			for (int i = 0; i < inputDatasegment.length; i = i + 3) {
				outputDatasegment[i] = inputDatasegment[i + 1];
				outputDatasegment[i + 1] = inputDatasegment[i];
				outputDatasegment[i + 2] = inputDatasegment[i + 2];
			}
		} else {
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
