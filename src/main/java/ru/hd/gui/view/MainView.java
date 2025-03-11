package ru.hd.gui.view;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import lombok.Getter;
import ru.hd.gui.MainLayout;

@Getter
@Route(value = "", layout = MainLayout.class)
public class MainView extends VerticalLayout {
    public MainView() {
        add(new H2("Добро пожаловать в банковскую систему"));
        setAlignItems(Alignment.CENTER);
    }
}