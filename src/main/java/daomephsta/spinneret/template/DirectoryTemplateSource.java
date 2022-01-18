package daomephsta.spinneret.template;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import daomephsta.spinneret.SpinneretArguments;

public class DirectoryTemplateSource implements TemplateSource
{
    private final Path sourceFolder;
    private final CopyOperation copyOperation;

    public DirectoryTemplateSource(Path sourceFolder, CopyOperation copyOperation)
    {
        this.sourceFolder = sourceFolder;
        this.copyOperation = copyOperation;
    }

    @Override
    public void generate(SpinneretArguments spinneretArgs) throws IOException
    {
        Path destinationFolder = Paths.get(spinneretArgs.folderName());
        copyOperation.execute(sourceFolder, destinationFolder, spinneretArgs);
    }
}
