package daomephsta.spinneret;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import daomephsta.spinneret.util.Json;
import daomephsta.spinneret.versioning.MinecraftVersion;

public class Config
{
    private static final Gson GSON = new GsonBuilder().setLenient()
        .registerTypeAdapter(Urls.class, (JsonDeserializer<Urls>) Urls::deserialise)
        .create();
    public static class Urls
    {
        public final URI
            minecraftVersions,
            fabricApiVersions;
        private final String
            yarnVersions,
            fabricLoaderVersions;

        private Urls(URI minecraftVersions, URI fabricApiVersions, String yarnVersions, String fabricLoaderVersions)
        {
            this.minecraftVersions = minecraftVersions;
            this.fabricApiVersions = fabricApiVersions;
            this.yarnVersions = yarnVersions;
            this.fabricLoaderVersions = fabricLoaderVersions;
        }

        static Urls deserialise(JsonElement json, Type type, JsonDeserializationContext context)
        {
            var jsonObj = Json.asObject(json);
            try
            {
                return new Urls(
                    new URI(Json.getAsString(jsonObj, "minecraftVersions")),
                    new URI(Json.getAsString(jsonObj, "fabricApiVersions")),
                    Json.getAsString(jsonObj, "yarnVersions"),
                    Json.getAsString(jsonObj, "fabricLoaderVersions")
                );
            }
            catch (URISyntaxException e)
            {
                throw new JsonParseException(e);
            }
        }

        public URI yarnVersionsFor(MinecraftVersion version)
        {
            return URI.create(yarnVersions.formatted(version.raw));
        }

        public URI fabricLoaderVersionsFor(MinecraftVersion version)
        {
            return URI.create(fabricLoaderVersions.formatted(version.raw));
        }
    }
    private Urls urls;
    private Map<String, URL> templateAliases;

    public static Config load()
    {
        try
        {
            Path config = Paths.get("spinneret.json");
            if (!Files.exists(config))
            {
                try (InputStream configStream = Config.class.getResourceAsStream("/spinneret.json"))
                {
                    Files.copy(configStream, config);
                }
            }
            try (Reader configReader = Files.newBufferedReader(config))
            {
                return GSON.fromJson(configReader, Config.class);
            }
        }
        catch (IOException e)
        {
            throw new ConfigurationException(e);
        }
    }

    public Urls urls()
    {
        return urls;
    }

    public Collection<String> getTemplateAliases()
    {
        return templateAliases.keySet();
    }

    public URL getTemplateByAlias(String alias)
    {
        return templateAliases.get(alias);
    }

    static class ConfigurationException extends RuntimeException
    {
        ConfigurationException(String message, Throwable cause)
        {
            super(message, cause);
        }

        ConfigurationException(String message)
        {
            super(message);
        }

        ConfigurationException(Throwable cause)
        {
            super(cause);
        }
    }
}