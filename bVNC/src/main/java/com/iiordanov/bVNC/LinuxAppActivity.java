package com.iiordanov.bVNC;

import static com.ft.fdevnc.Constants.BASEURL;
import static com.ft.fdevnc.Constants.BASIP;
import static com.ft.fdevnc.Constants.URL_GETALLAPP;
import static com.ft.fdevnc.Constants.URL_STOPAPP;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Configuration;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ft.fdevnc.AppAdapter;
import com.ft.fdevnc.AppListResult;
import com.ft.fdevnc.VncResult;
import com.iiordanov.util.CompatibleConfig;
import com.undatech.remoteClientUi.R;
import com.xiaokun.dialogtiplib.dialog_tip.TipLoadDialog;
import com.xiaokun.dialogtiplib.util.AppUtils;
import com.xiaokun.dialogtiplib.util.DimenUtils;
import com.xwdz.http.QuietOkHttp;
import com.xwdz.http.callback.JsonCallBack;
import com.yanzhenjie.recyclerview.SwipeRecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import razerdp.basepopup.BasePopupWindow;
import razerdp.util.animation.AnimationHelper;
import razerdp.util.animation.ScaleConfig;

public class LinuxAppActivity extends MainConfiguration {
    private final static String TAG = "LinuxAppActivity";

    private SwipeRefreshLayout mRefreshLayout;
    private SwipeRecyclerView mRecyclerView;
    private AppAdapter mAdapter;
    private List<AppListResult.DataBeanX.DataBean> mDataList = new ArrayList<>();

    public TipLoadDialog tipLoadDialog;

    private int port  ;

    private int mPage = 1;
    private int pageSize = 100;

    private String shortcutApp;
    private String shortcuPath;
    private boolean fromShortcut;
    private boolean reentry;
    private int globalWidth;
    private int globalHeight;
    private int screenWidth;
    private int screenHeight;
    private int spanCount;

    private long mLastClickTime = 0;
    public static final long TIME_INTERVAL = 300L;
    @Override
    public void onCreate(Bundle icicle) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        layoutID = R.layout.activity_linux_app;
//        setContentView(R.layout.activity_linux_app);
        super.onCreate(icicle);
        tipLoadDialog = new TipLoadDialog(this);
        AppUtils.init(this);
        initAppList();

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() called");

