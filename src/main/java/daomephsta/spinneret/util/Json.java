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
import com.google.gson.JsonPrimitive;
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
    
    public static int getAsInt(JsonObject json, String member)
    {
        return asInt(get(json, member));
    }

    public static String getAsString(JsonObject json, String member)
    {
        return asString(get(json, member));
    }
    
    public static int asInt(JsonElement element)
    {
        if (element instanceof JsonPrimitive primitive && primitive.isNumber())
            return primitive.getAsInt();
        throw new JsonSyntaxException("Expected JSON integer, got " + element);
    }

    public static String asString(JsonElement element)
    {
        if (element.isJsonPrimitive())
            return element.getAsString();
        throw new JsonSyntaxException("Expected JSON string, got " +
            element.getClass().getSimpleName());
    }
    
    public static Object asPrimitive(JsonElement element)
    {
        if (element instanceof JsonPrimitive primitive)
        {
            if (primitive.isString())
                return primitive.getAsString();
            else if (primitive.isNumber())
                return primitive.getAsNumber();
            else if (primitive.isBoolean())
                return primitive.getAsBoolean();
        }
        throw new JsonSyntaxException("Expected JSON primitive, got " +
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

    public JsonArray readArray(Reader reader)
    {
        return asArray(readElement(reader));
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
        return stream(getAsArray(json, member));
    }

    public <T> Stream<T> streamAs(JsonObject json, String member, Class<T> elementType)
    {
        return streamAs(getAsArray(json, member), elementType);
    }

    public static Stream<JsonElement> stream(JsonArray array)
    {
        return StreamSupport.stream(array.spliterator(), false);
    }

    public <T> Stream<T> streamAs(JsonArray array, Class<T> elementType)
    {
        return stream(array).map((Function<JsonElement, T>) element -> gson.fromJson(element, elementType));
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
