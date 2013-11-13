package com.givanse.flowords.engine.flowers;

import com.givanse.flowords.engine.Util;

import android.graphics.PointF;

class Knot {

	private final PointF position = new PointF();
	private float rotationSin;
	private float rotationCos;
	private float scale;
	
	/* PRIVATE METHODS */
	
	private void setRotationCos(float rotation) {
		this.rotationCos = (float) Math.cos(rotation);
	}
	
	private void setRotationSin(float rotation) {
		this.rotationSin = (float) Math.sin(rotation);
	}
	
	/* PUBLIC METHODS */
	public float getRotationCos() {
		return rotationCos;
	}
	
	public void setRandomRotationCos() {
		this.setRotationCos(Util.random(0, (float) (Math.PI * 2)));
	}
	
	public float getRotationSin() {
		return rotationSin;
	}
	
	public void setRandomRotationSin() {
		this.setRotationSin(Util.random(0, (float) (Math.PI * 2)));
	}
	
	public float getScale() {
		return scale;
	}
	
	public void setScale(float mScale) {
		this.scale = mScale;
	}

	public PointF getPosition() {
		return position;
	}
	
	public void setPosition(PointF p) {
		this.position.set(p);
	}
	
}
