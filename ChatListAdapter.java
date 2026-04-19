package com.basya.gramm.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.basya.gramm.R;
import com.basya.gramm.model.ChatItem;
import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.VH> {

    public interface OnClick {
        void onClick(ChatItem c);
    }

    private Context ctx;
    private List<ChatItem> items;
    private OnClick listener;

    public ChatListAdapter(Context ctx, List<ChatItem> items, OnClick listener) {
        this.ctx = ctx;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_chat, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        final ChatItem item = items.get(pos);
        h.tvAvatar.setText(item.avatarEmoji != null ? item.avatarEmoji : "😊");
        h.tvName.setText(item.displayName != null ? item.displayName : "");
        h.tvLast.setText(item.lastMessage != null ? item.lastMessage : "");
        h.tvTime.setText(fmtTime(item.lastTime));

        // Бейдж типа
        if ("group".equals(item.type)) {
            h.tvBadge.setText("👥");
            h.tvBadge.setVisibility(View.VISIBLE);
        } else if ("channel".equals(item.type)) {
            h.tvBadge.setText("📢");
            h.tvBadge.setVisibility(View.VISIBLE);
        } else if (item.isBot) {
            h.tvBadge.setText("🤖");
            h.tvBadge.setVisibility(View.VISIBLE);
        } else {
            h.tvBadge.setVisibility(View.GONE);
        }

        // Непрочитанные
        if (item.unread > 0) {
            h.tvUnread.setVisibility(View.VISIBLE);
            h.tvUnread.setText(item.unread > 99 ? "99+" : String.valueOf(item.unread));
        } else {
            h.tvUnread.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClick(item);
            }
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    private String fmtTime(String iso) {
        if (iso == null || iso.isEmpty()) return "";
        try {
            if (iso.contains("T") && iso.length() >= 16) return iso.substring(11, 16);
        } catch (Exception e) { /* ignore */ }
        return "";
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvLast, tvTime, tvUnread, tvBadge;
        VH(View v) {
            super(v);
            tvAvatar = v.findViewById(R.id.tv_avatar);
            tvName   = v.findViewById(R.id.tv_name);
            tvLast   = v.findViewById(R.id.tv_last);
            tvTime   = v.findViewById(R.id.tv_time);
            tvUnread = v.findViewById(R.id.tv_unread);
            tvBadge  = v.findViewById(R.id.tv_badge);
        }
    }
}
