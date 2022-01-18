package daomephsta.spinneret.template;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;

import daomephsta.spinneret.SpinneretArguments;
import daomephsta.spinneret.util.Json;
import liqp.RenderSettings;
import liqp.Template;
import liqp.exceptions.LiquidException;

@JsonAdapter(value = CopyOperation.Serialiser.class, nullSafe = false)
public class CopyOperation
{
    private static final RenderSettings STRICT_RENDERING = new RenderSettings.Builder().withStrictVariables(true).build();
    private final Set<Path> exclude;
    private final Map<Path, Template> rename;

    private CopyOperation(Set<Path> exclude, Map<Path, Template> rename)
    {
        this.exclude = exclude;
        this.rename = rename;
    }

    void execute(Path sourceFolder, Path destinationFolder, SpinneretArguments spinneretArgs) throws IOException
    {
        Files.walkFileTree(sourceFolder, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                throws IOException
            {
                if (isExcluded(sourceFolder, dir))
                    return FileVisitResult.SKIP_SUBTREE;
                return super.preVisitDirectory(dir, attrs);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
                throws IOException
            {
                if (isExcluded(sourceFolder, file))
                    return FileVisitResult.SKIP_SUBTREE;
                Path local = sourceFolder.relativize(file);
                Path destination = getDestinationPath(destinationFolder, local, spinneretArgs);
                String localFile = local.getFileName().toString();
                String destinationFile = destination.getFileName().toString();
                InputStream content;
                if (localFile.endsWith(".liquid"))
                {
                    if (destinationFile.endsWith(".liquid"))
                    {
                        destinationFile = destinationFile.substring(0, destinationFile.length() - ".liquid".length());
                        destination = destination.getParent().resolve(destinationFile);
                    }
                    var template = parseFile(file);
                    content = new ByteArrayInputStream(template
                        .withRenderSettings(STRICT_RENDERING)
                        .render(spinneretArgs)
                        .getBytes(StandardCharsets.UTF_8));
                }
                else
                    content = Files.newInputStream(file);
                Files.createDirectories(destination.getParent());
                Files.copy(content, destination, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exception) throws IOException
            {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private Path getDestinationPath(Path destinationFolder, Path local, SpinneretArguments spinneretArgs) throws IOException
    {
        Path renamingRoot = local;
        Template renamer = null;
        while (renamingRoot != null && (renamer = rename.get(renamingRoot)) == null)
            renamingRoot = renamingRoot.getParent();
        try
        {
            Path destination = renamer == null
                ? local
                : Paths.get(renamer.withRenderSettings(STRICT_RENDERING).render(spinneretArgs))
                    .resolve(renamingRoot.relativize(local));
            destination = destinationFolder.resolve(destination);
            return destination;
        }
        catch (LiquidException | IllegalArgumentException e)
        {
            System.err.println("Failed to parse path template for " + local);
            throw e;
        }
    }

    private Template parseFile(Path file) throws IOException
    {
        try
        {
            return liqp.Template.parse(Files.newInputStream(file));
        }
        catch (LiquidException e)
        {
            System.err.println("Failed to parse " + file);
            throw e;
        }
    }

    private boolean isExcluded(Path sourceFolder, Path path)
    {
        Path relative = sourceFolder.relativize(path);
        for (Path excluded : exclude)
        {
            if (relative.startsWith(excluded))
                return true;
        }
        return false;
    }

    private static class Serialiser implements JsonDeserializer<CopyOperation>
    {
        @Override
        public CopyOperation deserialize(JsonElement json, Type returnType, JsonDeserializationContext context)
            throws JsonParseException
        {
            var jsonObj = Json.asObject(json);
            var exclusions = Json.stream(jsonObj, "exclude")
                .map(p -> Paths.get(Json.asString(p)))
                .collect(toSet());
            Map<Path, liqp.Template> renamers = jsonObj.has("rename")
                ? Json.getAsObject(jsonObj, "rename").entrySet().stream().collect(toMap(
                    e -> Paths.get(e.getKey()),
                    e -> liqp.Template.parse(Json.asString(e.getValue()))))
                    : Collections.emptyMap();
            return new CopyOperation(exclusions, renamers);
        }
    }
}