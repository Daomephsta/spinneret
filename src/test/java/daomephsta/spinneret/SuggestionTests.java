package daomephsta.spinneret;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import daomephsta.spinneret.SpinneretArguments.InvalidArgumentException;

public class SuggestionTests
{
    private static String[] modNames()
    {
        return new String[] {"  ", "Tèst Mod", "T̷̿̆ë̵͌s̵̽̑t̸́͘ ̵̾̈Ṃ̵̊ò̶͝d̸̽̎", "Tɘƨt Mob",
            "Τеѕт Mοd", "test\u202Emod", "テストモヂゥ", "test😀mod"};
    }

    @ParameterizedTest(name = "With mod name {0}")
    @MethodSource("modNames")
    public void suggestModId(String modName) throws InvalidArgumentException
    {
        var args = new SpinneretArguments().modName(modName);
        String suggestion = args.suggestModId();
        if (suggestion != null)
        {
            // Will throw if suggestion is invalid
            args.modId(suggestion);
        }
    }

    @ParameterizedTest(name = "With mod name {0}")
    @MethodSource("modNames")
    public void suggestFolderName(String modName) throws InvalidArgumentException
    {
        var args = new SpinneretArguments().modName(modName);
        String suggestion = args.suggestFolderName();
        if (suggestion != null)
        {
            // Will throw if suggestion is invalid
            args.folderName(suggestion);
        }
    }

    private static Stream<Arguments> suggestRootPackageNameArgs()
    {
        return Stream.of(
            Arguments.of("blank", List.of("    ")),
            Arguments.of("test_mod", List.of("Alice", "Bob")),
            Arguments.of("hello-fabric", List.of("Charliè")),
            Arguments.of("mod-ja", List.of("たろう", "はなこ")),
            Arguments.of("zalgo", List.of("̴̜̚B̸̲̌ô̸̲B̸̲̌ô̸̲b̶̞̒b̶̞̒")),
            Arguments.of("smiley", List.of("😀liver")),
            Arguments.of("test-mod", List.of("1lorem-ipsum_DOLOR"))
        );
    }

    @ParameterizedTest(name = "With mod id {0}, authors {1}")
    @MethodSource("suggestRootPackageNameArgs")
    public void suggestRootPackageName(String modId, List<String> authors)
        throws InvalidArgumentException
    {
        var args = new SpinneretArguments().modId(modId);
        for (var author : authors)
            args.addAuthor(author);
        String suggestion = args.suggestRootPackageName();
        if (suggestion != null)
        {
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
        throws InvalidArgumentException
    {
        var args = new SpinneretArguments().minecraftVersion(minecraftVersion);
        Assertions.assertEquals(expected, args.suggestCompatibleMinecraftVersions());
    }
}
