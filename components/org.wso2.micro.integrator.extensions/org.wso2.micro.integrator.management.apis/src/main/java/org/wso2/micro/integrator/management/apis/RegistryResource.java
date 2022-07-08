package org.wso2.micro.integrator.management.apis;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMText;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.transport.passthru.util.RelayConstants;
import org.apache.synapse.transport.passthru.util.StreamingOnRequestDataSource;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.wso2.micro.integrator.registry.MicroIntegratorRegistry;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;

import static org.apache.synapse.SynapseConstants.HTTP_SC;
import static org.wso2.micro.integrator.management.apis.Constants.NO_ENTITY_BODY;

/**
 * This class provides mechanisms to monitor registry resources
 */
public class RegistryResource implements MiApiResource {

    private static final Log LOG = LogFactory.getLog(RegistryResource.class);

    // HTTP method types supported by the resource
    Set<String> methods;
    // Constants
    private static final String REGISTRY_PATH = "registryPath";
    private static final String META_DATA = "metadata";
    private static final String FETCH_TYPE = "type";
    private static final String EXPAND_PARAM = "expand";

    private static final String PROPERTIES = "properties";
    private static final String CONTENT = "content";
    private static final String MEDIA_TYPE = "mediaType";
    private static final String PROPERTY_NAME = "propertyName";
    private static final String PROPERTY_VALUE = "propertyValue";

    public RegistryResource(){
        methods = new HashSet<>();
        methods.add(Constants.HTTP_GET);
    }

     @Override
    public Set<String> getMethods() {
        return methods;
    }

    @Override
    public boolean invoke(MessageContext messageContext,
                          org.apache.axis2.context.MessageContext axis2MessageContext,
                          SynapseConfiguration synapseConfiguration) {

        String registryPath = Utils.getQueryParameter(messageContext,REGISTRY_PATH);
        String fetchType = Utils.getQueryParameter(messageContext,FETCH_TYPE);
        String expandedEnabled = Utils.getQueryParameter(messageContext,EXPAND_PARAM);
        MicroIntegratorRegistry microIntegratorRegistry = new MicroIntegratorRegistry();

        if (Objects.nonNull(registryPath) && Objects.nonNull(fetchType) && fetchType.equals(META_DATA)){
            populateRegistryMetadata(axis2MessageContext, microIntegratorRegistry,registryPath);
        } else if (Objects.nonNull(registryPath) && Objects.nonNull(fetchType) && fetchType.equals(PROPERTIES)){
            populateRegistryProperties(axis2MessageContext, microIntegratorRegistry,registryPath);
        } else if (Objects.nonNull(registryPath) && Objects.nonNull(fetchType) && fetchType.equals(CONTENT)){
            populateRegistryContent(messageContext, axis2MessageContext,registryPath);
        } else if(Objects.nonNull(registryPath) && Objects.nonNull(expandedEnabled) && expandedEnabled.equals("true")){
            populateRegistryResourceJSON(axis2MessageContext,microIntegratorRegistry,registryPath);
        } else if(Objects.nonNull(registryPath)){
            populateImmediateChildren(axis2MessageContext, microIntegratorRegistry,registryPath);
        } else {
//            populateRegistryResourceJSON(axis2MessageContext,microIntegratorRegistry);
            populateRegistryResourceMap(axis2MessageContext,microIntegratorRegistry);
        }
        axis2MessageContext.removeProperty(Constants.NO_ENTITY_BODY);
        return true;
    }

    /**
     * This method is used to get a list of all the available registry resource paths.
     * @param axis2MessageContext
     * @param microIntegratorRegistry
     */
    private void populateRegistryResourceMap(org.apache.axis2.context.MessageContext axis2MessageContext,
                                             MicroIntegratorRegistry microIntegratorRegistry){

        String carbonHomePath = Utils.getCarbonHome();
        List<String> registryPathList = microIntegratorRegistry.getRegistryPathList(carbonHomePath);
        JSONObject jsonBody = Utils.createJSONList(registryPathList.size());
        for (String registryPath : registryPathList){
            JSONObject registryPathObject = new JSONObject();
            registryPathObject.put(REGISTRY_PATH,registryPath);
            jsonBody.getJSONArray(Constants.LIST).put(registryPathObject);
        }
        Utils.setJsonPayLoad(axis2MessageContext, jsonBody);

    }

    /**
     * This method is used to get the <MI-HOME>/registry directory and its content as a JSON
     * @param axis2MessageContext
     * @param microIntegratorRegistry
     */
    private void populateRegistryResourceJSON(org.apache.axis2.context.MessageContext axis2MessageContext,
                                              MicroIntegratorRegistry microIntegratorRegistry,
                                              String path){

        String carbonHomePath = Utils.getCarbonHome();
        String folderPath = carbonHomePath + File.separator + path + File.separator;
        JSONObject jsonBody = microIntegratorRegistry.getRegistryMapJSON(folderPath);
        Utils.setJsonPayLoad(axis2MessageContext,jsonBody);
    }

    /**
     * This method is used to fetch the metadata(media type) of a specified registry file
     * @param axis2MessageContext
     * @param microIntegratorRegistry
     * @param path
     */
    private void populateRegistryMetadata(org.apache.axis2.context.MessageContext axis2MessageContext,
                                          MicroIntegratorRegistry microIntegratorRegistry,
                                          String path){

        String carbonHomePath = Utils.getCarbonHome();
        String registryPath = carbonHomePath + File.separator + path;
        JSONObject jsonBody =  new JSONObject();
        String mediaType = microIntegratorRegistry.getRegistryMediaType(registryPath);
        jsonBody.put(REGISTRY_PATH,path);
        jsonBody.put(MEDIA_TYPE,mediaType);
        Utils.setJsonPayLoad(axis2MessageContext,jsonBody);
    }

