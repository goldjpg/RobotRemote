package de.horn.robot.remote;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
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
    boolean recording = false;
    private Polyline planedRoute;
    private Polyline finishedRoute;
    private List<RouteElement> route = new ArrayList<>();
    private Vibrator vibrator;
    private boolean pendingRequest = false;
    private View recordingView;
    private View playView;
    private UserMode currentUiMode;

    private enum UserMode{
        editMode, playMode
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_map);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        vibrator  = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        recordingView = findViewById(R.id.recordPanel);
        playView = findViewById(R.id.playPanel);

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
                MainActivity.backgroundExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        if(route.size() > 1){
                            try {
                                MainActivity.con.postRoute(route);
                                MapActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        setCurrentUiMode(UserMode.playMode);
                                        Toast.makeText(MapActivity.this, "Route sent successfully.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                                MapActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MapActivity.this, "Failed to send route.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }else{
                            MapActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MapActivity.this, "You have to specify at least two route elements.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                });
            }
        });
        startBut = findViewById(R.id.startBut);
        startBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.backgroundExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            MainActivity.con.startRoute(false);
                            MapActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MapActivity.this, "Route started successfully.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            MapActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MapActivity.this, "Failed to start route.", Toast.LENGTH_SHORT).show();
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
                MainActivity.backgroundExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            MainActivity.con.startRoute(true);
                            MapActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MapActivity.this, "Route restarted successfully.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            MapActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MapActivity.this, "Failed to restarted route.", Toast.LENGTH_SHORT).show();
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
                MainActivity.backgroundExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            MainActivity.con.stopRoute();
                            MapActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MapActivity.this, "Route stopped successfully.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            MapActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MapActivity.this, "Failed stop route.", Toast.LENGTH_SHORT).show();
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
                        MapActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setCurrentUiMode(UserMode.editMode);
                            }
                        });
                    }
                });
            }
        });
        setCurrentUiMode(UserMode.editMode);
    }

    private void setCurrentUiMode(UserMode newMode){
        recordingView.setVisibility(View.GONE);
        playView.setVisibility(View.GONE);
        if(newMode == UserMode.editMode){
            recordingView.setVisibility(View.VISIBLE);
        }else if(newMode == UserMode.playMode){
            playView.setVisibility(View.VISIBLE);
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
        }else{
            return false;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Settings.saveSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startTimer();
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
        try {
            timer.cancel();
            timer = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        if(currentUiMode == UserMode.editMode || currentUiMode == UserMode.playMode){
            for(int i=0;i<route.size();i++){
                RouteElement cur = route.get(i);
                if(cur.mymarker == null){
                    cur.mymarker = mMap.addMarker(new MarkerOptions().position(new LatLng(cur.lat,cur.lon)));
                }
                if(i < MainActivity.con.routepos && currentUiMode == UserMode.playMode){
                    finishedPoints.add(new LatLng(cur.lat, cur.lon));
                }else if(i == MainActivity.con.routepos && currentUiMode == UserMode.playMode){
                    plannedPoints.add(new LatLng(cur.lat, cur.lon));
                    finishedPoints.add(new LatLng(cur.lat, cur.lon));
                }else{
                    plannedPoints.add(new LatLng(cur.lat, cur.lon));
                }
            }
        }
        planedRoute.setPoints(plannedPoints);
        finishedRoute.setPoints(finishedPoints);
        robot.setPosition(new LatLng(MainActivity.con.lastRobotLat, MainActivity.con.lastRobotLon));
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        Bitmap oldMarker = BitmapFactory.decodeResource(getResources(), R.drawable.robotmarker);
        Bitmap smallMarker = Bitmap.createScaledBitmap(oldMarker, 100, 100, false);
        robot = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(smallMarker)).position(new LatLng(Settings.lastRobotLat,Settings.lastRobotLon)).title("Robot"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(Settings.lastLat,Settings.lastLon), Settings.lastZoom));
        planedRoute = mMap.addPolyline(new PolylineOptions().clickable(false).color(Color.RED));
        finishedRoute = mMap.addPolyline(new PolylineOptions().clickable(false).color(Color.GREEN));
        mMap.setOnMapLongClickListener(this);
        mMap.getUiSettings().setRotateGesturesEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        startTimer();
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