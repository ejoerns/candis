package candis.client;

/**
 * Holds system info.
 * Singleton.
 *
 * @author Enrico Joerns
 */
public class CurrentSystemStatus {

  public boolean charging;
  public double chargingState;
  public String servername = "not connected";
  public String serverport = "-----";
}
