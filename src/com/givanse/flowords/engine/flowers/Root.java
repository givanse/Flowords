package com.givanse.flowords.engine.flowers;

import java.util.Vector;

/**
 * Root element consists of splines for actual root and branch elements.
 */
class Root {

	private static final int BRANCHES_TOTAL = 5;
	private static final int SPLINES_TOTAL = 5;
	
	private int rootIndex;
	private final Branch[] branches = new Branch[Root.BRANCHES_TOTAL];
	private final Spline[] splines = new Spline[Root.SPLINES_TOTAL];
	private long startTime, duration;

	/**
	 * Default constructor.
	 */
	public Root() {
		for (int i = 0; i < Root.BRANCHES_TOTAL; ++i) {
			this.splines[i] = new Spline();
			this.branches[i] = new Branch();
		}
	}

	/**
	 * Returns branch for current root spline.
	 */
	public Branch getCurrentBranch() {
		return this.branches[this.rootIndex - 1];
	}

	/**
	 * Returns next spline structure.
	 */
	public Spline getNextSpline() {
		this.branches[this.rootIndex].reset();
		return this.splines[this.rootIndex++];
	}

	/**
	 * Getter for spline and point structs for rendering.
	 */
	public void setForRenderSplinesKnots(Vector<Spline> splinesArg,
			                             Vector<Knot> pointsArg, 
			                             float startT, float endT, 
			                             float zoomLvl) {
		
		for (int i = 0; i < this.rootIndex; ++i) {
			Spline spline = this.splines[i];
			if (startT != 0f || endT != 1f) {
				float localStartT = (float) i / this.rootIndex;
				float localEndT = (float) (i + 1) / this.rootIndex;
				spline.setStart(Math.min(Math.max((startT - localStartT) / 
						                  (localEndT - localStartT), 0f), 1f));
				spline.setEnd(Math.min(Math.max((endT - localStartT) / 
						                (localEndT - localStartT), 0f), 1f));
			} else {
				spline.setStart(0f);
				spline.setEnd(1f);
			}

			if (spline.getStart() != spline.getEnd()) {
				splinesArg.add(spline);
				this.branches[i].setForRenderSplinesKnots(splinesArg, pointsArg, 
						                          spline.getStart(), 
						                          spline.getEnd(), zoomLvl);
			}
		}
	}

	/**
	 * Resets root element to its initial state.
	 */
	public void reset() {
		this.rootIndex = 0;
		this.startTime = this.duration = 0;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	
	public void setDuration(long duration) {
		this.duration = duration;
	}
	
	public long getStartTime() {
		return this.startTime;
	}
	
	public long getDuration() {
		return this.duration;
	}
}
