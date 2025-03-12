package org.swdc.hls.views;

import javafx.geometry.Pos;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.swdc.hls.core.HLSDownloadTask;

public class ProgressCell extends TableCell<HLSDownloadTask,Void> {

    private HBox root = new HBox();

    private ProgressBar progressBar = new ProgressBar();

    public ProgressCell() {
        HBox.setHgrow(progressBar, Priority.ALWAYS);
        root.setAlignment(Pos.CENTER);
        root.getChildren().add(progressBar);
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            return;
        }
        setGraphic(root);
        HLSDownloadTask task = getTableRow().getItem();
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(task.progressProperty());
        progressBar.prefWidthProperty().bind(getTableColumn().widthProperty().subtract(8));
    }

}
