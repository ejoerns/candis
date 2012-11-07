/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.distributed;

/**
 *
 * @author swillenborg
 */
public interface IScheduler {
	public void setCommunicationIO(CommunicationIO io);
	public void start();
	public void abort();
}
