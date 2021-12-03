package daomephsta.spinneret.versioning;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.JsonAdapter;

import daomephsta.spinneret.versioning.VersionExtension.Other;
import daomephsta.spinneret.versioning.VersionExtension.PreRelease;
import daomephsta.spinneret.versioning.VersionExtension.ReleaseCandidate;
import daomephsta.spinneret.versioning.VersionExtension.Snapshot;

@JsonAdapter(MinecraftVersion.Serialiser.class)
public class MinecraftVersion implements Comparable<MinecraftVersion>
{
    private static final Pattern
        TRI_VERSION = Pattern.compile("(?<major>\\d+)\\.(?<minor>\\d+)(?:[._](?<patch>\\d+))?(?<extension>.+)?"),
        RC = Pattern.compile("-rc(?<rc>\\d+)$"),
        PRE = Pattern.compile("(?:-pre| Pre-Release )(?<pre>\\d+)$"),
        SNAPSHOT = Pattern.compile("(?<year>\\d+)w(?<week>\\d+)(?<alpha>\\w)");

    public final VersionType type;
    public final int major, minor, patch;
    public final VersionExtension extension;
    private final String raw;

    private MinecraftVersion(VersionType type, int major, int minor, int patch, VersionExtension extension, String raw)
    {
        this.type = type;
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.extension = extension;
        this.raw = raw;
    }

    public static MinecraftVersion parse(String id, MinecraftVersion lastRelease)
    {
        return VersionType.fromVersionId(id).parse(id, lastRelease);
    }

    @Override
    public int compareTo(MinecraftVersion other)
    {
        if (type != other.type)
            return type.compareTo(other.type);
        if (major != other.major)
            return Integer.compare(major, other.major);
        if (minor != other.minor)
            return Integer.compare(minor, other.minor);
        if (patch != other.patch)
            return Integer.compare(patch, other.patch);
        return extension.compareTo(other.extension);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(type, extension, major, minor, patch);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (!(obj instanceof MinecraftVersion other)) return false;
        return type == other.type && major == other.major &&
            minor == other.minor && patch == other.patch &&
            Objects.equals(extension, other.extension);
    }

    public String toDebugString()
    {
        String extensionDebug = extension != VersionExtension.NONE
            ? extension.getClass().getSimpleName() + '(' + extension + ')'
            : "None";
        return String.format("%s %s.%s.%s %s", type.name(),
            major, minor, patch, extensionDebug);
    }

    @Override
    public String toString()
    {
        if (major == -1)
            return extension.toString();
        return String.format("%s%d.%d.%d%s", type, major, minor, patch, extension);
    }

    public enum VersionType
    {
        OTHER("")
        {
            @Override
            public MinecraftVersion parse(String version, MinecraftVersion lastRelease)
            {
                return new MinecraftVersion(this, -1, 0, 0, new Other(version), version);
            }
        },
        RUBYDUNG("rd-")
        {
            @Override
            public MinecraftVersion parse(String version, MinecraftVersion lastRelease)
            {
                try
                {
                    if (version.length() - prefix.length() == 6)
                    {
                        int day = Integer.parseInt(version.substring(prefix.length() + 0, prefix.length() + 2));
                        int hour = Integer.parseInt(version.substring(prefix.length() + 2, prefix.length() + 4));
                        int minute = Integer.parseInt(version.substring(prefix.length() + 4, prefix.length() + 6));
                        return new MinecraftVersion(this, day, hour, minute, VersionExtension.NONE, version);
                    }
                    else if (version.length() - prefix.length() == 8)
                        return dateVersion(version, prefix.length());
                    else
                        throw new IllegalArgumentException(version + " is not rubydung era version format");
                }
                catch (NumberFormatException e)
                {
                    throw new IllegalArgumentException(version + " is not rubydung era version format", e);
                }
            }
        },
        CLASSIC("c")
        {
            @Override
            public MinecraftVersion parse(String version, MinecraftVersion lastRelease)
            {
                return triVersion(version, "classic");
            }
        },
        INFDEV("inf-") {
            @Override
            public MinecraftVersion parse(String version, MinecraftVersion lastRelease)
            {
                try
                {
                    return dateVersion(version, prefix.length());
                }
                catch (NumberFormatException e)
                {
                    throw new IllegalArgumentException(version + " is not infdev era version format", e);
                }
            }
        },
        ALPHA("a")
        {
            @Override
            public MinecraftVersion parse(String version, MinecraftVersion lastRelease)
            {
                return triVersion(version, "alpha");
            }
        },
        BETA("b")
        {
            @Override
            public MinecraftVersion parse(String version, MinecraftVersion lastRelease)
            {
                return triVersion(version, "beta");
            }
        },
        RELEASE("")
        {
            @Override
            public MinecraftVersion parse(String version, MinecraftVersion lastRelease)
            {
                Matcher type = TRI_VERSION.matcher(version);
                if (type.matches())
                    return parseSemverish(version, type);
                else if ((type = SNAPSHOT.matcher(version)).matches())
                {
                    var snapshot = new Snapshot(
                        Integer.parseInt(type.group("year")),
                        Integer.parseInt(type.group("week")),
                        type.group("alpha"));
                    return new MinecraftVersion(this, lastRelease.major,
                        lastRelease.minor, lastRelease.patch, snapshot, version);
                }
                throw new IllegalArgumentException(version + " is not release era version format");
            }
        };

