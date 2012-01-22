package fi.harism.wallpaper.flowers;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Vector;

import android.content.Context;
import android.graphics.PointF;
import android.opengl.GLES20;
import android.os.SystemClock;

public final class FlowerObjects {

	private static final float[] DIRECTIONS = { 0, 1, 1, 1, 1, 0, 1, -1, 0, -1,
			-1, -1, -1, 0, -1, 1 };
	private final PointF mAspectRatio = new PointF();
	private FloatBuffer mBufferSpline;

	private ByteBuffer mBufferTexture;
	private final PointF[] mDirections = new PointF[8];

	private final Vector<StructFlower> mFlowerContainer = new Vector<StructFlower>();
	private final FlowerFbo mFlowerFbo = new FlowerFbo();
	private final ElementPlant[] mPlants = new ElementPlant[FlowerConstants.PLANT_COUNT];
	private final FlowerShader mShaderPoint = new FlowerShader();
	private final FlowerShader mShaderSpline = new FlowerShader();
	private final FlowerShader mShaderTexture = new FlowerShader();
	private final Vector<StructSpline> mSplineContainer = new Vector<StructSpline>();
	private int mSplineVertexCount;
	private int mWidth, mHeight;

	public FlowerObjects() {
		mSplineVertexCount = FlowerConstants.SPLINE_SPLIT_COUNT + 2;

		ByteBuffer bBuffer = ByteBuffer
				.allocateDirect(4 * 4 * mSplineVertexCount);
		mBufferSpline = bBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
		for (int i = 0; i < mSplineVertexCount; ++i) {
			float t = (float) i / (mSplineVertexCount - 1);
			mBufferSpline.put(t).put(1);
			mBufferSpline.put(t).put(-1);
		}
		mBufferSpline.position(0);

		final byte TEXTURE_VERTICES[] = { -1, 1, -1, -1, 1, 1, 1, -1 };
		mBufferTexture = ByteBuffer.allocateDirect(2 * 4);
		mBufferTexture.put(TEXTURE_VERTICES).position(0);

		for (int i = 0; i < mDirections.length; ++i) {
			mDirections[i] = new PointF();
		}
		for (int i = 0; i < mPlants.length; ++i) {
			mPlants[i] = new ElementPlant();
		}
	}

	public void onDrawFrame(PointF offset) {
		long renderTime = SystemClock.uptimeMillis();
		for (ElementPlant plant : mPlants) {
			plant.update(renderTime, offset);
		}

		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

		for (int i = 0; i < mPlants.length; ++i) {
			mSplineContainer.clear();
			mFlowerContainer.clear();
			mPlants[i].getRenderElements(mSplineContainer, mFlowerContainer,
					renderTime);
			renderSplines(mSplineContainer, FlowerConstants.PLANT_COLORS[i],
					offset);
			renderFlowers(mFlowerContainer, FlowerConstants.PLANT_COLORS[i],
					offset);
		}

		GLES20.glDisable(GLES20.GL_BLEND);
	}

	public void onSurfaceChanged(int width, int height) {
		mWidth = width;
		mHeight = height;

		mAspectRatio.x = (float) Math.min(mWidth, mHeight) / mWidth;
		mAspectRatio.y = (float) Math.min(mWidth, mHeight) / mHeight;
		for (int i = 0; i < 8; ++i) {
			PointF dir = mDirections[i];
			dir.set(DIRECTIONS[i * 2 + 0], DIRECTIONS[i * 2 + 1]);
			float lenInv = 1f / dir.length();
			dir.x *= mAspectRatio.x * lenInv;
			dir.y *= mAspectRatio.y * lenInv;
		}

		mFlowerFbo.init(64, 64, 1);
		mFlowerFbo.bind();
		mFlowerFbo.bindTexture(0);
		GLES20.glClearColor(0f, 0f, 0f, 0f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

		ByteBuffer bBuffer = ByteBuffer.allocateDirect(6 * 2 * 4);
		FloatBuffer fBuffer = bBuffer.order(ByteOrder.nativeOrder())
				.asFloatBuffer();
		fBuffer.put(0).put(0);
		float dist = 1.7f / 3f;
		for (int i = 0; i < 5; ++i) {
			double r = Math.PI * 2 * i / 5;
			fBuffer.put((float) Math.sin(r) * dist).put(
					(float) Math.cos(r) * dist);
		}
		fBuffer.position(0);

		mShaderPoint.useProgram();
		int uPointSize = mShaderPoint.getHandle("uPointSize");
		int uColor = mShaderPoint.getHandle("uColor");
		int aPosition = mShaderPoint.getHandle("aPosition");

		GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT, false, 0,
				fBuffer);
		GLES20.glEnableVertexAttribArray(aPosition);

		GLES20.glUniform4f(uColor, .8f, 0, 0, 0);
		GLES20.glUniform1f(uPointSize, 36);
		GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
		GLES20.glUniform1f(uPointSize, 24);
		GLES20.glDrawArrays(GLES20.GL_POINTS, 1, 6);

		GLES20.glUniform4f(uColor, 1f, 0, 0, 0);
		GLES20.glUniform1f(uPointSize, 30);
		GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
		GLES20.glUniform1f(uPointSize, 18);
		GLES20.glDrawArrays(GLES20.GL_POINTS, 1, 6);

		GLES20.glUniform4f(uColor, .8f, 0, 0, 0);
		GLES20.glUniform1f(uPointSize, 24);
		GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);

