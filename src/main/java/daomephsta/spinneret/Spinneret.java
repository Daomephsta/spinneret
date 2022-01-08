package daomephsta.spinneret;

import java.io.IOException;

public class Spinneret
{
    private static Config configuration = null;

    public static void spin(SpinneretArguments args) throws IOException
    {
        args.template().generate(args);
    }

    public static Config configuration()
    {
        if (configuration == null)
        {
            System.out.println("Loading configuration");
            configuration = Config.load();
        }
        return configuration;
    }
}
