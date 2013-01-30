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
{include:examples/mini/miniTask/src/candis/example/mini/MiniInitParameter.java}
```

#### Job Parameter
Each job is specified by its own job parameters (implementation of `DistributedJobParameter`).

```java
{include:examples/mini/miniTask/src/candis/example/mini/MiniJobParameter.java}
```

#### Result Data
The result of a successful done task will be transferred back in it's result container (implementation of `DistributedJobResult`).

```java
{include:examples/mini/miniTask/src/candis/example/mini/MiniJobResult.java}
```


### Distributed Code

The runnable code of a task is an implementation of `DistributedRunnable`.
 
```java
{include:examples/mini/miniTask/src/candis/example/mini/MiniRunnable.java}
```

### Task Management

The task distribution management will be done serverside. It is possible to write an own scheduler by implementing/extending `Scheduler`.
This is also the class which receives the results of the distributed tasks.
An simple Scheduler is already provided by `SimpleScheduler`.

The setup of a scheduler is done by an implementation of `DistributedControl`. To get each calculated job result directly after it's send to the server additionally implement
 `ResultReceiver`.

```java
{include:examples/mini/miniControl/src/candis/example/mini/MiniControl.java}
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
