package de.horn.robot.remote;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    View loadingSpinner;
    View loginPane;
    static Connection con;
    EditText robotIp;
    Button connectBut;
    static ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loginPane = findViewById(R.id.setup_ip_pane);
        loadingSpinner = findViewById(R.id.main_loading_spinner);
        Settings.myprefs = getSharedPreferences("config", MODE_PRIVATE);
        Settings.loadSettings();
        con = new Connection();
        con.latOffset = Settings.lastRobotLatOffset;
        con.lonOffset = Settings.lastRobotLonOffset;
        robotIp = findViewById(R.id.robot_ip_input);
        connectBut = findViewById(R.id.connect_button);
        connectBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect();
            }
        });
        robotIp.setText(Settings.lastIp);
        connect();
    }

    private void connect(){
        final String myIp = robotIp.getText().toString();
        if(myIp.equals("0.0.0.0")||myIp.split("\\.").length == 0){
            return;
        }
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 0);
            Toast.makeText(MainActivity.this, "Please allow access to location.", Toast.LENGTH_LONG).show();
            return;
        }
        loadingSpinner.setVisibility(View.VISIBLE);
        loginPane.setVisibility(View.GONE);
        backgroundExecutor.submit(() -> {
            try {
                con.connect(myIp);
                Settings.lastIp = myIp;
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(MainActivity.this, MapActivity.class));
                        Toast.makeText(MainActivity.this, "Connected!", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Failed to connect.", Toast.LENGTH_LONG).show();
                        loadingSpinner.setVisibility(View.GONE);
                        loginPane.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

}