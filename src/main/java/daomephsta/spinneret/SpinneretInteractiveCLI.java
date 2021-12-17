package daomephsta.spinneret;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import daomephsta.spinneret.SpinneretArguments.InvalidArgumentException;
import daomephsta.spinneret.template.Template;
import daomephsta.spinneret.versioning.MinecraftVersion;

public class SpinneretInteractiveCLI
{
    private static final Gson JSON = new GsonBuilder().setLenient().create();

    public static void main(String[] args)
    {
        var spinneretArgs = new SpinneretArguments();
        try (var input = new Scanner(System.in))
        {
            prompt(input, "Template", "spinneret-java", spinneretArgs::template);
            prompt(input, "Minecraft version", Spinneret.minecraftVersions().getLatest().raw,
                spinneretArgs::minecraftVersion);
            Template template = selectTemplate(input,
                spinneretArgs.template(), spinneretArgs.minecraftVersion());

            prompt(input, "Mod name", null, spinneretArgs::modName);
            prompt(input, "Mod ID", spinneretArgs.suggestModId(), spinneretArgs::modId);
            prompt(input, "Folder name", spinneretArgs.suggestFolderName(), spinneretArgs::folderName);
            prompt(input, "Mod version", "0.0.1", spinneretArgs::modVersion);
        }
    }

    private static Template selectTemplate(Scanner input, URL templateUrl, MinecraftVersion minecraftVersion)
    {
        List<Template> templates = readSelectors(templateUrl);
        for (var template : templates)
        {
            if (template.matches(minecraftVersion))
                return template;
        }
        return noMatchingTemplate(input, minecraftVersion, templates);
    }

    private static Template noMatchingTemplate(Scanner input,
        MinecraftVersion minecraftVersion,
        List<Template> templates)
    {
        System.out.printf("No template for %s. Attempt to use latest template? (y/n) ", minecraftVersion);
        String response = input.nextLine().toLowerCase();
        if (response.startsWith("y"))
            return templates.stream().reduce((a, b) -> a.isLater(b) ? a : b).get();
        else if (response.startsWith("n"))
        {
            System.out.println("Template required, aborting");
            System.exit(0);
        }
        else
            return noMatchingTemplate(input, minecraftVersion, templates);
        throw new IllegalStateException("Unreachable");
    }

    private interface ArgumentConsumer
    {
        public void consume(String argument) throws InvalidArgumentException;
    }

    private static void prompt(Scanner input, String prompt, String defaultValue, ArgumentConsumer consumer)
    {
        try
        {
            if (defaultValue == null || defaultValue.isBlank())
            {
                System.out.printf(prompt + ": ");
                var value = input.nextLine();
                if (value == null || value.isBlank())
                {
                    System.out.println("Invalid value");
                    prompt(input, prompt, defaultValue, consumer);
                }
                else
                    consumer.consume(value);
            }
            else
            {
                System.out.printf("%s (default '%s'): ", prompt, defaultValue);
                var value = input.nextLine();
                if (value == null || value.isBlank())
                    consumer.consume(defaultValue);
                else
                    consumer.consume(value);
            }
        }
        catch (InvalidArgumentException e)
        {
            System.out.println(e.getMessage());
            prompt(input, prompt, defaultValue, consumer);
        }
    }

    private static List<Template> readSelectors(URL selectors)
    {
        List<Template> templates = new ArrayList<>();
        try (Reader selectorsReader = new InputStreamReader(selectors.openStream()))
        {
            var json = JSON.fromJson(selectorsReader, JsonObject.class);
            var templateDefaults = json.has("template_defaults")
                ? JSON.fromJson(json.get("template_defaults"), JsonObject.class)
                : null;
            for (JsonObject templateJson : JSON.fromJson(json.get("templates"), JsonObject[].class))
            {
                if (templateDefaults != null)
                    templateJson = withDefaults(templateJson, templateDefaults);
                templates.add(Template.read(templateJson, Spinneret.minecraftVersions(), JSON));
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        return templates;
    }

    private static JsonObject withDefaults(JsonObject json, JsonObject defaults)
    {
        for (Entry<String, JsonElement> entry : defaults.entrySet())
        {
            JsonElement existing = json.get(entry.getKey());
            if (existing == null)
                json.add(entry.getKey(), entry.getValue());
            else if (existing instanceof JsonObject existingObj &&
                entry.getValue() instanceof JsonObject defaultsObj)
            {
                withDefaults(existingObj, defaultsObj);
            }
        }
        return json;
    }
}