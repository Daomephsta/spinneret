package daomephsta.spinneret.template;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

import daomephsta.spinneret.Platform;
import daomephsta.spinneret.SpinneretArguments;

class GitTemplateSource implements TemplateSource
{
    private final URL location;
    private final String branch;
    private final CopyOperation copyOperation;

    GitTemplateSource(URL location, String branch, CopyOperation copyOperation)
    {
        this.location = location;
        this.branch = branch;
        this.copyOperation = copyOperation;
    }

    @Override
    public void generate(SpinneretArguments spinneretArgs) throws IOException
    {
        Path git = findGit();
        if (git != null)
        {
            Path working = Files.createTempDirectory("spinneret");
            try
            {
                var gitProcess = new ProcessBuilder("git", "clone",
                    "--depth", "1",
                    "--branch", branch,
                    location.toExternalForm(),
                    working.toString())
                    .directory(working.toFile())
                    .inheritIO();
                gitProcess.start().
                    onExit().get();
                Path destinationFolder = Paths.get(spinneretArgs.folderName());
                copyOperation.execute(working, destinationFolder, spinneretArgs);
            }
            catch (InterruptedException | ExecutionException | IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private static Path findGit()
    {
        String gitBinaryName = Platform.get() == Platform.WINDOWS ? "git.exe" : "git";
        for (String pathElement : System.getenv("PATH").split(File.pathSeparator))
        {
            Path candidate = Paths.get(pathElement, gitBinaryName);
            if (Files.exists(candidate))
                return candidate;
        }
        return null;
    }

    @Override
    public String toString()
    {
        return String.format("GitTemplateSource(location=%s, branch=%s)", location, branch);
    }
}