package com.angelde.filetopng;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public final class PNGDecoder {

	/**
	 * The default size of the buffer used when reading from the input stream when converting.
	 */
	public static int defaultBufferSize = 65536;

	private final static int readInt(InputStream is) throws IOException {
		byte b[] = read(is, 4);
		return ((b[0] & 0xFF) << 24) +
				((b[1] & 0xFF) << 16) +
				((b[2] & 0xFF) << 8) +
				((b[3] & 0xFF));
	}

	private final static byte[] tbytes = new byte[16];
	private final static byte[] read(InputStream is, int count) throws IOException {
		byte[] bytes = tbytes;
		if (count > tbytes.length) {
			bytes = new byte[count];
		}
		is.read(bytes, 0, count);
		return bytes;
	}
	
	private final static boolean compare(byte[] b1, byte[] b2) {
		//if(b1.length != b2.length) {
		//	return false;
		//}

		for(int i = 0; i < b2.length; i++) {
			if(b1[i] != b2[i]) {
				return false;
			}
		}

		return true;
	}
	
	private final static void checkEquality(byte[] b1, byte[] b2) {
		if(!compare(b1, b2)) {
			throw new RuntimeException("Format error");
		}
	}
	
	public final static Image decode(InputStream is) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(is, defaultBufferSize);

		checkEquality(read(bis, 12), PNGHelper.ID);

		checkEquality(read(bis, 4), PNGHelper.IHDR);
		
		int width = readInt(bis);
		int height = readInt(bis);
		
		Image result = new Image(width, height);
		
		checkEquality(read(bis, 5), PNGHelper.HEAD);

		read(bis, 4);

		int size = readInt(bis);
		
		checkEquality(read(bis, 4), PNGHelper.IDAT);
		
		byte[] pixdata = read(bis, size);

		Inflater inflater = new Inflater();
		inflater.setInput(pixdata, 0, size);
		
		try {
			byte[] row = new byte[width * 4];
			for (int y = 0; y < height; y++) {
				inflater.inflate(new byte[1]);
				inflater.inflate(row);
				for (int x = 0; x < width; x++) {
					result.setPixel(x, y, 
							((row[x*4 + 0] & 0xFF) << 16) +//R
							((row[x*4 + 1] & 0xFF) << 8) +//G
							((row[x*4 + 2] & 0xFF)) +//B
							((row[x*4 + 3] & 0xFF) << 24));//A
				}
			}
		} catch(DataFormatException e) {
			throw new RuntimeException("ZIP error", e);
		}

		inflater.end();
		
		readInt(bis);
		readInt(bis);

		checkEquality(read(bis, 4), PNGHelper.IEND);
		
		bis.close();
		
		return result;
	}
	
}
