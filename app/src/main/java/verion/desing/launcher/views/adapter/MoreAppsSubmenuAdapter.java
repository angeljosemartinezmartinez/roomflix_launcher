package verion.desing.launcher.views.adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;
import verion.desing.launcher.R;
import verion.desing.launcher.database.tables.Translations;
import verion.desing.launcher.databinding.ItemMoreAppsBinding;
import verion.desing.launcher.helpers.ImageHelper;

public class MoreAppsSubmenuAdapter extends RecyclerView.Adapter<MoreAppsSubmenuAdapter.ViewHolder>{

    public ClickAppCallBack listener;
    private ArrayList<Translations> mBtnList;
    private ItemMoreAppsBinding binding;
    private String baseUrl;

    public MoreAppsSubmenuAdapter(ArrayList<Translations> mBtnList, ClickAppCallBack listener, String baseUrl) {
        this.listener = listener;
        this.mBtnList = mBtnList;
        this.baseUrl = baseUrl;
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

                if (mBtnList.indexOf(item) == 0) {
                    binding.getRoot().clearFocus();
                    binding.image.clearFocus();
                    binding.image.requestFocus();
                    binding.image.requestFocus();
                }
            }
        }
    }
}
