package verion.desing.launcher.network.service.callbacks;

public interface CallBackData<T> {
    void finishAction(T body);

    void error(String s);
}
