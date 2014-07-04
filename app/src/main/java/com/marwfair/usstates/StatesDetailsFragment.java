package com.marwfair.usstates;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.marwfair.usstates.StateContract.StateEntry;

public class StatesDetailsFragment extends Fragment {
	public static int currentPosition = -1;
	final static String ARG_POSITION = "position";

	private TextView beginTv;
	private TextView nameTv;
	private TextView capitalTv;
	private TextView abbreviationTv;
	private TextView populatedCityTv;
	private TextView populationTv;
	private TextView squareMilesTv;
	private TextView timeZone1Tv;
	private TextView timeZone2Tv;
	private TextView dstTv;
	
	private StatesDbHelper statesDbHelper;
	private SQLiteDatabase statesDb;
	private GoogleMap map;
	private SupportMapFragment mapFragment;
	
	private Typeface lightFont;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		setRetainInstance(true);
		
		ViewGroup layout = (ViewGroup)inflater.inflate(R.layout.states_details_layout, null);
		
		// Assign the fonts.
		lightFont = Typeface.createFromAsset(getActivity().getAssets(), "Roboto-Light.ttf");
		
		beginTv = (TextView)layout.findViewById(R.id.begin);
		nameTv = (TextView)layout.findViewById(R.id.name);
		capitalTv = (TextView)layout.findViewById(R.id.capital);
		abbreviationTv = (TextView)layout.findViewById(R.id.abbreviation);
		populatedCityTv = (TextView)layout.findViewById(R.id.populatedCity);
		populationTv = (TextView)layout.findViewById(R.id.population);
		squareMilesTv = (TextView)layout.findViewById(R.id.squareMiles);
		timeZone1Tv = (TextView)layout.findViewById(R.id.timeZone1);
		timeZone2Tv = (TextView)layout.findViewById(R.id.timeZone2);
		dstTv = (TextView)layout.findViewById(R.id.dst);
		
		
		// Add the map if Google Play Services are available.
    	if (isGooglePlayServicesAvailable()) {
	    	FragmentManager fragmentManager = getFragmentManager();
			mapFragment = (SupportMapFragment)fragmentManager.findFragmentById(R.id.mapview);
			map = mapFragment.getMap();
			// Hide the zoom buttons
			map.getUiSettings().setZoomControlsEnabled(false);
    	}
    	
    	statesDbHelper = new StatesDbHelper(getActivity());
		statesDb = statesDbHelper.getReadableDatabase();
    	
		beginTv.setTypeface(lightFont);
		capitalTv.setTypeface(lightFont);
    	abbreviationTv.setTypeface(lightFont);
    	populatedCityTv.setTypeface(lightFont);
    	populationTv.setTypeface(lightFont);
    	squareMilesTv.setTypeface(lightFont);
    	timeZone1Tv.setTypeface(lightFont);
    	timeZone2Tv.setTypeface(lightFont);
    	dstTv.setTypeface(lightFont);
    	
    	if (savedInstanceState != null) {
        	currentPosition = savedInstanceState.getInt(ARG_POSITION);
        }
		
