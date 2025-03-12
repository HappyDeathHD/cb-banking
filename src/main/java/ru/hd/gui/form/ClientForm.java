package ru.hd.gui.form;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;
import ru.hd.jpa.Client;
import ru.hd.util.ValidationPattern;

import java.util.stream.Stream;

public class ClientForm extends FormLayout {
    private final TextField fullNameField = new TextField("ФИО");
    private final TextField phoneField = new TextField("Телефон");
    private final TextField innField = new TextField("ИНН");
    private final TextField addressField = new TextField("Адрес");
    private final Upload passportUpload = new Upload();
    @Getter
    private final MemoryBuffer memoryBuffer = new MemoryBuffer();
    @Getter
    private final Binder<Client> binder = new Binder<>(Client.class);
    @Getter
    private Client client;
    @Getter
    private final Button saveButton = new Button("Сохранить");

    public ClientForm() {
        configureFields();
        configureBinder();
        setupLayout();
        configureUpload();
    }

    private void configureFields() {
        Stream.of(fullNameField, phoneField, innField, addressField)
                .forEach(field -> {
                    field.setWidthFull();
                    field.setRequiredIndicatorVisible(true);
                });

        passportUpload.setReceiver(memoryBuffer);
        passportUpload.setWidthFull();
    }

    private void setupLayout() {
        add(fullNameField, phoneField, innField, addressField, passportUpload, saveButton);
        setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep("600px", 2)
        );
        setWidth("800px");
    }

    private void configureBinder() {
        binder.forField(fullNameField)
                .asRequired("ФИО обязательно")
                .bind(Client::getFullName, Client::setFullName);

        binder.forField(phoneField)
                .asRequired("Телефон обязателен")
                .withValidator(value -> ValidationPattern.PHONE.matcher(value).matches(),
                        "Формат: +7XXXXXXXXXX")
                .bind(Client::getPhoneNumber, Client::setPhoneNumber);

        binder.forField(innField)
                .asRequired("ИНН обязателен")
                .withValidator(value -> ValidationPattern.INN.matcher(value).matches(),
                        "ИНН должен содержать 12 цифр")
                .bind(Client::getInn, Client::setInn);

        binder.forField(addressField)
                .asRequired("Адрес обязателен")
                .bind(Client::getAddress, Client::setAddress);
    }

    private void configureUpload() {
        passportUpload.addSucceededListener(event -> Notification.show("Файл успешно загружен: " + event.getFileName()));

        passportUpload.addFailedListener(_ -> Notification.show("Не удалось загрузить файл"));
    }

    public void setClient(Client client) {
        this.client = client;
        binder.readBean(client);

        if (client.getPassportScan() != null) {
            Notification.show("Скан паспорта уже загружен");
        }
    }
}