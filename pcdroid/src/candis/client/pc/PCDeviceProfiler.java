package candis.client.pc;

import candis.client.DeviceProfiler;

/**
 *
 * @author Enrico Joerns
 */
public class PCDeviceProfiler extends DeviceProfiler {

  @Override
  public long getMemorySize() {
    return Runtime.getRuntime().totalMemory();
  }

  @Override
  public int getNumCores() {
    return Runtime.getRuntime().availableProcessors();
  }

  @Override
  public String getDeviecID() {
    return "0000";
  }

  @Override
  public String getModel() {
    return "PC";
  }
}
