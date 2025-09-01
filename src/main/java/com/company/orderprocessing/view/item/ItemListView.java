package com.company.orderprocessing.view.item;

import com.company.orderprocessing.app.ResetService;
import com.company.orderprocessing.entity.Item;
import com.company.orderprocessing.event.RefreshItemsEvent;
import com.company.orderprocessing.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.HasValueAndElement;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import io.jmix.core.validation.group.UiCrossFieldChecks;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.component.validation.ValidationErrors;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.*;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;

@Route(value = "items", layout = MainView.class)
@ViewController(id = "ord_Item.list")
@ViewDescriptor(path = "item-list-view.xml")
@DialogMode(width = "64em")
public class ItemListView extends StandardListView<Item> {

    @Autowired
    private ResetService resetService;

    @ViewComponent
    private DataContext dataContext;

    @ViewComponent
    private CollectionContainer<Item> itemsDc;

    @ViewComponent
    private InstanceContainer<Item> itemDc;

    @ViewComponent
    private InstanceLoader<Item> itemDl;

    @ViewComponent
    private VerticalLayout listLayout;

    @ViewComponent
    private FormLayout form;

    @ViewComponent
    private HorizontalLayout detailActions;
    @ViewComponent
    private CollectionLoader<Item> itemsDl;


    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        updateControls(false);
    }

    @Subscribe("itemsDataGrid.create")
    public void onItemsDataGridCreate(final ActionPerformedEvent event) {
        dataContext.clear();
        Item entity = dataContext.create(Item.class);
        itemDc.setItem(entity);
        updateControls(true);
    }

    @Subscribe("itemsDataGrid.edit")
    public void onItemsDataGridEdit(final ActionPerformedEvent event) {
        updateControls(true);
    }

    @Subscribe("saveButton")
    public void onSaveButtonClick(final ClickEvent<JmixButton> event) {
        Item item = itemDc.getItem();
        ValidationErrors validationErrors = validateView(item);
        if (!validationErrors.isEmpty()) {
            ViewValidation viewValidation = getViewValidation();
            viewValidation.showValidationErrors(validationErrors);
            viewValidation.focusProblemComponent(validationErrors);
            return;
        }
        dataContext.save();
        itemsDc.replaceItem(item);
        updateControls(false);
    }

    @Subscribe("cancelButton")
    public void onCancelButtonClick(final ClickEvent<JmixButton> event) {
        dataContext.clear();
        itemDl.load();
        updateControls(false);
    }

    @Subscribe(id = "itemsDc", target = Target.DATA_CONTAINER)
    public void onItemsDcItemChange(final InstanceContainer.ItemChangeEvent<Item> event) {
        Item entity = event.getItem();
        dataContext.clear();
        if (entity != null) {
            itemDl.setEntityId(entity.getId());
            itemDl.load();
        } else {
            itemDl.setEntityId(null);
            itemDc.setItem(null);
        }
        updateControls(false);
    }

    protected ValidationErrors validateView(Item entity) {
        ViewValidation viewValidation = getViewValidation();
        ValidationErrors validationErrors = viewValidation.validateUiComponents(form);
        if (!validationErrors.isEmpty()) {
            return validationErrors;
        }
        validationErrors.addAll(viewValidation.validateBeanGroup(UiCrossFieldChecks.class, entity));
        return validationErrors;
    }

    private void updateControls(boolean editing) {
        UiComponentUtils.getComponents(form).forEach(component -> {
            if (component instanceof HasValueAndElement<?, ?> field) {
                field.setReadOnly(!editing);
            }
        });

        detailActions.setVisible(editing);
        listLayout.setEnabled(!editing);
    }

    private ViewValidation getViewValidation() {
        return getApplicationContext().getBean(ViewValidation.class);
    }

    @Subscribe(id = "resetButton", subject = "clickListener")
    public void onResetButtonClick(final ClickEvent<JmixButton> event) {
        resetService.initItems();
        itemsDl.load();
        updateControls(false);
    }

    @EventListener
    private void onRefreshEvent(RefreshItemsEvent event) {
       itemDl.load();
    }
}