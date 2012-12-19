package candis.distributed;

/**
 *
 * @author Sebastian Willenborg
 */
public interface ResultReceiver {

	void onReceiveResult(DistributedJobParameter param, DistributedJobResult result);
}
