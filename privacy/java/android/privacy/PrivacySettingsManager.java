package android.privacy;

import java.util.Random;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.privacy.utilities.PrivacyDebugger;
import android.util.Log;
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
 * Provides API access to the privacy settings
 * @author Svyatoslav Hresyk; modified & improved by CollegeDev (Stefan. T)
 * TODO: selective contacts access
 * {@hide}
 */
public final class PrivacySettingsManager {

    private static final String TAG = "PrivacySettingsManager";
    
    public static final String ACTION_PRIVACY_NOTIFICATION = "com.privacy.pdroid.PRIVACY_NOTIFICATION";
    
    public static final String ACTION_KILL_TASKS ="com.privacy.pdroid.KILL_TASKS";
    
    public static final String ACTION_FAIL_SAFE_MODE_TRIGGERED = "com.privacy.pdroid.FAIL_SAFE_MODE_TRIGGERED";
    
    public static final String ACTION_FAIL_SAFE_BACKUP_FAILED = "com.privacy.pdroid.FAIL_SAFE_BACKUP_FAILED";
    
    public static final String ACTION_FAIL_SAFE_BACKUP_COMPLETE = "com.privacy.pdroid.FAIL_SAFE_BACKUP_COMPLETE";
    
    public static final String ACTION_DISABLE_ENABLE_APPLICATION = "com.privacy.pdroid.DISABLE_ENABLE_APPLICATION";
    
    private static final int MAXIMUM_RECONNECT_COUNT = 5;
    
    private IPrivacySettingsManager service;
    
    /**
     * @hide - this should be instantiated through Context.getSystemService
     * @param context
     */
    public PrivacySettingsManager(Context context, IPrivacySettingsManager service) {
        PrivacyDebugger.d(TAG, "PrivacySettingsManager initializing.", context != null ? context.getPackageName() : null);
        this.service = service;
    }

    public PrivacySettings getSettings(String packageName, int uid) {
        return getSettings(packageName);
    }
    
    public PrivacySettings getSettings(String packageName) {
    	int count = 0;
        try {
            PrivacyDebugger.d(TAG, "getSettings() - getting settings for package: " + packageName);
            if (isServiceAvailable()) {
                return service.getSettings(packageName);
            } else {
            	
            	while(!isServiceAvailable() && count < MAXIMUM_RECONNECT_COUNT) {
            		reinitalizeService();
            		count++;
            	}

            	if(isServiceAvailable())
            		return service.getSettings(packageName);
            	else 
            		PrivacyDebugger.e(TAG, "getSettings - PrivacySettingsManagerService is null -> returning default deny object");
            	
            	return PrivacySettings.getDefaultDenyObject();
            }
        } catch (RemoteException e) {
            PrivacyDebugger.e(TAG, "getSettings - Remote Exception asking for package: " + packageName, e);
            PrivacyDebugger.w(TAG, "getSettings - passing now default deny object, because we can't get stable connection to service");
            return PrivacySettings.getDefaultDenyObject();
        }
    }

    public boolean saveSettings(PrivacySettings settings) {
    	int count = 0;
        try {
        	PrivacyDebugger.d(TAG, "saveSettings() - saving settings for package: " + settings.getPackageName());
            if (isServiceAvailable()) {            
                return service.saveSettings(settings);
            } else {
            	
            	while(!isServiceAvailable() && count < MAXIMUM_RECONNECT_COUNT) {
            		reinitalizeService();
            		count++;
            	}
            	
            	if(isServiceAvailable())
            		return service.saveSettings(settings);
            	else 
            		PrivacyDebugger.e(TAG, "saveSettings - PrivacySettingsManagerService is null");
                return false;
            }
        } catch (RemoteException e) {
        	PrivacyDebugger.e(TAG, "RemoteException in saveSettings - saving setttings for package: " + settings.getPackageName(), e);
            return false;
        }
    }
    
