package top.saymzx.wired_forward.adb;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface AdbChannel {
  void write(ByteBuffer data) throws IOException, InterruptedException;

  void flush() throws IOException;

  ByteBuffer read(int size) throws IOException, InterruptedException;

  void close();

}
