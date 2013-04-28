package android.privacy.surrogate;

import java.net.InetAddress;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.os.Binder;
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
 * Provides privacy handling for phone
 * @author CollegeDev (Stefan T.)
 * {@hide}
 */
public class PrivacyConnectivityManager extends ConnectivityManager{

	private static final String P_TAG = "PrivacyConnectivityManager";
	
	private Context context;
	
	private PrivacySettingsManager pSetMan;
	
	public PrivacyConnectivityManager(IConnectivityManager service, Context context) {
		super(service);
		this.context = context;
		pSetMan = new PrivacySettingsManager(context, IPrivacySettingsManager.Stub.asInterface(ServiceManager.getService("privacy")));
		PrivacyDebugger.i(P_TAG,"now in constructor for package: " + context.getPackageName());
	}
	
	@Override
	public boolean getMobileDataEnabled() {
		PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
		if(pSetMan != null && settings != null && settings.getForceOnlineState() == PrivacySettings.REAL){
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null); 
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.EMPTY, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null); 
			return true;
		}
//		} else if(pSetMan != null && settings != null && settings.getNetworkInfoSetting() != PrivacySettings.REAL){
//			pSetMan.notification(context.getPackageName(),-1, PrivacySettings.EMPTY, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);  
//			return false;
//		}
		else {
			if(settings != null && settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);  
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.REAL, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);  
			return super.getMobileDataEnabled();
		}
			
	}
	
	@Override
	public void setMobileDataEnabled(boolean enabled) {
		PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
		if(pSetMan != null && settings != null && settings.getSwitchConnectivitySetting() != PrivacySettings.REAL){
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_SWITCH_CONNECTIVITY, null, null);
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.EMPTY, PrivacySettings.DATA_SWITCH_CONNECTIVITY, null, null);
			//do nothing
		} else{
			if(settings != null && settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_SWITCH_CONNECTIVITY, null, null);
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.REAL, PrivacySettings.DATA_SWITCH_CONNECTIVITY, null, null);
			super.setMobileDataEnabled(enabled);
		}
	}
	
	@Override
	public NetworkInfo[] getAllNetworkInfo() {
		PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
		NetworkInfo output[] =  {new NetworkInfo(TYPE_MOBILE, 0, "MOBILE", "CONNECTED")};
		boolean forceOnline = false;
		if(pSetMan != null && settings != null && settings.getForceOnlineState() == PrivacySettings.REAL){
			output[0].setIsAvailable(true); 
			output[0].setState(NetworkInfo.State.CONNECTED);
			forceOnline = true;
		}
		if(pSetMan != null && settings != null && settings.getNetworkInfoSetting() != PrivacySettings.REAL) {
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);  
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.EMPTY, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);  
			return output;
		}
		else{
			if(settings != null && settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.REAL, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
			output = super.getAllNetworkInfo();
			if(forceOnline)
				for(int i = 0; i < output.length; i++) {
					output[i].setIsAvailable(true);
					output[i].setState(NetworkInfo.State.CONNECTED);
				}
			return output;
			//return super.getAllNetworkInfo();
		}
			
	}
	
	@Override
	public NetworkInfo getNetworkInfo(int networkType) {
		PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
		NetworkInfo output =  new NetworkInfo(TYPE_MOBILE, 0, "MOBILE", "CONNECTED");
		boolean forceOnline = false;
		if(pSetMan != null && settings != null && settings.getForceOnlineState() == PrivacySettings.REAL){
			output.setIsAvailable(true);
			output.setState(NetworkInfo.State.CONNECTED);
			forceOnline = true;
		}
		if(pSetMan != null && settings != null && settings.getNetworkInfoSetting() != PrivacySettings.REAL){
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null); 
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.EMPTY, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);  
			return output;
		}
		else {
			if(settings != null && settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.REAL, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
			output = super.getNetworkInfo(networkType);
			if(forceOnline) {
				output.setIsAvailable(true);
				output.setState(NetworkInfo.State.CONNECTED);
			}
			return output;
		}
	}
	
	/**
	 * {@hide}
	 */
	@Override
	public NetworkInfo getActiveNetworkInfoForUid(int uid) {
		PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
		NetworkInfo output =  new NetworkInfo(TYPE_MOBILE, 0, "MOBILE", "UNKNOWN");
		boolean forceOnline = false;
		if(pSetMan != null && settings != null && settings.getForceOnlineState() == PrivacySettings.REAL){
			output.setIsAvailable(true);
			output.setState(NetworkInfo.State.CONNECTED);
			forceOnline = true;
		}
		
		if(pSetMan != null && settings != null && settings.getNetworkInfoSetting() != PrivacySettings.REAL){
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null); 
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.EMPTY, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
			return output;
		}
		else{
			if(settings != null && settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null); 
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.REAL, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
			output = super.getActiveNetworkInfoForUid(uid);
			if(forceOnline) {
				output.setIsAvailable(true);
				output.setState(NetworkInfo.State.CONNECTED);
			}
			return output;
		}
			
	}
	
	@Override
	public NetworkInfo getActiveNetworkInfo() {
		PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
		NetworkInfo output =  new NetworkInfo(TYPE_MOBILE, 0, "MOBILE", "UNKNOWN");
		boolean forceOnline = false;
		if(pSetMan != null && settings != null && settings.getForceOnlineState() == PrivacySettings.REAL) {
			output.setIsAvailable(true);
			output.setState(NetworkInfo.State.CONNECTED);
		}
		
		if(pSetMan != null && settings != null && settings.getNetworkInfoSetting() != PrivacySettings.REAL) {
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);  
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.EMPTY, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);  
			return output;
		}
		else { 
			if(settings != null && settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null); 
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.REAL, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null); 
			output = super.getActiveNetworkInfo();
			if(forceOnline) {
				output.setIsAvailable(true);
				output.setState(NetworkInfo.State.CONNECTED);
			}	
			return output;
		}
			
	}
	
	@Override
	public LinkProperties getLinkProperties(int networkType) { //method to prevent getting device IP
		LinkProperties output = new LinkProperties();
		PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
		if(pSetMan != null && settings != null && settings.getNetworkInfoSetting() != PrivacySettings.REAL) {
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null); 
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.EMPTY, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null); 
			return output;
		}
		else{
			if(settings != null && settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null); 
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.REAL, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null); 
			return super.getLinkProperties(networkType);
		}
	}
	
	public LinkProperties getActiveLinkProperties() { //also for prevent getting device IP
		LinkProperties output = new LinkProperties();
		PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
		if(pSetMan != null && settings != null && settings.getNetworkInfoSetting() != PrivacySettings.REAL){
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);  
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.EMPTY, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);  
			return output;
		}
		else {
			if(settings != null && settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null); 
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.REAL, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null); 
			return super.getActiveLinkProperties();
		}
	}
	
	@Override
	public boolean requestRouteToHost(int networkType, int hostAddress){
		PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
		if(pSetMan != null && settings != null && settings.getForceOnlineState() == PrivacySettings.REAL) {
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);  
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.EMPTY, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);  
			return true;
		} else if(pSetMan != null && settings != null && settings.getNetworkInfoSetting() != PrivacySettings.REAL) {
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);  
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.EMPTY, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null); 
			return false;
		} else {
			if(settings != null && settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null); 
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.REAL, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null); 
			return super.requestRouteToHost(networkType, hostAddress);
		}
	}
	
	@Override
	public boolean requestRouteToHostAddress(int networkType, InetAddress hostAddress){
		PrivacySettings settings = pSetMan.getSettings(context.getPackageName());
		if(pSetMan != null && settings != null && settings.getForceOnlineState() == PrivacySettings.REAL){
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null); 
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.EMPTY, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
			return true;
		} else if(pSetMan != null && settings != null && settings.getNetworkInfoSetting() != PrivacySettings.REAL){
			if(settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);  
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.EMPTY, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);  
			return false;
		} else{
			if(settings != null && settings.isDefaultDenyObject())
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null); 
			else
				pSetMan.notification(context.getPackageName(),-1, PrivacySettings.REAL, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null); 
			return super.requestRouteToHostAddress(networkType, hostAddress);
		}
	}
	

}
