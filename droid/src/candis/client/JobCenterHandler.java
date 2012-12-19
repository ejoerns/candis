package candis.client;

import java.util.UUID;

/**
 *
 * @author Enrico Joerns
 */
public interface JobCenterHandler {
	
	public void onBinaryReceived(UUID uuid);
	
	public void onInitialParameterReceived(UUID uuid);
	
	public void onJobExecutionStart(UUID uuid);
	
	public void onJobExecutionDone(UUID uuid);

}
