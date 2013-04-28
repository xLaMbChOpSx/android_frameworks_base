package android.privacy.utilities;

import android.content.pm.IPackageManager;
import android.os.ServiceManager;
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
 * Provides Central helper methods for privacy checks
 * @author CollegeDev (Stefan T.)
 *
 */
public final class ResolveHelper {
	
	private static final String TAG = "ResolveHelper";
	
	public static String[] getCallingPackageName (int uid) {
		IPackageManager mPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
    	try{
        	String[] package_names = mPm.getPackagesForUid(uid);
        	return package_names;
    	}
    	catch(Exception e){
    		Log.e(TAG,"not able to parse calling packageName for uid: " + uid);
    		return null;
    	}
    }
	
	/**
	 * 
	 * @param packageNames calling packages
	 * @param packageName parsed package
	 * @return position in packageNames array or -1 if it does not exist
	 */
	public static int fitsToCallingPackage (String[] packageNames, String packageName) {
		if(packageNames == null || packageName == null) return -1;
		for(int i = 0; i < packageNames.length; i++) {
			if(packageNames[i].equals(packageName))
				return i;
		}
		return -1;
	}
	
	/**
	 * 
	 * @param uid calling uid 
	 * @param packageName parsed package
	 * @return position in packageNames array or -1 if it does not exist! Please note that you need an temporary packageNames object by calling getCallingPackageName()!
	 */
	public static int fitsToCallingPackage (int uid, String packageName) {
		String[] packageNames = getCallingPackageName(uid);
		if(packageNames == null) return -1;
		for(int i = 0; i < packageNames.length; i++) {
			if(packageNames[i].equals(packageName))
				return i;
		}
		return -1;
	}
	
	
}
