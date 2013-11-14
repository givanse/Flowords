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

package com.givanse.flowords.engine.flowers;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Vector;
import com.givanse.flowords.R;
import com.givanse.flowords.engine.HelperShader;
import com.givanse.flowords.engine.Screen;
import com.givanse.flowords.engine.Util;
import com.givanse.flowords.engine.flowers.Spline.CTRL_POINT_ID;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.SystemClock;

/**
 * Main flower handling and rendering class.
 */
public final class FlowerObjects {

	private final PointF aspectRatio = new PointF();
	
	/* Flower movement directions, coords are stored in pairs */
	private final PointF[] directionPts = new PointF[Screen.DIRS_TOTAL];

	private final Vector<Knot> knotsList = new Vector<Knot>();
	private final Vector<Spline> splinesList = new Vector<Spline>();
	private Flower[] flowersList = new Flower[0];

	/* Shaders */
	private final HelperShader shaderSpline = new HelperShader();
	private final HelperShader shaderFlowerTexture = new HelperShader();

	/* Texture */
	private final int flowerTextureId[] = { -1 };        /* Flower texture ID */
	private ByteBuffer bufferTexture;             /* Texture rendering buffer */

    /* Values set through user preferences. */
	private FloatBuffer bufferSpline;              /* Used for spline quality */
	private int splineVertexCount;                 /* Used for Spline quality */
	private float zoomLvl;
	private float branchPropability;                  /* Value between [0, 1] */
	
	/**
	 * Default constructor.
	 */
	public FlowerObjects() {
		/* Not intuitive at all, but both arrays are equal. */
		//final byte[] textureCoordinates = { -1, 1, -1, -1, 1, 1, 1, -1 };
		final byte[] textureCoordinates = Screen.VERTICES_COORDS;
		this.bufferTexture = 
				           ByteBuffer.allocateDirect(textureCoordinates.length);
		this.bufferTexture.put(textureCoordinates).position(0);
		
		for (int i = 0; i < directionPts.length; ++i) {
			directionPts[i] = new PointF();
		}
	}

	/**
	 * Sets branch values based on given parameters.
	 */
	private void setBranchVals(Branch branchArg, PointF startPos, int startDir,
			               	   int rotateDir, float len) {

		float maxBranchWidth = Branch.WIDTH_MIN + 
							   this.zoomLvl *
							   (Branch.WIDTH_MAX - Branch.WIDTH_MIN);
		PointF dirPt = directionPts[(8 + startDir) % 8];
		PointF normalPt = directionPts[(8 + startDir - 2 * rotateDir) % 8];
		Spline spline = branchArg.getNextSpline();
		spline.setWidthStart(maxBranchWidth);
		spline.setWidthEnd(0f);
		spline.curveCtrlPoints(startPos, dirPt, len, normalPt, false);
		startPos = spline.getCtrlPoint(CTRL_POINT_ID.FOUR);

		float rand = Util.random(0, 3);
		if (rand < 1) {
			Knot point = branchArg.getNextKnot();
			point.setPosition(startPos);
			point.setRandomRotationSin();
			point.setRandomRotationCos();
		}
		if (rand >= 1) {
			spline.setWidthEnd(maxBranchWidth / 2);
			dirPt = directionPts[(8 + startDir + 2 * rotateDir) % 8];
			normalPt = directionPts[(8 + startDir) % 8];
			spline = branchArg.getNextSpline();
			spline.setWidthStart(maxBranchWidth / 2);
			spline.setWidthEnd(0f);
			spline.curveCtrlPoints(startPos, dirPt, len, normalPt, false);

			Knot point = branchArg.getNextKnot();
			point.setPosition(spline.getCtrlPoint(CTRL_POINT_ID.FOUR));
			point.setRandomRotationSin();
			point.setRandomRotationCos();
		}
		if (rand >= 2) {
			dirPt = directionPts[(8 + startDir - rotateDir) % 8];
			normalPt = directionPts[(8 + startDir + rotateDir) % 8];
			spline = branchArg.getNextSpline();
			spline.setWidthStart(maxBranchWidth / 2);
			spline.setWidthEnd(0f);
			spline.curveCtrlPoints(startPos, dirPt, len * .5f, normalPt, false);

			Knot point = branchArg.getNextKnot();
			point.setPosition(spline.getCtrlPoint(CTRL_POINT_ID.FOUR));
			point.setRandomRotationSin();
			point.setRandomRotationCos();
		}
	}

