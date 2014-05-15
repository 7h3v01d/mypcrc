package com.thevoid.mypcrc;

import com.thevoid.mypcrc.comm.Connection;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.TextView;

public class MyPCRC extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.mypcrc);

		int buttonIds[] = new int[] { R.id.leave_fullscreen, R.id.quit,
				R.id.close, R.id.play_pause, R.id.jumpMediumBackwards,
				R.id.stop, R.id.jumpMediumForward,
				R.id.jumpExtraShortBackwards, R.id.jumpExtraShortForward,
				R.id.volDown, R.id.mute, R.id.volUp, R.id.toggelFullscreen };

		for (int i = 0; i < buttonIds.length; i++) {
			View button = findViewById(buttonIds[i]);
			if (button != null) {
				button.setOnClickListener(buttonClickListener);
				button.setOnTouchListener(buttonTouchListener);
			}
		}
	}

	OnClickListener buttonClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {

			Connection conn = Connection.getInstance(getApplication());

			switch (v.getId()) {
			case R.id.close:
				conn.disconnect();
				finish();
				break;
			case R.id.quit:
				conn.send("QUIT\r\n".getBytes());
				break;
			}
		}
	};

	OnTouchListener buttonTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {

			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				v.setBackgroundColor(getResources().getColor(
						R.color.selectedButtonBackgroundColor));
				((TextView) v).setTextColor(getResources().getColor(
						R.color.selectedButtonTextColor));
				break;
			case MotionEvent.ACTION_UP:
				v.setBackgroundColor(getResources().getColor(
						R.color.defaultButtonBackgroundColor));
				((TextView) v).setTextColor(getResources().getColor(
						R.color.defaultButtonTextColor));
				break;
			}

			return false;
		}
	};
}
