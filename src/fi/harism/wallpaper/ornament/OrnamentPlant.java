package fi.harism.wallpaper.ornament;

import java.util.Vector;

public class OrnamentPlant {

	private Vector<OrnamentPlantRoot> mRootElements = new Vector<OrnamentPlantRoot>();
	private float[] mColor;
	
	public OrnamentPlant(float[] color) {
		mColor = color;
	}
	
	public void reset() {
		mRootElements.clear();
	}
	
	public float[] getColor() {
		return mColor;
	}
	
	public void getSplines(Vector<OrnamentSpline> splines, long time) {
		if (mRootElements.size() == 0) {
			OrnamentPlantRoot rootElement = new OrnamentPlantRoot(time, OrnamentUtils.rand(1000, 2000));
			rootElement.init(null, OrnamentUtils.rand(-1f, 1f), OrnamentUtils.rand(-1f, 1f));
			mRootElements.add(rootElement);
		} else {
			OrnamentPlantRoot lastElement = mRootElements.lastElement();
			long additionTime = time;
			while (time > lastElement.getStartTime() + lastElement.getDuration()) {
				OrnamentPlantRoot newElement = new OrnamentPlantRoot(additionTime, OrnamentUtils.rand(1000, 2000));
				newElement.init(lastElement.getLastSpline(), OrnamentUtils.rand(-1f, 1f), OrnamentUtils.rand(-1f, 1f));
				mRootElements.add(newElement);
				lastElement = newElement;
				additionTime += lastElement.getDuration();
			}
			
			while (mRootElements.size() > 5) {
				mRootElements.remove(0);
			}
		}
		
		float t = (float)(time - mRootElements.lastElement().getStartTime()) / mRootElements.lastElement().getDuration();
		for (OrnamentPlantRoot rootElement : mRootElements) {
			if (mRootElements.lastElement() == rootElement) {
				rootElement.getSplines(splines, 0f, t);
			} else if (mRootElements.size() >= 5 && mRootElements.firstElement() == rootElement) {
				rootElement.getSplines(splines, t, 1f);
			} else {
				rootElement.getSplines(splines, 0f, 1f);
			}
		}
		
	}
	
}
