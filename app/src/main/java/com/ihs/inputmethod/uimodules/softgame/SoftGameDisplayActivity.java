package com.ihs.inputmethod.uimodules.softgame;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.ihs.app.framework.HSApplication;
import com.ihs.app.framework.activity.HSFragmentActivity;
import com.ihs.commons.connection.HSHttpConnection;
import com.ihs.commons.utils.HSError;
import com.ihs.commons.utils.HSJsonUtil;
import com.ihs.inputmethod.api.utils.HSDisplayUtils;
import com.ihs.inputmethod.uimodules.R;
import com.ihs.keyboardutils.iap.RemoveAdsManager;
import com.ihs.keyboardutils.nativeads.NativeAdParams;
import com.ihs.keyboardutils.nativeads.NativeAdView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import softgames.de.softgamesapilib.SoftgamesSearchConfig;


public class SoftGameDisplayActivity extends HSFragmentActivity {

    public static final String SOFT_GAME_PLACEMENT_MESSAGE = "soft_game_placement_msg";
    public static final int SOFT_GAME_LOAD_COUNT = 50;
    public static final String TOP_50_URL = "http://api.famobi.com/feed?a=A-KCVWU&n=50&sort=top_games";
    public static final String JSON_KEY_NUM_GAMES = "num_games";
    public static final String JSON_CATEGORIES = "categories";
    public static final String JSON_GAMES = "games";

    private ArrayList<SoftGameItemBean> softGameItemArrayList = new ArrayList<>();

    private RecyclerView recyclerView;
    private SoftGameItemAdapter softGameItemAdapter;
    private NativeAdView nativeAdView;
    private ProgressBar progressBar;
    private String placementName;

    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Hot Games");
        setContentView(R.layout.activity_soft_game_display);
        Intent intent = getIntent();
        placementName = intent.getStringExtra(SOFT_GAME_PLACEMENT_MESSAGE);

        progressBar = (ProgressBar) findViewById(R.id.soft_game_progress_bar);
        recyclerView = (RecyclerView) findViewById(R.id.soft_game_main_rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(HSApplication.getContext(), LinearLayoutManager.VERTICAL, false));
        if (!RemoveAdsManager.getInstance().isRemoveAdsPurchased()) {
            initNativeAdView();
        }
        softGameItemAdapter = new SoftGameItemAdapter(nativeAdView);
        recyclerView.setAdapter(softGameItemAdapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        final SoftgamesSearchConfig sgConfig = new SoftgamesSearchConfig(SoftGameManager.getPartnerId());
        sgConfig.setSortByPopularity(true);
        sgConfig.setLimit(SOFT_GAME_LOAD_COUNT);


        HSHttpConnection hsHttpConnection = new HSHttpConnection(TOP_50_URL);
        hsHttpConnection.startAsync();
        hsHttpConnection.setConnectionFinishedListener(new HSHttpConnection.OnConnectionFinishedListener() {
            @Override
            public void onConnectionFinished(HSHttpConnection hsHttpConnection) {
                JSONObject bodyJSON = hsHttpConnection.getBodyJSON();
                try {
                    List<Object> jsonMap = HSJsonUtil.toList(bodyJSON.getJSONArray(JSON_GAMES));
                    for (Object stringObjectMap : jsonMap) {
                        Map<String, String> object = (Map<String, String>) stringObjectMap;
                        String name = object.get("name");
                        String description = object.get("description");
                        String thumb = object.get("thumb");
                        String link = object.get("link");
                        SoftGameItemBean bean = new SoftGameItemBean(name, description, thumb, link);
                        softGameItemArrayList.add(bean);
                    }
                    softGameItemAdapter.refreshDataList(softGameItemArrayList);
                    progressBar.setVisibility(View.GONE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConnectionFailed(HSHttpConnection hsHttpConnection, HSError hsError) {
                hsError.getMessage();
            }
        });
    }

    private void initNativeAdView() {
        View view = LayoutInflater.from(HSApplication.getContext()).inflate(R.layout.ad_style_theme_card, null);
        LinearLayout loadingView = (LinearLayout) LayoutInflater.from(HSApplication.getContext()).inflate(R.layout.ad_loading_3, null);
        int width = HSDisplayUtils.getScreenWidthForContent() - HSDisplayUtils.dip2px(16);
        LinearLayout.LayoutParams loadingLP = new LinearLayout.LayoutParams(width, (int) (width / 1.9f));
        loadingView.setLayoutParams(loadingLP);
        loadingView.setGravity(Gravity.CENTER);
        nativeAdView = new NativeAdView(HSApplication.getContext(), view, loadingView);
        nativeAdView.configParams(new NativeAdParams(placementName, width, 1.9f));
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (nativeAdView != null) {
            nativeAdView.release();
            nativeAdView = null;
        }
        super.onDestroy();
    }
}
