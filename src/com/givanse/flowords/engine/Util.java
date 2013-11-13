package com.givanse.flowords.engine;

import android.graphics.Color;
import android.graphics.PointF;

public class Util {

	/**
	 * Calculates distance between point1 and point2.
	 */
	public static float getDistance(PointF p1, PointF p2) {
		float dx = p1.x - p2.x;
		float dy = p1.y - p2.y;
		return (float) Math.sqrt(dx * dx + dy * dy);
	}

	/**
	 * Calculates distance between point1 + point2 and point3.
	 */
	public static float getDistance(PointF p1, PointF p2, PointF p3) {
		float dx = p1.x + p2.x - p3.x;
		float dy = p1.y + p2.y - p3.y;
		return (float) Math.sqrt(dx * dx + dy * dy);
	}

	/**
	 * Generates random value between [min, max).
	 */
	public static float random(float min, float max) {
		return min + (float) (Math.random() * (max - min));
	}
	

	/**
	 * Converts a color value to an array of floats.
	 */
	public static float[] getColor(int value) {
		float[] retVal = new float[4];
		retVal[0] = (float) Color.red(value) / 255;
		retVal[1] = (float) Color.green(value) / 255;
		retVal[2] = (float) Color.blue(value) / 255;
		retVal[3] = (float) Color.alpha(value) / 255;
		return retVal;
	}
}
