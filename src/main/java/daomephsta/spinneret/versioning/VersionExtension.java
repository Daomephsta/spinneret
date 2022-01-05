package daomephsta.spinneret.versioning;

import com.google.gson.JsonObject;

public interface VersionExtension extends Comparable<VersionExtension>
{
    void write(JsonObject json);

    int getSortIndex();

    @Override
    default int compareTo(VersionExtension other)
    {
        return Integer.compare(getSortIndex(), other.getSortIndex());
    }

    static VersionExtension read(JsonObject json)
    {
        var extensionType = json.get("extension").getAsString();
        VersionExtension extension = switch (extensionType)
        {
        case "none" -> NONE;
        case "pre" -> new PreRelease(json.get("pre").getAsInt());
        case "rc" -> new ReleaseCandidate(json.get("rc").getAsInt());
        case "snapshot" -> new Snapshot(
                json.get("year").getAsInt(),
                json.get("week").getAsInt(),
                json.get("alpha").getAsString());
        case "other" -> new Other(json.get("other").getAsString());
        default ->
            throw new IllegalArgumentException("Unexpected value: " + extensionType);
        };
        return extension;
    }

    public static final VersionExtension NONE = new VersionExtension()
    {
        @Override
        public int getSortIndex()
        {
            return 4;
        }

        @Override
        public void write(JsonObject json)
        {
            json.addProperty("extension", "none");
        }

        @Override
        public String toString()
        {
            return "";
        }
    };

    record PreRelease(int id) implements VersionExtension
    {
        @Override
        public int getSortIndex()
        {
            return 3;
        }

        @Override
        public int compareTo(VersionExtension other)
        {
            if (other instanceof PreRelease otherPrerelease)
                return Integer.compare(id, otherPrerelease.id);
            return VersionExtension.super.compareTo(other);
        }

        @Override
        public void write(JsonObject json)
        {
            json.addProperty("extension", "pre");
            json.addProperty("pre", id);
        }

        @Override
        public String toString()
        {
            return "-pre" + id;
        }
    }

    record ReleaseCandidate(int id) implements VersionExtension
    {
        @Override
        public int getSortIndex()
        {
            return 2;
        }

        @Override
        public int compareTo(VersionExtension other)
        {
            if (other instanceof ReleaseCandidate otherReleaseCandidate)
                return Integer.compare(id, otherReleaseCandidate.id);
            return VersionExtension.super.compareTo(other);
        }

        @Override
        public void write(JsonObject json)
        {
            json.addProperty("extension", "rc");
            json.addProperty("rc", id);
        }

        @Override
        public String toString()
        {
            return "-rc" + id;
        }
    }

    public record Snapshot(int year, int week, String alpha) implements VersionExtension
    {
        @Override
        public int getSortIndex()
        {
            return 1;
        }

        @Override
        public int compareTo(VersionExtension other)
        {
            if (other instanceof Snapshot otherSnapshot)
            {
                if (year != otherSnapshot.year)
                    return Integer.compare(year, otherSnapshot.year);
                if (week != otherSnapshot.week)
                    return Integer.compare(week, otherSnapshot.week);
                return alpha.compareTo(otherSnapshot.alpha);
            }
            return VersionExtension.super.compareTo(other);
        }

        @Override
        public void write(JsonObject json)
        {
            json.addProperty("extension", "snapshot");
            json.addProperty("year", year);
            json.addProperty("week", week);
            json.addProperty("alpha", alpha);
        }

        @Override
        public String toString()
        {
            return String.format("-%02dw%02d%s", year, week, alpha);
        }
    }

    public record Other(String extension) implements VersionExtension
    {
        @Override
        public int getSortIndex()
        {
            return 0;
        }

        @Override
        public int compareTo(VersionExtension other)
        {
            if (other instanceof Other otherOther)
                return extension.compareTo(otherOther.extension);
            return VersionExtension.super.compareTo(other);
        }

        @Override
        public void write(JsonObject json)
        {
            json.addProperty("extension", "other");
            json.addProperty("other", extension);
        }

        @Override
        public String toString()
        {
            return extension;
        }
    }
}
