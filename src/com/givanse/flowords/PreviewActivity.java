package com.givanse.flowords;

import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Resources;
import android.widget.Toast;

public class PreviewActivity extends Activity {

	private int REQUEST_CODE = 1;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
        
        if (Build.VERSION.SDK_INT >= 16) {
            /*
             * Open live wallpaper preview (API Level 16 or greater).
             */
        	// TODO: programatically set the .Flowords param
            ComponentName component = new ComponentName(getPackageName(), 
                    									getPackageName() + 
                    									".FlowordsService");
            intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component);
            this.startActivityForResult(intent, REQUEST_CODE);
	    } else {
	    	/*
			 * Open live wallpaper picker (API Level 15 or lower).
			 * Display a quick little message (toast) with instructions.
			 */
	    	intent.setAction(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
			Resources res = getResources();
			String hint = res.getString(R.string.toast_instruct_lwp_list) +
			              res.getString(R.string.app_name);
			Toast toast = Toast.makeText(this, hint, Toast.LENGTH_LONG);
			toast.show();
	    }
        
        finish();
    }

}