package com.example.client;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class WebServer {
    private static final String SERVER_URL = "http://localhost:8080";
    private static final Gson gson = new Gson();
    
    public static void main(String[] args) throws Exception {
        Server server = new Server(8081);
        
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        
        // Static resources
        context.addServlet(new ServletHolder(new StaticResourceServlet()), "/assets/*");
        context.addServlet(new ServletHolder(new StaticResourceServlet()), "/*");
        
        // API endpoints
        context.addServlet(new ServletHolder(new DeviceListServlet()), "/api/devices");
        context.addServlet(new ServletHolder(new SendNotificationServlet()), "/api/send");
        
        server.setHandler(context);
        server.start();
        
        System.out.println("Web server started at http://localhost:8081");
        System.out.println("Open your browser and go to: http://localhost:8081");
        
        server.join();
    }
    
    public static class DeviceDto {
        public int id;
        public String token;
        public String label;
        
        public DeviceDto(int id, String token, String label) {
            this.id = id;
            this.token = token;
            this.label = label;
        }
    }
    
    public static class StaticResourceServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
                throws ServletException, IOException {
            String path = req.getRequestURI(); // includes leading '/'
            if (path == null || "/".equals(path)) {
                path = "/index.html";
            }

            // Serve static files from resources (prefix resource base)
            InputStream is = WebServer.class.getClassLoader().getResourceAsStream("webapp" + path);
            if (is == null) {
                // Fallback try with leading slash
                is = WebServer.class.getResourceAsStream("/webapp" + path);
            }
            try (InputStream closeable = is) {
                if (is == null) {
                    resp.setStatus(404);
                    return;
                }
                
                // Set content type
                if (path.endsWith(".html")) {
                    resp.setContentType("text/html; charset=UTF-8");
                } else if (path.endsWith(".css")) {
                    resp.setContentType("text/css");
                } else if (path.endsWith(".js")) {
                    resp.setContentType("application/javascript");
                } else if (path.endsWith(".png")) {
                    resp.setContentType("image/png");
                } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
                    resp.setContentType("image/jpeg");
                } else if (path.endsWith(".woff")) {
                    resp.setContentType("font/woff");
                } else if (path.endsWith(".woff2")) {
                    resp.setContentType("font/woff2");
                } else if (path.endsWith(".ttf")) {
                    resp.setContentType("font/ttf");
                } else if (path.endsWith(".eot")) {
                    resp.setContentType("application/vnd.ms-fontobject");
                }
                
                // Copy stream
                try (OutputStream os = resp.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                }
            }
        }
    }
    
    public static class DeviceListServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
                throws ServletException, IOException {
            resp.setContentType("application/json; charset=UTF-8");
            
            try {
                String response = get(SERVER_URL + "/devices");
                // Parse the "statusCode:body" format and return just the JSON body
                if (response.contains(":")) {
                    String[] parts = response.split(":", 2);
                    int statusCode = Integer.parseInt(parts[0]);
                    String body = parts[1];
                    if (statusCode == 200) {
                        resp.getWriter().write(body);
                    } else {
                        resp.setStatus(statusCode);
                        resp.getWriter().write("{\"error\":\"Server error: " + body + "\"}");
                    }
                } else {
                    resp.getWriter().write(response);
                }
            } catch (Exception e) {
                resp.setStatus(500);
                resp.getWriter().write("{\"error\":\"Failed to load devices: " + e.getMessage() + "\"}");
            }
        }
    }
    
    public static class SendNotificationServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
                throws ServletException, IOException {
            resp.setContentType("application/json; charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");
            
            try {
                String title = req.getParameter("title");
                String body = req.getParameter("body");
                String[] ids = req.getParameterValues("ids[]");
                
                // Debug logging
                System.out.println("=== SendNotificationServlet Debug ===");
                System.out.println("Title: " + title);
                System.out.println("Body: " + body);
                System.out.println("IDs parameter: " + java.util.Arrays.toString(ids));
                
                // Try alternative parameter names
                if (ids == null || ids.length == 0) {
                    ids = req.getParameterValues("ids");
                    System.out.println("IDs (no brackets): " + java.util.Arrays.toString(ids));
                }
                
                if (ids == null || ids.length == 0) {
                    resp.setStatus(400);
                    resp.getWriter().write("{\"error\":\"No device IDs provided\"}");
                    return;
                }
                
                // Build form data
                StringBuilder formData = new StringBuilder();
                formData.append("title=").append(URLEncoder.encode(title != null ? title : "", StandardCharsets.UTF_8));
                formData.append("&body=").append(URLEncoder.encode(body != null ? body : "", StandardCharsets.UTF_8));
                for (int i = 0; i < ids.length; i++) {
                    formData.append("&ids[]=").append(URLEncoder.encode(ids[i], StandardCharsets.UTF_8));
                }
                
                // Call server directly and get proper response
                HttpURLConnection conn = (HttpURLConnection) new URL(SERVER_URL + "/send-selected").openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                
                byte[] bytes = formData.toString().getBytes(StandardCharsets.UTF_8);
                conn.setFixedLengthStreamingMode(bytes.length);
                
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(bytes);
                }
                
                int statusCode = conn.getResponseCode();
                String responseBody;
                try (InputStream is = conn.getInputStream()) {
                    responseBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    try (InputStream es = conn.getErrorStream()) {
                        responseBody = es != null ? new String(es.readAllBytes(), StandardCharsets.UTF_8) : e.getMessage();
                    }
                }
                
                if (statusCode == 200) {
                    // Check if responseBody is wrapped in another JSON object
                    if (responseBody.trim().startsWith("{\"result\":")) {
                        // Extract the inner JSON from {"result":"200:{...}"}
                        try {
                            com.google.gson.JsonObject outerJson = gson.fromJson(responseBody, com.google.gson.JsonObject.class);
                            String innerResult = outerJson.get("result").getAsString();
                            if (innerResult.contains(":")) {
                                String[] innerParts = innerResult.split(":", 2);
                                int innerStatus = Integer.parseInt(innerParts[0]);
                                String innerBody = innerParts[1];
                                if (innerStatus == 200) {
                                    // Parse the inner body as JSON and create proper response
                                    try {
                                        com.google.gson.JsonObject detailsJson = gson.fromJson(innerBody, com.google.gson.JsonObject.class);
                                        String finalResponse = "{\"success\":true,\"message\":\"Notification sent successfully\",\"details\":" + detailsJson.toString() + "}";
                                        System.out.println("Sending response: " + finalResponse);
                                        resp.getWriter().write(finalResponse);
                                    } catch (Exception e) {
                                        String finalResponse = "{\"success\":true,\"message\":\"Notification sent successfully\",\"details\":\"" + innerBody + "\"}";
                                        System.out.println("Sending response (fallback): " + finalResponse);
                                        resp.getWriter().write(finalResponse);
                                    }
                                } else {
                                    resp.getWriter().write("{\"error\":\"FCM error: " + innerBody + "\"}");
                                }
                            } else {
                                resp.getWriter().write("{\"success\":true,\"message\":\"Notification sent successfully\",\"details\":\"" + innerResult + "\"}");
                            }
                        } catch (Exception e) {
                            resp.getWriter().write("{\"success\":true,\"message\":\"Notification sent successfully\",\"details\":\"" + responseBody + "\"}");
                        }
                    } else if (responseBody.trim().startsWith("{")) {
                        resp.getWriter().write(responseBody);
                    } else {
                        // Wrap in success response
                        resp.getWriter().write("{\"success\":true,\"message\":\"Notification sent successfully\",\"details\":\"" + responseBody + "\"}");
                    }
                } else {
                    resp.setStatus(statusCode);
                    resp.getWriter().write("{\"error\":\"Server error: " + responseBody + "\"}");
                }
            } catch (Exception e) {
                resp.setStatus(500);
                resp.getWriter().write("{\"error\":\"Failed to send notification: " + e.getMessage() + "\"}");
            }
        }
    }
    
    private static String get(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        
        int code = conn.getResponseCode();
        String response;
        try (InputStream is = conn.getInputStream()) {
            response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            try (InputStream es = conn.getErrorStream()) {
                response = es != null ? new String(es.readAllBytes(), StandardCharsets.UTF_8) : e.getMessage();
            }
        }
        return code + ":" + response;
    }
    
    private static String post(String url, String formData) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        
        byte[] bytes = formData.getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(bytes.length);
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(bytes);
        }
        
        int code = conn.getResponseCode();
        String response;
        try (InputStream is = conn.getInputStream()) {
            response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            try (InputStream es = conn.getErrorStream()) {
                response = es != null ? new String(es.readAllBytes(), StandardCharsets.UTF_8) : e.getMessage();
            }
        }
        return code + ":" + response;
    }
}
