package com.thevoid.mypcrc;

import com.thevoid.mypcrc.comm.Connection;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class MyPCRC extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.mypcrc);

		int buttonIds[] = new int[] { R.id.quit };

		for (int i = 0; i < buttonIds.length; i++) {
			View button = findViewById(buttonIds[i]);
			if (button != null) {
				button.setOnClickListener(buttonClickListener);
			}
		}
	}

	OnClickListener buttonClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {

			Connection conn = Connection.getInstance();

			switch (v.getId()) {
			case R.id.quit:
				conn.send("QUIT\r\n".getBytes());
				break;
			}
		}
	};
}
