package com.roomflix.tv.listener;

public interface CallBackGetOne<T> {
    void finish(T template);

    void error(String localizedMessage);
}
