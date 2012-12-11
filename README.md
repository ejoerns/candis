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
 *
 * @author Sebastian Willenborg
 */
public class MiniInitParameter extends DistributedParameter implements Serializable {
	public final int offset;

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
	 * Initalizes the Resultdata.
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
	 * Gets called, when the Task should be aborted.
	 */
	@Override
	public void stop() {
		// Nothing to do here
	}

	/**
	 * Main code for this Task.
	 *
	 * @param parameter
	 * @return The generated MiniResult, when the task is finished
	 */
	@Override
	public DistributedResult run(DistributedParameter parameter) {
		// Cast incomming Parameter
		MiniParameter p = (MiniParameter) parameter;
		return new MiniResult(p.foo * p.bar + initial.offset);
	}

	@Override
	public void setInitialParameter(DistributedParameter parameter) {
		initial =  (MiniInitParameter) parameter;
	}
}
```

### Task Management

Simple serverside Task management using `SimpleScheduler`. It is possible to write own scheduler by implementing `Scheduler`.

```java
package candis.example.mini;

import candis.distributed.DistributedControl;
import candis.distributed.Scheduler;
import candis.distributed.SimpleScheduler;

/**
 * Minimalistic example how to initialize a set of Tasks.
 */
public class MiniControl implements DistributedControl {

	@Override
	public Scheduler initScheduler() {
		Scheduler sch = new SimpleScheduler();
		sch.setInitialParameter(new MiniInitParameter(23));
		for (int i = 0; i < 10; i++) {
			sch.addParameter(new MiniParameter(i, 3.5f));
		}
		return sch;
	}

}
```

### Generate Candis Distributed Bundle (cdb)

Using `mkcdb.py` (located in `tools/`) to create the cdb-file:

```
>> mkcdb.py miniTaks.cdb examples/mini/miniControl/ examples/mini/miniTask/
```

The generated file `miniTask.cdb` is ready to be read by the server-control application.
