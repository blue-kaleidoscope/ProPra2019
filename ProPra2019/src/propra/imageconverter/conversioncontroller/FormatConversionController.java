package propra.imageconverter.conversioncontroller;

import propra.imageconverter.codecs.Decoder;
import propra.imageconverter.codecs.Encoder;
import propra.imageconverter.codecs.RGBEncoder;
import propra.imageconverter.codecs.huffman.HuffmanDecoder;
import propra.imageconverter.codecs.huffman.HuffmanEncoder;
import propra.imageconverter.codecs.rle.RLEDecoder;
import propra.imageconverter.codecs.rle.RLEEncoder;
import propra.imageconverter.error.ImageConverterErrorCode;
import propra.imageconverter.error.ImageHandlingException;
import propra.imageconverter.image.Image;
import propra.imageconverter.image.ImagePropra;
import propra.imageconverter.image.ImageTGA;
import propra.imageconverter.util.FileHandler;
import propra.imageconverter.util.arguments.CompressionFormat;
import propra.imageconverter.util.arguments.ConverterOperationMode;
import propra.imageconverter.util.arguments.Format;

/**
 * A <code>FormatConversionController</code> performs all the necessary
 * operations in order to convert images using various conversion and/or
 * compression algorithms.
 * 
 * @author Oliver Eckstein
 *
 */
public class FormatConversionController extends ConversionController {

	/**
	 * <code>FileHandler</code> which stores already decoded data and which can be
	 * re-used so that the data does not need to get decoded again if it is
	 * necessary at a later step.
	 */
	private FileHandler uncompressedInputDataFileHandler;
	private Format inputFormat;
	private Format outputFormat;
	private CompressionFormat inputCompressionFormat;
	private CompressionFormat outputCompressionFormat;
	private Image inputImage;
	private Image outputImage;

	private Encoder rgbEncoder;
	private Encoder rleEncoder;
	private Encoder huffmanEncoder;
	private Decoder rleDecoder;
	private Decoder huffmanDecoder;

	/**
	 * The path of the source file.
	 */
	private String inputPath;

	/**
	 * The length of the output image if it gets stored uncompressed.
	 */
	private long uncompressedLength;

	/**
	 * The length of the output image if it gets stored run-length-encoded.
	 */
	private long rleCompressedLength;

	/**
	 * The length of the output image if it gets stored using Huffman encoding.
	 */
	private long huffmanCompressedLength;

	/**
	 * Indicates whether the 'auto' option was set for compression.
	 */
	private boolean autoCompressionWasSet;

	/**
	 * To create a new <code>FormatConversionController</code> and initiate the file
	 * handlers.
	 * 
	 * @param inputFormat   the input image's format.
	 * @param outputFormat  the output image's format.
	 * @param operationMode the <code>BaseConversionController</code>'s operation
	 *                      mode.
	 * @param inputPath     the path of the source file.
	 * @param outputPath    the path of the destination file.
	 * @throws ImageHandlingException when an error occurred during initiating the
	 *                                file handlers for reading from the source file
	 *                                or writing into the destination file.
	 */
	public FormatConversionController(Format inputFormat, Format outputFormat, ConverterOperationMode operationMode,
			String inputPath, String outputPath, CompressionFormat outputCompressionFormat)
			throws ImageHandlingException {
		super(operationMode, inputPath, outputPath);
		this.inputFormat = inputFormat;
		this.outputFormat = outputFormat;
		this.outputCompressionFormat = outputCompressionFormat;
		autoCompressionWasSet = (outputCompressionFormat == CompressionFormat.AUTO);
		super.initHandlers(inputPath, outputPath);

		uncompressedLength = 0;
		rleCompressedLength = 0;
		huffmanCompressedLength = 0;
	}

