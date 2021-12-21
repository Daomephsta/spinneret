package daomephsta.spinneret;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.Test;

import daomephsta.spinneret.SpinneretArguments.InvalidArgumentException;
import daomephsta.spinneret.template.JsonFilter;
import daomephsta.spinneret.template.PascalCaseFilter;
import daomephsta.spinneret.template.Template;
import daomephsta.spinneret.versioning.MinecraftVersion;
import liqp.filters.Filter;

public class IntegrationTests
{
    @Test
    public void test()
    {
        try
        {
            SpinneretArguments spinneretArgs = new SpinneretArguments();
            spinneretArgs
                .template("spinneret-java")
                .minecraftVersion(Spinneret.minecraftVersions().getLatest().raw)
                .modName("Test Mod")
                .modId(spinneretArgs.suggestModId())
                .folderName(spinneretArgs.suggestFolderName())
                .modVersion("0.0.1");
            Template template = selectTemplate(
                spinneretArgs.template(), spinneretArgs.minecraftVersion());
            Filter.registerFilter(new JsonFilter());
            Filter.registerFilter(new PascalCaseFilter());
            template.generate(spinneretArgs);
        }
        catch (IOException | InvalidArgumentException e)
        {
            e.printStackTrace();
        }
    }

    private static Template selectTemplate(URL templateUrl, MinecraftVersion minecraftVersion)
    {
        List<Template> templates = SpinneretInteractiveCLI.readSelectors(templateUrl);
        for (var template : templates)
        {
            if (template.matches(minecraftVersion))
                return template;
        }
        throw new IllegalStateException("No matching template");
    }
}
