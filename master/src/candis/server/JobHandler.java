package candis.server;

import candis.distributed.DistributedJobResult;

/**
 * Invoked by FSM.
 *
 * @author Enrico Joerns
 */
public interface JobHandler {

  public void onJobDone(String droidID, String jobID, DistributedJobResult[] results, long exectime);

  public void onJobReceived(String droidID, String jobID);

  public void onDroidConnected(final String droidID);
}
