package candis.droid.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import recandis.droid.R;

public class MainActivity extends Activity {

  private static final int EDIT_ID = Menu.FIRST + 2;

  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    menu.add(Menu.NONE, EDIT_ID, Menu.NONE, "Edit Prefs")
            .setIcon(R.drawable.action_settings)
            .setAlphabeticShortcut('e');

    return (super.onCreateOptionsMenu(menu));
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case EDIT_ID:
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
          startActivity(new Intent(this, EditPreferences.class));
        }
        else {
          startActivity(new Intent(this, EditPreferencesHC.class));
        }

        return (true);
    }

    return (super.onOptionsItemSelected(item));
  }
}
