package verion.desing.launcher.views.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import androidx.fragment.app.Fragment;
import verion.desing.launcher.database.models.Translation;
import com.roomflix.tv.databinding.InfocardImageBinding;

public class InfoCardImage extends Fragment {

    private static final String ARG_MODEL_ONBOARD = "onboard";
    private static final String ARG_BASE_URL = "baseUrl";
    InfocardImageBinding binding;
    private Translation trans;
    private String baseUrl;

    public static InfoCardImage newInstance(Translation modelTutorial, String baseUrl) {
        InfoCardImage instance = new InfoCardImage();
        Bundle args = new Bundle(5);
        args.putSerializable(ARG_MODEL_ONBOARD, modelTutorial);
        args.putSerializable(ARG_BASE_URL, baseUrl);
        instance.setArguments(args);
        return instance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
            trans = (Translation) getArguments().getSerializable(ARG_MODEL_ONBOARD);
            baseUrl = (String) getArguments().getSerializable(ARG_BASE_URL);


    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = InfocardImageBinding.inflate(inflater, container, false);
        //new ImageHelper().loadSquareFragment(trans.getImg(), binding.image,this);
        Picasso.get().load(baseUrl +  trans.getPicture())
                .into((ImageView) binding.image);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
