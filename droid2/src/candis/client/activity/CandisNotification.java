package candis.client.activity;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import candis.client.R;

/**
 *
 * @author Enrico Joerns
 */
public class CandisNotification {

  public static final int NOTIFICATION_ID = 4711;

  public static Notification getNotification(Context context, CharSequence text) {

    // Set the icon, scrolling text and timestamp
    Notification notification = new Notification(R.drawable.ic_stat_notify, text,
                                                 System.currentTimeMillis());

    // The PendingIntent to launch our activity if the user selects this notification
    PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                                                            new Intent(context, MainActivity.class), 0);

    // Set the info for the views that show in the notification panel.
    notification.setLatestEventInfo(
            context, "Candis client",
            text, contentIntent);

    // return the generated notification.
    return notification;
  }
}
