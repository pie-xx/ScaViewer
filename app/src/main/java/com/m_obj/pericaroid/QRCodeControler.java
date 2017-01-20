package com.m_obj.pericaroid;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Hashtable;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import android.graphics.Bitmap;

public class QRCodeControler {
	/// �G���R�[�h�ݒ�
	private static final String ENCORD_NAME = "UTF-8";

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Bitmap createQRCode(String contents){
	Bitmap ret = null;
	try{
		// QR�R�[�h�𐶐�
		QRCodeWriter writer = new QRCodeWriter();
		Hashtable encodeHint = new Hashtable();
		encodeHint.put(EncodeHintType.CHARACTER_SET, ENCORD_NAME);
		encodeHint.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
		BitMatrix bitData = writer.encode(contents, BarcodeFormat.QR_CODE, 350, 350, encodeHint);
		int width = bitData.getWidth();
		int height = bitData.getHeight();
		int[] pixels = new int[width * height];
		// All are 0, or black, by default
		for (int y = 0; y < height; y++) {
			int offset = y * width;
			for (int x = 0; x < width; x++) {
				pixels[offset + x] = bitData.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
			}
		}
		// Bitmap�ɕϊ�
		ret = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		ret.setPixels(pixels, 0, width, 0, 0, width, height);
		return ret;
	}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}

	public static InputStream mkQRimage(String str){
		Bitmap bm = createQRCode( str );
		ByteArrayOutputStream outbuff = new ByteArrayOutputStream();
		bm.compress(Bitmap.CompressFormat.PNG, 100, outbuff);
		return new ByteArrayInputStream(outbuff.toByteArray());
	}
}
