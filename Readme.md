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
1. **Provided jar**: You can run the provided released jar file with the command:
   ```bash
   java -jar RCLS.jar
   ```
   For this you will need to have Java 21 or higher installed. You may also provide your own config overrides.
   This might be useful when certain timeout parameters are set too strict for your environment. 
   To archive that you must provide a .yml file and run the jar with the following command:
    ```bash
    java -jar RCLS.jar --spring.config.import=file:/path/to/config.yml
    ```

### Building the Project
1. **Docker**: [TODO]

Running via Docker is not supported, as the project needs access to local Process and Network information.
#### Licenses and Dependencies

[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2FJulianw03%2FRCLS.svg?type=large&issueType=license)](https://app.fossa.com/projects/git%2Bgithub.com%2FJulianw03%2FRCLS?ref=badge_large&issueType=license)