package candis.distributed;

/**
 *
 * @author Sebastian Willenborg
 */
public abstract class DistributedTask {

	abstract public void setInitialParameter(DistributedParameter parameter);

	abstract public DistributedResult run(DistributedParameter parameter);

	abstract public void stop();
}
