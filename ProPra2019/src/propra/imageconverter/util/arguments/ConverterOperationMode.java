package propra.imageconverter.util.arguments;

public enum ConverterOperationMode {
	CONVERT,
	CODE_BASE32,
	DECODE_BASE32,
	CODE_BASEN,
	DECODE_BASEN;
	
	public boolean operationIsBaseCoding() {
		return this.ordinal() >= CODE_BASE32.ordinal();
	}
	
	public boolean operationIsBaseEncoding() {
		return (this == CODE_BASE32 || this == CODE_BASEN);
	}
	
	public boolean operationIsBaseDecoding() {
		return (this == DECODE_BASE32 || this == DECODE_BASEN);
	}
}
