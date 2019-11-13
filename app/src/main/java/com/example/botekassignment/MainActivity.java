package com.example.botekassignment;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnSuccessListener;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;

import java.io.Console;
import java.io.File;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;


public class MainActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    MapView _mapView = null;
    TextView _textView = null;
    private FusedLocationProviderClient _fusedLocationClient = null;
    double _totalDistanceTraveled;
    Float _orientation = null;

    private LocationRequest _locationRequest;

    GeoPoint _userPos = null;
    GeoPoint _newPos = null;


    //GeoPoint testPos1 = new GeoPoint(57.770062, 14.147508);
    //GeoPoint testPos2 = new GeoPoint(57.766077, 14.186415);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        _mapView = findViewById(R.id.map);
        final ITileSource tileSource = new XYTileSource("Mapnik", 1, 20, 256, ".png", new String[]{"https://tile.openstreetmap.org/"});
        _mapView.setTileSource(tileSource);
        _mapView.setTilesScaledToDpi(true);
        _mapView.setMaxZoomLevel(20d);
        _mapView.setMultiTouchControls(true);
        _mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        _mapView.setFlingEnabled(false);
        _mapView.getController().setCenter(new GeoPoint(57.791222, 13.418087));
        _mapView.getController().setZoom(5d);

        IConfigurationProvider osmConfig = Configuration.getInstance();
        osmConfig.setOsmdroidBasePath(new File(this.getFilesDir().getAbsoluteFile(), "osmdroid"));
        osmConfig.setOsmdroidTileCache(new File(this.getFilesDir().getAbsoluteFile(), "tile"));

        _textView = findViewById(R.id.distTraveled);

        if(checkPermissions()){
            startLocationUpdates();
        }

    }
    private LocationCallback locationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            Location location = locationResult.getLastLocation();
            if(location != null) {

                _newPos = new GeoPoint(location.getLatitude(),location.getLongitude());

                if(_userPos == null){
                    _userPos = _newPos;
                }

                double dist = (_userPos.distanceToAsDouble(_newPos));


                _totalDistanceTraveled = _totalDistanceTraveled + dist;
                _orientation = angleBetweenPoints(_userPos,_newPos);
                Log.d("ori", Double.toString(_orientation));
                _mapView.setMapOrientation(_orientation);



                DecimalFormat df = new DecimalFormat("#.##");
                _textView.setText(String.format(df.format(_totalDistanceTraveled)));

                if(dist < 5 ){
                    clearMarkers();
                    setMapCenter(_newPos);
                    addMarker(_newPos,_orientation);

                }else{
                    //drawPolyline(_userPos,_newPos,dist);
                    addPolyline(_userPos,_newPos,colorDistance(dist));
                    clearMarkers();
                    setMapCenter(_newPos);
                    addMarker(_newPos,_orientation);
                    _userPos = _newPos;
                }


            }
        }
    };

    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            requestPermissions();
            return false;
        }
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION} ,1);
    }

    protected void startLocationUpdates() {

        _locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(20000)
                .setFastestInterval(5000);


        if(_fusedLocationClient == null) {
            _fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            _fusedLocationClient.requestLocationUpdates(_locationRequest,
                    locationCallback,
                    null /* Looper */);
        }
        _fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            GeoPoint _userPos = new GeoPoint(location.getLatitude(),location.getLongitude());
                            double angle = Math.atan2(location.getLatitude(), location.getLongitude());

                            setMapCenter(_userPos);
                            setMapZoom(14);
                            addMarker(_userPos,(float)angle);
                        }
                    }
                });
    }

    @Override
    protected void onRestart(){
        super.onRestart();
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        _orientation = prefs.getFloat("ori",0);
        GeoPoint oldPoint = new GeoPoint(prefs.getFloat("locLng",0),prefs.getFloat("locLat",0));

        _mapView.getController().setCenter(oldPoint);
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat("locLng", (float)_userPos.getLongitude());
        editor.putFloat("locLat", (float)_userPos.getLatitude());
        editor.putFloat("ori",_orientation);
        editor.apply();

    }

    private float angleBetweenPoints(GeoPoint point1, GeoPoint point2){

        double angleDeg = Math.atan2((point2.getLongitude()- point1.getLongitude()),point2.getLatitude()- point1.getLatitude()) * 180 / Math.PI;

        return  (float)angleDeg;

    }

    private void drawPolyline(GeoPoint point1, GeoPoint point2, double distace){
        List<GeoPoint> geoPoints = new ArrayList<>();

        geoPoints.add(point1);
        geoPoints.add(point2);

        Polyline line = new Polyline();

        //line.getOutlinePaint().setColor(colorDistance(distace));
        line.setPoints(geoPoints);

        _mapView.getOverlayManager().add(line);
        _mapView.invalidate();


    }

    private int colorDistance(double distance){
        if(distance > 5 && distance <= 20){
            return 1;
        }else if(distance > 20 && distance <= 100){
            return 2;

        }else if (distance > 100&& distance <= 999){
            return 3;
        }else{
            return 4;
        }

    }


    public void setMapCenter(GeoPoint point) {
        if (_mapView != null && point != null) {
            _mapView.getController().setCenter(point);
        }
    }

    public void setMapZoom(double zoom) {
        if (_mapView != null) {
            _mapView.getController().setZoom(zoom);
        }
    }

    public IGeoPoint getMapCenter() {
        if (_mapView != null) {
            return _mapView.getMapCenter();
        }

        return null;
    }

    public Double getMapZoom() {
        if (_mapView != null) {
            _mapView.getZoomLevelDouble();
        }
        return null;
    }

    public Marker addMarker(GeoPoint location, float rotation) {
        if (_mapView != null && location != null) {
            Marker marker = new Marker(_mapView);
            marker.setPosition(location);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            marker.setRotation(rotation);
            _mapView.getOverlays().add(marker);
            return marker;
        }
        return null;
    }


    public Polyline addPolyline(GeoPoint location1, GeoPoint location2, int color) {
        if (location1 != null && location2 != null && _mapView != null) {
            Polyline polyline = new Polyline(_mapView);
            polyline.addPoint(location1);
            polyline.addPoint(location2);
            polyline.getOutlinePaint().setStrokeWidth(4f);
            //polyline.getOutlinePaint().setColor(color);
            _mapView.getOverlays().add(polyline);
            return polyline;
        }
        return null;
    }

    public List<Marker> getAllMarkers() {
        if (_mapView == null) {
            return null;
        }

        List<Marker> newList = new ArrayList<>();
        for (Overlay overlay : _mapView.getOverlays()) {
            if (overlay.getClass() != Marker.class) {
                newList.add((Marker) overlay);
            }
        }

        return newList;
    }

    public List<Polyline> getAllPolylines() {
        if (_mapView == null) {
            return null;
        }

        List<Polyline> newList = new ArrayList<>();
        for (Overlay overlay : _mapView.getOverlays()) {
            if (overlay.getClass() != Polyline.class) {
                newList.add((Polyline) overlay);
            }
        }

        return newList;
    }

    public void clearMarkers() {
        if (_mapView != null) {
            List<Overlay> newList = new ArrayList<>();
            for (Overlay overlay : _mapView.getOverlays()) {
                if (overlay.getClass() != Marker.class) {
                    newList.add(overlay);
                }
            }
            _mapView.getOverlays().clear();
            _mapView.getOverlays().addAll(newList);
        }
    }

    public void clearPolyline() {
        if (_mapView != null) {
            List<Overlay> newList = new ArrayList<>();
            for (Overlay overlay : _mapView.getOverlays()) {
                if (overlay.getClass() != Polyline.class) {
                    newList.add(overlay);
                }
            }
            _mapView.getOverlays().clear();
            _mapView.getOverlays().addAll(newList);
        }
    }

}