    public boolean deleteSettings(String packageName) {
        return deleteSettings(packageName, -1);
    }
    
    public boolean deleteSettings(String packageName, int uid) {
    	int count = 0;
        try {
        	PrivacyDebugger.d(TAG, "deleteSettings() - deleting settings for package: " + packageName);
            if (isServiceAvailable()) {
                return service.deleteSettings(packageName);
            } else {

            	while(!isServiceAvailable() && count < MAXIMUM_RECONNECT_COUNT) {
            		reinitalizeService();
            		count++;
            	}
            	
            	if(isServiceAvailable())
            		return service.deleteSettings(packageName);
            	else
            		PrivacyDebugger.e(TAG, "deleteSettings - PrivacySettingsManagerService is null");
                return false;
            }
        } catch (RemoteException e) {
        	PrivacyDebugger.e(TAG, "RemoteException in deleteSettings", e);
            return false;
        }
    }
    
    /**
     * Checks whether the PrivacySettingsManagerService is available. For some reason,
     * occasionally it appears to be null. In this case it should be initialized again.
     */
    private boolean isServiceAvailable() {
        if (service != null) return true;
        return false;
    }

    /**
     * reinitializes our service if it is null for some reason!
     */
    private synchronized void reinitalizeService() {
    	this.service = IPrivacySettingsManager.Stub.asInterface(ServiceManager.getService("privacy"));
    	PrivacyDebugger.i(TAG, "service reinitalized now!");
    }
    
    /**
     * @deprecated
     * @param packageName
     * @param uid
     * @param accessMode
     * @param dataType
     * @param output
     * @param pSet
     */
    public void notification(String packageName, int uid, byte accessMode, String dataType, String output, PrivacySettings pSet) {
        notification(packageName, accessMode, dataType, output, pSet);
    }
    
    public void notification(String packageName, byte accessMode, String dataType, String output, PrivacySettings pSet) {
    	int count = 0;
        try {
            if (isServiceAvailable()) {
                service.notification(packageName, accessMode, dataType, output);
            } else {

            	while(!isServiceAvailable() && count < MAXIMUM_RECONNECT_COUNT) {
            		reinitalizeService();
            		count++;
            	}
            	
            	if(isServiceAvailable())
            		service.notification(packageName, accessMode, dataType, output);
            	else 
            		PrivacyDebugger.e(TAG, "notification - PrivacySettingsManagerService is null");
            }
	      
        } catch (RemoteException e) {
            PrivacyDebugger.e(TAG, "RemoteException in notification", e);
        }
    }
    
    public void registerObservers() {
    	int count = 0;
        try {
            if (isServiceAvailable()) {
                service.registerObservers();
            } else {

            	while(!isServiceAvailable() && count < MAXIMUM_RECONNECT_COUNT) {
            		reinitalizeService();
            		count++;
            	}
            	
            	if(isServiceAvailable())
            		service.registerObservers();
            	else 
            		PrivacyDebugger.e(TAG, "registerObservers - PrivacySettingsManagerService is null");
            }
        } catch (RemoteException e) {
            PrivacyDebugger.e(TAG, "RemoteException in registerObservers", e);
        }
    }
    
    public void killTask(String[] packageName, int UID){
    	int count = 0;
		try {
		   if (isServiceAvailable()) {
			   service.killTask(packageName, UID);
		   } else { 

	           while(!isServiceAvailable() && count < MAXIMUM_RECONNECT_COUNT) {
	           		reinitalizeService();
	           		count++;
	           }
           	
	       	   if(isServiceAvailable())
	       		   service.killTask(packageName, UID);
	       	   else
	       		   PrivacyDebugger.e(TAG,"killTask - PrivacySettingsManagerService is null");
		   }
		} catch (RemoteException e) {
			PrivacyDebugger.e(TAG, "KillTasks - RemoteException", e);
	    }
    }
    
