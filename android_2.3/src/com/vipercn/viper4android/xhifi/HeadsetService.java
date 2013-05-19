package com.vipercn.viper4android.xhifi;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.vipercn.viper4android.xhifi.activity.ViPER4Android_XHiFi;
import com.vipercn.viper4android.xhifi.preference.EqualizerSurface;

public class HeadsetService extends Service
{
	private class V4ADSPModule
	{
		private final UUID EFFECT_TYPE_NULL = UUID.fromString("ec7178ec-e5e1-4432-a3f4-4657e6795210");
		public AudioEffect mInstance = null;

		public V4ADSPModule(UUID uModuleID, int nPriority)
		{
			try
			{
				mInstance = AudioEffect.class.getConstructor(UUID.class, UUID.class, Integer.TYPE, Integer.TYPE).newInstance(EFFECT_TYPE_NULL, uModuleID, nPriority, 0);
				Log.i("ViPER4Android_XHiFi", "Creating viper4android module, " + uModuleID.toString());
			}
			catch (Exception e)
			{
				Log.i("ViPER4Android_XHiFi", e.getMessage());
				mInstance = null;
			}
		}

		public void release()
		{
			Log.i("ViPER4Android_XHiFi", "Free viper4android module.");
			if (mInstance != null)
				mInstance.release();
			mInstance = null;
		}

		private byte[] intToByteArray(int value)
		{
			ByteBuffer converter = ByteBuffer.allocate(4);
			converter.order(ByteOrder.nativeOrder());
			converter.putInt(value);
			return converter.array();
		}

		private int byteArrayToInt(byte[] valueBuf, int offset)
		{
			ByteBuffer converter = ByteBuffer.wrap(valueBuf);
			converter.order(ByteOrder.nativeOrder());
			return converter.getInt(offset);
		}

		private byte[] concatArrays(byte[]... arrays)
		{
			int len = 0;
			for (byte[] a : arrays)
			{
				len += a.length;
			}
			byte[] b = new byte[len];
			int offs = 0;
			for (byte[] a : arrays)
			{
				System.arraycopy(a, 0, b, offs, a.length);
				offs += a.length;
			}
			return b;
		}

		public void setParameter_px4_vx4x1(int param, int valueL)
		{
			try
			{
				byte[] p = intToByteArray(param);
				byte[] v = intToByteArray(valueL);
				setParameter_Native(p, v);
			}
			catch (Exception e)
			{
				Log.i("ViPER4Android_XHiFi", "setParameter_px4_vx4x1: " + e.getMessage());
			}
		}

		public void setParameter_px4_vx4x2(int param, int valueL, int valueH)
		{
			try
			{
				byte[] p = intToByteArray(param);
				byte[] vL = intToByteArray(valueL);
				byte[] vH = intToByteArray(valueH);
				byte[] v = concatArrays(vL, vH);
				setParameter_Native(p, v);
			}
			catch (Exception e)
			{
				Log.i("ViPER4Android_XHiFi", "setParameter_px4_vx4x2: " + e.getMessage());
			}
		}

		public void setParameter_px4_vx4x3(int param, int valueL, int valueH, int valueE)
		{
			try
			{
				byte[] p = intToByteArray(param);
				byte[] vL = intToByteArray(valueL);
				byte[] vH = intToByteArray(valueH);
				byte[] vE = intToByteArray(valueE);
				byte[] v = concatArrays(vL, vH, vE);
				setParameter_Native(p, v);
			}
			catch (Exception e)
			{
				Log.i("ViPER4Android_XHiFi", "setParameter_px4_vx4x3: " + e.getMessage());
			}
		}

		public void setParameter_px4_vx4x4(int param, int valueL, int valueH, int valueE, int valueR)
		{
			try
			{
				byte[] p = intToByteArray(param);
				byte[] vL = intToByteArray(valueL);
				byte[] vH = intToByteArray(valueH);
				byte[] vE = intToByteArray(valueE);
				byte[] vR = intToByteArray(valueR);
				byte[] v = concatArrays(vL, vH, vE, vR);
				setParameter_Native(p, v);
			}
			catch (Exception e)
			{
				Log.i("ViPER4Android_XHiFi", "setParameter_px4_vx4x4: " + e.getMessage());
			}
		}

