package daomephsta.spinneret.template;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import daomephsta.spinneret.versioning.MinecraftVersion;
import daomephsta.spinneret.versioning.MinecraftVersions;
import daomephsta.spinneret.versioning.Range;

public class Template
{
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

    public void generate() throws IOException
    {
        source.generate();
    }

    @Override
    public String toString()
    {
        return String.format("Template(minecraftRange=%s, %s)", minecraftRange, source);
    }
}
