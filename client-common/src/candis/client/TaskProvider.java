/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.client;

import candis.common.ClassloaderObjectInputStream;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedRunnable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.io.RandomAccessFile;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides access to stored Task files.
 *
 * New instances of DistributedRunnables, initial parameters
 */
public abstract class TaskProvider {

  private static final Logger LOGGER = Logger.getLogger(TaskProvider.class.getName());
  // Holds the ID of the currently loaded task.
  private String mCurrentTaskID;
  /// Holds the runnable Class for Task
  private Class mRunnableClass;
  /// Holds initial parameter for Task
  private DistributedJobParameter mInitialParam;
  /// Holds the Taks classloader
  private ClassLoader mClassLoader;
  /// Directory for tempory data
  private final File mTmpDir;
  /// Map of all tasks available on disk.
  //    private final Map<String, TaskFileManipulator> mTaskCache;
  ///
  private final JobCenter.TaskFileManipulator mTFManipulator;

  /**
   * Creates new TaskProvider.
   *
   * @param tmpDir Directory in which temporary data may be stored.
   * @param taskCache Map of all tasks available on disk.
   */
  public TaskProvider(File tmpDir) {
    mTmpDir = tmpDir;
    mTFManipulator = new JobCenter.TaskFileManipulator(mTmpDir);
  }

  /**
   * Returns the initial parameter associated with the specified task.
   *
   * @param taskID ID of task for that the initial parameter should be
   * returned.
   * @return Initial parameter
   */
  public DistributedJobParameter getInitialParam(String taskID) throws JobCenter.TaskNotFoundException {
    loadTask(taskID);
    return mInitialParam;
  }

  /**
   * Creates new instance of a runnable for specified task.
   *
   * @param taskID ID of task for that the runnable should be created.
   * @return New instance of DistributedRunnable
   * @throws InstantiationException
   * @throws IllegalAccessExceptions
   */
  public DistributedRunnable newRunnableInstance(String taskID) throws InstantiationException, IllegalAccessException, JobCenter.TaskNotFoundException {
    loadTask(taskID);
    return (DistributedRunnable) mRunnableClass.newInstance();
  }

  private void loadTask(String taskID) throws JobCenter.TaskNotFoundException {
    // if already loaded, return without loading
    if (taskID.equals(mCurrentTaskID)) {
      return;
    }
    mCurrentTaskID = taskID;
    loadClassesFromJar(mTFManipulator.getRunnableFile(taskID));
    // load initial parameter from file
    RandomAccessFile file = null;
    try {
      try {
        file = new RandomAccessFile(mTFManipulator.getInitParamFile(taskID), "r");
        byte[] rawdata = new byte[(int) file.length()];
        file.read(rawdata);
        mInitialParam = deserializeJobParameters(taskID, rawdata)[0];
      }
      finally {
        if (file != null) {
          file.close();
        }
      }
    }
    catch (FileNotFoundException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      throw new JobCenter.TaskNotFoundException();
    }
    catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Deserializes the provided serialized DistributedJobParameter
   *
   * @param runnableID runnable ID to deserialize for
   * @return Loaded DistributedJobParameter
   */
  DistributedJobParameter[] deserializeJobParameters(String taskID, byte[] rawdata) throws JobCenter.TaskNotFoundException {
    //
    loadTask(taskID);
    //
    ObjectInputStream objInstream;
    Object obj = null;
    try {
      objInstream = new ClassloaderObjectInputStream(new ByteArrayInputStream(rawdata), mClassLoader);
      obj = objInstream.readObject();
      objInstream.close();
    }
    catch (OptionalDataException ex) {
      Logger.getLogger(JobCenter.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (ClassNotFoundException ex) {
      Logger.getLogger(JobCenter.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (IOException ex) {
      Logger.getLogger(JobCenter.class.getName()).log(Level.SEVERE, null, ex);
    }
    return (DistributedJobParameter[]) obj;
  }

  /**
   * As it name claims, it loads classes from the given jar.
   *
   * @param runnableID
   * @param jarfile
   */
  protected abstract void loadClassesFromJar(final File jarfile) throws JobCenter.TaskNotFoundException;

  protected void setClassLoader(ClassLoader cl) {
    mClassLoader = cl;
  }

  protected void setRunnableClass(Class runnable) {
    mRunnableClass = runnable;
  }
}
