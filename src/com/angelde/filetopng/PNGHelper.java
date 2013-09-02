package com.angelde.filetopng;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/** Minimal PNG encoder to create PNG streams (and MIDP images) from RGBA arrays.<br>
 * Copyright 2006-2009 Christian Fröschlin www.chrfr.de<br>
 * Terms of Use: You may use the PNG encoder free of charge for any purpose you desire, as long as you do not claim credit for
 * the original sources and agree not to hold me responsible for any damage arising out of its use.<br>
 * If you have a suitable location in GUI or documentation for giving credit, I'd appreciate a non-mandatory mention of:<br>
 * PNG encoder (C) 2006-2009 by Christian Fröschlin, www.chrfr.de 
 * 
 * <br>
 * Adapted from LibGDX' PixmapIO. Modified to fit File2PNG. Don't blame about missing documentation.*/
public class PNGHelper {
	public static int[] crcTable;
	public static final int ZLIB_BLOCK_SIZE = 32000;

	public static byte[] write(Image image) throws IOException {
		byte[] signature = new byte[] {(byte)137, (byte)80, (byte)78, (byte)71, (byte)13, (byte)10, (byte)26, (byte)10};
		byte[] header = createHeaderChunk(image.width, image.height);
		byte[] data = createDataChunk(image);
		byte[] trailer = createTrailerChunk();

		ByteArrayOutputStream png = new ByteArrayOutputStream(signature.length + header.length + data.length + trailer.length);
		png.write(signature);
		png.write(header);
		png.write(data);
		png.write(trailer);
		return png.toByteArray();
	}

	private static byte[] createHeaderChunk(int width, int height) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(13);
		DataOutputStream chunk = new DataOutputStream(baos);
		chunk.writeInt(width);
		chunk.writeInt(height);
		chunk.writeByte(8); // Bitdepth
		chunk.writeByte(6); // Colortype ARGB
		chunk.writeByte(0); // Compression
		chunk.writeByte(0); // Filter
		chunk.writeByte(0); // Interlace
		return toChunk("IHDR", baos.toByteArray());
	}

	private static byte[] createDataChunk(Image image) throws IOException {
		int width = image.width;
		int height = image.height;
		int dest = 0;
		byte[] raw = new byte[4 * width * height + height];
		for (int y = 0; y < height; y++) {
			raw[dest++] = 0; // No filter
			for (int x = 0; x < width; x++) {
				// 32-bit ARGB8888
				int pixel = image.getPixel(x, y);

				int mask = pixel & 0xFFFFFFFF;
				int aa = mask >> 24 & 0xff;
				int rr = mask >> 16 & 0xff;
				int gg = mask >> 8 & 0xff;
				int bb = mask & 0xff;

				raw[dest++] = (byte)rr;
				raw[dest++] = (byte)gg;
				raw[dest++] = (byte)bb;
				raw[dest++] = (byte)aa;
			}
		}
		return toChunk("IDAT", toZLIB(raw));
	}

	private static byte[] createTrailerChunk() throws IOException {
		return toChunk("IEND", new byte[] {});
	}

	private static byte[] toChunk (String id, byte[] raw) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(raw.length + 12);
		DataOutputStream chunk = new DataOutputStream(baos);

		chunk.writeInt(raw.length);

		byte[] bid = new byte[4];
		for (int i = 0; i < 4; i++) {
			bid[i] = (byte)id.charAt(i);
		}

		chunk.write(bid);

		chunk.write(raw);

		int crc = 0xFFFFFFFF;
		crc = updateCRC(crc, bid);
		crc = updateCRC(crc, raw);
		chunk.writeInt(~crc);

		return baos.toByteArray();
	}

	private static void createCRCTable() {
		crcTable = new int[256];
		for (int i = 0; i < 256; i++) {
			int c = i;
			for (int k = 0; k < 8; k++)
				c = (c & 1) > 0 ? 0xedb88320 ^ c >>> 1 : c >>> 1;
			crcTable[i] = c;
		}
	}

	private static int updateCRC(int crc, byte[] raw) {
		if (crcTable == null) createCRCTable();
		for (byte element : raw)
			crc = crcTable[(crc ^ element) & 0xFF] ^ crc >>> 8;
		return crc;
	}

	/*
	 * This method is called to encode the image data as a zlib block as required by the PNG specification. This is 
	 * a minimal ZLIB encoder which uses uncompressed deflate blocks (fast, short, easy, but no compression). 
	 * 
	 * TODO: Replace this ZLIB encoder with something real. JZlib maybe?
	 */
	private static byte[] toZLIB(byte[] raw) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(raw.length + 6 + raw.length / ZLIB_BLOCK_SIZE * 5);
		DataOutputStream zlib = new DataOutputStream(baos);

		byte tmp = (byte)8;
		zlib.writeByte(tmp); // CM = 8, CMINFO = 0
		zlib.writeByte((31 - (tmp << 8) % 31) % 31); // FCHECK
		// (FDICT/FLEVEL=0)

		int pos = 0;
		while (raw.length - pos > ZLIB_BLOCK_SIZE) {
			writeUncompressedDeflateBlock(zlib, false, raw, pos, (char)ZLIB_BLOCK_SIZE);
			pos += ZLIB_BLOCK_SIZE;
		}

		writeUncompressedDeflateBlock(zlib, true, raw, pos, (char)(raw.length - pos));

		// zlib check sum of uncompressed data
		zlib.writeInt(calcADLER32(raw));

		return baos.toByteArray();
	}

	private static void writeUncompressedDeflateBlock(DataOutputStream zlib, boolean last, byte[] raw, int off, char len)
		throws IOException {
		zlib.writeByte((byte)(last ? 1 : 0)); // Final flag, Compression type 0
		zlib.writeByte((byte)(len & 0xFF)); // Length LSB
		zlib.writeByte((byte)((len & 0xFF00) >> 8)); // Length MSB
		zlib.writeByte((byte)(~len & 0xFF)); // Length 1st complement LSB
		zlib.writeByte((byte)((~len & 0xFF00) >> 8)); // Length 1st complement
		// MSB
		zlib.write(raw, off, len); // Data
	}

	private static int calcADLER32(final byte[] raw) {
		int s1 = 1;
		int s2 = 0;
		for (int i = 0; i < raw.length; i++) {
			final int abs = raw[i] >= 0 ? raw[i] : (raw[i] + 256);
			s1 = (s1 + abs) % 65521;
			s2 = (s2 + s1) % 65521;
		}
		return (s2 << 16) + s1;
	}
}
