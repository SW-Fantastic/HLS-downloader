package org.swdc.hls.core.mpeg;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVIOContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;

import java.io.Closeable;
import java.io.File;

public class FFMpegWriter implements Closeable {

    private volatile boolean opend = false;

    private volatile boolean headerWritten = false;

    private AVFormatContext outputContext;

    private AVStream videoStream;

    private AVStream audioStream;

    private File targetFile;

    public FFMpegWriter(File targetFile) {
        this.targetFile = targetFile;
    }

    public boolean open() {
        outputContext = avformat.avformat_alloc_context();
        int state = avformat.avformat_alloc_output_context2(outputContext, null, null, targetFile.getAbsolutePath());
        if (state < 0) {
            close();
            return false;
        }
        if ((outputContext.flags() & avformat.AVFMT_NOFILE) == 0) {
            if (!targetFile.exists()) {
                try {
                    targetFile.createNewFile();
                } catch (Exception e) {
                    close();
                    return false;
                }
            }
            AVIOContext context = new AVIOContext();
            state = avformat.avio_open(context,targetFile.getAbsolutePath(),avformat.AVIO_FLAG_WRITE);
            if (state < 0) {
                close();
                return false;
            }
            outputContext.pb(context);
        }

        opend = true;

        return true;
    }


    /**
     * 从源AVFormatContext创建新的音频和视频流
     *
     * @param sourceContext 源AVFormatContext
     */
    public boolean fromSource(AVFormatContext sourceContext) {
        if(!opend) {
            // FFMPEG的输出尚未初始化，无法创建流。
            return false;
        }
        if (headerWritten) {
            return true;
        }
        for (int i = 0; i < sourceContext.nb_streams(); ++i) {
            AVStream stream = sourceContext.streams(i);
            if (stream.codecpar().codec_type() == avutil.AVMEDIA_TYPE_AUDIO || stream.codecpar().codec_type() == avutil.AVMEDIA_TYPE_VIDEO) {
                AVStream newStream = avformat.avformat_new_stream(outputContext, null);
                if (newStream == null) {
                    return false;
                }
                avcodec.avcodec_parameters_copy(newStream.codecpar(), stream.codecpar());
                // Codec tag不一致时，会导致写入文件失败，这里设置为0来解决这个问题。
                newStream.codecpar().codec_tag(0);
                if (stream.codecpar().codec_type() == avutil.AVMEDIA_TYPE_AUDIO) {
                    audioStream = newStream;
                }
                if (stream.codecpar().codec_type() == avutil.AVMEDIA_TYPE_VIDEO) {
                    videoStream = newStream;
                }
            }
        }

        int state = avformat.avformat_write_header(outputContext, (AVDictionary) null);
        headerWritten = state >= 0;

        return headerWritten;
    }


    /**
     * 将数据包写入目标流
     *
     * @param packet 要写入的数据包
     * @param sourceContext 源AVFormatContext
     */
    public boolean writeFrame(AVPacket packet, AVFormatContext sourceContext) {

        AVStream targetStream = null;

        AVStream sourceStream = sourceContext.streams(packet.stream_index());
        if (sourceStream.codecpar().codec_type() == avutil.AVMEDIA_TYPE_AUDIO && audioStream != null) {
            targetStream = audioStream;
        } else if (sourceStream.codecpar().codec_type() == avutil.AVMEDIA_TYPE_VIDEO && videoStream != null) {
            targetStream = videoStream;
        } else {
            return true;
        }

        packet.stream_index(targetStream.index());
        avcodec.av_packet_rescale_ts(packet, sourceStream.time_base(), targetStream.time_base());
        int state = avformat.av_write_frame(outputContext, packet);
        return state >= 0;

    }

    public File getTargetFile() {
        return targetFile;
    }

    @Override
    public void close()  {

        if (audioStream != null) {
            audioStream = null;
        }

        if (videoStream != null) {
            videoStream = null;
        }

        if (outputContext != null && !outputContext.isNull()) {
            if (opend) {
                avformat.av_write_trailer(outputContext);
            }
            if (outputContext.pb() != null && !outputContext.pb().isNull()) {
                avformat.avio_close(outputContext.pb());
            }
            opend = false;
            outputContext = null;
        }

        headerWritten = false;

    }
}
