package android.privacy.surrogate;

import android.content.Context;
import android.os.Binder;
import android.os.ServiceManager;
import android.privacy.IPrivacySettingsManager;
import android.privacy.PrivacySettings;
import android.privacy.PrivacySettingsManager;
import android.privacy.utilities.PrivacyDebugger;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.os.Process;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.telephony.CellInfo;

/**
 * Copyright (C) 2012 Svyatoslav Hresyk
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
 * Provides privacy handling for {@link android.telephony.TelephonyManager}
 * @author Svyatoslav Hresyk, modified & improved by CollegeDev (Stefan T.)
 * {@hide}
 */
public final class PrivacyTelephonyManager extends TelephonyManager {

    private static final String TAG = "PrivacyTelephonyManager";
    
    private Context context;
    
    private PrivacySettingsManager pSetMan;
    
    /** {@hide} */
    public PrivacyTelephonyManager(Context context) {
        super(context);
        this.context = context;
//        pSetMan = (PrivacySettingsManager) context.getSystemService("privacy");
        // don't call getSystemService to avoid getting java.lang.IllegalStateException: 
        // System services not available to Activities before onCreate()
        pSetMan = new PrivacySettingsManager(context, IPrivacySettingsManager.Stub.asInterface(ServiceManager.getService("privacy")));
    }
    
