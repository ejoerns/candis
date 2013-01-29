package candis.client;

/**
 * Holds system info.
 * Singleton.
 *
 * @author Enrico Joerns
 */
public class CurrentSystemStatus {

  public static final String CURRENT_SYSTEM_STATUS = "CurrentSystemStatus";
  
  public static final String SERVER_NAME = "server_name";
  public static final String SERVER_PORT = "server_port";
  public static final String POWER_CHARGING = "power_charging";
  public static final String POWER_LEVEL = "power_level";
  
  private static boolean charging;
  private static double chargingState;
  private static String servername = "not connected";
  private static int serverport = 0;

  //-- Connection
  public static String getServerName() {
    return servername;
  }

  public static int getServerPort() {
    return serverport;
  }

  //-- Power management
  public static boolean isCharging() {
    return charging;
  }

  public static double getChargingState() {
    return chargingState;
  }
}
