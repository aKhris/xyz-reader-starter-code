package com.example.xyzreader.data;

import android.database.Cursor;

/**
 * Class representing one article that ArticleDetailFragment should show.
 * This class was made to reduce the number of database usages and it looks like it works faster -
 * to parse a cursor entry into Article object and pass it to ArticleDetailFragment than to load it
 * there using Loader.
 */

public class Article {

    private long mId;
    private String mTitle;
    private String mAuthor;
    private String mBody;
    private String mThumb;
    private String mPhoto;
    private float mAspect;
    private String mDate;

    public Article(long mId, String mTitle, String mAuthor, String mBody, String mThumb, String mPhoto, float mAspect, String mDate) {
        this.mId = mId;
        this.mTitle = mTitle;
        this.mAuthor = mAuthor;
        this.mBody = mBody;
        this.mThumb = mThumb;
        this.mPhoto = mPhoto;
        this.mAspect = mAspect;
        this.mDate = mDate;
    }

    public long getId() {
        return mId;
    }


    public String getTitle() {
        return mTitle;
    }

    public String getAuthor() {
        return mAuthor;
    }

    public String getBody() {
        return mBody;
    }

    public String getThumb() {
        return mThumb;
    }

    public String getPhoto() {
        return mPhoto;
    }

    public float getAspect() {
        return mAspect;
    }

    public String getDate() {
        return mDate;
    }

    public static Article parseCursor(Cursor cursor){
        long id = cursor.getLong(ArticleLoader.Query._ID);
        String title = cursor.getString(ArticleLoader.Query.TITLE);
        String author = cursor.getString(ArticleLoader.Query.AUTHOR);
        String body = cursor.getString(ArticleLoader.Query.BODY);
        String thumb = cursor.getString(ArticleLoader.Query.THUMB_URL);
        String photo = cursor.getString(ArticleLoader.Query.PHOTO_URL);
        float aspect = cursor.getFloat(ArticleLoader.Query.ASPECT_RATIO);
        String date = cursor.getString(ArticleLoader.Query.PUBLISHED_DATE);

        return new Article(
                id, title, author, body, thumb, photo, aspect, date
        );
    }
}
