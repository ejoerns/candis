package candis.distributed.droid;

/**
 * System Profile to provide scheduler information about device.
 *
 * @author Enrico Joerns
 */
public class StaticProfile {

	public final long memoryMB;
	public final int processors;
	public final int benchmark;

	/**
	 * Creates a new system profile from provided Data.
	 *
	 * @param mem Total device memory
	 * @param proc Total number of processors
	 * @param bench Benchmark result
	 */
	public StaticProfile(final int mem, final int proc, final int bench) {
		this.memoryMB = mem;
		this.processors = proc;
		this.benchmark = bench;
	}
	
}
