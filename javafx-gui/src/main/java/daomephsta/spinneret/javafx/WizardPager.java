package daomephsta.spinneret.javafx;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

import daomephsta.spinneret.SpinneretArguments;
import daomephsta.spinneret.SpinneretArguments.InvalidArgumentException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.StackPane;

public class WizardPager
{
    private final SpinneretArguments spinneretArgs;
    private final List<String> pageOrder;
    private final NavigableMap<String, WizardPage> pages;
    private final StackPane content = new StackPane();
    private String currentPageName;

    public WizardPager(SpinneretArguments spinneretArgs, WizardPage... pages)
    {
        this.spinneretArgs = spinneretArgs;
        this.pageOrder = Arrays.stream(pages)
            .filter(page -> !page.special)
            .map(page -> page.name)
            .toList();
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
        for (WizardPage page : pages)
        {
            this.pages.put(page.name, page);
            page.content.setVisible(false);
            content.getChildren().add(page.content);
        }
        this.currentPageName = pageOrder.get(0);
        showPage(currentPageName);
    }

    public Node getContent()
    {
        return content;
    }

    public void showPage(String name)
    {
        pages.get(currentPageName).content.setVisible(false);
        WizardPage page = pages.get(name);
        if (page == null)
            throw new IllegalArgumentException("Unknown page " + name);
        if (!page.setUp)
        {
            page.setUp = true;
            page.setupContent();
        }
        page.content.setVisible(true);
        this.currentPageName = name;
    }

    public void next()
    {
        if (!applyPage())
            return;
        String next = pages.higherKey(currentPageName);
        if (next != null && pageOrder.indexOf(next) != -1)
            showPage(next);
    }

    public boolean hasNext()
    {
        String next = pages.higherKey(currentPageName);
        return next != null && pageOrder.indexOf(next) != -1;
    }

    public boolean applyPage()
    {
        try
        {
            pages.get(currentPageName).apply(spinneretArgs);
        }
        catch (InvalidArgumentException e)
        {
            var errorDialog = new Dialog<>();
            errorDialog.setTitle("Error");
            errorDialog.setContentText(e.getMessage());
            errorDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            errorDialog.showAndWait();
            return false;
        }
        return true;
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

    public static class WizardPage
    {
        private final String name;
        private final Node content;
        private final boolean special;
        private boolean setUp = false;

        public WizardPage(String name)
        {
            this(name, false);
        }

        public WizardPage(String name, boolean special)
        {
            this.name = name;
            this.content = createContent();
            this.special = special;
        }

        private Node createContent()
        {
            var fxml = new FXMLLoader();
            fxml.setController(this);
            var layoutPath = "/layout/" + name + ".fxml";
            try
            {
                InputStream layout = getClass().getResourceAsStream(layoutPath);
                Objects.requireNonNull(layout, "Missing " + layoutPath);
                return fxml.load(layout);
            }
            catch (IOException e)
            {
                throw new RuntimeException("Failed to load layout " + layoutPath, e);
            }
        }

        protected void setupContent() {}

        void apply(SpinneretArguments spinneretArgs) throws InvalidArgumentException {}
    }
}
