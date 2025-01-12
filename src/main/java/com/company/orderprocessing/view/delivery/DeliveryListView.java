package com.company.orderprocessing.view.delivery;

import com.company.orderprocessing.entity.Delivery;
import com.company.orderprocessing.entity.Order;
import com.company.orderprocessing.repository.DeliveryRepository;
import com.company.orderprocessing.repository.OrderRepository;
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
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.*;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Route(value = "deliveries", layout = MainView.class)
@ViewController(id = "ord_Delivery.list")
@ViewDescriptor(path = "delivery-list-view.xml")
@LookupComponent("deliveriesDataGrid")
@DialogMode(width = "64em")
public class DeliveryListView extends StandardListView<Delivery> {

    @ViewComponent
    private DataContext dataContext;

    @ViewComponent
    private CollectionContainer<Delivery> deliveriesDc;

    @ViewComponent
    private InstanceContainer<Delivery> deliveryDc;

    @ViewComponent
    private InstanceLoader<Delivery> deliveryDl;

    @ViewComponent
    private VerticalLayout listLayout;

    @ViewComponent
    private FormLayout form;

    @ViewComponent
    private HorizontalLayout detailActions;

    @ViewComponent
    private CollectionPropertyContainer<Order> ordersDc;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        updateControls(false);
    }

    @Subscribe("saveButton")
    public void onSaveButtonClick(final ClickEvent<JmixButton> event) {
        Delivery item = deliveryDc.getItem();
        ValidationErrors validationErrors = validateView(item);
        if (!validationErrors.isEmpty()) {
            ViewValidation viewValidation = getViewValidation();
            viewValidation.showValidationErrors(validationErrors);
            viewValidation.focusProblemComponent(validationErrors);
            return;
        }
        dataContext.save();
        deliveriesDc.replaceItem(item);
        updateControls(false);
    }

    @Subscribe("cancelButton")
    public void onCancelButtonClick(final ClickEvent<JmixButton> event) {
        dataContext.clear();
        deliveryDl.load();
        updateControls(false);
    }

    @Subscribe(id = "deliveriesDc", target = Target.DATA_CONTAINER)
    public void onDeliveriesDcItemChange(final InstanceContainer.ItemChangeEvent<Delivery> event) {
        Delivery entity = event.getItem();
        dataContext.clear();
        if (entity != null) {
            deliveryDl.setEntityId(entity.getId());
            deliveryDl.load();
        } else {
            deliveryDl.setEntityId(null);
            deliveryDc.setItem(null);
        }
        updateControls(false);
        loadOrders(entity);
    }

    protected ValidationErrors validateView(Delivery entity) {
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

    @Subscribe(id = "removeAllButton", subject = "clickListener")
    public void onRemoveAllButtonClick(final ClickEvent<JmixButton> event) {
        deliveryRepository.deleteAll();
        deliveryDl.load();
        deliveriesDc.setItems(null);
        deliveryDc.setItem(null);
        updateControls(false);
    }

    private void loadOrders(Delivery delivery) {
        List<Order> orders = orderRepository.findByDelivery(delivery);
        ordersDc.setItems(orders);
    }
}