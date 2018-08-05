package com.akhris.xyzreader;

import android.support.design.widget.AppBarLayout;

/**
 * Got this listener class from here:
 * https://stackoverflow.com/a/33891727/7635275
 *
 * It transforms standard offset value to the state value
 */

public abstract class AppBarStateChangeListener implements AppBarLayout.OnOffsetChangedListener {

    protected final static int STATE_IDLE=0;
    protected final static int STATE_EXPANDED=1;
    protected final static int STATE_COLLAPSED=2;

    private int mCurrentState = STATE_IDLE;

    @Override
    public final void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        if (i == 0) {
            if (mCurrentState != STATE_EXPANDED) {
                onStateChanged(appBarLayout, STATE_EXPANDED);
            }
            mCurrentState = STATE_EXPANDED;
        } else if (Math.abs(i) >= appBarLayout.getTotalScrollRange()) {
            if (mCurrentState != STATE_COLLAPSED) {
                onStateChanged(appBarLayout, STATE_COLLAPSED);
            }
            mCurrentState = STATE_COLLAPSED;
        } else {
            if (mCurrentState != STATE_IDLE) {
                onStateChanged(appBarLayout, STATE_IDLE);
            }
            mCurrentState = STATE_IDLE;
        }
    }

    public abstract void onStateChanged(AppBarLayout appBarLayout, int state);
}
