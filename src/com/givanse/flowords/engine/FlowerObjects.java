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
import java.util.Vector;
import com.givanse.flowords.R;
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

	// Flower movement directions.
	private static final float[] directions = 
		               { 0, 1, 1, 1, 1, 0, 1, -1, 0, -1, -1, -1, -1, 0, -1, 1 };
	// Render area aspect ratio.
	private final PointF mAspectRatio = new PointF();
	// Branch propability preference value between [0, 1].
	private float mBranchPropability;
	// Spline rendering buffer.
	private FloatBuffer mBufferSpline;
	// Texture rendering buffer.
	private ByteBuffer mBufferTexture;

	private final Vector<Point> pointsList = new Vector<Point>();
	private final Vector<Spline> splinesList = new Vector<Spline>();
	private Flower[] flowersList = new Flower[0];

	// Flower movement directions. These are calculated from directions so that
	// first length is set to 1, and then multiplied with aspect ratio.
	private final PointF[] mDirections = new PointF[8];
	// Flower texture id.
	private final int mFlowerTextureId[] = { -1 };
	// Shader for rendering splines.
	private final HelperShader mShaderSpline = new HelperShader();
	// Shader for rendering flower textures.
	private final HelperShader mShaderTexture = new HelperShader();
	// Spline vertex count,
	private int mSplineVertexCount;

    /* Values set through user preferences. */
	private float zoomLvl;

	/**
	 * Default constructor.
	 */
	public FlowerObjects() {
		final byte[] textureCoordinates = { -1, 1, -1, -1, 1, 1, 1, -1 };
		mBufferTexture = ByteBuffer.allocateDirect(2 * 4); // 8
		mBufferTexture.put(textureCoordinates).position(0);
		for (int i = 0; i < mDirections.length; ++i) {
			mDirections[i] = new PointF();
		}
	}

	/**
	 * Calculates distance between point1 and point2.
	 */
	private float distance(PointF point1, PointF point2) {
		float dx = point1.x - point2.x;
		float dy = point1.y - point2.y;
		return (float) Math.sqrt(dx * dx + dy * dy);
	}

	/**
	 * Calculates distance between point1 + point2 and point3.
	 */
	private float distance(PointF point1, PointF point2, PointF point3) {
		float dx = point1.x + point2.x - point3.x;
		float dy = point1.y + point2.y - point3.y;
		return (float) Math.sqrt(dx * dx + dy * dy);
	}

	/**
	 * Sets spline control points based on given parameters.
	 */
	private void genArc(Spline spline, PointF startPos, PointF dir,
			            float length, PointF normal, boolean straightEnd) {

		// Bezier curve circle estimation.
		// 2 * (sqrt(2) - 1) / 3
		final float NORMAL_FACTOR = 0.27614237f;
		final float normalLen = length * NORMAL_FACTOR;

		// Initially set all control points to startPos.
		for (PointF point : spline.mPoints) {
			point.set(startPos);
		}
		// Move second control point into target direction plus same length in
		// normal direction.
		spline.mPoints[1].offset((dir.x + normal.x) * normalLen,
				(dir.y + normal.y) * normalLen);
		// Move third control point to (startPos + (length - normalLen) * dir).
		spline.mPoints[2].offset(dir.x * (length - normalLen), dir.y
				* (length - normalLen));
		// If straight end is not requested move third control point among
		// normal.
		if (!straightEnd) {
			spline.mPoints[2]
					.offset(normal.x * normalLen, normal.y * normalLen);
		}
		// Set last control point to (startPos + dir * length).
		spline.mPoints[3].offset(dir.x * length, dir.y * length);
	}

	/**
	 * Sets branch values based on given parameters.
	 */
	private void genBranch(Branch branch, PointF startPos, int startDir,
			               int rotateDir, float len) {

		float maxBranchWidth = Branch.WIDTH_MIN + 
							   this.zoomLvl *
							   (Branch.WIDTH_MAX - 
						       Branch.WIDTH_MIN);
		PointF dir = mDirections[(8 + startDir) % 8];
		PointF normal = mDirections[(8 + startDir - 2 * rotateDir) % 8];
		Spline spline = branch.getNextSpline();
		spline.mWidthStart = maxBranchWidth;
		spline.mWidthEnd = 0f;
		genArc(spline, startPos, dir, len, normal, false);
		startPos = spline.mPoints[3];

		float rand = rand(0, 3);
		if (rand < 1) {
			Point point = branch.getNextPoint();
			point.mPosition.set(startPos);
			final float rotation = rand(0, (float) (Math.PI * 2));
			point.mRotationSin = (float) Math.sin(rotation);
			point.mRotationCos = (float) Math.cos(rotation);
		}
		if (rand >= 1) {
			spline.mWidthEnd = maxBranchWidth / 2;
			dir = mDirections[(8 + startDir + 2 * rotateDir) % 8];
			normal = mDirections[(8 + startDir) % 8];
			spline = branch.getNextSpline();
			spline.mWidthStart = maxBranchWidth / 2;
			spline.mWidthEnd = 0f;
			genArc(spline, startPos, dir, len, normal, false);

			Point point = branch.getNextPoint();
			point.mPosition.set(spline.mPoints[3]);
			final float rotation = rand(0, (float) (Math.PI * 2));
			point.mRotationSin = (float) Math.sin(rotation);
			point.mRotationCos = (float) Math.cos(rotation);
		}
		if (rand >= 2) {
			dir = mDirections[(8 + startDir - rotateDir) % 8];
			normal = mDirections[(8 + startDir + rotateDir) % 8];
			spline = branch.getNextSpline();
			spline.mWidthStart = maxBranchWidth / 2;
			spline.mWidthEnd = 0f;
			genArc(spline, startPos, dir, len * .5f, normal, false);

			Point point = branch.getNextPoint();
			point.mPosition.set(spline.mPoints[3]);
			final float rotation = rand(0, (float) (Math.PI * 2));
			point.mRotationSin = (float) Math.sin(rotation);
			point.mRotationCos = (float) Math.cos(rotation);
		}
	}

	/**
	 * Sets spline to straight line between (start, start + length * dir).
	 */
	private void genLine(Spline spline, PointF start, PointF dir,
			             float length) {
		for (int i = 0; i < 4; ++i) {
			float t = (i * length) / 3;
			PointF point = spline.mPoints[i];
			point.set(start);
			point.offset(dir.x * t, dir.y * t);
		}
	}

	/**
	 * Generates random value between [min, max).
	 */
	private float rand(float min, float max) {
		return min + (float) (Math.random() * (max - min));
	}

	/**
	 * Renders flower textures.
	 */
	private void renderFlowers(Vector<Point> flowersList, float[] color,
			                   PointF offset) {

		this.mShaderTexture.useProgram();
		int uAspectRatio = this.mShaderTexture.getHandleID("uAspectRatio");
		int uOffset = this.mShaderTexture.getHandleID("uOffset");
		int uScale = this.mShaderTexture.getHandleID("uScale");
		int uRotationM = this.mShaderTexture.getHandleID("uRotationM");
		int uColor = this.mShaderTexture.getHandleID("uColor");
		int aPosition = this.mShaderTexture.getHandleID("aPosition");

		GLES20.glUniform2f(uAspectRatio, mAspectRatio.x, mAspectRatio.y);
		GLES20.glUniform4fv(uColor, 1, color, 0);
		GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_BYTE, false, 0,
				                     mBufferTexture);
		GLES20.glEnableVertexAttribArray(aPosition);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFlowerTextureId[0]);

		for (Point point : flowersList) {
			final float rotationM[] = { point.mRotationCos, 
										point.mRotationSin,
					                    -point.mRotationSin, 
					                    point.mRotationCos };
			GLES20.glUniformMatrix2fv(uRotationM, 1, false, rotationM, 0);
			GLES20.glUniform2f(uOffset, 
							   point.mPosition.x - offset.x,
							   point.mPosition.y - offset.y);
			GLES20.glUniform1f(uScale, point.mScale);
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
								(Flower.ROOT_WIDTH_MAX - 
								Flower.ROOT_WIDTH_MIN);

		PointF targetPos = flower.mTargetPosition;
		PointF currentPos = flower.mCurrentPosition;
		int currentDirIdx = flower.mCurrentDirIndex;
		ElementRoot lastElement = flower.getLastRootElement();
		long additionTime = renderTime;
		while (renderTime >= lastElement.mStartTime + lastElement.mDuration) {
			ElementRoot element = flower.getNextRootElement();
			element.mStartTime = additionTime;
			element.mDuration = 500 + (long) (Math.random() * 500);

			targetPos.set(rand(-.8f, .8f), rand(-.8f, .8f));
			targetPos.offset(offset.x, offset.y);

			float minDist = distance(currentPos, mDirections[currentDirIdx],
					                 targetPos);
			int minDirIndex = currentDirIdx;
			for (int i = 1; i < 8; ++i) {
				PointF dir = mDirections[(currentDirIdx + i) % 8];
				float dist = distance(currentPos, dir, targetPos);
				if (dist < minDist) {
					minDist = dist;
					minDirIndex = (currentDirIdx + i) % 8;
				}
			}

			final float splineLen = Math.max(rand(.3f, .5f),
					distance(currentPos, targetPos) / 2f);

			if (minDirIndex != currentDirIdx) {
				int k = minDirIndex > currentDirIdx ? 1 : -1;
				for (int i = currentDirIdx + k; 
					 i * k <= minDirIndex * k; i += 2 * k) {
					PointF dir = mDirections[i];
					PointF normal = mDirections[(8 + i - 2 * k) % 8];
					Spline spline = element.getNextSpline();
					spline.mWidthStart = spline.mWidthEnd = rootWidth;
					genArc(spline, currentPos, dir, splineLen, normal,
							i == minDirIndex);

					if (Math.random() < mBranchPropability) {
						Branch b = element.getCurrentBranch();
						int branchDir = Math.random() < 0.5 ? -k : k;
						float branchLen = Math.min(splineLen, .5f) * 
								          rand(.6f, .8f);
						genBranch(b, currentPos, i + branchDir, branchDir,
								  branchLen);
					}

					currentPos.set(spline.mPoints[3]);
				}
				currentDirIdx = minDirIndex;
			} else {
				PointF dir = mDirections[currentDirIdx];
				Spline spline = element.getNextSpline();
				spline.mWidthStart = spline.mWidthEnd = rootWidth;
				genLine(spline, currentPos, dir, splineLen);

				if (Math.random() < mBranchPropability) {
					Branch b = element.getCurrentBranch();
					int branchDir = Math.random() < 0.5 ? -1 : 1;
					float branchLen = Math.min(splineLen, .5f) * rand(.6f, .8f);
					genBranch(b, currentPos, currentDirIdx + branchDir,
							  branchDir, branchLen);
				}

				currentPos.set(spline.mPoints[3]);
			}

			additionTime += element.mDuration;
			lastElement = element;
		}
		flower.mCurrentDirIndex = currentDirIdx;
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
		for (int i = 0; i < flowersList.length; i++) {
			pointsList.clear();
			splinesList.clear();

			Flower flower = flowersList[i];
			this.update(flower, renderTime, offset);
			flower.getRenderStructs(splinesList, 
									pointsList,
									renderTime);
			this.renderSplines(splinesList, flower.mColor, offset);
			this.renderFlowers(pointsList, flower.mColor, offset);
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
		mAspectRatio.x = (float) Math.min(width, height) / width;
		mAspectRatio.y = (float) Math.min(width, height) / height;
		for (int i = 0; i < 8; ++i) {
			PointF dir = mDirections[i];
			dir.set(directions[i * 2 + 0], directions[i * 2 + 1]);
			float lenInv = 1f / dir.length();
			dir.x *= mAspectRatio.x * lenInv;
			dir.y *= mAspectRatio.y * lenInv;
		}
		for (Flower flower : flowersList) {
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
		this.mShaderSpline.setProgram(
				                  context.getString(R.string.shader_spline_vs),
				                  context.getString(R.string.shader_spline_fs));
		this.mShaderTexture.setProgram(
				                 context.getString(R.string.shader_texture_vs),
				                 context.getString(R.string.shader_texture_fs));

		GLES20.glDeleteTextures(1, mFlowerTextureId, 0);
		GLES20.glGenTextures(1, mFlowerTextureId, 0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFlowerTextureId[0]);
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
	 * Renders splines.
	 */
	public void renderSplines(Vector<Spline> splines, 
							  float[] color,
							  PointF offset) {
		this.mShaderSpline.useProgram();
		int uControlPts = this.mShaderSpline.getHandleID("uControlPts");
		int uWidth = this.mShaderSpline.getHandleID("uWidth");
		int uBounds = this.mShaderSpline.getHandleID("uBounds");
		int uColor = this.mShaderSpline.getHandleID("uColor");
		int uAspectRatio = this.mShaderSpline.getHandleID("uAspectRatio");
		int aSplinePos = this.mShaderSpline.getHandleID("aSplinePos");

		GLES20.glUniform2f(uAspectRatio, mAspectRatio.x, mAspectRatio.y);
		GLES20.glUniform4fv(uColor, 1, color, 0);
		GLES20.glVertexAttribPointer(aSplinePos, 2, GLES20.GL_FLOAT, false, 0,
									 mBufferSpline);
		GLES20.glEnableVertexAttribArray(aSplinePos);

		final float[] controlPts = new float[8];
		float boundX = Spline.WIDTH_MIN +
					   this.zoomLvl * 
					   (Spline.WIDTH_MAX - 
					    Spline.WIDTH_MIN);
		float boundY = 1f + boundX * mAspectRatio.y;
		boundX = 1f + boundX * mAspectRatio.x;

		for (Spline spline : splines) {
			int visiblePointCount = 0;
			for (int i = 0; i < 4; ++i) {
				float x = spline.mPoints[i].x - offset.x;
				float y = spline.mPoints[i].y - offset.y;
				controlPts[i * 2 + 0] = x;
				controlPts[i * 2 + 1] = y;
				if (Math.abs(x) < boundX && Math.abs(y) < boundY) {
					++visiblePointCount;
				}
			}
			if (visiblePointCount != 0) {
				GLES20.glUniform2fv(uControlPts, 4, controlPts, 0);
				GLES20.glUniform2f(uWidth, spline.mWidthStart, spline.mWidthEnd);
				GLES20.glUniform2f(uBounds, spline.mStartT, spline.mEndT);

				if (spline.mStartT != 0f || spline.mEndT != 1f) {
					int startIdx = (int) Math.floor(spline.mStartT *
							             (mSplineVertexCount - 1)) * 2;
					int endIdx = 2 + (int) Math.ceil(spline.mEndT * 
							     (mSplineVertexCount - 1)) * 2;
					GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 
										startIdx,
									    endIdx - startIdx);
				} else {
					GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 
										0,
										mSplineVertexCount * 2);
				}
			}
		}
	}
	
    /**                                                                          
     * Updates preference values.                                                
     */                                                                          
    public void setPreferences(int flowerCount, float[][] flowerColors,          
                               int splineQuality, float branchPropability,          
                               float zoomLevel) {                                
        if (flowerCount != flowersList.length) {                             
            flowersList = new Flower[flowerCount];                    
            for (int i = 0; i < flowersList.length; ++i) {                   
                flowersList[i] = new Flower();                        
                flowersList[i].mColor = flowerColors[i];                     
            }                                                                    
        }                                                                        
        for (int i = 0; i < flowersList.length; ++i) {                       
            flowersList[i].mColor = flowerColors[i];                         
        }                                                                        
                                                                                 
        if (mSplineVertexCount != splineQuality + 2) {                           
            mSplineVertexCount = splineQuality + 2;                              
            ByteBuffer bBuffer =                                                 
                          ByteBuffer.allocateDirect(4 * 4 * mSplineVertexCount); 
            mBufferSpline = bBuffer.order(ByteOrder.nativeOrder())               
                                   .asFloatBuffer();                             
            for (int i = 0; i < mSplineVertexCount; ++i) {                       
                float t = (float) i / (mSplineVertexCount - 1);                  
                mBufferSpline.put(t).put(1);                                     
                mBufferSpline.put(t).put(-1);                                    
            }                                                                    
            mBufferSpline.position(0);                                           
        }                                                                        
                                                                                 
        mBranchPropability = branchPropability;                                  
        this.zoomLvl = zoomLevel;                                                  
    }

    /**
     * INNER CLASSES
     */

	/**
	 * Branch element for handling branch data. Namely splines and points that
	 * create a branch.
	 */
	private final class Branch {
		
		public static final float WIDTH_MAX = 0.1f;
        public static final float WIDTH_MIN = 0.05f;
        
		public int mBranchPointCount;
		private final Point[] mBranchPoints = new Point[2];
		public int mBranchSplineCount;
		private final Spline[] mBranchSplines = new Spline[3];

		/**
		 * Default constructor.
		 */
		public Branch() {
			for (int i = 0; i < mBranchSplines.length; ++i) {
				mBranchSplines[i] = new Spline();
			}
			for (int i = 0; i < mBranchPoints.length; ++i) {
				mBranchPoints[i] = new Point();
			}
		}

		/**
		 * Returns next point structure.
		 */
		public Point getNextPoint() {
			return mBranchPoints[mBranchPointCount++];
		}

		/**
		 * Returns next splien structure.
		 */
		public Spline getNextSpline() {
			return mBranchSplines[mBranchSplineCount++];
		}

		/**
		 * Getter for splines and points this branch holds. Parameters startT
		 * and endT are values between [0, 1] plus additionally startT < endT.
		 */
		public void getRenderStructs(Vector<Spline> splines,
				                     Vector<Point> points, 
				                     float startT, float endT) {
			// First iterate over splines.
			for (int i = 0; i < mBranchSplineCount; ++i) {
				Spline spline = mBranchSplines[i];
				switch (i) {
				case 0:
					spline.mStartT = startT > 0f ? Math.min(startT * 2, 1f) : 
						                           0f;
					spline.mEndT = endT < 1f ? Math.min(endT * 2, 1f) : 1f;
					break;
				default:
					spline.mStartT = startT > 0f ? 
							         Math.max((startT - .5f) * 2, 0f) : 
							         0f;
					spline.mEndT = endT < 1f ? 
							       Math.max((endT - .5f) * 2, 0f) : 
							       1f;
					break;
				}
				splines.add(spline);
			}
			// Scale factor is calculated from current zoom level.
			// TODO: scaling might be best done during rendering.
			final float PT_SCALE_FACTOR = 
					                   Flower.POINT_SCALE_MIN +
					                   zoomLvl *       // this == FlowerObjects
					                   (Flower.POINT_SCALE_MAX -
					                   Flower.POINT_SCALE_MIN);
			// Iterate over points.
			for (int i = 0; i < mBranchPointCount; ++i) {
				Point point = mBranchPoints[i];
				float scale = endT - startT;
				if (mBranchSplineCount == 1) {
					scale = scale < 1f ? Math.max((scale - .5f) * 2, 0f) : 1f;
				}
				point.mScale = scale * PT_SCALE_FACTOR;
				points.add(point);
			}
		}

		/**
		 * Resets branch to initial state.
		 */
		public void reset() {
			mBranchSplineCount = mBranchPointCount = 0;
		}
	}

	/**
	 * Flower element for handling flower related data. Namely root elements
	 * which are used to build a flower.
	 */
	private final class Flower {

        public static final float POINT_SCALE_MAX = .24f;
        public static final float POINT_SCALE_MIN = .12f;
        public static final int ROOT_ELEMENT_COUNT = 6;
        public static final float ROOT_WIDTH_MAX = 0.12f;
        public static final float ROOT_WIDTH_MIN = 0.06f;

		public float[] mColor = new float[4];
		public int mCurrentDirIndex;
		public final PointF mCurrentPosition = new PointF();
		private int mRootElementCount;
		private final Vector<ElementRoot> mRootElements = new Vector<ElementRoot>();
		public final PointF mTargetPosition = new PointF();

		/**
		 * Default constructor.
		 */
		public Flower() {
			for (int i = 0; i < Flower.ROOT_ELEMENT_COUNT; ++i) {
				mRootElements.add(new ElementRoot());
			}
		}

		/**
		 * Returns last active root element. If there are none, returns next
		 * root element.
		 */
		public ElementRoot getLastRootElement() {
			if (mRootElementCount == 0) {
				return getNextRootElement();
			} else {
				return mRootElements.get(mRootElementCount - 1);
			}
		}

		/**
		 * Returns next root element.
		 */
		public ElementRoot getNextRootElement() {
			ElementRoot element;
			if (mRootElementCount < mRootElements.size()) {
				element = mRootElements.get(mRootElementCount++);
			} else {
				element = mRootElements.remove(0);
				mRootElements.add(element);
			}
			element.reset();
			return element;
		}

		/**
		 * Getter for spline and point structures for rendering. Time is current
		 * rendering time used for deciding which root element is fading in.
		 */
		public void getRenderStructs(Vector<Spline> splines,
				                     Vector<Point> points, long time) {
			ElementRoot lastElement = mRootElements.get(mRootElementCount - 1);
			float t = (float) (time - lastElement.mStartTime) / 
					  lastElement.mDuration;
			for (int i = 0; i < mRootElementCount; ++i) {
				ElementRoot element = mRootElements.get(i);
				if (i == mRootElementCount - 1) {
					element.getRenderStructs(splines, points, 0f, t);
				} else if (i == 0 && mRootElementCount == mRootElements.size()) {
					element.getRenderStructs(splines, points, t, 1f);
				} else {
					element.getRenderStructs(splines, points, 0f, 1f);
				}
			}
		}

		/**
		 * Resets this flower element to its initial state.
		 */
		public void reset() {
			mRootElementCount = 0;
			mCurrentDirIndex = 0;
			mCurrentPosition.set(0, 0);
		}

	}

	/**
	 * Root element for handling root related data. Root element consists of
	 * splines for actual root and branch elements.
	 */
	private final class ElementRoot {

		private final Branch[] mBranchElements = new Branch[5];
		private int mRootSplineCount;
		private final Spline[] mRootSplines = new Spline[5];
		private long mStartTime, mDuration;

		/**
		 * Default constructor.
		 */
		public ElementRoot() {
			for (int i = 0; i < 5; ++i) {
				mRootSplines[i] = new Spline();
				mBranchElements[i] = new Branch();
			}
		}

		/**
		 * Returns branch for current root spline.
		 */
		public Branch getCurrentBranch() {
			return mBranchElements[mRootSplineCount - 1];
		}

		/**
		 * Returns next spline structure.
		 */
		public Spline getNextSpline() {
			mBranchElements[mRootSplineCount].reset();
			return mRootSplines[mRootSplineCount++];
		}

		/**
		 * Getter for spline and point structs for rendering. Values startT and
		 * endT are between [0, 1] plus additionally startT <= endT.
		 */
		public void getRenderStructs(Vector<Spline> splines,
				                     Vector<Point> points, float startT, 
				                     float endT) {
			for (int i = 0; i < mRootSplineCount; ++i) {
				Spline spline = mRootSplines[i];
				if (startT != 0f || endT != 1f) {
					float localStartT = (float) i / mRootSplineCount;
					float localEndT = (float) (i + 1) / mRootSplineCount;
					spline.mStartT = Math.min(Math.max((startT - localStartT) / 
							         (localEndT - localStartT), 0f), 1f);
					spline.mEndT = Math.min(Math.max((endT - localStartT) / 
							       (localEndT - localStartT), 0f), 1f);
				} else {
					spline.mStartT = 0f;
					spline.mEndT = 1f;
				}

				if (spline.mStartT != spline.mEndT) {
					splines.add(spline);
					mBranchElements[i].getRenderStructs(splines, points, 
							                            spline.mStartT, 
							                            spline.mEndT);
				}
			}
		}

		/**
		 * Resets root element to its initial state.
		 */
		public void reset() {
			mRootSplineCount = 0;
			mStartTime = mDuration = 0;
		}
	}

	/**
	 * Holder for point data.
	 */
	private final class Point {
		public final PointF mPosition = new PointF();
		public float mRotationSin, mRotationCos;
		public float mScale;
	}

	/**
	 * Holder for spline data.
	 */
	private final class Spline {
		
		public static final float WIDTH_MAX = Flower.ROOT_WIDTH_MAX;
		public static final float WIDTH_MIN = Flower.ROOT_WIDTH_MIN;
		
		public final PointF mPoints[] = new PointF[4];
		public float mStartT = 0f, mEndT = 1f;
		public float mWidthStart, mWidthEnd;

		public Spline() {
			for (int i = 0; i < mPoints.length; ++i) {
				mPoints[i] = new PointF();
			}
		}
	}

}
