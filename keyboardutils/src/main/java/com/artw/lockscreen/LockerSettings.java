package com.artw.lockscreen;

import com.ihs.app.analytics.HSAnalytics;
import com.ihs.app.framework.HSApplication;
import com.ihs.app.framework.HSSessionMgr;
import com.ihs.commons.config.HSConfig;
import com.ihs.commons.utils.HSLog;
import com.ihs.commons.utils.HSPreferenceHelper;
import com.ihs.keyboardutils.R;
import com.ihs.keyboardutils.utils.PublisherUtils;


public class LockerSettings {

    public static final int LOCKER_MUTED = 0;
    public static final int LOCKER_DEFAULT_DISABLED = 1;
    public static final int LOCKER_DEFAULT_ACTIVE = 2;

    private static final String
            RECORD_CURRENT_PLIST_SETTING = "record_current_plist_setting";

    private static final String PREF_KEY_LOCKER_ENABLED = HSApplication.getContext().getResources().getString(R.string.locker_switcher);
    private static final String PREF_KEY_LOCKER_SHOW_COUNT = "pref_key_locker_show_count";
    private static final String PREF_KEY_LOCKER_ADS_SHOW_COUNT = "pref_key_locker_ads_show_count";
    public static final String LOCKER_PREFS = "lockerprefs";

    private static final String app_screen_locker_enable = "app_screen_locker_enable";
    private static final String app_screen_locker_disable = "app_screen_locker_disable";

    private static final String USER_ENABLED_LOCKER = "user_enabled_locker";

    //与键值有关，所以需要使用默认的prefs
    public static boolean isLockerEnabled() {
        return getLockerEnableStates() == LOCKER_DEFAULT_ACTIVE;
    }

    public static void setLockerEnabled(boolean isEnabled,String from) {
        getPref().putBoolean(USER_ENABLED_LOCKER, isEnabled);

        if (isEnabled) {
            lockerEnableOnce(from);
        } else {
            lockerDisableOnce(from);
        }

        getDefaultPref().putBoolean(PREF_KEY_LOCKER_ENABLED, isEnabled);
    }

    public static void increaseLockerShowCount() {
        getPref().putInt(PREF_KEY_LOCKER_SHOW_COUNT, getLockerShowCount() + 1);
    }

    public static int getLockerShowCount() {
        return getPref().getInt(PREF_KEY_LOCKER_SHOW_COUNT, 0);
    }

    public static void setLockerAdsShowCount() {
        getPref().putInt(PREF_KEY_LOCKER_ADS_SHOW_COUNT, getLockerShowCount());
    }

    public static int getLockerAdsShowCount() {
        return getPref().getInt(PREF_KEY_LOCKER_ADS_SHOW_COUNT, 0);
    }

    public static HSPreferenceHelper getPref() {
        return HSPreferenceHelper.create(HSApplication.getContext(), LOCKER_PREFS);
    }

    public static HSPreferenceHelper getDefaultPref() {
        return HSPreferenceHelper.getDefault();
    }


    /**
     * locker 是否已经被完全关闭
     *
     * @return
     */
    public static boolean isLockerMuted() {
        return getLockerEnableStates() == LOCKER_MUTED;
    }


    static void initLockerState() {
        if (!getDefaultPref().contains(PREF_KEY_LOCKER_ENABLED)) {
            getDefaultPref().putBoolean(PREF_KEY_LOCKER_ENABLED, isLockerEnabled());
        }
    }

    public void refreshLockerSetting() {
        if (HSSessionMgr.getCurrentSessionId() <= 1) {
            getDefaultPref().putBoolean(PREF_KEY_LOCKER_ENABLED, isLockerEnabled());
        }
    }


    public static int getLockerEnableStates() {
        //用户设置过的话，直接返回用户设置的状态。不管plist任何值，包括静默。
        if (getPref().contains(USER_ENABLED_LOCKER)) {
            HSLog.e("locker 获取用户设置");
            return getPref().getBoolean(USER_ENABLED_LOCKER, false) ? LOCKER_DEFAULT_ACTIVE : LOCKER_DEFAULT_DISABLED;
        }

        //用户没有设置过，并且不在第一个session的话。就返回第一个session结束时，被记录的plist值。
        if (HSSessionMgr.getCurrentSessionId() > 3 || getPref().contains(RECORD_CURRENT_PLIST_SETTING)) {
            HSLog.e("locker 获取已经记录值");
            return getPref().getInt(RECORD_CURRENT_PLIST_SETTING, LOCKER_DEFAULT_DISABLED);
        } else {
            HSLog.e("locker 获取plist");
            //第一个session就取plist值
            return getLockerPlistState();
        }
    }

    //session结束退出检查用户是否设置过
    public static void setLockerForFirstSession() {
        //3次
        if(HSSessionMgr.getCurrentSessionId() >= 3){
            return;
        }
        //如果用户设置过了就不用记录
        if (!getPref().contains(USER_ENABLED_LOCKER) && !getPref().contains(RECORD_CURRENT_PLIST_SETTING)) {
            HSLog.e("locker 正在记录 ");
            //没有设置的话就获取当前plist配置 并记录下来。
            getPref().putInt(RECORD_CURRENT_PLIST_SETTING, getLockerPlistState());

        }
        HSLog.e("locker 已经记录 ");
    }

    public static boolean isLockerEnabledBefore() {
        return getPref().contains(app_screen_locker_enable);
    }

    private static void lockerEnableOnce(String from) {
        if (!getPref().contains(app_screen_locker_enable)) {
            getPref().putBoolean(app_screen_locker_enable, true);
            HSAnalytics.logEvent(app_screen_locker_enable, app_screen_locker_enable, from, "install_type", PublisherUtils.getInstallType());
        }
    }

    private static void lockerDisableOnce(String from) {
        if (!getPref().contains(app_screen_locker_disable)) {
            getPref().putBoolean(app_screen_locker_disable, true);
            HSAnalytics.logEvent(app_screen_locker_disable, app_screen_locker_enable, from, "install_type", PublisherUtils.getInstallType());
        }
    }

    private static int getLockerPlistState() {
        return HSConfig.optInteger(LOCKER_MUTED, "Application", "Locker", "state");
    }
}
