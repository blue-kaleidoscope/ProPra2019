package propra.imageconverter;

import propra.imageconverter.codecs.Decoder;
import propra.imageconverter.codecs.Encoder;
import propra.imageconverter.codecs.RGBEncoder;
import propra.imageconverter.codecs.base.BaseDecoder;
import propra.imageconverter.codecs.base.BaseEncoder;
import propra.imageconverter.codecs.huffman.HuffmanDecoder;
import propra.imageconverter.codecs.rle.RLEDecoder;
import propra.imageconverter.codecs.rle.RLEEncoder;
import propra.imageconverter.error.ErrorCodes;
import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.image.Image;
import propra.imageconverter.image.ImagePropra;
import propra.imageconverter.image.ImageTGA;
import propra.imageconverter.image.Image.CompressionType;
import propra.imageconverter.util.FileHandler;
import propra.imageconverter.util.arguments.ConverterCompressionOperation;
import propra.imageconverter.util.arguments.ConverterOperationMode;

public class ConversionController {

	private FileHandler inputHandler;
	private FileHandler outputHandler;

	private ConverterOperationMode operationMode;
	private ConverterCompressionOperation compressionOperation;

	private Image inputImage;
	private Image outputImage;

	private String encodingAlphabet;
	
	public ConversionController(
			ConverterOperationMode operationMode,
			String inputPath,
			String outputPath,
			ConverterCompressionOperation compressionOperation,
			String encodingAlphabet) throws ImageHandlingException {
		
		this.operationMode = operationMode;
		
		
		if(operationMode.operationIsConversion()) {
			if(compressionOperation == null) {
				this.compressionOperation = ConverterCompressionOperation.UNCOMPRESSED_TO_UNCOMPRESSED;
			} else {
				this.compressionOperation = compressionOperation;
			}
		} else if(operationMode.operationIsBaseCoding()) {
			if(encodingAlphabet == null && (operationMode == ConverterOperationMode.CODE_BASEN || 
					operationMode == ConverterOperationMode.DECODE_BASEN)) {
				throw new ImageHandlingException(
						"Encoding alphabet necessary in order to perform base-n coding.",
						ErrorCodes.INVALID_USER_INPUT);
			}
			this.encodingAlphabet = encodingAlphabet;
		}		
		initHandlers(inputPath, outputPath);
	}

	private void initHandlers(String inputPath, String outputPath) throws ImageHandlingException {
		inputHandler = new FileHandler(inputPath);
		inputHandler.createFile();
		inputHandler.openInputFile();
		
		outputHandler = new FileHandler(outputPath);
		outputHandler.createFile();
		outputHandler.openOutputFile();	
		
	}

	public void convert() throws ImageHandlingException {

		if (operationMode.operationIsConversion()) {
			handleConversion();
		}

		if (operationMode.operationIsBaseCoding()) {
			handleBaseCoding();
		}

		inputHandler.closeFile();
		outputHandler.closeFile();
	}

	private void handleBaseCoding() throws ImageHandlingException {
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
			baseDecoder = new BaseDecoder(encodingAlphabet);
			inputHandler.skipFirstLine();
		}
		
