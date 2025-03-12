package ru.hd.gui.form;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;
import ru.hd.jpa.Account;
import ru.hd.model.Currency;
import ru.hd.util.ValidationPattern;

import java.math.BigDecimal;

public class AccountForm extends FormLayout {
    private final Binder<Account> binder = new Binder<>(Account.class);
    private Account account;

    private final TextField accountNumberField = new TextField("Номер счета");
    private final TextField bikField = new TextField("БИК");
    private final ComboBox<Currency> currencyComboBox = new ComboBox<>("Валюта");
    private final Button saveButton = new Button("Сохранить");

    public AccountForm() {
        configureFields();
        configureBinder();
        setupLayout();
        setupValidation();
    }

    private void configureFields() {
        accountNumberField.setWidthFull();
        accountNumberField.setRequiredIndicatorVisible(true);

        bikField.setWidthFull();
        bikField.setRequiredIndicatorVisible(true);

        currencyComboBox.setWidthFull();
        setupComboBox(currencyComboBox, Currency.values());
    }

    private <T> void setupComboBox(ComboBox<T> comboBox, T[] items) {
        comboBox.setItems(items);
        comboBox.setItemLabelGenerator(Object::toString);
        comboBox.setRequired(true);
    }

    private void setupLayout() {
        add(accountNumberField, bikField, currencyComboBox, saveButton);
        setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep("600px", 2)
        );
        setWidth("800px");
    }

    private void configureBinder() {
        binder.forField(accountNumberField)
                .asRequired("Номер счета обязателен")
                .withValidator(value -> ValidationPattern.ACCOUNT_NUMBER.matcher(value).matches(),
                        "БИК должен содержать 20 цифр")
                .bind(Account::getAccountNumber, Account::setAccountNumber);

        binder.forField(bikField)
                .asRequired("БИК обязателен")
                .withValidator(value -> ValidationPattern.BIK.matcher(value).matches(),
                        "БИК должен содержать 9 цифр и начинаться на 04")
                .bind(Account::getBik, Account::setBik);

        binder.forField(currencyComboBox)
                .asRequired("Валюта обязательна")
                .bind(Account::getCurrency, Account::setCurrency);

        binder.addStatusChangeListener(_ -> saveButton.setEnabled(binder.isValid()));
    }

    private void setupValidation() {
        saveButton.addClickListener(_ -> {
            if (binder.writeBeanIfValid(account)) {
                fireEvent(new SaveEvent(this, account));
            }
        });
    }

    public void setAccount(Account account) {
        this.account = account;
        binder.readBean(account);
        updateComboBoxState();
    }

    @Getter
    public static abstract class AccountFormEvent extends ComponentEvent<AccountForm> {
        private final Account account;

        protected AccountFormEvent(AccountForm source, Account account) {
            super(source, false);
            this.account = account;
        }
    }

    public static class SaveEvent extends AccountFormEvent {
        SaveEvent(AccountForm source, Account account) {
            super(source, account);
        }
    }

    public void addSaveListener(ComponentEventListener<SaveEvent> listener) {
        addListener(SaveEvent.class, listener);
    }

    private void updateComboBoxState() {
        boolean isBalanceZero = account != null
                && account.getBalance() != null
                && account.getBalance().compareTo(BigDecimal.ZERO) == 0;
        currencyComboBox.setEnabled(isBalanceZero);
    }
}