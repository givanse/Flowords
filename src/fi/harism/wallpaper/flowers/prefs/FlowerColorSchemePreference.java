package fi.harism.wallpaper.flowers.prefs;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class FlowerColorSchemePreference extends ListPreference {

	public FlowerColorSchemePreference(Context context, AttributeSet attrs) {
		super(context, attrs);

	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		boolean disableDependentsOrig = shouldDisableDependents();
		super.onDialogClosed(positiveResult);
		boolean disableDependentsNew = shouldDisableDependents();
		if (disableDependentsOrig != disableDependentsNew) {
			notifyDependencyChange(disableDependentsNew);
		}
	}

	@Override
	public boolean shouldDisableDependents() {
		return !"0".equals(getPersistedString(null));
	}

}
