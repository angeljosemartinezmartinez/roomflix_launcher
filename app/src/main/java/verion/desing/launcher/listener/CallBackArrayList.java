package verion.desing.launcher.listener;

import java.util.ArrayList;

public interface CallBackArrayList<T> {

    void finish(ArrayList<T> s);

    void error(String localizedMessage);
}
