package biz.hcnet.eu;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

import emds.epi.decl.server.deployment.deploymentservice.AquireDeploymentTokenRequest;
import emds.epi.decl.server.deployment.deploymentservice.AquireDeploymentTokenResponse;
import emds.epi.decl.server.deployment.deploymentservice.DeActivateScenarioRequest;
import emds.epi.decl.server.deployment.deploymentservice.DeActivateScenarioRequest.ScenarioID;
import emds.epi.decl.server.deployment.deploymentservice.DeActivateScenarioRequest.Token;
import emds.epi.decl.server.deployment.deploymentservice.DeActivateScenarioResponse;
import emds.epi.decl.server.deployment.deploymentservice.GetAllDeployedScenariosRequest;
import emds.epi.decl.server.deployment.deploymentservice.GetAllDeployedScenariosResponse;
import emds.epi.decl.server.deployment.deploymentservice.GetAllDeployedScenariosResponse.Result;
import emds.epi.decl.server.deployment.deploymentservice.ReDeployScenarioCallbackRequest;
import emds.epi.decl.server.deployment.deploymentservice.ReDeployScenarioCallbackResponse;

public class DeploySenarioAdHocTest extends WebServiceGatewaySupport {

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	private final String SCENARIO_ID = "fb9e271d-0f1e-4f89-922c-c19a96de2265";

	private String soapUrl = "http://chrzmdb1017.eu.hcnet.biz:7019";

	// http://TC01-BH.internal.schlaepfer.com:8019/OrchestraRemoteService/DeploymentService/Service

	public Jaxb2Marshaller marshaller() {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setContextPath("emds.epi.decl.server.deployment.deploymentservice");
		return marshaller;
	}

	public Jaxb2Marshaller unmarshaller() {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setContextPath("emds.epi.decl.server.deployment.deploymentservice");
		return marshaller;
	}

	public UsernamePasswordCredentials usernamePasswordCredentials() {
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("hhci-go", "hhci1234");
		return credentials;
	}

	public HttpComponentsMessageSender httpComponentsMessageSender() {
		HttpComponentsMessageSender sender = new HttpComponentsMessageSender();
		sender.setCredentials(usernamePasswordCredentials());
		return sender;
	}

	public SoapActionCallback soapActionCallback() {
		SoapActionCallback callback = new SoapActionCallback("getAllDeployedScenarios");
		return callback;
	}

	public RequestConfig requestConfig() {

		RequestConfig requestConfig = RequestConfig.custom().setAuthenticationEnabled(true).build();
		return requestConfig;
	}

	public CredentialsProvider credentialsProvider() {
		CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(AuthScope.ANY, usernamePasswordCredentials());
		return credentialsProvider;
	}

	private static class ContentLengthHeaderRemover implements HttpRequestInterceptor {

		@Override
		public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {

			// fighting org.apache.http.protocol.RequestContent's
			// ProtocolException("Content-Length header already present");
			request.removeHeaders(HTTP.CONTENT_LEN);
		}
	}

	public HttpComponentsMessageSender messageSender() {

		RequestConfig requestConfig = RequestConfig.custom().setAuthenticationEnabled(true).build();

		HttpClientBuilder httpClientBuilder = HttpClients.custom();

		HttpClient httpClient = httpClientBuilder.addInterceptorFirst(new ContentLengthHeaderRemover())
				.setDefaultRequestConfig(requestConfig).setDefaultCredentialsProvider(credentialsProvider()).build();

		HttpComponentsMessageSender messageSender = new HttpComponentsMessageSender(httpClient);
		return messageSender;
	}

	@Before
	public void Setup() {
		getWebServiceTemplate().setMarshaller(marshaller());
		getWebServiceTemplate().setMessageSender(messageSender());

		getWebServiceTemplate().setUnmarshaller(unmarshaller());
	}

