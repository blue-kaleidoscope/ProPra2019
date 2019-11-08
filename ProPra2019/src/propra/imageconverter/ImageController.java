package propra.imageconverter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
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
	private final int BUFFERSIZE = 9 * 1024;
	
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
		BufferedInputStream buffI = null;
		FileOutputStream oStream = null;
		int bytesRead = 0;
		
		try {
			buffI = new BufferedInputStream(
					new FileInputStream(inputImage.getFile()), BUFFERSIZE);
			oStream = new FileOutputStream(outputImage.getFile());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();				
		}
		
		
		if (!inputImage.getExtension().equals(outputImage.getExtension())) {
			// Either tga>propra or propra>tga			
			int[] intHeader = outputImage.getHeader();
			byte[] byteHeader = new byte[intHeader.length];
			for (int i = 0; i < byteHeader.length; i++) {
				byteHeader[i] = (byte) intHeader[i];
			}
			try {
				oStream.write(byteHeader);
				byte[] inputDatasegment = new byte[BUFFERSIZE];				
				buffI.skip(inputImage.getHeaderLength());				
				while((bytesRead = buffI.read(inputDatasegment)) != -1) {					
					// Change the order of the pixels of input image.
					// propra: GBR --> tga: BGR					
					byte[] outputDatasegment = new byte[bytesRead];
					for (int i = 0; i < bytesRead; i = i + 3) {
						outputDatasegment[i] = inputDatasegment[i + 1];
						outputDatasegment[i + 1] = inputDatasegment[i];
						outputDatasegment[i + 2] = inputDatasegment[i +2];
					}
					oStream.write(outputDatasegment);
				}
				buffI.close();
				oStream.close();
			} catch (IOException e) {
				throw new ImageHandlingException("Error writing output file.", ErrorCodes.IO_ERROR);
			}			
		} else {
			// tga>tga or propra>propra
			//outputDatasegment = inputDatasegment;
		}
		System.out.println("Input image successfully converted.");
		System.out.println(inputImage.getPath() + " --> " + outputImage.getPath());
	}
	
	
}
