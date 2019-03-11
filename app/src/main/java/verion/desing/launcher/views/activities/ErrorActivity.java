package verion.desing.launcher.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.databinding.DataBindingUtil;
import verion.desing.launcher.Constants;
import verion.desing.launcher.R;
import verion.desing.launcher.databinding.ActivityErrorBinding;
import verion.desing.launcher.listener.CallBackCheckConnection;

public class ErrorActivity extends NetworkBaseActivity {

    private static final String TAG = "ErrorActivity";
    ActivityErrorBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_error);
        imageHelper.loadRoundCorner(R.drawable.error_403, binding.background);
        Bundle b = getIntent().getExtras();
        checkErrorMode(b);
        checkCasesConnection(new CallBackCheckConnection() {
            @Override
            public void success() {
                startActivity(new Intent(getApplicationContext(), MainMenu.class));
            }

            @Override
            public void noPing() {
                runOnUiThread(() -> binding.textError.setText((b.getString(Constants.INTENT_EXTRA.SPLASH_ERROR_MESSAGE))));
            }

            @Override
            public void noConnection() {
                runOnUiThread(() -> binding.textError.setText((b.getString(Constants.INTENT_EXTRA.SPLASH_ERROR_MESSAGE))));
            }
        });
    }

    private boolean checkErrorMode(Bundle b) {
        if (b.getBoolean(Constants.INTENT_EXTRA.SPLASH_ERROR_MODE)) {
            binding.textError.setText((b.getString(Constants.INTENT_EXTRA.SPLASH_ERROR_MESSAGE)));
            return true;
        }
        return false;
    }
}
