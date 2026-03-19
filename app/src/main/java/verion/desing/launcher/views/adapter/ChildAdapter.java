package verion.desing.launcher.views.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.roomflix.tv.R;
import verion.desing.launcher.database.tables.Translations;
import com.roomflix.tv.databinding.ItemSubmenuBinding;
import verion.desing.launcher.helpers.ImageHelper;

public class ChildAdapter extends RecyclerView.Adapter<ChildAdapter.ViewHolder> {

    private String baseUrl;
    public ClickAppCallBack listener;
    private ArrayList<Translations> mBtnList;
    private ItemSubmenuBinding binding;

    public ChildAdapter(ArrayList<Translations> buttonList, ClickAppCallBack listener, String baseUrl) {
        this.mBtnList = buttonList;
        this.listener = listener;
        this.baseUrl = baseUrl;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.item_submenu, parent, false);
        return new ViewHolder(binding.getRoot(), binding, listener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Translations btn = mBtnList.get(position);
        holder.bind(btn);

        float factor = holder.itemView.getContext().getResources().getDisplayMetrics().density;

        if (position == 0) {
            binding.image.setLayoutParams(new LinearLayout.LayoutParams((int)(212 * factor), (int)(103 * factor)));
            ((ViewGroup.MarginLayoutParams) binding.image.getLayoutParams()).setMargins((int)(6 * factor), 0, 0, 0);
        } else if (position == 1) {
            binding.image.setLayoutParams(new LinearLayout.LayoutParams((int)(212 * factor), (int)(103 * factor)));
            ((ViewGroup.MarginLayoutParams) binding.image.getLayoutParams()).setMargins((int)(6 * factor), 0, 0, 0);
        } else {
            binding.image.setLayoutParams(new LinearLayout.LayoutParams((int)(103 * factor), (int)(103 * factor)));
            ((ViewGroup.MarginLayoutParams) binding.image.getLayoutParams()).setMargins((int)(6 * factor), 0, 0, 0);
        }
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
        private ItemSubmenuBinding binding;

        public ViewHolder(View itemView, ItemSubmenuBinding itemSubmenuBinding, ClickAppCallBack simpleCallBack) {
            super(itemView);
            this.binding = itemSubmenuBinding;

        }

        public void bind(Translations item) {
            if (item != null) {

                binding.image.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View view, boolean b) {
                        if (b) {
                            new ImageHelper().loadRoundCorner(baseUrl + item.getPictureFocused(), binding.image);
                        } else
                            new ImageHelper().loadRoundCorner(baseUrl + item.getPicture(), binding.image);
                    }
                });
                new ImageHelper().loadRoundCorner(baseUrl + item.getPicture(), binding.image);
                binding.image.setOnClickListener(v -> listener.click(item));

                if (mBtnList.indexOf(item) == 0) {
                    binding.getRoot().clearFocus();
                    binding.image.clearFocus();
                    //binding.image.requestFocus();
                    //binding.image.requestFocus();
                }
            }
        }
    }
}
