package daomephsta.spinneret.javafx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import daomephsta.spinneret.ArgumentSuggestions;
import daomephsta.spinneret.SpinneretArguments;
import daomephsta.spinneret.SpinneretArguments.InvalidArgumentException;
import daomephsta.spinneret.javafx.WizardPager.WizardPage;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ModInfoPage extends WizardPage
{
    @FXML
    private TextField
        modName,
        modId;
    @FXML
    private TextArea description;
    @FXML
    private TextField
        authors,
        rootPackage,
        folderName,
        modVersion;

    public ModInfoPage()
    {
        super("mod-info");
    }

    @FXML
    private void initialize()
    {
        SuggestionSupport.enhance(modId, () -> ArgumentSuggestions.modId(modName.getText()), modName);
        SuggestionSupport.enhance(rootPackage, () -> ArgumentSuggestions.rootPackageName(modId.getText(), getAuthors()), modId, authors);
        SuggestionSupport.enhance(folderName, () -> ArgumentSuggestions.folderName(modName.getText()), modName);
        modVersion.setText("0.0.1");
    }

    private static class SuggestionSupport implements ChangeListener<String>
    {
        private TextField target;
        private final Supplier<String> suggester;
        private final TextField[] dependencies;
        private String lastSuggestion = null;

        private SuggestionSupport(TextField target, Supplier<String> suggester, TextField[] dependencies)
        {
            this.target = target;
            this.suggester = suggester;
            this.dependencies = dependencies;
        }

        public static void enhance(TextField target, Supplier<String> suggester, TextField... dependencies)
        {
            var suggestionSupport = new SuggestionSupport(target, suggester, dependencies);
            for (TextField dependency : dependencies)
                dependency.textProperty().addListener(suggestionSupport);
        }

        @Override
        public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
        {
            if (!target.getText().isBlank() && !target.getText().equals(lastSuggestion))
                return;
            for (TextField dependency : dependencies)
            {
                if (dependency.getText().isBlank())
                    return;
            }
            target.setText(lastSuggestion = suggester.get());
        }
    }

    @Override
    void apply(SpinneretArguments spinneretArgs) throws InvalidArgumentException
    {
        List<String> problems = new ArrayList<>();
        if (modName.getText().isBlank())
            problems.add(I18n.get("infoPage.missingModName"));
        if (modId.getText().isBlank())
            problems.add(I18n.get("infoPage.missingModId"));
        if (description.getText().isBlank())
            problems.add(I18n.get("infoPage.missingDescription"));
        if (authors.getText().isBlank())
            problems.add(I18n.get("infoPage.missingAuthors"));
        if (rootPackage.getText().isBlank())
            problems.add(I18n.get("infoPage.missingRootPackage"));
        if (folderName.getText().isBlank())
            problems.add(I18n.get("infoPage.missingFolderName"));
        if (modVersion.getText().isBlank())
            problems.add(I18n.get("infoPage.missingModVersion"));
        if (!problems.isEmpty())
            throw new InvalidArgumentException(I18n.get("error.missingInformation"), problems);
        spinneretArgs.modName(modName.getText())
            .modId(modId.getText())
            .description(description.getText())
            .authors(getAuthors());
        spinneretArgs.rootPackageName(rootPackage.getText())
            .folderName(folderName.getText())
            .modVersion(modVersion.getText());
        var minecraftVersion = spinneretArgs.minecraftVersion();
        spinneretArgs.compatibleMinecraftVersions(minecraftVersion.major + "." + minecraftVersion.minor + ".x");
    }

    private List<String> getAuthors()
    {
        return Arrays.stream(authors.getText().split(","))
            .map(String::strip)
            .toList();
    }
}