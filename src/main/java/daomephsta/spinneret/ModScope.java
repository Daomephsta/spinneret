package daomephsta.spinneret;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import daomephsta.spinneret.versioning.MinecraftVersion;
import liqp.parser.Inspectable;
import liqp.parser.LiquidSupport;

class ModScope implements LiquidSupport
{
    public MinecraftVersion minecraftVersion;
    public String compatibleMinecraftVersions;
    public String name;
    public String id;
    public String version;
    public List<String> authors = new ArrayList<>(4);
    public String description;
    public ModScope.RootPackage rootPackage;
    public String folderName;
    public Map<String, String> dependencies;

    @Override
    public Map<String, Object> toLiquid()
    {
        return Map.of(
            "minecraftVersion", minecraftVersion.raw,
            "compatibleMinecraftVersions", compatibleMinecraftVersions,
            "name", name,
            "id", id,
            "version", version,
            "authors", authors,
            "description", description,
            "rootPackage", rootPackage,
            "dependencies", dependencies
        );
    }

    static class RootPackage implements Inspectable
    {
        public final String name;
        public final String directories;

        public RootPackage(String packageName)
        {
            this.name = packageName;
            this.directories = packageName.replace('.', '/');
        }
    }
}