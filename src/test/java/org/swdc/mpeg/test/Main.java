package org.swdc.mpeg.test;

import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.model.MediaSegment;
import io.lindstrom.m3u8.parser.MediaPlaylistParser;
import org.swdc.hls.core.mpeg.HLSMerger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Main {


    public static void main(String[] args) throws IOException {

        String url = "https://v.dyjyzyk.dtdjzx.gov.cn/zyk-shengnei/transcode/20250227/3737950026714855679/hls/1500/3737950179806940219.m3u8";
        String playlistText = loadFromURL(url);

        MediaPlaylistParser parser = new MediaPlaylistParser();
        MediaPlaylist playlist = parser.readPlaylist(playlistText);

        /*List<URL> segments = getSegmentsUrls(url, playlist);
        for (URL seg : segments) {
            downloadSegments(seg, new File("output"));
        }*/

        HLSMerger merger = new HLSMerger(playlist, new File("output"),new File("output/merged.mp4"));
        merger.merge();
    }

    /**
     * 从给定的URL下载分段并保存到指定的输出目录。
     *
     * @param seg 分段文件的URL
     * @param outputDir 输出目录，用于保存下载的分段文件
     * @throws RuntimeException 如果无法创建输出目录时抛出
     */
    public static void downloadSegments(URL seg, File outputDir) {
        if (!outputDir.exists()) {
            if(!outputDir.mkdirs()) {
                throw new RuntimeException("Failed to create output directory");
            }
        }
        try {
            String fileName = seg.getFile().substring(seg.getFile().lastIndexOf('/') + 1);
            File target = new File(outputDir,fileName);
            HttpURLConnection connection = (HttpURLConnection) seg.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.setDoOutput(false);
            connection.connect();
            if(connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream is = connection.getInputStream();
                FileOutputStream fos = new FileOutputStream(target);
                is.transferTo(fos);
                fos.close();
                is.close();
                System.out.println("Downloaded: " + target.getAbsolutePath());
            } else {
                connection.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据给定的M3U8播放列表URL和播放列表对象，获取所有分段文件的URL列表。
     *
     * @param m3u8Url    M3U8播放列表的URL
     * @param playlist   包含媒体分段的播放列表对象
     * @return 包含所有分段文件URL的列表
     */
    public static List<URL> getSegmentsUrls(String m3u8Url, MediaPlaylist playlist) {
        List<URL> segmentUrls = new ArrayList<>();
        String parent = m3u8Url.substring(0, m3u8Url.lastIndexOf('/'));
        for (MediaSegment segment : playlist.mediaSegments()) {
            try {
                String segmentUrl = parent + "/" + segment.uri();
                segmentUrls.add(new URI(segmentUrl).toURL());
            } catch (Exception e) {
                continue;
            }
        }
        return segmentUrls;
    }

    /**
     * 从给定的URL加载内容，并返回其内容作为字符串。
     *
     * @param m3u8Url 目标M3U8文件的URL
     * @return 从URL加载的内容作为字符串，如果加载失败或发生异常则返回null
     * @throws Exception 如果发生任何异常，将捕获并返回null
     */
    public static String loadFromURL(String m3u8Url) {
        try {
            URL target = new URL(m3u8Url);
            HttpURLConnection connection = (HttpURLConnection) target.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.setDoOutput(false);
            connection.connect();
            if(connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream is = connection.getInputStream();
                String text = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                connection.disconnect();
                return text;
            }
            connection.disconnect();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

}