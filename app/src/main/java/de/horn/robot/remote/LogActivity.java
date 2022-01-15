package de.horn.robot.remote;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

public class LogActivity extends AppCompatActivity {

    private ListView logView;
    private int lastSize = 0;
    private Timer timer = null;
    private ArrayAdapter<String> myAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        logView = findViewById(R.id.logListView);
        String[] startArray = {"No messages"};
        ArrayList<String> startList = new ArrayList<String>(Arrays.asList(startArray));
        myAdapter = new ArrayAdapter<String>(this, R.layout.log_item_view, R.id.logTextView, startList);
        myAdapter.setNotifyOnChange(false);
        logView.setAdapter(myAdapter);
        if (timer == null) {
            timer = new Timer();
        }
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                LogActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateItemList();
                    }
                });
            }
        }, 0, 1000);
    }

    private void updateItemList(){
        if(MainActivity.con.robotLogMessages.size() != lastSize){
            myAdapter.clear();
            for(String s:MainActivity.con.robotLogMessages){
                myAdapter.add(s);
            }
            myAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            timer.cancel();
            timer = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
}