import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Random;

public class ApiTestScript {
    private static final String API_ENDPOINT = "https://bfhldevapigw.healthrx.co.in/automation-campus/create/user";
    private static final String ROLL_NUMBER = "YOUR_ROLL_NUMBER";
    private static final HttpClient httpClient = HttpClientBuilder.create().build();
    private static final Gson gson = new Gson();
    private static final Random random = new Random();

    public static void main(String[] args) {
        System.out.println("Starting API tests...");
        testValidInput();
        testMissingRequiredField();
        testInvalidDataTypes();
        testDuplicatePhoneNumber();
        testDuplicateEmail();
        testMissingRollNumber();
        testInvalidRollNumberFormat();
        testBoundaryValues();
        testSpecialCharacters();
        testRateLimiting();
        testDifferentHttpMethods();
        System.out.println("All tests completed.");
    }

    private static void testValidInput() {
        JsonObject data = new JsonObject();
        data.addProperty("firstName", "Test");
        data.addProperty("lastName", "User");
        data.addProperty("phoneNumber", generateRandomPhone());
        data.addProperty("emailId", "test.user" + random.nextInt(10000) + "@example.com");

        HttpResponse response = sendPostRequest(data, true);
        assert response.getStatusLine().getStatusCode() == 201 : "Expected 201, got " + response.getStatusLine().getStatusCode();
        System.out.println("Valid input test passed");
    }

    private static void testMissingRequiredField() {
        String[] requiredFields = {"firstName", "lastName", "phoneNumber", "emailId"};
        for (String field : requiredFields) {
            JsonObject data = new JsonObject();
            data.addProperty("firstName", "Test");
            data.addProperty("lastName", "User");
            data.addProperty("phoneNumber", generateRandomPhone());
            data.addProperty("emailId", "test.user" + random.nextInt(10000) + "@example.com");
            data.remove(field);

            HttpResponse response = sendPostRequest(data, true);
            assert response.getStatusLine().getStatusCode() == 400 : "Expected 400 for missing " + field + ", got " + response.getStatusLine().getStatusCode();
        }
        System.out.println("Missing required field tests passed");
    }

    private static void testInvalidDataTypes() {
        JsonObject[] invalidData = {
            createJsonObject("firstName", 123, "lastName", "User", "phoneNumber", generateRandomPhone(), "emailId", "test@example.com"),
            createJsonObject("firstName", "Test", "lastName", 456, "phoneNumber", generateRandomPhone(), "emailId", "test@example.com"),
            createJsonObject("firstName", "Test", "lastName", "User", "phoneNumber", "invalid", "emailId", "test@example.com"),
            createJsonObject("firstName", "Test", "lastName", "User", "phoneNumber", generateRandomPhone(), "emailId", 789)
        };

        for (JsonObject data : invalidData) {
            HttpResponse response = sendPostRequest(data, true);
            assert response.getStatusLine().getStatusCode() == 400 : "Expected 400 for invalid data type, got " + response.getStatusLine().getStatusCode();
        }
        System.out.println("Invalid data type tests passed");
    }

    private static void testDuplicatePhoneNumber() {
        long phoneNumber = generateRandomPhone();
        JsonObject data1 = createJsonObject("firstName", "Test1", "lastName", "User1", "phoneNumber", phoneNumber, "emailId", "test.user1" + random.nextInt(10000) + "@example.com");
        JsonObject data2 = createJsonObject("firstName", "Test2", "lastName", "User2", "phoneNumber", phoneNumber, "emailId", "test.user2" + random.nextInt(10000) + "@example.com");

        HttpResponse response1 = sendPostRequest(data1, true);
        HttpResponse response2 = sendPostRequest(data2, true);
        assert response2.getStatusLine().getStatusCode() == 400 : "Expected 400 for duplicate phone number, got " + response2.getStatusLine().getStatusCode();
        System.out.println("Duplicate phone number test passed");
    }

    private static void testDuplicateEmail() {
        String email = "test.user" + random.nextInt(10000) + "@example.com";
        JsonObject data1 = createJsonObject("firstName", "Test1", "lastName", "User1", "phoneNumber", generateRandomPhone(), "emailId", email);
        JsonObject data2 = createJsonObject("firstName", "Test2", "lastName", "User2", "phoneNumber", generateRandomPhone(), "emailId", email);

        HttpResponse response1 = sendPostRequest(data1, true);
        HttpResponse response2 = sendPostRequest(data2, true);
        assert response2.getStatusLine().getStatusCode() == 400 : "Expected 400 for duplicate email, got " + response2.getStatusLine().getStatusCode();
        System.out.println("Duplicate email test passed");
    }

