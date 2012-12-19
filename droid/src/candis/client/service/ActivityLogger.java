package candis.client.service;

import android.content.Context;
import android.content.Intent;
import candis.client.JobCenterHandler;
import candis.client.MainActivity;
import java.util.UUID;

/**
 *
 * @author Enrico Joerns
 */
public class ActivityLogger implements JobCenterHandler {

	private final Context mContext;
	Intent intent;
	String msg = "Pseudo message";

	public ActivityLogger(Context context) {
		mContext = context;
		intent = new Intent(mContext, MainActivity.class);
		intent.setAction(BackgroundService.JOB_CENTER_HANDLER)
						.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
						.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	}

	public void onBinaryReceived(UUID uuid) {
		msg = String.format("Task with UUID %s received", uuid.toString());
		intent.putExtra("Message", msg);
		mContext.startActivity(intent);
	}

	public void onInitialParameterReceived(UUID uuid) {
		msg = String.format("Initial Paramater for Task with UUID %s received", uuid.toString());
		intent.putExtra("Message", msg);
		mContext.startActivity(intent);
	}

	public void onJobExecutionStart(UUID uuid) {
		msg = String.format("Job for Task with UUID %s started", uuid.toString());
		intent.putExtra("Message", msg);
		mContext.startActivity(intent);
	}

	public void onJobExecutionDone(UUID uuid) {
		msg = String.format("Job for Task with UUID %s stopped", uuid.toString());
		intent.putExtra("Message", msg);
		mContext.startActivity(intent);
	}
}
