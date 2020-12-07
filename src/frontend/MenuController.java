package frontend;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;

public abstract class MenuController {
    @Getter
    @Setter
    private Stage stage;
    @Setter
    private Scene previousScene;

    @FXML
    public void backButtonAction(ActionEvent actionEvent) throws Exception{
        stage.setScene(previousScene);
    }
}
