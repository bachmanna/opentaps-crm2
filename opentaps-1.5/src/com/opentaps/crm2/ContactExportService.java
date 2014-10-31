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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.HttpClient;
import org.ofbiz.base.util.HttpClientException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDataSourceException;
// comment this out if not using with opentaps 1.5
import org.ofbiz.entity.Delegator;
//uncomment to use with opentaps 1.4
//import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.jdbc.SQLProcessor;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

public class ContactExportService {

    private static String MODULE = ContactExportService.class.getName();

    private ContactExportService() { }

    public static Map<String, Object> exportContactsToCrm2(DispatchContext dctx, Map<String, Object> context) {
        // comment this out if not using with opentaps 1.5
        Delegator delegator = dctx.getDelegator();
        //uncomment to use with opentaps 1.4        
        // GenericDelegator delegator = dctx.getDelegator();
        String exportContactUrl = (String) context.get("exportContactUrl");
        String exportUserUrl = (String) context.get("exportUserUrl");
        String authToken = (String) context.get("authToken");
        String clientDomain = (String) context.get("clientDomain");
        String partyId = (String) context.get("partyId");
        Map<String, String> crm2Purpose = new HashMap<String, String>();
        crm2Purpose.put("PRIMARY_EMAIL", "PRIMARY");
        crm2Purpose.put("OTHER_EMAIL", "OTHER");
        crm2Purpose.put("ORDER_EMAIL", "ORDER");
        crm2Purpose.put("BILLING_EMAIL", "BILLING_AR");
        crm2Purpose.put("PAYMENT_EMAIL", "PAYMENT_AR");
        crm2Purpose.put("SHIPMENT_EMAIL", "SHIPMENT");
        int contactsCount = 0;
        boolean firstExport = false;

        // get new or updated contacts list
        // comment this out if not using with opentaps 1.5
        SQLProcessor sqlproc = new SQLProcessor(delegator.getGroupHelperInfo("org.ofbiz"));
        //uncomment to use with opentaps 1.4
        // SQLProcessor sqlproc = new SQLProcessor(delegator.getGroupHelperName("org.ofbiz"));
        Timestamp now = UtilDateTime.nowTimestamp();

        try {
            Timestamp lastTimestamp = getLastExportTimestamp(sqlproc);
            if (lastTimestamp == null) {
                lastTimestamp = UtilDateTime.toTimestamp(1, 1, 2000, 0, 0, 1);
                firstExport = true;
            }
            ResultSet contacts = getContactsSet(sqlproc, lastTimestamp, now, partyId);
            contactsCount = exportContacts(contacts, crm2Purpose, exportContactUrl, exportUserUrl, authToken, clientDomain, contactsCount, true, sqlproc, lastTimestamp, now);

            ResultSet partyGroups = getPartyGroupsSet(sqlproc, lastTimestamp, now, partyId);
            contactsCount = exportContacts(partyGroups, crm2Purpose, exportContactUrl, exportUserUrl, authToken, clientDomain, contactsCount, false, sqlproc, lastTimestamp, now);

            if (UtilValidate.isEmpty(partyId)) {
                saveLastExportTimestamp(sqlproc, now, firstExport);
            }
            if (contactsCount == 0) {
                Debug.logInfo("No contacts for export", MODULE);
            }

        } catch (GenericDataSourceException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (SQLException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (HttpClientException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        return ServiceUtil.returnSuccess("Exported " + contactsCount + " contacts");
    }

    private static Timestamp getLastExportTimestamp(SQLProcessor sqlproc) throws GenericDataSourceException, GenericEntityException, SQLException {
        if (sqlproc == null) {
            throw new IllegalArgumentException();
        }
        Timestamp lastTimestamp = null;
        String qStr = "SELECT last_successful_synch_time FROM ENTITY_SYNC WHERE  entity_sync_id = 'exportContactsToCrm2'";
        sqlproc.prepareStatement(qStr);
        ResultSet result = sqlproc.executeQuery();

        if (result != null && result.next()) {
            lastTimestamp = result.getTimestamp("last_successful_synch_time");

            result.close();
        }

        return lastTimestamp;
    }

    private static void saveLastExportTimestamp(SQLProcessor sqlproc, Timestamp lastTimestamp, boolean firstExport) throws GenericDataSourceException, GenericEntityException, SQLException {
        if (lastTimestamp == null) {
            throw new IllegalArgumentException();
        }

        String qStr = "";
        if (firstExport) {
            qStr = "INSERT INTO ENTITY_SYNC (entity_sync_id, last_successful_synch_time) VALUES ('exportContactsToCrm2', ?)";
        } else {
            qStr = "UPDATE ENTITY_SYNC SET last_successful_synch_time = ? WHERE entity_sync_id = 'exportContactsToCrm2'";
        }
        sqlproc.prepareStatement(qStr);
        sqlproc.setValue(lastTimestamp);
        sqlproc.executeUpdate();
    }

    private static ResultSet getContactsSet(SQLProcessor sqlproc, Timestamp lastTimestamp, Timestamp now, String partyId) throws GenericDataSourceException, GenericEntityException, SQLException {
        if (sqlproc == null || lastTimestamp == null || now == null) {
            throw new IllegalArgumentException();
        }
        ResultSet result = null;

        StringBuilder qStr =  new StringBuilder("SELECT PCM.party_id, PS.first_name, PS.last_name, PCM.contact_mech_id, CM.info_string, PCMP.contact_mech_purpose_type_id FROM PARTY_CONTACT_MECH AS PCM ")
            .append("JOIN CONTACT_MECH AS CM ON PCM.contact_mech_id = CM.contact_mech_id ")
            .append("LEFT JOIN PARTY_CONTACT_MECH_PURPOSE AS PCMP ON PCM.party_id = PCMP.party_id AND PCM.contact_mech_id = PCMP.contact_mech_id ")
            .append("JOIN PERSON as PS ON PCM.party_id = PS.party_id ");
        if (UtilValidate.isEmpty(partyId)) {
            qStr.append("JOIN ")
                .append("(SELECT party_id FROM (SELECT PCM.party_id FROM PARTY_CONTACT_MECH AS PCM ")
                .append("WHERE PCM.last_updated_stamp >= ? ")
                .append("UNION ")
                .append("SELECT PS.party_id FROM PERSON AS PS ")
                .append("WHERE PS.last_updated_stamp >= ? ")
                .append(") AS NEW_P) AS NEW_ITEM ON PCM.party_id = NEW_ITEM.party_id ");
        }
        qStr.append("WHERE PCM.from_date <= ? ")
            .append("AND (PCM.thru_date >= ? OR PCM.thru_date is null) ")
            .append("AND ((PCMP.from_date <= ? AND (PCMP.thru_date >= ? OR PCMP.thru_date is null)) OR PCMP.contact_mech_purpose_type_id IS NULL ) ")
            .append("AND CM.contact_mech_type_id = 'EMAIL_ADDRESS' ");
        if (UtilValidate.isNotEmpty(partyId)) {
            qStr.append("AND PCM.party_id = ? ");
        }
        qStr.append("ORDER BY PCM.party_id, PCM.contact_mech_id ");

        sqlproc.prepareStatement(qStr.toString());
        if (UtilValidate.isEmpty(partyId)) {
            sqlproc.setValue(lastTimestamp);
            sqlproc.setValue(lastTimestamp);
        }
        sqlproc.setValue(now);
        sqlproc.setValue(now);
        sqlproc.setValue(now);
        sqlproc.setValue(now);
        if (UtilValidate.isNotEmpty(partyId)) {
            sqlproc.setValue(partyId);
        }

        result = sqlproc.executeQuery();

        return result;
    }

    private static ResultSet getPartyGroupsSet(SQLProcessor sqlproc, Timestamp lastTimestamp, Timestamp now, String partyId) throws GenericDataSourceException, GenericEntityException, SQLException {
        if (sqlproc == null || lastTimestamp == null || now == null) {
            throw new IllegalArgumentException();
        }
        ResultSet result = null;

        StringBuilder qStr =  new StringBuilder("SELECT PCM.party_id, PS.group_name, PCM.contact_mech_id, CM.info_string, PCMP.contact_mech_purpose_type_id FROM PARTY_CONTACT_MECH AS PCM ")
            .append("JOIN CONTACT_MECH AS CM ON PCM.contact_mech_id = CM.contact_mech_id ")
            .append("LEFT JOIN PARTY_CONTACT_MECH_PURPOSE AS PCMP ON PCM.party_id = PCMP.party_id AND PCM.contact_mech_id = PCMP.contact_mech_id ")
            .append("JOIN PARTY_GROUP as PS ON PCM.party_id = PS.party_id ");
        if (UtilValidate.isEmpty(partyId)) {
            qStr.append("JOIN ")
                .append("(SELECT party_id FROM (SELECT PCM.party_id FROM PARTY_CONTACT_MECH AS PCM ")
                .append("WHERE PCM.last_updated_stamp >= ? ")
                .append("UNION ")
                .append("SELECT PS.party_id FROM PARTY_GROUP AS PS ")
                .append("WHERE PS.last_updated_stamp >= ? ")
                .append(") AS NEW_P) AS NEW_ITEM ON PCM.party_id = NEW_ITEM.party_id ");
        }
        qStr.append("WHERE PCM.from_date <= ? ")
            .append("AND (PCM.thru_date >= ? OR PCM.thru_date is null) ")
            .append("AND ((PCMP.from_date <= ? AND (PCMP.thru_date >= ? OR PCMP.thru_date is null)) OR PCMP.contact_mech_purpose_type_id IS NULL ) ")
            .append("AND CM.contact_mech_type_id = 'EMAIL_ADDRESS' ");
        if (UtilValidate.isNotEmpty(partyId)) {
            qStr.append("AND PCM.party_id = ? ");
        }
        qStr.append("ORDER BY PCM.party_id, PCM.contact_mech_id ");

        sqlproc.prepareStatement(qStr.toString());
        if (UtilValidate.isEmpty(partyId)) {
            sqlproc.setValue(lastTimestamp);
            sqlproc.setValue(lastTimestamp);
        }
        sqlproc.setValue(now);
        sqlproc.setValue(now);
        sqlproc.setValue(now);
        sqlproc.setValue(now);
        if (UtilValidate.isNotEmpty(partyId)) {
            sqlproc.setValue(partyId);
        }

        result = sqlproc.executeQuery();

        return result;
    }

    private static String exportContact(String partyId, String firstName, String lastName, String companyName, List<String> emails, List<String> purposes, String exportContactUrl, String authToken, String clientDomain) throws HttpClientException {
        if (partyId == null || exportContactUrl == null) {
            throw new IllegalArgumentException();
        }
        String contactId = null;

        if (UtilValidate.isNotEmpty(emails) && UtilValidate.isNotEmpty(purposes) && (emails.size() == purposes.size())) {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("attribute_opentapsPartyId", partyId);
            if (UtilValidate.isNotEmpty(firstName)) {
                params.put("firstName", firstName);
            }
            if (UtilValidate.isNotEmpty(lastName)) {
                params.put("lastName", lastName);
            }
            if (UtilValidate.isNotEmpty(companyName)) {
                params.put("companyName", companyName);
            }

            params.put("clientDomain", clientDomain);
            params.put("authToken", authToken);
            params.put("emailsNumber", emails.size());

            for (int i = 0; i < emails.size(); i++) {
                params.put("email" + i, emails.get(i));
                params.put("emailPurpose" + i, purposes.get(i));
            }

            HttpClient httpClient = new HttpClient(exportContactUrl, params, UtilMisc.toMap("content-type", "application/x-www-form-urlencoded"));

            String response = httpClient.post();
            if (UtilValidate.isNotEmpty(response)) {
                JSONObject responseJSON = (JSONObject) JSONSerializer.toJSON(response);
                if (UtilValidate.isNotEmpty(responseJSON)) {
                    JSONObject result = responseJSON.getJSONObject("result");
                    if (UtilValidate.isNotEmpty(result)) {
                        JSONObject resultValue = result.getJSONObject("resultValue");
                        if (UtilValidate.isNotEmpty(resultValue)) {
                            JSONObject contact = resultValue.getJSONObject("contact");
                            if (UtilValidate.isNotEmpty(contact)) {
                                contactId = contact.getString("contactId");
                            }
                        }
                    }
                }
            }
        }

        return contactId;
    }

    private static int exportContacts(ResultSet contacts, Map<String, String> crm2Purpose, String exportContactUrl, String exportUserUrl, String authToken, String clientDomain, int contactsCount, boolean isContact, SQLProcessor sqlproc, Timestamp lastTimestamp, Timestamp now) throws HttpClientException, SQLException, GenericDataSourceException, GenericEntityException {
        if (contacts != null) {
            String currentPartyId = null;
            List<String> emails =  new ArrayList<String>();
            List<String> purposes =  new ArrayList<String>();

            String partyId = null;
            String firstName = null;
            String lastName = null;
            String companyName = null;
            String email = null;
            String purpose = null;
            while (contacts.next()) {
                partyId = contacts.getString("party_id");

                if (!partyId.equalsIgnoreCase(currentPartyId)) {
                    if (currentPartyId != null && UtilValidate.isNotEmpty(emails)) {
                        String contactId = exportContact(currentPartyId, firstName, lastName, companyName, emails, purposes, exportContactUrl, authToken, clientDomain);
                        exportUsers(contactId, currentPartyId, exportUserUrl, authToken, clientDomain, sqlproc, lastTimestamp, now);
                    }

                    currentPartyId = partyId;
                    emails.clear();
                    purposes.clear();
                    contactsCount++;
                }

                if (isContact) {
                    firstName = contacts.getString("first_name");
                    lastName = contacts.getString("last_name");
                } else {
                    companyName = contacts.getString("group_name");
                }
                email = contacts.getString("info_string");
                purpose = null;
                String purposeId = contacts.getString("contact_mech_purpose_type_id");
                if (UtilValidate.isNotEmpty(purposeId)) {
                    purpose = crm2Purpose.get(purposeId);
                } else {
                    purpose = "PRIMARY";
                }

                if (UtilValidate.isNotEmpty(email) && UtilValidate.isNotEmpty(purpose)) {
                    emails.add(email);
                    purposes.add(purpose);
                }
            }
            if (currentPartyId != null) {
                String contactId = exportContact(currentPartyId, firstName, lastName, companyName, emails, purposes, exportContactUrl, authToken, clientDomain);
                exportUsers(contactId, currentPartyId, exportUserUrl, authToken, clientDomain, sqlproc, lastTimestamp, now);
            }
            contacts.close();
        }

        return contactsCount;
    }

    private static void exportUsers(String contactId, String partyId, String exportUserUrl, String authToken, String clientDomain, SQLProcessor sqlproc, Timestamp lastTimestamp, Timestamp now) throws GenericDataSourceException, GenericEntityException, SQLException, HttpClientException {
        ResultSet userLoginSet = getContactUserLoginSet(sqlproc, lastTimestamp, now, partyId);
        List<String> userIdList = new ArrayList<String>();
        if (userLoginSet != null) {
            while (userLoginSet.next()) {
                String userId = userLoginSet.getString("user_login_id");
                if (!userIdList.contains(userId)) {
                    exportUser(userId, "INTERNAL", contactId, exportUserUrl, authToken, clientDomain);
                    userIdList.add(userId);
                }
            }
        }
    }

    private static void exportUser(String userId, String userIdType, String contactId, String exportUserUrl, String authToken, String clientDomain) throws HttpClientException {
        if (UtilValidate.isNotEmpty(contactId) && UtilValidate.isNotEmpty(exportUserUrl)) {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("userId", userId);
            params.put("userIdType", userIdType);
            params.put("contactId", contactId);
            params.put("clientDomain", clientDomain);
            params.put("authToken", authToken);

            HttpClient httpClient = new HttpClient(exportUserUrl, params, UtilMisc.toMap("content-type", "application/x-www-form-urlencoded"));

            httpClient.post();
        }
    }

    private static ResultSet getContactUserLoginSet(SQLProcessor sqlproc, Timestamp lastTimestamp, Timestamp now, String partyId) throws GenericDataSourceException, GenericEntityException, SQLException {
        if (sqlproc == null || lastTimestamp == null || now == null || partyId == null) {
            throw new IllegalArgumentException();
        }
        ResultSet result = null;

        StringBuilder qStr =  new StringBuilder("SELECT UL.user_login_id, UL.party_id, UL.current_password, UL.password_hint, UL.enabled, ULSG.group_id, SGP.permission_id FROM USER_LOGIN UL ")
             .append("JOIN USER_LOGIN_SECURITY_GROUP ULSG ON ULSG.user_login_id = UL.user_login_id ")
             .append("JOIN SECURITY_GROUP_PERMISSION SGP ON SGP.group_id = ULSG.group_id ")
             .append("WHERE UL.party_id = ? ")
             .append("AND (UL.enabled = 'Y' OR UL.enabled is null) ")
             .append("AND SGP.permission_id in ('FINANCIALS_ADMIN', 'FINANCIALS_VIEW', 'CRMSFA_VIEW') ")
             .append("AND ULSG.created_stamp >= ? ")
             .append("AND (ULSG.from_date <= ? AND (ULSG.thru_date >= ? OR ULSG.thru_date is null)) ");

        sqlproc.prepareStatement(qStr.toString());
        sqlproc.setValue(partyId);
        sqlproc.setValue(lastTimestamp);
        sqlproc.setValue(now);
        sqlproc.setValue(now);

        result = sqlproc.executeQuery();

        return result;
    }
}
