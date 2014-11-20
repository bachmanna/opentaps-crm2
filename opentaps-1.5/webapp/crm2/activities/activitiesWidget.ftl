<#--
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
-->
<script type="text/javascript">
  window.require = { waitSeconds: 600 };
</script>
<script type="text/javascript" charset="utf-8" src="${widgetBaseUrl!}/contactsWidget/client/js/libs/requirejs/require.js" data-main="${widgetBaseUrl!}/js/activity-widgets/build/activity-widgets.packed.js" >
</script>

<#assign userParty = userLogin.getRelatedOne("Party") />
<#if userParty?has_content>
  <#if userParty.partyTypeId == "PERSON">
    <#assign userParty = userParty.getRelatedOne("Person") />
    <#assign userName = "${userParty.firstName} ${userParty.lastName}" />
  <#elseif userParty.partyTypeId == "PARTY_GROUP">
    <#assign userParty = userParty.getRelatedOne("PartyGroup") />
    <#assign userName = "${userParty.groupName}" />
  </#if>
</#if>

<#assign queryParam = ""/>
<#assign createParam = ""/>
<#assign widgetClass = ""/>
<#assign linkPageId = ""/>

<#-- Tag paramaters -->
<#assign tagName = ""/>
<#assign tagKeywords = ""/>
<#assign tagUrl = ""/>
<#assign useSearch = false/>

<#-- Used to assign a note to particular application object, e.g. to an order. -->
<#assign relateTo = ""/>

<#-- tag synonyms
    ["Order", "PO", "Orders"],
    ["Invoice", "Bill", "Order"],
    ["Payment"]
-->

<#if orderId?has_content>
  <#assign linkPageId = orderId />
  <#assign widgetClass = "opentapsOrderId_${orderId}"/>
  <#assign useSearch = true/>
  <#assign notTerms = "opentapsOrderId:${orderId}">
  <#assign tagName = "SO::${orderId}"/>
  <#if order?has_content>
    <#if order.isPurchaseOrder()>
      <#assign tagName = "PO::${orderId}"/>
    </#if>
  </#if>
  <#assign tagKeywords = "${orderId},order|po|orders"/>
<#elseif invoiceId?has_content>
  <#assign linkPageId = invoiceId />
  <#assign widgetClass = "opentapsInvoiceId_${invoiceId}"/>
  <#assign useSearch = true/>
  <#assign tagKeywords = "${invoiceId},invoice|bill|order"/>
  <#if invoice?has_content && invoice.referenceNumber?has_content>
      <#assign tagKeywords =  tagKeywords + ",${invoice.referenceNumber!}"/>
   </#if>
  <#if ordersList?has_content>
      <#assign tagKeywords =  tagKeywords + ",${ordersList!}"/>
   </#if>
  <#assign notTerms = "opentapsInvoiceId:${invoiceId}">
  <#assign tagName = "Invoice::${invoiceId}"/>
<#elseif paymentId?has_content>
  <#assign linkPageId = paymentId />
  <#assign widgetClass = "opentapsPaymentId_${paymentId}"/>
  <#assign useSearch = true/>
  <#assign tagKeywords = "${paymentId},payment"/>
  <#if paymentApplicationsList?has_content>
    <#list paymentApplicationsList as invoice>
      <#if invoice.invoiceId?has_content>
        <#assign tagKeywords = "${paymentId},payment"/>
      </#if>
      <#if invoice.invoiceRefNum?has_content>
        <#assign tagKeywords =  tagKeywords + ",${invoice.invoiceRefNum!}"/>
      </#if>
    </#list>
  </#if>
  <#assign tagName = "Payment::${paymentId}"/>
  <#assign notTerms = "opentapsPaymentId:${paymentId}">
<#elseif parameters.partyId?has_content>
  <#assign linkPageId = parameters.partyId />
  <#assign tagName = "party::${parameters.partyId}"/>
  <#assign contactTagName = "party::${parameters.partyId}"/>
  <#assign tagKeywords = "party,${parameters.partyId}"/>
  <#assign queryParam  = "&contactTagName=party::${parameters.partyId}" />
  <#assign createParam = "&contactTagName=party::${parameters.partyId}" />
  <#assign widgetClass = "opentapsPartyId_${parameters.partyId}"/>
