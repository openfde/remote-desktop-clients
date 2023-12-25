/**
 * Copyright (C) 2012 Iordan Iordanov
 * Copyright (C) 20?? Michael A. MacDonald
 * <p>
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */


package com.iiordanov.bVNC;

import static com.ft.fdevnc.Constants.BASEURL;
import static com.ft.fdevnc.Constants.BASIP;
import static com.ft.fdevnc.Constants.URL_GETALLAPP;
import static com.ft.fdevnc.Constants.URL_STOPAPP;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import android.util.Log;

import androidx.appcompat.view.menu.MenuBuilder;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ft.fdevnc.AppAdapter;
import com.ft.fdevnc.AppListResult;
import com.ft.fdevnc.VncResult;
import com.iiordanov.bVNC.dialogs.AutoXCustomizeDialog;
import com.iiordanov.bVNC.dialogs.RepeaterDialog;
import com.undatech.opaque.ConnectionSettings;
import com.undatech.opaque.util.ConnectionLoader;
import com.undatech.opaque.util.GeneralUtils;
import com.undatech.remoteClientUi.*;
import com.xiaokun.dialogtiplib.dialog_tip.TipLoadDialog;
import com.xiaokun.dialogtiplib.util.AppUtils;
import com.xwdz.http.QuietOkHttp;
import com.xwdz.http.callback.JsonCallBack;
import com.yanzhenjie.recyclerview.SwipeRecyclerView;

import java.util.ArrayList;
import java.util.List;

import me.kareluo.ui.OptionMenu;
import me.kareluo.ui.OptionMenuView;
import me.kareluo.ui.PopupMenuView;
import me.kareluo.ui.PopupView;
import okhttp3.Call;

/**
 * bVNC is the Activity for setting up VNC connections.
 */
public class bVNC extends MainConfiguration {
    private final static String TAG = "androidVNC";
    private LinearLayout layoutUseX11Vnc;
    private LinearLayout layoutAdvancedSettings;
    private EditText sshServer;
    private EditText sshPort;
    private EditText sshUser;
    private EditText portText;
    private EditText passwordText;
    private Button repeaterButton;
    private Button buttonCustomizeX11Vnc;
    private ToggleButton toggleAdvancedSettings;
    private LinearLayout repeaterEntry;
    private TextView repeaterText;
    private RadioGroup groupForceFullScreen;
    private Spinner colorSpinner;
    private EditText textNickname;
    private EditText textUsername;
    private TextView autoXStatus;
    private CheckBox checkboxKeepPassword;
    private CheckBox checkboxUseDpadAsArrows;
    private CheckBox checkboxRotateDpad;
    private CheckBox checkboxUseLastPositionToolbar;
    private CheckBox checkboxUseSshPubkey;
    private CheckBox checkboxPreferHextile;
    private CheckBox checkboxViewOnly;
    private boolean repeaterTextSet;
    public TipLoadDialog tipLoadDialog;

    private Spinner spinnerVncGeometry;
    private EditText resWidth;
    private EditText resHeight;
    private ProgressBar loadingView;
    private SwipeRefreshLayout mRefreshLayout;
    private SwipeRecyclerView mRecyclerView;
    private AppAdapter mAdapter;
    private List<AppListResult.DataBeanX.DataBean> mDataList = new ArrayList<>();

    private int page = 1;
    private int pageSize = 100;
    //todo mock addr
    private boolean MOCK_ADDR = false;
    private String shortcutApp;
    private String shortcuPath;
    private boolean fromShortcut;
    private boolean reentry;

