package com.ihs.inputmethod.uimodules.ui.sticker;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import com.ihs.inputmethod.api.utils.HSDisplayUtils;
import com.ihs.inputmethod.uimodules.R;
import com.ihs.inputmethod.uimodules.ui.common.BaseTabViewAdapter;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.util.List;

/**
 * Created by yanxia on 2017/6/5.
 */

public class StickerTabAdapter extends BaseTabViewAdapter {

    private DisplayImageOptions displayImageOptions = new DisplayImageOptions.Builder()
            .cacheInMemory(true)
            .showImageOnFail(null)
            .imageScaleType(ImageScaleType.EXACTLY)
            .cacheOnDisk(true).build();

    public StickerTabAdapter(List<String> stickerTabNameList, OnTabChangeListener onTabChangeListener) {
        super(stickerTabNameList, onTabChangeListener);
    }

    @Override
    public void onBindViewHolder(TagViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        final String tabName = tabNameList.get(position);
        Drawable tabDrawable = getTabView(tabName);
        if (tabDrawable == null) {
            ImageView stickerTabImageView = (ImageView) holder.itemView.findViewById(R.id.tab_icon_iv);
            int padding = HSDisplayUtils.dip2px(10);
            stickerTabImageView.setPadding(padding, padding, padding, padding);
            StickerGroup stickerGroup = StickerUtils.getStickerGroupByName(tabName);
            String stickerPreviewImageUriStr;
            if (stickerGroup != null) {
                stickerPreviewImageUriStr = stickerGroup.getStickerGroupPreviewImageUri();
            } else {
                StringBuilder stickerPreviewImageUri = new StringBuilder(StickerUtils.getStickerDownloadBaseUrl())
                        .append(tabName).append("/").append(tabName).append(StickerUtils.STICKER_TAB_IMAGE_SUFFIX);
                stickerPreviewImageUriStr = stickerPreviewImageUri.toString();
            }
            ImageLoader.getInstance().displayImage(stickerPreviewImageUriStr, stickerTabImageView, displayImageOptions, new ImageLoadingListener() {
                @Override
                public void onLoadingStarted(String imageUri, View view) {

                }

                @Override
                public void onLoadingFailed(String imageUri, View view, FailReason failReason) {

                }

                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    if (tabName.equals(currentTab)) {
                        view.setAlpha(1.0f);
                    } else {
                        view.setAlpha(0.5f);
                    }
                }

                @Override
                public void onLoadingCancelled(String imageUri, View view) {

                }
            });
        }
    }

    @Override
    protected Drawable getTabView(String tab) {
        return getStickerTabDrawable(tab);
    }

    /**
     * just select tab
     *
     * @param tab
     */
    @Override
    public void setTabSelected(String tab) {
        super.setTabSelected(tab);
        final View stickerTabView = tabImageViews.get(tab);
        if (stickerTabView != null) {
            stickerTabView.setAlpha(1.0f);
        }
    }

    @Override
    protected void clearSelection() {
        super.clearSelection();
        for (final String tab : tabNameList) {
            final View iv = tabImageViews.get(tab);
            if (iv != null && !tab.equals(StickerPanelManager.STICKER_RECENT)) {
                iv.setAlpha(0.5f);
            }
        }
    }
}
