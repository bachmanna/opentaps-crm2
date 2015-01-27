<?php 
/**
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


/**
 * Plugin Name: Post Contact Form 7 Data To CRM2
 * Plugin URI: 
 * Description: Post the contact form 7 data (first name, last name, company, phone, email, comments only) into CRM2. Requires client domain and auth token of CRM2 to post the information.
 * Author: Debashis Chowdhury
 * Version: 1.0
 */	

if(!class_exists('Post_To_CRM'))
{
    class Post_To_CRM
    {
        /**
         * Construct the plugin object
         */
        public function __construct()
        {
            // register actions
			add_action('admin_init', array(&$this, 'admin_init'));
			add_action('admin_menu', array(&$this, 'add_menu'));
                        add_action('wpcf7_before_send_mail', array(&$this, 'post_to_opentaps'));
        }


        public static function activate()
        {
           //add_action('wpcf7_before_send_mail', 'post_to_opentaps');
        } 
  
        public static function deactivate()
        {
           //remove_action( "wpcf7_before_send_mail", "post_to_opentaps");
        }
				
		public function admin_init()
		{
			// Set up the settings for this plugin
			$this->init_settings();
		} 
  
		public function init_settings()
		{
			// register the settings for this plugin
			register_setting('post-to-crm-group', 'crm_baseurl');
			register_setting('post-to-crm-group', 'crm_client_domain');
			register_setting('post-to-crm-group', 'crm_auth_token');
		} 

		public function add_menu()
		{
			add_options_page('Post CF7 Data To CRM2 Settings', 'Post CF7 Data To CRM2', 'manage_options', 'post_to_crm', array(&$this, 'plugin_settings_page'));
		}

		
		/**
		 * Menu Callback
		 */  
		public function plugin_settings_page()
		{
			if(!current_user_can('manage_options'))
			{
				wp_die(__('You do not have sufficient permissions to access this page.'));
			}

			// Render the settings template
			include(sprintf("%s/templates/settings.php", dirname(__FILE__)));
		} 




function post_to_opentaps($cf7_data) {
	
	$crm2config['authToken']          = get_option('crm_auth_token');
	$crm2config['clientDomain']       = get_option('crm_client_domain');
	
	$crmUrl = get_option('crm_baseurl');

	if( ($crmUrl == '') || ($crm2config['authToken'] == '') || ($crm2config['clientDomain'] == '')) {
		echo 'CRM2 Credentials are not configured';
	} else {
	
		

		$first_name_post = trim($_REQUEST['first-name']);
		$last_name_post = trim($_REQUEST['last-name']);
		$company_name_post = trim($_REQUEST['company-name']);
		$phone_number_post = trim($_REQUEST['phone-number']);
		$email_post = $_REQUEST['email-address'];
		$comment_post = trim($_REQUEST['comment']);
	
		$data = array(
                	'authToken' => $crm2config['authToken'],
                	'clientDomain' => $crm2config['clientDomain'],
                	'firstName' => $first_name_post,
                	'lastName' => $last_name_post,
                	'companyName' => $company_name_post,
                	'phones' => array ( "phone" => $phone_number_post, "purpose" => "Work" ),
                	'emails' => array ( "email" => $email_post, "purpose" => "PRIMARY" ), 
                	'comment' => $comment_post,
                	'createActivity' => 'Y',
                	'activityType' => 'CONTACT_FORM',
                	'attributes' => array ( "formId" => 'OSSIContactForm'),
                	'notify' => 'Y'
            	);

		$json_data = json_encode($data);

		$ch = curl_init();
		curl_setopt($ch, CURLOPT_URL, $crmUrl);
		curl_setopt($ch, CURLOPT_HTTPHEADER,array('Content-Type: application/json')); 
		curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
		curl_setopt($ch, CURLOPT_POST, 1);
		curl_setopt($ch, CURLOPT_POSTFIELDS, $json_data);
	
		$result = curl_exec($ch);

		$info = curl_getinfo($ch);

		curl_close($ch);

		if ($result) {
			$obj = json_decode($result);

			if ($info['http_code'] === 200) {
				//print_r("Contact is successfully created ");
			} else { print_r('Contact not posted');
			 	if ($obj->error) {
				 	print_r($obj->error);
			 	} else if ($obj->errors && $obj->message) {
				 	print_r($obj->message);
			 	} else {
				 	print_r("Create contact error");
			 	}
			}
		}

    		// If you want to skip mailing the data, you can do it...
    		//$wpcf7_data->skip_mail = true;
	}
 
}


    } // END class Post_To_CRM
	
} // END if(!class_exists('Post_To_CRM'))



if(class_exists('Post_To_CRM'))
{
    // Installation and uninstallation hooks
    register_activation_hook(__FILE__, array('Post_To_CRM', 'activate'));
    register_deactivation_hook(__FILE__, array('Post_To_CRM', 'deactivate'));

    // instantiate the plugin class
    $post_to_crm = new Post_To_CRM();
	
	if(isset($post_to_crm))
	{
		// Add the settings link to the plugins page
		function plugin_settings_link($links)
		{ 
			$settings_link = '<a href="options-general.php?page=post_to_crm">Settings</a>'; 
			array_unshift($links, $settings_link); 
			return $links; 
		}

		$plugin = plugin_basename(__FILE__); 
		add_filter("plugin_action_links_$plugin", 'plugin_settings_link');
	}
}

?>
