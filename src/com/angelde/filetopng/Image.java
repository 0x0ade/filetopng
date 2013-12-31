package com.angelde.filetopng;

/**
 * Platform-independent fixed-size image class. <br>
 * Simple enough to contain width, height and the pixel data.
 */
public final class Image {

	protected final int[] pixels;
	protected final int width;
	protected final int height;

	public Image(int width, int height) {
		this.width = width;
		this.height = height;
		this.pixels = new int[this.width*this.height];
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int[] getPixels() {
		return pixels;
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
