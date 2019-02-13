package verion.desing.launcher.dragger;

import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.lang.reflect.Type;

import javax.inject.Inject;

import dagger.Module;

@Module
public class MySharedPreferences {

    private SharedPreferences mSharedPreferences;

    @Inject
    public MySharedPreferences(SharedPreferences mSharedPreferences) {
        this.mSharedPreferences = mSharedPreferences;
    }

    public void putInt(String key, int data) {
        mSharedPreferences.edit().putInt(key, data).apply();
    }

    public int getInt(String key) {
        return mSharedPreferences.getInt(key, 0);
    }

    public int getIntOne(String key) {
        return mSharedPreferences.getInt(key, 1);
    }

    public void putBoolean(String key, boolean data) {
        mSharedPreferences.edit().putBoolean(key, data).apply();
    }

    public void putString(String key, String data) {
        mSharedPreferences.edit().putString(key, data).apply();

    }

    public boolean getBoolean(String key) {
        return mSharedPreferences.getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return mSharedPreferences.getBoolean(key, defaultValue);
    }

    public String getString(String key) {
        return mSharedPreferences.getString(key, "");
    }

    public void putStringObject(String key, Object data, Type t) {
        Gson g = new Gson();
        String json = g.toJson(data, t);
        mSharedPreferences.edit().putString(key, json).apply();

    }

    public Object getObject(String key, Type t) {
        String json = mSharedPreferences.getString(key, "");
        Gson g = new Gson();
        return g.fromJson(json, t);

    }

    public void deleteAll() {
        mSharedPreferences.edit().clear().apply();


    }

}
