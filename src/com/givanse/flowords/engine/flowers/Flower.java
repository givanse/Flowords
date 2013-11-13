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
    public static final float ROOT_WIDTH_MAX = 0.12f;
    public static final float ROOT_WIDTH_MIN = 0.06f;

    private static final int ROOTS_TOTAL = 6;
    
	private float[] color = new float[4];
	private int dirIndex;
	private int rootsIndex;
	private final Vector<Root> roots = new Vector<Root>();
	private final PointF currentPosition = new PointF();
	private final PointF targetPosition = new PointF();

	/**
	 * Default constructor.
	 */
	public Flower() {
		for (int i = 0; i < Flower.ROOTS_TOTAL; ++i) {
			this.roots.add(new Root());
		}
	}

	/**
	 * Returns last active root element. If there are none, returns next
	 * root element.
	 */
	public Root getLastRootElement() {
		if (rootsIndex == 0) {
			return this.getNextRootElement();
		} else {
			return this.roots.get(this.rootsIndex - 1);
		}
	}

	/**
	 * Returns next root element.
	 */
	public Root getNextRootElement() {
		Root root;
		if (this.rootsIndex < this.roots.size()) {
			root = this.roots.get(this.rootsIndex++);
		} else {
			root = this.roots.remove(0);
			this.roots.add(root);
		}
		root.reset();
		return root;
	}

	/**
	 * Getter for spline and point structures for rendering. Time is current
	 * rendering time used for deciding which root element is fading in.
	 */
	public void setForRenderSplinesKnots(Vector<Spline> splinesArg, 
								 Vector<Knot> knotsArg, 
								 long time, float zoomLvl) {
		Root lastElement = this.roots.get(this.rootsIndex - 1);
		float t = (float) (time - lastElement.getStartTime()) / 
				          lastElement.getDuration();
		for (int i = 0; i < this.rootsIndex; ++i) {
			Root root = this.roots.get(i);
			
			float startT, endT;
			if (i == this.rootsIndex - 1) {
				startT = 0f; endT = t;
			} else if (i == 0 && this.rootsIndex == this.roots.size()) {
				startT = t; endT = 1f;
			} else {
				startT = 0f; endT = 1f;
			}
			root.setForRenderSplinesKnots(splinesArg, knotsArg, 
					                      startT, endT, zoomLvl);
		}
	}

	/**
	 * Resets this flower element to its initial state.
	 */
	public void reset() {
		this.rootsIndex = 0;
		this.setDirIndex(0);
		this.setCurrentPosition(0, 0);
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
	
	public void setCurrentPosition(float x, float y) {
		this.currentPosition.set(x, y);
	}
}