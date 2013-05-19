package com.vipercn.viper4android.xhifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ServiceLaunchReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		Log.i("ViPER4Android_XHiFi", "Starting service, reason = ServiceLaunchReceiver");
		context.startService(new Intent(HeadsetService.NAME));
	}
}
