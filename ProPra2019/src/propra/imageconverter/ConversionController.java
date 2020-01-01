package propra.imageconverter;

import propra.imageconverter.codecs.Decoder;
import propra.imageconverter.codecs.Encoder;
import propra.imageconverter.codecs.RGBEncoder;
import propra.imageconverter.codecs.base.BaseDecoder;
import propra.imageconverter.codecs.base.BaseEncoder;
import propra.imageconverter.codecs.huffman.HuffmanDecoder;
import propra.imageconverter.codecs.huffman.HuffmanEncoder;
import propra.imageconverter.codecs.rle.RLEDecoder;
import propra.imageconverter.codecs.rle.RLEEncoder;
import propra.imageconverter.error.ErrorCodes;
import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.image.Image;
import propra.imageconverter.image.ImagePropra;
import propra.imageconverter.image.ImageTGA;
import propra.imageconverter.util.FileHandler;
import propra.imageconverter.util.arguments.CompressionFormat;
import propra.imageconverter.util.arguments.ConverterOperationMode;
import propra.imageconverter.util.arguments.Format;

public class ConversionController {

	private FileHandler inputHandler;
	private FileHandler outputHandler;

	private ConverterOperationMode operationMode;
	private Format inputFormat;
	private Format outputFormat;
	private CompressionFormat inputCompressionFormat;
	private CompressionFormat outputCompressionFormat;
	private String encodingAlphabet;

	private Image inputImage;
	private Image outputImage;

	private Encoder rgbEncoder;
	private Encoder rleEncoder;
	private Encoder huffmanEncoder;
	private Decoder rleDecoder;
	private Decoder huffmanDecoder;

	private long uncompressedLength;
	private long rleCompressedLength;
	private long huffmanCompressedLength;

	public ConversionController(Format inputFormat, Format outputFormat, ConverterOperationMode operationMode,
			String inputPath, String outputPath, CompressionFormat outputCompressionFormat, String encodingAlphabet)
			throws ImageHandlingException {

		this.operationMode = operationMode;

		if (encodingAlphabet == null && operationMode == ConverterOperationMode.CODE_BASEN) {
			throw new ImageHandlingException("Encoding alphabet necessary in order to perform base-n encoding.",
					ErrorCodes.INVALID_USER_INPUT);
		}

		this.inputFormat = inputFormat;
		this.outputFormat = outputFormat;
		this.outputCompressionFormat = outputCompressionFormat;
		this.encodingAlphabet = encodingAlphabet;
		initHandlers(inputPath, outputPath);

		uncompressedLength = 0;
		rleCompressedLength = 0;
		huffmanCompressedLength = 0;
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

		if (operationMode == ConverterOperationMode.CONVERT) {
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
			encodingAlphabet = inputHandler.readFirstLine();
			baseDecoder = new BaseDecoder(encodingAlphabet);
		}

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
	}

	private void handleConversion() throws ImageHandlingException {

		if (inputFormat == Format.TGA) {
			inputImage = new ImageTGA(inputHandler);
		} else if (inputFormat == Format.PROPRA) {
			inputImage = new ImagePropra(inputHandler);
		} else {
			throw new ImageHandlingException("Unknown input format.", ErrorCodes.INVALID_ARGUMENT);
		}

		inputCompressionFormat = inputImage.getCompressionMode();		

		rgbEncoder = new RGBEncoder();
		rleEncoder = new RLEEncoder(inputImage.getWidth());
		huffmanEncoder = new HuffmanEncoder();
		rleDecoder = new RLEDecoder(inputImage.getUncompressedImageDataLength());
		huffmanDecoder = new HuffmanDecoder(inputImage.getUncompressedImageDataLength());

		boolean autoModeActive = outputCompressionFormat == CompressionFormat.AUTO;

		if (autoModeActive) {
			// First it must be found out which compression setting is the best one and then
			// the according mode will be chosen and executed.
			simulateConversion();
		}
		
		if (outputFormat == Format.TGA) {
			outputImage = new ImageTGA(outputHandler, outputCompressionFormat);
		} else if (outputFormat == Format.PROPRA) {
			outputImage = new ImagePropra(outputHandler, outputCompressionFormat);
		} else {
			throw new ImageHandlingException("Unknown output format.", ErrorCodes.INVALID_ARGUMENT);
		}
		
		outputImage.setDimensions(inputImage);
		outputHandler.writeData(outputImage.getHeader());
		
		performConversion(true);
		if (outputCompressionFormat == CompressionFormat.HUFFMAN) {
			performConversion(false);
		}

		outputImage.finalizeConversion();
	}

