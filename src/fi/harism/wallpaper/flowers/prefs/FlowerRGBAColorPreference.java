/*
   Copyright 2012 Harri Smått

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package fi.harism.wallpaper.flowers.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import fi.harism.wallpaper.flowers.R;

/**
 * RGBA color chooser dialog preferece.
 */
public class FlowerRGBAColorPreference extends DialogPreference implements
		SeekBar.OnSeekBarChangeListener {

	// Color value SeekBars.
	private SeekBar mSeekBarR, mSeekBarG, mSeekBarB, mSeekBarA;
	// Current color value.
	private int mValue;
	// Color preview View.
	private View mViewColor;

	/**
	 * Default constructor.
	 */
	public FlowerRGBAColorPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/**
	 * Generates current color value from color SeekBars.
	 * 
	 * @return Current color value.
	 */
	private int getCurrentColor() {
		return Color.argb(mSeekBarA.getProgress(), mSeekBarR.getProgress(),
				mSeekBarG.getProgress(), mSeekBarB.getProgress());
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);

		// Do not apply alpha to color preview.
		mViewColor.setBackgroundColor(mValue | 0xFF000000);
		// Set SeekBar values.
		mSeekBarR.setProgress(Color.red(mValue));
		mSeekBarG.setProgress(Color.green(mValue));
		mSeekBarB.setProgress(Color.blue(mValue));
		mSeekBarA.setProgress(Color.alpha(mValue));
	}

	@Override
	protected View onCreateDialogView() {
		View view = LayoutInflater.from(getContext()).inflate(
				R.layout.preference_color, null);

		// Get color preview View.
		mViewColor = view.findViewById(R.id.color_view);
		// Get and adjust color SeekBars.
		mSeekBarR = (SeekBar) view.findViewById(R.id.color_red_seekbar);
		mSeekBarR.setMax(255);
		mSeekBarR.setOnSeekBarChangeListener(this);
		mSeekBarG = (SeekBar) view.findViewById(R.id.color_green_seekbar);
		mSeekBarG.setMax(255);
		mSeekBarG.setOnSeekBarChangeListener(this);
		mSeekBarB = (SeekBar) view.findViewById(R.id.color_blue_seekbar);
		mSeekBarB.setMax(255);
		mSeekBarB.setOnSeekBarChangeListener(this);
		mSeekBarA = (SeekBar) view.findViewById(R.id.color_alpha_seekbar);
		mSeekBarA.setMax(255);
		mSeekBarA.setOnSeekBarChangeListener(this);

		return view;
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			int color = getCurrentColor();
			if (callChangeListener(color)) {
				mValue = color;
				persistInt(mValue);
			}
		}
	}

	@Override
	protected Object onGetDefaultValue(TypedArray ta, int index) {
		return Color.parseColor(ta.getString(index));
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		// Do not apply alpha to preview color View.
		mViewColor.setBackgroundColor(getCurrentColor() | 0xFF000000);
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		mValue = restoreValue ? getPersistedInt(mValue)
				: (Integer) defaultValue;
		if (!restoreValue) {
			persistInt(mValue);
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

}
