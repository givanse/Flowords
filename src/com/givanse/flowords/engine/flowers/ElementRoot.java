package com.givanse.flowords.engine.flowers;

import java.util.Vector;

/**
 * Root element for handling root related data. Root element consists of
 * splines for actual root and branch elements.
 */
class ElementRoot {

	private final Branch[] mBranchElements = new Branch[5];
	private int mRootSplineCount;
	private final Spline[] mRootSplines = new Spline[5];
	private long mStartTime, mDuration;

	/**
	 * Default constructor.
	 */
	public ElementRoot() {
		for (int i = 0; i < 5; ++i) {
			mRootSplines[i] = new Spline();
			mBranchElements[i] = new Branch();
		}
	}

	/**
	 * Returns branch for current root spline.
	 */
	public Branch getCurrentBranch() {
		return mBranchElements[mRootSplineCount - 1];
	}

	/**
	 * Returns next spline structure.
	 */
	public Spline getNextSpline() {
		mBranchElements[mRootSplineCount].reset();
		return mRootSplines[mRootSplineCount++];
	}

	/**
	 * Getter for spline and point structs for rendering. Values startT and
	 * endT are between [0, 1] plus additionally startT <= endT.
	 */
	public void getRenderStructs(Vector<Spline> splines,
			                     Vector<Point> points, float startT, 
			                     float endT, float zoomLvl) {
		for (int i = 0; i < mRootSplineCount; ++i) {
			Spline spline = mRootSplines[i];
			if (startT != 0f || endT != 1f) {
				float localStartT = (float) i / mRootSplineCount;
				float localEndT = (float) (i + 1) / mRootSplineCount;
				spline.mStartT = Math.min(Math.max((startT - localStartT) / 
						         (localEndT - localStartT), 0f), 1f);
				spline.mEndT = Math.min(Math.max((endT - localStartT) / 
						       (localEndT - localStartT), 0f), 1f);
			} else {
				spline.mStartT = 0f;
				spline.mEndT = 1f;
			}

			if (spline.mStartT != spline.mEndT) {
				splines.add(spline);
				mBranchElements[i].getRenderStructs(splines, points, 
						                            spline.mStartT, 
						                            spline.mEndT, zoomLvl);
			}
		}
	}

	/**
	 * Resets root element to its initial state.
	 */
	public void reset() {
		mRootSplineCount = 0;
		mStartTime = mDuration = 0;
	}

	public void setStartTime(long startTime) {
		this.mStartTime = startTime;
	}
	
	public void setDuration(long duration) {
		this.mDuration = duration;
	}
	
	public long getStartTime() {
		return this.mStartTime;
	}
	
	public long getDuration() {
		return this.mDuration;
	}
}
