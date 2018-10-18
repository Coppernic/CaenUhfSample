package fr.coppernic.sample.caenuhf.reader;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import fr.coppernic.sample.caenuhf.R;

public class Adapter extends RecyclerView.Adapter<Adapter.MyViewHolder> {
    private ArrayList<Tag> tags;

    // Provide a reference to the views for each data item
    static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvTag, tvCount;

        MyViewHolder(View itemView) {
            super(itemView);
            tvTag = itemView.findViewById(R.id.tvTag);
            tvCount = itemView.findViewById(R.id.tvCount);
        }
    }

    //Constructor
    public Adapter(ArrayList<Tag> tags) {
        this.tags = tags;
    }

    @Override
    public Adapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tag, parent, false);

        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.tvTag.setText(tags.get(position).getEpc());
        holder.tvCount.setText(String.valueOf(tags.get(position).getCount()));
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }


}
