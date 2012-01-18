package fi.harism.wallpaper.ornament;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class OrnamentRenderer implements GLSurfaceView.Renderer {
	
	private Context mContext;
	
	public OrnamentRenderer(Context context) {
		mContext = context;
	}

	@Override
	public void onDrawFrame(GL10 unused) {
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
	}
	
	public void setXOffset(float offset) {
	}

}
