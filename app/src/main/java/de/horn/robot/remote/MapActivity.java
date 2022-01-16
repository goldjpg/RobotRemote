package de.horn.robot.remote;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap = null;
    private Timer timer = null;
    private Marker robot;
    private Button playBut;
    private Button recordBut;
    private Button backBut;
    private Button resetBut;
    private Button startBut;
    private Button restartBut;
    private Button pauseBut;
    private Button exitBut;
    private Button setOffsetBut;
    boolean recording = false;
    private Polyline planedRoute;
    private Polyline finishedRoute;
    private List<RouteElement> route = new ArrayList<>();
    private Vibrator vibrator;
    private boolean pendingRequest = false;
    private View recordingView;
    private View playView;
    private View offsetView;
    private UserMode currentUiMode;
    private Marker robotOffsetMarker = null;
    private ProgressDialog myActionProgress;
    private boolean didAskForCalibration = false;

    private enum UserMode{
        editMode, playMode, setRobotOffset
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_map);
        setTitle(R.string.app_name);

        vibrator  = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        recordingView = findViewById(R.id.recordPanel);
        playView = findViewById(R.id.playPanel);
        offsetView = findViewById(R.id.setOffsetPanel);


        recordBut = findViewById(R.id.recordBut);
        recordBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(recording){
                    recording = false;
                    recordBut.setText("Record");
                    playBut.setEnabled(true);
                }else{
                    recording = true;
                    recordBut.setText("Finish");
                    playBut.setEnabled(false);
                }
                renderRoute();
            }
        });
        backBut = findViewById(R.id.removeLastBut);
        backBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(recording){
                    if(route.size() > 1){
                        RouteElement ele = route.get(route.size()-1);
                        if(ele.mymarker != null){
                            ele.mymarker.remove();
                        }
                        route.remove(ele);
                        renderRoute();
                    }
                }
            }
        });
        resetBut = findViewById(R.id.clearBut);
        resetBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for(RouteElement ele:route){
                    if(ele.mymarker != null){
                        ele.mymarker.remove();
                    }
                }
                route.clear();
                renderRoute();
            }
        });
        playBut = findViewById(R.id.playBut);
        playBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!didAskForCalibration){
                    AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
                    builder.setMessage("It's recommended to calibrate the robot before playing a route.");
                    builder.setTitle("Robot not calibrated");
                    builder.setCancelable(false);
                    builder.setNegativeButton(("cancel"), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    builder.setPositiveButton("calibrate", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            enterCalibrationMode();
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                    didAskForCalibration = true;
                }else{
                    if(route.size() > 1) {
                        startLoadingAction("Sending route...");
                        MainActivity.backgroundExecutor.submit(new Runnable() {
                            @Override
                            public void run() {
                                if (route.size() > 1) {
                                    try {
                                        MainActivity.con.postRoute(route);
                                        MapActivity.this.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                setCurrentUiMode(UserMode.playMode);
                                                endLoadingAction("");
                                            }
                                        });
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        MapActivity.this.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                endLoadingAction("Failed to send route.");
                                            }
                                        });
                                    }
                                }
                            }
                        });
                    }else{
                        MapActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MapActivity.this, "You have to specify at least two route points.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        });
        startBut = findViewById(R.id.startBut);
        startBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLoadingAction("Starting route...");
                MainActivity.backgroundExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            MainActivity.con.startRoute(false);
                            MapActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    endLoadingAction("");
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            MapActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    endLoadingAction("Failed to start route.");
                                }
                            });
                        }
                    }
                });
            }
        });
        restartBut = findViewById(R.id.restartBut);
        restartBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLoadingAction("Restarting route...");
                MainActivity.backgroundExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            MainActivity.con.startRoute(true);
                            MapActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    endLoadingAction("");
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            MapActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    endLoadingAction("Failed to restart route.");
                                }
                            });
                        }
                    }
                });
            }
        });
        pauseBut = findViewById(R.id.pauseBut);
        pauseBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLoadingAction("Stopping route...");
                MainActivity.backgroundExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            MainActivity.con.stopRoute();
                            MapActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    endLoadingAction("");
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            MapActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    endLoadingAction("Failed to stop route.");
                                }
                            });
                        }
                    }
                });
            }
        });
        exitBut = findViewById(R.id.exitBut);
        exitBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.backgroundExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            MainActivity.con.stopRoute();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                setCurrentUiMode(UserMode.editMode);
            }
        });
        setOffsetBut = findViewById(R.id.setOffsetBut);
        setOffsetBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLoadingAction("Calculating offset. This will take about 5 seconds.");
                LatLng markerPos = robotOffsetMarker.getPosition();
                MainActivity.backgroundExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            List<double[]> calculatedOffsets = new ArrayList<>();
                            int sleepSeconds = 0;
                            while (sleepSeconds <= 5){
                                MainActivity.con.getLatestPos();
                                double newLatOffset = markerPos.latitude - MainActivity.con.lastRobotLat;
                                double newLonOffset = markerPos.longitude - MainActivity.con.lastRobotLon;
                                calculatedOffsets.add(new double[]{newLatOffset, newLonOffset});
                                Thread.sleep(1000);
                                sleepSeconds += 1;
                            }
                            double latOffsetSum = 0;
                            double lonOffsetSum = 0;
                            for(double[] values:calculatedOffsets){
                                latOffsetSum += values[0];
                                lonOffsetSum += values[0];
                            }
                            MainActivity.con.setOffset(latOffsetSum/calculatedOffsets.size(), lonOffsetSum/calculatedOffsets.size());
                            MapActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    endLoadingAction("");
                                    robotOffsetMarker.setVisible(false);
                                    setCurrentUiMode(UserMode.editMode);
                                    renderRoute();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            MapActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    endLoadingAction("Failed to set offset.");
                                }
                            });
                        }
                    }
                });
            }
        });
        setCurrentUiMode(UserMode.editMode);
        startTimer();
        startLoadingAction("Loading map...");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        moveTaskToBack(true);
    }

    private void startLoadingAction(String message){
        myActionProgress = new ProgressDialog(MapActivity.this);
        myActionProgress.setTitle("Loading");
        myActionProgress.setMessage(message);
        myActionProgress.setCancelable(false);
        myActionProgress.show();
    }

    private void endLoadingAction(String errorMessage){
        myActionProgress.hide();
        if(!errorMessage.equals("")){
            Toast.makeText(MapActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private void setCurrentUiMode(UserMode newMode){
        recordingView.setVisibility(View.GONE);
        playView.setVisibility(View.GONE);
        offsetView.setVisibility(View.GONE);
        if(newMode == UserMode.editMode){
            setTitle("Edit Mode");
            recordingView.setVisibility(View.VISIBLE);
        }else if(newMode == UserMode.playMode){
            setTitle("Play Mode");
            playView.setVisibility(View.VISIBLE);
        }else if(newMode == UserMode.setRobotOffset){
            setTitle("Offset Mode");
            offsetView.setVisibility(View.VISIBLE);
        }
        currentUiMode = newMode;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_options, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        if(item.getItemId() == R.id.action_show_log){
            startActivity(new Intent(this, LogActivity.class));
        }else if(item.getItemId() == R.id.action_calibration_mode){
            if(currentUiMode == UserMode.editMode && !recording && mMap != null){
                enterCalibrationMode();
            }else{
                Toast.makeText(MapActivity.this, "You have to be in Edit-Mode to use this feature.", Toast.LENGTH_SHORT).show();
            }
        }else{
            return false;
        }
        return true;
    }

    private void enterCalibrationMode(){
        LatLng markerPos = new LatLng(MainActivity.con.lastRobotLat + MainActivity.con.latOffset,MainActivity.con.lastRobotLon + MainActivity.con.lonOffset);
        if(robotOffsetMarker == null){
            robotOffsetMarker = mMap.addMarker(new MarkerOptions().position(markerPos).draggable(true));
        }else{
            robotOffsetMarker.setPosition(markerPos);
            robotOffsetMarker.setVisible(true);
        }
        setCurrentUiMode(UserMode.setRobotOffset);
        renderRoute();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            timer.cancel();
            timer = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        Settings.saveSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mMap != null) {
            LatLng camPos = mMap.getCameraPosition().target;
            Settings.lastLat = (float) camPos.latitude;
            Settings.lastLon = (float) camPos.longitude;
            Settings.lastZoom = mMap.getCameraPosition().zoom;
        }
        Settings.saveSettings();
    }

    private void startTimer() {
        if(mMap == null){
            return;
        }
        if (timer == null) {
            timer = new Timer();
        }
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(pendingRequest){
                    return;
                }
                pendingRequest = true;
                try {
                    MainActivity.con.getLatestPos();
                    MainActivity.con.getRoutePos();
                } catch (IOException e) {
                    MainActivity.con.connected = false;
                    e.printStackTrace();
                } catch (Connection.ServerErrorException | JSONException e) {
                    e.printStackTrace();
                }
                MapActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        renderRoute();
                    }
                });
                pendingRequest = false;
            }
        }, 0, 1000);
    }

    private void renderRoute(){
        List<LatLng> plannedPoints = new ArrayList<>();
        List<LatLng> finishedPoints = new ArrayList<>();
        for(int i=0;i<route.size();i++){
            RouteElement cur = route.get(i);
            if(cur.mymarker == null){
                cur.mymarker = mMap.addMarker(new MarkerOptions().position(new LatLng(cur.lat,cur.lon)));
            }
            if(currentUiMode == UserMode.editMode || currentUiMode == UserMode.playMode){
                if(!cur.mymarker.isVisible()){
                    cur.mymarker.setVisible(true);
                }
                if(i < MainActivity.con.routepos && currentUiMode == UserMode.playMode){
                    finishedPoints.add(new LatLng(cur.lat, cur.lon));
                }else if(i == MainActivity.con.routepos && currentUiMode == UserMode.playMode){
                    plannedPoints.add(new LatLng(cur.lat, cur.lon));
                    finishedPoints.add(new LatLng(cur.lat, cur.lon));
                }else{
                    plannedPoints.add(new LatLng(cur.lat, cur.lon));
                }
            }else{
                if(cur.mymarker.isVisible()){
                    cur.mymarker.setVisible(false);
                }
            }
        }
        planedRoute.setPoints(plannedPoints);
        finishedRoute.setPoints(finishedPoints);
        if(currentUiMode != UserMode.setRobotOffset){
            robot.setPosition(new LatLng(MainActivity.con.lastRobotLat + MainActivity.con.latOffset, MainActivity.con.lastRobotLon + MainActivity.con.lonOffset));
        }else{
            robot.setPosition(new LatLng(MainActivity.con.lastRobotLat, MainActivity.con.lastRobotLon));
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        Bitmap oldMarker = BitmapFactory.decodeResource(getResources(), R.drawable.robotmarker);
        Bitmap smallMarker = Bitmap.createScaledBitmap(oldMarker, 100, 100, false);
        robot = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(smallMarker)).position(new LatLng(Settings.lastRobotLat + MainActivity.con.latOffset,Settings.lastRobotLon+ MainActivity.con.lonOffset)));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(Settings.lastLat,Settings.lastLon), Settings.lastZoom));
        planedRoute = mMap.addPolyline(new PolylineOptions().clickable(false).color(Color.RED));
        finishedRoute = mMap.addPolyline(new PolylineOptions().clickable(false).color(Color.GREEN));
        mMap.setOnMapLongClickListener(this);
        mMap.getUiSettings().setRotateGesturesEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(@NonNull Marker marker) {}
            @Override
            public void onMarkerDragEnd(@NonNull Marker marker) {}
            @Override
            public void onMarkerDrag(@NonNull Marker marker) {}
        });
        startTimer();
        endLoadingAction("");
    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        if(recording){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(100);
            }
            route.add(new RouteElement(latLng.latitude, latLng.longitude));
            renderRoute();
        }
    }
}