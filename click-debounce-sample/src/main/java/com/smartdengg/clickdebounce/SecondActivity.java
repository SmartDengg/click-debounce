package com.smartdengg.clickdebounce;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;

/**
 * 创建时间:  2018/03/23 15:43 <br>
 * 作者:  SmartDengg <br>
 * 描述:
 */
public class SecondActivity extends Activity {

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_second);
    Toast.makeText(this, "hello", Toast.LENGTH_SHORT).show();
  }
}
