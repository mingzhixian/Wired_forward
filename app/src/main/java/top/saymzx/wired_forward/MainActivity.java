package top.saymzx.wired_forward;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.usb.UsbDevice;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.annotation.Nullable;
import top.saymzx.wired_forward.adb.Adb;
import top.saymzx.wired_forward.buffer.BufferStream;
import top.saymzx.wired_forward.databinding.ActivityMainBinding;
import top.saymzx.wired_forward.databinding.ItemFloatBinding;
import top.saymzx.wired_forward.helper.AppData;
import top.saymzx.wired_forward.helper.MyBroadcastReceiver;
import top.saymzx.wired_forward.helper.ViewTools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity {
  private ActivityMainBinding activityMainBinding;
  public static UsbDevice usbDevice;
  private Adb adb;
  private BufferStream bufferStream;
  private Socket socket;
  private InputStream inputStream;
  private OutputStream outputStream;
  private Thread inThread;
  private Thread outThread;

  // 迷你悬浮窗
  private ItemFloatBinding itemFloatBinding;
  private final WindowManager.LayoutParams floatViewParams = new WindowManager.LayoutParams(
    WindowManager.LayoutParams.WRAP_CONTENT,
    WindowManager.LayoutParams.WRAP_CONTENT,
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
    PixelFormat.TRANSLUCENT
  );

  // 广播
  private final MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver();

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    AppData.init(this);
    ViewTools.setStatusAndNavBar(this);
    ViewTools.setFullScreen(this);
    activityMainBinding = ActivityMainBinding.inflate(this.getLayoutInflater());
    setContentView(activityMainBinding.getRoot());
    // 注册广播监听
    myBroadcastReceiver.register(this);
    // 重置已连接设备
    myBroadcastReceiver.resetUSB();
    // 悬浮球
    itemFloatBinding= ItemFloatBinding.inflate(LayoutInflater.from(AppData.applicationContext));
    setFloatListener();
    floatViewParams.gravity = Gravity.START | Gravity.TOP;
    // 绘制UI
    activityMainBinding.text.setText(String.valueOf(AppData.setting.getForwardPort()));
    setButtonStart();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (!checkPermission()) {
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setData(Uri.parse("package:$packageName"));
        startActivity(intent);
      }
    }
  }

  // 检查权限
  private boolean checkPermission() {
    // 检查悬浮窗权限，防止某些设备如鸿蒙不兼容
    try {
      return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    } catch (Exception ignored) {
      return true;
    }
  }

  @Override
  protected void onDestroy() {
    myBroadcastReceiver.unRegister(this);
    super.onDestroy();
  }

  private void startWiredForward(int port) {
    new Thread(() -> {
      try {
        adb = new Adb(usbDevice, AppData.keyPair);
      } catch (Exception e) {
        close("无法连接ADB");
        return;
      }
      // 启动监听
      try (ServerSocket serverSocket = new ServerSocket(port)) {
        socket = serverSocket.accept();
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
      } catch (Exception ignored) {
        close("端口监听失败，请检查占用情况");
        return;
      }
      // 连接ADB转发
      boolean connectAdb = false;
      for (int i = 0; i < 30; i++) {
        try {
          bufferStream = adb.tcpForward(port);
          connectAdb = true;
          break;
        } catch (Exception ignored) {
          try {
            Thread.sleep(50);
          } catch (Exception ignored1) {
          }
        }
      }
      if (!connectAdb) {
        close("端口监听失败，请检查占用情况");
        return;
      }
      // 启动读取与写入线程
      inThread = new Thread(this::inThread);
      outThread = new Thread(this::outThread);
      inThread.setPriority(Thread.MAX_PRIORITY);
      outThread.setPriority(Thread.MAX_PRIORITY);
      inThread.start();
      outThread.start();
    }).start();
  }

  // 设置监听控制
  @SuppressLint("ClickableViewAccessibility")
  private void setFloatListener() {
    AtomicInteger yy = new AtomicInteger();
    AtomicInteger oldYy = new AtomicInteger();
    itemFloatBinding.getRoot().setOnTouchListener((v, event) -> {
      switch (event.getActionMasked()) {
        case MotionEvent.ACTION_OUTSIDE:
          break;
        case MotionEvent.ACTION_DOWN: {
          yy.set((int) event.getRawY());
          oldYy.set(floatViewParams.y);
          break;
        }
        case MotionEvent.ACTION_MOVE: {
          floatViewParams.y = oldYy.get() + (int) event.getRawY() - yy.get();
          AppData.windowManager.updateViewLayout(itemFloatBinding.getRoot(), floatViewParams);
          break;
        }
      }
      return true;
    });
  }

  private void inThread() {
    byte[] bytes = new byte[1024];
    while (!Thread.interrupted()) {
      try {
        int len = inputStream.read(bytes);
        byte[] bytes1 = new byte[len];
        System.arraycopy(bytes, 0, bytes1, 0, len);
        bufferStream.write(ByteBuffer.wrap(bytes1));
      } catch (Exception ignored) {
        close("连接断开");
        break;
      }
    }
  }

  private void outThread() {
    byte[] bytes = new byte[1024];
    while (!Thread.interrupted()) {
      try {
        ByteBuffer byteBuffer = bufferStream.readAllBytes();
        outputStream.write(byteBuffer.array());
      } catch (Exception ignored) {
        close("连接断开");
        break;
      }
    }
  }

  private void close(String err) {
    if (err != null) AppData.uiHandler.post(() -> Toast.makeText(this, err, Toast.LENGTH_SHORT).show());
  }

  private void setButtonStart() {
    activityMainBinding.buttonForward.setText("启动转发");
    activityMainBinding.buttonForward.setOnClickListener(v -> {
      int port = Integer.parseInt(String.valueOf(activityMainBinding.text.getText()));
      if (port <= 0 || port >= 65535) {
        Toast.makeText(this, "请输入正确的端口", Toast.LENGTH_SHORT).show();
        return;
      }
      setButtonStop();
      AppData.setting.setForwardPort(port);
      floatViewParams.x = 0;
      floatViewParams.y=200;
      AppData.windowManager.addView(itemFloatBinding.getRoot(), floatViewParams);
      startWiredForward(port);
    });
  }

  private void setButtonStop() {
    activityMainBinding.buttonForward.setText("停止转发");
    activityMainBinding.buttonForward.setOnClickListener(v -> {
      AppData.windowManager.removeView(itemFloatBinding.getRoot());
      setButtonStart();
      try {
        inputStream.close();
        outputStream.close();
        socket.close();
      } catch (IOException ignored) {
      }
      if (inThread != null) inThread.interrupt();
      if (outThread != null) outThread.interrupt();
      adb.close();
    });
  }
}
