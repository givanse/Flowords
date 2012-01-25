/*
   Copyright 2012 Harri SmŒtt

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package fi.harism.wallpaper.flowers;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PointF;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.SystemClock;
import android.widget.Toast;

/**
 * Main renderer class.
 */
public final class FlowerRenderer implements GLSurfaceView.Renderer {

	// Buffer for background colors.
	private FloatBuffer mBackgroundColors;
	// Current context.
	private Context mContext;
	// FBO for offscreen rendering.
	private FlowerFbo mFlowerFbo = new FlowerFbo();
	// Actual flower renderer instance.
	private FlowerObjects mFlowerObjects = new FlowerObjects();
	// "Final" calculated offset value.
	private final PointF mOffset = new PointF();
	// Scroll offset value.
	private final PointF mOffsetScroll = new PointF();
	// Additional animated offset source and destination values.
	private PointF mOffsetSrc = new PointF(), mOffsetDst = new PointF();
	// Animated offset time value for iterating between src and dst.
	private long mOffsetTime;
	// Vertex buffer for full scene coordinates.
	private ByteBuffer mScreenVertices;

	// Shader for rendering background gradient.
	private final FlowerShader mShaderBackground = new FlowerShader();
	// Shader for copying offscreen texture on screen.
	private final FlowerShader mShaderCopy = new FlowerShader();
	// Surface/screen dimensions.
	private int mWidth, mHeight;

	/**
	 * Default constructor.
	 */
	public FlowerRenderer(Context context) {
		mContext = context;

		// Create screen coordinates buffer.
		final byte SCREEN_COORDS[] = { -1, 1, -1, -1, 1, 1, 1, -1 };
		mScreenVertices = ByteBuffer.allocateDirect(2 * 4);
		mScreenVertices.put(SCREEN_COORDS).position(0);

		// Create background color float buffer.
		ByteBuffer bBuf = ByteBuffer.allocateDirect(4 * 4 * 4);
		mBackgroundColors = bBuf.order(ByteOrder.nativeOrder()).asFloatBuffer();
	}

	private float[] getColor(int keyId, SharedPreferences prefs) {
		String key = mContext.getString(keyId);
		int value = prefs.getInt(key, Color.CYAN);
		float[] retVal = new float[4];
		retVal[0] = (float) Color.red(value) / 255;
		retVal[1] = (float) Color.green(value) / 255;
		retVal[2] = (float) Color.blue(value) / 255;
		retVal[3] = (float) Color.alpha(value) / 255;
		return retVal;
	}

	@Override
	public synchronized void onDrawFrame(GL10 unused) {
		// Update offset.
		long time = SystemClock.uptimeMillis();
		// If time passed generate new target.
		if (time - mOffsetTime > 5000) {
			mOffsetTime = time;
			mOffsetSrc.set(mOffsetDst);
			mOffsetDst.x = -1f + (float) (Math.random() * 2f);
			mOffsetDst.y = -1f + (float) (Math.random() * 2f);
		}
		// Calculate final offset values.
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
		mFlowerFbo.bind();
		mFlowerFbo.bindTexture(0);

		// Render background gradient.
		mShaderBackground.useProgram();
		int uAspectRatio = mShaderBackground.getHandle("uAspectRatio");
		int uOffset = mShaderBackground.getHandle("uOffset");
		int uDarkenWidth = mShaderBackground.getHandle("uDarkenWidth");
		int aPosition = mShaderBackground.getHandle("aPosition");
		int aColor = mShaderBackground.getHandle("aColor");

		float aspectX = (float) Math.min(mWidth, mHeight) / mHeight;
		float aspectY = (float) Math.min(mWidth, mHeight) / mWidth;
		GLES20.glUniform2f(uAspectRatio, aspectX, aspectY);
		GLES20.glUniform2f(uOffset, mOffset.x, mOffset.y);
		GLES20.glUniform2f(uDarkenWidth, aspectX * 40f / mFlowerFbo.getWidth(),
				aspectY * 40f / mFlowerFbo.getHeight());
		GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_BYTE, false, 0,
				mScreenVertices);
		GLES20.glEnableVertexAttribArray(aPosition);
		GLES20.glVertexAttribPointer(aColor, 4, GLES20.GL_FLOAT, false, 0,
				mBackgroundColors);
		GLES20.glEnableVertexAttribArray(aColor);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

