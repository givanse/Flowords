package fi.harism.wallpaper.ornament;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.PointF;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.SystemClock;
import android.widget.Toast;

public final class OrnamentRenderer implements GLSurfaceView.Renderer {

	private FloatBuffer mBackgroundColors;
	private Context mContext;
	private PointF mOffset = new PointF(), mOffsetScroll = new PointF();
	private PointF mOffsetSrc = new PointF(), mOffsetDst = new PointF();
	private long mOffsetTime;
	private OrnamentFbo mOrnamentFbo = new OrnamentFbo();
	private OrnamentPlants mOrnamentPlants = new OrnamentPlants();
	private ByteBuffer mScreenVertices;

	// Shader for rendering background gradient.
	private final OrnamentShader mShaderBackground = new OrnamentShader();
	// Shader for copying offscreen texture on screen.
	private final OrnamentShader mShaderCopy = new OrnamentShader();
	// Surface/screen dimensions.
	private int mWidth, mHeight;

	public OrnamentRenderer(Context context) {
		mContext = context;

		// Create screen coordinates buffer.
		final byte SCREEN_COORDS[] = { -1, 1, -1, -1, 1, 1, 1, -1 };
		mScreenVertices = ByteBuffer.allocateDirect(2 * 4);
		mScreenVertices.put(SCREEN_COORDS).position(0);

		// Create background color float buffer.
		ByteBuffer bBuf = ByteBuffer.allocateDirect(3 * 4 * 4);
		mBackgroundColors = bBuf.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mBackgroundColors.put(OrnamentConstants.COLOR_BG_TOP)
				.put(OrnamentConstants.COLOR_BG_BOTTOM)
				.put(OrnamentConstants.COLOR_BG_TOP)
				.put(OrnamentConstants.COLOR_BG_BOTTOM).position(0);
	}

	@Override
	public void onDrawFrame(GL10 unused) {
		long time = SystemClock.uptimeMillis();
		if (time - mOffsetTime > 5000) {
			mOffsetTime = time;
			mOffsetSrc.set(mOffsetDst);
			OrnamentUtils.rand(mOffsetDst, -1f, -1f, 1f, 1f);
		}
		float t = (float) (time - mOffsetTime) / 5000;
		t = t * t * (3 - 2 * t);
		mOffset.x = mOffsetScroll.x + mOffsetSrc.x + t
				* (mOffsetDst.x - mOffsetSrc.x);
		mOffset.y = mOffsetScroll.y + mOffsetSrc.y + t
				* (mOffsetDst.y - mOffsetSrc.y);

		// Disable unneeded rendering flags.
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		GLES20.glDisable(GLES20.GL_BLEND);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);

		// Set render target to fbo.
		mOrnamentFbo.bind();
		mOrnamentFbo.bindTexture(0);

		// Render background gradient.
		mShaderBackground.useProgram();
		int uOffset = mShaderBackground.getHandle("uOffset");
		int aPosition = mShaderBackground.getHandle("aPosition");
		int aColor = mShaderBackground.getHandle("aColor");

		GLES20.glUniform2f(uOffset, mOffset.x, mOffset.y);
		GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_BYTE, false, 0,
				mScreenVertices);
		GLES20.glEnableVertexAttribArray(aPosition);
		GLES20.glVertexAttribPointer(aColor, 3, GLES20.GL_FLOAT, false, 0,
				mBackgroundColors);
		GLES20.glEnableVertexAttribArray(aColor);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

		// Render scene.
		mOrnamentPlants.onDrawFrame(mOffset);

		// Copy FBO to screen buffer.
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glViewport(0, 0, mWidth, mHeight);
		mShaderCopy.useProgram();
		int uBrightness = mShaderCopy.getHandle("uBrightness");
		aPosition = mShaderCopy.getHandle("aPosition");
		GLES20.glUniform1f(uBrightness, 1f);
		GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_BYTE, false, 0,
				mScreenVertices);
		GLES20.glEnableVertexAttribArray(aPosition);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOrnamentFbo.getTexture(0));
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		mWidth = width;
		mHeight = height;
		mOrnamentFbo.init(mWidth, mHeight, 1);
		mOrnamentPlants.onSurfaceChanged(mOrnamentFbo.getWidth(),
				mOrnamentFbo.getHeight());
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		boolean[] retVal = new boolean[1];
		GLES20.glGetBooleanv(GLES20.GL_SHADER_COMPILER, retVal, 0);
		if (retVal[0] == false) {
			Handler handler = new Handler(mContext.getMainLooper());
			handler.post(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(mContext, R.string.error_shader_compiler,
							Toast.LENGTH_LONG).show();
				}
			});
		} else {
			mShaderCopy.setProgram(mContext.getString(R.string.shader_copy_vs),
					mContext.getString(R.string.shader_copy_fs));
			mShaderBackground.setProgram(
					mContext.getString(R.string.shader_background_vs),
					mContext.getString(R.string.shader_background_fs));
			mOrnamentPlants.onSurfaceCreated(mContext);
		}
	}

	public void setOffset(float xOffset, float yOffset) {
		mOffsetScroll.set(xOffset * 2f, yOffset * 2f);
	}

}
