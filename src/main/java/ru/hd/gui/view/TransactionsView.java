package ru.hd.gui.view;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.hibernate.Session;
import ru.hd.gui.MainLayout;
import ru.hd.jpa.Account;
import ru.hd.jpa.TransactionRecord;
import ru.hd.service.TransactionService;
import ru.hd.util.HibernateUtil;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@PageTitle("Транзакции")
@Route(value = "transactions", layout = MainLayout.class)
public class TransactionsView extends VerticalLayout {
    private final TransactionService transactionService = new TransactionService();
    private final Grid<TransactionRecord> grid = new Grid<>(TransactionRecord.class, false);

    public TransactionsView() {
        configureGrid();
        add(grid);
        updateGridData();
    }

    private void configureGrid() {
        grid.addColumn(transaction -> transaction.getAmount().toString())
                .setHeader("Сумма")
                .setWidth("150px")
                .setFlexGrow(1);

        grid.addColumn(transaction -> transaction.getType().getDisplayName())
                .setHeader("Тип")
                .setWidth("135px")
                .setFlexGrow(0);

        grid.addColumn(transaction -> transaction.getStatus().getDisplayName())
                .setHeader("Статус")
                .setWidth("125px")
                .setFlexGrow(0);

        grid.addColumn(transaction -> getAccountNumber(transaction.getFromAccount()))
                .setHeader("Счет отправителя")
                .setWidth("220px")
                .setFlexGrow(0);

        grid.addColumn(transaction -> getAccountNumber(transaction.getToAccount()))
                .setHeader("Счет получателя")
                .setWidth("220px")
                .setFlexGrow(0);

        grid.addColumn(transaction -> formatDateTime(transaction.getCreatedAt()))
                .setHeader("Дата создания")
                .setWidth("200px")
                .setFlexGrow(0);

        grid.addColumn(transaction -> formatDateTime(transaction.getUpdatedAt()))
                .setHeader("Дата обновления")
                .setWidth("200px")
                .setFlexGrow(0);

        grid.setWidthFull();
        grid.setHeight("calc(100vh - 115px)");
        grid.setPageSize(50);

        grid.setDataProvider(DataProvider.fromCallbacks(
                query -> {
                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        return transactionService.getTransactions(session, query.getOffset(), query.getLimit()).stream();
                    }
                },
                _ -> {
                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        return transactionService.getTotalTransactionsCount(session);
                    }
                }
        ));
    }

    private String getAccountNumber(Account account) {
        if (account == null) {
            return "-";
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Account managedAccount = session.get(Account.class, account.getId());
            return managedAccount != null ? managedAccount.getAccountNumber() : "-";
        }
    }

    private String formatDateTime(Instant instant) {
        if (instant == null) {
            return "-";
        }
        return instant.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
    }

    private void updateGridData() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            grid.setItems(transactionService.getAllTransactions(session));
        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки данных транзакций", e);
        }
    }
}