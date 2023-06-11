# Authentication with custom attributes in Amazon Cognito
Amazon Cognito user pools are often used by the customers of AWS for managing users and authentication. Amazon Cognito natively provides features for sign up and sign in with username, email and phone number. But mostly mobile customer look for easy sign up and sign in using custom attributes for their choice instead of username/email/phone number & password. This solution will demonstrate how use Amazon Cognito to sign up and sign in with custom attributes using phone number and mpin.

## Getting Started
The entire solution is built on Java11 and SAM. The instructions below shows the prerequisities, deployment instructions and testing steps.
### Architecture Diagram
![Cognito-custom-auth-arch.png](Cognito-custom-auth-arch.png)
### Prerequisites
* AWS account
* AWS CLI installed and configured
* Java 11
* Apache Maven
* AWS SAM CLI
### Deployment Instructions
1. Create a new directory, navigate to that directory in a terminal and clone the GitHub repository.
2. Go inside the directory using ```cd <directory name>``` command.
3. Use AWS SAM to build the application:
    ```
   sam build
    ```
4. Use AWS SAM to deploy the AWS resources
    ```
   sam deploy --guided
    ```
5. During the prompts:
    * Enter a stack name
    * Enter the desired AWS Region
    * Allow SAM CLI to create IAM roles with the required permissions.

    Once you have run `sam deploy --guided` mode once and saved arguments to a configuration file (samconfig.toml), you can use `sam deploy` in future to use these defaults.
6. Copy the value of `MobileClientId` from the output once SAM executes successfully.
### How it works
### Testing

### Clean up

### License
