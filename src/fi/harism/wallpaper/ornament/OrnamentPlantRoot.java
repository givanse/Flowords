package fi.harism.wallpaper.ornament;

import java.util.Vector;

import android.graphics.PointF;

public final class OrnamentPlantRoot {

	private int mRootSplineCount;
	private Vector<OrnamentSpline> mRootSplines = new Vector<OrnamentSpline>();
	private long mStartTime, mDuration;

	public OrnamentPlantRoot() {
		for (int i = 0; i < 11; ++i) {
			OrnamentSpline spline = new OrnamentSpline();
			spline.mWidthStart = spline.mWidthEnd = .03f;
			mRootSplines.add(spline);
		}
	}

	private OrnamentSpline addLoop(OrnamentSpline root, PointF dir,
			PointF normal, int side) {
		PointF start = root.mCtrlPoints[3];
		float dirLen = dir.length();
		float normalX = normal.x * side * dirLen;
		float normalY = normal.y * side * dirLen;
		float xTop = start.x + normalX;
		float yTop = start.y + normalY;
		float xRight = start.x + (normalX / 2) + (dir.x / 2);
		float yRight = start.y + (normalY / 2) + (dir.y / 2);
		float xLeft = start.x + (normalX / 2) - (dir.x / 2);
		float yLeft = start.y + (normalY / 2) - (dir.y / 2);

		OrnamentSpline prev = addSpline(root, xRight, yRight);
		prev = addSpline(prev, xTop, yTop);
		prev = addSpline(prev, xLeft, yLeft);
		prev = addSpline(prev, start.x, start.y);
		return prev;
	}

	private OrnamentSpline addSpline(OrnamentSpline previous, float x3, float y3) {
		OrnamentSpline spline = mRootSplines.get(mRootSplineCount++);
		spline.mCtrlPoints[0].set(previous.mCtrlPoints[1]);
		spline.mCtrlPoints[1].set(previous.mCtrlPoints[2]);
		spline.mCtrlPoints[2].set(previous.mCtrlPoints[3]);
		spline.mCtrlPoints[3].set(x3, y3);
		return spline;
	}

	public long getDuration() {
		return mDuration;
	}

	public OrnamentSpline getLastSpline() {
		return mRootSplines.get(mRootSplineCount - 1);
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

	public void setDuration(long duration) {
		mDuration = duration;
	}

	public void setStartTime(long startTime) {
		mStartTime = startTime;
	}

	public void setTarget(OrnamentSpline previous, PointF dir, PointF normal) {
		mRootSplineCount = 0;
		final PointF start = previous.mCtrlPoints[3];
		final float t[] = { 0.33f, 0.66f };
		for (int i = 0; i < 2; ++i) {
			float randLen = OrnamentUtils.randF(-.05f, .05f);
			float x = (start.x + t[i] * dir.x) + (normal.x * randLen);
			float y = (start.y + t[i] * dir.y) + (normal.y * randLen);
			previous = addSpline(previous, x, y);

			int loopRand = OrnamentUtils.randI(-1, 6);
			if (loopRand == -1 || loopRand == 1) {
				previous = addLoop(previous, dir, normal, loopRand);
			}
		}
		addSpline(previous, start.x + dir.x, start.y + dir.y);
	}

}
