package com.vipercn.viper4android.xhifi.service;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;

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
import android.util.Log;
import android.widget.Toast;

import com.vipercn.viper4android.xhifi.activity.ViPER4Android_XHiFi;
import com.vipercn.viper4android.xhifi.preference.EqualizerSurface;

public class HeadsetService extends Service
{
	private class ResourceMutex
	{
		private Semaphore mSignal = new Semaphore(1);

		public boolean acquire()
		{
			try
			{
				mSignal.acquire();
				return true;
			}
			catch (InterruptedException e)
			{
				return false;
			}
		}

		public void release()
		{
			mSignal.release();
		}
	}

	private class V4ADSPModule
	{
		private final UUID EFFECT_TYPE_NULL = UUID.fromString("ec7178ec-e5e1-4432-a3f4-4657e6795210");
		public AudioEffect mInstance = null;

		public V4ADSPModule(UUID uModuleID, int nPriority, int nAudioSession)
		{
			try
			{
				mInstance = AudioEffect.class.getConstructor(UUID.class, UUID.class, Integer.TYPE, Integer.TYPE).newInstance(EFFECT_TYPE_NULL, uModuleID, nPriority, nAudioSession);
				Log.i("ViPER4Android_XHiFi", "Creating viper4android module, " + uModuleID.toString() + ", on session " + nAudioSession);
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

	private final LocalBinder mBinder = new LocalBinder();
	protected boolean mUseHeadset = false;
	protected boolean mUseBluetooth = false;
	protected String mPreviousMode = "none";
	private float[] mOverriddenEqualizerLevels;

	private V4ADSPModule mGeneralXHiFi = null;
	private boolean mServicePrepared = false;
	private boolean mDriverIsReady = false;

	private Map<Integer, V4ADSPModule> mGeneralXHiFiList = new HashMap<Integer, V4ADSPModule>();
	private ResourceMutex mV4AMutex = new ResourceMutex();

    private final BroadcastReceiver mAudioSessionReceiver = new BroadcastReceiver()
    {
		@Override
		public void onReceive(Context context, Intent intent)
		{
			Log.i("ViPER4Android_XHiFi", "mAudioSessionReceiver::onReceive()");

			SharedPreferences prefSettings = getSharedPreferences(ViPER4Android_XHiFi.SHARED_PREFERENCES_BASENAME + ".settings", MODE_PRIVATE);
			String szCompatibleMode = prefSettings.getString("viper4android.settings.compatiblemode", "global");
			boolean mFXInLocalMode = false;
			if (szCompatibleMode.equals("global")) mFXInLocalMode = false;
			else mFXInLocalMode = true;

			String action = intent.getAction();
			int sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0);
			if (sessionId == 0)
			{
				Log.i("ViPER4Android_XHiFi", String.format("New audio session: %d", sessionId));
				return;
			}

			if (action.equals(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION))
			{
				Log.i("ViPER4Android_XHiFi", String.format("New audio session: %d", sessionId));
				if (!mFXInLocalMode)
				{
					Log.i("ViPER4Android_XHiFi", "Only global effect allowed.");
					return;
				}
				if (mV4AMutex.acquire())
				{
					if (!mGeneralXHiFiList.containsKey(sessionId))
					{
						Log.i("ViPER4Android_XHiFi", "Creating local V4ADSPModule ...");
						mGeneralXHiFiList.put(sessionId, new V4ADSPModule(ID_V4A_GENERAL_XHIFI, 0x7FFF, sessionId));
					}
					mV4AMutex.release();
				}
				else Log.i("ViPER4Android_XHiFi", "Semaphore accquire failed.");
			}

			if (action.equals(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION))
			{
				Log.i("ViPER4Android_XHiFi", String.format("Audio session removed: %d", sessionId));
				if (mV4AMutex.acquire())
				{
					if (mGeneralXHiFiList.containsKey(sessionId))
					{
						V4ADSPModule v4aRemove = mGeneralXHiFiList.remove(sessionId);
						if (v4aRemove != null)
							v4aRemove.release();
					}
					mV4AMutex.release();
				}
				else Log.i("ViPER4Android_XHiFi", "Semaphore accquire failed.");
			}

			updateSystem();
		}
	};

	private final BroadcastReceiver mPreferenceUpdateReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			Log.i("ViPER4Android_XHiFi", "mPreferenceUpdateReceiver::onReceive()");
			updateSystem();
		}
	};

