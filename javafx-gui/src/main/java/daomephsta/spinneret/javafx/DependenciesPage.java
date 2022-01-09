package daomephsta.spinneret.javafx;

import java.awt.Desktop;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import daomephsta.spinneret.FabricMeta;
import daomephsta.spinneret.FabricMeta.FabricApiVersionData;
import daomephsta.spinneret.Spinneret;
import daomephsta.spinneret.SpinneretArguments;
import daomephsta.spinneret.SpinneretArguments.InvalidArgumentException;
import daomephsta.spinneret.javafx.WizardPager.WizardPage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.util.StringConverter;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;

class DependenciesPage extends WizardPage
{
    private final SpinneretArguments spinneretArgs;
    @FXML
    private ComboBox<String>
        yarnVersion,
        fabricLoaderVersion;
    @FXML
    private ComboBox<FabricApiVersionData> fabricApiVersion;
    @FXML
    private CheckBox useFabricApi;
    @FXML
    private Hyperlink fabricApiCurseForgeLink;

    public DependenciesPage(SpinneretArguments spinneretArgs)
    {
        super("dependencies");
        this.spinneretArgs = spinneretArgs;
    }

    @Override
    protected void setupContent()
    {
        setupVersionCombo(yarnVersion,
            FabricMeta.getYarnVersionsFor(spinneretArgs.minecraftVersion()));
        setupVersionCombo(fabricLoaderVersion,
            FabricMeta.getFabricLoaderVersionsFor(spinneretArgs.minecraftVersion()));
        setupFabricApiVersion();

        useFabricApi.setOnAction(e -> fabricApiVersion.setDisable(!useFabricApi.isSelected()));

        fabricApiCurseForgeLink.setOnAction(e ->
        {
            try
            {
                Desktop.getDesktop().browse(Spinneret.configuration().urls().fabricApiCurseForge);
            }
            catch (IOException ex)
            {
                new Alert(AlertType.ERROR, ex.getMessage());
                ex.printStackTrace(System.err);
            }
        });
    }

    public CompletableFuture<Void> setupVersionCombo(
        ComboBox<String> versionCombo, CompletableFuture<Collection<String>> versionsFuture)
    {
        versionCombo.setPromptText("Loading...");
        return versionsFuture.thenAcceptAsync(versions ->
        {
            versionCombo.getItems().addAll(versions);
            versionCombo.getSelectionModel().selectFirst();
        }, Platform::runLater);
    }

    private void setupFabricApiVersion()
    {
        fabricApiVersion.setPromptText("Loading...");
        FabricMeta.getFabricApiVersions().thenAcceptAsync(versions ->
        {
            fabricApiVersion.getItems().addAll(versions);
            // Decorate versions matching the selected minecraft version with stars
            fabricApiVersion.setConverter(new StringConverter<FabricMeta.FabricApiVersionData>()
            {
                @Override
                public String toString(FabricApiVersionData version)
                {
                    var display = new StringBuilder(version.versionNumber());
                    if (version.gameVersions().contains(spinneretArgs.minecraftVersion().raw))
                        display.append(" \u2605");
                    return display.toString();
                }

                @Override
                public FabricApiVersionData fromString(String string)
                {
                    // fabricApiVersion isn't editable, so fromString can be unsupported
                    throw new UnsupportedOperationException("Unexpected fromString() call");
                }
            });
            // Find latest version matching the selected minecraft version
            versions.stream().filter(v -> v.gameVersions().contains(spinneretArgs.minecraftVersion().raw))
                .findFirst().ifPresent(fabricApiVersion.getSelectionModel()::select);
        }, Platform::runLater);
    }

    @Override
    public void apply(SpinneretArguments spinneretArgs) throws InvalidArgumentException
    {
        var problems = new ArrayList<String>();
        if (yarnVersion.getItems().isEmpty() || fabricLoaderVersion.getItems().isEmpty() || fabricApiVersion.getItems().isEmpty())
            problems.add("One or more version controls has not finished loading");

        var dependencies = new HashMap<String, String>();
        dependencies.put("mappings", yarnVersion.getSelectionModel().getSelectedItem());
        dependencies.put("fabricLoader", fabricLoaderVersion.getSelectionModel().getSelectedItem());
        dependencies.put("fabricApi", useFabricApi.isSelected()
            ? fabricApiVersion.getSelectionModel().getSelectedItem().versionNumber()
            : null);
        spinneretArgs.dependencies(dependencies);

        if (!problems.isEmpty())
            throw new InvalidArgumentException("Missing required information", problems);
    }
}