	/**
	 * To handle the conversion from all supported input formats to all supported
	 * output formats including the supported input and output compression format.
	 * 
	 * @throws ImageHandlingException when an error occurred during the conversion
	 *                                process.
	 */
	@Override
	public void convert() throws ImageHandlingException {

		// Create the input image
		if (inputFormat == Format.TGA) {
			inputImage = new ImageTGA(inputHandler);
		} else if (inputFormat == Format.PROPRA) {
			inputImage = new ImagePropra(inputHandler);
		} else {
			throw new ImageHandlingException("Unknown input format.", ImageConverterErrorCode.UNEXPECTED_ERROR);
		}

		inputCompressionFormat = inputImage.getCompressionMode();

		// Create the encoders and decoders. Not all of them will necessarily be used.
		rgbEncoder = new RGBEncoder();
		rleEncoder = new RLEEncoder(inputImage.getWidth());
		huffmanEncoder = new HuffmanEncoder();
		rleDecoder = new RLEDecoder(inputImage.getUncompressedImageDataLength());
		huffmanDecoder = new HuffmanDecoder(inputImage.getUncompressedImageDataLength());

		if (autoCompressionWasSet) {
			// First it must be found out which compression setting is the best one and then
			// the according mode will be chosen and executed.
			// After calling this method 'outputCompressionFormat' is changed from AUTO to
			// the
			// best option which produces the smallest output images.
			findBestCompressionMode();
		}

		// Create the output image
		if (outputFormat == Format.TGA) {
			outputImage = new ImageTGA(outputHandler, outputCompressionFormat);
		} else if (outputFormat == Format.PROPRA) {
			outputImage = new ImagePropra(outputHandler, outputCompressionFormat);
		} else {
			throw new ImageHandlingException("Unknown output format.", ImageConverterErrorCode.UNEXPECTED_ERROR);
		}

		// Prepare the output image
		outputImage.setDimensions(inputImage.getWidth(), inputImage.getHeight());
		outputHandler.writeData(outputImage.getHeader());

		// Now the conversion begins...
		executeConversion(true);
		if (outputCompressionFormat == CompressionFormat.HUFFMAN && !autoCompressionWasSet) {
			// Currently a quite ugly way to reset the Huffman decoder ... :-(
			// This is necessary if the inputImage and the outputImage are both Huffman
			// compressed
			huffmanDecoder = null;
			huffmanDecoder = new HuffmanDecoder(inputImage.getUncompressedImageDataLength());
			executeConversion(false);
		}

		// Some housekeeping before the conversion finishes
		outputImage.finalizeConversion();
		this.finalizeConversion();
	}