</#if>
<#if pageUrlBase?has_content && linkPageId?has_content>
  <#assign tagUrl = "${StringUtil.wrapString(pageUrlBase)?url('ISO-8859-1')}${linkPageId}"/>
</#if>

<#assign userParam = ""/>
<#if userLogin?has_content>
  <#assign userParam>created_by_user_userLoginId=${userLogin.userLoginId}<#if userName?has_content>&created_by_user_name=${userName}<#else>${userLogin.userLoginId}</#if></#assign>
</#if>

<link rel="stylesheet" type="text/css" href="https://crm2.opentaps.com/contactsWidget/client/css/opentapsActivitiesWidgets.css"/>

<a name="activities"></a>
<div class="crm2widgets">
  <div class="crm2header"><a href="http://www.opentaps.com" target="blank_"><img src="https://crm2.opentaps.com/contactsWidget/client/img/opentaps-crm2-brand.png" alt="Opentaps CRM2" /></a></div>
  <div class="crm2body">
    <table>
      <tr>
        <#if isTempTokenValid && clientDomain?has_content>
          <#if (queryParam?has_content || tagName?has_content)>
            <td  <#if !useSearch> width="100%" <#else> width="50%" </#if>>
              <div class="crm2header">Related Activities</div>
              <div class="crm2body">
                <div
                  class="activityWidget ${widgetClass!}"
                  data-maxheight="300px"
                  data-domain="${clientDomain}"
                  data-widgetstyle="activities-list"
                  data-authtoken="${authToken!}"
                  data-canedit="true"
                  data-shownomore="false"
                  data-userparams="${userParam}"
                  <#if tagName?has_content>
                  data-tagname="${tagName}"
                  data-tagkeywords="${tagKeywords}"
                  data-tagurl="${tagUrl}"
                  </#if>
                  data-queryparams="activityType=EMAIL NOTE TASK${queryParam!}"
                  <#if widgetClass?has_content> data-linkedclass="${widgetClass}"</#if>
                ></div>

                <div style="padding-bottom:5px"
                  class="activityWidget createActivity ${widgetClass!}"
                  data-domain="${clientDomain}"
                  data-widgetstyle="create-activity"
                  data-buttonclass="smallSubmit"
                  data-authtoken="${authToken!}"
                  data-queryparams="note_field_activityType=NOTE${createParam}<#if userParam?has_content>&${userParam}</#if>"
                  <#if tagName?has_content>
                  data-tagname="${tagName}"
                  data-tagkeywords="${tagKeywords}"
                  data-tagurl="${tagUrl}"
                  </#if>
                  <#if widgetClass?has_content> data-linkedclass="${widgetClass}"</#if>
                ></div>
              </div>
            </td>
          </#if>

          <#if useSearch>
            <td <#if (queryParam?has_content || tagName?has_content)> width="100%" <#else> width="50%" </#if>>
              <div class="crm2header">Similar Activities</div>
              <div class="crm2body">
                <div
                    class="activityWidget ${widgetClass}"
                    data-maxheight="300px"
                    data-domain="${clientDomain}"
                    data-widgetstyle="activities-list"
                    data-authtoken="${authToken}"
                    data-canedit="true"
                    data-disablereply="true"
                    data-shownomore="false"
                    data-userparams="${userParam}"
                    data-queryparams="activityType=EMAIL NOTE TASK"
                    data-relateto="tag"
                    <#if tagName?has_content>
                    data-tagname="${tagName}"
                    data-tagkeywords="${tagKeywords}"
                    data-tagurl="${tagUrl}"
                    </#if>
                    data-searchparams="tagName=${tagName}&amp;tagKeywords=${tagKeywords}&amp;tagUrl=${tagUrl}"
                    <#if widgetClass?has_content> data-linkedclass="${widgetClass}"</#if>></div>
              </div>
            </td>
          </#if>
        <#else>
          <td><p style="color:red">&nbsp;Could not get a valid authToken.</p></td>
        </#if>
      </tr>
      <tr>
        <td <#if (queryParam?has_content || tagName?has_content) && useSearch> colspan="2"</#if>>
          <div class="crm2terms bootstrap"><a href="http://crm2.opentaps.com/static/terms_of_use.html" target="_blank">Terms of Use</a></div>
        </td>
      </tr>
    </table>
  </div>
</div>

