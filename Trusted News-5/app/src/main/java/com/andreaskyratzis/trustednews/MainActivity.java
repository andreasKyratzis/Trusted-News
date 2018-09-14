package com.andreaskyratzis.trustednews;

import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ibm.watson.developer_cloud.http.ServiceCall;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.NaturalLanguageUnderstanding;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.AnalysisResults;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.AnalyzeOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.EntitiesOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.Features;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.KeywordsOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


import twitter4j.GeoLocation;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.URLEntity;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.Twitter;

public class MainActivity extends AppCompatActivity {

    private static final String SEARCH_QUERY = "terror OR UK weather OR politics OR witness OR disaster OR shooting OR score OR explosion OR police OR shock OR protest"; // query string
    private static final Double latitude = 54.541709; //center of united kingdom radius
    private static final Double longitude = -3.617396;
    private static final int radius = 509;            //radius to cover all area
    private static final String resUnit = "km";
    private static final int count = 100;             //100 tweets at a time(max)
    private static final String language = "en";
    private ArrayList<String> tweetArr;

    private static final String twitter_CONSUMER_KEY = "wjMIjhamKZdAmOiDNhTg4f8ou";
    private static final String twitter_CONSUMER_SECRET = "jtnqa59pRNOpzSdw3ObewnHuNAcD3A8h1jVBN3dfRmzGF7dzrg";
    private static final String twitter_ACCESS_TOKEN = "706651736-NYxmJsGUPa5CuWAsNG4l5FZH5waAwRFw8emVob0T";
    private static final String twitter_ACCESS_TOKEN_SECRET = "I0fV4vzqgTPqPguotBIDBA4w0ZojajGSL38L5ZX0xK3NI";


    private LinearLayout sentiment_Layout;
    private BottomNavigationView bottomNavigationView;
    private TextView newsfeed_loadingView, sentiment_loadingView;
    private ListView newsfeed_listview;
    private PieChartView pieChartView;
    private boolean checkScreen;
    private String completedStr;
    int totalCount;
    int positiveCount;
    int neutralCount;
    int negativeCount;

