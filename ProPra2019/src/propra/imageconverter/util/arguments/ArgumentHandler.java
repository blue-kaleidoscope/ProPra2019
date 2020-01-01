package propra.imageconverter.util.arguments;

import propra.imageconverter.error.ErrorCodes;
import propra.imageconverter.error.ImageHandlingException;

public class ArgumentHandler {

	private static final String INPUT = "--input=";
	private static final String OUTPUT = "--output=";
	private static final String COMPRESSION = "--compression=";
	private static final String DECODE32 = "--decode-base-32";
	private static final String ENCODE32 = "--encode-base-32";
	private static final String DECODE_N = "--decode-base-n";
	private static final String ENCODE_N = "--encode-base-n=";
	
	private static final String EXT_TGA = "tga";
	private static final String EXT_PROPRA = "propra";
	private static final String COMP_UNC = "uncompressed";
	private static final String COMP_RLE = "rle";
	private static final String COMP_HUF = "huffman";
	private static final String COMP_AUTO = "auto";

	private String inputPath;
	private String outputPath;
	private ConverterOperationMode operationMode;
	private Format inputFormat;
	private Format outputFormat;
	private CompressionFormat outputCompressionFormat;
	private String encodingAlphabet;

	private String[] args;
	
	public ArgumentHandler(String[] args) throws ImageHandlingException {

		this.args = args;
		checkArguments();
	}

