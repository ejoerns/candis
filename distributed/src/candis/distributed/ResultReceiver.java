package candis.distributed;

/**
 *
 * @author Sebastian Willenborg
 */
public interface ResultReceiver {
	void onReceiveResult(DistributedParameter param, DistributedResult result);
}
