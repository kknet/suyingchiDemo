package com.ihs.inputmethod.api;

import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import com.acb.interstitialads.AcbInterstitialAdManager;
import com.acb.irrelevant.AcbIrrelevantAdsManager;
import com.acb.nativeads.AcbNativeAdManager;
import com.crashlytics.android.Crashlytics;
import com.ihs.app.alerts.HSAlertMgr;
import com.ihs.app.analytics.HSAnalytics;
import com.ihs.app.framework.HSApplication;
import com.ihs.app.framework.HSNotificationConstant;
import com.ihs.app.framework.HSSessionMgr;
import com.ihs.app.utils.HSVersionControlUtils;
import com.ihs.chargingscreen.HSChargingScreenManager;
import com.ihs.chargingscreen.utils.ChargingManagerUtil;
import com.ihs.chargingscreen.utils.ChargingPrefsUtil;
import com.ihs.commons.analytics.publisher.HSPublisherMgr;
import com.ihs.commons.config.HSConfig;
import com.ihs.commons.diversesession.HSDiverseSession;
import com.ihs.commons.notificationcenter.HSGlobalNotificationCenter;
import com.ihs.commons.notificationcenter.INotificationObserver;
import com.ihs.commons.utils.HSBundle;
import com.ihs.commons.utils.HSLog;
import com.ihs.commons.utils.HSPreferenceHelper;
import com.ihs.inputmethod.api.framework.HSInputMethodService;
import com.ihs.inputmethod.api.theme.HSKeyboardThemeManager;
import com.ihs.inputmethod.delete.HSInputMethodApplication;
import com.ihs.inputmethod.feature.lucky.LuckyActivity;
import com.ihs.inputmethod.uimodules.KeyboardPanelManager;
import com.ihs.inputmethod.uimodules.R;
import com.ihs.inputmethod.uimodules.ui.theme.analytics.ThemeAnalyticsReporter;
import com.ihs.inputmethod.uimodules.ui.theme.iap.IAPManager;
import com.ihs.inputmethod.utils.CustomUIRateAlertUtils;
import com.ihs.keyboardutils.notification.KCNotificationManager;

import java.util.ArrayList;

import io.fabric.sdk.android.Fabric;

import static com.ihs.chargingscreen.HSChargingScreenManager.registerChargingService;
import static com.ihs.inputmethod.charging.ChargingConfigManager.PREF_KEY_USER_SET_CHARGING_TOGGLE;

public class HSUIApplication extends HSInputMethodApplication {

    private Intent actionService;

    private INotificationObserver notificationObserver = new INotificationObserver() {

        @Override
        public void onReceive(String notificationName, HSBundle bundle) {
            if (HSNotificationConstant.HS_SESSION_START.equals(notificationName)) {
//                int currentapiVersion = android.os.Builder.VERSION.SDK_INT;
//                if (currentapiVersion <= android.os.Builder.VERSION_CODES.JELLY_BEAN_MR1) {
//                    HSLog.d("should delay rate alert for sdk version between 4.0 and 4.2");
//                }
                HSAlertMgr.delayRateAlert();
                onSessionStart();
                IAPManager.getManager().queryOwnProductIds();

            } else if (HSNotificationConstant.HS_CONFIG_CHANGED.equals(notificationName)) {
                IAPManager.getManager().onConfigChange();

                registerChargingService();

            } else if (HSNotificationConstant.HS_SESSION_END.equals(notificationName)) {
                ChargingPrefsUtil.getInstance().setChargingForFirstSession();
                if (ChargingPrefsUtil.getChargingEnableStates() == ChargingPrefsUtil.CHARGING_DEFAULT_ACTIVE) {
                    KCNotificationManager.getInstance().removeNotificationEvent("Charging");
                }
            }
        }
    };

