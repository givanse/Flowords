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
	private float mBranchPropability;

	private FloatBuffer mBufferSpline;
	private ByteBuffer mBufferTexture;

	private final PointF[] mDirections = new PointF[8];
	private float[][] mFlowerColors;
	private ElementFlower[] mFlowerElements;
	private final FlowerFbo mFlowerFbo = new FlowerFbo();
	private final Vector<StructPoint> mPointContainer = new Vector<StructPoint>();
	private final FlowerShader mShaderPoint = new FlowerShader();
	private final FlowerShader mShaderSpline = new FlowerShader();
	private final FlowerShader mShaderTexture = new FlowerShader();
	private final Vector<StructSpline> mSplineContainer = new Vector<StructSpline>();

	private int mSplineVertexCount;
	private int mWidth, mHeight;
	private float mZoomLevel;

	public FlowerObjects() {
		final byte TEXTURE_VERTICES[] = { -1, 1, -1, -1, 1, 1, 1, -1 };
		mBufferTexture = ByteBuffer.allocateDirect(2 * 4);
		mBufferTexture.put(TEXTURE_VERTICES).position(0);
		for (int i = 0; i < mDirections.length; ++i) {
			mDirections[i] = new PointF();
		}
	}

	private float distance(PointF point1, PointF point2) {
		float dx = point1.x - point2.x;
		float dy = point1.y - point2.y;
		return (float) Math.sqrt(dx * dx + dy * dy);
	}

	private float distance(PointF point1, PointF point2, PointF point3) {
		float dx = point1.x + point2.x - point3.x;
		float dy = point1.y + point2.y - point3.y;
		return (float) Math.sqrt(dx * dx + dy * dy);
	}

	private void genArc(StructSpline spline, PointF start, PointF dir,
			float length, PointF normal, float normalPos1, float normalPos2,
			boolean flatEnd) {

		for (PointF point : spline.mPoints) {
			point.set(start);
		}
		spline.mPoints[1].offset((dir.x + normal.x) * normalPos1,
				(dir.y + normal.y) * normalPos1);
		spline.mPoints[2].offset(dir.x * normalPos2, dir.y * normalPos2);
		if (!flatEnd) {
			spline.mPoints[2].offset(normal.x * (length - normalPos2), normal.y
					* (length - normalPos2));
		}
		spline.mPoints[3].offset(dir.x * length, dir.y * length);
	}

	private void genBranch(ElementBranch branch, PointF pos, int startDir,
			int rotateDir, float len, float normalLen) {

		float maxBranchWidth = FlowerConstants.FLOWER_BRANCH_WIDTH_MIN
				+ mZoomLevel
				* (FlowerConstants.FLOWER_BRANCH_WIDTH_MAX - FlowerConstants.FLOWER_BRANCH_WIDTH_MIN);
		PointF dir = mDirections[(8 + startDir + rotateDir) % 8];
		PointF normal = mDirections[(8 + startDir - rotateDir) % 8];
		StructSpline spline = branch.getSpline();
		spline.mWidthStart = maxBranchWidth;
		spline.mWidthEnd = 0f;
		genArc(spline, pos, dir, len, normal, normalLen, len - normalLen, false);
		pos = spline.mPoints[3];

		float rand = rand(0, 3);
		if (rand < 1) {
			StructPoint point = branch.getPoint();
			point.mPosition.set(pos);
			double rotation = Math.PI * 2 * startDir / 8;
			point.mRotationSin = (float) Math.sin(rotation);
			point.mRotationCos = (float) Math.cos(rotation);
		}
		if (rand >= 1) {
			spline.mWidthEnd = maxBranchWidth / 2;
			dir = mDirections[(8 + startDir + 3 * rotateDir) % 8];
			normal = mDirections[(8 + startDir + rotateDir) % 8];
			spline = branch.getSpline();
			spline.mWidthStart = maxBranchWidth / 2;
			spline.mWidthEnd = 0f;
			genArc(spline, pos, dir, len, normal, normalLen, len - normalLen,
					false);

			StructPoint point = branch.getPoint();
			point.mPosition.set(spline.mPoints[3]);
			double rotation = Math.PI * 2 * startDir / 8;
			point.mRotationSin = (float) Math.sin(rotation);
			point.mRotationCos = (float) Math.cos(rotation);
		}
		if (rand >= 2) {
			dir = mDirections[(8 + startDir) % 8];
			normal = mDirections[(8 + startDir + 2 * rotateDir) % 8];
			spline = branch.getSpline();
			spline.mWidthStart = maxBranchWidth / 2;
			spline.mWidthEnd = 0f;
			genArc(spline, pos, dir, len * .5f, normal, normalLen * .5f,
					(len - normalLen) * .5f, false);

			StructPoint point = branch.getPoint();
			point.mPosition.set(spline.mPoints[3]);
			double rotation = Math.PI * 2 * ((startDir + 1) % 8) / 8;
			point.mRotationSin = (float) Math.sin(rotation);
			point.mRotationCos = (float) Math.cos(rotation);
		}
	}

	private void genLine(StructSpline spline, PointF start, PointF dir,
			float length) {
		for (int i = 0; i < 4; ++i) {
			float t = (i * length) / 3;
			PointF point = spline.mPoints[i];
			point.set(start);
			point.offset(dir.x * t, dir.y * t);
		}
	}

	public void onDrawFrame(PointF offset) {
		long renderTime = SystemClock.uptimeMillis();
		for (ElementFlower flower : mFlowerElements) {
			update(flower, renderTime, offset);
		}

		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

		for (int i = 0; i < mFlowerElements.length; ++i) {
			mSplineContainer.clear();
			mPointContainer.clear();
			mFlowerElements[i].getRenderStructs(mSplineContainer,
					mPointContainer, renderTime);
			renderSplines(mSplineContainer, mFlowerColors[i], offset);
			renderFlowers(mPointContainer, mFlowerColors[i], offset);
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

		mFlowerFbo.init(256, 256, 1);
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
		GLES20.glUniform1f(uPointSize, 144);
		GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
		GLES20.glUniform1f(uPointSize, 96);
		GLES20.glDrawArrays(GLES20.GL_POINTS, 1, 6);

		GLES20.glUniform4f(uColor, 1f, 0, 0, 0);
		GLES20.glUniform1f(uPointSize, 120);
		GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
		GLES20.glUniform1f(uPointSize, 72);
		GLES20.glDrawArrays(GLES20.GL_POINTS, 1, 6);

		GLES20.glUniform4f(uColor, .8f, 0, 0, 0);
		GLES20.glUniform1f(uPointSize, 96);
		GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);

		GLES20.glUniform4f(uColor, 0, 0, 0, 0);
		GLES20.glUniform1f(uPointSize, 72);
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

		for (ElementFlower flower : mFlowerElements) {
			flower.reset();
		}
	}

	private float rand(float min, float max) {
		return min + (float) (Math.random() * (max - min));
	}

	private void renderFlowers(Vector<StructPoint> flowers, float[] color,
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

		for (StructPoint point : flowers) {
			GLES20.glUniform2f(uOffset, point.mPosition.x - offset.x,
					point.mPosition.y - offset.y);
			GLES20.glUniform2f(uRotation, point.mRotationSin,
					point.mRotationCos);
			GLES20.glUniform1f(uScale, point.mScale);
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

				int startIdx = (int) Math.floor(spline.mStartT
						* mSplineVertexCount) * 2;
				int endIdx = (int) (Math
						.ceil(spline.mEndT * mSplineVertexCount)) * 2;
				GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, startIdx, endIdx
						- startIdx);
			}
		}
	}

	public void setPreferences(int flowerCount, float[][] flowerColors,
			int splineQuality, float branchPropability, float zoomLevel) {
		mFlowerElements = new ElementFlower[flowerCount];
		for (int i = 0; i < mFlowerElements.length; ++i) {
			mFlowerElements[i] = new ElementFlower();
		}

		mFlowerColors = flowerColors;
		mSplineVertexCount = splineQuality + 2;

		ByteBuffer bBuffer = ByteBuffer
				.allocateDirect(4 * 4 * mSplineVertexCount);
		mBufferSpline = bBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
		for (int i = 0; i < mSplineVertexCount; ++i) {
			float t = (float) i / (mSplineVertexCount - 1);
			mBufferSpline.put(t).put(1);
			mBufferSpline.put(t).put(-1);
		}
		mBufferSpline.position(0);

		mBranchPropability = branchPropability;
		mZoomLevel = zoomLevel;
	}

	private void update(ElementFlower flower, long time, PointF offset) {
		float maxRootWidth = FlowerConstants.FLOWER_ROOT_WIDTH_MIN
				+ mZoomLevel
				* (FlowerConstants.FLOWER_ROOT_WIDTH_MAX - FlowerConstants.FLOWER_ROOT_WIDTH_MIN);

		PointF targetPos = flower.mTargetPosition;
		PointF currentPos = flower.mCurrentPosition;
		int currentDirIdx = flower.mCurrentDirIndex;
		ElementRoot lastElement = flower.getLastRootElement();
		long additionTime = time;
		while (time > lastElement.mStartTime + lastElement.mDuration) {
			ElementRoot element = flower.getNextRootElement();
			element.mStartTime = additionTime;
			element.mDuration = 1000 + (long) (500 * Math.sin(((SystemClock
					.uptimeMillis() % 5000) * Math.PI * 2) / 5000)); // FlowerUtils.randI(500,
																		// 1000);

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

			float randLen = rand(.3f, .5f);
			randLen = Math.max(randLen, distance(currentPos, targetPos) / 2f);
			// 2 * (sqrt(2) - 1) / 3
			float normalLen = 0.27614237f * randLen;

			if (minDirIndex != currentDirIdx) {
				int k = minDirIndex > currentDirIdx ? 1 : -1;
				for (int i = currentDirIdx + k; i * k <= minDirIndex * k; i += 2 * k) {
					PointF dir = mDirections[i];
					PointF normal = mDirections[(8 + i - 2 * k) % 8];
					StructSpline spline = element.getSpline();
					spline.mWidthStart = spline.mWidthEnd = maxRootWidth;
					genArc(spline, currentPos, dir, randLen, normal, normalLen,
							randLen - normalLen, i == minDirIndex);

					if (Math.random() < mBranchPropability) {
						ElementBranch b = element.getCurrentBranch();
						int branchDir = Math.random() < 0.5 ? -k : k;
						genBranch(b, currentPos, i, branchDir, randLen * .7f,
								normalLen * .7f);
					}

					currentPos.offset(dir.x * randLen, dir.y * randLen);
				}
				currentDirIdx = minDirIndex;
			} else {
				PointF dir = mDirections[currentDirIdx];
				StructSpline spline = element.getSpline();
				spline.mWidthStart = spline.mWidthEnd = maxRootWidth;
				genLine(spline, currentPos, dir, randLen);

				if (Math.random() < mBranchPropability) {
					ElementBranch b = element.getCurrentBranch();
					int branchDir = Math.random() < 0.5 ? -1 : 1;
					genBranch(b, currentPos, currentDirIdx, branchDir,
							randLen * .7f, normalLen * .7f);
				}

				currentPos.offset(dir.x * randLen, dir.y * randLen);
			}

			additionTime += element.mDuration;
			lastElement = element;
		}
		flower.mCurrentDirIndex = currentDirIdx;
	}

	private final class ElementBranch {
		public int mBranchPointCount;
		private final StructPoint[] mBranchPoints = new StructPoint[2];
		public int mBranchSplineCount;
		private final StructSpline[] mBranchSplines = new StructSpline[3];

		public ElementBranch() {
			for (int i = 0; i < mBranchSplines.length; ++i) {
				mBranchSplines[i] = new StructSpline();
			}
			for (int i = 0; i < mBranchPoints.length; ++i) {
				mBranchPoints[i] = new StructPoint();
			}
		}

		public StructPoint getPoint() {
			return mBranchPoints[mBranchPointCount++];
		}

		public void getRenderStructs(Vector<StructSpline> splines,
				Vector<StructPoint> points, float startT, float endT) {
			float maxPointSize = FlowerConstants.FLOWER_POINT_SIZE_MIN
					+ mZoomLevel
					* (FlowerConstants.FLOWER_POINT_SIZE_MAX - FlowerConstants.FLOWER_POINT_SIZE_MIN);
			for (int i = 0; i < mBranchSplineCount; ++i) {
				StructSpline spline = mBranchSplines[i];
				switch (i) {
				case 0:
					spline.mStartT = Math.min(startT * 2, 1f);
					spline.mEndT = Math.min(endT * 2, 1f);
					break;
				default:
					spline.mStartT = Math.max((startT - .5f) * 2, 0f);
					spline.mEndT = Math.max((endT - .5f) * 2, 0f);
					break;
				}
				splines.add(spline);
			}
			for (int i = 0; i < mBranchPointCount; ++i) {
				StructPoint point = mBranchPoints[i];
				if (mBranchSplineCount == 1) {
					point.mScale = Math.max((endT - startT - .5f) * 2, 0f);
				} else {
					point.mScale = Math.max(endT - startT, 0f);
				}
				point.mScale *= maxPointSize;
				points.add(point);
			}
		}

		public StructSpline getSpline() {
			return mBranchSplines[mBranchSplineCount++];
		}

		public void reset() {
			mBranchSplineCount = mBranchPointCount = 0;
		}
	}

	private final class ElementFlower {

		public int mCurrentDirIndex;
		public final PointF mCurrentPosition = new PointF();
		private int mRootElementCount;
		private final Vector<ElementRoot> mRootElements = new Vector<ElementRoot>();
		public final PointF mTargetPosition = new PointF();

		public ElementFlower() {
			for (int i = 0; i < FlowerConstants.FLOWER_ROOT_ELEMENT_COUNT; ++i) {
				mRootElements.add(new ElementRoot());
			}
		}

		public ElementRoot getLastRootElement() {
			if (mRootElementCount == 0) {
				return getNextRootElement();
			} else {
				return mRootElements.get(mRootElementCount - 1);
			}
		}

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

		public void getRenderStructs(Vector<StructSpline> splines,
				Vector<StructPoint> points, long time) {
			ElementRoot lastElement = mRootElements.get(mRootElementCount - 1);
			float t = (float) (time - lastElement.mStartTime)
					/ lastElement.mDuration;
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

		public void reset() {
			mRootElementCount = 0;
			mCurrentDirIndex = 0;
			mCurrentPosition.set(0, 0);
		}

	}

	private final class ElementRoot {

		private final ElementBranch[] mBranchElements = new ElementBranch[5];
		private int mRootSplineCount;
		private final StructSpline[] mRootSplines = new StructSpline[5];
		private long mStartTime, mDuration;

		public ElementRoot() {
			for (int i = 0; i < 5; ++i) {
				mRootSplines[i] = new StructSpline();
				mBranchElements[i] = new ElementBranch();
			}
		}

		public ElementBranch getCurrentBranch() {
			return mBranchElements[mRootSplineCount - 1];
		}

		public void getRenderStructs(Vector<StructSpline> splines,
				Vector<StructPoint> points, float startT, float endT) {
			for (int i = 0; i < mRootSplineCount; ++i) {
				StructSpline spline = mRootSplines[i];
				float localStartT = (float) i / mRootSplineCount;
				float localEndT = (float) (i + 1) / mRootSplineCount;
				spline.mStartT = Math.min(
						Math.max((startT - localStartT)
								/ (localEndT - localStartT), 0f), 1f);
				spline.mEndT = Math.min(
						Math.max((endT - localStartT)
								/ (localEndT - localStartT), 0f), 1f);
				if (spline.mStartT != spline.mEndT) {
					splines.add(spline);
					mBranchElements[i].getRenderStructs(splines, points,
							spline.mStartT, spline.mEndT);
				}
			}
		}

		public StructSpline getSpline() {
			mBranchElements[mRootSplineCount].reset();
			return mRootSplines[mRootSplineCount++];
		}

		public void reset() {
			mRootSplineCount = 0;
			mStartTime = mDuration = 0;
		}
	}

	private final class StructPoint {
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
