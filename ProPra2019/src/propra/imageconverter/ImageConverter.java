package propra.imageconverter;

import java.io.File;

/**
 * Converts images from a given input file path to a desired output file path.
 * @author Oliver Eckstein
 *
 */
public class ImageConverter {
	private static final String INPUT = "--input=";
	private static final String OUTPUT = "--output=";
	private static final String COMPRESSION = "--compression=";
	private static final String DECODE32 = "--decode-base-32";
	private static final String ENCODE32 = "--encode-base-32";
	private static final String DECODE_N = "--decode-base-n";
	private static final String ENCODE_N = "--encode-base-n=";

	public static void main(String[] args) {
		System.out.println(" +++ ImageConverter started +++");

		if (args.length < 2) {
			System.err.println("Not enough arguments specified.");
			System.out.println(" +++ Error while converting - Shutting down ImageConverter +++");
			System.exit(123);
		}

		// Let's find out which arguments the user has specified
		String inputPath = findCommand(args, INPUT);
		String outputPath = findCommand(args, OUTPUT);
		String compression = findCommand(args, COMPRESSION);
		String decode32 = findCommand(args, DECODE32);
		String encode32 = findCommand(args, ENCODE32);
		String decode_n = findCommand(args, DECODE_N);
		String encode_n = findCommand(args, ENCODE_N);
		
		// Some error handling here if the user did not provide enough/the right arguments
		if(args.length == 3) {
			if(inputPath == null || outputPath == null || compression == null) {
				System.out.println("--input --output --compression not set.");
				System.out.println(" +++ Shutting down ImageConverter +++");
				System.exit(123);
			}
		} else if(args.length == 2) {
			if((encode32 == null && decode32 == null) && (encode_n == null && decode_n == null)) {
				System.out.println("Either --encode or --decode must be set.");
				System.out.println(" +++ Shutting down ImageConverter +++");
				System.exit(123);
			} else {
				if(inputPath == null) {
					System.out.println("--input not set.");
					System.out.println(" +++ Shutting down ImageConverter +++");
					System.exit(123);
				}
			}
		} else {
			System.out.println("Wrong number of input parameters given.");
			System.out.println(" +++ Shutting down ImageConverter +++");
			System.exit(123);
		}
		
		File inputImage = new File(inputPath);
		if (!inputImage.exists()) {
			System.out.println(" Invalid file path given for input image.");
			System.out.println(" +++ Shutting down ImageConverter +++");
			System.exit(123);
		}
		
		// Here we go
		if(args.length == 3) {
			// Handle image conversion			
			File outputImage = new File(outputPath);			
			try {
				ConversionController controller = new ConversionController(inputImage, outputImage, compression);
				controller.convert();
				System.out.println(" +++ Image conversion successful +++");
			} catch (ImageHandlingException e) {
				System.err.println(e.getMessage());
				System.out.println(" +++ Error while converting - Shutting down ImageConverter +++");
				System.exit(123);
			}			
		} else if(args.length == 2) {			
			try {
				// Handle file encoding/decoding
				if(encode32 != null) {
					ConverterHelper.baseCoder(inputImage, true, true, null);
				} else if (decode32 != null){
					ConverterHelper.baseCoder(inputImage, false, true, null);
				} else if(encode_n != null) {					
					ConverterHelper.baseCoder(inputImage, true, false, encode_n);
				} else if (decode_n != null	 ) {
					ConverterHelper.baseCoder(inputImage, false, false, null);
				}
				System.out.println(" +++ Encoding/Decoding successful +++");
			} catch (ImageHandlingException e) {
				System.err.println(e.getMessage());
				System.out.println(" +++ Error while encoding - Shutting down ImageConverter +++");
				System.exit(123);
			}			
		}
		System.out.println(" +++ Shutting down ImageConverter +++");
		
	}

	/**
	 * Helper function to find out which input parameter can be found in an array of arguments.
	 * @param args the array of arguments.
	 * @param commandToFind the input parameter related to a input command.
	 * @return the input parameter without the command.
	 */
	private static String findCommand(String[] args, String commandToFind) {
		for (int i = 0; i < args.length; i++) {
			if(args[i].startsWith(commandToFind)) {
				return args[i].replace(commandToFind, "");
			}
		}
		return null;
	}
}