    private static void testMissingRollNumber() {
        JsonObject data = createJsonObject("firstName", "Test", "lastName", "User", "phoneNumber", generateRandomPhone(), "emailId", "test.user" + random.nextInt(10000) + "@example.com");
        HttpResponse response = sendPostRequest(data, false);
        assert response.getStatusLine().getStatusCode() == 401 : "Expected 401 for missing roll number, got " + response.getStatusLine().getStatusCode();
        System.out.println("Missing roll number test passed");
    }

    private static void testInvalidRollNumberFormat() {
        String[] invalidRollNumbers = {"abc", "123abc", "-123", "0"};
        JsonObject data = createJsonObject("firstName", "Test", "lastName", "User", "phoneNumber", generateRandomPhone(), "emailId", "test.user" + random.nextInt(10000) + "@example.com");

        for (String rollNumber : invalidRollNumbers) {
            HttpResponse response = sendPostRequest(data, true, rollNumber);
            int statusCode = response.getStatusLine().getStatusCode();
            assert statusCode == 400 || statusCode == 401 : "Expected 400 or 401 for invalid roll number, got " + statusCode;
        }
        System.out.println("Invalid roll number format tests passed");
    }

    private static void testBoundaryValues() {
        String longString = generateRandomString(256);
        JsonObject[] boundaryData = {
            createJsonObject("firstName", longString, "lastName", "User", "phoneNumber", generateRandomPhone(), "emailId", "test@example.com"),
            createJsonObject("firstName", "Test", "lastName", longString, "phoneNumber", generateRandomPhone(), "emailId", "test@example.com"),
            createJsonObject("firstName", "Test", "lastName", "User", "phoneNumber", 99999999999L, "emailId", "test@example.com"),
            createJsonObject("firstName", "Test", "lastName", "User", "phoneNumber", 999999999L, "emailId", "test@example.com"),
            createJsonObject("firstName", "Test", "lastName", "User", "phoneNumber", generateRandomPhone(), "emailId", longString + "@example.com")
        };

        for (JsonObject data : boundaryData) {
            HttpResponse response = sendPostRequest(data, true);
            int statusCode = response.getStatusLine().getStatusCode();
            assert statusCode == 400 || statusCode == 201 : "Expected 400 or 201 for boundary value, got " + statusCode;
        }
        System.out.println("Boundary value tests passed");
    }

    private static void testSpecialCharacters() {
        String specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?";
        JsonObject data = createJsonObject(
            "firstName", "Test" + specialChars,
            "lastName", "User" + specialChars,
            "phoneNumber", generateRandomPhone(),
            "emailId", "test" + specialChars + "@example.com"
        );
        HttpResponse response = sendPostRequest(data, true);
        System.out.println("Special characters test response: " + response.getStatusLine().getStatusCode());
        System.out.println("Special characters test completed");
    }

    private static void testRateLimiting() {
        JsonObject data = createJsonObject("firstName", "Test", "lastName", "User", "phoneNumber", generateRandomPhone(), "emailId", "test.user" + random.nextInt(10000) + "@example.com");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            HttpResponse response = sendPostRequest(data, true);
            if (response.getStatusLine().getStatusCode() == 429) {
                System.out.println("Rate limiting detected");
                return;
            }
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Sent 10 requests in " + (endTime - startTime) / 1000.0 + " seconds without rate limiting");
    }

    private static void testDifferentHttpMethods() {
        // This method is left as an exercise for the reader, as HttpClient doesn't directly support other methods on POST endpoints
        System.out.println("Different HTTP methods test skipped - implementation required");
    }

    private static HttpResponse sendPostRequest(JsonObject data, boolean includeRollNumber) {
        return sendPostRequest(data, includeRollNumber, ROLL_NUMBER);
    }

    private static HttpResponse sendPostRequest(JsonObject data, boolean includeRollNumber, String rollNumber) {
        try {
            HttpPost request = new HttpPost(API_ENDPOINT);
            StringEntity params = new StringEntity(gson.toJson(data));
            request.addHeader("content-type", "application/json");
            if (includeRollNumber) {
                request.addHeader("roll-number", rollNumber);
            }
            request.setEntity(params);
            return httpClient.execute(request);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static long generateRandomPhone() {
        return 1000000000L + random.nextInt(900000000);
    }

    private static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static JsonObject createJsonObject(Object... keyValuePairs) {
        JsonObject jsonObject = new JsonObject();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            String key = (String) keyValuePairs[i];
            Object value = keyValuePairs[i + 1];
            if (value instanceof String) {
                jsonObject.addProperty(key, (String) value);
            } else if (value instanceof Number) {
                jsonObject.addProperty(key, (Number) value);
            } else if (value instanceof Boolean) {
                jsonObject.addProperty(key, (Boolean) value);
            }
        }
        return jsonObject;
    }
}
