package daomephsta.spinneret.javafx;

import java.nio.file.Paths;
import java.util.ArrayList;

import daomephsta.spinneret.Spinneret;
import daomephsta.spinneret.SpinneretArguments;
import daomephsta.spinneret.SpinneretArguments.InvalidArgumentException;
import daomephsta.spinneret.javafx.WizardPager.WizardPage;
import daomephsta.spinneret.versioning.MinecraftVersion;
import daomephsta.spinneret.versioning.MinecraftVersions;
import daomephsta.spinneret.versioning.VersionExtension;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;

class TemplateSelectionPage extends WizardPage
{
    @FXML
    private ComboBox<String> template;
    @FXML
    private ComboBox<MinecraftVersion> minecraftVersion;
    @FXML
    private CheckBox releasesOnly;
    private MinecraftVersions minecraftVersions = null;

    public TemplateSelectionPage()
    {
        super("template");
    }

    @FXML
    private void initialize()
    {
        template.getItems().addAll(Spinneret.configuration().getTemplateAliases());
        template.getSelectionModel().selectFirst();

        minecraftVersion.setPromptText(I18n.get("wizard.loading"));
        MinecraftVersions.load(Paths.get("minecraft_versions.json"),
                Spinneret.configuration().urls().minecraftVersions)
            .thenAcceptAsync(minecraftVersions ->
            {
                MinecraftVersion mc114 = minecraftVersions.get("1.14");
                ObservableList<MinecraftVersion> allVersions = FXCollections.observableArrayList();
                for (MinecraftVersion version : minecraftVersions.getDescending())
                    allVersions.add(version);
                var filteredVersions = new FilteredList<>(allVersions, v -> v.compareTo(mc114) >= 0);
                minecraftVersion.setItems(filteredVersions);
                minecraftVersion.getSelectionModel().selectFirst();

                releasesOnly.setOnAction(e ->
                {
                    filteredVersions.setPredicate(releasesOnly.isSelected()
                        ? v -> v.compareTo(mc114) >= 0 && v.extension == VersionExtension.NONE
                        : v -> v.compareTo(mc114) >= 0);
                    minecraftVersion.getSelectionModel().selectFirst();
                });
                this.minecraftVersions = minecraftVersions;
            }, Platform::runLater);
    }

    @Override
    public void apply(SpinneretArguments spinneretArgs) throws InvalidArgumentException
    {
        var problems = new ArrayList<String>();
        if (minecraftVersions == null || minecraftVersion.getItems().isEmpty())
            problems.add(I18n.get("template_page.waitForMcVersions"));
        if (!problems.isEmpty())
            throw new InvalidArgumentException(I18n.get("error.missingInformation"), problems);

        spinneretArgs.template(template.getSelectionModel().getSelectedItem());
        spinneretArgs.minecraftVersion(minecraftVersion.getSelectionModel().getSelectedItem());
        spinneretArgs.selectTemplateVariant(minecraftVersions, (version, templateVariants) ->
        {
            var yes = new ButtonType(I18n.get("dialog.yes"), ButtonData.LEFT);
            var exit = new ButtonType(I18n.get("dialog.exit"), ButtonData.RIGHT);
            var alert = new Alert(AlertType.CONFIRMATION,
                I18n.get("templatePage.noMatch", version.raw), yes, exit);
            alert.showAndWait();

            ButtonType result = alert.getResult();
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