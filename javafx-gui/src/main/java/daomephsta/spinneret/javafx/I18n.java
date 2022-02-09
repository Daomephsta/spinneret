package daomephsta.spinneret.javafx;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javafx.fxml.FXMLLoader;

public class I18n
{
    private static final ResourceBundle STRINGS;
    static
    {
        STRINGS = ResourceBundle.getBundle("strings");
    }

    public static String get(String key, Object... args)
    {
        try
        {
            return STRINGS.getString(key).formatted(args);
        }
        catch (MissingResourceException e)
        {
            return key;
        }
    }

    public static String get(String key)
    {
        try
        {
            return STRINGS.getString(key);
        }
        catch (MissingResourceException e)
        {
            return key;
        }
    }

    public static void configureFxml(FXMLLoader loader)
    {
        loader.setResources(STRINGS);
    }
}
