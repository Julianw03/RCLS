# openapi-java-client

## Requirements

Building the API client library requires [Maven](https://maven.apache.org/) to be installed.

## Installation & Usage

To install the API client library to your local Maven repository, simply execute:

```shell
mvn install
```

To deploy it to a remote Maven repository instead, configure the settings of the repository and execute:

```shell
mvn deploy
```

Refer to the [official documentation](https://maven.apache.org/plugins/maven-deploy-plugin/usage.html) for more information.

After the client library is installed/deployed, you can use it in your Maven project by adding the following to your *pom.xml*:

```xml
<dependency>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-java-client</artifactId>
    <version>110.0.0.3217</version>
    <scope>compile</scope>
</dependency>

```

And to use the api you can follow the examples bellow:

```java

    //Set bearer token manually
    ApiClient apiClient = new ApiClient("petstore_auth_client");
    apiClient.setBasePath("https://localhost:8243/petstore/1/");
    apiClient.setAccessToken("TOKEN", 10000);

    //Use api key
    ApiClient apiClient = new ApiClient("api_key", "API KEY");
    apiClient.setBasePath("https://localhost:8243/petstore/1/");

    //Use http basic authentication
    ApiClient apiClient = new ApiClient("basicAuth");
    apiClient.setBasePath("https://localhost:8243/petstore/1/");
    apiClient.setCredentials("username", "password");

    //Oauth password
    ApiClient apiClient = new ApiClient("oauth_password");
    apiClient.setBasePath("https://localhost:8243/petstore/1/");
    apiClient.setOauthPassword("username", "password", "client_id", "client_secret");

    //Oauth client credentials flow
    ApiClient apiClient = new ApiClient("oauth_client_credentials");
    apiClient.setBasePath("https://localhost:8243/petstore/1/");
    apiClient.setClientCredentials("client_id", "client_secret");

    PetApi petApi = apiClient.buildClient(PetApi.class);
    Pet petById = petApi.getPetById(12345L);

    System.out.println(petById);
  }
```

## Recommendation

It's recommended to create an instance of `ApiClient` per thread in a multithreaded environment to avoid any potential issues.

## Author



