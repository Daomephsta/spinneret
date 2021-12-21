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

import daomephsta.spinneret.SpinneretArguments;
import liqp.RenderSettings;

public class DirectoryTemplateSource implements TemplateSource
{
    private final Path path;

    public DirectoryTemplateSource(Path path)
    {
        this.path = path;
    }

    @Override
    public void generate(SpinneretArguments spinneretArgs) throws IOException
    {
        Path destinationFolder = Paths.get(spinneretArgs.folderName());
        Files.walkFileTree(path, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
            {
                if (dir.endsWith(".git"))
                    return FileVisitResult.SKIP_SUBTREE;
                return super.preVisitDirectory(dir, attrs);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException
            {
                var renderSettings = new RenderSettings.Builder().withStrictVariables(true).build();
                // Template substitution
                Path destination = Paths.get(
                    liqp.Template.parse(file.toAbsolutePath().toString())
                        .withRenderSettings(renderSettings)
                        .render(spinneretArgs));
                // Make relative to destinationFolder
                destination = destinationFolder.resolve(path.relativize(destination));
                String fileName = destination.getFileName().toString();
                InputStream content;
                if (fileName.endsWith(".liquid"))
                {
                    fileName = fileName.substring(0, fileName.length() - ".liquid".length());
                    destination = destination.getParent().resolve(fileName);
                    var template = liqp.Template.parse(Files.newInputStream(file));
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
}
