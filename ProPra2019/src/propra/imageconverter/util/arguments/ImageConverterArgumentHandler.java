package propra.imageconverter.util.arguments;

import propra.imageconverter.error.ImageConverterErrorCode;
import propra.imageconverter.error.ImageHandlingException;

/**
 * The <code>ImageConverterArgumentHandler</code> handles command line argument
 * inputs given by the user when executing the <code>ImageConverter</code>. It
 * reads the user's input and based on that define's the working mode of the
 * <code>ImageConverter</code>.
 * 
 * @author Oliver Eckstein
 *
 */
public class ImageConverterArgumentHandler {

	/*
	 * All valid input argument prefixes.
	 */
	private final String INPUT = "--input=";
	private final String OUTPUT = "--output=";
	private final String COMPRESSION = "--compression=";
	private final String DECODE32 = "--decode-base-32";
	private final String ENCODE32 = "--encode-base-32";
	private final String DECODE_N = "--decode-base-n";
	private final String ENCODE_N = "--encode-base-n=";
	
	/*
	 * All valid input argument control words.
	 */
	private final String EXT_TGA = "tga";
	private final String EXT_PROPRA = "propra";
	private final String COMP_UNC = "uncompressed";
	private final String COMP_RLE = "rle";
	private final String COMP_HUF = "huffman";
	private final String COMP_AUTO = "auto";

	private final String EXTENSION_BASE32 = ".base-32";
	private final String EXTENSION_BASEN = ".base-n";
	/*
	 * The arguments which are necessary to create a new ConversionHandler. They get
	 * decoded from the user input.
	 */
	private String inputPath;
	private String outputPath;
	private ConverterOperationMode operationMode;
	private Format inputFormat;
	private Format outputFormat;
	private CompressionFormat outputCompressionFormat;
	private String encodingAlphabet;

	/**
	 * To create a new <code>ImageConverterArgumentHandler</code>, check the user
	 * input and decode it for further use when executing the
	 * <code>ImageConverter</code>.
	 * 
	 * @param args the user's command line input
	 * @throws ImageHandlingException when <code>args</code> is <code>null</code> or
	 *                                invalid user input was given.
	 */
	public ImageConverterArgumentHandler(String[] args) throws ImageHandlingException {
		if (args == null) {
			throw new ImageHandlingException("No input arguments were defined.",
					ImageConverterErrorCode.INVALID_USER_INPUT);
		}
		readArguments(args);
	}

