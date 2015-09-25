/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.management.web.browser.client.ui.browser;

import java.util.List;

import org.apache.cxf.management.web.browser.client.service.settings.Subscription;
import org.apache.cxf.management.web.browser.client.ui.View;

public interface NavigationSidebarView extends View {

    public interface Presenter {
        void onExploreSubcriptionItemClicked(int row);

        void onFilterSubcriptionItemClicked(int row);

        void onManageSubscriptionsButtonClicked();

        void onEditCriteriaHyperinkClicked();
    }

    void setSubscriptions(List<Subscription> subscriptions);
    
    void setPresenter(Presenter presenter);
}
