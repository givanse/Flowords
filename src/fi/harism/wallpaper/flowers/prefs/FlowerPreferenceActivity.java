package fi.harism.wallpaper.flowers.prefs;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import fi.harism.wallpaper.flowers.R;

public final class FlowerPreferenceActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}

}
