package com.example.move;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.gms.location.GeofencingEvent;

import java.util.TimerTask;

public class GeofenceTransitionsIntentService extends IntentService {

    public GeofenceTransitionsIntentService() {
        super("GeofenceTransitionsIntentService");
    }

    // ...
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            Log.e("IntentService", "genfencingEventhasError");
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        sendBroadcastMessage(geofenceTransition);
    }

    private void sendBroadcastMessage(int transition) {
        Intent intent = new Intent("GeofenceTransitionsIntentService");
        intent.putExtra("geofenceTransition", transition);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
