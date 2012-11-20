package candis.distributed.droid;

import java.io.Serializable;

/**
 * System Profile to provide scheduler information about device.
 *
 * @author Enrico Joerns
 */
public class StaticProfile implements Serializable {

	public final long memoryMB;
	public final int processors;
	public final long benchmark;

	/**
	 * Creates a new system profile from provided Data.
	 *
	 * @param mem Total device memory in megabyte
	 * @param proc Total number of processors
	 * @param bench Benchmark result
	 */
	public StaticProfile(final long mem, final int proc, final long bench) {
		this.memoryMB = mem;
		this.processors = proc;
		this.benchmark = bench;
	}

	public StaticProfile() {
		this(0, 0, 0);
	}
}
