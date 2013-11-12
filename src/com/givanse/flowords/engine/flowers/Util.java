package com.givanse.flowords.engine.flowers;

import android.graphics.PointF;

class Util {

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
}
