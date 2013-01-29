package candis.client.gui.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import candis.client.R;
import candis.client.service.BackgroundService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrico Joerns
 */
public class SettingsActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

  // Keys as defined in xml/preferences.xml
  public static final String HOSTNAME = "pref_host";
  public static final String PORTNAME = "pref_port";
  public static final String POWERMODE = "pref_powermode";
  // 
  private EditTextPreference mHostnamePreference;
  private EditTextPreference mPortnamePreference;
  private ListPreference mPowermodePreference;
  //
  SharedPreferences mSharedPreferences;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Load the preferences from an XML resource
    addPreferencesFromResource(R.xml.preferences);

    // 
    mHostnamePreference = (EditTextPreference) getPreferenceScreen().findPreference(HOSTNAME);
    mPortnamePreference = (EditTextPreference) getPreferenceScreen().findPreference(PORTNAME);
    mPowermodePreference = (ListPreference) getPreferenceScreen().findPreference(POWERMODE);
  }
  
  @Override
  protected void onResume() {
    super.onResume();
    getPreferenceScreen().getSharedPreferences()
            .registerOnSharedPreferenceChangeListener(this);

    mSharedPreferences = getPreferenceScreen().getSharedPreferences();
    mHostnamePreference.setSummary(mHostnamePreference.getText());
    mPortnamePreference.setSummary(mPortnamePreference.getText());
    mPowermodePreference.setSummary(mPowermodePreference.getEntry());
  }

  @Override
  protected void onPause() {
    Log.d("SettingsActivity", "onPause()");
    super.onPause();
    Log.e("BUH", "Commiting Preferences");
//    mSharedPreferences.edit().apply();
    getPreferenceScreen().getSharedPreferences()
            .unregisterOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    Preference pref = findPreference(key);


    Log.i("SettingsActivity", "onSharedPreferenceChanged");
    // Let's do something a preference value changes
    if (pref instanceof EditTextPreference) {
      EditTextPreference textPref = (EditTextPreference) pref;
      pref.setSummary(textPref.getText());
//      bundle.putString(key, textPref.getText());
    }
    else if (pref instanceof ListPreference) {
      ListPreference listPref = (ListPreference) pref;
      pref.setSummary(listPref.getEntry());
//      bundle.putCharSequence(key, listPref.getEntry());
    }


  }
}
