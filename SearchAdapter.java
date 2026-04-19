package com.basya.gramm.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.basya.gramm.R;
import org.json.JSONObject;
import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.VH> {

    public interface OnClick {
        void onClick(JSONObject user);
    }

    private Context ctx;
    private List<JSONObject> items;
    private OnClick listener;

    public SearchAdapter(Context ctx, List<JSONObject> items, OnClick listener) {
        this.ctx = ctx;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_user, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        final JSONObject user = items.get(pos);
        try {
            h.tvAvatar.setText(user.optString("avatar_emoji", "😊"));
            h.tvName.setText(user.optString("display_name", ""));
            h.tvUser.setText("@" + user.optString("username", ""));
            h.tvBio.setText(user.optString("bio", ""));
        } catch (Exception e) { /* ignore */ }

        h.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClick(user);
            }
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvUser, tvBio;
        VH(View v) {
            super(v);
            tvAvatar = v.findViewById(R.id.tv_avatar);
            tvName   = v.findViewById(R.id.tv_name);
            tvUser   = v.findViewById(R.id.tv_username);
            tvBio    = v.findViewById(R.id.tv_bio);
        }
    }
}
