package propra.testenvironment;

import propra.imageconverter.ImageConverter;

public class PropraTest {
	public static void main(String[] args) {
		String[] argsArray = new String[3];
		
		argsArray[0] = "--input=../KE3_TestBilder/test_01.tga";
		argsArray[1] = "--output=../KE3_Konvertiert/test_01_rle.tga";
		argsArray[2] = "--compression=rle";
		ImageConverter.main(argsArray);
		argsArray[0] = "--input=../KE3_Konvertiert/test_01_rle.tga";
		argsArray[1] = "--output=../KE3_Konvertiert/test_01_huffman.propra";
		argsArray[2] = "--compression=huffman";
		ImageConverter.main(argsArray);
		argsArray[0] = "--input=../KE3_TestBilder/test_01.tga";
		argsArray[1] = "--output=../KE3_Konvertiert/test_01_auto.propra";
		argsArray[2] = "--compression=auto";
		ImageConverter.main(argsArray);		
		
		argsArray[0] = "--input=../KE3_Konvertiert/test_01_rle.tga";
		argsArray[1] = "--output=../KE3_Konvertiert/test_01_wieder_unc_aus_rle.tga";
		argsArray[2] = "--compression=uncompressed";
		ImageConverter.main(argsArray);
		
		argsArray[0] = "--input=../KE3_Konvertiert/test_01_huffman.propra";
		argsArray[1] = "--output=../KE3_Konvertiert/test_01_wieder_unc_aus_huffman.tga";
		argsArray[2] = "--compression=uncompressed";
		ImageConverter.main(argsArray);
		
		argsArray[0] = "--input=../KE3_TestBilder/test_01.tga";
		argsArray[1] = "--output=../KE3_Konvertiert/test_01_auto.tga";
		argsArray[2] = "--compression=auto";
		ImageConverter.main(argsArray);
		
		argsArray[0] = "--input=../KE3_Konvertiert/test_01_auto.propra";
		argsArray[1] = "--output=../KE3_Konvertiert/test_01_wieder_unc_aus_auto.tga";
		argsArray[2] = "--compression=uncompressed";
		ImageConverter.main(argsArray);
		
		argsArray[0] = "--input=../KE2_TestBilder/test_grosses_bild_uncompressed.tga";
		argsArray[1] = "--output=../KE3_Konvertiert/test_grosses_bild_rle.propra";
		argsArray[2] = "--compression=huffman";
		ImageConverter.main(argsArray);
		
		argsArray = new String[2];
		argsArray[0] = "--input=../KE2_TestBilder_optional/test_base-2_a.propra.base-n";
		argsArray[1] = "--decode-base-n";
		ImageConverter.main(argsArray);
		
		argsArray[0] = "--input=../KE2_TestBilder_optional/test_base-2_b.propra.base-n";
		argsArray[1] = "--decode-base-n";
		ImageConverter.main(argsArray);
		
		argsArray[0] = "--input=../KE2_TestBilder_optional/test_base-4.propra.base-n";
		argsArray[1] = "--decode-base-n";
		ImageConverter.main(argsArray);
		
		argsArray[0] = "--input=../KE2_TestBilder_optional/test_base-8.propra.base-n";
		argsArray[1] = "--decode-base-n";
		ImageConverter.main(argsArray);
		
		argsArray[0] = "--input=../KE2_TestBilder_optional/test_base-64.propra.base-n";
		argsArray[1] = "--decode-base-n";
		ImageConverter.main(argsArray);
		
		argsArray[0] = "--input=../KE2_TestBilder_optional/test_grosses_bild.propra";
		argsArray[1] = "--encode-base-32";
		ImageConverter.main(argsArray);
		
		argsArray = new String[3];
		argsArray[0] = "--input=../KE2_TestBilder_optional/test_base-2_a.propra";
		argsArray[1] = "--output=../KE3_Konvertiert/test_base-2_a.tga";
		argsArray[2] = "--compression=uncompressed";
		ImageConverter.main(argsArray);
		argsArray[0] = "--input=../KE2_TestBilder_optional/test_base-2_b.propra";
		argsArray[1] = "--output=../KE3_Konvertiert/test_base-2_b.tga";
		argsArray[2] = "--compression=uncompressed";
		ImageConverter.main(argsArray);
		argsArray[0] = "--input=../KE2_TestBilder_optional/test_base-4.propra";
		argsArray[1] = "--output=../KE3_Konvertiert/test_base-4.tga";
		argsArray[2] = "--compression=uncompressed";
		ImageConverter.main(argsArray);
		argsArray[0] = "--input=../KE2_TestBilder_optional/test_base-8.propra";
		argsArray[1] = "--output=../KE3_Konvertiert/test_base-8.tga";
		argsArray[2] = "--compression=uncompressed";
		ImageConverter.main(argsArray);
		argsArray[0] = "--input=../KE2_TestBilder_optional/test_base-64.propra";
		argsArray[1] = "--output=../KE3_Konvertiert/test_base-64.tga";
		argsArray[2] = "--compression=uncompressed";
		ImageConverter.main(argsArray);
		argsArray[0] = "--input=../KE2_TestBilder_optional/test_grosses_bild.propra";
		argsArray[1] = "--output=../KE3_Konvertiert/test_grosses_bild.tga";
		argsArray[2] = "--compression=uncompressed";
		ImageConverter.main(argsArray);
	}
}
