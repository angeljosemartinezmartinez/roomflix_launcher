package verion.desing.launcher.model;

import java.io.Serializable;

public class ModelTutorial implements Serializable {
    private String title, explanation;
    private boolean needExitBtn;
    private int imageAndroidInt;
    private String imageAndroid;
    private String imageIos;
    private int imageIosInt;
    private int background;


    public ModelTutorial(String title, String explanation, boolean needExitBtn, int imageAndroidInt) {
        this.title = title;
        this.explanation = explanation;
        this.needExitBtn = needExitBtn;
        this.imageAndroidInt = imageAndroidInt;
    }

    public ModelTutorial(String title, String explanation, boolean needExitBtn, String imageAndroid) {
        this.title = title;
        this.explanation = explanation;
        this.needExitBtn = needExitBtn;
        this.imageAndroid = imageAndroid;
    }

    public ModelTutorial(int background, String title, String explanation, boolean needExitBtn, String imageAndroid) {
        this.background = background;
        this.title = title;
        this.explanation = explanation;
        this.needExitBtn = needExitBtn;
        this.imageAndroid = imageAndroid;
    }

    public ModelTutorial(int background, String title, String explanation, boolean needExitBtn, int imageAndroidInt) {
        this.background = background;
        this.title = title;
        this.explanation = explanation;
        this.needExitBtn = needExitBtn;
        this.imageAndroidInt = imageAndroidInt;
    }


    public String getTitle() {
        return title;
    }

    public String getExplanation() {
        return explanation;
    }

    public boolean getNeedExitBtn() {
        return needExitBtn;
    }

    public int getImageAndroidInt() {
        return imageAndroidInt;
    }

    public String getImageIos() {
        return imageIos;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getBackground() {
        return background;
    }
}
