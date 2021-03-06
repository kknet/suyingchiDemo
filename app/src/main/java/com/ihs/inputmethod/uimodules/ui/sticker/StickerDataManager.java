package com.ihs.inputmethod.uimodules.ui.sticker;

import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.ihs.app.framework.HSApplication;
import com.ihs.commons.config.HSConfig;
import com.ihs.commons.notificationcenter.HSGlobalNotificationCenter;
import com.ihs.commons.utils.HSBundle;
import com.ihs.commons.utils.HSPreferenceHelper;
import com.ihs.inputmethod.api.framework.HSInputMethodService;
import com.ihs.inputmethod.api.utils.HSYamlUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by yanxia on 2017/6/9.
 */

public class StickerDataManager {
    public static final String STICKER_DATA_LOAD_FINISH_NOTIFICATION = "sticker_data_load_finish";
    public static final String STICKER_GROUP_DOWNLOAD_SUCCESS_NOTIFICATION = "sticker_data_change_finish";
    public static final String STICKER_GROUP_ORIGINAL = "sticker_group_original_position";
    public static final String PREFERENCE_KEY_NEW_STICKER_SET = "sp_key_new_sticker_set";
    private static final String PREFERENCE_STICKER_IS_FIRST_LOAD = "sp_sticker_is_first_load";
    private static final String SHOW_NEW_MASK = "show_new_mask";
    private static StickerDataManager instance;
    private List<StickerGroup> stickerGroups;
    private List<StickerGroup> stickerGroupsInKeyboard;
    private boolean isReady = false;

    private StickerDataManager() {
        stickerGroups = new ArrayList<>();
        stickerGroupsInKeyboard = new ArrayList<>();
        loadStickersAsync();
    }

    public static StickerDataManager getInstance() {
        if (instance == null) {
            synchronized (StickerDataManager.class) {
                if (instance == null) {
                    instance = new StickerDataManager();
                }
            }
        }
        return instance;
    }

    public void onConfigChange() {
        loadStickersAsync();
    }

    private void loadStickersAsync() {
        new LoadDataTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private class LoadDataTask extends AsyncTask<Void, Void, List<StickerGroup>> {

        @Override
        protected List<StickerGroup> doInBackground(Void... params) {
            isReady = false;

            return loadStickers();
        }

        @Override
        protected void onPostExecute(List<StickerGroup> stickerGroupList) {
            List<String> newGroupNameList = new ArrayList<>();

            if (stickerGroups.size() > 0) {
                for (StickerGroup stickerGroup : stickerGroupList) {
                    if (!stickerGroups.contains(stickerGroup)) {
                        newGroupNameList.add(stickerGroup.getStickerGroupName());
                    }
                }
            }
            stickerGroups = stickerGroupList;

            for (StickerGroup stickerGroup : stickerGroups) {
                if (stickerGroup.isShowInKeyboard() || stickerGroup.isInternalStickerGroup()) {
                    if (!stickerGroupsInKeyboard.contains(stickerGroup)) {
                        stickerGroupsInKeyboard.add(stickerGroup);
                    }
                }
            }

            // 如果是第一次加载，则将StickerGroups中前两个位置置为new
            if (isFirstLoad() && stickerGroups.size() > 1) {
                Set<String> firstNewStickerSet = new HashSet<>();
                firstNewStickerSet.add(stickerGroups.get(0).getStickerGroupName());
                firstNewStickerSet.add(stickerGroups.get(1).getStickerGroupName());
                saveCurrentNewStickerSet(HSApplication.getContext(), firstNewStickerSet);
                saveFirstLoadState();
            }

            // 有新的Sticker
            if (newGroupNameList.size() > 0) {
                Set<String> currentNewStickerSet = getCurrentNewStickerSet(HSApplication.getContext());
                currentNewStickerSet.addAll(newGroupNameList);
                saveCurrentNewStickerSet(HSApplication.getContext(), currentNewStickerSet);
                saveShowNewMask(true);
            } else if (!shouldShowNewMask()) { //如果新Sticker的为空而且从未点击过plusButton
                saveShowNewMask(false);
            }

            isReady = true;
            HSGlobalNotificationCenter.sendNotificationOnMainThread(STICKER_DATA_LOAD_FINISH_NOTIFICATION);
        }

    }

    private List<StickerGroup> loadStickers() {

        List<StickerGroup> stickerGroups = new ArrayList<>();
        List<Map<String, Object>> stickerConfigList = (List<Map<String, Object>>) HSConfig.getList("Application", "StickersGifs", "Sticker", "StickerGroupList");
        for (Map<String, Object> map : stickerConfigList) {
            Boolean showAnimatedMark = (Boolean) map.get("showAnimatedMark");
            Boolean showInKeyboard = (Boolean) map.get("showInKeyboard");
            String stickerGroupName = (String) map.get("name");
            String stickerGroupDownloadDisplayName = (String) map.get("showName");
            StickerGroup stickerGroup = new StickerGroup(stickerGroupName);
            stickerGroup.setDownloadDisplayName(stickerGroupDownloadDisplayName);
            stickerGroup.downloadLockerToUnlock = HSYamlUtils.convertObjectToBool(map.get("downloadLockerToUnlock"));
            stickerGroup.needNewVersionToUnlock = HSYamlUtils.convertObjectToBool(map.get("needNewVersionToUnlock"));
            stickerGroup.rateToUnlock = HSYamlUtils.convertObjectToBool(map.get("rateToUnlock"));
            stickerGroup.shareToUnlock = HSYamlUtils.convertObjectToBool(map.get("shareToUnlock"));
            if (showInKeyboard != null) {
                stickerGroup.setShowInKeyboard(showInKeyboard);
            }
            if (showAnimatedMark != null) {
                stickerGroup.setShowAnimatedMark(showAnimatedMark);
            }
            stickerGroups.add(stickerGroup);
        }

        return stickerGroups;
    }

    private boolean isFirstLoad() {
        HSPreferenceHelper helper = HSPreferenceHelper.getDefault();
        return helper.getBoolean(PREFERENCE_STICKER_IS_FIRST_LOAD, true);
    }

    private void saveFirstLoadState() {
        HSPreferenceHelper helper = HSPreferenceHelper.getDefault();
        helper.putBoolean(PREFERENCE_STICKER_IS_FIRST_LOAD, false);
    }

    public void saveCurrentNewStickerSet(Context context, Set<String> currentNewStickerSet) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet(PREFERENCE_KEY_NEW_STICKER_SET, currentNewStickerSet).apply();

    }

