package daomephsta.spinneret;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import daomephsta.spinneret.template.Template;
import daomephsta.spinneret.versioning.MinecraftVersions;

public class Spinneret
{
    private static Config configuration;
    private static MinecraftVersions versionCache;
    private static final Gson gson = new Gson();

    public static void spin(SpinneretArguments args) throws IOException
    {
        System.out.println("Loading configuration");
        configuration = Config.load();
        System.out.println("Fetching Minecraft versions");
        versionCache = MinecraftVersions.load(
            Paths.get("minecraft_versions.json"),
            Spinneret.configuration().minecraftVersions());
    }

    private static void readSelectors(Path selectors) throws IOException
    {
        try (var selectorsReader = Files.newBufferedReader(selectors))
        {
            var json = gson.fromJson(selectorsReader, JsonObject.class);
            var templateDefaults = gson.fromJson(json.get("template_defaults"), JsonObject.class);
            for (JsonObject templateJson : gson.fromJson(json.get("templates"), JsonObject[].class))
            {
                var template = Template.read(withDefaults(templateJson, templateDefaults), versionCache, gson);
                if (template.matches(versionCache.get("1.17")))
                {
                    template.generate();
                }
            }
        }
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

    public static Config configuration()
    {
        return configuration;
    }
}
