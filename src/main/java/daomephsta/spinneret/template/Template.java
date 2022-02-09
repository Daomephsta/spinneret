package daomephsta.spinneret.template;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.BiFunction;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import daomephsta.spinneret.SpinneretArguments;
import daomephsta.spinneret.util.Json;
import daomephsta.spinneret.util.JsonBundleLoader;
import daomephsta.spinneret.versioning.MinecraftVersion;
import daomephsta.spinneret.versioning.MinecraftVersions;
import daomephsta.spinneret.versioning.Range;
import liqp.filters.Filter;

public class Template
{
    private static final Json JSON = new Json(new GsonBuilder().setLenient().create());
    // Ensure liqp additions are registered before any usage of liqp
    static
    {
        Filter.registerFilter(new JsonFilter());
        Filter.registerFilter(new PascalCaseFilter());
    }

    public static class Variant
    {
        private final Range<MinecraftVersion> minecraftRange;
        public final Map<String, TemplateVariable> templateVariables;
        private final TemplateSource source;

        private Variant(Range<MinecraftVersion> minecraftRange, 
            Map<String, TemplateVariable> templateVariables, TemplateSource source)
        {
            this.minecraftRange = minecraftRange;
            this.templateVariables = templateVariables;
            this.source = source;
        }

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

    public static Variant select(URL templateUrl, MinecraftVersion minecraftVersion, MinecraftVersions minecraftVersions,
        BiFunction<MinecraftVersion, List<Variant>, Variant> defaultFactory)
    {
        List<Variant> variants = Template.readVariants(templateUrl, minecraftVersions);
        for (var variant : variants)
        {
            if (variant.matches(minecraftVersion))
                return variant;
        }
        return defaultFactory.apply(minecraftVersion, variants);
    }

    private static List<Variant> readVariants(URL selectors, MinecraftVersions minecraftVersions)
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
                    return readVariant(templateJson, minecraftVersions);
                })
                .collect(toList());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static Variant readVariant(JsonObject json, MinecraftVersions minecraftVersions)
    {
        var minecraftRange = Range.parse(minecraftVersions::get,
            Json.getAsString(json, "minecraft"));
        ResourceBundle translations = readTranslations(json);
        Map<String, TemplateVariable> variables = json.has("variables") 
            ? TemplateVariable.readVariables(Json.getAsObject(json, "variables"), 
                key -> translations.containsKey(key) ? translations.getString(key) : key)
            : Collections.emptyMap();
        var source = JSON.getAs(json, "source", TemplateSource.class);      
        return new Variant(minecraftRange, variables, source);
    }

    private static ResourceBundle readTranslations(JsonObject json)
    {
        var languages = Json.getAsObject(json, "languages");
        String baseUrl = Json.getAsString(languages, "base-url");
        Set<Locale> supports = JSON.getAsSet(languages, "supports", Locale.class);
        var translations = JsonBundleLoader.load("lang", baseUrl, supports);
        return translations;
    }
}
