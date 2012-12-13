package candis.server.gui;

import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.swing.JTextArea;

/**
 * Einfacher Logger fuer JTextArea-Komponente.
 *
 * @author Enrico Joerns
 */
class CandisLoggerHandler extends Handler {

	private final JTextArea mTextArea;

	/**
	 * Constructor.
	 */
	public CandisLoggerHandler(final JTextArea textarea) {
		mTextArea = textarea;
		configure();
	}

	/**
	 * This method loads the configuration properties from the JDK level
	 * configuration file with the help of the LogManager class. It then sets its
	 * level, filter and formatter properties.
	 */
	private void configure() {
		final LogManager manager = LogManager.getLogManager();
		String className = this.getClass().getName();
		String level = manager.getProperty(className + ".level");
		String filter = manager.getProperty(className + ".filter");
		String formatter = manager.getProperty(className + ".formatter");

		//accessing super class methods to set the parameters
		setLevel(level == null ? Level.INFO : Level.parse(level));
		if (filter != null) {
			setFilter(makeFilter(filter));
		}
		setFormatter(makeFormatter(formatter));
	}

	/**
	 * private method constructing a Filter object with the filter name.
	 *
	 * @param filterName the name of the filter
	 * @return the Filter object
	 */
	private Filter makeFilter(String filterName) {
		Class c = null;
		Filter f = null;
		try {
			c = Class.forName(filterName);
			f = (Filter) c.newInstance();
		}
		catch (InstantiationException ex) {
			Logger.getLogger(CandisLoggerHandler.class.getName()).log(Level.SEVERE, null, ex);
		}
		catch (IllegalAccessException ex) {
			Logger.getLogger(CandisLoggerHandler.class.getName()).log(Level.SEVERE, null, ex);
		}
		catch (ClassNotFoundException ex) {
			Logger.getLogger(CandisLoggerHandler.class.getName()).log(Level.SEVERE, null, ex);
		}
		return f;
	}

	/**
	 * private method creating a Formatter object with the formatter name. If no
	 * name is specified, it returns a SimpleFormatter object
	 *
	 * @param formatterName the name of the formatter
	 * @return Formatter object
	 */
	private Formatter makeFormatter(String formatterName) {
		Class c = null;
		Formatter f = null;

		try {
			c = Class.forName(formatterName);
			f = (Formatter) c.newInstance();
		}
		catch (Exception e) {
			f = new SimpleFormatter();
		}
		return f;
	}

	/**
	 * This is the overridden publish method of the abstract super class Handler.
	 * This method writes the logging information to the associated Java window.
	 * This method is synchronized to make it thread-safe. In case there is a
	 * problem, it reports the problem with the ErrorManager, only once and
	 * silently ignores the others.
	 *
	 * @record the LogRecord object
	 *
	 */
	public synchronized void publish(LogRecord record) {
		String message = null;
		//check if the record is loggable
		if (!isLoggable(record)) {
			return;
		}
		try {
			message = getFormatter().format(record);
		}
		catch (Exception e) {
			reportError(null, e, ErrorManager.FORMAT_FAILURE);
		}

		try {
			mTextArea.append(message);
		}
		catch (Exception ex) {
			reportError(null, ex, ErrorManager.WRITE_FAILURE);
		}
	}

	@Override
	public void flush() {
	}

	@Override
	public void close() {
	}
}