	private void checkArguments() throws ImageHandlingException {
		if (args.length < 2 || args.length > 3) {
			throw new ImageHandlingException("Wrong number of arguments specified.", ErrorCodes.INVALID_USER_INPUT);
		}		

		// Let's find out which arguments the user has specified
		String inputPath = findCommand(args, INPUT, true);
		String outputPath = findCommand(args, OUTPUT, true);
		String targetCompression = findCommand(args, COMPRESSION, true);
		String decode32 = findCommand(args, DECODE32, false);
		String encode32 = findCommand(args, ENCODE32, false);
		String decode_n = findCommand(args, DECODE_N, false);
		String encode_n = findCommand(args, ENCODE_N, false);
		String encodingAlphabet = findCommand(args, ENCODE_N, true);
		
		
		
		if (inputPath == null) {
			throw new ImageHandlingException("Input path must be set.", ErrorCodes.INVALID_USER_INPUT);
		} else {
			this.inputPath = inputPath;
		}

		if (outputPath == null) {
			// This is only allowed in case a base-coding operation should be performed
			if (targetCompression != null) {
				throw new ImageHandlingException("An output path must be given.", ErrorCodes.INVALID_USER_INPUT);
			}
			// Now it gets checked whether the user passed multiple base-coding commands
			// This would not be allowed currently because it would not be clear which
			// operation
			// should be performed.
			String[] baseArgs = { decode32, encode32, decode_n, encode_n };
			if (!argumentsSet(baseArgs, 1)) {
				throw new ImageHandlingException("One base-coding command must be set.", ErrorCodes.INVALID_USER_INPUT);
			}
			
			boolean fileExtensionIsCorrect = false;
			if (decode32 != null) {
				fileExtensionIsCorrect = inputPath.contains(".base-32");
				this.outputPath = inputPath.replace(".base-32", "");
				operationMode = ConverterOperationMode.DECODE_BASE32;
			}

			if (decode_n != null) {
				fileExtensionIsCorrect = inputPath.contains(".base-n");
				this.outputPath = inputPath.replace(".base-n", "");
				operationMode = ConverterOperationMode.DECODE_BASEN;				
			}
			
			if(!fileExtensionIsCorrect && (decode_n != null || decode32 != null)) {
				throw new ImageHandlingException(
						"Invalid file extension of the source file. To decode a base-coded file it must be of type *.base-32 or *.base-n.",
						ErrorCodes.INVALID_USER_INPUT);
			}
			
			if (encode32 != null) {
				this.outputPath = inputPath + ".base-32";
				operationMode = ConverterOperationMode.CODE_BASE32;
			}

			if (encode_n != null) {
				if(encodingAlphabet == null || encodingAlphabet.equals("")) {
					throw new ImageHandlingException("Invalid encoding alphabet given for base-n encoding.", ErrorCodes.INVALID_USER_INPUT);
				}
				this.outputPath = inputPath + ".base-n";
				operationMode = ConverterOperationMode.CODE_BASEN;
				this.encodingAlphabet = encodingAlphabet;
			}
			
			
		} else {
			this.outputPath = outputPath;
			String inputExtension = getFileExtension(inputPath);
			String outputExtension = getFileExtension(outputPath);
			operationMode = ConverterOperationMode.CONVERT;
			/*
			 * Now there are two possibilities: Either the user uses the ImageConverter
			 * according to KE1 specifications: Only --input=... and --output=... are set or
			 * according to KE2 or later including --compression=...
			 */			
			if (inputExtension.equals(EXT_TGA)) {
				inputFormat = Format.TGA;
			} else if (inputExtension.equals(EXT_PROPRA)) {
				inputFormat = Format.PROPRA;
			} else {
				throw new ImageHandlingException(
						"Input format unknown. Currently only *.tga or *.propra files allowed for image conversion operations.",
						ErrorCodes.INVALID_USER_INPUT);
			}
			
			if (outputExtension.equals(EXT_TGA)) {
				outputFormat = Format.TGA;				
			} else if (outputExtension.equals(EXT_PROPRA)) {				
				outputFormat = Format.PROPRA;
			} else {
				throw new ImageHandlingException(
						"Output format unknown. Currently only *.tga or *.propra files allowed for image conversion operations.",
						ErrorCodes.INVALID_USER_INPUT);
			}
			
			if (targetCompression == null) {
				// KE1
				outputCompressionFormat = CompressionFormat.UNCOMPRESSED;
			} else {
				// KE2 and later				
				if(targetCompression.equals(COMP_UNC)) {
					outputCompressionFormat = CompressionFormat.UNCOMPRESSED;					
				} else if(targetCompression.equals(COMP_RLE)) {
					outputCompressionFormat = CompressionFormat.RLE;					
				} else if(targetCompression.equals(COMP_HUF)) {
					if(outputFormat == Format.PROPRA) {
						outputCompressionFormat = CompressionFormat.HUFFMAN;	
					} else {
						throw new ImageHandlingException(
								"Huffman encoding currently only valid if encoding to *.propra file.",
								ErrorCodes.INVALID_USER_INPUT);
					}					
				} else if(targetCompression.equals(COMP_AUTO)) {
					if(outputFormat == Format.PROPRA) {
						outputCompressionFormat = CompressionFormat.AUTO;	
					} else {
						throw new ImageHandlingException(
								"Auto compression setting currently only valid if encoding to *.propra file.",
								ErrorCodes.INVALID_USER_INPUT);
					}					
				} else {
					throw new ImageHandlingException(
							"Compression type unknown. Currently only 'uncompressed', 'rle', 'huffman' or 'auto' are allowed.",
							ErrorCodes.INVALID_USER_INPUT);
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
	 * @param commandToFind the input parameter related to a input command.
	 * @return the input parameter without the command.
	 * @throws ImageHandlingException
	 */
	private String findCommand(String[] args, String commandToFind, boolean replaceCommand) throws ImageHandlingException {
		String foundCommand = null;
		for (int i = 0; i < args.length; i++) {
			String currentString = args[i];
			if(currentString == null) {
				throw new ImageHandlingException("Null input not allowed!",
						ErrorCodes.INVALID_USER_INPUT);
			}
			if (currentString.startsWith(commandToFind)) {
				if (foundCommand == null) {
					if(replaceCommand) {
						foundCommand = currentString.replace(commandToFind, "");	
					} else {
						foundCommand = currentString;
					}					
				} else {
					throw new ImageHandlingException(commandToFind + " was given multiple times.",
							ErrorCodes.INVALID_USER_INPUT);
				}

			}
		}
		return foundCommand;
	}

	private boolean argumentsSet(String[] args, int allowedArgs) {
		int foundArgs = 0;
		for (int i = 0; i < args.length; i++) {
			if (args[i] != null) {
				foundArgs++;
			}
		}
		return foundArgs == allowedArgs;
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
					ErrorCodes.INVALID_FILEPATH);
		}
		return fileExtension.toLowerCase();
	}

}
