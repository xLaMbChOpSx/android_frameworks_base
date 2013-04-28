package android.privacy;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.privacy.utilities.PrivacyConstants;
import android.privacy.utilities.PrivacyDebugger;
import android.privacy.PrivacyFileObserver;

import java.io.File;
import java.lang.SecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
 * PrivacySettingsManager's counterpart running in the system process, which
 * allows write access to /data/
 * @author Svyatoslav Hresyk; modified & improved by CollegeDev (Stefan. T)
 * TODO: add selective contact access management API
 * {@hide}
 */
public final class PrivacySettingsManagerService extends IPrivacySettingsManager.Stub {

    private static final String TAG = "PrivacySettingsManagerService";
    
    /**
     * TODO: implement all constants in privacyconstants
     */
    
    private static final String WRITE_PRIVACY_SETTINGS = "android.privacy.WRITE_PRIVACY_SETTINGS";

    private static final String READ_PRIVACY_SETTINGS = "android.privacy.READ_PRIVACY_SETTINGS";

    private static final String KILL_TASKS_AGGRESSIVE = "android.privacy.KILL_TASKS_AGGRESSIVE";
    
    private static final String RECEIVE_TASK_KILL_COMMAND = "android.privacy.RECEIVE_TASK_KILL_COMMAND";
    
    private static final String GET_PRIVACY_NOTIFICATION = "android.privacy.GET_PRIVACY_NOTIFICATION";
    
    private static final String RECEIVE_FAIL_SAFE_TRIGGERED = "android.privacy.RECEIVE_FAIL_SAFE_TRIGGERED";
    
    private static final String SET_FAIL_SAFE_MODE = "android.privacy.SET_FAIL_SAFE_MODE";
    
    private static final String GET_FAIL_SAFE_STATE = "android.privacy.GET_FAIL_SAFE_STATE";
    
    private static final String DISABLE_ENABLE_APPLICATIONS = "android.privacy.DISABLE_ENABLE_APPLICATIONS";
    
    private static final String RECEIVE_DISABLE_ENABLE_APPLICATIONS = "android.privacy.RECEIVE_DISABLE_ENABLE_APPLICATIONS";

    private PrivacyPersistenceAdapter persistenceAdapter;

    private Context context;
    
    public static PrivacyFileObserver obs;
    
    private boolean enabled;
    private boolean notificationsEnabled;
    private boolean bootCompleted;
    
    /**
     * Current framework-version
     */
    private static final double VERSION = 1.54;
    
    /**
     * Used as default value for the callingUID
     */
    private static final int UNKNOWN_CALLER = -1;
    
    /**
     * For security reason. Indicates the last UID which called successful the method killTasks().
     * Normally an application should have ALL additional permissions declared, so we need only one variable
     * which indicates the UID of the manager application.
     */
    private AtomicInteger lastCallingUID = new AtomicInteger(UNKNOWN_CALLER);

    /**
     * TODO: add description here
     */
    private List<Long> callerRegister = Collections.synchronizedList(new ArrayList<Long>());
    
    /**
     * TODO: add description here
     */
    private Random generator = new Random();
    
 
    
    /**
     * @hide - this should be instantiated through Context.getSystemService
     * @param context
     */
    public PrivacySettingsManagerService(Context context) {
        PrivacyDebugger.i(TAG, "PrivacySettingsManagerService - initializing for package: " + context.getPackageName() + " UID: " + Binder.getCallingUid(), context.getPackageName());
        this.context = context;
        
        persistenceAdapter = new PrivacyPersistenceAdapter(context);
        obs = new PrivacyFileObserver("/data/system/privacy", this);
        
        enabled = persistenceAdapter.getValue(PrivacyPersistenceAdapter.SETTING_ENABLED).equals(PrivacyPersistenceAdapter.VALUE_TRUE);
        notificationsEnabled = persistenceAdapter.getValue(PrivacyPersistenceAdapter.SETTING_NOTIFICATIONS_ENABLED).equals(PrivacyPersistenceAdapter.VALUE_TRUE);
        bootCompleted = false;
    }
    
