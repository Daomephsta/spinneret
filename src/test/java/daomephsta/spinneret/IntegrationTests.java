package daomephsta.spinneret;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import daomephsta.spinneret.SpinneretArguments.InvalidArgumentException;

public class IntegrationTests
{
    @Test
    public void test()
    {
        try
        {
            var latest = Spinneret.minecraftVersions().getLatest();
            String modName = "Test Mod";
            String modId = ArgumentSuggestions.modId(modName);
            List<String> authors = List.of("Alice", "Bob");
            SpinneretArguments spinneretArgs = new SpinneretArguments()
                .template("spinneret-java")
                .minecraftVersion(latest.raw)
                .compatibleMinecraftVersions(latest.major + "." + latest.minor + ".x")
                .modName(modName)
                .modId(modId)
                .authors(authors);
            spinneretArgs
                .description("A mod for testing Spinneret")
                .rootPackageName(ArgumentSuggestions.rootPackageName(modId, authors))
                .folderName(ArgumentSuggestions.folderName(modName))
                .modVersion("0.0.1");
            Spinneret.spin(spinneretArgs.selectTemplateVariant(
                (mcVersion, templates) -> {throw new IllegalStateException("No matching template");}));
        }
        catch (IOException | InvalidArgumentException e)
        {
            e.printStackTrace();
        }
    }
}
