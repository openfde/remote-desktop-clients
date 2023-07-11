/*
 * Copyright 2017 Yan Zhenjie
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ft.fdevnc;

import static com.ft.fdevnc.Constants.BASEURL;
import static com.ft.fdevnc.Constants.URL_GETALLAPP;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.undatech.remoteClientUi.R;
import com.xwdz.http.QuietOkHttp;
import com.xwdz.http.callback.JsonCallBack;
import com.yanzhenjie.recyclerview.OnItemClickListener;
import com.yanzhenjie.recyclerview.SwipeRecyclerView;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;

/**
 * <p>
 * 默认的加载更多的View。
 * </p>
 * Created by YanZhenjie on 2017/7/21.
 */
public class AllAppActivity extends Activity {

    private SwipeRefreshLayout mRefreshLayout;
    private SwipeRecyclerView mRecyclerView;
    private AppAdapter mAdapter;
    private List<AppListResult.DataBeanX.DataBean> mDataList = new ArrayList<>();

    private int page = 1;
    private int pageSize = 10;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_refresh_loadmore);

        mRefreshLayout = findViewById(R.id.refresh_layout);
        mRefreshLayout.setOnRefreshListener(mRefreshListener); // 刷新监听。

        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
//        mRecyclerView.addItemDecoration(new DefaultItemDecoration(getColor( R.color.divider_color)));
        mRecyclerView.setOnItemClickListener(mItemClickListener); // RecyclerView Item点击监听。

        mRecyclerView.useDefaultLoadMore(); // 使用默认的加载更多的View。
        mRecyclerView.setLoadMoreListener(mLoadMoreListener); // 加载更多的监听。
        mRecyclerView.setAutoLoadMore(true);
        mAdapter = new AppAdapter(this, mDataList, null);
        mRecyclerView.setAdapter(mAdapter);
        // 请求服务器加载数据。
        getVncAllApp();

