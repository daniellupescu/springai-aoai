## Spring AI implementation example

This SpringBoot app exposes 2 chat completion APIs: a synchronous one and a stream one.
It relies on spring-ai-azure-openai-spring-boot-starter 0.8.0-SNAPSHOT.
The controller exposes the 2 endpoints.
The service uses:
- the ChatClient class to call Azure OpenAI for the synchronous API
- the AzureOpenAiChatClient class to call Azure OpenAI using a stream 

It uses the tokenizer from https://github.com/knuddelsgmbh/jtokkit to check if the context lenght is valid and also adjust the context window if necessary. 

Spring Retry is used in case Azure OpenAI API returns a 429 Too Many Requests status code.

The System message is read from the file: /src/main/resources/prompts/prompt-template.txt

The class ApplicationReadyListener contains 2 calls to check that the 2 APIs are working well.

Don't forget to define your Azure OpenAI endpoint and Api-Key in application.yml.

## Why this app?

The goal is to provide more advanced examples than the ones on https://github.com/rd-1-2022
This app does not pretend to be deployed as is, as a SpringBoot app on a server.
It is only a piece of code that anyone can copy into its SpringBoot app.

## Prerequisites
- Maven
- JDK 17 (not tested on other versions)

## Build and run the app
In the root folder:

```sh
mvn clean package
```

```sh
mvn spring-boot:run
```

## License
[MIT](https://choosealicense.com/licenses/mit/)