    public PrivacySettings getSettings(String packageName) {
    	  PrivacyDebugger.d(TAG, "getSettings() - called for package: " + packageName);
          if (enabled || context.getPackageName().equals("com.privacy.pdroid") || context.getPackageName().equals("com.android.privacy.pdroid.extension"))  
//	  if (Binder.getCallingUid() != 1000)
//            	context.enforceCallingPermission(READ_PRIVACY_SETTINGS, "Requires READ_PRIVACY_SETTINGS");
          return persistenceAdapter.getSettings(packageName);
          else return null;
    }

    public boolean saveSettings(PrivacySettings settings) {
        PrivacyDebugger.d(TAG, "saveSettings - checking if caller (UID: " + Binder.getCallingUid() + ") has sufficient permissions");
        // check permission if not being called by the system process
        if (Binder.getCallingUid() != 1000)
            	context.enforceCallingPermission(WRITE_PRIVACY_SETTINGS, "Requires WRITE_PRIVACY_SETTINGS");
        PrivacyDebugger.d(TAG, "saveSettings: " + settings);
        boolean result = persistenceAdapter.saveSettings(settings);
        if (result == true) obs.addObserver(settings.getPackageName());
        return result;
    }
    
    public boolean deleteSettings(String packageName) {
        PrivacyDebugger.d(TAG, "deleteSettings - " + packageName + " checking if caller (UID: " + Binder.getCallingUid() + ") has sufficient permissions");
        // check permission if not being called by the system process
        if (Binder.getCallingUid() != 1000)
            	context.enforceCallingPermission(WRITE_PRIVACY_SETTINGS, "Requires WRITE_PRIVACY_SETTINGS");
        boolean result = persistenceAdapter.deleteSettings(packageName);
        // update observer if directory exists
        String observePath = PrivacyPersistenceAdapter.SETTINGS_DIRECTORY + "/" + packageName;
        if (new File(observePath).exists() && result == true) {
            obs.addObserver(observePath);
        } else if (result == true) {
            obs.children.remove(observePath);
        }
        return result;
    }
    
    public double getVersion() {
        return VERSION;
    }
    
    public void notification(final String packageName, final byte accessMode, final String dataType, final String output) {
        if (bootCompleted && notificationsEnabled) {
        	Intent intent = new Intent();
            intent.setAction(PrivacySettingsManager.ACTION_PRIVACY_NOTIFICATION);
            intent.putExtra("packageName", packageName);
            intent.putExtra("uid", PrivacyPersistenceAdapter.DUMMY_UID);
            intent.putExtra("accessMode", accessMode);
            intent.putExtra("dataType", dataType);
            intent.putExtra("output", output);
            context.sendBroadcast(intent, GET_PRIVACY_NOTIFICATION);
        }
    }
    
    public void registerObservers() {
        context.enforceCallingPermission(WRITE_PRIVACY_SETTINGS, "Requires WRITE_PRIVACY_SETTINGS");        
        obs = new PrivacyFileObserver("/data/system/privacy", this);
    }
    
    public void addObserver(String packageName) {
        context.enforceCallingPermission(WRITE_PRIVACY_SETTINGS, "Requires WRITE_PRIVACY_SETTINGS");        
        obs.addObserver(packageName);
    }
    
    public boolean purgeSettings() {
        return persistenceAdapter.purgeSettings();
    }
    
    public void setBootCompleted() {
        bootCompleted = true;
    }
    
