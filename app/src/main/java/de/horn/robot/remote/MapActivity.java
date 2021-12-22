package de.horn.robot.remote;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
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

public class MapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap = null;
    private Timer timer = null;
    private Marker robot;
    private Button startBut;
    private Button recordBut;
    private Button backBut;
    private Button resetBut;
    boolean recording = false;
    private Polyline planedRoute;
    private Polyline finishedRoute;
    private List<RouteElement> route = new ArrayList<>();
    private Vibrator vibrator;
    private int lastRoutePos = 0;
    private boolean pendingRequest = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_map);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        vibrator  = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        recordBut = findViewById(R.id.recordBut);
        recordBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(recording){
                    recording = false;
                    recordBut.setText("Record");
                    startBut.setEnabled(true);
                }else{
                    recording = true;
                    recordBut.setText("Finish");
                    startBut.setEnabled(false);
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
        startBut = findViewById(R.id.startBut);
        startBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.backgroundExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            MainActivity.con.postRoute(route);
                            MainActivity.con.startRoute();
                            MapActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
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
                    }
                });
            }
        });
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
                if(lastRoutePos != MainActivity.con.routepos){
                    lastRoutePos = MainActivity.con.routepos;
                    MapActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            renderRoute();
                        }
                    });
                }
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
            if(i < MainActivity.con.routepos && !recording){
                finishedPoints.add(new LatLng(cur.lat, cur.lon));
            }else if(i == MainActivity.con.routepos && !recording){
                plannedPoints.add(new LatLng(cur.lat, cur.lon));
                finishedPoints.add(new LatLng(cur.lat, cur.lon));
            }else{
                plannedPoints.add(new LatLng(cur.lat, cur.lon));
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
        robot = mMap.addMarker(new MarkerOptions().position(new LatLng(Settings.lastRobotLat,Settings.lastRobotLon)).title("Robot"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(Settings.lastLat,Settings.lastLon), Settings.lastZoom));
        planedRoute = mMap.addPolyline(new PolylineOptions().clickable(false).color(Color.RED));
        finishedRoute = mMap.addPolyline(new PolylineOptions().clickable(false).color(Color.GREEN));
        mMap.setOnMapLongClickListener(this);
        startTimer();
    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        if(recording){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(500);
            }
            route.add(new RouteElement(latLng.latitude, latLng.longitude));
            renderRoute();
        }
    }
}