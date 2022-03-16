package daomephsta.spinneret.javafx;

import java.util.function.Consumer;

import daomephsta.spinneret.SpinneretArguments;
import daomephsta.spinneret.javafx.WizardPager.WizardPage;
import daomephsta.spinneret.template.TemplateVariable;
import daomephsta.spinneret.template.TemplateVariable.Type;
import javafx.beans.property.Property;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

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
    
    private static class ListValue
    {
        @FXML
        Button add, remove, moveUp, moveDown;
        @FXML
        Pane controls;
        @FXML
        ListView<Object> values;
        Property<Object> valueProperty;
        
        @FXML
        private void handleButtons(Event event)
        {
            if (event.getSource() == add)
                values.getItems().add(valueProperty.getValue());
            else if (!values.getSelectionModel().isEmpty())
            {
                if (event.getSource() == remove)
                    values.getItems().remove(values.getSelectionModel().getSelectedItem());
                else if (event.getSource() == moveUp)
                {
                    int selectedIndex = values.getSelectionModel().getSelectedIndex();
                    if (selectedIndex - 1 < 0) return;
                    Object selected = values.getItems().remove(selectedIndex);
                    values.getItems().add(selectedIndex - 1, selected);
                }
                else if (event.getSource() == moveDown)
                {
                    int selectedIndex = values.getSelectionModel().getSelectedIndex();
                    if (selectedIndex + 1 == values.getItems().size()) return;
                    Object selected = values.getItems().remove(selectedIndex);
                    values.getItems().add(selectedIndex + 1, selected);
                }
            }
        }
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
           Node valueControl;
           if (variable.type instanceof Type.Array)
           {
               var fxml = new FXMLLoader();
               var controller = new ListValue();
               valueControl = createValueControl(variable, property -> 
               {
                   @SuppressWarnings("unchecked")
                   Property<Object> cast = (Property<Object>) property;
                   controller.valueProperty = cast;
               });
               fxml.setController(controller);
               Node listValue = FxmlHelper.loadLayout(fxml, "list-value");
               controller.controls.getChildren().add(0, valueControl);
               bind(variable, controller.values.itemsProperty());
               valueControl = listValue;
           }
           else 
               valueControl = createValueControl(variable, property -> bind(variable, property));
           Tooltip.install(valueControl, new Tooltip(variable.tooltip));
           templateVariables.add(valueControl, 1, row);
           row += 1;
        }
    }

    private Control createValueControl(TemplateVariable variable, Consumer<Property<?>> binding)
    {
        Type.Primitive type = variable.type instanceof Type.Array array 
            ? array.elementType() : (Type.Primitive) variable.type;
        return switch (type)
        {
            case STRING -> createDefaultedControl(variable, binding);
            case NUMBER -> createDefaultedControl(variable, binding);
            case BOOLEAN ->
            {
                var checkBox = new CheckBox();
                binding.accept(checkBox.selectedProperty());
                yield checkBox;
            }
            default ->
            throw new IllegalArgumentException("Unexpected value: " + variable.type);
        };
    }

    private Control createDefaultedControl(TemplateVariable variable, Consumer<Property<?>> binding)
    {
        return switch (variable.defaults.size())
        {
            case 0 -> 
            {
                var textField = new TextField();
                binding.accept(textField.textProperty());
                yield textField;
            }
            case 1 -> 
            {
                var textField = new TextField(variable.defaults.iterator().next());
                binding.accept(textField.textProperty());
                yield textField;
            }
            default -> 
            {
                var combo = new ComboBox<>();
                combo.getItems().setAll(variable.defaults);
                combo.getSelectionModel().selectFirst();
                binding.accept(combo.valueProperty());
                yield combo;
            }
        };
    }

    private void bind(TemplateVariable variable, Property<?> property)
    {
        property.addListener((observable, oldValue, newValue) -> 
               spinneretArgs.templateValues.put(variable.name, newValue));
    }
}