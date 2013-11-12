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

	private float[] color = new float[4];
	private int dirIndex;
	private int rootsCount;
	private final Vector<Root> roots = new Vector<Root>();
	private final PointF currentPosition = new PointF();
	private final PointF targetPosition = new PointF();

	/**
	 * Default constructor.
	 */
	public Flower() {
		for (int i = 0; i < Flower.ROOT_ELEMENT_COUNT; ++i) {
			roots.add(new Root());
		}
	}

	/**
	 * Returns last active root element. If there are none, returns next
	 * root element.
	 */
	public Root getLastRootElement() {
		if (rootsCount == 0) {
			return getNextRootElement();
		} else {
			return roots.get(rootsCount - 1);
		}
	}

	/**
	 * Returns next root element.
	 */
	public Root getNextRootElement() {
		Root element;
		if (rootsCount < roots.size()) {
			element = roots.get(rootsCount++);
		} else {
			element = roots.remove(0);
			roots.add(element);
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
		Root lastElement = roots.get(rootsCount - 1);
		float t = (float) (time - lastElement.getStartTime()) / 
				  lastElement.getDuration();
		for (int i = 0; i < rootsCount; ++i) {
			Root element = roots.get(i);
			if (i == rootsCount - 1) {
				element.getRenderStructs(splines, points, 0f, t, zoomLvl);
			} else if (i == 0 && rootsCount == roots.size()) {
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
		rootsCount = 0;
		setDirIndex(0);
		currentPosition.set(0, 0);
	}

	public PointF getTargetPosition() {
		return targetPosition;
	}

	public float[] getColor() {
		return color;
	}

	public void setColor(float[] color) {
		this.color = color;
	}

	public int getDirIndex() {
		return dirIndex;
	}

	public void setDirIndex(int dirIndex) {
		this.dirIndex = dirIndex;
	}

	public PointF getCurrentPosition() {
		return currentPosition;
	}
}