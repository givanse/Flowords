package fi.harism.wallpaper.ornament;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Vector;

import android.content.Context;
import android.graphics.PointF;
import android.opengl.GLES20;
import android.os.SystemClock;

public final class OrnamentPlants {

	private static final float[] DIRECTIONS = { 0, 1, 1, 1, 1, 0, 1, -1, 0, -1,
			-1, -1, -1, 0, -1, 1 };
	private FloatBuffer mBufferSpline;

	private final PointF[] mDirections = new PointF[8];
	private final Plant[] mPlants = new Plant[OrnamentConstants.PLANT_COUNT];

	private OrnamentShader mShaderSpline = new OrnamentShader();
	private Vector<Spline> mSplines = new Vector<Spline>();
	private int mSplineVertexCount;

	private int mWidth, mHeight;

	public OrnamentPlants() {
		mSplineVertexCount = OrnamentConstants.SPLINE_SPLIT_COUNT + 2;

		ByteBuffer bBuffer = ByteBuffer
				.allocateDirect(4 * 4 * mSplineVertexCount);
		mBufferSpline = bBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
		for (int i = 0; i < mSplineVertexCount; ++i) {
			float t = (float) i / (mSplineVertexCount - 1);
			mBufferSpline.put(t).put(1);
			mBufferSpline.put(t).put(-1);
		}
		mBufferSpline.position(0);

		for (int i = 0; i < mDirections.length; ++i) {
			mDirections[i] = new PointF();
		}
		for (int i = 0; i < mPlants.length; ++i) {
			mPlants[i] = new Plant();
		}
	}

	public void onDrawFrame(PointF offset) {
		long renderTime = SystemClock.uptimeMillis();
		for (Plant plant : mPlants) {
			plant.update(renderTime, offset);
		}
		for (int i = 0; i < mPlants.length; ++i) {
			mSplines.clear();
			mPlants[i].getSplines(mSplines, renderTime);
			renderSplines(mSplines, OrnamentConstants.PLANT_COLORS[i], offset);
		}
	}

	public void onSurfaceChanged(int width, int height) {
		mWidth = width;
		mHeight = height;

		float aspectX = (float) Math.min(mWidth, mHeight) / mWidth;
		float aspectY = (float) Math.min(mWidth, mHeight) / mHeight;
		for (int i = 0; i < 8; ++i) {
			PointF dir = mDirections[i];
			dir.set(DIRECTIONS[i * 2 + 0], DIRECTIONS[i * 2 + 1]);
			float len = dir.length();
			dir.x /= len;
			dir.y /= len;
			dir.x *= aspectX;
			dir.y *= aspectY;
		}
	}

	public void onSurfaceCreated(Context context) {
		mShaderSpline.setProgram(context.getString(R.string.shader_spline_vs),
				context.getString(R.string.shader_spline_fs));
		for (int i = 0; i < mPlants.length; ++i) {
			mPlants[i].mRootElementCount = 0;
		}
	}

