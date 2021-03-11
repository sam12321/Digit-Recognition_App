package com.example.digitrecognitionapp.models;

public class Classification {

    //conf is the output
    private float conf;
    //input label
    private String label;

    public final String title;
    public final float confidence;

  //  Classification() { }

    void update(float conf, String label) {
        this.conf = conf;
        this.label = label;
    }

    public Classification(String title, float confidence) {
        this.title = title;
        this.confidence = confidence;
        this.conf = -1.0F;
        this.label = null;
    }

    @Override
    public String toString() {
        return title + " " + String.format("(%.1f%%) ", confidence * 100.0f);
    }

    public String getLabel() {
        return label;
    }

    public float getConf() {
        return conf;
    }
}
