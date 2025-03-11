package ru.hd.gui.view;

import com.vaadin.flow.component.button.Button;
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
import ru.hd.gui.form.DepositForm;
import ru.hd.gui.form.TransferForm;
import ru.hd.gui.form.WithdrawForm;
import ru.hd.jpa.Account;
import ru.hd.model.AccountStatus;
import ru.hd.service.AccountService;
import ru.hd.service.TransactionService;
import ru.hd.util.HibernateUtil;

import java.math.BigDecimal;

@PageTitle("Счета")
@Route(value = "accounts", layout = MainLayout.class)
public class AccountsView extends VerticalLayout {
    private static final Logger logger = LoggerFactory.getLogger(AccountsView.class);

    private final AccountService accountService = new AccountService();
    private final TransactionService transactionService = new TransactionService();

    private final Grid<Account> grid = new Grid<>(Account.class, false);

    private final Dialog accountDialog = new Dialog();
    private final AccountForm accountForm = new AccountForm();
    private final Dialog depositDialog = new Dialog();
    private final DepositForm depositForm = new DepositForm();
    private final Dialog transferDialog = new Dialog();
    private final TransferForm transferForm = new TransferForm();
    private final Dialog withdrawDialog = new Dialog();
    private final WithdrawForm withdrawForm = new WithdrawForm();

    public AccountsView() {
        configureGrid();
        configureAccountDialog();
        configureDepositDialog();
        configureTransferDialog();
        configureWithdrawDialog();
        add(grid);
        updateGridData();
    }

