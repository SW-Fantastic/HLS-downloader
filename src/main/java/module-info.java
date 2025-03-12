module org.swdc.hls {

    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;

    requires m3u8.parser;
    requires org.bytedeco.ffmpeg;
    requires swdc.application.fx;
    requires swdc.application.configs;
    requires swdc.application.dependency;
    requires jakarta.annotation;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.swing;
    requires jakarta.inject;
    requires swdc.commons;

    requires java.desktop;

    opens org.swdc.hls to
            javafx.graphics,
            swdc.application.fx,
            swdc.application.dependency,
            swdc.application.configs;

    opens org.swdc.hls.views to
            javafx.fxml,
            javafx.graphics,
            javafx.base,
            com.fasterxml.jackson.databind,
            swdc.commons,
            swdc.application.dependency,
            swdc.application.fx;

    opens org.swdc.hls.core to
            com.fasterxml.jackson.databind,
            javafx.base,
            javafx.fxml,
            javafx.graphics,
            swdc.application.dependency,
            swdc.application.fx,
            swdc.commons;

    opens views.main;

    opens icons;



}