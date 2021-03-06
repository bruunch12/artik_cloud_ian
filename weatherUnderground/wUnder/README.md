This is a template project. Based on it, you can write custom Cloud Connector code, configure parameters, and perform both unit and integration testing. 

Refer to the sibling repository sample-xxx for examples of the Cloud Connector. These Cloud Connectors have been tested and work in production. For example, you can connect a device of type "moves" in the ARTIK Cloud [User Portal](https://www.artik.cloud).

# Install

* Pre-requisite: [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html) already installed. 
* Sync the repository that contains the Cloud Connector SDK and the template from [GitHub](https://github.com/artikcloud/artikcloud-cloudconnector-sdk).

Each command mentioned in this document should be launched from the current project directory, which contains the `build.gradle` file. 

When running '../gradlew XXXX' commands in Section "Usages", `gradlew` will download the required tools and libraries on demand.

# Usages

### Code & compile Groovy code:

You can compile the template project without changing any code. However, the Cloud Connector built from the template does not perform any real operations.

 * Edit [src/main/groovy/cloudconnector/MyCloudConnector.groovy](src/main/groovy/cloudconnector/MyCloudConnector.groovy)
    * Overwrite necessary methods to process subscription, notifications, and data fetching from the third-party cloud
    * Use the following libraries in Groovy code :
     * [artikcloud-cloudconnector-api](http://artikcloud.github.io/artikcloud-cloudconnector-sdk/apidoc/)
     * [commons-codec 1.10](https://commons.apache.org/proper/commons-codec/archives/1.10/apidocs/index.html)
     * [scalactic 2.2.4](http://www.scalactic.org/), which provides a few helper constructs including classes [Or, Good, Bad](http://www.scalactic.org/user_guide/OrAndEvery)
 * Compile to check compilation errors
  ```
  ../gradlew classes
  ```

### Unit test

 * Edit [src/test/groovy/cloudconnector/MyCloudConnectorSpec.groovy](src/test/groovy/cloudconnector/MyCloudConnectorSpec.groovy)
 * Run unit test. Make sure to provide 'cleanTest' in the command to force run. Otherwise `gradlew` skips running test if the code was not changed since the command was last run:
  ```
  ../gradlew cleanTest test
  ```

### Integration testing in the local environment:
You can perform manual integration testing on an HTTP (HTTPS) local server. The local server provides the minimal runtime environment to run the Cloud Connector. On this server, you can test authentication and fetching data from the third-party cloud before uploading your Cloud Connector code to the ARTIK Cloud [Developer Portal](https://developer.artik.cloud/).

 * Edit [src/main/groovy/cloudconnector/cfg.json](src/main/groovy/cloudconnector/cfg.json) to set up the authentication from your local test server to the third-party cloud. The information in cfg.json is pretty much the same as the information provided in [Cloud Authentication UI](https://developer.artik.cloud/documentation/connect-the-data/using-cloud-connectors.html#set-authentication-parameters) at the ARTIK Cloud Developer Portal. Here you will have to use cfg.json instead of the UI to do that. You can refer to the following resources to learn how to write cfg.json:
    * [cfg.json.sample](src/main/groovy/cloudconnector/cfg.json.sample) explains each JSON key.
    * sample-xxx/src/main/groovy/\<package\>/cfg.json is for each example cloud.
 * Test the Cloud Connector on a local HTTP server.
    * To receive notifications, your local server should be accessible via Internet (for example, use a server accessible from the outside or use ssh tunnel with port forwarding).
    * You can customize the port of the local server (9080 by default for HTTP and 9083 for HTTPS) by editing [utils.MyCloudConnectorRun](src/test/groovy/utils/MyCloudConnectorRun.groovy).
    * Temporarily update the configurations on the third-party cloud to use your local server for authentication and notification.
    * Run the test server (device type is hardcoded to 0000).
  ```
  ../gradlew runTestServer
  ```
    In the console, you should see the redirect and notification URIs as follows:
     ```
     redirect uri: http://localhost:9080/cloudconnectors/0000/auth
     notification uri: http://localhost:9080/cloudconnectors/0000/thirdpartynotifications
     ```
    * Open http://localhost:9080/ in your browser, click on "Authorize".
    * Follow the instructions displayed on the web page.
    * Generate new data in the third-party application, which triggers the notification from the third-party cloud to your local test server.
    * In the console, the test server should print a line with "0000: queuing event Event(" for each event generated by MyCloudConnector. One event is for one ARTIK Cloud message.

*After finishing your integration testing, you should change the configuration on the third-party cloud to use ARTIK Cloud instead of your local test server for authentication and notification.*

# Notes for MyCloudConnector.groovy

MyCloudConnector is a derived class that extends [CloudConnector](http://artikcloud.github.io/artikcloud-cloudconnector-sdk/apidoc/#cloud.artik.cloudconnector.api_v1.CloudConnector). Check out the following documentation articles to learn how to code it.

 * [High-level view of the methods of CloudConnector class](https://developer.artik.cloud/documentation/connect-the-data/using-cloud-connectors.html#about-the-cloud-connector-groovy-code)
 * [Moves Cloud Connector code explained](https://developer.artik.cloud/documentation/tutorials/your-first-cloud-connector.html#implementation-details)
 * [CloudConnector API Doc](http://artikcloud.github.io/artikcloud-cloudconnector-sdk/apidoc/), which lists functions and structures, and explains goals and usages.

### Best practices

 * CloudConnector should be stateless. So it should not have instance data, but you can use final constants.
 * All methods return [Or\<T, Failure\>](http://doc.scalatest.org/2.2.4/index.html#org.scalactic.Or) instead of a value of type T. `Or` is a `Good` or `Bad` instance depending on whether the execution of the method succeeds or runs into an error. Create `Good` or `Bad` instance as follows: 
    * `new Good(t)`, where t is an instance of `T`.
    * `new Bad(new Failure('this is an error message, put error details here'))`.

### Tips

* Using custom parameters in your Cloud Connector Groovy code improves the flexibility of your code. Please refer to [About custom parameters](https://developer.artik.cloud/documentation/connect-the-data/using-cloud-connectors.html#about-custom-parameters) to learn about custom parameters and how to use them. Per the doc, you add custom parameters to the CUSTOM PARAMETERS table in the Connector Code tab in the Developer Portal. When performing unit and integration testing locally, your Groovy code cannot access custom parameters since the table is not accessible locally. In order to pass the testing, you edit src/main/groovy/cloudconnector/cfg.json. Specifically, add all custom parameters in CUSTOM PARAMETERS table to `parameters` JSON object in cfg.json as follows:

```
{
  ...
    "parameters": {
      "myUrl": "http://www.foo.com/bar",
      "myNumber": "10",
    },
  ...
}
```

* Perform unit and integration testing before submitting the Groovy Code in the ARTIK Cloud Developer Portal. This will increase the probability that the code is approved by ARTIK Cloud and it works as you expected. 
* If you want to do type checking, uncomments the class annotation `//@CompileStatic`. Then, JSON manipulation will be more verbose.

### Unit Tests

Both the samples and the template use [Spock framework](http://spockframework.github.io/spock/docs/1.0/index.html) for unit tests. It is a Groovy framework that reports inequality more easily than JUnit does. However, you can use your favorite framework to do your own unit tests.

The class `utils.FakeContext` provides a default Context implementation, which you can use and customize in your tests.

The class `utils.Tools` provides helper functions, e.g. for comparing list of events.

You can create text files under src/test/resources/<package> to store JSON and CSV, etc. The unit test calls utils.Tools.readFile() to read the content from these files.

There are more unit test examples in the sample projects, e.g., how to compare JSON and Events, how to read text file, etc.

### Integration Testing

If you change the package name of `MyCloudConnector.groovy` from `cloudconnector` to something else (for example, `com.moves`), you will have to modify `build.gradle` file to use the correct package name before running `../gradlew runTestServer`. 

Open `build.gradle` in an editor, and replace `cloudconnector` in the following code snippet with the correct package name (for example, `com.moves`). 
```
task runTestServer(type:JavaExec) {
  main = System.getProperty("exec.mainClass") ?: "utils.MyCloudConnectorRun"
  args = ["cloudconnector"]
  classpath = sourceSets.test.runtimeClasspath
}
```

It is possible to customize ports, hostname, certificate by editing the file [src/test/groovy/utils/MyCloudConnectorRun.groovy](src/test/groovy/utils/MyCloudConnectorRun.groovy).

You can configure logging in [src/test/resources/logback-test.xml](src/test/resources/logback-test.xml).

#### Using ngrok

[Ngrok](https://ngrok.com) is a service to ease the dev of web service by providing a "Secure tunnels to localhost", with support of http & https.
1. download and install the ngrok client on your desktop (the free version is enough to work)
2. launch the ngrok client (You can let the ngrok client running all the day)
```
ngrok http 9080
```
3. it should display the your public http and https url, like 
```
...
Forwarding                    http://92832de0.ngrok.io -> localhost:9080
Forwarding                    https://92832de0.ngrok.io -> localhost:9080
```
4. on an other shell, start your test server with the public name ngrok give you:
```
../gradlew -DbaseUri=https://92832de0.ngrok.io runTestServer
```
5. Update the configuration of "authentication" and "webhook" of your application on the third party cloud.

#### Enable HTTS/SSL

If you don't use ngrok and if the third-party cloud requires HTTPS for authentication and notification, then you have to customize `utils/MyCloudConnectorRun.groovy`.

1. Select a domain/host name that you can use (e.g., my.domain.com). **Don't forget to register it into your /etc/hosts or your DNS' registrar**
1. Enable HTTPS port by setting a no-null value for httpsPort parameter (eg. 9083). If you try to connect to https://my.domain.com:9083/ a self-signed certificate will be generated and used.

  ```
  def srvCfg = SimpleHttpServer.makeServerConfig('my.domain.com', 9080, 9083, null, null, null, null)
  ```
1. If the third-party cloud doesn't accept self-signed certificate (often the case, because doing so is a security failure), then you have to acquire an SSL certificate from an authority, or you can (since the end of 2015) acquire it with [Let's Encrypt](https://letsencrypt.org/howitworks/) (free).

  ```
  # install letsencrypt
  
  # generate a certificate from the server (my.domain.com)
  
  DOMAIN=my.domain.com
  
  sudo ./letsencrypt-auto -d $DOMAIN certonly --standalone
  
  sudo tar -czvf ../$DOMAIN.tar.gz /etc/letsencrypt/archive/$DOMAIN
  ```
1. Store the certificate in a keystore (usable by TestServer)

  ```
  cd artikcloud-cloudconnector-sdk/<my_cloudconnector>
  
  tar -xzvf $DOMAIN.tar.gz
  
  cd etc/letsencrypt/archive/$DOMAIN
  
  # convert certificate chain + private key to the PKCS#12 file format, select a password of at least 6 characters
  
  openssl pkcs12 -export -out keystore.pkcs12 -in fullchain1.pem -inkey privkey1.pem
  
  # convert PKCS#12 file into Java keystore format, use the same password than previously for keystore (source and destination), else you'll have Exception like "java.security.UnrecoverableKeyException: Cannot recover key"
  
  keytool -importkeystore -srckeystore keystore.pkcs12 -srcstoretype pkcs12 -destkeystore keystore.jks
  
  # don't need the PKCS#12 file anymore
  
  rm keystore.pkcs12
  ```
1. Edit `MyCloudConnectorRun.groovy` to use the keystore with the certificate (don't forget to change the domain name, the path, and the password).

  ```
  def srvCfg = SimpleHttpServer.makeServerConfig('my.domain.com', 9080, 9083, "etc/letsencrypt/archive/my.domain.com/keystore.jks", null, "keyStorePassword", null)
  ```
1. Try it https://my.domain.com:9083/
