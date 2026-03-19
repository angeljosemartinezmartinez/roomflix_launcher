package verion.desing.launcher.views.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;

import androidx.fragment.app.DialogFragment;

import com.roomflix.tv.R;

public class FragmentExitControl extends DialogFragment {

    public FragmentExitControl() {

    }

    public static FragmentExitControl newInstance(String param1) {
        FragmentExitControl fragment = new FragmentExitControl();
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
                .setView(R.layout.fragment_exit_player);
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
