package com.wikitude.samples;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.content.Intent;
import android.opengl.GLES20;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.wikitude.architect.ArchitectView;
import com.wikitude.sdksamples.R;


/**
 * Activity launched when pressing app-icon.
 * It uses very basic ListAdapter for UI representation
 */
public class MainActivity extends ListActivity{
	
	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		this.setContentView( this.getContentViewId() );
		
		// ensure to clean cache when it is no longer required
		MainActivity.deleteDirectoryContent ( ArchitectView.getCacheDirectoryAbsoluteFilePath(this) );

		// extract names of samples from res/arrays
		final String[] values = this.getListLabels();

		// use default list-ArrayAdapter */
		this.setListAdapter( new ArrayAdapter<String>( this, android.R.layout.simple_list_item_1, android.R.id.text1, values ) );
	}

	@Override
	protected void onListItemClick( ListView l, View v, int position, long id ) {
		super.onListItemClick( l, v, position, id );
			
			final Intent intent = new Intent( this, MainSamplesListActivity.class );

			final List<SampleMeta> activitiesToLaunch = getActivitiesToLaunch(position);
			final String activityTitle = activitiesToLaunch.get(0).categoryId + ". " + activitiesToLaunch.get(0).categoryName.replace("$", " ");
			String[] activityTitles = new String[activitiesToLaunch.size()];
			String[] activityUrls = new String[activitiesToLaunch.size()];
			String[] activityClasses = new String[activitiesToLaunch.size()];
			
			// check if AR.VideoDrawables are supported on the current device. if not -> show hint-Toast message
			if (activitiesToLaunch.get(0).categoryId == 6 && ! MainActivity.isVideoDrawablesSupported()) {
				Toast.makeText(this, R.string.videosrawables_fallback, Toast.LENGTH_LONG).show();
			}
			
			// find out which Activity to launch when sample row was pressed, some handle document.location = architectsdk:// events, others inject poi data from native via javascript
			for (int i= 0; i< activitiesToLaunch.size(); i++) {
				final SampleMeta meta = activitiesToLaunch.get(i);
				activityTitles[i] = (meta.categoryId + "." + meta.sampleId + " " + meta.sampleName.replace("$", " "));
				activityUrls[i] =  (meta.path);
				if (meta.categoryId == 4 && meta.sampleId==1) {
					activityClasses[i] = ("com.wikitude.samples.SampleCamContentFromNativeActivity");
				} else if (meta.categoryId == 5 && meta.sampleId==5) {
					activityClasses[i] = ("com.wikitude.samples.SampleCamHandlePoiDetailActivity");
				} else if (meta.categoryId == 5 && meta.sampleId==6) {
					activityClasses[i] = ("com.wikitude.samples.SampleCamCaptureScreenActivity");
				} else {
					activityClasses[i] = ("com.wikitude.samples.SampleCamActivity");
				}
			}
			
			intent.putExtra(MainSamplesListActivity.EXTRAS_KEY_ACTIVITIES_ARCHITECT_WORLD_URLS_ARRAY, activityUrls);
			intent.putExtra(MainSamplesListActivity.EXTRAS_KEY_ACTIVITIES_CLASSNAMES_ARRAY, activityClasses);
			intent.putExtra(MainSamplesListActivity.EXTRAS_KEY_ACTIVITIES_TILES_ARRAY, activityTitles);
			intent.putExtra(MainSamplesListActivity.EXTRAS_KEY_ACTIVITY_TITLE_STRING, activityTitle);
			
			/* launch activity */
			this.startActivity( intent );
			
	}

	protected final String[] getListLabels() {
		
		final Map<Integer, List<SampleMeta>> samples = getActivitiesToLaunch();
		final String[] labels = new String[samples.keySet().size()];
		for (int i = 0; i<labels.length; i++) {
			labels[i] = samples.get(i).get(0).categoryId + ". " + samples.get(i).get(0).categoryName.replace("$", " ");
		}
		return labels;
	}
	
	protected int getContentViewId() {
		return R.layout.list_startscreen;
	}
	
	public void buttonClicked(final View view)
	 {
		try {
			this.startActivity( new Intent( this, Class.forName( "com.wikitude.samples.utils.urllauncher.ARchitectUrlLauncherActivity" ) ) );
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	 }
	
	/**
	 * deletes content of given directory
	 * @param path
	 */
	private static void deleteDirectoryContent(final String path) {
		try {
			final File dir = new File (path);
			if (dir.exists() && dir.isDirectory()) {
				final String[] children = dir.list();
		        for (int i = 0; i < children.length; i++) {
		            new File(dir, children[i]).delete();
		        }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private List<SampleMeta> getActivitiesToLaunch(final int position){
		return getActivitiesToLaunch().get(position);
	}
	
	private Map<Integer, List<SampleMeta>> getActivitiesToLaunch(){
		final Map<Integer, List<SampleMeta>> pos2activites = new HashMap<Integer, List<SampleMeta>>();

		String[] assetsIWant;
		try {
			assetsIWant = getAssets().list("samples");
			int pos = -1;
			int lastCategoryId = -1;
			for(final String asset: assetsIWant) {
				try {
					SampleMeta sampleMeta = new SampleMeta(asset);
					if (sampleMeta.categoryId!=lastCategoryId) {
						pos++;
						pos2activites.put(pos, new ArrayList<SampleMeta>());
					} 
					pos2activites.get(pos).add(sampleMeta);
					lastCategoryId = sampleMeta.categoryId;
				} catch (IllegalArgumentException e) {
					// Log.e("Ignored Asset to load", asset + " invalid: "+ e.getMessage());
				}
		}
		
		return pos2activites;
			
		
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private static class SampleMeta {
		
		final String path, categoryName, sampleName;
		final int categoryId, sampleId;
		
		public SampleMeta(String path) {
			super();
			this.path = path;
			if (path.indexOf("_")<0) {
				throw new IllegalArgumentException("all files in asset folder must be folders and define category and subcategory as predefined (with underscore)");
			}
			this.categoryId = Integer.valueOf(path.substring(0, path.indexOf("_")));
			path = path.substring(path.indexOf("_")+1);
			this.categoryName = path.substring(0, path.indexOf("_"));
			path = path.substring(path.indexOf("_")+1);
			this.sampleId = Integer.valueOf(path.substring(0, path.indexOf("_")));
			path = path.substring(path.indexOf("_")+1);
			this.sampleName = path;
		}
		
		@Override
		public String toString() {
			return "categoryId:" + this.categoryId + ", categoryName:" + this.categoryName + ", sampleId:" + this.sampleId +", sampleName: " + this.sampleName + ", path: " + this.path;
		}
	}
	
	/**
	 * helper to check if video-drawables are supported by this device. recommended to check before launching ARchitect Worlds with videodrawables
	 * @return true if AR.VideoDrawables are supported, false if fallback rendering would apply (= show video fullscreen)
	 */
	public static final boolean isVideoDrawablesSupported() {
		String extensions = GLES20.glGetString( GLES20.GL_EXTENSIONS );
		return extensions != null && extensions.contains( "GL_OES_EGL_image_external" ) && android.os.Build.VERSION.SDK_INT >= 14 ;
	}
	

}