    public void disableOrEnableApplication(String packageName, int UID, boolean disable) {
    	int count = 0;
		try {
		   if (isServiceAvailable()) {
			   service.disableOrEnableApplication(packageName, UID, disable);
		   } else { 

	           while(!isServiceAvailable() && count < MAXIMUM_RECONNECT_COUNT) {
	           		reinitalizeService();
	           		count++;
	           }
           	
	       	   if(isServiceAvailable())
	       		   service.disableOrEnableApplication(packageName, UID, disable);
	       	   else
	       		   PrivacyDebugger.e(TAG,"disableApplication - PrivacySettingsManagerService is null");
		   }
		} catch (RemoteException e) {
			PrivacyDebugger.e(TAG, "disableApplication - RemoteException", e);
	    }
    }
    
    public void addObserver(String packageName) {
    	int count = 0;
        try {
            if (isServiceAvailable()) {
                service.addObserver(packageName);
            } else {

            	while(!isServiceAvailable() && count < MAXIMUM_RECONNECT_COUNT) {
            		reinitalizeService();
            		count++;
            	}
            	
            	if(isServiceAvailable())
            		service.addObserver(packageName);
            	else
            		PrivacyDebugger.e(TAG, "addObserver - PrivacySettingsManagerService is null");
            }
        } catch (RemoteException e) {
            PrivacyDebugger.e(TAG, "RemoteException in addObserver", e);
        }
    }
    
    public boolean purgeSettings() {
    	int count = 0;
        try {
            if (isServiceAvailable()) {
                return service.purgeSettings();
            } else {

            	while(!isServiceAvailable() && count < MAXIMUM_RECONNECT_COUNT) {
            		reinitalizeService();
            		count++;
            	}
            	
            	if(isServiceAvailable())
            		return service.purgeSettings();
            	else
            		PrivacyDebugger.e(TAG, "purgeSettings - PrivacySettingsManagerService is null");
            }
        } catch (RemoteException e) {
            PrivacyDebugger.e(TAG, "RemoteException in purgeSettings", e);
        }
        return false;
    }
    
    public double getVersion() {
    	int count = 0;
        try {
            if (isServiceAvailable()) {
                return service.getVersion();
            } else {

            	while(!isServiceAvailable() && count < MAXIMUM_RECONNECT_COUNT) {
            		reinitalizeService();
            		count++;
            	}
            	
            	if(isServiceAvailable())
            		return service.getVersion();
            	else
            		PrivacyDebugger.e(TAG, "getVersion - PrivacySettingsManagerService is null");
            }
        } catch (RemoteException e) {
            PrivacyDebugger.e(TAG, "RemoteException in getVersion", e);
        }
        return 0;
    }
    
    public boolean setEnabled(boolean enable) {
    	int count = 0;
        try {
            if (isServiceAvailable()) {
                return service.setEnabled(enable);
            } else {

            	while(!isServiceAvailable() && count < MAXIMUM_RECONNECT_COUNT) {
            		reinitalizeService();
            		count++;
            	}
            	
            	if(isServiceAvailable())
            		return service.setEnabled(enable);
            	else
            		PrivacyDebugger.e(TAG, "setEnabled - PrivacySettingsManagerService is null");
            }
        } catch (RemoteException e) {
            PrivacyDebugger.e(TAG, "RemoteException in setEnabled", e);
        }
        return false;
    }
    
    public int getLastCallerId(long uniqueId) {
    	int count = 0;
    	try {
            if (isServiceAvailable()) {
                return service.getLastCallerId(uniqueId);
            } else {

            	while(!isServiceAvailable() && count < MAXIMUM_RECONNECT_COUNT) {
            		reinitalizeService();
            		count++;
            	}
            	
            	if(isServiceAvailable())
            		return service.getLastCallerId(uniqueId);
            	else
            		PrivacyDebugger.e(TAG, "getLastCallerId - PrivacySettingsManagerService is null");
            }
        } catch (RemoteException e) {
            PrivacyDebugger.e(TAG, "RemoteException in getLastCallerId", e);
        }
    	return new Random().nextInt();
    }
    
