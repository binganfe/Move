package com.example.move;

import android.Manifest;
import android.app.Dialog;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.ErrorDialogFragment;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER;
import static com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT;
import static com.google.android.gms.location.Geofence.NEVER_EXPIRE;
/*
* Note: I wanted to use geofence api to implement the office reminder but I did not know
*       why only GEOFENCE_EXIT is triggered. Then I used another way to implement
*       the functionality.
*
*       However, I did not delete the geofence related part but just commented it out because
*       I would like you to know my whole thinking process of resolving this assignment.
*       You can delete those comments and GeofenceTransitionsIntentService and <service> tag
*       in the manifest if you would like to.*/

public class Main2Activity extends AppCompatActivity {

    static Main2Activity instance;
    DailyDistanceDB userDb;
    static String formattedDate;
    String username =null;
    float distance = 0;
    float distanceStored = 0;
    int multiplesof1000 = 0;//for milestones
    Location last;
    LocationManager mLocationManager;
    Context mContext;
    TextView tvbasic;
    TextView distanceView;
    TextView officeView;
    private LocationListener listener;
    private Button display;
    private Button recordOffice;
    private Button leaderBoard;
    private GeofencingClient geofencingClient;
    double officeLatitude = -91;
    double officeLongitude = -181;
    Location officeLocation = null;
    boolean inOffice = false;
    List<Geofence> geofenceList;
    PendingIntent geofencePendingIntent = null;
    Timer officeTime;
    TimerTask tt;
    final Handler handler = new Handler();
    int count = 0;
    int multiplesof3600 = 0;//for office reminder

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        instance = this;
        tvbasic = findViewById(R.id.textView);
        tvbasic.setMovementMethod(new ScrollingMovementMethod());
        distanceView = findViewById(R.id.textViewDis);
        officeView = findViewById(R.id.textViewOffice);
        display = findViewById(R.id.button1);
        recordOffice = findViewById(R.id.button2);
        leaderBoard = findViewById(R.id.buttonLeaderBoard);
        mContext = this;
        createUserDB();
        setDisplay();
        setRecordOffice();
        setLeaderBoard();
        officeTime = new Timer();
        buildTimerTask();
        startLocationUpdate();
        //buildGeofence();
//        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                int geofenceTransition = intent.getIntExtra("geofenceTransition",0);
//                if(geofenceTransition!=0){
//                    officeAlert(geofenceTransition);
//                }
//            }
//        },new IntentFilter("GeofenceTransitionsIntentService"));
    }

    public void createUserDB(){
        Intent intent = getIntent();
        username = intent.getStringExtra("SENDTOSECONDACTIVITY");
        userDb = new DailyDistanceDB(Main2Activity.this,username);
        Date currentime = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("MM-dd-yyyy");
        formattedDate = df.format(currentime);
        //if office location is already stored, extract the location.
        if(userDb.checkDateExist("officeLatitude")){
            officeLatitude = (double)userDb.getDistance("officeLatitude");
            officeLongitude = (double)userDb.getDistance("officeLongitude");
            officeLocation = new Location("");
            officeLocation.setLatitude(officeLatitude);
            officeLocation.setLongitude(officeLongitude);
        }
        //check if it is the first time for this user to open the app today, if yes, start distance from 0, if no, retrieve data
        if(userDb.checkDateExist(formattedDate)){
            distance = userDb.getDistance(formattedDate);
            distanceView.setText("Today Distance(feet): "+distance);
            distanceStored = distance;
            multiplesof1000 = (int)distance/1000;
            Toast.makeText(Main2Activity.this, "Retrieved Today Data", Toast.LENGTH_LONG).show();
        }else{
            boolean inserted = userDb.insertDistance(formattedDate, 0.0f);
            if(!inserted){
                Toast.makeText(Main2Activity.this, "Distance Insertion Failed", Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(Main2Activity.this, "Start Recording", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void setDisplay(){
        display.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Cursor res = userDb.getAllData();
                if(res.getCount()==0){
                    Toast.makeText(mContext,"No data recorded for this user",Toast.LENGTH_LONG).show();
                }
                while(res.moveToNext()){
                    String date = res.getString(0);
                    if(date.equals("officeLatitude")||date.equals("officeLongitude")){

                    }else{
                        tvbasic.append("Date: "+res.getString(0)+"      Distance(feet): "+res.getFloat(1)+"\n");
                    }

                }
            }
        });
    }

    public void setRecordOffice(){
        recordOffice.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                //record current location as office location
                officeLatitude = last.getLatitude();
                officeLongitude = last.getLongitude();
                officeLocation = new Location("");
                officeLocation.setLatitude(officeLatitude);
                officeLocation.setLongitude(officeLongitude);
                officeAlert(GEOFENCE_TRANSITION_ENTER);
                inOffice = true;
                //check if this user's database already contains the office location, if yes update it, if not insert it.
                if(userDb.checkDateExist("officeLatitude")){
                    boolean isLatitudeUpdated = userDb.updateDistance("officeLatitude", (float)officeLatitude);
                    boolean isLongitudeUpdated = userDb.updateDistance("officeLongitude", (float)officeLongitude);
                    if(isLatitudeUpdated&&isLongitudeUpdated){
                        Toast.makeText(mContext,"Office Location Update Successed",Toast.LENGTH_LONG).show();
                        //buildGeofence();
                    }else{
                        Toast.makeText(mContext,"Office Location Update Failed",Toast.LENGTH_LONG).show();
                    }
                }else{
                    boolean latitudeInserted = userDb.insertDistance("officeLatitude", (float)officeLatitude);
                    boolean longitudeInserted = userDb.insertDistance("officeLongitude", (float)officeLongitude);
                    if(!(latitudeInserted&&longitudeInserted)){
                        Toast.makeText(Main2Activity.this, "Office Location Insertion Failed", Toast.LENGTH_LONG).show();
                    }else{
                        Toast.makeText(Main2Activity.this, "Office Location Insertion Successed", Toast.LENGTH_LONG).show();
                        //buildGeofence();
                    }
                }
            }
        });
    }

    public void setLeaderBoard(){
        leaderBoard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Main2Activity.this, LeaderBoardActivity.class);
                intent.putExtra("SENDTOTHIRDACTIVITY",username);
                startActivityForResult(intent,1);
            }
        });
    }

    public void startLocationUpdate(){
        mLocationManager = (LocationManager)this.getSystemService(this.LOCATION_SERVICE);
        checkPermission();
        last = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        //check if start point is already in office area
        if(officeLocation!=null&&last!=null){
            float distanceFromOffice = last.distanceTo(officeLocation);
            if(distanceFromOffice<100){
                inOffice = true;
                officeAlert(GEOFENCE_TRANSITION_ENTER);
            }
        }
        //update walk distance when location changed
        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                float meter = location.distanceTo(last);
                distance += meter*3.28084;
                distanceView.setText("Today Distance(feet): "+distance);
                last = location;
                if(officeLocation!=null){
                    float distanceFromOffice = last.distanceTo(officeLocation);
                    if(distanceFromOffice<=100&&(!inOffice)){
                        officeAlert(GEOFENCE_TRANSITION_ENTER);
                        inOffice = true;
                    }else if(distanceFromOffice>100&&inOffice){
                        officeAlert(GEOFENCE_TRANSITION_EXIT);
                        inOffice = false;
                    }
                }
                checkMilestones();
                updateDistanceinDB();

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        mLocationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 2000, 10, listener);
    }

    public void checkPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        1);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
        }
    }

    public void checkMilestones(){
        if((distance/1000-multiplesof1000)>1){
            multiplesof1000+=1;
            Toast.makeText(mContext,"You have achieved "+multiplesof1000*1000+" feet",Toast.LENGTH_LONG).show();
        }
    }

    public void updateDistanceinDB(){
        //reduce frequency of updating database
        if((distance-distanceStored)>150){
            boolean isUpdated = userDb.updateDistance(formattedDate, distance);
            if(isUpdated){
                distanceStored = distance;
            }else{
                Toast.makeText(mContext,"Distance updated failed",Toast.LENGTH_LONG).show();
            }

        }
    }



