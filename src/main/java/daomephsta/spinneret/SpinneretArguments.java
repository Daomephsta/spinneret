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
import java.util.function.Consumer;
import java.util.function.IntBinaryOperator;
import daomephsta.spinneret.ModScope.RootPackage;
import daomephsta.spinneret.template.Template;
import daomephsta.spinneret.versioning.MinecraftVersion;
import liqp.parser.Inspectable;
import liqp.parser.LiquidSupport;

public class SpinneretArguments implements LiquidSupport
{
    static class TemplateScope implements Inspectable
    {
        public URL url;
    }
    private final TemplateScope template = new TemplateScope();
    private final ModScope mod = new ModScope();
    private Template.Variant selectedTemplateVariant = null;

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
                handleProblems("Invalid URL or incorrect template alias/name",
                    Collections.singletonList(e.getLocalizedMessage()));
            }
        }
        return this;
    }

    public URL templateUrl()
    {
        return this.template.url;
    }

    public Template.Variant template()
    {
        if (selectedTemplateVariant == null)
        {
            throw new IllegalStateException(
                "SpinneretArguments.selectTemplateVariant() must be called");
        }
        return selectedTemplateVariant;
    }

    public SpinneretArguments minecraftVersion(String minecraftVersion) throws InvalidArgumentException
    {
        this.mod.minecraftVersion = Spinneret.minecraftVersions().get(minecraftVersion);
        if (this.mod.minecraftVersion == null)
            throw new InvalidArgumentException("Unknown version " + minecraftVersion);
        return this;
    }

    public SpinneretArguments minecraftVersion(MinecraftVersion minecraftVersion)
    {
        this.mod.minecraftVersion = minecraftVersion;
        return this;
    }

    public SpinneretArguments compatibleMinecraftVersions(String versionRange) throws InvalidArgumentException
    {
        var problems = new ArrayList<String>();
        if (versionRange.equals("*")) // Any range
        {
            for (int i = 0; i < versionRange.length(); i++)
            {
                char ch = versionRange.charAt(i);
                if (!Character.isDigit(ch) && ch != '.' && ch != 'x')
                    problems.add("Character at index " + i + " is not 0-9, ., or x");
            }
        }
        handleProblems("Invalid compatible version range", problems);
        this.mod.compatibleMinecraftVersions = versionRange;
        return this;
    }

    public SpinneretArguments selectTemplateVariant(
        BiFunction<MinecraftVersion, List<Template.Variant>, Template.Variant> defaultFactory)
    {
        this.selectedTemplateVariant = Template.select(template.url, mod.minecraftVersion, defaultFactory);
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

    public SpinneretArguments authors(List<String> authors)
    {
        this.mod.authors = authors;
        return this;
    }

    public SpinneretArguments description(String description)
    {
        mod.description = description;
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
        int offset = 0;
        for (var segment : packageName.split("\\."))
        {
            validatePackageSegment(segment, offset, problems::add);
            offset += segment.length();
        }
        return problems;
    }

    private void validatePackageSegment(String segment, int offset, Consumer<String> problems)
    {
        if (segment.isEmpty())
            problems.accept("Empty segment at index " + offset);
        for (int i = 0; i < segment.length(); i++)
        {
            char c = segment.charAt(i);
            if (i == 0)
            {
                if (!Character.isJavaIdentifierStart(c))
                    problems.accept("Invalid start character " + c + " at index " + (offset + i));
            }
            else if (!Character.isJavaIdentifierPart(c))
                problems.accept("Invalid character " + c + " at index " + (offset + i));
            if (Character.isAlphabetic(c) && !Character.isLowerCase(c))
                problems.accept("Non-lowercase character " + c + " at index " + (offset + i));
        }
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

    static boolean inRange(int c, int lower, int upper)
    {
        return lower <= c && c <= upper;
    }

    private void handleProblems(String header, Collection<String> problems) throws InvalidArgumentException
    {
        if (!problems.isEmpty())
            throw new InvalidArgumentException(header, problems);
    }

    static String normalise(String input, IntBinaryOperator charNormaliser)
    {
        // Deliberate user locale usage as the string is being normalised anyway
        String lowercase = Normalizer.normalize(input.toLowerCase(), Normalizer.Form.NFD);
        StringBuilder normalised = new StringBuilder(lowercase.length());
        for (int i = 0; i < lowercase.length(); i++)
        {
            char c = lowercase.charAt(i);
            int normalisedChar = charNormaliser.applyAsInt(i, c);
            if (normalisedChar != -1)
                normalised.append((char) normalisedChar);
        }
        if (normalised.isEmpty() || !inRange(normalised.charAt(0), 'a', 'z'))
            return null;
        return normalised.toString();
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