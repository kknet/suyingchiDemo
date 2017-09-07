package com.ihs.inputmethod.uimodules.ui.fonts.homeui;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ihs.app.framework.HSApplication;
import com.ihs.commons.config.HSConfig;
import com.ihs.commons.connection.HSHttpConnection;
import com.ihs.commons.utils.HSError;
import com.ihs.inputmethod.api.analytics.HSGoogleAnalyticsUtils;
import com.ihs.inputmethod.api.specialcharacter.HSSpecialCharacter;
import com.ihs.inputmethod.uimodules.R;
import com.ihs.inputmethod.uimodules.ui.fonts.common.HSFontDownloadManager;
import com.ihs.inputmethod.utils.DownloadUtils;
import com.ihs.keyboardutils.adbuffer.AdLoadingView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by guonan.lv on 17/8/14.
 */

public class FontHomeFragment extends Fragment implements FontCardAdapter.OnFontCardClickListener {

    private RecyclerView recyclerView;
    private FontCardAdapter fontCardAdapter;
    private List<FontModel> fontModelList = new ArrayList<>();
    public static final String tabTitle = HSApplication.getContext().getString(R.string.tab_font);

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_font, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        initView();
        return view;
    }

    private void initView() {
        loadFontModel();
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 2);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {

            @Override
            public int getSpanSize(int position) {
                if (fontCardAdapter.getItemViewType(position) == FontCardAdapter.ITEM_TYPE.ITEM_TYPE_MORE.ordinal()) {
                    return 2;
                }
                return 1;
            }
        });
        fontCardAdapter = new FontCardAdapter(fontModelList, this);
        fontCardAdapter.setFragmentType(FontHomeFragment.class.getSimpleName());
        recyclerView.setAdapter(fontCardAdapter);
        recyclerView.setLayoutManager(layoutManager);
    }

    private void loadFontModel() {
        List<Map<String, Object>> fontList = (List<Map<String, Object>>) HSConfig.getList("Application", "FontList");
        for (Map<String, Object> map : fontList) {
            String fontName = (String) map.get("name");
            String example = (String) map.get("example");
            Object SDKVersion = map.get("sdkVersion");
            int minSDKVersion = 0;
            if (SDKVersion != null) {
                minSDKVersion = (int) SDKVersion;
            }
            HSSpecialCharacter hsSpecialCharacter = new HSSpecialCharacter();
            hsSpecialCharacter.name = fontName;
            hsSpecialCharacter.example = example;
            FontModel fontModel = new FontModel(hsSpecialCharacter);
            if (!fontModel.isFontDownloaded() && Build.VERSION.SDK_INT >= minSDKVersion) {
                fontModelList.add(fontModel);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onFontCardClick(final int position) {
        final FontModel fontModel = fontModelList.get(position);
        final String fontName = fontModel.getFontName();
        DownloadUtils.getInstance().startForegroundDownloading(HSApplication.getContext(), fontName, fontModel.getFontDownloadFilePath(fontName), fontModel.getFontDownloadBaseURL(),
                null, new AdLoadingView.OnAdBufferingListener() {
                    @Override
                    public void onDismiss(boolean success) {
                        if (success) {
                            int position = fontModelList.indexOf(fontModel);
                            fontModelList.remove(position);
                            fontCardAdapter.notifyItemRemoved(position);
                            fontCardAdapter.notifyItemRangeChanged(position, fontModelList.size());
                        }
                    }
                }, new HSHttpConnection.OnConnectionFinishedListener() {
                    @Override
                    public void onConnectionFinished(HSHttpConnection hsHttpConnection) {
                        HSFontDownloadManager.getInstance().updateFontModel(fontModel);
                        HSGoogleAnalyticsUtils.getInstance().logAppEvent("font_download_succeed", fontName);
                    }

                    @Override
                    public void onConnectionFailed(HSHttpConnection hsHttpConnection, HSError hsError) {

                    }
                });
    }
}