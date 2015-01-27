<div class="wrap">
    <h2>Post Contact Form 7 Data to CRM2</h2>
    <form method="post" action="options.php"> 
        <?php @settings_fields('post-to-crm-group'); ?>
        <?php @do_settings_fields('post-to-crm-group'); ?>

        <table class="form-table">  
		
			<tr valign="top">
                <th scope="row"><label for="crm_client_domain">Base URL</label></th>
                <td><textarea name="crm_baseurl" id="crm_baseurl" required ><?php echo get_option('crm_baseurl'); ?></textarea></td>
            </tr>
			
            <tr valign="top">
                <th scope="row"><label for="crm_client_domain">Client Domain</label></th>
                <td><textarea name="crm_client_domain" id="crm_client_domain" required><?php echo get_option('crm_client_domain'); ?></textarea></td>
            </tr>
            <tr valign="top">
                <th scope="row"><label for="crm_auth_token">Auth Token</label></th>
                <td><textarea name="crm_auth_token" id="crm_auth_token" required><?php echo get_option('crm_auth_token'); ?></textarea></td>
            </tr>
			
			<tr valign="top">
                <td>All fields are required.</td>
            </tr>
        </table>

        <?php @submit_button(); ?>
    </form>
</div>