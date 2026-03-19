package verion.desing.launcher.views.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;

import androidx.fragment.app.DialogFragment;
import com.roomflix.tv.R;

public class FragmentCodes extends DialogFragment {

    public FragmentCodes() {

    }

    public static FragmentCodes newInstance(String param1) {
        FragmentCodes fragment = new FragmentCodes();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    public AlertDialog createDialog(Context context) {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(context);
        builder.setCancelable(false)
                .setView(R.layout.fragment_codes)
                .setPositiveButton("Ok", null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                //Dismiss once everything is OK.
                dialog.dismiss();
            });
        });
        return dialog;
    }


    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }
}
