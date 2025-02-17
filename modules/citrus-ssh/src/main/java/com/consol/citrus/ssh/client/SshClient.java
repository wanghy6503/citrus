/*
 * Copyright 2006-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.consol.citrus.ssh.client;

import com.consol.citrus.endpoint.AbstractEndpoint;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.messaging.*;
import com.consol.citrus.ssh.SshRequest;
import com.consol.citrus.ssh.SshResponse;
import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Ssh client connects to ssh server and sends commands to that server.
 *
 * @author Roland Huss, Christoph Deppisch
 * @since 1.4
 */
public class SshClient extends AbstractEndpoint implements Producer, ReplyConsumer {

    public static final String CLASSPATH_PREFIX = "classpath:";

    /** Store of reply messages */
    private Map<String, Message<?>> replyMessages = new HashMap<String, Message<?>>();

    /** Retry logger */
    private static final Logger RETRY_LOG = LoggerFactory.getLogger("com.consol.citrus.MessageRetryLogger");

    // Session for the SSH communication
    private Session session;

    // SSH implementation
    private JSch jsch = new JSch();

    /**
     * Default constructor initializing endpoint configuration.
     */
    public SshClient() {
        super(new SshEndpointConfiguration());
    }

    /**
     * Default constructor using endpoint configuration.
     * @param endpointConfiguration
     */
    protected SshClient(SshEndpointConfiguration endpointConfiguration) {
        super(endpointConfiguration);
    }

    @Override
    public SshEndpointConfiguration getEndpointConfiguration() {
        return (SshEndpointConfiguration) super.getEndpointConfiguration();
    }

    /**
     * Send a message as SSH request. The message format is created from
     * {@link com.consol.citrus.ssh.server.SshServer}.
     *
     * @param message the message object to send.
     */
    public void send(Message<?> message) {
        String payload = (String) message.getPayload();
        SshRequest request = (SshRequest) getEndpointConfiguration().getXmlMapper().fromXML(payload);

        if (getEndpointConfiguration().isStrictHostChecking()) {
            setKnownHosts();
        }

        String rUser = getRemoteUser(message);
        connect(rUser);
        ChannelExec channelExec = null;
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errStream = new ByteArrayOutputStream();
        int rc = 0;
        try {
            channelExec = openChannelExec();
            channelExec.setErrStream(errStream);
            channelExec.setOutputStream(outStream);
            channelExec.setCommand(request.getCommand());
            doConnect(channelExec);
            if (request.getStdin() != null) {
                sendStandardInput(channelExec, request.getStdin());
            }
            waitCommandToFinish(channelExec);
            rc = channelExec.getExitStatus();
        } finally {
            if (channelExec != null && channelExec.isConnected()) {
                channelExec.disconnect();
            }
            disconnect();
        }
        SshResponse sshResp = new SshResponse(outStream.toString(),errStream.toString(),rc);
        Message<String> response = MessageBuilder.withPayload(getEndpointConfiguration().getXmlMapper().toXML(sshResp))
                .setHeader("user", rUser).build();
        onReplyMessage(message, response);
    }

    @Override
    public Message<?> receive() {
        return receive("", getEndpointConfiguration().getTimeout());
    }

    @Override
    public Message<?> receive(String selector) {
        return receive(selector, getEndpointConfiguration().getTimeout());
    }

    @Override
    public Message<?> receive(long timeout) {
        return receive("", timeout);
    }

    @Override
    public Message<?> receive(String selector, long timeout) {
        long timeLeft = timeout;
        Message<?> message = findReplyMessage(selector);

        while (message == null && timeLeft > 0) {
            timeLeft -= getEndpointConfiguration().getPollingInterval();

            if (RETRY_LOG.isDebugEnabled()) {
                RETRY_LOG.debug("Reply message did not arrive yet - retrying in " + (timeLeft > 0 ? getEndpointConfiguration().getPollingInterval() : getEndpointConfiguration().getPollingInterval() + timeLeft) + "ms");
            }

            try {
                Thread.sleep(timeLeft > 0 ? getEndpointConfiguration().getPollingInterval() : getEndpointConfiguration().getPollingInterval() + timeLeft);
            } catch (InterruptedException e) {
                RETRY_LOG.warn("Thread interrupted while waiting for retry", e);
            }

            message = findReplyMessage(selector);
        }

        return message;
    }

