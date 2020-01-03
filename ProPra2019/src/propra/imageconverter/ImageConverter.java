package propra.imageconverter;

import propra.imageconverter.conversioncontroller.BaseConversionController;
import propra.imageconverter.conversioncontroller.ConversionController;
import propra.imageconverter.conversioncontroller.FormatConversionController;
import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.util.arguments.ImageConverterArgumentHandler;

/**
 * Converts images or base encodes/base decodes files.
 * Supported image formats: *.tga, *.propra
 * Supported compression algorithms: Uncompressed, run-length-encoding (rle), Huffman compression
 * Supported base-codecs: Base-2, Base-4, Base-8, Base-16, Base-32, Base-64 with a custom selectable encoding alphabet.
 * 
 * @author Oliver Eckstein
 *
 */
public class ImageConverter {

	public static void main(String[] args) {
		System.out.println(" +++ ImageConverter started +++");
		try {
			ImageConverterArgumentHandler argHandler = new ImageConverterArgumentHandler(args);
			ConversionController conversionController;
			if(argHandler.getConverterOperationMode().operationIsBaseCoding()) {
				conversionController = new BaseConversionController(
						argHandler.getConverterOperationMode(),
						argHandler.getInputPath(),
						argHandler.getOutputPath(),
						argHandler.getEncodingAlphabet());
			} else {
				conversionController = new FormatConversionController(
						argHandler.getInputFormat(),
						argHandler.getOutputFormat(),
						argHandler.getConverterOperationMode(),
						argHandler.getInputPath(),
						argHandler.getOutputPath(),
						argHandler.getOutputCompressionFormat());
			}			
			conversionController.convert();
		} catch (ImageHandlingException e1) {
			System.err.println(e1);
			System.exit(123);
		}

		System.out.println(" +++ Shutting down ImageConverter +++");

	}
}