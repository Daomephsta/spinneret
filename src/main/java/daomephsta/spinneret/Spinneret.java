package daomephsta.spinneret;

import java.io.IOException;
import java.nio.file.Path;

public class Spinneret
{
    public final Config configuration;
    public final FabricMeta fabricMeta;

    public Spinneret(Path configFolder)
    {
        this.configuration = Config.load(configFolder);
        this.fabricMeta = new FabricMeta(configuration);
    }

    public SpinneretArguments createArguments()
    {
        return new SpinneretArguments(this);
    }

    public void spin(SpinneretArguments args) throws IOException
    {
        args.template().generate(args);
    }
}
