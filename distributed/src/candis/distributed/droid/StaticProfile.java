package candis.distributed.droid;

import java.io.Serializable;

/**
 * System Profile to provide scheduler information about device.
 *
 * @author Enrico Joerns
 */
public class StaticProfile implements Serializable {

	public final String id;
	public final String model;
	public final long memoryMB;
	public final int processors;
	public final long benchmark;

	/**
	 * Creates a new system profile from provided Data.
	 *
	 * @param id ID of the device (e.g.: IMEI)
	 * @param model Device model
	 * @param mem Total device memory
	 * @param proc Total number of processors
	 * @param bench Benchmark result
	 */
	public StaticProfile(
					final String id,
					final String model,
					final long mem,
					final int proc,
					final long bench) {
		this.id = id;
		this.model = model;
		this.memoryMB = mem;
		this.processors = proc;
		this.benchmark = bench;
	}

	public StaticProfile() {
		this("not_set", "not_set", 0, 0, 0);
	}
}
