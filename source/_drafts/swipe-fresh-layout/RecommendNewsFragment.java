package com.tencent.PmdCampus.view.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.tencent.PmdCampus.R;
import com.tencent.PmdCampus.adapter.BaseAdapter;
import com.tencent.PmdCampus.comm.view.LoadingFragment;

/**
 * 新鲜事 - 推荐的新鲜事.
 * <p>
 * Created by kingcmchen on 2017/3/7.
 */

public class RecommendNewsFragment extends LoadingFragment implements SwipeRefreshLayout.OnRefreshListener {

    private SwipeRefreshLayout mSrlRoot;
    private RecyclerView mRvList;
    private BaseAdapter<String> mAdapter;

    public static RecommendNewsFragment newInstance() {

        Bundle args = new Bundle();

        RecommendNewsFragment fragment = new RecommendNewsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getContentResourceId() {
        return R.layout.fragment_recommend_news;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mSrlRoot = (SwipeRefreshLayout) view.findViewById(R.id.srl_root);
        mRvList = (RecyclerView) view.findViewById(R.id.rv_list);

        mSrlRoot.setOnRefreshListener(this);

        mAdapter = new BaseAdapter<>(getContext());
        mRvList.setLayoutManager(new LinearLayoutManager(getContext()));
        mRvList.setAdapter(mAdapter);
    }

    @Override
    protected void loadData() {

    }

    @Override
    public void onRefresh() {
        showToast("加载数据");
        mSrlRoot.setRefreshing(false);

        mAdapter.add((mAdapter.getItemCount() + 1) + "");
        mAdapter.notifyDataSetChanged();
    }
}
