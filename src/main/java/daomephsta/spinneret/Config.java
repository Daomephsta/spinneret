package daomephsta.spinneret;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Config
{
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private final URI fabricSupportedVersions,
                      minecraftVersions;

    private Config(URI fabricSupportedVersions, URI minecraftVersions)
    {
        this.fabricSupportedVersions = fabricSupportedVersions;
        this.minecraftVersions = minecraftVersions;
    }

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

    public URI fabricSupportedVersions()
    {
        return fabricSupportedVersions;
    }

    public URI minecraftVersions()
    {
        return minecraftVersions;
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