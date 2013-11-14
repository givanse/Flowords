package com.givanse.flowords.engine.flowers;

import android.graphics.PointF;

/**
 * This spline is made of 4 ctrlPoints (Spline.CTRL_POINTS_TOTAL).
 * 
 * Spline (mathematics): en.wikipedia.org/wiki/Spline_%28mathematics%29
 */
class Spline {
	
	public static final float WIDTH_MAX = Flower.ROOT_WIDTH_MAX;
	public static final float WIDTH_MIN = Flower.ROOT_WIDTH_MIN;
	
	protected static final int CTRL_POINTS_TOTAL = 4;
	protected enum CTRL_POINT_ID {ONE, TWO, THREE, FOUR};
	
	private final PointF ctrlPoints[] = new PointF[Spline.CTRL_POINTS_TOTAL];
	
	/**
	 * 0 <= startT, endT <= 1 
	 * startT < endT
	 */
	private float start = 0f;
	private float end = 1f;
	
	private float widthStart;
	private float widthEnd;

	public Spline() {
		for (int i = 0; i < Spline.CTRL_POINTS_TOTAL; i++) {
			this.ctrlPoints[i] = new PointF();
		}
	}

	public float getStart() {
		return start;
	}

	// TODO: enforce startT < endT
	public void setStart(float start) {
		if(start < 0f)
			this.start = 0f;
		
		if(start > 1f)
			this.start = 1f;
		
		this.start = start;
	}

	public float getEnd() {
		return end;
	}

	// TODO: enforce startT < endT
	public void setEnd(float end) {
		if(end < 0f)
			this.end = 0f;
		
		if(end > 1f)
			this.end = 1f;
		
		this.end = end;
	}

	public float getWidthStart() {
		return widthStart;
	}

	public void setWidthStart(float widthStart) {
		this.widthStart = widthStart;
	}

	public float getWidthEnd() {
		return widthEnd;
	}

	public void setWidthEnd(float widthEnd) {
		this.widthEnd = widthEnd;
	}
	
	public PointF getCtrlPoint(int pointId) {
		return this.ctrlPoints[pointId];
	}
	
	public PointF getCtrlPoint(CTRL_POINT_ID pointId) {
		return this.ctrlPoints[pointId.ordinal()];
	}
	

	/**
	 * Sets spline to straight line between (start, start + length * dir).
	 */
	public void setStraight(PointF start, PointF direction, float length) {
		for (int i = 0; i < Spline.CTRL_POINTS_TOTAL; ++i) {
			float t = (i * length) / 3; // TODO: magic number
			PointF ctrlPoint = this.ctrlPoints[i];
			ctrlPoint.set(start);
			ctrlPoint.offset(direction.x * t, direction.y * t);
		}
	}
	
	/**
	 * Update the control points positions, curve like, based on 
	 * the given parameters.
	 */
	public void curveCtrlPoints(PointF startPosArg, PointF dir, float length, 
			                    PointF normalDir, boolean hasStraightEnd) {

		// TODO: Bezier curve circle estimation.
		final float normalFactor = 0.27614237491f;  /* 2 * (sqrt(2) - 1) / 3 */
		final float normalLen = length * normalFactor;

		// Initially set all control knots to startPos.
		for (CTRL_POINT_ID ctrlPointId : CTRL_POINT_ID.values()) {
			this.getCtrlPoint(ctrlPointId).set(startPosArg);
		}
		// Move second control point into target direction plus same length in
		// normal direction.
		this.getCtrlPoint(CTRL_POINT_ID.TWO)
		    .offset((dir.x + normalDir.x) * normalLen, 
		    		(dir.y + normalDir.y) * normalLen);
		// Move third control point to (startPos + (length - normalLen) * dir).
		this.getCtrlPoint(CTRL_POINT_ID.THREE)
			.offset(dir.x * (length - normalLen), 
					dir.y * (length - normalLen));
		// If straight end is not requested move third control point among
		// normal.
		if (!hasStraightEnd) {
			this.getCtrlPoint(CTRL_POINT_ID.THREE)
				.offset(normalDir.x * normalLen, normalDir.y * normalLen);
		}
		// Set last control point to (startPos + dir * length).
		this.getCtrlPoint(CTRL_POINT_ID.FOUR)
		    .offset(dir.x * length, dir.y * length);
	}
}