		return layout;
	}
	
	public void updateStateDetails(int position) {
		// Get the record from the database that is selected
    	final Cursor cursor = statesDb.rawQuery("SELECT * FROM " + StateEntry.TABLE_NAME + " WHERE " + StateEntry._ID + " = \"" + position + "\"", null);
		cursor.moveToFirst();
    	
    	String name = cursor.getString(cursor.getColumnIndexOrThrow(StateEntry.COLUMN_NAME_NAME));
    	String capital = cursor.getString(cursor.getColumnIndexOrThrow(StateEntry.COLUMN_NAME_CAPITAL));
    	String abbreviation = cursor.getString(cursor.getColumnIndexOrThrow(StateEntry.COLUMN_NAME_ABBREVIATION));
    	String populatedCity = cursor.getString(cursor.getColumnIndexOrThrow(StateEntry.COLUMN_NAME_POP_CITY));
    	String population = cursor.getString(cursor.getColumnIndexOrThrow(StateEntry.COLUMN_NAME_POPULATION));
    	String squareMiles = cursor.getString(cursor.getColumnIndexOrThrow(StateEntry.COLUMN_NAME_SQUARE_MILES));
    	String timeZone1 = cursor.getString(cursor.getColumnIndexOrThrow(StateEntry.COLUMN_NAME_TIME_ZONE_1));
    	String timeZone2 = cursor.getString(cursor.getColumnIndexOrThrow(StateEntry.COLUMN_NAME_TIME_ZONE_2));
    	String dst = cursor.getString(cursor.getColumnIndexOrThrow(StateEntry.COLUMN_NAME_DST));
    	
    	beginTv.setVisibility(View.GONE);
    	
    	nameTv.setText(name);
    	capitalTv.setText(capital);
    	abbreviationTv.setText(abbreviation);
    	populatedCityTv.setText(populatedCity);
    	populationTv.setText(population);
    	squareMilesTv.setText(squareMiles);
    	timeZone1Tv.setText(timeZone1);
    	if (timeZone2 == null)
    		timeZone2Tv.setText("NA");
    	else
    		timeZone2Tv.setText(timeZone2);
    	dstTv.setText(dst);
    	
    	
    	double latitude = Double.parseDouble(cursor.getString(cursor.getColumnIndexOrThrow(StateEntry.COLUMN_NAME_LATITUDE)));
	    double longitude = Double.parseDouble(cursor.getString(cursor.getColumnIndexOrThrow(StateEntry.COLUMN_NAME_LONGITUDE)));
	    final LatLng latlng= new LatLng(latitude, longitude);
        
	    // Show location on map if Google Play Services are available.
    	if (isGooglePlayServicesAvailable()) {
    		
    		// Remove any marker that may be on the map.
        	map.clear();
		    
		    // Set animation to location
		    CameraPosition cameraPosition =
		      new CameraPosition.Builder()
		        .target(latlng)
		        .bearing(0)
		        .zoom(4)
		        .build();
		    
		    
		    // Check if we have a two-pane layout.
		    // If we don't, there is no need to animate the camera.
		    StatesSelectionFragment statesSelectionFragment = (StatesSelectionFragment)
	    			getActivity().getSupportFragmentManager().findFragmentById(R.id.states_list);
		    
	        if (statesSelectionFragment != null) {
	        	map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 2000, null);
	        } else {
	        	map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
	        }
		    
		    // Add marker
		    map.addMarker(new MarkerOptions()
		    .position(latlng)
		    .title(capital));
    	}
    	
    	currentPosition = position;
	}
	
	@Override
    public void onStart() {
        super.onStart();

        // During startup, check if there are arguments passed to the fragment.
        // onStart is a good place to do this because the layout has already been
        // applied to the fragment at this point so we can safely call the method
        // below that sets the details text.
        Bundle args = getArguments();
        if (args != null) {
            
            updateStateDetails(args.getInt(ARG_POSITION));
        } else if (currentPosition != -1) {
        	
            // Set state based on saved instance state defined during onCreateView
            updateStateDetails(currentPosition);
        }
    }

	/**
	* Checks if Google Play Services is available.
	* @return true if available
	*/
	public boolean isGooglePlayServicesAvailable() {
		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());
		if (status == ConnectionResult.SUCCESS) {
			return true;
		} else {
			return false;
		}
	}
	    
	public void onDestroyView() {
		super.onDestroyView(); 
			
		try {
			if (getFragmentManager().findFragmentById(R.id.mapview) != null) {
			    Fragment fragment = (getFragmentManager().findFragmentById(R.id.mapview));  
			    FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
			    ft.remove(fragment);
			    ft.commit();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
	    super.onSaveInstanceState(outState);

	    // Save selected state
	    outState.putInt(ARG_POSITION, currentPosition);
	}
}