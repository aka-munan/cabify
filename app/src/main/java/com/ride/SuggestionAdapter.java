package com.ride;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cab.app.MainActivity;
import com.cab.app.R;

import java.util.List;
import java.util.Map;

public class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.ViewHolder> {

    private final MainActivity activity;
    private List<Map<String, String>> dataSet;

    public SuggestionAdapter(MainActivity activity,List<Map<String,String>> dataSet){
        this.activity = activity;
        this.dataSet = dataSet;
    }
    public void updateData(List<Map<String,String>> dataSet){
        this.dataSet = dataSet;
        Log.i("suggestion", "updateData: "+dataSet);
        this.notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.suggestion_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String,String> item = dataSet.get(position);
        String name = item.get("name");
        holder.itemView.setOnClickListener(view->{
            assert activity!=null;
            activity.locationPicked(dataSet.get(position));
        });
        if (name==null)
            return;
        int index = name.indexOf(",");
        if (index<=0){
            holder.title.setText(name);
            return;
        }
        String title=name.substring(0,index);
        String desc = item.get("name").substring(index+1, name.length());
        holder.title.setText(title);
        holder.description.setHint(desc);
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView title,description;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title=itemView.findViewById(R.id.title);
            description = itemView.findViewById(R.id.secondary_text);

        }

    }
}
