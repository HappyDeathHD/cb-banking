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

public class TransferForm extends FormLayout {
    private Account fromAccount;
    private final TextField fromAccountField = new TextField("Счет отправителя");
    private final TextField toAccountField = new TextField("Счет получателя");
    private final TextField amountField = new TextField("Сумма перевода");

    public TransferForm() {
        fromAccountField.setReadOnly(true);
        toAccountField.setRequiredIndicatorVisible(true);
        amountField.setRequiredIndicatorVisible(true);
        amountField.setPattern("\\d+(\\.\\d{2})?");
        amountField.setErrorMessage("Введите корректную сумму");

        Button transferButton = new Button("Перевести", VaadinIcon.EXCHANGE.create());
        transferButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        transferButton.addClickListener(_ -> validateAndFireEvent());

        add(fromAccountField, toAccountField, amountField, transferButton);
    }

    public void setAccount(Account account) {
        this.fromAccount = account;
        fromAccountField.setValue(account.getAccountNumber());
        toAccountField.setValue("");
        amountField.setValue("");
    }

    private void validateAndFireEvent() {
        if (toAccountField.isEmpty() || amountField.isEmpty()) {
            toAccountField.setInvalid(toAccountField.isEmpty());
            amountField.setInvalid(amountField.isEmpty());
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

        if (amount.compareTo(fromAccount.getBalance()) > 0) {
            amountField.setErrorMessage("Недостаточно средств");
            amountField.setInvalid(true);
            return;
        }

        fireEvent(new TransferEvent(this, fromAccount,toAccountField.getValue(), amount));
    }

    @Getter
    public static class TransferEvent extends ComponentEvent<TransferForm> {
        private final Account fromAccount;
        private final String toAccountNumber;
        private final BigDecimal amount;

        public TransferEvent(TransferForm source, Account fromAccount, String toAccountNumber, BigDecimal amount) {
            super(source, false);
            this.fromAccount = fromAccount;
            this.toAccountNumber = toAccountNumber;
            this.amount = amount;
        }
    }

    public void addTransferListener(ComponentEventListener<TransferEvent> listener) {
        addListener(TransferEvent.class, listener);
    }
}