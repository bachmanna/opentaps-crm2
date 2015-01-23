#!/usr/bin/python
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
# This script emails a daily list of all your open tasks and activities using
# your Gmail account, which you can set up below.  It shows you how to interact 
# with opentaps CRM2 using Python.  Feel free to customize to meet your needs 
# and drop us a note at opentaps.com or tweet @opentaps.  Enjoy!
#

##############################################
# Note: some of those imports are for Python 3
##############################################
import httplib2 as http
import json
try:
  # for python 3
  from urllib.parse import urlencode
  from urllib.parse import urlparse
except ImportError:
  # older
  from urllib import urlencode
  from urlparse import urlparse
import sys
import re
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

###################################
# config
###################################
# opentaps CRM2 Settings
baseUrl = 'https://crm2.opentaps.com'
crm2BaseUrl = 'https://crm2.opentaps.com/crm2/contactsWidget/client/'
authToken = 'setmeup'
clientDomain = 'mydomain.com'
# Gmail settings
# use Google SMTP with TLS
emailServer = 'smtp.gmail.com:587'
emailUseTLS = True
# emailFrom and emailUser should match on Google, in other case they might be different
emailFrom = 'notifications@mydomain.com'
emailUser = 'notifications@mydomain.com'
# set this to match the emailUser
emailPassword = 'setmeup'
emailSubject = 'Your daily CRM2 tasks report'
# for testing send all emails to that address, original recipient is shown in the subject
emailRedirectTo = None
###################################


headers = {
    'Accept': 'application/json',
    'Content-Type': 'application/json; charset=UTF-8'
}

EMAIL_REGEX = re.compile(r"[^@]+@[^@]+\.[^@]+")

# get the current tasks
def getTasks():
  target = urlparse(baseUrl + '/task')
  method = 'GET'
  body = {
    'status': 'STARTED,CREATED',
    'per_page': '9999',
    'authToken': authToken,
    'clientDomain': clientDomain
  }
  headers = {'Content-type': 'application/x-www-form-urlencoded'}

  print("Getting current tasks...")

  h = http.Http()

  response, content = h.request(
          target.geturl(),
          method,
          urlencode(body),
          headers)

  # assume that content is a json reply
  # parse content with the json module
  print("Got response.")
  data = json.loads(content.decode('ascii'))

  if 'result' not in data:
    print('No tasks in server response')
    sys.exit(2)

  if 'resultValue' not in data['result']:
    print('No tasks in server response')
    sys.exit(2)

  tasks =  data['result']['resultValue']
  print('Got ', len(tasks), 'tasks')
  return tasks

# get the current users
def getUsers():
  target = urlparse(baseUrl + '/contacts-users')
  method = 'GET'
  body = {
    'authToken': authToken,
    'clientDomain': clientDomain
  }
  headers = {'Content-type': 'application/x-www-form-urlencoded'}

  print("Getting current users...")

  h = http.Http()

  response, content = h.request(
          target.geturl(),
          method,
          urlencode(body),
          headers)

  # assume that content is a json reply
  # parse content with the json module
  print("Got response.")
  data = json.loads(content.decode('ascii'))
  return data

def getUser(contact):
  target = urlparse(baseUrl + '/contact-users/' + contact['_id'])
  method = 'GET'
  body = {
    'authToken': authToken,
    'clientDomain': clientDomain
  }
  headers = {'Content-type': 'application/x-www-form-urlencoded'}

  print("Getting user " + contact['_id'] + " ...")

  h = http.Http()

  response, content = h.request(
          target.geturl(),
          method,
          urlencode(body),
          headers)

  # assume that content is a json reply
  # parse content with the json module
  print("Got response.")
  data = json.loads(content.decode('ascii'))
  return data

def getEmail(contact):
  email = None
  if contact['users']:
    for user in contact['users']:
      if 'userId' in user and 'userIdType' in user and 'INTERNAL' == user['userIdType'] and EMAIL_REGEX.match(user['userId']):
        email = user['userId']
        print('** found google email ' + email)
        break
  if not email and 'emails' in contact:
    for cemail in contact['emails']:
      if cemail and 'email' in cemail and cemail['email']:
        email = cemail['email']
        print('** found contact email ' + email)
        break
  if not email:
    print('** NO email for contact ' + contact['_id'])
  return email

