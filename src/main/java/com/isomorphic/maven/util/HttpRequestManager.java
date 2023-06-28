package com.isomorphic.maven.util;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Proxy;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * A trivial convenience class, useful for login / logout operations on SmartClient and Reify 
 * sites.  Includes support for proxy servers.
 */
public class HttpRequestManager {


    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestManager.class);

    private UsernamePasswordCredentials credentials;
    private HttpHost host;

    private String loginUrl = "/devlogin/login.jsp";
    private String logoutUrl = "/logout.jsp";

    private DefaultHttpClient httpClient = new DefaultHttpClient();

    /**
     * Constructor taking the host, login credentials, and any proxy needed to reach the given host.
     *
     * @param host An HttpHost representing the target site.
     * @param credentials The credentials needed for authentication on smartclient.com.
     * @param proxyConfiguration A Proxy configuration that can be used to set up the httpClient used during all communication to the server by this object
     * @throws MojoExecutionException if there is any error during the attempt to detect and set up any proxy
     */
    public HttpRequestManager(HttpHost host, UsernamePasswordCredentials credentials, Proxy proxyConfiguration) throws MojoExecutionException {
        this.host = host;
        this.credentials = credentials;

        try {
            if (proxyConfiguration != null && isProxied(proxyConfiguration) ) {
                if (proxyConfiguration.getUsername() != null) {
                    httpClient.getCredentialsProvider().setCredentials(
                        new AuthScope(proxyConfiguration.getHost(), proxyConfiguration.getPort()),
                        new UsernamePasswordCredentials(proxyConfiguration.getUsername(), proxyConfiguration.getPassword()));
                }
                HttpHost proxy = new HttpHost(proxyConfiguration.getHost(), proxyConfiguration.getPort());
                httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
            }
        } catch (Exception e) {
            throw new MojoExecutionException ("Unable to setup HTTP proxy", e);
        }
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }
    public void setLogoutUrl(String logoutUrl) {
        this.logoutUrl = logoutUrl;
    }
    public String getHostName() {
        return host.getHostName();
    }

    /**
     * If {@link #credentials} have been supplied, uses them to authenticate to the isomorphic web site,
     * allowing download of protected resources.
     *
     * @throws MojoExecutionException on any error during login
     */
    public void login() throws MojoExecutionException {

        if (credentials == null) {
            return;
        }

        String username = credentials.getUserName();
        String password = credentials.getPassword();

        LOGGER.debug("Authenticating to '{}' with username: '{}'", host.getHostName() + loginUrl, username);

        HttpPost login = new HttpPost(loginUrl);

        List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        nvps.add(new BasicNameValuePair("USERNAME", username));
        nvps.add(new BasicNameValuePair("PASSWORD", password));

        try {

            login.setEntity(new UrlEncodedFormEntity(nvps));
            HttpResponse response = httpClient.execute(host, login);
            EntityUtils.consume(response.getEntity());

        } catch (IOException e) {
            throw new MojoExecutionException("Error during POST request for authentication", e);
        }
    }

    /**
     * Logs off at smartclient.com.
     */
    public void logout() {
        HttpPost logout = new HttpPost(logoutUrl);
        LOGGER.debug("Logging off at '{}'", host.getHostName() + logoutUrl);
        try {
            HttpResponse response = httpClient.execute(host, logout);
            EntityUtils.consume(response.getEntity());
        } catch (Exception e) {
            LOGGER.debug("Error at logout ", e);
        }
    }

    /**
     * Adapted from the Site plugin's AbstractDeployMojo to allow http operations through proxy.
     * <p>
     * Refer to
     * http://maven.apache.org/guides/mini/guide-proxies.html
     * <br>
     * http://maven.apache.org/plugins/maven-site-plugin/xref/org/apache/maven/plugins/site/AbstractDeployMojo.html.
     */
    private boolean isProxied(Proxy proxyConfig) throws MalformedURLException {
        String nonProxyHostsAsString = proxyConfig.getNonProxyHosts();
        String domain = host.getHostName();

        for (String nonProxyHost : StringUtils.split(nonProxyHostsAsString, ",;|")) {

            if (StringUtils.contains(nonProxyHost, "*")) {

                // Handle wildcard at the end, beginning or middle of the nonProxyHost
                final int pos = nonProxyHost.indexOf('*');
                String nonProxyHostPrefix = nonProxyHost.substring(0, pos);
                String nonProxyHostSuffix = nonProxyHost.substring(pos + 1);

                // prefix*
                if (StringUtils.isNotEmpty(nonProxyHostPrefix)
                        && domain.startsWith(nonProxyHostPrefix)
                        && StringUtils.isEmpty(nonProxyHostSuffix)) {

                    return false;
                }

                // *suffix
                if (StringUtils.isEmpty(nonProxyHostPrefix)
                        && StringUtils.isNotEmpty(nonProxyHostSuffix)
                        && domain.endsWith(nonProxyHostSuffix)) {

                    return false;
                }

                // prefix*suffix
                if (StringUtils.isNotEmpty(nonProxyHostPrefix)
                        && domain.startsWith(nonProxyHostPrefix)
                        && StringUtils.isNotEmpty(nonProxyHostSuffix)
                        && domain.endsWith(nonProxyHostSuffix)) {

                    return false;
                }

            } else if (domain.equals(nonProxyHost)) {
                return false;
            }
        }
        return true;
    }

    public HttpResponse execute(HttpRequestBase request) {
        try {
            return httpClient.execute(host, request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

