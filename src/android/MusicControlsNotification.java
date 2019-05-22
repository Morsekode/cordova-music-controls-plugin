package com.homerours.musiccontrols;




import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import android.annotation.SuppressLint;
import android.app.Service;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.R;
import android.content.Context;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Build;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.net.Uri;

import android.app.NotificationChannel;

import org.json.JSONObject;

import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

public class MusicControlsNotification extends Service {
	private Activity cordovaActivity;
	private NotificationManager notificationManager;
	private Notification.Builder notificationBuilder;
	private int notificationID;
	private MusicControlsInfos infos;
	private Bitmap bitmapCover;
	private String CHANNEL_ID;
	private static final String TAG = "MyActivity";

	// Binder given to clients
	private final IBinder binder = new MusicControlsBinder();

	// Partial wake lock to prevent the app from going to sleep when locked
	private PowerManager.WakeLock wakeLock;

	private Notification theNotification;

	// Public Constructor
	public MusicControlsNotification() {

	}

	public MusicControlsNotification(Activity cordovaActivity,int id){
		this.CHANNEL_ID ="cordova-music-channel-id";
		this.notificationID = id;
		this.cordovaActivity = cordovaActivity;
		Context context = cordovaActivity;
		this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		// use channelid for Oreo and higher
		if (Build.VERSION.SDK_INT >= 26) {
			// The user-visible name of the channel.
			CharSequence name = "cordova-music-controls-plugin";
			// The user-visible description of the channel.
			String description = "cordova-music-controls-plugin notification";

			int importance = NotificationManager.IMPORTANCE_LOW;

			NotificationChannel mChannel = new NotificationChannel(this.CHANNEL_ID, name,importance);

			// Configure the notification channel.
			mChannel.setDescription(description);

			this.notificationManager.createNotificationChannel(mChannel);
		}

	}

	private Notification makeNotification ()
	{
		// use channelid for Oreo and higher
		String CHANNEL_ID = "cordova-music-channel-id";
		if(Build.VERSION.SDK_INT >= 26){
			// The user-visible name of the channel.
			CharSequence name = "cordova-music-controls-plugin";
			// The user-visible description of the channel.
			String description = "cordova-music-controls-plugin notification";

			int importance = NotificationManager.IMPORTANCE_LOW;

			NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name,importance);

			// Configure the notification channel.
			mChannel.setDescription(description);

			getNotificationManager().createNotificationChannel(mChannel);
		}
		String title    ="test";
		String text     = "test";
		boolean bigText = false;

		Context context = getApplicationContext();
		String pkgName  = context.getPackageName();
		Intent intent   = context.getPackageManager()
				.getLaunchIntentForPackage(pkgName);

		Notification.Builder notification = new Notification.Builder(context)
				.setContentTitle(title)
				.setContentText(text)
				.setOngoing(true);
		//.setSmallIcon(getIconResId(settings));

		if(Build.VERSION.SDK_INT >= 26){
			notification.setChannelId(CHANNEL_ID);
		}

		//if (settings.optBoolean("hidden", true)) {
		//	notification.setPriority(Notification.PRIORITY_MIN);
		//}

		if (bigText || text.contains("\n")) {
			notification.setStyle(
					new Notification.BigTextStyle().bigText(text));
		}

		//setColor(notification, settings);

		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent contentIntent = PendingIntent.getActivity(
				context, this.notificationID, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);


		notification.setContentIntent(contentIntent);