	/**
	 * To convert the input image to the output image.
	 * 
	 * @param firstPass <code>true</code> when this is the first conversion pass.
	 *                  Sometimes two passes are necessary i.e. for Huffman encoding
	 *                  because in the first pass the Huffman tree gets built and in
	 *                  the second pass the actual encoding is executed.
	 * @throws ImageHandlingException when an error occurred during conversion
	 */
	private void executeConversion(boolean firstPass) throws ImageHandlingException {
		byte[] imageData;

		if (!autoCompressionWasSet && outputCompressionFormat == CompressionFormat.HUFFMAN && firstPass) {
			// Initiate the temp-file file handler which stores the uncompressed data
			uncompressedInputDataFileHandler = new FileHandler(inputPath + ".tmp");
			uncompressedInputDataFileHandler.createFile();
			uncompressedInputDataFileHandler.openOutputFile();
		}

		if (!firstPass && outputCompressionFormat == CompressionFormat.HUFFMAN) {
			// Huffman tree was created. The input data must be read again to perform the
			// encoding.
			// This only is necessary when Huffman encoding should be performed and only
			// if 'huffman' was set as an input argument.
			// In case 'auto' was set the resetting of the inputHandler already happened
			// in the simulateConversion() method.
			swapFileHandlers();
		}

		if (autoCompressionWasSet && outputCompressionFormat == CompressionFormat.HUFFMAN) {
			// In case 'auto' compression was set and Huffman encoding is the best
			// compression scenario
			// the preparation of the Huffman encoder (i.e. creating the tree) can be
			// avoided
			// because it was already done in simulateConversion() below.
			firstPass = false;
		}

		while (((imageData = inputHandler.readData()) != null)) {
			if (!autoCompressionWasSet && firstPass) {
				// This part is only necessary when compression was not set to 'auto' because
				// if it was set to 'auto' the inputHandler already points to the uncompressed
				// data

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

				if (outputCompressionFormat == CompressionFormat.HUFFMAN) {
					// Write the uncompressed input image data into the temp-file
					uncompressedInputDataFileHandler.writeData(imageData);
				}
			}

			// Finally the data gets encoded if desired by the user
			if (outputCompressionFormat == CompressionFormat.RLE) {
				imageData = rleEncoder.encode(imageData);
			} else if (outputCompressionFormat == CompressionFormat.HUFFMAN) {
				if (firstPass && !autoCompressionWasSet) {
					huffmanEncoder.prepareEncoding(imageData);
					// This only creates the Huffman tree. No encoding is done here.
					// The encoding of the data will take place the next time this method will be
					// called
				} else {
					imageData = huffmanEncoder.encode(imageData);
					// This method was now called with "false", the Huffman tree was created in the
					// first pass
					// and now the image data gets encoded.
				}
			}

			if (outputCompressionFormat != CompressionFormat.HUFFMAN) {
				// If Huffman compression is NOT the desired output format then the data can
				// already be written in the first pass.
				outputHandler.writeData(imageData);
			} else {
				if (!firstPass) {
					// The data can only be written in the 2nd pass when the Huffman tree was
					// created.
					outputHandler.writeData(imageData);
				}
			}
		}

		// All encoders and/or decoders should be flushed now to either check whether
		// there is still some data to be written (i.e. HuffmanEncoder) or whether
		// the input file was corrupt and there is missing data (i.e. RLEEncoder).
		if (inputFormat != outputFormat) {
			rgbEncoder.flush();
		}

		if (outputCompressionFormat == CompressionFormat.RLE) {
			rleEncoder.flush();
		}

		if (outputCompressionFormat == CompressionFormat.HUFFMAN && !firstPass) {
			// Writing the very last bytes when the file should be encoded using Huffman
			// encoding
			outputHandler.writeData(huffmanEncoder.flush());
		}
	}

