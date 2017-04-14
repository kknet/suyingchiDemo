package com.ihs.chargingscreen.utils;

import android.content.Context;

import com.ihs.app.analytics.HSAnalytics;
import com.ihs.app.framework.HSApplication;

public class ChargingAnalytics {

    private static ChargingAnalytics instance = null;
    private static Context context = HSApplication.getContext();

    private String NativeAd_Charging_Load;
    private String NativeAd_Charging_Show;
    private String NativeAd_Charging_Click;

    private ChargingAnalytics() {
        NativeAd_Charging_Load = "NativeAd_" + context.getPackageName() + "_Charging_Load";
        NativeAd_Charging_Show = "NativeAd_" + context.getPackageName() + "_Charging_Show";
        NativeAd_Charging_Click = "NativeAd_" + context.getPackageName() + "_Charging_Click";
    }

    public static synchronized ChargingAnalytics getInstance() {
        if (instance == null) {
            instance = new ChargingAnalytics();
        }
        return instance;
    }


    static final String app_chargingLocker_enable = "app_chargingLocker_enable";//充电锁屏开启 - 每个用户只记一次
    private static final String app_chargingLocker_show = "app_chargingLocker_show";//充电锁屏展示
    private static final String notification_chargingLocker_show = "notification_chargingLocker_show";//充电锁屏通知出现
    private static final String notification_chargingLocker_click = "notification_chargingLocker_click";//充电锁屏通知点击
    private static final String app_chargingLocker_disable_clicked = "app_chargingLocker_disable_clicked";//关闭按钮点击  - 每个用户只记一次
    private static final String app_chargingLocker_disable = "app_chargingLocker_disable";//充电锁屏关闭 - 每个用户只记一次


    public void chargingEnableOnce() {
        if (!ChargingPrefsUtil.getInstance().getSpHelper().contains(app_chargingLocker_enable)) {
            ChargingPrefsUtil.getInstance().getSpHelper().putBoolean(app_chargingLocker_enable, true);
            HSAnalytics.logEvent(app_chargingLocker_enable);
        }
    }

    public void chargingScreenShowed() {
        HSAnalytics.logEvent(app_chargingLocker_show);
    }

    public void chargingEnableNotificationShowed() {
        HSAnalytics.logEvent(notification_chargingLocker_show);
    }

    public void chargingEnableNotificationClicked() {
        HSAnalytics.logEvent(notification_chargingLocker_click);
    }

    public void chargingDisableTouchedOnce() {
        if (!ChargingPrefsUtil.getInstance().getSpHelper().contains(app_chargingLocker_disable_clicked)) {
            ChargingPrefsUtil.getInstance().getSpHelper().putBoolean(app_chargingLocker_disable_clicked, true);
            HSAnalytics.logEvent(app_chargingLocker_disable_clicked);
        }
    }

    public void chargingDisableConfirmedOnce() {
        if (!ChargingPrefsUtil.getInstance().getSpHelper().contains(app_chargingLocker_disable)) {
            ChargingPrefsUtil.getInstance().getSpHelper().putBoolean(app_chargingLocker_disable, true);
            HSAnalytics.logEvent(app_chargingLocker_disable);
        }
    }


    public void nativeAdLoad() {
        HSAnalytics.logEvent(NativeAd_Charging_Load);
    }

    public void nativeAdShow() {
        HSAnalytics.logEvent(NativeAd_Charging_Show);
    }

    public void nativeAdClick() {
        HSAnalytics.logEvent(NativeAd_Charging_Click);
    }
}