def getResponsibleTasks(tasks, contact):
  responsibleTasks = [];
  for task in tasks:
    if 'responsibleContactId' in task and task['responsibleContactId'] == contact['_id']:
      responsibleTasks.append(task)
  return responsibleTasks

def getAssignedTasks(tasks, contact):
  assignedTasks = [];
  for task in tasks:
    if 'assignedToContact' in task and contact['_id'] in task['assignedToContact'] and ('responsibleContactId' not in task or ('responsibleContactId' in task and task['responsibleContactId'] != contact['_id'])):
      assignedTasks.append(task)
  return assignedTasks

def getOpenTasks(tasks):
  openTasks = [];
  for task in tasks:
    if 'responsibleContactId' not in task or not task['responsibleContactId']:
      openTasks.append(task)
  return openTasks

def taskUrl(task):
  return crm2BaseUrl + "#/tasks?taskId=" + task['_id']

users = getUsers()
tasks = getTasks()

print()
print('Contacts with users:')
print('--------------------')
for user in users:
  print(user['_id'] + ' - ' + user['fullName']);
  user['users'] = getUser(user)

print()
print('Tasks:')
print('--------------------')
for task in tasks:
  print(task['noteText'] + ' - ' + task['status'])

print()
print('Reports:')
print('--------------------')

# for each user, check the email on file, use the GOOGLE_APPS email account first
# otherwise tak the first contact email
# if he has an email, then get the list of
#  1. Open tasks he's responsible for
#  2. Open tasks he's been assigned to
#  3. Unassigned open tasks
for contact in users:
  email = getEmail(contact)
  if not email:
    continue
  text = ""
  html = "<html><head></head><body>"
  responsibleTasks = getResponsibleTasks(tasks, contact)
  assignedTasks = getAssignedTasks(tasks, contact)
  openTasks = getOpenTasks(tasks)
  print()
  print(contact['_id'] + ' - ' + contact['fullName'])
  print('----------------------------------------')
  print('1. Open tasks he\'s responsible for')
  text += "1. Open tasks you are responsible for\n"
  html += "<b>1. Open tasks you are responsible for</b>\n<ul>\n"
  for task in responsibleTasks:
    print(task['noteText'])
    text += " * " + task['noteText'] + "( " + taskUrl(task) + " )\n"
    html += '<li><a href="' + taskUrl(task) + '">' + task['noteText'] + "</a></li>\n"
  if not responsibleTasks:
    text += " * None.\n"
    html += "<li><i>None</i></li>\n"
  html += "</ul>\n"
  print('----------------------------------------')
  print('2. Open tasks he\'s been assigned to')
  text += "2. Open tasks you are assigned to\n"
  html += "<b>2. Open tasks you are assigned to</b>\n<ul>\n"
  for task in assignedTasks:
    print(task['noteText'])
    text += " * " + task['noteText'] + "( " + taskUrl(task) + " )\n"
    html += '<li><a href="' + taskUrl(task) + '">' + task['noteText'] + "</a></li>\n"
  if not assignedTasks:
    text += " * None.\n"
    html += "<li><i>None</i></li>\n"
  html += "</ul>\n"
  print('----------------------------------------')
  print('3. Unassigned open tasks')
  text += "3. Unassigned open tasks\n"
  html += "<b>3. Unassigned open tasks</b>\n<ul>\n"
  for task in openTasks:
    print(task['noteText'])
    text += " * " + task['noteText'] + "( " + taskUrl(task) + " )\n"
    html += '<li><a href="' + taskUrl(task) + '">' + task['noteText'] + "</a></li>\n"
  if not openTasks:
    text += " * None.\n"
    html += "<li><i>None</i></li>\n"
  html += "</ul>\n"
  html += "</body></html>"

  # send the email
  msg = MIMEMultipart('alternative')
  msg['From'] = emailFrom
  subject = emailSubject
  if emailRedirectTo:
    subject = emailSubject + ' [TO: ' + email + ']'
    email = emailRedirectTo
  msg['Subject'] = subject
  msg['To'] = email

  msg.attach(MIMEText(text, 'plain'))
  msg.attach(MIMEText(html, 'html'))
  print('Sending email via ' + emailServer + ' ...')
  server = smtplib.SMTP(emailServer)
  server.ehlo()
  if emailUseTLS:
    server.starttls()
  server.ehlo()
  if emailUser and emailPassword:
    server.login(emailUser, emailPassword)
  server.sendmail(emailFrom, email, msg.as_string())
  server.quit()
  print('Email Sent.')
  print()


