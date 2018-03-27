package com.smartdengg.clickdebounce;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

public class MainActivity extends Activity
    implements View.OnClickListener, AdapterView.OnItemClickListener {

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    findViewById(R.id.button).setOnClickListener(this);
  }

  @Override public void onClick(View v) {
    startActivity(new Intent(MainActivity.this, SecondActivity.class));
  }

  @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    System.out.println("MainActivity.onItemClick");
  }
}
