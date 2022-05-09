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
        Spinneret spinneret = new Spinneret(Paths.get("."));
        String testTemplate = System.getenv("SPINNERET_TEST_TEMPLATE");
        if (testTemplate == null)
            throw new AssertionError("Environment variable SPINNERET_TEST_TEMPLATE must be defined");
        try
        {
            var minecraftVersions = MinecraftVersions.load(
                Paths.get("minecraft_versions.json"),
                spinneret.configuration.urls().minecraftVersions).join();
            var mc1_18_1 = minecraftVersions.get("1.18.1");
            String modName = "Test Mod";
            String modId = ArgumentSuggestions.modId(modName);
            List<String> authors = List.of("Alice", "Bob");
            SpinneretArguments spinneretArgs = spinneret.createArguments()
                .template(testTemplate)
                .minecraftVersion(mc1_18_1)
                .compatibleMinecraftVersions(mc1_18_1.major + "." + mc1_18_1.minor + ".x")
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
            spinneret.spin(spinneretArgs.selectTemplateVariant(minecraftVersions,
                (mcVersion, templates) -> {throw new IllegalStateException("No matching template");}));
        }
        catch (IOException | InvalidArgumentException e)
        {
            e.printStackTrace();
        }
    }
}
