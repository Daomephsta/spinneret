package daomephsta.spinneret.javafx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import daomephsta.spinneret.FabricMeta;
import daomephsta.spinneret.SpinneretArguments;
import daomephsta.spinneret.SpinneretArguments.InvalidArgumentException;
import daomephsta.spinneret.javafx.WizardPager.WizardPage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;

class DependenciesPage extends WizardPage
{
    private final SpinneretArguments spinneretArgs;
    @FXML
    private ComboBox<String>
        yarnVersion,
        fabricLoaderVersion,
        fabricApiVersion;
    @FXML
    private CheckBox useFabricApi;

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
        setupVersionCombo(fabricApiVersion,
            FabricMeta.getFabricApiVersionsFor(spinneretArgs.minecraftVersion()));

        useFabricApi.setOnAction(e -> fabricApiVersion.setDisable(!useFabricApi.isSelected()));
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
            ? fabricApiVersion.getSelectionModel().getSelectedItem()
            : null);
        spinneretArgs.dependencies(dependencies);

        if (!problems.isEmpty())
            throw new InvalidArgumentException("Missing required information", problems);
    }
}