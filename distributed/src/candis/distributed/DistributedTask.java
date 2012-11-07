package candis.distributed;

/**
 *
 * @author Sebastian Willenborg
 */
public interface DistributedTask {
	DistributedResult run(DistributedParameter parameter);
	void stop();
}
