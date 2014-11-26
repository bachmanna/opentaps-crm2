/*
 * Copyright (c) Open Source Strategies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.opentaps.crm2;

import java.util.Map;
import javax.servlet.http.HttpSession;

import net.sf.json.JSONObject;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;

/**
 * Actions for the Activities Widget.
 */
public final class ActivitiesWidgetActions {

    private ActivitiesWidgetActions() { }

    private static final String MODULE = ActivitiesWidgetActions.class.getName();

    private static final int CONNECT_TIMEOUT = 1000;
    private static final int TIMEOUT = 5000;

    private static final String CONFIG_RESOURCE = "crm2";
    private static final String OAUTH_BASE_URL = UtilProperties.getPropertyValue(CONFIG_RESOURCE, "crm2.oauth.baseUrl");
    private static final String ACTIVITIES_WIDGET_BASE_OPENTAPS_URL = UtilProperties.getPropertyValue(CONFIG_RESOURCE, "crm2.activities.widget.baseOpentapsUrl");
    private static final String ACTIVITIES_WIDGET_BASE_URL = UtilProperties.getPropertyValue(CONFIG_RESOURCE, "crm2.activities.widget.baseUrl");
    private static final String ACTIVITIES_WIDGET_AUTH_TOKEN = UtilProperties.getPropertyValue(CONFIG_RESOURCE, "crm2.activities.widget.authToken");
    private static final String ACTIVITIES_WIDGET_CLIENT_DOMAIN = UtilProperties.getPropertyValue(CONFIG_RESOURCE, "crm2.activities.widget.clientDomain");
    private static final boolean ACTIVITIES_WIDGET_USE_PERM_AUTH_TOKEN = "Y".equals(UtilProperties.getPropertyValue(CONFIG_RESOURCE, "crm2.activities.widget.usePermAuthToken"));

    private static void validateConfig(Map<String, Object> context) throws IllegalStateException {
        if (UtilValidate.isEmpty(ACTIVITIES_WIDGET_AUTH_TOKEN)) {
            context.put("error_noAuthToken", true);
            throw new IllegalStateException("No authToken configured in crm2.properties, if you do not have a token yet please sign up for an account for opentaps CRM2, then please correct the crm2.activities.widget.authToken entry in the configuration file and restart your instance.");
        }
        if (UtilValidate.isEmpty(ACTIVITIES_WIDGET_BASE_URL)) {
            throw new IllegalStateException("No baseUrl configured in crm2.properties, please correct the crm2.activities.widget.baseOpentapsUrl entry in the configuration file and restart your instance.");
        }
        if (UtilValidate.isEmpty(ACTIVITIES_WIDGET_CLIENT_DOMAIN)) {
            throw new IllegalStateException("No clientDomain configured in crm2.properties, please correct the crm2.activities.widget.clientDomain entry in the configuration file and restart your instance.");
        }
        if (!ACTIVITIES_WIDGET_USE_PERM_AUTH_TOKEN) {
            if (UtilValidate.isEmpty(OAUTH_BASE_URL)) {
                throw new IllegalStateException("No oauth baseUrl configured in crm2.properties, please correct the crm2.oauth.baseUrl entry in the configuration file and restart your instance.");
            }
        }
    }

    /**
     * Action Activities Widget screen.
     * @param context the screen context
     * @throws GeneralException if an error occurs
     */
    public static void getWidgetConfigurationPermanentToken(Map<String, Object> context) throws GeneralException {
        getWidgetConfiguration(context, false);
    }

    /**
     * Action Activities Widget screen.
     * @param context the screen context
     * @throws GeneralException if an error occurs
     */
    public static void getWidgetConfiguration(Map<String, Object> context) throws GeneralException {
        getWidgetConfiguration(context, true);
    }

