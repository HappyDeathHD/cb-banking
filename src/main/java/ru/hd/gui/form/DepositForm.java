package ru.hd.gui.form;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import lombok.Getter;
import ru.hd.jpa.Account;

import java.math.BigDecimal;

public class DepositForm extends FormLayout {

    private Account account;

    private final TextField currentBalance = new TextField("Текущий баланс");
    private final TextField amountField = new TextField("Сумма пополнения");

    public DepositForm() {
        currentBalance.setReadOnly(true);
        amountField.setRequiredIndicatorVisible(true);
        amountField.setPattern("\\d+(\\.\\d{2})?");
        amountField.setErrorMessage("Введите корректную сумму");

        Button depositButton = new Button("Пополнить", VaadinIcon.MONEY_DEPOSIT.create());
        depositButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        depositButton.addClickListener(_ -> validateAndFireEvent());

        add(currentBalance, amountField, depositButton);
    }

    public void setAccount(Account account) {
        this.account = account;
        currentBalance.setValue(account.getBalance().toString());
        amountField.setValue("");
    }

    private void validateAndFireEvent() {
        if (amountField.isEmpty()) {
            amountField.setInvalid(true);
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountField.getValue());
        } catch (NumberFormatException e) {
            amountField.setInvalid(true);
            return;
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            amountField.setErrorMessage("Сумма должна быть положительной");
            amountField.setInvalid(true);
            return;
        }

        fireEvent(new DepositEvent(this, account, amount));
    }

    @Getter
    public static class DepositEvent extends ComponentEvent<DepositForm> {
        private final Account account;
        private final BigDecimal amount;

        public DepositEvent(DepositForm source, Account account, BigDecimal amount) {
            super(source, false);
            this.account = account;
            this.amount = amount;
        }

    }

    public void addDepositListener(ComponentEventListener<DepositEvent> listener) {
        addListener(DepositEvent.class, listener);
    }
}