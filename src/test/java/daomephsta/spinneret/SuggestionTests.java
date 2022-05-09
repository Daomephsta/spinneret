package daomephsta.spinneret;

import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ArgumentConverter;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import daomephsta.spinneret.SpinneretArguments.InvalidArgumentException;
import daomephsta.spinneret.versioning.MinecraftVersions;

public class SuggestionTests
{
    private Spinneret spinneret = new Spinneret(Paths.get("."));

    @ParameterizedTest(name = "With mod name {0}")
    @CsvFileSource(resources = "/mod_names.csv")
    public void suggestModId(String modName) throws InvalidArgumentException
    {
        String suggestion = ArgumentSuggestions.modId(modName);
        if (suggestion != null)
        {
            var args = spinneret.createArguments();
            // Will throw if suggestion is invalid
            args.modId(suggestion);
        }
    }

    @ParameterizedTest(name = "With mod name {0}")
    @CsvFileSource(resources = "/mod_names.csv")
    public void suggestFolderName(String modName) throws InvalidArgumentException
    {
        String suggestion = ArgumentSuggestions.folderName(modName);
        if (suggestion != null)
        {
            var args = spinneret.createArguments();
            // Will throw if suggestion is invalid
            args.folderName(suggestion);
        }
    }

    private static class AuthorsConverter implements ArgumentConverter
    {
        private static final Pattern SPLITTER = Pattern.compile(",\\s?");

        @Override
        public Object convert(Object source, ParameterContext context) throws ArgumentConversionException
        {
            if (source instanceof String argument)
                return SPLITTER.splitAsStream(argument).toList();
            else
                throw new ArgumentConversionException("Expected string as conversion input");
        }
    }

    @ParameterizedTest(name = "With mod id {0}, authors [{1}]")
    @CsvFileSource(resources = "/suggestRootPackageName_args.csv")
    public void suggestRootPackageName(String modId, @ConvertWith(AuthorsConverter.class) List<String> authors)
        throws InvalidArgumentException
    {
        String suggestion = ArgumentSuggestions.rootPackageName(ArgumentSuggestions.modId(modId), authors);
        if (suggestion != null)
        {
            var args = spinneret.createArguments();
            // Will throw if suggestion is invalid
            args.rootPackageName(suggestion);
        }
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "19w13a, 1.14.x",
        "1.15-pre3, 1.15.x",
        "1.16-rc1, 1.16.x",
        "1.16, 1.16.x",
        "1.17.1, 1.17.x",
        "21w44a, 1.18.x",
        "1.18-pre4, 1.18.x",
        "1.18-rc4, 1.18.x",
        "1.18, 1.18.x",
        "1.18.1, 1.18.x"
    })
    public void suggestCompatibleMinecraftVersions(String minecraftVersion, String expected)
    {
        Spinneret spinneret = new Spinneret(Paths.get("."));
        var minecraftVersions = MinecraftVersions.load(
            Paths.get("minecraft_versions.json"),
            spinneret.configuration.urls().minecraftVersions).join();
        Assertions.assertEquals(expected, ArgumentSuggestions.compatibleMinecraftVersions(
            minecraftVersions.get(minecraftVersion)));
    }
}
