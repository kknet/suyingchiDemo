package com.keyboard.common;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.kc.utils.KCAnalytics;
import com.ihs.app.framework.HSApplication;
import com.ihs.app.framework.activity.HSActivity;
import com.ihs.commons.utils.HSLog;
import com.ihs.inputmethod.api.dialogs.HSAlertDialog;
import com.ihs.inputmethod.api.framework.HSInputMethodListManager;
import com.ihs.inputmethod.api.utils.HSDisplayUtils;
import com.ihs.inputmethod.api.utils.HSToastUtils;
import com.ihs.inputmethod.uimodules.R;
import com.ihs.inputmethod.uimodules.widget.CustomDesignAlert;
import com.kc.commons.utils.KCCommonUtils;

public class KeyboardActivationGuideActivity extends HSActivity {
    public static final String EXTRA_DISABLE_ACTIVATION_PROMPT = "disable_activation_prompt";
    public static final String EXTRA_ACTIVATION_PROMPT_MESSAGE = "activation_prompt_message";

    private Handler handler = new Handler(Looper.getMainLooper());

    private boolean isShowingKeyboardPicker = false;

    private boolean isShowingKeyboardSettings = false;

    private boolean isActivityStoppedWhenShowingKeyboardPicker = false;

    private String activationPromptMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (HSInputMethodListManager.isMyInputMethodSelected()) {
            setResult(RESULT_OK);
            finish();
            return;
        }

        boolean shouldDisableActivationPrompt = getIntent().getBooleanExtra(EXTRA_DISABLE_ACTIVATION_PROMPT, false);

        String customMessage = getIntent().getStringExtra(EXTRA_ACTIVATION_PROMPT_MESSAGE);
        if (TextUtils.isEmpty(customMessage)) {
            activationPromptMessage = getString(R.string.dialog_msg_select_keyboard_apply_rain, getString(R.string.app_name));
        } else {
            activationPromptMessage = customMessage;
        }

