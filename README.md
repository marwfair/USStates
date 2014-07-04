USStates
========

Originally committed on 2/9/2014 as an Eclipse project. Since then, it was imported into Android Studio and several changes were made. So, the original project was removed, and the new project was committed on 7/4/2014.

Simple Android app that reads in a file in JSON format and stores the data into
a SQLite database. It purposely downloads the largest flag image from 
Wikipedia to demonstrate how to load the extra large bitmaps
efficiently without causing memory errors while keeping the ListView
scrolling smoothly. The app will use different layouts on phones and tables
by making use of fragments. When a state is selected from the selection
menu, details of the state will appear in the details fragment and show
where the state capital is located on a Google map.
The JSON file can be found at http://fairwareapps.com/states.
Download the app from Google Play here: https://play.google.com/store/apps/details?id=com.marwfair.usstates.
Google Play Services must be installed on you Android device for the app to work properly.
