package top.saymzx.wired_forward;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import top.saymzx.wired_forward.helper.AppData;
import top.saymzx.wired_forward.helper.MyBroadcastReceiver;

public class UsbActivity extends Activity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (AppData.mainActivity == null) startActivity(new Intent(this, MainActivity.class));
    else {
      Intent intent = new Intent();
      intent.setAction(MyBroadcastReceiver.ACTION_UPDATE_USB);
      sendBroadcast(intent);
    }
    finish();
  }
}