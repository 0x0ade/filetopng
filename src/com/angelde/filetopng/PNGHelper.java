package com.angelde.filetopng;

import java.io.UnsupportedEncodingException;

public final class PNGHelper {
	private PNGHelper() {
	}

	public final static byte[] ID = {-119, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13};
	public final static byte[] IHDR = getBytes("IHDR", "ASCII");
	public final static byte[] IDAT = getBytes("IDAT", "ASCII");
	public final static byte[] IEND = getBytes("IEND", "ASCII");
	public final static byte[] HEAD = {8, 6, 0, 0, 0};

	private final static byte[] getBytes(String string, String charsetName) {
		try {
			return string.getBytes(charsetName);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

}
