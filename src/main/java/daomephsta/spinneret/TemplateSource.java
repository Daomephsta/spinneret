package daomephsta.spinneret;

import java.net.URL;

public interface TemplateSource
{

}

class GitTemplateSource implements TemplateSource
{
    private final URL location;
    private final String branch;

    GitTemplateSource(URL location, String branch)
    {
        this.location = location;
        this.branch = branch;
    }
}