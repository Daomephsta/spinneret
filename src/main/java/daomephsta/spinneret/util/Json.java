package daomephsta.spinneret.util;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

public class Json
{
    private final Gson gson;

    public Json(Gson gson)
    {
        this.gson = gson;
    }

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

    public static JsonElement get(JsonObject json, String member)
    {
        if (!json.has(member))
            throw new JsonSyntaxException("Missing member " + member);
        return json.get(member);
    }

    public static String getAsString(JsonObject json, String member)
    {
        return asString(get(json, member));
    }

    public static String asString(JsonElement element)
    {
        if (element.isJsonPrimitive())
            return element.getAsString();
        throw new JsonSyntaxException("Expected JSON string, got " +
            element.getClass().getSimpleName());
    }

    public JsonElement readElement(Reader reader)
    {
        return gson.fromJson(reader, JsonElement.class);
    }

    public static JsonObject getAsObject(JsonObject json, String member)
    {
        return asObject(get(json, member));
    }

    public static JsonObject asObject(JsonElement element)
    {
        if (element.isJsonObject())
            return (JsonObject) element;
        throw new JsonSyntaxException("Expected JSON object, got " +
            element.getClass().getSimpleName());
    }

    public JsonObject readObject(Reader reader)
    {
        return asObject(readElement(reader));
    }

    public static Stream<JsonObject> streamAsObjects(JsonObject json, String member)
    {
        return stream(json, member).map(Json::asObject);
    }

    public static JsonArray getAsArray(JsonObject json, String member)
    {
        return asArray(get(json, member));
    }

    public static JsonArray asArray(JsonElement element)
    {
        if (element.isJsonArray())
            return (JsonArray) element;
        throw new JsonSyntaxException("Expected JSON array, got " +
            element.getClass().getSimpleName());
    }

    public <T> Set<T> getAsSet(JsonObject json, String member, Class<T> elementType)
    {
        if (!json.has(member))
            return Collections.emptySet();
        Type setType = TypeToken.getParameterized(Set.class, elementType).getType();
        return gson.fromJson(get(json, member), setType);
    }

    public <T> T getAs(JsonObject json, String member, Class<T> type)
    {
        return as(get(json, member), type);
    }

    public <T> T as(JsonElement json, Class<T> type)
    {
        return gson.fromJson(json, type);
    }

    public static Stream<JsonElement> stream(JsonObject json, String member)
    {
        var array = getAsArray(json, member);
        return StreamSupport.stream(array.spliterator(), false);
    }

    public <T> Stream<T> streamAs(JsonObject json, String member, Class<T> elementType)
    {
        var array = getAsArray(json, member);
        return StreamSupport.stream(array.spliterator(), false)
            .map((Function<JsonElement, T>) element -> gson.fromJson(element, elementType));
    }

    public JsonElement writeElement(Object object)
    {
        return gson.toJsonTree(object);
    }

    public void writeElement(JsonObject json, Writer writer)
    {
        gson.toJson(json, writer);
    }
}
