package daomephsta.spinneret;

import java.util.Locale;

public enum Platform
{
    WINDOWS,
    LINUX,
    MAC_OS,
    UNKNOWN;

    private static Platform cache;

    public static Platform get()
    {
        if (cache == null)
        {
            String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            if (osName.contains("windows"))
                cache = WINDOWS;
            else if (osName.contains("linux"))
                cache = LINUX;
            else if (osName.contains("mac os"))
                cache = MAC_OS;
            else
            {
                cache = UNKNOWN;
            }
        }
        return cache;
    }
}
