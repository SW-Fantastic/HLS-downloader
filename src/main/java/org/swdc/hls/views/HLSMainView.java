package org.swdc.hls.views;

import jakarta.annotation.PostConstruct;
import org.swdc.fx.view.AbstractView;
import org.swdc.fx.view.View;

@View(
        viewLocation = "views/main/HLSMainView.fxml",
        title = "HLS下载工具",
        resizeable = false
)
public class HLSMainView extends AbstractView {

    @PostConstruct
    public void init() {
    }

}
