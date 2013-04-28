/*
 * Copyright (C) 2006 The Android Open Source Project
 * This code has been modified.  Portions copyright (C) 2010, T-Mobile USA, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.os;

import java.util.ArrayList;

import android.util.Log;

//-----------------------------------------------------------
import com.android.internal.telephony.TelephonyProperties;

import android.os.Process;
import android.os.ServiceManager;

import android.privacy.IPrivacySettingsManager;
import android.privacy.PrivacySettings;
import android.privacy.PrivacySettingsManager;
import android.privacy.utilities.PrivacyDebugger;

import android.content.Context;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.os.Binder;
//----------------------------------------------------------


/**
 * Gives access to the system properties store.  The system properties
 * store contains a list of string key-value pairs.
 *
 * {@hide}
 */
public class SystemProperties
{
    public static final int PROP_NAME_MAX = 31;
    public static final int PROP_VALUE_MAX = 91;

    public static final boolean QCOM_HARDWARE = native_get_boolean("com.qc.hardware", false);

    private static final ArrayList<Runnable> sChangeCallbacks = new ArrayList<Runnable>();

    private static native String native_get(String key);
    private static native String native_get(String key, String def);
    private static native int native_get_int(String key, int def);
    private static native long native_get_long(String key, long def);
    private static native boolean native_get_boolean(String key, boolean def);
    private static native void native_set(String key, String def);
    private static native void native_add_change_callback();

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //BEGIN PRIVACY 

    private static final int IS_ALLOWED = -1;
    private static final int IS_NOT_ALLOWED = -2;
    private static final int GOT_ERROR = -3;
    
    private static final String PRIVACY_TAG = "SystemProperties";
    private static Context context;
    
    private static PrivacySettingsManager pSetMan;
    
    private static boolean privacyMode = false;
    
    private static IPackageManager mPm;
    
