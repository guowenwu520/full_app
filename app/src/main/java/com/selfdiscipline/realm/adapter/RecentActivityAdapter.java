package com.selfdiscipline.realm.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.selfdiscipline.realm.R;

import java.util.ArrayList;
import java.util.List;

public class RecentActivityAdapter
        extends RecyclerView.Adapter<RecentActivityAdapter.ActivityViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(RecentActivity item, int position);
    }

    private final List<RecentActivity> items = new ArrayList<>();
    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<RecentActivity> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_activity, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ActivityViewHolder holder,
            int position
    ) {
        RecentActivity item = items.get(position);

        holder.icon.setImageResource(item.getIconRes());
        holder.type.setText(item.getType());
        holder.content.setText(item.getContent());
        holder.time.setText(item.getTime());

        // 最后一条不显示底部分割线
        holder.divider.setVisibility(
                position == items.size() - 1 ? View.INVISIBLE : View.VISIBLE
        );

        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (listener != null
                    && adapterPosition != RecyclerView.NO_POSITION) {
                listener.onItemClick(
                        items.get(adapterPosition),
                        adapterPosition
                );
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ActivityViewHolder extends RecyclerView.ViewHolder {

        final ImageView icon;
        final TextView type;
        final TextView content;
        final TextView time;
        final View divider;

        ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.ivActivityIcon);
            type = itemView.findViewById(R.id.tvActivityType);
            content = itemView.findViewById(R.id.tvActivityContent);
            time = itemView.findViewById(R.id.tvActivityTime);
            divider = itemView.findViewById(R.id.activityDivider);
        }
    }
}
