package android.privacy;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.FileUtils;
import android.util.Log;

import android.app.ActivityManagerNative;
import android.os.RemoteException;
import android.os.Process;
import android.privacy.PrivacyWatchDog.PrivacyWatchDogInterface;
import android.privacy.utilities.PrivacyDebugger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
 * Responsible for persisting privacy settings to built-in memory
 * @author Svyatoslav Hresyk; modified & improved by CollegeDev (Stefan T.)
 * {@hide}
 */
public final class PrivacyPersistenceAdapter implements PrivacyWatchDogInterface {

    private static final String TAG = "PrivacyPersistenceAdapter";
    
    private static final int RETRY_QUERY_COUNT = 5;

    private static final String DATABASE_FILE = "/data/system/privacy.db";
    
    private static final String DATABASE_JOURNAL_FILE = "/data/system/privacy.db-journal";
    
    private static final int DATABASE_VERSION = 4;
    
    public static final int DUMMY_UID = -1;
    
    /**
     * Used to save settings for access from core libraries
     */
    public static final String SETTINGS_DIRECTORY = "/data/system/privacy";

    /**
     * Thread safe object for determine how many threads currently have access to database
     */
    private final AtomicInteger dbThreads = new AtomicInteger();
    
    /**
     * For locking the database!
     */
    private final ReentrantReadWriteLock mLock = new ReentrantReadWriteLock();
    
    
    /**
     * Since we're not able to extend from SQLiteDatabase we have to call the onauthorized methods on every write access to database!
     */
    private PrivacyWatchDog watchDog;

    private static final String TABLE_SETTINGS = "settings";
    
    private static final String TABLE_MAP = "map";
    
    private static final String TABLE_ALLOWED_CONTACTS = "allowed_contacts";
    
    private static final String TABLE_VERSION = "version";
    
    private static final String RECEIVE_FAIL_SAFE_TRIGGERED = "android.privacy.RECEIVE_FAIL_SAFE_TRIGGERED";
    
    /**
     * Used for caching some settings
     */
    private PrivacyCache mCache = new PrivacyCache();
    
    private static final String CREATE_TABLE_SETTINGS = 
        "CREATE TABLE IF NOT EXISTS " + TABLE_SETTINGS + " ( " + 
        " _id INTEGER PRIMARY KEY AUTOINCREMENT, " + 
        " packageName TEXT, " + 
        " uid INTEGER, " + 
        " deviceIdSetting INTEGER, " + 
        " deviceId TEXT, " + 
        " line1NumberSetting INTEGER, " + 
        " line1Number TEXT, " + 
        " locationGpsSetting INTEGER, " + 
        " locationGpsLat TEXT, " + 
        " locationGpsLon TEXT, " + 
        " locationNetworkSetting INTEGER, " + 
        " locationNetworkLat TEXT, " + 
        " locationNetworkLon TEXT, " + 
        " networkInfoSetting INTEGER, " + 
        " simInfoSetting INTEGER, " + 
        " simSerialNumberSetting INTEGER, " + 
        " simSerialNumber TEXT, " + 
        " subscriberIdSetting INTEGER, " + 
        " subscriberId TEXT, " + 
        " accountsSetting INTEGER, " + 
        " accountsAuthTokensSetting INTEGER, " + 
        " outgoingCallsSetting INTEGER, " + 
        " incomingCallsSetting INTEGER, " + 
        " contactsSetting INTEGER, " + 
        " calendarSetting INTEGER, " + 
        " mmsSetting INTEGER, " + 
        " smsSetting INTEGER, " + 
        " callLogSetting INTEGER, " + 
        " bookmarksSetting INTEGER, " + 
        " systemLogsSetting INTEGER, " + 
        " externalStorageSetting INTEGER, " + 
        " cameraSetting INTEGER, " + 
        " recordAudioSetting INTEGER, " + 
        " notificationSetting INTEGER, " + 
        " intentBootCompletedSetting INTEGER," + 
        " smsSendSetting INTEGER," + 
        " phoneCallSetting INTEGER," +
        " ipTableProtectSetting INTEGER," +
        " iccAccessSetting INTEGER," +
        " addOnManagementSetting INTEGER," + 
        " androidIdSetting INTEGER," +
        " androidId TEXT," +
        " wifiInfoSetting INTEGER," +
        " switchConnectivitySetting INTEGER," +
        " sendMmsSetting INTEGER," +
        " forceOnlineState INTEGER," + 
        " switchWifiStateSetting INTEGER" +
        ");";
    
    
    private static final String CREATE_TABLE_MAP = 
        "CREATE TABLE IF NOT EXISTS " + TABLE_MAP + " ( name TEXT PRIMARY KEY, value TEXT );";
    
    private static final String CREATE_TABLE_ALLOWED_CONTACTS = 
        "CREATE TABLE IF NOT EXISTS " + TABLE_ALLOWED_CONTACTS + " ( settings_id, contact_id, PRIMARY KEY(settings_id, contact_id) );";
    
    private static final String INSERT_VERSION = 
        "INSERT OR REPLACE INTO " + TABLE_MAP + " (name, value) " + "VALUES (\"db_version\", " + DATABASE_VERSION + ");";
    
    private static final String INSERT_ENABLED = 
        "INSERT OR REPLACE INTO " + TABLE_MAP + " (name, value) " + "VALUES (\"enabled\", \"1\");";
    
    private static final String INSERT_NOTIFICATIONS_ENABLED = 
        "INSERT OR REPLACE INTO " + TABLE_MAP + " (name, value) " + "VALUES (\"notifications_enabled\", \"1\");";
    
