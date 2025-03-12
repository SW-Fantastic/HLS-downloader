package org.swdc.hls;

import org.swdc.config.annotations.ConfigureSource;
import org.swdc.config.annotations.Property;
import org.swdc.config.configs.JsonConfigHandler;
import org.swdc.fx.config.ApplicationConfig;
import org.swdc.fx.config.PropEditor;
import org.swdc.fx.config.editors.FolderSelectEditor;

@ConfigureSource(value = "assets/config.json", handler = JsonConfigHandler.class)
public class HlsConfigure extends ApplicationConfig {

    @Property(value = "outputDir")
    @PropEditor(
            editor = FolderSelectEditor.class,
            description = "文件被存储在这里",
            name = "下载目录"
    )
    private String outputDir;

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }
}
