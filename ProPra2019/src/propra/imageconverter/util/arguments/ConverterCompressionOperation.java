package propra.imageconverter.util.arguments;

import propra.imageconverter.image.Image.CompressionType;

public enum ConverterCompressionOperation {
	
	UNCOMPRESSED_TO_UNCOMPRESSED,
	RLE_TO_RLE,
	HUFFMAN_TO_HUFFMAN,
	UNCOMPRESSED_TO_RLE,
	UNCOMPRESSED_TO_HUFFMAN,
	RLE_TO_UNCOMPRESSED,
	HUFFMAN_TO_UNCOMPRESSED,
	RLE_TO_HUFFMAN,
	HUFFMAN_TO_RLE,
	AUTO;
		
	public CompressionType getCompressionType(ConverterCompressionOperation arg) {
		if(arg == UNCOMPRESSED_TO_UNCOMPRESSED ||
				arg == RLE_TO_UNCOMPRESSED ||
				arg == HUFFMAN_TO_UNCOMPRESSED) {
			return CompressionType.UNCOMPRESSED;
		}
		
		if(arg == RLE_TO_RLE ||
				arg == UNCOMPRESSED_TO_RLE ||
				arg == HUFFMAN_TO_RLE) {
			return CompressionType.RLE;
		}
		
		if(arg == HUFFMAN_TO_HUFFMAN ||
				arg == UNCOMPRESSED_TO_HUFFMAN ||
				arg == RLE_TO_HUFFMAN) {
			return CompressionType.HUFFMAN;
		}
		
		return null;
	}

}