        if(screenHeight == 0 | screenWidth == 0){
            screenWidth = DimenUtils.getScreenWidth();
            screenHeight = DimenUtils.getScreenHeight();
        } else if( screenHeight != DimenUtils.getScreenHeight()){
            screenWidth = DimenUtils.getScreenWidth();
            screenHeight = DimenUtils.getScreenHeight();
            spanCount = DimenUtils.getScreenWidth() / (int) DimenUtils.dpToPx(160.0f);
            int count = Math.max(spanCount, 3);
            if(spanCount != count){
                initAppList();
            }
        }
        Log.d(TAG, "onConfigurationChanged() screenHeight = [" + screenHeight + "] screenWidth = [" + screenWidth + "]");
    }

    private void initAppList() {
        spanCount = DimenUtils.getScreenWidth() / (int) DimenUtils.dpToPx(160.0f);
        spanCount = Math.max(spanCount, 3);
        mRefreshLayout = findViewById(R.id.refresh_layout);
        mRefreshLayout.setOnRefreshListener(mRefreshListener); // 刷新监听。
        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, spanCount));

        mRecyclerView.useDefaultLoadMore(); // 使用默认的加载更多的View。
        mRecyclerView.setLoadMoreListener(mLoadMoreListener); // 加载更多的监听。
        mRecyclerView.setAutoLoadMore(true);
        mAdapter = new AppAdapter(this, mDataList, mItemClickListener);
        mRecyclerView.setAdapter(mAdapter);
        // 请求服务器加载数据。
        getVncAllApp(true, 1);
        mRefreshLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                globalWidth = mRefreshLayout.getMeasuredWidth();
                globalHeight = mRefreshLayout.getMeasuredHeight();
            }
        });
    }


    private SwipeRefreshLayout.OnRefreshListener mRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            getVncAllApp(true, 1);
        }
    };

    private SwipeRecyclerView.LoadMoreListener mLoadMoreListener = new SwipeRecyclerView.LoadMoreListener() {
        @Override
        public void onLoadMore() {
            getVncAllApp(false, mPage + 1);
        }
    };

    private void getVncAllApp(boolean forceRefresh, int page) {
        QuietOkHttp.get(BASEURL + URL_GETALLAPP)
                .addParams("page", new Integer(page).toString())
                .addParams("page_size", new Integer(pageSize).toString())
                .addParams("refresh", String.valueOf(forceRefresh))
                .addParams("page_enable", "true")
                .setCallbackToMainUIThread(true)
                .execute(new JsonCallBack<AppListResult>() {

                    @Override
                    public void onFailure(Call call, Exception e) {
                        Log.d(TAG, "onFailure() called with: call = [" + call + "], e = [" + e + "]");
                        mRecyclerView.loadMoreFinish(false, false);
                        mRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onSuccess(Call call, AppListResult response) {
                        Log.d(TAG, "onSuccess() called with: call = [" + call + "], response = [" + response + "]");
                        List<AppListResult.DataBeanX.DataBean> data = response.getData().getData();
                        if (fromShortcut) {
                            gotoShortcutApp(data);
                            return;
                        }
                        if (forceRefresh) {
                            mDataList.clear();
                        }
                        mDataList.addAll(data);
                        mAdapter.notifyDataSetChanged();
                        if (data.size() > 0) {
                            mPage = page;
                        }
                        mRecyclerView.loadMoreFinish(mDataList.size() == 0, response.getData().getPage().getTotal() > mDataList.size());
                        mRefreshLayout.setRefreshing(false);

                        if(forceRefresh){
                            mRecyclerView.scrollToPosition(0);
                        }

                        String fromOther =  getIntent().getStringExtra("fromOther");
                        String appName =  getIntent().getStringExtra("vnc_activity_name");
                        if(fromOther !=null){
                            if(mDataList !=null){
                                AppListResult.DataBeanX.DataBean app = new AppListResult.DataBeanX.DataBean();

                                for(int i = 0 ; i < mDataList.size();i++){
                                    if(appName.equals(mDataList.get(i).getName())||appName.equals(mDataList.get(i).getZhName()) ){
                                        app = mDataList.get(i);
                                        break ;
                                    }
                                }
                                Log.d(TAG, "tryStartVncApp:  "+app +", appName "+appName);
                                if(app.getName() != null){
                                    tryStartVncApp(app);
                                    finish();
                                }
                            }
                        }else {
                            Log.d(TAG, "fromOther is null .....");
                        }
                    }
                });
    }

    private void gotoShortcutApp(List<AppListResult.DataBeanX.DataBean> data) {
        for (AppListResult.DataBeanX.DataBean bean : data){
            Log.d(TAG, "gotoShortcutApp() called with: bean = [" + bean.getName() + "]");
            if (TextUtils.equals(shortcutApp, bean.getName())){
                load2Start(bean);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public interface ItemClickListener {
        void onItemClick(View itemView, int position, AppListResult.DataBeanX.DataBean app, boolean isRight, MotionEvent event);
    }

    bVNC.ItemClickListener mItemClickListener = new bVNC.ItemClickListener() {
        @Override
        public void onItemClick(View itemView, int position, AppListResult.DataBeanX.DataBean app, boolean isRight, MotionEvent event) {
            long nowTime = System.currentTimeMillis();
            if (nowTime - mLastClickTime < TIME_INTERVAL) {
                // do something
                mLastClickTime = nowTime;
                Log.d(TAG, "onItemClick() click too quickly");
                return;
            }
            Log.d(TAG, "onItemClick() called with: itemView = [" + itemView + "], position = [" + position + "], app = [" + app + "], isRight = [" + isRight + "]");
            if (isRight) {
                showOptionView(itemView, app, event);
            } else {
                load2Start(app);
            }
        }
    };


    private void load2Start(AppListResult.DataBeanX.DataBean app) {
        tipLoadDialog.setBackground(R.drawable.custom_dialog_bg_corner)
                .setNoShadowTheme()
                .setMsgAndType("正在打开程序:" + app.getName(), TipLoadDialog.ICON_TYPE_LOADING)
                .setTipTime(5000)
                .show();
        selected.setApp(app.id);
        tryStartVncApp(app);
    }

    private Animation createTranslateAnimation(float fromX, float toX, float fromY, float toY) {
        Animation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
                fromX,
                Animation.RELATIVE_TO_SELF,
                toX,
                Animation.RELATIVE_TO_SELF,
                fromY,
                Animation.RELATIVE_TO_SELF,
                toY);
        animation.setDuration(200);
        animation.setInterpolator(new DecelerateInterpolator());
        return animation;
    }

    private void showOptionView(View v, AppListResult.DataBeanX.DataBean app, MotionEvent event) {
        PopupSlideSmall mPopupSlideSmall;
        BasePopupWindow popupWindow;
        int gravity = Gravity.TOP;
        boolean blur =false;
        BasePopupWindow.GravityMode horizontalGravityMode = BasePopupWindow.GravityMode.RELATIVE_TO_ANCHOR;
        BasePopupWindow.GravityMode verticalGravityMode = BasePopupWindow.GravityMode.RELATIVE_TO_ANCHOR;
        float fromX = 0;
        float fromY = 0;
        float toX = 0;
        float toY = 0;
        Animation showAnimation = AnimationHelper.asAnimation()
                .withScale(ScaleConfig.CENTER)
                .toShow();
        Animation dismissAnimation = AnimationHelper.asAnimation()
                .withScale(ScaleConfig.CENTER)
                .toDismiss();
        mPopupSlideSmall = new PopupSlideSmall(v.getContext());
        mPopupSlideSmall.setOptionItemClickListener(new PopupSlideSmall.onAppOptionItemClickListener() {
            @Override
            public void onOptionOpenClick() {
                load2Start(app);
            }

            @Override
            public void onOptionRefreshClick() {
                mRefreshLayout.setRefreshing(true);
                getVncAllApp(true, 1);
            }

            @Override
            public void onOptionShortcutClick() {
                createShortcut(app);
            }

            @Override
            public void onOptionInfoClick() {

            }

            @Override
            public void onOptionCompatibleClick() {
                String showAppName = getRealAppName(app);
                Log.i("bella","app "+app.toString()+" ,showAppName "+showAppName);

                Intent intent = new Intent();
                ComponentName cn = ComponentName.unflattenFromString("com.android.settings/.Settings$SetCompatibleActivity");
                intent.setComponent(cn);
                intent.putExtra("appName", "VNC_"+showAppName);
                intent.putExtra("packageName", showAppName);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
        popupWindow = mPopupSlideSmall;
        boolean withAnchor = true;
        switch (gravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.LEFT:
                fromX = withAnchor ? 1f : -1f;
                break;
            case Gravity.RIGHT:
                fromX = withAnchor ? -1f : 1f;
                break;
        }

        fromY = globalHeight - event.getY() > DimenUtils.dpToPx(240.0f)? -1 : 1;
        gravity = globalHeight - event.getY() > DimenUtils.dpToPx(240.0f)? Gravity.BOTTOM : Gravity.TOP;
        if (fromX != 0 || fromY != 0) {
            showAnimation = createTranslateAnimation(fromX, toX, fromY, toY);
            dismissAnimation = createTranslateAnimation(toX, fromX, toY, fromY);
        }
        popupWindow.setBlurBackgroundEnable(blur);
        popupWindow.setBackground(null);
        popupWindow.setPopupGravityMode(horizontalGravityMode, verticalGravityMode);
        popupWindow.setPopupGravity(gravity);
        popupWindow.setShowAnimation(showAnimation);
        popupWindow.setDismissAnimation(dismissAnimation);
        if (withAnchor) {
            popupWindow.showPopupWindow(v);
        } else {
            popupWindow.showPopupWindow();
        }
    }

    private void createShortcut(AppListResult.DataBeanX.DataBean app) {
        Log.d(TAG, "createShortcut() called with: app = [" + app + "]");
        byte[] decode = Base64.decode(app.getIcon(), Base64.DEFAULT);
//        Bitmap bitmap = BitmapFactory.decodeByteArray(decode, 0, decode.length);
        Icon icon = Icon.createWithBitmap(Utils.getScaledBitmap(decode, this));
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
        if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
//            Intent launchIntentForPackage = getPackageManager().getLaunchIntentForPackage(getPackageName());
            Intent launchIntentForPackage = new Intent(this, bVNC.class);
            launchIntentForPackage.setAction(Intent.ACTION_MAIN);
            launchIntentForPackage.putExtra("App", app.getName());
            launchIntentForPackage.putExtra("Path", app.getName());
            ShortcutInfo pinShortcutInfo = new ShortcutInfo.Builder(this, app.getName())
                    .setShortLabel(app.getName())
                    .setLongLabel(app.getName())
                    .setIcon(icon)
                    .setIntent(launchIntentForPackage)
                    .build();
            Intent pinnedShortcutCallbackIntent = shortcutManager.createShortcutResultIntent(pinShortcutInfo);
            PendingIntent successCallback = PendingIntent.getBroadcast(this, 0,
                    pinnedShortcutCallbackIntent, PendingIntent.FLAG_IMMUTABLE);
            shortcutManager.requestPinShortcut(pinShortcutInfo, successCallback.getIntentSender());
        }
    }

    private void tryStartVncApp(AppListResult.DataBeanX.DataBean app) {
        // todo mock
        QuietOkHttp.post(BASEURL + URL_STOPAPP)
                .setCallbackToMainUIThread(true)
                .addParams("App", app.Name)
                .addParams("Path", app.Path)
                .addParams("SysOnly", "false")
                .execute(new JsonCallBack<VncResult.GetPortResult>() {
                    @Override
                    public void onFailure(Call call, Exception e) {
                        Log.d(TAG, "onFailure() called with: call = [" + call + "], e = [" + e + "]");
                        tipLoadDialog.dismiss();
                    }

                    @Override
                    public void onSuccess(Call call, VncResult.GetPortResult response) {
                        Log.i(TAG, "onSuccess() called with: call = [" + call + "], response = [" + response + "]");
                        ipText.setText(BASIP);
                        port = response.Data.Port;
                        save();
                        tryLunchApp(app);
                    }
                });
    }

    private void save() {
        Log.d(TAG, "saveConnectionAndCloseLayout called");
        if (selected != null) {
            updateSelectedFromView();
//            selected.saveAndWriteRecent(false, this);
        }
    }

    private String getRealAppName(AppListResult.DataBeanX.DataBean app){
        String showAppName = app.getName();
        if(app.getName().contains("~")){
            String[] arrName = app.getName().split("~");
            showAppName = arrName[0]  ;
        }
        return  showAppName;
    }
    private void tryLunchApp(AppListResult.DataBeanX.DataBean app) {
        String showAppName = getRealAppName(app);
        String queryValue = CompatibleConfig.queryValueData(this.getApplicationContext(),showAppName,"isAllowMuliWindows");
        Log.i(TAG,"queryValue "+queryValue + " ----app: "+app);
        if(queryValue !=null && "true".equals(queryValue)){
            if(app.Name.contains("~")){
                String[] arrName = app.getName().split("~");
                app.Name = arrName[0] + "~"+System.currentTimeMillis();
            }else {
                app.Name =  app.getName() +"~"+ System.currentTimeMillis() ;
            }
        }


        Utils.hideKeyboard(this, getCurrentFocus());
        Log.i(TAG, "Launch Connection");

        ActivityManager.MemoryInfo info = Utils.getMemoryInfo(this);
        if (info.lowMemory)
        {
            Log.i(TAG,"lowMemory............ ");
            System.gc();
        }

        Intent intent = new Intent(this, RemoteCanvasActivity.class);

        if (Constants.SURFFIX_SVG.equals(app.getIconType()) || Constants.SURFFIX_SVGZ.equals(app.getIconType()) ) {
            intent.putExtra("vnc_activity_icon_path", Utils.getSVGPath(app.Icon, app.getIconType(), app.getName()));
        } else {
            byte[] decode = Base64.decode(app.getIcon(), Base64.DEFAULT);
            intent.putExtra("vnc_activity_icon", Utils.getScaledBitmap(decode, this));
        }
        if(!TextUtils.isEmpty(app.getName())){
            intent.putExtra("vnc_activity_name", showAppName);
        }
        if(!TextUtils.isEmpty(app.getName())){
            intent.putExtra("vnc_app_path", app.getPath());
        }
        String p = Utils.getConnectionString(this.getApplicationContext());
        ContentValues v = selected.Gen_getValues();
        for (Map.Entry<String, Object> entry : v.valueSet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            // 处理键值对
            Log.d("bella", "ContentValues Key: " + key + ", Value: " + value);
        }
        intent.putExtra(p, v);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(intent);
        tipLoadDialog.dismiss();
        if(fromShortcut && reentry){
            finish();
            App.getApp().movetoBack(getClass().getName());
        }
    }


    public void updateViewFromSelected() {
        Log.d(TAG, "updateViewFromSelected called");
        if (selected == null)
            return;
        super.commonUpdateViewFromSelected();


        // If we are doing automatic X session discovery, then disable
        // vnc address, vnc port, and vnc password, and vice-versa
        if (selectedConnType == 1 && selected.getAutoXEnabled()) {
            ipText.setVisibility(View.GONE);
        } else {
            ipText.setVisibility(View.VISIBLE);
        }


        textNickname.setText(selected.getNickname());
        COLORMODEL cm = COLORMODEL.C24bit;
        try {
            cm = COLORMODEL.valueOf(selected.getColorModel());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        COLORMODEL[] colors = COLORMODEL.values();

        updateRepeaterInfo(selected.getUseRepeater(), selected.getRepeaterId());
    }

    /**
     * Called when changing view to match selected connection or from
     * Repeater dialog to update the repeater information shown.
     *
     * @param repeaterId If null or empty, show text for not using repeater
     */
    public void updateRepeaterInfo(boolean useRepeater, String repeaterId) {
        Log.d(TAG, "updateRepeaterInfo called");
        if (useRepeater) {
            ipText.setHint(R.string.repeater_caption_hint);
        } else {
            ipText.setHint(R.string.address_caption_hint);
        }
    }

    protected void updateSelectedFromView() {
        Log.d(TAG, "updateSelectedFromView called");
        super.commonUpdateSelectedFromView();

        if (selected == null) {
            return;
        }
        selected.setPort(port);
        selected.setNickname(textNickname.getText().toString());
    }

    public void save(MenuItem item) {
        Log.d(TAG, "save called");
        if (port != 0) {
            saveConnectionAndCloseLayout();
        } else {
            Toast.makeText(this, R.string.vnc_server_empty, Toast.LENGTH_LONG).show();
        }
    }
}