	private void renderFlowersTextures(Vector<Knot> knotsArg, float[] color,
			                   PointF offset) {

		this.shaderFlowerTexture.useProgram();
		int uAspectRatio = this.shaderFlowerTexture.getAUHandleId("uAspectRatio");
		int uOffset = this.shaderFlowerTexture.getAUHandleId("uOffset");
		int uScale = this.shaderFlowerTexture.getAUHandleId("uScale");
		int uRotationM = this.shaderFlowerTexture.getAUHandleId("uRotationM");
		int uColor = this.shaderFlowerTexture.getAUHandleId("uColor");
		int aPosition = this.shaderFlowerTexture.getAUHandleId("aPosition");

		GLES20.glUniform2f(uAspectRatio, aspectRatio.x, aspectRatio.y);
		GLES20.glUniform4fv(uColor, 1, color, 0);
		GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_BYTE, false, 0,
				                     this.bufferTexture);
		GLES20.glEnableVertexAttribArray(aPosition);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, flowerTextureId[0]);

		for (Knot knot: knotsArg) {
			final float rotationM[] = { knot.getRotationCos(), 
										knot.getRotationSin(),
					                    -knot.getRotationSin(), 
					                    knot.getRotationCos() };
			GLES20.glUniformMatrix2fv(uRotationM, 1, false, rotationM, 0);
			GLES20.glUniform2f(uOffset, 
							   knot.getPosition().x - offset.x,
							   knot.getPosition().y - offset.y);
			GLES20.glUniform1f(uScale, knot.getScale());
			GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		}
	}

	/**
	 * Animates flower element regarding to current renderTime value.
	 */
	private void update(Flower flower, long renderTime, PointF offset) {
		// TODO: it might be best to do scaling during rendering instead.
		final float rootWidth = Flower.ROOT_WIDTH_MIN +
								this.zoomLvl * 
								(Flower.ROOT_WIDTH_MAX - Flower.ROOT_WIDTH_MIN);

		PointF targetPos = flower.getTargetPosition();
		PointF currentPos = flower.getCurrentPosition();
		int currentDirIdx = flower.getDirIndex();
		Root lastElement = flower.getLastRootElement();
		long additionTime = renderTime;
		while (renderTime >= lastElement.getStartTime() + lastElement.getDuration()) {
			Root element = flower.getNextRootElement();
			element.setStartTime(additionTime);
			element.setDuration(500 + (long) (Math.random() * 500));

			targetPos.set(Util.random(-.8f, .8f), 
					      Util.random(-.8f, .8f));
			targetPos.offset(offset.x, offset.y);

			float minDist = Util.getDistance(currentPos, directionPts[currentDirIdx],
					                 targetPos);
			int minDirIndex = currentDirIdx;
			for (int i = 1; i < 8; ++i) {
				PointF dir = directionPts[(currentDirIdx + i) % 8];
				float dist = Util.getDistance(currentPos, dir, targetPos);
				if (dist < minDist) {
					minDist = dist;
					minDirIndex = (currentDirIdx + i) % 8;
				}
			}

			final float splineLen = Math.max(Util.random(.3f, .5f),
					Util.getDistance(currentPos, targetPos) / 2f);

			if (minDirIndex != currentDirIdx) {
				int k = minDirIndex > currentDirIdx ? 1 : -1;
				for (int i = currentDirIdx + k; 
					 i * k <= minDirIndex * k; i += 2 * k) {
					PointF dir = this.directionPts[i];
					PointF normal = this.directionPts[(8 + i - 2 * k) % 8];
					Spline spline = element.getNextSpline();
					spline.setWidthStart(rootWidth);
					spline.setWidthEnd(rootWidth);
					spline.curveCtrlPoints(currentPos, dir, splineLen, 
							      normal, i == minDirIndex);

					if (Math.random() < branchPropability) {
						Branch b = element.getCurrentBranch();
						int branchDir = Math.random() < 0.5 ? -k : k;
						float branchLen = Math.min(splineLen, .5f) * 
								          Util.random(.6f, .8f);
						setBranchVals(b, currentPos, i + branchDir, branchDir,
								  branchLen);
					}

					currentPos.set(spline.getCtrlPoint(CTRL_POINT_ID.FOUR));
				}
				currentDirIdx = minDirIndex;
			} else {
				PointF dir = directionPts[currentDirIdx];
				Spline spline = element.getNextSpline();
				spline.setWidthStart(rootWidth);
				spline.setWidthEnd(rootWidth);
				spline.setStraight(currentPos, dir, splineLen);
				
				if (Math.random() < branchPropability) {
					Branch b = element.getCurrentBranch();
					int branchDir = Math.random() < 0.5 ? -1 : 1;
					float branchLen = Math.min(splineLen, .5f) * 
							          Util.random(.6f, .8f);
					setBranchVals(b, currentPos, currentDirIdx + branchDir,
							  branchDir, branchLen);
				}

				currentPos.set(spline.getCtrlPoint(CTRL_POINT_ID.FOUR));
			}

			additionTime += element.getDuration();
			lastElement = element;
		}
		flower.setDirIndex(currentDirIdx);
	}
	
	/**
	 * Renders splines.
	 */
	private void renderSplines(Vector<Spline> splines, 
							   float[] color,
							   PointF offset) {
		this.shaderSpline.useProgram();
		int uControlPts = this.shaderSpline.getAUHandleId("uControlPts");
		int uWidth = this.shaderSpline.getAUHandleId("uWidth");
		int uBounds = this.shaderSpline.getAUHandleId("uBounds");
		int uColor = this.shaderSpline.getAUHandleId("uColor");
		int uAspectRatio = this.shaderSpline.getAUHandleId("uAspectRatio");
		int aSplinePos = this.shaderSpline.getAUHandleId("aSplinePos");

		GLES20.glUniform2f(uAspectRatio, 
				           this.aspectRatio.x, this.aspectRatio.y);
		GLES20.glUniform4fv(uColor, 1, color, 0);
		GLES20.glVertexAttribPointer(aSplinePos, 2, GLES20.GL_FLOAT, false, 0,
									 this.bufferSpline);
		GLES20.glEnableVertexAttribArray(aSplinePos);

		final float[] controlPts = new float[8];
		float boundX = Spline.WIDTH_MIN +
					   this.zoomLvl * 
					   (Spline.WIDTH_MAX - 
					    Spline.WIDTH_MIN);
		float boundY = 1f + boundX * this.aspectRatio.y;
		boundX = 1f + boundX * this.aspectRatio.x;

		for (Spline spline : splines) {
			int visiblePointCount = 0;
			for (int i = 0; i < Spline.CTRL_POINTS_TOTAL; ++i) {
				float x = spline.getCtrlPoint(i).x - offset.x;
				float y = spline.getCtrlPoint(i).y - offset.y;
				controlPts[i * 2 + 0] = x;
				controlPts[i * 2 + 1] = y;
				if (Math.abs(x) < boundX && Math.abs(y) < boundY) {
					++visiblePointCount;
				}
			}
			if (visiblePointCount != 0) {
				GLES20.glUniform2fv(uControlPts, 4, controlPts, 0);
				GLES20.glUniform2f(uWidth, 
						          spline.getWidthStart(), spline.getWidthEnd());
				GLES20.glUniform2f(uBounds, spline.getStart(), spline.getEnd());

				if (spline.getStart() != 0f || spline.getEnd() != 1f) {
					int startIdx = (int) Math.floor(spline.getStart() *
							             (splineVertexCount - 1)) * 2;
					int endIdx = 2 + (int) Math.ceil(spline.getEnd() * 
							     (splineVertexCount - 1)) * 2;
					GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 
										startIdx,
									    endIdx - startIdx);
				} else {
					GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 
										0,
										splineVertexCount * 2);
				}
			}
		}
	}
	
	private void rebuildFlowers(int flowerCount, float[][] flowerColors) {
		this.flowersList = new Flower[flowerCount];                    
        for (int i = 0; i < flowersList.length; i++) {                   
            flowersList[i] = new Flower();                        
            flowersList[i].setColor(flowerColors[i]);                     
        }
	}
	
    /**
     * PUBLIC METHODS 
     */
	
	/**
	 * Renders flowersList into scene.
	 * 
	 * @param offset
	 *            Global offset value.
	 */
	public void drawFrame(PointF offset) {
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

		long renderTime = SystemClock.uptimeMillis();
		for (int i = 0; i < this.flowersList.length; i++) {
			this.knotsList.clear();
			this.splinesList.clear();

			Flower flower = this.flowersList[i];
			this.update(flower, renderTime, offset);
			flower.setForRenderSplinesKnots(this.splinesList, 
									this.knotsList,
									renderTime,
									this.zoomLvl);
			this.renderSplines(this.splinesList, flower.getColor(), offset);
			this.renderFlowersTextures(
					                 this.knotsList, flower.getColor(), offset);
		}

		GLES20.glDisable(GLES20.GL_BLEND);
	}
	
	/**
	 * Called once underlying surface size has changed.
	 * 
	 * @param width
	 *            Surface width.
	 * @param height
	 *            Surface height.
	 */
	public void onSurfaceChanged(int width, int height) {
		/**
		 * Update the aspect ratio.
		 *   aspectRatio = units of equal length / dimension 
		 */
		this.aspectRatio.x = (float) Math.min(width, height) / width;
		this.aspectRatio.y = (float) Math.min(width, height) / height;
		
		/**
		 * Adjust BASE_COORDS to the new aspect ratio.
		 */
		for (int i = 0; i < this.directionPts.length; i++) {
			PointF directionPt = this.directionPts[i];
			/* Use base directions, read BASE_COORDS in pairs */
			directionPt.set(Screen.BASE_COORDS[i * 2 + 0], 
					        Screen.BASE_COORDS[i * 2 + 1]);
			
			/* Scale directions to the new aspect ratio */
			float lenInv = 1f / directionPt.length();
			directionPt.x *= this.aspectRatio.x * lenInv;
			directionPt.y *= this.aspectRatio.y * lenInv;
		}
		for (Flower flower : this.flowersList) {
			flower.reset();
		}
	}

	/**
	 * Called once Surface has been created.
	 * 
	 * @param context
	 *            Context to read resources from.
	 */
	public void onSurfaceCreated(Context context) {
		this.shaderSpline.setProgram(
				                  context.getString(R.string.shader_spline_vs),
				                  context.getString(R.string.shader_spline_fs));
		this.shaderFlowerTexture.setProgram(
				                 context.getString(R.string.shader_texture_vs),
				                 context.getString(R.string.shader_texture_fs));

        /**
          * Flower texture generation made with Bitmap, Canvas and Paint 
          *   github.com/harism/android_wallpaper_flowers/
          *   commit/42a0a124315f301db726287db1d0edb225d2ef4c
          */
		GLES20.glDeleteTextures(1, flowerTextureId, 0);
		GLES20.glGenTextures(1, flowerTextureId, 0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, flowerTextureId[0]);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
				GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
				GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

		Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
		bitmap.eraseColor(Color.BLACK);

		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint();
		paint.setStyle(Paint.Style.FILL);

		int borderColor = Color.rgb((int) (.8f * 255), 0, 0);
		int mainColor = Color.rgb(255, 0, 0);

		float leafDist = 1.7f * 128 / 3f;
		float leafPositions[] = new float[10];
		for (int i = 0; i < 5; ++i) {
			double r = Math.PI * 2 * i / 5;
			leafPositions[i * 2 + 0] = 128 + (float) (Math.sin(r) * leafDist);
			leafPositions[i * 2 + 1] = 128 + (float) (Math.cos(r) * leafDist);
		}

		paint.setColor(borderColor);
		for (int i = 0; i < 5; ++i) {
			canvas.drawCircle(leafPositions[i * 2 + 0],
					leafPositions[i * 2 + 1], 48, paint);
		}
		paint.setColor(mainColor);
		for (int i = 0; i < 5; ++i) {
			canvas.drawCircle(leafPositions[i * 2 + 0],
					leafPositions[i * 2 + 1], 36, paint);
		}
		paint.setColor(borderColor);
		canvas.drawCircle(128, 128, 48, paint);
		paint.setColor(Color.BLACK);
		canvas.drawCircle(128, 128, 36, paint);

		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
		bitmap.recycle();
	}
	
    /**                                                                          
     * Updates preference values.                                                
     */                                                                          
    public void setPreferences(int flowerCount, float[][] flowerColors,          
                               int splineQuality, float branchProbability,          
                               float zoomLevel) {
    	
        if (flowerCount != this.flowersList.length) {
            this.rebuildFlowers(flowerCount, flowerColors);                                                  
        }
        
        for (int i = 0; i < flowersList.length; ++i) {                       
            flowersList[i].setColor(flowerColors[i]);                         
        }                                                                        
                                
        // TODO: magic numbers 2, 2, 4, 4
        if (splineVertexCount != splineQuality + 2) {                           
            splineVertexCount = splineQuality + 2;                              
            ByteBuffer byteBuffer =                                                 
                          ByteBuffer.allocateDirect(4 * 4 * splineVertexCount); 
            this.bufferSpline = byteBuffer.order(ByteOrder.nativeOrder())               
                                          .asFloatBuffer();                             
            for (int i = 0; i < splineVertexCount; ++i) {                       
                float t = (float) i / (splineVertexCount - 1);                  
                this.bufferSpline.put(t).put(1)
                		    .put(t).put(-1);                                    
            }                                                                    
            this.bufferSpline.position(0);                                           
        }                                                                 
                                                                                 
        this.branchPropability = branchProbability;                                  
        this.zoomLvl = zoomLevel;                                                  
    }
    
}
