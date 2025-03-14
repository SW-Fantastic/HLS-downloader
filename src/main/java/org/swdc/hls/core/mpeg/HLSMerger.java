package org.swdc.hls.core.mpeg;

import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.model.MediaSegment;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
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

    public boolean findParameterForWrite() {

        File currentFile = null;
        for (MediaSegment currentSeg : playlist.mediaSegments()) {
            if (currentSeg.uri().contains("/")) {
                currentFile = new File(outputDir, currentSeg.uri().substring(
                        currentSeg.uri().lastIndexOf('/') + 1)
                );
            } else {
                currentFile = new File(outputDir, currentSeg.uri());
            }

            // 如果当前文件不存在，则直接返回
            if (!currentFile.exists()) {
                return false;
            }

            int state = avformat.avformat_open_input(sourceContext, currentFile.getAbsolutePath(), null, null);
            // 如果打开失败，则关闭资源并返回
            if (state < 0) {
                writer.abort();
                return false;
            }

            // 查找流信息
            state = avformat.avformat_find_stream_info(sourceContext, (AVDictionary) null);
            // 如果查找失败，则关闭资源并返回
            if (state < 0) {
                writer.abort();
                close();
                return false;
            }

            boolean found = false;
            // 从源AVFormatContext初始化写入器
            if(writer.fromSource(sourceContext)) {
                found = true;
            }
            // 关闭当前输入媒体文件
            avformat.avformat_close_input(sourceContext);
            if (found) {
                return true;
            }
        }

        return false;

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

        // 注册所有设备
        avdevice.avdevice_register_all();
        // 分配一个新的AVFormatContext
        sourceContext = avformat.avformat_alloc_context();

        // 打开写入器
        if (!writer.open()) {
            writer.abort();
            close();
            return;
        }

        if (!findParameterForWrite()) {
            writer.abort();
            close();
            return;
        }

        long mergedFiles = 0;
        long totalFiles = segments.size();

        // 分配一个新的AVPacket
        AVPacket packet = avcodec.av_packet_alloc();
        MediaSegment currentSeg = null;
        File currentFile = null;
        int state = 0;

        while(iterator.hasNext()) {

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

            // 打印当前正在合并的文件路径
            logger.info("Merging:  " + currentFile.getAbsolutePath());

            if (currentFile.exists()) {

                // 打开当前输入媒体文件
                state = avformat.avformat_open_input(sourceContext, currentFile.getAbsolutePath(), null, null);
                // 如果打开失败，则关闭资源并返回
                if (state < 0) {
                    close();
                    return;
                }

                // 查找流信息
                state = avformat.avformat_find_stream_info(sourceContext, (AVDictionary) null);
                // 如果查找失败，则关闭资源并返回
                if (state < 0) {
                    writer.abort();
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

            // 关闭当前输入媒体文件
            avformat.avformat_close_input(sourceContext);

        }

        // 关闭资源
        close();
        // 打印合并后的文件路径
        logger.info("Merged:  " + writer.getTargetFile().getAbsolutePath());
    }

    public boolean isFailed() {
        return writer.isFailed();
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
