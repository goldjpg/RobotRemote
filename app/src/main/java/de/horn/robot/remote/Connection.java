package de.horn.robot.remote;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Connection {

    boolean connected = false;
    private final OkHttpClient client;
    String robotUrl;
    String robotVersion;
    double lastRobotLat;
    double lastRobotLon;
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");
    int routepos;
    List<String> robotLogMessages = new ArrayList<>();

    public Connection(){
        client = new OkHttpClient();
    }

    public void connect(String ip) throws IOException, JSONException, ServerErrorException {
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

    public void getLatestPos() throws IOException, JSONException, ServerErrorException {
        Request request = new Request.Builder()
                .url(robotUrl + "getPosition")
                .get()
                .build();
        Response resp = client.newCall(request).execute();
        if(resp.code() == 200){
            JSONObject responeObject = new JSONObject(resp.body().string());
            resp.body().close();
            lastRobotLat = responeObject.getDouble("lat");
            lastRobotLon = responeObject.getDouble("lng");
            Settings.lastRobotLat = (float) lastRobotLat;
            Settings.lastRobotLon = (float) lastRobotLon;
        }else{
            throw new ServerErrorException();
        }
    }

    public void startRoute(boolean restart) throws IOException, ServerErrorException, JSONException {
        JSONObject outJson = new JSONObject();
        outJson.put("restart", restart);
        RequestBody body = RequestBody.create(outJson.toString(), JSON);
        Request request = new Request.Builder()
                .url(robotUrl + "startRoute")
                .post(body)
                .build();
        Response resp = client.newCall(request).execute();
        if(resp.code() != 200){
            throw new ServerErrorException();
        }
    }

    public void stopRoute() throws IOException, JSONException, ServerErrorException {
        Request request = new Request.Builder()
                .url(robotUrl + "stopRoute")
                .get()
                .build();
        Response resp = client.newCall(request).execute();
        if(resp.code() != 200){
            throw new ServerErrorException();
        }
    }

    public void getRoutePos() throws IOException, JSONException, ServerErrorException {
        Request request = new Request.Builder()
                .url(robotUrl + "getStatus")
                .get()
                .build();
        Response resp = client.newCall(request).execute();
        if(resp.code() == 200){
            JSONObject responeObject = new JSONObject(resp.body().string());
            resp.body().close();
            routepos = responeObject.getInt("status");
            JSONArray logArray = responeObject.getJSONArray("log");
            for(int i=0;i<logArray.length();i++){
                robotLogMessages.add(logArray.getString(i));
            }
        }else{
            throw new ServerErrorException();
        }
    }

    public void postRoute(List<RouteElement> route) throws IOException, JSONException, ServerErrorException {
        JSONObject outJson = new JSONObject();
        JSONArray outRoute = new JSONArray();
        for(RouteElement re:route){
            JSONArray ob = new JSONArray();
            ob.put( re.lat);
            ob.put( re.lon);
            outRoute.put(ob);
        }
        outJson.put("route", outRoute);
        RequestBody body = RequestBody.create(outJson.toString(), JSON);
        Request request = new Request.Builder()
                .url(robotUrl + "setRoute")
                .post(body)
                .build();
        Response resp = client.newCall(request).execute();
        if(resp.code() != 200){
            throw new ServerErrorException();
        }
    }

    static class ServerErrorException extends Exception{}

}