        public final String prefix;

        private VersionType(String prefix)
        {
            this.prefix = prefix;
        }

        public static VersionType fromVersionId(String id)
        {
            for (VersionType era : VersionType.values())
            {
                if (!era.prefix.isEmpty() && id.startsWith(era.prefix))
                    return era;
            }
            if (TRI_VERSION.matcher(id).matches() || SNAPSHOT.matcher(id).matches())
            {
                return VersionType.RELEASE;
            }
            return VersionType.OTHER;
        }

        public abstract MinecraftVersion parse(String version, MinecraftVersion lastRelease);

        @Override
        public String toString()
        {
            return prefix;
        }

        protected MinecraftVersion dateVersion(String version, int dateStart) throws NumberFormatException
        {
            int year = Integer.parseInt(version.substring(dateStart + 0, dateStart + 4));
            int month = Integer.parseInt(version.substring(dateStart + 4, dateStart + 6));
            int day = Integer.parseInt(version.substring(dateStart + 6, dateStart + 8));
            return new MinecraftVersion(this, year, month, day, VersionExtension.NONE, version);
        }

        protected MinecraftVersion triVersion(String version, String era)
        {
            String triVersion = version.substring(prefix.length());
            Matcher triVersionMatcher = TRI_VERSION.matcher(triVersion);
            if (triVersionMatcher.matches())
                return parseSemverish(triVersion, triVersionMatcher);
            throw new IllegalArgumentException(version + " is not " + era + " era version format");
        }
    }

    private static MinecraftVersion parseSemverish(String version, Matcher type)
    {
        var extension = VersionExtension.NONE;
        Matcher subtype = RC.matcher(version);
        if (subtype.find())
            extension = new ReleaseCandidate(Integer.parseInt(subtype.group("rc")));
        else if ((subtype = PRE.matcher(version)).find())
            extension = new PreRelease(Integer.parseInt(subtype.group("pre")));
        else if(type.end() != version.length())
            extension = new Other(version.substring(type.end()));
        String patchGroup = type.group("patch");
        return new MinecraftVersion(VersionType.RELEASE,
            Integer.parseInt(type.group("major")),
            Integer.parseInt(type.group("minor")),
            patchGroup != null ? Integer.parseInt(patchGroup) : 0,
            extension, version);
    }

    static class Serialiser implements JsonDeserializer<MinecraftVersion>, JsonSerializer<MinecraftVersion>
    {
        @Override
        public JsonElement serialize(MinecraftVersion version, Type type, JsonSerializationContext context)
        {
            var json = new JsonObject();
            json.add("era", context.serialize(version.type));
            json.addProperty("major", version.major);
            json.addProperty("minor", version.minor);
            json.addProperty("patch", version.patch);
            json.addProperty("raw", version.raw);
            version.extension.write(json);
            return json;
        }

        @Override
        public MinecraftVersion deserialize(JsonElement e, Type type, JsonDeserializationContext context)
            throws JsonParseException
        {
            JsonObject json = e.getAsJsonObject();
            VersionType era = context.deserialize(json.get("era"), VersionType.class);
            int major = json.get("major").getAsInt(),
                minor = json.get("minor").getAsInt(),
                patch = json.get("patch").getAsInt();
            String raw = json.get("raw").getAsString();
            return new MinecraftVersion(era, major, minor, patch, VersionExtension.read(json), raw);
        }
    }
}