//    public void buildGeofence(){
//        if((officeLatitude!=-91)&&(officeLongitude!=-181)){
//            geofencingClient = LocationServices.getGeofencingClient(this);
//            geofenceList = new ArrayList<Geofence>();
//            geofenceList.add(new Geofence.Builder().setRequestId("officeLocation")
//                    .setCircularRegion(officeLatitude,officeLongitude,100)
//                    .setExpirationDuration(NEVER_EXPIRE)
//                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT).build());
//            checkPermission();
//            geofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
//                    .addOnSuccessListener(this, new OnSuccessListener<Void>() {
//                        @Override
//                        public void onSuccess(Void aVoid) {
//                            // Geofences added
//                            // ...
//                            Toast.makeText(mContext,"Start Monitoring Time Staying in Office",Toast.LENGTH_LONG).show();
//                        }
//                    })
//                    .addOnFailureListener(this, new OnFailureListener() {
//                        @Override
//                        public void onFailure(@NonNull Exception e) {
//                            // Failed to add geofences
//                            // ...
//                            Toast.makeText(mContext,"Can't Start Monitoring",Toast.LENGTH_LONG).show();
//                        }
//                    });
//
//        }
//    }
//
//    private GeofencingRequest getGeofencingRequest() {
//        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
//        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER );
//        builder.addGeofences(geofenceList);
//        return builder.build();
//    }
//
//    private PendingIntent getGeofencePendingIntent() {
//        // Reuse the PendingIntent if we already have it.
//        if (geofencePendingIntent != null) {
//            return geofencePendingIntent;
//        }
//        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
//        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
//        // calling addGeofences() and removeGeofences().
//        geofencePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.
//                FLAG_UPDATE_CURRENT);
//        return geofencePendingIntent;
//    }

    public void buildTimerTask(){
        tt = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        officeView.setText("You've stayed in office for: "+count+" seconds");
                        count++;
                        if(count/3600>multiplesof3600){
                            int hour = count/3600;
                            multiplesof3600 = hour;
                            Toast.makeText(mContext,"You've stayed in office for: "+hour+" hours.Stand up and walk!",Toast.LENGTH_LONG).show();
                        }
                    }
                });

            }
        };
    }

    public void officeAlert(int geofenceTransition){
        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            officeTime.scheduleAtFixedRate(tt,0,1000);//timer task runs every 1s.
        }else if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT){
            count = 0;
            multiplesof3600 = 0;
            tt.cancel();
            officeView.setText("");
            buildTimerTask();
        } else {
            // Log the error.
            Log.e("TransitionInvalidType", getString(geofenceTransition));
        }
    }

    //restore Main2Activity when come back from LeaderBoardActivity
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if(resultCode == RESULT_OK) {
                username = data.getStringExtra("SENDBACKTOSECONDACTIVITY");
            }
        }
    }


}
