# Spring Boot, Microsoft Azure IoT Hub

src/main/resourcesにあるapplication.propertiesにAzureに登録したデバイスの情報を記述します。

## Azureにデバイスを登録する手順

### Node.JSとnpmのインストール

```
$ sudo -s
$ sudo apt-get install nodejs
$ sudo apt-get install npm
$ sudo ln -s /usr/bin/nodejs /usr/bin/node
```

### iothub-explorerのインストール

GitHubの[iothub-explorer](https://github.com/Azure/azure-iot-sdks/blob/master/tools/iothub-explorer/readme.md)を参考にインストールします。

```
$ sudo npm install -g iothub-explorer@latest
```

```
$ iothub-explorer help
```

application.propertiesのmqtt.hostnameを設定します。

[Azure Portal](https://portal.azure.com/)のIoT Hubの設定から、共有アクセスポリシーを選択し、iothubownerの接続文字列をクリップボードにコピーして、つぎのコマンドを実行します。

```
$ iothub-explorer login "HostName=<my-hub>.azure-devices.net;SharedAccessKeyName=<my-policy>;SharedAccessKey=<my-policy-key>"
```

次のコマンドを実行して、application.propertiesのmqtt.deviceIdとmqtt.deviceKeyを設定します。

```
$ iothub-explorer create new-device-id --connection-string
```

# MQTTに接続するユーザ名とパスワード

## ユーザ名

{iothubhostname}/{device_id}

## パスワード

SharedAccessSignature sig={signature-string}&se={expiry}&sr={URL-encoded-resourceURI}

```
String password = String.format(
    "SharedAccessSignature sig=%s&se=%s&sr=%s",
    URLEncoder.encode(sig, StandardCharsets.UTF_8.name()),
    expiry,
    scope);
```

### expiry

有効期限でUNIXタイムスタンプ(秒)のlong値です

### URL-encoded-resourceURI(scope)

{iothubhostname}/devices/{device_id}

### signature-string(sig)

デバイスのキーをBase64でデコードしたバイナリをHMAC-SHA256のキーとして使用します。

```
String deviceKey = properties.getDeviceKey();

Mac mac = Mac.getInstance("HmacSHA256");
mac.init(new SecretKeySpec(Base64.getMimeDecoder().decode(deviceKey),
    "HmacSHA256"));
```

URL-encoded-resourceURIとexpiryを改行で分けたメッセージを署名します

```
String scope = String.format("%s/devices/%s",
    properties.getHostname(),
    URLEncoder.encode(properties.getDeviceId(), StandardCharsets.UTF_8.name()));
String message = String.format("%s\n%s", scope, expiry);
String sig = Base64.getMimeEncoder().encodeToString(mac.doFinal(message.getBytes()));
```

# IoT Hubへのメッセージの送信

devices/{device_id}/messages/events/ または devices/{device_id}/messages/events/{property_bag} をトピック名としてメッセージを送信できます。

# IoT Hubからのメッセージの受信

devices/{device_id}/messages/devicebound/# をトピックフィルターとして使用してサブスクライブできます。

