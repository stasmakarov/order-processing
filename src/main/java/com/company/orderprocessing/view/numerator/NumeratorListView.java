package com.company.orderprocessing.view.numerator;

import com.company.orderprocessing.app.ResetService;
import com.company.orderprocessing.entity.Numerator;
import com.company.orderprocessing.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.HasValueAndElement;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.validation.group.UiCrossFieldChecks;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.component.validation.ValidationErrors;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.*;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "numerators", layout = MainView.class)
@ViewController(id = "ord_Numerator.list")
@ViewDescriptor(path = "numerator-list-view.xml")
@DialogMode(width = "64em")
public class NumeratorListView extends StandardListView<Numerator> {

    @Autowired
    private DataManager dataManager;
    @Autowired
    private ResetService resetService;

    @ViewComponent
    private DataContext dataContext;

    @ViewComponent
    private CollectionContainer<Numerator> numeratorsDc;

    @ViewComponent
    private InstanceContainer<Numerator> numeratorDc;

    @ViewComponent
    private InstanceLoader<Numerator> numeratorDl;

    @ViewComponent
    private VerticalLayout listLayout;

    @ViewComponent
    private FormLayout form;

    @ViewComponent
    private HorizontalLayout detailActions;
    @ViewComponent
    private CollectionLoader<Numerator> numeratorsDl;

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        updateControls(false);
    }

    @Subscribe("numeratorsDataGrid.create")
    public void onNumeratorsDataGridCreate(final ActionPerformedEvent event) {
        dataContext.clear();
        Numerator entity = dataContext.create(Numerator.class);
        numeratorDc.setItem(entity);
        updateControls(true);
    }

    @Subscribe("saveButton")
    public void onSaveButtonClick(final ClickEvent<JmixButton> event) {
        Numerator item = numeratorDc.getItem();
        ValidationErrors validationErrors = validateView(item);
        if (!validationErrors.isEmpty()) {
            ViewValidation viewValidation = getViewValidation();
            viewValidation.showValidationErrors(validationErrors);
            viewValidation.focusProblemComponent(validationErrors);
            return;
        }
        dataContext.save();
        numeratorsDc.replaceItem(item);
        updateControls(false);
    }

    @Subscribe("cancelButton")
    public void onCancelButtonClick(final ClickEvent<JmixButton> event) {
        dataContext.clear();
        numeratorDl.load();
        updateControls(false);
    }

    @Subscribe(id = "numeratorsDc", target = Target.DATA_CONTAINER)
    public void onNumeratorsDcItemChange(final InstanceContainer.ItemChangeEvent<Numerator> event) {
        Numerator entity = event.getItem();
        dataContext.clear();
        if (entity != null) {
            numeratorDl.setEntityId(entity.getId());
            numeratorDl.load();
        } else {
            numeratorDl.setEntityId(null);
            numeratorDc.setItem(null);
        }
        updateControls(false);
    }

    protected ValidationErrors validateView(Numerator entity) {
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

    @Subscribe(id = "resetAllBtn", subject = "clickListener")
    public void onResetAllBtnClick(final ClickEvent<JmixButton> event) {
        resetService.initNumerators();
        numeratorsDl.load();
    }

}