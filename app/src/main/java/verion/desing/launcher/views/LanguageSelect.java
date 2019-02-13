package verion.desing.launcher.views;

import android.content.Context;
import android.os.Bundle;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import verion.desing.launcher.R;
import verion.desing.launcher.databinding.ActivityIdiomasBinding;

public class LanguageSelect extends AppCompatActivity {

    private ActivityIdiomasBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_idiomas);
        String background = "/storage/emulated/0/Download/language_select_background/demoimgfondofondoHotelplayIdioma.png";
        loadRoundCorner(background, binding.background);
    }

    public void loadRoundCorner(String url, ImageView view) {
        Context context = view.getContext();
        Glide.with(context).load(url)
                .apply(new RequestOptions()
                        .dontAnimate().diskCacheStrategy(DiskCacheStrategy.NONE)).into(view);
    }
}
