package verion.desing.launcher.helpers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class FileHelper {
    @Provides
    @Singleton
    FileHelper provideFileHelper() {
        return new FileHelper();
    }


    /*
     * Load file content to String
     * @param filePath string path
     * @return the file String
     */

    public String loadFileAsString(String filePath) throws IOException {
        StringBuilder fileData = new StringBuilder(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
        }
        reader.close();
        return fileData.toString();
    }

}
