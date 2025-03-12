package org.swdc.hls.views;

import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.stage.DirectoryChooser;
import org.swdc.fx.FXResources;
import org.swdc.fx.view.ViewController;
import org.swdc.hls.HlsConfigure;
import org.swdc.hls.core.HLSDownloadTask;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class HLSViewController extends ViewController<HLSMainView> {


    @Inject
    private HlsConfigure configure;

    @Inject
    private FXResources resources;

    @FXML
    private TextField txtUrl;

    @FXML
    private TextField txtOutput;

    @FXML
    private TextField txtFileName;

    @FXML
    private TableView<HLSDownloadTask> taskTableView;

    @FXML
    private TableColumn<HLSDownloadTask, String> taskNameColumn;

    @FXML
    private TableColumn<HLSDownloadTask, Void> taskStatusColumn;

    @FXML
    private TableColumn<HLSDownloadTask,String> taskControlColumn;

    @Override
    protected void viewReady(URL url, ResourceBundle resourceBundle) {

        HLSMainView view = getView();
        view.getView().setDisable(true);

        cleanUnFinishedTasks();

        view.getView().setDisable(false);

        File outFile = new File(configure.getOutputDir());
        if (!outFile.exists()) {
            configure.setOutputDir("");
            configure.save();
            txtOutput.setText("");
        } else {
            txtOutput.setText(configure.getOutputDir());
        }

        txtUrl.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                txtFileName.setText("");
            } else {
                if (newValue.contains("/") && newValue.contains(".m3u8")) {
                    String fileUrl = newValue.substring(0, newValue.lastIndexOf(".m3u8"));
                    txtFileName.setText(fileUrl.substring(fileUrl.lastIndexOf("/") + 1));
                }
            }
        });

        taskNameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        taskControlColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        taskStatusColumn.setCellFactory(p -> new ProgressCell());

        taskTableView.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                HLSDownloadTask task = taskTableView.getSelectionModel().getSelectedItem();
                if (task != null && task.progressProperty().getValue() == 1.0) {
                    try {
                        Desktop.getDesktop().open(task.getResult());
                    } catch (IOException ex) {
                        Alert alert = getView().alert("提示", "打开文件失败！", Alert.AlertType.ERROR);
                        alert.showAndWait();
                    }
                }
            }
        });

    }

    private void cleanUnFinishedTasks() {
        if (configure.getOutputDir().isBlank()) {
            return;
        }
        File dir = new File(configure.getOutputDir());
        if (!dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        if(files == null) {
            return;
        }
        for (File file : files) {
            if(!file.isDirectory()) {
                continue;
            }
            File[] chunks = file.listFiles();
            if (chunks == null) {
                continue;
            }
            for (File chunk : chunks) {
                chunk.delete();
            }
            file.delete();
        }
    }


    @FXML
    private void onBrowserOutput() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择输出目录");
        File file = chooser.showDialog(getView().getStage());
        if (file != null) {
            txtOutput.setText(file.getAbsolutePath());
            configure.setOutputDir(file.getAbsolutePath());
            configure.save();
        }
    }

    @FXML
    private void onDownload() {

        HLSMainView view = getView();

        if (txtUrl.getText().isEmpty()) {
            Alert alert = view.alert("提示", "请输入URL", Alert.AlertType.ERROR);
            alert.showAndWait();
            return;
        }

        if (txtOutput.getText().isEmpty()) {
            Alert alert = view.alert("提示", "请选择输出目录", Alert.AlertType.ERROR);
            alert.showAndWait();
            return;
        }

        String fileName = txtFileName.getText();
        String hlsUrl = txtUrl.getText();

        txtUrl.setText("");
        txtFileName.setText("");

        resources.getExecutor().submit(() -> {
            HLSDownloadTask task = new HLSDownloadTask(fileName, hlsUrl, txtOutput.getText());
            taskTableView.getItems().add(task);
            task.initPlayList();
            task.download();
            task.merge();
        });
    }



}