    /**
     * TODO: add detailed description
     * @param callingUID
     * @return
     */
    private long handleAccessRequest(int callingUID) {
    	PrivacyDebugger.i(TAG, "handleAccessRequest - callingUID: " + callingUID);
    	if(lastCallingUID.get() == UNKNOWN_CALLER)
    		lastCallingUID.set(callingUID);
    	final long uniqueId = generator.nextLong();
    	callerRegister.add(uniqueId);
    	PrivacyDebugger.i(TAG, "handleAccessRequest - unique DataAccessId: " + uniqueId);
    	new Thread(new Runnable() {
		    public void run() {
		    	final long copyId = uniqueId;
		    	try {
		    		Thread.sleep(3000); //now wait 3 seconds!
		    	} catch (Exception e) {
		    		// nothing here
		    	}
 		    	if(callerRegister.contains(copyId)) {
 		    		PrivacyDebugger.w(TAG,"handleAccessRequest Thread - no operation executed, closing now the door");
 		    		callerRegister.remove(copyId);
 		    		if(callerRegister.isEmpty()) {
 		    			PrivacyDebugger.w(TAG, "handleAccessRequest Thread - callerRegister is empty, set last calling uid to unknown caller");
 		    			lastCallingUID.set(UNKNOWN_CALLER);
 		    		}
 		    	} else {
 		    		PrivacyDebugger.i(TAG, "handleAccessRequest Thread - successful executed requested command, nothing todo here.");
 		    	}
		    }
		}).start();
    	
    	return uniqueId;
    }
    
    /**
     * 
     * @param uniqueId
     * @return the current verified user id which is allowed to execute the requested operation
     */
    private int handleExecuteRequest(long uniqueId) {
    	int temp;
    	if(Binder.getCallingUid() != 1000) {
    		PrivacyDebugger.e(TAG, "handleExecuteRequest - attack reason: 1? now closing all doors");
			lastCallingUID.set(UNKNOWN_CALLER);
			callerRegister.clear();
	 		throw new SecurityException("You're not allowed to do this operation! Reason: 1");
		}
		if(callerRegister.isEmpty()) {
			PrivacyDebugger.i(TAG,"handleExecuteRequest - attack reason: 4, closing now all doors");
			lastCallingUID.set(UNKNOWN_CALLER);
			callerRegister.clear(); //just to be sure
			throw new SecurityException("You're not allowed to do this operation! Reason: 4");
		}
		if(callerRegister.contains(uniqueId)) { //user is allowed and registered in queue
			callerRegister.remove(uniqueId);
		} else {
			PrivacyDebugger.e(TAG, "handleExecuteRequest - attack reason: 2? now closing all doors");
			lastCallingUID.set(UNKNOWN_CALLER);
			callerRegister.clear();
	 		throw new SecurityException("You're not allowed to do this operation! Reason: 2");
		}
		if(lastCallingUID.get() == UNKNOWN_CALLER)  {
			PrivacyDebugger.e(TAG, "handleExecuteRequest - attack reason: 3? now closing all doors");
			lastCallingUID.set(UNKNOWN_CALLER);
			callerRegister.clear();
			throw new SecurityException("You're not allowed to do this operation! Reason: 3");
		} else {
			temp = lastCallingUID.get();
			if(callerRegister.isEmpty()) {
				PrivacyDebugger.i(TAG, "handleExecuteRequest - successful asked for executing permission. CallerRegistry is empty, closing doors now.");
				lastCallingUID.set(UNKNOWN_CALLER);
				callerRegister.clear();
			}
			PrivacyDebugger.i(TAG, "handleExecuteRequest - got last calling UID: " + temp);
		}
		PrivacyDebugger.i(TAG, "Amount of entries callerRegister: " + callerRegister.size() + ". LastCallingUID: " + lastCallingUID.get());
	    return temp;
    }
    
