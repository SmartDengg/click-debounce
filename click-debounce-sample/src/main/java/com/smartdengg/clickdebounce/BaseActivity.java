package com.smartdengg.clickdebounce;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.AdapterView;

/**
 * 创建时间:  2019/01/04 15:23 <br>
 * 作者:  SmartDengg <br>
 * 描述:
 */
public abstract class BaseActivity extends Activity implements View.OnClickListener {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public void onClick(View v) {
    /*no-op*/
  }

  public abstract void onItemClick(AdapterView<?> parent, View view, int position, long id);
}
