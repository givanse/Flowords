package fi.harism.wallpaper.ornament;

import java.util.Vector;

import android.graphics.PointF;

public class OrnamentPlant {

	private static final float[] DIRECTIONS = { 0, 1, 1, 1, 1, 0, 1, -1, 0, -1,
			-1, -1, -1, 0, -1, 1 };
	private final float[] mColor;
	private int mCurrentDirIndex;
	private final PointF mCurrentPosition = new PointF();
	private final PointF[] mDirections = new PointF[8];
	private final PointF mOffset = new PointF();
	private int mRootElementCount;
	private final Vector<OrnamentPlantRoot> mRootElements = new Vector<OrnamentPlantRoot>();

	public OrnamentPlant(float[] color) {
		mColor = color;
		for (int i = 0; i < 8; ++i) {
			mDirections[i] = new PointF();
		}
		for (int i = 0; i < 6; ++i) {
			mRootElements.add(new OrnamentPlantRoot());
		}
	}

	public float[] getColor() {
		return mColor;
	}

	public void getSplines(Vector<OrnamentSpline> splines, long time) {
		if (mRootElementCount == 0) {
			OrnamentPlantRoot rootElement = mRootElements
					.get(mRootElementCount++);
			rootElement.setStartTime(time);
			rootElement.setDuration(OrnamentUtils.randI(500, 2000));
			rootElement.reset();

			OrnamentUtils.rand(mCurrentPosition, -.7f, -.7f, .7f, .7f);
			mCurrentPosition.offset(mOffset.x, mOffset.y);

			float randLen = OrnamentUtils.randF(.5f, .8f);
			mCurrentDirIndex = OrnamentUtils.randI(0, 8);
			PointF dir = mDirections[mCurrentDirIndex];
			PointF normal = mDirections[(mCurrentDirIndex + 2) % 8];
			rootElement.addLine(mCurrentPosition, dir, randLen, normal, 1);
		} else {
			OrnamentPlantRoot lastElement = mRootElements
					.get(mRootElementCount - 1);
			long additionTime = time;
			while (time > lastElement.getStartTime()
					+ lastElement.getDuration()) {

				OrnamentPlantRoot element;
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
				targetPos.offset(mOffset.x, mOffset.y);

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
				randLen = Math.max(randLen,
						OrnamentUtils.dist(mCurrentPosition, targetPos) / 2f);

				if (minDirIndex != mCurrentDirIndex) {
					// 2 * (sqrt(2) - 1) / 3
					float normalLen = 0.27614237f * randLen;
					int k = minDirIndex > mCurrentDirIndex ? 1 : -1;
					for (int i = mCurrentDirIndex + k; i * k <= minDirIndex * k; i += 2 * k) {
						PointF dir = mDirections[i];
						PointF normal = mDirections[(8 + i - 2 * k) % 8];
						element.addArc(mCurrentPosition, dir, randLen, normal,
								normalLen, randLen - normalLen,
								i == minDirIndex);
					}
					mCurrentDirIndex = minDirIndex;
				} else {
					PointF dir = mDirections[mCurrentDirIndex];
					PointF normal = mDirections[(mCurrentDirIndex + 2) % 8];
					if (element.getSplineCount() == 0) {
						element.addLine(mCurrentPosition, dir, randLen, normal,
								2);
					}

				}

				lastElement = element;
				additionTime += lastElement.getDuration();
			}
		}

		OrnamentPlantRoot lastElement = mRootElements
				.get(mRootElementCount - 1);
		float t = (float) (time - lastElement.getStartTime())
				/ lastElement.getDuration();
		for (int i = 0; i < mRootElementCount; ++i) {
			OrnamentPlantRoot element = mRootElements.get(i);
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

	public void setAspectRatio(float aspectX, float aspectY) {
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

	public void setOffset(PointF offset) {
		mOffset.set(offset);
	}

}