    /**
     * Saves reply message with correlation key to local store for later processing.
     * @param correlationKey
     * @param replyMessage the reply message.
     */
    public void onReplyMessage(String correlationKey, Message<?> replyMessage) {
        replyMessages.put(correlationKey, replyMessage);
    }

    /**
     * Saves reply message to local store for later processing. Constructs correlation key from initial request.
     * @param requestMessage
     * @param replyMessage
     */
    public void onReplyMessage(Message<?> requestMessage, Message<?> replyMessage) {
        if (getEndpointConfiguration().getCorrelator() != null) {
            onReplyMessage(getEndpointConfiguration().getCorrelator().getCorrelationKey(requestMessage), replyMessage);
        } else {
            onReplyMessage("", replyMessage);
        }
    }

    /**
     * Tries to find reply message for correlation key from local store.
     * @param correlationKey
     * @return
     */
    public Message<?> findReplyMessage(String correlationKey) {
        return replyMessages.remove(correlationKey);
    }

    @Override
    public Producer createProducer() {
        return this;
    }

    @Override
    public SelectiveConsumer createConsumer() {
        return this;
    }

    private void connect(String rUser) {
        if (session == null || !session.isConnected()) {
            try {
                if (StringUtils.hasText(getEndpointConfiguration().getPrivateKeyPath())) {
                    jsch.addIdentity(getPrivateKeyPath(), getEndpointConfiguration().getPrivateKeyPassword());
                }
            } catch (JSchException e) {
                throw new CitrusRuntimeException("Cannot add private key " + getEndpointConfiguration().getPrivateKeyPath() + ": " + e,e);
            } catch (IOException e) {
                throw new CitrusRuntimeException("Cannot open private key file " + getEndpointConfiguration().getPrivateKeyPath() + ": " + e,e);
            }
            try {
                session = jsch.getSession(rUser, getEndpointConfiguration().getHost(), getEndpointConfiguration().getPort());
                if (StringUtils.hasText(getEndpointConfiguration().getPassword())) {
                    session.setUserInfo(new UserInfoWithPlainPassword(getEndpointConfiguration().getPassword()));
                    session.setPassword(getEndpointConfiguration().getPassword());
                }
                session.setConfig("StrictHostKeyChecking", getEndpointConfiguration().isStrictHostChecking() ? "yes" : "no");
                session.connect();
            } catch (JSchException e) {
                throw new CitrusRuntimeException("Cannot connect via SSH: " + e,e);
            }
        }
    }

    private void disconnect() {
        if (session.isConnected()) {
            session.disconnect();
        }
    }

    private ChannelExec openChannelExec() throws CitrusRuntimeException {
        ChannelExec channelExec;
        try {
            channelExec = (ChannelExec) session.openChannel("exec");
        } catch (JSchException e) {
            throw new CitrusRuntimeException("Cannot open EXEC SSH channel: " + e,e);
        }
        return channelExec;
    }

    private void waitCommandToFinish(ChannelExec pCh) {
        final long until = System.currentTimeMillis() + getEndpointConfiguration().getCommandTimeout();

        try {
            while (!pCh.isClosed() && System.currentTimeMillis() < until) {
                Thread.sleep(250);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted", e);
        }

        if (!pCh.isClosed()) {
            throw new CitrusRuntimeException("Timeout: Channel not finished within " + getEndpointConfiguration().getCommandTimeout() + " ms");
        }
    }

    private void sendStandardInput(ChannelExec pCh, String pInput) {
        OutputStream os = null;
        try {
            os = pCh.getOutputStream();
            os.write(pInput.getBytes());
        } catch (IOException e) {
            throw new CitrusRuntimeException("Cannot write to standard input of SSH channel: " + e,e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    // best try
                }
            }
        }
    }