    @Override
    public void onCreate() {
        Log.e("time log", "time log application oncreated started");
        super.onCreate();

        if (HSConfig.optBoolean(false, "Application", "RemindChangeKeyboard", "Enable")) {
            startService(new Intent(getApplicationContext(), WakeKeyboardService.class));
        }

        HSGlobalNotificationCenter.addObserver(HSNotificationConstant.HS_SESSION_START, notificationObserver);
        HSGlobalNotificationCenter.addObserver(HSNotificationConstant.HS_CONFIG_CHANGED, notificationObserver);
        HSGlobalNotificationCenter.addObserver(HSNotificationConstant.HS_SESSION_END, notificationObserver);

        //IAPManager.getManager().init()内部也会监听Session Start，由于存储监听集合的数据结构是List，因此确保HSUIApplication先接收SessionStart事件
        IAPManager.getManager().queryOwnProductIds();
        HSKeyboardThemeManager.init();

        AcbNativeAdManager.sharedInstance().initSingleProcessMode(this);

        CustomUIRateAlertUtils.initialize();

        if (!HSLog.isDebugging()) {
            Fabric.with(this, new Crashlytics());//0,5s
        }
        Log.e("time log", "time log application oncreated finished");

        if (HSVersionControlUtils.isFirstLaunchSinceInstallation()) {
            ThemeAnalyticsReporter.getInstance().enableThemeAnalytics(HSKeyboardThemeManager.getCurrentTheme().mThemeName);
        }

        AcbInterstitialAdManager.getInstance(this);

        HSChargingScreenManager.init(true, "Charging Master", getResources().getString(R.string.ad_placement_charging), new HSChargingScreenManager.IChargingScreenListener() {
            @Override
            public void onClosedByChargingPage() {
                PreferenceManager.getDefaultSharedPreferences(HSApplication.getContext()).edit()
                        .putBoolean(getString(R.string.config_charge_switchpreference_key), false).apply();
                HSChargingScreenManager.getInstance().stop();
            }
        });

        setChargingFunctionStatus();

        HSInputMethodService.setKeyboardSwitcher(new KeyboardPanelManager());
        HSInputMethodService.initResourcesBeforeOnCreate();

        registerNotificationEvent();
        LuckyActivity.installShortCut();

        HSPublisherMgr.init(this);
        recordInstallType();

        AcbIrrelevantAdsManager.init(this);
    }

    private void registerNotificationEvent() {

        KCNotificationManager.getInstance().setNotificationResponserType(KCNotificationManager.TYPE_BROADCAST);
        //注册notification事件
        ArrayList<String> eventList = new ArrayList<>();
        eventList.add("ChangeFont");
        eventList.add("Charging");
        eventList.add("SetPhotoAsBackground");
        eventList.add("ChangeTheme");
        for (String event : eventList) {
            Intent resultIntent = new Intent(this, NotificationBroadcastReceiver.class);
            resultIntent.putExtra("eventName", event);
            KCNotificationManager.getInstance().addNotificationEvent(event, resultIntent);
        }
        if (ChargingPrefsUtil.getChargingEnableStates() == ChargingPrefsUtil.CHARGING_DEFAULT_ACTIVE) {
            KCNotificationManager.getInstance().removeNotificationEvent("Charging");
        }
    }

    /**
     * 设置charging
     */
    private void setChargingFunctionStatus() {
        HSPreferenceHelper prefs = HSPreferenceHelper.getDefault(HSApplication.getContext());
        if (HSSessionMgr.getCurrentSessionId() > 1) {
            // 如果不是第一个sesstion 并且 不包含 PREF_KEY_CHARGING_NEW_USER
            if (!prefs.contains(PREF_KEY_USER_SET_CHARGING_TOGGLE)) {
                HSLog.d("jx,未发现remote config变化 shouldOpenChargingFunction");
                ChargingManagerUtil.enableCharging(false, "plist");
                prefs.putBoolean(PREF_KEY_USER_SET_CHARGING_TOGGLE, true);
            } else {
                boolean userSetting = prefs.getBoolean(PREF_KEY_USER_SET_CHARGING_TOGGLE, false);
                if (userSetting) {
                    ChargingManagerUtil.enableCharging(false, "plist");
                }
            }
        } else {
            prefs.putBoolean(PREF_KEY_USER_SET_CHARGING_TOGGLE, false);
        }
    }

    protected void onSessionStart() {
        HSDiverseSession.start();
    }

    private static final String SP_INSTALL_TYPE_ALREADY_RECORD = "SP_INSTALL_TYPE_ALREADY_RECORD";
    private void recordInstallType() {
        boolean alreadyRecord = HSPreferenceHelper.getDefault().getBoolean(SP_INSTALL_TYPE_ALREADY_RECORD, false);
        if (!alreadyRecord) {
            HSPublisherMgr.PublisherData data = HSPublisherMgr.getPublisherData(this);
            if (data.isDefault()) {
                return;
            }

            String installType;
            if (data.getInstallMode() != HSPublisherMgr.PublisherData.InstallMode.NON_ORGANIC) {
                installType = data.getInstallMode().name();
            } else {
                installType = data.getMediaSource();
            }

            HSAnalytics.logEvent("install_type", "install_type", installType);

            HSPreferenceHelper.getDefault().putBoolean(SP_INSTALL_TYPE_ALREADY_RECORD, true);
        }
    }
}
