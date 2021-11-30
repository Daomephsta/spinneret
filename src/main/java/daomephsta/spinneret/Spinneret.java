package daomephsta.spinneret;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import daomephsta.spinneret.versioning.MinecraftVersions;

public class Spinneret
{
    private static Config configuration;
    public static void main(String[] args) throws Exception
    {
        configuration = Config.load();
        MinecraftVersions.load(Paths.get("minecraft_versions.json"), Spinneret.configuration().minecraftVersions());
    }

    public static Config configuration()
    {
        return configuration;
    }

    private static Path findGit()
    {
        for (String pathElement : System.getenv("PATH").split(File.pathSeparator))
        {
            Path candidate = Paths.get(pathElement, "git.exe");
            if (Files.exists(candidate))
                return candidate;
        }
        return null;
    }
}
