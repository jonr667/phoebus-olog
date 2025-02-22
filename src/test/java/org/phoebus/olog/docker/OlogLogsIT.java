/*
 * Copyright (C) 2021 European Spallation Source ERIC.
 */

package org.phoebus.olog.docker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.phoebus.olog.entity.Log;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration tests for Olog and Elasticsearch that make use of existing dockerization
 * with docker-compose.yml / Dockerfile.
 *
 * <p>
 * Focus of this class is to have Olog and Elasticsearch up and running together with usage of
 * {@link org.phoebus.olog.OlogResourceDescriptors#LOG_RESOURCE_URI}.
 *
 * @author Lars Johansson
 *
 * @see org.phoebus.olog.LogResource
 */
@Testcontainers
public class OlogLogsIT {

    // Note
    //     ------------------------------------------------------------------------------------------------
    //     About
    //         requires
    //             elastic indices for Olog, ensured at start-up
    //             environment
    //                 default ports, 8080 for Olog, 9200 for Elasticsearch
    //                 demo_auth enabled
    //         json
    //             check(s) for json and objects written as json
    //             objects representing data/entities sent/received - serialized/deserialized
    //         docker containers shared for tests
    //             each test to leave Olog, Elasticsearch in clean state - not disturb other tests
    //             clean state may be content with status inactive
    //         each test uses multiple endpoints in Olog API
    //     ------------------------------------------------------------------------------------------------
    //     Olog - Service Documentation
    //         https://olog.readthedocs.io/en/latest/
    //     ------------------------------------------------------------------------------------------------
    //     OLOG API                                         LogbooksResource
    //     --------------------                             --------------------
    //     Retrieve a Log                  .../logs/<id>    (GET)       getLog(String)
    //     Retrieve attachment for Log     .../logs/attachments/{logId}/{attachmentName}
    //                                                      (GET)       findResources(String, String)
    //     List Logs / Query by Pattern    .../logs         (GET)       findAll()
    //     Create a Log                    .../logs         (PUT)       createLog(String, Log, Principal)
    //     Upload attachment               .../logs/attachments/{logId}
    //                                                      (POST)      uploadAttachment(String, MultipartFile, String, String, String)
    //     Upload multiple attachments     .../logs/attachments-multi/{logId}
    //                                                      (POST)      uploadMultipleAttachments(String, MultipartFile[])
    //     ------------------------------------------------------------------------------------------------

    static final String LOGS = "/logs";

    static final String HTTP_IP_PORT_OLOG_LOGS            = ITUtil.HTTP +                           ITUtil.IP_PORT_OLOG + LOGS;
    static final String HTTP_AUTH_USER_IP_PORT_OLOG_LOGS  = ITUtil.HTTP + ITUtil.AUTH_USER  + "@" + ITUtil.IP_PORT_OLOG + LOGS;
    static final String HTTP_AUTH_ADMIN_IP_PORT_OLOG_LOGS = ITUtil.HTTP + ITUtil.AUTH_ADMIN + "@" + ITUtil.IP_PORT_OLOG + LOGS;

    @Container
    public static final DockerComposeContainer<?> ENVIRONMENT =
        new DockerComposeContainer<>(new File("docker-compose.yml"))
            .waitingFor(ITUtil.OLOG, Wait.forLogMessage(".*Started Application.*", 1));

    @Test
    public void ologUp() {
        try {
            String address = ITUtil.HTTP_IP_PORT_OLOG;
            int responseCode = ITUtil.doGet(address);

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
        } catch (IOException e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#LOG_RESOURCE_URI}.
     */
    @Test
    public void handleLogRetrieveCheck() {
        // what
        //     check(s) for retrieve log
        //         e.g.
        //             retrieve non-existing log
        //     --------------------------------------------------------------------------------
        //     x   Retrieve a Log
        //         Retrieve attachment for Log
        //         List Logs / Query by Pattern
        //         Create a Log
        //         Upload attachment
        //         Upload multiple attachments

        try {
            String[] response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGS + "/l11");
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_NOT_FOUND);
        } catch (IOException e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#LOG_RESOURCE_URI}.
     */
    @Test
    public void handleLogCreateCheckJson() {
        // what
        //     check(s) for create log
        //         e.g.
        //             user without required role
        //             content
        //                 json       - incomplete
        //                 name       - null, empty
        //                 owner      - null, empty
        //                 state      - null (empty, incorrect value (ok: Active, Inactive))
        //     --------------------------------------------------------------------------------
        //         Retrieve a Log
        //         Retrieve attachment for Log
        //     x   List Logs / Query by Pattern
        //     x   Create a Log
        //         Upload attachment
        //         Upload multiple attachments

        String json_incomplete1 = "{\"incomplete\"}";
        String json_incomplete2 = "{\"incomplete\"";
        String json_incomplete3 = "{\"incomplete}";
        String json_incomplete4 = "{\"\"}";
        String json_incomplete5 = "{incomplete\"}";
        String json_incomplete6 = "\"incomplete\"}";
        String json_incomplete7 = "{";
        String json_incomplete8 = "}";
        String json_incomplete9 = "\"";

        try {
            String[] response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGS);
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.runShellCommand(createCurlLogForAdmin(json_incomplete1));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlLogForAdmin(json_incomplete2));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlLogForAdmin(json_incomplete3));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlLogForAdmin(json_incomplete4));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlLogForAdmin(json_incomplete5));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlLogForAdmin(json_incomplete6));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlLogForAdmin(json_incomplete7));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlLogForAdmin(json_incomplete8));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlLogForAdmin(json_incomplete9));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGS);
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);
        } catch (IOException e) {
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test {@link org.phoebus.olog.OlogResourceDescriptors#LOG_RESOURCE_URI}.
     */
    @Test
    public void handleLogCreateCheck() {
        // what
        //     check(s) for create log
        //         e.g.
        //             user without required role
        //             content
        //                 json       - incomplete
        //                 name       - null, empty
        //                 owner      - null, empty
        //                 state      - null (empty, incorrect value (ok: Active, Inactive))
        //     --------------------------------------------------------------------------------
        //         Retrieve a Log
        //         Retrieve attachment for Log
        //     x   List Logs / Query by Pattern
        //     x   Create a Log
        //         Upload attachment
        //         Upload multiple attachments

        Log log_check = new Log.LogBuilder().build();

        ObjectMapper mapper = new ObjectMapper();

        try {
            String[] response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGS);
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);

            response = ITUtil.runShellCommand(createCurlLogForUser(mapper.writeValueAsString(log_check)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.runShellCommand(createCurlLogForAdmin(mapper.writeValueAsString(log_check)));
            ITUtil.assertResponseLength2Code(response, HttpURLConnection.HTTP_BAD_REQUEST);

            response = ITUtil.doGetJson(HTTP_IP_PORT_OLOG_LOGS);
            ITUtil.assertResponseLength2CodeOKContent(response, ITUtil.EMPTY_JSON);
        } catch (IOException e) {
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Utility method to return curl to create log for regular user.
     *
     * @param logJson log json
     * @return curl to create log
     */
    private static String createCurlLogForUser(String logJson) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XPUT -i " + HTTP_AUTH_USER_IP_PORT_OLOG_LOGS + " -d '" + logJson + "'";
    }

    /**
     * Utility method to return curl to create log for admin user.
     *
     * @param logJson log json
     * @return curl to create log
     */
    private static String createCurlLogForAdmin(String logJson) {
        return "curl -H " + ITUtil.HEADER_JSON + " -XPUT -i " + HTTP_AUTH_ADMIN_IP_PORT_OLOG_LOGS + " -d '" + logJson + "'";
    }

}
