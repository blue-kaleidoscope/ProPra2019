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

import propra.imageconverter.error.ImageConverterErrorCode;
import propra.imageconverter.error.ImageHandlingException;

/**
 * A <code>FileHandler</code> provides helper methods to access files, read from
 * files or write into files.
 * 
 * @author Oliver Eckstein
 *
 */
public class FileHandler {

	private BufferedInputStream inputStream;
	private BufferedOutputStream outputStream;
	private String filePath;
	private final int BUFFER_SIZE = 8 * 1024;
	private File file;

	/**
	 * Creates a new <code>FileHandler</code>.
	 * 
	 * @param filePath the file's file path.
	 * @throws ImageHandlingException
	 */
	public FileHandler(String filePath) throws ImageHandlingException {
		if (filePath == null) {
			throw new ImageHandlingException("File path cannot be null.", ImageConverterErrorCode.UNEXPECTED_ERROR);
		}
		this.filePath = filePath;
		inputStream = null;
		outputStream = null;
		file = null;
	}

	/**
	 * To assign a file to this <code>FileHandler</code>.
	 * 
	 */
	public void createFile() {
		file = new File(filePath);
	}

	/**
	 * Opens and input stream of this <code>FileHandler</code>'s assigned file.
	 * 
	 * @throws ImageHandlingException when the assigned file was not found.
	 */
	public void openInputStream() throws ImageHandlingException {
		try {
			inputStream = new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE);
		} catch (FileNotFoundException e) {
			throw new ImageHandlingException("File not found: " + filePath, ImageConverterErrorCode.INVALID_FILEPATH);
		}
	}

	/**
	 * Opens and output stream of this <code>FileHandler</code>'s assigned file.
	 * 
	 * @throws ImageHandlingException when the assigned file was not found.
	 */
	public void openOutputFile() throws ImageHandlingException {
		try {
			outputStream = new BufferedOutputStream(new FileOutputStream(file), BUFFER_SIZE);
		} catch (FileNotFoundException e) {
			throw new ImageHandlingException("File not found: " + filePath, ImageConverterErrorCode.INVALID_FILEPATH);
		}
	}

	/**
	 * Writes a new line into this <code>FileHandler</code>'s assigned file. The
	 * <code>FileHandler</code>'s output stream points to the next line after the
	 * written new line after calling this method.
	 * 
	 * @param input the new line's input.
	 * @throws ImageHandlingException when the <code>FileHandler</code>'s output
	 *                                stream could not write into the file.
	 */
	public void writeNewLine(String input) throws ImageHandlingException {
		if (input != null && input.length() > 0) {
			try {
				outputStream.write(input.getBytes());
				outputStream.write('\n');
				outputStream.flush();
			} catch (IOException e) {
				throw new ImageHandlingException("Could not write a new line into file: " + filePath,
						ImageConverterErrorCode.IO_ERROR);
			}
		}
	}

	/**
	 * Writes data into the output file.
	 * 
	 * @param data the data to be written
	 * @throws ImageHandlingException when the <code>FileHandler</code>'s output
	 *                                stream could not write into the file.
	 */
	public void writeData(byte[] data) throws ImageHandlingException {
		if (data != null && data.length > 0) {
			try {
				outputStream.write(data);
				outputStream.flush();
			} catch (IOException e) {
				throw new ImageHandlingException("Could not write data into file: " + filePath,
						ImageConverterErrorCode.IO_ERROR);
			}
		}
	}

	/**
	 * To read data from the <code>FileHandler</code>'s assigned file.
	 * 
	 * @return the file's data or <code>null</code> if there is no more data because
	 *         the end of the stream has been reached.
	 * @throws ImageHandlingException when the <code>FileHandler</code>'s input
	 *                                stream could not read from the file.
	 */
	public byte[] readData() throws ImageHandlingException {
		byte[] outputData = new byte[BUFFER_SIZE];
		int bytesRead = 0;
		try {
			bytesRead = inputStream.read(outputData);
		} catch (IOException e) {
			throw new ImageHandlingException("Could not read data from file: " + filePath,
					ImageConverterErrorCode.IO_ERROR);
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

	/**
	 * Reads up to a specified number of bytes from the input stream.
	 * 
	 * @param length the number of bytes to be read.
	 * @return the read data or null if no data was read.
	 * @throws ImageHandlingException when the <code>FileHandler</code>'s input
	 *                                stream could not read from the file.
	 */
	public byte[] readNBytes(int length) throws ImageHandlingException {
		byte[] outputData;

		try {
			outputData = inputStream.readNBytes(length);
		} catch (IOException e) {
			throw new ImageHandlingException("Could not read data from file: " + filePath,
					ImageConverterErrorCode.IO_ERROR);
		}

		if (outputData.length == 0) {
			return null;
		}

		return outputData;
	}

	/**
	 * Closes this <code>FileHandler</code>'s assigned file and sets the reference
	 * of the input or output stream to <code>null</code>.
	 * 
	 * @throws ImageHandlingException when the file could not be closed.
	 */
	public void closeFile() throws ImageHandlingException {
		try {
			if (inputStream != null) {
				inputStream.close();
				inputStream = null;
			} else if (outputStream != null) {
				outputStream.close();
				outputStream = null;
			}
		} catch (IOException e) {
			throw new ImageHandlingException("Could not close file: " + filePath, ImageConverterErrorCode.IO_ERROR);
		}
	}

	/**
	 * Removes this <code>FileHandler</code>'s assigned file.
	 * 
	 * @throws ImageHandlingException when the file could not be removed.
	 */
	public void removeFile() throws ImageHandlingException {
		if (!file.delete()) {
			throw new ImageHandlingException("Could not remove file: " + filePath, ImageConverterErrorCode.IO_ERROR);
		}
	}

	/**
	 * Returns the file path of this <code>FileHandler</code>'s assigned file.
	 * 
	 * @return the file path
	 */
	public String getFilePath() {
		return file.getPath();
	}

	/**
	 * Returns this <code>FileHandler</code>'s assigned file.
	 * 
	 * @return the file
	 */
	public File getFile() {
		return file;
	}

	/**
	 * Reads the first line of this <code>FileHandler</code>'s assigned file. The
	 * pointer of this <code>FileHandler</code>'s assigned input stream points to
	 * the position after the first line after calling this method.
	 * 
	 * @return the first line.
	 * @throws ImageHandlingException when the file was not found.
	 */
	public String readFirstLine() throws ImageHandlingException {
		String firstLine;
		try (Scanner fileReader = new Scanner(file)) {
			firstLine = fileReader.nextLine();
			fileReader.close();
		} catch (FileNotFoundException e) {
			throw new ImageHandlingException("Could not read first line of file: " + filePath,
					ImageConverterErrorCode.IO_ERROR);
		}
		skipNBytes(firstLine.length() + 1);
		return firstLine;
	}

	/**
	 * To write data into an already existing file.
	 * 
	 * @param inputData the data to be written
	 * @param offset    the offset at which position the data should be written.<br>
	 *                  0 if the data should be written at the beginning of the
	 *                  file.
	 * @throws ImageHandlingException
	 */
	public void writeDataRandomlyIntoFile(byte[] inputData, int offset) throws ImageHandlingException {
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(file, "rw");
		} catch (FileNotFoundException e) {
			throw new ImageHandlingException("Error writing into file. Could not find file: " + filePath,
					ImageConverterErrorCode.IO_ERROR);
		}
		try {
			for (int i = offset; i < inputData.length; i++) {
				raf.seek(i);
				raf.write(inputData[i]);
			}
			raf.close();
		} catch (IOException e) {
			throw new ImageHandlingException("Error while closing file: " + filePath, ImageConverterErrorCode.IO_ERROR);
		}
	}

	/**
	 * To let this <code>FileHandler</code>'s assigned input stream skip a specified
	 * number of bytes.
	 * 
	 * @param n the number of bytes to be skipped
	 * @throws ImageHandlingException when an error occurred during skipping.
	 */
	public void skipNBytes(long n) throws ImageHandlingException {
		try {
			inputStream.skip(n);
		} catch (IOException e) {
			throw new ImageHandlingException("Error while skipping bytes", ImageConverterErrorCode.IO_ERROR);
		}
	}

	/**
	 * Closes this <code>FileHandler</code>'s assigned input stream and recreates it
	 * to set it to the beginning of the file.
	 * 
	 * @throws ImageHandlingException when the input stream could not neither be
	 *                                closed nor recreated.
	 */
	public void reset() throws ImageHandlingException {
		try {
			inputStream.close();
			inputStream = new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE);
		} catch (IOException e) {
			throw new ImageHandlingException("Error while resetting this input stream: " + filePath,
					ImageConverterErrorCode.IO_ERROR);
		}

	}

}