    /**
     * Action Activities Widget screen.
     * @param context the screen context
     * @param getTempToken a <code>boolean</code> value
     * @exception GeneralException if an error occurs
     */
    private static void getWidgetConfiguration(Map<String, Object> context, boolean getTempToken) throws GeneralException {
        try {
            validateConfig(context);
        } catch (IllegalStateException e) {
            Debug.logError(e, MODULE);
            context.put("widgetConfigurationError", e.getMessage());
            context.put("isTempTokenValid", false);
            return;
        }

        // obtain a temporary, this can be skip setting the usePermAuthToken settings for testing
        if (getTempToken && !ACTIVITIES_WIDGET_USE_PERM_AUTH_TOKEN) {
            String t = null;
            boolean isTempTokenValid = false;
            // check if we have a Session
            HttpSession session = (HttpSession) context.get("session");
            if (session != null) {
                t = (String) session.getAttribute(MODULE + "_authToken");
            }
            if (UtilValidate.isNotEmpty(t)) {
                // we should first validate the token
                try {
                    HttpClient httpClient = new HttpClient(OAUTH_BASE_URL + "/validate");
                    httpClient.setConnectTimeout(CONNECT_TIMEOUT);
                    httpClient.setTimeout(TIMEOUT);
                    httpClient.setHeader("content-type", "application/json");
                    JSONObject response = JSONObject.fromObject(httpClient.post("{\"authToken\":\"" + t + "\"}"));
                    // the response is also in JSON and should have authenticated set to true
                    isTempTokenValid = response.getBoolean("authenticated");
                    if (!isTempTokenValid) {
                        Debug.logWarning("Found temp authToken [" + t + "] in session but this token is no longer valid.", MODULE);
                    }
                } catch (Exception e) {
                    Debug.logError(e, MODULE);
                    isTempTokenValid = false;
                }
            }
            // if we did not find a token or the token was invalid, then request a new one
            if (!isTempTokenValid) {
                try {
                    HttpClient httpClient = new HttpClient(OAUTH_BASE_URL + "/temp-token");
                    httpClient.setConnectTimeout(CONNECT_TIMEOUT);
                    httpClient.setTimeout(TIMEOUT);
                    httpClient.setParameter("authToken", ACTIVITIES_WIDGET_AUTH_TOKEN);
                    JSONObject response = JSONObject.fromObject(httpClient.post());
                    t = response.getString("authToken");
                    isTempTokenValid = true;
                    if (session != null) {
                        session.setAttribute(MODULE + "_authToken", t);
                    }
                } catch (Exception e) {
                    Debug.logError(e, MODULE);
                    isTempTokenValid = false;
                }

            }
            // now put the temp token in the context, if we could not get a valid one indicate that as well
            context.put("authToken", t);
            context.put("isTempTokenValid", isTempTokenValid);
        } else {
            context.put("authToken", ACTIVITIES_WIDGET_AUTH_TOKEN);
            context.put("isTempTokenValid", true);
        }

        context.put("widgetBaseUrl", ACTIVITIES_WIDGET_BASE_URL);
        context.put("clientDomain", ACTIVITIES_WIDGET_CLIENT_DOMAIN);
    }

    /**
     * Action Activities Widget screen.
     * @param context the screen context
     */
    public static void getContactLink(Map<String, Object> context) {
        context.put("pageUrlBase", ACTIVITIES_WIDGET_BASE_OPENTAPS_URL + "/crmsfa/control/viewContact?partyId=");
    }

    /**
     * Action Activities Widget screen.
     * @param context the screen context
     */
    public static void getAccountLink(Map<String, Object> context) {
        context.put("pageUrlBase", ACTIVITIES_WIDGET_BASE_OPENTAPS_URL + "/crmsfa/control/viewAccount?partyId=");
    }

    /**
     * Action Activities Widget screen.
     * @param context the screen context
     */
    public static void getSupplierLink(Map<String, Object> context) {
        context.put("pageUrlBase", ACTIVITIES_WIDGET_BASE_OPENTAPS_URL + "/purchasing/control/viewSupplier?partyId=");
    }

    /**
     * Action Activities Widget screen.
     * @param context the screen context
     */
    public static void getSalesOrderLink(Map<String, Object> context) {
        context.put("pageUrlBase", ACTIVITIES_WIDGET_BASE_OPENTAPS_URL + "/crmsfa/control/orderview?orderId=");
    }

    /**
     * Action Activities Widget screen.
     * @param context the screen context
     */
    public static void getPurchaseOrderLink(Map<String, Object> context) {
        context.put("pageUrlBase", ACTIVITIES_WIDGET_BASE_OPENTAPS_URL + "/purchasing/control/orderview?orderId=");
    }

    /**
     * Action Activities Widget screen.
     * @param context the screen context
     */
    public static void getInvoiceLink(Map<String, Object> context) {
        context.put("pageUrlBase", ACTIVITIES_WIDGET_BASE_OPENTAPS_URL + "/financials/control/viewInvoice?invoiceId=");
    }

    /**
     * Action Activities Widget screen.
     * @param context the screen context
     */
    public static void getPaymentLink(Map<String, Object> context) {
        context.put("pageUrlBase", ACTIVITIES_WIDGET_BASE_OPENTAPS_URL + "/financials/control/viewPayment?paymentId=");
    }
}
