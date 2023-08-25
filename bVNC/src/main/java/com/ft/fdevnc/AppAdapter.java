package com.ft.fdevnc;


import static com.ft.fdevnc.Constants.BASEURL;
import static com.ft.fdevnc.Constants.URL_STOPAPP;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.iiordanov.bVNC.ConnectionBean;
import com.iiordanov.bVNC.Utils;
import com.iiordanov.bVNC.bVNC;
import com.undatech.opaque.ConnectionSettings;
import com.undatech.opaque.util.ConnectionLoader;
import com.undatech.opaque.util.GeneralUtils;
import com.undatech.remoteClientUi.R;
import com.xwdz.http.QuietOkHttp;
import com.xwdz.http.callback.JsonCallBack;

import java.util.List;

import okhttp3.Call;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    Handler handler =  new Handler(Looper.getMainLooper());

    private List<AppListResult.DataBeanX.DataBean> list;
    private Context context;
    private String TAG = "AppAdapter";
    private boolean isConnecting;

    bVNC.ItemClickListener itemClickListener;

    public AppAdapter(@NonNull Context context, List<AppListResult.DataBeanX.DataBean> list, bVNC.ItemClickListener listener) {
        this.context = context;
        this.list = list;
        this.itemClickListener = listener;
    }

    public static Bitmap getImage(String imageStr) {
//        imageStr = "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAFo9M/3AAAABGdBTUEAALGPC/xhBQAAAAZiS0dEAP8A/wD/oL2nkwAAAAlwSFlzAAALEwAACxMBAJqcGAAAAAd0SU1FB+ULAwYRBlEPRPkAAAMISURBVDjLlZNNbFRlFIaf797v3pm5c6eUKTOETkEikaaaGCOuMAFMkDQmRv4SXNhA4mgkgRCMWzcuMCbYneFHiUo3lgYxBOPCmIqNVEUqltLyJ7XTJoUWOtMZ5n/ud1xMgA0b3/XJc97zJAeAFVuPJuC+cLOzU1T71qMJQKnxREKC+XkAuHzrvsyJyO1Xt4iCFL1ffy6uo4kvX4laue14pIHxAWbPvDevhibuSerEYeyuZ9k+rNBtG7qYHvyb56wcDF9Cj529zN3RMTJtSQAUwMhkVjDgJVtxfShP55FGnd2HfsACKBRrLJ3PYJ5OUonFiNoBJhblk9zghEKE8WRSxgd+prC4QL1WoXXNM4TKDT76cri5Yl26T3hCXn6+420F0LHj+HITGCOOC406SgQd84NM31sLrEv3yYVr9+TKVE5ubdsh12cWZWQqL+OJhKze803cAnC0Qi/zqX73LZH2FsIvraE08COqUrQtgCDkIatXYESwDn1M+a8pYh8ewIS85pktL66iPJrBVAS7u5vkvjdxdu4iKD5AA9z+6Sr50asU8zmceBvhDw7juR5y/vvmwOy/k5QLi1hKocs1KjXBWRoFY5TasL9/bbFcu/4kD5e+6FEKYNOBU52FYvUa/yObXliV/nTfxhOPTPce7CYastC2jWsrvOwcpc3rWTY3R372AYEbQk/eoPraKwD0vNHLQlUlrYdEpcAYCCIe3pWLlDavB8D69TfY20PQlcL5fYjq8AS1P24AIEGtKQGg4cVYMnSOxv40Dd8jfPoc3uifFI58Ru3kGQp3s9htPq0H3yV/8itI9xFYIR418KdvEry/l3/O/sKFIwMM3lmk308xsnELdKWIro1T3d7Nndd3MTPWfBcpFR43yLR0EPSfp1osYWkX13eI+0so0c7FY6dBBGXZhKIxwpU6AEbUY0C4USZrhdCREJYNCgulBEShHQdlWRgBYwzZfBUAV6tAPQQ8tfNYBCFm6mUa5ayu5WZcUy9pRFC2NjqaqLutHVXL8cTWOpg89c4CwH/WKUyEookaGwAAAABJRU5ErkJggg==";
        byte[] decode = Base64.decode(imageStr, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(decode, 0, decode.length);
        return bitmap;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_item, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        final AppListResult.DataBeanX.DataBean app = list.get(position);
        app.id = position;
        TextView textView = holder.textView;
        textView.setText(app.Name);
        textView.setTypeface(null, Typeface.NORMAL);
        textView.setTextColor(ContextCompat.getColor(context, R.color.black));
        ImageView imageView =holder.imageView;
        imageView.setImageBitmap(getImage(app.Icon));
        holder.itemView.setTag(position);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                tryStartVncApp(app);
            }
        });
        holder.entryView.setListener(new RightClickView.RightClickListener() {
            @Override
            public void onRightClick(boolean b) {
                itemClickListener.onItemClick(holder.itemView, position, app, b);
            }
        });
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
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
//                                Toast.makeText(context, "无法启动此程序", Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onSuccess(Call call, VncResult.GetPortResult response) {
                        Log.d("huyang", "onSuccess() called with: call = [" + call + "], response = [" + response + "]");
                        Activity activity = (Activity) context;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
