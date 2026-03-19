package com.roomflix.tv.network.callbacks;

public interface CallBackData<T> {
    void finishAction(T body);

    void error(String s);
}
