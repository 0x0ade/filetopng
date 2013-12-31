package com.angelde.filetopng;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * This class converts any binary input into image output. <br>
 * This class contains helper methods to get used
 *
 * @author maik
 *
 */
public class FileToPNG {

	private FileToPNG() {
	}

	public static Use useGZIP = Use.AUTO;

	/**
	 * The version number of FileToPNG. <br>
	 * It is increased as soon as a change appears that may
	 * create cross-version incompatibilities. <br>
	 * It is checked against when converting back but a mismatch doesn't stop
	 * the conversion from PNG to binary data - it may "crash" itself later on.
	 */
	public final static byte VERSION = (byte)((int)10);
	/**
	 * The size of the default F2P header containing data needed for reconversion.
	 */
	public final static byte HEADERSIZE = (byte)((int)8);

	/**
	 * @return header[i] when i is inside header's bounds or data[i] when i - header.length is inside data's bounds;
	 * 0 otherwise.
	 */
	public static byte get(byte[] header, byte[] data, int i) {
		if (i < header.length) {
			return header[i];
		}
		if (i - header.length < data.length) {
			return data[i - header.length];
		}
		return (byte) 0x00;
	}

	/**
	 * Pulls data from the given InputStream and converts it to an image.
	 * @param is InputStream to read data from.
	 * @return Resulting image
	 * @throws IOException when any of the underlying Java IO (GZIP, ByteArray streams) throw errors.
	 */
	public static Image convertStreamToPNG(InputStream is) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buf = new byte[65536];
		int n;
		while ((n = is.read(buf)) >= 0) {
			baos.write(buf, 0, n);
		}
		baos.close();
		return convertBlobToPNG(baos.toByteArray());
	}

	/**
	 * Converts the given data to an image.
	 * @param indata bytes to convert into an image.
	 * @return Resulting image
	 * @throws IOException when any of the underlying Java IO (GZIP, ByteArray streams) throw errors.
	 */
	public static Image convertBlobToPNG(byte[] indata) throws IOException {
		byte[] data = indata;

		//System.out.println("GZIP-ing...");
		//GZIP when specified or when resulting data smaller than original data.
		boolean gzip = false;
		if (useGZIP != Use.FALSE) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			GZIPOutputStream gos = new GZIPOutputStream(baos);
			gos.write(indata);
			gos.close();
			data = baos.toByteArray();
			gzip = true;
		}
		if (useGZIP == Use.AUTO) {
			if (data.length >= indata.length) {
				data = indata;
				gzip = false;
			}
		}
		//System.out.println("Done GZIP-ing.");

		//Determine the full resulting size of the data + header + addition
		//Find out whether an extra pixel containing trailing bytes is needed or not.
		float sizef = (float)(data.length+HEADERSIZE) / 4f;
		int add = 0;
		if (Math.ceil(sizef) != sizef) {
			add = 4;
		}
		int size = ((int)(sizef))*4+add;

		//Get best width and height coordinates for the image.
		int height = 16;
		if (height > (size/4)) {
			height = (size/4);
		}
		boolean isRound = Math.ceil((size/4f)/(float)height) == (float)(size/4)/(float)height;
		int width = isRound?0:1 + (data.length/4)/height;
		while (width > height) {
			height *= 2;
			isRound = Math.ceil((size/4f)/(float)height) == (float)(size/4)/(float)height;
			width = isRound?0:1 + (size/4)/height;
		}
		if (width == 0) {
			width = 1;
		}

		System.out.println("Available data (w*h*4): "+((width*height)*4)+"; Real data size: "+size);
		if (((width*height)*4) == size) {
			System.out.println("Perfect match!");
		} else if (((width*height)*4) > size){
			System.out.println("Additional free data.");
		} else {
			System.err.println("Missing trailing data! Blob will most likely become corrupt in conversion.");
		}

		//Create the header containing a simple setup.
		//Create the header array.
		byte[] header = new byte[HEADERSIZE];

		//Fill the header with data needed to load properly.
		header[0] = VERSION;
		header[1] = HEADERSIZE;
		header[2] = gzip?((byte)((int)0x01)):((byte)((int)0x00));

		//Fill the header with version-dependent data.
		//... nothing as for now.

		//Fill the header with the blob size in Big Endian.
		header[header.length-4] = (byte) (data.length >> 24);
		header[header.length-3] = (byte) (data.length >> 16);
		header[header.length-2] = (byte) (data.length >> 8);
		header[header.length-1] = (byte) (data.length);

		//Convert.
		//System.out.println("Converting...");
		Image image = new Image(width, height);
		int x = 0;
		int y = 0;
		int rgba = 0;
		for (int i = 0; i < size; i += 4) {
			rgba = (((int)get(header, data, i+0) & 0xFF) << 24) | //alpha
					(((int)get(header, data, i+1) & 0xFF) << 16) | //red
					(((int)get(header, data, i+2) & 0xFF) << 8)  | //green
					(((int)get(header, data, i+3) & 0xFF) << 0); //blue
			image.setPixel(x, y, rgba);
			x++;
			if (x >= width) {
				y++;
				x = 0;
			}
		}
		//System.out.println("Done converting.");
		return image;
	}

	/**
	 * Pulls pixels from the given image and converts them back into a binary blob.
	 * @param image Image to read pixels from
	 * @return Original data converted back into a byte[]
	 * @throws IOException when any of the underlying Java IO (GZIP, ByteArray streams) throw errors.
	 */
	public static byte[] convertPNGToBlob(Image image) throws IOException {
		//Create the byte array
		int width = image.width;
		int height = image.height;
		byte[] data = new byte[width*height*4];

		//Convert pixels to data.
		int ci = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int argb = image.getPixel(x, y);

				byte red = (byte) (0xFF & (argb >> 24));
				byte green = (byte) (0xFF & (argb >> 16));
				byte blue = (byte) (0xFF & (argb >> 8));
				byte alpha = (byte) (0xFF & (argb >> 0));

				data[ci+3] = alpha;
				data[ci+0] = red;
				data[ci+1] = green;
				data[ci+2] = blue;

				ci += 4;
			}
		}

		//Compare the header.

		//Get version and header size.
		byte version = data[0];
		byte headersize = data[1];
		if (version != VERSION) {
			System.err.println("CORE VERSION missmatch! Continuing...");
		}
		if (headersize != HEADERSIZE) {
			System.err.println("CORE HEADERSIZE missmatch! Continuing...");
		}

		byte gzip = data[2];

		//Get version-dependent values.
		//... nothing for now.

		//Get blob size. Convert it back from Big Endian to integer.
		byte[] size = new byte[4];
		size[0] = data[(headersize & 0xFF)-4];
		size[1] = data[(headersize & 0xFF)-3];
		size[2] = data[(headersize & 0xFF)-2];
		size[3] = data[(headersize & 0xFF)-1];
		int blobsize = (((size[0] & 0xFF) << 24) |
				((size[1] & 0xFF) << 16) |
				((size[2] & 0xFF) << 8) |
				(size[3] & 0xFF));

		System.out.println("Blob size: "+blobsize);

		//Get the blob data and return it.
		byte[] indata = new byte[blobsize];
		System.arraycopy(data, headersize, indata, 0, blobsize);
		byte[] bindata = indata;

		if (gzip == (byte)((int)1)) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ByteArrayInputStream bais = new ByteArrayInputStream(indata);
			GZIPInputStream gis = new GZIPInputStream(bais);
			byte[] buf = new byte[2048];
			int n;
			while ((n = gis.read(buf)) >= 0) {
				baos.write(buf, 0, n);
			}
			gis.close();
			baos.close();
			bindata = baos.toByteArray();
		}

		return bindata;
	}

	/**
	 * Prints basic information and usage
	 */
	public static void printInfo() {
		System.out.println("FileToPNG conversion tool");
		System.out.println("\"Core Version\": "+((int)VERSION & 0xFF));
		System.out.println("USAGE:");
		System.out.println("java -jar filetopng.jar [input file] [direction] [output] [deflate]");
		System.out.println();
		System.out.println("- [direction] is either \"topng\" or \"tofile\".");
		System.out.println("- [input] is the file to convert and [output] the file to save to.");
		System.out.println();
		System.out.println("When [direction] is \"topng\":");
		System.out.println("- [gzip] is optional and can be either \"y\" or \"n\". When ignored, it");
		System.out.println("checks whether the deflated result is larger than the inflated result");
		System.out.println("and continues on with the smallest-size result.");
	}

	public static void main(String[] args) {
		//Check whether usage needs to be printed or not.
		if (args.length != 3 && args.length != 4) {
			printInfo();
			return;
		}

		//Set arguments (files, direction, GZIP)
		String inarg = args[0];
		String dirarg = args[1];
		String outarg = args[2];
		boolean topng = dirarg.equalsIgnoreCase("topng");
		if (args.length == 4) {
			if (!topng) {
				printInfo();
				return;
			}
			useGZIP = args[3].toLowerCase().equals("y")?Use.TRUE:
					args[3].toLowerCase().equals("n")?Use.FALSE:
							Use.AUTO;
		}

		//JAVAMON! GOTTA CATCH 'EM ALL!
		try {
			File input = new File(inarg);
			FileInputStream fis = new FileInputStream(input);
			File output = new File(outarg);
			if (output.exists()) {
				output.delete();
			}
			output.createNewFile();
			FileOutputStream fos = new FileOutputStream(output);

			if (topng) {
				Image png = convertStreamToPNG(fis);
				PNGEncoder.encode(png, fos);
			} else {
				Image png = PNGDecoder.decode(fis);
				byte[] data = convertPNGToBlob(png);
				fos.write(data);
			}

			fis.close();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
