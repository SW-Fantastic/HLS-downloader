package org.swdc.hls.core;

import java.net.HttpURLConnection;
import java.net.URL;

public class HLSSegment {

    private String segmentName;

    private URL segmentURL;

    private HLSKeySet keySet;

    public HLSSegment(String segmentName, URL segmentURL) {
        this.segmentName = segmentName;
        this.segmentURL = segmentURL;
    }

    public HLSKeySet getKeySet() {
        return keySet;
    }

    public void setKeySet(HLSKeySet keySet) {
        this.keySet = keySet;
    }

    public String getSegmentName() {
        return segmentName;
    }

    public void setSegmentName(String segmentName) {
        this.segmentName = segmentName;
    }

    public URL getSegmentURL() {
        return segmentURL;
    }

    public void setSegmentURL(URL segmentURL) {
        this.segmentURL = segmentURL;
    }

    HttpURLConnection openConnection() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) segmentURL.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoInput(true);
        connection.setDoOutput(false);
        connection.setConnectTimeout(1000 * 30);
        connection.setReadTimeout(1000 * 30);
        connection.connect();
        return connection;
    }

}
