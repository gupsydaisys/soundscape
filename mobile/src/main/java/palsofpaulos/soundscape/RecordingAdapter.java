package palsofpaulos.soundscape;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import palsofpaulos.soundscape.common.Recording;
import palsofpaulos.soundscape.common.Recording.*;


public class RecordingAdapter extends ArrayAdapter<Recording> {
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
        View row = convertView;
        final RecHolder holder;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new RecHolder();
            holder.delButton = (RelativeLayout) row.findViewById(R.id.delete_button);
            holder.recText = (TextView) row.findViewById(R.id.rec_text);
            holder.recLength = (TextView) row.findViewById(R.id.rec_length);

            row.setTag(holder);
        } else {
            holder = (RecHolder) row.getTag();
        }

        final Recording rec = recs.get(position);
        holder.recText.setText(String.valueOf(rec.getId()));
        holder.recLength.setText(rec.lengthString());

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
        RelativeLayout delButton;
        TextView recText;
        TextView recLength;
    }
}