		GLES20.glUniform4f(uColor, 0, 0, 0, 0);
		GLES20.glUniform1f(uPointSize, 18);
		GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
	}

	public void onSurfaceCreated(Context context) {
		mShaderSpline.setProgram(context.getString(R.string.shader_spline_vs),
				context.getString(R.string.shader_spline_fs));
		mShaderTexture.setProgram(
				context.getString(R.string.shader_texture_vs),
				context.getString(R.string.shader_texture_fs));
		mShaderPoint.setProgram(context.getString(R.string.shader_point_vs),
				context.getString(R.string.shader_point_fs));

		mFlowerFbo.reset();

		for (int i = 0; i < mPlants.length; ++i) {
			mPlants[i].mRootElementCount = 0;
		}
	}

	private void renderFlowers(Vector<StructFlower> flowers, float[] color,
			PointF offset) {

		mShaderTexture.useProgram();
		int uAspectRatio = mShaderTexture.getHandle("uAspectRatio");
		int uOffset = mShaderTexture.getHandle("uOffset");
		int uScale = mShaderTexture.getHandle("uScale");
		int uRotation = mShaderTexture.getHandle("uRotation");
		int uColor = mShaderTexture.getHandle("uColor");
		int aPosition = mShaderTexture.getHandle("aPosition");

		GLES20.glUniform2f(uAspectRatio, mAspectRatio.x, mAspectRatio.y);
		GLES20.glUniform4fv(uColor, 1, color, 0);
		GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_BYTE, false, 0,
				mBufferTexture);
		GLES20.glEnableVertexAttribArray(aPosition);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFlowerFbo.getTexture(0));

		for (StructFlower flower : flowers) {
			GLES20.glUniform2f(uOffset, flower.mPosition.x - offset.x,
					flower.mPosition.y - offset.y);
			GLES20.glUniform2f(uRotation, flower.mRotationSin,
					flower.mRotationCos);
			GLES20.glUniform1f(uScale, flower.mScale);
			GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		}
	}

	public void renderSplines(Vector<StructSpline> splines, float[] color,
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

		GLES20.glUniform2f(uAspectRatio, mAspectRatio.x, mAspectRatio.y);
		GLES20.glUniform4fv(uColor, 1, color, 0);
		GLES20.glVertexAttribPointer(aSplinePos, 2, GLES20.GL_FLOAT, false, 0,
				mBufferSpline);
		GLES20.glEnableVertexAttribArray(aSplinePos);

		final int[] controlIds = { uControl0, uControl1, uControl2, uControl3 };

		for (StructSpline spline : splines) {
			int visiblePointCount = 0;
			for (int i = 0; i < 4; ++i) {
				float x = spline.mPoints[i].x - offset.x;
				float y = spline.mPoints[i].y - offset.y;
				GLES20.glUniform2f(controlIds[i], x, y);
				if (x > -1f && x < 1f && y > -1f && y < 1f) {
					++visiblePointCount;
				}
			}
			if (visiblePointCount != 0) {
				GLES20.glUniform2f(uWidth, spline.mWidthStart, spline.mWidthEnd);
				GLES20.glUniform2f(uBounds, spline.mStartT, spline.mEndT);

				int startIdx = (int) (spline.mStartT * mSplineVertexCount) * 2;
				int endIdx = (int) (Math
						.ceil(spline.mEndT * mSplineVertexCount)) * 2;
				GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, startIdx, endIdx
						- startIdx);
			}
		}
	}

	private final class ElementBranch {
		public int mBranchSplineCount;
		private final StructSpline[] mBranchSplines = new StructSpline[3];
		public int mFlowerCount;
		private final StructFlower[] mFlowers = new StructFlower[2];

		public ElementBranch() {
			for (int i = 0; i < mBranchSplines.length; ++i) {
				mBranchSplines[i] = new StructSpline();
			}
			for (int i = 0; i < mFlowers.length; ++i) {
				mFlowers[i] = new StructFlower();
			}
		}

		public StructFlower getNextFlower() {
			return mFlowers[mFlowerCount++];
		}

		public StructSpline getNextSpline() {
			return mBranchSplines[mBranchSplineCount++];
		}

		public void getRenderElements(Vector<StructSpline> splines,
				Vector<StructFlower> flowers, float t) {
			for (int i = 0; i < mBranchSplineCount; ++i) {
				StructSpline spline = mBranchSplines[i];
				spline.mEndT = 1f;
				switch (i) {
				case 0:
					spline.mStartT = 1f - Math.max((t - .5f) * 2, 0f);
					break;
				default:
					spline.mStartT = 1f - Math.min(t * 2, 1f);
					break;
				}
				splines.add(spline);
			}
			for (int i = 0; i < mFlowerCount; ++i) {
				StructFlower flower = mFlowers[i];
				flower.mScale = Math.max((t - .5f) * 2, 0f);
				flower.mScale *= .1f;
				flowers.add(flower);
			}
		}
	}

	private final class ElementPlant {

		private int mCurrentDirIndex;
		private final PointF mCurrentPosition = new PointF();
		public int mRootElementCount;
		private final Vector<ElementRoot> mRootElements = new Vector<ElementRoot>();

		public ElementPlant() {
			for (int i = 0; i < 6; ++i) {
				mRootElements.add(new ElementRoot());
			}
		}

		public void genArc(StructSpline spline, PointF start, PointF dir,
				float length, PointF normal, float normalPos1,
				float normalPos2, boolean flatEnd) {

			for (PointF point : spline.mPoints) {
				point.set(start);
			}
			spline.mPoints[1].offset((dir.x + normal.x) * normalPos1,
					(dir.y + normal.y) * normalPos1);
			spline.mPoints[2].offset(dir.x * normalPos2, dir.y * normalPos2);
			if (!flatEnd) {
				spline.mPoints[2].offset(normal.x * (length - normalPos2),
						normal.y * (length - normalPos2));
			}
			spline.mPoints[3].offset(dir.x * length, dir.y * length);
		}

		private void genBranch(ElementBranch branch, PointF pos, int startDir,
				int rotateDir, float len, float normalLen) {
			PointF p = new PointF();
			p.set(pos);

			PointF dir = mDirections[(8 + startDir + rotateDir) % 8];
			PointF normal = mDirections[(8 + startDir - rotateDir) % 8];
			StructSpline spline = branch.getNextSpline();
			spline.mWidthStart = FlowerConstants.SPLINE_BRANCH_WIDTH;
			spline.mWidthEnd = 0f;
			genArc(spline, p, dir, len, normal, normalLen, len - normalLen,
					false);
			p.offset(dir.x * len, dir.y * len);

			int rand = FlowerUtils.randI(0, 3);
			if (rand == 0) {
				StructFlower flower = branch.getNextFlower();
				flower.mPosition.set(p);
				double rotation = Math.PI * 2 * startDir / 8;
				flower.mRotationSin = (float) Math.sin(rotation);
				flower.mRotationCos = (float) Math.cos(rotation);
			}
			if (rand > 0) {
				spline.mWidthEnd = FlowerConstants.SPLINE_BRANCH_WIDTH / 2;
				dir = mDirections[(8 + startDir + 3 * rotateDir) % 8];
				normal = mDirections[(8 + startDir + rotateDir) % 8];
				spline = branch.getNextSpline();
				spline.mWidthStart = FlowerConstants.SPLINE_BRANCH_WIDTH / 2;
				spline.mWidthEnd = 0f;
				genArc(spline, p, dir, len, normal, normalLen, len - normalLen,
						false);

				StructFlower flower = branch.getNextFlower();
				flower.mPosition.set(p);
				flower.mPosition.offset(dir.x * len, dir.y * len);
				double rotation = Math.PI * 2 * startDir / 8;
				flower.mRotationSin = (float) Math.sin(rotation);
				flower.mRotationCos = (float) Math.cos(rotation);
			}
			if (rand > 1) {
				dir = mDirections[(8 + startDir) % 8];
				normal = mDirections[(8 + startDir + 2 * rotateDir) % 8];
				spline = branch.getNextSpline();
				spline.mWidthStart = FlowerConstants.SPLINE_BRANCH_WIDTH / 2;
				spline.mWidthEnd = 0f;
				genArc(spline, p, dir, len * .5f, normal, normalLen * .5f,
						(len - normalLen) * .5f, false);

				StructFlower flower = branch.getNextFlower();
				flower.mPosition.set(p);
				flower.mPosition.offset(dir.x * len * .5f, dir.y * len * .5f);
				double rotation = Math.PI * 2 * ((startDir + 1) % 8) / 8;
				flower.mRotationSin = (float) Math.sin(rotation);
				flower.mRotationCos = (float) Math.cos(rotation);
			}
		}

		public void genLine(StructSpline spline, PointF start, PointF dir,
				float length) {

			for (int i = 0; i < 4; ++i) {
				float t = (i * length) / 3;
				PointF point = spline.mPoints[i];
				point.set(start);
				point.offset(dir.x * t, dir.y * t);
			}
		}

		public void getRenderElements(Vector<StructSpline> splines,
				Vector<StructFlower> flowers, long time) {
			ElementRoot lastElement = mRootElements.get(mRootElementCount - 1);
			float t = (float) (time - lastElement.mStartTime)
					/ lastElement.mDuration;
			for (int i = 0; i < mRootElementCount; ++i) {
				ElementRoot element = mRootElements.get(i);
				if (i == mRootElementCount - 1) {
					element.getRenderElements(splines, flowers, 0f, t);
				} else if (i == 0 && mRootElementCount == mRootElements.size()) {
					element.getRenderElements(splines, flowers, t, 1f);
				} else {
					element.getRenderElements(splines, flowers, 0f, 1f);
				}
			}
		}

		public void update(long time, PointF offset) {
			if (mRootElementCount == 0) {
				ElementRoot element = mRootElements.get(mRootElementCount++);
				element.mStartTime = time;
				element.mDuration = FlowerUtils.randI(500, 2000);
				element.mRootSplineCount = 0;

				FlowerUtils.rand(mCurrentPosition, -.5f, -.5f, .5f, .5f);
				mCurrentPosition.offset(offset.x, offset.y);

				float randLen = FlowerUtils.randF(.5f, .8f);
				mCurrentDirIndex = FlowerUtils.randI(0, 8);
				PointF dir = mDirections[mCurrentDirIndex];
				StructSpline spline = element.getNextSpline();
				genLine(spline, mCurrentPosition, dir, randLen);
				mCurrentPosition.offset(dir.x * randLen, dir.y * randLen);
			} else {
				ElementRoot lastElement = mRootElements
						.get(mRootElementCount - 1);
				long additionTime = time;
				while (time > lastElement.mStartTime + lastElement.mDuration) {

					ElementRoot element;
					if (mRootElementCount >= mRootElements.size()) {
						element = mRootElements.remove(0);
						mRootElements.add(element);
					} else {
						element = mRootElements.get(mRootElementCount++);
					}
					element.mStartTime = additionTime;
					element.mDuration = FlowerUtils.randI(500, 2000);
					element.mRootSplineCount = 0;

					PointF targetPos = new PointF();
					FlowerUtils.rand(targetPos, -.5f, -.5f, .5f, .5f);
					targetPos.offset(offset.x, offset.y);

					float minDist = FlowerUtils.dist(mCurrentPosition,
							mDirections[mCurrentDirIndex], targetPos);
					int minDirIndex = mCurrentDirIndex;
					for (int i = 1; i < 8; ++i) {
						PointF dir = mDirections[(mCurrentDirIndex + i) % 8];
						float dist = FlowerUtils.dist(mCurrentPosition, dir,
								targetPos);
						if (dist < minDist) {
							minDist = dist;
							minDirIndex = (mCurrentDirIndex + i) % 8;
						}
					}

					float randLen = FlowerUtils.randF(.3f, .5f);
					randLen = Math.max(randLen,
							FlowerUtils.dist(mCurrentPosition, targetPos) / 2f);
					// 2 * (sqrt(2) - 1) / 3
					float normalLen = 0.27614237f * randLen;

					if (minDirIndex != mCurrentDirIndex) {
						int k = minDirIndex > mCurrentDirIndex ? 1 : -1;
						for (int i = mCurrentDirIndex + k; i * k <= minDirIndex
								* k; i += 2 * k) {
							PointF dir = mDirections[i];
							PointF normal = mDirections[(8 + i - 2 * k) % 8];
							StructSpline spline = element.getNextSpline();
							genArc(spline, mCurrentPosition, dir, randLen,
									normal, normalLen, randLen - normalLen,
									i == minDirIndex);

							if (FlowerUtils.randI(0, 3) == 0) {
								ElementBranch b = element.getCurrentBranch();
								int branchDir = FlowerUtils.randI(0, 2) == 0 ? -k
										: k;
								genBranch(b, mCurrentPosition, i, branchDir,
										randLen * .7f, normalLen * .7f);
							}

							mCurrentPosition.offset(dir.x * randLen, dir.y
									* randLen);
						}
						mCurrentDirIndex = minDirIndex;
					} else {
						PointF dir = mDirections[mCurrentDirIndex];
						StructSpline spline = element.getNextSpline();
						genLine(spline, mCurrentPosition, dir, randLen);

						ElementBranch b = element.getCurrentBranch();
						int branchDir = FlowerUtils.randI(0, 2) == 0 ? -1 : 1;
						genBranch(b, mCurrentPosition, mCurrentDirIndex,
								branchDir, randLen * .7f, normalLen * .7f);

						mCurrentPosition.offset(dir.x * randLen, dir.y
								* randLen);
					}

					lastElement = element;
					additionTime += lastElement.mDuration;
				}
			}
		}

	}

	private final class ElementRoot {

		private final ElementBranch[] mBranches = new ElementBranch[5];
		public int mRootSplineCount;
		private final StructSpline[] mRootSplines = new StructSpline[5];
		public long mStartTime, mDuration;

		public ElementRoot() {
			for (int i = 0; i < 5; ++i) {
				StructSpline spline = new StructSpline();
				spline.mWidthStart = spline.mWidthEnd = FlowerConstants.SPLINE_ROOT_WIDTH;
				mRootSplines[i] = spline;
				mBranches[i] = new ElementBranch();
			}
		}

		public ElementBranch getCurrentBranch() {
			return mBranches[mRootSplineCount - 1];
		}

		public StructSpline getNextSpline() {
			mBranches[mRootSplineCount].mBranchSplineCount = 0;
			mBranches[mRootSplineCount].mFlowerCount = 0;
			return mRootSplines[mRootSplineCount++];
		}

		public void getRenderElements(Vector<StructSpline> splines,
				Vector<StructFlower> flowers, float t1, float t2) {
			for (int i = 0; i < mRootSplineCount; ++i) {
				float startT = (float) i / mRootSplineCount;
				float endT = (float) (i + 1) / mRootSplineCount;
				StructSpline spline = mRootSplines[i];
				if (startT >= t1 && endT <= t2) {
					spline.mStartT = 0f;
					spline.mEndT = 1f;
					splines.add(spline);
					mBranches[i].getRenderElements(splines, flowers, 1f);
				} else if (startT < t1 && endT > t1) {
					spline.mStartT = (t1 - startT) / (endT - startT);
					spline.mEndT = 1f;
					splines.add(spline);
					mBranches[i].getRenderElements(splines, flowers,
							1f - spline.mStartT);
				} else if (startT < t2 && endT > t2) {
					spline.mStartT = 0f;
					spline.mEndT = (t2 - startT) / (endT - startT);
					splines.add(spline);
					mBranches[i].getRenderElements(splines, flowers,
							spline.mEndT);
				}
			}
		}
	}

	private final class StructFlower {
		public final PointF mPosition = new PointF();
		public float mRotationSin, mRotationCos;
		public float mScale;
	}

	private final class StructSpline {
		public final PointF mPoints[] = new PointF[4];
		public float mStartT = 0f, mEndT = 1f;
		public float mWidthStart, mWidthEnd;

		public StructSpline() {
			for (int i = 0; i < mPoints.length; ++i) {
				mPoints[i] = new PointF();
			}
		}
	}

}
