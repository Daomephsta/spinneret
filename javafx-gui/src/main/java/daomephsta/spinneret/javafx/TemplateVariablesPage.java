package daomephsta.spinneret.javafx;


import daomephsta.spinneret.SpinneretArguments;
import daomephsta.spinneret.javafx.WizardPager.WizardPage;
import daomephsta.spinneret.template.TemplateVariable;
import javafx.beans.property.Property;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;

class TemplateVariablesPage extends WizardPage
{
    private final SpinneretArguments spinneretArgs;
    @FXML
    private GridPane templateVariables;
    
    public TemplateVariablesPage(SpinneretArguments spinneretArgs)
    {
        super("template-variables");
        this.spinneretArgs = spinneretArgs;
    }

    @Override
    protected void setupContent()
    {
        if (spinneretArgs.getTemplateVariables().isEmpty())
        {
            templateVariables.add(new Label(I18n.get("variablesPage.noVariables")), 0, 0);
            return;
        }
        
        int row = 0;
        for (TemplateVariable variable : spinneretArgs.getTemplateVariables().values())
        {
           templateVariables.add(new Label(variable.display), 0, row);
           Node valueControl = switch (variable.type)
           {
               case STRING -> createDefaultedControl(variable);
               case NUMBER -> createDefaultedControl(variable);
               case BOOLEAN ->
               {
                   var checkBox = new CheckBox();
                   bind(variable, checkBox.selectedProperty());
                   yield checkBox;
               }
               default ->
                   throw new IllegalArgumentException("Unexpected value: " + variable.type);
           };
           Tooltip.install(valueControl, new Tooltip(variable.tooltip));
           templateVariables.add(valueControl, 1, row);
           row += 1;
        }
    }

    private Node createDefaultedControl(TemplateVariable variable)
    {
        Node control = switch (variable.defaults.size())
        {
            case 0 -> 
            {
                var textField = new TextField();
                bind(variable, textField.textProperty());
                yield textField;
            }
            case 1 -> 
            {
                var textField = new TextField(variable.defaults.iterator().next());
                bind(variable, textField.textProperty());
                yield textField;
            }
            default -> 
            {
                var combo = new ComboBox<>();
                combo.getItems().setAll(variable.defaults);
                combo.getSelectionModel().selectFirst();
                bind(variable, combo.valueProperty());
                yield combo;
            }
        };
        return control;
    }

    private void bind(TemplateVariable variable, Property<?> property)
    {
        property.addListener((observable, oldValue, newValue) -> 
               spinneretArgs.templateValues.put(variable.name, newValue));
    }
}