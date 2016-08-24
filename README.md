# Spring Boot, Microsoft Azure IoT Hub

## MQTT Client

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
