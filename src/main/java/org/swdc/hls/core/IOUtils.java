package org.swdc.hls.core;

import org.bytedeco.ffmpeg.global.avutil;
import org.slf4j.Logger;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class IOUtils {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(IOUtils.class);

    public static byte[] loadBytesFromURL(String keyUrl) {
        try {
            byte[] bytes = null;
            URL keyUrlTarget = new URI(keyUrl).toURL();
            HttpURLConnection connection = (HttpURLConnection) keyUrlTarget.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.setDoOutput(false);
            connection.connect();
            if(connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream is = connection.getInputStream();
                bytes = is.readAllBytes();
                is.close();
            } else {
                return null;
            }

            connection.disconnect();
            return bytes;
        } catch (Exception e) {
            logger.error("failed to load key from url", e);
            return null;
        }
    }

    /**
     * 从给定的URL加载内容，并返回其内容作为字符串。
     *
     * @param m3u8Url 目标M3U8文件的URL
     * @return 从URL加载的内容作为字符串，如果加载失败或发生异常则返回null
     * @throws Exception 如果发生任何异常，将捕获并返回null
     */
    public static String loadFromURL(String m3u8Url) {
        byte[] bytes = loadBytesFromURL(m3u8Url);
        if (bytes != null) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return null;
    }

    /**
     * 将FFMpeg的异常转换为JavaException
     * @param errCode 异常编码
     * @return 运行时异常
     */
    public static String getException(int errCode) {
        byte[] err = new byte[128];
        avutil.av_make_error_string(err,err.length - 1,errCode);
        for (int idx = err.length - 1; idx > 0; idx --) {
            if (err[idx] != 0) {
                return new String(err,0,idx + 1);
            }
        }
        return new String(err, StandardCharsets.UTF_8);
    }

}
