package daomephsta.spinneret.template;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Set;

import daomephsta.spinneret.SpinneretArguments;
import liqp.RenderSettings;
import liqp.Template;
import liqp.exceptions.LiquidException;

public class DirectoryTemplateSource implements TemplateSource
{
    private static final RenderSettings STRICT_RENDERING = new RenderSettings.Builder().withStrictVariables(true).build();
    private final Path sourceDirectory;
    private final Set<Path> exclude;
    private final Map<Path, liqp.Template> rename;

    public DirectoryTemplateSource(Path path, Set<Path> exclude, Map<Path, liqp.Template> rename)
    {
        this.sourceDirectory = path;
        this.exclude = exclude;
        this.rename = rename;
    }

    @Override
    public void generate(SpinneretArguments spinneretArgs) throws IOException
    {
        Path destinationFolder = Paths.get(spinneretArgs.folderName());
        Files.walkFileTree(sourceDirectory, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                throws IOException
            {
                if (isExcluded(dir))
                    return FileVisitResult.SKIP_SUBTREE;
                return super.preVisitDirectory(dir, attrs);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
                throws IOException
            {
                if (isExcluded(file))
                    return FileVisitResult.SKIP_SUBTREE;
                var renderSettings = STRICT_RENDERING;
                Path local = sourceDirectory.relativize(file);
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
                        .withRenderSettings(renderSettings)
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

    private boolean isExcluded(Path dir)
    {
        Path relative = sourceDirectory.relativize(dir);
        for (Path excluded : exclude)
        {
            if (relative.startsWith(excluded))
                return true;
        }
        return false;
    }
}