	@Test
	@Ignore
	public void listDeployments() {
		GetAllDeployedScenariosRequest request = new GetAllDeployedScenariosRequest();

		GetAllDeployedScenariosResponse response = (GetAllDeployedScenariosResponse) getWebServiceTemplate()
				.marshalSendAndReceive(soapUrl + "/OrchestraRemoteService/DeploymentService/Service", request,
						soapActionCallback());

		List<Result> results = response.getResult();
		for (Result result : results) {

			log.info("Scenario Name: {}", result.getName());
			log.info("               {}", result.getNodeId());

		}

	}
	
	@Test
	public void testRedeploy() {
	    
	    log.info("acquire deployment token");
	    String t = aquireDeploymentToken();
	    log.info("deactivate scenarion");
	    deActivateScenaio(t, SCENARIO_ID);
	    
	    byte[] base64EncodedData = null;
	    
	    try {
            base64EncodedData = Base64.encodeBase64(loadFileAsBytesArray("C:/tmp/sc_ESB_X_MDM_Stamm_sync.psc"));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	    
	    log.info("Redeploy scenario");
	    reDeployScenarioCallback(t, base64EncodedData);
	    
	}
	
	private String aquireDeploymentToken() {
	    
	    AquireDeploymentTokenRequest request = new AquireDeploymentTokenRequest();
	    
	    AquireDeploymentTokenResponse response = (AquireDeploymentTokenResponse) getWebServiceTemplate() 
	            .marshalSendAndReceive(soapUrl + "/OrchestraRemoteService/DeploymentService/Service", request,
                        soapActionCallback());
	    
	    log.info("tokenDate: {}",response.getResult().getTokenData());
	    
	    return response.getResult().getTokenData();
	
	}
	
	private void deActivateScenaio(String tokenData, String scenarioId) {
	    
	    DeActivateScenarioRequest request = new DeActivateScenarioRequest();
	    
	    Token token = new Token();
	    token.setNodeID("RZ1-RT-Z01");
	    token.setTokenData(tokenData);
	    
	    ScenarioID scenarioID = new ScenarioID();
	    scenarioID.setScenario(scenarioId);
	    
	    request.setScenarioID(scenarioID);
	    request.setToken(token);
	    
	    DeActivateScenarioResponse response = (DeActivateScenarioResponse) getWebServiceTemplate()
	            .marshalSendAndReceive(soapUrl + "/OrchestraRemoteService/DeploymentService/Service", request,
                        soapActionCallback());
	    
	    log.info("response: {}", response);
	            
	}
	
	private boolean reDeployScenarioCallback(String tokenData, byte[] serializedData) {
	    
	    ReDeployScenarioCallbackRequest request = new ReDeployScenarioCallbackRequest();
	    
	    emds.epi.decl.server.deployment.deploymentservice.ReDeployScenarioCallbackRequest.Token token = new emds.epi.decl.server.deployment.deploymentservice.ReDeployScenarioCallbackRequest.Token();
        token.setNodeID("RZ1-RT-Z01");
        token.setTokenData(tokenData);
        
        request.setToken(token);
	    request.setComment("Deployed by ci-go");
	    
	    request.setSerializedScenario(serializedData);
	    
	    ReDeployScenarioCallbackResponse response = (ReDeployScenarioCallbackResponse)  getWebServiceTemplate()
                .marshalSendAndReceive(soapUrl + "/OrchestraRemoteService/DeploymentService/Service", request,
                        soapActionCallback());
	    
	    log.info("n: {}", response.toString());
	    
	    return true;
	    
	}
	
	private static byte[] loadFileAsBytesArray(String fileName) throws Exception {
	    File file = new File(fileName);
        int length = (int) file.length();
        BufferedInputStream reader = new BufferedInputStream(new FileInputStream(file));
        byte[] bytes = new byte[length];
        reader.read(bytes, 0, length);
        reader.close();
        return bytes;
	    
	}

}
