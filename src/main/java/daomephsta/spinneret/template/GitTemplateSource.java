package daomephsta.spinneret.template;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

import daomephsta.spinneret.DeletingFileVisitor;
import daomephsta.spinneret.Platform;
import daomephsta.spinneret.SpinneretArguments;

class GitTemplateSource implements TemplateSource
{
    private final URL location;
    private final String branch;

    GitTemplateSource(URL location, String branch)
    {
        this.location = location;
        this.branch = branch;
    }

    @Override
    public void generate(SpinneretArguments arguments) throws IOException
    {
        Path git = findGit();
        if (git != null)
        {
            Path working = Files.createTempDirectory("spinneret");
            try
            {
                new ProcessBuilder("git", "clone",
                    "--depth", "1",
                    "--branch", branch,
                    location.toExternalForm(),
                    working.toString())
                    .directory(working.toFile())
                    .inheritIO()
                    .start().
                    onExit().get();
            }
            catch (InterruptedException | ExecutionException | IOException e)
            {
                e.printStackTrace();
            }
            Files.walkFileTree(working.resolve(".git"), new DeletingFileVisitor());
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