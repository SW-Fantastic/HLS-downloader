package org.swdc.hls.core;

import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.model.MediaSegment;
import io.lindstrom.m3u8.model.SegmentKey;
import io.lindstrom.m3u8.parser.MediaPlaylistParser;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swdc.hls.core.mpeg.HLSMerger;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class HLSDownloadTask {

    private File segmentDir;

    private String fileName;

    private String hlsUrl;

    private String outputDir;

    private String segmentDirName = UUID.randomUUID().toString();

    private HLSKeySet keySet;

    private List<HLSSegment> segmentUrls = new ArrayList<>();

    private MediaPlaylist playlist;

    private SimpleDoubleProperty progress = new SimpleDoubleProperty();

    private SimpleStringProperty status = new SimpleStringProperty("准备中");

    private static final Logger logger = LoggerFactory.getLogger(HLSDownloadTask.class);

    private volatile CountDownLatch latch;

    public HLSDownloadTask(String fileName, String hlsUrl, String outputPath) {
        this.fileName = fileName;
        this.hlsUrl = hlsUrl;
        this.outputDir = outputPath;
    }

    public SimpleDoubleProperty progressProperty() {
        return progress;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getStatus() {
        return status.get();
    }

    public void setStatus(String status) {
        this.status.set(status);
    }

    public SimpleStringProperty statusProperty() {
        return status;
    }

    public File getResult() {
        return new File(outputDir, fileName + ".mp4");
    }


    /**
     * 从给定的URL和媒体播放列表中提取分段信息。
     * @return 初始化完毕发返回 true
     */
    public boolean initPlayList() {
        String m3u8Content = IOUtils.loadFromURL(hlsUrl);
        if(m3u8Content == null || m3u8Content.isEmpty()) {
            return false;
        }
        try {

            MediaPlaylistParser parser = new MediaPlaylistParser();
            this.playlist = parser.readPlaylist(m3u8Content);
            this.segmentUrls = getSegmentsUrls(hlsUrl, playlist);

            segmentDir = new File(outputDir + "/" + segmentDirName);
            if(!segmentDir.exists()) {
                segmentDir.mkdir();
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从给定的URL下载分段并保存到指定的输出目录。
     */
    public void download() {

        latch = new CountDownLatch(segmentUrls.size());
        progress.set(0);
        for (int i = 0; i < segmentUrls.size(); i++) {

            HLSSegment segmentUrl = segmentUrls.get(i);
            Thread.ofVirtual().start(() -> {
                try {
                    downloadSegments(segmentUrl, segmentDir,0, 5);
                    progress.set((segmentUrls.size() - latch.getCount()) / (double) segmentUrls.size());
                    status.set("正在下载");
                } catch (Exception e) {
                    logger.error("Error downloading segment: " + e.getMessage());
                }
                latch.countDown();
            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error("Error waiting for download completion:", e);
        }
    }

    /**
     * 合并下载的分段文件。
     */
    public void merge() {

        progress.set(0);
        File targetFile = new File(outputDir, fileName + ".mp4");
        HLSMerger merger = new HLSMerger(playlist, segmentDir, targetFile);
        merger.setProgressListener((direction, merged, totals) -> {
            progress.set(merged / (double) totals);
            status.set("正在合并");
        });
        merger.merge();
        progress.set(1);

        status.set("清理中");
        File[] files = segmentDir.listFiles();
        if (files != null && files.length > 0) {
            for (File segment : files) {
                segment.delete();
            }
        }

        segmentDir.delete();
        if (merger.isFailed()) {
            targetFile.delete();
            status.set("下载失败");
        } else {
            status.set("完成");
        }

    }

    /**
     * 从给定的URL下载分段并保存到指定的输出目录。
     *
     * @param seg 分段文件的URL
     * @param outputDir 输出目录，用于保存下载的分段文件
     * @throws RuntimeException 如果无法创建输出目录时抛出
     */
    public void downloadSegments(HLSSegment seg, File outputDir, int retryCount, int maxRetry) {
        if (!outputDir.exists()) {
            if(!outputDir.mkdirs()) {
                throw new RuntimeException("Failed to create output directory");
            }
        }
        File target = null;
        FileOutputStream fos = null;
        try {
            String fileName = seg.getSegmentName();
            target = new File(outputDir,fileName);
            HttpURLConnection connection = seg.openConnection();
            if(connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

                Cipher cipher = null;
                if (seg.getKeySet() != null) {
                    cipher = seg.getKeySet().createCipher();
                }

                InputStream is = connection.getInputStream();
                fos = new FileOutputStream(target);

                byte[] buffer = new byte[1024 * 1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    if (cipher != null) {
                        byte[] decoded = cipher.update(buffer, 0, bytesRead);
                        fos.write(decoded);
                    } else {
                        fos.write(buffer, 0, bytesRead);
                    }
                }

                if (cipher != null) {
                    byte[] finalBytes = cipher.doFinal();
                    fos.write(finalBytes);
                }

                fos.close();
                is.close();
                logger.info("Downloaded: " + target.getAbsolutePath());
                fos = null;
                connection.disconnect();
            } else {
                connection.disconnect();
            }

        } catch (Exception e) {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {}
            }
            if (target != null && target.exists()) {
                target.delete();
            }
            if (retryCount < maxRetry) {
                downloadSegments(seg, outputDir, retryCount + 1, maxRetry);
            }
        }
    }

    /**
     * 根据给定的M3U8播放列表URL和播放列表对象，获取所有分段文件的URL列表。
     *
     * @param m3u8Url    M3U8播放列表的URL
     * @param playlist   包含媒体分段的播放列表对象
     * @return 包含所有分段文件URL的列表
     */
    public List<HLSSegment> getSegmentsUrls(String m3u8Url, MediaPlaylist playlist) {

        List<HLSSegment> segments = new ArrayList<>();

        String parent = m3u8Url.substring(0, m3u8Url.lastIndexOf('/'));
        for (MediaSegment segment : playlist.mediaSegments()) {
            try {

                String uri = segment.uri();
                String name = null;
                String segmentUrl = null;
                if (uri.contains("/")) {
                    name = segment.uri().substring(segment.uri().lastIndexOf('/') + 1);
                } else {
                    name = segment.uri();
                }

                segmentUrl = parent + "/" + name;

                String keyName = null;
                if (segment.segmentKey().isPresent()) {

                    // HLS协议是支持加密的，这里加载密钥，并生成密钥集。
                    // HLS目前支持一种AES-128加密方式，这里用的是AES/CBC/PKCS5Padding算法。
                    // 这里需要判断是否存在密钥的URL，如果有的话，就加载它，如果没有，就直接使用当前的密钥。
                    // 因为HLS有可能会在中途更换密钥，所以需要按照顺序读取和使用它们。

                    SegmentKey key = segment.segmentKey().get();
                    String segKeyUrl = key.uri().orElse("");
                    String segIv = key.iv().orElse("");

                    if (!segKeyUrl.isBlank()) {

                        if (segKeyUrl.contains("/")) {
                            keyName = segKeyUrl.substring(segKeyUrl.lastIndexOf('/') + 1);
                        } else {
                            keyName = segKeyUrl;
                        }

                        byte[] keyBytes = IOUtils.loadBytesFromURL(parent + "/" + keyName);
                        if (keyBytes != null) {
                            byte[] keyIv = HexFormat.of().parseHex(segIv.substring(2));
                            IvParameterSpec iv = new IvParameterSpec(keyIv);
                            SecretKeySpec spec = new SecretKeySpec(keyBytes, "AES");
                            keySet = new HLSKeySet(spec, iv);
                        }

                    }

                }

                HLSSegment segmentObj = new HLSSegment(name, new URI(segmentUrl).toURL());
                segmentObj.setKeySet(keySet);
                segments.add(segmentObj);

            } catch (Exception e) {
                logger.warn("failed to parse segment url", e);
            }
        }
        return segments;
    }



}
