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

public class OrnamentRenderer implements GLSurfaceView.Renderer {

	private Context mContext;
	// Shader for copying offscreen texture on screen.
	private final OrnamentShader mShaderCopy = new OrnamentShader();
	// Shader for rendering background gradient.
	private final OrnamentShader mShaderFill = new OrnamentShader();
	// Surface/screen dimensions.
	private int mWidth, mHeight;
	private OrnamentFbo mOrnamentFbo = new OrnamentFbo();
	private ByteBuffer mScreenVertices;
	private FloatBuffer mBackgroundColors;
	private OrnamentPlantRenderer mOrnamentAnimation = new OrnamentPlantRenderer(OrnamentConstants.SPLINE_SPLIT_COUNT);
	private OrnamentPlant mOrnamentPlant1 = new OrnamentPlant(OrnamentConstants.COLOR_PLANT1);
	private OrnamentPlant mOrnamentPlant2 = new OrnamentPlant(OrnamentConstants.COLOR_PLANT2);
	
	private PointF mOffset = new PointF(), mOffsetScroll = new PointF();
	private PointF mOffsetSrc = new PointF(), mOffsetDst = new PointF();
	private long mOffsetTime;

	public OrnamentRenderer(Context context) {
		mContext = context;

		// Create screen coordinates float buffer.
		final byte SCREEN_COORDS[] = { -1, 1, -1, -1, 1, 1, 1, -1 };
		mScreenVertices = ByteBuffer.allocateDirect(2 * 4);
		mScreenVertices.put(SCREEN_COORDS).position(0);

		// Create background color float buffer.
		final float BG_COLOR_TOP[] = { .8f, .6f, .2f };
		final float BG_COLOR_BOTTOM[] = { .7f, .7f, .1f };
		ByteBuffer bBuf = ByteBuffer.allocateDirect(3 * 4 * 4);
		mBackgroundColors = bBuf.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mBackgroundColors.put(BG_COLOR_TOP).put(BG_COLOR_BOTTOM)
				.put(BG_COLOR_TOP).put(BG_COLOR_BOTTOM).position(0);
	}

	@Override
	public void onDrawFrame(GL10 unused) {
		long time = SystemClock.uptimeMillis();
		if (time - mOffsetTime > 5000) {
			mOffsetTime = time;
			mOffsetSrc.set(mOffsetDst);
			OrnamentUtils.rand(mOffsetDst, -.2f, -.2f, .2f, .2f);
		}
		float t = (float)(time - mOffsetTime) / 5000;
		t = t * t * (3 - 2 * t);
		mOffset.x = mOffsetScroll.x + mOffsetSrc.x + t * (mOffsetDst.x - mOffsetSrc.x);
		mOffset.y = mOffsetScroll.y + mOffsetSrc.y + t * (mOffsetDst.y - mOffsetSrc.y);		
		
		// Disable unneeded rendering flags.
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		GLES20.glDisable(GLES20.GL_BLEND);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);

		// Set render target to fbo.
		mOrnamentFbo.bind();
		mOrnamentFbo.bindTexture(0);

		// Render background gradient.
		mShaderFill.useProgram();
		int positionAttribLocation = mShaderFill.getHandle("aPosition");
		GLES20.glVertexAttribPointer(positionAttribLocation, 2, GLES20.GL_BYTE,
				false, 0, mScreenVertices);
		GLES20.glEnableVertexAttribArray(positionAttribLocation);
		int colorAttribLocation = mShaderFill.getHandle("aColor");
		GLES20.glVertexAttribPointer(colorAttribLocation, 3, GLES20.GL_FLOAT,
				false, 0, mBackgroundColors);
		GLES20.glEnableVertexAttribArray(colorAttribLocation);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

		// Render scene.
		mOrnamentAnimation.onDrawFrame(mOrnamentPlant1, mOffset);
		mOrnamentAnimation.onDrawFrame(mOrnamentPlant2, mOffset);

		// Copy FBO to screen buffer.
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glViewport(0, 0, mWidth, mHeight);
		mShaderCopy.useProgram();
		int uBrightness = mShaderCopy.getHandle("uBrightness");
		int aPosition = mShaderCopy.getHandle("aPosition");
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
		mOrnamentFbo.init(width, height, 1);
		mOrnamentAnimation.onSurfaceChanged(width, height);
		
		mOrnamentPlant1.reset();
		mOrnamentPlant2.reset();
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
			mShaderFill.setProgram(mContext.getString(R.string.shader_fill_vs),
					mContext.getString(R.string.shader_fill_fs));
			mOrnamentAnimation.onSurfaceCreated(mContext);
		}
	}

	public void setOffset(float xOffset, float yOffset) {
		mOffsetScroll.set(xOffset, yOffset);
	}

}
