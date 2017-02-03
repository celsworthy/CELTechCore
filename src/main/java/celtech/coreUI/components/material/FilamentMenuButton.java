package celtech.coreUI.components.material;

import celtech.Lookup;
import celtech.roboxbase.MaterialType;
import celtech.roboxbase.configuration.Filament;
import celtech.roboxbase.configuration.datafileaccessors.FilamentContainer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

/**
 *
 * @author Ian
 */
public class FilamentMenuButton extends MenuButton implements FilamentSelectionListener
{

    private SelectedFilamentDisplayNode filamentDisplayNode = new SelectedFilamentDisplayNode();
    private FilamentOnReelDisplay filamentOnReelDisplayNode = new FilamentOnReelDisplay();
    private FilamentSelectionListener filamentSelectionListener = null;
    private SpecialItemSelectionListener specialItemSelectionListener = null;
    private boolean dontDisplayDuplicateNamedFilaments = false;

    private Map<String, CustomMenuItem> permanentMenuItems = new TreeMap<>();
    private Map<String, Filament> permanentMenuFilaments = new HashMap<>();

    private static final String roboxCategoryPrefix = "Robox";
    private static final String customCategoryPrefix = "Custom";

    protected static Comparator<Filament> byCategory = ((Filament o1, Filament o2) ->
    {
        int comparisonStatus = o1.getCategory().compareTo(o2.getCategory());
        if (comparisonStatus > 0
                && (o1.getCategory().startsWith(roboxCategoryPrefix)
                && !o2.getCategory().startsWith(roboxCategoryPrefix))
                || (!o1.getCategory().startsWith(customCategoryPrefix)
                && o2.getCategory().startsWith(customCategoryPrefix)))
        
        {
            comparisonStatus = -1;
        } else if (comparisonStatus < 0
                && (!o1.getCategory().startsWith(roboxCategoryPrefix)
                && o2.getCategory().startsWith(roboxCategoryPrefix))
                || (o1.getCategory().startsWith(customCategoryPrefix)
                && !o2.getCategory().startsWith(customCategoryPrefix)))
        {
            comparisonStatus = 1;
        }
        return comparisonStatus;
    });

    protected static Comparator<String> byCategoryName = ((String o1, String o2) ->
    {
        int comparisonStatus = o1.compareTo(o2);
        if (comparisonStatus > 0
                && (o1.startsWith(roboxCategoryPrefix)
                && !o2.startsWith(roboxCategoryPrefix))
                || (!o1.startsWith(customCategoryPrefix)
                && o2.startsWith(customCategoryPrefix)))
        {
            comparisonStatus = -1;
        } else if (comparisonStatus < 0
                && (!o1.startsWith(roboxCategoryPrefix)
                && o2.startsWith(roboxCategoryPrefix))
                || (o1.startsWith(customCategoryPrefix)
                && !o2.startsWith(customCategoryPrefix)))
        {
            comparisonStatus = 1;
        }
        return comparisonStatus;
    });

    public FilamentMenuButton()
    {
        setGraphic(filamentDisplayNode);
        getStyleClass().add("filament-menu-button");

        Lookup.getUserPreferences().advancedModeProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
        {
            repopulateFilaments();
        });

