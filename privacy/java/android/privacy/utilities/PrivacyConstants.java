package android.privacy.utilities;

import java.util.Random;
import android.telephony.ServiceState;

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
 * Provides Central API for all security related constants
 * @author CollegeDev (Stefan Thiele)
 *
 */
public final class PrivacyConstants {
	
	private static final String TAG = "PhoneHelper";
	
	private static final Random gen = new Random();
	
	
	/**
	 * Provides Central implementation of PrivacyPermissions. All this permissions normally signature protected!
	 * @author CollegeDev (Stefan Thiele)
	 * @TODO: use all this constants in framework and delete other declared variables
	 *
	 */
	public static final class PrivacyPermissions {
		
		/**
		 * Allows applications to write privacy Settings to Database.
		 */
	    public static final String WRITE_PRIVACY_SETTINGS = "android.privacy.WRITE_PRIVACY_SETTINGS";

	    /**
	     * Allows an application to read privacy Settings from Database.
	     */
	    public static final String READ_PRIVACY_SETTINGS = "android.privacy.READ_PRIVACY_SETTINGS";
	    
	    /**
	     * Allows an application to kill Tasks
	     */
	    public static final String KILL_TASKS_AGGRESSIVE = "android.privacy.KILL_TASKS_AGGRESSIVE";
	    
	    /**
	     * Allows an application to receive the kill task command. This is used by core applications. Do not declare 
	     * it in your manager application.
	     */
	    public static final String RECEIVE_TASK_KILL_COMMAND = "android.privacy.RECEIVE_TASK_KILL_COMMAND";
	    
	    /**
	     * Allows an application to receive privacy notification. 
	     */
	    public static final String GET_PRIVACY_NOTIFICATION = "android.privacy.GET_PRIVACY_NOTIFICATION";
	    
	    /**
	     * Allows an application to receive the fail safe mode triggered
	     */
	    public static final String RECEIVE_FAIL_SAFE_TRIGGERED = "android.privacy.RECEIVE_FAIL_SAFE_TRIGGERED";
	    
	    /**
	     * allows an application to set the fail save mode
	     */
	    public static final String SET_FAIL_SAFE_MODE = "android.privacy.SET_FAIL_SAFE_MODE";
	    
	    /**
	     * Allows an application to read the fail save mode e.g. if system is in failSafeMode or not.
	     */
	    public static final String GET_FAIL_SAFE_STATE = "android.privacy.GET_FAIL_SAFE_STATE";
	    
	    /**
	     * Allows an application to enable or disable other applications.
	     */
	    public static final String DISABLE_ENABLE_APPLICATIONS = "android.privacy.DISABLE_ENABLE_APPLICATIONS";
	    
	    /**
	     * Allows an application to receive the disable/enable applications command. This is used by core applications. Do not declare 
	     * it in your manager application.
	     */
	    public static final String RECEIVE_DISABLE_ENABLE_APPLICATIONS = "android.privacy.RECEIVE_DISABLE_ENABLE_APPLICATIONS";
	}
	
	/**
	 * Provides Central Intent data for privacy intents / broadcasts
	 * @author CollegeDev (Stefan Thiele)
	 * @TODO: use all this constants in framework and delete other declared variables
	 *
	 */
	public static final class PrivacyIntent {
		
		/**
		 * Action indicates the Privacy Notification
		 */
	    public static final String ACTION_PRIVACY_NOTIFICATION = "com.privacy.pdroid.PRIVACY_NOTIFICATION";
	    
	    /**
	     * Action indicates the Kill tasks command. core feature.
	     */
	    public static final String ACTION_KILL_TASKS ="com.privacy.pdroid.KILL_TASKS";
	    
	    /**
	     * Action indicates that fail safe mode triggered.
	     */
	    public static final String ACTION_FAIL_SAFE_MODE_TRIGGERED = "com.privacy.pdroid.FAIL_SAFE_MODE_TRIGGERED";
	    
	    /**
	     * Action indicates that the fail safe backup failed
	     */
	    public static final String ACTION_FAIL_SAFE_BACKUP_FAILED = "com.privacy.pdroid.FAIL_SAFE_BACKUP_FAILED";
	    
	    /**
	     * action indicates that the backup after fail safe mode was successful
	     */
	    public static final String ACTION_FAIL_SAFE_BACKUP_COMPLETE = "com.privacy.pdroid.FAIL_SAFE_BACKUP_COMPLETE";
	    
	    /**
	     * Action to get 
	     */
	    public static final String ACTION_DISABLE_ENABLE_APPLICATION = "com.privacy.pdroid.DISABLE_ENABLE_APPLICATION";
	}
	
	/**
	 * Provides constants for several intent data (callerRegister)
	 * @author CollegeDev
	 *
	 */
	public static final class CallerRegistry {
		
		/**
		 * key for intent which contains the unique data access id!
		 */
		public static final String EXTRA_UNIQUE_DATA_ACCESS_ID = "uniqueId";
	}
	
	/**
	 * provides central constants for AppDisabler
	 * @author root
	 *
	 */
	public static final class AppDisabler {
		
		/**
		 * Extra key for getting the uid from intent
		 */
		public static final String EXTRA_UID = "uid-extra";
		
		/**
		 * Extra key for getting the package names to kill from intent
		 */
		public static final String EXTRA_PACKAGE = "package";
		
		/**
		 * Extra key for getting the state if app should be disabled or enabled
		 * If the variable from intent is true, disable application otherwise enable it
		 */
		public static final String EXTRA_DISABLE_OR_ENABLE = "disorenable";
		
	}
	