    //END PRIVACY
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //BEGIN PRIVACY
    /**
     * {@hide}
     * @return package names of current process which is using this object or null if something went wrong
     */
    private static String[] getPackageName(){
    	try{
    		if(mPm != null){
        		int uid = Process.myUid();
        		String[] package_names = mPm.getPackagesForUid(uid);
        		return package_names;
        	}
    		else{
    			mPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
    			int uid = Process.myUid();
        		String[] package_names = mPm.getPackagesForUid(uid);
        		return package_names;
    		}
    	}
    	catch(Exception e){
    		PrivacyDebugger.e(PRIVACY_TAG,"something went wrong with getting package name", e);
    		return null;
    	}
    }
    /**
     * {@hide}
     * This method sets up all variables which are needed for privacy mode! It also writes to privacyMode, if everything was successfull or not! 
     * -> privacyMode = true ok! otherwise false!
     * CALL THIS METHOD IN CONSTRUCTOR!
     */
    private static void initiate(){
    	try{
    		context = null;
    		pSetMan = new PrivacySettingsManager(context, IPrivacySettingsManager.Stub.asInterface(ServiceManager.getService("privacy")));
    		mPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
       	 	privacyMode = true;
    	}
    	catch(Exception e){
    		e.printStackTrace();
    		PrivacyDebugger.e(PRIVACY_TAG, "Something went wrong with initalize variables");
    		privacyMode = false;
    	}
    }
    /**
     * This method should be used, because in some devices the uid has more than one package within!
     * It also includes the notification! It also handles the default deny mode!
     * @return IS_ALLOWED (-1) if all packages allowed, IS_NOT_ALLOWED(-2) if one of these packages not allowed
     */
    private static int checkIfPackagesAllowed(){
    	try{
    		if(pSetMan == null) 
    			pSetMan = new PrivacySettingsManager(context, IPrivacySettingsManager.Stub.asInterface(ServiceManager.getService("privacy")));
			PrivacySettings pSet = null;
    		String[] package_names = getPackageName();
    		if(package_names != null){
	        	for(String pack : package_names){
	        		pSet = pSetMan.getSettings(pack);
	        		if(pSet != null && (pSet.getNetworkInfoSetting() != PrivacySettings.REAL)){ //if pSet is null, we allow application to access to mic
	        			if(pSet.isDefaultDenyObject())
	        				pSetMan.notification(pack, 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
	        			else
	        				pSetMan.notification(pack, 0, PrivacySettings.EMPTY, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
	        			PrivacyDebugger.i(PRIVACY_TAG, "checkIfPackagesAllowed - package: " + pack + " is not allowed to network - info Default deny mode on: " + pSet.isDefaultDenyObject());
	        			return IS_NOT_ALLOWED;
	        		}
	        		pSet = null;
	        	}
	        	PrivacyDebugger.w(PRIVACY_TAG,"checkIfPackagesAllowed - allowing package: " + package_names[0] + " accessing network info");
	        	pSetMan.notification(package_names[0], 0, PrivacySettings.REAL, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);
		    	return IS_ALLOWED;
    		} else {
    			int output;
    			PrivacyDebugger.w(PRIVACY_TAG, "checkIfPackagesAllowed - can't parse packages, going to apply default deny mode");
    			if(PrivacySettings.CURRENT_DEFAULT_DENY_MODE != PrivacySettings.DEFAULT_DENY_REAL) {
    				pSetMan.notification("UNKNOWN", 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);	
    				output = IS_NOT_ALLOWED;
    			} else {
    				pSetMan.notification("UNKNOWN", 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);	
    				output = IS_ALLOWED;
    			}
    			return output;
    		}
    		
    	}
    	catch (Exception e){
    		PrivacyDebugger.e(PRIVACY_TAG,"Got exception in checkIfPackagesAllowed()", e);
    		int output;
    		PrivacyDebugger.e(PRIVACY_TAG, "checkIfPackagesAllowed - got error while trying to check permission. Going to apply default deny mode.");
    		if(PrivacySettings.CURRENT_DEFAULT_DENY_MODE != PrivacySettings.DEFAULT_DENY_REAL) {
				pSetMan.notification("UNKNOWN", 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);	
				output = IS_NOT_ALLOWED;
			} else {
				pSetMan.notification("UNKNOWN", 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_CURRENT, null, null);	
				output = IS_ALLOWED;
			}
    		return output;
    	}
    }
    //END PRIVACY
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////    


    /**
     * Get the value for the given key.
     * @return an empty string if the key isn't found
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    public static String get(String key) {
        if (key.length() > PROP_NAME_MAX) {
            throw new IllegalArgumentException("key.length > " + PROP_NAME_MAX);
        }
        if (key.equals(TelephonyProperties.PROPERTY_OPERATOR_ALPHA)   || 
            key.equals(TelephonyProperties.PROPERTY_OPERATOR_NUMERIC)     ){
		initiate();
		if (checkIfPackagesAllowed() == IS_NOT_ALLOWED) {
			return "";
		}
	}
        return native_get(key);
    }

    /**
     * Get the value for the given key.
     * @return if the key isn't found, return def if it isn't null, or an empty string otherwise
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    public static String get(String key, String def) {
        if (key.length() > PROP_NAME_MAX) {
            throw new IllegalArgumentException("key.length > " + PROP_NAME_MAX);
        }
	if (key.equals(TelephonyProperties.PROPERTY_OPERATOR_ALPHA)   || 
            key.equals(TelephonyProperties.PROPERTY_OPERATOR_NUMERIC)     ){
		initiate();
		if (checkIfPackagesAllowed() == IS_NOT_ALLOWED) {
			return "";
		}
	}
        return native_get(key, def);
    }

    /**
     * Get the value for the given key, and return as an integer.
     * @param key the key to lookup
     * @param def a default value to return
     * @return the key parsed as an integer, or def if the key isn't found or
     *         cannot be parsed
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    public static int getInt(String key, int def) {
        if (key.length() > PROP_NAME_MAX) {
            throw new IllegalArgumentException("key.length > " + PROP_NAME_MAX);
        }
        return native_get_int(key, def);
    }

    /**
     * Get the value for the given key, and return as a long.
     * @param key the key to lookup
     * @param def a default value to return
     * @return the key parsed as a long, or def if the key isn't found or
     *         cannot be parsed
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    public static long getLong(String key, long def) {
        if (key.length() > PROP_NAME_MAX) {
            throw new IllegalArgumentException("key.length > " + PROP_NAME_MAX);
        }
        return native_get_long(key, def);
    }

    /**
     * Get the value for the given key, returned as a boolean.
     * Values 'n', 'no', '0', 'false' or 'off' are considered false.
     * Values 'y', 'yes', '1', 'true' or 'on' are considered true.
     * (case sensitive).
     * If the key does not exist, or has any other value, then the default
     * result is returned.
     * @param key the key to lookup
     * @param def a default value to return
     * @return the key parsed as a boolean, or def if the key isn't found or is
     *         not able to be parsed as a boolean.
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    public static boolean getBoolean(String key, boolean def) {
        if (key.length() > PROP_NAME_MAX) {
            throw new IllegalArgumentException("key.length > " + PROP_NAME_MAX);
        }
        return native_get_boolean(key, def);
    }

    /**
     * Set the value for the given key.
     * @throws IllegalArgumentException if the key exceeds 32 characters
     * @throws IllegalArgumentException if the value exceeds 92 characters
     */
    public static void set(String key, String val) {
        if (key.length() > PROP_NAME_MAX) {
            throw new IllegalArgumentException("key.length > " + PROP_NAME_MAX);
        }
        if (val != null && val.length() > PROP_VALUE_MAX) {
            throw new IllegalArgumentException("val.length > " +
                PROP_VALUE_MAX);
        }
        native_set(key, val);
    }

    public static void addChangeCallback(Runnable callback) {
        synchronized (sChangeCallbacks) {
            if (sChangeCallbacks.size() == 0) {
                native_add_change_callback();
            }
            sChangeCallbacks.add(callback);
        }
    }

    static void callChangeCallbacks() {
        synchronized (sChangeCallbacks) {
            //Log.i("foo", "Calling " + sChangeCallbacks.size() + " change callbacks!");
            if (sChangeCallbacks.size() == 0) {
                return;
            }
            ArrayList<Runnable> callbacks = new ArrayList<Runnable>(sChangeCallbacks);
            for (int i=0; i<callbacks.size(); i++) {
                callbacks.get(i).run();
            }
        }
    }

    /**
     * Get the value for the given key.
     * @return def string if the key isn't found
     */
    public static String getLongString(String key, String def) {
        if (key.length() + 1 > PROP_NAME_MAX) {
            throw new IllegalArgumentException("key.length > " + PROP_NAME_MAX);
        }
        int chunks = getInt(key + '0', 0);
        if (chunks == 0) {
            return def;
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 1; i <= chunks; i++) {
            sb.append(native_get(key + Integer.toString(i)));
        }
        return sb.toString();
    }

    /**
     * Set the value for the given key.
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    public static void setLongString(String key, String val) {
        if (key.length() + 1 > PROP_NAME_MAX) {
            throw new IllegalArgumentException("key.length > " + PROP_NAME_MAX);
        }
        int chunks = 0;
        if (val != null && val.length() > 0) {
            chunks = 1 + val.length() / (PROP_VALUE_MAX + 1);
        }
        native_set(key + '0', Integer.toString(chunks));
        if (chunks > 0) {
            for (int i = 1, start = 0; i <= chunks; i++) {
                int end = start + PROP_VALUE_MAX;
                if (end > val.length()) {
                    end = val.length();
                }
                native_set(key + Integer.toString(i), val.substring(start, end));
                start = end;
            }
        }
    }

}
