package com.angelde.filetopng;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public final class PNGEncoder extends Object {
	
	protected final static void write(OutputStream os, CRC32 crc, int i) throws IOException {
		write(os, crc, new byte[] {
				(byte) ((i >> 24) & 0xFF),
				(byte) ((i >> 16) & 0xFF),
				(byte) ((i >> 8) & 0xFF),
				(byte) (i & 0xFF)
		});
	}
	
	protected final static void write(OutputStream os, CRC32 crc, byte b[]) throws IOException {
		os.write(b);
		if (crc != null) {
			crc.update(b);
		}
	}

	protected final static void writeCRC(OutputStream os, CRC32 crc) throws IOException {
		int i = (int) crc.getValue();
		os.write(new byte[] {
				(byte) ((i >> 24) & 0xFF),
				(byte) ((i >> 16) & 0xFF),
				(byte) ((i >> 8) & 0xFF),
				(byte) (i & 0xFF)
		});
		crc.reset();
	}

	public final static void encode(Image image, OutputStream os) throws IOException {
		CRC32 crc = new CRC32();

		int width = image.width;
		int height = image.height;

		write(os, null, PNGHelper.ID);

		write(os, crc, PNGHelper.IHDR);
		write(os, crc, width);
		write(os, crc, height);
		write(os, crc, PNGHelper.HEAD);

		writeCRC(os, crc);

		ByteArrayOutputStream compressed = new ByteArrayOutputStream(16777215);
		BufferedOutputStream cbos = new BufferedOutputStream(
				new DeflaterOutputStream(compressed, new Deflater(9)), 16777215);
		int pixel;
		for (int y = 0; y < height; y++) {
			cbos.write(0);
			for (int x=0;x<width;x++) {
				pixel = image.getPixel(x,y);
				cbos.write((byte)((pixel >> 16) & 0xFF));//R
				cbos.write((byte)((pixel >> 8) & 0xFF));//G
				cbos.write((byte)(pixel & 0xFF));//B
				cbos.write((byte)((pixel >> 24) & 0xFF));//A
			}
		}
		cbos.close();
		write(os, null, compressed.size());

		write(os, crc, PNGHelper.IDAT);
		write(os, crc, compressed.toByteArray());
		writeCRC(os, crc);
		write(os, null, 0);

		write(os, crc, PNGHelper.IEND);
		writeCRC(os, crc);

		os.close();
	}

}
