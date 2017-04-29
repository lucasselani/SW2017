package br.sw.cacadoresdelivros.view.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import br.sw.cacadoresdelivros.R;
import br.sw.cacadoresdelivros.view.fragments.MapFragment;

public class MainActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private final String DEBUG_TAG = getClass().getSimpleName();
    LocationRequest mLocationRequest = null;
    GoogleApiClient mGoogleApiClient = null;
    Location mCurrentLocation = null;
    Location mLastLocation = null;

    LatLng mCurrlatLng = null;
    MapFragment mMapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        if(askPermissions()){
            mMapFragment = MapFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, mMapFragment)
                    .commit();
        }
    }

    public void mapReady(){
        Log.v("Main", "MapReady");
        buildGoogleApiClient();
        mGoogleApiClient.connect();
    }

    protected synchronized void buildGoogleApiClient() {
        Log.v("Main","buildGoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        Log.v("locationUpdates", "Gettins Last Location");
        try{
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if(mLastLocation != null){
                mCurrlatLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                mMapFragment.updateMap(mCurrlatLng);
            }
        } catch (SecurityException se){
            mLastLocation = null;
        }
        try{
            Log.v("locationUpdates", "Gettins Current Location");
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(5000); //5 seconds
            mLocationRequest.setFastestInterval(3000); //3 seconds
            mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
            //mLocationRequest.setSmallestDisplacement(0.1F); //1/10 meter

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } catch (SecurityException se){
            mCurrentLocation = null;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this,"onConnectionSuspended",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this,"onConnectionFailed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrlatLng = new LatLng(location.getLatitude(), location.getLongitude());
        Log.v(DEBUG_TAG, mCurrlatLng.toString());
    }

    public boolean askPermissions() {
        Log.v("Main", "AskingPermissions");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.CALL_PHONE};
            ActivityCompat.requestPermissions(this, permissions, 3030);
            Log.v("Main", "InsideIf");
        }
        else return true;
        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        int permissionsNotGiven = 0;
        for(int i=0; i<grantResults.length; i++){
            if(grantResults[i] == -1) permissionsNotGiven++;
        }

        if(permissionsNotGiven == 0){
            try {
                mMapFragment = MapFragment.newInstance();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, mMapFragment)
                        .commitAllowingStateLoss();
                Log.d("Main", "Successfully got location permission. Starting updates.");
            } catch (SecurityException se) {
                mCurrentLocation = null;
            }
        }
        else{
            String[] missedPermissions = new String[permissionsNotGiven];
            int cont = 0;
            for(int i=0; i<grantResults.length; i++){
                if(grantResults[i] == -1){
                    missedPermissions[cont] = permissions[i];
                    cont++;
                }
            }
            ActivityCompat.requestPermissions(this, missedPermissions, 1010);
        }

    }
}
