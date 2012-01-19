package fi.harism.wallpaper.ornament;

import android.graphics.PointF;

public final class OrnamentUtils {

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

	public static OrnamentSpline genSpline(PointF p0, PointF p1, PointF p2,
			PointF p3) {
		OrnamentSpline spline = new OrnamentSpline();
		spline.mCtrlPoints[0].set(p0);
		spline.mCtrlPoints[1].set(p1);
		spline.mCtrlPoints[2].set(p2);
		spline.mCtrlPoints[3].set(p3);
		spline.mStartT = 0f;
		spline.mEndT = 1f;
		return spline;
	}

	public static float length(PointF point) {
		return (float) Math.sqrt(point.x * point.x + point.y * point.y);
	}

	public static void rand(PointF ret, float xMin, float yMin, float xMax,
			float yMax) {
		ret.x = (float) (xMin + Math.random() * (xMax - xMin));
		ret.y = (float) (yMin + Math.random() * (yMax - yMin));
	}

	public static float randF(float min, float max) {
		return (float) (min + Math.random() * (max - min));
	}

	public static int randI(int min, int max) {
		return (int) (min + Math.random() * (max - min));
	}

	public static long randL(long min, long max) {
		return (long) (min + Math.random() * (max - min));
	}

}
