package com.duitang.milanserver.model;

/**
 * Created by zhangwenbo on 2017/9/13.
 */
public class Word {
    private int[] alternativeList;
    private double wc;
    private long wordBg;
    private long wordEd;
    private String wordsName;
    private String wp;

    public int[] getAlternativeList() {
        return alternativeList;
    }

    public void setAlternativeList(int[] alternativeList) {
        this.alternativeList = alternativeList;
    }

    public double getWc() {
        return wc;
    }

    public void setWc(double wc) {
        this.wc = wc;
    }

    public long getWordBg() {
        return wordBg;
    }

    public void setWordBg(long wordBg) {
        this.wordBg = wordBg;
    }

    public long getWordEd() {
        return wordEd;
    }

    public void setWordEd(long wordEd) {
        this.wordEd = wordEd;
    }

    public String getWordsName() {
        return wordsName;
    }

    public void setWordsName(String wordsName) {
        this.wordsName = wordsName;
    }

    public String getWp() {
        return wp;
    }

    public void setWp(String wp) {
        this.wp = wp;
    }
}

