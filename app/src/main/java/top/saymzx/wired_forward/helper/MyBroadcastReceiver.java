package top.saymzx.wired_forward.helper;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import top.saymzx.wired_forward.MainActivity;
import top.saymzx.wired_forward.adb.UsbChannel;

import java.util.Map;

public class MyBroadcastReceiver extends BroadcastReceiver {

  public static final String ACTION_UPDATE_USB = "top.saymzx.easycontrol.app.UPDATE_USB";
  private static final String ACTION_USB_PERMISSION = "top.saymzx.easycontrol.app.USB_PERMISSION";

  // 注册广播
  @SuppressLint("UnspecifiedRegisterReceiverFlag")
  public void register(Context context) {
    IntentFilter filter = new IntentFilter();
    filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
    filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
    filter.addAction(ACTION_USB_PERMISSION);
    filter.addAction(ACTION_UPDATE_USB);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) context.registerReceiver(this, filter, Context.RECEIVER_EXPORTED);
    else context.registerReceiver(this, filter);
  }

  public void unRegister(Context context) {
    context.unregisterReceiver(this);
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) AppData.uiHandler.postDelayed(() -> onConnectUsb(context, intent), 1000);
    else if (ACTION_UPDATE_USB.equals(action) || ACTION_USB_PERMISSION.equals(action) || UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) updateUSB();
  }

  // 请求USB设备权限
  @SuppressLint({"MutableImplicitPendingIntent", "UnspecifiedImmutableFlag"})
  private void onConnectUsb(Context context, Intent intent) {
    UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
    if (usbDevice == null || AppData.usbManager == null) return;
    if (!AppData.usbManager.hasPermission(usbDevice)) {
      Intent usbPermissionIntent = new Intent(ACTION_USB_PERMISSION);
      usbPermissionIntent.setPackage(AppData.applicationContext.getPackageName());
      PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 1, usbPermissionIntent, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0);
      AppData.usbManager.requestPermission(usbDevice, permissionIntent);
    }
  }

  public synchronized void updateUSB() {
    if (AppData.usbManager == null) return;
    for (Map.Entry<String, UsbDevice> entry : AppData.usbManager.getDeviceList().entrySet()) {
      UsbDevice usbDevice = entry.getValue();
      if (usbDevice == null) return;
      if (AppData.usbManager.hasPermission(usbDevice)) MainActivity.usbDevice = usbDevice;
    }
  }

  public synchronized void resetUSB() {
    if (AppData.usbManager == null) return;
    try {
      for (Map.Entry<String, UsbDevice> entry : AppData.usbManager.getDeviceList().entrySet()) {
        UsbDevice usbDevice = entry.getValue();
        if (usbDevice == null) return;
        if (AppData.usbManager.hasPermission(usbDevice)) new UsbChannel(usbDevice).close();
      }
    } catch (Exception ignored) {
    }
  }

}
