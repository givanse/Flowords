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

		public long getStartTime() {
			return mStartTime;
		}
		
		public long getDuration() {
			return mDuration;
		}

		public OrnamentSpline getLastSpline() {
			return mRootSplines.lastElement();
		}

		public void init(OrnamentSpline previous, float targetX, float targetY) {
			PointF P0, P1, P2;
			if (previous == null) {
				P0 = P1 = P2 = new PointF(-1, -1);
			} else {
				P0 = previous.mCtrlPoints[1];
				P1 = previous.mCtrlPoints[2];
				P2 = previous.mCtrlPoints[3];
			}

			float dirX = targetX - P2.x;
			float dirY = targetY - P2.y;
			float normalX = -dirY;
			float normalY = dirX;
			double normalLen = Math.sqrt(normalX * normalX + normalY * normalY);
			normalX = (float) (normalX / normalLen);
			normalY = (float) (normalY / normalLen);
			double dirLen = Math.sqrt(dirX * dirX + dirY * dirY);
			dirX = (float) (dirX / dirLen);
			dirY = (float) (dirY / dirLen);

			float t1 = 0.3f;
			float t2 = 0.7f;

			PointF P3 = new PointF(P2.x + t1 * dirX, P2.y + t1 * dirY);
			PointF P4 = new PointF(P2.x + t2 * dirX, P2.y + t2 * dirY);
			PointF P5 = new PointF(P2.x + dirX, P2.y + dirY);

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

		public void getSplines(Vector<OrnamentSpline> splines, float t1, float t2) {
			for (int i = 0; i < mRootSplines.size(); ++i) {
				float startT = (float)i / mRootSplines.size();
				float endT = (float)(i+1) / mRootSplines.size();
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
	
}
