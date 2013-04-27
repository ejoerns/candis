package candis.client;

/**
 *
 * @author Enrico Joerns
 */
public class CandisSettings {

  private static CandisSettings mInstance = new CandisSettings();
  
  public static CandisSettings getInstance() {
    return mInstance;
  }
  
  private String HostAddress;
  private int HostPort;
  private boolean NotificationEnabled;
  private boolean MultithreadingEnabled;

  /**
   * @return the HostAddress
   */
  public String getHostAddress() {
    return HostAddress;
  }

  /**
   * @param HostAddress the HostAddress to set
   */
  public void setHostAddress(String HostAddress) {
    this.HostAddress = HostAddress;
  }

  /**
   * @return the HostPort
   */
  public int getHostPort() {
    return HostPort;
  }

  /**
   * @param HostPort the HostPort to set
   */
  public void setHostPort(int HostPort) {
    this.HostPort = HostPort;
  }

  /**
   * @return the NotificationEnabled
   */
  public boolean isNotificationEnabled() {
    return NotificationEnabled;
  }

  /**
   * @param NotificationEnabled the NotificationEnabled to set
   */
  public void setNotificationEnabled(boolean NotificationEnabled) {
    this.NotificationEnabled = NotificationEnabled;
  }

  /**
   * @return the MultithreadingEnabled
   */
  public boolean isMultithreadingEnabled() {
    return MultithreadingEnabled;
  }

  /**
   * @param MultithreadingEnabled the MultithreadingEnabled to set
   */
  public void setMultithreadingEnabled(boolean MultithreadingEnabled) {
    this.MultithreadingEnabled = MultithreadingEnabled;
  }
}
