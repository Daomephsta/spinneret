package daomephsta.spinneret;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class DeletingFileVisitor extends SimpleFileVisitor<Path>
{
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
        throws IOException
    {
        // Files.delete() throws AccessDeniedException on Windows for read-only files
        if (Platform.get() == Platform.WINDOWS)
            Files.setAttribute(file, "dos:readonly", false);
        Files.delete(file);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc)
        throws IOException
    {
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
    }
}
