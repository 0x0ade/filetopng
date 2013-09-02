package com.angelde.filetopng;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * The converter class converts any binary input into an image output. <br> 
 * It GZIPs the binary input for an additional space save by default.
 * @author maik
 *
 */
public class Converter {
	
	public static enum Use {
		TRUE,
		FALSE,
		AUTO;
	}
	
	private Converter() {
	}
	
	/**
	 * The version number of the converter. <br>
	 * It becomes increased as soon as an codechange appears that may 
	 * create cross-version incompatibilities. <br>
	 * It is checked against when converting back but an mismatch doesn't stop 
	 * the conversion from PNG to file - it may "crash" itself.
	 */
	public final static byte VERSION = (byte)((int)7);
	/**
	 * The size of the F2P header containing special data.
	 */
	public final static byte HEADERSIZE = (byte)((int)8);
	
	/**
	 * Whether to GZIP the binary data upon conversion to PNG or not. AUTO by default.
	 */
	public static Use useGZIP = Use.AUTO;
	
	/**
	 * Pulls data from the given InputStream and converts it to an image.
	 * @param is InputStream to read data from
	 * @return BufferedImage being the resulting image
	 * @throws IOException
	 */
	public static Image convertToPNG(InputStream is) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buf = new byte[2048];
		int n;
		while ((n = is.read(buf)) >= 0) {
			baos.write(buf, 0, n);
		}
		baos.close();
		byte[] data = baos.toByteArray();
		Image png = convertToPNG(data);
		return png;
	}
	
	/**
	 * Converts the given data to an image.
	 * @param is InputStream to read data from
	 * @return BufferedImage being the resulting image
	 * @throws IOException
	 */
	public static Image convertToPNG(byte[] data) throws IOException {
		byte[] newdata = data;
		boolean gzip = false;
		if (useGZIP != Use.FALSE) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			GZIPOutputStream gos = new GZIPOutputStream(baos);
			gos.write(data);
			gos.close();
			newdata = baos.toByteArray();
			gzip = true;
		}
		if (useGZIP == Use.AUTO) {
			if (newdata.length >= data.length) {
				newdata = data;
				gzip = false;
			}
		}
		Image png = convertToPNG0(newdata, gzip);
		return png;
	}

	private static Image convertToPNG0(byte[] argdata, boolean gzip) {
		System.out.println("Converting to PNG"+(gzip?" compressing data with GZIP ":"")+"...");
		//Create the data array to convert. It contains the magic bytes and header.
		float size = (float)(argdata.length+HEADERSIZE) / 4f;
		int add = 0;
		if (Math.ceil(size) != size) {
			add = 4;
		}
		byte[] data = new byte[((int)(size))*4+add];
		
		//Fill the header with data needed to load properly.
		data[0] = VERSION;
		data[1] = HEADERSIZE;
		data[2] = gzip?((byte)((int)0x01)):((byte)((int)0x00));
		
		//Fill the header with version-dependant data. 
		//... nothing as for now.
		
		//Fill the header with the filesize. It should be an integer stored in Big Endian if I recall correctly.
		data[(HEADERSIZE & 0xFF)-4] = (byte) (argdata.length >> 24);
		data[(HEADERSIZE & 0xFF)-3] = (byte) (argdata.length >> 16);
		data[(HEADERSIZE & 0xFF)-2] = (byte) (argdata.length >> 8);
		data[(HEADERSIZE & 0xFF)-1] = (byte) (argdata.length);
		
		//Copy the file data into the data array.
		System.arraycopy(argdata, 0, data, (HEADERSIZE & 0xFF), argdata.length);
		
		//Get best width and height coordinates for the image.
		int height = 16;
		if (height > (data.length/4)) {
			height = (data.length/4);
		}
		boolean isRound = Math.ceil((float)(data.length/4f)/(float)height) == (float)(data.length/4)/(float)height;
		int width = isRound?0:1 + (data.length/4)/height;
		while (width > height) {
			height *= 2;
			isRound = Math.ceil((float)(data.length/4f)/(float)height) == (float)(data.length/4)/(float)height;
			width = isRound?0:1 + (data.length/4)/height;
		}
		if (width == 0) {
			width = 1;
		}
		System.out.println("Available data: "+((width*height)*4)+"; Filelength: "+data.length);
		if (((width*height)*4) == data.length) {
			System.out.println("Perfect match!");
		} else if (((width*height)*4) > data.length){
			System.out.println("Additional free data. Not bad.");
		} else {
			System.err.println("Missing data! Output won't match input anymore!");
		}
		
		//Convert.
		Image bi = new Image(width, height);
		int x = 0;
		int y = 0;
		int rgba = 0;
		for (int i = 0; i < width*height*4; i += 4) {
			rgba = (((int)get(data, i+0) & 0xFF) << 24) | //alpha
					(((int)get(data, i+1) & 0xFF) << 16) | //red
					(((int)get(data, i+2) & 0xFF) << 8)  | //green
					(((int)get(data, i+3) & 0xFF) << 0); //blue
			bi.setPixel(x, y, rgba);
			y++;
			if (y >= height) {
				x++;
				y = 0;
			}
		}
		
		System.out.println("Done!");
		return bi;
	}
	
	/**
	 * Pulls pixels from the given image and converts it into an file.
	 * @param bi BufferedImage to read pixels from
	 * @return Data of the file converted into the image in form of an byte[].
	 * @throws IOException
	 */
	public static byte[] convertFromPNG(Image bi) throws IOException {
		System.out.println("Converting png back to file...");
		//Create the byte array
		int width = bi.width;
		int height = bi.height;
		byte[] data = new byte[width*height*4];
		
		//Convert pixels to data.
		int ci = 0;
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int argb = bi.getPixel(x, y);
				
				byte alpha = (byte) (0xFF & (argb >> 24));
				byte red = (byte) (0xFF & (argb >> 16));
				byte green = (byte) (0xFF & (argb >> 8));
				byte blue = (byte) (0xFF & (argb >> 0));
				
				data[ci+0] = alpha;
				data[ci+1] = red;
				data[ci+2] = green;
				data[ci+3] = blue;
				
				ci += 4;
			}
		}
		
		//Compare the header.
		
		//Get version and header size.
		byte version = data[0];
		byte headersize = data[1];
		if (version != VERSION) {
			System.err.println("VERSION missmatch! Continuing...");
		}
		if (headersize != HEADERSIZE) {
			System.err.println("HEADERSIZE missmatch! Continuing...");
		}
		
		byte gzip = data[2];
		
		//Get version-dependent values.
		//... nothing for now. 
		
		//Get filesize. Convert it back from Big Endian(?) to integer.
		byte[] size = new byte[4];
		size[0] = data[(headersize & 0xFF)-4];
		size[1] = data[(headersize & 0xFF)-3];
		size[2] = data[(headersize & 0xFF)-2];
		size[3] = data[(headersize & 0xFF)-1];
		int filesize = (int) (((int)(size[0] & 0xFF) << 24) |
				((int)(size[1] & 0xFF) << 16) |
				((int)(size[2] & 0xFF) << 8) |
				(int)(size[3] & 0xFF));
		
		System.out.println("Filesize: "+filesize);
		
		//Get the filedata and return it.
		byte[] filedata = new byte[filesize];
		System.arraycopy(data, headersize, filedata, 0, filesize);
		byte[] enddata = filedata;
		if (gzip == (byte)((int)1)) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ByteArrayInputStream bais = new ByteArrayInputStream(filedata);
			GZIPInputStream gis = new GZIPInputStream(bais);
			byte[] buf = new byte[2048];
			int n;
			while ((n = gis.read(buf)) >= 0) {
				baos.write(buf, 0, n);
			}
			gis.close();
			baos.close();
			enddata = baos.toByteArray();
		}
		return enddata;
	}
	
	private static byte get(byte[] data, int i) {
		if (i < data.length) {
			return data[i];
		}
		return (byte) 0x00;
	}
	
	private static void printInfo() {
		System.out.println("FileToPNG conversion tool");
		System.out.println("\"Core Version\": "+((int)VERSION & 0xFF));
		System.out.println("Usage: java -jar filetopng.jar input dir output gzip");
		System.out.println("dir (direction) is either topng or tofile.");
		System.out.println("input is the file to convert and output the file to save to.");
		System.out.println("gzip can be either y or n. It's optional and available only when dir is topng.");
	}
	
	public static void main(String[] args) {
		if (args.length != 3 && args.length != 4) {
			printInfo();
			return;
		}
		
		String inarg = args[0];
		String dirarg = args[1];
		String outarg = args[2];
		boolean topng = dirarg.equalsIgnoreCase("topng");
		if (args.length == 4) {
			if (!topng) {
				printInfo();
				return;
			}
			useGZIP = args[3].toLowerCase().equals("y")?Use.TRUE:args[3].toLowerCase().equals("n")?Use.FALSE:Use.AUTO;
		}
		
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
				Image png = convertToPNG(fis);
				byte[] data = PNGHelper.write(png);
				fos.write(data);
			} else {
				Image png = PNGHelper.read(fis);//TODO
				byte[] data = convertFromPNG(png);
				fos.write(data);
			}
			
			fis.close();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
