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
		//ImageConverter converter = new ImageConverter(args[0], args[1]);
		ImageConverter converter = new ImageConverter("test_03_uncompressed.propra", "output.tga");
		converter.convert();
	}

	private void convert() {
		String inputExtension = getFileExtension(inputPath);
		String outputExtension = getFileExtension(outputPath);

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
		 * If fileName does not contain "." or starts with "." then it is not a valid file.
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
		
		int width;
		int height;
		if(extension.equals("propra")) {
			/*
			 * Check if input file size is valid of propra file.
			 */
			String hex = String.format("%02x", inputImage[11]) + String.format("%02x", inputImage[10]);
			width = Integer.parseInt(hex, 16);
			hex = String.format("%02x", inputImage[13]) + String.format("%02x", inputImage[12]);
			height = Integer.parseInt(hex, 16);
			hex = "";
			for (int i = 0; i < 8; i++) {
				hex += String.format("%02x", inputImage[23 - i]);
			}

			dataLength = Integer.parseInt(hex, 16);
			if (dataLength != width * height * 3) {
				System.err.println("Source propra file corrupt. Invalid size of data segment.");			
				System.exit(123);
			}

			/*
			 * Check if checksum is valid of propra file.
			 */
			hex = "";
			for (int i = 0; i < 4; i++) {
				hex += String.format("%02x", inputImage[27 - i]);
			}

			byte[] inputImageData = new byte[dataLength];
			System.arraycopy(inputImage, 28, inputImageData, 0, dataLength);

			if (!hex.equals(getCheckSum(inputImageData))) {
				System.err.println("Source propra file corrupt. Invalid check sum.");			
				System.exit(123);
			}
		} else if (extension.equals("tga")) {
			// TODO tga handling and also propra->propra or tga->tga
		} else {
			System.err.println("Unknown file format.");			
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
			 * Write pixel data into outputImage and write into output file.
			 * From TGA: BGR --> PROPRA: GBR
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
				System.err
						.println("Error writing output file. Invalid path or user does not have enough rights.");
				e.printStackTrace();
				System.exit(123);
			} catch (IOException e) {
				System.err
						.println("Error writing output file. Invalid path or user does not have enough rights.");
				e.printStackTrace();
				System.exit(123);
			}
		} // TODO handle further output images
		
	}
}
