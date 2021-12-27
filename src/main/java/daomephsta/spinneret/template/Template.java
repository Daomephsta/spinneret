package daomephsta.spinneret.template;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import daomephsta.spinneret.Spinneret;
import daomephsta.spinneret.SpinneretArguments;
import daomephsta.spinneret.util.Json;
import daomephsta.spinneret.versioning.MinecraftVersion;
import daomephsta.spinneret.versioning.Range;

public class Template
{
    private static final Json JSON = new Json(new GsonBuilder().setLenient().create());
    public record Variant(
        Range<MinecraftVersion> minecraftRange,
        TemplateSource source,
        Set<Path> exclude)
    {
        private boolean matches(MinecraftVersion minecraft)
        {
            return minecraftRange.contains(minecraft);
        }

        public boolean isLater(Variant other)
        {
            return other.minecraftRange.max.compareTo(minecraftRange.max) > 0;
        }

        public void generate(SpinneretArguments spinneretArgs) throws IOException
        {
            source.generate(spinneretArgs);
        }
    }

    public static Variant select(URL templateUrl, MinecraftVersion minecraftVersion,
        BiFunction<MinecraftVersion, List<Variant>, Variant> defaultFactory)
    {
        List<Variant> variants = Template.readVariants(templateUrl);
        for (var variant : variants)
        {
            if (variant.matches(minecraftVersion))
                return variant;
        }
        return defaultFactory.apply(minecraftVersion, variants);
    }

    private static List<Variant> readVariants(URL selectors)
    {
        try (Reader selectorsReader = new InputStreamReader(selectors.openStream()))
        {
            var json = JSON.readObject(selectorsReader);
            var templateDefaults = json.has("variant_defaults")
                ? Json.getAsObject(json, "variant_defaults")
                : null;
            return Json.streamAsObjects(json, "variants")
                .map(templateJson ->
                {
                    if (templateDefaults != null)
                        templateJson = Json.withDefaults(templateJson, templateDefaults);
                    return readVariant(templateJson);
                })
                .collect(toList());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static Variant readVariant(JsonObject json)
    {
        var minecraftRange = Range.parse(Spinneret.minecraftVersions()::get,
            Json.getAsString(json, "minecraft"));
        return new Variant(minecraftRange,
            JSON.getAs(json, "source", TemplateSource.class),
            JSON.streamAs(json, "exclude", String.class)
                .map(Paths::get).collect(toSet()));
    }
}
