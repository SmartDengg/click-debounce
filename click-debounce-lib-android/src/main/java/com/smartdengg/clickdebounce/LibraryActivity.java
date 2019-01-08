package com.smartdengg.clickdebounce;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

/**
 * 创建时间:  2018/04/11 15:42 <br>
 * 作者:  SmartDengg <br>
 * 描述:
 */
public class LibraryActivity extends Activity implements View.OnClickListener {

  private static final String TAG = LibraryActivity.class.getSimpleName();

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    new TextView(this).setOnClickListener(this);
    new TextView(this).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Log.d(TAG, "onClick : " + this.getClass().getName());
      }
    });
  }

  @Override
  public void onClick(View v) {
    Log.d(TAG, "onClick : " + this.getClass().getName());
  }
}
