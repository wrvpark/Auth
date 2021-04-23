package com.wrvpark.authserver.configuration.keycloak;

import org.keycloak.Config;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.util.JsonConfigProviderFactory;
import org.keycloak.util.JsonSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.NoSuchElementException;

/**
 * @author Vahid Haghighat
 */
public class EmbeddedKeycloakApplication extends KeycloakApplication {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedKeycloakApplication.class);

    static KeycloakServerProperties keycloakServerProperties;

    protected void loadConfig() {
        JsonConfigProviderFactory factory = new RegularJsonConfigProviderFactory();
        Config.init(factory.create()
                .orElseThrow(() -> new NoSuchElementException("No value present")));
    }

    public EmbeddedKeycloakApplication() {

        super();

        updateMasterRealm();

        createMasterRealmAdminUser();

        createRVParkRealm();
    }

    private void updateMasterRealm() {
        KeycloakSession session = getSessionFactory().create();

        try {
            session.getTransactionManager().begin();

            RealmManager manager = new RealmManager(session);
            ClientModel adminCli =manager.getRealmByName("master").getClientByClientId("admin-cli");
            adminCli.setServiceAccountsEnabled(true);
            adminCli.setPublicClient(false);
            adminCli.setSecret("55c23ef4-fae0-464a-bace-b76c2ec27bc1");
            session.getTransactionManager().commit();
        } catch (Exception ex) {
            LOG.warn("Failed to update Master Realm: ", ex.getMessage());
            session.getTransactionManager().rollback();
        }

        session.close();
    }

    private void createMasterRealmAdminUser() {

        KeycloakSession session = getSessionFactory().create();

        ApplianceBootstrap applianceBootstrap = new ApplianceBootstrap(session);

        KeycloakServerProperties.AdminUser admin = keycloakServerProperties.getAdminUser();

        try {
            session.getTransactionManager().begin();
            applianceBootstrap.createMasterRealmUser(admin.getUsername(), admin.getPassword());
            session.getTransactionManager().commit();
        } catch (Exception ex) {
            LOG.warn("Couldn't create keycloak master admin user: {}", ex.getMessage());
            session.getTransactionManager().rollback();
        }

        session.close();
    }

    private void createRVParkRealm() {
        KeycloakSession session = getSessionFactory().create();

        try {
            session.getTransactionManager().begin();

            RealmManager manager = new RealmManager(session);
            Resource lessonRealmImportFile = new ClassPathResource(keycloakServerProperties.getRealmImportFile());

            manager.importRealm(
                    JsonSerialization.readValue(lessonRealmImportFile.getInputStream(), RealmRepresentation.class));

            session.getTransactionManager().commit();
        } catch (Exception ex) {
            LOG.warn("Failed to import Realm json file: {}", ex.getMessage());
            session.getTransactionManager().rollback();
        }

        session.close();
    }
}