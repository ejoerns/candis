package candis.client.gui;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import candis.client.R;

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

  public void onClick(View v) {
    if (v == mClearLogButton) {
      mLogView.clearComposingText();
    }
  }
}
