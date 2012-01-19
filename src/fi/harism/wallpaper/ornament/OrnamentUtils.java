package fi.harism.wallpaper.ornament;

import android.graphics.PointF;

public final class OrnamentUtils {
	
	public static long rand(long min, long max) {
		return (long)(min + Math.random() * (max - min));
	}
	
	public static float rand(float min, float max) {
		return (float)(min + Math.random() * (max - min));
	}

	public static void rand(PointF ret, float xMin, float yMin, float xMax, float yMax) {
		ret.x = (float) (xMin + Math.random() * (xMax - xMin));
		ret.y = (float) (yMin + Math.random() * (yMax - yMin));
	}
	
	public static OrnamentSpline genSpline(PointF p0, PointF p1, PointF p2, PointF p3) {
		OrnamentSpline spline = new OrnamentSpline();
		spline.mCtrlPoints[0].set(p0);
		spline.mCtrlPoints[1].set(p1);
		spline.mCtrlPoints[2].set(p2);
		spline.mCtrlPoints[3].set(p3);
		spline.mStartT = 0f;
		spline.mEndT = 1f;
		return spline;
	}

}
