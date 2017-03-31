package com.ihs.inputmethod.uimodules;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.ihs.app.framework.HSApplication;
import com.ihs.commons.notificationcenter.HSGlobalNotificationCenter;
import com.ihs.commons.notificationcenter.INotificationObserver;
import com.ihs.commons.utils.HSBundle;
import com.ihs.inputmethod.api.theme.HSKeyboardThemeManager;
import com.ihs.inputmethod.api.theme.HSThemeNewTipController;
import com.ihs.inputmethod.api.utils.HSColorUtils;
import com.ihs.inputmethod.api.utils.HSDisplayUtils;
import com.ihs.inputmethod.api.utils.HSResourceUtils;
import com.ihs.inputmethod.uimodules.settings.SettingsButton;
import com.ihs.inputmethod.uimodules.ui.fonts.common.HSFontSelectViewAdapter;
import com.ihs.inputmethod.uimodules.widget.ClothButton;

import static com.ihs.inputmethod.uimodules.utils.RippleDrawableUtils.getTransparentRippleBackground;


public final class BaseFunctionBar extends LinearLayout implements View.OnClickListener {
    private LinearLayout functionLayout;
    private SettingsButton settingsButton;
    private OnFunctionBarItemClickListener onFunctionBarClickListener;
    private BaseFunction baseFunction;
    private RelativeLayout clothButtonVG;


    public BaseFunctionBar(Context context) {
        this(context, null);
    }

    public BaseFunctionBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseFunctionBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        HSGlobalNotificationCenter.addObserver(HSKeyboardThemeManager.HS_NOTIFICATION_THEME_CHANGED, observer);
        HSGlobalNotificationCenter.addObserver(HSFontSelectViewAdapter.HS_NOTIFICATION_FONT_CHANGED, observer);

        setGravity(Gravity.CENTER_VERTICAL);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        functionLayout = (LinearLayout) findViewById(R.id.function_layout);
        initFunctionBar();
    }


    private BaseFunction webSeachView;
    private BaseFunction clothView;


    private void initFunctionBar() {
        settingsButton = new SettingsButton(getContext());
        baseFunction = new BaseFunction(HSApplication.getContext());
        baseFunction.setId(R.id.func_setting_button);
        baseFunction.setFunctionView(settingsButton);
        baseFunction.setOnClickListener(this);
        baseFunction.setNewTipStatueChangeListener(settingsButton);
        updateFunctionAndSettingButtonSize();
        functionLayout.addView(baseFunction);

        ClothButton clothButton = new ClothButton(getContext());
        clothView = new BaseFunction(getContext());
        clothView.setId(R.id.func_cloth_button);
        clothView.setFunctionView(clothButton);
        clothView.setOnClickListener(this);

        this.setBackgroundDrawable(getTransparentRippleBackground());

        functionLayout.addView(clothView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));


        //search view
        webSeachView = new BaseFunction(getContext());
        ImageView webIcon = new ImageView(getContext());
        refreshDrawable(webIcon, "menu_search.png", R.drawable.web_search_icon_funcbar);
        webIcon.setScaleType(ImageView.ScaleType.CENTER);
        webSeachView.setFunctionView(webIcon);
        webSeachView.setId(R.id.web_search_icon);
        webSeachView.setOnClickListener(this);

        LayoutParams param = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT, 1.0f);
        functionLayout.addView(new View(getContext()), param);

        if (HSDisplayUtils.getRotation(getContext()) == Surface.ROTATION_0) {
            functionLayout.addView(webSeachView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
//        functionLayout.addView(webSeachView, new LayoutParams(HSDisplayUtils.dip2px(38), LayoutParams.MATCH_PARENT));
    }


    public void setOnFunctionBarClickListener(OnFunctionBarItemClickListener onFunctionBarClickListener) {
        this.onFunctionBarClickListener = onFunctionBarClickListener;
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final Resources res = getResources();
        final int width = HSResourceUtils.getDefaultKeyboardWidth(res);
        final int height = res.getDimensionPixelSize(R.dimen.config_suggestions_strip_height);
        setMeasuredDimension(width, height);
    }

    private void updateFunctionAndSettingButtonSize() {
        final FrameLayout.LayoutParams llp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.gravity = Gravity.CENTER;
        settingsButton.setLayoutParams(llp);

        LayoutParams functionLayoutParam = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, getResources().getDimensionPixelSize(R.dimen.config_suggestions_strip_height));

        baseFunction.setLayoutParams(functionLayoutParam);

    }

    INotificationObserver observer = new INotificationObserver() {
        @Override
        public void onReceive(String s, HSBundle hsBundle) {
            if (HSKeyboardThemeManager.HS_NOTIFICATION_THEME_CHANGED.equals(s)) {
                resetSettingButtonType();
            } else if (HSFontSelectViewAdapter.HS_NOTIFICATION_FONT_CHANGED.equals(s)) {
                resetSettingButtonType();
            }
        }
    };

    private void resetSettingButtonType() {
        setSettingButtonType(SettingsButton.SettingButtonType.MENU);
    }

    @Override
    public void onClick(View view) {
        if (onFunctionBarClickListener != null) {
            onFunctionBarClickListener.onFunctionBarItemClick(view);
        }
    }

    public void setFunctionEnable(boolean enabled) {
        baseFunction.setEnabled(enabled);
    }

    public void setSettingButtonType(int type) {
        settingsButton.setButtonType(type);
        if (type == SettingsButton.SettingButtonType.MENU) {
            webSeachView.setVisibility(VISIBLE);
        } else {
            webSeachView.setVisibility(GONE);
        }

        updateFunctionAndSettingButtonSize();
    }

    public int getSettingButtonType() {
        return settingsButton.getButtonType();
    }

    public void onDestroy() {
        HSGlobalNotificationCenter.removeObserver(observer);
    }

    public void showNewMarkIfNeed() {
        boolean shouldShowNewTip = HSThemeNewTipController.getInstance().hasNewTipNow();
        if (shouldShowNewTip) {
            baseFunction.showNewTip();
        } else {
            baseFunction.hideNewTip();
        }
    }

    public void hideNewMark() {
        baseFunction.hideNewTip();
    }

