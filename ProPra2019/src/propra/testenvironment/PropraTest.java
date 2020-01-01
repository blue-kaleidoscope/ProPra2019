package propra.testenvironment;

import propra.imageconverter.ImageConverter;

public class PropraTest {
	public static void main(String[] args) {
		String[] argsArray = new String[3];		
		/*argsArray[0] = "--input=../KE3_Konvertiert/test_01.tga";
		argsArray[1] = "--output=../KE3_Konvertiert/test_01_huffman_oli.propra";
		argsArray[2] = "--compression=huffman";
		ImageConverter.main(argsArray);*/
		
		/*argsArray[0] = "--input=../KE3_Konvertiert/test_05_unc.tga";
		argsArray[1] = "--output=../KE3_Konvertiert/test_05_rle.tga";
		argsArray[2] = "--compression=rle";
		ImageConverter.main(argsArray);*/
		argsArray[0] = "--input=../KE3_Konvertiert/test_05_rle.tga";
		argsArray[1] = "--output=../KE3_Konvertiert/test_05_auto.propra";
		argsArray[2] = "--compression=auto";
		ImageConverter.main(argsArray);
		argsArray[0] = "--input=../KE3_Konvertiert/test_05_auto.propra";
		argsArray[1] = "--output=../KE3_Konvertiert/test_05_wieder_unc.tga";
		argsArray[2] = "--compression=uncompressed";
		ImageConverter.main(argsArray);
		
		
		/*argsArray[0] = "--input=../KE3_Konvertiert/test_02.propra";
		argsArray[1] = "--output=../KE3_Konvertiert/test_02_unc.tga";
		argsArray[2] = "--compression=uncompressed"; ImageConverter.main(argsArray);*/
		
		/*argsArray[0] = "--input=../KE2_TestBilder/test_04_rle.propra";
		argsArray[1] = "--output=../KE3_Konvertiert/test_04.tga";
		argsArray[2] = "--compression=rle";
		ImageConverter.main(argsArray);*/
		
		/*argsArray[0] = "--input=../KE2_TestBilder/test_04_rle.propra";
		argsArray[1] = "--output=../KE3_Konvertiert/test_04_unc.tga";
		argsArray[2] = "--compression=uncompressed";
		ImageConverter.main(argsArray);*/
		
		/*argsArray[0] = "--input=../KE2_TestBilder/test_grosses_bild_uncompressed.tga";
		argsArray[1] = "--output=../KE3_Konvertiert/test_grosses_bild_rle.tga";
		argsArray[2] = "--compression=rle";
		ImageConverter.main(argsArray);*/
		
		/*argsArray = new String[2];
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
		ImageConverter.main(argsArray);*/
	}
}