	/**
	 * Reads the user input and decodes it for further use when executing the
	 * <code>ImageConverter</code>.
	 * 
	 * @param args the user's command line input
	 * @throws ImageHandlingException when invalid user input was given
	 */
	private void readArguments(String[] args) throws ImageHandlingException {
		if (args.length < 2 || args.length > 3) {
			throw new ImageHandlingException("Wrong number of arguments specified.",
					ImageConverterErrorCode.INVALID_USER_INPUT);
		}

		// Find out which arguments the user has specified
		String inputPath = findCommand(args, INPUT);
		String outputPath = findCommand(args, OUTPUT);
		String targetCompression = findCommand(args, COMPRESSION);
		String decode32 = findCommand(args, DECODE32);
		String encode32 = findCommand(args, ENCODE32);
		String decode_n = findCommand(args, DECODE_N);
		String encode_n = findCommand(args, ENCODE_N);
		String encodingAlphabet = findCommand(args, ENCODE_N);

		// Now a series of checks begins which ultimately define in which mode the
		// ImageConverter is operating

		// Begin with the input file path
		if (inputPath == null) {
			throw new ImageHandlingException("Input path must be set.", ImageConverterErrorCode.INVALID_USER_INPUT);
		} else {
			this.inputPath = inputPath;
		}

		// Check the output file path
		if (outputPath == null) {
			// This is only allowed in case a base-coding operation should be performed

			if (targetCompression != null) {
				// User did not pass an output path but defined a compression for the target
				// file
				throw new ImageHandlingException("An output path must be given in order to compress an image.",
						ImageConverterErrorCode.INVALID_USER_INPUT);
			}

			// Now it gets checked whether the user passed multiple base-coding commands
			// This would not be allowed currently because it would not be clear which
			// operation should be performed.
			String[] baseArgs = { decode32, encode32, decode_n, encode_n };
			if (!argumentsSet(baseArgs, 1)) {
				throw new ImageHandlingException("One base-coding command must be set.",
						ImageConverterErrorCode.INVALID_USER_INPUT);
			}

			// The user wants to perform a base-coding operation
			// Now it gets checked whether the input file name matches the base-coding
			// operation mode
			boolean fileExtensionIsCorrect = false;
			if (decode32 != null) {
				fileExtensionIsCorrect = inputPath.contains(EXTENSION_BASE32);
				this.outputPath = inputPath.replace(EXTENSION_BASE32, "");
				operationMode = ConverterOperationMode.DECODE_BASE32;
			}

			if (decode_n != null) {
				fileExtensionIsCorrect = inputPath.contains(EXTENSION_BASEN);
				this.outputPath = inputPath.replace(EXTENSION_BASEN, "");
				operationMode = ConverterOperationMode.DECODE_BASEN;
			}

			if (!fileExtensionIsCorrect && (decode_n != null || decode32 != null)) {
				throw new ImageHandlingException(
						"Invalid file extension of the source file. To decode a base-coded file it must be of type *.base-32 or *.base-n.",
						ImageConverterErrorCode.INVALID_USER_INPUT);
			}

			if (encode32 != null) {
				this.outputPath = inputPath + EXTENSION_BASE32;
				operationMode = ConverterOperationMode.CODE_BASE32;
			}

			if (encode_n != null) {
				if (encodingAlphabet == null || encodingAlphabet.equals("")) {
					throw new ImageHandlingException("Invalid encoding alphabet given for base-n encoding.",
							ImageConverterErrorCode.INVALID_USER_INPUT);
				}
				this.outputPath = inputPath + EXTENSION_BASEN;
				operationMode = ConverterOperationMode.CODE_BASEN;
				this.encodingAlphabet = encodingAlphabet;
			}

		} else {
			// The user wants to perform an image conversion since an input and an output path were given
			this.outputPath = outputPath;
			String inputExtension = getFileExtension(inputPath);
			String outputExtension = getFileExtension(outputPath);
			operationMode = ConverterOperationMode.CONVERT;
			
			/*
			 * Now there are two possibilities: Either the user uses the ImageConverter
			 * according to KE1 specifications: Only --input=... and --output=... are set or
			 * according to KE2 or later including --compression=<...>
			 */
			if (inputExtension.equals(EXT_TGA)) {
				inputFormat = Format.TGA;
			} else if (inputExtension.equals(EXT_PROPRA)) {
				inputFormat = Format.PROPRA;
			} else {
				throw new ImageHandlingException(
						"Input format unknown. Currently only *.tga or *.propra files allowed for image conversion operations.",
						ImageConverterErrorCode.INVALID_USER_INPUT);
			}

			if (outputExtension.equals(EXT_TGA)) {
				outputFormat = Format.TGA;
			} else if (outputExtension.equals(EXT_PROPRA)) {
				outputFormat = Format.PROPRA;
			} else {
				throw new ImageHandlingException(
						"Output format unknown. Currently only *.tga or *.propra files allowed for image conversion operations.",
						ImageConverterErrorCode.INVALID_USER_INPUT);
			}

			if (targetCompression == null) {
				// KE1 case
				outputCompressionFormat = CompressionFormat.UNCOMPRESSED;
			} else {
				// KE2 and later case
				if (targetCompression.equals(COMP_UNC)) {
					outputCompressionFormat = CompressionFormat.UNCOMPRESSED;
				} else if (targetCompression.equals(COMP_RLE)) {
					outputCompressionFormat = CompressionFormat.RLE;
				} else if (targetCompression.equals(COMP_HUF)) {
					if (outputFormat == Format.PROPRA) {
						outputCompressionFormat = CompressionFormat.HUFFMAN;
					} else {
						throw new ImageHandlingException(
								"Huffman encoding currently only supported if target file is a *.propra file.",
								ImageConverterErrorCode.INVALID_USER_INPUT);
					}
				} else if (targetCompression.equals(COMP_AUTO)) {
					outputCompressionFormat = CompressionFormat.AUTO;
				} else {
					throw new ImageHandlingException(
							"Given compression type unknown. Currently only 'uncompressed', 'rle', 'huffman' or 'auto' are allowed.",
							ImageConverterErrorCode.INVALID_USER_INPUT);
				}
			}
		}
	}