		// Render scene.
		mFlowerObjects.onDrawFrame(mOffset);

		// Copy FBO to screen buffer.
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glViewport(0, 0, mWidth, mHeight);
		mShaderCopy.useProgram();
		aPosition = mShaderCopy.getHandle("aPosition");
		GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_BYTE, false, 0,
				mScreenVertices);
		GLES20.glEnableVertexAttribArray(aPosition);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFlowerFbo.getTexture(0));
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		mWidth = width;
		mHeight = height;
		mFlowerFbo.init(mWidth, mHeight, 1);
		mFlowerObjects.onSurfaceChanged(mFlowerFbo.getWidth(),
				mFlowerFbo.getHeight());
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
			mFlowerObjects.onSurfaceCreated(mContext);
		}
	}

	/**
	 * Sets scroll offset. Called from wallpaper engine once user scrolls
	 * between home screens.
	 * 
	 * @param xOffset
	 *            Offset value between [0, 1].
	 * @param yOffset
	 *            Offset value between [0, 1]
	 */
	public void setOffset(float xOffset, float yOffset) {
		mOffsetScroll.set(xOffset * 2f, yOffset * 2f);
	}

	/**
	 * Updates preference values from provided ShaderPrefence instance.
	 * 
	 * @param prefs
	 *            New preferences.
	 */
	public synchronized void setPreferences(SharedPreferences prefs) {
		// Get general preferences values.
		String key = mContext.getString(R.string.key_general_flower_count);
		int flowerCount = Integer.parseInt(prefs.getString(key, "2"));
		key = mContext.getString(R.string.key_general_spline_quality);
		int splineQuality = prefs.getInt(key, 10);
		key = mContext.getString(R.string.key_general_branch_propability);
		float branchPropability = (float) prefs.getInt(key, 5) / 10;
		key = mContext.getString(R.string.key_general_zoom);
		float zoomLevel = (float) prefs.getInt(key, 4) / 10;

		// Get color preference values.
		key = mContext.getString(R.string.key_colors_scheme);
		int colorScheme = Integer.parseInt(prefs.getString(key, "1"));
		float bgTop[], bgBottom[], flowerColors[][] = new float[2][];
		switch (colorScheme) {
		case 1:
			bgTop = FlowerConstants.SCHEME_SUMMER_BG_TOP;
			bgBottom = FlowerConstants.SCHEME_SUMMER_BG_BOTTOM;
			flowerColors[0] = FlowerConstants.SCHEME_SUMMER_PLANT_1;
			flowerColors[1] = FlowerConstants.SCHEME_SUMMER_PLANT_2;
			break;
		case 2:
			bgTop = FlowerConstants.SCHEME_AUTUMN_BG_TOP;
			bgBottom = FlowerConstants.SCHEME_AUTUMN_BG_BOTTOM;
			flowerColors[0] = FlowerConstants.SCHEME_AUTUMN_PLANT_1;
			flowerColors[1] = FlowerConstants.SCHEME_AUTUMN_PLANT_2;
			break;
		case 3:
			bgTop = FlowerConstants.SCHEME_WINTER_BG_TOP;
			bgBottom = FlowerConstants.SCHEME_WINTER_BG_BOTTOM;
			flowerColors[0] = FlowerConstants.SCHEME_WINTER_PLANT_1;
			flowerColors[1] = FlowerConstants.SCHEME_WINTER_PLANT_2;
			break;
		case 4:
			bgTop = FlowerConstants.SCHEME_SPRING_BG_TOP;
			bgBottom = FlowerConstants.SCHEME_SPRING_BG_BOTTOM;
			flowerColors[0] = FlowerConstants.SCHEME_SPRING_PLANT_1;
			flowerColors[1] = FlowerConstants.SCHEME_SPRING_PLANT_2;
			break;
		default:
			bgTop = getColor(R.string.key_colors_bg_top, prefs);
			bgBottom = getColor(R.string.key_colors_bg_bottom, prefs);
			flowerColors[0] = getColor(R.string.key_colors_flower_1, prefs);
			flowerColors[1] = getColor(R.string.key_colors_flower_2, prefs);
			break;
		}

		mBackgroundColors.put(bgTop).put(bgBottom).put(bgTop).put(bgBottom)
				.position(0);
		mFlowerObjects.setPreferences(flowerCount, flowerColors, splineQuality,
				branchPropability, zoomLevel);
	}

}
