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
<script type="text/javascript" charset="utf-8" src="${widgetBaseUrl!}/contactsWidget/client/js/libs/requirejs/require.js" data-main="${widgetBaseUrl!}/js/activity-widgets/build/activity-widgets.packed.js">
</script>

<#if userLogin?has_content>

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

  <#assign userParam>created_by_user_userLoginId=${userLogin.userLoginId}<#if userName?has_content>&created_by_user_name=${userName}<#else>${userLogin.userLoginId}</#if></#assign>

  <#if clientDomain?has_content>
    <a name="activities"></a>
    <table border="0" width="100%" cellspacing="0" cellpadding="0" class="boxoutside">
      <tr>
        <td width="100%">
          <table border="0" width="100%" cellspacing="0" cellpadding="0" class="boxtop">
            <tr>
              <td valign="middle" align="left">
                <div style="padding:4px 0px" class="boxhead">&nbsp;${uiLabelMap.CrmActivities}</div>
              </td>          
            </tr>
          </table>
        </td>
      </tr>
      <tr>
        <td width="100%">
          <div style="width:600px;" class="activityWidget" data-domain="${clientDomain}" data-widgetstyle="activities-list" data-authtoken="${authToken!}" data-canedit="true" data-shownomore="false"  data-showreflinks="true" data-userparams="${userParam}" data-queryparams="mentionedUserIds=${userLogin.userLoginId}"></div>
        </td>
      </tr>
    </table>
  <#else>
    <p>No clientDomain found, please check the configuration.</p>
  </#if>
<#else>
  <p>No current userLogin found, you must login first.</p>
</#if>
