package fi.harism.wallpaper.ornament;

import java.util.Vector;

import android.graphics.PointF;

public final class OrnamentPlantRoot {

	private int mRootSplineCount;
	private Vector<OrnamentSpline> mRootSplines = new Vector<OrnamentSpline>();
	private long mStartTime, mDuration;

	public OrnamentPlantRoot() {
		for (int i = 0; i < 6; ++i) {
			OrnamentSpline spline = new OrnamentSpline();
			spline.mWidthStart = spline.mWidthEnd = .04f;
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

	private boolean intersection(PointF ret, PointF A, PointF B, float Cx,
			float Cy, float Dx, float Dy) {
		float Ax = A.x, Ay = A.y;
		float Bx = B.x, By = B.y;

		if (Ax == Bx && Ay == By || Cx == Dx && Cy == Dy) {
			return false;
		}

		Bx -= Ax;
		By -= Ay;
		Cx -= Ax;
		Cy -= Ay;
		Dx -= Ax;
		Dy -= Ay;

		float distAB = (float) Math.sqrt(Bx * Bx + By * By);
		float cos = Bx / distAB;
		float sin = By / distAB;
		float newX = Cx * cos + Cy * sin;
		Cy = Cy * cos - Cx * sin;
		Cx = newX;
		newX = Dx * cos + Dy * sin;
		Dy = Dy * cos - Dx * sin;
		Dx = newX;

		if (Cy == Dy) {
			return false;
		}

		float ABpos = Dx + (Cx - Dx) * Dy / (Dy - Cy);
		ret.x = Ax + ABpos * cos;
		ret.y = Ay + ABpos * sin;
		return true;
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
		for (int i = 0; i < 2; ++i) {
			OrnamentSpline spline = mRootSplines.get(mRootSplineCount++);
			spline.mCtrlPoints[0].set(previous.mCtrlPoints[3]);
			for (int j = 1; j < 4; ++j) {
				float t = ((float) i / 3) + ((float) j / 9);
				float randLen = OrnamentUtils.randF(-.1f, .1f);
				float x1 = start.x + t * dir.x;
				float y1 = start.y + t * dir.y;
				float x2 = normal.x * randLen;
				float y2 = normal.y * randLen;
				spline.mCtrlPoints[j].set(x1 + x2, y1 + y2);

				if (j == 1) {
					intersection(spline.mCtrlPoints[j],
							previous.mCtrlPoints[2], previous.mCtrlPoints[3],
							x1, y1, x1 + x2, y1 + y2);
				}
			}
			if (i == 1) {
				spline.mCtrlPoints[3].set(start.x + dir.x, start.y + dir.y);
			}
			previous = spline;

			/*
			 * int loopRand = OrnamentUtils.randI(-1, 6); if (loopRand == -1 ||
			 * loopRand == 1) { previous = addLoop(previous, dir, normal,
			 * loopRand); }
			 */
		}
	}

}
