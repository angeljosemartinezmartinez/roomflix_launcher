package verion.desing.launcher.views.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import javax.inject.Inject;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;
import verion.desing.launcher.Constants;
import verion.desing.launcher.R;
import verion.desing.launcher.database.models.Data;
import verion.desing.launcher.database.tables.Language;
import verion.desing.launcher.databinding.ItemIdiomasBinding;
import verion.desing.launcher.dragger.LauncherApplication;
import verion.desing.launcher.dragger.MySharedPreferences;
import verion.desing.launcher.helpers.ImageHelper;
import verion.desing.launcher.helpers.RoundCornerFocus;
import verion.desing.launcher.listener.CallBackViewEvents;

public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.ViewHolder> {
    private final List<Data> mValues;
    public CallBackViewEvents callBackClick;


    public LanguageAdapter(List<Data> values, CallBackViewEvents callBack) {
        mValues = values;
        this.callBackClick = callBack;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ItemIdiomasBinding binding = DataBindingUtil.inflate(layoutInflater, R.layout.item_idiomas, parent, false);

        return new ViewHolder(binding.getRoot(), binding, callBackClick);
    }


    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Data lang = mValues.get(position);
        holder.bind(lang);
    }


    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        if (mValues != null)
            return mValues.size();
        return 0;
    }


    // inner class to hold a reference to each item of RecyclerView
    public class ViewHolder extends RecyclerView.ViewHolder {
        private ItemIdiomasBinding binding;

        public ViewHolder(View rowView, ItemIdiomasBinding binding, CallBackViewEvents callBackClick) {
            super(rowView);
            this.binding = binding;
        }


        public void bind(final Data item) {
            if (item != null) {
                binding.getRoot().setOnFocusChangeListener(new RoundCornerFocus(new RoundCornerFocus.CallBackFocus() {
                    @Override
                    public void focused(View v) {
                        //v.setPadding(10, 10, 10, 10);
                    }

                    @Override
                    public void unFocused(View v) {

                    }
                }));

                binding.getRoot().setOnClickListener(v -> callBackClick.click(item, v));
                new ImageHelper().loadRoundCorner("http://192.168.11.7:8000" + item.getPicture(), binding.image);
                if (item.getCode().toLowerCase().equals("en")) {
                    binding.image.requestFocus();
                    binding.image.setBackground(binding.getRoot().getContext()
                            .getResources().getDrawable(R.drawable.backstreaming));
//                    binding.image.clearFocus();
                    binding.image.requestFocus();
                    binding.image.bringToFront();
                }

            }

        }

    }
}
