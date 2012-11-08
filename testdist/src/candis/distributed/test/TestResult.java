/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.distributed.test;

import candis.distributed.IDistributedResult;

/**
 *
 * @author swillenborg
 */
public class TestResult implements IDistributedResult{
	public int value;
	public TestResult(int n) {
		value = n;
	}
}
