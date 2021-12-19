package de.horn.robot.remote;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Connection {

    boolean connected = false;
    private final OkHttpClient client;
    String robotUrl;
    String robotVersion;

    public Connection(){
        client = new OkHttpClient();
    }

    public void connect(String ip) throws IOException, JSONException {
        robotUrl = "http://" + ip + "/";
        Request request = new Request.Builder()
                .url(robotUrl + "version")
                .get()
                .build();
        Response resp = client.newCall(request).execute();
        if(resp.code() == 200){
            JSONObject responeObject = new JSONObject(resp.body().string());
            resp.body().close();
            robotVersion = responeObject.getString("ver");
            connected = true;
        }else{
            throw new ServerErrorException();
        }
    }

    static class ServerErrorException extends IOException{}

}
