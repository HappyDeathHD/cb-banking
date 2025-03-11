package ru.hd.gui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLink;
import lombok.Getter;
import ru.hd.gui.view.AccountsView;
import ru.hd.gui.view.ClientsView;
import ru.hd.gui.view.MainView;
import ru.hd.gui.view.TransactionsView;

import java.util.Arrays;
import java.util.List;

public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
        setDrawerOpened(false);
    }

    private void createHeader() {
        RouterLink logoLink = new RouterLink(MainView.class);
        Image logo = new Image("img/logo.png", "Логотип");
        logo.addClassName("logo");
        logoLink.add(logo);

        List<RouterLink> navLinks = Arrays.stream(NavigationItem.values())
                .map(this::createNavLink)
                .toList();

        HorizontalLayout navContainer = new HorizontalLayout(navLinks.toArray(new RouterLink[0]));
        navContainer.setSpacing(false);
        navContainer.addClassName("nav-container");

        FlexLayout headerContent = new FlexLayout(logoLink, navContainer);
        headerContent.addClassName("header-content");
        headerContent.setFlexGrow(1, navContainer);
        headerContent.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerContent.setWidthFull();

        Header header = new Header(headerContent);
        header.addClassName("main-header");
        addToNavbar(header);
    }

    private RouterLink createNavLink(NavigationItem item) {
        RouterLink link = new RouterLink(item.getLabel(), item.getViewClass());
        link.addClassName("nav-button");
        link.getElement().setAttribute("theme", "large tertiary");
        return link;
    }

    @Getter
    private enum NavigationItem {
        CLIENTS("Клиенты", ClientsView.class),
        ACCOUNTS("Счета", AccountsView.class),
        TRANSACTIONS("Транзакции", TransactionsView.class);

        private final String label;
        private final Class<? extends Component> viewClass;

        NavigationItem(String label, Class<? extends Component> viewClass) {
            this.label = label;
            this.viewClass = viewClass;
        }

    }
}