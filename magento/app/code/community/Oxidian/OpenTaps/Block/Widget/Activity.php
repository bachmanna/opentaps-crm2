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
class Oxidian_OpenTaps_Block_Widget_Activity extends Oxidian_OpenTaps_Block_Widget
{
	protected $widgetClass;
	protected $tagKeywords;
	protected $tagName;

	protected function _construct()
	{
		parent::_construct();
		$orderParam = (int) $this->getRequest()->getParam('order_id');

		$orderId = Mage::getModel('sales/order')->load($orderParam)->getIncrementId();

		$admin = Mage::getSingleton('admin/session')->getUser();
		$userName =  $admin->getEmail();
		$linkPageId = $orderId;
		$this->widgetClass = "opentapsOrderId_$orderId";
		$useSearch = 'true';
		$notTerms = "opentapsOrderId:$orderId";
		$this->tagName = "SO::$orderId";
		$this->userParam = "&created_by_user_name=$userName&created_by_user_userLoginId=$userName";
		$this->tagKeywords = "$orderId,order|po|orders";
		$queryParam = '';

		$this->requestData['class']="activityWidget $this->widgetClass";
		$this->requestData['data-widgetstyle']='activities-list';
		$this->requestData['data-maxheight']='300px';
		$this->requestData['data-linkedclass']="$this->widgetClass";
		$this->requestData['data-userparams']="$this->userParam";
		$this->requestData['data-tagname']=$this->tagName;
		$this->requestData['data-tagkeywords']=$this->tagKeywords;

		$this->requestData['data-queryparams']="activityType=EMAIL NOTE TASK$queryParam";
	}
}
