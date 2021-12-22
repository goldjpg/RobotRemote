package de.horn.robot.remote;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;

public class RouteElement {

    Marker mymarker = null;
    double lat;
    double lon;

    public RouteElement(double lat, double lon){
        this.lat = lat;
        this.lon = lon;
    }

}
