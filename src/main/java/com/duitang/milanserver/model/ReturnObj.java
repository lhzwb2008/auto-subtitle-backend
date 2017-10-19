package com.duitang.milanserver.model;

/**
 * Created by zhangwenbo on 2017/9/14.
 */
public class ReturnObj {

    private String name;

    private String url;

    public ReturnObj(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
