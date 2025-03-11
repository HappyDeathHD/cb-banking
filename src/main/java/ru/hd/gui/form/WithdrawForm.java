package ru.hd.gui.form;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import lombok.Getter;
import lombok.Setter;
import ru.hd.jpa.Account;

import java.math.BigDecimal;

public class WithdrawForm extends FormLayout {
    private final TextField amountField = new TextField("Сумма снятия");

    @Setter
    private Account account;

    public WithdrawForm() {
        amountField.setRequiredIndicatorVisible(true);
        amountField.setPattern("\\d+(\\.\\d{2})?");
        amountField.setErrorMessage("Введите корректную сумму");

        Button withdrawButton = new Button("Снять", VaadinIcon.MONEY_WITHDRAW.create());
        withdrawButton.addThemeName("primary");
        withdrawButton.addClickListener(_ -> validateAndFireEvent());

        add(amountField, withdrawButton);
    }

    private void validateAndFireEvent() {
        if (amountField.isEmpty()) {
            amountField.setInvalid(true);
            Notification.show("Введите сумму снятия", 3000, Notification.Position.TOP_CENTER);
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountField.getValue());
        } catch (NumberFormatException e) {
            amountField.setInvalid(true);
            Notification.show("Некорректная сумма", 3000, Notification.Position.TOP_CENTER);
            return;
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            amountField.setErrorMessage("Сумма должна быть положительной");
            amountField.setInvalid(true);
            return;
        }

        if (amount.compareTo(account.getBalance()) > 0) {
            amountField.setErrorMessage("Недостаточно средств на счете");
            amountField.setInvalid(true);
            return;
        }

        fireEvent(new WithdrawEvent(this, account, amount));
    }

    @Getter
    public static class WithdrawEvent extends ComponentEvent<WithdrawForm> {
        private final Account account;
        private final BigDecimal amount;

        public WithdrawEvent(WithdrawForm source, Account account, BigDecimal amount) {
            super(source, false);
            this.account = account;
            this.amount = amount;
        }

    }

    public void addWithdrawListener(ComponentEventListener<WithdrawEvent> listener) {
        addListener(WithdrawEvent.class, listener);
    }
}