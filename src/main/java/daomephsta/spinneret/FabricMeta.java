package daomephsta.spinneret;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import daomephsta.spinneret.util.Json;
import daomephsta.spinneret.versioning.MinecraftVersion;

public class FabricMeta
{
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Json JSON = new Json(new Gson());

    public static CompletableFuture<Collection<String>>
        getYarnVersionsFor(MinecraftVersion version)
    {
        return getVersions(Spinneret.configuration().urls().yarnVersionsFor(version),
            reader -> JSON.streamAs(JSON.readArray(reader), JsonObject.class)
                .map(e -> Json.getAsString(e, "version"))
                .toList());
    }

    public static CompletableFuture<Collection<String>>
        getFabricLoaderVersionsFor(MinecraftVersion version)
    {
        return getVersions(Spinneret.configuration().urls().fabricLoaderVersionsFor(version),
            reader -> JSON.streamAs(JSON.readArray(reader), JsonObject.class)
                .map(e -> Json.getAsString(Json.getAsObject(e, "loader"), "version"))
                .toList());
    }

    public static CompletableFuture<Collection<String>>
        getFabricApiVersionsFor(MinecraftVersion version)
    {
        return getVersions(Spinneret.configuration().urls().fabricApiVersions,
            reader -> JSON.streamAs(JSON.readArray(reader), JsonObject.class)
                .filter(e -> JSON.getAsSet(e, "game_versions", String.class).contains(version.raw))
                .map(e -> Json.getAsString(e, "version_number"))
                .toList());
    }

    private static CompletableFuture<Collection<String>>
        getVersions(URI versionsUrl, Function<Reader, List<String>> parser)
    {
        var request = HttpRequest.newBuilder(versionsUrl).GET().build();
        return CLIENT.sendAsync(request, BodyHandlers.ofInputStream()).thenApply(response ->
        {
            switch (response.statusCode())
            {
            case 200 /*OK*/ ->
            {
                try (Reader reader = new InputStreamReader(response.body()))
                {
                    return parser.apply(reader);
                }
                catch (IOException io)
                {
                    io.printStackTrace(System.err);
                    return Collections.emptyList();
                }
            }
            default -> throw new IllegalStateException("Unexpected status code " + response.statusCode());
            }
        });
    }
}
