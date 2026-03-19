package com.roomflix.tv.listener;

import java.util.ArrayList;

public interface CallBackArrayList<T> {

    void finish(ArrayList<T> s);

    void error(String localizedMessage);
}
