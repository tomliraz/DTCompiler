package edu.ufl.cise.plc.runtime;

import java.awt.image.BufferedImage;

import edu.ufl.cise.plc.runtime.ImageOps.BoolOP;
import edu.ufl.cise.plc.runtime.ImageOps.OP;

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
	
	public static boolean binaryImageImageOp(BoolOP op, BufferedImage left, BufferedImage right) {
		int lwidth = left.getWidth();
		int rwidth = right.getWidth();
		int lheight = left.getHeight();
		int rheight = right.getHeight();
		if (lwidth != rwidth || lheight != rheight) {
			throw new PLCRuntimeException("Attempting binary operation on images with unequal sizes");
		}
		
		for (int x = 0; x < lwidth; x++) {
			for (int y = 0; y < lheight; y++) {
				ColorTuple leftColor = ColorTuple.unpack(left.getRGB(x, y));
				ColorTuple rightColor = ColorTuple.unpack(right.getRGB(x, y));
				if (!ImageOps.binaryTupleOp(op, leftColor, rightColor))
					return false;
			}
		}
		return true;
	}
	

}
