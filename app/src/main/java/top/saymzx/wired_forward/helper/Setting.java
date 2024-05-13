package top.saymzx.wired_forward.helper;

import android.content.SharedPreferences;

import java.util.UUID;

public final class Setting {
  private final SharedPreferences sharedPreferences;

  private final SharedPreferences.Editor editor;

  public int getForwardPort() {
    return sharedPreferences.getInt("forwardPort", 0);
  }

  public void setForwardPort(int value) {
    editor.putInt("forwardPort", value);
    editor.apply();
  }

  public Setting(SharedPreferences sharedPreferences) {
    this.sharedPreferences = sharedPreferences;
    this.editor = sharedPreferences.edit();
  }
}
