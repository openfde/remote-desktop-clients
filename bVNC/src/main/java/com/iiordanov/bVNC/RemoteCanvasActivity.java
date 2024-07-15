/**
 * Copyright (C) 2012-2017 Iordan Iordanov
 * Copyright (C) 2010 Michael A. MacDonald
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

//
// CanvasView is the Activity for showing VNC Desktop.
//
package com.iiordanov.bVNC;

import static android.view.inputmethod.InputMethodManager.HIDE_NOT_ALWAYS;
import static com.ft.fdevnc.Constants.BASEURL;
import static com.ft.fdevnc.Constants.BASIP;
import static com.ft.fdevnc.Constants.URL_KILLAPP;
import static com.ft.fdevnc.Constants.URL_STOPAPP;
import static com.undatech.opaque.util.GeneralUtils.debugLog;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.ft.fdevnc.AppListResult;
import com.ft.fdevnc.VncResult;
import com.iiordanov.android.bc.BCFactory;
import com.iiordanov.bVNC.dialogs.EnterTextDialog;
import com.iiordanov.bVNC.dialogs.MetaKeyDialog;
import com.iiordanov.bVNC.input.InputHandler;
import com.iiordanov.bVNC.input.InputHandlerDirectDragPan;
import com.iiordanov.bVNC.input.InputHandlerDirectSwipePan;
import com.iiordanov.bVNC.input.InputHandlerSingleHanded;
import com.iiordanov.bVNC.input.InputHandlerTouchpad;
import com.iiordanov.bVNC.input.MetaKeyBean;
import com.iiordanov.bVNC.input.Panner;
import com.iiordanov.bVNC.input.RemoteCanvasHandler;
import com.iiordanov.bVNC.input.RemoteKeyboard;
import com.iiordanov.util.SamsungDexUtils;
import com.iiordanov.util.UriIntentParser;
import com.undatech.opaque.Connection;
import com.undatech.opaque.ConnectionSettings;
import com.undatech.opaque.MessageDialogs;
import com.undatech.opaque.RemoteClientLibConstants;
import com.undatech.opaque.dialogs.SelectTextElementFragment;
import com.undatech.opaque.util.FileUtils;
import com.undatech.opaque.util.OnTouchViewMover;
import com.undatech.opaque.util.RemoteToolbar;
import com.undatech.remoteClientUi.R;
import com.xwdz.http.QuietOkHttp;
import com.xwdz.http.callback.JsonCallBack;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;

public class RemoteCanvasActivity extends AppCompatActivity implements OnKeyListener,
        SelectTextElementFragment.OnFragmentDismissedListener {
    private final static String TAG = "CanvasAct_ime";
    InputHandler inputHandler;
    private Vibrator myVibrator;
    private RemoteCanvas canvas;
    private int mDecorViewWidth;
    private int mDecorViewHeight;
    private MenuItem[] inputModeMenuItems;
    private MenuItem[] scalingModeMenuItems;
    private InputHandler inputModeHandlers[];
    private Connection connection;
    public static final int[] inputModeIds = {R.id.itemInputTouchpad,
            R.id.itemInputTouchPanZoomMouse,
            R.id.itemInputDragPanZoomMouse,
            R.id.itemInputSingleHanded};
    private static final int scalingModeIds[] = {R.id.itemZoomable, R.id.itemFitToScreen,
            R.id.itemOneToOne};
    public static final int INPUT_MODE_FULL_FUNCTION = 1;
    public static final int INPUT_MODE_ONLY_KEYBOARD = 2;

    private int mInputModeFlag = INPUT_MODE_FULL_FUNCTION;
    ViewOutlineProvider outlineProvider;

    public static final Map<Integer, String> inputModeMap;

    static {
        Map<Integer, String> temp = new HashMap<>();
        temp.put(R.id.itemInputTouchpad, InputHandlerTouchpad.ID);
        temp.put(R.id.itemInputDragPanZoomMouse, InputHandlerDirectDragPan.ID);
        temp.put(R.id.itemInputTouchPanZoomMouse, InputHandlerDirectSwipePan.ID);
        temp.put(R.id.itemInputSingleHanded, InputHandlerSingleHanded.ID);
        inputModeMap = Collections.unmodifiableMap(temp);
    }

    Panner panner;
    Handler handler;

    RelativeLayout layoutKeys;
    LinearLayout layoutArrowKeys;
    ImageButton keyCtrl;
    boolean keyCtrlToggled;
    ImageButton keySuper;
    boolean keySuperToggled;
    ImageButton keyAlt;
    boolean keyAltToggled;
    ImageButton keyTab;
    ImageButton keyEsc;
    ImageButton keyShift;
    boolean keyShiftToggled;
    ImageButton keyUp;
    ImageButton keyDown;
    ImageButton keyLeft;
    ImageButton keyRight;
    boolean hardKeyboardExtended;
    boolean extraKeysHidden = false;
    volatile boolean softKeyboardUp;
    RemoteToolbar toolbar;
    View rootView;
    private boolean isFirst = true;
    private int canvasWidth, canvasHight;

    public DetectEventEditText detectEventEditText;
    private String vnc_activity_name;
    private String vnc_app_path;
    android.content.ClipboardManager mClipboardManager;
    /**
     * This runnable enables immersive mode.
     */
    private Runnable immersiveEnabler = new Runnable() {
        public void run() {
            try {
                if (Utils.querySharedPreferenceBoolean(RemoteCanvasActivity.this,
                        Constants.disableImmersiveTag)) {
                    return;
                }

                if (Constants.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    canvas.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
//                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    );
                }

            } catch (Exception e) {
            }
        }
    };

    /**
     * Enables sticky immersive mode if supported.
     */
    private void enableImmersive() {
        //handler.removeCallbacks(immersiveEnabler);
        //handler.postDelayed(immersiveEnabler, 200);
    }

    /**
     * This runnable disables immersive mode.
     */
    private Runnable immersiveDisabler = new Runnable() {
        public void run() {
            try {
                if (Constants.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    canvas.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    );
                }

            } catch (Exception e) {
            }
        }
    };

    /**
     * Disables sticky immersive mode.
     */
    private void disableImmersive() {
        //handler.removeCallbacks(immersiveDisabler);
        //handler.postDelayed(immersiveDisabler, 200);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Log.d(TAG,"onWindowFocusChanged: " + hasFocus);
        updateClipboard();

//        if (hasFocus) {
//            enableImmersive();
//            ReflectionUtils.set("fde.click_as_touch", "false");
//        }else{
//            ReflectionUtils.set("fde.click_as_touch", "true");
//        }
    }

    private void updateClipboard() {
        if(canvas != null && handler != null){
            handler.post(new ClipboardMonitor(this, canvas, mClipboardManager));
        }
    }

    @SuppressLint("ResourceType")
    @Override
    public void onCreate(Bundle icicle) {
        debugLog(App.debugLog, TAG, "OnCreate called:");
        super.onCreate(icicle);
        if(!bVNC.MOCK_ADDR){
            vnc_activity_name = getIntent().getStringExtra("vnc_activity_name");
            vnc_app_path = getIntent().getStringExtra("vnc_app_path");
            Log.d(TAG, "onCreate():  vnc_app_path :" + vnc_app_path );
            if(!TextUtils.isEmpty(vnc_activity_name)){
                setTitle(getString(R.string.bvnc_app_name) + ":" + vnc_activity_name);
                vnc_activity_name = getString(R.string.bvnc_app_name) + ":" + vnc_activity_name;
            } else {
                setTitle(getString(R.string.bvnc_app_name));
                vnc_activity_name = getString(R.string.bvnc_app_name);
            }

            Bitmap bitmap = (Bitmap) getIntent().getExtras().get("vnc_activity_icon");
            if( bitmap == null){
                String path = getIntent().getExtras().getString("vnc_activity_icon_path");
                bitmap = Utils.getSVGBitmap(path);
            }
            ActivityManager.TaskDescription description = new ActivityManager.TaskDescription(vnc_activity_name , bitmap, 0);
            this.setTaskDescription(description);
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        Utils.showMenu(this);
        setContentView(R.layout.canvas);
        canvas = (RemoteCanvas) findViewById(R.id.canvas);
        canvas.setName(vnc_activity_name);
        detectEventEditText = (DetectEventEditText) findViewById(R.id.inputlayout);
        detectEventEditText.setInputMode(mInputModeFlag);
        detectEventEditText.connect2canvas(canvas);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            canvas.setDefaultFocusHighlightEnabled(false);
        }
        if (Build.VERSION.SDK_INT >= 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        myVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        try {
                            correctAfterRotation();
                        } catch (Exception e) {
                            //e.printStackTrace();
                        }
                        //handler.postDelayed(rotationCorrector, 300);
                    }
                });
        vncConnect();
        if(bVNC.MOCK_ADDR){
            initSendString();
        }
        outlineProvider = getWindow().getDecorView().getOutlineProvider();
        updateStyle();
        mClipboardManager = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

    }

    private void vncConnect() {
        Runnable setModes = new Runnable() {
            public void run() {
                try {
                    setModes();
                } catch (NullPointerException e) {
                }
            }
        };
        Runnable hideKeyboardAndExtraKeys = new Runnable() {
            public void run() {
                try {
                    hideKeyboardAndExtraKeys();
                } catch (NullPointerException e) {
                }
            }
        };

        if (Utils.isOpaque(this)) {
            initializeOpaque(setModes, hideKeyboardAndExtraKeys);
        } else {
            initialize(setModes, hideKeyboardAndExtraKeys);
        }
        if (connection != null && connection.isReadyForConnection()) {
            continueConnecting();
        }
        debugLog(App.debugLog, TAG, "OnCreate complete");

    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "onRestoreInstanceState():  savedInstanceState :" + savedInstanceState + "");
        retryStartVncApp(vnc_activity_name, vnc_app_path);
    }


    private void retryStartVncApp(String appName, String appPath) {
        QuietOkHttp.post(BASEURL + URL_STOPAPP)
                .setCallbackToMainUIThread(true)
                .addParams("App", appName)
                .addParams("Path", appPath)
                .addParams("SysOnly", "false")
                .execute(new JsonCallBack<VncResult.GetPortResult>() {
                    @Override
                    public void onFailure(Call call, Exception e) {
                        Log.d(TAG, "onFailure() called with: call = [" + call + "], e = [" + e + "]");
                    }

                    @Override
                    public void onSuccess(Call call, VncResult.GetPortResult response) {
                        Log.d(TAG, "onSuccess() called with: call = [" + call + "], response = [" + response + "]");
                        canvas.setPort(response.Data.Port);
                        vncConnect();
                    }
                });
    }

    String s = "《中共中央国务院关于促进民营经济发展壮大的意见》（下称《意见》）19日发布，提出31条政策支持民营经济发展。《意见》提出，坚持“两个毫不动摇”，加快营造市场化、法治化、国际化一流营商环境，优化民营经济发展环境，依法保护民营企业产权和企业家权益，全面构建亲清政商关系，使各种所有制经济依法平等使用生产要素、公平参与市场竞争、同等受到法律保护，引导民营企业通过自身改革发展、合规经营、转型升级不断提升发展质量，促进民营经济做大做优做强。《意见》要求，构建高水平社会主义市场经济体制，持续优化稳定公平透明可预期的发展环境，充分激发民营经济生机活力。具体举措包括：持续破除市场准入壁垒，各地区各部门不得以备案、注册、年检、认定、认证、指定、要求设立分公司等形式设定或变相设定准入障碍；全面落实公平竞争政策制度，强化竞争政策基础地位，健全公平竞争制度框架和政策实施机制，坚持对各类所有制企业一视同仁、平等对待；强化制止滥用行政权力排除限制竞争的反垄断执法等。《意见》明确，加大对民营经济政策支持力度，精准制定实施各类支持政策，完善政策执行方式，加强政策协调性，及时回应关切和利益诉求，切实解决实际困难。其中提出，要完善融资支持政策制度。健全银行、保险、担保、券商等多方共同参与的融资风险市场化分担机制；健全中小微企业和个体工商户信用评级和评价体系，加强涉企信用信息归集，推广“信易贷”等服务模式；支持符合条件的民营中小微企业在债券市场融资，鼓励符合条件的民营企业发行科技创新公司债券，推动民营企业债券融资专项支持计划扩大覆盖面、提升增信力度。支持符合条件的民营企业上市融资和再融资。《意见》还要求，健全对各类所有制经济平等保护的法治环境，为民营经济发展营造良好稳定的预期。包括：依法保护民营企业产权和企业家权益；防止和纠正利用行政或刑事手段干预经济纠纷，以及执法司法中的地方保护主义；进一步规范涉产权强制性措施，避免超权限、超范围、超数额、超时限查封扣押冻结财产；对不宜查封扣押冻结的经营性涉案财物，在保证侦查活动正常进行的同时，可以允许有关当事人继续合理使用，并采取必要的保值保管措施，最大限度减少侦查办案对正常办公和合法生产经营的影响；完善涉企案件申诉、再审等机制，健全冤错案件有效防范和常态化纠正机制。《意见》提出，支持提升科技创新能力。鼓励民营企业根据国家战略需要和行业发展趋势，持续加大研发投入，开展关键核心技术攻关，按规定积极承担国家重大科技项目；培育一批关键行业民营科技领军企业、专精特新中小企业和创新能力强的中小企业特色产业集群；加大政府采购创新产品力度，发挥首台（套）保险补偿机制作用，支持民营企业创新产品迭代应用；推动不同所有制企业、大中小企业融通创新，开展共性技术联合攻关；完善高等学校、科研院所管理制度和成果转化机制，调动其支持民营中小微企业创新发展积极性；支持民营企业与科研机构合作建立技术研发中心、产业研究院、中试熟化基地、工程研究中心、制造业创新中心等创新平台；支持民营企业加强基础性前沿性研究和成果转化。此外，《意见》还明确依法规范和引导民营资本健康发展。具体举措包括：健全规范和引导民营资本健康发展的法律制度，为资本设立“红绿灯”，完善资本行为制度规则，集中推出一批“绿灯”投资案例；全面提升资本治理效能，提高资本监管能力和监管体系现代化水平；引导平台经济向开放、创新、赋能方向发展，补齐发展短板弱项，支持平台企业在创造就业、拓展消费、国际竞争中大显身手，推动平台经济规范健康持续发展；鼓励民营企业集中精力做强做优主业，提升核心竞争力。";

    private void initSendString() {
        Button sendString = (Button)findViewById(R.id.button);
        sendString.setVisibility(View.VISIBLE);
        sendString.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int i = 0;
                        while (true){
                            char c = s.charAt(i%(s.length()-1));
                            sendString.setText(String.valueOf(c));
                            canvas.getKeyboard().keyEvent(0xff, null,sendString.getText().toString());
                            i++;
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }

                    }
                }).start();
            }
        });

    }

    public void onTextViewClicked(View view) {
        detectEventEditText.requestFocus();
        detectEventEditText.setFocusableInTouchMode(true);
//        InputMethodManager inputMgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//        inputMgr.showSoftInput(inputlayout,
//                InputMethodManager.SHOW_FORCED);
    }



    @SuppressLint("SourceLockedOrientationActivity")
    void initialize(final Runnable setModes, final Runnable hideKeyboardAndExtraKeys) {
        handler = new RemoteCanvasHandler(this);

        if (Utils.querySharedPreferenceBoolean(this, Constants.keepScreenOnTag))
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Utils.querySharedPreferenceBoolean(this, Constants.forceLandscapeTag))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        Intent i = getIntent();
        Uri data = i.getData();
        boolean isSupportedScheme = isSupportedScheme(data);
        if (isSupportedScheme || !Utils.isNullOrEmptry(i.getType())) {
            if (handleSupportedUri(data)) return;
        } else {
            handleSerializedConnection(i);
        }
        ((RemoteCanvasHandler) handler).setConnection(connection);
        canvas.initializeCanvas(connection, setModes, hideKeyboardAndExtraKeys);
    }

    private void handleSerializedConnection(Intent i) {
        debugLog(App.debugLog, TAG, "Initializing serialized connection");
        connection = new ConnectionBean(this);
        Bundle extras = i.getExtras();

        if (extras != null) {
            debugLog(App.debugLog, TAG, "Loading values from serialized connection");
            connection.populateFromContentValues((ContentValues) extras.getParcelable(Utils.getConnectionString(this)));
            connection.load(this);
        }
        parsePortIfIpv4Address();
        setDefaultProtocolAndSshPorts();
    }

    private void setDefaultProtocolAndSshPorts() {
        if (connection.getPort() == 0)
            connection.setPort(Constants.DEFAULT_PROTOCOL_PORT);

        if (connection.getSshPort() == 0)
            connection.setSshPort(Constants.DEFAULT_SSH_PORT);
    }

    private void parsePortIfIpv4Address() {
        // Parse a HOST:PORT entry but only if not ipv6 address
        String host = connection.getAddress();
        if (!Utils.isValidIpv6Address(host) && host.indexOf(':') > -1) {
            String p = host.substring(host.indexOf(':') + 1);
            try {
                int parsedPort = Integer.parseInt(p);
                connection.setPort(parsedPort);
                connection.setAddress(host.substring(0, host.indexOf(':')));
            } catch (Exception e) {
                Log.i(TAG, "Could not parse port from address, will use default");
            }
        }
    }

    private boolean handleSupportedUri(Uri data) {
        debugLog(App.debugLog, TAG, "Initializing classic connection from Intent.");
        if (isMasterPasswordEnabled()) {
            Utils.showFatalErrorMessage(this, getResources().getString(R.string.master_password_error_intents_not_supported));
            return true;
        }

        createConnectionFromUri(data);
        if (showConnectionScreenOrExitIfNotReadyForConnection()) return true;
        return false;
    }

    private void createConnectionFromUri(Uri data) {
        connection = UriIntentParser.loadFromUriOrCreateNew(data, this);
        String host = null;
        if (data != null) {
            host = data.getHost();
        }
        if (host != null && !host.startsWith(Utils.getConnectionString(this))) {
            UriIntentParser.parseFromUri(this, connection, data);
        }
    }

    private boolean isSupportedScheme(Uri data) {
        boolean isSupportedScheme = false;
        if (data != null) {
            String s = data.getScheme();
            isSupportedScheme = s.equals("rdp") || s.equals("spice") || s.equals("vnc");
        }
        return isSupportedScheme;
    }

    private boolean showConnectionScreenOrExitIfNotReadyForConnection() {
        // we need to save the connection to display the loading screen, so otherwise we should exit
        if (!connection.isReadyForConnection()) {
            Toast.makeText(this, getString(R.string.error_uri_noinfo_nosave), Toast.LENGTH_LONG).show();
            ;
            if (connection.isReadyToBeSaved()) {
                Log.i(TAG, "Exiting - Insufficent information to connect and connection was not saved.");
            } else {
                Log.i(TAG, "Insufficent information to connect, showing connection dialog.");
                // launch appropriate activity
                Class cls = bVNC.class;
                if (Utils.isRdp(this)) {
                    cls = aRDP.class;
                } else if (Utils.isSpice(this)) {
                    cls = aSPICE.class;
                }
                Intent bVncIntent = new Intent(this, cls);
                startActivity(bVncIntent);
            }
            Utils.justFinish(this);
            return true;
        }
        return false;
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private void initializeOpaque(final Runnable setModes, final Runnable hideKeyboardAndExtraKeys) {
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        Intent i = getIntent();
        String vvFileName = retrieveVvFileFromIntent(i);
        if (vvFileName == null) {
            debugLog(App.debugLog, TAG, "Initializing session from connection settings.");
            connection = (ConnectionSettings) i.getSerializableExtra("com.undatech.opaque.ConnectionSettings");
        } else {
            debugLog(App.debugLog, TAG, "Initializing session from vv file: " + vvFileName);
            File f = new File(vvFileName);
            if (!f.exists()) {
                // Quit with an error if the file does not exist.
                MessageDialogs.displayMessageAndFinish(this, R.string.vv_file_not_found, R.string.error_dialog_title);
                return;
            }
            connection = new ConnectionSettings(RemoteClientLibConstants.DEFAULT_SETTINGS_FILE);
            connection.load(getApplicationContext());
        }
        handler = new RemoteCanvasHandler(this, canvas, connection);
        canvas.init(connection, handler, setModes, hideKeyboardAndExtraKeys, vvFileName);
    }


    void continueConnecting() {
        debugLog(App.debugLog, TAG, "continueConnecting");
        // Initialize and define actions for on-screen keys.
        initializeOnScreenKeys();

        canvas.setOnKeyListener(this);
        canvas.setFocusableInTouchMode(true);
        canvas.setDrawingCacheEnabled(false);

        // This code detects when the soft keyboard is up and sets an appropriate visibleHeight in vncCanvas.
        // When the keyboard is gone, it resets visibleHeight and pans zero distance to prevent us from being
        // below the desktop image (if we scrolled all the way down when the keyboard was up).
        // TODO: Move this into a separate thread, and post the visibility changes to the handler.
        //       to avoid occupying the UI thread with this.
        rootView = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                relayoutViews(rootView);
            }
        });

        /**
         * some first time {@link DetectInputConnection#commitText(CharSequence, int)} will called after dispatchkeyevent
         * so send text here
         */
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mDecorViewWidth = getWindow().getDecorView().getWidth();
                mDecorViewHeight = getWindow().getDecorView().getHeight();
