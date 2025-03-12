package org.swdc.hls.core.mpeg;

import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.model.MediaSegment;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avdevice;
import org.bytedeco.ffmpeg.global.avformat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swdc.ours.common.helper.ProgressDirection;
import org.swdc.ours.common.helper.ProgressListener;

import java.io.File;
import java.util.Iterator;
import java.util.List;

public class HLSMerger {

    private MediaPlaylist playlist;

    private File outputDir;

    private AVFormatContext sourceContext;

    private FFMpegWriter writer;

    private ProgressListener progressListener;

    private static Logger logger = LoggerFactory.getLogger(HLSMerger.class);

    public HLSMerger(MediaPlaylist playlist, File outputDir, File targetFile) {
        this.playlist = playlist;
        this.outputDir = outputDir;
        this.writer = new FFMpegWriter(targetFile);
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public void merge() {

        // 获取播放列表中的所有媒体片段
        List<MediaSegment> segments = playlist.mediaSegments();
        // 如果播放列表为空，则直接返回
        if (segments.isEmpty()) {
            return;
        }

        // 获取媒体片段的迭代器
        Iterator<MediaSegment> iterator = playlist.mediaSegments().iterator();

        // 获取第一个媒体片段
        MediaSegment currentSeg = iterator.next();
        // 创建当前媒体片段对应的文件对象
        File currentFile = null;
        if (currentSeg.uri().contains("/")) {
            currentFile = new File(outputDir, currentSeg.uri().substring(
                    currentSeg.uri().lastIndexOf('/') + 1)
            );
        } else {
           currentFile = new File(outputDir, currentSeg.uri());
        }
        // 如果当前文件不存在，则直接返回
        if (!currentFile.exists()) {
            return;
        }

        // 注册所有设备
        avdevice.avdevice_register_all();
        // 分配一个新的AVFormatContext
        sourceContext = avformat.avformat_alloc_context();

        // 打开输入媒体文件
        int state = avformat.avformat_open_input(sourceContext, currentFile.getAbsolutePath(), null, null);
        // 如果打开失败，则关闭资源并返回
        if (state < 0) {
            close();
            return;
        }

        // 查找流信息
        state = avformat.avformat_find_stream_info(sourceContext, (AVDictionary) null);
        // 如果查找失败，则关闭资源并返回
        if (state < 0) {
            close();
            return;
        }

        // 打开写入器
        if (!writer.open()) {
            close();
            throw new RuntimeException("Open writer failed.");
        }

        // 从源AVFormatContext初始化写入器
        if(!writer.fromSource(sourceContext)) {
            close();
            throw new RuntimeException("Init stream failed.");
        }

        long mergedFiles = 0;
        long totalFiles = segments.size();

        // 分配一个新的AVPacket
        AVPacket packet = avcodec.av_packet_alloc();
        do {

            // 关闭当前输入媒体文件
            avformat.avformat_close_input(sourceContext);

            // 打印当前正在合并的文件路径
            logger.info("Merging:  " + currentFile.getAbsolutePath());

            if (currentFile.exists()) {

                // 重新打开当前输入媒体文件
                state = avformat.avformat_open_input(sourceContext, currentFile.getAbsolutePath(), null, null);
                // 如果打开失败，则关闭资源并返回
                if (state < 0) {
                    close();
                    return;
                }

                // 重新查找流信息
                state = avformat.avformat_find_stream_info(sourceContext, (AVDictionary) null);
                // 如果查找失败，则关闭资源并返回
                if (state < 0) {
                    close();
                    return;
                }

                while (true) {

                    // 读取下一帧数据
                    state = avformat.av_read_frame(sourceContext, packet);
                    // 如果没有更多帧数据，则跳出循环
                    if (state < 0) {
                        break;
                    }

                    // 将帧数据写入目标文件
                    if(!writer.writeFrame(packet, sourceContext)) {
                        close();
                        return;
                    }

                    // 释放AVPacket资源
                    avcodec.av_packet_unref(packet);

                }

            }

            mergedFiles ++;
            if (progressListener != null) {
                progressListener.onProgress(ProgressDirection.WRITE, mergedFiles, totalFiles);
            }

            if(!iterator.hasNext()) {
                // 没有更多片段了。
                break;
            }
            // 获取下一个媒体片段
            currentSeg = iterator.next();
            // 创建下一个媒体片段对应的文件对象
            if (currentSeg.uri().contains("/")) {
                currentFile = new File(outputDir, currentSeg.uri().substring(
                        currentSeg.uri().lastIndexOf('/') + 1)
                );
            } else {
                currentFile = new File(outputDir, currentSeg.uri());
            }

        } while (true);

        // 关闭资源
        close();
        // 打印合并后的文件路径
        logger.info("Merged:  " + writer.getTargetFile().getAbsolutePath());
    }


    private void close() {

        if (writer != null) {
            writer.close();
        }
        if (sourceContext != null && !sourceContext.isNull()) {
            avformat.avformat_close_input(sourceContext);
            avformat.avformat_free_context(sourceContext);
            sourceContext = null;
        }

    }

}
