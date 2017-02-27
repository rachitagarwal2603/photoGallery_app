package com.example.rachit.photogallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Rachit on 10/5/2016.
 */
public class StartupReceiver extends BroadcastReceiver {
    private static final String TAG="StartupReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Received Broadcast Intent : "+ intent.getAction());
        Boolean isOn= QueryPreferences.isAlarmOn(context);
        PollService.setServiceAlarm(context, isOn);
    }
}
