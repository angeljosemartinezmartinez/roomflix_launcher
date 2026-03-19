package com.roomflix.tv.listener;

import android.view.View;

public interface CallBackViewEvents<T> {
    void click(T item, View v);
}
