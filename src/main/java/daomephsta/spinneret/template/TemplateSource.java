package daomephsta.spinneret.template;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;

@JsonAdapter(value = TemplateSourceSerialiser.class, nullSafe = false)
public interface TemplateSource
{
    public void generate() throws IOException;
}

class TemplateSourceSerialiser implements JsonDeserializer<TemplateSource>
{
    @Override
    public TemplateSource deserialize(JsonElement json, Type returnType, JsonDeserializationContext context)
        throws JsonParseException
    {
        var jsonObj = json.getAsJsonObject();
        var type = jsonObj.get("type").getAsString();
        return switch (type)
        {
        case "local_directory":
        {
            Path path = context.deserialize(jsonObj.get("path"), Path.class);
            yield new DirectoryTemplateSource(path);
        }
        default:
            throw new IllegalArgumentException("Unknown template source type: " + type);
        };
    }
}