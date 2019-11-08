package propra.imageconverter;

import java.io.File;

/**
 * Converts images from a given input file path to a desired output file path.
 * Expects two arguments specified as
 * <p><ul>
 * <li><code>--input="FILEPATH"</code>
 * <li><code>--output="FILEPATH"</code>
 * </ul>
 * @author Oliver Eckstein
 *
 */
public class ImageConverter {
	public static void main(String[] args) {
		System.out.println(" +++ ImageConverter started +++");
		
		if (args.length != 2) {
			System.err.println("Not enough arguments specified.");
			System.out.println(" +++ Error while converting - Shutting down ImageConverter +++");
			System.exit(123);
		}

		String inputPath = args[0].replace("--input=", "");
		String outputPath = args[1].replace("--output=", "");

		File inputImage = new File(inputPath);
		File outputImage = new File(outputPath);
		
		if (!inputImage.exists()) {
			System.out.println(" Invalid file paths given for input or output image.");
			System.out.println(" +++ Shutting down ImageConverter +++");
			System.exit(123);
		}
		try {
			ImageController controller = new ImageController(inputImage, outputImage);
			controller.convert();
		} catch (ImageHandlingException e) {
			System.err.println(e.getMessage());
			System.out.println(" +++ Error while converting - Shutting down ImageConverter +++");
			System.exit(123);
		}
		
		System.out.println(" +++ Shutting down ImageConverter +++");
	}
}