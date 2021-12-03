package daomephsta.spinneret;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class SpinneretArguments
{
    private String modId;
    private String modName;
    private String folderName;

    public SpinneretArguments modName(String modName)
    {
        this.modName = modName;
        return this;
    }

    public SpinneretArguments modId(String modId) throws InvalidArgumentException
    {
        handleProblems("Invalid mod ID " + modId, validateModId(modId));
        this.modId = modId;
        return this;
    }

    private Collection<String> validateModId(String modId)
    {
        Collection<String> problems = new ArrayList<>(4);
        if (modId.length() < 1)
        {
            problems.add("Minimum mod ID length is 1 character");
            return problems;
        }
        if (modId.length() > 64)
            problems.add("Maximum mod ID length is 64 characters");
        char start = modId.charAt(0);
        if (!inRange(start, 'a', 'z'))
            problems.add("Start of mod ID must be a-z");
        for (int i = 1; i < modId.length(); i++)
        {
            char c = modId.charAt(i);
            if (!inRange(c, 'a', 'z') && !inRange(c, '0', '9') &&
                c != '-' && c != '_')
            {
                problems.add("Invalid character at index " + i +
                    ". Non-start mod ID characters must be a-z, 0-9, -, or _");
            }
        }
        return problems;
    }

    public String suggestModId()
    {
        if (modName == null)
            throw new IllegalStateException("Mod name required");
        // Deliberate user locale usage as the string is being normalised anyway
        String normalised = Normalizer.normalize(modName.toLowerCase(), Normalizer.Form.NFD);
        StringBuilder suggestion = new StringBuilder(normalised.length());
        for (int i = 0; i < normalised.length(); i++)
        {
            char c = normalised.charAt(i);
            if (Character.isWhitespace(c))
                suggestion.append('_');
            else if (!inRange(c, 'a', 'z') && !inRange(c, '0', '9') &&
                c != '-' && c != '_')
            {
                // NO OP
            }
            else
                suggestion.append(c);
        }
        if (suggestion.isEmpty() || !inRange(suggestion.charAt(0), 'a', 'z'))
            return null;
        return suggestion.toString();
    }

    public SpinneretArguments folderName(String folderName) throws InvalidArgumentException
    {
        handleProblems("Invalid folder name " + folderName, validateFolderName(folderName));
        this.folderName = folderName;
        return this;
    }

    private Collection<String> validateFolderName(String folderName)
    {
        Collection<String> problems = new ArrayList<>(4);
        if (folderName.length() < 1)
        {
            problems.add("Minimum folder name length is 1 character");
            return problems;
        }
        for (int i = 0; i < folderName.length(); i++)
        {
            char c = folderName.charAt(i);
            if ("\\/:*?\"<>|".indexOf(c) != -1)
                problems.add(c + " at index " + i + ". Invalid or non-portable.");
            else if (c == '\0')
                problems.add("Null byte at index " + i + ". Invalid or non-portable.");
        }
        return problems;
    }

    public String suggestFolderName()
    {
        if (modName == null)
            throw new IllegalStateException("Mod name required");
        return modName.replaceAll("[\\\\\\/:*?\\\"<>|\\x00]", "");
    }

    public Map<String, Object> buildMap()
    {
        return Map.of(
            "spinneret:mod_id", modId,
            "spinneret:mod_name", modName,
            "spinneret:folder_name", folderName);
    }

    private static boolean inRange(char c, char lower, char upper)
    {
        return lower <= c && c <= upper;
    }

    private void handleProblems(String header, Collection<String> problems)
        throws InvalidArgumentException
    {
        if (!problems.isEmpty())
            throw new InvalidArgumentException(header, problems);
    }

    public static class InvalidArgumentException extends Exception
    {
        public final Collection<String> problems;

        public InvalidArgumentException(String header, Collection<String> problems)
        {
            super(header + "\n" + String.join("\n", problems));
            this.problems = problems;
        }
    }
}