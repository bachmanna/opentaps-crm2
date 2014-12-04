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
class Oxidian_OpenTaps_Block_Widget_Activitycreate extends Oxidian_OpenTaps_Block_Widget_Activity
{
	protected function _construct()
	{
		parent::_construct();

		$widgetClass = $this->widgetClass;
        $this->requestData['class']="activityWidget createActivity $this->widgetClass";

        $this->requestData['data-widgetstyle']='create-activity';
        $this->requestData['data-buttonclass']='smallSubmit';

        $this->requestData['data-queryparams']="note_field_activityType=NOTE&$this->userParam;";

	}
}
