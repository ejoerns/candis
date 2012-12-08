candis
======

Distributed computing on Android-Devices.


## Minimalistic Example

This is an example, how to implement a distributed Task with candis. 

### Data exchange

To transfer data between management server and the remote App with serializable classes.

#### Task Parameter
Each task is specified by an implementation of `DistributedParameter`.
```java
{include:examples/mini/miniTask/src/candis/example/mini/MiniParameter.java}
```

#### Result Data
The result of a successful done task will be transferred back with an implementation of `DistributedResult`.
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