    private void doConnect(ChannelExec pCh) {
        try {
            if (getEndpointConfiguration().getConnectionTimeout() != 0) {
                pCh.connect(getEndpointConfiguration().getConnectionTimeout());
            } else {
                pCh.connect();
            }
        } catch (JSchException e) {
            throw new CitrusRuntimeException("Cannot connect EXEC SSH channel: " + e,e);
        }
    }

    private String getRemoteUser(Message<?> message) {
        String rUser = (String) message.getHeaders().get("user");
        if (rUser == null) {
            // Use default uses
            rUser = getEndpointConfiguration().getUser();
        }
        if (rUser == null) {
            throw new CitrusRuntimeException("No user given for connecting to SSH server");
        }
        return rUser;
    }

    private void setKnownHosts() {
        if (getEndpointConfiguration().getKnownHosts() == null) {
            throw new CitrusRuntimeException("Strict host checking is enabled but no knownHosts given");
        }
        try {
            InputStream khIs = getInputStreamFromPath(getEndpointConfiguration().getKnownHosts());
            if (khIs == null) {
                throw new CitrusRuntimeException("Cannot find knownHosts at " + getEndpointConfiguration().getKnownHosts());
            }
            jsch.setKnownHosts(khIs);
        } catch (JSchException e) {
            throw new CitrusRuntimeException("Cannot add known hosts from " + getEndpointConfiguration().getKnownHosts() + ": " + e,e);
        } catch (FileNotFoundException e) {
            throw new CitrusRuntimeException("Cannot find known hosts file " + getEndpointConfiguration().getKnownHosts() + ": " + e,e);
        }
    }

    private InputStream getInputStreamFromPath(String pPath) throws FileNotFoundException {
        if (pPath.startsWith(CLASSPATH_PREFIX)) {
            return getClass().getClassLoader().getResourceAsStream(pPath.substring(CLASSPATH_PREFIX.length()));
        } else {
            return new FileInputStream(pPath);
        }
    }

    private String getPrivateKeyPath() throws IOException {
        if (!StringUtils.hasText(getEndpointConfiguration().getPrivateKeyPath())) {
            return null;
        } else if (getEndpointConfiguration().getPrivateKeyPath().startsWith(CLASSPATH_PREFIX)) {
            File priv = File.createTempFile("citrus-ssh-test","priv");
            InputStream is = getClass().getClassLoader().getResourceAsStream(getEndpointConfiguration().getPrivateKeyPath().substring(CLASSPATH_PREFIX.length()));
            if (is == null) {
                throw new CitrusRuntimeException("No private key found at " + getEndpointConfiguration().getPrivateKeyPath());
            }
            FileCopyUtils.copy(is, new FileOutputStream(priv));
            return priv.getAbsolutePath();
        } else {
            return getEndpointConfiguration().getPrivateKeyPath();
        }
    }

    // UserInfo which simply returns a plain password
    private static class UserInfoWithPlainPassword implements UserInfo {
        private String password;

        public UserInfoWithPlainPassword(String pPassword) {
            password = pPassword;
        }

        public String getPassphrase() {
            return null;
        }

        public String getPassword() {
            return password;
        }

        public boolean promptPassword(String message) {
            return false;
        }

        public boolean promptPassphrase(String message) {
            return false;
        }

        public boolean promptYesNo(String message) {
            return false;
        }

        public void showMessage(String message) {
        }
    }

    /**
     * Gets the JSch implementation.
     * @return
     */
    public JSch getJsch() {
        return jsch;
    }

    /**
     * Sets the JSch implementation.
     * @param jsch
     */
    public void setJsch(JSch jsch) {
        this.jsch = jsch;
    }

}
