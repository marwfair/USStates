package com.marwfair.usstates;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.util.LruCache;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.marwfair.usstates.StateContract.StateEntry;

public class StatesSelectionFragment extends Fragment {
	public OnStateSelectedListener callBack;
	
	public interface OnStateSelectedListener {
		public void onStateSelected(int position);
	}
	
	public static final String STATES_URL = "http://fairwareapps.com/states";
	private ListView listView;
	private StatesDbHelper statesDbHelper;
	private SQLiteDatabase statesDb;
	private Typeface boldFont;
	private SharedPreferences sharedPref;
	private LruCache<String, Bitmap> memoryCache;
	private Resources res;
	private File cachePath;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		setRetainInstance(true);
		
		final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

	    // Use 1/8th of the available memory for this memory cache.
	    final int cacheSize = maxMemory / 8;
	    
    	memoryCache = new LruCache<String, Bitmap>(cacheSize) {
	        @Override
	        protected int sizeOf(String key, Bitmap bitmap) {
	            // The cache size will be measured in kilobytes rather than number or items.
	        	return (bitmap.getRowBytes() * bitmap.getHeight()) / 1024;
	        }
	    };
	    
	    /*
	     * Check if we are going to be using the external cache directory or not. 
	     */
	    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
	    	cachePath = getActivity().getExternalCacheDir();
	    } else {
	    	cachePath = getActivity().getCacheDir();
	    }
	    
	    // Initialize sharedPref, which holds the lastModified value.
	    sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
		
	    res = getResources();
		statesDbHelper = new StatesDbHelper(getActivity());
		statesDb = statesDbHelper.getWritableDatabase();
		
		// Assign the fonts.
		boldFont = Typeface.createFromAsset(getActivity().getAssets(), "Roboto-Bold.ttf");
		
		ViewGroup layout = (ViewGroup)inflater.inflate(R.layout.states_selection_layout, null);
		listView = (ListView)layout.findViewById(R.id.listView);
		
		// Get the record count.
		Cursor cursor = statesDb.rawQuery("SELECT COUNT(*) FROM " + StateEntry.TABLE_NAME, null);
		cursor.moveToFirst();

		// If user has a data connection, check for updates
		// Else, show no connection dialog and retry
		if (isOnline()) {
			new GetStateInfoTask().execute();
		} else if (cursor.getInt(0) == 0 && !isOnline()) {
			noData();
		} else {
			populateList();
		}
		
		return layout;
	}
		
	public Boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		if (ni != null && ni.isConnected())
			return true;
		
		return false;
	}
	
	private void noData() {
		new AlertDialog.Builder(getActivity())
		.setTitle(res.getString(R.string.uh_oh))
		.setMessage(res.getString(R.string.not_connected))
		.setCancelable(false)
		.setPositiveButton(res.getString(R.string.retry), new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (isOnline()) {
					new GetStateInfoTask().execute();
				} else {
					noData();
				}
			}
		})
		.show();
	}
	
	private void errorDownloading() {
		new AlertDialog.Builder(getActivity())
		.setTitle(res.getString(R.string.uh_oh))
		.setMessage(res.getString(R.string.error_downloading))
		.setCancelable(false)
		.setPositiveButton(res.getString(R.string.retry), new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (isOnline()) {
					new GetStateInfoTask().execute();
				} else {
					noData();
				}
			}
		})
		.show();
	}
	
	/**
	 * Populates the ListView.
	 */
	private void populateList() {
		Cursor cursor = statesDb.query(StateEntry.TABLE_NAME, null, null, null, null, null, null);
		StatesAdapter statesAdapter = new StatesAdapter(getActivity(), cursor, 0);
		listView.setAdapter(statesAdapter);
	}

    static class ViewHolder {
        ImageView flagIv;
        TextView nameTv;
    }

	private class StatesAdapter extends CursorAdapter {

		public StatesAdapter(Context context, Cursor cursor, int flags) {
			super(context, cursor, flags);
		}

		@Override
        public View newView (Context context, Cursor cursor, ViewGroup parent) {
			ViewHolder viewHolder = new ViewHolder();
			View rowView = ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.state_row_layout, parent, false);
			
			viewHolder.flagIv = (ImageView)rowView.findViewById(R.id.flag_image);
			viewHolder.nameTv = (TextView)rowView.findViewById(R.id.state_name);
			rowView.setTag(viewHolder);
			
			return rowView;
		}

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
        	ViewHolder viewHolder = (ViewHolder)view.getTag();
        	
			viewHolder.nameTv.setTypeface(boldFont);
			
			String url = cursor.getString(cursor.getColumnIndexOrThrow(StateEntry.COLUMN_NAME_FLAG_URL));
			
			/*
			 * Check if bitmap is stored in memory cache. If it is, go ahead and show it.
			 * If not, load it. This will load it from disk cache if it exists, or
			 * download it if it isn't there.
			 */
			final Bitmap bitmap = getBitmapFromMemoryCache(String.valueOf(url.hashCode()));
		    if (bitmap != null) {
		        viewHolder.flagIv.setImageBitmap(bitmap);
		    } else {
		    	loadBitmap(url, viewHolder.flagIv);
		    }

		    viewHolder.nameTv.setText(cursor.getString(cursor.getColumnIndexOrThrow(StateEntry.COLUMN_NAME_NAME)));
	
			listView.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> adapter, View arg1, final int position,
						long id) {
					// Notify the parent activity of selected item, which is the row id of the db
			        callBack.onStateSelected((int)id);
				}
			});
        }
	}
	
	class GetStateInfoTask extends AsyncTask<Void, Void, Boolean> {
		
		ProgressDialog progressDialog;
		ProgressBar progressBar;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			progressDialog = new ProgressDialog(getActivity());
			progressBar = new ProgressBar(getActivity());
			
			progressDialog.show();
			progressDialog.setContentView(progressBar);
			progressDialog.setCancelable(false);
		}

		@Override
		protected Boolean doInBackground(Void... v) {
			boolean successful = true;
			int responseCode;
			
			/*
			 * Use HttpURLConnection instead of DefaultHttpClient
			 */
			HttpURLConnection urlConnection = null;
			try {
				URL url = new URL(STATES_URL);
				urlConnection = (HttpURLConnection)url.openConnection();
				
				String lastModified = sharedPref.getString("lastModified", "0");
				urlConnection.setRequestProperty("If-Modified-Since", lastModified);
				
				urlConnection.connect();
				responseCode = urlConnection.getResponseCode();
				
				switch(responseCode) {
				case 200: // Successful

					// Empty the table
					statesDb.delete(StateEntry.TABLE_NAME, null, null);
					
					// Example of what will be returned: Wed, 29 Jan 2014 23:40:10 GMT
				    lastModified = urlConnection.getHeaderField("Last-Modified");
				    
				    // Save lastModified in SharedPreferences.
				    SharedPreferences.Editor editor = sharedPref.edit();
				    editor.putString("lastModified", lastModified);
				    editor.commit();
					
					BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = br.readLine()) != null) {
						sb.append(line);
					}
					br.close();
					
					JSONObject object = new JSONObject(sb.toString());
					JSONArray statesArray = new JSONArray(object.getJSONArray("states").toString());
					
					for (int i = 0; i < statesArray.length(); i++) {
						JSONObject statesObject = new JSONObject(statesArray.getJSONObject(i).toString());
						
						ContentValues values = new ContentValues();
						values.put(StateEntry.COLUMN_NAME_NAME, statesObject.getString("name"));
						values.put(StateEntry.COLUMN_NAME_ABBREVIATION, statesObject.getString("abbreviation"));
						values.put(StateEntry.COLUMN_NAME_CAPITAL, statesObject.getString("capital"));
						values.put(StateEntry.COLUMN_NAME_POP_CITY, statesObject.getString("most_populous_city"));
						values.put(StateEntry.COLUMN_NAME_POPULATION, statesObject.getString("population"));
						values.put(StateEntry.COLUMN_NAME_SQUARE_MILES, statesObject.getString("square_miles"));
						values.put(StateEntry.COLUMN_NAME_TIME_ZONE_1, statesObject.getString("time_zone_1"));
						// time_zone_2 can be null
						if (!statesObject.isNull("time_zone_2"))
							values.put(StateEntry.COLUMN_NAME_TIME_ZONE_2, statesObject.getString("time_zone_2"));
						values.put(StateEntry.COLUMN_NAME_DST, statesObject.getString("dst"));
						values.put(StateEntry.COLUMN_NAME_FLAG_URL, statesObject.getString("flag_url"));
						values.put(StateEntry.COLUMN_NAME_LATITUDE, statesObject.getString("latitude"));
						values.put(StateEntry.COLUMN_NAME_LONGITUDE, statesObject.getString("longitude"));
						
						// Store the data into the database.
						statesDb.insert(StateEntry.TABLE_NAME, null, values);
					}
					
					break;
				case 304: // No changes
					break;
				default:
				}
				
			} catch(Exception e) {
				e.printStackTrace();
			} finally {
				urlConnection.disconnect();
			}

			return successful;
		}
		
		@Override
		protected void onPostExecute(Boolean successful) {
			progressDialog.dismiss();
			
			if (successful) {
				populateList();
			} else {
				errorDownloading();
			}
		}
	}
	
	/**
	 * Download and cache the flag bitmap.
	 * @param flagUrl
	 * @return logoBitmap
	 */
	private Bitmap downloadFlag(String flagUrl) {
		Bitmap flagBitmap = null;
		
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = 4;
		
		try {
			String fileName = String.valueOf(flagUrl.hashCode());
			File imageFile = new File(cachePath, fileName);
				
		    InputStream in = new java.net.URL(flagUrl).openStream();
		    flagBitmap = BitmapFactory.decodeStream(in, null, options);
		        
		    FileOutputStream out = null;
			try {
				out = new FileOutputStream(imageFile);
				flagBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					out.flush();
					out.close();
				} catch (Exception ex) {
				}
			}
			in.close();
			        
	      } catch (Exception e) {
	          e.printStackTrace();
	      }
		//addBitmapToMemoryCache(flagUrl, flagBitmap);
		return flagBitmap;
	}
	
	/**
	 * Adds bitmap to memory cache.
	 * @param key This is the URL of the image.
	 * @param bitmap Bitmap of flag.
	 */
	public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
		key = String.valueOf(key.hashCode());
	    if (getBitmapFromMemoryCache(key) == null) {
	        memoryCache.put(key, bitmap);
	    }
	}

	/**
	 * Gets the bitmap from memory cache.
	 * @param key This is the URL of the image.
	 * @return The bitmap of the flag.
	 */
	public Bitmap getBitmapFromMemoryCache(String key) {
        return memoryCache.get(key);
	}
	
	public Bitmap getBitmapFromDiskCache(String key) {
		Bitmap flagBitmap = null;
		String fileName = String.valueOf(key.hashCode());
		// Check if images exists on disk
		File imageFile = new File(cachePath, fileName);
		if (imageFile != null) {
			flagBitmap = BitmapFactory.decodeFile(imageFile.getPath());
		}
		
		return flagBitmap;
	}
	
	public void loadBitmap(String resId, ImageView imageView) {
		
		if (cancelPotentialWork(resId, imageView)) {
	        final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
	        final AsyncDrawable asyncDrawable =
	                new AsyncDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher), task);
	        imageView.setImageDrawable(asyncDrawable);

            // Starting with Honeycomb, use executeOnExecutor to run the tasks in parallel.
            if (Build.VERSION.SDK_INT >= 11) {
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, resId);
            } else {
                task.execute(resId);
            }
	    }
	}
	
	public static boolean cancelPotentialWork(String data, ImageView imageView) {
	    final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

	    if (bitmapWorkerTask != null) {
	        final int bitmapData = bitmapWorkerTask.data;
	        // If bitmapData is not yet set or it differs from the new data
	        Log.d("bitmap data", bitmapData + " " + data.hashCode());
	        if (bitmapData == 0 || bitmapData != data.hashCode()) {
	            // Cancel previous task
	            bitmapWorkerTask.cancel(true);
	        } else {
	            // The same work is already in progress
	            return false;
	        }
	    }
	    // No task associated with the ImageView, or an existing task was cancelled
	    return true;
	}
	
	private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
		
		if (imageView != null) {
			final Drawable drawable = imageView.getDrawable();
		    if (drawable instanceof AsyncDrawable) {
		    	final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
		        return asyncDrawable.getBitmapWorkerTask();
		    }
		}
		return null;
	}
	
	static class AsyncDrawable extends BitmapDrawable {
	    private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

	    public AsyncDrawable(Resources res, Bitmap bitmap,
	            BitmapWorkerTask bitmapWorkerTask) {
	        super(res, bitmap);
	        bitmapWorkerTaskReference =
	            new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
	    }

	    public BitmapWorkerTask getBitmapWorkerTask() {
	        return bitmapWorkerTaskReference.get();
	    }
	}
	
	class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
	    private final WeakReference<ImageView> imageViewReference;
	    private int data = 0;

	    public BitmapWorkerTask(ImageView imageView) {
	        // Use a WeakReference to ensure the ImageView can be garbage collected
	        imageViewReference = new WeakReference<ImageView>(imageView);
	    }

	    // Decode image in background.
	    @Override
	    protected Bitmap doInBackground(String... params) {
	    	Bitmap bitmap = getBitmapFromDiskCache(params[0]);
	    	if (bitmap == null) {
	    		if (isOnline())
	    			bitmap = downloadFlag(params[0]);
	    	}
	    	
	    	if (bitmap != null)
	    		addBitmapToMemoryCache(params[0], bitmap);
			
	        return bitmap;
	    }

	    // Once complete, see if ImageView is still around and set bitmap.
	    @Override
	    protected void onPostExecute(Bitmap bitmap) {
	    	if (isCancelled()) {
	            bitmap = null;
	        }

	        if (imageViewReference != null && bitmap != null) {
	            final ImageView imageView = imageViewReference.get();
	            final BitmapWorkerTask bitmapWorkerTask =
	                    getBitmapWorkerTask(imageView);
	            if (this == bitmapWorkerTask && imageView != null) {
	                imageView.setImageBitmap(bitmap);
	            }
	        }
	    }
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        try {
            callBack = (OnStateSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnStatesSelectedListener");
        }
    }
}