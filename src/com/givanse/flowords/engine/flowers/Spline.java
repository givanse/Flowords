package com.givanse.flowords.engine.flowers;

import android.graphics.PointF;

class Spline {
	
	public static final float WIDTH_MAX = Flower.ROOT_WIDTH_MAX;
	public static final float WIDTH_MIN = Flower.ROOT_WIDTH_MIN;
	
	public final PointF mPoints[] = new PointF[4];
	public float mStartT = 0f, mEndT = 1f;
	public float mWidthStart, mWidthEnd;

	public Spline() {
		for (int i = 0; i < mPoints.length; ++i) {
			mPoints[i] = new PointF();
		}
	}
}