		if (operationMode.operationIsBaseEncoding()) {
			while ((inputData = inputHandler.readData()) != null) {
				outputData = baseEncoder.encode(inputData);
				outputHandler.writeData(outputData);
			}	
			outputData = baseEncoder.flush();
			if(outputData.length > 0) {
				outputHandler.writeData(outputData);
			}
		} else if (operationMode.operationIsBaseDecoding()) {			
			while ((inputData = inputHandler.readData()) != null) {
				outputData = baseDecoder.decode(inputData);
				outputHandler.writeData(outputData);
			}
			outputData = baseDecoder.flush();
			if(outputData.length > 0) {
				outputHandler.writeData(outputData);
			}
		}
		
		
	}

	private void handleConversion() throws ImageHandlingException {
		byte[] inputData;
		byte[] outputData;
		boolean inputFormatEqualOutputFormat = false;
		CompressionType compType = compressionOperation.getCompressionType(compressionOperation);

		if (operationMode == ConverterOperationMode.TGA_TO_PROPRA) {
			inputImage = new ImageTGA(inputHandler);
			outputImage = new ImagePropra(outputHandler, compType);
		}

		if (operationMode == ConverterOperationMode.PROPRA_TO_TGA) {
			inputImage = new ImagePropra(inputHandler);
			outputImage = new ImageTGA(outputHandler, compType);
		}

		if (operationMode == ConverterOperationMode.TGA_TO_TGA) {
			inputImage = new ImageTGA(inputHandler);
			outputImage = new ImageTGA(outputHandler, compType);
			inputFormatEqualOutputFormat = true;
		}

		if (operationMode == ConverterOperationMode.PROPRA_TO_PROPRA) {
			inputImage = new ImagePropra(inputHandler);
			outputImage = new ImagePropra(outputHandler, compType);
			inputFormatEqualOutputFormat = true;
		}

		outputImage.setDimensions(inputImage);
		outputHandler.writeData(outputImage.getHeader());

		if (compressionOperation == ConverterCompressionOperation.UNCOMPRESSED_TO_UNCOMPRESSED) {
			Encoder rgbEncoder = new RGBEncoder();
			while (((inputData = inputHandler.readData()) != null)) {
				if (inputFormatEqualOutputFormat) {
					outputData = inputData;
				} else {
					outputData = rgbEncoder.encode(inputData);
				}
				outputHandler.writeData(outputData);
			}
		}

		if (compressionOperation == ConverterCompressionOperation.UNCOMPRESSED_TO_RLE) {
			Encoder rleEncoder = new RLEEncoder(inputImage.getWidth());
			Encoder rgbEncoder = new RGBEncoder();
			while (((inputData = inputHandler.readData()) != null)) {
				if (inputFormatEqualOutputFormat) {
					outputData = rleEncoder.encode(inputData);
				} else {
					outputData = rgbEncoder.encode(inputData);
					outputData = rleEncoder.encode(outputData);
				}
				outputHandler.writeData(outputData);
			}
		}

		if (compressionOperation == ConverterCompressionOperation.RLE_TO_UNCOMPRESSED) {
			Encoder rgbEncoder = new RGBEncoder();
			Decoder rleDecoder = new RLEDecoder();
			while (((inputData = inputHandler.readData()) != null)) {

				if (inputFormatEqualOutputFormat) {
					outputData = rleDecoder.decode(inputData);
				} else {
					outputData = rleDecoder.decode(inputData);
					outputData = rgbEncoder.encode(outputData);
				}
				outputHandler.writeData(outputData);
			}
		}

		if (compressionOperation == ConverterCompressionOperation.RLE_TO_RLE) {
			Encoder rgbEncoder = new RGBEncoder();
			Decoder rleDecoder = new RLEDecoder();
			Encoder rleEncoder = new RLEEncoder(inputImage.getWidth());
			while (((inputData = inputHandler.readData()) != null)) {
				if (inputFormatEqualOutputFormat) {
					outputData = inputData;
				} else {
					outputData = rleDecoder.decode(inputData);
					outputData = rgbEncoder.encode(outputData);
					outputData = rleEncoder.encode(outputData);
				}
				outputHandler.writeData(outputData);
			}
		}

		if (compressionOperation == ConverterCompressionOperation.HUFFMAN_TO_UNCOMPRESSED) {
			Encoder rgbEncoder = new RGBEncoder();
			Decoder huffmanDecoder = new HuffmanDecoder();
			while (((inputData = inputHandler.readData()) != null)) {
				outputData = huffmanDecoder.decode(inputData);
				if (!inputFormatEqualOutputFormat) {
					outputData = rgbEncoder.encode(outputData);
				}
				outputHandler.writeData(outputData);
			}
		}

		if (compressionOperation == ConverterCompressionOperation.HUFFMAN_TO_RLE) {
			Encoder rgbEncoder = new RGBEncoder();
			Decoder huffmanDecoder = new HuffmanDecoder();
			Encoder rleEncoder = new RLEEncoder(inputImage.getWidth());
			while (((inputData = inputHandler.readData()) != null)) {
				outputData = huffmanDecoder.decode(inputData);
				if (!inputFormatEqualOutputFormat) {
					outputData = rgbEncoder.encode(outputData);
				}
				outputData = rleEncoder.encode(outputData);
				outputHandler.writeData(outputData);
			}			
		}
		outputImage.finalizeConversion();
	}

}
