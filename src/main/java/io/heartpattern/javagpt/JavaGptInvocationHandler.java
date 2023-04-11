package io.heartpattern.javagpt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import io.heartpattern.javagpt.dto.ChatRequest;
import io.heartpattern.javagpt.dto.Message;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;

class JavaGptInvocationHandler implements InvocationHandler {
    private final String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    private final JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper);

    public JavaGptInvocationHandler(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String className = proxy.getClass().getName();
        String methodName = method.getName();
        String returnType = method.getReturnType().getName();
        String returnTypeDescription = objectMapper.writeValueAsString(jsonSchemaGenerator.generateSchema(method.getReturnType()));

        Gpt gpt = method.getDeclaredAnnotation(Gpt.class);
        String description = gpt == null ? null : gpt.value();

        StringBuilder builder = new StringBuilder();
        builder.append("Evaluate method. Emit only return value in result field of json format without and description or decorator\n");
        builder.append("Class: ").append(className).append("\n");
        builder.append("Method: ").append(methodName).append("\n");
        if (description != null)
            builder.append("Method description: ").append(description).append("\n");
        builder.append("Return type: ").append(returnType).append("\n");
        builder.append("Return type json schema: ").append(returnTypeDescription).append("\n");
        builder.append("Arguments: ").append("\n");
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            String argName = method.getParameters()[i].getName();
            String argValue = objectMapper.writeValueAsString(arg);
            builder.append("  ").append(argName).append(": ").append(argValue).append("\n");
        }

        String prompt = builder.toString();
        ChatRequest request = new ChatRequest(
                "gpt-3.5-turbo",
                Collections.singletonList(new Message(Message.Role.USER, prompt))
        );

        HttpPost get = new HttpPost("https://api.openai.com/v1/chat/completions");
        get.setHeader("Authorization", "Bearer " + apiKey);
        get.setHeader("Accept", "application/json");
        get.setHeader("Content-Type", "application/json");
        get.setEntity(new StringEntity(objectMapper.writeValueAsString(request)));


        try (CloseableHttpClient client = HttpClients.createDefault(); CloseableHttpResponse request1 = client.execute(get)) {
            String rawResponse = new BufferedReader(new InputStreamReader(request1.getEntity().getContent())).readLine();
            JsonNode node = objectMapper.readTree(rawResponse);
            JsonNode node2 = objectMapper.readTree(objectMapper.treeToValue(node.get("choices").get(0).get("message").get("content"),String.class));
            return objectMapper.treeToValue(node2.get("result"), method.getReturnType());
        } catch (Exception ignored) {
            throw new Exception("Invalid chat gpt response.");
        }
    }
}
