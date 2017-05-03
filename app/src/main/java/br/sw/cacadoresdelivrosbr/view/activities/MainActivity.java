package br.sw.cacadoresdelivrosbr.view.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.facebook.login.LoginManager;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

import br.sw.cacadoresdelivrosbr.R;
import br.sw.cacadoresdelivrosbr.model.Book;
import br.sw.cacadoresdelivrosbr.view.fragments.BookDialog;
import br.sw.cacadoresdelivrosbr.view.fragments.MapFragment;
import br.sw.cacadoresdelivrosbr.view.fragments.MarkerDialog;

public class MainActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private final String DEBUG_TAG = getClass().getSimpleName();
    LocationRequest mLocationRequest = null;
    GoogleApiClient mGoogleApiClient = null;
    Location mCurrentLocation = null;
    Location mLastLocation = null;
    private GeoFire geoFire;
    public ArrayList<Marker> markerList;
    public ArrayList<Book> bookList;
    private DatabaseReference refBooks;
    public LatLng mCurrlatLng = null;
    public Marker markerToDelete;
    MapFragment mMapFragment;
    ProgressDialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setSplashScreen();
        askPermissions();
        bookList = new ArrayList<>();
        markerList = new ArrayList<>();
        mMapFragment = MapFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, mMapFragment)
                .commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseApp.initializeApp(this);
    }

    public void setSplashScreen(){
        dialog = new ProgressDialog(this);
        dialog.show();
        dialog.setContentView(R.layout.splashscreen);
        dialog.setCancelable(false);
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                dialog.dismiss();
            }
        },5000);
    }

    public void showBookDialog(){
        FragmentManager fragmentManager = getSupportFragmentManager();
        DialogFragment bookDialog = new BookDialog();
        bookDialog.show(fragmentManager, "Main");
    }

    public void showMarkerDialog(Marker marker){
        FragmentManager fragmentManager = getSupportFragmentManager();
        DialogFragment markerDialog = new MarkerDialog();
        markerToDelete = marker;
        markerDialog.show(fragmentManager, "Main");
    }

    public void deleteMarker(){
        if(markerToDelete != null){
            markerToDelete.remove();
            markerList.remove(markerToDelete);
        }
    }

    public void mapReady(){
        Log.v("Main", "MapReady");
        buildGoogleApiClient();
        mGoogleApiClient.connect();
        getFirabaseDatabase();
    }

    protected synchronized void getFirabaseDatabase(){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("location");
        geoFire = new GeoFire(ref);
        refBooks = FirebaseDatabase.getInstance().getReference("books");
        refBooks.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                bookList.add(dataSnapshot.getValue(Book.class));
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Book book = dataSnapshot.getValue(Book.class);
                for(int i=0; i<bookList.size(); i++){
                    if(bookList.get(i).bookId.equals(book.bookId)){
                        bookList.add(i, book);
                        break;
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Book book = dataSnapshot.getValue(Book.class);
                for(int i=0; i<bookList.size(); i++){
                    if(bookList.get(i).bookId.equals(book.bookId)){
                        bookList.remove(i);
                        break;
                    }
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    protected synchronized void buildGoogleApiClient() {
        Log.v("Main","buildGoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
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
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates();
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
        if(location == null) return;
        mCurrlatLng = new LatLng(location.getLatitude(), location.getLongitude());
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(location.getLatitude(),
                location.getLongitude()),
                10);
        geoQuery.addGeoQueryEventListener(geoQueryEventListener);
        Log.v(DEBUG_TAG, mCurrlatLng.toString());
    }

    public GeoQueryEventListener geoQueryEventListener = new GeoQueryEventListener(){
        @Override
        public void onKeyEntered(String key, GeoLocation location) {
            boolean repeated = false;
            for(Marker m : markerList){
                if(m.getTitle().equals(key)){
                    repeated = true;
                }
            }
            if(!repeated){
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(new LatLng(location.latitude, location.longitude));
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                markerOptions.title(key);
                mMapFragment.markBook(markerOptions);
                if(!markerList.isEmpty()) markerList.get(markerList.size()-1).hideInfoWindow();
            }
        }

        @Override
        public void onKeyExited(String key) {
            String bookKey = refBooks.child(key).toString();
            for(Marker marker : markerList){
                if(marker.getTitle().equals(bookKey)){
                    markerToDelete = marker;
                    deleteMarker();
                }
            }
        }

        @Override
        public void onKeyMoved(String key, GeoLocation location) {

        }

        @Override
        public void onGeoQueryReady() {

        }

        @Override
        public void onGeoQueryError(DatabaseError error) {

        }
    };

    public void askPermissions() {
        Log.v("Main", "AskingPermissions");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(this, permissions, 3030);
            Log.v("Main", "InsideIf");
        }
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

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Sair")
                .setMessage("Deseja sair da aplicação?")
                .setPositiveButton("Sair", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FirebaseAuth.getInstance().signOut();
                        LoginManager.getInstance().logOut();
                        Intent i = new Intent(getApplication(), LoginActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(i);
                    }
                })
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).create().show();
    }
}
