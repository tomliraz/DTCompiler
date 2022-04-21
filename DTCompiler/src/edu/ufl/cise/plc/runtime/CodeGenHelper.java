package edu.ufl.cise.plc.runtime;

import java.awt.image.BufferedImage;

public class CodeGenHelper {
	
	
	public static BufferedImage setAllPixels(BufferedImage image, int val) {
		ColorTuple c = new ColorTuple(val);
		for (int x = 0; x < image.getWidth(); x++)
			for (int y = 0; y < image.getHeight(); y++) {
				ImageOps.setColor(image, x, y, c);
			}
		return image;
	}
	
	public static BufferedImage setAllPixelsColor(BufferedImage image, ColorTuple val) {
		for (int x = 0; x < image.getWidth(); x++)
			for (int y = 0; y < image.getHeight(); y++) {
				ImageOps.setColor(image, x, y, val);
			}
		return image;
	}
	
	public static BufferedImage setAllPixelsFloat(BufferedImage image, float val) {
		ColorTuple c = new ColorTuple(Math.round(val));
		for (int x = 0; x < image.getWidth(); x++)
			for (int y = 0; y < image.getHeight(); y++) {
				ImageOps.setColor(image, x, y, c);
			}
		return image;
	}
	

}
