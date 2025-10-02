# RCLS

RCLS is a proof-of-concept for a Client build on top of the existing Riot Client Technology. Developed to learn a bit of
Spring Boot and Frontend development with React.

### TLDR

Create your own Frontend and custom logic for the Riot Client.

### Disclaimer

RCLS isn't endorsed by Riot Games and doesn't reflect the views or opinions of Riot Games or anyone officially involved
in producing or managing Riot Games properties.
Riot Games, and all associated properties are trademarks or registered trademarks of Riot Games, Inc.

### Running the Project

You have the following options:

1. **Provided jar**:
   For this you will need to have Java 21 or higher installed. You can check your Java version with:
   ```bash
    java -version
   ```
   
   
   You can run the provided released jar file with the command:
   ```bash
   java -jar RCLS.jar
   ```
   You may also provide your own config overrides.
   This might be useful when certain timeout parameters are set too strict for your environment or you want to manually
   tweak some parameters. For more information on the available configuration options, refer to the `application.yml`
   file in the Backend.

   To archive that you must provide a .yml file and run the jar with the following command:

   ```bash
    java -jar RCLS.jar --spring.config.import=file:/path/to/config.yml
   ```

Running via Docker is not supported, as the project needs access to local Process and Network information.

### Building the Project

1. **Docker**: If you wish to build the project yourself via docker or make changes to the code and test them, you can
   use the provided Dockerfile.
   ```bash
   docker build -t rcls-builder .
   ```
   After that you can run the image with:
   ```bash
   docker run --volume RCLS-Build-Out:/app/volume rcls-builder
   ```
   The build output will be located in the `RCLS-Build-Out` volume. You may change the volume to a mount of your
   preference.

### Testing out the API

**Please do not use 'localhost' to access the frontend as that is blocked by hcaptcha. Use the explicit IP address '127.0.0.1' instead.**

Once the application is running, you can access the [Swagger UI](https://127.0.0.1/swagger-ui/index.html#/) or the 
minimalistic [Frontend](https://127.0.0.1/) via your browser.

The frontend is very basic and hacked together. It is mainly there to demonstrate some of the basic capabilities of the
backend. Most of the features are accessible via the Swagger UI.

#### Planned Features / ToDo's

- [ ] Create a pipeline that periodically checks if there are new versions of the Riot Client and updating the generated
  OpenAPI specification.
- [ ] Better Frontend
- [x] Communication of Backend and Frontend via Websockets -> Basic implementation done

#### Licenses and Dependencies

[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2FJulianw03%2FRCLS.svg?type=large&issueType=license)](https://app.fossa.com/projects/git%2Bgithub.com%2FJulianw03%2FRCLS?ref=badge_large&issueType=license)