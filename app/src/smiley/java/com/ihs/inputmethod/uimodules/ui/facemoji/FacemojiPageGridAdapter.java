package com.ihs.inputmethod.uimodules.ui.facemoji;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.ihs.app.framework.HSApplication;
import com.ihs.inputmethod.api.theme.HSKeyboardThemeManager;
import com.ihs.inputmethod.uimodules.R;
import com.ihs.inputmethod.uimodules.ui.facemoji.bean.FacemojiSticker;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dsapphire on 15/11/27.
 */
public class FacemojiPageGridAdapter extends BaseAdapter implements Recoverable {

    private FacemojiPageGridView.OnFacemojiClickListener mListener;
    private List<FacemojiSticker> mData;
    private int stickerWidth;
    private int stickerHeight;
    private LayoutInflater mInflater;
    boolean isCurrentThemeDarkBg;


    public FacemojiPageGridAdapter(final List<FacemojiSticker> data,
                                   FacemojiPageGridView.OnFacemojiClickListener listener,
                                   final Context context,
                                   final int stickerWidth, int stickerHeight) {
        this.mData = data;
        this.mListener = listener;
        this.mInflater = LayoutInflater.from(context);
        this.stickerWidth = stickerWidth;
        this.stickerHeight = stickerHeight;
        isCurrentThemeDarkBg = HSKeyboardThemeManager.getCurrentTheme().isDarkBg();
    }

    @Override
    public int getCount() {
        if (mData == null) return 0;
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        if (mData != null) {
            if (mData.size() > position)
                return mData.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        StickerViewHolder holder;
        FacemojiSticker sticker = (FacemojiSticker) getItem(position);
        if (convertView != null) {
            holder = (StickerViewHolder) convertView.getTag();
        }else {
            convertView = mInflater.inflate(R.layout.facemoji_custom_view, null);
            final View containerLayout = convertView.findViewById(R.id.facemoji_layout);
            containerLayout.setLayoutParams(new GridView.LayoutParams(stickerWidth, stickerHeight));
            final FacemojiAnimationView facemojiAnimationView = (FacemojiAnimationView) containerLayout.findViewById(R.id.sticker_player_view);
            final FacemojiView facemojiView = (FacemojiView) containerLayout.findViewById(R.id.facemoji_layout);
            facemojiAnimationView.setSticker(sticker);
            facemojiAnimationView.setTag(sticker);
            holder = new StickerViewHolder();
            holder.facemojiAnimationView = facemojiAnimationView;
            holder.facemojiView = facemojiView;
            holder.facemojiView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null){
                        mListener.onFacemojiClicked(sticker);
                    }
                }
            });
            convertView.setTag(holder);
        }

        holder.facemojiAnimationView.setSticker(sticker);
        holder.facemojiAnimationView.setTag(sticker);

        if (sticker.getName() == null){
            holder.facemojiAnimationView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            Drawable drawable;
            if (isCurrentThemeDarkBg){
                drawable = HSApplication.getContext().getResources().getDrawable(R.drawable.ic_sticker_loading_image);
            }else {
                drawable = HSApplication.getContext().getResources().getDrawable(R.drawable.ic_sticker_loading_image_grey);
            }
            holder.facemojiAnimationView.setImageDrawable(drawable);
        }else {
            holder.facemojiAnimationView.setImageDrawable(null);
            holder.facemojiAnimationView.setScaleType(ImageView.ScaleType.FIT_XY);
            holder.facemojiAnimationView.start();
        }

        return convertView;
    }

    public void setData(List<FacemojiSticker> data) {
        mData = data;
    }

    public static class StickerViewHolder {
        FacemojiView facemojiView;
        FacemojiAnimationView facemojiAnimationView;
    }

    @Override
    public void save() {
        mDataSnapshot.clear();
        mDataSnapshot.addAll(mData);
    }

    @Override
    public void release() {
        mData.clear();
    }

    @Override
    public void restore() {
        mData.addAll(mDataSnapshot);
        mDataSnapshot.clear();
    }

    @Override
    public Recoverable.State currentState() {
        throw new UnsupportedOperationException();
    }

    private List<FacemojiSticker> mDataSnapshot = new ArrayList<>();
}