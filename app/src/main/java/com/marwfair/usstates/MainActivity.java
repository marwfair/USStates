package com.marwfair.usstates;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

/*
 * Simple app that reads in a file in JSON format, and stores the data into
 * a SQLite database. It purposely downloads the largest flag image from 
 * Wikipedia to demonstrates how to load the extra large bitmaps
 * efficiently without causing memory errors and keeping the ListView
 * scrolling smoothly. The app will use different layouts on phones and tables
 * by making use of fragments. When a state is selected from the selection
 * menu, details of the state will appear in the details fragment and show
 * where the state capital is located on a Google map.
 * 
 * Created by Mark Fairless.
 */

public class MainActivity extends FragmentActivity implements StatesSelectionFragment.OnStateSelectedListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.states_list);
		
		// Check if we are in a one or two pane layout
		if (findViewById(R.id.fragment_container) != null) {
			// We are in a one pane layout
			
			if (savedInstanceState != null) {
				return;
			}
			
			// Create instance of StatesSelectionFragment
			StatesSelectionFragment statesSelectionFragment = new StatesSelectionFragment();
			
			statesSelectionFragment.setArguments(getIntent().getExtras());
			
			// Add the fragment to the fragment_container FrameLayout
			getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, statesSelectionFragment).commit();
		}
	}

	@Override
	public void onStateSelected(int position) {
		StatesDetailsFragment statesDetailsFragment = (StatesDetailsFragment)this.getSupportFragmentManager().findFragmentById(R.id.states_details);
		
		// Check if we are in a two pane layout
		if (statesDetailsFragment != null) {
			statesDetailsFragment.updateStateDetails(position);
		} else {
			StatesDetailsFragment newStatesDetailsFragment = new StatesDetailsFragment();
			
			Bundle args = new Bundle();
            args.putInt(StatesDetailsFragment.ARG_POSITION, position);
            newStatesDetailsFragment.setArguments(args);
			
			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
			transaction.replace(R.id.fragment_container, newStatesDetailsFragment);
			transaction.addToBackStack(null);
			transaction.commit();
		}
	}
}