package daomephsta.spinneret.template;

import liqp.TemplateContext;
import liqp.filters.Filter;

public class PascalCaseFilter extends Filter
{
    public PascalCaseFilter()
    {
        super("pascalcase");
    }

    @Override
    public Object apply(Object value, TemplateContext context, Object... params)
    {
        var s = asString(value, context);
        if (s.isEmpty())
            return s;
        var pascalCased = new StringBuilder(s.length());
        pascalCased.append(Character.toUpperCase(s.charAt(0)));
        for (int i = 1; i < s.length(); i++)
        {
            char prev = s.charAt(i - 1),
                 current = s.charAt(i);
            if (isSeparator(prev))
                pascalCased.append(Character.toUpperCase(current));
            else if (!isSeparator(current))
                pascalCased.append(Character.toLowerCase(current));
        }
        return pascalCased.toString();
    }

    private boolean isSeparator(char prev)
    {
        return prev == ' ' || prev == '_' || prev == '-';
    }
}
