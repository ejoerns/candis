package candis.client.pc;

import candis.client.JobCenter;
import candis.client.TaskProvider;
import candis.common.Utilities;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.DistributedRunnable;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrico Joerns
 */
public class PCTaskProvider extends TaskProvider {

  private static final Logger LOGGER = Logger.getLogger(PCTaskProvider.class.getName());
  private final File mTmpDir;

  public PCTaskProvider(File tmpDir) {
    super(tmpDir);
    mTmpDir = tmpDir;
  }

  @Override
  protected void loadClassesFromJar(File jarfile) throws JobCenter.TaskNotFoundException {
    LOGGER.info("Calling DexClassLoader with jarfile: " + jarfile.getAbsolutePath());
    final File optDexOutDir = new File(mTmpDir, "dex");
    optDexOutDir.mkdirs();
    // set new classloader
    ClassLoader classloader = null;
    try {
      classloader = new URLClassLoader(new URL[]{jarfile.toURI().toURL()});
      super.setClassLoader(classloader);
    }
    catch (MalformedURLException ex) {
      Logger.getLogger(PCTaskProvider.class.getName()).log(Level.SEVERE, null, ex);
      return;
    }

    List<String> classList = Utilities.getClassNamesInJar(jarfile);

    for (String classname : classList) {
      try {
        // finds the DistributedControl instance
        Class classToLoad = classloader.loadClass(classname);
        if ((!DistributedJobParameter.class.isAssignableFrom(classToLoad))
                && (!DistributedJobResult.class.isAssignableFrom(classToLoad))
                && (!DistributedRunnable.class.isAssignableFrom(classToLoad))) {
          LOGGER.log(Level.INFO, "Loaded class with non-default interface: {0}", classToLoad.getName());
        }
        else if (DistributedRunnable.class.isAssignableFrom(classToLoad)) {
          super.setRunnableClass(classToLoad);
        }
        else {
          LOGGER.log(Level.FINE, "Loaded class : {0}", classToLoad.getName());
        }
      }
      catch (ClassNotFoundException ex) {
        Logger.getLogger(PCTaskProvider.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }
}
