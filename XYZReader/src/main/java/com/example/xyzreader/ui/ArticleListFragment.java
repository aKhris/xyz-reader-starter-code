package com.example.xyzreader.ui;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.TransitionInflater;
import android.transition.TransitionSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.xyzreader.Adapter;
import com.example.xyzreader.ColorUtils;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.UpdaterService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A simple {@link Fragment} subclass.
 */
public class ArticleListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,Adapter.AdapterCallback {

    @BindView(R.id.recycler_view) RecyclerView mRecyclerView;
    @BindView(R.id.swipe_refresh) SwipeRefreshLayout mSwipeRefresh;


    private static final int LOADER_ID_ALL_ARTICLES=0;

    private AtomicBoolean enterTransitionStarted;

    public ArticleListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_article_list, container, false);
        ButterKnife.bind(this, rootView);
        enterTransitionStarted = new AtomicBoolean();
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        GridLayoutManager layoutManager =
                new GridLayoutManager(getContext(), columnCount);
        mRecyclerView.setLayoutManager(layoutManager);

        setExitTransition(TransitionInflater.from(getContext())
                .inflateTransition(R.transition.grid_exit_transition));

        setExitSharedElementCallback(new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                RecyclerView.ViewHolder selectedViewHolder = mRecyclerView
                        .findViewHolderForAdapterPosition(MainActivity.currentPosition);
                if (selectedViewHolder == null || selectedViewHolder.itemView == null) {
                    return;
                }

                // Map the first shared element name to the child ImageView.
                sharedElements
                        .put(names.get(0), selectedViewHolder.itemView.findViewById(R.id.thumbnail));
            }
            });

        postponeEnterTransition();
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getLoaderManager().initLoader(LOADER_ID_ALL_ARTICLES, null, this);

    }


    private void refresh(){
        getActivity().startService(new Intent(getActivity(), UpdaterService.class));
    }



    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return ArticleLoader.newAllArticlesInstance(getContext());
    }


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if(cursor!=null && cursor.getCount()==0){
            refresh();
            return;
        }
        Adapter adapter = new Adapter(cursor, this);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        scrollToCorrectPosition();

    }

    private void scrollToCorrectPosition(){
        mRecyclerView.addOnLayoutChangeListener(
                new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View view,
                                               int left,
                                               int top,
                                               int right,
                                               int bottom,
                                               int oldLeft,
                                               int oldTop,
                                               int oldRight,
                                               int oldBottom) {
                        mRecyclerView.removeOnLayoutChangeListener(this);
                        final RecyclerView.LayoutManager layoutManager =
                                mRecyclerView.getLayoutManager();
                        View viewAtPosition =
                                layoutManager.findViewByPosition(MainActivity.currentPosition);
                        // Scroll to position if the view for the current position is null (not
                        // currently part of layout manager children), or it's not completely
                        // visible.
                        if (viewAtPosition == null
                                || layoutManager.isViewPartiallyVisible(viewAtPosition, false, true)){
                            mRecyclerView.post(new Runnable() {
                                @Override
                                public void run() {
                                    layoutManager.scrollToPosition(MainActivity.currentPosition);
                                }
                            });
                        }
                    }
                });

    }


    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                boolean mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                if(!mIsRefreshing){
                    getLoaderManager().restartLoader(LOADER_ID_ALL_ARTICLES, null, ArticleListFragment.this);
                }
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        ColorUtils.setStatusBarColor(getActivity().getWindow(), ContextCompat.getColor(getContext(), R.color.primaryDarkColor));
        getActivity().registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mRefreshingReceiver);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    @Override
    public void onItemClick(View itemView, long itemId) {

        ((TransitionSet) getExitTransition()).excludeTarget(itemView, true);

        View thumbnailView = itemView.findViewById(R.id.thumbnail);

        getFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .addSharedElement(thumbnailView, ViewCompat.getTransitionName(thumbnailView))
                .replace(R.id.main_fragments_container, ViewPagerFragment.getInstance(itemId), ViewPagerFragment.class.getSimpleName())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onPictureLoaded(int position) {
        if (MainActivity.currentPosition != position) {
            return;
        }
        if (enterTransitionStarted.getAndSet(true)) {
            return;
        }
        startPostponedEnterTransition();
    }
}
