package com.givanse.flowords.engine.flowers;

import java.util.Vector;

/**
 * Root element for handling root related data. Root element consists of
 * splines for actual root and branch elements.
 */
class Root {

	private final Branch[] branches = new Branch[5];
	private int mRootSplineCount;
	private final Spline[] splines = new Spline[5];
	private long mStartTime, mDuration;

	/**
	 * Default constructor.
	 */
	public Root() {
		for (int i = 0; i < 5; ++i) {
			this.splines[i] = new Spline();
			this.branches[i] = new Branch();
		}
	}

	/**
	 * Returns branch for current root spline.
	 */
	public Branch getCurrentBranch() {
		return this.branches[this.mRootSplineCount - 1];
	}

	/**
	 * Returns next spline structure.
	 */
	public Spline getNextSpline() {
		this.branches[this.mRootSplineCount].reset();
		return this.splines[this.mRootSplineCount++];
	}

	/**
	 * Getter for spline and point structs for rendering. Values startT and
	 * endT are between [0, 1] plus additionally startT <= endT.
	 */
	public void getRenderStructs(Vector<Spline> splinesArg,
			                     Vector<Point> pointsArg, float startT, 
			                     float endT, float zoomLvl) {
		for (int i = 0; i < this.mRootSplineCount; ++i) {
			Spline spline = this.splines[i];
			if (startT != 0f || endT != 1f) {
				float localStartT = (float) i / this.mRootSplineCount;
				float localEndT = (float) (i + 1) / this.mRootSplineCount;
				spline.mStartT = Math.min(Math.max((startT - localStartT) / 
						                  (localEndT - localStartT), 0f), 1f);
				spline.mEndT = Math.min(Math.max((endT - localStartT) / 
						                (localEndT - localStartT), 0f), 1f);
			} else {
				spline.mStartT = 0f;
				spline.mEndT = 1f;
			}

			if (spline.mStartT != spline.mEndT) {
				splinesArg.add(spline);
				this.branches[i].getRenderStructs(splinesArg, pointsArg, 
						                          spline.mStartT, 
						                          spline.mEndT, zoomLvl);
			}
		}
	}

	/**
	 * Resets root element to its initial state.
	 */
	public void reset() {
		this.mRootSplineCount = 0;
		this.mStartTime = this.mDuration = 0;
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
