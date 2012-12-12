candis
======

Distributed computing on Android-Devices.


## Minimalistic Example

This is an example, how to implement a distributed Task with candis. 

### Data exchange

To transfer data between management server and the remote App you implement your own serializable container classes.

#### Initial Parameter
Initial Parameters are used to set Task parameters which are same for all tasks (Implementation of `DistributedParameter`).

```java
package candis.example.mini;

import candis.distributed.DistributedParameter;
import java.io.Serializable;

/**
 * Serializable initial parameter for "global" settings.
 */
public class MiniInitParameter extends DistributedParameter implements Serializable {

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

#### Task Parameter
Each task is specified by its own Parameters (implementation of `DistributedParameter`).

```java
package candis.example.mini;

import candis.distributed.DistributedParameter;
import java.io.Serializable;

/**
 * Serializable task parameter for MiniTask.
 */
public class MiniParameter extends DistributedParameter implements Serializable {

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
	public MiniParameter(final int foo, final float bar) {
		this.foo = foo;
		this.bar = bar;
	}
}
```

#### Result Data
The result of a successful done task will be transferred back in it's result container (implementation of `DistributedResult`).

```java
package candis.example.mini;

import candis.distributed.DistributedResult;
import java.io.Serializable;

/**
 * Serializable result for MiniTask.
 */
public class MiniResult extends DistributedResult implements Serializable {

	/// Some value
	public final float foobar;

	/**
	 * Initializes the result data.
	 *
	 * @param foobar
	 */
	public MiniResult(final float foobar) {
		this.foobar = foobar;
	}
}
```


### Distributed Code

The task is an implementation of `DistributedTask`.
 
```java
package candis.example.mini;

import candis.distributed.DistributedParameter;
import candis.distributed.DistributedResult;
import candis.distributed.DistributedTask;

/**
 * Example Task.
 * Multiplies MiniParameter.foo with MiniParamter.bar
 */
public class MiniTask extends DistributedTask {

	private MiniInitParameter initial;

	/**
	 * Gets called when the Task should be aborted.
	 */
	@Override
	public void stop() {
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
	public DistributedResult run(DistributedParameter parameter) {
		// Cast incomming Parameter
		MiniParameter p = (MiniParameter) parameter;
		return new MiniResult(p.foo * p.bar + initial.offset);
	}

	/**
	 * Gets called to set the initial parameter.
	 *
	 * @param parameter Transfered initial parameter
	 */
	@Override
	public void setInitialParameter(DistributedParameter parameter) {
		initial = (MiniInitParameter) parameter;
	}
}
```

### Task Management

Simple serverside Task management using `SimpleScheduler`. It is possible to write own scheduler by implementing/extending `Scheduler`.
This is also the class which receives the results of the distributed tasks.

```java
package candis.example.mini;

import candis.distributed.DistributedControl;
import candis.distributed.DistributedParameter;
import candis.distributed.DistributedResult;
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
			scheduler.addParameter(new MiniParameter(i, 3.5f));
		}

		return scheduler;
	}

	@Override
	public void onSchedulerDone() {
		// Now all tasks are completed successfully
		System.out.println("done!");
	}

	@Override
	public void onReceiveResult(DistributedParameter param, DistributedResult result) {
		/// One result is finished and we can use it, somehow ...
		MiniResult miniResult = (MiniResult) result;
		System.out.println(String.format("%.3f", miniResult.foobar));

	}
}
```

### Generate Candis Distributed Bundle (cdb)

Using `mkcdb.py` (located in [`tools/`](https://github.com/ejoerns/candis/tree/master/tools)) to create the cdb-file:

```
>> mkcdb.py miniTaks.cdb examples/mini/miniControl/ examples/mini/miniTask/
```

The generated file `miniTask.cdb` is ready to be read by the server-control application.


