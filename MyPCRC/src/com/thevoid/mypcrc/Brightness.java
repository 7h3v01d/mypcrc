package com.thevoid.mypcrc;

import com.thevoid.mypcrc.comm.Connection;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class Brightness extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.brightness);

		Connection conn = Connection.getInstance(getApplication());
		float brightness = conn.getBrightness();
		setScreenBrightness(brightness);

		SeekBar adjust = (SeekBar) findViewById(R.id.brightnessAdjust);
		adjust.setProgress(brightnessToProgress(conn.getBrightness(),
				adjust.getMax()));
		adjust.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {

				SeekBar adjust = (SeekBar) findViewById(R.id.brightnessAdjust);
				float brightness = progressToBrightness(adjust.getProgress(),
						adjust.getMax());
				setScreenBrightness(brightness);

				Connection conn = Connection.getInstance(getApplication());
				conn.setBrightness(brightness);
			}
		});
	}

	private int brightnessToProgress(float brightness, int max) {
		return (int) (brightness * (float) max);
	}

	private float progressToBrightness(int progress, int max) {

		float brightness = (float) progress / (float) max;
		float min = 1.0f / (float) max;
		if (brightness < min) {
			brightness = min;
		}

		return brightness;
	}

	private void setScreenBrightness(float brightness) {

		WindowManager.LayoutParams params = getWindow().getAttributes();
		params.screenBrightness = brightness;
		getWindow().setAttributes(params);
	}
}
