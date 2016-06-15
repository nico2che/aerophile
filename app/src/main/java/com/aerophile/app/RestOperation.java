package com.aerophile.app;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Created by Nicolas on 17/12/2015.
 */
public class RestOperation {

    public static final String URL = "http://chevigne.fr/films/";

    private String parametres;
    private String reponse;

    public RestOperation(String chemin, String params) throws IOException {
        parametres = params;
        InputStream stream = null;
        try {
            stream = downloadUrl(URL + chemin);
            reponse = readIt(stream);
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    public String getReponse() {
        return reponse;
    }

    private InputStream downloadUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000);
        conn.setConnectTimeout(15000);
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(parametres);
        writer.flush();
        writer.close();
        conn.connect();
        InputStream stream = conn.getInputStream();
        return stream;
    }

    private String readIt(InputStream stream) throws IOException {
        String line, buffer = "";
        BufferedReader br = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        while ((line = br.readLine()) != null) {
            buffer += line;
        }
        return new String(buffer);
    }
}