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
	
	private static final int UPDATE_RATE = 5000;              /* Milliseconds */
	private static final int BYTES_PER_FLOAT = 4;
	private static final int VERTEX_ATTRIBUTES = 4;            /* Color: RGBA */
		                          /* bckdTop, bckdBottom, bckdTop, bckdBottom */
	private static final int BCKD_COLOR_PREFERENCES = 4;
	
	/* Buffers */
	private HelperFrameBufferObject helperFBO = new HelperFrameBufferObject();
	private FloatBuffer buffBckdColors;       /* Buffer for background colors */
    							  /* Vertex buffer for full scene coordinates */
	private ByteBuffer buffScreenVertices; 
	
	// Current context.
	private Context mContext;
	// FBO for offscreen rendering.
	private FlowerObjects mFlowerObjects = new FlowerObjects();
	// Scroll offset value.
	private final PointF mOffsetScroll = new PointF();

	// Additional animated offset source and destination values.
	private PointF mOffsetSrc = new PointF();
	private PointF mOffsetDst = new PointF();
	private final PointF mOffsetFinal = new PointF();

	// Animated offset time value for iterating between src and dst.
	private long mOffsetTime;
	
	/* Shaders */
	private final HelperShader shdrBckndGradient = new HelperShader();
	                                    /* Copies offscreen texture on screen */
	private final HelperShader shdrCopyOffscreen = new HelperShader();
	private final boolean[] isShaderCompilerSupported = new boolean[1];
		
	private int mWidth, mHeight;                 /* Surface/Screen dimensions */
	
	/**
	 * Default constructor.
	 */
	public Renderer(Context context) {
		this.mContext = context;

		this.buffScreenVertices = 
				         ByteBuffer.allocateDirect(Screen.VERTEX_COORDS.length);
		this.buffScreenVertices.put(Screen.VERTEX_COORDS).position(0);

		/** 
		 * Create background color float buffer
		 *   vertex attributes * background color preferences * bytes per float
		 */
		ByteBuffer byteBuff = ByteBuffer.allocateDirect(
				                               Renderer.VERTEX_ATTRIBUTES *
											   Renderer.BCKD_COLOR_PREFERENCES *
				                               Renderer.BYTES_PER_FLOAT);
		this.buffBckdColors = byteBuff.order(ByteOrder.nativeOrder())
				                      .asFloatBuffer();
	}

	@Override
	public synchronized void onDrawFrame(GL10 unused) {
		// If shader compiler is not supported, clear screen buffer only.
		if (isShaderCompilerSupported[0] == false) {
			GLES20.glClearColor(0, 0, 0, 1);
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
			return;
		}

		// Update offset.
		long time = SystemClock.uptimeMillis();
		// If time passed generate new target.
		if (time - mOffsetTime > Renderer.UPDATE_RATE) {
			mOffsetTime = time;
			mOffsetSrc.set(mOffsetDst);
			mOffsetDst.x = -1f + (float) (Math.random() * 2f);
			mOffsetDst.y = -1f + (float) (Math.random() * 2f);
		}
		
		// Calculate final offset values.
		float t = (float) (time - mOffsetTime) / Renderer.UPDATE_RATE;
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
		this.shdrBckndGradient.useProgram();
		int uAspectRatio = this.shdrBckndGradient.getAUHandleId("uAspectRatio");
		int uOffset = this.shdrBckndGradient.getAUHandleId("uOffset");
		int uLineWidth = this.shdrBckndGradient.getAUHandleId("uLineWidth");
		int aPosition = this.shdrBckndGradient.getAUHandleId("aPosition");
		int aColor = this.shdrBckndGradient.getAUHandleId("aColor");

		float aspectX = (float) Math.min(mWidth, mHeight) / mHeight;
		float aspectY = (float) Math.min(mWidth, mHeight) / mWidth;
		GLES20.glUniform2f(uAspectRatio, aspectX, aspectY);
		GLES20.glUniform2f(uOffset, mOffsetFinal.x, mOffsetFinal.y);
		GLES20.glUniform2f(uLineWidth, 
						   aspectX * 40f / this.helperFBO.getWidth(),
				           aspectY * 40f / this.helperFBO.getHeight());
		GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_BYTE, false, 0,
				                     buffScreenVertices);
		GLES20.glEnableVertexAttribArray(aPosition);
		
		int numCompPerVertexAttr = 4;
		GLES20.glVertexAttribPointer(aColor, numCompPerVertexAttr, 
									 GLES20.GL_FLOAT, false, 0,
				                     buffBckdColors);
		GLES20.glEnableVertexAttribArray(aColor);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

		// Render scene.
		this.mFlowerObjects.drawFrame(mOffsetFinal);

		// Copy FBO to screen buffer.
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glViewport(0, 0, mWidth, mHeight);
		this.shdrCopyOffscreen.useProgram();
		aPosition = this.shdrCopyOffscreen.getAUHandleId("aPosition");
		GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_BYTE, false, 0,
				buffScreenVertices);
		GLES20.glEnableVertexAttribArray(aPosition);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 
				             this.helperFBO.getTexture(0));
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		// If shader compiler is not supported set viewport size only.
		if (isShaderCompilerSupported[0] == false) {
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
				             isShaderCompilerSupported, 0);

		// If not, show user an error message and return immediately.
		if (isShaderCompilerSupported[0] == false) {
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

		this.shdrCopyOffscreen.setProgram(
				                   mContext.getString(R.string.shader_copy_vs),
				                   mContext.getString(R.string.shader_copy_fs));
		this.shdrBckndGradient.setProgram(
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
			bckdTop = Util.getColor(
				 prefs.getInt(mContext.getString(R.string.key_colors_bg_top),
				 Color.BLACK));
			bckdBottom = Util.getColor(
				 prefs.getInt(mContext.getString(R.string.key_colors_bg_bottom),
				 Color.BLACK));
			flowerColors[0] = Util.getColor(
				 prefs.getInt(mContext.getString(R.string.key_colors_flower_1),
				 Color.WHITE));
			flowerColors[1] = Util.getColor(
				 prefs.getInt(mContext.getString(R.string.key_colors_flower_2),
				 Color.WHITE));
			break;
		}

		buffBckdColors.put(bckdTop).put(bckdBottom)
					  .put(bckdTop).put(bckdBottom)
				      .position(0);
		mFlowerObjects.setPreferences(flowerCount, flowerColors, splineQuality,
				                      branchPropability, zoomLevel);
	}

}
