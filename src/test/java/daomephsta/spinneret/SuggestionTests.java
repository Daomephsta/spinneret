package daomephsta.spinneret;

import org.junit.jupiter.params.ParameterizedTest;
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
}