    private static final String[] DATABASE_FIELDS = new String[] { "_id", "packageName", "uid", 
        "deviceIdSetting", "deviceId", "line1NumberSetting", "line1Number", "locationGpsSetting", 
        "locationGpsLat", "locationGpsLon", "locationNetworkSetting", "locationNetworkLat", 
        "locationNetworkLon", "networkInfoSetting", "simInfoSetting", "simSerialNumberSetting", 
        "simSerialNumber", "subscriberIdSetting", "subscriberId", "accountsSetting", "accountsAuthTokensSetting", 
        "outgoingCallsSetting", "incomingCallsSetting", "contactsSetting", "calendarSetting", 
        "mmsSetting", "smsSetting", "callLogSetting", "bookmarksSetting", "systemLogsSetting", 
        "externalStorageSetting", "cameraSetting", "recordAudioSetting", "notificationSetting", 
        "intentBootCompletedSetting", "smsSendSetting", "phoneCallSetting", "ipTableProtectSetting", "iccAccessSetting"
        , "addOnManagementSetting", "androidIdSetting", "androidId", "wifiInfoSetting", "switchConnectivitySetting", "sendMmsSetting"
        , "forceOnlineState" , "switchWifiStateSetting"};
    
    public static final String SETTING_ENABLED = "enabled";
    public static final String SETTING_NOTIFICATIONS_ENABLED = "notifications_enabled";
    public static final String SETTING_DB_VERSION = "db_version";
    public static final String VALUE_TRUE = "1";
    public static final String VALUE_FALSE = "0";

    private SQLiteDatabase db;
    
    private Context context;
    
    private boolean isFailSaveActive = false;
    

    public PrivacyPersistenceAdapter(Context context) {
        this.context = context;
        
        boolean canWrite = new File("/data/system/").canWrite();
        watchDog = new PrivacyWatchDog(this);
        
        PrivacyDebugger.d(TAG, "Constructing " + TAG + " for package: " +  context.getPackageName() + "; Write permission for /data/system/: " + canWrite);
        
        if (canWrite) {
        	announceConnection();
        	mLock.writeLock().lock();
        	watchDog.onBeginAuthorizedTransaction();
        	try {
        		if (!new File(DATABASE_FILE).exists()) 
        			createDatabase();
        		
                if (!new File(SETTINGS_DIRECTORY).exists()) 
                	createSettingsDir();
                
                int currentVersion = getDbVersion();
                PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter - current DB version: " + currentVersion);
                
                if (currentVersion < DATABASE_VERSION) 
                	upgradeDatabase(currentVersion);
                
                fillPrivacyCache();
                
        	} catch(Exception e) {
        		PrivacyDebugger.e(TAG, "got exception while trying to create database and/or settingsDirectories");
        	} finally {
        		watchDog.onEndAuthorizedTransaction();
        		mLock.writeLock().unlock();
        		closeIdlingDatabase();
        	}
        }
    }
    
   

