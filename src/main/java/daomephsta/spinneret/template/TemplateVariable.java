package daomephsta.spinneret.template;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import daomephsta.spinneret.util.Json;

public class TemplateVariable
{
    private static final Json JSON = new Json(new Gson());
    public final String name, display, tooltip;
    public sealed interface Type
    {
        public enum Primitive implements Type {STRING, NUMBER, BOOLEAN}
        public record Array(Type.Primitive elementType) implements Type {}
    }
    public final Type type;
    public final Predicate<Object> condition;
    public final Set<String> defaults;
    
    private TemplateVariable(String name, String display, String tooltip, 
        Type type, Predicate<Object> condition, Set<String> defaults)
    {
        this.name = name;
        this.display = display;
        this.tooltip = tooltip;
        this.type = type;
        this.condition = condition;
        this.defaults = defaults;
    }

    public static Map<String, TemplateVariable> readVariables(JsonObject variablesJson, UnaryOperator<String> translations)
    {   
        return variablesJson.entrySet().stream().collect(toMap(Entry::getKey, e -> 
            {
                var json = Json.asObject(e.getValue());
                var type = readType(json);

                Predicate<Object> condition;
                if (type instanceof Type.Primitive primitive)
                    condition = readCondition(primitive, json);
                else if (type instanceof Type.Array array)
                    condition = readArrayConditions(json, array.elementType);
                else
                    throw new IllegalStateException("Unexpected type " + type);
                
                Set<String> defaults = json.has("values") 
                    ? JSON.getAsSet(json, "values", String.class)
                    : Collections.emptySet();
                return new TemplateVariable(e.getKey(),
                    translations.apply(e.getKey() + ".display"),
                    translations.apply(e.getKey() + ".tooltip"),
                    type, condition, defaults);
            }));
    }

    private static Predicate<Object> readCondition(Type.Primitive type, JsonObject json)
    {
        return switch (type)
        {
        case STRING -> 
        {
            if (json.has("values") && json.has("regex"))
                throw new JsonSyntaxException("Cannot specify both 'values' and 'regex'");
            Predicate<Object> conditions = string -> true;
            if (json.has("values"))
                conditions = conditions.and(valuesCondition(json, JsonElement::getAsString));
            if (json.has("regex"))
            {
                var pattern = Pattern.compile(Json.getAsString(json, "regex"));
                conditions = conditions.and(string -> pattern.matcher((String) string).matches());
            }
            yield conditions;
        }
        case NUMBER -> 
        {
            if (json.has("values"))
                yield valuesCondition(json, JsonElement::getAsNumber);
            yield number -> true;
        }
        case BOOLEAN -> bool -> true;
        };
    }
    
    @SuppressWarnings("unchecked")
    private static Predicate<Object> readArrayConditions(JsonObject json, Type.Primitive elementType)
    {
        Predicate<Object> conditions = array -> true;
        if (json.has("size"))
        {
            int expectedSize = Json.getAsInt(json, "size");
            conditions = conditions.and(array -> ((List<Object>) array).size() == expectedSize);
        }
        conditions = conditions.and(allMatch(readCondition(elementType, json)));
        return conditions;
    }

    @SuppressWarnings("unchecked")
    private static Predicate<Object> allMatch(Predicate<Object> condition)
    {
        return array -> 
        {
            for (Object element : (List<Object>) array)
            {
                if (!condition.test(element))
                    return false;
            }
            return true;
        };
    }

    private static <T> Predicate<T> valuesCondition(JsonObject json, Function<JsonElement, T> function)
    {
        var values = Json.stream(Json.getAsArray(json, "values"))
            .map(function)
            .collect(toSet());
        return values::contains;
    }

    private static Type readType(JsonObject json)
    {
        var type = Json.getAsString(json, "type");
        try
        {
            if (type.endsWith("_array"))
            {
                String elementType = type.substring(0, type.length() - "_array".length());
                return new Type.Array(Type.Primitive.valueOf(elementType.toUpperCase(Locale.ROOT)));
            }
            return Type.Primitive.valueOf(type.toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException e)
        {
            throw new JsonSyntaxException("Invalid variable type " + type);
        }
    }
}
