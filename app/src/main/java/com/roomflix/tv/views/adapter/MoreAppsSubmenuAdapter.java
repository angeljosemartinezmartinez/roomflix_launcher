package com.roomflix.tv.views.adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.roomflix.tv.R;
import com.roomflix.tv.database.tables.Translations;
import com.roomflix.tv.databinding.ItemMoreAppsBinding;
import com.roomflix.tv.helpers.ImageHelper;

public class MoreAppsSubmenuAdapter extends RecyclerView.Adapter<MoreAppsSubmenuAdapter.ViewHolder>{

    public ClickAppCallBack listener;
    private ArrayList<Translations> mBtnList;
    private ItemMoreAppsBinding binding;
    private String baseUrl;
    private boolean isGridMode;

    public MoreAppsSubmenuAdapter(ArrayList<Translations> mBtnList, ClickAppCallBack listener, String baseUrl) {
        this(mBtnList, listener, baseUrl, false);
    }

    public MoreAppsSubmenuAdapter(ArrayList<Translations> mBtnList, ClickAppCallBack listener, String baseUrl, boolean isGridMode) {
        this.listener = listener;
        this.mBtnList = mBtnList;
        this.baseUrl = baseUrl;
        this.isGridMode = isGridMode;
    }

    public void setItems(ArrayList<Translations> items) {
        this.mBtnList = items != null ? items : new ArrayList<>();
    }

    public void setGridMode(boolean gridMode) {
        this.isGridMode = gridMode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.item_more_apps, parent, false);
        return new ViewHolder(binding.getRoot(), binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (isGridMode) {
            holder.itemView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        Translations btn = mBtnList.get(position);
        holder.bind(btn);
    }

    @Override
    public int getItemCount() {
        if (mBtnList != null)
            return mBtnList.size();
        return 0;
    }

    public interface ClickAppCallBack {
        void click(Translations action);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private ItemMoreAppsBinding binding;

        public ViewHolder(View itemView, ItemMoreAppsBinding itemMoreAppsBinding, ClickAppCallBack simpleCallBack) {
            super(itemView);
            this.binding = itemMoreAppsBinding;

        }

        public void bind(Translations item) {
            if (item != null) {
                binding.image.setOnFocusChangeListener((view, b) -> {
                    if (b) {
                        new ImageHelper().loadRoundCorner(baseUrl + item.getPictureFocused(), binding.image);
                    } else
                        new ImageHelper().loadRoundCorner(baseUrl + item.getPicture(), binding.image);
                });
                new ImageHelper().loadRoundCorner(baseUrl + item.getPicture(), binding.image);
                binding.image.setOnClickListener(v -> listener.click(item));

                if (getBindingAdapterPosition() == 0) {
                    binding.getRoot().clearFocus();
                    binding.image.clearFocus();
                    binding.image.requestFocus();
                    binding.image.requestFocus();
                }
            }
        }
    }
}
