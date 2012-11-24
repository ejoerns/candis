package candis.distributed;

/**
 *
 * @author Sebastian Willenborg
 */
public abstract class DistributedTask {

	abstract public DistributedResult run(DistributedParameter parameter);

	abstract public void stop();
}
