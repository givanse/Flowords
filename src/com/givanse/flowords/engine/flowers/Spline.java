package com.givanse.flowords.engine.flowers;

import android.graphics.PointF;

/**
 * This spline is made of 4 points (Spline.POINTS_TOTAL).
 * 
 * Spline (mathematics): en.wikipedia.org/wiki/Spline_%28mathematics%29
 */
class Spline {
	
	public static final float WIDTH_MAX = Flower.ROOT_WIDTH_MAX;
	public static final float WIDTH_MIN = Flower.ROOT_WIDTH_MIN;
	
	protected static final int POINTS_TOTAL = 4;
	protected enum POINT_ID {FIRST, SECOND, THIRD, FOURTH};
	
	private final PointF points[] = new PointF[Spline.POINTS_TOTAL];
	
	/**
	 * 0 <= startT, endT <= 1 
	 * startT < endT
	 */
	private float start = 0f;
	private float end = 1f;
	
	private float widthStart;
	private float widthEnd;

	public Spline() {
		for (int i = 0; i < Spline.POINTS_TOTAL; i++) {
			this.points[i] = new PointF();
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
	
	public PointF getPoint(int pointId) {
		return this.points[pointId];
	}
	
	public PointF getPoint(POINT_ID pointId) {
		return this.points[pointId.ordinal()];
	}
	

	/**
	 * Sets spline to straight line between (start, start + length * dir).
	 */
	public void setStraight(PointF start, PointF direction, float length) {
		for (int i = 0; i < Spline.POINTS_TOTAL; ++i) {
			float t = (i * length) / 3; // TODO: magic number
			PointF point = this.points[i];
			point.set(start);
			point.offset(direction.x * t, direction.y * t);
		}
	}
	
}
