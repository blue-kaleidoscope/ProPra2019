package propra.imageconverter.util.arguments;

/**
 * Modes an <code>ImageConverter</code> can work in.
 * @author Oliver Eckstein
 *
 */
public enum ConverterOperationMode {
	CONVERT,
	CODE_BASE32,
	DECODE_BASE32,
	CODE_BASEN,
	DECODE_BASEN;
	
	/**
	 * Checks whether the given <code>ConverterOperationMode</code> is for encoding/decoding base-32 or base-n files.
	 * @return true if this <code>ConverterOperationMode</code> stands for encoding/decoding base-32 or base-n files, false otherwise.
	 */
	public boolean operationIsBaseCoding() {
		return this.ordinal() >= CODE_BASE32.ordinal();
	}
	
	/**
	 * Checks whether the given <code>ConverterOperationMode</code> is for encoding base-32 or base-n files.
	 * @return true if this <code>ConverterOperationMode</code> stands for encoding/decoding base-32 or base-n files, false otherwise.
	 */
	public boolean operationIsBaseEncoding() {
		return (this == CODE_BASE32 || this == CODE_BASEN);
	}
	
	/**
	 * Checks whether the given <code>ConverterOperationMode</code> is for decoding base-32 or base-n files.
	 * @return true if this <code>ConverterOperationMode</code> stands for decoding base-32 or base-n files, false otherwise.
	 */
	public boolean operationIsBaseDecoding() {
		return (this == DECODE_BASE32 || this == DECODE_BASEN);
	}
}
