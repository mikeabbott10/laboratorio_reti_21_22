package client.http;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;

public class HttpRequests {

    public static String get(String uri, String username, String password) throws InterruptedException, IOException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .headers("username", username, "password", password)
                .uri(URI.create(uri))
                .GET()
                .build();
    
        HttpResponse<String> response =
              client.send(request, BodyHandlers.ofString());
    
        return response.body();
    }

    public static String delete(String uri, String username, String password) throws InterruptedException, IOException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .headers("username", username, "password", password)
                .uri(URI.create(uri))
                .DELETE()
                .build();
    
        HttpResponse<String> response =
              client.send(request, BodyHandlers.ofString());
    
        return response.body();
    }

    public static String put(String url, String parameters, String username, String password) throws IOException {
        StringBuilder content;
        byte[] putData = null;
        if(parameters!=null) 
            putData = parameters.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection con = null;
        try {
            var myurl = new URL(url);
            con = (HttpURLConnection) myurl.openConnection();

            con.setDoOutput(true);
            con.setRequestMethod("PUT");
            con.setRequestProperty("User-Agent", "Java client");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setRequestProperty("username", username);
            con.setRequestProperty("password", password);

            if(parameters!=null){
                try (var wr = new DataOutputStream(con.getOutputStream())) {
                    wr.write(putData);
                }
            }

            try (var br = new BufferedReader(
                    new InputStreamReader(con.getInputStream()))) {
                String line;
                content = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    content.append(line);
                    content.append(System.lineSeparator());
                }
            }
            //System.out.println(content.toString());

        } finally {
            con.disconnect();
        }
        return content.toString();
    }

    public static String post(String url, String parameters, String username, String password) throws IOException {
        StringBuilder content;
        byte[] postData = parameters.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection con = null;
        try {

            var myurl = new URL(url);
            con = (HttpURLConnection) myurl.openConnection();

            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "Java client");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setRequestProperty("username", username);
            con.setRequestProperty("password", password);

            try (var wr = new DataOutputStream(con.getOutputStream())) {
                wr.write(postData);
            }

            try (var br = new BufferedReader(
                    new InputStreamReader(con.getInputStream()))) {

                String line;
                content = new StringBuilder();

                while ((line = br.readLine()) != null) {
                    content.append(line);
                    content.append(System.lineSeparator());
                }
            }

        } finally {
            con.disconnect();
        }
        return content.toString();

    }
}
