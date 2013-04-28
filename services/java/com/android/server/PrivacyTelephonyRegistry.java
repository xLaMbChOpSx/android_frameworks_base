package com.android.server;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.android.internal.telephony.IPhoneStateListener;
import com.android.server.TelephonyRegistry.Record;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.privacy.IPrivacySettingsManager;
import android.privacy.PrivacySettings;
import android.privacy.PrivacySettingsManager;
import android.privacy.utilities.PrivacyConstants;
import android.privacy.utilities.PrivacyDebugger;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityCdma;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

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
 * Provides Central Handling
 * @author CollegeDev (Stefan Thiele)
 */
public class PrivacyTelephonyRegistry extends TelephonyRegistry {

	private static final String P_TAG = "PrivacyTelephonyRegistry";
	
	private PrivacySettingsManager pSetMan;
	
	private static final int PERMISSION_CELL_LOCATION = 0;
	
	private static final int PERMISSION_CELL_INFO = 1;
	
	private static final int PERMISSION_SIGNAL_STRENGTH = 2;
	
	private static final int PERMISSION_CALL_STATE = 3;
	
	private static final int PERMISSION_SERVICE_STATE = 4;
	
	private static final int CELL_INFO_GSM = 1;
	
	private static final int CELL_INFO_CDMA = 2;
	
	private static final int CELL_INFO_LTE = 3;
	
	private boolean preventBroadcasting = false;
	
	private Context context;
	
	/**
	 * do not needs to synchronize, because only gets call while inside synchronized(mRecords)
	 */
	private ArrayList<Record> mCacheAll = new ArrayList<Record>();
	private ArrayList<Record> mCacheAllow = new ArrayList<Record>();
	private ArrayList<Record> mCacheBlock = new ArrayList<Record>();
	private ArrayList<IBinder> mIdlingCache = new ArrayList<IBinder>();
	
	public PrivacyTelephonyRegistry(Context context) {
		super(context);
		this.context = context;
		pSetMan = new PrivacySettingsManager(context, IPrivacySettingsManager.Stub.asInterface(ServiceManager.getService("privacy")));
		try{
			registerPrivacy();
		} catch(Exception e){
			PrivacyDebugger.e(P_TAG,"failed to register privacy broadcastreceiver");
		}
		PrivacyDebugger.i(P_TAG,"constructor ready");
	}
	