    protected static ArrayList<NewsFeed> tweetModels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();
        setControlEvents();
    }

    private void initUI() {
        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        sentiment_Layout = (LinearLayout) findViewById(R.id.sentiment_layout);
        newsfeed_listview = (ListView) findViewById(R.id.newsfeed_listview);
        newsfeed_loadingView = (TextView) findViewById(R.id.newsfeed_loadingView);
        sentiment_loadingView = (TextView) findViewById(R.id.sentiment_loadingView);
        pieChartView = (PieChartView) findViewById(R.id.pie_chart);
        checkScreen = false;
        new GetNewsFeedTask().execute(SEARCH_QUERY);
    }


    private void setControlEvents() {                                                 //bottom navigation operation
        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_news:
                                newsfeed_listview.setVisibility(View.INVISIBLE);
                                sentiment_Layout.setVisibility(View.INVISIBLE);
                                sentiment_loadingView.setVisibility(View.GONE);
                                pieChartView.setVisibility(View.GONE);
                                checkScreen = false;
                                new GetNewsFeedTask().execute(SEARCH_QUERY);
                                return true;
                            case R.id.action_sentiment:
                                newsfeed_listview.setVisibility(View.INVISIBLE);
                                sentiment_Layout.setVisibility(View.VISIBLE);
                                pieChartView.setVisibility(View.GONE);
                                checkScreen = true;
                                new GetNewsFeedTask().execute(SEARCH_QUERY);
                                return true;
                        }
                        return true;
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {  //refresh operation
        switch (item.getItemId()) {
            case R.id.ic_menu_refresh:
                if (checkScreen) {
                    pieChartView.setVisibility(View.GONE);
                } else {
                    newsfeed_listview.setVisibility(View.INVISIBLE);
                }
                new GetNewsFeedTask().execute(SEARCH_QUERY);
                return true;
        }
        // User didn't trigger a refresh, let the superclass handle this action
        return super.onOptionsItemSelected(item);

    }

    private class GetNewsFeedTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            newsfeed_loadingView.setVisibility(View.VISIBLE);
            tweetModels = new ArrayList<>();
            super.onPreExecute();
        }

        protected String doInBackground(String... searchTerms) {
            completedStr = "";
            ConfigurationBuilder cb = new ConfigurationBuilder();
            cb.setDebugEnabled(true)
                    .setOAuthConsumerKey(twitter_CONSUMER_KEY)
                    .setOAuthConsumerSecret(twitter_CONSUMER_SECRET)
                    .setOAuthAccessToken(twitter_ACCESS_TOKEN)
                    .setOAuthAccessTokenSecret(twitter_ACCESS_TOKEN_SECRET)
                    .setTweetModeExtended(true);
            TwitterFactory tf = new TwitterFactory(cb.build());
            Twitter twitter = (Twitter) tf.getInstance();
            try {
                Query query = new Query().geoCode(new GeoLocation(latitude,longitude), radius, resUnit);
                query.query(SEARCH_QUERY);
                query.count(count);
                query.lang(language);                //The query to the API, uses location, radius of location, radius in km, the string query request, 100 tweets and language
                QueryResult result = twitter.search(query);

                List<twitter4j.Status> tweets = result.getTweets();
                tweetArr = new ArrayList<>();
                for (twitter4j.Status tweet : tweets) {
                    String tweet_text = tweet.getText();
                    if (tweet.getRetweetCount() > 0) {
                        try {
                            if (tweet_text != tweet.getRetweetedStatus().getText()) {
                                tweet_text = tweet.getRetweetedStatus().getText();      //removes duplication of retweeted material
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    tweetArr.add(tweet_text);     //tweet text is added to arraylist
                    tweetModels.add(new NewsFeed(tweet_text)); //show the tweet on the timeline
                }
                completedStr = "Completed";
            }
            catch (TwitterException ex) {
                ex.printStackTrace();
                completedStr = "Failed search: " + ex.getMessage();
            }
            return completedStr;
        }
        @Override
        protected void onPostExecute(String result) {
            Log.e("Process tweet extraction: ", result);
            if (completedStr == "Completed") {
                if (checkScreen) {
                    new CallWatsonTask().execute();
                }
                else {
                    newsfeed_listview.setVisibility(View.VISIBLE);
                    newsfeed_loadingView.setVisibility(View.GONE);
                    newsfeed_listview.setAdapter(new NewsFeedAdapter(tweetModels, getApplicationContext()));
                }

            }
            else {
                if (checkScreen) {
                    sentiment_loadingView.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, completedStr, Toast.LENGTH_LONG).show();
                }
                else {
                    newsfeed_listview.setVisibility(View.VISIBLE);
                    newsfeed_loadingView.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, completedStr, Toast.LENGTH_LONG).show();
                }

            }
        }
    }



    private class CallWatsonTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            totalCount = 0;
            positiveCount = 0;
            neutralCount = 0;
            negativeCount = 0;
            super.onPreExecute();
        }
        protected String doInBackground(String... searchTerms) {
            if (tweetArr.size() > 0) {
                for (String tweetStr : tweetArr) {  //searches for the string in the arraylist of strings
                    try {
                        NaturalLanguageUnderstanding service = new NaturalLanguageUnderstanding("2018-03-16", "3f75cda4-6e1d-4ff3-b34a-b36973fe9c6f", "8bItaIUfyXWS"); //API authentication
                        EntitiesOptions entitiesOptions = new EntitiesOptions.Builder()
                                .emotion(true)
                                .sentiment(true)
                                .limit(2)
                                .build();
                        KeywordsOptions keywordsOptions = new KeywordsOptions.Builder()
                                .emotion(true)
                                .sentiment(true)
                                .limit(2)
                                .build();
                        Features features = new Features.Builder()
                                .entities(entitiesOptions)
                                .keywords(keywordsOptions)
                                .build();
                        AnalyzeOptions parameters = new AnalyzeOptions.Builder()
                                .text(tweetStr)
                                .features(features)
                                .build();
                        AnalysisResults response = service.analyze(parameters).execute();    //the WATSON NLU query
                        System.out.println(response);
                        JSONObject jsonObject = new JSONObject(String.valueOf(response));
                        JSONArray data = jsonObject.getJSONArray("entities");
                        JSONObject subJsonObject = data.getJSONObject(0);
                        JSONObject sentimentObject = subJsonObject.getJSONObject("sentiment");
                        double sentimentValue = sentimentObject.getDouble("score");            //the sentiment score is extracted from json
                        if (sentimentValue > 0) {                //value over 0 is positive
                            positiveCount = positiveCount + 1;
                        }
                        else if (sentimentValue < 0) {         //value below 0 is negative
                            negativeCount = negativeCount + 1;
                        }
                        else {                                 //value = 0
                            neutralCount = neutralCount + 1;
                        }
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

            return completedStr;
        }

        @Override
        protected void onPostExecute(String result) {   //sets the values of each sentiment category to the chart
            Log.e("twitter Result ", result);
            sentiment_loadingView.setVisibility(View.GONE);
            pieChartView.setVisibility(View.VISIBLE);
            totalCount = positiveCount + negativeCount + neutralCount;
            double neutralPer = neutralCount * 100 / totalCount;
            double negativePer = negativeCount * 100 / totalCount;
            double positivePer = 100 - neutralPer - negativePer;
            List<PieChartView.PieceDataHolder> pieceDataHolders = new ArrayList<>();
            pieceDataHolders.add(new PieChartView.PieceDataHolder(neutralCount, Color.CYAN, "Neutral - " + neutralPer + "%"));
            pieceDataHolders.add(new PieChartView.PieceDataHolder(negativeCount, Color.RED, "Negative - " + negativePer + "%"));
            pieceDataHolders.add(new PieChartView.PieceDataHolder(positiveCount, Color.GREEN, "Positive - " + positivePer + "%"));
            pieChartView.setData(pieceDataHolders);
        }
    }

}