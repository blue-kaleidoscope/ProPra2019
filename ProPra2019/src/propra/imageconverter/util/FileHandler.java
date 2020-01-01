package propra.imageconverter.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Scanner;

import propra.imageconverter.error.ErrorCodes;
import propra.imageconverter.error.ImageHandlingException;

public class FileHandler {

	private BufferedInputStream inputStream;
	private BufferedOutputStream outputStream;
	private String filePath;
	private final int BUFFER_SIZE = 8 * 1024;
	private File file;

	public FileHandler(String filePath) {
		this.filePath = filePath;
		inputStream = null;
		outputStream = null;
		file = null;
	}

	public void createFile() throws ImageHandlingException {
		file = new File(filePath);
	}

	public void openInputFile() throws ImageHandlingException {
		try {
			inputStream = new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE);
		} catch (FileNotFoundException e) {
			throw new ImageHandlingException("File not found: " + filePath, ErrorCodes.INVALID_FILEPATH);
		}
	}

	public void openOutputFile() throws ImageHandlingException {
		try {
			outputStream = new BufferedOutputStream(new FileOutputStream(file), BUFFER_SIZE);
		} catch (FileNotFoundException e) {
			throw new ImageHandlingException("File not found: " + filePath, ErrorCodes.INVALID_FILEPATH);
		}
	}
	
	public void writeNewLine(String input) throws ImageHandlingException {
		if (input != null && input.length() > 0) {
			try {
				outputStream.write(input.getBytes());
				outputStream.write('\n');
				outputStream.flush();
			} catch (IOException e) {
				throw new ImageHandlingException(
						"Could not write a new line into file: " + filePath,
						ErrorCodes.IO_ERROR);
			}			
		}
	}

	public void writeData(byte[] data) throws ImageHandlingException {
		if (data != null && data.length > 0) {
			try {
				outputStream.write(data);
				outputStream.flush();
			} catch (IOException e) {
				throw new ImageHandlingException(
						"Could not write data into file: " + filePath,
						ErrorCodes.IO_ERROR);
			}			
		}
	}

	/**
	 * 
	 * @return the data from this <code>FileHandler</code>'s <code>File</code><br>
	 *         <code>null</code> when the end of the file was reached
	 * @throws ImageHandlingException
	 */
	public byte[] readData() throws ImageHandlingException {
		byte[] outputData = new byte[BUFFER_SIZE];
		int bytesRead = 0;
		try {
			bytesRead = inputStream.read(outputData);
		} catch (IOException e) {
			throw new ImageHandlingException(
					"Could not read data from file: " + filePath,
					ErrorCodes.IO_ERROR);
		}

		if (bytesRead == -1) {
			return null;
		} else if (bytesRead < BUFFER_SIZE) {
			byte[] tmpOutputData = new byte[bytesRead];
			System.arraycopy(outputData, 0, tmpOutputData, 0, bytesRead);
			outputData = tmpOutputData;
		}

		return outputData;
	}
	
	public byte[] readData(int length) throws ImageHandlingException {
		byte[] outputData;
		
		try {
			outputData = inputStream.readNBytes(length);
		} catch (IOException e) {
			throw new ImageHandlingException(
					"Could not read data from file: " + filePath,
					ErrorCodes.IO_ERROR);
		}

		if (outputData.length == 0) {
			return null;
		}
		
		return outputData;
	}	

	public void closeFile() throws ImageHandlingException {
		try {
			if (inputStream != null) {
				inputStream.close();
			} else if (outputStream != null) {
				outputStream.close();
			}
		} catch (IOException e) {
			throw new ImageHandlingException(
					"Could not close file: " + filePath,
					ErrorCodes.IO_ERROR);
		}
	}
	
	public String getFilePath() {
		return file.getPath();
	}
	
	public File getFile() {
		return file;
	}
	
	public String readFirstLine() throws ImageHandlingException {
		String firstLine;
		try (Scanner fileReader = new Scanner(file)) {
			firstLine = fileReader.nextLine();
			fileReader.close();
		} catch (FileNotFoundException e) {
			throw new ImageHandlingException(
					"Could not read first line of file: " + filePath,
					ErrorCodes.IO_ERROR);
		}
		
		return firstLine;
	}
	
	/**
	 * To write data into an already existing file.
	 * @param inputData the data to be written
	 * @param offset the offset at which position the data should be written.<br>
	 * 0 if the data should be written at the beginning of the file.
	 * @throws ImageHandlingException 
	 */
	public void writeDataRandomlyIntoFile(byte[] inputData, int offset) throws ImageHandlingException {
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(file, "rw");
		} catch (FileNotFoundException e) {
			throw new ImageHandlingException(
					"Error writing into file. Could not find file: " + filePath,
					ErrorCodes.IO_ERROR);
		}
		try {
			for (int i = offset; i < inputData.length; i++) {
				raf.seek(i);
				raf.write(inputData[i]);
			}
			raf.close();
		} catch (IOException e) {
			throw new ImageHandlingException(
					"Error while closing file: " + filePath,
					ErrorCodes.IO_ERROR);
		}
	}
	
	public void skipNBytes(long n) throws ImageHandlingException {
		try {
			inputStream.skip(n);
		} catch (IOException e) {
			throw new ImageHandlingException(
					"Error while skipping bytes",
					ErrorCodes.IO_ERROR);
		}
	}
	
	public void reset() throws ImageHandlingException {
		try {
			inputStream.close();
			inputStream = new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE);
		} catch (IOException e) {
			throw new ImageHandlingException(
					"Error while resetting this input stream: " + filePath,
					ErrorCodes.IO_ERROR);
		}
		
	}

}
