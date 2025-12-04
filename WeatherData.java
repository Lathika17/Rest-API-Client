package restapi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WeatherData {

	// Example coordinates for Bangalore; change latitude/longitude to get other cities
    private static final String API_URL =
            "https://api.open-meteo.com/v1/forecast?latitude=12.97&longitude=77.59&current_weather=true";

    public static void main(String[] args) {
        try {
            String json = fetch(API_URL);
            if (json == null) {
                System.out.println("No response from API.");
                return;
            }

            // Print raw JSON (helpful for debugging)
            System.out.println("Raw JSON:\n" + json + "\n");

            // Parse current_weather block manually and print values
            String cw = extractBlock(json, "\"current_weather\":", "}");
            if (cw == null || cw.isEmpty()) {
                System.out.println("Couldn't find current_weather in response.");
                return;
            }

            String temperature = extractValue(cw, "\"temperature\":", ",");
            String windspeed   = extractValue(cw, "\"windspeed\":", ",");
            String winddir     = extractValue(cw, "\"winddirection\":", ",");
            String time        = extractValue(cw, "\"time\":\"", "\"");

            System.out.println("=== CURRENT WEATHER ===");
            System.out.println("Location: Bangalore (lat 12.97, lon 77.59)");
            System.out.println("Temperature : " + safe(temperature) + " °C");
            System.out.println("Wind Speed  : " + safe(windspeed) + " km/h");
            System.out.println("Wind Dir    : " + safe(winddir) + "°");
            System.out.println("Time        : " + safe(time));

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Simple HTTP GET
    private static String fetch(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        int code = conn.getResponseCode();
        if (code != 200) {
            String err = readStream(conn.getErrorStream());
            throw new Exception("HTTP " + code + " : " + (err == null ? "no error body" : err));
        }

        return readStream(conn.getInputStream());
    }

    // Read whole stream into a string
    private static String readStream(java.io.InputStream in) throws Exception {
        if (in == null) return null;
        BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }

    // Extract a small JSON block starting at 'startToken' up to the matching closing brace for that object
    // This is tuned for simple responses like: ..."current_weather":{...},...
    private static String extractBlock(String json, String startToken, String endToken) {
        int idx = json.indexOf(startToken);
        if (idx == -1) return null;
        idx += startToken.length();
        // if next char is '{', include until corresponding '}' (handles nested braces lightly)
        int open = json.indexOf('{', idx);
        if (open == -1) return null;
        int depth = 0;
        for (int i = open; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    // return substring that contains object content including braces
                    return json.substring(open, i + 1);
                }
            }
        }
        return null;
    }

    // Extract value after keyStart until delimiter (very simple)
    // If keyStart contains a trailing quote (e.g. "\"time\":\""), this function returns the text between that and delimiter
    private static String extractValue(String block, String keyStart, String delimiter) {
        int s = block.indexOf(keyStart);
        if (s == -1) return null;
        s += keyStart.length();
        int e = block.indexOf(delimiter, s);
        if (e == -1) {
            // if delimiter not found, try until next comma or brace
            int comma = block.indexOf(',', s);
            int brace = block.indexOf('}', s);
            if (comma != -1 && (brace == -1 || comma < brace)) e = comma;
            else if (brace != -1) e = brace;
            else e = block.length();
        }
        String raw = block.substring(s, e).trim();
        // remove surrounding quotes if present
        if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() >= 2) {
            raw = raw.substring(1, raw.length() - 1);
        }
        return raw;
    }

    private static String safe(String v) {
        return (v == null || v.isEmpty()) ? "N/A" : v;
    }
}