    /**
     * This method is used to fetch all the properties of a specified registry file
     * @param axis2MessageContext
     * @param microIntegratorRegistry
     * @param path
     */
    private void populateRegistryProperties(org.apache.axis2.context.MessageContext axis2MessageContext,
                                            MicroIntegratorRegistry microIntegratorRegistry,
                                            String path){

        String carbonHomePath = Utils.getCarbonHome();
        String registryPath = carbonHomePath + File.separator + path;
        Properties propertiesList = microIntegratorRegistry.getRegistryProperties(registryPath);
        JSONObject jsonBody = Utils.createJSONList(propertiesList.size());
        if (propertiesList != null){
            for (Object property : propertiesList.keySet()){
                Object value = propertiesList.get(property);
                JSONObject propertyObject = new JSONObject();
                propertyObject.put(PROPERTY_NAME,property);
                propertyObject.put(PROPERTY_VALUE,value);
                jsonBody.getJSONArray(Constants.LIST).put(propertyObject);
            }
        } else {
            JSONObject propertyObject = new JSONObject();
            propertyObject.put(REGISTRY_PATH,path);
            propertyObject.put(PROPERTY_NAME,"No properties found.");
            jsonBody.getJSONArray(Constants.LIST).put(propertyObject);
        }
        Utils.setJsonPayLoad(axis2MessageContext, jsonBody);

    }

    /**
     * This method is to get the immediate child files and folders of a given directory with their metadata and properties.
     * @param axis2MessageContext
     * @param microIntegratorRegistry
     * @param path
     */
    private void populateImmediateChildren(org.apache.axis2.context.MessageContext axis2MessageContext,
                                           MicroIntegratorRegistry microIntegratorRegistry,
                                           String path){

        String carbonHomePath = Utils.getCarbonHome();
        String registryPath = carbonHomePath + File.separator + path;
        JSONArray childrenList = microIntegratorRegistry.getChildrenList(registryPath);
        JSONObject jsonBody = Utils.createJSONList(childrenList.length());
        jsonBody.put(Constants.LIST,childrenList);
        Utils.setJsonPayLoad(axis2MessageContext, jsonBody);
    }

    /**
     * This method is to get the content of the specified registry file
     * @param synCtx
     * @param axis2MessageContext
     * @param pathParameter
     */
    private void populateRegistryContent(MessageContext synCtx,
                                         org.apache.axis2.context.MessageContext axis2MessageContext,
                                         String pathParameter) {

        DataHandler dataHandler = downloadArchivedLogFiles(pathParameter);
        if (dataHandler != null) {
            try {
                InputStream fileInput = dataHandler.getInputStream();
                SOAPFactory factory = OMAbstractFactory.getSOAP12Factory();
                SOAPEnvelope env = factory.getDefaultEnvelope();
                OMNamespace ns =
                        factory.createOMNamespace(RelayConstants.BINARY_CONTENT_QNAME.getNamespaceURI(), "ns");
                OMElement omEle = factory.createOMElement(RelayConstants.BINARY_CONTENT_QNAME.getLocalPart(), ns);
                StreamingOnRequestDataSource ds = new StreamingOnRequestDataSource(fileInput);
                dataHandler = new DataHandler(ds);
                OMText textData = factory.createOMText(dataHandler, true);
                omEle.addChild(textData);
                env.getBody().addChild(omEle);
                synCtx.setEnvelope(env);
                axis2MessageContext.removeProperty(NO_ENTITY_BODY);
                axis2MessageContext.setProperty(Constants.MESSAGE_TYPE, "application/octet-stream");
                axis2MessageContext.setProperty(Constants.CONTENT_TYPE, "application/txt");
            } catch (AxisFault e) {
                LOG.error("Error occurred while creating the response", e);
                sendFaultResponse(axis2MessageContext);
            } catch (IOException e) {
                LOG.error("Error occurred while reading the input stream", e);
                sendFaultResponse(axis2MessageContext);
            }
        } else {
            sendFaultResponse(axis2MessageContext);
        }
    }

    private void sendFaultResponse(org.apache.axis2.context.MessageContext axis2MessageContext) {

        axis2MessageContext.setProperty(NO_ENTITY_BODY, true);
        axis2MessageContext.setProperty(HTTP_SC, 500);
    }

    private DataHandler downloadArchivedLogFiles(String path) {

        ByteArrayDataSource bytArrayDS;
        String carbonHomePath = Utils.getCarbonHome();
        String registryPath = carbonHomePath + File.separator + path;

        File file = new File(registryPath);
        if (file.exists() && !file.isDirectory()) {
            try (InputStream is = new BufferedInputStream(new FileInputStream(registryPath))) {
                bytArrayDS = new ByteArrayDataSource(is, "text/xml");
                return new DataHandler(bytArrayDS);
            } catch (FileNotFoundException e) {
                LOG.error("Could not find the requested file : " + path + " in : " + registryPath, e);
                return null;
            } catch (IOException e) {
                LOG.error("Error occurred while reading the file : " + path + " in : " + registryPath, e);
                return null;
            }
        } else {
            LOG.error("Could not find the requested file : " + path + " in : " + registryPath);
            return null;
        }
    }


}
