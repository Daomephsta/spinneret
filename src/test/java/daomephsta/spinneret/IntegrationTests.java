package daomephsta.spinneret;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import daomephsta.spinneret.SpinneretArguments.InvalidArgumentException;
import daomephsta.spinneret.versioning.MinecraftVersions;

public class IntegrationTests
{
    @Test
    public void test()
    {
        String testTemplate = System.getenv("SPINNERET_TEST_TEMPLATE");
        if (testTemplate == null)
            throw new AssertionError("Environment variable SPINNERET_TEST_TEMPLATE must be defined");
        try
        {
            var minecraftVersions = MinecraftVersions.load(
                Paths.get("minecraft_versions.json"),
                Spinneret.configuration().urls().minecraftVersions).join();
            var latest = minecraftVersions.getLatest();
            String modName = "Test Mod";
            String modId = ArgumentSuggestions.modId(modName);
            List<String> authors = List.of("Alice", "Bob");
            SpinneretArguments spinneretArgs = new SpinneretArguments()
                .template(testTemplate)
                .minecraftVersion(latest)
                .compatibleMinecraftVersions(latest.major + "." + latest.minor + ".x")
                .modName(modName)
                .modId(modId)
                .authors(authors)
                .description("A mod for testing Spinneret")
                .rootPackageName(ArgumentSuggestions.rootPackageName(modId, authors))
                .folderName(ArgumentSuggestions.folderName(modName))
                .modVersion("0.0.1")
                .dependencies(Map.of(
                    "mappings", "dummy",
                    "fabricLoader", "dummy",
                    "fabricApi", "dummy"));
            Spinneret.spin(spinneretArgs.selectTemplateVariant(minecraftVersions,
                (mcVersion, templates) -> {throw new IllegalStateException("No matching template");}));
        }
        catch (IOException | InvalidArgumentException e)
        {
            e.printStackTrace();
        }
    }
}
