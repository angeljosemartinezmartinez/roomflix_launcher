package verion.desing.launcher.views.activities;

import android.content.Intent;
import android.os.Bundle;

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

        Bundle b = getIntent().getExtras();
        checkErrorMode(b);
        checkCasesConnection(new CallBackCheckConnection() {
            @Override
            public void success() {
                startActivity(new Intent(getApplicationContext(), MainMenu.class));
            }

            @Override
            public void noPing() {
                runOnUiThread(() -> imageHelper.loadRoundCorner(b.getInt(Constants.INTENT_EXTRA.ERROR_BACKGROUND), binding.background));
            }

            @Override
            public void noConnection() {
                runOnUiThread(() -> imageHelper.loadRoundCorner(b.getInt(Constants.INTENT_EXTRA.ERROR_BACKGROUND), binding.background));
            }
        });
    }

    private boolean checkErrorMode(Bundle b) {
        if (b.getBoolean(Constants.INTENT_EXTRA.ERROR_TYPE))
            return true;
        return false;
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
    }
}
