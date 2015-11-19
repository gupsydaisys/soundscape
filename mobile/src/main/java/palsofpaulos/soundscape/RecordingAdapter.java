package palsofpaulos.soundscape;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import palsofpaulos.soundscape.common.Recording;


public class RecordingAdapter extends ArrayAdapter<Recording> {

    private static final String TAG = "Recording Adapter";

    private Context context;
    private int layoutResourceId;
    private ArrayList<Recording> recs;

    private boolean touchUp = false;
    private boolean touchDown = false;

    public RecordingAdapter(Context context, int layoutResourceId, ArrayList<Recording> data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.recs = data;
    }

    @Override
    public View getView(final int position, final View convertView, ViewGroup parent) {
        final RecHolder holder;
        View row = convertView;
        final ListView parentList = (ListView) parent;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new RecHolder();
            //holder.hScrollView = (HorizontalScrollView) row.findViewById(R.id.h_scroll_view);
            holder.container = (RelativeLayout) row.findViewById(R.id.h_scroll_view_container);
            holder.delButton = (RelativeLayout) row.findViewById(R.id.delete_button);
            holder.delVisible = false;
            holder.recData = (LinearLayout) row.findViewById(R.id.rec_data);
            holder.recText = (TextView) row.findViewById(R.id.rec_text);
            holder.recLength = (TextView) row.findViewById(R.id.rec_length);

            row.setTag(holder);
        } else {
            holder = (RecHolder) row.getTag();
        }

        final Recording rec = recs.get(position);
        holder.recText.setText(String.valueOf(rec.getId()));
        holder.recLength.setText(rec.lengthString());

        /*
        holder.hScrollView.requestDisallowInterceptTouchEvent(true);
        holder.recData.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    touchDown = true;
                }
                else if(event.getAction() == MotionEvent.ACTION_UP){
                    touchUp = true;
                }
                else{
                    touchDown = false;
                    touchUp = false;
                }

                if(touchDown && touchUp){
                    parentList.performItemClick(holder.recData, position, holder.recData.getId());
                }

                return false;
            }
        });

        // delete button click action
        holder.delButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    touchDown = true;
                }
                else if(event.getAction() == MotionEvent.ACTION_UP){
                    touchUp = true;
                }
                else{
                    touchDown = false;
                    touchUp = false;
                }

                if(touchDown && touchUp){
                    rec.delete();
                    recs.remove(position);
                    notifyDataSetChanged();
                }
                return false;
            }
        });
        */
        holder.delButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rec.delete();
                recs.remove(position);
                notifyDataSetChanged();
            }
        });

        return row;
    }

    private static class RecHolder {
        //HorizontalScrollView hScrollView;
        RelativeLayout container;
        boolean delVisible;
        RelativeLayout delButton;
        LinearLayout recData;
        TextView recText;
        TextView recLength;
    }

}