	public void renderSplines(Vector<Spline> splines, float[] color,
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
		GLES20.glUniform4fv(uColor, 1, color, 0);
		GLES20.glVertexAttribPointer(aSplinePos, 2, GLES20.GL_FLOAT, false, 0,
				mBufferSpline);
		GLES20.glEnableVertexAttribArray(aSplinePos);

		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

		final int[] controlIds = { uControl0, uControl1, uControl2, uControl3 };

		for (Spline spline : splines) {
			int visiblePointCount = 0;
			for (int i = 0; i < 4; ++i) {
				float x = spline.mCtrlPoints[i].x - offset.x;
				float y = spline.mCtrlPoints[i].y - offset.y;
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

		GLES20.glDisable(GLES20.GL_BLEND);
	}

	private final class Branch {
		private int mBranchSplineCount;
		private final Spline[] mBranchSplines = new Spline[3];

		public Branch() {
			for (int i = 0; i < mBranchSplines.length; ++i) {
				mBranchSplines[i] = new Spline();
			}
		}

		public Spline getNextSpline() {
			return mBranchSplines[mBranchSplineCount++];
		}

		public void getSplines(Vector<Spline> splines, float t) {
			for (int i = 0; i < mBranchSplineCount; ++i) {
				Spline spline = mBranchSplines[i];
				switch (i) {
				case 0:
					spline.mWidthStart = t
							* OrnamentConstants.SPLINE_BRANCH_WIDTH;
					if (mBranchSplineCount > 1) {
						spline.mWidthEnd = spline.mWidthStart / 2;
					} else {
						spline.mWidthEnd = 0f;
					}
					break;
				default:
					spline.mWidthStart = t
							* OrnamentConstants.SPLINE_BRANCH_WIDTH / 2;
					spline.mWidthEnd = 0f;
					break;
				}
				splines.add(spline);
			}
		}

	}

	private final class Plant {

		private int mCurrentDirIndex;
		private final PointF mCurrentPosition = new PointF();
		public int mRootElementCount;
		private final Vector<RootElement> mRootElements = new Vector<RootElement>();

		public Plant() {
			for (int i = 0; i < 6; ++i) {
				mRootElements.add(new RootElement());
			}
		}

		public void genArc(Spline spline, PointF start, PointF dir,
				float length, PointF normal, float normalPos1,
				float normalPos2, boolean flatEnd) {

			spline.mCtrlPoints[0].set(start);

			float Px = start.x + dir.x * normalPos1 + normal.x * normalPos1;
			float Py = start.y + dir.y * normalPos1 + normal.y * normalPos1;
			float Qx = start.x + dir.x * normalPos2;
			float Qy = start.y + dir.y * normalPos2;

			if (!flatEnd) {
				Qx += normal.x * (length - normalPos2);
				Qy += normal.y * (length - normalPos2);
			}

			spline.mCtrlPoints[1].set(Px, Py);
			spline.mCtrlPoints[2].set(Qx, Qy);

			spline.mCtrlPoints[3].set(start);
			spline.mCtrlPoints[3].offset(dir.x * length, dir.y * length);
		}

		private void genBranch(Branch branch, PointF pos, int startDir,
				int rotateDir, float len, float normalLen) {
			PointF p = new PointF();
			p.set(pos);

			PointF dir = mDirections[(8 + startDir + rotateDir) % 8];
			PointF normal = mDirections[(8 + startDir - rotateDir) % 8];
			Spline spline = branch.getNextSpline();
			genArc(spline, p, dir, len, normal, normalLen, len - normalLen,
					false);
			p.offset(dir.x * len, dir.y * len);

			int rand = OrnamentUtils.randI(0, 3);
			if (rand > 0) {
				dir = mDirections[(8 + startDir + 3 * rotateDir) % 8];
				normal = mDirections[(8 + startDir + rotateDir) % 8];
				spline = branch.getNextSpline();
				genArc(spline, p, dir, len, normal, normalLen, len - normalLen,
						false);
			}
			if (rand > 1) {
				dir = mDirections[(8 + startDir) % 8];
				normal = mDirections[(8 + startDir + 2 * rotateDir) % 8];
				spline = branch.getNextSpline();
				genArc(spline, p, dir, len * .5f, normal, normalLen * .5f,
						(len - normalLen) * .5f, false);
			}
		}

		public void genLine(Spline spline, PointF start, PointF dir,
				float length, PointF normal, float startOffset, float endOffset) {

			spline.mCtrlPoints[0].set(start);
			spline.mCtrlPoints[0].offset(normal.x * startOffset, normal.y
					* startOffset);

			spline.mCtrlPoints[1].set(start.x + dir.x * (length / 3f), start.y
					+ dir.y * (length / 3f));
			spline.mCtrlPoints[1].offset(normal.x * startOffset, normal.y
					* startOffset);

			spline.mCtrlPoints[2].set(start.x + dir.x * (2 * length / 3f),
					start.y + dir.y * (2 * length / 3f));
			spline.mCtrlPoints[2].offset(normal.x * endOffset, normal.y
					* endOffset);

			spline.mCtrlPoints[3].set(start.x + dir.x * length, start.y + dir.y
					* length);
			spline.mCtrlPoints[3].offset(normal.x * endOffset, normal.y
					* endOffset);
		}

		public void getSplines(Vector<Spline> splines, long time) {
			RootElement lastElement = mRootElements.get(mRootElementCount - 1);
			float t = (float) (time - lastElement.mStartTime)
					/ lastElement.mDuration;
			for (int i = 0; i < mRootElementCount; ++i) {
				RootElement element = mRootElements.get(i);
				if (i == mRootElementCount - 1) {
					element.getSplines(splines, 0f, t);
				} else if (i == 0 && mRootElementCount == mRootElements.size()) {
					element.getSplines(splines, t, 1f);
				} else {
					element.getSplines(splines, 0f, 1f);
				}
			}

		}

		public void update(long time, PointF offset) {
			if (mRootElementCount == 0) {
				RootElement element = mRootElements.get(mRootElementCount++);
				element.mStartTime = time;
				element.mDuration = OrnamentUtils.randI(500, 2000);
				element.mRootSplineCount = 0;

				OrnamentUtils.rand(mCurrentPosition, -.7f, -.7f, .7f, .7f);
				mCurrentPosition.offset(offset.x, offset.y);

				float randLen = OrnamentUtils.randF(.5f, .8f);
				mCurrentDirIndex = OrnamentUtils.randI(0, 8);
				PointF dir = mDirections[mCurrentDirIndex];
				PointF normal = mDirections[(mCurrentDirIndex + 2) % 8];
				Spline spline = element.getNextSpline();
				genLine(spline, mCurrentPosition, dir, randLen, normal, .2f, 0f);
				mCurrentPosition.offset(dir.x * randLen, dir.y * randLen);
			} else {
				RootElement lastElement = mRootElements
						.get(mRootElementCount - 1);
				long additionTime = time;
				while (time > lastElement.mStartTime + lastElement.mDuration) {

					RootElement element;
					if (mRootElementCount >= mRootElements.size()) {
						element = mRootElements.remove(0);
						mRootElements.add(element);
					} else {
						element = mRootElements.get(mRootElementCount++);
					}
					element.mStartTime = additionTime;
					element.mDuration = OrnamentUtils.randI(500, 2000);
					element.mRootSplineCount = 0;

					PointF targetPos = new PointF();
					OrnamentUtils.rand(targetPos, -.7f, -.7f, .7f, .7f);
					targetPos.offset(offset.x, offset.y);

					float minDist = OrnamentUtils.dist(mCurrentPosition,
							mDirections[mCurrentDirIndex], targetPos);
					int minDirIndex = mCurrentDirIndex;
					for (int i = 1; i < 8; ++i) {
						PointF dir = mDirections[(mCurrentDirIndex + i) % 8];
						float dist = OrnamentUtils.dist(mCurrentPosition, dir,
								targetPos);
						if (dist < minDist) {
							minDist = dist;
							minDirIndex = (mCurrentDirIndex + i) % 8;
						}
					}

					float randLen = OrnamentUtils.randF(.3f, .5f);
					randLen = Math
							.max(randLen, OrnamentUtils.dist(mCurrentPosition,
									targetPos) / 2f);
					// 2 * (sqrt(2) - 1) / 3
					float normalLen = 0.27614237f * randLen;

					if (minDirIndex != mCurrentDirIndex) {
						int k = minDirIndex > mCurrentDirIndex ? 1 : -1;
						for (int i = mCurrentDirIndex + k; i * k <= minDirIndex
								* k; i += 2 * k) {
							PointF dir = mDirections[i];
							PointF normal = mDirections[(8 + i - 2 * k) % 8];
							Spline spline = element.getNextSpline();
							genArc(spline, mCurrentPosition, dir, randLen,
									normal, normalLen, randLen - normalLen,
									i == minDirIndex);
							mCurrentPosition.offset(dir.x * randLen, dir.y
									* randLen);

							if (OrnamentUtils.randI(0, 3) == 0) {
								Branch b = element.getCurrentBranch();
								int branchDir = OrnamentUtils.randI(0, 2) == 0 ? -k
										: k;
								genBranch(b, mCurrentPosition, i + k,
										branchDir, randLen * .7f,
										normalLen * .7f);
							}
						}
						mCurrentDirIndex = minDirIndex;
					} else {
						PointF dir = mDirections[mCurrentDirIndex];
						PointF normal = mDirections[(mCurrentDirIndex + 2) % 8];
						float lineOffset = OrnamentUtils.randF(-.1f, .1f);
						Spline spline = element.getNextSpline();
						genLine(spline, mCurrentPosition, dir, randLen, normal,
								0f, lineOffset);
						mCurrentPosition.offset(dir.x * randLen, dir.y
								* randLen);
						spline = element.getNextSpline();
						genLine(spline, mCurrentPosition, dir, randLen, normal,
								lineOffset, 0f);
						mCurrentPosition.offset(dir.x * randLen, dir.y
								* randLen);

						Branch b = element.getCurrentBranch();
						int branchDir = OrnamentUtils.randI(0, 2) == 0 ? -1 : 1;
						genBranch(b, mCurrentPosition, mCurrentDirIndex,
								branchDir, randLen * .7f, normalLen * .7f);
					}

					lastElement = element;
					additionTime += lastElement.mDuration;
				}
			}
		}

	}

	private final class RootElement {

		private final Branch[] mBranches = new Branch[5];
		public int mRootSplineCount;
		private final Spline[] mRootSplines = new Spline[5];
		public long mStartTime, mDuration;

		public RootElement() {
			for (int i = 0; i < 5; ++i) {
				Spline spline = new Spline();
				spline.mWidthStart = spline.mWidthEnd = OrnamentConstants.SPLINE_ROOT_WIDTH;
				mRootSplines[i] = spline;
				mBranches[i] = new Branch();
			}
		}

		public Branch getCurrentBranch() {
			return mBranches[mRootSplineCount - 1];
		}

		public Spline getNextSpline() {
			mBranches[mRootSplineCount].mBranchSplineCount = 0;
			return mRootSplines[mRootSplineCount++];
		}

		public void getSplines(Vector<Spline> splines, float t1, float t2) {
			for (int i = 0; i < mRootSplineCount; ++i) {
				float startT = (float) i / mRootSplineCount;
				float endT = (float) (i + 1) / mRootSplineCount;
				Spline spline = mRootSplines[i];
				if (startT >= t1 && endT <= t2) {
					spline.mStartT = 0f;
					spline.mEndT = 1f;
					splines.add(spline);
					mBranches[i].getSplines(splines, 1f);
				} else if (startT < t1 && endT > t1) {
					spline.mStartT = (t1 - startT) / (endT - startT);
					spline.mEndT = 1f;
					splines.add(spline);
					mBranches[i].getSplines(splines, 1f - spline.mStartT);
				} else if (startT < t2 && endT > t2) {
					spline.mStartT = 0f;
					spline.mEndT = (t2 - startT) / (endT - startT);
					splines.add(spline);
					mBranches[i].getSplines(splines, spline.mEndT
							* spline.mEndT);
				}
			}
		}

	}

	private final class Spline {
		public final PointF mCtrlPoints[] = new PointF[] { new PointF(),
				new PointF(), new PointF(), new PointF() };
		public float mStartT = 0f, mEndT = 1f;
		public float mWidthStart, mWidthEnd;
	}

}
