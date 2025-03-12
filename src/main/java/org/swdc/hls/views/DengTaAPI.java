package org.swdc.hls.views;

import org.swdc.ours.common.network.EndPoint;
import org.swdc.ours.common.network.Methods;
import org.swdc.ours.common.network.Path;

import java.util.List;

public interface DengTaAPI {

    class DengTaSource {

        private String courseName;
        private String sdPath;

        public String getCourseName() {
            return courseName;
        }

        public void setCourseName(String courseName) {
            this.courseName = courseName;
        }

        public String getSdPath() {
            return sdPath;
        }

        public void setSdPath(String sdPath) {
            this.sdPath = sdPath;
        }
    }

     class DengTaResponse {
        private List<DengTaSource> data;

        public List<DengTaSource> getData() {
            return data;
        }

        public void setData(List<DengTaSource> data) {
            this.data = data;
        }
    }

    @EndPoint(url = "/course/findByPublishMonth?publishMonth={yearAndMonth}", method = Methods.POST)
    DengTaResponse getSources(@Path("yearAndMonth") String yearAndMonth);

}
