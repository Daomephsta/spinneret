package daomephsta.spinneret.template;

import com.google.gson.Gson;

import liqp.TemplateContext;
import liqp.filters.Filter;

public class JsonFilter extends Filter
{
    private static final Gson JSON = new Gson();

    public JsonFilter()
    {
        super("json");
    }

    @Override
    public Object apply(Object value, TemplateContext context, Object... params)
    {
        return JSON.toJson(value);
    }
}
