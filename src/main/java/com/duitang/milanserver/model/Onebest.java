package com.duitang.milanserver.model;

import java.util.List;

/**
 * Created by zhangwenbo on 2017/9/13.
 */
public class Onebest {
    private long bg;
    private long ed;
    private String onebest;
    private int speaker;
    private double nc;
    private long si;
    private List<Word> wordsResultList;

    public long getBg() {
        return bg;
    }

    public void setBg(long bg) {
        this.bg = bg;
    }

    public long getEd() {
        return ed;
    }

    public void setEd(long ed) {
        this.ed = ed;
    }

    public String getOnebest() {
        return onebest;
    }

    public void setOnebest(String onebest) {
        this.onebest = onebest;
    }

    public int getSpeaker() {
        return speaker;
    }

    public void setSpeaker(int speaker) {
        this.speaker = speaker;
    }

    public double getNc() {
        return nc;
    }

    public void setNc(double nc) {
        this.nc = nc;
    }

    public long getSi() {
        return si;
    }

    public void setSi(long si) {
        this.si = si;
    }

    public List<Word> getWordsResultList() {
        return wordsResultList;
    }

    public void setWordsResultList(List<Word> wordsResultList) {
        this.wordsResultList = wordsResultList;
    }
}
