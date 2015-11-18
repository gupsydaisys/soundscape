package palsofpaulos.soundscape;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import palsofpaulos.soundscape.common.LayoutAnimations.WidthAnimation;
import palsofpaulos.soundscape.common.Recording;
import palsofpaulos.soundscape.common.Recording.*;
import palsofpaulos.soundscape.common.SwipeListener;


public class RecordingAdapter extends ArrayAdapter<Recording> {

    private static final String TAG = "Recording Adapter";

    private Context context;
    private int layoutResourceId;
    private ArrayList<Recording> recs;

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

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new RecHolder();
            holder.delButton = (RelativeLayout) row.findViewById(R.id.delete_button);
            holder.delVisible = false;
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
        final View dataView = row.findViewById(R.id.rec_data);
        dataView.setOnTouchListener(new SwipeListener(row.getContext()) {
            @Override
            public void onClick() {
                if (holder.delVisible) {
                    WidthAnimation closeAnim = new WidthAnimation(dataView, holder.initViewWidth - holder.delButton.getWidth(), holder.initViewWidth, 400);
                    closeAnim.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            holder.delVisible = false;
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    dataView.startAnimation(closeAnim);
                }
            }

            @Override
            public void onSwipeLeft() {
                Log.d(TAG, "Swipe left detected on view " + dataView.toString());
                if (!holder.delVisible) {
                    holder.delVisible = true;
                    holder.initViewWidth = dataView.getWidth();
                    dataView.startAnimation(new WidthAnimation(dataView, holder.initViewWidth, holder.initViewWidth - holder.delButton.getWidth(), 400));
                }
            }
        });
        */

        // delete button click action
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
        int initViewWidth;
        boolean delVisible;
        RelativeLayout delButton;
        TextView recText;
        TextView recLength;
    }

    public interface ClickHandler {
        void onClick(View v, int position);
    }

}