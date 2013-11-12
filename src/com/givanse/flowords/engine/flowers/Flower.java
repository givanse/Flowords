package com.givanse.flowords.engine.flowers;

import java.util.Vector;

import android.graphics.PointF;

/**
 * Flower element for handling flower related data. Namely root elements
 * which are used to build a flower.
 */
class Flower {

    public static final float POINT_SCALE_MAX = .24f;
    public static final float POINT_SCALE_MIN = .12f;
    public static final int ROOT_ELEMENT_COUNT = 6;
    public static final float ROOT_WIDTH_MAX = 0.12f;
    public static final float ROOT_WIDTH_MIN = 0.06f;

	public float[] mColor = new float[4];
	public int mCurrentDirIndex;
	public final PointF mCurrentPosition = new PointF();
	private int mRootElementCount;
	private final Vector<ElementRoot> mRootElements = new Vector<ElementRoot>();
	public final PointF mTargetPosition = new PointF();

	/**
	 * Default constructor.
	 */
	public Flower() {
		for (int i = 0; i < Flower.ROOT_ELEMENT_COUNT; ++i) {
			mRootElements.add(new ElementRoot());
		}
	}

	/**
	 * Returns last active root element. If there are none, returns next
	 * root element.
	 */
	public ElementRoot getLastRootElement() {
		if (mRootElementCount == 0) {
			return getNextRootElement();
		} else {
			return mRootElements.get(mRootElementCount - 1);
		}
	}

	/**
	 * Returns next root element.
	 */
	public ElementRoot getNextRootElement() {
		ElementRoot element;
		if (mRootElementCount < mRootElements.size()) {
			element = mRootElements.get(mRootElementCount++);
		} else {
			element = mRootElements.remove(0);
			mRootElements.add(element);
		}
		element.reset();
		return element;
	}

	/**
	 * Getter for spline and point structures for rendering. Time is current
	 * rendering time used for deciding which root element is fading in.
	 */
	public void getRenderStructs(Vector<Spline> splines,
			                     Vector<Point> points, long time, float zoomLvl) {
		ElementRoot lastElement = mRootElements.get(mRootElementCount - 1);
		float t = (float) (time - lastElement.getStartTime()) / 
				  lastElement.getDuration();
		for (int i = 0; i < mRootElementCount; ++i) {
			ElementRoot element = mRootElements.get(i);
			if (i == mRootElementCount - 1) {
				element.getRenderStructs(splines, points, 0f, t, zoomLvl);
			} else if (i == 0 && mRootElementCount == mRootElements.size()) {
				element.getRenderStructs(splines, points, t, 1f, zoomLvl);
			} else {
				element.getRenderStructs(splines, points, 0f, 1f, zoomLvl);
			}
		}
	}

	/**
	 * Resets this flower element to its initial state.
	 */
	public void reset() {
		mRootElementCount = 0;
		mCurrentDirIndex = 0;
		mCurrentPosition.set(0, 0);
	}

}