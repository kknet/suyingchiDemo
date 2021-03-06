package com.ihs.inputmethod.uimodules.ui.theme.ui.model;

import android.view.View;

import com.ihs.inputmethod.api.keyboard.HSKeyboardTheme;

/**
 * Created by wenbinduan on 2017/1/4.
 */

public final class ThemePanelModel {

	public boolean isTitle;
	public String title;
	public boolean isCustomThemeTitle;
	public View.OnClickListener customTitleOnClickListener;

	public boolean isCustomThemeInEditMode;

	public boolean isMoreButton;
	public boolean isCreateButton;
	public boolean isAddButtonClickToThemeHome;

	public HSKeyboardTheme keyboardTheme;
	public boolean isCustomTheme;

	public ThemePanelModel() {
		isTitle = false;
		title = "";
		isCustomTheme = false;
		customTitleOnClickListener = null;
		isCustomThemeInEditMode = false;
		isMoreButton = false;
		isCreateButton = false;
		isAddButtonClickToThemeHome = false;
		keyboardTheme = null;
		isCustomTheme = false;
	}
}
