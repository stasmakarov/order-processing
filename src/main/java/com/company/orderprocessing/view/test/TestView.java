package com.company.orderprocessing.view.test;


import com.company.orderprocessing.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.flowable.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "test-view", layout = MainView.class)
@ViewController(id = "ord_TestView")
@ViewDescriptor(path = "test-view.xml")
public class TestView extends StandardView {
    @ViewComponent
    private TypedTextField<Object> nameField;
    @Autowired
    private RuntimeService runtimeService;

    @Subscribe(id = "startBtn", subject = "clickListener")
    public void onStartBtnClick(final ClickEvent<JmixButton> event) {
        String name = nameField.getValue();
        runtimeService.startProcessInstanceByMessage(name);
    }
}