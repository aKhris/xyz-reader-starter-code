package com.akhris.xyzreader.ui;


import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.akhris.xyzreader.R;
import com.akhris.xyzreader.data.Article;
import com.akhris.xyzreader.data.ArticleLoader;

import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Fragment containing just the viewpager and adapter for it.
 *
 * All the transition-related parts are taken from here:
 * https://android-developers.googleblog.com/2018/02/continuous-shared-element-transitions.html
 * (and corresponding github repository: https://github.com/google/android-transition-examples/tree/master/GridToPager)
 */

public class ViewPagerFragment extends Fragment
    implements LoaderManager.LoaderCallbacks<Cursor>,
        ArticleDetailFragment.ArticleCallback
    {
    @BindView(R.id.pager) ViewPager mViewPager;

    private Cursor mCursor;

    private static final int LOADER_ID_ALL_ARTICLES=0;


    private MyPagerAdapter mPagerAdapter;

    public ViewPagerFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Retain instance to save mCursor variable to keep ArticleDetailFragment.ArticleCallback working
        //after orientation change
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_view_pager, container, false);
        ButterKnife.bind(this, rootView);
        mPagerAdapter = new MyPagerAdapter(getChildFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){
            @Override
            public void onPageSelected(int position) {
                    MainActivity.currentPosition = position;
            }
        });

        Transition transition =
                TransitionInflater.from(getContext())
                        .inflateTransition(R.transition.image_shared_element_transition);

        setSharedElementEnterTransition(transition);

        setEnterSharedElementCallback(new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                // Locate the image view at the primary fragment (the ImageFragment that is currently
                // visible). To locate the fragment, call instantiateItem with the selection position.
                // At this stage, the method will simply return the fragment at the position and will
                // not create a new one.
                Fragment currentFragment = (Fragment) mPagerAdapter
                        .instantiateItem(mViewPager, MainActivity.currentPosition);
                View view = currentFragment.getView();
                if (view == null) {
                    return;
                }

                // Map the first shared element name to the child ImageView.
                sharedElements.put(names.get(0), view.findViewById(R.id.photo));
            }
        });

        // Avoid a postponeEnterTransition on orientation change, and postpone only of first creation.
        if (savedInstanceState == null) {
            postponeEnterTransition();
        }

        return rootView;
    }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            getLoaderManager().initLoader(LOADER_ID_ALL_ARTICLES, null, this);
        }

        @NonNull
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return ArticleLoader.newAllArticlesInstance(getContext());
        }

        @Override
        public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
            mCursor = cursor;
            mPagerAdapter.notifyDataSetChanged();
            mViewPager.setCurrentItem(MainActivity.currentPosition, false);
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Cursor> loader) {
            mCursor = null;
            mPagerAdapter.notifyDataSetChanged();
        }

        /**
         * called from ArticleDetailFragment when it's ready to fill it's views with article details;
         */
        @Override
        public Article getArticle(final int positionInCursor) {
            mCursor.moveToPosition(positionInCursor);
            return Article.parseCursor(mCursor);
        }

        /**
         * Simple adapter for Viewpager
         */
        private class MyPagerAdapter extends FragmentStatePagerAdapter {
        MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return ArticleDetailFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }

}
