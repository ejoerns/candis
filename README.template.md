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
{include:examples/mini/miniTask/src/candis/example/mini/MiniInitParameter.java}
```

#### Task Parameter
Each task is specified by its own Parameters (implementation of `DistributedParameter`).

```java
{include:examples/mini/miniTask/src/candis/example/mini/MiniParameter.java}
```

#### Result Data
The result of a successful done task will be transferred back in it's result container (implementation of `DistributedResult`).

```java
{include:examples/mini/miniTask/src/candis/example/mini/MiniResult.java}
```


### Distributed Code

The task is an implementation of `DistributedTask`.
 
```java
{include:examples/mini/miniTask/src/candis/example/mini/MiniTask.java}
```

### Task Management

Simple serverside Task management using `SimpleScheduler`. It is possible to write own scheduler by implementing `Scheduler`.

```java
{include:examples/mini/miniControl/src/candis/example/mini/MiniControl.java}
```

### Generate Candis Distributed Bundle (cdb)

Using `mkcdb.py` (located in `tools/`) to create the cdb-file:

```
>> mkcdb.py miniTaks.cdb examples/mini/miniControl/ examples/mini/miniTask/
```

The generated file `miniTask.cdb` is ready to be read by the server-control application.