//        WebView viewById = (WebView)findViewById(R.id.web);
//        viewById.loadUrl(BASEURL + URL_GETALLAPP);
    }

    /**
     * 刷新。
     */
    private SwipeRefreshLayout.OnRefreshListener mRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            getVncAllApp();
        }
    };

    /**
     * 加载更多。
     */
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
                        mDataList.addAll(data);
                        mAdapter.notifyDataSetChanged();
                        if(data.size() > 0){
                            page++;
                        }
                        mRecyclerView.loadMoreFinish(mDataList.size() == 0, response.getData().getPage().getTotal() > mDataList.size());
                        mRefreshLayout.setRefreshing(false);
                    }
                });
    }

    /**
     * Item点击监听。
     */
    private OnItemClickListener mItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(View itemView, int position) {
//            Toast.makeText(AllAppActivity.this, "第" + position + "个", Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * 第一次加载数据。
     */
    private void loadData() {
        //todo mock
        AppListResult.DataBeanX.DataBean app = new AppListResult.DataBeanX.DataBean();
        app.Type = "Application";
        app.Path = "/usr/share/code/code";
        app.Icon = "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAMAAAAoLQ9TAAAABGdBTUEAALGPC/xhBQAAACBjSFJNAAB6JgAAgIQAAPoAAACA6AAAdTAAAOpgAAA6mAAAF3CculE8AAABC1BMVEXza2fsW1fkSknyamfxaGXxZ2XwZmPvZGLvZGHvYl/uYV/tYF3tX1ztXlzsXVrsXFnxZ2TvY2DuYV7uYF3tXlvsW1jrWlfvZWLvY2HuYF7tXVvsW1nsWlfqWVbwZmTuYl/rWVbrWFX98/P////97+72srLsXlvqV1XqV1TwZWLvYmDubWv50M/1srDqWFXqVlPuX1ztXVrubGn97u7pVFL++/vrW1jta2jpVlPqVVLpVFHuX13ucW7rWVf5z871sK/pVVLpU1HpU1Dvc3D73d3+/v7pWVboU1DoUk/4xMP2srDub2znUE32sbDnUU7ucG773dzpVVPoUU7nT0zmTUroUE3nTkvmTkvmTEmsXIMGAAAAA3RSTlPAwMA7R0dgAAAAAWJLR0QjKmJsOgAAAAd0SU1FB+YCHAsqO77zI5UAAAD1SURBVBjTHY7rWoJAFEWPhQhKjFxUAhJwyoYuTkXesHKyjLHISsx6/ydp6Of69tn7LIB9qSrLNUWtN7QDHVVKbsqKoZqWZeuo1QZJkjuKo6qHmu0izz+CaldWnMC0tP88jKDbKbmH8fFJ3w9PCcSxcaY2cN89v7gMB4RC7BimdYWvUesmiQi9BSMwxb8hGvnjhE6mKQR1c2brd979+GFO2eMCxP7T8xJjnMxfplnGQfisXtFbGBFxn/EcZra7eh8NB4SxlPN1DsLn43PZI18sXfBNUYDw80NS5tlG8BaQJ3wJLfvr72K3hUpb+E2Y2M/zn93v3h+L7ybTkyKAdAAAACV0RVh0ZGF0ZTpjcmVhdGUAMjAyMi0wMi0yOFQxOTo0Mjo0OSswODowMIuHjewAAAAldEVYdGRhdGU6bW9kaWZ5ADIwMjItMDEtMTBUMTU6MDM6NDMrMDg6MDAY1Ik7AAAAAElFTkSuQmCC";
//        app.Icon = "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAMAAAAoLQ9TAAAABGdBTUEAALGPC/xhBQAAACBjSFJNAAB6JgAAgIQAAPoAAACA6AAAdTAAAOpgAAA6mAAAF3CculE8AAABC1BMVEXza2fsW1fkSknyamfxaGXxZ2XwZmPvZGLvZGHvYl/uYV/tYF3tX1ztXlzsXVrsXFnxZ2TvY2DuYV7uYF3tXlvsW1jrWlfvZWLvY2HuYF7tXVvsW1nsWlfqWVbwZmTuYl/rWVbrWFX98/P////97+72srLsXlvqV1XqV1TwZWLvYmDubWv50M/1srDqWFXqVlPuX1ztXVrubGn97u7pVFL++/vrW1jta2jpVlPqVVLpVFHuX13ucW7rWVf5z871sK/pVVLpU1HpU1Dvc3D73d3+/v7pWVboU1DoUk/4xMP2srDub2znUE32sbDnUU7ucG773dzpVVPoUU7nT0zmTUroUE3nTkvmTkvmTEmsXIMGAAAAA3RSTlPAwMA7R0dgAAAAAWJLR0QjKmJsOgAAAAd0SU1FB+YCHAsqO77zI5UAAAD1SURBVBjTHY7rWoJAFEWPhQhKjFxUAhJwyoYuTkXesHKyjLHISsx6/ydp6Of69tn7LIB9qSrLNUWtN7QDHVVKbsqKoZqWZeuo1QZJkjuKo6qHmu0izz+CaldWnMC0tP88jKDbKbmH8fFJ3w9PCcSxcaY2cN89v7gMB4RC7BimdYWvUesmiQi9BSMwxb8hGvnjhE6mKQR1c2brd979+GFO2eMCxP7T8xJjnMxfplnGQfisXtFbGBFxn/EcZra7eh8NB4SxlPN1DsLn43PZI18sXfBNUYDw80NS5tlG8BaQJ3wJLfvr72K3hUpb+E2Y2M/zn93v3h+L7ybTkyKAdAAAACV0RVh0ZGF0ZTpjcmVhdGUAMjAyMi0wMi0yOFQxOTo0Mjo0OSswODowMIuHjewAAAAldEVYdGRhdGU6bW9kaWZ5ADIwMjItMDEtMTBUMTU6MDM6NDMrMDg6MDAY1Ik7AAAAAElFTkSuQmCC"
        app.IconPath = "/usr/share/icons/hicolor/16x16/mimetypes/wps-office2019-kprometheus.png";
        app.IconType = ".png";
        app.Name = "Visual Studio Code";
        app.ZhName = " ";
        mDataList.add(app);
        mDataList.add(app);
        mDataList.add(app);
        mDataList.add(app);
        mDataList.add(app);
        mDataList.add(app);
        mDataList.add(app);
        mDataList.add(app);
        mDataList.add(app);
        mDataList.add(app);
        mAdapter.notifyDataSetChanged(mDataList);
        mRefreshLayout.setRefreshing(false);

        // 第一次加载数据：一定要调用这个方法，否则不会触发加载更多。
        // 第一个参数：表示此次数据是否为空，假如你请求到的list为空(== null || list.size == 0)，那么这里就要true。
        // 第二个参数：表示是否还有更多数据，根据服务器返回给你的page等信息判断是否还有更多，这样可以提供性能，如果不能判断则传true。
        mRecyclerView.loadMoreFinish(false, true);
    }

    protected List<String> createDataList(int start) {
        List<String> strings = new ArrayList<>();
        for (int i = start; i < start + 100; i++) {
            strings.add("第" + i + "个Item");
        }
        return strings;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }
}