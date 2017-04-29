package com.ihs.inputmethod.uimodules.ui.theme.ui.adapter.delegate;

import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.ihs.app.framework.HSApplication;
import com.ihs.chargingscreen.utils.ChargingManagerUtil;
import com.ihs.commons.config.HSConfig;
import com.ihs.commons.notificationcenter.HSGlobalNotificationCenter;
import com.ihs.commons.notificationcenter.INotificationObserver;
import com.ihs.commons.utils.HSBundle;
import com.ihs.commons.utils.HSLog;
import com.ihs.inputmethod.api.analytics.HSGoogleAnalyticsUtils;
import com.ihs.inputmethod.api.utils.HSDisplayUtils;
import com.ihs.inputmethod.api.utils.HSToastUtils;
import com.ihs.inputmethod.charging.ChargingConfigManager;
import com.ihs.inputmethod.uimodules.NativeAdViewButtonHelper;
import com.ihs.inputmethod.uimodules.R;
import com.ihs.inputmethod.uimodules.ui.common.adapter.AdapterDelegate;
import com.ihs.inputmethod.uimodules.ui.theme.ui.ThemeHomeFragment;
import com.ihs.inputmethod.uimodules.ui.theme.ui.adapter.ThemeHomeAdapter;
import com.ihs.inputmethod.uimodules.ui.theme.ui.model.ThemeHomeModel;
import com.ihs.keyboardutils.nativeads.NativeAdParams;
import com.ihs.keyboardutils.nativeads.NativeAdView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wenbinduan on 2016/12/22.
 */

public class ThemeAdAdapterDelegate extends AdapterDelegate<List<ThemeHomeModel>> {

	protected Map<String, View> nativeAdViewCached;

	protected List<Map<String, Object>> themeAdInfos;

	private ThemeHomeAdapter.OnThemeAdItemClickListener themeAdOnClickListener;

	private boolean shouldShowChargingEnableCard;

	protected int width;

	private final INotificationObserver notificationObserver = new INotificationObserver() {
		@Override
		public void onReceive(String s, HSBundle hsBundle) {
			if (ThemeHomeFragment.NOTIFICATION_THEME_HOME_DESTROY.equals(s)) {
				release();
				HSGlobalNotificationCenter.removeObserver(notificationObserver);
			}
		}
	};

	public void release() {
		for(Map.Entry<String, View> entry : nativeAdViewCached.entrySet()) {
			if(entry.getValue() instanceof NativeAdView) {
				((NativeAdView)entry.getValue()).release();
			}
		}
	}

	public ThemeAdAdapterDelegate(ThemeHomeAdapter.OnThemeAdItemClickListener themeAdOnClickListener) {
		themeAdInfos = (List<Map<String, Object>>) HSConfig.getList("Application", "NativeAds", "NativeAdPosition", "ThemeAd");
		nativeAdViewCached = new HashMap<>();
		this.themeAdOnClickListener = themeAdOnClickListener;
		HSGlobalNotificationCenter.addObserver(ThemeHomeFragment.NOTIFICATION_THEME_HOME_DESTROY, notificationObserver);
	}

	@Override
	protected boolean isForViewType(@NonNull List<ThemeHomeModel> items, int position) {
		return items.get(position).span == 2 && items.get(position).isAd;
	}

