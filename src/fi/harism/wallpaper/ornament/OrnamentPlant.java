package fi.harism.wallpaper.ornament;

import java.util.Vector;

import android.graphics.PointF;

public class OrnamentPlant {

	private static final float[] DIRECTIONS = { 0, 1, 1, 1, 1, 0, 1, -1, 0, -1,
			-1, -1, -1, 0, -1, 1 };
	private final float[] mColor;
	private final PointF mCurrentDir = new PointF();
	private int mCurrentDirIndex;
	private final PointF mCurrentPos = new PointF();
	private final PointF[] mDirections = new PointF[8];
	private final PointF mOffset = new PointF();
	private final Vector<OrnamentPlantRoot> mRootElements = new Vector<OrnamentPlantRoot>();

	public OrnamentPlant(float[] color) {
		mColor = color;
		for (int i = 0; i < 8; ++i) {
			PointF dir = new PointF(DIRECTIONS[i * 2 + 0],
					DIRECTIONS[i * 2 + 1]);
			float len = dir.length();
			dir.x /= len;
			dir.y /= len;
			mDirections[i] = dir;
		}
	}

	public float[] getColor() {
		return mColor;
	}

	public void getSplines(Vector<OrnamentSpline> splines, long time) {
		if (mRootElements.size() == 0) {
			OrnamentPlantRoot rootElement = new OrnamentPlantRoot(time,
					OrnamentUtils.randI(500, 2000));

			mCurrentDirIndex = OrnamentUtils.randI(0, 8);
			mCurrentDir.set(mDirections[mCurrentDirIndex]);
			float randLen = OrnamentUtils.randF(.3f, 1f);
			mCurrentDir.x *= randLen;
			mCurrentDir.y *= randLen;

			PointF startPos = new PointF();
			OrnamentUtils.rand(startPos, -.5f, -.5f, .5f, .5f);
			OrnamentSpline startSpline = OrnamentUtils.genSpline(startPos,
					startPos, startPos, startPos);
			mCurrentPos.set(startPos);
			mCurrentPos.offset(mCurrentDir.x, mCurrentDir.y);

			rootElement.init(startSpline, mCurrentDir,
					mDirections[(mCurrentDirIndex + 2) % 8]);
			mRootElements.add(rootElement);
		} else {
			OrnamentPlantRoot lastElement = mRootElements.lastElement();
			long additionTime = time;
			while (time > lastElement.getStartTime()
					+ lastElement.getDuration()) {
				OrnamentPlantRoot newElement = new OrnamentPlantRoot(
						additionTime, OrnamentUtils.randI(500, 2000));

				PointF targetPos = new PointF();
				OrnamentUtils.rand(targetPos, -.5f, -.5f, .5f, .5f);
				targetPos.offset(mOffset.x, mOffset.y);

				float minDist = OrnamentUtils.dist(mCurrentPos,
						mDirections[mCurrentDirIndex], targetPos);
				for (int i = 1; i <= 7; i += 6) {
					PointF dir = mDirections[(mCurrentDirIndex + i) % 8];
					float dist = OrnamentUtils
							.dist(mCurrentPos, dir, targetPos);
					if (dist < minDist) {
						minDist = dist;
						mCurrentDirIndex = (mCurrentDirIndex + i) % 8;
					}
				}

				mCurrentDir.set(mDirections[mCurrentDirIndex]);
				float randLen = OrnamentUtils.randF(.3f, 1f);
				mCurrentDir.x *= randLen;
				mCurrentDir.y *= randLen;
				mCurrentPos.offset(mCurrentDir.x, mCurrentDir.y);

				newElement.init(lastElement.getLastSpline(), mCurrentDir,
						mDirections[(mCurrentDirIndex + 2) % 8]);
				mRootElements.add(newElement);
				lastElement = newElement;
				additionTime += lastElement.getDuration();
			}

			while (mRootElements.size() > 5) {
				mRootElements.remove(0);
			}
		}

		float t = (float) (time - mRootElements.lastElement().getStartTime())
				/ mRootElements.lastElement().getDuration();
		for (OrnamentPlantRoot rootElement : mRootElements) {
			if (mRootElements.lastElement() == rootElement) {
				rootElement.getSplines(splines, 0f, t);
			} else if (mRootElements.size() >= 5
					&& mRootElements.firstElement() == rootElement) {
				rootElement.getSplines(splines, t, 1f);
			} else {
				rootElement.getSplines(splines, 0f, 1f);
			}
		}

	}

	public void reset() {
		mRootElements.clear();
	}

	public void setOffset(PointF offset) {
		mOffset.set(offset);
	}

}
