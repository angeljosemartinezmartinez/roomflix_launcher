package verion.desing.launcher.helpers;

import android.view.View;
import android.widget.ImageView;

import com.roomflix.tv.R;

public class RoundCornerFocus implements View.OnFocusChangeListener {

    private final CallBackFocus listener;
    public static RoundCornerFocus instance;

    public static RoundCornerFocus getInstance(CallBackFocus listener) {
        if (instance == null)
            instance = new RoundCornerFocus(listener);
        return instance;
    }
    public RoundCornerFocus(CallBackFocus listener) {
        this.listener = listener;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {

        //Animation zoomin = AnimationUtils.loadAnimation(v.getContext(), R.anim.zoom_in);
        //Animation zoomout = AnimationUtils.loadAnimation(v.getContext(), R.anim.zoom_out);
        if (hasFocus) {
            v.bringToFront();
            if (listener != null) listener.focused(v);
            //v.startAnimation(zoomin);

            if (v instanceof ImageView)
                ((ImageView) v).setAdjustViewBounds(true);

            //v.setPadding(5, 5, 5, 5);
            v.setBackgroundResource(R.drawable.backstreaming);


        } else {
            if (listener != null) listener.unFocused(v);
            //v.startAnimation(zoomout);
            v.setPadding(0, 0, 0, 0);
            v.setBackgroundColor(v.getContext().getColor(R.color.transparent));
        }
    }

    public interface CallBackFocus {
        void focused(View v);

        void unFocused(View v);
    }
}
