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
		for (int i = 0; i < mPlants.length; ++i) {
			mSplines.clear();
			mPlants[i].getSplines(mSplines, renderTime, offset);
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
			mPlants[i].reset();
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
		GLES20.glUniform3fv(uColor, 1, color, 0);
		GLES20.glVertexAttribPointer(aSplinePos, 2, GLES20.GL_FLOAT, false, 0,
				mBufferSpline);
		GLES20.glEnableVertexAttribArray(aSplinePos);

		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

		final int[] controlIds = { uControl0, uControl1, uControl2, uControl3 };

		for (Spline spline : splines) {
			if (renderSplinesSetControlPoints(controlIds, spline, offset)) {
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

	private boolean renderSplinesSetControlPoints(int[] ids, Spline spline,
			PointF offset) {
		int ret = 0;
		for (int i = 0; i < 4; ++i) {
			float x = spline.mCtrlPoints[i].x - offset.x;
			float y = spline.mCtrlPoints[i].y - offset.y;
			GLES20.glUniform2f(ids[i], x, y);
			if (x > -1f && x < 1f && y > -1f && y < 1f) {
				++ret;
			}
		}
		return ret != 0;
	}

	private final class Plant {

		private int mCurrentDirIndex;
		private final PointF mCurrentPosition = new PointF();
		private int mRootElementCount;
		private final Vector<RootElement> mRootElements = new Vector<RootElement>();

		public Plant() {
			for (int i = 0; i < 6; ++i) {
				mRootElements.add(new RootElement());
			}
		}

		public void getSplines(Vector<Spline> splines, long time, PointF offset) {
			if (mRootElementCount == 0) {
				RootElement element = mRootElements.get(mRootElementCount++);
				element.setStartTime(time);
				element.setDuration(OrnamentUtils.randI(500, 2000));
				element.reset();

				OrnamentUtils.rand(mCurrentPosition, -.7f, -.7f, .7f, .7f);
				mCurrentPosition.offset(offset.x, offset.y);

				float randLen = OrnamentUtils.randF(.5f, .8f);
				mCurrentDirIndex = OrnamentUtils.randI(0, 8);
				PointF dir = mDirections[mCurrentDirIndex];
				PointF normal = mDirections[(mCurrentDirIndex + 2) % 8];
				element.addLine(mCurrentPosition, dir, randLen, normal, 1);
			} else {
				RootElement lastElement = mRootElements
						.get(mRootElementCount - 1);
				long additionTime = time;
				while (time > lastElement.getStartTime()
						+ lastElement.getDuration()) {

					RootElement element;
					if (mRootElementCount >= mRootElements.size()) {
						element = mRootElements.remove(0);
						mRootElements.add(element);
					} else {
						element = mRootElements.get(mRootElementCount++);
					}
					element.setStartTime(additionTime);
					element.setDuration(OrnamentUtils.randI(500, 2000));
					element.reset();

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

					if (minDirIndex != mCurrentDirIndex) {
						// 2 * (sqrt(2) - 1) / 3
						float normalLen = 0.27614237f * randLen;
						int k = minDirIndex > mCurrentDirIndex ? 1 : -1;
						for (int i = mCurrentDirIndex + k; i * k <= minDirIndex
								* k; i += 2 * k) {
							PointF dir = mDirections[i];
							PointF normal = mDirections[(8 + i - 2 * k) % 8];
							element.addArc(mCurrentPosition, dir, randLen,
									normal, normalLen, randLen - normalLen,
									i == minDirIndex);
						}
						mCurrentDirIndex = minDirIndex;
					} else {
						PointF dir = mDirections[mCurrentDirIndex];
						PointF normal = mDirections[(mCurrentDirIndex + 2) % 8];
						if (element.getSplineCount() == 0) {
							element.addLine(mCurrentPosition, dir, randLen,
									normal, 2);
						}

					}

					lastElement = element;
					additionTime += lastElement.getDuration();
				}
			}

			RootElement lastElement = mRootElements.get(mRootElementCount - 1);
			float t = (float) (time - lastElement.getStartTime())
					/ lastElement.getDuration();
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

		public void reset() {
			mRootElementCount = 0;
		}

	}

	private final class RootElement {

		private int mRootSplineCount;
		private final Spline[] mRootSplines = new Spline[5];
		private long mStartTime, mDuration;

		public RootElement() {
			for (int i = 0; i < 5; ++i) {
				Spline spline = new Spline();
				spline.mWidthStart = spline.mWidthEnd = OrnamentConstants.SPLINE_ROOT_WIDTH;
				mRootSplines[i] = spline;
			}
		}

		public void addArc(PointF start, PointF dir, float length,
				PointF normal, float normalPos1, float normalPos2,
				boolean flatEnd) {
			Spline spline = mRootSplines[mRootSplineCount++];
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

			start.offset(dir.x * length, dir.y * length);
			spline.mCtrlPoints[3].set(start);
		}

		public void addLine(PointF start, PointF dir, float length,
				PointF normal, int count) {
			float randLen = 0, offsetX = 0, offsetY = 0;
			for (int i = 0; i < count; ++i) {
				Spline spline = mRootSplines[mRootSplineCount++];
				for (int j = 0; j < 4; ++j) {
					PointF P = spline.mCtrlPoints[j];
					P.set(start);
					P.offset(length * dir.x * (j / 3f), length * dir.y
							* (j / 3f));
					P.offset(offsetX, offsetY);

					if (j == 1) {
						if (i < count - 1) {
							randLen = OrnamentUtils.randF(-.1f, .1f);
							offsetX = normal.x * randLen;
							offsetY = normal.y * randLen;
						} else {
							randLen = offsetX = offsetY = 0;
						}
					}
				}
				start.set(spline.mCtrlPoints[3]);
				start.offset(-offsetX, -offsetY);
			}
		}

		public long getDuration() {
			return mDuration;
		}

		public int getSplineCount() {
			return mRootSplineCount;
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
				} else if (startT < t1 && endT > t1) {
					spline.mStartT = (t1 - startT) / (endT - startT);
					spline.mEndT = 1f;
					splines.add(spline);
				} else if (startT < t2 && endT > t2) {
					spline.mStartT = 0f;
					spline.mEndT = (t2 - startT) / (endT - startT);
					splines.add(spline);
				}
			}
		}

		public long getStartTime() {
			return mStartTime;
		}

		public void reset() {
			mRootSplineCount = 0;
		}

		public void setDuration(long duration) {
			mDuration = duration;
		}

		public void setStartTime(long startTime) {
			mStartTime = startTime;
		}

	}

	private final class Spline {
		final PointF mCtrlPoints[] = new PointF[] { new PointF(), new PointF(),
				new PointF(), new PointF() };
		float mStartT, mEndT;
		float mWidthStart, mWidthEnd;
	}

}
