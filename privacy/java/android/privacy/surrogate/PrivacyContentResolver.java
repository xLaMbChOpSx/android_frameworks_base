package android.privacy.surrogate;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.RemoteException;
import android.privacy.PrivacySettings;
import android.privacy.PrivacySettingsManager;
import android.privacy.utilities.PrivacyDebugger;
import android.provider.Browser;
import android.provider.CalendarContract;
import android.provider.CallLog;
import android.provider.ContactsContract;


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
 * Provides privacy handling for {@link android.content.ContentResolver}
 * @author Svyatoslav Hresyk; modified & improved by CollegeDev (Stefan. T)
 * {@hide}
 */
public final class PrivacyContentResolver {
    
    private static final String TAG = "PrivacyContentResolver";
    
    private static final Uri MMS_CONTENT_URI = Uri.parse("content://mms");
    
    private static final Uri MMS_SMS_CONTENT_URI = Uri.parse("content://mms-sms/");
    
    private static final Uri SMS_CONTENT_URI = Uri.parse("content://sms");
    
    
    private static PrivacySettingsManager pSetMan;
    
    /**
     * Returns a dummy database cursor if access is restricted by privacy settings
     * @param uri
     * @param context
     * @param realCursor
     */
    public static Cursor enforcePrivacyPermission(Uri uri, String[] projection, Context context, Cursor realCursor) throws RemoteException {
//    public static Cursor enforcePrivacyPermission(Uri uri, Context context, Cursor realCursor) {
        if (uri != null) {
            if (pSetMan == null) pSetMan = (PrivacySettingsManager) context.getSystemService("privacy");
            String packageName = context.getPackageName();
            int uid = Binder.getCallingUid();
            PrivacySettings pSet = pSetMan.getSettings(packageName);
            String auth = uri.getAuthority();
            String output_label = "[real]";
            Cursor output = realCursor;
            if (auth != null) {
                if (auth.equals(android.provider.Contacts.AUTHORITY) || auth.equals(ContactsContract.AUTHORITY)) {

                    if (pSet != null) {
                        if (pSet.getContactsSetting() == PrivacySettings.EMPTY) {
                            output_label = "[empty]";
                            output = new PrivacyCursor();
                            if(pSet.isDefaultDenyObject())
                            	pSetMan.notification(packageName, uid, PrivacySettings.ERROR, PrivacySettings.DATA_CONTACTS, null, pSet);
                            else
                            	pSetMan.notification(packageName, uid, PrivacySettings.EMPTY, PrivacySettings.DATA_CONTACTS, null, pSet);
                            
                        } else if (pSet.getContactsSetting() == PrivacySettings.CUSTOM && 
                                uri.toString().contains(ContactsContract.Contacts.CONTENT_URI.toString())) {
                            PrivacyDebugger.d(TAG, "enforcePrivacyPermission - URI: " + uri.toString() + " " + uri.getAuthority() + " " + uri.getEncodedAuthority() + " " + uri.getEncodedFragment() + " " + uri.getEncodedPath() + " " + uri.getEncodedQuery() + " " + uri.getEncodedSchemeSpecificPart() + " " + uri.getEncodedUserInfo() + " " + uri.getFragment() + " " + uri.getPath());
//                            PrivacyDebugger.d(TAG, "enforcePrivacyPermission - projection: " + arrayToString(projection) + " selection: " + selection + " selectionArgs: " + arrayToString(selectionArgs));
                            PrivacyDebugger.d(TAG, "enforcePrivacyPermission - cursor entries: " + output.getCount());
                            
                            boolean idFound = false;
                            if (projection != null) {
                                for (String p : projection) {
                                    if (p.equals(ContactsContract.Contacts._ID)) {
                                        idFound = true;
                                        break;
                                    }
                                }
                                
//                                if (!idFound) { // add ID to projection
//                                    String[] newProjection = new String[projection.length + 1];
//                                    System.arraycopy(projection, 0, newProjection, 0, projection.length);
//                                    newProjection[projection.length] = ContactsContract.Contacts._ID;
//                                    projection = newProjection;
//                                }
                            }
                            
                            if (!idFound) {
                                output = new PrivacyCursor();
                            } else {
//                            PrivacyDebugger.d(TAG, "enforcePrivacyPermission - new projection: " + arrayToString(projection) + " selection: " + selection + " selectionArgs: " + arrayToString(selectionArgs));
                            
                            // re-query
//                            output = provider.query(uri, projection, selection, selectionArgs, sortOrder);
                            PrivacyDebugger.d(TAG, "enforcePrivacyPermission - new cursor entries: " + output.getCount());
                                output = new PrivacyCursor(output, pSet.getAllowedContacts());
                            }
                            if(pSet.isDefaultDenyObject())
                            	pSetMan.notification(packageName, uid, PrivacySettings.ERROR, PrivacySettings.DATA_CONTACTS, null, pSet);
                            else
                            	pSetMan.notification(packageName, uid, PrivacySettings.CUSTOM, PrivacySettings.DATA_CONTACTS, null, pSet);
                        } else { // REAL
                        	if(pSet.isDefaultDenyObject())
                        		pSetMan.notification(packageName, uid, PrivacySettings.ERROR, PrivacySettings.DATA_CONTACTS, null, pSet);
                        	else
                        		pSetMan.notification(packageName, uid, PrivacySettings.REAL, PrivacySettings.DATA_CONTACTS, null, pSet);
                        }
                    } else {
                    	PrivacyDebugger.e(TAG, "pSet is NULL -> handle default deny mode!");
                    	switch(PrivacySettings.CURRENT_DEFAULT_DENY_MODE) {
                    		case PrivacySettings.DEFAULT_DENY_EMPTY:
                    		case PrivacySettings.DEFAULT_DENY_RANDOM:
                    			output = new PrivacyCursor();
                    			PrivacyDebugger.w(TAG,"default mode is empty or random. Set cursor to privacyCursor");
                    			break;
                    		case PrivacySettings.DEFAULT_DENY_REAL:
                    			//nothing here
                    			PrivacyDebugger.w(TAG,"default deny mode is real -> return real cursor");
                    			break;
                    	}
                    	pSetMan.notification(packageName, uid, PrivacySettings.ERROR, PrivacySettings.DATA_CONTACTS, null, pSet);
                    }
                    
                } else if (auth.equals(CalendarContract.AUTHORITY)) {
                    
                    if (pSet != null && pSet.getCalendarSetting() == PrivacySettings.EMPTY) {
                        output_label = "[empty]";
                        output = new PrivacyCursor();
                        if(pSet.isDefaultDenyObject())
                        	pSetMan.notification(packageName, uid, PrivacySettings.ERROR, PrivacySettings.DATA_CALENDAR, null, pSet);
                        else
                        	pSetMan.notification(packageName, uid, PrivacySettings.EMPTY, PrivacySettings.DATA_CALENDAR, null, pSet);
                    } else {
                    	if(pSet != null && pSet.isDefaultDenyObject())
                    		pSetMan.notification(packageName, uid, PrivacySettings.ERROR, PrivacySettings.DATA_CALENDAR, null, pSet);
                    	else
                    		pSetMan.notification(packageName, uid, PrivacySettings.REAL, PrivacySettings.DATA_CALENDAR, null, pSet);
                    }
                    
                } else if (auth.equals(MMS_CONTENT_URI.getAuthority())) {
                    
                    if (pSet != null && pSet.getMmsSetting() == PrivacySettings.EMPTY) {
                        output_label = "[empty]";
                        output = new PrivacyCursor();
                        if(pSet.isDefaultDenyObject())
                        	pSetMan.notification(packageName, uid, PrivacySettings.ERROR, PrivacySettings.DATA_MMS, null, pSet);
                        else
                        	pSetMan.notification(packageName, uid, PrivacySettings.EMPTY, PrivacySettings.DATA_MMS, null, pSet);
                    } else {
                    	if(pSet != null && pSet.isDefaultDenyObject())
                    		pSetMan.notification(packageName, uid, PrivacySettings.ERROR, PrivacySettings.DATA_MMS, null, pSet);
                    	else
                    		pSetMan.notification(packageName, uid, PrivacySettings.REAL, PrivacySettings.DATA_MMS, null, pSet);
                    }
                    
                } else if (auth.equals(SMS_CONTENT_URI.getAuthority())) {
                    
                    if (pSet != null && pSet.getSmsSetting() == PrivacySettings.EMPTY) {
                        output_label = "[empty]";
                        output = new PrivacyCursor();
                        if(pSet.isDefaultDenyObject())
                        	pSetMan.notification(packageName, uid, PrivacySettings.ERROR, PrivacySettings.DATA_SMS, null, pSet);
                        else
                        	pSetMan.notification(packageName, uid, PrivacySettings.EMPTY, PrivacySettings.DATA_SMS, null, pSet);
                    } else {
                    	if(pSet != null && pSet.isDefaultDenyObject())
                    		pSetMan.notification(packageName, uid, PrivacySettings.ERROR, PrivacySettings.DATA_SMS, null, pSet);
                    	else
                    		pSetMan.notification(packageName, uid, PrivacySettings.REAL, PrivacySettings.DATA_SMS, null, pSet);
                    }
                // all messages, sms and mms
                } else if (auth.equals(MMS_SMS_CONTENT_URI.getAuthority()) || 
                        auth.equals("mms-sms-v2") /* htc specific, accessed by system messages application */) { 
                    
                    // deny access if access to either sms, mms or both is restricted by privacy settings
                    if (pSet != null && (pSet.getMmsSetting() == PrivacySettings.EMPTY || 
                            pSet.getSmsSetting() == PrivacySettings.EMPTY)) {
                        output_label = "[empty]";
                        output = new PrivacyCursor();
                        if(pSet.isDefaultDenyObject())
                        	pSetMan.notification(packageName, uid, PrivacySettings.ERROR, PrivacySettings.DATA_MMS_SMS, null, pSet);
                        else
                        	pSetMan.notification(packageName, uid, PrivacySettings.EMPTY, PrivacySettings.DATA_MMS_SMS, null, pSet);
                    } else {
                    	if(pSet != null && pSet.isDefaultDenyObject())
                    		pSetMan.notification(packageName, uid, PrivacySettings.ERROR, PrivacySettings.DATA_MMS_SMS, null, pSet);
                    	else
                    		pSetMan.notification(packageName, uid, PrivacySettings.REAL, PrivacySettings.DATA_MMS_SMS, null, pSet);
                    }

                } else if (auth.equals(CallLog.AUTHORITY)) {
                    
                    if (pSet != null && pSet.getCallLogSetting() == PrivacySettings.EMPTY) {
                        output_label = "[empty]";
                        output = new PrivacyCursor();
                        if(pSet.isDefaultDenyObject())
                        	pSetMan.notification(packageName, uid, PrivacySettings.ERROR, PrivacySettings.DATA_CALL_LOG, null, pSet);
                        else
                        	pSetMan.notification(packageName, uid, PrivacySettings.EMPTY, PrivacySettings.DATA_CALL_LOG, null, pSet);
                    } else {
                    	if(pSet != null && pSet.isDefaultDenyObject())
                    		pSetMan.notification(packageName, uid, PrivacySettings.ERROR, PrivacySettings.DATA_CALL_LOG, null, pSet);
                    	else
                    		pSetMan.notification(packageName, uid, PrivacySettings.REAL, PrivacySettings.DATA_CALL_LOG, null, pSet);
                    }

                } else if (auth.equals(Browser.BOOKMARKS_URI.getAuthority())) {
                    
                    if (pSet != null && pSet.getBookmarksSetting() == PrivacySettings.EMPTY) {
                        output_label = "[empty]";
                        output = new PrivacyCursor();
                        if(pSet.isDefaultDenyObject())
                        	pSetMan.notification(packageName, uid, PrivacySettings.ERROR, PrivacySettings.DATA_BOOKMARKS, null, pSet);
                        else
                        	pSetMan.notification(packageName, uid, PrivacySettings.EMPTY, PrivacySettings.DATA_BOOKMARKS, null, pSet);
                    } else {
                    	if(pSet != null && pSet.isDefaultDenyObject())
                    		pSetMan.notification(packageName, uid, PrivacySettings.ERROR, PrivacySettings.DATA_BOOKMARKS, null, pSet);
                    	else
                    		pSetMan.notification(packageName, uid, PrivacySettings.REAL, PrivacySettings.DATA_BOOKMARKS, null, pSet);
                    }
                    
                }
            }
            PrivacyDebugger.d(TAG, "query - " + packageName + " (" + uid + ") auth: " + auth + " output: " + output_label);
            return output;
        }
        return realCursor;
    }
    
