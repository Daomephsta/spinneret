package daomephsta.spinneret.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonBundleLoader extends ResourceBundle.Control
{ 
    private static final List<String> FORMATS = List.of("json");
    private final String base;
    private final Set<Locale> supportedLocales;

    private JsonBundleLoader(String base, Set<Locale> supportedLocales)
    {
        this.base = base;
        this.supportedLocales = supportedLocales;
    }
    
    public static ResourceBundle load(String baseName, String baseUrl, Set<Locale> supports)
    {
        return ResourceBundle.getBundle(baseName, new JsonBundleLoader(baseUrl, supports));
    }

    @Override
    public List<String> getFormats(String baseName)
    {
        return FORMATS;
    }

    @Override
    public ResourceBundle newBundle(
        String baseName, Locale locale, String format, ClassLoader loader, boolean reload) 
        throws IllegalAccessException, InstantiationException, IOException
    {
        if (!"json".equals(format) || (!supportedLocales.contains(locale) && locale != Locale.ROOT))
            return null;
        var url = new URL(base + toBundleName(baseName, locale) + ".json");
        try (Reader lang = new InputStreamReader(url.openStream()))
        {
            var gson = new Gson();
            var root = gson.fromJson(lang, JsonObject.class);
            return new JsonResourceBundle(root);
        }
    }
    
    private static class JsonResourceBundle extends ResourceBundle
    {
        private final JsonObject json;
        
        JsonResourceBundle(JsonObject json)
        {
            this.json = json;
        }

        @Override
        protected Object handleGetObject(String key)
        {
            JsonElement element = json.get(key);
            if (element == null) 
                return null;
            return element.getAsString();
        }
        
        @Override
        public Enumeration<String> getKeys()
        {
            var keys = Collections.enumeration(json.keySet());
            if (parent != null)
                return new ConcatEnumeration<>(keys, parent.getKeys());
            return keys;
        }
    }
}
