package com.akhris.xyzreader.ui;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.akhris.xyzreader.Adapter;
import com.akhris.xyzreader.R;
import com.akhris.xyzreader.data.ArticleLoader;
import com.akhris.xyzreader.data.UpdaterService;
import com.akhris.xyzreader.utils.ColorUtils;
import com.akhris.xyzreader.utils.NetworkUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Fragment with a recyclerview containing articles list.
 *
 * All the transition-related parts are taken from here:
 * https://android-developers.googleblog.com/2018/02/continuous-shared-element-transitions.html
 * (and corresponding github repository: https://github.com/google/android-transition-examples/tree/master/GridToPager)
 */
public class ArticleListFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>,Adapter.AdapterCallback, SwipeRefreshLayout.OnRefreshListener {

    private static final String BUNDLE_SCROLL_POSITION = "recyclerview_scroll_position";
    @BindView(R.id.recycler_view) RecyclerView mRecyclerView;
    @BindView(R.id.swipe_refresh) SwipeRefreshLayout mSwipeRefresh;


    // Loader id
    private static final int LOADER_ID_ALL_ARTICLES=0;

    // Saving scrollPosition here on restoring state
    private int scrollPosition = RecyclerView.NO_POSITION;

    private AtomicBoolean enterTransitionStarted;

    // Using this boolean to differentiate screen rotation and getting to details screen
    private boolean itemClicked = false;

    public ArticleListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_article_list, container, false);
        ButterKnife.bind(this, rootView);
        enterTransitionStarted = new AtomicBoolean();
        int columnCount = getResources().getInteger(R.integer.list_column_count);

        GridLayoutManager layoutManager =
                new GridLayoutManager(getContext(), columnCount);
        mRecyclerView.setLayoutManager(layoutManager);

        if (savedInstanceState != null && savedInstanceState.containsKey(BUNDLE_SCROLL_POSITION)) {
            scrollPosition = savedInstanceState.getInt(BUNDLE_SCROLL_POSITION);
        }

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


        // Wait for necessary picture to get loaded and then resume this transition:
        postponeEnterTransition();

        mSwipeRefresh.setOnRefreshListener(this);
        mSwipeRefresh.setColorSchemeResources(R.color.secondaryLightColor, R.color.secondaryColor, R.color.secondaryDarkColor);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID_ALL_ARTICLES, null, this);
    }


    /**
     * Using intent service to load data from Internet
     * If there is no internet connection show a snackbar:
     * - if there is a list of data already - just show a short snackbar and go away;
     * - if there is no list of data (no possibility of swipe-to-refresh) - show an indefinite snackbar with retry button;
     */
    private void refreshDataFromService(){
        if(getContext()==null){return;}
        mSwipeRefresh.setRefreshing(true);
        mRecyclerView.animate().alpha(0.5f);
        if(NetworkUtils.isOnline(getContext())) {
            getContext().startService(new Intent(getActivity(), UpdaterService.class));
        } else {
            if(getView()!=null) {

                Snackbar snackbar = Snackbar.make(
                        getView(),
                        R.string.message_check_internet,
                        isListExists()?Snackbar.LENGTH_SHORT:Snackbar.LENGTH_INDEFINITE
                );
                    if(!isListExists()) {
                        snackbar.setAction(R.string.retry, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                refreshDataFromService();
                            }
                        });
                    }

                snackbar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        super.onDismissed(transientBottomBar, event);
                        mSwipeRefresh.setRefreshing(false);
                        mRecyclerView.animate().alpha(1f);
                    }
                });
                snackbar.show();
            } else {
                Toast.makeText(getContext(), R.string.message_check_internet, Toast.LENGTH_SHORT).show();
                mSwipeRefresh.setRefreshing(false);
                mRecyclerView.animate().alpha(1f);
            }
        }
    }


    /**
     * LoaderCallbacks methods
     */

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return ArticleLoader.newAllArticlesInstance(getContext());
    }


    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        if(cursor == null || cursor.getCount() == 0){
            //If there is no data in local database, then load it from Internet:
            refreshDataFromService();
            return;
        }
        //If there is some data, stop refreshing animation and bring recyclerview to full alpha.
        mRecyclerView.animate().alpha(1f);
        mSwipeRefresh.setRefreshing(false);
        Adapter adapter = new Adapter(cursor, this);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        scrollToCorrectPosition();

    }


    /**
     * Scroll to correct position:
     * - when rotating the screen (scrollPosition variable is used);
     * - when returning from an article fragment
     * (for article that was navigated to using viewpager and that was not initially on the screen);
     */
    private void scrollToCorrectPosition(){
        Log.d(getClass().getSimpleName(), "scrollToCorrectPosition() called: scrollPosition = "+scrollPosition);
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
                            if(scrollPosition!=RecyclerView.NO_POSITION){
                                layoutManager.scrollToPosition(scrollPosition);
                            } else {
                                View viewAtPosition =
                                        layoutManager.findViewByPosition(MainActivity.currentPosition);
                                // Scroll to position if the view for the current position is null (not
                                // currently part of layout manager children), or it's not completely
                                // visible.
                                if (viewAtPosition == null
                                        || layoutManager.isViewPartiallyVisible(viewAtPosition, false, true)) {
                                    mRecyclerView.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            layoutManager.scrollToPosition(MainActivity.currentPosition);
                                        }
                                    });
                                }
                            }
                        }
                    });
    }

    /**
     * Restart loader if loading from internet is complete
     */
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

    /**
     * Setting status bar color (because it's been changed in ArticleDetailFragment) back to initial
     * value;
     * Registering the receiver;
     */
    @Override
    public void onResume() {
        super.onResume();
        if(getActivity()==null){return;}
        ColorUtils.setStatusBarColor(getActivity().getWindow(), ContextCompat.getColor(getActivity().getApplicationContext(), R.color.primaryDarkColor));
        getActivity().registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    /**
     * Unregistering the receiver
     */
    @Override
    public void onPause() {
        super.onPause();
        if(getActivity()!=null) {
            getActivity().unregisterReceiver(mRefreshingReceiver);
        }
    }

    /**
     * Saving instance state only if the screen is rotating.
     * If it was due to clicking an item - no saving needed (to make possible shared element transition on returning back)
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if(!itemClicked) {
            outState.putInt(BUNDLE_SCROLL_POSITION,
                    ((GridLayoutManager) mRecyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition());
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }


    /**
     *  Callback of recyclerview's adapter.
     *  Replacing this fragment with viewpagerfragment.
     */
    @Override
    public void onItemClick(View itemView, int position) {
        Object transition = getExitTransition();
        if(transition!=null && transition instanceof TransitionSet) {
            ((TransitionSet) transition).excludeTarget(itemView, true);
        }
        View thumbnailView = itemView.findViewById(R.id.thumbnail);
        itemClicked = true;
        if(getFragmentManager()!=null) {
            getFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .addSharedElement(thumbnailView, ViewCompat.getTransitionName(thumbnailView))
                    .replace(R.id.main_fragments_container, new ViewPagerFragment(), ViewPagerFragment.class.getSimpleName())
                    .addToBackStack(null)
                    .commit();
        }
    }

    private boolean isListExists(){
        if(mRecyclerView==null){
            return false;
        }
        if(mRecyclerView.getAdapter()==null){
            return false;
        }
        if(mRecyclerView.getAdapter().getItemCount()==0){
            return false;
        }
        return true;
    }

    /**
     * Callback of recyclerview's adapter.
     * Called when Picasso loaded picture or got an error to postpone transition here;
     */
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



    /**
     * Callback of SwipeRefreshLayout.
     * Force to update data from internet here.
     */
    @Override
    public void onRefresh() {
        refreshDataFromService();
    }
}
