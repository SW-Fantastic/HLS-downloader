package org.swdc.hls.views;

import jakarta.inject.Inject;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.slf4j.Logger;
import org.swdc.fx.view.Toast;
import org.swdc.fx.view.ViewController;
import org.swdc.ours.common.network.ApacheRequester;
import org.swdc.ours.common.network.Network;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class SourceController extends ViewController<SourceView> {

    @FXML
    private DatePicker dateSelector;

    @FXML
    private TableView<DengTaAPI.DengTaSource> tableView;

    @FXML
    private TableColumn<DengTaAPI.DengTaSource, String> colDesc;

    @FXML
    private TableColumn<DengTaAPI.DengTaSource, String> colUrl;

    @Inject
    private Logger logger;

    private DengTaAPI api;

    @Override
    protected void viewReady(URL url, ResourceBundle resourceBundle) {
        dateSelector.valueProperty().addListener(this::onDateChanged);
        colDesc.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        colUrl.setCellValueFactory(new PropertyValueFactory<>("sdPath"));
        dateSelector.setValue(LocalDate.now());

        ContextMenu menu = new ContextMenu();
        MenuItem item = new MenuItem("复制此链接...");
        item.setOnAction(e -> {

            SourceView view = getView();
            DengTaAPI.DengTaSource source = tableView.getSelectionModel().getSelectedItem();
            if (source == null) {
                Alert alert = view.alert("提示", "请选择一个课程", Alert.AlertType.WARNING);
                alert.showAndWait();
                return;
            }
            ClipboardContent content = new ClipboardContent();
            content.putString(source.getSdPath());
            Clipboard clipboard = Clipboard.getSystemClipboard();
            clipboard.setContent(content);

            Toast.showMessage("复制成功，请粘贴到“HLS地址”以下载该内容。");

        });

        menu.getItems().add(item);

        tableView.setContextMenu(menu);


    }

    private void onDateChanged(ObservableValue<? extends LocalDate> observable, LocalDate oldValue,LocalDate newValue) {
        if (newValue == null) {
            return;
        }
        if (api == null) {
            api = Network.create(
                    DengTaAPI.class,
                    "https://dkt.dtdjzx.gov.cn/",
                    map -> {},
                    new ApacheRequester()
            );
        }

        ObservableList<DengTaAPI.DengTaSource> sources = tableView.getItems();
        sources.clear();
        try {
            DengTaAPI.DengTaResponse response = api.getSources(
                    String.format("%04d%02d", newValue.getYear(), newValue.getMonthValue() - 1)
            );
            sources.setAll(response.getData());
        } catch (Exception e) {
            Alert alert = getView().alert("网络请求失败", "获取数据失败，请稍后再试", Alert.AlertType.ERROR);
            alert.showAndWait();
            logger.error("Api call failed", e);
        }

    }

}
