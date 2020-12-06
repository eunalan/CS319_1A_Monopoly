package frontend;

import board.*;
import card.LandTitleDeedCard;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;

public class DynamicBoardController {
    @FXML
    private AnchorPane dynamicBoard;
    @FXML
    private Pane bottomBoard, rightBoard, leftBoard, topBoard, bottomLeftBoard, bottomRightBoard, topLeftBoard, topRightBoard;

    private ImageView[] tokenImages;
    //public void setDynamicBoard(Space[] spaces, String[] colors) {

    public void initialize() {
        tokenImages = new ImageView[4];
    }

    public void setDynamicBoard(Board gameBoard) {

        // Need to reverse the non-corner spaces on the left and bottom parts of the map,
        // because the index 0 corresponds to the lower right space of the map
        // and the insertion to GUI elements happen from left to right or top to bottom
        // so the ordering of the spaces will be wrong

        Space[] spaces = gameBoard.getSpaces();
        String[] colors = gameBoard.getPropertyGroupColors();

        Space[] newSpaces = new Space[40];
        newSpaces[0] = spaces[0];
        for (int i = 0; i < 9; i++) {
            newSpaces[1+i] = spaces[9-i];
        }
        newSpaces[10] = spaces[10];
        for (int i = 0; i < 9; i++) {
            newSpaces[11+i] = spaces[19-i];
        }
        for (int i = 20; i < 40; i++) {
            newSpaces[i] = spaces[i];
        }
        spaces = newSpaces;

        for (int i = 0; i < 40; i++) {
            BorderPane spacePane = new BorderPane();

            Label propertyName = new Label(spaces[i].getName());
            propertyName.setWrapText(true);
            propertyName.textAlignmentProperty().setValue(TextAlignment.CENTER);

            VBox vb = new VBox();
            vb.setAlignment(Pos.CENTER);
            spacePane.setCenter(vb);

            if (spaces[i] instanceof PropertySpace) {
                PropertySpace currentSpace = (PropertySpace) spaces[i];
                Label price = new Label("M"+Integer.toString(currentSpace.getAssociatedProperty().getValue()));

                vb.getChildren().addAll(propertyName, price);
                //VBox vb = new VBox(propertyName, price);
                //vb.setAlignment(Pos.CENTER);
                //spacePane.setCenter(vb);

                if (currentSpace.getType() == PropertySpace.PropertyType.LAND) {
                    Pane colorPane = new Pane();
                    // Get the color specified for this property group, read from the json file
                    colorPane.setStyle("-fx-background-color: #"
                            + colors[((LandTitleDeedCard) (currentSpace.getAssociatedProperty().getCard())).getPropertyGroup()]);
                    if (i < 10) {
                        colorPane.getStyleClass().add("colortop");
                        spacePane.setTop(colorPane);
                    }
                    else if (i < 21) {
                        colorPane.getStyleClass().add("colorside");
                        spacePane.setRight(colorPane);
                    }
                    else if (i < 30) {
                        colorPane.getStyleClass().add("colortop");
                        spacePane.setBottom(colorPane);
                    }
                    else {
                        colorPane.getStyleClass().add("colorside");
                        spacePane.setLeft(colorPane);
                    }
                }
            }
            else {
                //spacePane.setCenter(propertyName);
                vb.getChildren().add(propertyName);
            }
            HBox tokenBox = new HBox();
            tokenBox.setId("tokenBox");
            vb.getChildren().add(tokenBox);

            if (i % 10 == 0) {
                spacePane.getStyleClass().add("corner");
            }

            if (i == 0) {
                bottomRightBoard.getChildren().add(spacePane);
            }
            else if (i < 10) {
                spacePane.getStyleClass().add("vspace");
                bottomBoard.getChildren().add(spacePane);
            }
            else if (i == 10) {
                bottomLeftBoard.getChildren().add(spacePane);
            }
            else if (i < 20) {
                spacePane.getStyleClass().add("hspace");
                leftBoard.getChildren().add(spacePane);
            }
            else if (i == 20) {
                topLeftBoard.getChildren().add(spacePane);
            }
            else if (i < 30) {
                spacePane.getStyleClass().add("vspace");
                topBoard.getChildren().add(spacePane);
            }
            else if (i == 30) {
                topRightBoard.getChildren().add(spacePane);
            }
            else {
                spacePane.getStyleClass().add("hspace");
                rightBoard.getChildren().add(spacePane);
            }

        }

    }

