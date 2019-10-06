package propra.imageconverter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

public class ImageConverter {

	private String inputPath;
	private String outputPath;
	private byte[] inputImage;
	private byte[] outputImage;
	private int dataLength;

	public ImageConverter(String inputPath, String outputPath) {
		this.inputPath = inputPath;
		this.outputPath = outputPath;
		inputImage = null;
		outputImage = null;
	}

	public static void main(String[] args) {
		// toTGA("test_03_uncompressed.propra");
		// ImageConverter converter = new ImageConverter(args[0], args[1]);
		// TODO work with input parameters
		ImageConverter converter = new ImageConverter("test_03_uncompressed.propra", "output.tga");
		converter.convert();
	}

	private void convert() {
		String inputExtension = getFileExtension(inputPath);
		String outputExtension = getFileExtension(outputPath);
		
		/*
		 * Check if input or output file format is known.
		 * Either *.tga or *.propra
		 */
		if (!inputExtension.equals("tga") && !inputExtension.equals("propra")) {
			System.err.println("Unknown input file format.");
			System.exit(123);
		}

		if (!outputExtension.equals("tga") && !outputExtension.equals("propra")) {
			System.err.println("Unknown output file format.");
			System.exit(123);
		}

		File inputFile = new File(inputPath);
		try {
			inputImage = Files.readAllBytes(inputFile.toPath());
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Error reading source file.");
			e.printStackTrace();
			System.exit(123);
		}

		checkFile(inputExtension);
		writeData(inputExtension, outputExtension, dataLength);
	}

	private String getFileExtension(String filePath) {
		String fileExtension = "";

		/*
		 * If fileName does not contain "." or starts with "." then it is not a valid
		 * file.
		 */
		if (filePath.contains(".") && filePath.lastIndexOf(".") != 0) {
			fileExtension = filePath.substring(filePath.lastIndexOf(".") + 1);
		}
		if (fileExtension == null) {
			System.err.println("Could not read file extension. Invalid file path: " + filePath);
			System.exit(123);
		}
		return fileExtension.toLowerCase();
	}

	private void checkFile(String extension) {

		int width = 0;
		int height = 0;
		int compression = -1;
		String hex;

		if (extension.equals("propra")) {
			// TODO check whether width * height * 3 = length of pixel data
			
			/*
			 * Get source image dimensions.
			 */
			hex = String.format("%02x", inputImage[11]) + String.format("%02x", inputImage[10]);
			width = Integer.parseInt(hex, 16);
			hex = String.format("%02x", inputImage[13]) + String.format("%02x", inputImage[12]);
			height = Integer.parseInt(hex, 16);

			// Check if data segment is of correct length according to header
			hex = "";
			for (int i = 0; i < 8; i++) {
				hex += String.format("%02x", inputImage[23 - i]);
			}

			dataLength = Integer.parseInt(hex, 16);
			if (dataLength != width * height * 3) {
				System.err.println("Source file corrupt. Invalid header.");
				System.exit(123);
			}

			/*
			 * Check if checksum is valid of propra file.
			 */
			hex = "";
			for (int i = 0; i < 4; i++) {
				hex += String.format("%02x", inputImage[27 - i]);
			}
			
			/**
			 * inputImageData will only contain the pixel data without header data.
			 */
			byte[] inputImageData = new byte[dataLength];
			/*
			 * arraycopy will fail if dataLength is smaller than image data segment of inputImage.
			 * dataLength is calculated from the header of inputImage.
			 * The size of inputImage after the header refers to the real size of the image.
			 */
			try {
				// Copy TODO finish comment
				System.arraycopy(inputImage, 28, inputImageData, 0, dataLength);
			} catch (ArrayIndexOutOfBoundsException e) {
				System.err.println("Source file corrupt. Invalid size of image data segment.");
				System.exit(123);
			}

			if (!hex.equals(getCheckSum(inputImageData))) {
				System.err.println("Source propra file corrupt. Invalid check sum.");
				System.exit(123);
			}
			
			/*
			 * Get compression type.
			 */
			hex = String.format("%02x", inputImage[16]);
			compression = Integer.parseInt(hex, 16);
			
		} else if (extension.equals("tga")) {
			// TODO get values of header for width/height
			// TODO check whether width * height * 3 = length of pixel data
			
			/*
			 * Get compression type.
			 */
			hex = String.format("%02x", inputImage[2]);
			compression = Integer.parseInt(hex, 16);
			
			/*
			 * Get source image dimensions.
			 */
			hex = String.format("%02x", inputImage[12]) + String.format("%02x", inputImage[13]);
			width = Integer.parseInt(hex, 16);
			hex = String.format("%02x", inputImage[14]) + String.format("%02x", inputImage[15]);
			height = Integer.parseInt(hex, 16);
		}

		if (width <= 0 || height <= 0) {
			System.err.println("Source file corrupt. Invalid image size.");
			System.exit(123);
		}
		
		if (compression != 0) {
			System.err.println("Invalid compression of source file.");
			System.exit(123);
		}
	}

	public static String getCheckSum(byte[] data) {
		int x = 65513;

		int a_i = 0; // initial sum
		int b_i = 1; // initial b_0

		for (int i = 0; i < data.length; i++) {
			a_i += (i + 1) + Byte.toUnsignedInt(data[i]);
			a_i %= x;
			b_i = (b_i % x + a_i) % x;
		}
		return Integer.toHexString(a_i * (int) Math.pow(2, 16) + b_i);
	}

	private void writeData(String inputExtension, String outputExtension, int dataLength) {

		if (inputExtension.equals("propra") && outputExtension.equals("tga")) {
			/*
			 * Set up header data of the output image.
			 */
			outputImage = new byte[18 + dataLength];
			outputImage[2] = 2;
			outputImage[16] = 24;
			outputImage[17] = 32;
			for (int i = 12; i < 16; i++) {
				outputImage[i] = inputImage[i - 2];
			}

			/*
			 * Write pixel data into outputImage and write into output file. From TGA: BGR
			 * --> PROPRA: GBR
			 */
			for (int i = 0; i < dataLength; i = i + 3) {
				outputImage[18 + i] = inputImage[28 + i + 1];
				outputImage[18 + i + 1] = inputImage[28 + i];
				outputImage[18 + i + 2] = inputImage[28 + i + 2];
			}

			/*
			 * Create output file and write into the file.
			 */
			File outputFile = new File(outputPath);

			try (FileOutputStream stream = new FileOutputStream(outputFile.getPath())) {
				stream.write(outputImage);
			} catch (FileNotFoundException e) {
				System.err.println("Error writing output file. Invalid path or user does not have enough rights.");
				e.printStackTrace();
				System.exit(123);
			} catch (IOException e) {
				System.err.println("Error writing output file. Invalid path or user does not have enough rights.");
				e.printStackTrace();
				System.exit(123);
			}
		} // TODO handle further output images

	}
}
