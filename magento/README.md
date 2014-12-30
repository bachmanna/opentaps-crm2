opentaps-crm2 Magento Integration
=================================

Installation:
 * Go to http://www.opentaps.com to sign up for an account with opentaps CRM2
 * You will need the domain name and authToken from your sign up confirmation email.  You can also get this information by logging into opentaps CRM2, clicking on the configuration wheel in the upper right hand corner, and choosing the My Account option.
 * copy all files from the app and skin folders to the magento directory (which already has an app/ and skin/ folders) and overwrite the existing files if there are any from a previous installation, eg: cp -rf app/ skin/ /path/to/your/magento/
 * logout and re-login from the magento web UI if you were already logged in
 * in the magento backend, logged as an admin user, go to System (from the top menu) > Configuration then in the left menu under Sales > Opentaps CRM2
   * setup the authToken and domain according to your subscription
   * the other 2 options should be left blank

Usage:
  * on any Sales Order view in the magento backend, the CRM2 UI will show at the bottom of the page
