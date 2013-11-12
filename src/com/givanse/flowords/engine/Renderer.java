/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.givanse.flowords.engine;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import com.givanse.flowords.R;
import com.givanse.flowords.engine.flowers.FlowerObjects;
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
public final class Renderer implements GLSurfaceView.Renderer {
	
	// Current context.
	private Context mContext;
	// FBO for offscreen rendering.
	private HelperFrameBufferObject helperFBO = new HelperFrameBufferObject();
	// Actual flower renderer instance.
	private FlowerObjects mFlowerObjects = new FlowerObjects();
	// Scroll offset value.
	private final PointF mOffsetScroll = new PointF();

	// Additional animated offset source and destination values.
	private PointF mOffsetSrc = new PointF();
	private PointF mOffsetDst = new PointF();
	private final PointF mOffsetFinal = new PointF();

	// Animated offset time value for iterating between src and dst.
	private long mOffsetTime;
	private final int updateRate = 5000; // milliseconds 
	
	private FloatBuffer buffBckdColors;         // Buffer for background colors.
	private ByteBuffer mScreenVertices;//Vertex buffer for full scene coordinates.
	
	// Shader for rendering background gradient.
	private final HelperShader mShaderBackground = new HelperShader();
	// Shader for copying offscreen texture on screen.
	private final HelperShader mShaderCopy = new HelperShader();
	// Flag for indicating whether shader compiler is supported.
	private final boolean[] mShaderCompilerSupported = new boolean[1];
		
	// Surface/screen dimensions.
	private int mWidth, mHeight;
	
	/**
	 * Default constructor.
	 */
	public Renderer(Context context) {
		mContext = context;

		// Create screen coordinates buffer.
		final byte screenCoords[] = { -1, 1, -1, -1, 1, 1, 1, -1 }; // 8
		mScreenVertices = ByteBuffer.allocateDirect(2 * 4); // 8
		mScreenVertices.put(screenCoords).position(0);

		// Create background color float buffer.
		ByteBuffer bBuf = ByteBuffer.allocateDirect(4 * 4 * 4); // 64
		buffBckdColors = bBuf.order(ByteOrder.nativeOrder()).asFloatBuffer();
	}

	/**
	 * Retrieves color value from preferences with given key.
	 */
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
		// If shader compiler is not supported, clear screen buffer only.
		if (mShaderCompilerSupported[0] == false) {
			GLES20.glClearColor(0, 0, 0, 1);
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
			return;
		}

		// Update offset.
		long time = SystemClock.uptimeMillis();
		// If time passed generate new target.
		if (time - mOffsetTime > this.updateRate) {
			mOffsetTime = time;
			mOffsetSrc.set(mOffsetDst);
			mOffsetDst.x = -1f + (float) (Math.random() * 2f);
			mOffsetDst.y = -1f + (float) (Math.random() * 2f);
		}
		// Calculate final offset values.
		float t = (float) (time - mOffsetTime) / this.updateRate;
		t = t * t * (3 - 2 * t);
		mOffsetFinal.x = mOffsetScroll.x + mOffsetSrc.x + t *
				    (mOffsetDst.x - mOffsetSrc.x);
		mOffsetFinal.y = mOffsetScroll.y + mOffsetSrc.y + t *
				    (mOffsetDst.y - mOffsetSrc.y);

