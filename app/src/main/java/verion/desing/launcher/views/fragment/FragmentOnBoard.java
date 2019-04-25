package verion.desing.launcher.views.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import verion.desing.launcher.databinding.FragmentOnBoardBinding;
import verion.desing.launcher.model.ModelTutorial;

/**
 * Created by Daniel Redondo on 02/02/2016.
 */
public class FragmentOnBoard extends Fragment {

    private static final String ARG_MODEL_ONBOARD = "onboard";
    FragmentOnBoardBinding binding;
    private ModelTutorial modelTutorial;

    public static FragmentOnBoard newInstance(ModelTutorial modelTutorial) {
        FragmentOnBoard instance = new FragmentOnBoard();
        Bundle args = new Bundle(5);
        args.putSerializable(ARG_MODEL_ONBOARD, modelTutorial);
        instance.setArguments(args);
        return instance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
            modelTutorial = (ModelTutorial) getArguments().getSerializable(ARG_MODEL_ONBOARD);


    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOnBoardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        fillFields();

    }

    private void fillFields() {

        if (modelTutorial != null) {

            if (modelTutorial.getTitle() != null) binding.title.setText(modelTutorial.getTitle());
            if (modelTutorial.getExplanation() != null)
                binding.longText.setText(modelTutorial.getExplanation());
            if (modelTutorial.getBackground() != 0)
                binding.background.setBackgroundResource(modelTutorial.getBackground());
        }
    }

}

