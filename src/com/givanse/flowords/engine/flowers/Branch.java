package com.givanse.flowords.engine.flowers;

import java.util.Vector;

/**
 * A Branch is made of Splines and Knots.
 */
class Branch {

	public static final float WIDTH_MAX = 0.1f;
    public static final float WIDTH_MIN = 0.05f;
    
    private static final int KNOTS_TOTAL = 2;
    private static final int SPLINES_TOTAL = 3;
    
	private int knotsIndex;
	private final Knot[] knots = new Knot[Branch.KNOTS_TOTAL];
	
	private int splineCount;
	private final Spline[] splines = new Spline[Branch.SPLINES_TOTAL];

	/**
	 * Default constructor.
	 */
	public Branch() {
		for (int i = 0; i < this.splines.length; ++i) {
			this.splines[i] = new Spline();
		}
		for (int i = 0; i < knots.length; ++i) {
			this.knots[i] = new Knot();
		}
	}

	/**
	 * Returns next the next Knot.
	 */
	public Knot getNextKnot() {
		return this.knots[this.knotsIndex++];
	}

	/**
	 * Returns next the Spline.
	 */
	public Spline getNextSpline() {
		return this.splines[this.splineCount++];
	}

	/**
	 * Getter for splines and knots this branch holds.
	 */
	public void getSplinesKnots(Vector<Spline> splinesArg, 
								Vector<Knot> knotsArg, 
			                    float startT, float endT, float zoomLvl) {
		// First iterate over splines.
		for (int i = 0; i < this.splineCount; ++i) {
			Spline spline = this.splines[i];
			switch (i) {
			case 0: // first spline only
				spline.setStart(startT > 0f ? Math.min(startT * 2, 1f) : 0f);
				spline.setEnd(endT < 1f ? Math.min(endT * 2, 1f) : 1f);
				break;
			default: // every other spline
				spline.setStart(startT > 0f ? 
						        Math.max((startT - .5f) * 2, 0f) : 0f);
				spline.setEnd(endT < 1f ? 
						      Math.max((endT - .5f) * 2, 0f) : 1f);
				break;
			}
			splinesArg.add(spline);
		}
		
		// Scale factor is calculated from current zoom level.
		// TODO: scaling might be best done during rendering.
		final float scaleFACTOR = Flower.POINT_SCALE_MIN +
				                  zoomLvl *
				                  (Flower.POINT_SCALE_MAX - 
				                  Flower.POINT_SCALE_MIN);
		// Iterate over knots.
		for (int i = 0; i < this.knotsIndex; ++i) {
			Knot knot = this.knots[i];
			float scale = endT - startT;
			if (this.splineCount == 1) {
				scale = scale < 1f ? Math.max((scale - .5f) * 2, 0f) : 1f;
			}
			knot.setScale(scale * scaleFACTOR);
			knotsArg.add(knot);
		}
	}

	/**
	 * Resets the Branch to initial state.
	 */
	public void reset() {
		this.splineCount = this.knotsIndex = 0;
	}
	
}