    @Override
    public void onCreate(Bundle icicle) {
        Log.d(TAG, "onCreate called");
        layoutID = R.layout.main;
        super.onCreate(icicle);

        AppUtils.init(this);
        initAppList();

        sshServer = (EditText) findViewById(R.id.sshServer);
        sshPort = (EditText) findViewById(R.id.sshPort);
        sshUser = (EditText) findViewById(R.id.sshUser);
        layoutUseX11Vnc = (LinearLayout) findViewById(R.id.layoutUseX11Vnc);
        portText = (EditText) findViewById(R.id.textPORT);
        passwordText = (EditText) findViewById(R.id.textPASSWORD);
        textNickname = (EditText) findViewById(R.id.textNickname);
        textUsername = (EditText) findViewById(R.id.textUsername);
        autoXStatus = (TextView) findViewById(R.id.autoXStatus);
        loadingView = (ProgressBar) findViewById(R.id.loadingView);
        // Define what happens when the Repeater button is pressed.
        repeaterButton = (Button) findViewById(R.id.buttonRepeater);
        repeaterEntry = (LinearLayout) findViewById(R.id.repeaterEntry);
        repeaterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(R.layout.repeater_dialog);
            }
        });
        tipLoadDialog = new TipLoadDialog(this);


        // Here we say what happens when the Pubkey Checkbox is checked/unchecked.
        checkboxUseSshPubkey = (CheckBox) findViewById(R.id.checkboxUseSshPubkey);

        // Define what happens when somebody clicks on the customize auto X session dialog.
        buttonCustomizeX11Vnc = (Button) findViewById(R.id.buttonCustomizeX11Vnc);
        buttonCustomizeX11Vnc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bVNC.this.updateSelectedFromView();
                showDialog(R.layout.auto_x_customize);
            }
        });

        // Define what happens when somebody selects different VNC connection types.
        connectionType = (Spinner) findViewById(R.id.connectionType);
        connectionType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> ad, View view, int itemIndex, long id) {
                android.util.Log.d(TAG, "connectionType onItemSelected called");
                selectedConnType = itemIndex;
                selected.setConnectionType(selectedConnType);
                selected.save(bVNC.this);
                if (selectedConnType == Constants.CONN_TYPE_PLAIN ||
                        selectedConnType == Constants.CONN_TYPE_ANONTLS ||
                        selectedConnType == Constants.CONN_TYPE_STUNNEL) {
                    setVisibilityOfSshWidgets(View.GONE);
                    setVisibilityOfUltraVncWidgets(View.GONE);
                    ipText.setHint(R.string.address_caption_hint);
                    textUsername.setHint(R.string.username_hint_optional);
                } else if (selectedConnType == Constants.CONN_TYPE_SSH) {
                    setVisibilityOfSshWidgets(View.VISIBLE);
                    setVisibilityOfUltraVncWidgets(View.GONE);
                    if (ipText.getText().toString().equals(""))
                        ipText.setText("localhost");
                    ipText.setHint(R.string.address_caption_hint_tunneled);
                    textUsername.setHint(R.string.username_hint_optional);
                } else if (selectedConnType == Constants.CONN_TYPE_ULTRAVNC) {
                    setVisibilityOfSshWidgets(View.GONE);
                    setVisibilityOfUltraVncWidgets(View.VISIBLE);
                    ipText.setHint(R.string.address_caption_hint);
                    textUsername.setHint(R.string.username_hint);
                } else if (selectedConnType == Constants.CONN_TYPE_VENCRYPT) {
                    setVisibilityOfSshWidgets(View.GONE);
                    textUsername.setVisibility(View.VISIBLE);
                    repeaterEntry.setVisibility(View.GONE);
                    if (passwordText.getText().toString().equals(""))
                        checkboxKeepPassword.setChecked(false);
                    ipText.setHint(R.string.address_caption_hint);
                    textUsername.setHint(R.string.username_hint_vencrypt);
                }
                updateViewFromSelected();
            }

            @Override
            public void onNothingSelected(AdapterView<?> ad) {
            }
        });

        // The advanced settings button.
        toggleAdvancedSettings = (ToggleButton) findViewById(R.id.toggleAdvancedSettings);
        layoutAdvancedSettings = (LinearLayout) findViewById(R.id.layoutAdvancedSettings);
        toggleAdvancedSettings.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean checked) {
                if (checked)
                    layoutAdvancedSettings.setVisibility(View.VISIBLE);
                else
                    layoutAdvancedSettings.setVisibility(View.GONE);
            }
        });

        colorSpinner = (Spinner) findViewById(R.id.colorformat);
        COLORMODEL[] models = COLORMODEL.values();
        ArrayAdapter<COLORMODEL> colorSpinnerAdapter = new ArrayAdapter<COLORMODEL>(this, R.layout.connection_list_entry, models);
        groupForceFullScreen = (RadioGroup) findViewById(R.id.groupForceFullScreen);
        checkboxKeepPassword = (CheckBox) findViewById(R.id.checkboxKeepPassword);
        checkboxUseDpadAsArrows = (CheckBox) findViewById(R.id.checkboxUseDpadAsArrows);
        checkboxRotateDpad = (CheckBox) findViewById(R.id.checkboxRotateDpad);
        checkboxUseLastPositionToolbar = (CheckBox) findViewById(R.id.checkboxUseLastPositionToolbar);
        checkboxPreferHextile = (CheckBox) findViewById(R.id.checkboxPreferHextile);
        checkboxViewOnly = (CheckBox) findViewById(R.id.checkboxViewOnly);
        colorSpinner.setAdapter(colorSpinnerAdapter);
        colorSpinner.setSelection(0);

        spinnerVncGeometry = (Spinner) findViewById(R.id.spinnerVncGeometry);
        resWidth = (EditText) findViewById(R.id.rdpWidth);
        resHeight = (EditText) findViewById(R.id.rdpHeight);

        spinnerVncGeometry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View view, int itemIndex, long id) {
                if (selected != null) {
                    selected.setRdpResType(itemIndex);
                    setRemoteWidthAndHeight();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        repeaterText = (TextView) findViewById(R.id.textRepeaterId);

        setConnectionTypeSpinnerAdapter(R.array.connection_type);
        if(getIntent() != null && getIntent().getExtras() != null){
            shortcutApp = (String)getIntent().getExtras().get("App");
            shortcuPath = (String)getIntent().getExtras().get("Path");
            Log.d("huyang", "onCreate() called with: shortcutApp = [" + shortcuPath + "]  shortcutApp = [" + shortcuPath + "]");
            fromShortcut = !TextUtils.isEmpty(shortcuPath) && !TextUtils.isEmpty(shortcutApp);
        }
        reentry = App.isRunning(getClass().getName());
    }


    private void initAppList() {
        mRefreshLayout = findViewById(R.id.refresh_layout);
        mRefreshLayout.setOnRefreshListener(mRefreshListener); // 刷新监听。

        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
//        mRecyclerView.addItemDecoration(new DefaultItemDecoration(getColor( R.color.divider_color)));
//        mRecyclerView.setOnItemClickListener(mItemClickListener); // RecyclerView Item点击监听。

        mRecyclerView.useDefaultLoadMore(); // 使用默认的加载更多的View。
        mRecyclerView.setLoadMoreListener(mLoadMoreListener); // 加载更多的监听。
        mRecyclerView.setAutoLoadMore(true);
        mAdapter = new AppAdapter(this, mDataList, mItemClickListener);
        mRecyclerView.setAdapter(mAdapter);
        // 请求服务器加载数据。
        getVncAllApp();
    }

    private SwipeRefreshLayout.OnRefreshListener mRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            getVncAllApp();
        }
    };

    private SwipeRecyclerView.LoadMoreListener mLoadMoreListener = new SwipeRecyclerView.LoadMoreListener() {
        @Override
        public void onLoadMore() {
            getVncAllApp();
        }
    };

    private void getVncAllApp() {
        QuietOkHttp.get(BASEURL + URL_GETALLAPP)
                .addParams("page", new Integer(page).toString())
                .addParams("page_size", new Integer(pageSize).toString())
                .setCallbackToMainUIThread(true)
                .execute(new JsonCallBack<AppListResult>() {

                    @Override
                    public void onFailure(Call call, Exception e) {
                        Log.d("huyang", "onFailure() called with: call = [" + call + "], e = [" + e + "]");
                        mRecyclerView.loadMoreFinish(false, false);
                        mRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onSuccess(Call call, AppListResult response) {
                        Log.d("huyang", "onSuccess() called with: call = [" + call + "], response = [" + response + "]");
                        List<AppListResult.DataBeanX.DataBean> data = response.getData().getData();
                        if(fromShortcut){
                            gotoShortcutApp(data);
                            return;
                        }
                        mDataList.addAll(data);
                        mAdapter.notifyDataSetChanged();
                        if (data.size() > 0) {
                            page++;
                        }
                        mRecyclerView.loadMoreFinish(mDataList.size() == 0, response.getData().getPage().getTotal() > mDataList.size());
                        mRefreshLayout.setRefreshing(false);
                    }
                });
    }

    private void gotoShortcutApp(List<AppListResult.DataBeanX.DataBean> data) {
        for (AppListResult.DataBeanX.DataBean bean : data){
            Log.d("huyang", "gotoShortcutApp() called with: bean = [" + bean.getName() + "]");
            if (TextUtils.equals(shortcutApp, bean.getName())){
                load2Start(bean);
            }
        }
    }

    public interface ItemClickListener {
        void onItemClick(View itemView, int position, AppListResult.DataBeanX.DataBean app, boolean isRight);
    }

    ItemClickListener mItemClickListener = new ItemClickListener() {
        @Override
        public void onItemClick(View itemView, int position, AppListResult.DataBeanX.DataBean app, boolean isRight) {
//            loadingView.setVisibility(View.VISIBLE);
            Log.d(TAG, "onItemClick() called with: itemView = [" + itemView + "], position = [" + position + "], app = [" + app + "], isRight = [" + isRight + "]");
            if (isRight) {
                showOptionView(itemView, app);
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

    private void showOptionView(View itemView, AppListResult.DataBeanX.DataBean app) {
        PopupMenuView menuView = new PopupMenuView(this, R.menu.vncsessionmenu, new MenuBuilder(this));
        menuView.setOrientation(LinearLayout.VERTICAL);
        menuView.setOnMenuClickListener(new OptionMenuView.OnOptionMenuClickListener() {
            @Override
            public boolean onOptionMenuClick(int position, OptionMenu menu) {
                if(position == 0){
                    load2Start(app);
                }
                if(position == 2){
                    createShortcut(app);
                }
                return true;
            }
        });
        menuView.setSites(PopupView.SITE_RIGHT, PopupView.SITE_LEFT, PopupView.SITE_TOP, PopupView.SITE_BOTTOM);
        menuView.show(itemView);
    }

    private void createShortcut(AppListResult.DataBeanX.DataBean app) {
        Log.d(TAG, "createShortcut() called with: app = [" + app + "]");
        byte[] decode = Base64.decode(app.getIcon(), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(decode, 0, decode.length);
        Icon icon = Icon.createWithBitmap(bitmap);
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
                        Log.d("huyang", "onFailure() called with: call = [" + call + "], e = [" + e + "]");
                        tipLoadDialog.dismiss();
                    }

                    @Override
                    public void onSuccess(Call call, VncResult.GetPortResult response) {
                        Log.d("huyang", "onSuccess() called with: call = [" + call + "], response = [" + response + "]");
                        ipText.setText(BASIP);
                        portText.setText(Integer.toString(response.Data.Port));
                        save();
                        tryLunchApp(app);
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("huyang", "onResume() called");
        if (MOCK_ADDR) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    tryLunchApp(null);
                }
            }, 3000);
        }
    }

    private void save() {
        Log.d(TAG, "saveConnectionAndCloseLayout called");
        if (selected != null) {
            updateSelectedFromView();
            selected.saveAndWriteRecent(false, this);
        }
    }

    private void tryLunchApp(AppListResult.DataBeanX.DataBean app) {
        if (MOCK_ADDR) {
            ipText.setText("128.128.0.1");
            portText.setText("5903");
            save();
        }
//        if (app != null) {
//            com.ft.fdevnc.Constants.app = app.Name;
//        }
        Utils.hideKeyboard(this, getCurrentFocus());
        android.util.Log.i(TAG, "Launch Connection");

        ActivityManager.MemoryInfo info = Utils.getMemoryInfo(this);
        if (info.lowMemory)
            System.gc();
        if (App.generateCanvasActivityName(app.Name) == null) {
            Toast.makeText(this, "最多打开10个程序", Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(this, GeneralUtils.getClassByName(App.generateCanvasActivityName(app.Name)));
        ConnectionLoader connectionLoader = getConnectionLoader(this);
        if (Utils.isOpaque(this)) {
            ConnectionSettings cs = (ConnectionSettings) connectionLoader.getConnectionsById().get(Integer.toString(app.id));
            cs.loadFromSharedPreferences(getApplicationContext());
            intent.putExtra("com.undatech.opaque.ConnectionSettings", cs);
        } else {
//            ConnectionBean conn = (ConnectionBean) connectionLoader.getConnectionsById().get(Integer.toString(app.id));
//            intent.putExtra(Utils.getConnectionString(this.getApplicationContext()), conn.Gen_getValues());
            intent.putExtra(Utils.getConnectionString(this.getApplicationContext()), selected.Gen_getValues());
        }
        this.startActivity(intent);
        loadingView.setVisibility(View.GONE);
        tipLoadDialog.dismiss();
        if(fromShortcut && reentry){
            finish();
            App.getApp().movetoBack(getClass().getName());
        }
    }

    private ConnectionLoader getConnectionLoader(Context context) {
        boolean connectionsInSharedPrefs = Utils.isOpaque(context);
        ConnectionLoader connectionLoader = new ConnectionLoader(context.getApplicationContext(), (Activity) context, connectionsInSharedPrefs);
        return connectionLoader;
    }


    /**
     * Makes the ssh-related widgets visible/invisible.
     */
    protected void setVisibilityOfSshWidgets(int visibility) {
        Log.d(TAG, "setVisibilityOfSshWidgets called");
        super.setVisibilityOfSshWidgets(visibility);
        layoutUseX11Vnc.setVisibility(visibility);
    }

    /**
     * Enables and disables the EditText boxes for width and height of remote desktop.
     */
    private void setRemoteWidthAndHeight() {
        Log.d(TAG, "setRemoteWidthAndHeight called");
        if (selected.getRdpResType() != Constants.VNC_GEOM_SELECT_CUSTOM) {
            resWidth.setEnabled(false);
            resHeight.setEnabled(false);
        } else {
            resWidth.setEnabled(true);
            resHeight.setEnabled(true);
        }
    }

    /**
     * Makes the uvnc-related widgets visible/invisible.
     */
    private void setVisibilityOfUltraVncWidgets(int visibility) {
        Log.d(TAG, "setVisibilityOfUltraVncWidgets called");
        repeaterEntry.setVisibility(visibility);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        Log.d(TAG, "onCreateDialog called");
        if (id == R.layout.repeater_dialog) {
            return new RepeaterDialog(this);
        } else if (id == R.layout.auto_x_customize) {
            Dialog d = new AutoXCustomizeDialog(this, database);
            d.setCancelable(false);
            return d;
        }
        return null;
    }

    public void updateViewFromSelected() {
        Log.d(TAG, "updateViewFromSelected called");
        if (selected == null)
            return;
        super.commonUpdateViewFromSelected();

        sshServer.setText(selected.getSshServer());
        sshPort.setText(Integer.toString(selected.getSshPort()));
        sshUser.setText(selected.getSshUser());

        checkboxUseSshPubkey.setChecked(selected.getUseSshPubKey());

        // If we are doing automatic X session discovery, then disable
        // vnc address, vnc port, and vnc password, and vice-versa
        if (selectedConnType == 1 && selected.getAutoXEnabled()) {
            ipText.setVisibility(View.GONE);
            portText.setVisibility(View.GONE);
            textUsername.setVisibility(View.GONE);
            passwordText.setVisibility(View.GONE);
            checkboxKeepPassword.setVisibility(View.GONE);
            autoXStatus.setText(R.string.auto_x_enabled);
        } else {
            ipText.setVisibility(View.VISIBLE);
            portText.setVisibility(View.VISIBLE);
            textUsername.setVisibility(View.VISIBLE);
            passwordText.setVisibility(View.VISIBLE);
            checkboxKeepPassword.setVisibility(View.VISIBLE);
            autoXStatus.setText(R.string.auto_x_disabled);
        }

        portText.setText(Integer.toString(selected.getPort()));

        if (selected.getKeepPassword() || selected.getPassword().length() > 0) {
            passwordText.setText(selected.getPassword());
        }
        groupForceFullScreen.check(selected.getForceFull() == BitmapImplHint.AUTO ?
                R.id.radioForceFullScreenAuto : R.id.radioForceFullScreenOn);
        checkboxKeepPassword.setChecked(selected.getKeepPassword());
        checkboxUseDpadAsArrows.setChecked(selected.getUseDpadAsArrows());
        checkboxRotateDpad.setChecked(selected.getRotateDpad());
        checkboxUseLastPositionToolbar.setChecked((!isNewConnection) ? selected.getUseLastPositionToolbar() : this.useLastPositionToolbarDefault());
        checkboxPreferHextile.setChecked(selected.getPrefEncoding() == RfbProto.EncodingHextile);
        checkboxViewOnly.setChecked(selected.getViewOnly());
        textNickname.setText(selected.getNickname());
        textUsername.setText(selected.getUserName());
        COLORMODEL cm = COLORMODEL.C24bit;
        try {
            cm = COLORMODEL.valueOf(selected.getColorModel());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        COLORMODEL[] colors = COLORMODEL.values();

        spinnerVncGeometry.setSelection(selected.getRdpResType());
        resWidth.setText(Integer.toString(selected.getRdpWidth()));
        resHeight.setText(Integer.toString(selected.getRdpHeight()));

        for (int i = 0; i < colors.length; ++i) {
            if (colors[i] == cm) {
                colorSpinner.setSelection(i);
                break;
            }
        }
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
            repeaterText.setText(repeaterId);
            repeaterTextSet = true;
            ipText.setHint(R.string.repeater_caption_hint);
        } else {
            repeaterText.setText(getText(R.string.repeater_empty_text));
            repeaterTextSet = false;
            ipText.setHint(R.string.address_caption_hint);
        }
    }

    protected void updateSelectedFromView() {
        Log.d(TAG, "updateSelectedFromView called");
        super.commonUpdateSelectedFromView();

        if (selected == null) {
            return;
        }
        try {
            selected.setPort(Integer.parseInt(portText.getText().toString()));
            selected.setSshPort(Integer.parseInt(sshPort.getText().toString()));
        } catch (NumberFormatException nfe) {
        }

        selected.setNickname(textNickname.getText().toString());
        selected.setSshServer(sshServer.getText().toString());
        selected.setSshUser(sshUser.getText().toString());

        // If we are using an SSH key, then the ssh password box is used
        // for the key pass-phrase instead.
        selected.setUseSshPubKey(checkboxUseSshPubkey.isChecked());
        selected.setUserName(textUsername.getText().toString());
        selected.setForceFull(groupForceFullScreen.getCheckedRadioButtonId() == R.id.radioForceFullScreenAuto ? BitmapImplHint.AUTO : (groupForceFullScreen.getCheckedRadioButtonId() == R.id.radioForceFullScreenOn ? BitmapImplHint.FULL : BitmapImplHint.TILE));
        selected.setPassword(passwordText.getText().toString());
        selected.setKeepPassword(checkboxKeepPassword.isChecked());
        selected.setUseDpadAsArrows(checkboxUseDpadAsArrows.isChecked());
        selected.setRotateDpad(checkboxRotateDpad.isChecked());
        selected.setUseLastPositionToolbar(checkboxUseLastPositionToolbar.isChecked());
        if (!checkboxUseLastPositionToolbar.isChecked()) {
            selected.setUseLastPositionToolbarMoved(false);
        }
        if (checkboxPreferHextile.isChecked())
            selected.setPrefEncoding(RfbProto.EncodingHextile);
        else
            selected.setPrefEncoding(RfbProto.EncodingTight);
        selected.setViewOnly(checkboxViewOnly.isChecked());
        selected.setRdpResType(spinnerVncGeometry.getSelectedItemPosition());

        try {
            selected.setRdpWidth(Integer.parseInt(resWidth.getText().toString()));
            selected.setRdpHeight(Integer.parseInt(resHeight.getText().toString()));
        } catch (NumberFormatException nfe) {
        }

        selected.setColorModel(((COLORMODEL) colorSpinner.getSelectedItem()).nameString());
        if (repeaterTextSet) {
            selected.setRepeaterId(repeaterText.getText().toString());
            selected.setUseRepeater(true);
        } else {
            selected.setUseRepeater(false);
        }
    }

    public void save(MenuItem item) {
        Log.d(TAG, "save called");
        if (ipText.getText().length() != 0 && portText.getText().length() != 0) {
            saveConnectionAndCloseLayout();
        } else {
            Toast.makeText(this, R.string.vnc_server_empty, Toast.LENGTH_LONG).show();
        }
    }
}
