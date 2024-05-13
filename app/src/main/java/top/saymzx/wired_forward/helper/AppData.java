package top.saymzx.wired_forward.helper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.util.Base64;
import android.util.Pair;
import android.view.WindowManager;
import top.saymzx.wired_forward.MainActivity;
import top.saymzx.wired_forward.adb.AdbBase64;
import top.saymzx.wired_forward.adb.AdbKeyPair;

import java.io.File;

public class AppData {
  @SuppressLint("StaticFieldLeak")
  public static Context applicationContext;
  public static MainActivity mainActivity;
  public static Handler uiHandler;

  // 密钥文件
  public static AdbKeyPair keyPair;

  // 系统服务
  public static UsbManager usbManager;
  public static WindowManager windowManager;

  // 设置值
  public static Setting setting;

  public static void init(MainActivity m) {
    mainActivity = m;
    applicationContext = m.getApplicationContext();
    uiHandler = new Handler(m.getMainLooper());
    usbManager = (UsbManager) applicationContext.getSystemService(Context.USB_SERVICE);
    windowManager = (WindowManager) applicationContext.getSystemService(Context.WINDOW_SERVICE);
    setting = new Setting(applicationContext.getSharedPreferences("setting", Context.MODE_PRIVATE));
    // 读取密钥
    keyPair = readAdbKeyPair();
  }

  // 获取密钥文件
  public static Pair<File, File> getAdbKeyFile(Context context) {
    return new Pair<>(new File(context.getApplicationContext().getFilesDir(), "public.key"), new File(context.getApplicationContext().getFilesDir(), "private.key"));
  }

  // 读取密钥
  public static AdbKeyPair readAdbKeyPair() {
    try {
      AdbKeyPair.setAdbBase64(new AdbBase64() {
        @Override
        public String encodeToString(byte[] data) {
          return Base64.encodeToString(data, Base64.DEFAULT);
        }

        @Override
        public byte[] decode(byte[] data) {
          return Base64.decode(data, Base64.DEFAULT);
        }
      });
      Pair<File, File> adbKeyFile = getAdbKeyFile(AppData.applicationContext);
      if (!adbKeyFile.first.isFile() || !adbKeyFile.second.isFile()) AdbKeyPair.generate(adbKeyFile.first, adbKeyFile.second);
      return AdbKeyPair.read(adbKeyFile.first, adbKeyFile.second);
    } catch (Exception ignored) {
      return reGenerateAdbKeyPair();
    }
  }

  // 生成密钥
  public static AdbKeyPair reGenerateAdbKeyPair() {
    try {
      Pair<File, File> adbKeyFile = getAdbKeyFile(AppData.applicationContext);
      AdbKeyPair.generate(adbKeyFile.first, adbKeyFile.second);
      return AdbKeyPair.read(adbKeyFile.first, adbKeyFile.second);
    } catch (Exception ignored) {
      return null;
    }
  }

}
