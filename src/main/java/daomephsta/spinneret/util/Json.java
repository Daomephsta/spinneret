package daomephsta.spinneret.util;

import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Json
{

    public static JsonObject withDefaults(JsonObject json, JsonObject defaults)
    {
        for (Entry<String, JsonElement> entry : defaults.entrySet())
        {
            JsonElement existing = json.get(entry.getKey());
            if (existing == null)
                json.add(entry.getKey(), entry.getValue());
            else if (existing instanceof JsonObject existingObj &&
                entry.getValue() instanceof JsonObject defaultsObj)
            {
                withDefaults(existingObj, defaultsObj);
            }
        }
        return json;
    }

}
