package daomephsta.spinneret.template;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;

import daomephsta.spinneret.SpinneretArguments;
import daomephsta.spinneret.util.Json;

@JsonAdapter(value = TemplateSourceSerialiser.class, nullSafe = false)
public interface TemplateSource
{
    public void generate(SpinneretArguments spinneretArgs) throws IOException;
}

class TemplateSourceSerialiser implements JsonDeserializer<TemplateSource>
{
    @Override
    public TemplateSource deserialize(JsonElement json, Type returnType, JsonDeserializationContext context)
        throws JsonParseException
    {
        var jsonObj = json.getAsJsonObject();
        var type = Json.getAsString(jsonObj, "type");
        return switch (type)
        {
        case "local_directory":
        {
            Path path = Paths.get(Json.getAsString(jsonObj, "path"));
            Set<Path> exclude = Json.stream(jsonObj, "exclude")
                .map(p -> Paths.get(Json.asString(p)))
                .collect(toSet());
            Map<Path, liqp.Template> rename = jsonObj.has("rename")
                ? Json.getAsObject(jsonObj, "rename").entrySet().stream().collect(toMap(
                        e -> Paths.get(e.getKey()),
                        e -> liqp.Template.parse(Json.asString(e.getValue()))))
                : Collections.emptyMap();
            yield new DirectoryTemplateSource(path, exclude, rename);
        }
        default:
            throw new IllegalArgumentException("Unknown template source type: " + type);
        };
    }
}