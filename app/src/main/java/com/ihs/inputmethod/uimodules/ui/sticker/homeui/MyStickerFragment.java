package com.ihs.inputmethod.uimodules.ui.sticker.homeui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ihs.app.framework.HSApplication;
import com.ihs.commons.utils.HSPreferenceHelper;
import com.ihs.inputmethod.uimodules.R;
import com.ihs.inputmethod.uimodules.ui.sticker.StickerGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by guonan.lv on 17/8/14.
 */

public class MyStickerFragment extends Fragment {
    private RecyclerView recyclerView;
    private StickerCardAdapter stickerCardAdapter;
    private List<StickerModel> stickerModelList = new ArrayList<>();
    public static final String tabTitle = HSApplication.getContext().getString(R.string.tab_sticker_my);

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mysticker, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        initView();
        return view;
    }

    private void initView() {
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 2);
        loadStickerModel();
        stickerCardAdapter = new StickerCardAdapter(stickerModelList, new StickerCardAdapter.OnStickerCardClickListener() {
            @Override
            public void onCardViewClick(StickerModel stickerModel) {

            }

            @Override
            public void onDownloadButtonClick(StickerModel stickerModel) {

            }
        });
        recyclerView.setAdapter(stickerCardAdapter);
        recyclerView.setLayoutManager(layoutManager);
    }

    private void loadStickerModel() {
        String downloadStickerNameListJoin = HSPreferenceHelper.getDefault().getString(StickerHomeFragment.DOWNLOAD_STICKER_NAME_LIST, "");
        List<String> downloadStickerNameList = Arrays.asList(downloadStickerNameListJoin.split("\t"));
        for (String downloadStickerName : downloadStickerNameList) {
            if(!TextUtils.isEmpty(downloadStickerName)) {
                StickerGroup stickerGroup = new StickerGroup(downloadStickerName);
                StickerModel stickerModel = new StickerModel(stickerGroup);
                stickerModel.setIsDownloaded(true);
                stickerModelList.add(stickerModel);
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
}