    private void configureGrid() {
        grid.addColumn(Account::getAccountNumber)
                .setHeader("Номер счета")
                .setAutoWidth(false)
                .setWidth("250px")
                .setFlexGrow(0);

        grid.addColumn(Account::getBik)
                .setHeader("БИК")
                .setWidth("150px")
                .setFlexGrow(0);

        grid.addColumn(account -> account.getCurrency().name())
                .setHeader("Валюта")
                .setWidth("120px")
                .setFlexGrow(0);

        grid.addColumn(Account::getBalance)
                .setHeader("Баланс")
                .setWidth("150px")
                .setFlexGrow(0);

        grid.addColumn(account -> account.getStatus().getDisplayName())
                .setHeader("Статус")
                .setWidth("150px")
                .setFlexGrow(0);

        grid.addColumn(account -> account.getClient().getFullName())
                .setHeader("Клиент")
                .setAutoWidth(false)
                .setWidth("200px")
                .setFlexGrow(2);

        grid.addComponentColumn(account -> {
                    Button editButton = new Button(VaadinIcon.EDIT.create());
                    editButton.setTooltipText("Редактировать счет");
                    editButton.setEnabled(!account.getStatus().equals(AccountStatus.CLOSED));
                    editButton.addClickListener(_ -> openAccountForm(account));

                    Button depositButton = new Button(VaadinIcon.MONEY_DEPOSIT.create());
                    depositButton.setTooltipText("Пополнить баланс");
                    depositButton.setEnabled(!account.getStatus().equals(AccountStatus.CLOSED));
                    depositButton.addClickListener(_ -> openDepositForm(account));

                    Button transferButton = new Button(VaadinIcon.EXCHANGE.create());
                    transferButton.setTooltipText("Перевести средства");
                    transferButton.setEnabled(!account.getStatus().equals(AccountStatus.CLOSED));
                    transferButton.addClickListener(_ -> openTransferForm(account));

                    Button withdrawButton = new Button(VaadinIcon.MONEY_WITHDRAW.create());
                    withdrawButton.setTooltipText("Снять средства");
                    withdrawButton.setEnabled(
                            account.getStatus().equals(AccountStatus.OPEN) &&
                                    account.getBalance().compareTo(BigDecimal.ZERO) > 0
                    );
                    withdrawButton.addClickListener(_ -> openWithdrawForm(account));

                    Button closeButton = new Button(VaadinIcon.ARCHIVE.create());
                    closeButton.setTooltipText("Закрыть счет");
                    closeButton.setEnabled(
                            account.getStatus().equals(AccountStatus.OPEN) &&
                                    account.getBalance().compareTo(BigDecimal.ZERO) == 0
                    );
                    closeButton.addClickListener(_ -> closeAccount(account));

                    return new HorizontalLayout(editButton, depositButton, transferButton, withdrawButton, closeButton);
                }).setHeader("Действия")
                .setWidth("300px")
                .setFlexGrow(0);

        grid.setWidthFull();
        grid.setHeight("calc(100vh - 115px)");
        grid.setPageSize(50);

        grid.setDataProvider(DataProvider.fromCallbacks(
                query -> {
                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        return accountService.getAccounts(session, query.getOffset(), query.getLimit()).stream();
                    }
                },
                _ -> {
                    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                        return accountService.getTotalAccountsCount(session);
                    }
                }
        ));
    }

    private void configureAccountDialog() {
        accountDialog.setWidth("800px");
        accountDialog.add(accountForm);

        accountForm.addSaveListener(event -> {
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                accountService.updateAccount(session, event.getAccount());
                Notification.show("Счет обновлен", 3000, Notification.Position.TOP_CENTER);
                accountDialog.close();
                updateGridData();
            } catch (Exception e) {
                logger.error("Ошибка обновления счета", e);
                Notification.show("Ошибка: " + e.getMessage(), 10_000, Notification.Position.TOP_CENTER);
            }
        });
    }

    private void openAccountForm(Account account) {
        accountForm.setAccount(account);
        accountDialog.setHeaderTitle("Редактирование счета");
        accountDialog.open();
    }

    private void configureDepositDialog() {
        depositDialog.setWidth("500px");
        depositDialog.add(depositForm);

        depositForm.addDepositListener(event -> {
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                transactionService.depositToAccount(session, event.getAccount(), event.getAmount());
                Notification.show("Баланс пополнен", 3000, Notification.Position.TOP_CENTER);
                depositDialog.close();
                updateGridData();
            } catch (Exception e) {
                logger.error("Ошибка пополнения баланса", e);
                Notification.show("Ошибка: " + e.getMessage(), 10_000, Notification.Position.TOP_CENTER);
            }
        });
    }

    private void openDepositForm(Account account) {
        depositForm.setAccount(account);
        depositDialog.setHeaderTitle("Пополнение счета " + account.getAccountNumber());
        depositDialog.open();
    }

    private void configureTransferDialog() {
        transferDialog.setWidth("600px");
        transferDialog.add(transferForm);

        transferForm.addTransferListener(event -> {
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                Account toAccount = accountService.findAccountByNumber(session, event.getToAccountNumber());
                transactionService.transfer(
                        session,
                        event.getFromAccount().getId(),
                        toAccount.getId(),
                        event.getAmount()
                );

                Notification.show("Перевод выполнен", 3000, Notification.Position.TOP_CENTER);
                transferDialog.close();
                updateGridData();
            } catch (Exception e) {
                logger.error("Ошибка перевода средств", e);
                Notification.show("Ошибка: " + e.getMessage(), 10_000, Notification.Position.TOP_CENTER);
            }
        });
    }

    private void openTransferForm(Account account) {
        transferForm.setAccount(account);
        transferDialog.setHeaderTitle("Перевод со счета " + account.getAccountNumber());
        transferDialog.open();
    }

    private void closeAccount(Account account) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            accountService.closeAccount(session, account);
            Notification.show("Счет закрыт", 3000, Notification.Position.TOP_CENTER);
            updateGridData();
        } catch (Exception e) {
            logger.error("Ошибка закрытия счета", e);
            Notification.show("Ошибка: " + e.getMessage(), 10_000, Notification.Position.TOP_CENTER);
        }
    }

    private void configureWithdrawDialog() {
        withdrawDialog.setWidth("500px");
        withdrawDialog.add(withdrawForm);

        withdrawForm.addWithdrawListener(event -> {
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                transactionService.withdrawFromAccount(session, event.getAccount(), event.getAmount());
                Notification.show("Средства сняты", 3000, Notification.Position.TOP_CENTER);
                withdrawDialog.close();
                updateGridData();
            } catch (Exception e) {
                logger.error("Ошибка снятия средств", e);
                Notification.show("Ошибка: " + e.getMessage(), 10_000, Notification.Position.TOP_CENTER);
            }
        });
    }

    private void openWithdrawForm(Account account) {
        withdrawForm.setAccount(account);
        withdrawDialog.setHeaderTitle("Снятие со счета " + account.getAccountNumber());
        withdrawDialog.open();
    }

    private void updateGridData() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            grid.setItems(accountService.getAllAccounts(session));
        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки данных счетов", e);
        }
    }
}