	/** This broadCastReceiver receives the privacy intent for blocking phonecalls and faking phonestate */
	private final BroadcastReceiver privacyReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("android.privacy.BLOCKED_PHONE_CALL")){
            	Bundle data = new Bundle();
            	data = intent.getExtras();
            	String packageName = data.getString("packageName");
            	if(data.containsKey("packageName")){
            		PrivacyDebugger.i(P_TAG, "got blocked phone call INTENT from package: " + data.getString("packageName"));
            	} else{
            		PrivacyDebugger.i(P_TAG, "got blocked phone call INTENT without package information");
            	}
            	if(packageName == null) return;
            	if(data.containsKey("phoneState")){
            		int state = data.getInt("phoneState");
            		switch(state){
            			case TelephonyManager.CALL_STATE_IDLE:
            				notifyPrivacyCallState(TelephonyManager.CALL_STATE_IDLE, null, packageName);
            				return;
            			case TelephonyManager.CALL_STATE_OFFHOOK:
            				notifyPrivacyCallState(TelephonyManager.CALL_STATE_OFFHOOK, null, packageName);
            				return;
            			case TelephonyManager.CALL_STATE_RINGING:
            				notifyPrivacyCallState(TelephonyManager.CALL_STATE_RINGING, "12345", packageName);
            				return;
            			default:
            				return;
            		}
            	}
            	PrivacyDebugger.i(P_TAG,"we forgot to put phoneState in Intent?");
            }
        }
    };
    
    /**
     * This method allows us to fake a call state if application uses phoneStateListener. It will call the onCallStateChanged method with faked state and number
     * @param state {@link TelephonyManager} TelephonyManager.CALL_STATE_IDLE <br> TelephonyManager.CALL_STATE_OFFHOOK <br> TelephonyManager.CALL_STATE_RINGING <br>
     * @param incomingNumber pass null if you don't choose ringing!
     * @param packageName the affected package to fake callstate!
     * @author CollegeDev
     */
    private void notifyPrivacyCallState(int state, String incomingNumber, String packageName) {

        synchronized (mRecords) {

            for (Record r : mRecords) {
                if ((r.events & PhoneStateListener.LISTEN_CALL_STATE) != 0) {
                    try {
                    	//only notify the affected application
                    	if(r.pkgForDebug.equals(packageName)){
                    		r.callback.onCallStateChanged(state, incomingNumber);
                    	}
                    } catch (RemoteException ex) {
                        mRemoveList.add(r.binder);
                    }
                }
            }
            handleRemoveListLocked();
        }

    }
    
    private void registerPrivacy(){
    	 IntentFilter intentFilter = new IntentFilter("android.privacy.BLOCKED_PHONE_CALL");
    	 mContext.registerReceiver(privacyReceiver, intentFilter);
    }
	
    @Override
    public void listen(String pkgForDebug, IPhoneStateListener callback, int events, boolean notifyNow) {
		try{
			registerPrivacy();
		} catch(Exception e){
			PrivacyDebugger.e(P_TAG,"failed to register privacy receiver", e);
		}
    	
    	if(!isPackageAllowed(PERMISSION_CELL_LOCATION, pkgForDebug) || !isPackageAllowed(PERMISSION_CELL_INFO, pkgForDebug)) {
    		PrivacyDebugger.i(P_TAG, "package: " + pkgForDebug + " is now listening. notifyNow: false. isAllowed: false");
    		super.listen(pkgForDebug, callback, events, false);
    	} else {
    		PrivacyDebugger.i(P_TAG, "package: " + pkgForDebug + " is now listening. notifyNow: " + notifyNow + ". isAllowed: true");
    		super.listen(pkgForDebug, callback, events, notifyNow);
    	}
    }
	
	
	private boolean isPackageAllowed(int PERMISSION, String packageName){
		if(pSetMan == null) pSetMan = new PrivacySettingsManager(context, IPrivacySettingsManager.Stub.asInterface(ServiceManager.getService("privacy")));
		PrivacySettings settings = pSetMan.getSettings(packageName);
		if(settings == null) {
			PrivacyDebugger.e(P_TAG,"we return true, because settings are not available"); 
			return true; 
		}
		switch(PERMISSION){
			case PERMISSION_CELL_LOCATION:
				if(settings.getLocationNetworkSetting() != PrivacySettings.REAL)
					return false;
				else 
					return true;
			case PERMISSION_CELL_INFO:
				if(settings.getNetworkInfoSetting() != PrivacySettings.REAL)
					return false;
				else
					return true;
			case PERMISSION_SIGNAL_STRENGTH:
				if(settings.getNetworkInfoSetting() != PrivacySettings.REAL)
					return false;
				else
					return true;
			case PERMISSION_CALL_STATE:
				if(settings.getNetworkInfoSetting() != PrivacySettings.REAL)
					return false;
				else
					return true;
			case PERMISSION_SERVICE_STATE:
				if(settings.getNetworkInfoSetting() != PrivacySettings.REAL)
					return false;
				else
					return true;
			default:
				return false;
		}
	}

	@Override
	public void notifyServiceState(ServiceState state) {
        synchronized (mRecords) {
        	initOperations();
            for (Record r : mRecords) {
                if ((r.events & PhoneStateListener.LISTEN_SERVICE_STATE) != 0) {
                	if(isPackageAllowed(PERMISSION_SERVICE_STATE, r.pkgForDebug)) {
                		mCacheAllow.add(r);
                		PrivacyDebugger.i(P_TAG, "allow package: " + r.pkgForDebug +" for getting ServiceState");
                	} else {
                		mCacheBlock.add(r);
                		PrivacyDebugger.i(P_TAG, "block package: " + r.pkgForDebug +" for getting ServiceState");
                	}
                }
            }
            preventBroadcasting = true;
            onPrepareBlockedPackages();
            PrivacyDebugger.i(P_TAG,"now inform blocked packages (ServiceState), count: " + mCacheBlock.size());
            if(mCacheBlock.size() > 0)
            	super.notifyServiceState(PrivacyConstants.PrivacyServiceState.getPrivacyServiceState(state));

            
            onPrepareAllowedPackages();
            PrivacyDebugger.i(P_TAG,"now inform allowed packages (ServiceState), count: " + mCacheAllow.size());
            if(mCacheAllow.size() > 0)
            	super.notifyServiceState(state);

            
            onExit();
        }
    }

	@Override
	public void notifyCellInfo(List<CellInfo> cellInfo) {
		synchronized (mRecords) {
			initOperations();
            for (Record r : mRecords) {
                if ((r.events & PhoneStateListener.LISTEN_CELL_INFO) != 0) {
                	if(isPackageAllowed(PERMISSION_CELL_INFO, r.pkgForDebug)) {
                		mCacheAllow.add(r);
                		PrivacyDebugger.i(P_TAG, "allow package: " + r.pkgForDebug +" for getting CellInfo");
                	} else {
                		mCacheBlock.add(r);
                		PrivacyDebugger.i(P_TAG, "block package: " + r.pkgForDebug +" for getting CellInfo");
                	}
                }
            }
            onPrepareBlockedPackages();
            PrivacyDebugger.i(P_TAG,"now inform blocked packages (cellInfo), count: " + mCacheBlock.size());
            if(mCacheBlock.size() > 0)
            	super.notifyCellInfo(getPrivacyCellInfo(cellInfo));

            
            onPrepareAllowedPackages();
            PrivacyDebugger.i(P_TAG,"now inform allowed packages (cellInfo), count: " + mCacheAllow.size());
            if(mCacheAllow.size() > 0)
            	super.notifyCellInfo(cellInfo);

            
            onExit();
		}
    }
	
	@Override
	public void notifyCellLocation(Bundle cellLocation) { //take care of it!
		synchronized (mRecords) {
			initOperations();
            for (Record r : mRecords) {
                if (validateEventsAndUserLocked(r, PhoneStateListener.LISTEN_CELL_LOCATION)) {
                	if(isPackageAllowed(PERMISSION_CELL_LOCATION, r.pkgForDebug)) {
                		mCacheAllow.add(r);
                		PrivacyDebugger.i(P_TAG, "allow package: " + r.pkgForDebug +" for getting CellLocation");
                	} else {
                		mCacheBlock.add(r);
                		PrivacyDebugger.i(P_TAG, "block package: " + r.pkgForDebug +" for getting CellLocation");
                	}
                }
            }
            onPrepareBlockedPackages();
            PrivacyDebugger.i(P_TAG,"now inform blocked packages (cellLocation), count: " + mCacheBlock.size());
            if(mCacheBlock.size() > 0)
            	super.notifyCellLocation(getCellLocationBundle(cellLocation));

            
            onPrepareAllowedPackages();
            PrivacyDebugger.i(P_TAG,"now inform allowed packages (cellLocation), count: " + mCacheAllow.size());
            if(mCacheAllow.size() > 0)
            	super.notifyCellLocation(cellLocation);

            
            onExit();
        }
    }
	
	@Override
	protected void broadcastServiceStateChanged(ServiceState state) {
		if(preventBroadcasting) {
			preventBroadcasting = false;
			PrivacyDebugger.i(P_TAG, "prevent from broadcasting the service state");
			return;
		} else {
			PrivacyDebugger.i(P_TAG,"allowed to broadcast service state");
			super.broadcastServiceStateChanged(PrivacyConstants.PrivacyServiceState.getPrivacyServiceState(state));
		}
			
	}
	
	@Override
	protected void handleRemoveListLocked() {
		if (mRemoveList.size() > 0) {
			mIdlingCache.addAll(mRemoveList);
        }
		super.handleRemoveListLocked();
	}
	
	/**
	 * only call it if you parsed and filled the other caching lists
	 */
	private void onPrepareBlockedPackages () {
		mRecords.clear();
		mRecords.addAll(mCacheBlock);
	}
	
	/**
	 * only call it if you parsed and filled the other caching lists
	 */
	private void onPrepareAllowedPackages () {
		mRecords.clear();
		mRecords.addAll(mCacheAllow);
	}
	
	/**
	 * call it at the end of the operation
	 */
	private void onExit () {
		mRecords.clear();
		mRecords.addAll(mCacheAll);
		
		cleanUp();
		
		PrivacyDebugger.i(P_TAG,"ready with operation. Size of records: " + mRecords.size());
		clearCache();
	}
	
	/**
	 * This this method in the onExit to clean up variables! important!
	 */
	private void cleanUp () {
		if(mIdlingCache.size() > 0) {
			for(IBinder b : mIdlingCache)
				remove(b);
		}
		mIdlingCache.clear();
	}
	
	/**
	 * clears current caches
	 */
	private void clearCache() {
		mCacheAll.clear();
		mCacheAllow.clear();
		mCacheBlock.clear();
		mIdlingCache.clear();
	}
	
	/**
	 * call it right after locking the mRecords list
	 */
	private void initOperations () {
		clearCache();
		mCacheAll.addAll(mRecords);
		PrivacyDebugger.i(P_TAG,"begin operations. Size of records: " + mRecords.size());
	}
	
	/**
	 * Create ready privacy bundle for CellLocation
	 * @param cellLocation current celllocation
	 * @return privacy bundle (cellLocation)
	 */
	private Bundle getCellLocationBundle(Bundle cellLocation) {
		Bundle output = new Bundle();
		if(cellLocation.containsKey("lac")) {
			//it is gsm cell location object, handle it!
			GsmCellLocation location = new GsmCellLocation();
			location.setStateInvalid();
			PrivacyDebugger.i(P_TAG, "now creating fake gsm cellLocation");
			location.fillInNotifierBundle(output);
		} else {
			CdmaCellLocation location = new CdmaCellLocation();
			location.setStateInvalid();
			PrivacyDebugger.i(P_TAG, "now creating fake cdma cellLocation");
			location.fillInNotifierBundle(output);
		}
		return output;
	}
	
    /**
	 * Generates safety cellInfo
	 * @return ready list with cellInfo(s)
	 */
	private List<CellInfo> getPrivacyCellInfo (List<CellInfo> info) {
		List<CellInfo> output = new ArrayList<CellInfo>();
		int mState = -1;
		CellInfo mCache = null;
		for(CellInfo data : info) {
			if(data instanceof CellInfoGsm) {
				mState = CELL_INFO_GSM;
				mCache = data;
				break;
			} else if(data instanceof CellInfoCdma) {
				mState = CELL_INFO_CDMA;
				mCache = data;
				break;
			} else if(data instanceof CellInfoLte) {
				mState = CELL_INFO_LTE;
				mCache = data;
				break;
			}
		}
		if(mCache == null || mState == -1) {
			PrivacyDebugger.e(P_TAG, "SECURITY WARNING! Can't parse fake CellInfo, give empty list as result!");
			return output;
		}
		switch(mState) {
			case CELL_INFO_GSM:
				CellInfoGsm out = new CellInfoGsm((CellInfoGsm)mCache);
				out.setCellIdentity(new CellIdentityGsm(PrivacyConstants.GSM.getMobileCountryCode(), 
														PrivacyConstants.GSM.getMobileNetworkCode(),
														PrivacyConstants.GSM.getLocationAreaCode(),
														PrivacyConstants.GSM.getCellIdentity(),
														PrivacyConstants.GSM.getPrimaryScramblingCode()));//TODO: check for scrambling code! why do java need this stuff? 
				output.add(out);
				PrivacyDebugger.i(P_TAG,"created fake gsm info");
				break;
			case CELL_INFO_CDMA:
				CellInfoCdma out1 = new CellInfoCdma((CellInfoCdma)mCache);
				out1.setCellIdentity(new CellIdentityCdma(	PrivacyConstants.CDMA.getCdmaNetworkId(),
															PrivacyConstants.CDMA.getCdmaSystemId(),
															PrivacyConstants.CDMA.getCdmaBaseStationId(),
															PrivacyConstants.CDMA.getCdmaRandomLon(),
															PrivacyConstants.CDMA.getCdmaRandomLat()));
				output.add(out1);
				PrivacyDebugger.i(P_TAG,"created fake cdma info");
				break;
			case CELL_INFO_LTE:
				CellInfoLte out2 = new CellInfoLte((CellInfoLte)mCache);
				out2.setCellIdentity(new CellIdentityLte(	PrivacyConstants.LTE.getMobileCountryCode(),
															PrivacyConstants.LTE.getMobileNetworkCode(),
															PrivacyConstants.LTE.getCellIdentity(),
															PrivacyConstants.LTE.getPhysicalCellId(),
															PrivacyConstants.LTE.getTrackingAreaCode()));
				
				output.add(out2);
				PrivacyDebugger.i(P_TAG,"created fake lte info");
				break;
		}
		
		return output;
	}
}
