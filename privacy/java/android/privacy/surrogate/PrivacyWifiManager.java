package android.privacy.surrogate;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.IWifiManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.ServiceManager;
import android.privacy.IPrivacySettingsManager;
import android.privacy.PrivacySettings;
import android.privacy.PrivacySettingsManager;
import android.privacy.utilities.PrivacyDebugger;
import android.util.Log;

/**
 * Copyright (C) 2012-2013 Stefan Thiele (CollegeDev)
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see <http://www.gnu.org/licenses>.
 */

/**
 * Provides privacy handling for WifiManager
 * @author CollegeDev (Stefan T.)
 * {@hide}
 */
public class PrivacyWifiManager extends WifiManager{

	private Context context;
	
	private PrivacySettingsManager pSetMan;
	
	private static final String TAG = "PrivacyWifiManager";
	

	public PrivacyWifiManager(IWifiManager service, Context context){
		super(context,service);
		this.context = context;
		pSetMan = new PrivacySettingsManager(context, IPrivacySettingsManager.Stub.asInterface(ServiceManager.getService("privacy")));
	}
	
	@Override
	public List<WifiConfiguration> getConfiguredNetworks() {
		PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
		List<WifiConfiguration> output = new ArrayList<WifiConfiguration>(); //create empty list!
		if(pSetMan != null && settings != null && settings.getWifiInfoSetting() != PrivacySettings.REAL) {
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_WIFI_INFO, null, null);   
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.EMPTY, PrivacySettings.DATA_WIFI_INFO, null, null); 
		} else {
			output = super.getConfiguredNetworks();
			if(settings != null && settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_WIFI_INFO, null, null); 
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.REAL, PrivacySettings.DATA_WIFI_INFO, null, null); 
		}
		PrivacyDebugger.d(TAG, "getConfiguredNetworks - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: " + ((output != null) ? output : "null"));
		return output;
	}
	
	@Override
	public WifiInfo getConnectionInfo() {
		PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
		WifiInfo output = new WifiInfo(true);
		if(pSetMan != null && settings != null && settings.getWifiInfoSetting() != PrivacySettings.REAL) { 
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_WIFI_INFO, null, null); 
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.EMPTY, PrivacySettings.DATA_WIFI_INFO, null, null); 
		} else {
			output = super.getConnectionInfo();
			if(settings != null && settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_WIFI_INFO, null, null);
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.REAL, PrivacySettings.DATA_WIFI_INFO, null, null);
		}
		PrivacyDebugger.d(TAG, "getConnectionInfo - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: " + ((output != null) ? output : "null"));
		return output;
	}
	
	@Override
	public List<ScanResult> getScanResults() {
		PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
		List<ScanResult> output = new ArrayList<ScanResult>(); //create empty list!
		if(pSetMan != null && settings != null && settings.getWifiInfoSetting() != PrivacySettings.REAL) {
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_WIFI_INFO, null, null);  
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.EMPTY, PrivacySettings.DATA_WIFI_INFO, null, null);  
			
		} else {
			output = super.getScanResults();
			if(settings != null && settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_WIFI_INFO, null, null); 
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.REAL, PrivacySettings.DATA_WIFI_INFO, null, null); 
		}
		PrivacyDebugger.d(TAG, "getScanResults - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: " + ((output != null) ? output : "null"));
		return output;
	}
	
	@Override
	public int getFrequencyBand() {
		PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
		int output = -1;
		if(pSetMan != null && settings != null && settings.getWifiInfoSetting() != PrivacySettings.REAL) {
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_WIFI_INFO, null, null);
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.EMPTY, PrivacySettings.DATA_WIFI_INFO, null, null);
		} else {
			output = super.getFrequencyBand();
			if(settings != null && settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_WIFI_INFO, null, null); 
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.REAL, PrivacySettings.DATA_WIFI_INFO, null, null); 
		}
		PrivacyDebugger.d(TAG, "getFrequencyBand - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: " + output);
		return output;
	}
	
	@Override
	public DhcpInfo getDhcpInfo(){
		PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
		DhcpInfo output = new DhcpInfo();
		if(pSetMan != null && settings != null && settings.getWifiInfoSetting() != PrivacySettings.REAL) {
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_WIFI_INFO, null, null);
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.EMPTY, PrivacySettings.DATA_WIFI_INFO, null, null);
			
		} else {
			output = super.getDhcpInfo();
			if(settings != null && settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_WIFI_INFO, null, null); 
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.REAL, PrivacySettings.DATA_WIFI_INFO, null, null); 
		}
		PrivacyDebugger.d(TAG, "getDhcpInfo - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: " + ((output != null) ? output : "null"));
		return output;
	}
	
	/**
	 * @hide
	 * @return
	 */
	@Override
	public WifiConfiguration getWifiApConfiguration(){
		PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
		WifiConfiguration output = new WifiConfiguration();
		if(pSetMan != null && settings != null && settings.getWifiInfoSetting() != PrivacySettings.REAL) {
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_WIFI_INFO, null, null);
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.EMPTY, PrivacySettings.DATA_WIFI_INFO, null, null);
		} else {
			output = super.getWifiApConfiguration();
			if(settings != null && settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_WIFI_INFO, null, null); 
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.REAL, PrivacySettings.DATA_WIFI_INFO, null, null);
		}
		PrivacyDebugger.d(TAG, "getWifiApConfiguration - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: " + ((output != null) ? output : "null"));
		return output;
	}
	

	@Override
	public String getConfigFile() {
		PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
		String output = "";
		if(pSetMan != null && settings != null && settings.getWifiInfoSetting() != PrivacySettings.REAL) {
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_WIFI_INFO, null, null);
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.EMPTY, PrivacySettings.DATA_WIFI_INFO, null, null);
			
		} else {
			output = super.getConfigFile();
			if(settings != null && settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_WIFI_INFO, null, null); 
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.REAL, PrivacySettings.DATA_WIFI_INFO, null, null); 
		}
		PrivacyDebugger.d(TAG, "getConfigFile - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: " + ((output != null) ? output : "null"));
		return output;
	}

	
	@Override
	public boolean startScan(){
		PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
		boolean output = false;
		if(pSetMan != null && settings != null && settings.getWifiInfoSetting() != PrivacySettings.REAL) {
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_WIFI_INFO, null, null);  
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.EMPTY, PrivacySettings.DATA_WIFI_INFO, null, null);  
		} else {
			output = super.startScan();
			if(settings != null && settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_WIFI_INFO, null, null);
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.REAL, PrivacySettings.DATA_WIFI_INFO, null, null);
		}
		PrivacyDebugger.d(TAG, "startScan - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: " +  output);
		return output;
	}
	
	
	@Override
	public boolean startScanActive(){
		PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
		boolean output = false;
		if(pSetMan != null && settings != null && settings.getWifiInfoSetting() != PrivacySettings.REAL) {
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_WIFI_INFO, null, null);  
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.EMPTY, PrivacySettings.DATA_WIFI_INFO, null, null);  
			
		} else {
			output = super.startScanActive();
			if(settings != null && settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_WIFI_INFO, null, null); 
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.REAL, PrivacySettings.DATA_WIFI_INFO, null, null); 
		}
		PrivacyDebugger.d(TAG, "startScanActive - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: " +  output);
		return output;
	}
	
	@Override
	public boolean setWifiEnabled(boolean enabled){
		PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
		boolean output = false;
		if(pSetMan != null && settings != null && settings.getSwitchWifiStateSetting() != PrivacySettings.REAL) {
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_SWITCH_WIFI_STATE, null, null);  
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.EMPTY, PrivacySettings.DATA_SWITCH_WIFI_STATE, null, null);  
		} else {
			output = super.setWifiEnabled(enabled);
			if(settings != null && settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_SWITCH_WIFI_STATE, null, null); 
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.REAL, PrivacySettings.DATA_SWITCH_WIFI_STATE, null, null); 
		}
		PrivacyDebugger.d(TAG, "setWifiEnabled - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: " +  output);
		return output;
	}
	
	@Override
	public int getWifiState() {
		PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
		if(pSetMan != null && settings != null && settings.getForceOnlineState() == PrivacySettings.REAL) {
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_WIFI_INFO, null, null);  
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.EMPTY, PrivacySettings.DATA_WIFI_INFO, null, null); 
			PrivacyDebugger.d(TAG, "getWifiState - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: WIFI_STATE_ENABLED");
			return WIFI_STATE_ENABLED;
		} else if(pSetMan != null && settings != null && settings.getWifiInfoSetting() != PrivacySettings.REAL) {
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_WIFI_INFO, null, null); 
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.EMPTY, PrivacySettings.DATA_WIFI_INFO, null, null); 
			PrivacyDebugger.d(TAG, "getWifiState - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: WIFI_STATE_UNKNOWN");
			return WIFI_STATE_UNKNOWN;
		} else {
			if(settings != null && settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_WIFI_INFO, null, null);  
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.REAL, PrivacySettings.DATA_WIFI_INFO, null, null);
			PrivacyDebugger.d(TAG, "getWifiState - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: REAL_VALUE");
			return super.getWifiState();
		}
	}
	
	@Override
	public boolean isWifiEnabled() {
		PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
		if(pSetMan != null && settings != null && settings.getForceOnlineState() == PrivacySettings.REAL) {
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_WIFI_INFO, null, null);  
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.EMPTY, PrivacySettings.DATA_WIFI_INFO, null, null);  
			PrivacyDebugger.d(TAG, "isWifiEnabled - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: true");
			return true;
		} else if(pSetMan != null && settings != null && settings.getWifiInfoSetting() != PrivacySettings.REAL) {
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_WIFI_INFO, null, null);  
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.EMPTY, PrivacySettings.DATA_WIFI_INFO, null, null);  
			PrivacyDebugger.d(TAG, "isWifiEnabled - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: false");
			return false;
		} else {
			if(settings != null && settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_WIFI_INFO, null, null);
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.REAL, PrivacySettings.DATA_WIFI_INFO, null, null);
			PrivacyDebugger.d(TAG, "isWifiEnabled - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: REAL_VALUE");
			return super.isWifiEnabled();
		}
	}
}
