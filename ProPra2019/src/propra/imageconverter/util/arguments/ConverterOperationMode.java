package propra.imageconverter.util.arguments;

public enum ConverterOperationMode {
	
	TGA_TO_PROPRA,
	PROPRA_TO_TGA,
	TGA_TO_TGA,
	PROPRA_TO_PROPRA,
	CODE_BASE32,
	DECODE_BASE32,
	CODE_BASEN,
	DECODE_BASEN;
	
	public boolean operationIsConversion() {
		return this.ordinal() <= PROPRA_TO_PROPRA.ordinal();
	}
	
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
