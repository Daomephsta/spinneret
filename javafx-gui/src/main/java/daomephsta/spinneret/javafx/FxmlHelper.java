package daomephsta.spinneret.javafx;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import daomephsta.spinneret.javafx.WizardPager.WizardPage;
import javafx.fxml.FXMLLoader;

public class FxmlHelper
{
    static <T> T loadLayout(FXMLLoader fxml, String layoutName)
    {
        var layoutPath = "/layout/" + layoutName + ".fxml";
        try
        {
            InputStream layout = WizardPage.class.getResourceAsStream(layoutPath);
            Objects.requireNonNull(layout, "Missing " + layoutPath);
            return fxml.load(layout);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to load layout " + layoutPath, e);
        }
    }
}