    /**
     * IMEI
     */
    @Override
    public String getDeviceId() {
        String packageName = context.getPackageName();
        PrivacySettings pSet = pSetMan.getSettings(packageName);
        String output;
        if (pSet != null && pSet.getDeviceIdSetting() != PrivacySettings.REAL) {
            output = pSet.getDeviceId(); // can be empty, custom or random
            if(pSet.isDefaultDenyObject())
            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_DEVICE_ID, output, pSet);
            else
            	pSetMan.notification(packageName, 0, pSet.getDeviceIdSetting(), PrivacySettings.DATA_DEVICE_ID, output, pSet);
        } else {
            output = super.getDeviceId();
            if(pSet != null && pSet.isDefaultDenyObject())
            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_DEVICE_ID, output, pSet);
            else
            	pSetMan.notification(packageName, 0, PrivacySettings.REAL, PrivacySettings.DATA_DEVICE_ID, output, pSet);
        }
        PrivacyDebugger.d(TAG, "getDeviceId - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: " + output);
        return output;
    }
    
    /**
     * Phone number
     */
    @Override
    public String getLine1Number() {
        String packageName = context.getPackageName();
        PrivacySettings pSet = pSetMan.getSettings(packageName);

        String output;
        if (pSet != null && pSet.getLine1NumberSetting() != PrivacySettings.REAL) {
            output = pSet.getLine1Number(); // can be empty, custom or random
            if(pSet.isDefaultDenyObject())
            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_LINE_1_NUMBER, output, pSet);
            else
            	pSetMan.notification(packageName, 0, pSet.getLine1NumberSetting(), PrivacySettings.DATA_LINE_1_NUMBER, output, pSet);
        } else {
            output = super.getLine1Number();
            if(pSet != null && pSet.isDefaultDenyObject())
            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_LINE_1_NUMBER, output, pSet);
            else
            	pSetMan.notification(packageName, 0, PrivacySettings.REAL, PrivacySettings.DATA_LINE_1_NUMBER, output, pSet);
        }
        PrivacyDebugger.d(TAG, "getLine1Number - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: " + output);
        return output;
    }
    
    /**
     * Will be handled like the Line1Number, since voice mailbox numbers often
     * are similar to the phone number of the subscriber.
     */
    @Override
    public String getVoiceMailNumber() {
        String packageName = context.getPackageName();
        PrivacySettings pSet = pSetMan.getSettings(packageName);
        String output;
        if (pSet != null && pSet.getLine1NumberSetting() != PrivacySettings.REAL) {
            output = pSet.getLine1Number(); // can be empty, custom or random
            if(pSet.isDefaultDenyObject())
            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_LINE_1_NUMBER, output, pSet);
            else
            	pSetMan.notification(packageName, 0, pSet.getLine1NumberSetting(), PrivacySettings.DATA_LINE_1_NUMBER, output, pSet);
        } else {
            output = super.getVoiceMailNumber();
            if(pSet != null && pSet.isDefaultDenyObject())
            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_LINE_1_NUMBER, output, pSet);
            else
            	pSetMan.notification(packageName, 0, PrivacySettings.REAL, PrivacySettings.DATA_LINE_1_NUMBER, output, pSet);
        }
        PrivacyDebugger.d(TAG, "getVoiceMailNumber - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: " + output);
        return output;
    }
    
    /**
     * Intercept requests for mobile network cell information. This can be used for tracking network
     * based location.
     */
    @Override
    public List<NeighboringCellInfo> getNeighboringCellInfo() {
        PrivacySettings pSet = pSetMan.getSettings(context.getPackageName());
        List<NeighboringCellInfo> output = null;
        String output_label = "[null]";
        
        if (pSet != null) {
            if (pSet.getLocationNetworkSetting() == PrivacySettings.EMPTY) {
                // output = null;
            } else if (pSet.getLocationNetworkSetting() != PrivacySettings.REAL) {
                output = new ArrayList<NeighboringCellInfo>();
                output_label = "[empty list of cells]";
            } else {
                output = super.getNeighboringCellInfo();
                String cells = "";
                for (NeighboringCellInfo i : output) cells += "\t" + i + "\n";
                output_label = "[real value]:\n" + cells;
            }
        }
        
        PrivacyDebugger.d(TAG, "getNeighboringCellInfo - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: " + output_label);
        return output;
    }
    
    @Override
    public String getNetworkCountryIso() {
        String output = getNetworkInfo();
        if (output == null) output = super.getNetworkCountryIso();
        PrivacyDebugger.d(TAG, "getNetworkCountryIso - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: " + output);
        return output;
    }

    @Override
    public String getNetworkOperator() {
        String output = getNetworkInfo();
        if (output == null) output = super.getNetworkOperator();
        PrivacyDebugger.d(TAG, "getNetworkOperator - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: " + output);
        return output;
    }

    @Override
    public String getNetworkOperatorName() {
        String output = getNetworkInfo();
        if (output == null) output = super.getNetworkOperatorName();
        PrivacyDebugger.d(TAG, "getNetworkOperatorName - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: " + output);
        return output;
    }
    
    /**
     * Handles following Network Information requests: CountryIso, Operator Code, Operator Name
     * @return value to return if applicable or null if real value should be returned
     */
    private String getNetworkInfo() {
        String packageName = context.getPackageName();
        PrivacySettings pSet = pSetMan.getSettings(packageName);
        if (pSet != null && pSet.getNetworkInfoSetting() != PrivacySettings.REAL) {
        	if(pSet.isDefaultDenyObject())
        		pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, pSet);         
        	else
        		pSetMan.notification(packageName, 0, PrivacySettings.EMPTY, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, pSet);     
            return ""; // can only be empty
        } else {
        	if(pSet != null && pSet.isDefaultDenyObject())
        		pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, pSet);  
        	else
        		pSetMan.notification(packageName, 0, PrivacySettings.REAL, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, pSet);  
            return null;
        }        
    }
    
    @Override
    public String getSimCountryIso() {
        String output = getSimInfo();
        if (output == null) output = super.getSimCountryIso();
        PrivacyDebugger.d(TAG, "getSimCountryIso - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: " + output);
        return output;
    }

    @Override
    public String getSimOperator() {
        String output = getSimInfo();
        if (output == null) output = super.getSimOperator();
        PrivacyDebugger.d(TAG, "getSimOperator - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: " + output);
        return output;
    }

    @Override
    public String getSimOperatorName() {
        String output = getSimInfo();
        if (output == null) output = super.getSimOperatorName();
        PrivacyDebugger.d(TAG, "getSimOperatorName - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: " + output);
        return output;
    }
    
    /**
     * Handles following SIM Card information requests: CountryIso, Operator Code, Operator Name
     * @return value to return if applicable or null if real value should be returned
     */    
    private String getSimInfo() {
        String packageName = context.getPackageName();
        PrivacySettings pSet = pSetMan.getSettings(packageName);
        if (pSet != null && pSet.getSimInfoSetting() != PrivacySettings.REAL) {
        	if(pSet.isDefaultDenyObject())
        		pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_SIM, null, pSet);   
        	else
        		pSetMan.notification(packageName, 0, PrivacySettings.EMPTY, PrivacySettings.DATA_NETWORK_INFO_SIM, null, pSet);   
            return ""; // can only be empty
        } else {
        	if(pSet != null && pSet.isDefaultDenyObject())
        		pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_SIM, null, pSet);
        	else
        		pSetMan.notification(packageName, 0, PrivacySettings.REAL, PrivacySettings.DATA_NETWORK_INFO_SIM, null, pSet);
            return null;
        }                
    }
    
    /**
     * ICCID
     */
    @Override
    public String getSimSerialNumber() {
        String packageName = context.getPackageName();
        PrivacySettings pSet = pSetMan.getSettings(packageName);
        String output;
        if (pSet != null && pSet.getSimSerialNumberSetting() != PrivacySettings.REAL) {
            output = pSet.getSimSerialNumber(); // can be empty, custom or random
            if(pSet.isDefaultDenyObject())
            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_SIM_SERIAL, output, pSet); 
            else
            	pSetMan.notification(packageName, 0, pSet.getSimSerialNumberSetting(), PrivacySettings.DATA_SIM_SERIAL, output, pSet); 
        } else {
            output = super.getSimSerialNumber();
            if(pSet != null && pSet.isDefaultDenyObject())
            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_SIM_SERIAL, output, pSet);        
            else
            	pSetMan.notification(packageName, 0, PrivacySettings.REAL, PrivacySettings.DATA_SIM_SERIAL, output, pSet);     
        }
        PrivacyDebugger.d(TAG, "getSimSerialNumber - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: " + output);
        return output;
    }
    
    public String getSafeSubscriberId() {
    	PrivacyDebugger.i(TAG, "getSafeSubscriberId()", context.getPackageName());
    	return "1817209745362786";
    }
    
    /**
     * IMSI
     */
    @Override
    public String getSubscriberId() {
        String packageName = context.getPackageName();
        PrivacySettings pSet = pSetMan.getSettings(packageName);
        String output;
        if (pSet != null && pSet.getSubscriberIdSetting() != PrivacySettings.REAL) {
            output = pSet.getSubscriberId(); // can be empty, custom or random
            if(pSet.isDefaultDenyObject())
            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_SUBSCRIBER_ID, output, pSet);   
            else
            	pSetMan.notification(packageName, 0, pSet.getSubscriberIdSetting(), PrivacySettings.DATA_SUBSCRIBER_ID, output, pSet); 
        } else {
        	output = super.getSubscriberId();
        	if(pSet != null && pSet.isDefaultDenyObject())
        		pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_SUBSCRIBER_ID, output, pSet);  
        	else
        		pSetMan.notification(packageName, 0, PrivacySettings.REAL, PrivacySettings.DATA_SUBSCRIBER_ID, output, pSet);
        }
        PrivacyDebugger.d(TAG, "getSubscriberId - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: " + output);
        return output;
    }

    /**
     * For monitoring purposes only
     */    
    @Override
    public void enableLocationUpdates() {
        PrivacyDebugger.d(TAG, "enableLocationUpdates - " + context.getPackageName() + " (" + Binder.getCallingUid() + ")");
        super.enableLocationUpdates();
    }

    @Override
    public void listen(PhoneStateListener listener, int events) {
        PrivacyDebugger.d(TAG, "listen - package:" + context.getPackageName() + " uid:" + Binder.getCallingUid() + " events: " + events);
        if (((events & PhoneStateListener.LISTEN_CELL_LOCATION) != 0) || ((events & PhoneStateListener.LISTEN_CALL_STATE) != 0)) {
		    String pkgForDebug = context != null ? context.getPackageName() : null;
		    PrivacyDebugger.i(TAG,"initiate listening. Context: " + ((context != null) ? "available" : "NULL") + " listener: " + ((listener != null) ? "available" : "NULL"));
		    if(pkgForDebug != null){
				try{
		            	listener.setPackageName(pkgForDebug);
		            	listener.setContext(context);
				} catch (NullPointerException e){
					PrivacyDebugger.w(TAG, "catched nullPointerException - listen()", e);
				}
	        }
	        super.listen(listener, events);
	        PrivacyDebugger.d(TAG, "listen for cell location or call state - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: custom listener");
        } else {
            super.listen(listener, events);
        }
    }
    //NEW PRIVACY------------------------------------------------------------------------------------------------------------------------------------------
 

    @Override
    public CellLocation getCellLocation() {
    	String packageName = context.getPackageName();
        PrivacySettings pSet = pSetMan.getSettings(packageName);
        CellLocation output = null;
        if (pSet != null && ((pSet.getLocationNetworkSetting() != PrivacySettings.REAL) || (pSet.getLocationGpsSetting() != PrivacySettings.REAL))) {
        	if(pSet.isDefaultDenyObject())
        		pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_LOCATION_NETWORK, null, pSet);
        	else
        		pSetMan.notification(packageName, 0, pSet.getLocationNetworkSetting(), PrivacySettings.DATA_LOCATION_NETWORK, null, pSet);
        } else {
        	if(pSet != null && pSet.isDefaultDenyObject())
        		pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_LOCATION_NETWORK, null, pSet);
        	else
        		pSetMan.notification(packageName, 0, PrivacySettings.REAL, PrivacySettings.DATA_LOCATION_NETWORK, null, pSet);
        	output = super.getCellLocation();
        }
        PrivacyDebugger.d(TAG, "getCellLocation - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: " + ((output != null) ? output : "null"));
        return output;
    }
    
   /**
     * Returns the software version number for the device, for example,
     * the IMEI/SV for GSM phones. Can control with deviceIdSetting
     *
     */
    @Override
    public String getDeviceSoftwareVersion() {
	    String packageName = context.getPackageName();
        PrivacySettings pSet = pSetMan.getSettings(packageName);
	    String output = "";
	    if (pSet != null && pSet.getDeviceIdSetting() != PrivacySettings.REAL) {
        	output = pSet.getDeviceId(); // can be empty, custom or random
        	if(pSet.isDefaultDenyObject())
        		pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_DEVICE_ID, output, pSet);
        	else
        		pSetMan.notification(packageName, 0, pSet.getDeviceIdSetting(), PrivacySettings.DATA_DEVICE_ID, output, pSet);
       	} else {
        	output = super.getDeviceSoftwareVersion();
        	if(pSet != null && pSet.isDefaultDenyObject())
        		pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_DEVICE_ID, output, pSet);
        	else
        		pSetMan.notification(packageName, 0, PrivacySettings.REAL, PrivacySettings.DATA_DEVICE_ID, output, pSet);
        }
	    PrivacyDebugger.d(TAG, "getDeviceSoftwareVersion - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: " + ((output != null) ? output : "null"));
        return output;
    }

    /**
     * 
     * @hide
     */
    @Override
    public String getCompleteVoiceMailNumber() {
        String packageName = context.getPackageName();
        PrivacySettings pSet = pSetMan.getSettings(packageName);
	    String output = "";
	    if (pSet != null && pSet.getLine1NumberSetting() != PrivacySettings.REAL) {
            output = pSet.getLine1Number(); // can be empty, custom or random
            if(pSet.isDefaultDenyObject())
            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_LINE_1_NUMBER, output, pSet);
            else
            	pSetMan.notification(packageName, 0, pSet.getLine1NumberSetting(), PrivacySettings.DATA_LINE_1_NUMBER, output, pSet);
        } else {
        	output = super.getCompleteVoiceMailNumber();
        	if(pSet != null && pSet.isDefaultDenyObject())
        		pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_LINE_1_NUMBER, output, pSet);
        	else
        		pSetMan.notification(packageName, 0, PrivacySettings.REAL, PrivacySettings.DATA_LINE_1_NUMBER, output, pSet);
       	}
	    PrivacyDebugger.d(TAG, "getCompleteVoiceMailNumber - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: " + ((output != null) ? output : "null"));
        return output;
    }
   

    private static final int PHONE_TYPES[] = {PHONE_TYPE_NONE, PHONE_TYPE_GSM, PHONE_TYPE_CDMA, PHONE_TYPE_SIP};
    private static final int NETWORK_TYPES[] = {NETWORK_TYPE_UNKNOWN, NETWORK_TYPE_GPRS, NETWORK_TYPE_EDGE,
												NETWORK_TYPE_UMTS, NETWORK_TYPE_CDMA, NETWORK_TYPE_EVDO_0,
												NETWORK_TYPE_EVDO_A, NETWORK_TYPE_1xRTT, NETWORK_TYPE_HSDPA,
												NETWORK_TYPE_HSUPA, NETWORK_TYPE_HSPA, NETWORK_TYPE_IDEN,
												NETWORK_TYPE_EVDO_B, NETWORK_TYPE_LTE, NETWORK_TYPE_EHRPD,
												NETWORK_TYPE_HSPAP};

    /**
     * @deprecated
     */
    @Override
    public int getPhoneType() {
		String output = getNetworkInfo();
		int type = PHONE_TYPES[0];
		if(output == null) type = super.getPhoneType();
	        return type;
    }

    /**
     * @deprecated
     */
    @Override
    public int getNetworkType() {
            String output = getNetworkInfo();
            int type = NETWORK_TYPES[0];
            if(output == null) type = super.getNetworkType();
            return type;
    }
    
    /**
     * Will be handled like getLine1Number
     */
    @Override
    public String getLine1AlphaTag(){
    	return getLine1Number();
    }
    
    /**
     * 15 character long numbers -> handle same as imsi
     */
    public String getMsisdn() {
    	PrivacyDebugger.d(TAG, "getMsisdn - " + context.getPackageName() + " (" + Binder.getCallingUid() + ")");
    	return getSubscriberId();
    }
    
    /**
     * It doesn't matter if we give some shit to it, it will work
     */
    public String getVoiceMailAlphaTag() {
    	PrivacyDebugger.d(TAG, "getVoiceMailAlphaTag - " + context.getPackageName() + " (" + Binder.getCallingUid() + ")");
    	return getVoiceMailNumber();
    }
    
    /**
     * @hide
     * handles like subscriber id
     */
    public String getIsimImpi() {
    	PrivacyDebugger.d(TAG, "getIsimImpi - " + context.getPackageName() + " (" + Binder.getCallingUid() + ")");
    	return getSubscriberId();
    }
    
    /**
     * @hide
     * lets play with this function, handled like NetworkOperatorName
     */
    public String getIsimDomain() {
    	PrivacyDebugger.d(TAG, "getIsimDomain - " + context.getPackageName() + " (" + Binder.getCallingUid() + ")");
    	return getNetworkOperatorName();
    }
    
    /**
     * @hide
     */
    public String[] getIsimImpu() {
    	String packageName = context.getPackageName();
        PrivacySettings pSet = pSetMan.getSettings(packageName);
        String output[] = new String[1];
        if (pSet != null && pSet.getSubscriberIdSetting() != PrivacySettings.REAL) {
            output[0] = pSet.getSubscriberId(); // can be empty, custom or random
            if(pSet.isDefaultDenyObject())
            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_SUBSCRIBER_ID, output[0], pSet);    
            else
            	pSetMan.notification(packageName, 0, pSet.getSubscriberIdSetting(), PrivacySettings.DATA_SUBSCRIBER_ID, output[0], pSet);    
        } else {
            output = super.getIsimImpu();
            if(pSet != null && pSet.isDefaultDenyObject())
            	pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_SUBSCRIBER_ID, output[0], pSet); 
            else
            	pSetMan.notification(packageName, 0, PrivacySettings.REAL, PrivacySettings.DATA_SUBSCRIBER_ID, output[0], pSet); 
        }
        PrivacyDebugger.d(TAG, "getIsimImpu - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: " + ((output != null) ? output : "null"));
        return output;
    }
    /**
     * @hide
     * @return
     */
    public List<CellInfo> getAllCellInfo() {
    	PrivacySettings pSet = pSetMan.getSettings(context.getPackageName());
    	String packageName = context.getPackageName();
        List<CellInfo> output = null;
        if (pSet != null && ((pSet.getLocationNetworkSetting() != PrivacySettings.REAL) || (pSet.getNetworkInfoSetting() != PrivacySettings.REAL))) {
        	if(pSet.isDefaultDenyObject())
        		pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_LOCATION_NETWORK, null, pSet); 
        	else
        		pSetMan.notification(packageName, 0, PrivacySettings.EMPTY, PrivacySettings.DATA_LOCATION_NETWORK, null, pSet); 
        	output = new ArrayList<CellInfo>(); 
        } else {
        	if(pSet != null && pSet.isDefaultDenyObject())
        		pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_LOCATION_NETWORK, null, pSet); 
        	else
        		pSetMan.notification(packageName, 0, PrivacySettings.REAL, PrivacySettings.DATA_LOCATION_NETWORK, null, pSet); 
        	output = super.getAllCellInfo();
        }
        PrivacyDebugger.d(TAG, "getAllCellInfo - " + context.getPackageName() + " (" + Binder.getCallingUid() + ") output: " + ((output != null) ? output : "null"));
        return output;
    }
}
