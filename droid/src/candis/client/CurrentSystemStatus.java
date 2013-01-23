package candis.client;

/**
 * Holds system info.
 * Singleton.
 *
 * @author Enrico Joerns
 */
public class CurrentSystemStatus {

  private static CurrentSystemStatus mInstance = new CurrentSystemStatus();

  private CurrentSystemStatus() {
  }

  public static CurrentSystemStatus getInstance() {
    return mInstance;
  }
//  public boolean batteryLow;
  public boolean charging;
  public double chargingState;
}