		// Disable unneeded rendering flags.
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		GLES20.glDisable(GLES20.GL_BLEND);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);

		// Set render target to FBO.
		this.helperFBO.bind();
		this.helperFBO.bindTexture(0);

		// Render background gradient.
		this.mShaderBackground.useProgram();
		int uAspectRatio = this.mShaderBackground.getHandleID("uAspectRatio");
		int uOffset = this.mShaderBackground.getHandleID("uOffset");
		int uLineWidth = this.mShaderBackground.getHandleID("uLineWidth");
		int aPosition = this.mShaderBackground.getHandleID("aPosition");
		int aColor = this.mShaderBackground.getHandleID("aColor");

		float aspectX = (float) Math.min(mWidth, mHeight) / mHeight;
		float aspectY = (float) Math.min(mWidth, mHeight) / mWidth;
		GLES20.glUniform2f(uAspectRatio, aspectX, aspectY);
		GLES20.glUniform2f(uOffset, mOffsetFinal.x, mOffsetFinal.y);
		GLES20.glUniform2f(uLineWidth, 
						   aspectX * 40f / this.helperFBO.getWidth(),
				           aspectY * 40f / this.helperFBO.getHeight());
		GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_BYTE, false, 0,
				                     mScreenVertices);
		GLES20.glEnableVertexAttribArray(aPosition);
		GLES20.glVertexAttribPointer(aColor, 4, GLES20.GL_FLOAT, false, 0,
				                     buffBckdColors);
		GLES20.glEnableVertexAttribArray(aColor);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

		// Render scene.
		this.mFlowerObjects.drawFrame(mOffsetFinal);

		// Copy FBO to screen buffer.
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glViewport(0, 0, mWidth, mHeight);
		this.mShaderCopy.useProgram();
		aPosition = this.mShaderCopy.getHandleID("aPosition");
		GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_BYTE, false, 0,
				mScreenVertices);
		GLES20.glEnableVertexAttribArray(aPosition);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 
				             this.helperFBO.getTexture(0));
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		// If shader compiler is not supported set viewport size only.
		if (mShaderCompilerSupported[0] == false) {
			GLES20.glViewport(0, 0, width, height);
			return;
		}

		this.mWidth = width;
		this.mHeight = height;
		this.helperFBO.setTexturesPrefs(this.mWidth, this.mHeight, 1);
		mFlowerObjects.onSurfaceChanged(this.helperFBO.getWidth(),
									    this.helperFBO.getHeight());
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		// Check if shader compiler is supported.
		GLES20.glGetBooleanv(GLES20.GL_SHADER_COMPILER,
				             mShaderCompilerSupported, 0);

		// If not, show user an error message and return immediately.
		if (mShaderCompilerSupported[0] == false) {
			Handler handler = new Handler(mContext.getMainLooper());
			handler.post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(mContext, 
									   R.string.error_shader_compiler,
							           Toast.LENGTH_LONG).show();
					}
				});
			
			return;
		}

		this.mShaderCopy.setProgram(
				                   mContext.getString(R.string.shader_copy_vs),
				                   mContext.getString(R.string.shader_copy_fs));
		this.mShaderBackground.setProgram(
				             mContext.getString(R.string.shader_background_vs),
				             mContext.getString(R.string.shader_background_fs));
		mFlowerObjects.onSurfaceCreated(mContext);
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
		float bckdTop[], bckdBottom[], flowerColors[][] = new float[2][];
		switch (colorScheme) {
		case 1:
			bckdTop = ColorSchemes.SUMMER_BG_TOP;
			bckdBottom = ColorSchemes.SUMMER_BG_BOTTOM;
			flowerColors[0] = ColorSchemes.SUMMER_PLANT_1;
			flowerColors[1] = ColorSchemes.SUMMER_PLANT_2;
			break;
		case 2:
			bckdTop = ColorSchemes.AUTUMN_BG_TOP;
			bckdBottom = ColorSchemes.AUTUMN_BG_BOTTOM;
			flowerColors[0] = ColorSchemes.AUTUMN_PLANT_1;
			flowerColors[1] = ColorSchemes.AUTUMN_PLANT_2;
			break;
		case 3:
			bckdTop = ColorSchemes.WINTER_BG_TOP;
			bckdBottom = ColorSchemes.WINTER_BG_BOTTOM;
			flowerColors[0] = ColorSchemes.WINTER_PLANT_1;
			flowerColors[1] = ColorSchemes.WINTER_PLANT_2;
			break;
		case 4:
			bckdTop = ColorSchemes.SPRING_BG_TOP;
			bckdBottom = ColorSchemes.SPRING_BG_BOTTOM;
			flowerColors[0] = ColorSchemes.SPRING_PLANT_1;
			flowerColors[1] = ColorSchemes.SPRING_PLANT_2;
			break;
		default:
			bckdTop = getColor(R.string.key_colors_bg_top, prefs);
			bckdBottom = getColor(R.string.key_colors_bg_bottom, prefs);
			flowerColors[0] = getColor(R.string.key_colors_flower_1, prefs);
			flowerColors[1] = getColor(R.string.key_colors_flower_2, prefs);
			break;
		}

		buffBckdColors.put(bckdTop).put(bckdBottom)
					  .put(bckdTop).put(bckdBottom)
				      .position(0);
		mFlowerObjects.setPreferences(flowerCount, flowerColors, splineQuality,
				                      branchPropability, zoomLevel);
	}

}