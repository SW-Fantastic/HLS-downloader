package org.swdc.hls;

import org.swdc.dependency.DependencyContext;
import org.swdc.fx.FXApplication;
import org.swdc.fx.SWFXApplication;
import org.swdc.hls.views.HLSMainView;

@SWFXApplication(
        assetsFolder = "assets",
        splash = SplashView.class,
        configs = {
                HlsConfigure.class
        },
        icons = {
                "icon-16.png",
                "icon-24.png",
                "icon-32.png",
                "icon-64.png",
                "icon-128.png",
                "icon-256.png",
                "icon-512.png"
        }
)
public class HLSApplication extends FXApplication {


    @Override
    public void onStarted(DependencyContext dependencyContext) {
        HLSMainView mainView = dependencyContext.getByClass(HLSMainView.class);
        mainView.show();
    }

}
