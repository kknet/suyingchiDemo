package com.ihs.feature.boost;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.ihs.app.analytics.HSAnalytics;
import com.ihs.app.framework.HSApplication;
import com.ihs.app.framework.activity.HSActivity;
import com.ihs.commons.utils.HSLog;
import com.ihs.feature.boost.animation.BlackHoleLayout;
import com.ihs.feature.common.LauncherFiles;
import com.ihs.feature.common.PreferenceHelper;
import com.ihs.keyboardutils.R;

import static android.content.IntentFilter.SYSTEM_HIGH_PRIORITY;


/**
 * An activity for launching boost outside out launcher.
 */
public class BoostActivity extends HSActivity {

    public static final String INTENT_KEY_START_SOURCE = "start_source";
    public static final String INTENT_KEY_BOOST_TYPE = "boost_type";

    public static final int START_SOURCE_FOREIGN_LAUNCHER = 0;

    private ViewGroup mContent;

    private int mStartSource;
    private BoostType mBoostType;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(uiOptions);
        setContentView(R.layout.activity_boost);

        mContent = (ViewGroup) findViewById(android.R.id.content);

        handleIntent(getIntent());

        startForeignIconAnimation();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();// check out if ad shown needed this time.

        // Write down count.
        if (PreferenceHelper.get(LauncherFiles.BOOST_PREFS).getInt(BoostTipUtils.PREF_KEY_BOOST_TIP_SHOW_COUNT, 0) > 0) {
            BoostTipUtils.incrementShowCount();
        }
    }

    private void handleIntent(Intent intent) {
        mStartSource = intent.getIntExtra(INTENT_KEY_START_SOURCE, 0);

        mBoostType = BoostType.values()[intent.getIntExtra(INTENT_KEY_BOOST_TYPE, 0)];
        logBoostClickedEvent();
    }

    private void startForeignIconAnimation() {
        HSAnalytics.logEvent("Boost_Animation_Viewed", "type", "FloatingWindow");
        final BlackHoleLayout blackHoleLayout = new BlackHoleLayout(BoostActivity.this);
        blackHoleLayout.setBoostType(mBoostType);
        blackHoleLayout.setBoostSource(BoostSource.STANDALONE_ACTIVITY);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mContent.addView(blackHoleLayout, params);
        blackHoleLayout.setBlackHoleAnimationListener(new BlackHoleLayout.BlackHoleAnimationListener() {
            @Override
            public void onEnd() {
                finishWithoutAnimation();
            }
        });
        blackHoleLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                blackHoleLayout.startAnimation();
            }
        }, 300);
    }

    private void finishWithoutAnimation() {
        finish();
        overridePendingTransition(0, 0);
    }

    @Override
    public void onBackPressed() {
        finishWithoutAnimation();
        super.onBackPressed();
    }

    private void logBoostClickedEvent() {
        switch (mStartSource) {
            case START_SOURCE_FOREIGN_LAUNCHER:
                HSAnalytics.logEvent("Boost_Shortcut_Clicked");
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        HSLog.d("A", "BoostActivity stopped");
        super.onStop();
    }

    public static void initBoost() {
        final IntentFilter screenFilter = new IntentFilter();
        screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
        screenFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenFilter.setPriority(SYSTEM_HIGH_PRIORITY);
        HSApplication.getContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    ScreenStatusReceiver.onScreenOff();
                } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                    ScreenStatusReceiver.onScreenOn();
                } else if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
                    HSLog.d("Screen Receiver onReceiver screen present");
//                    HSGlobalNotificationCenter.sendNotification(NotificationCondition.EVENT_UNLOCK);
                }
            }
        }, screenFilter);

        Intent addShortcutIntent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
        addShortcutIntent.putExtra("duplicate", false);
        addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "key 1-Tap Boost");
        Context context = HSApplication.getContext();
        addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(context, R.drawable.boost_icon));
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.setClass(context, BoostActivity.class);
        addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launcherIntent);
        context.sendBroadcast(addShortcutIntent);
    }
}
