package candis.example.hash;

import candis.distributed.DistributedJobParameter;

/**
 * Serializable task parameter for MiniTask.
 */
public class TestHashJobParameter implements DistributedJobParameter {

	public final String string;
          
	public TestHashJobParameter(String s) {
    string = s;
	}
}