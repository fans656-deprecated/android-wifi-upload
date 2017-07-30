package com.example.fans6.myapplication;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.logging.Logger;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;

import static java.lang.Integer.parseInt;

public class HelloServer extends NanoHTTPD {

    private String text = "default";

    /**
     * logger to log to.
     */
    private static final Logger LOG = Logger.getLogger(HelloServer.class.getName());

    public HelloServer() {
        super(8080);
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String s = session.getMethod() + " " + session.getUri();
        String method = session.getMethod().toString();
        if (method.equals("POST")) {
            if (session.getHeaders().containsKey("content-length")) {
                int contentLength = parseInt(session.getHeaders().get("content-length"));
                String[] parts = session.getUri().split("/");
                String fname = parts[parts.length - 1];
                File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), fname);
                try {
                    InputStream inputStream = session.getInputStream();
                    OutputStream outputStream = new FileOutputStream(file);
                    FileWriter writer = new FileWriter(file);
                    byte[] buf = new byte[1024];
                    int nRemained = contentLength;
                    int nRead = 0;
                    while (nRemained > 0) {
                        int nCurRead = inputStream.read(buf, 0, Math.min(nRemained, 1024));
                        nRead += nCurRead;
                        nRemained -= nCurRead;
                        outputStream.write(buf, 0, nCurRead);
                    }
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return Response.newFixedLengthResponse(s);
        } else if (method.equals("GET")) {
            String html = "<html>"
                    + "<script>"
                    + "function doUpload(target) {"
                    + "  var file = target.files[0];"
                    + "  var fpath = file.name;"
                    + "  var fnameParts = fpath.split('/');"
                    + "  var fname = fnameParts[fnameParts.length - 1];"
                    + "  var url = '/' + fname;"
                    + "  var xhr = new XMLHttpRequest();"
                    + "  xhr.upload.onprogress = function(ev) {"
                    + "    console.log(ev);"
                    + "  };"
                    + "  xhr.onload = function(ev) {"
                    + "    console.log('uploaded');"
                    + "  };"
                    + "  xhr.open('POST', url, true);"
                    + "  xhr.send(file);"
                    + "}"
                    + "</script>"
                    + "<body>"
                    + "<input type=\"file\" text=\"Upload\" id=\"fileInput\" onchange=\"doUpload(this)\"/>"
                    + "</body></html>";
            return Response.newFixedLengthResponse(html);
        }
        return Response.newFixedLengthResponse(s);
//        Method method = session.getMethod();
//        String uri = session.getUri();
//        HelloServer.LOG.info(method + " '" + uri + "' ");
//
//        String msg = "<html><body><h1>Hello server</h1>\n";
//        Map<String, String> parms = session.getParms();
//        if (parms.get("username") == null) {
//            msg += "<form action='?' method='get'>\n" + "  <p>Your name: <input type='text' name='username'></p>\n" + "</form>\n";
//        } else {
//            msg += "<p>Hello, " + parms.get("username") + "!</p>";
//        }
//
//        msg += "</body></html>\n";
//
//        return Response.newFixedLengthResponse(msg);
    }
}