	public String getInputPath() {
		return inputPath;
	}

	public String getOutputPath() {
		return outputPath;
	}

	public ConverterOperationMode getConverterOperationMode() {
		return operationMode;
	}

	public Format getInputFormat() {
		return inputFormat;
	}

	public Format getOutputFormat() {
		return outputFormat;
	}

	public CompressionFormat getOutputCompressionFormat() {
		return outputCompressionFormat;
	}

	public String getEncodingAlphabet() {
		return encodingAlphabet;
	}

	/**
	 * Helper function to find out which input parameter can be found in an array of
	 * arguments.
	 * 
	 * @param args          the array of arguments.
	 * @param commandToFind the input parameter related to an input command.
	 * @return the input parameter without the command.
	 * @throws ImageHandlingException when a command was found a second time or when
	 *                                an args-array entry is <code>null</code>.
	 */
	private String findCommand(String[] args, String commandToFind) throws ImageHandlingException {
		String foundCommand = null;
		for (int i = 0; i < args.length; i++) {
			String currentString = args[i];
			if (currentString == null) {
				throw new ImageHandlingException("Null input not allowed!", ImageConverterErrorCode.INVALID_USER_INPUT);
			}
			if (currentString.startsWith(commandToFind)) {
				if (foundCommand == null) {
					foundCommand = currentString.replace(commandToFind, "");
				} else {
					throw new ImageHandlingException(commandToFind + " was given multiple times.",
							ImageConverterErrorCode.INVALID_USER_INPUT);
				}

			}
		}
		return foundCommand;
	}

	/**
	 * To find out whether a certain number of <code>String</code> values were set
	 * in an array.
	 * 
	 * @param args             the array
	 * @param allowedArgsCount the number of entries in the array which must be
	 *                         unequal <code>null</code>.
	 * @return <code>true</code> when the number of entries in the array which are
	 *         different than <code>null</code> are equal to
	 *         <code>allowedArgsCount</code>.
	 */
	private boolean argumentsSet(String[] args, int allowedArgsCount) {
		int foundArgs = 0;
		for (int i = 0; i < args.length; i++) {
			if (args[i] != null) {
				foundArgs++;
			}
		}
		return foundArgs == allowedArgsCount;
	}

	/**
	 * To extract the file extension of a given file path.
	 * 
	 * @param filePath the file path.
	 * @return the extension of the file path in lower case letters.
	 * @throws ImageHandlingException when file path does not contain a file
	 *                                extension.
	 */
	private String getFileExtension(String filePath) throws ImageHandlingException {
		String fileExtension = null;

		/*
		 * If fileName does not contain "." or starts with "." then it is not a valid
		 * file.
		 */
		if (filePath.contains(".") && filePath.lastIndexOf(".") != 0) {
			fileExtension = filePath.substring(filePath.lastIndexOf(".") + 1);
		}
		if (fileExtension == null) {
			throw new ImageHandlingException("Could not read file extension. Invalid file path: " + filePath,
					ImageConverterErrorCode.INVALID_FILEPATH);
		}
		return fileExtension.toLowerCase();
	}

}
