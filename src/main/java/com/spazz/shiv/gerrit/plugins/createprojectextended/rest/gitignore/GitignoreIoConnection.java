package com.spazz.shiv.gerrit.plugins.createprojectextended.rest.gitignore;

import com.google.gwt.http.client.Request;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by shivneil on 11/11/15.
 */
public class GitignoreIoConnection {
    private static final Logger log = LoggerFactory.getLogger(GitignoreIoConnection.class);
    private static final String GITIGNOREIO_BASEURL = "https://www.gitignore.io/" + "api/";

    private static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";
    private CloseableHttpClient client;

    public GitignoreIoConnection() {
        client = null;
    }

    private CloseableHttpClient getClient() {
        if (client == null) {
            log.trace("Creating new client connection");
            client = HttpClients.createDefault();
        }
        return client;
    }

    public String getGitIgnoreFile(List<String> templates) throws ClientProtocolException, IOException {

        StringBuilder sb = new StringBuilder(GITIGNOREIO_BASEURL);
        ListIterator<String> it = templates.listIterator();
        while(it.hasNext()) {
            String template = it.next();
            sb.append(template);
            if(it.hasNext()) {
                sb.append(',');
            }
        }

        log.info("Sending request to " + sb.toString());
        HttpGet getReq = new HttpGet(sb.toString());
        getReq.addHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_TEXT_PLAIN);
        CloseableHttpResponse response;
        log.info("Awaiting response...");
        response = getClient().execute(getReq);
        HttpEntity entity = response.getEntity();

        log.info("****************Begin GitIgnore Response*****************");
        log.info(EntityUtils.toString(entity));
        log.info("****************End GitIgnore Response*****************");

        return EntityUtils.toString(entity);
    }
}
