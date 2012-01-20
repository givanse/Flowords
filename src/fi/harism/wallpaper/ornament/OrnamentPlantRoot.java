package fi.harism.wallpaper.ornament;

import java.util.Vector;

import android.graphics.PointF;

public final class OrnamentPlantRoot {

	private int mRootSplineCount;
	private Vector<OrnamentSpline> mRootSplines = new Vector<OrnamentSpline>();
	private long mStartTime, mDuration;

	public OrnamentPlantRoot() {
		for (int i = 0; i < 5; ++i) {
			OrnamentSpline spline = new OrnamentSpline();
			spline.mWidthStart = spline.mWidthEnd = OrnamentConstants.SPLINE_ROOT_WIDTH;
			mRootSplines.add(spline);
		}
	}

	public void addArc(PointF start, PointF dir, float length, PointF normal,
			float normalPos1, float normalPos2, boolean flatEnd) {
		OrnamentSpline spline = mRootSplines.get(mRootSplineCount++);
		spline.mCtrlPoints[0].set(start);

		float Px = start.x + dir.x * normalPos1 + normal.x * normalPos1;
		float Py = start.y + dir.y * normalPos1 + normal.y * normalPos1;
		float Qx = start.x + dir.x * normalPos2;
		float Qy = start.y + dir.y * normalPos2;

		if (!flatEnd) {
			Qx += normal.x * (length - normalPos2);
			Qy += normal.y * (length - normalPos2);
		}

		spline.mCtrlPoints[1].set(Px, Py);
		spline.mCtrlPoints[2].set(Qx, Qy);

		start.offset(dir.x * length, dir.y * length);
		spline.mCtrlPoints[3].set(start);
	}

	public void addLine(PointF start, PointF dir, float length, PointF normal,
			int count) {
		float randLen = 0, offsetX = 0, offsetY = 0;
		for (int i = 0; i < count; ++i) {
			OrnamentSpline spline = mRootSplines.get(mRootSplineCount++);
			for (int j = 0; j < 4; ++j) {
				PointF P = spline.mCtrlPoints[j];
				P.set(start);
				P.offset(length * dir.x * (j / 3f), length * dir.y * (j / 3f));
				P.offset(offsetX, offsetY);

				if (j == 1) {
					if (i < count - 1) {
						randLen = OrnamentUtils.randF(-.1f, .1f);
						offsetX = normal.x * randLen;
						offsetY = normal.y * randLen;
					} else {
						randLen = offsetX = offsetY = 0;
					}
				}
			}
			start.set(spline.mCtrlPoints[3]);
			start.offset(-offsetX, -offsetY);
		}
	}

	public long getDuration() {
		return mDuration;
	}

	public OrnamentSpline getLastSpline() {
		return mRootSplines.get(mRootSplineCount - 1);
	}

	public int getSplineCount() {
		return mRootSplineCount;
	}

	public void getSplines(Vector<OrnamentSpline> splines, float t1, float t2) {
		for (int i = 0; i < mRootSplineCount; ++i) {
			float startT = (float) i / mRootSplineCount;
			float endT = (float) (i + 1) / mRootSplineCount;
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

	public void reset() {
		mRootSplineCount = 0;
	}

	public void setDuration(long duration) {
		mDuration = duration;
	}

	public void setStartTime(long startTime) {
		mStartTime = startTime;
	}

}
