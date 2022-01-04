package daomephsta.spinneret;

import java.util.List;

import daomephsta.spinneret.versioning.MinecraftVersion;

public class ArgumentSuggestions
{
    public static String modId(String modName)
    {
        return SpinneretArguments.normalise(modName, (i, ch) ->
        {
            if (Character.isWhitespace(ch))
                return '_';
            else if (SpinneretArguments.inRange(ch, 'a', 'z') || SpinneretArguments.inRange(ch, '0', '9') || ch == '-' || ch == '_')
                return ch;
            else
                return -1;
        });
    }

    public static String folderName(String modName)
    {
        return SpinneretArguments.normalise(modName, (i, ch) ->
        {
            if (ch == ' ')
                return '-';
            if ("\\/:*?\"<>|\0".indexOf(ch) != -1)
                return -1;
            return ch;
        });
    }

    public static String rootPackageName(String modId, List<String> authors)
    {
        if (authors.isEmpty())
            return null;
        String normalisedMainAuthor = SpinneretArguments.normalise(authors.get(0), (i, ch) ->
        {
            if (Character.isWhitespace(ch))
                return '_';
            else if ((i == 0 && Character.isJavaIdentifierStart(ch)) ||
                (i != 0 && Character.isJavaIdentifierPart(ch)))
            {
                return ch;
            }
            else
                return -1;
        });
        if (normalisedMainAuthor == null || normalisedMainAuthor.isEmpty())
            return null;
        String normalisedModId = SpinneretArguments.normalise(modId, (i, ch) ->
        {
            if (ch == '-' || Character.isWhitespace(ch))
                return '_';
            else if ((i == 0 && Character.isJavaIdentifierStart(ch)) ||
                (i != 0 && Character.isJavaIdentifierPart(ch)))
            {
                return ch;
            }
            else
                return -1;
        });
        if (normalisedModId == null || normalisedModId.isEmpty())
            return null;
        return normalisedMainAuthor + "." + normalisedModId;
    }

    public static String compatibleMinecraftVersions(MinecraftVersion minecraftVersion)
    {
        return minecraftVersion.major + "." +
            minecraftVersion.minor + ".x";
    }
}
