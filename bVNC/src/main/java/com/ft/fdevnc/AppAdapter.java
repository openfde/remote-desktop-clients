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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
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

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.iiordanov.bVNC.ConnectionBean;
import com.iiordanov.bVNC.Utils;
import com.iiordanov.bVNC.bVNC;
import com.undatech.opaque.ConnectionSettings;
import com.undatech.opaque.util.ConnectionLoader;
import com.undatech.opaque.util.GeneralUtils;
import com.undatech.remoteClientUi.R;
import com.xwdz.http.QuietOkHttp;
import com.xwdz.http.callback.JsonCallBack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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

    public Drawable getImage(String imageStr, String iconType, String name) {
        Log.d(TAG, "getImage() called with: imageStr = [" + imageStr.length() + "], iconType = [" + iconType + "], name = [" + name + "]");
//     bitmap
//     imageStr = "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAFo9M/3AAAABGdBTUEAALGPC/xhBQAAAAZiS0dEAP8A/wD/oL2nkwAAAAlwSFlzAAALEwAACxMBAJqcGAAAAAd0SU1FB+ULAwYRBlEPRPkAAAMISURBVDjLlZNNbFRlFIaf797v3pm5c6eUKTOETkEikaaaGCOuMAFMkDQmRv4SXNhA4mgkgRCMWzcuMCbYneFHiUo3lgYxBOPCmIqNVEUqltLyJ7XTJoUWOtMZ5n/ud1xMgA0b3/XJc97zJAeAFVuPJuC+cLOzU1T71qMJQKnxREKC+XkAuHzrvsyJyO1Xt4iCFL1ffy6uo4kvX4laue14pIHxAWbPvDevhibuSerEYeyuZ9k+rNBtG7qYHvyb56wcDF9Cj529zN3RMTJtSQAUwMhkVjDgJVtxfShP55FGnd2HfsACKBRrLJ3PYJ5OUonFiNoBJhblk9zghEKE8WRSxgd+prC4QL1WoXXNM4TKDT76cri5Yl26T3hCXn6+420F0LHj+HITGCOOC406SgQd84NM31sLrEv3yYVr9+TKVE5ubdsh12cWZWQqL+OJhKze803cAnC0Qi/zqX73LZH2FsIvraE08COqUrQtgCDkIatXYESwDn1M+a8pYh8ewIS85pktL66iPJrBVAS7u5vkvjdxdu4iKD5AA9z+6Sr50asU8zmceBvhDw7juR5y/vvmwOy/k5QLi1hKocs1KjXBWRoFY5TasL9/bbFcu/4kD5e+6FEKYNOBU52FYvUa/yObXliV/nTfxhOPTPce7CYastC2jWsrvOwcpc3rWTY3R372AYEbQk/eoPraKwD0vNHLQlUlrYdEpcAYCCIe3pWLlDavB8D69TfY20PQlcL5fYjq8AS1P24AIEGtKQGg4cVYMnSOxv40Dd8jfPoc3uifFI58Ru3kGQp3s9htPq0H3yV/8itI9xFYIR418KdvEry/l3/O/sKFIwMM3lmk308xsnELdKWIro1T3d7Nndd3MTPWfBcpFR43yLR0EPSfp1osYWkX13eI+0so0c7FY6dBBGXZhKIxwpU6AEbUY0C4USZrhdCREJYNCgulBEShHQdlWRgBYwzZfBUAV6tAPQQ8tfNYBCFm6mUa5ayu5WZcUy9pRFC2NjqaqLutHVXL8cTWOpg89c4CwH/WKUyEookaGwAAAABJRU5ErkJggg==";
//     svg
//        iconType = ".svg" ; imageStr = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4NCjwhLS0gR2VuZXJhdG9yOiBBZG9iZSBJbGx1c3RyYXRvciAyNC4wLjAsIFNWRyBFeHBvcnQgUGx1Zy1JbiAuIFNWRyBWZXJzaW9uOiA2LjAwIEJ1aWxkIDApICAtLT4NCjxzdmcgdmVyc2lvbj0iMS4xIiBpZD0i5Zu+5bGCXzEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgeG1sbnM6eGxpbms9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkveGxpbmsiIHg9IjBweCIgeT0iMHB4Ig0KCSB2aWV3Qm94PSIwIDAgMjU2IDI1NiIgc3R5bGU9ImVuYWJsZS1iYWNrZ3JvdW5kOm5ldyAwIDAgMjU2IDI1NjsiIHhtbDpzcGFjZT0icHJlc2VydmUiPg0KPHN0eWxlIHR5cGU9InRleHQvY3NzIj4NCgkuc3Qwe2ZpbGw6dXJsKCNwYXRoLTMtNV8xXyk7fQ0KCS5zdDF7b3BhY2l0eTowLjE7ZmlsbC1ydWxlOmV2ZW5vZGQ7Y2xpcC1ydWxlOmV2ZW5vZGQ7fQ0KCS5zdDJ7ZmlsbC1ydWxlOmV2ZW5vZGQ7Y2xpcC1ydWxlOmV2ZW5vZGQ7ZmlsbDp1cmwoI3BhdGgtNy0yXzFfKTt9DQoJLnN0M3tvcGFjaXR5OjAuMjtmaWxsLXJ1bGU6ZXZlbm9kZDtjbGlwLXJ1bGU6ZXZlbm9kZDtmaWxsOiMwMDAxN0Q7ZW5hYmxlLWJhY2tncm91bmQ6bmV3ICAgIDt9DQoJLnN0NHtmaWxsLXJ1bGU6ZXZlbm9kZDtjbGlwLXJ1bGU6ZXZlbm9kZDtmaWxsOnVybCgj55+p5b2iXzFfKTt9DQoJLnN0NXtmaWxsOnVybCgjcGF0aC0xMi0yXzFfKTt9DQo8L3N0eWxlPg0KPGxpbmVhckdyYWRpZW50IGlkPSJwYXRoLTMtNV8xXyIgZ3JhZGllbnRVbml0cz0idXNlclNwYWNlT25Vc2UiIHgxPSItMTAwMy44Mjg3IiB5MT0iNzAuNjcwNSIgeDI9Ii0xMDAzLjgyODciIHkyPSI3MS42ODA2IiBncmFkaWVudFRyYW5zZm9ybT0ibWF0cml4KDIxNiAwIDAgMjE2IDIxNjk1NSAtMTUyNDYpIj4NCgk8c3RvcCAgb2Zmc2V0PSIwIiBzdHlsZT0ic3RvcC1jb2xvcjojOEM4QzhDIi8+DQoJPHN0b3AgIG9mZnNldD0iMSIgc3R5bGU9InN0b3AtY29sb3I6IzRENEQ0RCIvPg0KPC9saW5lYXJHcmFkaWVudD4NCjxwYXRoIGlkPSJwYXRoLTMtNSIgY2xhc3M9InN0MCIgZD0iTTQyLDE4aDE3MmMxMy4zLDAsMjQsMTAuNywyNCwyNHYxNzJjMCwxMy4zLTEwLjcsMjQtMjQsMjRINDJjLTEzLjMsMC0yNC0xMC43LTI0LTI0VjQyDQoJQzE4LDI4LjcsMjguNywxOCw0MiwxOHoiLz4NCjxnPg0KCTxwYXRoIGlkPSJwYXRoLTciIGNsYXNzPSJzdDEiIGQ9Ik0xMzYuOCwxMTguOWw1My44LDQwLjRjNS45LDQuMyw3LjIsMTIuNSwyLjksMTguNGMtMC4yLDAuMi0wLjMsMC40LTAuNSwwLjYNCgkJYy0wLjcsMC45LTEuNSwxLjYtMi40LDIuM2wtNTMuOCw0MC40Yy01LjIsMy45LTEyLjMsMy45LTE3LjUsMGwtNTMuOC00MC40Yy01LjktNC4zLTcuMi0xMi41LTIuOS0xOC40YzAuMi0wLjIsMC4zLTAuNCwwLjUtMC42DQoJCWMwLjctMC45LDEuNS0xLjYsMi40LTIuM2w1My44LTQwLjRDMTI0LjQsMTE1LDEzMS41LDExNSwxMzYuOCwxMTguOXoiLz4NCgkNCgkJPGxpbmVhckdyYWRpZW50IGlkPSJwYXRoLTctMl8xXyIgZ3JhZGllbnRVbml0cz0idXNlclNwYWNlT25Vc2UiIHgxPSItMTAwMS4wODA5IiB5MT0iNzMuMzUxOCIgeDI9Ii0xMDAxLjA4MDkiIHkyPSI3NC4zNTE4IiBncmFkaWVudFRyYW5zZm9ybT0ibWF0cml4KDEzNiAwIDAgMTA4IDEzNjI3NSAtNzgxNCkiPg0KCQk8c3RvcCAgb2Zmc2V0PSIwIiBzdHlsZT0ic3RvcC1jb2xvcjojNjhGRkNBIi8+DQoJCTxzdG9wICBvZmZzZXQ9IjEiIHN0eWxlPSJzdG9wLWNvbG9yOiMyQjcwRkYiLz4NCgk8L2xpbmVhckdyYWRpZW50Pg0KCTxwYXRoIGlkPSJwYXRoLTctMiIgY2xhc3M9InN0MiIgZD0iTTEzNi44LDExMC45bDUzLjgsNDAuNGM1LjksNC4zLDcuMiwxMi41LDIuOSwxOC40Yy0wLjIsMC4yLTAuMywwLjQtMC41LDAuNg0KCQljLTAuNywwLjktMS41LDEuNi0yLjQsMi4zbC01My44LDQwLjRjLTUuMiwzLjktMTIuMywzLjktMTcuNSwwbC01My44LTQwLjRjLTUuOS00LjMtNy4yLTEyLjUtMi45LTE4LjRjMC4yLTAuMiwwLjMtMC40LDAuNS0wLjYNCgkJYzAuNy0wLjksMS41LTEuNiwyLjQtMi4zbDUzLjgtNDAuNEMxMjQuNCwxMDcsMTMxLjUsMTA3LDEzNi44LDExMC45eiIvPg0KPC9nPg0KPGc+DQoJPHBhdGggaWQ9IuefqeW9ouWkh+S7vS0yNiIgY2xhc3M9InN0MyIgZD0iTTEzNS4yLDkxLjVsNDQuMywzNC40YzUsMy45LDYsMTEuMSwyLjEsMTYuMWMwLDAtMC4xLDAuMS0wLjEsMC4xYy0wLjYsMC43LTEuMywxLjQtMiwyDQoJCWwtNDQuMywzNC40Yy00LjIsMy4zLTEwLjIsMy4zLTE0LjQsMGwtNDQuMy0zNC41Yy01LTMuOS02LTExLjEtMi4xLTE2LjFjMCwwLDAuMS0wLjEsMC4xLTAuMWMwLjYtMC43LDEuMy0xLjQsMi0ybDQ0LjMtMzQuNA0KCQlDMTI1LDg4LjIsMTMxLDg4LjIsMTM1LjIsOTEuNXoiLz4NCgkNCgkJPGxpbmVhckdyYWRpZW50IGlkPSLnn6nlvaJfMV8iIGdyYWRpZW50VW5pdHM9InVzZXJTcGFjZU9uVXNlIiB4MT0iLTEwMDEuMDgwOSIgeTE9Ijc0LjM1MTgiIHgyPSItMTAwMS4wODA5IiB5Mj0iNzMuMzUxOCIgZ3JhZGllbnRUcmFuc2Zvcm09Im1hdHJpeCgxMzYgMCAwIDEwOCAxMzYyNzUgLTc4NTgpIj4NCgkJPHN0b3AgIG9mZnNldD0iMCIgc3R5bGU9InN0b3AtY29sb3I6IzZEMURFQztzdG9wLW9wYWNpdHk6MC40Ii8+DQoJCTxzdG9wICBvZmZzZXQ9IjAuNjEiIHN0eWxlPSJzdG9wLWNvbG9yOiNBRDUwRjgiLz4NCgkJPHN0b3AgIG9mZnNldD0iMSIgc3R5bGU9InN0b3AtY29sb3I6I0NGNkNGRiIvPg0KCQk8c3RvcCAgb2Zmc2V0PSIxIiBzdHlsZT0ic3RvcC1jb2xvcjojRDA2RkZGIi8+DQoJPC9saW5lYXJHcmFkaWVudD4NCgk8cGF0aCBpZD0i55+p5b2iIiBjbGFzcz0ic3Q0IiBkPSJNMTM2LjgsNjYuOWw1My44LDQwLjRjNS45LDQuMyw3LjIsMTIuNSwyLjksMTguNGMtMC4yLDAuMi0wLjMsMC40LTAuNSwwLjYNCgkJYy0wLjcsMC45LTEuNSwxLjYtMi40LDIuM2wtNTMuOCw0MC40Yy01LjIsMy45LTEyLjMsMy45LTE3LjUsMGwtNTMuOC00MC40Yy01LjktNC4zLTcuMi0xMi41LTIuOS0xOC40YzAuMi0wLjIsMC4zLTAuNCwwLjUtMC42DQoJCWMwLjctMC45LDEuNS0xLjYsMi40LTIuM2w1My44LTQwLjRDMTI0LjQsNjMsMTMxLjUsNjMsMTM2LjgsNjYuOXoiLz4NCjwvZz4NCjxsaW5lYXJHcmFkaWVudCBpZD0icGF0aC0xMi0yXzFfIiBncmFkaWVudFVuaXRzPSJ1c2VyU3BhY2VPblVzZSIgeDE9Ii05OTMuNjYxNiIgeTE9Ijc0LjgzNzciIHgyPSItOTkzLjY2MTYiIHkyPSI3NS41Njc3IiBncmFkaWVudFRyYW5zZm9ybT0ibWF0cml4KDY4IDAgMCA4OCA2NzY5Ni45OTIyIC02NTI2KSI+DQoJPHN0b3AgIG9mZnNldD0iMCIgc3R5bGU9InN0b3AtY29sb3I6I0ZFRkZGRiIvPg0KCTxzdG9wICBvZmZzZXQ9IjEiIHN0eWxlPSJzdG9wLWNvbG9yOiNCQ0M4RDMiLz4NCjwvbGluZWFyR3JhZGllbnQ+DQo8cGF0aCBpZD0icGF0aC0xMi0yIiBjbGFzcz0ic3Q1IiBkPSJNMTM4LDM2YzQuNCwwLDgsMy42LDgsOHY0NGg4LjRjMS45LDAsMy44LDAuNyw1LjMsMS45bDAuMiwwLjJjMi42LDIuMywyLjgsNi4zLDAuNCw4LjkNCgljLTAuMiwwLjMtMC41LDAuNS0wLjcsMC43bDAsMGwtMjYuNCwyMi40Yy0zLDIuNS03LjQsMi41LTEwLjQsMGwwLDBMOTYuNCw5OS44Yy0xLjUtMS4yLTIuNC0zLjEtMi40LTVjMC0zLjgsMy40LTYuOCw3LjYtNi44aDguNA0KCVY0NGMwLTQuNCwzLjYtOCw4LThIMTM4eiIvPg0KPC9zdmc+DQo=";
        if(".svg".equals(iconType)){
            byte[] decodedData = Base64.decode(imageStr, Base64.DEFAULT);
            FileOutputStream svgFile = null;
            File file = new File(context.getFilesDir(), name + "output.svg");
            try {
                svgFile = new FileOutputStream(file.getAbsolutePath());
                svgFile.write(decodedData);
            } catch (IOException e) {
                e.printStackTrace();
            }
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(file.getAbsolutePath());
                SVG svg = SVG.getFromInputStream(inputStream);
                Drawable drawable = new PictureDrawable(svg.renderToPicture());
                return drawable;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SVGParseException e) {
                e.printStackTrace();
            }
            return null;
        } else {
            byte[] decode = Base64.decode(imageStr, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decode, 0, decode.length);
            return new BitmapDrawable(bitmap);
        }
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
        imageView.setImageDrawable(getImage(app.Icon, app.getIconType(), app.getName()));
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
