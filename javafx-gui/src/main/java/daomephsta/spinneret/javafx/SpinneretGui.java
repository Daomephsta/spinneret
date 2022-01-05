package daomephsta.spinneret.javafx;

import java.io.IOException;
import java.util.stream.Stream;

import daomephsta.spinneret.SpinneretArguments;
import daomephsta.spinneret.javafx.WizardPager.WizardPage;
import javafx.application.Application;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class SpinneretGui extends Application
{
    private static final String MOD_INFO_PAGE = "mod-info";
    private final SpinneretArguments spinneretArgs = new SpinneretArguments();
    private final WizardPager pager = new WizardPager(spinneretArgs,
        new TemplateSelectionPage(), new WizardPage(MOD_INFO_PAGE));
    @FXML
    private Button back, next;

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        primaryStage.setTitle("Spinneret");
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
        next.setDisable(!pager.hasNext());
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
