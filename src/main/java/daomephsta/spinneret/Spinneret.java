package daomephsta.spinneret;

import java.io.IOException;
import java.nio.file.Paths;

import daomephsta.spinneret.versioning.MinecraftVersions;

public class Spinneret
{
    private static Config configuration = null;
    private static MinecraftVersions versionCache;

    public static void spin(SpinneretArguments args) throws IOException
    {
        args.template().generate(args);
    }

    public static Config configuration()
    {
        if (configuration == null)
        {
            System.out.println("Loading configuration");
            configuration = Config.load();
        }
        return configuration;
    }

    public static MinecraftVersions minecraftVersions()
    {
        if (versionCache == null)
        {
            System.out.println("Fetching Minecraft versions");
            try
            {
                versionCache = MinecraftVersions.load(
                    Paths.get("minecraft_versions.json"),
                    Spinneret.configuration().minecraftVersions());
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
        return versionCache;
    }
}
