package com.example.aesndkexample;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class MyImagesAdapter extends RecyclerView.Adapter<MyImagesAdapter.MyViewHolder>
        implements View.OnClickListener {
    private List<Item> items;
    private Context mContext;
    private View.OnClickListener listener;
    public int position;


    public int getPosition() {
        return position;
    }

    public void setOnClickListener(View.OnClickListener listener) {
        this.listener = listener;
    }

    @Override
    public void onClick(View v) {
        if (listener != null)
            listener.onClick(v);
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        //public ImageView imageView;
        public TextView textView;

        public MyViewHolder(View v) {
            super(v);
            //imageView = v.findViewById(R.id.image);
            textView = v.findViewById(R.id.text);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public MyImagesAdapter(Context context, List<Item> items) {
        this.items = items;
        this.mContext = context;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MyImagesAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                           int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycle_list_item, parent, false);
        v.setOnClickListener(this);

        MyViewHolder vh = new MyViewHolder(v);
        return vh;
    }


    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        //holder.imageView.setImageResource(R.drawable.round_person_pin_black_48);
        //holder.imageView.setImageBitmap(items.get(position).getBitmap());
        holder.textView.setText(items.get(position).getFileName());
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return items.size();
    }
}
