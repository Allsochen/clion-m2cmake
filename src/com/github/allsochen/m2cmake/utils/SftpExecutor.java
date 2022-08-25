//package com.github.allsochen.m2cmake.utils;
//
//import org.apache.sshd.client.SshClient;
//import org.apache.sshd.client.session.ClientSession;
//import org.apache.sshd.sftp.client.SftpClient;
//import org.apache.sshd.sftp.client.SftpClientFactory;
//
//import java.io.IOException;
//
//public class SftpExecutor implements AutoCloseable {
//
//    private SftpConfig sftpConfig;
//    private SshClient sshClient;
//    private ClientSession clientSession;
//    private SftpClient sftpClient;
//
//    SftpExecutor(String host, int port, String user, String password) {
//        this.sftpConfig = new SftpConfig(host, port, user, password, 5000);
//        this.sshClient = SshClient.setUpDefaultClient();
//        this.sshClient.start();
//    }
//
//    private ClientSession createSession() throws IOException {
//        ClientSession session = sshClient.connect(sftpConfig.getUser(), sftpConfig.getHost(), sftpConfig.getPort())
//                .verify().getSession();
//        session.addPasswordIdentity(sftpConfig.getPassword());
//        session.auth().verify(sftpConfig.getTimeout());
//        return session;
//    }
//
//    public SftpClient createSftpClient() throws IOException {
//        if (this.sftpClient != null) {
//            return this.sftpClient;
//        }
//        this.clientSession = createSession();
//        SftpClientFactory factory = SftpClientFactory.instance();
//        this.sftpClient = factory.createSftpClient(this.clientSession);
//        return this.sftpClient;
//    }
//
//    /**
//     * 使用完成后必须执行close函数，否则会产生内存泄露
//     *
//     * @throws IOException
//     */
//    @Override
//    public void close() throws Exception {
//        sftpClient.close();
//        clientSession.close();
//        sshClient.stop();
//    }
//
//    public static class SftpConfig {
//        private String host;
//        private int port;
//        private String user;
//        private String password;
//
//        private int timeout;
//
//        public SftpConfig(String host, int port, String user, String password) {
//            this.host = host;
//            this.port = port;
//            this.user = user;
//            this.password = password;
//        }
//
//        public SftpConfig(String host, int port, String user, String password, int timeout) {
//            this(host, port, user, password);
//            this.timeout = timeout;
//        }
//
//
//        public String getHost() {
//            return host;
//        }
//
//        public void setHost(String host) {
//            this.host = host;
//        }
//
//        public int getPort() {
//            return port;
//        }
//
//        public void setPort(int port) {
//            this.port = port;
//        }
//
//        public String getUser() {
//            return user;
//        }
//
//        public void setUser(String user) {
//            this.user = user;
//        }
//
//        public String getPassword() {
//            return password;
//        }
//
//        public void setPassword(String password) {
//            this.password = password;
//        }
//
//        public int getTimeout() {
//            return timeout;
//        }
//
//        public void setTimeout(int timeout) {
//            this.timeout = timeout;
//        }
//    }
//}
