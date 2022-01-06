package daomephsta.spinneret;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import liqp.TemplateContext;
import liqp.filters.Filter;

public class FormatDateFilter extends Filter
{
    public FormatDateFilter()
    {
        super("format_date");
    }

    @Override
    public Object apply(Object value, TemplateContext context, Object... params)
    {
        var formatter = new SimpleDateFormat(super.asString(value, context));
        return formatter.format(Calendar.getInstance().getTime());
    }
}
