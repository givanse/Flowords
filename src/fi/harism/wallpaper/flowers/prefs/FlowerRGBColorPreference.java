package fi.harism.wallpaper.flowers.prefs;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import fi.harism.wallpaper.flowers.R;

public class FlowerRGBColorPreference extends FlowerRGBAColorPreference {

	public FlowerRGBColorPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public View onCreateDialogView() {
		View view = super.onCreateDialogView();
		view.findViewById(R.id.color_alpha_label).setVisibility(View.GONE);
		view.findViewById(R.id.color_alpha_seekbar).setVisibility(View.GONE);
		return view;
	}

}
