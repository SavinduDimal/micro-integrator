/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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


package org.wso2.carbon.micro.integrator.management.apis;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.inbound.endpoint.internal.http.api.APIResource;
import org.wso2.carbon.inbound.endpoint.internal.http.api.InternalAPI;

import static org.wso2.carbon.micro.integrator.management.apis.Constants.PREFIX_APIS;
import static org.wso2.carbon.micro.integrator.management.apis.Constants.PREFIX_CARBON_APPS;
import static org.wso2.carbon.micro.integrator.management.apis.Constants.PREFIX_ENDPOINTS;
import static org.wso2.carbon.micro.integrator.management.apis.Constants.PREFIX_INBOUND_ENDPOINTS;
import static org.wso2.carbon.micro.integrator.management.apis.Constants.PREFIX_PROXY_SERVICES;
import static org.wso2.carbon.micro.integrator.management.apis.Constants.PREFIX_SEQUENCES;
import static org.wso2.carbon.micro.integrator.management.apis.Constants.PREFIX_TASKS;
import static org.wso2.carbon.micro.integrator.management.apis.Constants.PREFIX_TEMPLATES;
import static org.wso2.carbon.micro.integrator.management.apis.Constants.REST_API_CONTEXT;

public class ManagementInternalApi implements InternalAPI {

    private String name;

    private static Log LOG = LogFactory.getLog(ManagementInternalApi.class);

    public ManagementInternalApi(){
        LOG.warn("Management Api has been enabled");
    }

    public APIResource[] getResources() {

        APIResource[] resources = new APIResource[8];
        resources[0] = new ApiResource(PREFIX_APIS);
        resources[1] = new EndpointResource(PREFIX_ENDPOINTS);
        resources[2] = new InboundEndpointResource(PREFIX_INBOUND_ENDPOINTS);
        resources[3] = new ProxyServiceResource(PREFIX_PROXY_SERVICES);
        resources[4] = new CarbonAppResource(PREFIX_CARBON_APPS);
        resources[5] = new TaskResource(PREFIX_TASKS);
        resources[6] = new SequenceResource(PREFIX_SEQUENCES);
        resources[7] = new TemplateResource(PREFIX_TEMPLATES);
        return resources;
    }

    public String getContext() {

        return REST_API_CONTEXT;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