	@NonNull
	@Override
	protected RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent) {
		width = parent.getMeasuredWidth() - parent.getPaddingLeft() - parent.getPaddingRight();
		CardView cardView = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_theme_ad_card, parent, false);
		FrameLayout.LayoutParams layoutParams = new CardView.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
		layoutParams.gravity = Gravity.CENTER;
		int margin = HSApplication.getContext().getResources().getDimensionPixelSize(R.dimen.theme_card_recycler_view_card_margin);
		layoutParams.setMargins(margin, margin, margin, margin);
		cardView.setLayoutParams(layoutParams);
		HSLog.e("CardWidth:" + cardView.getWidth());
		return new ThemeAdViewHolder(cardView);
	}

	@Override
	protected void onBindViewHolder(@NonNull List<ThemeHomeModel> items, final int position, @NonNull final RecyclerView.ViewHolder holder) {

		removeNativeAdViewFromHolder(holder);
		removeChargingEnableViewFromHolder(holder);

		final CardView cardView = (CardView) holder.itemView;

		if (items.get(position).isThemeAd()) {
			if (!this.shouldShowChargingEnableCard) {
				this.shouldShowChargingEnableCard = ChargingConfigManager.getManager().shouldShowEnableChargingCard(true);
				if (this.shouldShowChargingEnableCard) {
					ChargingConfigManager.getManager().increaseEnableCardShowCount();
					HSGoogleAnalyticsUtils.getInstance().logAppEvent("app_themeCard_prompt_show");
				}
			}
		}

		if (items.get(position).isThemeAd() && this.shouldShowChargingEnableCard) {// Show charging enable view
			View chargingEnableView = LayoutInflater.from(HSApplication.getContext()).inflate(R.layout.charging_enable_card, null);
			chargingEnableView.setTag("chargingenableview");
			cardView.addView(chargingEnableView);
			cardView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					HSToastUtils.toastCenterLong(HSApplication.getContext().getString(R.string.charging_enable_toast));
					ChargingManagerUtil.enableCharging(false);
					themeAdOnClickListener.onThemeAdClick(position);
					HSGoogleAnalyticsUtils.getInstance().logAppEvent("app_themeCard_prompt_click");
				}
			});
		} else {// Show ad
			String nativeAd = getNativeAd(position);
			if (nativeAdViewCached.get(nativeAd) == null) {
				View view = LayoutInflater.from(HSApplication.getContext()).inflate(R.layout.ad_style_2, null);
				LinearLayout loadingView = (LinearLayout) LayoutInflater.from(HSApplication.getContext()).inflate(R.layout.ad_loading_3, null);
				LinearLayout.LayoutParams loadingLP = new LinearLayout.LayoutParams(width, (int) (width / 1.9f) + HSDisplayUtils.dip2px(65));
				loadingView.setLayoutParams(loadingLP);
				loadingView.setGravity(Gravity.CENTER);
				loadingView.setTag("loadingview");
				NativeAdView nativeAdView = new NativeAdView(HSApplication.getContext(), view, loadingView);
				nativeAdView.setTag("nativeadview");
				nativeAdView.configParams(new NativeAdParams(nativeAd, width, 1.9f));
				cardView.addView(nativeAdView);
				NativeAdViewButtonHelper.autoHighlight(nativeAdView);
				nativeAdViewCached.put(nativeAd, nativeAdView);
			} else {
				ViewGroup parent = ((ViewGroup) nativeAdViewCached.get(nativeAd).getParent());
				if (parent != null) {
					parent.removeView(nativeAdViewCached.get(nativeAd));
				}
				cardView.addView(nativeAdViewCached.get(nativeAd));
			}
		}
	}

	private void removeNativeAdViewFromHolder(final RecyclerView.ViewHolder holder) {
		View nativeAdView = holder.itemView.findViewWithTag("nativeadview");
		if(nativeAdView != null) {
			((ViewGroup)holder.itemView).removeView(nativeAdView);
		}
	}

	private void removeChargingEnableViewFromHolder(final RecyclerView.ViewHolder holder) {
		View view = holder.itemView.findViewWithTag("chargingenableview");
		if (view != null) {
			((ViewGroup) holder.itemView).removeView(view);
		}
	}

	@Override
	public int getSpanSize(List<ThemeHomeModel> items, int position) {
		return 2;
	}

	protected String getNativeAd(int itemPosition) {
		for(Map<String, Object> item : themeAdInfos) {
			if((int)item.get("Position") == itemPosition - 4) {
				return (String) item.get("NativeAd");
			}
		}
		for(Map<String, Object> item : themeAdInfos) {
			if((int)item.get("Position") == 10000) {
				return (String) item.get("NativeAd");
			}
		}

		return null;
	}
}