    private void upgradeDatabase(int currentVersion) {
    	
        PrivacyDebugger.i(TAG, "upgradeDatabase - upgrading DB from version " + currentVersion + " to " + DATABASE_VERSION);
        
        announceConnection();
        mLock.writeLock().lock();
        watchDog.onBeginAuthorizedTransaction();
        SQLiteDatabase db = null;
        try {
        	db = getDatabase();
            db.beginTransaction();
        	switch (currentVersion) {
	            case 1:
	            case 2:
	            case 3:
                    if (db != null && db.isOpen()) {
                        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VERSION + ";");
                        db.execSQL(CREATE_TABLE_ALLOWED_CONTACTS); 
                        db.execSQL(CREATE_TABLE_MAP);
                        db.execSQL(INSERT_VERSION);
                        db.execSQL(INSERT_ENABLED);
                        db.execSQL(INSERT_NOTIFICATIONS_ENABLED);
                        
                        // remove uid dirs from the settings directory
                        File settingsDir = new File(SETTINGS_DIRECTORY);
                        for (File packageDir : settingsDir.listFiles()) {
                            for (File uidDir : packageDir.listFiles()) {
                                if (uidDir.isDirectory()) {
                                    File[] settingsFiles = uidDir.listFiles();
                                    // copy the first found (most likely the only one) one level up
                                    if (settingsFiles[0] != null) {
                                        File newPath = new File(packageDir + "/" + settingsFiles[0].getName());
                                        newPath.delete();
                                        settingsFiles[0].renameTo(newPath);
                                        deleteRecursive(uidDir);
                                    }
                                }
                            }
                        }
                        
                        db.setTransactionSuccessful();
                    } else {
                    	PrivacyDebugger.e(TAG, "cannot upgrade database because database is null or isn't open!");
                    }
	                break;
	                
	            case 4:
                	if (db != null && db.isOpen()) {
                		db.execSQL("DROP TABLE IF EXISTS " + TABLE_VERSION + ";");
                		db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS + ";");
                		db.execSQL(CREATE_TABLE_SETTINGS);
                		db.execSQL(INSERT_VERSION);
                		
                		removeFiles(SETTINGS_DIRECTORY);
                        
                        db.setTransactionSuccessful();
                	} else {
                    	PrivacyDebugger.e(TAG, "cannot upgrade database because database is null or isn't open!");
                    }
	                break;
        	}
        	purgeSettings();
        } catch(Exception e) {
        	PrivacyDebugger.w(TAG, "upgradeDatabase - could not upgrade DB", e);
        } finally {
        	watchDog.onEndAuthorizedTransaction();
        	mLock.writeLock().unlock();
        	closeIdlingDatabase();
        	
        }
    }
    
    private int getDbVersion() {
    	
        String versionString = getValue(SETTING_DB_VERSION);
        if (versionString == null) {
        	PrivacyDebugger.e(TAG, "getValue returned null for db version, returning version 1");
        	return 1;
        }
        
        try {
            return Integer.parseInt(versionString);
        } catch (Exception e) {
            PrivacyDebugger.e(TAG, "getDbVersion - failed to parse database version; returning 1");
            return 1;
        }
        
    }
    
    public String getValue(String name) {
        SQLiteDatabase db = null;
        Cursor c = null;
        String output = null;
        announceConnection();
        mLock.readLock().lock();
        try {
        	db = getDatabase();
            c = query(db, TABLE_MAP, new String[] { "value" }, "name=?", 
                    new String[] { name }, null, null, null, null);
            if (c != null && c.getCount() > 0 && c.moveToFirst()) {
                output = c.getString(c.getColumnIndex("value"));
                c.close();
            } else {
                PrivacyDebugger.w(TAG, "getValue - could not get value for name: " + name);
            }
        } catch (Exception e) {
            PrivacyDebugger.w(TAG, "getValue - could not get value for name: " + name, e);
        } finally {
        	mLock.readLock().unlock();
        	closeIdlingDatabase();
        }
        
        return output;
    }
    
    public boolean setValue(String name, String value) {
        PrivacyDebugger.e(TAG, "setValue - name " + name + " value " + value);
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("value", value);
        announceConnection();
        mLock.writeLock().lock();
        boolean success = false;
        watchDog.onBeginAuthorizedTransaction();
        try {
            SQLiteDatabase db = getDatabase();
            success = db.replace(TABLE_MAP, null, values) != -1;
            
        } catch (Exception e) {
        	PrivacyDebugger.e(TAG, "got error while trying to set value: " + name + "to: " + value);
        } finally {
        	watchDog.onEndAuthorizedTransaction();
        	mLock.writeLock().unlock();
        	closeIdlingDatabase();
        }
        return success;
    }
    
    public PrivacySettings getSettings(String packageName) {
    	return getSettings(packageName, false);
    }
    
    /**
     * Get the privacySettings for given packageName
     * @param packageName packageName of the application
     * @param fillCache pass true if you want to fill the cache (only needed for method fillPrivacyCache after we're in failSafeMode) -> bypassing failSafeMode
     * @return privacySettings
     */
    private PrivacySettings getSettings(String packageName, boolean fillCache) {
        PrivacySettings s = null;

        if (packageName == null) {
            PrivacyDebugger.e(TAG, "getSettings - insufficient application identifier - package name is required");
            return s;
        }
        
        if(isFailSafeActive() && !fillCache) { 
        	PrivacyDebugger.w(TAG,"failSafeMode is active -> return blocked privacy object!");
        	return PrivacySettings.getFailSaveObject();
        }
        
        if(mCache.containsSetting(packageName)) {
        	return mCache.getSettings(packageName);
        }

        announceConnection();
        mLock.readLock().lock();
        
        SQLiteDatabase db;
        try {
            db = getDatabase();
        } catch (SQLiteException e) {
            PrivacyDebugger.e(TAG, "getSettings - database could not be opened.", e);
            mLock.readLock().unlock();
            closeIdlingDatabase();
            PrivacyDebugger.w(TAG, "getSettings - returning now default deny settings object!");
            return PrivacySettings.getDefaultDenyObject();
        }
           
        Cursor c = null;

        try {
            c = query(db, TABLE_SETTINGS, DATABASE_FIELDS, "packageName=?", new String[] { packageName }, null, null, null, null);

            if (c != null && c.moveToFirst()) {
                s = new PrivacySettings(c.getInt(0), c.getString(1), c.getInt(2), (byte)c.getShort(3), c.getString(4), 
                        (byte)c.getShort(5), c.getString(6), (byte)c.getShort(7), c.getString(8), c.getString(9), (byte)c.getShort(10), 
                        c.getString(11), c.getString(12), (byte)c.getShort(13), (byte)c.getShort(14), (byte)c.getShort(15), 
                        c.getString(16), (byte)c.getShort(17), c.getString(18), (byte)c.getShort(19), (byte)c.getShort(20), 
                        (byte)c.getShort(21), (byte)c.getShort(22), (byte)c.getShort(23), (byte)c.getShort(24), (byte)c.getShort(25), 
                        (byte)c.getShort(26), (byte)c.getShort(27), (byte)c.getShort(28), (byte)c.getShort(29), (byte)c.getShort(30), 
                        (byte)c.getShort(31), (byte)c.getShort(32), (byte)c.getShort(33), (byte)c.getShort(34), null, (byte)c.getShort(35), (byte)c.getShort(36), 
                        (byte)c.getShort(37), (byte)c.getShort(38), (byte)c.getShort(39), (byte)c.getShort(40), c.getString(41), (byte)c.getShort(42),
                        (byte)c.getShort(43), (byte)c.getShort(44), (byte)c.getShort(45), (byte)c.getShort(46));
                
                // get allowed contacts IDs if necessary
                PrivacyDebugger.d(TAG, "getSettings - looking for allowed contacts for " + s.get_id());
                c = rawQuery(db, "SELECT * FROM allowed_contacts WHERE settings_id=" + Integer.toString(s.get_id()) + ";");
                
                if (c != null && c.getCount() > 0) {
                    PrivacyDebugger.d(TAG, "getSettings - found allowed contacts");
                    int[] allowedContacts = new int[c.getCount()];
                    while (c.moveToNext()) allowedContacts[c.getPosition()] = c.getInt(1);
                    s.setAllowedContacts(allowedContacts);
                }
                PrivacyDebugger.d(TAG, "getSettings - found settings entry for package: " + packageName);
                mCache.updateOrSaveSettings(s); 
            } 
            else {
                PrivacyDebugger.e(TAG, "getSettings - no settings found for package: " + packageName);
            } 
        } catch (Exception e) {
            PrivacyDebugger.e(TAG, "getSettings - failed to get settings for package: " + packageName, e);
            
            PrivacyDebugger.w(TAG, "getSettings - we now passing default deny object!");
            s = PrivacySettings.getDefaultDenyObject(); // we prevent leaking data by passing a default deny settings object
            
            if (c != null) 
            	c.close();
        } finally {
            if (c != null) 
            	c.close();
            mLock.readLock().unlock();
            closeIdlingDatabase();
        }
        PrivacyDebugger.d(TAG, "getSettings - returning settings: " + s);

        return s;
    }
    
    

    /**
     * Saves the settings object fields into DB and into plain text files where applicable. 
     * The DB changes will not be made persistent if saving settings to plain text files
     * fails.
     * @param s settings object
     * @return true if settings were saved successfully, false otherwise
     */
    public boolean saveSettings(PrivacySettings s) {
        boolean result = true;
        
        if(s == null) {
        	PrivacyDebugger.e(TAG, "settings are null, cannot save NULL to database!");
        	return false;
        }
        
        String packageName = s.getPackageName();
        
        if (packageName == null || packageName.isEmpty()) {
            PrivacyDebugger.e(TAG, "saveSettings - either package name, UID or both is missing");
            return false;
        }
        
        ContentValues values = new ContentValues();
        values.put("packageName", packageName);
        values.put("uid", DUMMY_UID);
        
        values.put("deviceIdSetting", s.getDeviceIdSetting());
        values.put("deviceId", s.getDeviceId());
        
        values.put("line1NumberSetting", s.getLine1NumberSetting());
        values.put("line1Number", s.getLine1Number());
        
        values.put("locationGpsSetting", s.getLocationGpsSetting());
        values.put("locationGpsLat", s.getLocationGpsLat());
        values.put("locationGpsLon", s.getLocationGpsLon());
        
        values.put("locationNetworkSetting", s.getLocationNetworkSetting());
        values.put("locationNetworkLat", s.getLocationNetworkLat());
        values.put("locationNetworkLon", s.getLocationNetworkLon());
        
        values.put("networkInfoSetting", s.getNetworkInfoSetting());        
        values.put("simInfoSetting", s.getSimInfoSetting());
        
        values.put("simSerialNumberSetting", s.getSimSerialNumberSetting());        
        values.put("simSerialNumber", s.getSimSerialNumber());
        values.put("subscriberIdSetting", s.getSubscriberIdSetting());        
        values.put("subscriberId", s.getSubscriberId());
        
        values.put("accountsSetting", s.getAccountsSetting());
        values.put("accountsAuthTokensSetting", s.getAccountsAuthTokensSetting());
        values.put("outgoingCallsSetting", s.getOutgoingCallsSetting());
        values.put("incomingCallsSetting", s.getIncomingCallsSetting());
        
        values.put("contactsSetting", s.getContactsSetting());
        values.put("calendarSetting", s.getCalendarSetting());
        values.put("mmsSetting", s.getMmsSetting());
        values.put("smsSetting", s.getSmsSetting());
        values.put("callLogSetting", s.getCallLogSetting());
        values.put("bookmarksSetting", s.getBookmarksSetting());
        values.put("systemLogsSetting", s.getSystemLogsSetting());
        values.put("notificationSetting", s.getNotificationSetting());
        values.put("intentBootCompletedSetting", s.getIntentBootCompletedSetting());
//        values.put("externalStorageSetting", s.getExternalStorageSetting());
        values.put("cameraSetting", s.getCameraSetting());
        values.put("recordAudioSetting", s.getRecordAudioSetting());
        values.put("smsSendSetting",s.getSmsSendSetting());
        values.put("phoneCallSetting",s.getPhoneCallSetting());
        values.put("ipTableProtectSetting", s.getIpTableProtectSetting());
        values.put("iccAccessSetting", s.getIccAccessSetting());
        values.put("addOnManagementSetting", s.getAddOnManagementSetting());
        values.put("androidIdSetting", s.getAndroidIdSetting());
        values.put("androidId", s.getAndroidID());
        values.put("wifiInfoSetting", s.getWifiInfoSetting());
        values.put("switchConnectivitySetting", s.getSwitchConnectivitySetting());
        values.put("sendMmsSetting", s.getSendMmsSetting());
        values.put("forceOnlineState", s.getForceOnlineState());
        values.put("switchWifiStateSetting", s.getSwitchWifiStateSetting());
        
        announceConnection();
        mLock.writeLock().lock();
        watchDog.onBeginAuthorizedTransaction();
        SQLiteDatabase db = null;
        Cursor c = null;
        
        try {
        	db = getDatabase();
            db.beginTransaction(); // make sure this ends up in a consistent state (DB and plain text files)
            Integer id = s.get_id();
            if (id != null) { // existing entry -> update
                PrivacyDebugger.d(TAG, "saveSettings - updating existing entry");
                if (db.update(TABLE_SETTINGS, values, "_id=?", new String[] { id.toString() }) < 1) {
                    throw new Exception("saveSettings - failed to update database entry");
                }
                
                db.delete(TABLE_ALLOWED_CONTACTS, "settings_id=?", new String[] { id.toString() });
                int[] allowedContacts = s.getAllowedContacts();
                if (allowedContacts != null) {
                    ContentValues contactsValues = new ContentValues();
                    for (int i = 0; i < allowedContacts.length; i++) {
                        contactsValues.put("settings_id", id);
                        contactsValues.put("contact_id", allowedContacts[i]);
                        if (db.insert(TABLE_ALLOWED_CONTACTS, null, contactsValues) == -1){
                        	throw new Exception("saveSettings - failed to update database entry (contacts)");
                        }
                            
                    }
                }

            } else { // new entry -> insert if no duplicates exist
                PrivacyDebugger.d(TAG, "saveSettings - new entry; verifying if duplicates exist");
                c = db.query(TABLE_SETTINGS, new String[] { "_id" }, "packageName=?", 
                        new String[] { s.getPackageName() }, null, null, null);
                
                if (c != null) {
                    if (c.getCount() == 1) { // exactly one entry
                        // exists -> update
                        PrivacyDebugger.d(TAG, "saveSettings - updating existing entry");
                        if (db.update(TABLE_SETTINGS, values, "packageName=?", 
                                new String[] { s.getPackageName() }) < 1) {
                            throw new Exception("saveSettings - failed to update database entry");
                        }
                        
                        if (c.moveToFirst()) {
                            Integer idAlt = c.getInt(0); // id of the found duplicate entry
                            db.delete(TABLE_ALLOWED_CONTACTS, "settings_id=?", new String[] { idAlt.toString() });
                            int[] allowedContacts = s.getAllowedContacts();
                            if (allowedContacts != null) {
                                ContentValues contactsValues = new ContentValues();
                                for (int i = 0; i < allowedContacts.length; i++) {
                                    contactsValues.put("settings_id", idAlt);
                                    contactsValues.put("contact_id", allowedContacts[i]);
                                    if (db.insert(TABLE_ALLOWED_CONTACTS, null, contactsValues) == -1) {
                                    	throw new Exception("saveSettings - failed to update database entry (contacts)");
                                    }
                                        
                                }
                            }    
                        }
                    } else if (c.getCount() == 0) { // no entries -> insert
                        PrivacyDebugger.d(TAG, "saveSettings - inserting new entry");
                        long rowId = db.insert(TABLE_SETTINGS, null, values);
                        if (rowId == -1) {
                            throw new Exception("saveSettings - failed to insert new record into DB");
                        }
                        
                        db.delete(TABLE_ALLOWED_CONTACTS, "settings_id=?", new String[] { Long.toString(rowId) });
                        int[] allowedContacts = s.getAllowedContacts();
                        if (allowedContacts != null) {
                            ContentValues contactsValues = new ContentValues();
                            for (int i = 0; i < allowedContacts.length; i++) {
                                contactsValues.put("settings_id", rowId);
                                contactsValues.put("contact_id", allowedContacts[i]);
                                if (db.insert(TABLE_ALLOWED_CONTACTS, null, contactsValues) == -1) {
                                	throw new Exception("saveSettings - failed to update database entry (contacts)");
                                }
                                    
                            }
                        }                        
                    } else { // something went totally wrong and there are multiple entries for same identifier
                        result = false;
                        throw new Exception("saveSettings - duplicate entries in the privacy.db");
                    }
                } else {
                    result = false;
                    // jump to catch block to avoid marking transaction as successful
                    throw new Exception("saveSettings - cursor is null, database access failed");
                }
            }
            
            // save settings to plain text file (for access from core libraries)
            result = writeExternalSettings("systemLogsSetting", packageName, s);
            result = writeExternalSettings("ipTableProtectSetting", packageName, s);
            
            // mark DB transaction successful (commit the changes)
            db.setTransactionSuccessful();
            
            mCache.updateOrSaveSettings(s);
            
            PrivacyDebugger.d(TAG, "saveSettings - completing transaction");
        } catch (Exception e) {
            result = false;
            PrivacyDebugger.d(TAG, "saveSettings - could not save settings", e);
        } finally {
            if(db != null) 
            	db.endTransaction();
            if (c != null) 
            	c.close();
            watchDog.onEndAuthorizedTransaction();
            mLock.writeLock().unlock();
            closeIdlingDatabase();
        }
        
        return result;
    }
    
    /**
     * This method creates external settings files for access from core librarys
     * @param settingsName field name from database
     * @param packageName name of package
     * @param s settings from package
     * @return true if file was successful written
     * @throws Exception if we cannot write settings to directory
     */
    private boolean writeExternalSettings(String settingsName, String packageName, PrivacySettings s) throws Exception{
	      // save settings to plain text file (for access from core libraries)
    	
		  PrivacyDebugger.d(TAG, "saveSettings - saving to plain text file");
    	  mLock.writeLock().lock();
	      try {
	    	  File settingsPackageDir = new File("/data/system/privacy/" + packageName + "/");
		      File systemLogsSettingFile = new File("/data/system/privacy/" + packageName + "/" + "/" + settingsName);
	    	  //create all parent directories on the file path
	    	  //settingsUidDir.mkdirs();
	    	  //make the directory readable (requires it to be executable as well)
	    	  //settingsUidDir.setReadable(true, false);
	    	  //settingsUidDir.setExecutable(true, false);
	    	  //make the parent directory readable (requires it to be executable as well)
	          settingsPackageDir.mkdirs();
	          settingsPackageDir.setReadable(true, false);
	          settingsPackageDir.setExecutable(true, false);
	          // create the setting files and make them readable
	          systemLogsSettingFile.createNewFile();
	          systemLogsSettingFile.setReadable(true, false);
	          // write settings to files
	          //PrivacyDebugger.d(TAG, "saveSettings - writing to file");
	          OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(systemLogsSettingFile));
	          //now decide which feature of setting we have to save
	          if(settingsName.equals("systemLogsSetting"))
	        	  writer.append(s.getSystemLogsSetting() + "");
	          else if(settingsName.equals("ipTableProtectSetting"))
	        	  writer.append(s.getIpTableProtectSetting() + "");
	          writer.flush();
	          writer.close();
	          return true;
	      } catch (IOException e) {
	          // jump to catch block to avoid marking transaction as successful
	    	  mLock.writeLock().unlock();
	          throw new Exception("saveSettings - could not write settings to file", e);
	          
	      } finally {
	    	  mLock.writeLock().unlock();
	      }
    }
    
    /**
     * Deletes a settings entry from the DB
     * @return true if settings were deleted successfully, false otherwise
     */
    public boolean deleteSettings(String packageName) {
        boolean result = true;

        announceConnection();
        mLock.writeLock().lock();
        watchDog.onBeginAuthorizedTransaction();
        
        SQLiteDatabase db = null;
        Cursor c = null;
        
        try {
        	db = getDatabase();
            db.beginTransaction(); // make sure this ends up in a consistent state (DB and plain text files)
            PrivacyDebugger.d(TAG, "deleteSettings - deleting database entry for " + packageName);
            // try deleting contacts allowed entries; do not fail if deletion not possible
            c = db.query(TABLE_SETTINGS, new String[] { "_id" }, "packageName=?", 
                    new String[] { packageName }, null, null, null);
            if (c != null && c.getCount() > 0 && c.moveToFirst()) {
                int id = c.getInt(0);
                db.delete(TABLE_ALLOWED_CONTACTS, "settings_id=?", new String[] { Integer.toString(id) });
                c.close();
            }
            
            if (db.delete(TABLE_SETTINGS, "packageName=?", new String[] { packageName }) == 0) {
                PrivacyDebugger.e(TAG, "deleteSettings - database entry for " + packageName + " not found");
                return false;
            }
            
            // delete settings from plain text file (for access from core libraries)
            File settingsPackageDir = new File("/data/system/privacy/" + packageName + "/");
            File systemLogsSettingFile = new File("/data/system/privacy/" + packageName + "/systemLogsSetting");
            // delete the setting files
            systemLogsSettingFile.delete();
            // delete the parent directories
            if (settingsPackageDir.list() == null || settingsPackageDir.list().length == 0) settingsPackageDir.delete();
            // mark DB transaction successful (commit the changes)
            db.setTransactionSuccessful();
            
            mCache.deleteSettings(packageName);
            
        } catch (Exception e) {
            result = false;
            PrivacyDebugger.e(TAG, "deleteSettings - could not delete settings", e);
        } finally {
        	if(c != null)
        		c.close();
        	if(db != null)
        		db.endTransaction();
        	watchDog.onEndAuthorizedTransaction();
        	mLock.writeLock().unlock();
        	closeIdlingDatabase();
        }
        
        return result;
    }
    
    private Cursor query(SQLiteDatabase db, String table, String[] columns, String selection, 
            String[] selectionArgs, String groupBy, String having, String orderBy, String limit) throws Exception {
        Cursor c = null;
        // make sure getting settings does not fail because of IllegalStateException (db already closed)
        boolean success = false;
        for (int i = 0; success == false && i < RETRY_QUERY_COUNT; i++) {
            try {
                if (c != null) 
                	c.close();
                c = db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
                success = true;
            } catch (IllegalStateException e) {
                success = false;
                closeIdlingDatabase();
                announceConnection();
                db = getDatabase();
            } 
        }
        if (success == false) 
        	throw new Exception("query - failed to execute query on the DB");
        return c;
    }
    
    private Cursor rawQuery(SQLiteDatabase db, String sql) throws Exception {
        Cursor c = null;
        // make sure getting settings does not fail because of IllegalStateException (db already closed)
        boolean success = false;
        for (int i = 0; success == false && i < RETRY_QUERY_COUNT; i++) {
            try {
                if (c != null) c.close();
                c = db.rawQuery(sql, null);
                success = true;
            } catch (IllegalStateException e) {
                success = false;
                closeIdlingDatabase();
                announceConnection();
                db = getDatabase();
            }
        }
        if (success == false) 
        	throw new Exception("query - failed to execute query on the DB");
        return c;
    }
    
    /**
     * Removes obsolete entries from the DB and file system. Should not be used in methods, which rely on the DB
     * being open after this method has finished. It will close the DB if no other threads has increased
     * the readingThread count.
     * @return true if purge was successful, false otherwise.
     */
    public boolean purgeSettings() {
        boolean result = true;
        PrivacyDebugger.d(TAG, "purgeSettings - begin purging settings");

        List<String> apps = new ArrayList<String>();
        PackageManager pMan = context.getPackageManager();
        List<ApplicationInfo> installedApps = pMan.getInstalledApplications(0);
        for (ApplicationInfo appInfo : installedApps) { 
            apps.add(appInfo.packageName);
        }
        
        announceConnection();
        mLock.writeLock().lock();
        watchDog.onBeginAuthorizedTransaction();
        
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
        	
        	File settingsDir = new File(SETTINGS_DIRECTORY);
            for (File packageDir : settingsDir.listFiles()) {
                String packageName = packageDir.getName();
                PrivacyDebugger.d(TAG, "purgeSettings - checking package directory " + packageName);
                
                if (!apps.contains(packageName)) { // remove package dir if no such app installed
                    PrivacyDebugger.d(TAG, "purgeSettings - deleting " + packageName);
                    deleteRecursive(packageDir);
                } 
            }
            
            PrivacyDebugger.d(TAG, "purgeSettings - purging database");
            // delete obsolete entries from DB and update outdated entries
        	
        	db = getDatabase();
        	
            c = query(db, TABLE_SETTINGS, new String[] {"packageName"}, null, null, null, null, null, null);
            PrivacyDebugger.d(TAG, "purgeSettings - found " + c.getCount() + " entries in the DB");
            List<String> appsInDb = new ArrayList<String>();
            while (c.moveToNext()) {
                String packageName = c.getString(0);
                if (!apps.contains(packageName)) {
                    deleteSettings(packageName);
                } else {
                    if (appsInDb.contains(packageName)) { // if duplicate entry, remove all duplicates and keep only one
                        PrivacySettings pSetTmp = getSettings(packageName);
                        deleteSettings(packageName);
                        saveSettings(pSetTmp);
                    } else {
                        appsInDb.add(packageName);
                    }
                }
            }
        } catch (Exception e) {
            PrivacyDebugger.e(TAG, "purgeSettings - purging DB failed.", e);
            result = false;
        } finally {
            if (c != null) c.close();
            watchDog.onEndAuthorizedTransaction();
            mLock.writeLock().unlock();
            closeIdlingDatabase();
        }
        return result;
    }
    
    /**
     * @deprecated
     * @param fileOrDirectory
     */
    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) deleteRecursive(child);
        }
        fileOrDirectory.delete();
    }
    
    private void createDatabase() {
        PrivacyDebugger.i(TAG, "createDatabase - creating privacy database file");
        watchDog.onBeginAuthorizedTransaction();
        try {
        	
        	
            SQLiteDatabase db = 
                SQLiteDatabase.openDatabase(DATABASE_FILE, null, SQLiteDatabase.OPEN_READWRITE | 
                        SQLiteDatabase.CREATE_IF_NECESSARY);
            PrivacyDebugger.i(TAG, "createDatabase - creating privacy database");
            db.execSQL(CREATE_TABLE_SETTINGS);
            db.execSQL(CREATE_TABLE_ALLOWED_CONTACTS);
            db.execSQL(CREATE_TABLE_MAP);
            db.execSQL(INSERT_VERSION);
            db.execSQL(INSERT_ENABLED);
            db.execSQL(INSERT_NOTIFICATIONS_ENABLED);
            PrivacyDebugger.d(TAG, "createDatabase - closing connection to privacy.db");
            if (db != null && db.isOpen()) 
            	db.close();
            
           
        } catch (SQLException e) {
            PrivacyDebugger.e(TAG, "createDatabase - failed to create privacy database", e);
        } finally {
        	watchDog.onEndAuthorizedTransaction();
        }
    }
    

    private void createSettingsDir() {
        // create settings directory (for settings accessed from core libraries)
        File settingsDir = new File("/data/system/privacy/");
        settingsDir.mkdirs();
        // make it readable for everybody
        settingsDir.setReadable(true, false);
        settingsDir.setExecutable(true, false);
    }
    
	@Override
	public void onUnauthorizedDatabaseAccess(int msg) {
		
		if(isFailSafeActive()) return; //prevents hammering -> only oneshot possible
		//make sure you only call thread safe methods!
		try {
			setFailSafeMode(true);
			watchDog.onBeginAuthorizedTransaction();
			// inform user first
			Intent intent = new Intent();
			Intent backup = new Intent();
			PrivacyDebugger.e(TAG,"recognized unauthorized databaseaccess. Reason: " + watchDog.msgToString(msg));
			intent.setAction(PrivacySettingsManager.ACTION_FAIL_SAFE_MODE_TRIGGERED);
			intent.putExtra(PrivacyWatchDog.MSG_WHAT_INT, msg);
			intent.putExtra(PrivacyWatchDog.MSG_WHAT_STRING, watchDog.msgToString(msg));
			context.sendBroadcast(intent, RECEIVE_FAIL_SAFE_TRIGGERED);
			
			// try to handle our self!
			deleteCompleteSettings();
			reinitAll();
			//once again to be sure!
			watchDog.startWatching();
			//now try to recover
			ArrayList<String> recovery = tryRecoverFromCache();
			
			//inform user!
			if(recovery.isEmpty()) { 
				backup.setAction(PrivacySettingsManager.ACTION_FAIL_SAFE_BACKUP_COMPLETE);
				context.sendBroadcast(backup, RECEIVE_FAIL_SAFE_TRIGGERED);
			} else {// some settings can't be saved, inform user about that!
				backup.putStringArrayListExtra(PrivacyWatchDog.MSG_RECOVERY_FAIL_INFO, recovery);
				backup.setAction(PrivacySettingsManager.ACTION_FAIL_SAFE_BACKUP_FAILED);
				context.sendBroadcast(backup, RECEIVE_FAIL_SAFE_TRIGGERED);
			}

		} catch (Exception e) {
			PrivacyDebugger.e(TAG,"something went totally wrong in onUnauthorizedDatabaseAccess()");
		} finally {
			watchDog.onEndAuthorizedTransaction();
		}
	}
	
	@Override
	public void onWatchDogFinalize(int authorizedAccessInProgress) {
		PrivacyDebugger.w(TAG,"got information that watchdog is dead, initiate a new one!");
		watchDog = new PrivacyWatchDog(this, authorizedAccessInProgress);
	}
	
	/**
	 * Call this method at the end on every database access. It closes automatically the idling database.
	 * Only call this method after you called watchDog.onEndAuthorized.....!
	 */
	private void closeIdlingDatabase() {
		int threads = (dbThreads.get() > 0) ? dbThreads.decrementAndGet() : 0;
		PrivacyDebugger.i(TAG,"amount of database threads: " + threads);
		if(threads == 0 && db != null && db.isOpen()) {
			watchDog.onBeginAuthorizedTransaction();
			db.close();
			watchDog.onEndAuthorizedTransaction();
			PrivacyDebugger.i(TAG,"closed idling database, because amount of threads are 0");
		}
	}
	
	/**
	 * Call this method right before you lock the read or write access
	 */
	private void announceConnection() {
		int threads = dbThreads.incrementAndGet();
		PrivacyDebugger.i(TAG, "current amount of dbThreads: " + threads);
	}
	
	private synchronized SQLiteDatabase getDatabase() {
		// create the database if it does not exist
        if (!new File(DATABASE_FILE).exists()) createDatabase();

		if (db == null || !db.isOpen() || db.isReadOnly()) {
			PrivacyDebugger.i(TAG, "opening privacy database");
            db = SQLiteDatabase.openDatabase(DATABASE_FILE, null, SQLiteDatabase.OPEN_READWRITE);
        }
		return db;
	}

	
	
	/**
	 * 
	 * @return true if failsafemode is active, false otherwise
	 */
	public boolean isFailSafeActive () {
		return isFailSaveActive;
	}
	
	
	/**
	 * Only call it after boot is ready and failSafeMode was not deactivated by user or to deactivate the fail safe mode
	 * @param state true -> activates the failSafeMode, false -> deactivates the failSafeMode
	 */
	public void setFailSafeMode(boolean state) {
		PrivacyDebugger.w(TAG,"setFailSafeMode() got a call, do you really want that?!");
		isFailSaveActive = state;
	}
	
    /**
     * Fills the complete cache with all data from database
     * Only call this method in constructor
     */
    private synchronized void fillPrivacyCache() {
    	PrivacyDebugger.i(TAG, "on entry fillPrivacyCache()");
        List<ApplicationInfo> apps = context.getPackageManager().getInstalledApplications(0);
        for (ApplicationInfo appInfo : apps) { 
        	PrivacyDebugger.i(TAG,"filling cache for package: " + appInfo.packageName);
            getSettings(appInfo.packageName, true); //only call it 
        }
        PrivacyDebugger.i(TAG,"on exit fillPrivacyCache()");
    }
    
    /**
     * Deletes the given folder or files and all sub-directories with files
     * @param path path t
     */
    private void removeFiles(String path) {
        File file = new File(path);
        PrivacyDebugger.w(TAG, "deleting now file(s) or folder(s): " + file.getAbsolutePath());
        if (file.exists()) {
            String cmd = "rm -r " + path;
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec(cmd);
                PrivacyDebugger.i(TAG, "deleted file(s) or folder(s) successful");
            } catch (IOException e) { 
            	PrivacyDebugger.e(TAG, "Got IOException while trying to delete file(s) or folder(s)", e);
            } catch (Exception e) {
            	PrivacyDebugger.e(TAG, "Got Exception while trying to delete file(s) or folder(s)", e);
            }
        } else {
        	PrivacyDebugger.e(TAG,"can't delete file(s) or folder(s) for path: " + file.getAbsolutePath() + " because it doesn't exists");
        }
    }
    
    /**
     * Deletes all PrivacySettings files:
     * 		- Database 
     * 		- Database journal file
     *  	- Settingsdirectory
     *  It also stops monitoring on privacy.db (for watchDog)
     */
    private void deleteCompleteSettings() {
    	if(watchDog != null) 
    		watchDog.stopWatching();
    	removeFiles(DATABASE_JOURNAL_FILE);
    	removeFiles(DATABASE_FILE);
    	removeFiles(SETTINGS_DIRECTORY);
    }
    
    /**
     * Recovers all settings from current cache to database. Call this method 
     * if database base is empty only!
     * @return true, if <b>all</b> settings have been successful saved, false otherwise
     */
    private ArrayList<String> tryRecoverFromCache() {
    	ArrayList<String> output = new ArrayList<String>();
    	announceConnection();
    	mLock.writeLock().lock();
    	watchDog.onBeginAuthorizedTransaction();
    	mCache.markAllAsNewEntry();
    	PrivacyDebugger.i(TAG,"now trying to recover settings from cache!");
    	try {
    		// TODO: speed this up by leaving the database open!!
    		ArrayList<PrivacySettings> settings = mCache.getAllSetings();
    		for(PrivacySettings insertion : settings) {
    			if(!saveSettings(insertion)) {
    				output.add(insertion.getPackageName());
    				PrivacyDebugger.e(TAG,"restore failed for package: " + insertion.getPackageName());
    			} 
    		}
    		mCache.removeAll();
    		if(settings.size() == output.size()) {
    			PrivacyDebugger.e(TAG, "nothing can't be restore :-! ?");
    		} else {
    			fillPrivacyCache();
    		}
    		
    	} catch (Exception e) {
    		PrivacyDebugger.e(TAG,"something went wrong while trying to recover settings from cache after unauthorized database access!");
    		
    	} finally {
    		if(output.isEmpty())
    			PrivacyDebugger.i(TAG,"successful recovered ALL settings from cache");
    		else 
    			PrivacyDebugger.e(TAG, "wasn't able to recover all settings?!");
    		watchDog.onEndAuthorizedTransaction();
    		mLock.writeLock().unlock();
    		closeIdlingDatabase();
    	}
    	return output;
    }
    
    /**
     * Reinit the whole set (privacy db, privacy settings dir, ...)
     * Only call this method after you called the deleteCompleteSettings() method!
     * this also triggers the watchdog to watch on privacy.db
     */
    private void reinitAll() {
    	boolean canWrite = new File("/data/system/").canWrite();
    	PrivacyDebugger.i(TAG, "called reinitAll() - canWrite: " + canWrite);
    	if (canWrite) {
    		PrivacyDebugger.i(TAG,"we're able to write, create complete new set now");
        	announceConnection();
        	mLock.writeLock().lock();
        	watchDog.onBeginAuthorizedTransaction();
        	try {
        		if (!new File(DATABASE_FILE).exists()) 
        			createDatabase();
        		
        		if(watchDog != null)
        			watchDog.startWatching();
        		
                if (!new File(SETTINGS_DIRECTORY).exists()) 
                	createSettingsDir();
                
                int currentVersion = getDbVersion();
                PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter - current DB version: " + currentVersion);
                
                if (currentVersion < DATABASE_VERSION) 
                	upgradeDatabase(currentVersion);

        	} catch(Exception e) {
        		PrivacyDebugger.e(TAG, "got exception while trying to create database and/or settingsDirectories for reinitializing!");
        	} finally {
        		PrivacyDebugger.i(TAG,"successful reinitialized the whole set");
        		watchDog.onEndAuthorizedTransaction();
        		mLock.writeLock().unlock();
        		closeIdlingDatabase();
        	}
        }
    }
}
