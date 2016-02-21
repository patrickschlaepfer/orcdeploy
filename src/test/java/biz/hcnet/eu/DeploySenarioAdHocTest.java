package biz.hcnet.eu;

import java.io.IOException;
import java.util.List;

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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

import emds.epi.decl.server.deployment.deploymentservice.GetAllDeployedScenariosRequest;
import emds.epi.decl.server.deployment.deploymentservice.GetAllDeployedScenariosResponse;
import emds.epi.decl.server.deployment.deploymentservice.GetAllDeployedScenariosResponse.Result;

public class DeploySenarioAdHocTest extends WebServiceGatewaySupport {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private String soapUrl = "http://TC01-BH.internal.schlaepfer.com:8019";

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
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("admin", "admin");
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
	public void listDeployments() {
		GetAllDeployedScenariosRequest request = new GetAllDeployedScenariosRequest();

		GetAllDeployedScenariosResponse response = (GetAllDeployedScenariosResponse) getWebServiceTemplate()
				.marshalSendAndReceive(soapUrl + "/OrchestraRemoteService/DeploymentService/Service", request,
						soapActionCallback());

		List<Result> results = response.getResult();
		for (Result result : results) {

			log.info(" {}", result.getName());

		}

	}

}
