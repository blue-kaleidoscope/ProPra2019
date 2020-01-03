package propra.imageconverter.conversioncontroller;

import propra.imageconverter.codecs.Decoder;
import propra.imageconverter.codecs.Encoder;
import propra.imageconverter.codecs.base.BaseDecoder;
import propra.imageconverter.codecs.base.BaseEncoder;
import propra.imageconverter.error.ImageConverterErrorCode;
import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.util.arguments.ConverterOperationMode;

/**
 * A <code>BaseConversionController</code> performs all the necessary operations in
 * order to handle base encoded files or to encode a file using base-n coding.
 * 
 * @author Oliver Eckstein
 *
 */
public class BaseConversionController extends ConversionController {
	
	/**
	 * The encoding/decoding alphabet to encode/decode the input file.
	 */
	private String encodingAlphabet;

	/**
	 * To create a new <code>BaseConversionController</code> and initiate the file
	 * handlers.
	 * @param operationMode the <code>BaseConversionController</code>'s operation mode.
	 * @param inputPath     the path of the source file.
	 * @param outputPath    the path of the destination file.
	 * @param encodingAlphabet the encoding/decoding alphabet.
	 * @throws ImageHandlingException when an invalid encoding/decoding alphabet was given.
	 */
	public BaseConversionController(ConverterOperationMode operationMode, String inputPath, String outputPath, String encodingAlphabet)
			throws ImageHandlingException {
		super(operationMode, inputPath, outputPath);
		if (encodingAlphabet == null && operationMode == ConverterOperationMode.CODE_BASEN) {
			throw new ImageHandlingException("Encoding alphabet necessary in order to perform base-n encoding.",
					ImageConverterErrorCode.INVALID_USER_INPUT);
		}
		this.encodingAlphabet = encodingAlphabet;
	}

	@Override
	public void convert() throws ImageHandlingException {

		byte[] inputData;
		byte[] outputData;
		Encoder baseEncoder = null;
		Decoder baseDecoder = null;

		if (operationMode == ConverterOperationMode.CODE_BASE32) {
			baseEncoder = new BaseEncoder();
		}

		if (operationMode == ConverterOperationMode.CODE_BASEN) {
			baseEncoder = new BaseEncoder(encodingAlphabet);
			outputHandler.writeNewLine(encodingAlphabet);
		}

		if (operationMode == ConverterOperationMode.DECODE_BASE32) {
			baseDecoder = new BaseDecoder();
		}

		if (operationMode == ConverterOperationMode.DECODE_BASEN) {
			encodingAlphabet = inputHandler.readFirstLine();
			baseDecoder = new BaseDecoder(encodingAlphabet);
		}

		// Perform the necessary encoding/decoding including flushing the
		// encoder/decoder.
		if (operationMode.operationIsBaseEncoding()) {
			while ((inputData = inputHandler.readData()) != null) {
				outputData = baseEncoder.encode(inputData);
				outputHandler.writeData(outputData);
			}
			outputData = baseEncoder.flush();
			if (outputData.length > 0) {
				outputHandler.writeData(outputData);
			}
		} else if (operationMode.operationIsBaseDecoding()) {
			while ((inputData = inputHandler.readData()) != null) {
				outputData = baseDecoder.decode(inputData);
				outputHandler.writeData(outputData);
			}
			outputData = baseDecoder.flush();
			if (outputData.length > 0) {
				outputHandler.writeData(outputData);
			}
		}
		super.finalizeConversion();
	}

}