		return notification.build();
	}

	/**
	 * Allow clients to call on to the service.
	 */
	@Override
	public IBinder onBind (Intent intent) {
		return binder;
	}

	/**
	 * Class used for the client Binder.  Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	class MusicControlsBinder extends Binder
	{
		MusicControlsNotification getService()
		{
			// Return this instance of ForegroundService
			// so clients can call public methods
			return MusicControlsNotification.this;
		}
	}

	/**
	 * Put the service in a foreground state to prevent app from being killed
	 * by the OS.
	 */
	@Override
	public void onCreate()
	{
		Log.v(TAG, "being created!!!!!!!!!!!");
		super.onCreate();
		keepAwake();
	}

	/**
	 * No need to run headless on destroy.
	 */
	@Override
	public void onDestroy()
	{
		Log.v(TAG, "being destroyed!!!!!!!!!!!");
		super.onDestroy();

		sleepWell();
	}

	/**
	 * Prevent Android from stopping the background service automatically.
	 */
	@Override
	public int onStartCommand (Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	/**
	 * Put the service in a foreground state to prevent app from being killed
	 * by the OS.
	 */
	@SuppressLint("WakelockTimeout")
	private void keepAwake()
	{
		Log.v(TAG, "Do the awake thing");
		startForeground(this.notificationID, makeNotification());

		PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);

		wakeLock = pm.newWakeLock(
				PARTIAL_WAKE_LOCK, "MusicControls:wakelock");

		wakeLock.acquire();
	}

	/**
	 * Stop background mode.
	 */
	private void sleepWell()
	{
		stopForeground(true);
		//this.destroy();

		if (wakeLock != null) {
			wakeLock.release();
			wakeLock = null;
		}
	}


	// Show or update notification
	public void updateNotification(MusicControlsInfos newInfos){
		// Check if the cover has changed
		if (!newInfos.cover.isEmpty() && (this.infos == null || !newInfos.cover.equals(this.infos.cover))){
			this.getBitmapCover(newInfos.cover);
		}
		this.infos = newInfos;
		this.createBuilder();
		this.theNotification = this.notificationBuilder.build();

		this.notificationManager.notify(this.notificationID, this.theNotification);
	}

	// Toggle the play/pause button
	public void updateIsPlaying(boolean isPlaying){
		this.infos.isPlaying=isPlaying;
		this.createBuilder();
		this.theNotification = this.notificationBuilder.build();
		this.notificationManager.notify(this.notificationID, this.theNotification);
	}

	// Toggle the dismissable status
	public void updateDismissable(boolean dismissable){
		this.infos.dismissable=dismissable;
		this.createBuilder();
		this.theNotification = this.notificationBuilder.build();
		this.notificationManager.notify(this.notificationID, this.theNotification);
	}

	// Get image from url
	private void getBitmapCover(String coverURL){
		try{
			if(coverURL.matches("^(https?|ftp)://.*$"))
				// Remote image
				this.bitmapCover = getBitmapFromURL(coverURL);
			else{
				// Local image
				this.bitmapCover = getBitmapFromLocal(coverURL);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	// get Local image
	private Bitmap getBitmapFromLocal(String localURL){
		try {
			Uri uri = Uri.parse(localURL);
			File file = new File(uri.getPath());
			FileInputStream fileStream = new FileInputStream(file);
			BufferedInputStream buf = new BufferedInputStream(fileStream);
			Bitmap myBitmap = BitmapFactory.decodeStream(buf);
			buf.close();
			return myBitmap;
		} catch (Exception ex) {
			try {
				InputStream fileStream = cordovaActivity.getAssets().open("www/" + localURL);
				BufferedInputStream buf = new BufferedInputStream(fileStream);
				Bitmap myBitmap = BitmapFactory.decodeStream(buf);
				buf.close();
				return myBitmap;
			} catch (Exception ex2) {
				ex.printStackTrace();
				ex2.printStackTrace();
				return null;
			}
		}
	}

	// get Remote image
	private Bitmap getBitmapFromURL(String strURL) {
		try {
			URL url = new URL(strURL);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.connect();
			InputStream input = connection.getInputStream();
			Bitmap myBitmap = BitmapFactory.decodeStream(input);
			return myBitmap;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	private void createBuilder(){
		Context context = cordovaActivity;
		Notification.Builder builder = new Notification.Builder(context);

		// use channelid for Oreo and higher
		if (Build.VERSION.SDK_INT >= 26) {
			builder.setChannelId(this.CHANNEL_ID);
		}

		//Configure builder
		builder.setContentTitle(infos.track);
		if (!infos.artist.isEmpty()){
			builder.setContentText(infos.artist);
		}
		builder.setWhen(0);

		// set if the notification can be destroyed by swiping
		if (infos.dismissable){
			builder.setOngoing(false);
			Intent dismissIntent = new Intent("music-controls-destroy");
			PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(context, 1, dismissIntent, 0);
			builder.setDeleteIntent(dismissPendingIntent);
		} else {
			builder.setOngoing(true);
		}
		if (!infos.ticker.isEmpty()){
			builder.setTicker(infos.ticker);
		}

		builder.setPriority(Notification.PRIORITY_MAX);

		//If 5.0 >= set the controls to be visible on lockscreen
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP){
			builder.setVisibility(Notification.VISIBILITY_PUBLIC);
		}

		//Set SmallIcon
		boolean usePlayingIcon = infos.notificationIcon.isEmpty();
		if(!usePlayingIcon){
			int resId = this.getResourceId(infos.notificationIcon, 0);
			usePlayingIcon = resId == 0;
			if(!usePlayingIcon) {
				builder.setSmallIcon(resId);
			}
		}

		if(usePlayingIcon){
			if (infos.isPlaying){
				builder.setSmallIcon(this.getResourceId(infos.playIcon, android.R.drawable.ic_media_play));
			} else {
				builder.setSmallIcon(this.getResourceId(infos.pauseIcon, android.R.drawable.ic_media_pause));
			}
		}

		//Set LargeIcon
		if (!infos.cover.isEmpty() && this.bitmapCover != null){
			builder.setLargeIcon(this.bitmapCover);
		}

		//Open app if tapped
		Intent resultIntent = new Intent(context, cordovaActivity.getClass());
		resultIntent.setAction(Intent.ACTION_MAIN);
		resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, 0);
		builder.setContentIntent(resultPendingIntent);

		//Controls
		int nbControls=0;
		/* Previous  */
		if (infos.hasPrev){
			nbControls++;
			Intent previousIntent = new Intent("music-controls-previous");
			PendingIntent previousPendingIntent = PendingIntent.getBroadcast(context, 1, previousIntent, 0);
			builder.addAction(this.getResourceId(infos.prevIcon, android.R.drawable.ic_media_previous), "", previousPendingIntent);
		}
		if (infos.isPlaying){
			/* Pause  */
			nbControls++;
			Intent pauseIntent = new Intent("music-controls-pause");
			PendingIntent pausePendingIntent = PendingIntent.getBroadcast(context, 1, pauseIntent, 0);
			builder.addAction(this.getResourceId(infos.pauseIcon, android.R.drawable.ic_media_pause), "", pausePendingIntent);
		} else {
			/* Play  */
			nbControls++;
			Intent playIntent = new Intent("music-controls-play");
			PendingIntent playPendingIntent = PendingIntent.getBroadcast(context, 1, playIntent, 0);
			builder.addAction(this.getResourceId(infos.playIcon, android.R.drawable.ic_media_play), "", playPendingIntent);
		}
		/* Next */
		if (infos.hasNext){
			nbControls++;
			Intent nextIntent = new Intent("music-controls-next");
			PendingIntent nextPendingIntent = PendingIntent.getBroadcast(context, 1, nextIntent, 0);
			builder.addAction(this.getResourceId(infos.nextIcon, android.R.drawable.ic_media_next), "", nextPendingIntent);
		}
		/* Close */
		if (infos.hasClose){
			nbControls++;
			Intent destroyIntent = new Intent("music-controls-destroy");
			PendingIntent destroyPendingIntent = PendingIntent.getBroadcast(context, 1, destroyIntent, 0);
			builder.addAction(this.getResourceId(infos.closeIcon, android.R.drawable.ic_menu_close_clear_cancel), "", destroyPendingIntent);
		}

		//If 5.0 >= use MediaStyle
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP){
			int[] args = new int[nbControls];
			for (int i = 0; i < nbControls; ++i) {
				args[i] = i;
			}
			builder.setStyle(new Notification.MediaStyle().setShowActionsInCompactView(args));
		}
		this.notificationBuilder = builder;
	}

	private int getResourceId(String name, int fallback){
		try{
			if(name.isEmpty()){
				return fallback;
			}

			int resId = this.cordovaActivity.getResources().getIdentifier(name, "drawable", this.cordovaActivity.getPackageName());
			return resId == 0 ? fallback : resId;
		}
		catch(Exception ex){
			return fallback;
		}
	}

	public void destroy(){
		this.notificationManager.cancel(this.notificationID);
	}

	/**
	 * Returns the shared notification service manager.
	 */
	private NotificationManager getNotificationManager()
	{
		return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	}
}