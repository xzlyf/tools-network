package com.xz.preview;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.xz.tool.network.OKHttpClickManager;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

	private static final String TAG = MainActivity.class.getName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		findViewById(R.id.amazing).setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		OKHttpClickManager instance = OKHttpClickManager.getInstance();
	}
}
