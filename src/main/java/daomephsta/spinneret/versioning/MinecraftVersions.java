package daomephsta.spinneret.versioning;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import daomephsta.spinneret.util.Json;

public class MinecraftVersions
{
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Json JSON = new Json(new GsonBuilder()
        .registerTypeAdapter(MinecraftVersion.class, new MinecraftVersion.Serialiser())
        .create());
    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");
    private static final DateFormat HTTP_DATE;
    static
    {
        HTTP_DATE = new SimpleDateFormat("E, dd LLL yyyy HH:mm:ss z",
            DateFormatSymbols.getInstance(Locale.ROOT));
        HTTP_DATE.setTimeZone(GMT);
    }
    private Date updated;
    private Map<String, MinecraftVersion> byId;
    private NavigableSet<MinecraftVersion> sorted;

    private MinecraftVersions(Date updated)
    {
        this.byId = new HashMap<>();
        this.sorted = new TreeSet<>();
        this.updated = updated;
    }

    public MinecraftVersion get(String version)
    {
        var minecraftVersion = byId.get(version);
        if (minecraftVersion == null)
            throw new IllegalArgumentException("Unknown version " + version);
        return minecraftVersion;
    }

    public MinecraftVersion getLatest()
    {
        return sorted.last();
    }

    public Iterable<MinecraftVersion> getAscending()
    {
        return sorted;
    }

    public Iterable<MinecraftVersion> getDescending()
    {
        return sorted.descendingSet();
    }

    public static CompletableFuture<MinecraftVersions> load(Path cache, URI versionManifest)
    {
        var mcVersionsFuture = Files.exists(cache)
            ? deserialise(cache)
            : CompletableFuture.supplyAsync(() -> new MinecraftVersions(new Date(0)));
        return mcVersionsFuture.thenApply(mcVersions ->
        {
            mcVersions.checkForUpdates(versionManifest).join();
            try
            {
                mcVersions.serialise(cache);
            }
            catch (IOException e)
            {
                throw new CompletionException(e);
            }
            return mcVersions;
        });
    }

    public CompletableFuture<Void> checkForUpdates(URI minecraftVersionManifest)
    {
        var request = HttpRequest.newBuilder(minecraftVersionManifest)
            .setHeader("If-Modified-Since", HTTP_DATE.format(updated))
            .GET().build();
        return CLIENT.sendAsync(request, BodyHandlers.ofInputStream()).thenAccept(response ->
        {
            switch (response.statusCode())
            {
            case 200 /*OK*/ ->
            {
                try (Reader reader = new InputStreamReader(response.body()))
                {
                    JsonArray versions = Json.getAsArray(JSON.readObject(reader), "versions");
                    byId = parseVersionManifest(versions);
                    sorted = new TreeSet<>(byId.values());
                }
                catch (IOException io)
                {
                    io.printStackTrace(System.err);
                }
            }
            case 304 /*NOT MODIFIED*/ -> {/*NO OP*/}
            default -> throw new IllegalStateException("Unexpected status code " + response.statusCode());
            }
            updated = Calendar.getInstance(GMT, Locale.ROOT).getTime();
        });
    }

    private MinecraftVersion lastRelease;
    private Map<String, MinecraftVersion> parseVersionManifest(JsonArray versions)
    {
        Map<String, MinecraftVersion> newVersionsById = new HashMap<>(versions.size());
        var latestVersion = versions.get(0).getAsJsonObject();
        try (Reader reader = new InputStreamReader(
            new URL(latestVersion.get("url").getAsString()).openStream()))
        {
            String assets = Json.getAsString(JSON.readObject(reader), "assets");
            lastRelease = MinecraftVersion.parse(assets, lastRelease);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Unable to determine parent of latest release", e);
        }
        for (JsonElement versionElement : versions)
        {
            var versionJson = versionElement.getAsJsonObject();
            var id = versionJson.get("id").getAsString();
            MinecraftVersion existing = byId.get(id);
            MinecraftVersion version = existing != null
                ? existing
                : MinecraftVersion.parse(id, lastRelease);
            if (version.extension == VersionExtension.NONE)
                lastRelease = version;
            newVersionsById.put(id, version);
        }
        return newVersionsById;
    }

    private static CompletableFuture<MinecraftVersions> deserialise(Path cache)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            try (Reader reader = Files.newBufferedReader(cache))
            {
                var root = JSON.readObject(reader);
                Date updated;
                try
                {
                    updated = HTTP_DATE.parse(Json.getAsString(root, "updated"));
                }
                catch (ParseException e)
                {
                    System.err.println("Failed to parse date, defaulting to Unix time 0");
                    e.printStackTrace(System.err);
                    updated = new Date(0);
                }
                JsonObject versions = root.get("versions").getAsJsonObject();
                MinecraftVersions mcVersions = new MinecraftVersions(updated);
                for (Entry<String, JsonElement> member : versions.entrySet())
                {
                    var version = JSON.as(member.getValue(), MinecraftVersion.class);
                    mcVersions.byId.put(member.getKey(), version);
                }
                mcVersions.sorted = new TreeSet<>(mcVersions.byId.values());
                return mcVersions;
            }
            catch (IOException e)
            {
                throw new CompletionException(e);
            }
        });
    }

    private void serialise(Path cache) throws IOException
    {
        cache = cache.toAbsolutePath();
        JsonObject root = new JsonObject();
        root.addProperty("updated", HTTP_DATE.format(Calendar.getInstance().getTime()));
        root.add("versions", JSON.writeElement(byId));
        Files.createDirectories(cache.getParent());
        try (Writer writer = Files.newBufferedWriter(cache))
        {
            JSON.writeElement(root, writer);
        }
    }
}
