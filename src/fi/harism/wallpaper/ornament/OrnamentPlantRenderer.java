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
	private int mWidth, mHeight;

	public OrnamentPlantRenderer(int splineSplitCount) {
		mSplineVertexCount = splineSplitCount + 2;
		ByteBuffer bBuffer = ByteBuffer
				.allocateDirect(4 * 4 * mSplineVertexCount);
		mBufferSpline = bBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
		for (int i = 0; i < mSplineVertexCount; ++i) {
			float t = (float) i / (mSplineVertexCount - 1);
			mBufferSpline.put(t).put(1);
			mBufferSpline.put(t).put(-1);
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
		mWidth = width;
		mHeight = height;
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
		int uAspectRatio = mShaderSpline.getHandle("uAspectRatio");
		int aSplinePos = mShaderSpline.getHandle("aSplinePos");

		float aspectX = (float) Math.max(mWidth, mHeight) / mWidth;
		float aspectY = (float) Math.max(mWidth, mHeight) / mHeight;
		GLES20.glUniform2f(uAspectRatio, aspectX, aspectY);
		GLES20.glUniform3fv(uColor, 1, color, 0);
		GLES20.glVertexAttribPointer(aSplinePos, 2, GLES20.GL_FLOAT, false, 0,
				mBufferSpline);
		GLES20.glEnableVertexAttribArray(aSplinePos);

		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

		for (OrnamentSpline spline : splines) {
			GLES20.glUniform2f(uControl0, spline.mCtrlPoints[0].x - offset.x,
					spline.mCtrlPoints[0].y - offset.y);
			GLES20.glUniform2f(uControl1, spline.mCtrlPoints[1].x - offset.x,
					spline.mCtrlPoints[1].y - offset.y);
			GLES20.glUniform2f(uControl2, spline.mCtrlPoints[2].x - offset.x,
					spline.mCtrlPoints[2].y - offset.y);
			GLES20.glUniform2f(uControl3, spline.mCtrlPoints[3].x - offset.x,
					spline.mCtrlPoints[3].y - offset.y);

			GLES20.glUniform2f(uWidth, spline.mWidthStart, spline.mWidthEnd);
			GLES20.glUniform2f(uBounds, spline.mStartT, spline.mEndT);

			int startIdx = (int) (spline.mStartT * mSplineVertexCount) * 2;
			int endIdx = (int) (Math.ceil(spline.mEndT * mSplineVertexCount)) * 2;
			GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, startIdx, endIdx
					- startIdx);
		}

		GLES20.glDisable(GLES20.GL_BLEND);
	}

}
