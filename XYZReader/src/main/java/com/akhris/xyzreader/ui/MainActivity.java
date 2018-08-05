package com.akhris.xyzreader.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.akhris.xyzreader.R;

public class MainActivity
        extends AppCompatActivity
{

    public static int currentPosition=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(savedInstanceState==null) {
            navigateToArticleListFragment();
        }
    }

    private void navigateToArticleListFragment(){
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.main_fragments_container, new ArticleListFragment(), ArticleDetailFragment.class.getSimpleName())
                .commit();
    }
}
