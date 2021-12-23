package daomephsta.spinneret;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import daomephsta.spinneret.SpinneretArguments.InvalidArgumentException;

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
                .addAuthor("Alice").addAuthor("Bob")
                .description("A mod for testing Spinneret")
                .rootPackageName(spinneretArgs.suggestRootPackageName())
                .folderName(spinneretArgs.suggestFolderName())
                .modVersion("0.0.1");
            Spinneret.spin(spinneretArgs.selectTemplate(
                (mcVersion, templates) -> {throw new IllegalStateException("No matching template");}));
        }
        catch (IOException | InvalidArgumentException e)
        {
            e.printStackTrace();
        }
    }
}
