/*
 * Copyright 2016 Pigumer Group Inc.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableConfigurationProperties(MqttProperties.class)
@EnableAutoConfiguration
public class MqttHandlerTest {

    @Autowired
    MqttProperties properties;

    @Test
    public void test() throws Exception {
        String hostname = properties.getHostname();
        String deviceId = properties.getDeviceId();
        String deviceKey = properties.getDeviceKey();

        assertThat(StandardCharsets.UTF_8.name(), is("UTF-8"));

        String username = String.format("%s/%s", hostname, deviceId);

        long expiryTime = Instant.now().plus(1, ChronoUnit.HOURS).getEpochSecond();
        String scope = String.format("%s/devices/%s", hostname, deviceId);
        String message = String.format("%s\n%s", scope, expiryTime);

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(Base64.getMimeDecoder().decode(deviceKey.getBytes()), "HmacSHA256"));
        String sig = Base64.getMimeEncoder().encodeToString(mac.doFinal(message.getBytes()));

        String expected = String.format("SharedAccessSignature sig=%s&se=%s&sr=%s",
                URLEncoder.encode(sig, StandardCharsets.UTF_8.name()), expiryTime, scope);
        assertThat(expected, is(not(nullValue())));
    }

}
