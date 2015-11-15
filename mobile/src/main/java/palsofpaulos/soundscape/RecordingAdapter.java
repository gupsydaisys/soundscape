package palsofpaulos.soundscape;

import android.app.Activity;
import android.content.Context;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import palsofpaulos.soundscape.common.Recording;
import palsofpaulos.soundscape.common.Recording.*;


public class RecordingAdapter extends ArrayAdapter<Recording> {
    Context context;
    int layoutResourceId;
    ArrayList<Recording> recs;

    private Recording playingRec;

    public RecordingAdapter(Context context, int layoutResourceId, ArrayList<Recording> data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.recs = data;
    }

    @Override
    public View getView(final int position, final View convertView, ViewGroup parent) {
        View row = convertView;
        final RecHolder holder;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new RecHolder();
            holder.playButton = (ImageView) row.findViewById(R.id.play_pause);
            holder.delButton = (TextView) row.findViewById(R.id.delete_button);
            holder.recText = (TextView) row.findViewById(R.id.recText);
            holder.recLenth = (TextView) row.findViewById(R.id.recLength);

            row.setTag(holder);
        } else {
            holder = (RecHolder) row.getTag();
        }

        final Recording rec = recs.get(position);
        holder.recText.setText(rec.getFilePath());
        holder.recLenth.setText(rec.lengthString());
        holder.playButton.setImageResource(R.drawable.play);
        playingRec = null;
        holder.isPlay = false;

        final PostPlayListener playListener = new PostPlayListener() {
            @Override
            public void onFinished() {
                playingRec = null;
                holder.isPlay = false;
                holder.playButton.setImageResource(R.drawable.play);
            }
        };
        holder.playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playingRec == null) {
                    playingRec = rec;
                    rec.play(playListener);
                    holder.isPlay = true;
                    holder.playButton.setImageResource(R.drawable.pause);
                }
                else if (playingRec.getId() != rec.getId()) {
                    playingRec.stop();
                    playingRec = rec;
                    rec.play(playListener);
                    holder.isPlay = true;
                    holder.playButton.setImageResource(R.drawable.pause);
                }
                if (holder.isPlay) {
                    holder.isPlay = false;
                    holder.playButton.setImageResource(R.drawable.play);
                    rec.pause();
                } else {
                    holder.isPlay = true;
                    holder.playButton.setImageResource(R.drawable.pause);
                    rec.play();
                }
            }
        });
        holder.delButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        return row;
    }

    private static class RecHolder {
        ImageView playButton;
        TextView delButton;
        boolean isPlay;
        TextView recText;
        TextView recLenth;
    }
}