//                                Toast.makeText(context, String.format("%s 启动中", app.Name), Toast.LENGTH_SHORT).show();
                            }
                        });
                        tryLunchApp(app, response.Data.Port);
                    }
                });
    }

    private void tryLunchApp(AppListResult.DataBeanX.DataBean app, int port) {
        Utils.hideKeyboard(context, ((AllAppActivity)context).getCurrentFocus());
        Log.i(TAG, "Launch Connection");

        ActivityManager.MemoryInfo info = Utils.getMemoryInfo(context);
        if (info.lowMemory)
            System.gc();

        isConnecting = true;
        Intent intent = new Intent(context, GeneralUtils.getClassByName("com.iiordanov.bVNC.RemoteCanvasActivity"));
        ConnectionLoader connectionLoader = getConnectionLoader(context);
        if (Utils.isOpaque(context)) {
            ConnectionSettings cs = (ConnectionSettings) connectionLoader.getConnectionsById().get(app.id);
            cs.loadFromSharedPreferences(context.getApplicationContext());
            intent.putExtra("com.undatech.opaque.ConnectionSettings", cs);
        }
        else{
            ConnectionBean conn = (ConnectionBean) connectionLoader.getConnectionsById().get(app.id);
            intent.putExtra(Utils.getConnectionString(context.getApplicationContext()), conn.Gen_getValues());
        }
        context.startActivity(intent);

    }

    private ConnectionLoader getConnectionLoader(Context context) {
        boolean connectionsInSharedPrefs = Utils.isOpaque(context);
        ConnectionLoader connectionLoader = new ConnectionLoader(context.getApplicationContext(), (Activity) context, connectionsInSharedPrefs);
        return connectionLoader;
    }

//    private void tryLunchApp(AppListResult.DataBeanX.DataBean app, int port) {
//        Intent intent = new Intent(context, VncCanvasActivity.class);
//        app.port = port;
//        //todo mock
//        ConnectionBean bean = new ConnectionBean(app.id, null,
//                BASIP,
////                "192.168.137.128",
//                port,
////                5902,
//                null, "C256", 0, "",
//                "MOUSE", "MATRIX", true, true,
//                false, false,  1, 0, true,
//                "" , "null", false, "null", app.Name);
//        intent.putExtra(VncConstants.CONNECTION, bean.Gen_getValues());
//        context.startActivity(intent);
//    }


    @Override
    public int getItemCount() {
        return list.size();
    }

    public void notifyDataSetChanged(List<AppListResult.DataBeanX.DataBean> mDataList) {
        this.list = mDataList;
        super.notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView textView;
        ImageView imageView;
        RightClickView entryView;
        public ViewHolder(View view){
            super(view);
            imageView = view.findViewById(R.id.icon);
            textView = view.findViewById(R.id.name);
            entryView = view.findViewById(R.id.entry);
        }
    }
}
