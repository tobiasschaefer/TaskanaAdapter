package pro.taskana.adapter.camunda.parselistener;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.UUID;

import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.camunda.bpm.engine.impl.cfg.IdGenerator;
import org.camunda.bpm.engine.impl.cfg.JtaProcessEngineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TaskanaParseListenerProcessEngineConfiguration extends JtaProcessEngineConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskanaParseListenerProcessEngineConfiguration.class);

    private static final String SQL_CREATE_TASKANA_SCHEMA = "CREATE SCHEMA IF NOT EXISTS taskana_tables";

    private static final String SQL_CREATE_SEQUENCE = "CREATE SEQUENCE IF NOT EXISTS taskana_tables.event_store_id_seq"
                                                        + " INCREMENT 1"
                                                        + " START 1"
                                                        + " MINVALUE 1"
                                                        + " MAXVALUE 2147483647"
                                                        + " CACHE 1 ";

    private static final String SQL_CREATE_EVENT_STORE = "CREATE TABLE IF NOT EXISTS taskana_tables.event_store"
                                                            + "("
                                                            + " ID integer NOT NULL DEFAULT nextval('taskana_tables.event_store_id_seq'::regclass),"
                                                            + " TYPE text COLLATE pg_catalog.\"default\","
                                                            + " CREATED timestamp(4) without time zone,"
                                                            + " PAYLOAD text COLLATE pg_catalog.\"default\","
                                                            + " CONSTRAINT event_store_pkey PRIMARY KEY (id)"
                                                            + ")";

    private IdGenerator uuidGenerator = () -> UUID.randomUUID().toString();


    @Override
    protected void init() {
        setIdGenerator(uuidGenerator);
        initOutbox();
        initCustomPostParseListener();
        super.init();
    }

    private void initCustomPostParseListener() {

        if (getCustomPostBPMNParseListeners() == null) {
            setCustomPostBPMNParseListeners(new ArrayList<BpmnParseListener>());
        }

        getCustomPostBPMNParseListeners().add(new TaskanaParseListener());

    }

    private void initOutbox() {

        setDataSource(dataSource);

        try (Connection connection = dataSource.getConnection()) {

            createTaskanaSchema(connection);
            createTaskanaTable(connection);

        } catch (Exception e) {
            LOGGER.warn("Caught {} while trying to initialize the outbox-table", e);
        }
    }

    private void createTaskanaTable(Connection connection) {

        try ( Statement statement = connection.createStatement()) {

            statement.execute(SQL_CREATE_EVENT_STORE);

        } catch (Exception e) {
            LOGGER.warn("Caught {} while trying to initialize the taskana table", e);

        }
    }

    private void createTaskanaSchema(Connection connection) {

        try (Statement statement = connection.createStatement();){

            statement.execute(SQL_CREATE_TASKANA_SCHEMA);
            statement.execute(SQL_CREATE_SEQUENCE);

        } catch (Exception e) {
            LOGGER.warn("Caught {} while trying to initialize the taskana schema", e);

        }
    }

}