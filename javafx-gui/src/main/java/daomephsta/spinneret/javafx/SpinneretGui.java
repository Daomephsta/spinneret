package daomephsta.spinneret.javafx;

import java.io.IOException;
import java.util.stream.Stream;

import daomephsta.spinneret.Spinneret;
import daomephsta.spinneret.SpinneretArguments;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class SpinneretGui extends Application
{
    private final SpinneretArguments spinneretArgs;
    private final WizardPager pager;
    @FXML
    private Button back, next;

    public SpinneretGui()
    {
        this.spinneretArgs = new SpinneretArguments();
        this.pager = new WizardPager(spinneretArgs,
            new TemplateSelectionPage(), new ModInfoPage(), new DependenciesPage(spinneretArgs));
    }

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        primaryStage.setTitle(I18n.get("app.name"));
        Stream.of("16", "32", "48", "256")
            .map(size -> new Image(getClass().getResourceAsStream("/image/icon-" + size + ".png")))
            .forEach(primaryStage.getIcons()::add);

        primaryStage.setScene(createScene());
        updatePageControls();
        primaryStage.show();
    }

    private Scene createScene() throws IOException
    {
        var fxml = new FXMLLoader();
        fxml.setController(this);
        I18n.configureFxml(fxml);
        BorderPane root = fxml.load(getClass().getResourceAsStream("/layout/wizard-frame.fxml"));
        root.setCenter(pager.getContent());
        return new Scene(root);
    }

    @FXML
    private void handlePageControls(Event event)
    {
        if (event.getSource() == next)
            pager.next();
        else if (event.getSource() == back)
            pager.previous();
        updatePageControls();
    }

    private void updatePageControls()
    {
        back.setDisable(!pager.hasPrevious());
        if (!pager.hasNext())
        {
            next.setText(I18n.get("wizard.finish"));
            next.setOnAction(this::finish);
        }
        else
        {
            next.setText(I18n.get("wizard.next"));
            next.setOnAction(this::handlePageControls);
        }
    }

    private void finish(ActionEvent event)
    {
        if (!pager.applyPage())
            return;
        try
        {
            Spinneret.spin(spinneretArgs);
        }
        catch (IOException e)
        {
            new Alert(AlertType.ERROR, "Template generation failed: " + e.getMessage());
            return;
        }
        Platform.exit();
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