//    @Override
//    public void onMeasured(int w, int h) {
//        ViewGroup.LayoutParams layoutParams = clothButtonVG.getLayoutParams();
//        layoutParams.width = w;
//    }
//
//    @Override
//    public void onSetMargin(int margin) {
//
//    }

    public interface OnFunctionBarItemClickListener {
        void onFunctionBarItemClick(View v);
    }


    public static Drawable getFuncButtonDrawable(Drawable drawable) {
        Drawable compatDrawable = DrawableCompat.wrap(drawable);
        int defaultValue = Color.TRANSPARENT;
        int normalColor = HSKeyboardThemeManager.getCurrentTheme().getFuncBarButtonColor(HSKeyboardThemeManager.getCurrentTheme().getKeyTextColor(defaultValue));
        int pressedColor = HSColorUtils.darkerColor(normalColor);
        ColorStateList colorStateList = new ColorStateList(
                new int[][]
                        {
                                new int[]{android.R.attr.state_pressed},
                                new int[]{android.R.attr.state_focused},
                                new int[]{android.R.attr.state_activated},
                                new int[]{}
                        },
                new int[]
                        {
                                pressedColor,
                                pressedColor,
                                pressedColor,
                                normalColor
                        }
        );
        DrawableCompat.setTintList(compatDrawable, colorStateList);
        return compatDrawable;
    }


    private void refreshDrawable(ImageView funcButton, String drawableName, int defaultDrawableId) {

        Drawable drawable = HSKeyboardThemeManager.getThemeSettingMenuDrawable(drawableName, null);
        if (drawable == null) {
            drawable = VectorDrawableCompat.create(getResources(), defaultDrawableId, null);
            drawable = getFuncButtonDrawable(drawable);
        }
        funcButton.setImageDrawable(drawable);
    }
}