package daomephsta.spinneret.swing;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.awt.Window;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import daomephsta.spinneret.SpinneretArguments;
import daomephsta.spinneret.SpinneretArguments.InvalidArgumentException;

public class WizardPager
{
    private final JPanel content;
    private final Window window;
    private final SpinneretArguments spinneretArgs;
    private final List<String> pageOrder;
    private final NavigableMap<String, WizardPage<?>> pages;
    private String currentPageName;

    public WizardPager(Window window, SpinneretArguments spinneretArgs, String... pageOrder)
    {
        this.content = new JPanel(new CardLayout());
        this.window = window;
        this.spinneretArgs = spinneretArgs;
        this.pageOrder = List.of(pageOrder);
        this.currentPageName = pageOrder[0];
        this.pages = new TreeMap<>((a, b) ->
        {
            int aIndex = this.pageOrder.indexOf(a),
                bIndex = this.pageOrder.indexOf(b);
            if (aIndex == -1)
                return bIndex == -1 ? 0 : -1;
            if (bIndex == -1)
                return aIndex == -1 ? 0 : 1;
            return Integer.compare(aIndex, bIndex);
        });
    }

    public void add(String name, WizardPage<?> page)
    {
        pages.put(name, page);
        content.add(name, page);
    }

    public void showPage(String name)
    {
        ((CardLayout) content.getLayout()).show(content, name);
        this.currentPageName = name;
        window.pack();
    }

    public void next()
    {
        try
        {
            pages.get(currentPageName).apply(spinneretArgs);
        }
        catch (InvalidArgumentException e)
        {
            JOptionPane.showMessageDialog(window, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        String next = pages.higherKey(currentPageName);
        if (next != null && pageOrder.indexOf(next) != -1)
            showPage(next);
    }

    public boolean hasNext()
    {
        String next = pages.higherKey(currentPageName);
        return next != null && pageOrder.indexOf(next) != -1;
    }

    public void previous()
    {
        String previous = pages.lowerKey(currentPageName);
        if (previous != null && pageOrder.indexOf(previous) != -1)
            showPage(previous);
    }

    public boolean hasPrevious()
    {
        String previous = pages.lowerKey(currentPageName);
        return previous != null && pageOrder.indexOf(previous) != -1;
    }

    public static class WizardPage<L> extends JPanel
    {
        private final Supplier<L> defaultLayoutConstraints;

        WizardPage()
        {
            this(new FlowLayout(), () -> null);
        }

        WizardPage(LayoutManager layout, Supplier<L> defaultLayoutConstraints)
        {
            super(layout);
            this.defaultLayoutConstraints = defaultLayoutConstraints;
        }

        protected JButton button(BiConsumer<JButton, L> config)
        {
            return add(config, new JButton());
        }

        protected <E> JComboBox<E> combo(BiConsumer<JComboBox<E>, L> config)
        {
            return add(config, new JComboBox<>());
        }

        protected JLabel label(String text, BiConsumer<JLabel, L> config)
        {
            return add(config, new JLabel(text));
        }

        protected <C extends Component> C add(BiConsumer<C, L> config, C component)
        {
            var constraints = defaultLayoutConstraints.get();
            config.accept(component, constraints);
            add(component, constraints);
            return component;
        }

        public void apply(SpinneretArguments spinneretArgs) throws InvalidArgumentException {}
    }

    public void attach(Container parent, Object constraints)
    {
        parent.add(content, constraints);
    }
}
