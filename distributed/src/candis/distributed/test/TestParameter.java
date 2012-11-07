/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.distributed.test;

import candis.distributed.IDistributedParameter;

/**
 *
 * @author swillenborg
 */
public class TestParameter implements IDistributedParameter{
	public int number;
	public TestParameter(int n) {
		number = n;
	}
}
