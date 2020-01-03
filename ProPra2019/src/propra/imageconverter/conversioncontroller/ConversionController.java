package propra.imageconverter.conversioncontroller;

import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.util.FileHandler;
import propra.imageconverter.util.arguments.ConverterOperationMode;

/**
 * A <code>ConversionController</code> performs all the necessary operations in
 * order to convert a file or an image into a desired output format.
 * 
 * @author Oliver Eckstein
 *
 */
public abstract class ConversionController {

	protected FileHandler inputHandler;
	protected FileHandler outputHandler;

	/**
	 * Determines what this <code>ConversionController</code> should be doing.
	 */
	protected ConverterOperationMode operationMode;

	/**
	 * To create a new <code>ConversionController</code> and initiate the file
	 * handlers.
	 * 
	 * @param operationMode the <code>ConversionController</code>'s operation mode.
	 * @param inputPath     the path of the source file.
	 * @param outputPath    the path of the destination file.
	 * @throws ImageHandlingException when an error occurred during initiating the
	 *                                file handlers for reading from the source file
	 *                                or writing into the destination file.
	 */
	public ConversionController(ConverterOperationMode operationMode, String inputPath, String outputPath)
			throws ImageHandlingException {
		this.operationMode = operationMode;
		initHandlers(inputPath, outputPath);
	}

	protected void initHandlers(String inputPath, String outputPath) throws ImageHandlingException {
		inputHandler = new FileHandler(inputPath);
		inputHandler.createFile();
		inputHandler.openInputStream();

		outputHandler = new FileHandler(outputPath);
		outputHandler.createFile();
		outputHandler.openOutputFile();
	}

	/**
	 * To start the conversion process from this <code>ConversionController</code>.
	 * 
	 * @throws ImageHandlingException when an error occurred during the conversion
	 *                                process.
	 */
	public abstract void convert() throws ImageHandlingException;

	/**
	 * Closes the input and output files.
	 * 
	 * @throws ImageHandlingException when an error occurred during closing the
	 *                                input and output files.
	 */
	protected void finalizeConversion() throws ImageHandlingException {
		inputHandler.closeFile();
		outputHandler.closeFile();
	}
}
