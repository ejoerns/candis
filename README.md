candis
======

Distributed computing on Android-Devices.


## Minimalistic Example

This is an example, how to implement a distributed Task with candis. 

### Data exchange

To transfer data between management server and the remote app you implement your own serializable container classes.

#### Initial Parameter
Initial parameters are used to submit task parameters which are same for all task executions. (implementation of `DistributedJobParameter`).

```java
package candis.example.mini;

import candis.distributed.DistributedJobParameter;

/**
 * Serializable initial parameter for "global" settings.
 */
public class MiniInitParameter implements DistributedJobParameter {

	/// Some example Value
	public final int offset;

	/**
	 * Initializes the Initial Parameters for MiniTask
	 *
	 * @param offset some integer value
	 */
	public MiniInitParameter(final int offset) {
		this.offset = offset;
	}
}

```

#### Job Parameter
Each job is specified by its own job parameters (implementation of `DistributedJobParameter`).

```java
package candis.example.mini;

import candis.distributed.DistributedJobParameter;

/**
 * Serializable task parameter for MiniTask.
 */
public class MiniJobParameter implements DistributedJobParameter {

	/// Some value
	public final int foo;
	/// Another value
	public final float bar;

	/**
	 * Initializes the Task Parameters for MiniTask
	 *
	 * @param foo some integer value
	 * @param bar some float value
	 */
	public MiniJobParameter(final int foo, final float bar) {
		this.foo = foo;
		this.bar = bar;
	}
}
```

#### Result Data
The result of a successful done task will be transferred back in it's result container (implementation of `DistributedJobResult`).

```java
package candis.example.mini;

import candis.distributed.DistributedJobResult;

/**
 * Serializable result for MiniTask.
 */
public class MiniJobResult implements DistributedJobResult {

	/// Some value
	public final float foobar;

	/**
	 * Initializes the result data.
	 *
	 * @param foobar
	 */
	public MiniJobResult(final float foobar) {
		this.foobar = foobar;
	}
}
```


### Distributed Code

The runnable code of a task is an implementation of `DistributedRunnable`.
 
```java
package candis.example.mini;

import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.DistributedRunnable;

/**
 * Example Task.
 * Multiplies MiniParameter.foo with MiniParamter.bar
 */
public class MiniRunnable implements DistributedRunnable {

	private MiniInitParameter initial;

	/**
	 * Gets called when the Task should be aborted.
	 */
	@Override
	public void stopJob() {
		// Nothing to do here
	}

	/**
	 * Gets called to start the Task with given parameter.
	 * Contains main code for this Task.
	 *
	 * @param parameter Parameter (MiniParameter) specifying the current Task
	 * @return The generated MiniResult, when the task is finished
	 */
	@Override
	public DistributedJobResult runJob(DistributedJobParameter parameter) {
		// Cast incomming Parameter
		MiniJobParameter p = (MiniJobParameter) parameter;
		System.out.println(String.format("HEY, I AM THE RUNNABLE, MY PARAMETERS ARE: %s and %s", p.bar, p.foo));
		return new MiniJobResult(p.foo * p.bar + initial.offset);
	}

	/**
	 * Gets called to set the initial parameter.
	 *
	 * @param parameter Transfered initial parameter
	 */
	@Override
	public void setInitialParameter(DistributedJobParameter parameter) {
		initial = (MiniInitParameter) parameter;
	}
}
```

### Task Management

The task distribution management will be done serverside. It is possible to write an own scheduler by implementing/extending `Scheduler`.
This is also the class which receives the results of the distributed tasks.
An simple Scheduler is already provided by `SimpleScheduler`.

The setup of a scheduler is done by an implementation of `DistributedControl`. To get each calculated job result directly after it's send to the server additionally implement
 `ResultReceiver`.

```java
package candis.example.mini;

import candis.distributed.DistributedControl;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.ResultReceiver;
import candis.distributed.Scheduler;
import candis.distributed.SimpleScheduler;

/**
 * Minimalistic example how to initialize a set of tasks and receive its
 * results.
 */
public class MiniControl implements DistributedControl, ResultReceiver {

	MiniInitParameter init;
	Scheduler scheduler;

	@Override
	public Scheduler initScheduler() {
		scheduler = new SimpleScheduler();

		// Register ResultReceiver
		scheduler.addResultReceiver(this);

		// Set initial Parameters
		init = new MiniInitParameter(23);
		scheduler.setInitialParameter(init);

		// Create some tasks
		for (int i = 0; i < 10; i++) {
			scheduler.addParameter(new MiniJobParameter(i, 3.5f));
		}

		return scheduler;
	}

	@Override
	public void onSchedulerDone() {
		// Now all tasks are completed successfully
		System.out.println("done!");
	}

	@Override
	public void onReceiveResult(DistributedJobParameter param, DistributedJobResult result) {
		/// One result is finished and we can use it, somehow ...
		MiniJobResult miniResult = (MiniJobResult) result;
		System.out.println(String.format("Got Result: %.3f", miniResult.foobar));

	}
}

```

### Generate Candis Distributed Bundle (cdb)

Using `mkcdb.py` (located in [`tools/`](https://github.com/ejoerns/candis/tree/master/tools)) to create the cdb-file:

```
>> mkcdb.py miniTaks.cdb examples/mini/miniControl/ examples/mini/miniTask/
```

The generated file `miniTask.cdb` is ready to be read by the server-control application.

## Short setup

	>> cd tools/

Download necessary external Libraries

	>> ./initdist.sh

Build candis into `dist/`

	>> ./mkdist.sh
	
Generate Server Certificate and remember the password
	
	>> ./mkkey.sh
	>> cd ../dist/
	
Now `testdist` and `master` are ready.

use `/ools/mkcdb.py` to generate cdb-files from NetBeans-projects

