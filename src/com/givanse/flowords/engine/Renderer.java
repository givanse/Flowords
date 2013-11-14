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
	private static final int VERTEX_COLOR_ATTRIBUTES = 4;      /* Color: RGBA */
	
	/* Buffers */
								  /* Vertex buffer for full scene coordinates */
	private ByteBuffer buffVerticesCoords; 
	private HelperFrameBuffer helperFrameBffr = new HelperFrameBuffer();
	private FloatBuffer buffBckdColors;       /* Buffer for background colors */

	/* Animated offset time value for iterating between src and dst */
	private long offsetTime;
	// Additional animated offset source and destination values.
	private PointF offsetSrc = new PointF();
	private PointF offsetDst = new PointF();
	private final PointF offsetFinal = new PointF();
	private final PointF offsetScroll = new PointF();
	
	/* Shaders */
	                                    /* Copies offscreen texture on screen */
	private final HelperShader shdrCopyOffscreen = new HelperShader();
	private final HelperShader shdrBckndGradient = new HelperShader();
	
	private final boolean[] isShaderCompilerSupported = new boolean[1];	
	private int width, height;                 /* Surface/Screen dimensions */
	private Context context;
	private FlowerObjects flowerObjects = new FlowerObjects();
	
	/**
	 * Default constructor.
	 */
	public Renderer(Context context) {
		this.context = context;

		this.buffVerticesCoords = 
				         ByteBuffer.allocateDirect(Screen.VERTICES_COORDS.length);
		this.buffVerticesCoords.put(Screen.VERTICES_COORDS).position(0);

		/** 
		 * Create background color float buffer
		 * 4 colored vertices (screen corners), 2 top, 2 bottom
		 */
		ByteBuffer byteBuff = ByteBuffer.allocateDirect(
				                              Renderer.VERTEX_COLOR_ATTRIBUTES *
											  Screen.VERTICES_TOTAL *
				                              Renderer.BYTES_PER_FLOAT);
		this.buffBckdColors = byteBuff.order(ByteOrder.nativeOrder())
				                      .asFloatBuffer();
	}

	@Override
	public synchronized void onDrawFrame(GL10 unused) {
		// If shader compiler is not supported, clear screen buffer only.
		if (this.isShaderCompilerSupported[0] == false) {
			GLES20.glClearColor(0, 0, 0, 1);
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
			return;
		}

		// Update offset.
		long time = SystemClock.uptimeMillis();
		// If time passed generate new target.
		if (time - this.offsetTime > Renderer.UPDATE_RATE) {
			this.offsetTime = time;
			this.offsetSrc.set(this.offsetDst);
			this.offsetDst.x = -1f + (float) (Math.random() * 2f);
			this.offsetDst.y = -1f + (float) (Math.random() * 2f);
		}
		
		// Calculate final offset values.
		float t = (float) (time - this.offsetTime) / Renderer.UPDATE_RATE;
		t = t * t * (3 - 2 * t);
		this.offsetFinal.x = this.offsetScroll.x + this.offsetSrc.x + t *
				              (this.offsetDst.x - this.offsetSrc.x);
		this.offsetFinal.y = this.offsetScroll.y + this.offsetSrc.y + t *
				              (this.offsetDst.y - this.offsetSrc.y);

		// Disable unneeded rendering flags.
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		GLES20.glDisable(GLES20.GL_BLEND);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);

		// Set render target to FBO.
		this.helperFrameBffr.bindFrameBuffer();
		this.helperFrameBffr.bindTexture(0); // TODO: textureID 0

		this.renderBackgroundGradient();
		
		this.flowerObjects.drawFrame(this.offsetFinal);       /* Render scene */

		// Copy FBO to screen buffer.
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0); // TODO: handleID 0
		GLES20.glViewport(0, 0, this.width, this.height);
		
		this.shdrCopyOffscreen.useProgram();
		int aPositionHndl = this.shdrCopyOffscreen.getAUHandleId("aPosition");
		GLES20.glVertexAttribPointer(aPositionHndl,
				  				     Screen.VERTEX_SIZE_COORDS,
									 GLES20.GL_BYTE, 
									 false, 
									 0,
									 this.buffVerticesCoords);
		GLES20.glEnableVertexAttribArray(aPositionHndl);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 
        		             this.helperFrameBffr.getTexture(0)); // TODO: id 0
        /* this.buffVerticesCoords - 4 vertices */
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
	}

	public void renderBackgroundGradient() {
		this.shdrBckndGradient.useProgram();
		int uAspectRatio = this.shdrBckndGradient.getAUHandleId("uAspectRatio");
		float aspectX = (float) Math.min(width, height) / height;
		float aspectY = (float) Math.min(width, height) / width;
		int uOffset = this.shdrBckndGradient.getAUHandleId("uOffset");
		GLES20.glUniform2f(uAspectRatio, aspectX, aspectY);
		GLES20.glUniform2f(uOffset, offsetFinal.x, offsetFinal.y);
		
		int uLineWidth = this.shdrBckndGradient.getAUHandleId("uLineWidth");
		GLES20.glUniform2f(uLineWidth, 
						   aspectX * 40f / this.width,
				           aspectY * 40f / this.height);
		
		/* Pass in position information */
		int aPositionHndl = this.shdrBckndGradient.getAUHandleId("aPosition");
		GLES20.glVertexAttribPointer(aPositionHndl, 
									 Screen.VERTEX_SIZE_COORDS, 
									 GLES20.GL_BYTE, 
									 false, 
									 0,                       /* stryde bytes */
									 this.buffVerticesCoords);
		GLES20.glEnableVertexAttribArray(aPositionHndl);
		
		/* Pass in color information */
		int aColorHndl = this.shdrBckndGradient.getAUHandleId("aColor");
		int vertexSize = 4;            /* Attribute - color: RGBA, 4 elements */
		GLES20.glVertexAttribPointer(aColorHndl, 
				 					 vertexSize, 
								     GLES20.GL_FLOAT, 
								     false, 
								     0,                       /* stryde bytes */
								     this.buffBckdColors);
		GLES20.glEnableVertexAttribArray(aColorHndl);
		
		/* this.buffBckdColors     - 4 color attributes */
		/* this.buffScreenVertices - 4 vertices         */
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
	}
	
	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		// If shader compiler is not supported set viewport size only.
		if (isShaderCompilerSupported[0] == false) {
			GLES20.glViewport(0, 0, width, height);
			return;
		}

		this.width = width;
		this.height = height;
		this.helperFrameBffr.setTexturesPrefs(this.width, this.height, 1);
		this.flowerObjects.onSurfaceChanged(this.width, this.height);
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		// Check if shader compiler is supported.
		GLES20.glGetBooleanv(GLES20.GL_SHADER_COMPILER,
				             isShaderCompilerSupported, 0);

		// If not, show user an error message and return immediately.
		if (isShaderCompilerSupported[0] == false) {
			Handler handler = new Handler(context.getMainLooper());
			handler.post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(context, 
									   R.string.error_shader_compiler,
							           Toast.LENGTH_LONG).show();
					}
				});
			
			return;
		}

		this.shdrCopyOffscreen.setProgram(
				                   context.getString(R.string.shader_copy_vs),
				                   context.getString(R.string.shader_copy_fs));
		this.shdrBckndGradient.setProgram(
				             context.getString(R.string.shader_background_vs),
				             context.getString(R.string.shader_background_fs));
		flowerObjects.onSurfaceCreated(context);
	}

	/**
	 * Sets scroll offset. Called from WallpaperEngine once user scrolls
	 * between home screens.
	 * 
	 * @param xOffset
	 *            Offset value between [0, 1].
	 * @param yOffset
	 *            Offset value between [0, 1]
	 */
	/*public void onOffsetsChanged(float xOffset, float yOffset) {
		this.offsetScroll.set(xOffset * 2f, yOffset * 2f);
	}*/

	/**
	 * Updates preference values from provided ShaderPrefence instance.
	 * 
	 * @param prefs
	 *            New preferences.
	 */
	public synchronized void setPreferences(SharedPreferences prefs) {
		// Get general preferences values.
		String key = context.getString(R.string.key_general_flower_count);
		int flowerCount = Integer.parseInt(prefs.getString(key, "2"));
		
		key = context.getString(R.string.key_general_spline_quality);
		int splineQuality = prefs.getInt(key, 10);
		
		key = context.getString(R.string.key_general_branch_propability);
		float branchPropability = (float) prefs.getInt(key, 5) / 10;
		
		key = context.getString(R.string.key_general_zoom);
		float zoomLevel = (float) prefs.getInt(key, 4) / 10;

		// Get color preference values.
		key = context.getString(R.string.key_colors_scheme);
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
				 prefs.getInt(context.getString(R.string.key_colors_bg_top),
				 Color.BLACK));
			bckdBottom = Util.getColor(
				 prefs.getInt(context.getString(R.string.key_colors_bg_bottom),
				 Color.BLACK));
			flowerColors[0] = Util.getColor(
				 prefs.getInt(context.getString(R.string.key_colors_flower_1),
				 Color.WHITE));
			flowerColors[1] = Util.getColor(
				 prefs.getInt(context.getString(R.string.key_colors_flower_2),
				 Color.WHITE));
			break;
		}

		this.buffBckdColors.put(bckdTop).put(bckdBottom)
					  .put(bckdTop).put(bckdBottom)
				      .position(0);
		this.flowerObjects.setPreferences(flowerCount, flowerColors, splineQuality,
				                      branchPropability, zoomLevel);
	}

}
