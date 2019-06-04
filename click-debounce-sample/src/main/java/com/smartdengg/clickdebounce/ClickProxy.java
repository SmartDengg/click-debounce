package com.smartdengg.clickdebounce;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

/**
 * 创建时间:  2019/06/03 11:58 <br>
 * 作者:  SmartDengg <br>
 * 描述:
 */
public class ClickProxy implements View.OnClickListener, AdapterView.OnItemClickListener {

  private static final String TAG = ClickProxy.class.getSimpleName();
  private static volatile ClickProxy instance;

  static ClickProxy getInstance() {
    synchronized (ClickProxy.class) {
      if (instance == null) {
        instance = new ClickProxy();
      }
      return instance;
    }
  }

  @Override public void onClick(View v) {
    Log.d(TAG, "onClick : " + this.getClass().getName());
  }

  @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    Log.d(TAG, "onClick : " + this.getClass().getName());
  }
}
