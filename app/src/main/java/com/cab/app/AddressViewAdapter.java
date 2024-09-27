package com.cab.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class AddressViewAdapter extends RecyclerView.Adapter<AddressViewAdapter.ViewHolder> {
    private List<Map<String, String>> dataSet;
    private AddressManager addressManager;

    public AddressViewAdapter(List<Map<String, String>> data) {
        this.dataSet = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.address_row_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> address = dataSet.get(position);
        holder.getTitleView().setText(address.get("name") + ", " + address.get("pin"));
        holder.getCityview().setText(address.get("city" + ", " + address.get("street")));
        holder.getStateView().setText(address.get("state"));
        holder.itemView.setOnLongClickListener(view -> {
            notifyDataSetChanged();
            addressManager.removeAddress(address);
            dataSet.remove(position);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return dataSet!=null?dataSet.size():0;
    }

    public void setAddressManager(AddressManager addressManager) {
        this.addressManager = addressManager;
    }

    public void updateData() {
        dataSet = addressManager.getAddresses();
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView state;
        TextView city;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            state = itemView.findViewById(R.id.state);
            city = itemView.findViewById(R.id.city);
        }

        public TextView getTitleView() {
            return title;
        }

        public TextView getCityview() {
            return city;
        }

        public TextView getStateView() {
            return state;
        }
    }
}
