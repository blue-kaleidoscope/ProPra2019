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
	private boolean autoCompressionWasSet;

	public ConversionController(
			Format inputFormat,
			Format outputFormat,
			ConverterOperationMode operationMode,
			String inputPath,
			String outputPath,
			CompressionFormat outputCompressionFormat,
			String encodingAlphabet) throws ImageHandlingException {
		
		this.operationMode = operationMode;
		if (encodingAlphabet == null && operationMode == ConverterOperationMode.CODE_BASEN) {
			throw new ImageHandlingException("Encoding alphabet necessary in order to perform base-n encoding.",
					ErrorCodes.INVALID_USER_INPUT);
		}
		this.inputFormat = inputFormat;
		this.outputFormat = outputFormat;
		this.outputCompressionFormat = outputCompressionFormat;
		autoCompressionWasSet = (outputCompressionFormat == CompressionFormat.AUTO);		
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

	/**
	 * To start the conversion process from this <code>ConversionController</code>.
	 * @throws ImageHandlingException when an error occured during the conversion process.
	 */
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

	/**
	 * To create the necessary base-32 or base-n encoder/decoder and call the
	 * correct methods.
	 * @throws ImageHandlingException when the base encoding/decoding could not be performed.
	 */
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

		// Perform the necessary encoding/decoding including flushing the encoder/decoder.
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

	/**
	 * To handle the conversion from all allowed input to output formats including the
	 * input and output compression format.
	 * @throws ImageHandlingException when an error occured during the conversion process.
	 */
	private void handleConversion() throws ImageHandlingException {

		if (inputFormat == Format.TGA) {
			inputImage = new ImageTGA(inputHandler);
		} else if (inputFormat == Format.PROPRA) {
			inputImage = new ImagePropra(inputHandler);
		} else {
			throw new ImageHandlingException("Unknown input format.", ErrorCodes.UNEXPECTED_ERROR);
		}

		inputCompressionFormat = inputImage.getCompressionMode();		

		rgbEncoder = new RGBEncoder();
		rleEncoder = new RLEEncoder(inputImage.getWidth());
		huffmanEncoder = new HuffmanEncoder();
		rleDecoder = new RLEDecoder(inputImage.getUncompressedImageDataLength());
		huffmanDecoder = new HuffmanDecoder(inputImage.getUncompressedImageDataLength());

		boolean autoModeActive = (outputCompressionFormat == CompressionFormat.AUTO);

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
			throw new ImageHandlingException("Unknown output format.", ErrorCodes.UNEXPECTED_ERROR);
		}
		
		outputImage.setDimensions(inputImage);
		outputHandler.writeData(outputImage.getHeader());
		
		performConversion(true);
		if (outputCompressionFormat == CompressionFormat.HUFFMAN && !autoCompressionWasSet) {
			// Ugly way to reset the Huffman decoder ... :-(
			// This is necessary if the inputImage and the outputImage are both Huffman compressed
			huffmanDecoder = null;
			huffmanDecoder = new HuffmanDecoder(inputImage.getUncompressedImageDataLength());
			performConversion(false);
		}

		outputImage.finalizeConversion();
	}

	private void performConversion(boolean firstPass) throws ImageHandlingException {
		byte[] imageData;		

		if (!firstPass) {
			// Huffman tree was created. The input data must be read again to perform the
			// encoding.
			// This only is necessary when Huffman encoding should be performed and only
			// if 'huffman' was set as an input argument.
			// In case 'auto' was set the resetting of the inputHandler already happened
			// in the simulateConversion() method.
			inputHandler.reset();
			inputHandler.skipNBytes(inputImage.getHeader().length);
		}
		
		if(autoCompressionWasSet && outputCompressionFormat == CompressionFormat.HUFFMAN) {
			// In case 'auto' compression was set and Huffman encoding is the best compression scenario
			// the preparation of the Huffman encoder (i.e. creating the tree) can be avoided
			// because it was already done in simulateConversion() below.
			firstPass = false;
		}

		while (((imageData = inputHandler.readData()) != null)) {			
			// First the data must be decompressed if it was compressed
			if (inputCompressionFormat == CompressionFormat.RLE) {
				imageData = rleDecoder.decode(imageData);
			} else if (inputCompressionFormat == CompressionFormat.HUFFMAN) {
				imageData = huffmanDecoder.decode(imageData);
			}

			// Now the pixel order will be changed if propra>tga or tga>propra conversion is
			// desired by the user
			if (inputFormat != outputFormat) {
				imageData = rgbEncoder.encode(imageData);
			}

			// Finally the data gets encoded if desired by the user
			if (outputCompressionFormat == CompressionFormat.RLE) {
				imageData = rleEncoder.encode(imageData);
			} else if (outputCompressionFormat == CompressionFormat.HUFFMAN) {
				if (firstPass) {
					huffmanEncoder.prepareEncoding(imageData);
					// This "only" creates the Huffman tree. No encoding is done here.
					// The encoding of the data will take place the next time this method will be
					// called with "false" as argument.
				} else {
					imageData = huffmanEncoder.encode(imageData);
					// This method was now called with "false", the Huffman tree was created in the first pass
					// and now the image data gets encoded.
				}
			}

			if (outputCompressionFormat != CompressionFormat.HUFFMAN) {
				// If Huffman compression is NOT the desired output format then the data can 
				// already be written in the first pass.
				outputHandler.writeData(imageData);
			} else {
				if (!firstPass) {
					// The data can only be written in the 2nd pass when the Huffman tree was created.
					outputHandler.writeData(imageData);
				}
			}
		}
		
		// All encoders and/or decoders should be flushed now to either check whether
		// there is still some data to be written (i.e. HuffmanEncoder) or whether
		// the input file was corrupt and there is missing data (i.e. RLEEncoder).
		if(inputFormat != outputFormat) {
			rgbEncoder.flush();
		}
		
		if(outputCompressionFormat == CompressionFormat.RLE) {
			rleEncoder.flush();
		}		
		
		if (outputCompressionFormat == CompressionFormat.HUFFMAN && !firstPass) {
			// Writing the very last bytes when the file should be encoded using Huffman encoding
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
				
				if(inputFormat != outputFormat) {
					// Change the pixel order if converting from tga>propra or propra>tga
					outputDataUncompressed = rgbEncoder.encode(outputDataUncompressed);
				}

				if(huffmanPreparationStep) {
					// Calculate the file size for an uncompressed or an RLE compressed image
					outputDataRLE = rleEncoder.encode(outputDataUncompressed);					
					uncompressedLength += outputDataUncompressed.length;
					rleCompressedLength += outputDataRLE.length;
					// Create the Huffman tree which will later be re-used in case Huffman compression
					// is the one which produces the smallest files
					huffmanEncoder.prepareEncoding(outputDataUncompressed);				
				} else {
					// Calculate the file size for a Huffman compressed image
					outputDataHuffman = huffmanEncoder.encode(outputDataUncompressed);
					huffmanCompressedLength += outputDataHuffman.length;
				}			
			}
			if(!huffmanPreparationStep) {
				// Retrieve the last bytes for a Huffman compressed image
				outputDataHuffman = huffmanEncoder.flush();
				huffmanCompressedLength += outputDataHuffman.length;	
			}			
			huffmanPreparationStep = false;
			// Once more the ugly way to reset the Huffman decoder ;-)
			huffmanDecoder = null;
			huffmanDecoder = new HuffmanDecoder(inputImage.getUncompressedImageDataLength());
		}
		// Now the file sizes of all three compression algorithms are known and the smallest one gets choosen
		if((uncompressedLength < rleCompressedLength) & (uncompressedLength < huffmanCompressedLength)) {
			outputCompressionFormat = CompressionFormat.UNCOMPRESSED;
		} else if ((rleCompressedLength < uncompressedLength) & (rleCompressedLength < huffmanCompressedLength)) {
			outputCompressionFormat = CompressionFormat.RLE;
		} else if ((huffmanCompressedLength < uncompressedLength) & (huffmanCompressedLength < rleCompressedLength)) {
			outputCompressionFormat = CompressionFormat.HUFFMAN;
		} else {
			// In the unlikely case all strategies create the same file size the strategy which needs
			// less computing time will be used --> uncompressed.
			outputCompressionFormat = CompressionFormat.UNCOMPRESSED;
		}
		// Now everything gets resetted and the inputHandler gets pointed to the beginning of the
		// image data segment again
		inputHandler.reset();
		inputHandler.skipNBytes(inputImage.getHeader().length);
		rleEncoder.reset();
		rgbEncoder.reset();
		huffmanEncoder.reset();		
	}
}
