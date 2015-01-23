# Copyright (c) Open Source Strategies Inc.

##########################################################################
# Copyright (c) Open Source Strategies Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################################################################

# 
# This is a script which sends follow up emails to prospects.  It will look for activities
# in your client domain of the type FOLLOWUP_EMAIL, and if that is scheduled now, it will use
# the Gmail account here to send the email.
#
# To make it work, you have to create a FOLLOWUP_EMAIL activity type for your domain using our
# valid activity types API: http://opentaps.org/docs/index.php/CRM-2_Administrative_API#Valid_Activity_Types
# 
# Then, on your sign up form, create an activity of type FOLLOWUP_EMAIL for the date/time in the
# future when you would like your follow up email sent.  This script will pick it up from there. 
# 
# Usage: ruby processFollowUp.rb https://crm2.opentaps.com clientDomain authToken gmailUser gmailPassword
# 
# You can cron it like this to run it on a schedule:
# /usr/bin/ruby ./processFollowUp.rb https://crm2.opentaps.com myClientDomain.com authToken gmailUser gmailPassword >> /tmp/processFollowUp.log 2>&1
#

require 'rubygems'
require 'json'
require "net/http"
require "uri"
require "date"
require "net/smtp"

# for debug, you can have all email BCC'd to yourself or redirect all emails to an address 
emailBcc = ""
redirectEmailTo = nil

def processActivities(baseUrl, clientDomain, authToken, gmailUser, gmailPassword, emailBcc, redirectEmailTo)
  print "== Start process activities : ",  DateTime.now
  puts

  baseActivitiesRequest = "/activities"
  activityType = "FOLLOWUP_EMAIL"
  status = "CREATED"

  begin
    uri = URI.parse(baseUrl)
    http = Net::HTTP.new(uri.host, uri.port)

    request = Net::HTTP::Get.new(baseActivitiesRequest + "?authToken=" + authToken + "&clientDomain=" + clientDomain + "&activityType=" + activityType + "&status=" + status)
    response = http.request(request)

    if response
      data = JSON.parse(response.body)
      if response.code == "200"
        res = data["result"]
        if res and res["resultValue"] and res["resultValue"].length > 0
          res["resultValue"].each do |activity|
            if DateTime.parse(activity["scheduledDateTime"]) <= DateTime.now
              puts "Process activity _id : [" + activity["_id"] + "]"

              updateActivityStatus(activity, "COMPLETED", baseUrl, authToken)
              sendEmail(activity, gmailUser, gmailPassword, emailBcc, redirectEmailTo)
            else
              puts "Skip activity _id : [" + activity["_id"] + "], datetime to process : " + activity["scheduledDateTime"]
            end
          end
        else
          puts "Nothing to process"
        end
      else
        error = data["error"]
        if not error
          error = data["errors"]
        end
        puts error
      end
    else
      puts "Response is empty"
    end
  rescue Errno::ECONNREFUSED
    puts "Error: Cannot get response from server"
  end
  puts "== End process activities"
end

def sendEmail(activity, gmailUser, gmailPassword, emailBcc, redirectEmailTo)
  subject = "Follow up from opentaps CRM2"
  if redirectEmailTo
    subject += ' [TO: ' + activity["attributes"]["email"] + '] [BCC: ' + emailBcc + ']';
    to = redirectEmailTo
  else
    to = activity["attributes"]["email"]
  end
  puts "Send email to " + to
  message = <<-EOF
From: #{gmailUser}
To: #{to}
Subject: #{subject}
Content-type: text/html
MIME-Version: 1.0

<p>Hi there! You got the follow up email from opentaps CRM2 to work!  Come and drop us a note at opentaps.com or tweet @opentaps.</p> 
<br/>
<p>We'd also appreciate it if you could rate opentaps CRM2 in the <a href="https://play.google.com/store/apps/details?id=com.opentaps.crm2client">Google Play Store</a>,
<a href="https://itunes.apple.com/us/app/opentaps-crm2/id899333198?mt=8&uo=4">Apple iOS App Store</a>, or
<a href="https://chrome.google.com/webstore/detail/opentaps-crm2/apkbgpfokhbplllnjkndenaopihfiaop">Chrome Extensions Marketplace</a>.
</p>
<br/>
<p>Enjoy!</p>
<br/>
<p><a href="http://www.opentaps.com">The opentaps Team</a></p>
EOF

  begin
    smtp = Net::SMTP.new('smtp.gmail.com', 587 )
    smtp.enable_starttls
    smtp.start('gmail.com', gmailUser, gmailPassword, :login) do |smtp|
      smtp.send_message message, gmailUser, to, emailBcc
    end
  rescue Net::SMTPAuthenticationError
    puts "Error: Gmail Username and Password not accepted."
  end

end

def updateActivityStatus(activity, newStatus, baseUrl, authToken)
  puts "Update activty _id [" + activity["_id"] + "] to " + newStatus

  baseActivitiesRequest = "/activities"

  params = {
    "partial" => "Yes",
    "clientDomain" => activity["clientDomain"],
    "authToken" => authToken,
    "status" => newStatus
  }

  begin
    uri = URI.parse(baseUrl)
    http = Net::HTTP.new(uri.host, uri.port)

    request = Net::HTTP::Put.new(baseActivitiesRequest + "/" + activity["_id"])
    request.set_form_data(params)
    response = http.request(request)

    if response
      data = JSON.parse(response.body)
      if response.code == "200"
        puts "Activty _id [" + activity["_id"] + "] successfully updated"
      else
        error = data["error"]
        if not error
          error = data["errors"]
        end
        puts error
      end
    else
      puts "Response is empty"
    end
  rescue Errno::ECONNREFUSED
    puts "Error: Cannot get response from server"
  end
end

# start process activities
# check argv
if ARGV.length >= 5
  baseUrl = ARGV[0]
  clientDomain = ARGV[1]
  authToken = ARGV[2]
  gmailUser = ARGV[3]
  gmailPassword = ARGV[4]

  processActivities(baseUrl, clientDomain, authToken, gmailUser, gmailPassword, emailBcc, redirectEmailTo)
else
  puts "Wrong command line arguments amout"
  puts "Usage: ruby processFollowUp.rb https://crm2.opentaps.com clientDomain authToken gmailUser gmailPassword"
end