	private final BroadcastReceiver mShowNotifyReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String mode = getAudioOutputRouting();
			if (mode.equalsIgnoreCase("headset"))
				ShowNotification(getString(getResources().getIdentifier("text_headset", "string", getApplicationInfo().packageName)));
			else if (mode.equalsIgnoreCase("bluetooth"))
				ShowNotification(getString(getResources().getIdentifier("text_bluetooth", "string", getApplicationInfo().packageName)));
			else ShowNotification(getString(getResources().getIdentifier("text_speaker", "string", getApplicationInfo().packageName)));

			Log.i("ViPER4Android_XHiFi", "mShowNotifyReceiver::onReceive()");
		}
	};

	private final BroadcastReceiver mCancelNotifyReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			CancelNotification();
			Log.i("ViPER4Android_XHiFi", "mCancelNotifyReceiver::onReceive()");
		}
	};

	private final BroadcastReceiver mScreenOnReceiver = new BroadcastReceiver()
	{  
		@Override  
		public void onReceive(final Context context, final Intent intent)
		{
			updateSystem();
			Log.i("ViPER4Android_XHiFi", "mScreenOnReceiver::onReceive()");
		}
	};

	private final BroadcastReceiver mRoutingReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(final Context context, final Intent intent)
		{
			final String action = intent.getAction();
			final boolean prevUseHeadset = mUseHeadset;
			final boolean prevUseBluetooth = mUseBluetooth;
			final AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

            if (action.equals(Intent.ACTION_HEADSET_PLUG))
            {
                mUseHeadset = intent.getIntExtra("state", 0) == 1;
            }
            else if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED))
            {
                final int deviceClass = ((BluetoothDevice)intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)).getBluetoothClass()
                        .getDeviceClass();
                if ((deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES) || (deviceClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET))
                {
					mUseBluetooth = true;
				}
			}
            else if (action.equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
            {
				mUseBluetooth = audioManager.isBluetoothA2dpOn();
				mUseHeadset = audioManager.isWiredHeadsetOn();
            }
            else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED))
            {
                final int deviceClass = ((BluetoothDevice)intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)).getBluetoothClass()
                        .getDeviceClass();
				if ((deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES) || (deviceClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET))
				{
					mUseBluetooth = false;
				}
			}

            Log.i("ViPER4Android_XHiFi", "Headset=" + mUseHeadset + ", Bluetooth=" + mUseBluetooth);
            Log.i("ViPER4Android_XHiFi", "mRoutingReceiver::onReceive()");

			if (prevUseHeadset != mUseHeadset || prevUseBluetooth != mUseBluetooth)
			{
				updateSystem();
			}
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
					mGeneralXHiFi = new V4ADSPModule(ID_V4A_GENERAL_XHIFI, 0x7FFF, 0);
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

			startForeground(ViPER4Android_XHiFi.NOTIFY_FOREGROUND_ID, new Notification());

			IntentFilter audioFilter = new IntentFilter();
			audioFilter.addAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
			audioFilter.addAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
			registerReceiver(mAudioSessionReceiver, audioFilter);

			final IntentFilter screenFilter = new IntentFilter();
			screenFilter.addAction(Intent.ACTION_SCREEN_ON);
		    registerReceiver(mScreenOnReceiver, screenFilter);

			final IntentFilter intentFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
			intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
			intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
			intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
			registerReceiver(mRoutingReceiver, intentFilter);

			registerReceiver(mPreferenceUpdateReceiver, new IntentFilter(ViPER4Android_XHiFi.ACTION_UPDATE_PREFERENCES));
			registerReceiver(mShowNotifyReceiver, new IntentFilter(ViPER4Android_XHiFi.ACTION_SHOW_NOTIFY));
			registerReceiver(mCancelNotifyReceiver, new IntentFilter(ViPER4Android_XHiFi.ACTION_CANCEL_NOTIFY));

			Log.i("ViPER4Android_XHiFi", "Service launched.");

			updateSystem();

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

			unregisterReceiver(mAudioSessionReceiver);
			unregisterReceiver(mScreenOnReceiver);
			unregisterReceiver(mRoutingReceiver);
			unregisterReceiver(mPreferenceUpdateReceiver);
			unregisterReceiver(mShowNotifyReceiver);
			unregisterReceiver(mCancelNotifyReceiver);

			CancelNotification();

			if (mGeneralXHiFi != null)
				mGeneralXHiFi.release();
			mGeneralXHiFi = null;

			Log.i("ViPER4Android_XHiFi", "Service destroyed.");
		}
		catch (Exception e)
		{
			CancelNotification();
		}
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return mBinder;
	}

	public void setEqualizerLevels(float[] levels)
	{
		mOverriddenEqualizerLevels = levels;
		updateSystem();
	}

	public String getAudioOutputRouting()
	{
		SharedPreferences prefSettings = getSharedPreferences(ViPER4Android_XHiFi.SHARED_PREFERENCES_BASENAME + ".settings", MODE_PRIVATE);
		String szLockedEffect = prefSettings.getString("viper4android.settings.lock_effect", "none");
		if (szLockedEffect.equalsIgnoreCase("none"))
		{
			if (mUseBluetooth) return "bluetooth";
			if (mUseHeadset) return "headset";
			return "speaker";
		}
		return szLockedEffect;
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

	protected void updateSystem()
	{
		String mode = getAudioOutputRouting();
		SharedPreferences preferences = getSharedPreferences(ViPER4Android_XHiFi.SHARED_PREFERENCES_BASENAME + "." + mode, 0);
		Log.i("ViPER4Android_XHiFi", "Begin system update(" + mode + ")");

		if (!mode.equalsIgnoreCase(mPreviousMode))
		{
			mPreviousMode = mode;
			if (mode.equalsIgnoreCase("headset"))
				ShowNotification(getString(getResources().getIdentifier("text_headset", "string", getApplicationInfo().packageName)));
			else if (mode.equalsIgnoreCase("bluetooth"))
				ShowNotification(getString(getResources().getIdentifier("text_bluetooth", "string", getApplicationInfo().packageName)));
			else ShowNotification(getString(getResources().getIdentifier("text_speaker", "string", getApplicationInfo().packageName)));
		}

		SharedPreferences prefSettings = getSharedPreferences(ViPER4Android_XHiFi.SHARED_PREFERENCES_BASENAME + ".settings", MODE_PRIVATE);
		String szCompatibleMode = prefSettings.getString("viper4android.settings.compatiblemode", "global");
		boolean mFXInLocalMode = false;
		if (szCompatibleMode.equals("global")) mFXInLocalMode = false;
		else mFXInLocalMode = true;

		Log.i("ViPER4Android_XHiFi", "<+++++++++++++++ Update global effect +++++++++++++++>");
		updateSystem_Global(preferences, mFXInLocalMode);
		Log.i("ViPER4Android_XHiFi", "<++++++++++++++++++++++++++++++++++++++++++++++++++++>");

		Log.i("ViPER4Android_XHiFi", "<+++++++++++++++ Update local effect +++++++++++++++>");
		updateSystem_Local(preferences, mFXInLocalMode);
		Log.i("ViPER4Android_XHiFi", "<+++++++++++++++++++++++++++++++++++++++++++++++++++>");
	}

	protected void updateSystem_Global(SharedPreferences preferences, boolean mLocalFX)
	{
		if ((mGeneralXHiFi == null) || (mGeneralXHiFi.mInstance == null) || (!mDriverIsReady))
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
				mGeneralXHiFi = new V4ADSPModule(ID_V4A_GENERAL_XHIFI, 0x7FFF, 0);
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
				mGeneralXHiFi = new V4ADSPModule(ID_V4A_GENERAL_XHIFI, 0x7FFF, 0);
				if ((mGeneralXHiFi == null) || (mGeneralXHiFi.mInstance == null)) return;
			}
			catch (Exception e)
			{
				return;
			}
		}

		if (mLocalFX) updateSystem_Module(preferences, mGeneralXHiFi, true);
		else updateSystem_Module(preferences, mGeneralXHiFi, false);
	}

	protected void updateSystem_Local(SharedPreferences preferences, boolean mLocalFX)
	{
		if ((mGeneralXHiFi == null) || (mGeneralXHiFi.mInstance == null) || (!mDriverIsReady))
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
			Log.i("ViPER4Android_XHiFi", "updateSystem(): Effects token lost!");
			return;
		}

		if (mV4AMutex.acquire())
		{
			for (Integer sessionId : new ArrayList<Integer>(mGeneralXHiFiList.keySet()))
			{
				try
				{
					V4ADSPModule v4aModule = mGeneralXHiFiList.get(sessionId);
					if (!mLocalFX) updateSystem_Module(preferences, v4aModule, true);
					else updateSystem_Module(preferences, v4aModule, false);
				}
				catch (Exception e)
				{
					Log.i("ViPER4Android_XHiFi", String.format("Trouble trying to manage session %d, removing...", sessionId), e);
					mGeneralXHiFiList.remove(sessionId);
					continue;
				}
			}
			mV4AMutex.release();
		}
		else Log.i("ViPER4Android_XHiFi", "Semaphore accquire failed.");
	}

	protected void updateSystem_Module(SharedPreferences preferences, V4ADSPModule v4aModule, boolean mMasterSwitchOff)
	{
		Log.i("ViPER4Android_XHiFi", "updateSystem(): Commiting effects type");

		/* Playback Auto Gain Control */
		Log.i("ViPER4Android_XHiFi", "updateSystem(): Updating Playback AGC.");
		v4aModule.setParameter_px4_vx4x1(PARAM_PLAYBACKGAIN_RATIO, Integer.valueOf(preferences.getString("viper4android.headphone_xhifi.playbackgain.ratio", "50")));
		v4aModule.setParameter_px4_vx4x1(PARAM_PLAYBACKGAIN_VOLUME, Integer.valueOf(preferences.getString("viper4android.headphone_xhifi.playbackgain.volume", "80")));
		v4aModule.setParameter_px4_vx4x1(PARAM_PLAYBACKGAIN_MAXSCALER, Integer.valueOf(preferences.getString("viper4android.headphone_xhifi.playbackgain.maxscaler", "400")));
		if (preferences.getBoolean("viper4android.headphone_xhifi.playbackgain.enable", false))
			v4aModule.setParameter_px4_vx4x1(PARAM_PLAYBACKGAIN_ENABLE, 1);
		else v4aModule.setParameter_px4_vx4x1(PARAM_PLAYBACKGAIN_ENABLE, 0);

		/* FIR Equalizer */
		Log.i("ViPER4Android_XHiFi", "updateSystem(): Updating FIR Equalizer.");
		if (mOverriddenEqualizerLevels != null)
		{
			for (int i = 0; i < mOverriddenEqualizerLevels.length; i++)
				SetV4AEqualizerBandLevel(i, (int)Math.round(Float.valueOf((mOverriddenEqualizerLevels[i]) / EqualizerSurface.MAX_DB) * 100), v4aModule);
		}
		else
		{
			String[] levels = preferences.getString("viper4android.headphone_xhifi.fireq.custom", "0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;").split(";");
			for (short i = 0; i < levels.length; i++)
				SetV4AEqualizerBandLevel(i, (int)Math.round((Float.valueOf(levels[i]) / EqualizerSurface.MAX_DB) * 100), v4aModule);
		}
		if (preferences.getBoolean("viper4android.headphone_xhifi.fireq.enable", false))
			v4aModule.setParameter_px4_vx4x1(PARAM_EQUALIZER_ENABLE, 1);
		else v4aModule.setParameter_px4_vx4x1(PARAM_EQUALIZER_ENABLE, 0);

		/* XFi Sound */
		Log.i("ViPER4Android_XHiFi", "updateSystem(): Updating XFi Sound.");
		v4aModule.setParameter_px4_vx4x1(PARAM_XFISOUND_PROCESS, Integer.valueOf(preferences.getString("viper4android.headphone_xhifi.xfisound.process", "110")));
		v4aModule.setParameter_px4_vx4x1(PARAM_XFISOUND_LOCONTOUR, Integer.valueOf(preferences.getString("viper4android.headphone_xhifi.xfisound.locontour", "0")));
		if (preferences.getBoolean("viper4android.headphone_xhifi.xfisound.enable", false))
			v4aModule.setParameter_px4_vx4x1(PARAM_XFISOUND_ENABLE, 1);
		else v4aModule.setParameter_px4_vx4x1(PARAM_XFISOUND_ENABLE, 0);

		/* Limiter */
		Log.i("ViPER4Android_XHiFi", "updateSystem(): Updating Limiter.");
		v4aModule.setParameter_px4_vx4x1(PARAM_OUTPUT_VOLUME, Integer.valueOf(preferences.getString("viper4android.headphone_xhifi.outvol", "100")));
		v4aModule.setParameter_px4_vx4x1(PARAM_LIMITER_THRESHOLD, Integer.valueOf(preferences.getString("viper4android.headphone_xhifi.limiter", "100")));

		/* Master Switch */
		boolean bMasterControl = preferences.getBoolean("viper4android.headphone_xhifi.enable", false);
		if (mMasterSwitchOff) bMasterControl = false;
		v4aModule.mInstance.setEnabled(bMasterControl);

		Log.i("ViPER4Android_XHiFi", "System updated.");
	}
}
