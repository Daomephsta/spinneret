package daomephsta.spinneret.template;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import daomephsta.spinneret.Spinneret;
import daomephsta.spinneret.SpinneretArguments;
import daomephsta.spinneret.util.Json;
import daomephsta.spinneret.versioning.MinecraftVersion;
import daomephsta.spinneret.versioning.MinecraftVersions;
import daomephsta.spinneret.versioning.Range;

public class Template
{
    private static final Gson JSON = new GsonBuilder().setLenient().create();
    private final Range<MinecraftVersion> minecraftRange;
    private final TemplateSource source;

    private Template(Range<MinecraftVersion> minecraftRange, TemplateSource source)
    {
        this.minecraftRange = minecraftRange;
        this.source = source;
    }

    public static Template read(JsonObject json, MinecraftVersions mcVersions, Gson gson)
    {
        Range<MinecraftVersion> minecraftRange = Range.parse(mcVersions::get, json.get("minecraft").getAsString());
        return new Template(minecraftRange, gson.fromJson(json.get("source"), TemplateSource.class));
    }

    public boolean matches(MinecraftVersion minecraft)
    {
        return minecraftRange.contains(minecraft);
    }

    public boolean isLater(Template other)
    {
        return other.minecraftRange.max.compareTo(minecraftRange.max) > 0;
    }

    public void generate(SpinneretArguments spinneretArgs) throws IOException
    {
        source.generate(spinneretArgs);
    }

    @Override
    public String toString()
    {
        return String.format("Template(minecraftRange=%s, %s)", minecraftRange, source);
    }

    public static Template select(URL templateUrl, MinecraftVersion minecraftVersion,
        BiFunction<MinecraftVersion, List<Template>, Template> defaultFactory)
    {
        var templates = Template.readSelectors(templateUrl);
        for (var template : templates)
        {
            if (template.matches(minecraftVersion))
                return template;
        }
        return defaultFactory.apply(minecraftVersion, templates);
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
                    templateJson = Json.withDefaults(templateJson, templateDefaults);
                templates.add(read(templateJson, Spinneret.minecraftVersions(), JSON));
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        return templates;
    }
}
