package fi.harism.wallpaper.flowers.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import fi.harism.wallpaper.flowers.R;

public final class FlowerSliderPreference extends DialogPreference {

	private SeekBar mSeekBar;
	private int mValue;

	public FlowerSliderPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		mSeekBar.setMax(100);
		mSeekBar.setProgress(mValue);
	}

	@Override
	protected View onCreateDialogView() {
		mSeekBar = (SeekBar) LayoutInflater.from(getContext()).inflate(
				R.layout.preference_slider, null);
		return mSeekBar;
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			int value = mSeekBar.getProgress();
			if (callChangeListener(value)) {
				mValue = value;
				persistInt(mValue);
			}
		}
	}

	@Override
	protected Object onGetDefaultValue(TypedArray ta, int index) {
		return ta.getInt(index, 0);
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		mValue = restoreValue ? getPersistedInt(mValue)
				: (Integer) defaultValue;
		if (!restoreValue) {
			persistInt(mValue);
		}
	}

}
