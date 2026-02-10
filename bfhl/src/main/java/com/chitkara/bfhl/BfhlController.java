package com.chitkara.bfhl;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;
import java.math.BigInteger;
import java.net.http.*;
import java.net.URI;

import com.fasterxml.jackson.databind.*;

@RestController
public class BfhlController {

    private static final String OFFICIAL_EMAIL = "geetanshi0265.be23@chitkara.edu.in";
    private static final String GEMINI_KEY = System.getenv("GEMINI_API_KEY");

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("is_success", true);
        res.put("official_email", OFFICIAL_EMAIL);
        return res;
    }

    @PostMapping("/bfhl")
    public ResponseEntity<?> bfhl(@RequestBody Map<String, Object> body) {

        if (body.size() != 1) {
            return error(HttpStatus.BAD_REQUEST);
        }

        String key = body.keySet().iterator().next();
        Object value = body.get(key);

        try {
            Object data;

            switch (key) {

                case "fibonacci":
                    int n = (int) value;
                    List<Integer> fib = new ArrayList<>();
                    int a = 0, b = 1;
                    for (int i = 0; i < n; i++) {
                        fib.add(a);
                        int c = a + b;
                        a = b;
                        b = c;
                    }
                    data = fib;
                    break;

                case "prime":
                    List<Integer> arr = (List<Integer>) value;
                    List<Integer> primes = new ArrayList<>();
                    for (int x : arr) {
                        if (x > 1) {
                            boolean ok = true;
                            for (int i = 2; i * i <= x; i++) {
                                if (x % i == 0) ok = false;
                            }
                            if (ok) primes.add(x);
                        }
                    }
                    data = primes;
                    break;

                case "lcm":
                    List<Integer> lcmArr = (List<Integer>) value;
                    BigInteger lcm = BigInteger.valueOf(lcmArr.get(0));
                    for (int i = 1; i < lcmArr.size(); i++) {
                        BigInteger x = BigInteger.valueOf(lcmArr.get(i));
                        lcm = lcm.multiply(x).divide(lcm.gcd(x));
                    }
                    data = lcm.intValue();
                    break;

                case "hcf":
                    List<Integer> hcfArr = (List<Integer>) value;
                    int hcf = hcfArr.get(0);
                    for (int i = 1; i < hcfArr.size(); i++) {
                        hcf = gcd(hcf, hcfArr.get(i));
                    }
                    data = hcf;
                    break;

                case "AI":
                    String question = value.toString();
                    data = askGemini(question);
                    break;

                default:
                    return error(HttpStatus.BAD_REQUEST);
            }

            return ResponseEntity.ok(success(data));

        } catch (Exception e) {
            return error(HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("is_success", true);
        res.put("official_email", OFFICIAL_EMAIL);
        res.put("data", data);
        return res;
    }


    private ResponseEntity<?> error(HttpStatus status) {
        Map<String, Object> res = new HashMap<>();
        res.put("is_success", false);
        res.put("official_email", OFFICIAL_EMAIL);
        return ResponseEntity.status(status).body(res);
    }

    private String askGemini(String question) throws Exception {

        if (GEMINI_KEY == null || GEMINI_KEY.isEmpty()) {
            return "GEMINI_KEY_NOT_SET";
        }

        HttpClient client = HttpClient.newHttpClient();

        String reqBody = """
    {
      "contents": [
        {
          "parts": [
            { "text": "%s" }
          ]
        }
      ]
    }
    """.formatted(question);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + GEMINI_KEY
                ))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(reqBody))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());

        // DEBUG SAFETY CHECK
        if (!root.has("candidates")) {
            return "NO_CANDIDATES_FROM_GEMINI";
        }

        JsonNode candidates = root.get("candidates");
        if (candidates.size() == 0) {
            return "EMPTY_CANDIDATES";
        }

        JsonNode parts = candidates.get(0)
                .path("content")
                .path("parts");

        if (parts.isArray() && parts.size() > 0) {
            return parts.get(0).path("text").asText();
        }

        return "INVALID_GEMINI_RESPONSE";
    }

}
