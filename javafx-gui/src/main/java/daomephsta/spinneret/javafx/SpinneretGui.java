package daomephsta.spinneret.javafx;

import java.io.IOException;
import java.util.stream.Stream;

import daomephsta.spinneret.Spinneret;
import daomephsta.spinneret.SpinneretArguments;
import daomephsta.spinneret.SpinneretArguments.InvalidArgumentException;
import daomephsta.spinneret.javafx.WizardPager.WizardPage;
import daomephsta.spinneret.versioning.MinecraftVersion;
import daomephsta.spinneret.versioning.VersionExtension;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class SpinneretGui extends Application
{
    private static final String
        LOADING_PAGE = "loading",
        TEMPLATE_PAGE = "template",
        MOD_INFO_PAGE = "mod-info";
    private final SpinneretArguments spinneretArgs = new SpinneretArguments();
    private final WizardPager pager = new WizardPager(spinneretArgs,
        new WizardPage(LOADING_PAGE, true), new TemplateSelectionPage(), new WizardPage(MOD_INFO_PAGE));
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
        pager.showPage(LOADING_PAGE);
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

    private static class TemplateSelectionPage extends WizardPage
    {
        @FXML
        private ComboBox<String> template;
        @FXML
        private ComboBox<MinecraftVersion> minecraftVersion;
        @FXML
        private CheckBox releasesOnly;

        public TemplateSelectionPage()
        {
            super(TEMPLATE_PAGE);
        }

        @Override
        protected void setupContent()
        {
            template.getItems().addAll(Spinneret.configuration().getTemplateAliases());
            template.getSelectionModel().selectFirst();

            MinecraftVersion mc114 = Spinneret.minecraftVersions().get("1.14");
            ObservableList<MinecraftVersion> minecraftVersions = FXCollections.observableArrayList();
            for (MinecraftVersion version : Spinneret.minecraftVersions().getDescending())
                minecraftVersions.add(version);
            var filteredVersions = new FilteredList<>(minecraftVersions, v -> v.compareTo(mc114) >= 0);
            minecraftVersion.setItems(filteredVersions);

            releasesOnly.setOnAction(e ->
            {
                filteredVersions.setPredicate(releasesOnly.isSelected()
                    ? v -> v.compareTo(mc114) >= 0 && v.extension == VersionExtension.NONE
                    : v -> v.compareTo(mc114) >= 0);
                minecraftVersion.getSelectionModel().selectFirst();
            });
        }

        @Override
        public void apply(SpinneretArguments spinneretArgs) throws InvalidArgumentException
        {
            spinneretArgs.template(template.getSelectionModel().getSelectedItem());
            spinneretArgs.minecraftVersion(minecraftVersion.getSelectionModel().getSelectedItem());
            spinneretArgs.selectTemplateVariant((version, templateVariants) ->
            {
                var dialog = new Dialog<ButtonType>();
                dialog.setTitle("No matching template variant");
                dialog.setContentText("No template variant for " + version.raw + ". Attempt to use the latest template?");
                var yes = new ButtonType("Yes", ButtonData.LEFT);
                var exit = new ButtonType("Exit", ButtonData.RIGHT);
                dialog.getDialogPane().getButtonTypes().addAll(yes, exit);
                dialog.showAndWait();

                ButtonType result = dialog.getResult();
                if (result == yes)
                    return templateVariants.stream().reduce((a, b) -> a.isLater(b) ? a : b).get();
                else if (result == exit)
                {
                    // TODO stay on the same page instead of exiting
                    System.exit(0);
                    throw new IllegalStateException("Unreachable");
                }
                else
                    throw new IllegalStateException("Unexpected result " + result);
            });
        }
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