//                if (inputlayout != null && !TextUtils.isEmpty(inputlayout.commitText) && isFirst && canvas.isConnected()) {
//                    Log.d(TAG, "huyang onGlobalLayout() called  commitText:" + inputlayout.commitText);
//                    isFirst = false;
//                    inputlayout.commitText = null;
//                    canvas.getKeyboard().keyEvent(0xff, null, inputlayout.commitText);
//                }
            }
        });

        canvas.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if(canvasWidth == 0 || canvasHight == 0){
                    canvasWidth = canvas.getWidth();
                    canvasHight = canvas.getHeight();
                    try {
                        canvas.rfb.requestResolution(canvasWidth, canvasHight);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if(canvasWidth != canvas.getWidth() || canvasHight != canvas.getHeight()){
                    canvasWidth = canvas.getWidth();
                    canvasHight = canvas.getHeight();
                    try {
                        canvas.rfb.requestResolution(canvasWidth, canvasHight);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    relayoutViews(rootView);
                }
            }
        });

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);

        if (Utils.querySharedPreferenceBoolean(this, Constants.leftHandedModeTag)) {
            params.gravity = Gravity.CENTER | Gravity.LEFT;
        } else {
            params.gravity = Gravity.CENTER | Gravity.RIGHT;
        }

        panner = new Panner(this, canvas.handler);

        toolbar = (RemoteToolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        toolbar.getBackground().setAlpha(64);
        toolbar.setLayoutParams(params);
        setSupportActionBar(toolbar);
        showToolbar();
    }

    void relayoutViews(View rootView) {
//        debugLog(App.debugLog, TAG, "onGlobalLayout: start");
        if (canvas == null) {
//            debugLog(App.debugLog, TAG, "onGlobalLayout: canvas null, returning");
            return;
        }

        Rect r = new Rect();

        rootView.getWindowVisibleDisplayFrame(r);
//        debugLog(App.debugLog, TAG, "onGlobalLayout: getWindowVisibleDisplayFrame: " + r.toString());

        // To avoid setting the visible height to a wrong value after an screen unlock event
        // (when r.bottom holds the width of the screen rather than the height due to a rotation)
        // we make sure r.top is zero (i.e. there is no notification bar and we are in full-screen mode)
        // It's a bit of a hack.
        // One additional situation that needed handling was that devices with notches / cutouts don't
        // ever have r.top equal to zero. so a special case for them.
        Rect re = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(re);
        if (r.top == 0 || re.top > 0) {
            if (canvas.myDrawable != null) {
//                debugLog(App.debugLog, TAG, "onGlobalLayout: Setting VisibleDesktopHeight to: " + (r.bottom - re.top));
                canvas.setVisibleDesktopHeight(r.bottom - re.top);
                canvas.relativePan(0, 0);
            } else {
//                debugLog(App.debugLog, TAG, "onGlobalLayout: canvas.myDrawable is null");
            }
        } else {
//            debugLog(App.debugLog, TAG, "onGlobalLayout: Found r.top to be non-zero");
        }

        // Enable/show the toolbar if the keyboard is gone, and disable/hide otherwise.
        // We detect the keyboard if more than 19% of the screen is covered.
        // Use the visible display frame of the decor view to compute notch dimensions.
        int rootViewHeight = rootView.getHeight();

        int layoutKeysBottom = layoutKeys.getBottom();
        int toolbarBottom = toolbar.getBottom();
        int rootViewBottom = layoutKeys.getRootView().getBottom();
        int diffArrowKeysPosition = r.right - re.left - layoutArrowKeys.getRight();
        int diffLayoutKeysPosition = r.bottom - re.top - layoutKeysBottom;
        int diffToolbarPosition = r.bottom - re.top - toolbarBottom - r.bottom / 2;
        int diffToolbarPositionRightAbsolute = r.right - toolbar.getWidth();
        int diffToolbarPositionTopAbsolute = r.bottom - re.top - toolbar.getHeight() - r.bottom / 2;
//        debugLog(App.debugLog, TAG, "onGlobalLayout: before: r.bottom: " + r.bottom +
//                " rootViewHeight: " + rootViewHeight + " re.top: " + re.top + " re.bottom: " + re.bottom +
//                " layoutKeysBottom: " + layoutKeysBottom + " rootViewBottom: " + rootViewBottom + " toolbarBottom: " + toolbarBottom +
//                " diffLayoutKeysPosition: " + diffLayoutKeysPosition + " diffToolbarPosition: " + diffToolbarPosition);

        boolean softKeyboardPositionChanged = false;
        if (r.bottom > rootViewHeight * 0.81) {
//            debugLog(App.debugLog, TAG, "onGlobalLayout: Less than 19% of screen is covered");
            if (softKeyboardUp) {
                softKeyboardPositionChanged = true;
            }
            softKeyboardUp = false;

            // Soft Kbd gone, shift the meta keys and arrows down.
            if (layoutKeys != null) {
//                debugLog(App.debugLog, TAG, "onGlobalLayout: shifting on-screen buttons down by: " + diffLayoutKeysPosition);
                layoutKeys.offsetTopAndBottom(diffLayoutKeysPosition);
                if (!connection.getUseLastPositionToolbar() || !connection.getUseLastPositionToolbarMoved()) {
                    toolbar.offsetTopAndBottom(diffToolbarPosition);
                } else {
                    toolbar.makeVisible(canvas.getWidth() - toolbar.getWidth(),
                            connection.getUseLastPositionToolbarY(),
                            r.right,
                            r.bottom,
                            diffToolbarPositionRightAbsolute,
                            diffToolbarPositionTopAbsolute);
                }
//                debugLog(App.debugLog, TAG, "onGlobalLayout: shifting arrow keys by: " + diffArrowKeysPosition);
                layoutArrowKeys.offsetLeftAndRight(diffArrowKeysPosition);
                if (softKeyboardPositionChanged) {
//                    debugLog(App.debugLog, TAG, "onGlobalLayout: hiding on-screen buttons");
                    setExtraKeysVisibility(View.GONE, false);
                    canvas.invalidate();
                }
            }
        } else {
//            debugLog(App.debugLog, TAG, "onGlobalLayout: More than 19% of screen is covered");
            softKeyboardUp = true;

            //  Soft Kbd up, shift the meta keys and arrows up.
            if (layoutKeys != null) {
//                debugLog(App.debugLog, TAG, "onGlobalLayout: shifting on-screen buttons up by: " + diffLayoutKeysPosition);
                layoutKeys.offsetTopAndBottom(diffLayoutKeysPosition);
                if (!connection.getUseLastPositionToolbar() || !connection.getUseLastPositionToolbarMoved()) {
                    toolbar.offsetTopAndBottom(diffToolbarPosition);
                } else {
                    toolbar.makeVisible(connection.getUseLastPositionToolbarX(),
                            connection.getUseLastPositionToolbarY(),
                            r.right,
                            r.bottom,
                            diffToolbarPositionRightAbsolute,
                            diffToolbarPositionTopAbsolute);
                }
//                debugLog(App.debugLog, TAG, "onGlobalLayout: shifting arrow keys by: " + diffArrowKeysPosition);
                layoutArrowKeys.offsetLeftAndRight(diffArrowKeysPosition);
                if (extraKeysHidden) {
//                    debugLog(App.debugLog, TAG, "onGlobalLayout: on-screen buttons should be hidden");
                    setExtraKeysVisibility(View.GONE, false);
                } else {
//                    debugLog(App.debugLog, TAG, "onGlobalLayout: on-screen buttons should be showing");
                    setExtraKeysVisibility(View.VISIBLE, true);
                }
                canvas.invalidate();
            }
        }
        layoutKeysBottom = layoutKeys.getBottom();
        rootViewBottom = layoutKeys.getRootView().getBottom();
//        debugLog(App.debugLog, TAG, "onGlobalLayout: after: r.bottom: " + r.bottom +
//                " rootViewHeight: " + rootViewHeight + " re.top: " + re.top + " re.bottom: " + re.bottom +
//                " layoutKeysBottom: " + layoutKeysBottom + " rootViewBottom: " + rootViewBottom + " toolbarBottom: " + toolbarBottom +
//                " diffLayoutKeysPosition: " + diffLayoutKeysPosition + " diffToolbarPosition: " + diffToolbarPosition);
    }

    /**
     * Retrieves a vv file from the intent if possible and returns the path to it.
     * @param i
     * @return the vv file name or NULL if no file was discovered.
     */
    private String retrieveVvFileFromIntent(Intent i) {
        final Uri data = i.getData();
        String vvFileName = null;
        final String tempVvFile = getFilesDir() + "/tempfile.vv";
        int msgId = 0;

        debugLog(App.debugLog, TAG, "Got intent: " + i.toString());

        if (data != null) {
            debugLog(App.debugLog, TAG, "Got data: " + data.toString());
            final String dataString = data.toString();
            if (dataString.startsWith("http")) {
                debugLog(App.debugLog, TAG, "Intent is with http scheme.");
                msgId = R.string.error_failed_to_download_vv_http;
                FileUtils.deleteFile(tempVvFile);

                // Spin up a thread to grab the file over the network.
                Thread t = new Thread() {
                    @Override
                    public void run() {
                        try {
                            // Download the file and write it out.
                            URL url = new URL(data.toString());
                            File file = new File(tempVvFile);

                            URLConnection ucon = url.openConnection();
                            FileUtils.outputToFile(ucon.getInputStream(), new File(tempVvFile));

                            synchronized (RemoteCanvasActivity.this) {
                                RemoteCanvasActivity.this.notify();
                            }
                        } catch (IOException e) {
                            int what = RemoteClientLibConstants.VV_OVER_HTTP_FAILURE;
                            if (dataString.startsWith("https")) {
                                what = RemoteClientLibConstants.VV_OVER_HTTPS_FAILURE;
                            }
                            // Quit with an error we could not download the .vv file.
                            handler.sendEmptyMessage(what);
                        }
                    }
                };
                t.start();

                synchronized (this) {
                    try {
                        this.wait(RemoteClientLibConstants.VV_GET_FILE_TIMEOUT);
                    } catch (InterruptedException e) {
                        vvFileName = null;
                        e.printStackTrace();
                    }
                    vvFileName = tempVvFile;
                }
            } else if (dataString.startsWith("file")) {
                debugLog(App.debugLog, TAG, "Intent is with file scheme.");
                msgId = R.string.error_failed_to_obtain_vv_file;
                vvFileName = data.getPath();
            } else if (dataString.startsWith("content")) {
                debugLog(App.debugLog, TAG, "Intent is with content scheme.");
                msgId = R.string.error_failed_to_obtain_vv_content;
                FileUtils.deleteFile(tempVvFile);

                try {
                    FileUtils.outputToFile(getContentResolver().openInputStream(data), new File(tempVvFile));
                    vvFileName = tempVvFile;
                } catch (IOException e) {
                    Log.e(TAG, "Could not write temp file: IOException.");
                    e.printStackTrace();
                } catch (SecurityException e) {
                    Log.e(TAG, "Could not write temp file: SecurityException.");
                    e.printStackTrace();
                }
            }

            // Check if we were successful in obtaining a file and put up an error dialog if not.
            if (dataString.startsWith("http") || dataString.startsWith("file") || dataString.startsWith("content")) {
                if (vvFileName == null)
                    MessageDialogs.displayMessageAndFinish(this, msgId, R.string.error_dialog_title);
            }
            debugLog(App.debugLog, TAG, "Got filename: " + vvFileName);
        }

        return vvFileName;
    }

    public void extraKeysToggle(MenuItem m) {
        if (layoutKeys.getVisibility() == View.VISIBLE) {
            extraKeysHidden = true;
            setExtraKeysVisibility(View.GONE, false);
        } else {
            extraKeysHidden = false;
            setExtraKeysVisibility(View.VISIBLE, true);
        }
        setKeyStowDrawableAndVisibility(m);
        relayoutViews(rootView);
    }

    private void setKeyStowDrawableAndVisibility(MenuItem m) {
        if (m == null) {
            return;
        }
        Drawable replacer;
        if (connection.getExtraKeysToggleType() == Constants.EXTRA_KEYS_OFF) {
            m.setVisible(false);
        } else {
            m.setVisible(true);
        }
        if (layoutKeys.getVisibility() == View.GONE)
            replacer = getResources().getDrawable(R.drawable.showkeys);
        else
            replacer = getResources().getDrawable(R.drawable.hidekeys);

        m.setIcon(replacer);
    }

    public void sendShortVibration() {
        if (myVibrator != null) {
            myVibrator.vibrate(Constants.SHORT_VIBRATION);
        } else {
            Log.i(TAG, "Device cannot vibrate, not sending vibration");
        }
    }

    /**
     * Initializes the on-screen keys for meta keys and arrow keys.
     */
    private void initializeOnScreenKeys() {
        layoutKeys = (RelativeLayout) findViewById(R.id.layoutKeys);
        layoutArrowKeys = (LinearLayout) findViewById(R.id.layoutArrowKeys);

        // Define action of tab key and meta keys.
        keyTab = (ImageButton) findViewById(R.id.keyTab);
        keyTab.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent e) {
                RemoteKeyboard k = canvas.getKeyboard();
                int key = KeyEvent.KEYCODE_TAB;
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    BCFactory.getInstance().getBCHaptic().performLongPressHaptic(canvas);
                    keyTab.setImageResource(R.drawable.tabon);
                    k.repeatKeyEvent(key, new KeyEvent(e.getAction(), key));
                    return true;
                } else if (e.getAction() == MotionEvent.ACTION_UP) {
                    keyTab.setImageResource(R.drawable.taboff);
                    resetOnScreenKeys(0);
                    k.stopRepeatingKeyEvent();
                    return true;
                }
                return false;
            }
        });

        keyEsc = (ImageButton) findViewById(R.id.keyEsc);
        keyEsc.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent e) {
                RemoteKeyboard k = canvas.getKeyboard();
                int key = 111; /* KEYCODE_ESCAPE */
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    BCFactory.getInstance().getBCHaptic().performLongPressHaptic(canvas);
                    keyEsc.setImageResource(R.drawable.escon);
                    k.repeatKeyEvent(key, new KeyEvent(e.getAction(), key));
                    return true;
                } else if (e.getAction() == MotionEvent.ACTION_UP) {
                    keyEsc.setImageResource(R.drawable.escoff);
                    resetOnScreenKeys(0);
                    k.stopRepeatingKeyEvent();
                    return true;
                }
                return false;
            }
        });

        keyCtrl = (ImageButton) findViewById(R.id.keyCtrl);
        keyCtrl.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                boolean on = canvas.getKeyboard().onScreenCtrlToggle();
                keyCtrlToggled = false;
                if (on)
                    keyCtrl.setImageResource(R.drawable.ctrlon);
                else
                    keyCtrl.setImageResource(R.drawable.ctrloff);
            }
        });

        keyCtrl.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                sendShortVibration();
                boolean on = canvas.getKeyboard().onScreenCtrlToggle();
                keyCtrlToggled = true;
                if (on)
                    keyCtrl.setImageResource(R.drawable.ctrlon);
                else
                    keyCtrl.setImageResource(R.drawable.ctrloff);
                return true;
            }
        });

        keySuper = (ImageButton) findViewById(R.id.keySuper);
        keySuper.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                boolean on = canvas.getKeyboard().onScreenSuperToggle();
                keySuperToggled = false;
                if (on)
                    keySuper.setImageResource(R.drawable.superon);
                else
                    keySuper.setImageResource(R.drawable.superoff);
            }
        });

        keySuper.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                sendShortVibration();
                boolean on = canvas.getKeyboard().onScreenSuperToggle();
                keySuperToggled = true;
                if (on)
                    keySuper.setImageResource(R.drawable.superon);
                else
                    keySuper.setImageResource(R.drawable.superoff);
                return true;
            }
        });

        keyAlt = (ImageButton) findViewById(R.id.keyAlt);
        keyAlt.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                boolean on = canvas.getKeyboard().onScreenAltToggle();
                keyAltToggled = false;
                if (on)
                    keyAlt.setImageResource(R.drawable.alton);
                else
                    keyAlt.setImageResource(R.drawable.altoff);
            }
        });

        keyAlt.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                sendShortVibration();
                boolean on = canvas.getKeyboard().onScreenAltToggle();
                keyAltToggled = true;
                if (on)
                    keyAlt.setImageResource(R.drawable.alton);
                else
                    keyAlt.setImageResource(R.drawable.altoff);
                return true;
            }
        });

        keyShift = (ImageButton) findViewById(R.id.keyShift);
        keyShift.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                boolean on = canvas.getKeyboard().onScreenShiftToggle();
                keyShiftToggled = false;
                if (on)
                    keyShift.setImageResource(R.drawable.shifton);
                else
                    keyShift.setImageResource(R.drawable.shiftoff);
            }
        });

        keyShift.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                sendShortVibration();
                boolean on = canvas.getKeyboard().onScreenShiftToggle();
                keyShiftToggled = true;
                if (on)
                    keyShift.setImageResource(R.drawable.shifton);
                else
                    keyShift.setImageResource(R.drawable.shiftoff);
                return true;
            }
        });

        // TODO: Evaluate whether I should instead be using:
        // vncCanvas.sendMetaKey(MetaKeyBean.keyArrowLeft);

        // Define action of arrow keys.
        keyUp = (ImageButton) findViewById(R.id.keyUpArrow);
        keyUp.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent e) {
                RemoteKeyboard k = canvas.getKeyboard();
                int key = KeyEvent.KEYCODE_DPAD_UP;
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    sendShortVibration();
                    keyUp.setImageResource(R.drawable.upon);
                    k.repeatKeyEvent(key, new KeyEvent(e.getAction(), key));
                    return true;
                } else if (e.getAction() == MotionEvent.ACTION_UP) {
                    keyUp.setImageResource(R.drawable.upoff);
                    resetOnScreenKeys(0);
                    k.stopRepeatingKeyEvent();
                    return true;
                }
                return false;
            }
        });

        keyDown = (ImageButton) findViewById(R.id.keyDownArrow);
        keyDown.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent e) {
                RemoteKeyboard k = canvas.getKeyboard();
                int key = KeyEvent.KEYCODE_DPAD_DOWN;
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    sendShortVibration();
                    keyDown.setImageResource(R.drawable.downon);
                    k.repeatKeyEvent(key, new KeyEvent(e.getAction(), key));
                    return true;
                } else if (e.getAction() == MotionEvent.ACTION_UP) {
                    keyDown.setImageResource(R.drawable.downoff);
                    resetOnScreenKeys(0);
                    k.stopRepeatingKeyEvent();
                    return true;
                }
                return false;
            }
        });

        keyLeft = (ImageButton) findViewById(R.id.keyLeftArrow);
        keyLeft.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent e) {
                RemoteKeyboard k = canvas.getKeyboard();
                int key = KeyEvent.KEYCODE_DPAD_LEFT;
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    sendShortVibration();
                    keyLeft.setImageResource(R.drawable.lefton);
                    k.repeatKeyEvent(key, new KeyEvent(e.getAction(), key));
                    return true;
                } else if (e.getAction() == MotionEvent.ACTION_UP) {
                    keyLeft.setImageResource(R.drawable.leftoff);
                    resetOnScreenKeys(0);
                    k.stopRepeatingKeyEvent();
                    return true;
                }
                return false;
            }
        });

        keyRight = (ImageButton) findViewById(R.id.keyRightArrow);
        keyRight.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent e) {
                RemoteKeyboard k = canvas.getKeyboard();
                int key = KeyEvent.KEYCODE_DPAD_RIGHT;
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    sendShortVibration();
                    keyRight.setImageResource(R.drawable.righton);
                    k.repeatKeyEvent(key, new KeyEvent(e.getAction(), key));
                    return true;
                } else if (e.getAction() == MotionEvent.ACTION_UP) {
                    keyRight.setImageResource(R.drawable.rightoff);
                    resetOnScreenKeys(0);
                    k.stopRepeatingKeyEvent();
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Resets the state and image of the on-screen keys.
     */
    private void resetOnScreenKeys(int keyCode) {
        // Do not reset on-screen keys if keycode is SHIFT.
        switch (keyCode) {
            case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
                return;
        }
        if (!keyCtrlToggled) {
            keyCtrl.setImageResource(R.drawable.ctrloff);
            canvas.getKeyboard().onScreenCtrlOff();
        }
        if (!keyAltToggled) {
            keyAlt.setImageResource(R.drawable.altoff);
            canvas.getKeyboard().onScreenAltOff();
        }
        if (!keySuperToggled) {
            keySuper.setImageResource(R.drawable.superoff);
            canvas.getKeyboard().onScreenSuperOff();
        }
        if (!keyShiftToggled) {
            keyShift.setImageResource(R.drawable.shiftoff);
            canvas.getKeyboard().onScreenShiftOff();
        }
    }


    /**
     * Sets the visibility of the extra keys appropriately.
     */
    private void setExtraKeysVisibility(int visibility, boolean forceVisible) {
        Configuration config = getResources().getConfiguration();
        //Log.e(TAG, "Hardware kbd hidden: " + Integer.toString(config.hardKeyboardHidden));
        //Log.e(TAG, "Any keyboard hidden: " + Integer.toString(config.keyboardHidden));
        //Log.e(TAG, "Keyboard type: " + Integer.toString(config.keyboard));

        boolean makeVisible = forceVisible;
        if (config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO)
            makeVisible = true;

        if (!extraKeysHidden && makeVisible &&
                connection.getExtraKeysToggleType() == Constants.EXTRA_KEYS_ON) {
//            layoutKeys.setVisibility(View.VISIBLE);
//            layoutKeys.invalidate();
            return;
        }

        if (visibility == View.GONE) {
            layoutKeys.setVisibility(View.GONE);
            layoutKeys.invalidate();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause called.");
//        ReflectionUtils.set("fde.click_as_touch", "true");
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(canvas.getWindowToken(), 0);
        } catch (NullPointerException e) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume called.");
//        ReflectionUtils.set("fde.click_as_touch", "false");
        try {
            canvas.postInvalidateDelayed(600);
        } catch (NullPointerException e) {
        }

        detectEventEditText.setFocusable(true);
        detectEventEditText.setFocusableInTouchMode(true);
        detectEventEditText.requestFocus();
//        setInputMethod("com.android.inputmethod.latin/.LatinIME");
        canvas.sendPointer(100, 100);
    }

    /**
     * Set modes on start to match what is specified in the ConnectionBean;
     * color mode (already done) scaling, input mode
     */
    void setModes() {
        debugLog(App.debugLog, TAG, "setModes");
        inputHandler = getInputHandlerByName(connection.getInputMode());
        AbstractScaling.getByScaleType(connection.getScaleMode()).setScaleTypeForActivity(this);
        initializeOnScreenKeys();
        try {
            COLORMODEL cm = COLORMODEL.valueOf(connection.getColorModel());
            canvas.setColorModel(cm);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        canvas.setOnKeyListener(this);
        canvas.setFocusableInTouchMode(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            canvas.setFocusedByDefault(true);
        }
//        canvas.requestFocus();
        canvas.setDrawingCacheEnabled(false);

        SamsungDexUtils.INSTANCE.dexMetaKeyCapture(this);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == R.layout.entertext) {
            return new EnterTextDialog(this);
        } else if (id == R.id.itemHelpInputMode) {
            return createHelpDialog();
        }

        // Default to meta key dialog
        return new MetaKeyDialog(this);
    }

    /**
     * Creates the help dialog for this activity.
     */
    private Dialog createHelpDialog() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this)
                .setMessage(R.string.input_mode_help_text)
                .setPositiveButton(R.string.close,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                // We don't have to do anything.
                            }
                        });
        Dialog d = adb.setView(new ListView(this)).create();
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        d.show();
        d.getWindow().setAttributes(lp);
        return d;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
     */
    @SuppressWarnings("deprecation")
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
        if (dialog instanceof ConnectionSettable)
            ((ConnectionSettable) dialog).setConnection(connection);
    }

    /**
     * This runnable fixes things up after a rotation.
     */
    private Runnable rotationCorrector = new Runnable() {
        public void run() {
            try {
                correctAfterRotation();
            } catch (Exception e) {
            }
        }
    };

    /**
     * This function is called by the rotationCorrector runnable
     * to fix things up after a rotation.
     */
    private void correctAfterRotation() throws Exception {
        debugLog(App.debugLog, TAG, "correctAfterRotation");
        canvas.waitUntilInflated();
        // Its quite common to see NullPointerExceptions here when this function is called
        // at the point of disconnection. Hence, we catch and ignore the error.
        float oldScale = canvas.canvasZoomer.getZoomFactor();
        int x = canvas.absoluteXPosition;
        int y = canvas.absoluteYPosition;
        canvas.canvasZoomer.setScaleTypeForActivity(RemoteCanvasActivity.this);
        float newScale = canvas.canvasZoomer.getZoomFactor();
        canvas.canvasZoomer.changeZoom(this, oldScale / newScale, 0, 0);
        newScale = canvas.canvasZoomer.getZoomFactor();
        if (newScale <= oldScale &&
                canvas.canvasZoomer.getScaleType() != ImageView.ScaleType.FIT_CENTER) {
            canvas.absoluteXPosition = x;
            canvas.absoluteYPosition = y;
            canvas.resetScroll();
        }
        // Automatic resolution update request handling
        if (canvas.isVnc && connection.getRdpResType() == Constants.VNC_GEOM_SELECT_AUTOMATIC) {
            canvas.rfbconn.requestResolution(canvas.getWidth(), canvas.getHeight());
        } else if (canvas.isSpice && connection.getRdpResType() == Constants.RDP_GEOM_SELECT_AUTO) {
//            canvas.spicecomm.requestResolution(canvas.getWidth(), canvas.getHeight());
        } else if (canvas.isOpaque && connection.isRequestingNewDisplayResolution()) {
//            canvas.spicecomm.requestResolution(canvas.getWidth(), canvas.getHeight());
        }

    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        enableImmersive();
        updateStyle();
        try {
            setExtraKeysVisibility(View.GONE, false);

            // Correct a few times just in case. There is no visual effect.
            handler.postDelayed(rotationCorrector, 300);
        } catch (NullPointerException e) {
        }
    }

    private void updateStyle() {
        WindowManager manager = this.getWindowManager();
        Rect bounds = manager.getCurrentWindowMetrics().getBounds();
        int top = bounds.top;
        Log.d(TAG, "updateStyle():  top:" + top);
        if (top == 0) {
            getWindow().getDecorView().setOutlineProvider(null);
        } else {
            getWindow().getDecorView().setOutlineProvider(outlineProvider);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart called.");
        try {
            canvas.postInvalidateDelayed(800);
        } catch (NullPointerException e) {
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop called.");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(TAG, "onRestart called.");
        try {
            canvas.postInvalidateDelayed(1000);
        } catch (NullPointerException e) {
        }
    }

    @Override
    public void onPanelClosed(int featureId, Menu menu) {
        super.onPanelClosed(featureId, menu);
        showToolbar();
        enableImmersive();
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (menu != null) {
            Log.i(TAG, "Menu opened, disabling hiding action bar");
            handler.removeCallbacks(toolbarHider);
            updateScalingMenu();
            updateInputMenu();
            disableImmersive();
        }
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Make sure extra keys stow item is gone if extra keys are disabled and vice versa.
//        setKeyStowDrawableAndVisibility(menu.findItem(R.id.extraKeysToggle));
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        debugLog(App.debugLog, TAG, "OnCreateOptionsMenu called");
        try {
            getMenuInflater().inflate(R.menu.vnccanvasactivitymenu, menu);
            Menu inputMenu = menu.findItem(R.id.itemInputMode).getSubMenu();
            inputModeMenuItems = new MenuItem[inputModeIds.length];
            for (int i = 0; i < inputModeIds.length; i++) {
                inputModeMenuItems[i] = inputMenu.findItem(inputModeIds[i]);
            }
            updateInputMenu();

            Menu scalingMenu = menu.findItem(R.id.itemScaling).getSubMenu();
            scalingModeMenuItems = new MenuItem[scalingModeIds.length];
            for (int i = 0; i < scalingModeIds.length; i++) {
                scalingModeMenuItems[i] = scalingMenu.findItem(scalingModeIds[i]);
            }
            updateScalingMenu();

            // Set the text of the Extra Keys menu item appropriately.
            // TODO: Implement for Opaque
//            if (connection != null && connection.getExtraKeysToggleType() == Constants.EXTRA_KEYS_ON)
//                menu.findItem(R.id.itemExtraKeys).setTitle(R.string.extra_keys_disable);
//            else
//                menu.findItem(R.id.itemExtraKeys).setTitle(R.string.extra_keys_enable);

            OnTouchListener moveListener = new OnTouchViewMover(toolbar, handler, toolbarHider, hideToolbarDelay);
            ImageButton moveButton = new ImageButton(this);

            moveButton.setBackgroundResource(R.drawable.ic_all_out_gray_36dp);
//            MenuItem moveToolbar = menu.findItem(R.id.moveToolbar);
//            moveToolbar.setActionView(moveButton);
//            moveToolbar.getActionView().setOnTouchListener(moveListener);

//            ((Toolbar)findViewById(R.id.toolbar)).getChildAt(0).setPointerIcon(PointerIcon.getSystemIcon(this, 1000));

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        debugLog(App.debugLog, TAG, "OnCreateOptionsMenu complete");
        return true;
    }

    /**
     * Change the scaling mode sub-menu to reflect available scaling modes.
     */
    void updateScalingMenu() {
        try {
            for (MenuItem item : scalingModeMenuItems) {
                // If the entire framebuffer is NOT contained in the bitmap, fit-to-screen is meaningless.
                if (item.getItemId() == R.id.itemFitToScreen) {
                    if (canvas != null && canvas.myDrawable != null &&
                            (canvas.myDrawable.bitmapheight != canvas.myDrawable.framebufferheight ||
                                    canvas.myDrawable.bitmapwidth != canvas.myDrawable.framebufferwidth)) {
                        item.setEnabled(false);
                    } else {
                        item.setEnabled(true);
                    }
                } else {
                    item.setEnabled(true);
                }

                AbstractScaling scaling = AbstractScaling.getById(item.getItemId());
                if (scaling.scaleType == connection.getScaleMode()) {
                    item.setChecked(true);
                }
            }
        } catch (NullPointerException e) {
        }
    }

    /**
     * Change the input mode sub-menu to reflect change in scaling
     */
    void updateInputMenu() {
        try {
            for (MenuItem item : inputModeMenuItems) {
                item.setEnabled(canvas.canvasZoomer.isValidInputMode(item.getItemId()));
                if (getInputHandlerById(item.getItemId()) == inputHandler)
                    item.setChecked(true);
            }
        } catch (NullPointerException e) {
        }
    }

    /**
     * If id represents an input handler, return that; otherwise return null
     *
     * @param id
     * @return
     */
    InputHandler getInputHandlerById(int id) {
        myVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        if (inputModeHandlers == null) {
            inputModeHandlers = new InputHandler[inputModeIds.length];
        }
        for (int i = 0; i < inputModeIds.length; ++i) {
            if (inputModeIds[i] == id) {
                if (inputModeHandlers[i] == null) {
                    if (id == R.id.itemInputTouchPanZoomMouse) {
                        inputModeHandlers[i] = new InputHandlerDirectSwipePan(this, canvas, canvas.getPointer(), App.debugLog);
                    } else if (id == R.id.itemInputDragPanZoomMouse) {
                        inputModeHandlers[i] = new InputHandlerDirectDragPan(this, canvas, canvas.getPointer(), App.debugLog);
                    } else if (id == R.id.itemInputTouchpad) {
                        inputModeHandlers[i] = new InputHandlerTouchpad(this, canvas, canvas.getPointer(), App.debugLog);
                    } else if (id == R.id.itemInputSingleHanded) {
                        inputModeHandlers[i] = new InputHandlerSingleHanded(this, canvas, canvas.getPointer(), App.debugLog);
                    } else {
                        throw new IllegalStateException("Unexpected value: " + id);
                    }
                }
                return inputModeHandlers[i];
            }
        }
        return null;
    }

    void clearInputHandlers() {
        if (inputModeHandlers == null)
            return;

        for (int i = 0; i < inputModeIds.length; ++i) {
            inputModeHandlers[i] = null;
        }
        inputModeHandlers = null;
    }

    InputHandler getInputHandlerByName(String name) {
        InputHandler result = null;
        for (int id : inputModeIds) {
            InputHandler handler = getInputHandlerById(id);
            if (handler.getId().equals(name)) {
                result = handler;
                break;
            }
        }
        if (result == null) {
            result = getInputHandlerById(R.id.itemInputTouchPanZoomMouse);
        }
        return result;
    }

    int getModeIdFromHandler(InputHandler handler) {
        for (int id : inputModeIds) {
            if (handler == getInputHandlerById(id))
                return id;
        }
        return R.id.itemInputTouchPanZoomMouse;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        RemoteKeyboard k = canvas.getKeyboard();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        if (k != null) {
            k.setAfterMenu(true);
        }
        int itemId = item.getItemId();
        if (itemId == R.id.itemInfo) {
            canvas.showConnectionInfo();
            return true;
        } else if (itemId == R.id.itemSpecialKeys) {
            showDialog(R.layout.metakey);
            return true;
        } else if (itemId == R.id.itemColorMode) {
            selectColorModel();
            return true;
            // Following sets one of the scaling options
        } else if (itemId == R.id.itemZoomable || itemId == R.id.itemOneToOne || itemId == R.id.itemFitToScreen) {
            AbstractScaling.getById(item.getItemId()).setScaleTypeForActivity(this);
            item.setChecked(true);
            showPanningState(false);
            return true;
        } else if (itemId == R.id.itemCenterMouse) {
            canvas.getPointer().movePointer(canvas.absoluteXPosition + canvas.getVisibleDesktopWidth() / 2,
                    canvas.absoluteYPosition + canvas.getVisibleDesktopHeight() / 2);
            return true;
        } else if (itemId == R.id.itemDisconnect) {
            canvas.closeConnection();
            Utils.justFinish(this);
            return true;
        }
//        else if (itemId == R.id.itemEnterText) {
//            showDialog(R.layout.entertext);
//            return true;
//        }
        else if (itemId == R.id.itemCtrlAltDel) {
            canvas.getKeyboard().sendMetaKey(MetaKeyBean.keyCtrlAltDel);
            return true;
        } else if (itemId == R.id.itemSendKeyAgain) {
            sendSpecialKeyAgain();
            return true;
            // Disabling Manual/Wiki Menu item as the original does not correspond to this project anymore.
            //case R.id.itemOpenDoc:
            //    Utils.showDocumentation(this);
            //    return true;
        } else if(itemId == R.id.itemInputModeInputMethod){
            mInputModeFlag = INPUT_MODE_FULL_FUNCTION;
            detectEventEditText.setInputMode(mInputModeFlag);
            item.setChecked(true);
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
            detectEventEditText.setVisibility(View.VISIBLE);
            detectEventEditText.requestFocus();
        } else if(itemId == R.id.itemInputModeKeyboard){
            mInputModeFlag = INPUT_MODE_ONLY_KEYBOARD;
            detectEventEditText.setInputMode(mInputModeFlag);
            item.setChecked(true);
            imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
            detectEventEditText.setVisibility(View.GONE);
        } else if (itemId == R.id.itemHelpInputMode) {
            showDialog(R.id.itemHelpInputMode);
            return true;
        } else {
            boolean inputModeSet = setInputMode(item.getItemId());
            item.setChecked(inputModeSet);
            if (inputModeSet) {
                return inputModeSet;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean setInputMode(int id) {
        InputHandler input = getInputHandlerById(id);
        if (input != null) {
            inputHandler = input;
            connection.setInputMode(input.getId());
            if (input.getId().equals(InputHandlerTouchpad.ID)) {
                connection.setFollowMouse(true);
                connection.setFollowPan(true);
            } else {
                connection.setFollowMouse(false);
                connection.setFollowPan(false);
                canvas.getPointer().setRelativeEvents(false);
            }

            showPanningState(true);
            connection.save(this);
            return true;
        }
        return false;
    }

    private MetaKeyBean lastSentKey;

    private void sendSpecialKeyAgain() {
        if (lastSentKey == null
                || lastSentKey.get_Id() != connection.getLastMetaKeyId()) {
            ArrayList<MetaKeyBean> keys = new ArrayList<MetaKeyBean>();
            Database database = new Database(this);
            Cursor c = database.getReadableDatabase().rawQuery(
                    MessageFormat.format("SELECT * FROM {0} WHERE {1} = {2}",
                            MetaKeyBean.GEN_TABLE_NAME,
                            MetaKeyBean.GEN_FIELD__ID, connection
                                    .getLastMetaKeyId()),
                    MetaKeyDialog.EMPTY_ARGS);
            MetaKeyBean.Gen_populateFromCursor(c, keys, MetaKeyBean.NEW);
            c.close();
            database.close();
            if (keys.size() > 0) {
                lastSentKey = keys.get(0);
            } else {
                lastSentKey = null;
            }
        }
        if (lastSentKey != null)
            canvas.getKeyboard().sendMetaKey(lastSentKey);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopApp();
        Log.i(TAG, "onDestroy called.");
        if (canvas != null)
            canvas.closeConnection();

        com.ft.fdevnc.Constants.app = null;
        System.gc();
    }

    private void stopApp() {
        int app = connection.getApp();
        QuietOkHttp.post(BASEURL + URL_KILLAPP)
                .setCallbackToMainUIThread(true)
                .addParams("App", App.getRunName(getClass().getName()))
                .addParams("SysOnly", "false")
                .execute(new JsonCallBack<VncResult.GetPortResult>() {
                    @Override
                    public void onFailure(Call call, Exception e) {
                        Log.e(TAG, "onFailure() called with: call = [" + call + "], e = [" + e + "]");
                    }

                    @Override
                    public void onSuccess(Call call, VncResult.GetPortResult response) {
                        com.ft.fdevnc.Constants.app = null;
                    }
                });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        Log.d(TAG, "dispatchTouchEvent() called with: ev = [" + ev + "]");
        return super.dispatchTouchEvent(ev);
    }


    private Set<Long> downTimes = new HashSet<>();

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.d(TAG, "dispatchKeyEvent() called with: event = [" + event + "] DetectEventEditText.commitText = [ " + detectEventEditText.commitText  + " ]");
        return canvas.getKeyboard().keyEvent(event.getKeyCode(), event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown() called with: keyCode = [" + keyCode + "], event = [" + event + "]");
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent evt) {
        Log.d(TAG, "onKey() called with: v = [" + v + "], keyCode = [" + keyCode + "], evt = [" + evt + "]");
        boolean consumed = false;
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (evt.getAction() == KeyEvent.ACTION_DOWN)
                return super.onKeyDown(keyCode, evt);
            else
                return super.onKeyUp(keyCode, evt);
        }

        try {
            if (evt.getAction() == KeyEvent.ACTION_DOWN || evt.getAction() == KeyEvent.ACTION_MULTIPLE) {
                consumed = inputHandler.onKeyDown(keyCode, evt);
            } else if (evt.getAction() == KeyEvent.ACTION_UP) {
                consumed = inputHandler.onKeyUp(keyCode, evt);
            }
            resetOnScreenKeys(keyCode);
        } catch (NullPointerException e) {
        }

        return consumed;
    }

    public void showPanningState(boolean showLonger) {
        if (showLonger) {
            final Toast t = Toast.makeText(this, inputHandler.getDescription(), Toast.LENGTH_LONG);
            TimerTask tt = new TimerTask() {
                @Override
                public void run() {
                    t.show();
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                    }
                    t.show();
                }
            };
            new Timer().schedule(tt, 2000);
            t.show();
        } else {
            Toast t = Toast.makeText(this, inputHandler.getDescription(), Toast.LENGTH_SHORT);
            t.show();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onTrackballEvent(android.view.MotionEvent)
     */
    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        try {
            // If we are using the Dpad as arrow keys, don't send the event to the inputHandler.
            if (connection.getUseDpadAsArrows())
                return false;
            return inputHandler.onTouchEvent(event);
        } catch (NullPointerException e) {
        }
        return super.onTrackballEvent(event);
    }

    // Send touch events or mouse events like button clicks to be handled.
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            return inputHandler.onTouchEvent(event);
        } catch (NullPointerException e) {
        }
        return super.onTouchEvent(event);
    }

    // Send e.g. mouse events like hover and scroll to be handled.
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        // Ignore TOOL_TYPE_FINGER events that come from the touchscreen with HOVER type action
        // which cause pointer jumping trouble in simulated touchpad for some devices.
        boolean toolTypeFinger = false;
        if (Constants.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            toolTypeFinger = event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER;
        }
        int a = event.getAction();
        if (!((a == MotionEvent.ACTION_HOVER_ENTER ||
                a == MotionEvent.ACTION_HOVER_EXIT ||
                a == MotionEvent.ACTION_HOVER_MOVE) &&
                event.getSource() == InputDevice.SOURCE_TOUCHSCREEN &&
                toolTypeFinger
        )) {
            try {
                return inputHandler.onTouchEvent(event);
            } catch (NullPointerException e) {
            }
        }
        return super.onGenericMotionEvent(event);
    }

    private void selectColorModel() {

        String[] choices = new String[COLORMODEL.values().length];
        int currentSelection = -1;
        for (int i = 0; i < choices.length; i++) {
            COLORMODEL cm = COLORMODEL.values()[i];
            choices[i] = cm.toString();
            if (canvas.isColorModel(cm))
                currentSelection = i;
        }

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ListView list = new ListView(this);
        list.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_checked, choices));
        list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        list.setItemChecked(currentSelection, true);
        list.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                dialog.dismiss();
                COLORMODEL cm = COLORMODEL.values()[arg2];
                canvas.setColorModel(cm);
                connection.setColorModel(cm.nameString());
                connection.save(RemoteCanvasActivity.this);
                Toast.makeText(RemoteCanvasActivity.this, getString(R.string.info_update_color_model_to) + cm.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        dialog.setContentView(list);
        dialog.show();
    }

    final long hideToolbarDelay = 2500;
    ToolbarHiderRunnable toolbarHider = new ToolbarHiderRunnable();

    public void showToolbar() {
        getSupportActionBar().show();
        handler.removeCallbacks(toolbarHider);
        handler.postAtTime(toolbarHider, SystemClock.uptimeMillis() + hideToolbarDelay);
    }

    private void setInputMethod(String inputMethod){
//        Settings.Secure.putString(getContentResolver(),
//                Settings.Secure.DEFAULT_INPUT_METHOD,inputMethod);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.setInputMethod(null, inputMethod);
        imm.hideSoftInputFromInputMethod(null, HIDE_NOT_ALWAYS );

    }


    @Override
    public void onTextSelected(String selectedString) {
        Log.i(TAG, "onTextSelected called with selectedString: " + selectedString);
//        canvas.pd.show();
        connection.setVmname(canvas.vmNameToId.get(selectedString));
        connection.save(this);
//        synchronized (canvas.spicecomm) {
//            canvas.spicecomm.notify();
//        }
    }

    private class ToolbarHiderRunnable implements Runnable {
        public void run() {
            ActionBar toolbar = getSupportActionBar();
            if (toolbar != null)
                toolbar.hide();
        }
    }

    public void toggleKeyboard(MenuItem menuItem) {
        if (softKeyboardUp) {
            hideKeyboard();
        } else {
            showKeyboard();
        }
    }

    public void showKeyboard() {
        Log.i(TAG, "Showing keyboard and hiding action bar");
        canvas.requestFocus();
        Utils.showKeyboard(this, canvas);
        softKeyboardUp = true;
        Objects.requireNonNull(getSupportActionBar()).hide();
    }

    public void hideKeyboard() {
        Log.i(TAG, "Hiding keyboard and hiding action bar");
        canvas.requestFocus();
        Utils.hideKeyboard(this, getCurrentFocus());
        softKeyboardUp = false;
        Objects.requireNonNull(getSupportActionBar()).hide();
    }

    public void hideKeyboardAndExtraKeys() {
        hideKeyboard();
        if (layoutKeys.getVisibility() == View.VISIBLE) {
            extraKeysHidden = true;
            setExtraKeysVisibility(View.GONE, false);
        }
    }

    public void stopPanner() {
        panner.stop();
    }

    public Connection getConnection() {
        return connection;
    }

    // Returns whether we are using D-pad/Trackball to send arrow key events.
    public boolean getUseDpadAsArrows() {
        return connection.getUseDpadAsArrows();
    }

    // Returns whether the D-pad should be rotated to accommodate BT keyboards paired with phones.
    public boolean getRotateDpad() {
        return connection.getRotateDpad();
    }

    public RemoteCanvas getCanvas() {
        return canvas;
    }

    public Panner getPanner() {
        return panner;
    }

    public void setPanner(Panner panner) {
        this.panner = panner;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
        super.onSaveInstanceState(outState);
    }

    private boolean isMasterPasswordEnabled() {
        SharedPreferences sp = getSharedPreferences(Constants.generalSettingsTag, Context.MODE_PRIVATE);
        return sp.getBoolean(Constants.masterPasswordEnabledTag, false);
    }

    @Override
    public void onBackPressed() {
        if (inputHandler != null) {
            inputHandler.onKeyDown(KeyEvent.KEYCODE_BACK, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
        }
    }

    public static class RemoteCanvasActivity1 extends RemoteCanvasActivity{

    }
    public static class RemoteCanvasActivity2 extends RemoteCanvasActivity{

    }
    public static class RemoteCanvasActivity3 extends RemoteCanvasActivity{

    }
    public static class RemoteCanvasActivity4 extends RemoteCanvasActivity{

    }
    public static class RemoteCanvasActivity5 extends RemoteCanvasActivity{

    }
    public static class RemoteCanvasActivity6 extends RemoteCanvasActivity{

    }
    public static class RemoteCanvasActivity7 extends RemoteCanvasActivity{

    }
    public static class RemoteCanvasActivity8 extends RemoteCanvasActivity{

    }
    public static class RemoteCanvasActivity9 extends RemoteCanvasActivity{

    }



}