	private void performConversion(boolean huffmanPreparationStep) throws ImageHandlingException {
		byte[] inputData;
		byte[] outputData;

		if (!huffmanPreparationStep) {
			// Huffman tree was created. The input data must be read again to perform the
			// encoding.
			inputHandler.reset();
			inputHandler.skipNBytes(inputImage.getHeader().length);
		}

		while (((inputData = inputHandler.readData()) != null)) {
			outputData = inputData;
			// First the data must be decompressed if it was compressed
			if (inputCompressionFormat == CompressionFormat.RLE) {
				outputData = rleDecoder.decode(outputData);
			} else if (inputCompressionFormat == CompressionFormat.HUFFMAN) {
				outputData = huffmanDecoder.decode(outputData);
			}

			// Now the pixel order will be changed if propra>tga or tga>propra conversion is
			// desired by the user
			if (inputFormat != outputFormat) {
				outputData = rgbEncoder.encode(outputData);
			}

			// Finally the data gets encoded if desired by the user
			if (outputCompressionFormat == CompressionFormat.RLE) {
				outputData = rleEncoder.encode(outputData);
			} else if (outputCompressionFormat == CompressionFormat.HUFFMAN) {
				if (huffmanPreparationStep) {
					huffmanEncoder.prepareEncoding(outputData);
					// When the while loop finishes only the huffman tree was created
					// The encoding of the data will take place the next time this method will be
					// called with "false" as argument
				} else {
					outputData = huffmanEncoder.encode(outputData);
				}
			}

			if (outputCompressionFormat != CompressionFormat.HUFFMAN) {
				outputHandler.writeData(outputData);
			} else {
				if (!huffmanPreparationStep) {
					// Only write after the huffman tree was built
					outputHandler.writeData(outputData);
				}
			}
		}
		if (outputCompressionFormat == CompressionFormat.HUFFMAN && !huffmanPreparationStep) {
			outputHandler.writeData(huffmanEncoder.flush());
		}
	}
	
	private void simulateConversion() throws ImageHandlingException {		
		byte[] inputData;
		byte[] outputDataUncompressed;
		byte[] outputDataRLE;
		byte[] outputDataHuffman;
		boolean huffmanPreparationStep = true;
		for(int i = 0; i < 2; i++) {
			if (!huffmanPreparationStep) {
				// Huffman tree was created. The input data must be read again to perform the
				// encoding.
				inputHandler.reset();
				inputHandler.skipNBytes(inputImage.getHeader().length);
			}

			while (((inputData = inputHandler.readData()) != null)) {			
				// First the data must be decompressed if it was compressed
				if (inputCompressionFormat == CompressionFormat.RLE) {
					outputDataUncompressed = rleDecoder.decode(inputData);
				} else if (inputCompressionFormat == CompressionFormat.HUFFMAN) {
					outputDataUncompressed = huffmanDecoder.decode(inputData);
				} else {
					outputDataUncompressed = inputData;
				}

				outputDataRLE = rleEncoder.encode(inputData);
				
				uncompressedLength += outputDataUncompressed.length;
				rleCompressedLength += outputDataRLE.length;
				
				if(huffmanPreparationStep) {
					huffmanEncoder.prepareEncoding(inputData);
				} else {
					outputDataHuffman = huffmanEncoder.encode(inputData);
					huffmanCompressedLength += outputDataHuffman.length;
				}			
			}
			if(!huffmanPreparationStep) {
				outputDataHuffman = huffmanEncoder.flush();
				huffmanCompressedLength += outputDataHuffman.length;	
			}			
			huffmanPreparationStep = false;
		}
		if((uncompressedLength < rleCompressedLength) & (uncompressedLength < huffmanCompressedLength)) {
			outputCompressionFormat = CompressionFormat.UNCOMPRESSED;
		} else if ((rleCompressedLength < uncompressedLength) & (rleCompressedLength < huffmanCompressedLength)) {
			outputCompressionFormat = CompressionFormat.RLE;
		} else {
			outputCompressionFormat = CompressionFormat.HUFFMAN;
		}
		inputHandler.reset();
		inputHandler.skipNBytes(inputImage.getHeader().length);
	}
}
