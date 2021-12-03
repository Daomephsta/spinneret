package daomephsta.spinneret.template;

import java.io.IOException;
import java.nio.file.Path;

public class DirectoryTemplateSource implements TemplateSource
{
    private final Path path;

    public DirectoryTemplateSource(Path path)
    {
        this.path = path;
    }

    @Override
    public void generate() throws IOException
    {

    }
}