    public boolean setNotificationsEnabled(boolean enable) {
    	int count = 0;
        try {
            if (isServiceAvailable()) {
                return service.setNotificationsEnabled(enable);
            } else {

            	while(!isServiceAvailable() && count < MAXIMUM_RECONNECT_COUNT) {
            		reinitalizeService();
            		count++;
            	}
            	
            	if (isServiceAvailable())
            		return service.setNotificationsEnabled(enable);
            	else 
            		PrivacyDebugger.e(TAG, "setNotificationsEnabled - PrivacySettingsManagerService is null");
            }
        } catch (RemoteException e) {
            PrivacyDebugger.e(TAG, "RemoteException in setNotificationsEnabled", e);
        }
        return false;
    }
    
    public void toggleDebugMode (boolean state) {
    	int count = 0;
    	try {
    		if (isServiceAvailable()) {
                service.toggleDebugMode(state);
            } else {

            	while(!isServiceAvailable() && count < MAXIMUM_RECONNECT_COUNT) {
            		reinitalizeService();
            		count++;
            	}
            	
            	if (isServiceAvailable())
            		service.toggleDebugMode(state);
            	else 
            		PrivacyDebugger.e(TAG, "toggleDebugMode - PrivacySettingsManagerService is null");
            }
    	} catch (RemoteException e) {
    		PrivacyDebugger.e(TAG, "RemoteException in toggleDegubMode", e);
    	}
    }
    
    public boolean isFailSafeActive () {
    	int count = 0;
    	boolean output = false;
    	try {
    		if (isServiceAvailable()) {
                output = service.isFailSafeActive();
            } else {

            	while(!isServiceAvailable() && count < MAXIMUM_RECONNECT_COUNT) {
            		reinitalizeService();
            		count++;
            	}
            	
            	if (isServiceAvailable())
            		output = service.isFailSafeActive();
            	else 
            		PrivacyDebugger.e(TAG, "isFaileSafeActive () - PrivacySettingsManagerService is null");
            }
    	} catch (RemoteException e) {
    		PrivacyDebugger.e(TAG, "RemoteException in isFaileSafeActive ()", e);
    	}
    	return output;
    }
    /**
	 * should get a call if you want to disable failSafeMode
	 * TODO: rename it!
	 */
    public void setFailSafeMode(boolean state) {
    	int count = 0;
    	try {
    		if (isServiceAvailable()) {
    			PrivacyDebugger.i(TAG, "set now fail safe mode to:" + state);
                service.setFailSafeMode(state);
            } else {

            	while(!isServiceAvailable() && count < MAXIMUM_RECONNECT_COUNT) {
            		reinitalizeService();
            		count++;
            	}
            	
            	if (isServiceAvailable()){
            		PrivacyDebugger.i(TAG, "set now fail safe mode to:" + state);
            		service.setFailSafeMode(state);
            	}
            	else 
            		PrivacyDebugger.e(TAG, "ackFailSafeInformed () - PrivacySettingsManagerService is null");
            }
    	} catch (RemoteException e) {
    		PrivacyDebugger.e(TAG, "RemoteException in ackFailSafeInformed ()", e);
    	}
    }
    
    public void setBootCompleted() {
    	int count = 0;
        try {
            if (isServiceAvailable()) {
                service.setBootCompleted();
            } else {

            	while(!isServiceAvailable() && count < MAXIMUM_RECONNECT_COUNT) {
            		reinitalizeService();
            		count++;
            	}
            	
            	if (isServiceAvailable())
            		service.setBootCompleted();
            	else
            		PrivacyDebugger.e(TAG, "setBootCompleted - PrivacySettingsManagerService is null");
            }
        } catch (RemoteException e) {
            PrivacyDebugger.e(TAG, "RemoteException in setBootCompleted", e);
        }
    }
}
