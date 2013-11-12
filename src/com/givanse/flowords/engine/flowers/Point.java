package com.givanse.flowords.engine.flowers;

import android.graphics.PointF;

class Point {

	private final PointF mPosition = new PointF();
	private float mRotationSin;
	private float mRotationCos;
	private float mScale;
	
	/* PRIVATE METHODS */
	
	private void setRotationCos(float rotation) {
		this.mRotationCos = (float) Math.cos(rotation);
	}
	
	private void setRotationSin(float rotation) {
		this.mRotationSin = (float) Math.sin(rotation);
	}
	
	/* PUBLIC METHODS */
	public float getRotationCos() {
		return mRotationCos;
	}
	
	public void setRandomRotationCos() {
		this.setRotationCos(Util.random(0, (float) (Math.PI * 2)));
	}
	
	public float getRotationSin() {
		return mRotationSin;
	}
	
	public void setRandomRotationSin() {
		this.setRotationSin(Util.random(0, (float) (Math.PI * 2)));
	}
	
	public float getScale() {
		return mScale;
	}
	
	public void setScale(float mScale) {
		this.mScale = mScale;
	}

	public PointF getPosition() {
		return mPosition;
	}
	
	public void setPosition(PointF p) {
		this.mPosition.set(p);
	}
	
}
