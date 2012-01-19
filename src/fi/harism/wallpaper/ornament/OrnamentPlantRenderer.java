package fi.harism.wallpaper.ornament;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Vector;

import android.content.Context;
import android.graphics.PointF;
import android.opengl.GLES20;
import android.os.SystemClock;

public class OrnamentPlantRenderer {

	private FloatBuffer mBufferSpline;
	private OrnamentShader mShaderSpline = new OrnamentShader();

	private Vector<OrnamentSpline> mSplines = new Vector<OrnamentSpline>();
	private int mSplineVertexCount;

	public OrnamentPlantRenderer(int splineSplitCount) {
		mSplineVertexCount = splineSplitCount + 2;
		ByteBuffer bBuffer = ByteBuffer
				.allocateDirect(2 * 4 * 4 * mSplineVertexCount);
		mBufferSpline = bBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
		for (int i = 0; i < mSplineVertexCount; ++i) {
			float t1 = (float) i / (mSplineVertexCount - 1);
			float t2 = t1 * t1;
			float t3 = t1 * t2;
			mBufferSpline.put(1).put(t1).put(t2).put(t3).put(-1).put(t1)
					.put(t2).put(t3);
		}
		mBufferSpline.position(0);
	}

	public void onDrawFrame(OrnamentPlant plant, PointF offset) {
		float[] color = plant.getColor();
		long renderTime = SystemClock.uptimeMillis();
		mSplines.clear();
		plant.getSplines(mSplines, renderTime);
		renderSplines(mSplines, color, offset);
	}

	public void onSurfaceChanged(int width, int height) {
	}

	public void onSurfaceCreated(Context context) {
		mShaderSpline.setProgram(context.getString(R.string.shader_spline_vs),
				context.getString(R.string.shader_spline_fs));
	}

	public void renderSplines(Vector<OrnamentSpline> splines, float[] color,
			PointF offset) {
		mShaderSpline.useProgram();
		int uControl0 = mShaderSpline.getHandle("uControl0");
		int uControl1 = mShaderSpline.getHandle("uControl1");
		int uControl2 = mShaderSpline.getHandle("uControl2");
		int uControl3 = mShaderSpline.getHandle("uControl3");
		int uWidth = mShaderSpline.getHandle("uWidth");
		int uBounds = mShaderSpline.getHandle("uBounds");
		int uColor = mShaderSpline.getHandle("uColor");
		int aSplinePosition = mShaderSpline.getHandle("aSplinePosition");

		GLES20.glUniform3fv(uColor, 1, color, 0);
		GLES20.glVertexAttribPointer(aSplinePosition, 4, GLES20.GL_FLOAT,
				false, 0, mBufferSpline);
		GLES20.glEnableVertexAttribArray(aSplinePosition);

		for (OrnamentSpline spline : splines) {
			final float Px0 = spline.mCtrlPoints[0].x - offset.x;
			final float Py0 = spline.mCtrlPoints[0].y - offset.y;
			final float Px1 = spline.mCtrlPoints[1].x - offset.x;
			final float Py1 = spline.mCtrlPoints[1].y - offset.y;
			final float Px2 = spline.mCtrlPoints[2].x - offset.x;
			final float Py2 = spline.mCtrlPoints[2].y - offset.y;
			final float Px3 = spline.mCtrlPoints[3].x - offset.x;
			final float Py3 = spline.mCtrlPoints[3].y - offset.y;

			GLES20.glUniform2f(uControl0, 2 * Px1, 2 * Py1);
			GLES20.glUniform2f(uControl1, -Px0 + Px2, -Py0 + Py2);
			GLES20.glUniform2f(uControl2, 2 * Px0 - 5 * Px1 + 4 * Px2 - Px3, 2
					* Py0 - 5 * Py1 + 4 * Py2 - Py3);
			GLES20.glUniform2f(uControl3, -Px0 + 3 * Px1 - 3 * Px2 + Px3, -Py0
					+ 3 * Py1 - 3 * Py2 + Py3);
			GLES20.glUniform2f(uWidth, spline.mWidthStart, spline.mWidthEnd);
			GLES20.glUniform2f(uBounds, spline.mStartT, spline.mEndT);

			int startIdx = (int) (spline.mStartT * mSplineVertexCount) * 2;
			int endIdx = (int) (Math.ceil(spline.mEndT * mSplineVertexCount)) * 2;
			GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, startIdx, endIdx
					- startIdx);
		}

	}

}
