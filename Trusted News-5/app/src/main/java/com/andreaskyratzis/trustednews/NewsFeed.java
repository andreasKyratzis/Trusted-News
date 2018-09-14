package com.andreaskyratzis.trustednews;

import android.graphics.Bitmap;

public class NewsFeed {
    private String tweet_Text;

    // Set Methods

    public NewsFeed (String tweet_Text) {
        this.tweet_Text = tweet_Text;
    }

    // Get Methods

    public String getTweet_Text() { return this.tweet_Text; }


}
