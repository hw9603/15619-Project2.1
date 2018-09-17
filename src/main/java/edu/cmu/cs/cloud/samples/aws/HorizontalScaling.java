package edu.cmu.cs.cloud.samples.aws;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.amazonaws.services.ec2.model.Instance;

/**
 * Main routine for horizontal scaling task.
 */
public class HorizontalScaling {

    /**
     * Main function.
     */
    public static void main(String[] args) throws IOException {
        CreateSecurityGroup createSecurityGroup = new CreateSecurityGroup("horizontalScaling");
        createSecurityGroup.createGroup();

        LaunchEC2Instance launchLG = new LaunchEC2Instance(
            "ami-013666ca8430e3646", "m3.medium", "horizontalScaling");
        Instance lgInstance;
        String lgDns = new String();
        try {
            lgInstance = launchLG.runInstance();
            lgDns = lgInstance.getPublicDnsName();
            System.out.println(lgInstance.getPublicDnsName());
            String url = "http://" + lgDns + "/password?passwd=DJ0nXHmZxJuYcAqbeAZTM2&username=wenhe@andrew.cmu.edu";
            System.out.println(url);
            buildConnection(url);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        LaunchEC2Instance launchWeb = new LaunchEC2Instance(
            "ami-01b6328e1cc04b493", "m3.medium", "horizontalScaling");
        Instance webInstance;
        String html = "";
        long startTime = System.currentTimeMillis();
        try {
            startTime = System.currentTimeMillis();
            webInstance = launchWeb.runInstance();
            String webDns = webInstance.getPublicDnsName();
            System.out.println(webInstance.getPublicDnsName());

            String url = "http://" + lgDns + "/test/horizontal?dns=" + webDns;
            InputStream inputStream = buildConnection(url);

            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                html += inputLine;
            }
            in.close();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.out.println(html);
        String path = html.substring(html.indexOf("<a href='") + 9, html.indexOf("'>Test</a>"));
        String testUrl = "http://" + lgDns + path;
        float rps = 0;

        while (rps < 60) {
            InputStream log = buildConnection(testUrl);
            rps = retrieveRPS(log);
            long endTime = System.currentTimeMillis();
            if (endTime - startTime > 100000) {
                System.out.println(rps);
                LaunchEC2Instance launchNewInstance = new LaunchEC2Instance(
                    "ami-01b6328e1cc04b493", "m3.medium", "horizontalScaling");
                try {
                    startTime = System.currentTimeMillis();
                    Instance newInstance = launchNewInstance.runInstance();
                    String newDns = newInstance.getPublicDnsName();
                    String url = "http://" + lgDns + "/test/horizontal/add?dns=" + newDns;
                    buildConnection(url);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Build connection given the URL, return an instance of inputstream.
     */
    public static InputStream buildConnection(String urlString) throws IOException {
        URL url = new URL(urlString);
        int statusCode = 400;
        HttpURLConnection conn = null;
        do {
            try {
                conn = (HttpURLConnection)url.openConnection();
                statusCode = conn.getResponseCode();
            } catch (Exception e) {
                // System.err.println("Error: Connection failed!");
            }
        } while (statusCode != 200);
        return conn.getInputStream();
    }

    /**
     * Retrieve the latest RPS score.
     */
    public static float retrieveRPS(InputStream inputStream) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        String inputLine;
        float latestRPS = 0;
        while ((inputLine = in.readLine()) != null) {
            if (inputLine.startsWith("[Current rps=")) {
                latestRPS = Float.parseFloat(inputLine.substring(13, inputLine.length() - 2));
            }
            if (inputLine.equals("[Load Generator]")) {
                break;
            }
        }
        in.close();
        return latestRPS;
    }
}
