# RCLS

RCLS is a proof-of-concept for a Client build on top of the existing Riot Client Technology. Developed to learn a bit of
Spring Boot and Frontend development with React.

### TLDR
Create your own Frontend and custom logic for the Riot Client.

### Security
RCLS does **NOT** make any security guarantees during execution.
Especially during the "unlocked" phase of accounts the Strings are available
in plaintext in memory. Even after the "locked" phase, the Strings are still available in memory, due to the Garbage Collector
and String Pooling. I have tried some concepts but eventually during the JSON Serialization the bytes are converted to a String
and are therefore affected by String Pooling. If anyone has some ideas on how to solve this and still being able to interact
with the Riot Client, feel free to create an issue or PR.
I will just assume that if your machine is compromised, you have bigger problems than the leak of your Riot account credentials.
Moreover, the provided keystore allows anyone to retrieve the used self-signed certificate.

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