package com.julianw03.rcls.service.base.riotclient.ssl;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.InetAddress;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class RiotSSLContext {
    private static final String PRINCIPAL_CLIENT = "CN=rclient";

    public static SSLContext create() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(
                null,
                new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                                throw new CertificateException("Untrusted client certificate");
                            }

                            @Override
                            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                                if (chain != null && chain.length > 0) {
                                    String clientHost = chain[0].getSubjectX500Principal().getName();
                                    if (isLoopbackAddress(clientHost) || PRINCIPAL_CLIENT.equals(clientHost)) {
                                        return;
                                    }
                                }
                                throw new CertificateException("Untrusted server certificate");
                            }

                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }
                        }
                },
                new SecureRandom()
        );
        return context;
    }

    private static boolean isLoopbackAddress(String host) {
        try {
            InetAddress address = InetAddress.getByName(host);
            return address.isLoopbackAddress();
        } catch (Exception e) {
            return false;
        }
    }
}
