package android.privacy;
import android.privacy.PrivacySettings;

/** {@hide} */
interface IPrivacySettingsManager
{
    PrivacySettings getSettings(String packageName);
    boolean saveSettings(in PrivacySettings settings);
    boolean deleteSettings(String packageName);
    void notification(String packageName, byte accessMode, String dataType, String output);
    void registerObservers();
    void addObserver(String packageName);
    boolean purgeSettings();
    double getVersion();
    boolean setEnabled(boolean enable);
    boolean setNotificationsEnabled(boolean enable);
    void setBootCompleted();
    void killTask(in String[] packageName, int UID);
    int getLastCallerId(long uniqueId);
    void toggleDebugMode(boolean state);
    boolean isFailSafeActive();
    void setFailSafeMode(boolean state);
    void disableOrEnableApplication(String packageName, int UID, boolean disable);
}