	/**
	 * All phone related constants and parameters
	 * TODO: move cdma,gsm,lte to this class
	 * @author CollegeDev (Stefan Thiele)
	 *
	 */
	public static final class Phone {
		
	}
	/**
	 * Provides constants for CDMA devices
	 * @author CollegeDev (Stefan Thiele)
	 *
	 */
	public static final class CDMA {
		/**
		 * random generator for cdma cell locations / info
		 * @return random latitude
		 */
		public static int getCdmaRandomLat() {
			int output = gen.nextInt(1296000);
	    	if(gen.nextBoolean())
	    		output *= -1;
	    	return output;
	    }
	    
		/**
		 * random generator for cdma cell locations / info
		 * @return random longitude
		 */
		public static int getCdmaRandomLon() {
	    	int output = gen.nextInt(2592000);
	    	if(gen.nextBoolean())
	    		output *= -1;
	    	return output;
	    }
		/**
		 * random generator for cdma base station ids
		 * @return random base station id
		 */
		public static int getCdmaBaseStationId() {
			return gen.nextInt(65535);
		}
		
		/**
		 * random generator for cdma system ids
		 * @return random cdma system id
		 */
		public static int getCdmaSystemId() {
			return gen.nextInt(32767);
		}
		
		/**
		 * random generator for cdma network ids
		 * @return random cdma network id
		 */
		public static int getCdmaNetworkId() {
			return gen.nextInt(65535);
		}
	}
	
	/**
	 * Provides constants for GSM devices
	 * @author CollegeDev (Stefan Thiele)
	 *
	 */
	public static final class GSM {
		
		/**
		 * random generator for mobile country code
		 * @return random mobile country code
		 */
		public static int getMobileCountryCode() {
			return gen.nextInt(999);
		}
		
		/**
		 * random generator for mobile network code
		 * @return random mobile network code
		 */
		public static int getMobileNetworkCode() {
			return gen.nextInt(999);
		}
		
		/**
		 * random generator for location area code
		 * @return random location area code
		 */
		public static int getLocationAreaCode() {
			return gen.nextInt(65535);
		}
		
		/**
		 * random generator for cell identity
		 * @return random cell identity
		 */
		public static int getCellIdentity() {
			return gen.nextInt(268435455);
		}
		
		/**
		 * random generator for scrambling code
		 * @return random scrambling code
		 * @deprecated
		 */
		public static int getPrimaryScramblingCode() {
			return gen.nextInt(511);
		}
	}
	
	/**
	 * Provides constants for LTE devices
	 * @author CollegeDev (Stefan Thiele)
	 *
	 */
	public static final class LTE {

		/**
		 * random generator for mobile country code
		 * @return random mobile country code
		 */
		public static int getMobileCountryCode() {
			return gen.nextInt(999);
		}
		
		/**
		 * random generator for mobile network code
		 * @return random mobile network code
		 */
		public static int getMobileNetworkCode() {
			return gen.nextInt(999);
		}
		
		/**
		 * random generator for cell identity
		 * @return random cell identity
		 */
		public static int getCellIdentity() {
			return gen.nextInt(268435455);
		}
		
		/**
		 * random generator for physical cell id
		 * @return random physical cell id
		 */
		public static int getPhysicalCellId() {
			return gen.nextInt(503);
		}
		
		/**
		 * random generator for tracking area code
		 * @return random tracking area code
		 */
		public static int getTrackingAreaCode() {
			return gen.nextInt(32768);
		}
		
	}
	
	/**
	 * Provides constants for ServiceState(s)
	 * TODO: move this class to network!
	 * @author CollegeDev (Stefan Thiele)
	 *
	 */
	public static final class PrivacyServiceState {
		
		/**
		 * Creates privacy service-state based on an existing one
		 * @param state existing servicestate
		 * @return privacy service state
		 */
		public static ServiceState getPrivacyServiceState(ServiceState state) {
			ServiceState output = new ServiceState(state);
			output.setOperatorAlphaLong("");
			output.setOperatorName("", "", "");
			return output;
		}
	}
	
	/**
	 * Provides alll network related constants (mobile network and wifi network)
	 * @author root
	 *
	 */
	public static final class Network {
		
		/**
		 * Provides constants for wifi networks
		 * @author CollegeDev (Stefan Thiele)
		 *
		 */
		public static final class WiFi {
			
			private static final String[] ID_PATTERN = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};
			
			private static final int MAC_LENGTH = 5;
			
			/**
			 * Creates a random generated mac-adress 
			 * @return MAC address in {@code XX:XX:XX:XX:XX:XX} form
			 */
			public static String getMacAddress() {
				StringBuilder localBuilder = new StringBuilder();
				for(int i = 0; i < MAC_LENGTH; i++) {
					if(i != MAC_LENGTH - 1)
						localBuilder.append(ID_PATTERN[gen.nextInt(ID_PATTERN.length-1)].toUpperCase()).append(":");
					else
						localBuilder.append(ID_PATTERN[gen.nextInt(ID_PATTERN.length-1)].toUpperCase());
				}
				return localBuilder.toString();	
			}
		}
		
		/**
		 * Provides constants for mobile networks
		 * @author CollegeDev (Stefan Thiele)
		 *
		 */
		public static final class Mobile {
			
		}
		
		
	}
	
	public static final class TaskKiller {
		
		/**
		 * Extra key for getting the uid from intent
		 */
		public static final String EXTRA_UID = "uid-extra";
		
		/**
		 * Extra key for getting the package names to kill from intent
		 */
		public static final String EXTRA_PACKAGES = "packages";
	}
	
	

	
}
