package com.wikitude.samples;

import java.io.IOException;

import android.location.Location;
import android.location.LocationListener;
import android.opengl.GLES20;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wikitude.architect.ArchitectView;
import com.wikitude.architect.ArchitectView.ArchitectConfig;
import com.wikitude.architect.ArchitectView.ArchitectUrlListener;
import com.wikitude.architect.ArchitectView.SensorAccuracyChangeListener;


	public abstract class AbstractArchitectCamFragmentV4 extends Fragment implements ArchitectViewHolderInterface{

		/**
		 * holds the Wikitude SDK AR-View, this is where camera, markers, compass, 3D models etc. are rendered
		 */
		private ArchitectView					architectView;
		
		/**
		 * sensor accuracy listener in case you want to display calibration hints
		 */
		protected SensorAccuracyChangeListener	sensorAccuracyListener;
		
		/**
		 * last known location of the user, used internally for content-loading after user location was fetched
		 */
		protected Location 						lastKnownLocaton;

		/**
		 * sample location strategy
		 */
		protected ILocationProvider				locationProvider;
		
		/**
		 * location listener receives location updates and must forward them to the architectView
		 */
		protected LocationListener 				locationListener;
		
		/**
		 * urlListener handling "document.location= 'architectsdk://...' " calls in JavaScript"
		 */
		protected ArchitectUrlListener 			urlListener;

		@Override
		public View onCreateView( final LayoutInflater inflater, final ViewGroup container, final Bundle bundle ) {
			return inflater.inflate( this.getContentViewId(), container, false );
		}

		@Override
		public void onActivityCreated( final Bundle bundle ) {
			super.onActivityCreated( bundle );

			// set architectView, important for upcoming lifecycle calls
			this.architectView = (ArchitectView)this.getView().findViewById( getArchitectViewId() );
			
			// pass license key to architectView while creating it
			final ArchitectConfig config = new ArchitectConfig( this.getWikitudeSDKLicenseKey() );
			
			// forwards mandatory life-cycle-events, unfortunately there is no onPostCreate() event in fragments so we have to call it that way
			this.architectView.onCreate( config );
			this.architectView.onPostCreate();
			
			try {
				
				// load architectView's content
				this.architectView.load( this.getARchitectWorldPath() );
				
				if (this.getInitialCullingDistanceMeters() != ArchitectViewHolderInterface.CULLING_DISTANCE_DEFAULT_METERS) {
					// set the culling distance - meaning: the maximum distance to render geo-content
					this.architectView.setCullingDistance( this.getInitialCullingDistanceMeters() );
				}
				
			} catch (IOException e) {
				// unexpected, if error occurs here your path is invalid
				e.printStackTrace();
			}
			
			// listener passed over to locationProvider, any location update is handled here
			this.locationListener = new LocationListener() {

				@Override
				public void onStatusChanged( String provider, int status, Bundle extras ) {
				}

				@Override
				public void onProviderEnabled( String provider ) {
				}

				@Override
				public void onProviderDisabled( String provider ) {
				}

				@Override
				public void onLocationChanged( final Location location ) {
					if (location!=null) {
						AbstractArchitectCamFragmentV4.this.lastKnownLocaton = location;
					if ( AbstractArchitectCamFragmentV4.this.architectView != null ) {
						// check if location has altitude at certain accuracy level & call right architect method (the one with altitude information)
						if ( location.hasAltitude() && location.hasAccuracy() && location.getAccuracy()<7) {
							AbstractArchitectCamFragmentV4.this.architectView.setLocation( location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getAccuracy() );
						} else {
							AbstractArchitectCamFragmentV4.this.architectView.setLocation( location.getLatitude(), location.getLongitude(), location.hasAccuracy() ? location.getAccuracy() : 1000 );
						}
					}
					}
				}
			};
			
			// set accuracy listener if implemented, you may e.g. show calibration prompt for compass using this listener
			this.sensorAccuracyListener = this.getSensorAccuracyListener();
			
			// set urlListener, any calls made in JS like "document.location = 'architectsdk://foo?bar=123'" is forwarded to this listener, use this to interact between JS and native Android activity/fragment
			this.urlListener = this.getUrlListener();  
			
			// register valid urlListener in architectView, ensure this is set before content is loaded to not miss any event
			if ( this.urlListener !=null ) {
				this.architectView.registerUrlListener( this.getUrlListener() );
			}

			// locationProvider used to fetch user position
			this.locationProvider = this.getLocationProvider(this.locationListener);

		}

		@Override
		public void onResume() {
			super.onResume();
			if ( this.architectView != null ) {
				
				// call mandatory live-cycle method of architectView
				this.architectView.onResume();
				
				// register accuracy listener in architectView, if set
				if (this.sensorAccuracyListener!=null) {
					this.architectView.registerSensorAccuracyChangeListener( this.sensorAccuracyListener );
				}
			}	

			// tell locationProvider to resume, usually location is then (again) fetched, so the GPS indicator appears in status bar
			if ( this.locationProvider != null ) {
				this.locationProvider.onResume();
			}
		}

		@Override
		public void onPause() {
			super.onPause();

			// call mandatory live-cycle method of architectView
			if ( this.architectView != null ) {
				this.architectView.onPause();
				
				// unregister accuracy listener in architectView, if set
				if ( this.sensorAccuracyListener != null ) {
					this.architectView.unregisterSensorAccuracyChangeListener( this.sensorAccuracyListener );
				}
			}
			
			// tell locationProvider to pause, usually location is then no longer fetched, so the GPS indicator disappears in status bar
			if ( this.locationProvider != null ) {
				this.locationProvider.onPause();
			}

		}

		@Override
		public void onStop() {
			super.onStop();
		}

		@Override
		public void onDestroy() {
			super.onDestroy();
			if ( this.architectView != null ) {
				this.architectView.onDestroy();
			}
		}


		@Override
		public void onLowMemory() {
			super.onLowMemory();
			if ( this.architectView != null ) {
				this.architectView.onLowMemory();
			}
		}
		
		@Override
		public float getInitialCullingDistanceMeters() {
			return ArchitectViewHolderInterface.CULLING_DISTANCE_DEFAULT_METERS;
		}
		
		/**
		 * path to the architect-file (AR-Experience HTML) to launch
		 * @return
		 */
		@Override
		public abstract String getARchitectWorldPath();
		
		/**
		 * url listener fired once e.g. 'document.location = "architectsdk://foo?bar=123"' is called in JS
		 * @return
		 */
		@Override
		public abstract ArchitectUrlListener getUrlListener();
		
		/**
		 * @return layout id of your layout.xml that holds an ARchitect View, e.g. R.layout.camview
		 */
		@Override
		public abstract int getContentViewId();
		
		/**
		 * @return Wikitude SDK license key, checkout www.wikitude.com for details
		 */
		@Override
		public abstract String getWikitudeSDKLicenseKey();
		
		/**
		 * @return layout-id of architectView, e.g. R.id.architectView
		 */
		@Override
		public abstract int getArchitectViewId();

		/**
		 * 
		 * @return Implementation of a Location
		 */
		@Override
		public abstract ILocationProvider getLocationProvider(final LocationListener locationListener);
		
		/**
		 * @return Implementation of Sensor-Accuracy-Listener. That way you can e.g. show prompt to calibrate compass
		 */
		@Override
		public abstract SensorAccuracyChangeListener getSensorAccuracyListener();
		
		/**
		 * helper to check if video-drawables are supported by this device. recommended to check before launching ARchitect Worlds with videodrawables
		 * @return true if AR.VideoDrawables are supported, false if fallback rendering would apply (= show video fullscreen)
		 */
		public static final boolean isVideoDrawablesSupported() {
			String extensions = GLES20.glGetString( GLES20.GL_EXTENSIONS );
			return extensions != null && extensions.contains( "GL_OES_EGL_image_external" ) && android.os.Build.VERSION.SDK_INT >= 14 ;
		}
}
