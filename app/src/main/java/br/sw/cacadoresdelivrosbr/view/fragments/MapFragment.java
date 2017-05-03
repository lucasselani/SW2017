package br.sw.cacadoresdelivrosbr.view.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.lapism.searchview.SearchView;

import br.sw.cacadoresdelivrosbr.R;
import br.sw.cacadoresdelivrosbr.view.activities.MainActivity;

/**
 * Created by lucasselani on 28/04/17.
 */

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = MapFragment.class.getSimpleName();
    private GoogleMap mGoogleMap;
    private SearchView searchView;

    public static MapFragment newInstance() {
        return new MapFragment();
    }

    public MapFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.map_fragment, container, false);
        MapView mMapView = (MapView) rootView.findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume(); // needed to get the map to display immediately
        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
        mMapView.getMapAsync(this);

        FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).showBookDialog();
            }
        });

        //searchView = (SearchView) rootView.findViewById(R.id.searchView);
        //searchView.setVersion(SearchView.VERSION_TOOLBAR);

        return rootView;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        try{
            googleMap.setMyLocationEnabled(true);
        } catch (SecurityException se){
            se.printStackTrace();
        }
        /*try {
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            getContext(), R.raw.style_json));

            if (!success) Log.e(TAG, "Style parsing failed.");
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }*/
        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Log.v("MapFragment", "OnMarkerClick");
                if(marker.isInfoWindowShown()) marker.hideInfoWindow();
                ((MainActivity)getActivity()).showMarkerDialog(marker);
                return true;
            }
        });
        googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                if(marker.isInfoWindowShown()) marker.hideInfoWindow();
            }
        });
        mGoogleMap = googleMap;
        Log.v(TAG,"MapReady");
        ((MainActivity) getActivity()).mapReady();
    }

    public void updateMap(LatLng latlgn){
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latlgn).zoom(14).build();
        mGoogleMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(cameraPosition));
    }

    public void markBook(MarkerOptions markerOptions){
        ((MainActivity)getActivity()).markerList.add(mGoogleMap.addMarker(markerOptions));
    }
}
