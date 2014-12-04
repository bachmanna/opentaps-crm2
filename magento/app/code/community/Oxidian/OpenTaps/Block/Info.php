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
class Oxidian_OpenTaps_Block_Info extends Mage_Adminhtml_Block_Template
{
    /**
     * Constructor. Set template.
     */
    protected function _construct()
    {
        parent::_construct();
        $this->setTemplate('oxidian_opentaps/info.phtml');
    }

    public function getWidget($name)
    {
    	return $this->widgetList[$name];
    }

	public function getWidgetActivity()
	{
		return new Oxidian_OpenTaps_Block_Widget_Activity();
	}
}
