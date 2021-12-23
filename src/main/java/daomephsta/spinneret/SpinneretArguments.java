package daomephsta.spinneret;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import daomephsta.spinneret.ModScope.RootPackage;
import daomephsta.spinneret.template.Template;
import daomephsta.spinneret.versioning.MinecraftVersion;
import liqp.parser.LiquidSupport;

public class SpinneretArguments implements LiquidSupport
{
    static class TemplateScope
    {
        public URL url;
    }
    private final TemplateScope template = new TemplateScope();
    private final ModScope mod = new ModScope();
    private Template selectedTemplate = null;

    public SpinneretArguments template(String template) throws InvalidArgumentException
    {
        this.template.url = Spinneret.configuration().getTemplateByAlias(template);
        if (this.template.url == null)
        {
            try
            {
                this.template.url = new URL(template);
            }
            catch (MalformedURLException e)
            {
                throw new InvalidArgumentException(e);
            }
        }
        return this;
    }

    public Template template()
    {
        if (selectedTemplate == null)
            throw new IllegalStateException("SpinneretArguments.selectTemplate() must be called");
        return selectedTemplate;
    }

    public SpinneretArguments minecraftVersion(String minecraftVersion) throws InvalidArgumentException
    {
        this.mod.minecraftVersion = Spinneret.minecraftVersions().get(minecraftVersion);
        if (this.mod.minecraftVersion == null)
            throw new InvalidArgumentException("Unknown version " + minecraftVersion);
        return this;
    }

    public SpinneretArguments selectTemplate(
        BiFunction<MinecraftVersion, List<Template>, Template> defaultFactory)
    {
        this.selectedTemplate = Template.select(template.url, mod.minecraftVersion, defaultFactory);
        return this;
    }

    public MinecraftVersion minecraftVersion()
    {
        return mod.minecraftVersion;
    }

    public SpinneretArguments modName(String modName)
    {
        this.mod.name = modName;
        return this;
    }

    public SpinneretArguments modId(String modId) throws InvalidArgumentException
    {
        handleProblems("Invalid mod ID " + modId, validateModId(modId));
        this.mod.id = modId;
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
        if (mod.name == null)
            throw new IllegalStateException("Mod name required");
        // Deliberate user locale usage as the string is being normalised anyway
        String normalised = Normalizer.normalize(mod.name.toLowerCase(), Normalizer.Form.NFD);
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
        mod.folderName = folderName;
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
        if (mod.name == null)
            throw new IllegalStateException("Mod name required");
        return mod.name.replaceAll("[\\\\\\/:*?\\\"<>|\\x00]", "");
    }

    public SpinneretArguments author(String author)
    {
        this.mod.authors.add(author);
        return this;
    }

    public SpinneretArguments rootPackageName(String packageName) throws InvalidArgumentException
    {
        handleProblems("Invalid package name " + packageName, validatePackageName(packageName));
        this.mod.rootPackage = new RootPackage(packageName);
        return this;
    }

    private Collection<String> validatePackageName(String packageName)
    {
        Collection<String> problems = new ArrayList<>();
        if (packageName.length() < 1)
        {
            problems.add("Minimum package name length is 1 character");
            return problems;
        }
        boolean isElementStart = true;
        for (int i = 0; i < packageName.length(); i++)
        {
            char c = mod.folderName.charAt(i);
            if (isElementStart)
            {
                if (!Character.isJavaIdentifierStart(c))
                    problems.add("Invalid start character " + c + " at index " + i);
            }
            else if (!Character.isJavaIdentifierPart(i))
                problems.add("Invalid character " + c + " at index " + i);
            if (!Character.isLowerCase(c))
                problems.add("Non-lowercase character " + c + " at index " + i);
        }
        return problems;
    }

    public String suggestRootPackageName()
    {
        if (mod.id == null)
            throw new IllegalStateException("Mod ID required");
        if (mod.authors.isEmpty())
            throw new IllegalStateException("Author required");
        return mod.authors.get(0).toLowerCase() + "." + mod.id;
    }

    public String folderName()
    {
        return mod.folderName;
    }

    public SpinneretArguments modVersion(String modVersion)
    {
        this.mod.version = modVersion;
        return this;
    }

    @Override
    public Map<String, Object> toLiquid()
    {
        return Map.of(
            "template", template,
            "mod", mod
        );
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

        public InvalidArgumentException(Throwable cause)
        {
            super(cause);
            this.problems = Collections.emptyList();
        }

        public InvalidArgumentException(String message)
        {
            super(message);
            this.problems = Collections.emptyList();
        }
    }
}