package ru.hd.gui.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hd.gui.MainLayout;
import ru.hd.gui.form.AccountForm;
import ru.hd.gui.form.ClientForm;
import ru.hd.jpa.Account;
import ru.hd.jpa.Client;
import ru.hd.service.AccountService;
import ru.hd.service.ClientService;
import ru.hd.util.HibernateUtil;

@PageTitle("Клиенты")
@Route(value = "clients", layout = MainLayout.class)
public class ClientsView extends VerticalLayout {
    private static final Logger logger = LoggerFactory.getLogger(ClientsView.class);

    private final ClientService clientService = new ClientService();
    private final AccountService accountService = new AccountService();
    private final Grid<Client> grid = new Grid<>(Client.class, false);

    private final Dialog clientDialog = new Dialog();
    private final ClientForm clientForm = new ClientForm();

    private final Dialog accountDialog = new Dialog();
    private final AccountForm accountForm = new AccountForm();

    public ClientsView() {
        configureGrid();
        configureClientDialog();
        configureAccountDialog();

        Button addButton = new Button("Добавить клиента", VaadinIcon.PLUS.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(_ -> openClientForm(new Client()));

        add(addButton, grid);
        updateGridData();
    }

    private void configureGrid() {
        grid.addColumn(Client::getFullName)
                .setHeader("ФИО")
                .setAutoWidth(false)
                .setWidth("200px")
                .setFlexGrow(2)
                .setResizable(true)
                .addClassName("flex-column");

        grid.addColumn(Client::getPhoneNumber)
                .setHeader("Телефон")
                .setWidth("160px")
                .setFlexGrow(0);

        grid.addColumn(Client::getInn)
                .setHeader("ИНН")
                .setWidth("180px")
                .setFlexGrow(0);

        grid.addColumn(Client::getAddress)
                .setHeader("Адрес")
                .setAutoWidth(false)
                .setWidth("300px")
                .setFlexGrow(3)
                .setResizable(true)
                .addClassName("flex-column");

        grid.addColumn(_ -> "Скан паспорта")
                .setHeader("Документы")
                .setWidth("140px")
                .setFlexGrow(0);

        grid.addComponentColumn(client -> {
                    Button editButton = new Button(VaadinIcon.EDIT.create());
                    editButton.setTooltipText("Редактировать клиента");
                    editButton.addClickListener(_ -> openClientForm(client));

                    Button createAccountButton = new Button(VaadinIcon.PLUS_CIRCLE.create());
                    createAccountButton.setTooltipText("Создать счет");
                    createAccountButton.addClickListener(_ -> openAccountForm(client));

                    HorizontalLayout buttons = new HorizontalLayout(editButton, createAccountButton);
                    buttons.setSpacing(true);
                    return buttons;
                })
                .setHeader("Действия")
                .setWidth("130px")
                .setFlexGrow(0);

        grid.setWidthFull();
        grid.setHeight("calc(100vh - 175px)");
        grid.getStyle().set("min-height", "400px");
        grid.setPageSize(50);


        grid.setDataProvider(DataProvider.fromCallbacks(
                query -> {
                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        return clientService.getClients(session, query.getOffset(), query.getLimit()).stream();
                    }
                },
                _ -> {
                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        return clientService.getTotalClientsCount(session);
                    }
                }
        ));
    }

    private void configureClientDialog() {
        clientDialog.setWidth("800px");
        clientDialog.add(clientForm);

        clientForm.addSaveListener(event -> {
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                if (event.getClient().getId() == null) {
                    clientService.createClient(session, event.getClient());
                    Notification.show("Клиент успешно добавлен", 3000, Notification.Position.TOP_CENTER);
                } else {
                    clientService.updateClient(session, event.getClient());
                    Notification.show("Изменения сохранены", 3000, Notification.Position.TOP_CENTER);
                }
                clientDialog.close();
                updateGridData();
            } catch (Exception e) {
                logger.error("Ошибка сохранения клиента", e);
                Notification.show("Ошибка: " + e.getMessage(), 10_000, Notification.Position.TOP_CENTER);
            }
        });
    }

    private void configureAccountDialog() {
        accountDialog.setWidth("800px");
        accountDialog.add(accountForm);

        accountForm.addSaveListener(event -> {
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                accountService.createAccount(session, event.getAccount());
                Notification.show("Счет создан", 3000, Notification.Position.TOP_CENTER);
                accountDialog.close();
            } catch (Exception e) {
                logger.error("Ошибка создания счета", e);
                Notification.show("Ошибка: " + e.getMessage(), 10_000, Notification.Position.TOP_CENTER);
            }
        });
    }

    private void openClientForm(Client client) {
        clientForm.setClient(client);
        clientDialog.setHeaderTitle(client.getId() == null ? "Новый клиент" : "Редактирование");
        clientDialog.open();
    }

    private void openAccountForm(Client client) {
        Account account = new Account();
        account.setClient(client);
        accountForm.setAccount(account);
        accountDialog.setHeaderTitle("Создание счета для " + client.getFullName());
        accountDialog.open();
    }

    private void updateGridData() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            grid.setItems(clientService.getAllClients(session));
        } catch (Exception e) {
            logger.error("Ошибка загрузки данных", e);
            Notification.show("Ошибка загрузки", 10_000, Notification.Position.TOP_CENTER);
        }
    }
}