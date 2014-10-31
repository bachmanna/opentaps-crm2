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

<#-- Full text search parameters -->
<#assign searchQuery = ""/>
<#assign searchEntity = ""/>
<#assign noTerms = ""/>

<#-- Used to assign a note to particular application object, e.g. to an order. -->
<#assign relateTo = ""/>

<#if orderId?has_content>
  <#assign linkPageId = orderId />
  <#assign widgetClass = "opentapsOrderId_${orderId}"/>
  <#assign queryParam  = "&opentapsOrderId=${orderId}" />
  <#assign createParam = "&note_field_opentapsOrderId=${orderId}&note_field_referenceName=Sales Order ${orderId}" />
  <#assign searchEntity = "Order" />
  <#assign searchQuery = "${orderId}"/>
  <#assign notTerms = "opentapsOrderId:${orderId}">
  <#if order?has_content>
    <#if order.isPurchaseOrder()>
      <#assign createParam = "&note_field_opentapsOrderId=${orderId}&note_field_referenceName=Purchase Order ${orderId}" />
    </#if>
  </#if>
<#elseif invoiceId?has_content>
  <#assign linkPageId = invoiceId />
  <#assign widgetClass = "opentapsInvoiceId_${invoiceId}"/>
  <#assign queryParam  = "&opentapsInvoiceId=${invoiceId}" />
  <#assign createParam = "&note_field_opentapsInvoiceId=${invoiceId}&note_field_referenceName=Invoice ${invoiceId}" />
  <#assign searchEntity = "Invoice" />
  <#assign searchQuery = "${invoiceId!}"/>
<#elseif paymentId?has_content>
  <#assign linkPageId = paymentId />
  <#assign widgetClass = "opentapsPaymentId_${paymentId}"/>
  <#assign queryParam  = "&opentapsPaymentId=${paymentId}" />
  <#assign createParam = "&note_field_opentapsPaymentId=${paymentId}&note_field_referenceName=Payment ${paymentId}" />
  <#assign searchEntity = "Payment" />
  <#assign searchQuery = "${paymentId!}"/>
<#elseif parameters.partyId?has_content>
  <#assign linkPageId = parameters.partyId />
  <#assign widgetClass = "opentapsPartyId_${parameters.partyId}"/>
  <#assign queryParam  = "&opentapsPartyId=${parameters.partyId}" />
  <#assign createParam = "&opentapsPartyId=${parameters.partyId}&note_field_referenceName=Contact ${parameters.partyId}" />
  <#if supplierPartyId?has_content && partySummary?has_content>
    <#assign createParam = "&opentapsPartyId=${parameters.partyId}&note_field_referenceName=Supplier ${partySummary.groupName!supplierPartyId}" />
  <#elseif partySummary?has_content>
    <!-- ${partySummary} -->
    <#if partySummary.groupName?has_content>
      <#assign createParam = "&opentapsPartyId=${parameters.partyId}&note_field_referenceName=Contact ${partySummary.groupName!parameters.partyId}" />
    <#elseif partySummary.firstName?has_content>
      <#assign createParam = "&opentapsPartyId=${parameters.partyId}&note_field_referenceName=Contact ${partySummary.firstName} ${partySummary.lastName!}" />
    </#if>
  </#if>
</#if>
<#if pageUrlBase?has_content && linkPageId?has_content>
  <#assign createParam = "${createParam}&note_field_referenceUrl=${StringUtil.wrapString(pageUrlBase)?url('ISO-8859-1')}${linkPageId}" />
</#if>

<#assign userParam = ""/>
<#if userLogin?has_content>
  <#assign userParam>created_by_user_userLoginId=${userLogin.userLoginId}<#if userName?has_content>&created_by_user_name=${userName}<#else>${userLogin.userLoginId}</#if></#assign>
</#if>

    <a name="activities"></a>
    <table border="0" width="100%" cellspacing="0" cellpadding="0" class="boxoutside">
            <tr>
              <td width="50%" class="boxhead"><div style="font-size: 10pt; padding:4px 0px; background-color: grey">&nbsp;Activities from opentaps CRM2</div></td>
            </tr>
 
<#if isTempTokenValid>

  <#if clientDomain?has_content && queryParam?has_content>
     <tr>
        <td width="100%">
          <div style="width:600px"
	       class="activityWidget ${widgetClass!}"
	       data-maxheight="300px"
	       data-domain="${clientDomain}"
	       data-widgetstyle="activities-list"
	       data-authtoken="${authToken!}"
	       data-canedit="true"
	       data-shownomore="false"
	       data-userparams="${userParam}"
	       data-queryparams="activityType=EMAIL NOTE TASK${queryParam}" <#if widgetClass?has_content> data-linkedclass="${widgetClass}"</#if>></div>

          <div style="width:600px; padding-bottom:5px" class="activityWidget createActivity ${widgetClass!}" data-domain="${clientDomain}" data-widgetstyle="create-activity" data-buttonclass="smallSubmit" data-authtoken="${authToken!}" data-queryparams="note_field_activityType=NOTE${createParam}<#if userParam?has_content>&${userParam}</#if>" <#if widgetClass?has_content> data-linkedclass="${widgetClass}"</#if>></div>
        </td>
      </tr>
  </#if>

  <#if clientDomain?has_content && searchEntity?has_content>
    <br/>

    <tr><td width="50%" class="boxhead"><div style="font-size: 10pt; padding:4px 0px; background-color: grey">&nbsp;More Related Activities</div></td><td>&nbsp;</td></tr>    

      <tr>
        <td width="100%">
          <div style="width:600px"
               class="activityWidget ${widgetClass!}"
	       data-maxheight="300px"
               data-domain="${clientDomain}"
               data-widgetstyle="activities-list"
               data-authtoken="${authToken}"
               data-canedit="true"
               data-disablereply="true"
               data-shownomore="false"
               data-userparams="${userParam}"
               data-queryparams="activityType=EMAIL NOTE TASK"
               data-relateto="${createParam?substring(1)}"
               data-searchparams="entity=${searchEntity}&query=${searchQuery!}<#if notTerms?has_content>&notTerms=${notTerms}</#if>"
               <#if widgetClass?has_content> data-linkedclass="${widgetClass}"</#if>></div>
        </td>
      </tr>
  </#if>
<#else>
    <tr>
      <td style="font-size: 9pt">
        <p style="color:red; font-weight: bold">&nbsp;Could not get a valid authToken.</p>
        <p>&nbsp;Have you <a href="https://crm2.opentaps.com/signup">signed up for an account</a> and <a href="http://www.opentaps.org/docs/index.php/Set_up_CRM2">set up CRM2</a>?</p>
      </td>
    </tr>
</#if>

    </table>