		public void setParameter_Native(byte[] parameter, byte[] value)
		{
			if (mInstance == null) return;
			try
			{
				Method setParameter = AudioEffect.class.getMethod("setParameter", byte[].class, byte[].class);
				setParameter.invoke(mInstance, parameter, value);
			}
			catch (Exception e)
			{
				Log.i("ViPER4Android_XHiFi", "setParameter_Native: " + e.getMessage());
			}
		}

		public int getParameter_px4_vx4x1(int param)
		{
			try
			{
				byte[] p = intToByteArray(param);
				byte[] v = new byte[4];
				getParameter_Native(p, v);
				int val = byteArrayToInt(v, 0);
				return val;
			}
			catch (Exception e)
			{
				Log.i("ViPER4Android_XHiFi", "getParameter_px4_vx4x1: " + e.getMessage());
				return -1;
			}
		}

		public void getParameter_Native(byte[] parameter, byte[] value)
		{
			if (mInstance == null) return;
			try
			{
				Method getParameter = AudioEffect.class.getMethod("getParameter", byte[].class, byte[].class);
				getParameter.invoke(mInstance, parameter, value);
			}
			catch (Exception e)
			{
				Log.i("ViPER4Android_XHiFi", "getParameter_Native: " + e.getMessage());
			}
		}
	}

	public class LocalBinder extends Binder
	{
		public HeadsetService getService()
		{
			return HeadsetService.this;
		}
	}

	public static final String NAME = "com.vipercn.viper4android.xhifi.HEADSET_SERVICE";
	public static final UUID ID_V4A_GENERAL_XHIFI = UUID.fromString("d92c3a90-3e26-11e2-a25f-0800200c9a66");

	/* ViPER4Android Driver Status */
	public static final int PARAM_GET_DRIVER_VERSION = 32769;
	public static final int PARAM_GET_ENABLED = 32770;
	public static final int PARAM_GET_CONFIGURE = 32771;
	public static final int PARAM_GET_STREAMING = 32772;
	public static final int PARAM_GET_PRECIOUS = 32773;
	public static final int PARAM_GET_SAMPLINGRATE = 32774;
	public static final int PARAM_GET_CHANNELS = 32775;
	/*******************************/

	/* ViPER4Android Driver Status Control */
	public static final int PARAM_SET_UPDATE_STATUS = 36865;
	/***************************************/

	/* ViPER4Android XHiFi Process Precious */
	public static final int V4A_XHIFI_PROCESS_PRECIOUS_FLOAT32 = 0;
	public static final int V4A_XHIFI_PROCESS_PRECIOUS_FLOAT64 = 1;
	/**************************/

	/* ViPER4Android General XHiFi Parameters */
	public static final int PARAM_PROCESS_PRECIOUS_SWITCH = 131073;
	public static final int PARAM_PLAYBACKGAIN_ENABLE = 131074;
	public static final int PARAM_PLAYBACKGAIN_RATIO = 131075;
	public static final int PARAM_PLAYBACKGAIN_VOLUME = 131076;
	public static final int PARAM_PLAYBACKGAIN_MAXSCALER = 131077;
	public static final int PARAM_EQUALIZER_ENABLE = 131078;
	public static final int PARAM_EQUALIZER_BANDLEVEL = 131079;
	public static final int PARAM_XFISOUND_ENABLE = 131080;
	public static final int PARAM_XFISOUND_PROCESS = 131081;
	public static final int PARAM_XFISOUND_LOCONTOUR = 131082;
	public static final int PARAM_OUTPUT_VOLUME = 131083;
	public static final int PARAM_LIMITER_THRESHOLD = 131084;
	/***************************************/

    private AudioManager mAudioManager = null;
    private V4ADSPModule mGeneralXHiFi = null;
	private boolean mServicePrepared = false;
	private boolean mDriverIsReady = false;

    private final LocalBinder mBinder = new LocalBinder();
	protected boolean useHeadphone = false;
	protected boolean inCall = false;
	protected String mPreviousMode = "none";

    private final BroadcastReceiver audioSessionReceiver = new BroadcastReceiver()
    {
		@Override
		public void onReceive(Context context, Intent intent)
		{
			updateDspSystem();
			Log.i("ViPER4Android_XHiFi", "audioSessionReceiver::onReceive()");
		}
	};

    private final BroadcastReceiver preferenceUpdateReceiver = new BroadcastReceiver()
    {
		@Override
		public void onReceive(Context context, Intent intent)
		{
			updateDspSystem();
			Log.i("ViPER4Android_XHiFi", "preferenceUpdateReceiver::onReceive()");
		}
	};

