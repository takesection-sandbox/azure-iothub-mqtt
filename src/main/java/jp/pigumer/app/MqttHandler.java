/*
 * Copyright 2016 Pigumer Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jp.pigumer.app;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

public class MqttHandler implements MqttCallback, InitializingBean, DisposableBean {

	private static final Logger LOG = LoggerFactory.getLogger(MqttHandler.class);
	
	MqttAsyncClient client;

	final String hostname;
	
	final String deviceId;

	final String deviceKey;
	
	public MqttHandler(String hostname, String deviceId, String deviceKey) {
		this.hostname = hostname;
		this.deviceId = deviceId;
		this.deviceKey = deviceKey;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		connect();
	}
	
	public void connect() throws Exception {
		String url = String.format("ssl://%s:8883", hostname);
		LOG.info("url: " + url);
		client = new MqttAsyncClient(url, deviceId, new MemoryPersistence());

		MqttConnectOptions options = new MqttConnectOptions();
		String userName = buildUserName(hostname, deviceId);
		char[] password = buildPassword(hostname, deviceId, deviceKey);
		options.setUserName(userName);
		options.setPassword(password);
		client.setCallback(this);
		
		IMqttToken token = client.connect(options);
		token.waitForCompletion();
		LOG.info("connected: " + token.toString());
		
		String topicFilter = String.format("devices/%s/messages/devicebound/#", deviceId);
		IMqttToken subscriptionToken = client.subscribe(topicFilter, 1);
		subscriptionToken.waitForCompletion();
		LOG.info("subscribe: " + topicFilter);
	}
	
	public MqttAsyncClient getClient() {
		return client;
	}
	
	@Override
	public void connectionLost(Throwable cause) {
		LOG.warn("connectionLost", cause);
		try {
			connect();
		} catch (Exception e) {
			LOG.error("connectionLost", e);
		}
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		LOG.info("messageArrives[" + topic + "]: " + message.toString());
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		LOG.info("deliveryCompleted");
	}

	public static String buildUserName(String hostname, String deviceId) {
		return String.format("%s/%s", hostname, deviceId);
	}
	
	public static char[] buildPassword(String hostname, String deviceId, String deviceKey) throws Exception {
		long expiryTime = Instant.now().plus(60, ChronoUnit.MINUTES).getEpochSecond();
		String scope = String.format("%s/devices/%s", hostname,
				URLEncoder.encode(deviceId, StandardCharsets.UTF_8.name()));
		String message = String.format("%s\n%s", scope, expiryTime);

		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(Base64.getMimeDecoder().decode(deviceKey), "HmacSHA256"));
		String sig = Base64.getMimeEncoder().encodeToString(mac.doFinal(message.getBytes()));

		String password = String.format("SharedAccessSignature sig=%s&se=%s&sr=%s",
				URLEncoder.encode(sig, StandardCharsets.UTF_8.name()),
				expiryTime,
				scope);
		return password.toCharArray();
	}
	
	@Override
	public void destroy() throws MqttException {
		if (client.isConnected()) {
			client.disconnect();
		}
		LOG.info("closed");
	}

	public void send(String message) throws MqttException {
		IMqttDeliveryToken token = client.publish(String.format("devices/%s/messages/events/", deviceId), new MqttMessage(message.getBytes()));
		token.waitForCompletion();
	}

}