    public Set<String> getCurrentNewStickerSet(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getStringSet(PREFERENCE_KEY_NEW_STICKER_SET, new HashSet<>());
    }

    public void saveShowNewMask(boolean show) {
        HSPreferenceHelper.getDefault().putBoolean(SHOW_NEW_MASK, show);
        HSInputMethodService.setShowNewMask(show);
    }

    public boolean shouldShowNewMask() {
        return HSPreferenceHelper.getDefault().getBoolean(SHOW_NEW_MASK, true);
    }

    public void removeNewTipOfStickerGroup(StickerGroup stickerGroup) {
        String stickerGroupName = stickerGroup.getStickerGroupName();
        Set<String> currentNewStickerSet = getCurrentNewStickerSet(HSApplication.getContext());
        if (currentNewStickerSet.contains(stickerGroupName)) {
            currentNewStickerSet.remove(stickerGroupName);
            saveCurrentNewStickerSet(HSApplication.getContext(), currentNewStickerSet);
        }

        if (currentNewStickerSet.isEmpty()) {
            saveShowNewMask(false);
        }
    }

    /**
     * stickerCardAdapter中调用
     *
     * @param stickerGroup
     * @return
     */
    public boolean isNewStickerGroup(StickerGroup stickerGroup) {
        return getCurrentNewStickerSet(HSApplication.getContext()).contains(stickerGroup.getStickerGroupName());

    }

// --Commented out by Inspection START (18/1/11 下午2:41):
//    boolean isStickersReady() {
//        return isReady;
//    }
// --Commented out by Inspection STOP (18/1/11 下午2:41)

    void updateStickerGroupList(StickerGroup stickerGroup) {
        if (!isReady) {
            return;
        }
        stickerGroup.reloadStickers();
        HSBundle bundle = new HSBundle();
        bundle.putObject(STICKER_GROUP_ORIGINAL, stickerGroup);
        HSGlobalNotificationCenter.sendNotificationOnMainThread(STICKER_GROUP_DOWNLOAD_SUCCESS_NOTIFICATION, bundle);
    }

    public List<StickerGroup> getStickerGroupList() {
        if (!isReady) {
            return Collections.emptyList();
        } else {
            return stickerGroups;
        }
    }

    public List<StickerGroup> getStickerGroupsInKeyboardList() {
        if (!isReady) {
            return Collections.emptyList();
        } else {
            return stickerGroupsInKeyboard;
        }
    }

    public boolean isStickerGroupDownloaded(String name) {
        for (StickerGroup stickerGroup : getStickerGroupList()) {
            if (stickerGroup.getStickerGroupName().equals(name)) {
                return stickerGroup.isStickerGroupDownloaded();
            }
        }
        return false;
    }

    public Sticker getSticker(String stickerName) {
        for (StickerGroup stickerGroup : getStickerGroupList()) {
            if (stickerGroup.getStickerGroupName().equals(StickerUtils.getGroupNameByStickerName(stickerName))) {
                if (stickerGroup.getStickerList().isEmpty()) {
                    stickerGroup.reloadStickers();
                }
                return stickerGroup.getSticker(stickerName);
            }
        }
        return null;
    }
}
