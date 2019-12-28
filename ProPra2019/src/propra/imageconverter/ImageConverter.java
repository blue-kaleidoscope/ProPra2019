package propra.imageconverter;

import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.util.arguments.ArgumentHandler;

/**
 * Converts images from a given input file path to a desired output file path.
 * 
 * @author Oliver Eckstein
 *
 */
public class ImageConverter {

	public static void main(String[] args) {
		System.out.println(" +++ ImageConverter started +++");
		try {
			ArgumentHandler argHandler = new ArgumentHandler(args);
			ConversionController conversionController = new ConversionController(
					argHandler.getConverterOperationMode(),
					argHandler.getInputPath(),
					argHandler.getOutputPath(),
					argHandler.getCompressionOperationMode(),
					argHandler.getEncodingAlphabet());
			conversionController.convert();
		} catch (ImageHandlingException e1) {
			System.err.println(e1);
			System.exit(123);
		}

		System.out.println(" +++ Shutting down ImageConverter +++");

	}
}