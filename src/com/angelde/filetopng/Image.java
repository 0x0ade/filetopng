package com.angelde.filetopng;

/**
 * Platform- and ADB-independent image class. <br>
 * Simple enough to contain width, height and the pixel data.
 */
public class Image {
	
	public final int width;
	public final int height;
	private final int[] pixels;
	
	public Image(int width, int height) {
		this.width = width;
		this.height = height;
		pixels = new int[width*height];
	}
	
	public int getPixel(int x, int y) {
		if (x < 0 || x >= width || y < 0 || y >= height) {
			throw new RuntimeException("That escalated quickly...");
		}
		
		return pixels[x + y*width];
	}
	
	public void setPixel(int x, int y, int color) {
		if (x < 0 || x >= width || y < 0 || y >= height) {
			throw new RuntimeException("That escalated quickly...");
		}
		
		pixels[x + y*width] = color;
	}

}
