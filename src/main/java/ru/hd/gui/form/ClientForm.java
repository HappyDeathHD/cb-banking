package ru.hd.gui.form;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.*;
import lombok.Getter;
import ru.hd.jpa.Client;
import ru.hd.util.ValidationPattern;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class ClientForm extends FormLayout {
    private final Binder<Client> binder = new Binder<>(Client.class);
    private final Map<TextField, Binder.Binding<Client, ?>> bindings = new HashMap<>();
    private Client client;

    private final TextField fullNameField = new TextField("ФИО");
    private final TextField phoneField = new TextField("Телефон");
    private final TextField innField = new TextField("ИНН");
    private final TextField addressField = new TextField("Адрес");
    private final TextField passportField = new TextField("Скан паспорта");

    private final Button saveButton = new Button("Сохранить");

    public ClientForm() {
        configureFields();
        configureBinder();
        setupLayout();
        setupValidation();
    }

    private void configureFields() {
        Stream.of(fullNameField, phoneField, innField, addressField, passportField)
                .forEach(field -> {
                    field.setWidthFull();
                    field.setRequiredIndicatorVisible(true);
                    field.addBlurListener(_ -> validateField(field));
                });
    }

    private void setupLayout() {
        add(fullNameField, phoneField, innField, addressField, passportField, saveButton);
        setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep("600px", 2)
        );
        setWidth("800px");
    }

    private void configureBinder() {
        bindings.put(fullNameField, binder.forField(fullNameField)
                .asRequired("ФИО обязательно")
                .bind(Client::getFullName, Client::setFullName));

        bindings.put(phoneField, binder.forField(phoneField)
                .asRequired("Телефон обязателен")
                .withValidator(value -> ValidationPattern.PHONE.matcher(value).matches(),
                        "Формат: +7XXXXXXXXXX")
                .bind(Client::getPhoneNumber, Client::setPhoneNumber));

        bindings.put(innField, binder.forField(innField)
                .asRequired("ИНН обязателен")
                .withValidator(value -> ValidationPattern.INN.matcher(value).matches(),
                        "ИНН должен содержать 12 цифр")
                .bind(Client::getInn, Client::setInn));

        bindings.put(addressField, binder.forField(addressField)
                .asRequired("Адрес обязателен")
                .bind(Client::getAddress, Client::setAddress));

        bindings.put(passportField, binder.forField(passportField)
                .asRequired("Скан паспорта обязателен")
                .bind(Client::getPassportScanCopy, Client::setPassportScanCopy));

        binder.addStatusChangeListener(_ ->
                saveButton.setEnabled(binder.isValid() && binder.hasChanges()));
    }

    private void setupValidation() {
        saveButton.addClickListener(_ -> {
            if (binder.writeBeanIfValid(client)) {
                fireEvent(new SaveEvent(this, client));
            }
        });
    }

    public void setClient(Client client) {
        this.client = client;
        binder.readBean(client);
    }

    private void validateField(TextField field) {
        Binder.Binding<Client, ?> binding = bindings.get(field);
        if (binding != null) {
            BindingValidationStatus<?> result = binding.validate();
            field.setInvalid(result.isError());
            result.getMessage().ifPresentOrElse(
                    field::setErrorMessage,
                    () -> field.setErrorMessage(null)
            );
        }
    }

    @Getter
    public static abstract class ClientFormEvent extends ComponentEvent<ClientForm> {
        private final Client client;

        protected ClientFormEvent(ClientForm source, Client client) {
            super(source, false);
            this.client = client;
        }
    }

    public static class SaveEvent extends ClientFormEvent {
        SaveEvent(ClientForm source, Client client) {
            super(source, client);
        }
    }

    public void addSaveListener(ComponentEventListener<SaveEvent> listener) {
        addListener(SaveEvent.class, listener);
    }
}