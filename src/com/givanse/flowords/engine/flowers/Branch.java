package com.givanse.flowords.engine.flowers;

import java.util.Vector;

/**
 * Branch element for handling branch data. Namely splines and points that
 * create a branch.
 */
class Branch {

	public static final float WIDTH_MAX = 0.1f;
    public static final float WIDTH_MIN = 0.05f;
    
	public int mBranchPointCount;
	private final Point[] mBranchPoints = new Point[2];
	public int mBranchSplineCount;
	private final Spline[] mBranchSplines = new Spline[3];

	/**
	 * Default constructor.
	 */
	public Branch() {
		for (int i = 0; i < mBranchSplines.length; ++i) {
			mBranchSplines[i] = new Spline();
		}
		for (int i = 0; i < mBranchPoints.length; ++i) {
			mBranchPoints[i] = new Point();
		}
	}

	/**
	 * Returns next point structure.
	 */
	public Point getNextPoint() {
		return mBranchPoints[mBranchPointCount++];
	}

	/**
	 * Returns next splien structure.
	 */
	public Spline getNextSpline() {
		return mBranchSplines[mBranchSplineCount++];
	}

	/**
	 * Getter for splines and points this branch holds. Parameters startT
	 * and endT are values between [0, 1] plus additionally startT < endT.
	 */
	public void getRenderStructs(Vector<Spline> splines,
			                     Vector<Point> points, 
			                     float startT, float endT, float zoomLvl) {
		// First iterate over splines.
		for (int i = 0; i < mBranchSplineCount; ++i) {
			Spline spline = mBranchSplines[i];
			switch (i) {
			case 0:
				spline.mStartT = startT > 0f ? Math.min(startT * 2, 1f) : 
					                           0f;
				spline.mEndT = endT < 1f ? Math.min(endT * 2, 1f) : 1f;
				break;
			default:
				spline.mStartT = startT > 0f ? 
						         Math.max((startT - .5f) * 2, 0f) : 
						         0f;
				spline.mEndT = endT < 1f ? 
						       Math.max((endT - .5f) * 2, 0f) : 
						       1f;
				break;
			}
			splines.add(spline);
		}
		// Scale factor is calculated from current zoom level.
		// TODO: scaling might be best done during rendering.
		final float PT_SCALE_FACTOR = 
				                   Flower.POINT_SCALE_MIN +
				                   zoomLvl *
				                   (Flower.POINT_SCALE_MAX -
				                   Flower.POINT_SCALE_MIN);
		// Iterate over points.
		for (int i = 0; i < mBranchPointCount; ++i) {
			Point point = mBranchPoints[i];
			float scale = endT - startT;
			if (mBranchSplineCount == 1) {
				scale = scale < 1f ? Math.max((scale - .5f) * 2, 0f) : 1f;
			}
			point.mScale = scale * PT_SCALE_FACTOR;
			points.add(point);
		}
	}

	/**
	 * Resets branch to initial state.
	 */
	public void reset() {
		mBranchSplineCount = mBranchPointCount = 0;
	}
	
}
