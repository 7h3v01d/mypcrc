package com.thevoid.mypcrc;

import java.io.DataOutputStream;
import java.io.IOException;

import com.thevoid.mypcrc.comm.Connection;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

public class MyPCRC extends Activity {

	private WakeLock lock;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.mypcrc);

		Connection conn = Connection.getInstance(getApplication());
		conn.authenticate();

		int buttonIds[] = new int[] { R.id.leave_fullscreen, R.id.quit,
				R.id.close, R.id.play_pause, R.id.jumpMediumBackwards,
				R.id.stop, R.id.jumpMediumForward,
				R.id.jumpExtraShortBackwards, R.id.jumpExtraShortForward,
				R.id.volDown, R.id.mute, R.id.volUp, R.id.brightDown,
				R.id.toggelFullscreen, R.id.brightUp };

		for (int i = 0; i < buttonIds.length; i++) {
			View button = findViewById(buttonIds[i]);
			if (button != null) {
				button.setOnClickListener(buttonClickListener);
				button.setOnTouchListener(buttonTouchListener);
			}
		}

		PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		lock = manager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
				| PowerManager.ON_AFTER_RELEASE, "MyPCRC");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		Connection conn = Connection.getInstance(getApplication());
		conn.disconnect();
	};

	@Override
	protected void onResume() {
		super.onResume();

		lock.acquire();

		WindowManager.LayoutParams params = getWindow().getAttributes();
		Connection conn = Connection.getInstance(getApplication());
		params.screenBrightness = conn.getBrightness();
		getWindow().setAttributes(params);

		executeCommand("echo 0 > /sys/class/leds/button-backlight/brightness");
	};

	@Override
	protected void onPause() {
		super.onPause();

		Connection conn = Connection.getInstance(getApplication());
		if (conn.isConnected()) {
			conn.send("FUNC pause\r\n");
		}

		executeCommand("echo 255 > /sys/class/leds/button-backlight/brightness");

		if (lock != null) {
			lock.release();
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.brightnessMenuOption:
			Intent brightness = new Intent(this, Brightness.class);
			startActivity(brightness);
			break;
		case R.id.hostMenuOption:
			showHostConfiguration();
			break;
		}

		return true;
	}

	private void showHostConfiguration() {

		LayoutInflater inflater = getLayoutInflater();
		final View hostLayout = inflater.inflate(R.layout.host, null);

		EditText edit = (EditText) hostLayout.findViewById(R.id.host);
		Connection conn = Connection.getInstance(getApplication());
		String host = conn.getHost();
		edit.setText(host);

		Builder builder = new Builder(this);
		builder.setTitle(R.string.hostConfigurationTitleText);
		builder.setView(hostLayout);
		builder.setPositiveButton(R.string.positiveButtonText,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						EditText edit = (EditText) hostLayout
								.findViewById(R.id.host);
						Editable text = edit.getEditableText();
						Connection conn = Connection
								.getInstance(getApplication());
						conn.setHost(text.toString());
						conn.disconnect();
					}
				});
		builder.setNegativeButton(R.string.negativeButtonText, null);
		builder.show();
	}

	private void executeCommand(String cmd) {

		Runtime runtime = Runtime.getRuntime();
		Process process = null;
		DataOutputStream output = null;

		try {
			process = runtime.exec("su");
			output = new DataOutputStream(process.getOutputStream());
			output.writeBytes(cmd + "\n");
			output.writeBytes("exit\n");
			output.flush();
			process.waitFor();
			runtime.exec(cmd);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (process != null) {
				process.destroy();
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
				conn.send("FUNC leave-fullscreen\r\n");
				break;
			case R.id.quit:
				conn.send("FUNC quit\r\n");
				break;
			case R.id.close:
				conn.disconnect();
				finish();
				break;
			case R.id.play_pause:
				conn.send("FUNC play-pause\r\n");
				break;
			case R.id.jumpMediumBackwards:
				conn.send("FUNC jump-medium\r\n");
				break;
			case R.id.stop:
				conn.send("FUNC stop\r\n");
				break;
			case R.id.jumpMediumForward:
				conn.send("FUNC jump+medium\r\n");
				break;
			case R.id.jumpExtraShortBackwards:
				conn.send("FUNC jump-extrashort\r\n");
				break;
			case R.id.jumpExtraShortForward:
				conn.send("FUNC jump+extrashort\r\n");
				break;
			case R.id.volDown:
				conn.send("FUNC vol-down\r\n");
				break;
			case R.id.mute:
				conn.send("FUNC vol-mute\r\n");
				break;
			case R.id.volUp:
				conn.send("FUNC vol-up\r\n");
				break;
			case R.id.brightDown:
				conn.send("FUNC bright-down\r\n");
				break;
			case R.id.toggelFullscreen:
				conn.send("FUNC toggle-fullscreen\r\n");
				break;
			case R.id.brightUp:
				conn.send("FUNC bright-up\r\n");
				break;
			}
		}
	};
}
