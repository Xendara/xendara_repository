package org.example.functions;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 */
public class HttpTriggerFunction {
    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    @FunctionName("HttpTrigger-Java")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req",
                    methods = {HttpMethod.GET, HttpMethod.POST},
                    authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");
// get the request
        String requestJson = request.getBody().orElse("");
        try {
            JSONObject message = new JSONObject(requestJson);
            String query = message.getJSONObject("message").getString("text");
            int chat_id = message.getJSONObject("message").getJSONObject("chat").getInt("id");
            int reply_to_message_id = message.getJSONObject("message").getInt("message_id");
            String jsonDocument = "{\"query\": \"" + query + "\"}";
//send http request to Dadata
            URL url = new URL("https://suggestions.dadata.ru/suggestions/api/4_1/rs/findById..");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Token cbf5ed1276128a3ad7827230be8a22e386cb2217");
            byte[] outputBytes = jsonDocument.getBytes(StandardCharsets.UTF_8);
            OutputStream os = connection.getOutputStream();
            os.write(outputBytes);
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }

//parse Dadata and build response
            JSONObject obj = new JSONObject(response.toString());
            String nameValue = obj.getJSONArray("suggestions").getJSONObject(0).getString("value");
// String kppValue = obj.getJSONArray("suggestions").getJSONObject(0).getJSONObject("data").getString("kpp");
            JSONObject sendMessage = new JSONObject();
            sendMessage.put("method", "sendMessage");
            sendMessage.put("chat_id", chat_id);
            sendMessage.put("reply_to_message_id", reply_to_message_id);
            sendMessage.put("text", nameValue);
            String stringAnswer = sendMessage.toString();

            return request.createResponseBuilder(HttpStatus.OK).header("Content-Type", "application/json").
                    body(stringAnswer).build();


        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return request.createResponseBuilder(HttpStatus.BAD_REQUEST).header("Content-Type", "application/json")
                .body("\"method\": \"send message\"" + "\n"
                        + "\"text\": " + "\"" + "\"error\"" + "\"")
                .build();
    }
}
