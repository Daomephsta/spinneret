package daomephsta.spinneret.template;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
            CopyOperation copyOperation = context.deserialize(json, CopyOperation.class);
            yield new DirectoryTemplateSource(path, copyOperation);
        }
        case "git":
        {
            CopyOperation copyOperation = context.deserialize(json, CopyOperation.class);
            yield new GitTemplateSource(readRepositoryUrl(jsonObj),
                Json.getAsString(jsonObj, "branch"), copyOperation);
        }
        default:
            throw new IllegalArgumentException("Unknown template source type: " + type);
        };
    }

    private URL readRepositoryUrl(JsonObject jsonObj) throws JsonParseException
    {
        var repository = Json.getAsString(jsonObj, "repository");
        try
        {
            return new URL(repository);
        }
        catch (MalformedURLException e)
        {
            throw new JsonParseException("Failed to parse repository " + repository, e);
        }
    }
}