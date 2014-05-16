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

	OnClickListener buttonClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {

			Connection conn = Connection.getInstance(getApplication());

			switch (v.getId()) {
			case R.id.leave_fullscreen:
				conn.send("FUNC leave-fullscreen\r\n".getBytes());
				break;
			case R.id.quit:
				conn.send("QUIT\r\n".getBytes());
				break;
			case R.id.close:
				conn.disconnect();
				finish();
				break;
			case R.id.play_pause:
				conn.send("FUNC play-pause\r\n".getBytes());
				break;
			case R.id.jumpMediumBackwards:
				conn.send("FUNC jump-medium\r\n".getBytes());
				break;
			case R.id.stop:
				conn.send("FUNC stop\r\n".getBytes());
				break;
			case R.id.jumpMediumForward:
				conn.send("FUNC jump+medium\r\n".getBytes());
				break;
			case R.id.jumpExtraShortBackwards:
				conn.send("FUNC jump-extrashort\r\n".getBytes());
				break;
			case R.id.jumpExtraShortForward:
				conn.send("FUNC jump+extrashort\r\n".getBytes());
				break;
			case R.id.volDown:
				conn.send("FUNC vol-down\r\n".getBytes());
				break;
			case R.id.mute:
				conn.send("FUNC vol-mute\r\n".getBytes());
				break;
			case R.id.volUp:
				conn.send("FUNC vol-up\r\n".getBytes());
				break;
			case R.id.toggelFullscreen:
				conn.send("FUNC toggle-fullscreen\r\n".getBytes());
				break;
			}
		}
	};
}
