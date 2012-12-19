package candis.client;

import java.util.UUID;

/**
 *
 * @author Enrico Joerns
 */
public interface JobCenterHandler {
	
	public void onBinaryReceived(String runnableID);
	
	public void onInitialParameterReceived(String runnableID);
	
	public void onJobExecutionStart(String runnableID);
	
	public void onJobExecutionDone(String runnableID);

}