    public void setTokenImage(int playerNo, String tokenName) {
        tokenName = tokenName.toLowerCase().replaceAll(" ","");
        Image token = new Image("img/token/cropped/" + tokenName + ".png");
        ImageView iv = new ImageView(token);
        iv.setFitHeight(30);
        iv.setFitWidth(30);

        tokenImages[playerNo] = iv;
    }

    public void drawToken(int playerNo, int oldIndex, int newIndex) {
        ImageView iv = tokenImages[playerNo];

        HBox tokenBox = null;
        if (oldIndex == -1) {
            tokenBox = (HBox) ((BorderPane) bottomRightBoard.getChildren().get(0)).lookup("#tokenBox");
        }
        else {
            //remove old token
            if (oldIndex == 0) {
                //((VBox)((BorderPane) bottomBoard.getChildren().get(8-index)).getCenter()).getChildren();
                tokenBox = (HBox) ((BorderPane) bottomRightBoard.getChildren().get(0)).lookup("#tokenBox");
            }
            else if (oldIndex < 10) {
                tokenBox = (HBox) ((BorderPane) bottomBoard.getChildren().get(9-oldIndex)).lookup("#tokenBox");
            }
            else if (oldIndex == 10) {
                tokenBox = (HBox) ((BorderPane) bottomLeftBoard.getChildren().get(0)).lookup("#tokenBox");
            }
            else if (oldIndex < 20) {
                tokenBox = (HBox) ((BorderPane) leftBoard.getChildren().get(19-oldIndex)).lookup("#tokenBox");
            }
            else if (oldIndex == 20) {
                tokenBox = (HBox) ((BorderPane) topLeftBoard.getChildren().get(0)).lookup("#tokenBox");
            }
            else if (oldIndex < 30) {
                tokenBox = (HBox) ((BorderPane) topBoard.getChildren().get(oldIndex-21)).lookup("#tokenBox");
            }
            else if (oldIndex == 30) {
                tokenBox = (HBox) ((BorderPane) topRightBoard.getChildren().get(0)).lookup("#tokenBox");
            }
            // index < 39
            else {
                tokenBox = (HBox) ((BorderPane) rightBoard.getChildren().get(oldIndex-31)).lookup("#tokenBox");
            }
            tokenBox.getChildren().remove(iv);

            if (newIndex == 0) {
                //((VBox)((BorderPane) bottomBoard.getChildren().get(8-index)).getCenter()).getChildren();
                tokenBox = (HBox) ((BorderPane) bottomRightBoard.getChildren().get(0)).lookup("#tokenBox");
            }
            else if (newIndex < 10) {
                tokenBox = (HBox) ((BorderPane) bottomBoard.getChildren().get(9-newIndex)).lookup("#tokenBox");
            }
            else if (newIndex == 10) {
                tokenBox = (HBox) ((BorderPane) bottomLeftBoard.getChildren().get(0)).lookup("#tokenBox");
            }
            else if (newIndex < 20) {
                tokenBox = (HBox) ((BorderPane) leftBoard.getChildren().get(19-newIndex)).lookup("#tokenBox");
            }
            else if (newIndex == 20) {
                tokenBox = (HBox) ((BorderPane) topLeftBoard.getChildren().get(0)).lookup("#tokenBox");
            }
            else if (newIndex < 30) {
                tokenBox = (HBox) ((BorderPane) topBoard.getChildren().get(newIndex-21)).lookup("#tokenBox");
            }
            else if (newIndex == 30) {
                tokenBox = (HBox) ((BorderPane) topRightBoard.getChildren().get(0)).lookup("#tokenBox");
            }
            // index < 39
            else {
                tokenBox = (HBox) ((BorderPane) rightBoard.getChildren().get(newIndex-31)).lookup("#tokenBox");
            }

        }


        tokenBox.getChildren().add(iv);
    }
}