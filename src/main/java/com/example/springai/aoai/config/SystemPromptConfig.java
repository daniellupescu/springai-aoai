package com.example.springai.aoai.config;

import com.example.springai.aoai.utils.ResourceReader;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Class used to initialize the SystemMessage object from a text file in the classpath
 */
@Configuration
public class SystemPromptConfig {

    @Value("classpath:/prompts/prompt-template.txt")
    private Resource promptTemplate;

    /**
     * Creates a SystemMessage object from a Resource object
     *
     * @return
     */
    @Bean
    public SystemMessage systemMessage() {
        return new SystemMessage(ResourceReader.asString(promptTemplate));
    }

}
