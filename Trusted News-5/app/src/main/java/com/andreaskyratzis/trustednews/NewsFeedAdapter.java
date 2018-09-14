package com.andreaskyratzis.trustednews;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class NewsFeedAdapter extends ArrayAdapter<NewsFeed> implements View.OnClickListener {

    private ArrayList<NewsFeed> dataSet;
    Context mContext;

    // View lookup cache
    private static class ViewHolder {
        public TextView tweet_Text;

    }

    public NewsFeedAdapter(ArrayList<NewsFeed> data, Context context) {
        super(context, R.layout.cell_listview, data);
        this.dataSet = data;
        this.mContext=context;
    }

    @Override
    public void onClick(View v) {

        int position=(Integer) v.getTag();
        Object object= getItem(position);
        NewsFeed mainModel =(NewsFeed)object;

    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        final NewsFeed newsFeed = getItem(position);
        // checks if existing view is reused, if not inflate the view
        final ViewHolder viewHolder; // view lookup cache stored in tag

        viewHolder = new ViewHolder();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        convertView = inflater.inflate(R.layout.cell_listview, parent, false);
        viewHolder.tweet_Text = (TextView) convertView.findViewById(R.id.cellTweetText);

        viewHolder.tweet_Text.setText(newsFeed.getTweet_Text());

        convertView.setTag(viewHolder);

        // Return the completed view to render on screen
        return convertView;
    }
}
