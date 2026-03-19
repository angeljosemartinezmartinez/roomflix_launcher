package com.roomflix.tv.model;

public class Button {

    private String img;
    private String imgFocused;

    public Button(String img, String imgFocused) {
        this.img = img;
        this.imgFocused = imgFocused;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public String getImgFocused() {
        return imgFocused;
    }

    public void setImgFocused(String imgFocused) {
        this.imgFocused = imgFocused;
    }

    @Override
    public String toString() {
        return "Button{" +
                "img='" + img + '\'' +
                ", imgFocused='" + imgFocused + '\'' +
                '}';
    }
}
