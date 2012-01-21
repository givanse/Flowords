package fi.harism.wallpaper.flowers;

import android.graphics.PointF;

public final class FlowerUtils {

	public static float dist(PointF point1, PointF point2) {
		float dx = point1.x - point2.x;
		float dy = point1.y - point2.y;
		return (float) Math.sqrt(dx * dx + dy * dy);
	}

	public static float dist(PointF point1, PointF point2, PointF point3) {
		float dx = point1.x + point2.x - point3.x;
		float dy = point1.y + point2.y - point3.y;
		return (float) Math.sqrt(dx * dx + dy * dy);
	}

	public static float length(PointF point) {
		return (float) Math.sqrt(point.x * point.x + point.y * point.y);
	}

	public static void rand(PointF ret, float xMin, float yMin, float xMax,
			float yMax) {
		ret.x = xMin + (float) (Math.random() * (xMax - xMin));
		ret.y = yMin + (float) (Math.random() * (yMax - yMin));
	}

	public static float randF(float min, float max) {
		return min + (float) (Math.random() * (max - min));
	}

	public static int randI(int min, int max) {
		return min + (int) (Math.random() * (max - min));
	}

	public static long randL(long min, long max) {
		return min + (long) (Math.random() * (max - min));
	}

}