	private final BroadcastReceiver showNotifyReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			SharedPreferences prefSettings = getSharedPreferences(ViPER4Android_XHiFi.SHARED_PREFERENCES_BASENAME + ".settings", MODE_PRIVATE);
			String szLockedEffect = prefSettings.getString("viper4android.settings.lock_effect", "none");
			String mode = "";

			if (szLockedEffect.equalsIgnoreCase("none"))
			{
				if (inCall) mode = "disable";
				else if (mAudioManager.isBluetoothA2dpOn()) mode = "bluetooth";
				else mode = useHeadphone ? "headset" : "speaker";
			}
			else mode = szLockedEffect;

			if (mode.equalsIgnoreCase("headset"))
				ShowNotification(getString(getResources().getIdentifier("text_headset", "string", getApplicationInfo().packageName)));
			else if (mode.equalsIgnoreCase("bluetooth"))
				ShowNotification(getString(getResources().getIdentifier("text_bluetooth", "string", getApplicationInfo().packageName)));
			else CancelNotification();

			Log.i("ViPER4Android_XHiFi", "showNotifyReceiver::onReceive()");
		}
	};

	private final BroadcastReceiver cancelNotifyReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			CancelNotification();
			Log.i("ViPER4Android_XHiFi", "cancelNotifyReceiver::onReceive()");
		}
	};

	private final BroadcastReceiver screenOnReceiver = new BroadcastReceiver()
	{
		@Override  
		public void onReceive(final Context context, final Intent intent)
		{
			updateDspSystem();
			Log.i("ViPER4Android_XHiFi", "screenOnReceiver::onReceive()");
		}
	};

    private final BroadcastReceiver headsetReceiver = new BroadcastReceiver()
    {
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String mode = "disable";
			final String action = intent.getAction();

			if (inCall)
			{
				mode = "disable";
				updateDspSystem();
				Log.i("ViPER4Android_XHiFi", "headsetReceiver::onReceive()");
				return;
			}

            if (action.equals(Intent.ACTION_HEADSET_PLUG))
            {
            	useHeadphone = intent.getIntExtra("state", 0) != 0;
        		if (mAudioManager.isBluetoothA2dpOn()) mode = "bluetooth";
        		else mode = useHeadphone ? "headset" : "speaker";
            }
            else if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED))
            {
                final int deviceClass = ((BluetoothDevice)intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)).getBluetoothClass().getDeviceClass();
                if ((deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES) || (deviceClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET))
                {
                	mode = "bluetooth";
				}
			}
            else if (action.equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
            {
        		if (mAudioManager.isBluetoothA2dpOn()) mode = "bluetooth";
        		else mode = useHeadphone ? "headset" : "speaker";
            }
            else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED))
            {
            	final int deviceClass = ((BluetoothDevice)intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)).getBluetoothClass().getDeviceClass();
                if ((deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES) || (deviceClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET))
				{
                	mode = useHeadphone ? "headset" : "speaker";
				}
			}

            updateDspSystem(mode);
            Log.i("ViPER4Android_XHiFi", "headsetReceiver::onReceive()");
		}
	};

	private final PhoneStateListener mPhoneListener = new PhoneStateListener()
	{
		@Override
		public void onCallStateChanged(int state, String incomingNumber)
		{
			switch (state)
			{
			case TelephonyManager.CALL_STATE_OFFHOOK:
				inCall = true;
				break;
			default:
				inCall = false;
				break;
			}
			updateDspSystem();
		}
	};

	private void ShowNotification(String nFXType)
	{
    	SharedPreferences preferences = getSharedPreferences(ViPER4Android_XHiFi.SHARED_PREFERENCES_BASENAME + ".settings", MODE_PRIVATE);
    	boolean bEnableNotify = preferences.getBoolean("viper4android.settings.show_notify_icon", false);
    	if (!bEnableNotify)
    	{
    		Log.i("ViPER4Android_XHiFi", "ShowNotification(): show_notify = false");
    		return;
    	}

        int nIconID = getResources().getIdentifier("icon", "drawable", getApplicationInfo().packageName);
		String szNotifyText = "ViPER4Android XHiFi " + nFXType;

        Notification notify = new Notification(nIconID, szNotifyText, System.currentTimeMillis());
        notify.flags |= Notification.FLAG_ONGOING_EVENT;
        notify.flags |= Notification.FLAG_NO_CLEAR;
        notify.defaults = 0;

        CharSequence contentTitle = "ViPER4Android XHiFi";
        CharSequence contentText = nFXType;

        Intent notificationIntent = new Intent(HeadsetService.this, ViPER4Android_XHiFi.class);
        PendingIntent contentItent = PendingIntent.getActivity(HeadsetService.this, 0, notificationIntent, 0);
        notify.setLatestEventInfo(HeadsetService.this, contentTitle, contentText, contentItent);

        NotificationManager notificationManager = (NotificationManager)getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0x1234, notify);
    }

	private void CancelNotification()
	{
		NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE); 
        notificationManager.cancel(0x1234);
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		mServicePrepared = false;
		
		try
		{
			CancelNotification();

			try
			{
				Log.i("ViPER4Android_XHiFi", "Creating global V4ADSPModule ...");
				if (mGeneralXHiFi == null)
					mGeneralXHiFi = new V4ADSPModule(ID_V4A_GENERAL_XHIFI, 0x7FFF);
			}
			catch (Exception e)
			{
				Log.i("ViPER4Android_XHiFi", "Creating V4ADSPModule failed.");
				mGeneralXHiFi = null;
			}

			if (mGeneralXHiFi == null)
				mDriverIsReady = false;
			else
			{
				mDriverIsReady = true;
				String szDriverVer = GetDriverVersion();
				if (szDriverVer.equals("0.0.0.0")) mDriverIsReady = false;
				else mDriverIsReady = true;
			}

			Context context = getApplicationContext();
			mAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);

			startForeground(ViPER4Android_XHiFi.NOTIFY_FOREGROUND_ID, new Notification());

			TelephonyManager tm = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
			tm.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);

			IntentFilter audioFilter = new IntentFilter();
			audioFilter.addAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
			audioFilter.addAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);

			registerReceiver(audioSessionReceiver, audioFilter);

			IntentFilter intentFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
			intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
			intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
			intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
			registerReceiver(headsetReceiver, intentFilter);

			registerReceiver(preferenceUpdateReceiver, new IntentFilter(ViPER4Android_XHiFi.ACTION_UPDATE_PREFERENCES));

			registerReceiver(showNotifyReceiver, new IntentFilter(ViPER4Android_XHiFi.ACTION_SHOW_NOTIFY));
			registerReceiver(cancelNotifyReceiver, new IntentFilter(ViPER4Android_XHiFi.ACTION_CANCEL_NOTIFY));

			final IntentFilter screenFilter = new IntentFilter();
			screenFilter.addAction(Intent.ACTION_SCREEN_ON);
		    registerReceiver(screenOnReceiver, screenFilter);

			Log.i("ViPER4Android_XHiFi", "Service launched.");

			updateDspSystem();

			mServicePrepared = true;
		}
		catch (Exception e)
		{
			mServicePrepared = false;
			CancelNotification();
			System.exit(0);
		}
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		mServicePrepared = false;

		try
		{
			stopForeground(true);

			unregisterReceiver(audioSessionReceiver);
			unregisterReceiver(headsetReceiver);
			unregisterReceiver(preferenceUpdateReceiver);
			unregisterReceiver(screenOnReceiver);

			unregisterReceiver(showNotifyReceiver);
			unregisterReceiver(cancelNotifyReceiver);

			TelephonyManager tm = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
			tm.listen(mPhoneListener, 0);

			CancelNotification();

			if (mGeneralXHiFi != null)
				mGeneralXHiFi.release();
			mGeneralXHiFi = null;

			Log.i("ViPER4Android_XHiFi", "Service destroyed.");
		}
		catch(Exception e)
		{
			CancelNotification();
		}
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return mBinder;
	}

	public boolean GetServicePrepared()
	{
		return mServicePrepared;
	}

	public boolean GetDriverIsReady()
	{
		return mDriverIsReady;
	}

	public void StartStatusUpdating()
	{
		if (mGeneralXHiFi != null && mDriverIsReady)
			mGeneralXHiFi.setParameter_px4_vx4x1(PARAM_SET_UPDATE_STATUS, 1);
	}

	public void StopStatusUpdating()
	{
		if (mGeneralXHiFi != null && mDriverIsReady)
			mGeneralXHiFi.setParameter_px4_vx4x1(PARAM_SET_UPDATE_STATUS, 0);
	}

	public String GetDriverVersion()
	{
		int nVerDWord = 0;
		if (mGeneralXHiFi != null && mDriverIsReady)
			nVerDWord = mGeneralXHiFi.getParameter_px4_vx4x1(PARAM_GET_DRIVER_VERSION);
		int VMain, VSub, VExt, VBuild;
		VMain  = (nVerDWord & 0xFF000000) >> 24;
		VSub   = (nVerDWord & 0x00FF0000) >> 16;
		VExt   = (nVerDWord & 0x0000FF00) >>  8;
		VBuild = (nVerDWord & 0x000000FF) >>  0;
		return VMain + "." + VSub + "." + VExt + "." + VBuild;
	}

	public boolean GetDriverEnabled()
	{
		boolean bResult = false;
		if (mGeneralXHiFi != null && mDriverIsReady)
		{
			if (mGeneralXHiFi.getParameter_px4_vx4x1(PARAM_GET_ENABLED) == 1)
				bResult = true;
		}
		return bResult;
	}

	public boolean GetDriverUsable()
	{
		boolean bResult = false;
		if (mGeneralXHiFi != null && mDriverIsReady)
		{
			if (mGeneralXHiFi.getParameter_px4_vx4x1(PARAM_GET_CONFIGURE) == 1)
				bResult = true;
		}
		return bResult;
	}

	public boolean GetDriverProcess()
	{
		boolean bResult = false;
		if (mGeneralXHiFi != null && mDriverIsReady)
		{
			if (mGeneralXHiFi.getParameter_px4_vx4x1(PARAM_GET_STREAMING) == 1)
				bResult = true;
		}
		return bResult;
	}

	public int GetDriverPrecious()
	{
		int nResult = V4A_XHIFI_PROCESS_PRECIOUS_FLOAT32;
		if (mGeneralXHiFi != null && mDriverIsReady)
			nResult = mGeneralXHiFi.getParameter_px4_vx4x1(PARAM_GET_PRECIOUS);
		return nResult;
	}

	public int GetDriverSamplingRate()
	{
		int nResult = 0;
		if (mGeneralXHiFi != null && mDriverIsReady)
			nResult = mGeneralXHiFi.getParameter_px4_vx4x1(PARAM_GET_SAMPLINGRATE);
		return nResult;
	}

	public int GetDriverChannels()
	{
		int nResult = 0;
		if (mGeneralXHiFi != null && mDriverIsReady)
			nResult = mGeneralXHiFi.getParameter_px4_vx4x1(PARAM_GET_CHANNELS);
		return nResult;
	}

	protected void SetV4AEqualizerBandLevel(int idx, int level, V4ADSPModule dsp)
	{
		if (dsp == null || !mDriverIsReady) return;
		dsp.setParameter_px4_vx4x2(PARAM_EQUALIZER_BANDLEVEL, idx, level);
	}

	protected void updateDspSystem()
	{
		final String mode;

		if (inCall) mode = "disable";
		else
		{
			SharedPreferences prefSettings = getSharedPreferences(ViPER4Android_XHiFi.SHARED_PREFERENCES_BASENAME + ".settings", MODE_PRIVATE);
			String szLockedEffect = prefSettings.getString("viper4android.settings.lock_effect", "none");
			if (szLockedEffect.equalsIgnoreCase("none"))
			{
				if (mAudioManager.isBluetoothA2dpOn()) mode = "bluetooth";
				else mode = useHeadphone ? "headset" : "speaker";
			}
			else mode = szLockedEffect;
		}

		if (!mode.equalsIgnoreCase(mPreviousMode))
		{
			mPreviousMode = mode;
			if (mode.equalsIgnoreCase("headset"))
				ShowNotification(getString(getResources().getIdentifier("text_headset", "string", getApplicationInfo().packageName)));
			else if (mode.equalsIgnoreCase("bluetooth"))
				ShowNotification(getString(getResources().getIdentifier("text_bluetooth", "string", getApplicationInfo().packageName)));
			else CancelNotification();
		}

		SharedPreferences preferences = getSharedPreferences(ViPER4Android_XHiFi.SHARED_PREFERENCES_BASENAME + "." + mode, 0);	
		Log.i("ViPER4Android_XHiFi", "Begin system update(" + mode + ")");

		if ((mGeneralXHiFi == null) || (mGeneralXHiFi.mInstance == null))
		{
			Log.i("ViPER4Android_XHiFi", "updateSystem(): Effects is invalid!");
			return;
		}

		AudioEffect.Descriptor mFXVerify = mGeneralXHiFi.mInstance.getDescriptor();
		if (mFXVerify == null)
		{
			Log.i("ViPER4Android_XHiFi", "updateSystem(): Effects token lost!");
			return;
		}
		if (!mFXVerify.uuid.equals(ID_V4A_GENERAL_XHIFI))
		{
			Toast.makeText(HeadsetService.this,
					getString(getResources().getIdentifier("text_token_lost", "string", getApplicationInfo().packageName)),
					Toast.LENGTH_LONG).show();

			Log.i("ViPER4Android_XHiFi", "updateSystem(): Effects token lost!");
			Log.i("ViPER4Android_XHiFi", "updateSystem(): The effects has been replaced by system!");
			Log.i("ViPER4Android_XHiFi", "updateSystem(): Reloading driver");
			try
			{
				mGeneralXHiFi.release();
				mGeneralXHiFi = new V4ADSPModule(ID_V4A_GENERAL_XHIFI, 0x7FFF);
				if ((mGeneralXHiFi == null) || (mGeneralXHiFi.mInstance == null)) return;
			}
			catch (Exception e)
			{
				return;
			}
		}
		if (!mGeneralXHiFi.mInstance.hasControl())
		{
			Toast.makeText(HeadsetService.this,
					getString(getResources().getIdentifier("text_token_lost", "string", getApplicationInfo().packageName)),
					Toast.LENGTH_LONG).show();

			Log.i("ViPER4Android_XHiFi", "updateSystem(): Effects token lost!");
			Log.i("ViPER4Android_XHiFi", "updateSystem(): The effects has been taken over by system!");
			Log.i("ViPER4Android_XHiFi", "updateSystem(): Reloading driver");
			try
			{
				mGeneralXHiFi.release();
				mGeneralXHiFi = new V4ADSPModule(ID_V4A_GENERAL_XHIFI, 0x7FFF);
				if ((mGeneralXHiFi == null) || (mGeneralXHiFi.mInstance == null)) return;
			}
			catch (Exception e)
			{
				return;
			}
		}

		/* Playback Auto Gain Control */
		Log.i("ViPER4Android_XHiFi", "updateSystem(): Updating Playback AGC.");
		mGeneralXHiFi.setParameter_px4_vx4x1(PARAM_PLAYBACKGAIN_RATIO, Integer.valueOf(preferences.getString("viper4android.headphone_xhifi.playbackgain.ratio", "50")));
		mGeneralXHiFi.setParameter_px4_vx4x1(PARAM_PLAYBACKGAIN_VOLUME, Integer.valueOf(preferences.getString("viper4android.headphone_xhifi.playbackgain.volume", "80")));
		mGeneralXHiFi.setParameter_px4_vx4x1(PARAM_PLAYBACKGAIN_MAXSCALER, Integer.valueOf(preferences.getString("viper4android.headphone_xhifi.playbackgain.maxscaler", "400")));
		if (preferences.getBoolean("viper4android.headphone_xhifi.playbackgain.enable", false))
			mGeneralXHiFi.setParameter_px4_vx4x1(PARAM_PLAYBACKGAIN_ENABLE, 1);
		else mGeneralXHiFi.setParameter_px4_vx4x1(PARAM_PLAYBACKGAIN_ENABLE, 0);

		/* FIR Equalizer */
		Log.i("ViPER4Android_XHiFi", "updateSystem(): Updating FIR Equalizer.");
		String[] levels = preferences.getString("viper4android.headphone_xhifi.fireq.custom", "0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;").split(";");
		for (short i = 0; i < levels.length; i++)
			SetV4AEqualizerBandLevel(i, (int)Math.round((Float.valueOf(levels[i]) / EqualizerSurface.MAX_DB) * 100), mGeneralXHiFi);

		if (preferences.getBoolean("viper4android.headphone_xhifi.fireq.enable", false))
			mGeneralXHiFi.setParameter_px4_vx4x1(PARAM_EQUALIZER_ENABLE, 1);
		else mGeneralXHiFi.setParameter_px4_vx4x1(PARAM_EQUALIZER_ENABLE, 0);

		/* XFi Sound */
		Log.i("ViPER4Android_XHiFi", "updateSystem(): Updating XFi Sound.");
		mGeneralXHiFi.setParameter_px4_vx4x1(PARAM_XFISOUND_PROCESS, Integer.valueOf(preferences.getString("viper4android.headphone_xhifi.xfisound.process", "110")));
		mGeneralXHiFi.setParameter_px4_vx4x1(PARAM_XFISOUND_LOCONTOUR, Integer.valueOf(preferences.getString("viper4android.headphone_xhifi.xfisound.locontour", "0")));
		if (preferences.getBoolean("viper4android.headphone_xhifi.xfisound.enable", false))
			mGeneralXHiFi.setParameter_px4_vx4x1(PARAM_XFISOUND_ENABLE, 1);
		else mGeneralXHiFi.setParameter_px4_vx4x1(PARAM_XFISOUND_ENABLE, 0);

		/* Limiter */
		Log.i("ViPER4Android_XHiFi", "updateSystem(): Updating Limiter.");
		mGeneralXHiFi.setParameter_px4_vx4x1(PARAM_OUTPUT_VOLUME, Integer.valueOf(preferences.getString("viper4android.headphone_xhifi.outvol", "100")));
		mGeneralXHiFi.setParameter_px4_vx4x1(PARAM_LIMITER_THRESHOLD, Integer.valueOf(preferences.getString("viper4android.headphone_xhifi.limiter", "100")));

		/* Master Switch */
		mGeneralXHiFi.mInstance.setEnabled(preferences.getBoolean("viper4android.headphone_xhifi.enable", false));
		
		Log.i("ViPER4Android_XHiFi", "System updated.");
	}

	protected void updateDspSystem(String mode)
	{
		if (!mode.equalsIgnoreCase("disable"))
		{
			SharedPreferences prefSettings = getSharedPreferences(ViPER4Android_XHiFi.SHARED_PREFERENCES_BASENAME + ".settings", MODE_PRIVATE);
			String szLockedEffect = prefSettings.getString("viper4android.settings.lock_effect", "none");
			if (!szLockedEffect.equalsIgnoreCase("none")) mode = szLockedEffect;
		}

		if (!mode.equalsIgnoreCase(mPreviousMode))
		{
			mPreviousMode = mode;
			if (mode.equalsIgnoreCase("headset"))
				ShowNotification(getString(getResources().getIdentifier("text_headset", "string", getApplicationInfo().packageName)));
			else if (mode.equalsIgnoreCase("bluetooth"))
				ShowNotification(getString(getResources().getIdentifier("text_bluetooth", "string", getApplicationInfo().packageName)));
			else CancelNotification();
		}

		SharedPreferences preferences = getSharedPreferences(ViPER4Android_XHiFi.SHARED_PREFERENCES_BASENAME + "." + mode, 0);	
		Log.i("ViPER4Android_XHiFi", "Begin system update(" + mode + ")");

		if ((mGeneralXHiFi == null) || (mGeneralXHiFi.mInstance == null))
		{
			Log.i("ViPER4Android_XHiFi", "updateSystem(): Effects is invalid!");
			return;
		}

		AudioEffect.Descriptor mFXVerify = mGeneralXHiFi.mInstance.getDescriptor();
		if (mFXVerify == null)
		{
			Log.i("ViPER4Android_XHiFi", "updateSystem(): Effects token lost!");
			return;
		}
		if (!mFXVerify.uuid.equals(ID_V4A_GENERAL_XHIFI))
		{
			Toast.makeText(HeadsetService.this,
					getString(getResources().getIdentifier("text_token_lost", "string", getApplicationInfo().packageName)),
					Toast.LENGTH_LONG).show();

			Log.i("ViPER4Android_XHiFi", "updateSystem(): Effects token lost!");
			Log.i("ViPER4Android_XHiFi", "updateSystem(): The effects has been replaced by system!");
			Log.i("ViPER4Android_XHiFi", "updateSystem(): Reloading driver");
			try
			{
				mGeneralXHiFi.release();
				mGeneralXHiFi = new V4ADSPModule(ID_V4A_GENERAL_XHIFI, 0x7FFF);
				if ((mGeneralXHiFi == null) || (mGeneralXHiFi.mInstance == null)) return;
			}
			catch (Exception e)
			{
				return;
			}
		}
		if (!mGeneralXHiFi.mInstance.hasControl())
		{
			Toast.makeText(HeadsetService.this,
					getString(getResources().getIdentifier("text_token_lost", "string", getApplicationInfo().packageName)),
					Toast.LENGTH_LONG).show();

			Log.i("ViPER4Android_XHiFi", "updateSystem(): Effects token lost!");
			Log.i("ViPER4Android_XHiFi", "updateSystem(): The effects has been taken over by system!");
			Log.i("ViPER4Android_XHiFi", "updateSystem(): Reloading driver");
			try
			{
				mGeneralXHiFi.release();
				mGeneralXHiFi = new V4ADSPModule(ID_V4A_GENERAL_XHIFI, 0x7FFF);
				if ((mGeneralXHiFi == null) || (mGeneralXHiFi.mInstance == null)) return;
			}
			catch (Exception e)
			{
				return;
			}
		}

		/* Playback Auto Gain Control */
		Log.i("ViPER4Android_XHiFi", "updateSystem(): Updating Playback AGC.");
		mGeneralXHiFi.setParameter_px4_vx4x1(PARAM_PLAYBACKGAIN_RATIO, Integer.valueOf(preferences.getString("viper4android.headphone_xhifi.playbackgain.ratio", "50")));
		mGeneralXHiFi.setParameter_px4_vx4x1(PARAM_PLAYBACKGAIN_VOLUME, Integer.valueOf(preferences.getString("viper4android.headphone_xhifi.playbackgain.volume", "80")));
		mGeneralXHiFi.setParameter_px4_vx4x1(PARAM_PLAYBACKGAIN_MAXSCALER, Integer.valueOf(preferences.getString("viper4android.headphone_xhifi.playbackgain.maxscaler", "400")));
		if (preferences.getBoolean("viper4android.headphone_xhifi.playbackgain.enable", false))
			mGeneralXHiFi.setParameter_px4_vx4x1(PARAM_PLAYBACKGAIN_ENABLE, 1);
		else mGeneralXHiFi.setParameter_px4_vx4x1(PARAM_PLAYBACKGAIN_ENABLE, 0);

		/* FIR Equalizer */
		Log.i("ViPER4Android_XHiFi", "updateSystem(): Updating FIR Equalizer.");
		String[] levels = preferences.getString("viper4android.headphone_xhifi.fireq.custom", "0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;").split(";");
		for (short i = 0; i < levels.length; i++)
			SetV4AEqualizerBandLevel(i, (int)Math.round((Float.valueOf(levels[i]) / EqualizerSurface.MAX_DB) * 100), mGeneralXHiFi);

		if (preferences.getBoolean("viper4android.headphone_xhifi.fireq.enable", false))
			mGeneralXHiFi.setParameter_px4_vx4x1(PARAM_EQUALIZER_ENABLE, 1);
		else mGeneralXHiFi.setParameter_px4_vx4x1(PARAM_EQUALIZER_ENABLE, 0);

		/* XFi Sound */
		Log.i("ViPER4Android_XHiFi", "updateSystem(): Updating XFi Sound.");
		mGeneralXHiFi.setParameter_px4_vx4x1(PARAM_XFISOUND_PROCESS, Integer.valueOf(preferences.getString("viper4android.headphone_xhifi.xfisound.process", "110")));
		mGeneralXHiFi.setParameter_px4_vx4x1(PARAM_XFISOUND_LOCONTOUR, Integer.valueOf(preferences.getString("viper4android.headphone_xhifi.xfisound.locontour", "0")));
		if (preferences.getBoolean("viper4android.headphone_xhifi.xfisound.enable", false))
			mGeneralXHiFi.setParameter_px4_vx4x1(PARAM_XFISOUND_ENABLE, 1);
		else mGeneralXHiFi.setParameter_px4_vx4x1(PARAM_XFISOUND_ENABLE, 0);

		/* Limiter */
		Log.i("ViPER4Android_XHiFi", "updateSystem(): Updating Limiter.");
		mGeneralXHiFi.setParameter_px4_vx4x1(PARAM_OUTPUT_VOLUME, Integer.valueOf(preferences.getString("viper4android.headphone_xhifi.outvol", "100")));
		mGeneralXHiFi.setParameter_px4_vx4x1(PARAM_LIMITER_THRESHOLD, Integer.valueOf(preferences.getString("viper4android.headphone_xhifi.limiter", "100")));

		/* Master Switch */
		mGeneralXHiFi.mInstance.setEnabled(preferences.getBoolean("viper4android.headphone_xhifi.enable", false));
		
		Log.i("ViPER4Android_XHiFi", "System updated.");
	}
}
