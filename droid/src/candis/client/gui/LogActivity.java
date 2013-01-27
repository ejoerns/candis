package candis.client.gui;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import candis.client.R;
import candis.client.service.ActivityLogger;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrico Joerns
 */
public class LogActivity extends Activity implements View.OnClickListener {

  private TextView mLogView;
  private Button mClearLogButton;

  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(R.layout.log);

    // init text view
    mLogView = (TextView) findViewById(R.id.log_textview);
    mLogView.setMovementMethod(new ScrollingMovementMethod());

    mClearLogButton = (Button) findViewById(R.id.clear_log_button);
    mClearLogButton.setOnClickListener(this);
  }

  @Override
  public void onResume() {
    super.onResume();
    // try to open log file and print its content
    try {
      FileInputStream fistream = openFileInput(ActivityLogger.LOGFILE);
      byte[] data = new byte[fistream.available()];
      fistream.read(data);
      fistream.close();
      mLogView.setText(new String(data));
    }
    // if no log file was found, print message
    catch (FileNotFoundException ex) {
      mLogView.setText("No log data found.");
    }
    catch (IOException ex) {
      Logger.getLogger(LogActivity.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  public void onClick(View v) {
    if (v == mClearLogButton) {
      // simply delete log file
      deleteFile(ActivityLogger.LOGFILE);
      mLogView.setText("Log cleared.");
    }
  }
}
