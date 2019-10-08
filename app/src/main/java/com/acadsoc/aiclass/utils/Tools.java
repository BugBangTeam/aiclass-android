package com.acadsoc.aiclass.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.inputmethod.InputMethodManager;
import com.google.gson.Gson;

import java.io.*;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.regex.Pattern;

public class Tools {

	public static PackageInfo getPackageInfo(Context ctx) {
		try {
			PackageManager pm = ctx.getPackageManager();
			PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_ACTIVITIES);
			if (pi != null) {
				return pi;
			}
		} catch (NameNotFoundException e) {
		}
		return null;
	}

	public static boolean hasSdcard() {
		String state = Environment.getExternalStorageState();
		if (state.equals(Environment.MEDIA_MOUNTED)) {
			return true;
		} else {
			return false;
		}
	}

	public static String getMetaData(Context ctx, String metaName) {
		String msg = "";
		try {
			ApplicationInfo appInfo = ctx.getPackageManager().getApplicationInfo(ctx.getPackageName(), PackageManager.GET_META_DATA);
			msg = String.valueOf(appInfo.metaData.getInt(metaName));
		} catch (NameNotFoundException e) {
		}
		return msg;
	}

	public static void hideSoftInput(Activity c) {
		InputMethodManager imm = (InputMethodManager) c.getSystemService(Activity.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.hideSoftInputFromWindow(c.getWindow().getDecorView().getWindowToken(), 0);
		}
	}

	public static int[] getViewSize(View v) {
		int widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		int heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		v.measure(widthMeasureSpec, heightMeasureSpec);
		return new int[] { v.getMeasuredWidth(), v.getMeasuredHeight() };
	}

	public static String sapi_GetStoragePath(Context ctx, String fileName) {
		String strStorageDirectory;
		if (hasSdcard()) {
			strStorageDirectory = Environment.getExternalStorageDirectory().toString() + "/" + fileName + "/";
			sapi_CreateDirectory(strStorageDirectory);
		} else {
			strStorageDirectory = sapi_GetFileStoragePath(ctx);
		}
		return strStorageDirectory;
	}

	public static void sapi_CreateDirectory(String szDir) {
		if (szDir != null) {
			File oDataDataDir = new File(szDir);
			if (!oDataDataDir.exists()) {
				oDataDataDir.mkdirs();
			}
		}
	}

	public static String sapi_GetFileStoragePath(Context ctx) {
		File fileDir = ctx.getFilesDir();
		String strStorageDirectory = fileDir.getParent() + File.separator + fileDir.getName() + File.separator;
		sapi_CreateDirectory(strStorageDirectory);
		return strStorageDirectory;
	}

	public static int dipToPx(Context c, float dipValue) {
		DisplayMetrics metrics = c.getResources().getDisplayMetrics();
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
	}

	public static int spToPx(Context context, float spValue) {
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue, metrics);
	}

	public static int dip2sp(Context c, float dipValue) {
		DisplayMetrics m = c.getResources().getDisplayMetrics();
		float fontScale = m.scaledDensity;
		float pxValue = dipToPx(c, dipValue);
		return (int) (pxValue / fontScale + 0.5f);
	}

	public static String getAndroidId(Context context) {
		return Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
	}

	public static boolean serializableObj(String path, Object obj, String name) {
		ObjectOutputStream oos = null;
		FileOutputStream fos = null;
		// AppUtils.e("--serializableObj--", "name:", obj.getClass().getName());
		try {
			File f = new File(path, name);
			if (f.exists()) {
				f.delete();
			}
			fos = new FileOutputStream(f);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(obj);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (oos != null) {
				try {
					oos.close();
				} catch (IOException e) {
				}
			}
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
				}
			}
		}
		return false;
	}

	public static boolean serializableObj(String path, Object obj) {
		return serializableObj(path, obj, obj.getClass().getName());
	}

	public static <T> T getSerializableObj(String path, String name) {
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try {
			File f = new File(path, name);
			if (f.exists()) {
				fis = new FileInputStream(f);
				ois = new ObjectInputStream(fis);
				return (T) ois.readObject();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
				}
			}
			if (ois != null) {
				try {
					ois.close();
				} catch (IOException e) {
				}
			}
		}
		return null;
	}

	public static <T> T getJsonObject(Context ctx, Class<T> cls, String jsonName) {
		return getJsonObject(ctx, (Type) cls, jsonName);
	}

	public static <T> T getJsonObject(Context ctx, Type cls, String jsonName) {
		try {
			InputStream is = ctx.getAssets().open(jsonName);
			int size = is.available();
			byte[] buffer = new byte[size];
			is.read(buffer);
			is.close();
			String text = new String(buffer, "UTF-8");
			Gson gson = new Gson();
			return gson.fromJson(text, cls);
		} catch (IOException e) {
		}
		return null;
	}

	private static long lastClickTime;

	/**
	 * 两次点击 时间间隔控制
	 * 
	 * @param click
	 *            两次点击间隔时间
	 * @return
	 */
	public static boolean isFastDoubleClick(long click) {
		long time = System.currentTimeMillis();
		long timeD = time - lastClickTime;
		if (0 < timeD && timeD < click) {
			return true;
		}
		lastClickTime = time;
		return false;
	}

	public static void setBackground(View view, Drawable background) {
		if (Build.VERSION.SDK_INT >= 16) {
			view.setBackground(background);
		} else {
			view.setBackgroundDrawable(background);
		}
	}

	/**
	 * 获取当前ip地址
	 * 
	 * @param context
	 * @return
	 */
	public static String getLocalIpAddress(Context context) {
		try {

			WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			int i = wifiInfo.getIpAddress();
			return int2ip(i);
		} catch (Exception ex) {
			return " 获取IP出错!!!!请保证是WIFI,或者请重新打开网络!\n" + ex.getMessage();
		}
		// return null;
	}

	/**
	 * 将ip的整数形式转换成ip形式
	 * 
	 * @param ipInt
	 * @return
	 */
	public static String int2ip(int ipInt) {
		StringBuilder sb = new StringBuilder();
		sb.append(ipInt & 0xFF).append(".");
		sb.append((ipInt >> 8) & 0xFF).append(".");
		sb.append((ipInt >> 16) & 0xFF).append(".");
		sb.append((ipInt >> 24) & 0xFF);
		return sb.toString();
	}
	
	public static String getWifiSSID(Context context) {
		WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wifiManager.getConnectionInfo();
		if (info != null) {
			return info.getSSID().replace("\"", "");
		}
		return "";
	}

	// 定义几种加密方式，一种是WEP，一种是WPA，还有没有密码的情况
	public enum WifiCipherType {
		WIFICIPHER_WEP, WIFICIPHER_WPA, WIFICIPHER_NOPASS, WIFICIPHER_INVALID
	}

	public static WifiConfiguration createWifiInfo(String SSID, String Password, WifiCipherType Type) {
		WifiConfiguration config = new WifiConfiguration();
		config.allowedAuthAlgorithms.clear();
		config.allowedGroupCiphers.clear();
		config.allowedKeyManagement.clear();
		config.allowedPairwiseCiphers.clear();
		config.allowedProtocols.clear();
		config.SSID = "\"" + SSID + "\"";
		// nopass
		if (Type == WifiCipherType.WIFICIPHER_NOPASS) {
			config.wepKeys[0] = "";
			config.allowedKeyManagement.set(KeyMgmt.NONE);
			config.wepTxKeyIndex = 0;
		}
		// wep
		if (Type == WifiCipherType.WIFICIPHER_WEP) {
			if (!TextUtils.isEmpty(Password)) {
				if (isHexWepKey(Password)) {
					config.wepKeys[0] = Password;
				} else {
					config.wepKeys[0] = "\"" + Password + "\"";
				}
			}
			config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
			config.allowedAuthAlgorithms.set(AuthAlgorithm.SHARED);
			config.allowedKeyManagement.set(KeyMgmt.NONE);
			config.wepTxKeyIndex = 0;
		}
		// wpa
		if (Type == WifiCipherType.WIFICIPHER_WPA) {
			config.preSharedKey = "\"" + Password + "\"";
			config.hiddenSSID = true;
			config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
			config.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
			config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
			// 此处需要修改否则不能自动重联
			config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
			config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
			config.status = WifiConfiguration.Status.ENABLED;
		}
		return config;
	}

	private static boolean isHexWepKey(String wepKey) {
		final int len = wepKey.length();
		if (len != 10 && len != 26 && len != 58) {
			return false;
		}

		return isHex(wepKey);
	}

	private static boolean isHex(String key) {
		for (int i = key.length() - 1; i >= 0; i--) {
			final char c = key.charAt(i);
			if (!(c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f')) {
				return false;
			}
		}
		return true;
	}

	public static int f2i(float f) {
		return (int) (f + 0.5);
	}

	public static boolean isPhoneNO(String phone, String phoneRegex) {
		if (TextUtils.isEmpty(phone))
			return false;
		Pattern p = Pattern.compile(phoneRegex);
		return p.matcher(phone).matches();
	}

	private static ThreadLocal<SimpleDateFormat> t1 = new ThreadLocal<>();

	public static SimpleDateFormat getSimpleDateFormat(String datePattern) {
		SimpleDateFormat sdf = t1.get();
		if (sdf == null) {
			sdf = new SimpleDateFormat(datePattern, Locale.SIMPLIFIED_CHINESE);
			t1.set(sdf);
		} else {
			sdf.applyPattern(datePattern);
		}
		return sdf;
	}

	public static boolean checkApkFile(Activity ctx, String filePath) {
		boolean result = false;
		try {
			PackageManager pm = ctx.getPackageManager();
			PackageInfo info = pm.getPackageArchiveInfo(filePath, PackageManager.GET_ACTIVITIES);
			if (info != null) {
				result = true;
			}
		} catch (Exception e) {
			result = false;
		}
		return result;
	}

}
