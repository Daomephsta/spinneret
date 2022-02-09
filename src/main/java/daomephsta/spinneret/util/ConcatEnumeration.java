package daomephsta.spinneret.util;

import java.util.Enumeration;

public class ConcatEnumeration<E> implements Enumeration<E>
{
    private final Enumeration<E>[] delegates;
    private int current = 0;
    
    @SafeVarargs
    public ConcatEnumeration(Enumeration<E>... delegates)
    {
        this.delegates = delegates;
    }

    @Override
    public boolean hasMoreElements()
    {
        for (int i = 0; i < delegates.length && !delegates[current].hasMoreElements(); i++);
        return delegates[current].hasMoreElements();
    }

    @Override
    public E nextElement()
    {
        return delegates[current].nextElement();
    }
}