    /**
     * 
     * @param packageName array with all packagenames of applications you want to kill
     * @param UID your own user id. You can get it with Process.myUid()
     */
    public synchronized void killTask(String[] packageName, int UID) {
    	context.enforceCallingPermission(KILL_TASKS_AGGRESSIVE, "Requires KILL_TASKS_AGGRESSIVE");

    	final long uniqueId = handleAccessRequest(UID);
    	PrivacyDebugger.i(TAG, "UID: " + lastCallingUID + " successful asked for killing packages. Unique Id: " + uniqueId);
    	
    	Intent killIntent = new Intent();
		killIntent.setAction(PrivacySettingsManager.ACTION_KILL_TASKS);
		killIntent.putExtra(PrivacyConstants.TaskKiller.EXTRA_UID, UID);
		killIntent.putExtra(PrivacyConstants.CallerRegistry.EXTRA_UNIQUE_DATA_ACCESS_ID, uniqueId);
		killIntent.putExtra(PrivacyConstants.TaskKiller.EXTRA_PACKAGES, packageName);
		context.sendBroadcast(killIntent, RECEIVE_TASK_KILL_COMMAND);
    }
    
    public synchronized void disableOrEnableApplication(String packageName, int UID, boolean disable) {
    	context.enforceCallingPermission(DISABLE_ENABLE_APPLICATIONS, "Requires DISABLE_APPLICATIONS");
    	
    	final long uniqueId = handleAccessRequest(UID);
    	PrivacyDebugger.i(TAG, "UID: " + lastCallingUID + " successful asked for disabling application. Unique Id: " + uniqueId);
    	
    	Intent disableIntent = new Intent();
    	disableIntent.setAction(PrivacySettingsManager.ACTION_DISABLE_ENABLE_APPLICATION);
    	disableIntent.putExtra(PrivacyConstants.AppDisabler.EXTRA_UID, UID);
    	disableIntent.putExtra(PrivacyConstants.CallerRegistry.EXTRA_UNIQUE_DATA_ACCESS_ID, uniqueId);
    	disableIntent.putExtra(PrivacyConstants.AppDisabler.EXTRA_PACKAGE, packageName);
    	disableIntent.putExtra(PrivacyConstants.AppDisabler.EXTRA_DISABLE_OR_ENABLE, disable);
    	context.sendBroadcast(disableIntent, RECEIVE_DISABLE_ENABLE_APPLICATIONS);
    }
    
    /**
     * TODO: add description
     */
    public int getLastCallerId(long uniqueId) throws SecurityException {
    	return handleExecuteRequest(uniqueId);
    }
    
    /**
     * 
     * @return true if the failsafe mode is active, false otherwise. Can be
     * only called by system process!
     */
    public boolean isFailSafeActive () {
    	context.enforceCallingPermission(GET_FAIL_SAFE_STATE, "Requires GET_FAIL_SAFE_STATE");
    	return persistenceAdapter.isFailSafeActive();
    }
    
    public void setFailSafeMode(boolean state) {
    	context.enforceCallingPermission(SET_FAIL_SAFE_MODE, "Requires SET_FAIL_SAFE_MODE");
    	persistenceAdapter.setFailSafeMode(state);
    }
    
    
    public boolean setNotificationsEnabled(boolean enable) {
        String value = enable ? PrivacyPersistenceAdapter.VALUE_TRUE : PrivacyPersistenceAdapter.VALUE_FALSE;
        if (persistenceAdapter.setValue(PrivacyPersistenceAdapter.SETTING_NOTIFICATIONS_ENABLED, value)) {
            this.notificationsEnabled = true;
            this.bootCompleted = true;
            return true;
        } else {
            return false;
        }
    }
    
    public boolean setEnabled(boolean enable) {
    	context.enforceCallingPermission(WRITE_PRIVACY_SETTINGS, "Requires WRITE_PRIVACY_SETTINGS");
        String value = enable ? PrivacyPersistenceAdapter.VALUE_TRUE : PrivacyPersistenceAdapter.VALUE_FALSE;
        if (persistenceAdapter.setValue(PrivacyPersistenceAdapter.SETTING_ENABLED, value)) {
            this.enabled = true;
            return true;
        } else {
            return false;
        }
    }
    
    public void toggleDebugMode (boolean state) {
    	PrivacyDebugger.setDebuggerState(state, context);
    }
}