	/**
	 * Finds the compression mode for the output image which produces the smallest
	 * output files. It basically simulates the encoding for all uncompressed,
	 * run-length encoded compressed or Huffman algorithm compressed images. This
	 * simulation is taken as a basis for the best choice. After having called this
	 * method, 'outputCompressionFormat' is changed from <code>AUTO</code> to the
	 * optimal compression setting.
	 * 
	 * @throws ImageHandlingException when an error occurred during the encoding
	 *                                simulation.
	 */
	private void findBestCompressionMode() throws ImageHandlingException {
		byte[] inputData;
		byte[] outputDataUncompressed = null;
		byte[] outputDataRLE;
		byte[] outputDataHuffman;
		boolean huffmanPreparationStep = true;

		// Writes the uncompressed data from the input image into a temp-file which will
		// be reused
		// so that the decoding is only executed once to save computing power.
		uncompressedInputDataFileHandler = new FileHandler(inputPath + ".tmp");
		uncompressedInputDataFileHandler.createFile();
		uncompressedInputDataFileHandler.openOutputFile();

		for (int i = 0; i < 2; i++) {
			// Two passes are necessary because of the Huffman algorithm.
			if (!huffmanPreparationStep) {
				// Huffman tree was created. The uncompressed input data will be used to
				// simulate the Huffman encoding
				swapFileHandlers();
			}

			if (i == 0 || i == 1 && outputFormat == Format.PROPRA) {
				while (((inputData = inputHandler.readData()) != null)) {
					if (huffmanPreparationStep) {
						// First the data must be decompressed if it was compressed
						if (inputCompressionFormat == CompressionFormat.RLE) {
							outputDataUncompressed = rleDecoder.decode(inputData);
						} else if (inputCompressionFormat == CompressionFormat.HUFFMAN) {
							outputDataUncompressed = huffmanDecoder.decode(inputData);
						} else {
							outputDataUncompressed = inputData;
						}

						if (inputFormat != outputFormat) {
							// Change the pixel order if converting from tga>propra or propra>tga
							outputDataUncompressed = rgbEncoder.encode(outputDataUncompressed);
						}
						// Write the uncompressed input image data into the temp-file
						uncompressedInputDataFileHandler.writeData(outputDataUncompressed);

						// Calculate the file size for an uncompressed or an RLE compressed image
						outputDataRLE = rleEncoder.encode(outputDataUncompressed);
						uncompressedLength += outputDataUncompressed.length;
						rleCompressedLength += outputDataRLE.length;
						// Create the Huffman tree which will later be re-used in case Huffman
						// compression
						// is the one which produces the smallest files
						huffmanEncoder.prepareEncoding(outputDataUncompressed);
					} else {
						// Calculate the file size for a Huffman compressed image
						outputDataHuffman = huffmanEncoder.encode(outputDataUncompressed);
						huffmanCompressedLength += outputDataHuffman.length;
					}
				}
			} else {
				// In case the output file is *.tga, Huffman cannot be used
				// Therefore it will always be the "worst" compression option.
				huffmanCompressedLength = Integer.MAX_VALUE;
			}

			if (!huffmanPreparationStep && outputFormat == Format.PROPRA) {
				// Retrieve the last bytes for a Huffman compressed image
				outputDataHuffman = huffmanEncoder.flush();
				huffmanCompressedLength += outputDataHuffman.length;
			}
			huffmanPreparationStep = false;
		}
		// Now the file sizes of all three compression algorithms are known and the
		// smallest one gets choosen
		if ((uncompressedLength < rleCompressedLength) & (uncompressedLength < huffmanCompressedLength)) {
			outputCompressionFormat = CompressionFormat.UNCOMPRESSED;
		} else if ((rleCompressedLength < uncompressedLength) & (rleCompressedLength < huffmanCompressedLength)) {
			outputCompressionFormat = CompressionFormat.RLE;
		} else if ((huffmanCompressedLength < uncompressedLength) & (huffmanCompressedLength < rleCompressedLength)) {
			outputCompressionFormat = CompressionFormat.HUFFMAN;
		} else {
			// In the unlikely case all strategies create the same file size the strategy
			// which needs
			// less computing time will be used --> uncompressed.
			outputCompressionFormat = CompressionFormat.UNCOMPRESSED;
		}
		// If the input data was corrupt (i.e. invalid RLE data such as missing bytes)
		// it will be detected
		// when calling the Encoder's flush() method
		uncompressedInputDataFileHandler.reset();
		rgbEncoder.flush();
		rleEncoder.flush();
		rleEncoder.reset();
		rgbEncoder.reset();
		huffmanEncoder.reset();
	}

	/**
	 * Swaps the uncompressedInputDataFileHandler with the inputHandler.
	 * 
	 * @throws ImageHandlingException
	 */
	private void swapFileHandlers() throws ImageHandlingException {
		uncompressedInputDataFileHandler.closeFile();
		uncompressedInputDataFileHandler.openInputStream();
		uncompressedInputDataFileHandler.reset();
		inputHandler.closeFile();
		inputHandler = uncompressedInputDataFileHandler;
	}

	/**
	 * Closes the input and output files and removes the temp-file which contained
	 * the uncompressed image data segment of the input image.
	 */
	@Override
	protected void finalizeConversion() throws ImageHandlingException {
		super.finalizeConversion();
		if (outputCompressionFormat == CompressionFormat.HUFFMAN) {
			inputHandler.removeFile();
		}
	}

}
