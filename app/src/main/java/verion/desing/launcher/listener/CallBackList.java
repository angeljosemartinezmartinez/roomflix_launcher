package verion.desing.launcher.listener;

import java.util.List;

public interface CallBackList<T> {
    void finish(List<T> buttons);

    void error(String localizedMessage);

}
