package com.roomflix.tv.listener;

public interface CallBackAllInfoCheck {

    void dataChange();

    void dataNoChange();

    void error(String macAddress);
}