    private static String arrayToString(String[] array) {
        StringBuffer sb = new StringBuffer();
        if (array != null) for (String bla : array) sb.append("[" + bla + "]");
        else return "";
        return sb.toString();
    }
    /**
     * This method is especially for faking android_id if google wants to read it in their privacy database
     * @deprecated
     * @param uri
     * @param projection
     * @param context
     * @param realCursor
     */
    public static Cursor enforcePrivacyPermission(Uri uri, String[] projection, Context context, Cursor realCursor, boolean google_access) throws RemoteException {
		if (uri != null) {
	            if (pSetMan == null) pSetMan = (PrivacySettingsManager) context.getSystemService("privacy");
	            String packageName = context.getPackageName();
	            PrivacySettings pSet = pSetMan.getSettings(packageName);
	            String auth = uri.getAuthority();
	            String output_label = "[real]";
	            Cursor output = realCursor;
	            if (auth != null && auth.equals("com.google.android.gsf.gservices")) {
			
					if (pSet != null && pSet.getSimInfoSetting() != PrivacySettings.REAL){
						int actual_pos = realCursor.getPosition();
						int forbidden_position = -1;
						try{
							for(int i=0;i<realCursor.getCount();i++){
								realCursor.moveToNext();
								if(realCursor.getString(0).equals("android_id")){
									forbidden_position = realCursor.getPosition();
									break;
								}
							}
						} catch (Exception e){
							PrivacyDebugger.e(TAG,"something went wrong while getting blocked permission for android id");
						} finally{
							realCursor.moveToPosition(actual_pos);
							if(forbidden_position == -1) {PrivacyDebugger.i(TAG,"now we return real cursor, because forbidden_pos is -1"); return output;} //give realcursor, because there is no android_id to block
						}
						PrivacyDebugger.i(TAG,"now blocking google access to android id and give fake cursor. forbidden_position: " + forbidden_position);
						output_label = "[fake]";
						output = new PrivacyCursor(realCursor,forbidden_position);	
						if(pSet.isDefaultDenyObject())
							pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_SIM, null, pSet);
						else
							pSetMan.notification(packageName, 0, PrivacySettings.EMPTY, PrivacySettings.DATA_NETWORK_INFO_SIM, null, pSet);
					} else {
						PrivacyDebugger.i(TAG,"google is allowed to get real cursor");
						if(pSet != null && pSet.isDefaultDenyObject())
							pSetMan.notification(packageName, 0, PrivacySettings.ERROR, PrivacySettings.DATA_NETWORK_INFO_SIM, null, pSet);
						else
							pSetMan.notification(packageName, 0, PrivacySettings.REAL, PrivacySettings.DATA_NETWORK_INFO_SIM, null, pSet);
					}
			    }
			    return output;
		}
		return realCursor;   
    }


}
