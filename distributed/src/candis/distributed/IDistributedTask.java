/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.distributed;

/**
 *
 * @author swillenborg
 */
public interface IDistributedTask {
	IDistributedResult run(IDistributedParameter parameter);
	void stop();
}
