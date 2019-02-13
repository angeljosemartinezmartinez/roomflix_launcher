package verion.desing.launcher.listener;

import android.view.View;

public interface CallBackViewEvents<T> {
    void click(T item, View v);

    void focus(T item, View v);

    void unFocus(T item, View v);
}
