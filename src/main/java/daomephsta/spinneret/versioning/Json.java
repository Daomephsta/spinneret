package daomephsta.spinneret.versioning;

import java.io.IOException;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

public class Json
{
    public static Object next(JsonReader reader) throws IOException
    {
        return switch (reader.peek())
        {
        case BEGIN_OBJECT ->
            {
                reader.beginObject();
                yield JsonToken.BEGIN_OBJECT;
            }
        case END_OBJECT ->
            {
                reader.endObject();
                yield JsonToken.END_OBJECT;
            }
        case BEGIN_ARRAY ->
            {
                reader.beginArray();
                yield JsonToken.BEGIN_ARRAY;
            }
        case END_ARRAY ->
            {
                reader.endArray();
                yield JsonToken.END_ARRAY;
            }
        case BOOLEAN -> reader.nextBoolean();
        case NAME -> reader.nextName();
        case NULL ->
            {
                reader.nextNull();
                yield null;
            }
        case NUMBER -> reader.nextDouble();
        case STRING -> reader.nextString();
        case END_DOCUMENT -> throw new IllegalStateException("End of document");
        };
    }
}
