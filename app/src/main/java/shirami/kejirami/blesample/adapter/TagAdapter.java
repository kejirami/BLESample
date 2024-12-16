package shirami.kejirami.blesample.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import shirami.kejirami.blesample.R;

public class TagAdapter extends RecyclerView.Adapter<TagAdapter.ViewHolder> {

    private List<ItemTag> mItemTags;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvId;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            tvId = (TextView) view.findViewById(R.id.litag_id);
        }

        public TextView getTextViewId() {
            return tvId;
        }
    }

    public TagAdapter(List<ItemTag> itemTags) {
        mItemTags = itemTags;
    }

    @NonNull
    @Override
    public TagAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_tag, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TagAdapter.ViewHolder holder, int position) {
        holder.getTextViewId().setText(mItemTags.get(position).getId());
    }

    @Override
    public int getItemCount() {
        return mItemTags.size();
    }
}
