package candis.client.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 *
 * @author Enrico Joerns
 */
public class BackgroundService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
}
