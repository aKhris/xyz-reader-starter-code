package com.example.xyzreader.ui;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.xyzreader.AppBarStateChangeListener;
import com.example.xyzreader.ColorUtils;
import com.example.xyzreader.R;
import com.example.xyzreader.TransitionUtils;
import com.example.xyzreader.data.ArticleLoader;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * A fragment representing a single Article detail screen.
 *
 * All the transition-related parts are taken from here:
 * https://android-developers.googleblog.com/2018/02/continuous-shared-element-transitions.html
 * (and corresponding github repository: https://github.com/google/android-transition-examples/tree/master/GridToPager)
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, MenuItem.OnMenuItemClickListener {

    @BindView(R.id.article_title) TextView titleView;
    @BindView(R.id.article_byline) TextView bylineView;
    @BindView(R.id.photo) ImageView mPhotoView;
    @BindView(R.id.article_body) TextView bodyView;
    @BindView(R.id.app_bar) AppBarLayout appBarLayout;
    @BindView(R.id.scrollview) NestedScrollView nestedScrollView;
    @BindView(R.id.share_fab) FloatingActionButton shareFab;
    @BindView(R.id.progress_bar) ProgressBar progressBar;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.collapsing_bar) CollapsingToolbarLayout collapsingBar;

    // Key name for saving scroll position of nestedscrollview during orientation change;
    // however it cannot be exactly restored because it's saving position of scrolling, but not the
    // position of text.
    private static final String BUNDLE_SCROLL_POSITION = "scrollview_scroll_position";

    // Array of integers to save scroll coordinates during orientation change;
    private int[] scrollCoordinates = {0,0};

    // Tag using for some logs
    private static final String TAG = "ArticleDetailFragment";

    // Key name of argument passing into static new instance method
    public static final String ARG_ITEM_ID = "item_id";
    private long mItemId;

    //Loader ID
    private static final int LOADER_ID_ONE_ARTICLE=1;

    //Cursor containing one article for this fragment
    private Cursor mCursor;

    private View mRootView;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss", Locale.getDefault());
    // Use default locale format
    @SuppressLint("SimpleDateFormat")
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    /*
        To get bitmap using Picasso.
        Got here: https://stackoverflow.com/a/20181629/7635275
     */
    private Target target = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            mPhotoView.setImageBitmap(bitmap);
            //after setting bitmap to ImageView we must start postponed transition in a parent fragment
            scheduleStartPostponedTransition(mPhotoView);
            Palette
                    .from(bitmap)
                    .generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(@NonNull Palette p) {
                    if(getActivity()!=null) {
                        int darkMutedColor = p.getDarkMutedColor(ContextCompat.getColor(getActivity(), R.color.primaryDarkColor));
                        int mutedColor = p.getMutedColor(ContextCompat.getColor(getActivity(), R.color.primaryColor));
                        ColorUtils.setStatusBarColor(getActivity().getWindow(), darkMutedColor);
                        collapsingBar.setContentScrimColor(mutedColor);
                    }
            }});


        }

        @Override
        public void onBitmapFailed(Exception e, Drawable errorDrawable) {
            if(getParentFragment()!=null) {
                getParentFragment().startPostponedEnterTransition();
            }
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    };



    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }


    /**
     * Static method to instantiate new fragment and set itemId as an argument
     */
    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    /**
     * Getting itemID from arguments (after first initialization)
     * and scrollCoordinates after orientation change
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments()!=null && getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        if(savedInstanceState!=null && savedInstanceState.containsKey(BUNDLE_SCROLL_POSITION)){
            scrollCoordinates = savedInstanceState.getIntArray(BUNDLE_SCROLL_POSITION);
        }

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(LOADER_ID_ONE_ARTICLE, null, this);
    }


    /**
     * Initialize views here
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        ButterKnife.bind(this, mRootView);

        //setting transition name according to given itemID
        ViewCompat.setTransitionName(mPhotoView, TransitionUtils.getTransitionName(mItemId));

        //setting up toolbar.
        //setting navigation icon to make possible to go back to the fragment with a list:
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(getActivity()!=null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            }
        });
        //setting up a menu with a "share" button for sharing content after FAB is scrolled out
        toolbar.inflateMenu(R.menu.menu_article_details);
        toolbar.getMenu().findItem(R.id.share).setOnMenuItemClickListener(this);

        // Listen to AppBarLayout state changing and show/hide it's "share" item
        // (not to have two "share" buttons at the same time on the screen: from the menu
        // and floating action button.

        appBarLayout.addOnOffsetChangedListener(new AppBarStateChangeListener() {
            @Override
            public void onStateChanged(AppBarLayout appBarLayout, int state) {
                MenuItem shareItem = toolbar.getMenu().findItem(R.id.share);

                switch (state){
                    case STATE_IDLE:
                        shareItem.setVisible(false);
                        break;
                    case STATE_COLLAPSED:
                        shareItem.setVisible(true);
                        break;
                    case STATE_EXPANDED:
                        shareItem.setVisible(false);
                        break;
                }
            }
        });

        //show progress bar to show the loading process of the long text to textview
        setLoading(true);

        return mRootView;
    }

    /**
     * This method is binded to share floatingactionbutton and also is called in onclick method for
     * menu item.
     * Here we make some Strings to share.
     */
    @OnClick(R.id.share_fab)
    public void onShareClick(){
        if(mCursor==null){return;}
        Date publishedDate = parsePublishedDate();
        String dateString = DateUtils.getRelativeTimeSpanString(
                publishedDate.getTime(),
                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_ALL).toString();

        String shareString = String.format(Locale.getDefault(),
                "\"%s\" by %s (%s)",
                mCursor.getString(ArticleLoader.Query.TITLE),
                mCursor.getString(ArticleLoader.Query.AUTHOR),
                dateString
                );
        if(getActivity()!=null) {
            startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                    .setType("text/plain")
                    .setText(shareString)
                    .getIntent(), getString(R.string.action_share)));
        }
    }


    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    /**
     * Refreshing views after content changed.
     * Getting Strings from cursor and setting them to textviews
     */
    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        if(mCursor==null || !mCursor.moveToFirst()){
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A" );
            bodyView.setText("N/A");
            return;
        }

            mRootView.setVisibility(View.VISIBLE);

            String title = mCursor.getString(ArticleLoader.Query.TITLE);
            titleView.setText(title);

            toolbar.setTitle(title);

            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                bylineView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            } else {
                // If date is before 1902, just show the string
                bylineView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            }

            // Using background thread to get long String from cursor and setting it to bodyView.
            // It helped to reduce lags a little bit.
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final CharSequence bodyString = Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY).replaceAll("(\r\n|\n)", "<br />"));
                    bodyView.post(new Runnable() {
                        @Override
                        public void run() {
                            setLoading(false);
                            bodyView.setText(bodyString);
                            scroll();
                        }
                    });
                }
            }).start();

            String photoUrl = mCursor.getString(ArticleLoader.Query.PHOTO_URL);
            Picasso.get()
                    .load(photoUrl)
                    .networkPolicy(NetworkPolicy.OFFLINE)
                    .into(target);
    }

    private void scroll(){
        nestedScrollView.post(new Runnable() {
            public void run() {
                nestedScrollView.scrollTo(scrollCoordinates[0], scrollCoordinates[1]);
            }
        });
    }

    /**
     * Methods of LoaderCallbacks
     */

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.v(getClass().getSimpleName(), "Load finished:\nloader: "+cursorLoader.toString()+"\nfor fragment: "+this.toString());
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }
        bindViews();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }


    /**
     * Hide/show progress bar
     */
    private void setLoading(boolean isLoading){
        if(isLoading){
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.INVISIBLE);
        }
    }


    private void scheduleStartPostponedTransition(final View sharedElement) {
        sharedElement.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        sharedElement.getViewTreeObserver().removeOnPreDrawListener(this);
                        if(getParentFragment()!=null) {
                            getParentFragment().startPostponedEnterTransition();
                        }
                        return true;
                    }
                });
    }

    /**
     * Saving scroll position on NestedScrollView.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntArray(BUNDLE_SCROLL_POSITION, new int[]{nestedScrollView.getScrollX(), nestedScrollView.getScrollY()});
    }

    /**
     * Method listening to "share" menu item click.
     */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()){
            case R.id.share:
                onShareClick();
                return true;
        }
        return false;
    }
}
