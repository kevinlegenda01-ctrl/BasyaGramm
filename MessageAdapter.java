package com.basya.gramm.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.basya.gramm.R;
import com.basya.gramm.model.Message;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.VH> {

    private static final int TYPE_OWN    = 0;
    private static final int TYPE_OTHER  = 1;
    private static final int TYPE_SYSTEM = 2;

    private Context ctx;
    private List<Message> msgs;
    private String me;

    public MessageAdapter(Context ctx, List<Message> msgs, String me) {
        this.ctx = ctx;
        this.msgs = msgs;
        this.me   = me;
    }

    @Override
    public int getItemViewType(int pos) {
        Message m = msgs.get(pos);
        if ("system".equals(m.type)) return TYPE_SYSTEM;
        return m.isOwn ? TYPE_OWN : TYPE_OTHER;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout;
        if (viewType == TYPE_OWN)         layout = R.layout.item_msg_own;
        else if (viewType == TYPE_SYSTEM) layout = R.layout.item_msg_system;
        else                              layout = R.layout.item_msg_other;
        View v = LayoutInflater.from(ctx).inflate(layout, parent, false);
        return new VH(v, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Message m = msgs.get(pos);

        String display = buildDisplay(m);

        if (h.tvText != null)   h.tvText.setText(display);
        if (h.tvTime != null)   h.tvTime.setText(fmtTime(m.timestamp));
        if (h.tvSender != null && !m.isOwn) h.tvSender.setText("@" + m.from);

        // Цвет для подарков
        if (h.tvText != null && "gift".equals(m.type)) {
            h.tvText.setTextSize(22f);
        }
    }

    private String buildDisplay(Message m) {
        String t = m.type;
        if ("image".equals(t))  return "📷 Фото";
        if ("video".equals(t))  return "🎬 Видео";
        if ("voice".equals(t))  return "🎤 Голосовое сообщение";
        if ("gift".equals(t))   return m.text;
        return m.text;
    }

    @Override
    public int getItemCount() { return msgs.size(); }

    private String fmtTime(String iso) {
        if (iso == null || iso.isEmpty()) return "";
        try {
            if (iso.contains("T") && iso.length() >= 16) return iso.substring(11, 16);
        } catch (Exception e) { /* ignore */ }
        return "";
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvText, tvTime, tvSender;
        VH(View v, int type) {
            super(v);
            tvText   = v.findViewById(R.id.tv_text);
            tvTime   = v.findViewById(R.id.tv_time);
            tvSender = v.findViewById(R.id.tv_sender);
        }
    }
}
