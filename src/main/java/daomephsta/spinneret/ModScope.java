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
    public String name;
    public String id;
    public String version;
    public List<String> authors = new ArrayList<>(4);
    public ModScope.RootPackage rootPackage;
    public String folderName;

    @Override
    public Map<String, Object> toLiquid()
    {
        return Map.of(
            "minecraftVersion", minecraftVersion.raw,
            "name", name,
            "id", id,
            "version", version,
            "authors", authors,
            "rootPackage", rootPackage
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