        FilamentContainer.getInstance().getUserFilamentList().addListener(
                (ListChangeListener.Change<? extends Filament> c) ->
        {
            repopulateFilaments();
        });
    }

    public void initialiseButton(FilamentSelectionListener filamentSelectionListener,
            SpecialItemSelectionListener specialItemSelectionListener,
            boolean dontDisplayDuplicateNamedFilaments)
    {
        this.filamentSelectionListener = filamentSelectionListener;
        this.specialItemSelectionListener = specialItemSelectionListener;
        this.dontDisplayDuplicateNamedFilaments = dontDisplayDuplicateNamedFilaments;
        repopulateFilaments();
        displayFirstFilament();
    }

    private void addSeparator()
    {
        SeparatorMenuItem separator = new SeparatorMenuItem();
        getItems().add(separator);
    }

    private void repopulateFilaments()
    {
        List<String> allTheFilamentNamesIHaveEverLoaded = new ArrayList<>();

        Map<String, Map<MaterialType, List<Filament>>> filamentMap = FilamentContainer.getInstance().getCompleteFilamentList()
                .stream()
                .collect(Collectors.groupingBy(Filament::getCategory, () -> new TreeMap(byCategoryName), Collectors.groupingBy(Filament::getMaterial)));

        if (dontDisplayDuplicateNamedFilaments)
        {
            for (Map.Entry<String, Map<MaterialType, List<Filament>>> categoryEntry : filamentMap.entrySet())
            {
                for (Map.Entry<MaterialType, List<Filament>> materialEntry : categoryEntry.getValue().entrySet())
                {
                    List<Filament> filamentsToDelete = new ArrayList<>();
                    for (Filament filament : materialEntry.getValue())
                    {
                        if (allTheFilamentNamesIHaveEverLoaded.contains(filament.getFriendlyFilamentName()))
                        {
                            filamentsToDelete.add(filament);
                        }
                        allTheFilamentNamesIHaveEverLoaded.add(filament.getFriendlyFilamentName());
                    }
                    filamentsToDelete.forEach((filament) ->
                    {
                        materialEntry.getValue().remove(filament);
                    });
                }
            }
        }

        getItems().clear();

        boolean firstItem = true;

        for (Map.Entry<String, CustomMenuItem> permanentMenuItem : permanentMenuItems.entrySet())
        {
            if (!firstItem)
            {
                addSeparator();
            }
            getItems().add(permanentMenuItem.getValue());
            firstItem = false;
        }

        for (Map.Entry<String, Map<MaterialType, List<Filament>>> entry : filamentMap.entrySet())
        {
            if (!firstItem)
            {
                addSeparator();
            }
            String category = entry.getKey();
            Map<MaterialType, List<Filament>> materialMap = entry.getValue();
            FilamentCategory filCat = new FilamentCategory(this);
            filCat.setCategoryData(category, materialMap);
            FilamentCategoryMenuItem filCatMenuItem = new FilamentCategoryMenuItem(filCat);
            filCatMenuItem.setHideOnClick(false);
            getItems().add(filCatMenuItem);
            firstItem = false;
        }
    }

    public void displayFirstFilament()
    {
        Filament firstFilament = null;

        for (MenuItem menuItem : getItems())
        {
            if (menuItem instanceof CustomMenuItem)
            {
                Iterator<Entry<String, CustomMenuItem>> permanentMenuItemIterator = permanentMenuItems.entrySet().iterator();
                while (permanentMenuItemIterator.hasNext())
                {
                    Entry<String, CustomMenuItem> foundItem = permanentMenuItemIterator.next();
                    if (foundItem.getValue() == menuItem)
                    {
                        displaySpecialItemOnButton(foundItem.getKey());
                        break;
                    }
                }
                break;
            } else if (menuItem instanceof FilamentCategoryMenuItem)
            {
                FilamentCategoryMenuItem filCatMenuItem = (FilamentCategoryMenuItem) menuItem;
                FilamentCategory filCat = (FilamentCategory) filCatMenuItem.getContent();
                Iterator<Entry<MaterialType, List<Filament>>> iterator = filCat.getFilamentMap().entrySet().iterator();
                if (iterator.hasNext())
                {
                    List<Filament> availableFilaments = iterator.next().getValue();
                    if (availableFilaments.size() > 0)
                    {
                        firstFilament = availableFilaments.get(0);
                    }
                }
                break;
            }
        }

        if (firstFilament != null)
        {
            displayFilamentOnButton(firstFilament);
        }
    }

    public void displayFilamentOnButton(Filament filamentToDisplay)
    {
        filamentDisplayNode.updateSelectedFilament(filamentToDisplay);
        setGraphic(filamentDisplayNode);
    }

    public void displaySpecialItemOnButton(String title)
    {
        filamentOnReelDisplayNode.updateFilamentOnReelDisplay(title, permanentMenuFilaments.get(title));
        setGraphic(filamentOnReelDisplayNode);
    }

    public Filament getCurrentlyDisplayedFilament()
    {
        return filamentDisplayNode.getSelectedFilament();
    }

    public void deleteSpecialMenuItem(String title)
    {
        permanentMenuItems.remove(title);
        permanentMenuFilaments.remove(title);
        repopulateFilaments();
    }

    public void addSpecialMenuItem(String title, Filament filament)
    {
        FilamentOnReelDisplay filamentOnReelDisplay = new FilamentOnReelDisplay(title, filament);
        filamentOnReelDisplay.setPrefWidth(getPrefWidth());
        filamentOnReelDisplay.setMaxWidth(USE_PREF_SIZE);
        CustomMenuItem newMenuItem = new CustomMenuItem(filamentOnReelDisplay, false);
        newMenuItem.setOnAction((event) ->
        {
            specialItemSelectedAction(title);
        });
        permanentMenuItems.put(title, newMenuItem);
        permanentMenuFilaments.put(title, filament);
        repopulateFilaments();
    }

    private void specialItemSelectedAction(String title)
    {
        displaySpecialItemOnButton(title);
        specialItemSelectionListener.specialItemSelected(title);
        hide();
    }

    private void filamentSelectedAction(Filament filament)
    {
        displayFilamentOnButton(filament);
        filamentSelectionListener.filamentSelected(filament);
        hide();
    }

    //Proxy the filament selection from the swatch
    @Override
    public void filamentSelected(Filament filament)
    {
        filamentSelectedAction(filament);
    }
}
