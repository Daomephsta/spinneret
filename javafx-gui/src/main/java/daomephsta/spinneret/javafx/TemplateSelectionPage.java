package daomephsta.spinneret.javafx;

import daomephsta.spinneret.Spinneret;
import daomephsta.spinneret.SpinneretArguments;
import daomephsta.spinneret.SpinneretArguments.InvalidArgumentException;
import daomephsta.spinneret.javafx.WizardPager.WizardPage;
import daomephsta.spinneret.versioning.MinecraftVersion;
import daomephsta.spinneret.versioning.VersionExtension;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonBar.ButtonData;

class TemplateSelectionPage extends WizardPage
{
    @FXML
    private ComboBox<String> template;
    @FXML
    private ComboBox<MinecraftVersion> minecraftVersion;
    @FXML
    private CheckBox releasesOnly;

    public TemplateSelectionPage()
    {
        super("template");
    }

    @FXML
    private void initialize()
    {
        template.getItems().addAll(Spinneret.configuration().getTemplateAliases());
        template.getSelectionModel().selectFirst();

        MinecraftVersion mc114 = Spinneret.minecraftVersions().get("1.14");
        ObservableList<MinecraftVersion> minecraftVersions = FXCollections.observableArrayList();
        for (MinecraftVersion version : Spinneret.minecraftVersions().getDescending())
            minecraftVersions.add(version);
        var filteredVersions = new FilteredList<>(minecraftVersions, v -> v.compareTo(mc114) >= 0);
        minecraftVersion.setItems(filteredVersions);
        minecraftVersion.getSelectionModel().selectFirst();

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