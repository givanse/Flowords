package fi.harism.wallpaper.ornament;

import java.util.Vector;

import android.graphics.PointF;

public final class OrnamentPlantRoot {

	private Vector<OrnamentSpline> mRootSplines = new Vector<OrnamentSpline>();
	private long mStartTime, mDuration;

	public OrnamentPlantRoot(long startTime, long duration) {
		mStartTime = startTime;
		mDuration = duration;
	}

	public long getDuration() {
		return mDuration;
	}

	public OrnamentSpline getLastSpline() {
		return mRootSplines.lastElement();
	}

	public void getSplines(Vector<OrnamentSpline> splines, float t1, float t2) {
		for (int i = 0; i < mRootSplines.size(); ++i) {
			float startT = (float) i / mRootSplines.size();
			float endT = (float) (i + 1) / mRootSplines.size();
			OrnamentSpline spline = mRootSplines.get(i);
			if (startT >= t1 && endT <= t2) {
				spline.mStartT = 0f;
				spline.mEndT = 1f;
				splines.add(spline);
			} else if (startT < t1 && endT > t1) {
				spline.mStartT = (t1 - startT) / (endT - startT);
				spline.mEndT = 1f;
				splines.add(spline);
			} else if (startT < t2 && endT > t2) {
				spline.mStartT = 0f;
				spline.mEndT = (t2 - startT) / (endT - startT);
				splines.add(spline);
			}
		}
	}

	public long getStartTime() {
		return mStartTime;
	}

	public void init(OrnamentSpline previous, PointF dir, PointF normal) {
		PointF P0, P1, P2;
		if (previous == null) {
			P0 = P1 = P2 = new PointF(-1, -1);
		} else {
			P0 = previous.mCtrlPoints[1];
			P1 = previous.mCtrlPoints[2];
			P2 = previous.mCtrlPoints[3];
		}

		float t1 = 0.3f;
		float t2 = 0.7f;

		PointF P3 = new PointF(P2.x + t1 * dir.x, P2.y + t1 * dir.y);
		PointF P4 = new PointF(P2.x + t2 * dir.x, P2.y + t2 * dir.y);
		PointF P5 = new PointF(P2.x + dir.x, P2.y + dir.y);

		float randLen = OrnamentUtils.randF(-.05f, .05f);
		P3.offset(normal.x * randLen, normal.y * randLen);
		randLen = OrnamentUtils.randF(-.05f, .05f);
		P4.offset(normal.x * randLen, normal.y * randLen);

		OrnamentSpline spline = OrnamentUtils.genSpline(P0, P1, P2, P3);
		spline.mWidthStart = spline.mWidthEnd = .03f;
		mRootSplines.add(spline);
		spline = OrnamentUtils.genSpline(P1, P2, P3, P4);
		spline.mWidthStart = spline.mWidthEnd = .03f;
		mRootSplines.add(spline);
		spline = OrnamentUtils.genSpline(P2, P3, P4, P5);
		spline.mWidthStart = spline.mWidthEnd = .03f;
		mRootSplines.add(spline);
	}

}