        if (shouldDisableActivationPrompt) {
            enableOrSelectKeyboard();
        } else {
            showActivationPromptDialog();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 如果从Picker里跳转到键盘列表再跳转回来时会黑屏，所以这种情况下加个标志位来延时Finish来避免黑屏
        // 这里会有误伤，比如锁屏或是广告弹出之类的，会导致不必要的延时，不过概率很小。
        if (isShowingKeyboardPicker) {
            isActivityStoppedWhenShowingKeyboardPicker = true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            getContentResolver().unregisterContentObserver(settingsContentObserver);
        } catch (Exception e) {
            e.printStackTrace();
        }

        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus && isShowingKeyboardPicker) {
            isShowingKeyboardPicker = false;

            if (HSInputMethodListManager.isMyInputMethodSelected()) {
                KeyboardActivationGuideActivity.this.setResult(RESULT_OK);
                finish();
            } else {
                KeyboardActivationGuideActivity.this.setResult(RESULT_CANCELED);
                if (isActivityStoppedWhenShowingKeyboardPicker) {
                    // 这里如果直接Finish，从输入法列表切回时会显示黑屏，所以延迟处理
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                        }
                    }, 1000);
                } else {
                    // 如果直接从Picker返回，不会黑屏，所以不延时
                    finish();
                }
            }
        } else if (hasFocus && isShowingKeyboardSettings) {
            isShowingKeyboardSettings = false;

            try {
                getContentResolver().unregisterContentObserver(settingsContentObserver);
            } catch (Exception e) {
                HSLog.e("unregister ENABLED_INPUT_METHODS content observer failed.");
            }

            if (HSInputMethodListManager.isMyInputMethodEnabled()) {

                Intent i = new Intent(KeyboardActivationGuideActivity.this, KeyboardActivationGuideActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                HSToastUtils.cancel();
                startActivity(i);

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showKeyboardPicker();
                    }
                }, 700);
            } else {
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }

    private void showActivationPromptDialog() {
        AlertDialog alertDialog = HSAlertDialog.build(this)
                .setTitle(getString(R.string.dialog_title_select_keyboard_apply_rain, getString(R.string.app_name)))
                .setMessage(activationPromptMessage)
                .setPositiveButton(getString(R.string.dialog_confirm_select_keyboard_apply_rain), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        KCAnalytics.logEvent("activate_alert_clicked");
                        enableOrSelectKeyboard();
                    }
                }).create();
        KCCommonUtils.showDialog(alertDialog);
        KCAnalytics.logEvent("activate_alert_show");

        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                KeyboardActivationGuideActivity.this.setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    private void enableOrSelectKeyboard() {
        if (!HSInputMethodListManager.isMyInputMethodEnabled()) {
            getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.ENABLED_INPUT_METHODS), false,
                    settingsContentObserver);

            Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);

            try {
                startActivity(intent);
                isShowingKeyboardSettings = true;
            } catch (Exception e) {
                HSToastUtils.toastBottomShort("Launch Settings Failed, please setup keyboard manually");
            }

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showKeyboardEnableDialog();
                }
            }, 500);
        } else {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showKeyboardPicker();
                }
            }, 200);
        }
    }

    private void showKeyboardEnableDialog() {
        CustomDesignAlert dialog = new CustomDesignAlert(HSApplication.getContext());
        dialog.setTitle(getString(R.string.toast_enable_keyboard, getString(R.string.app_name)));
        dialog.setMessage(getString(R.string.alert_attention_messenger, getString(R.string.app_name)));
        dialog.setImageResource(R.drawable.enable_keyboard_alert_top_bg);
        dialog.setPositiveButton(getString(R.string.got_it), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImageView imageCodeProject = new ImageView(KeyboardActivationGuideActivity.this);
                imageCodeProject.setBackgroundResource(R.drawable.toast_enable_rain);
                CustomViewDialog customViewDialog = new CustomViewDialog(imageCodeProject, 3000, Gravity.BOTTOM, 0, HSDisplayUtils.dip2px(20));
                KCCommonUtils.showDialog(customViewDialog);
            }
        });

        KCCommonUtils.showDialog(dialog);
    }

    private void showKeyboardPicker() {
        isShowingKeyboardPicker = true;
        InputMethodManager m = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        m.showInputMethodPicker();

        Toast toast = Toast.makeText(KeyboardActivationGuideActivity.this, getString(R.string.toast_select_keyboard, getString(R.string.app_name)), Toast.LENGTH_LONG);
        toast.show();
    }

    private ContentObserver settingsContentObserver = new ContentObserver(handler) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (HSInputMethodListManager.isMyInputMethodEnabled()) {

                Intent i = new Intent(KeyboardActivationGuideActivity.this, KeyboardActivationGuideActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                HSToastUtils.cancel();
                startActivity(i);

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showKeyboardPicker();
                    }
                }, 700);
            }
        }
    };

    public static class CustomViewDialog extends Dialog {
        private int gravity, xOffset, yOffset;

// --Commented out by Inspection START (18/1/11 下午2:41):
//        public CustomViewDialog(View contentView, int duration) {
//            super(HSApplication.getContext(), R.style.CustomToastDialog);
//            init(contentView, duration, Gravity.CENTER, 0, 0);
//        }
// --Commented out by Inspection STOP (18/1/11 下午2:41)


        public CustomViewDialog(View contentView, int duration, int gravity, int xOffset, int yOffset) {
            super(HSApplication.getContext(), R.style.CustomToastDialog);
            init(contentView, duration, gravity, xOffset, yOffset);
        }

        private void init(View contentView, int duration, int gravity, int xOffSet, int yOffSet) {
            setContentView(contentView);
            setTitle(null);
            contentView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    CustomViewDialog.this.cancel();
                }
            }, duration);
            setCanceledOnTouchOutside(true);

            this.gravity = gravity;
            this.xOffset = xOffSet;
            this.yOffset = yOffSet;
        }


        @Override
        public void show() {
            Window window = getWindow();
            if (window != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                WindowManager.LayoutParams params = window.getAttributes();
                params.y = yOffset;
                params.x = xOffset;
                window.setGravity(gravity);
                window.setAttributes(params);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(HSApplication.getContext())) {
                    getWindow().setType(WindowManager.LayoutParams.TYPE_TOAST);
                } else {
                    getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                }
            }
            super.show();
        }

        @Override
        public boolean dispatchTouchEvent(@NonNull MotionEvent ev) {
            return onTouchEvent(ev);
        }

        @Override
        public boolean onTouchEvent(@NonNull MotionEvent event) {
            cancel();
            return false;
        }

    }
}
