/*
 * Copyright 2014-2015 CyberVision, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kaaproject.kaa.client;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.kaaproject.kaa.client.bootstrap.BootstrapManager;
import org.kaaproject.kaa.client.bootstrap.DefaultBootstrapManager;
import org.kaaproject.kaa.client.channel.BootstrapTransport;
import org.kaaproject.kaa.client.channel.ConfigurationTransport;
import org.kaaproject.kaa.client.channel.EventTransport;
import org.kaaproject.kaa.client.channel.KaaChannelManager;
import org.kaaproject.kaa.client.channel.KaaDataChannel;
import org.kaaproject.kaa.client.channel.KaaInternalChannelManager;
import org.kaaproject.kaa.client.channel.LogTransport;
import org.kaaproject.kaa.client.channel.MetaDataTransport;
import org.kaaproject.kaa.client.channel.NotificationTransport;
import org.kaaproject.kaa.client.channel.ProfileTransport;
import org.kaaproject.kaa.client.channel.RedirectionTransport;
import org.kaaproject.kaa.client.channel.TransportConnectionInfo;
import org.kaaproject.kaa.client.channel.TransportProtocolId;
import org.kaaproject.kaa.client.channel.UserTransport;
import org.kaaproject.kaa.client.channel.impl.DefaultBootstrapDataProcessor;
import org.kaaproject.kaa.client.channel.impl.DefaultChannelManager;
import org.kaaproject.kaa.client.channel.impl.DefaultOperationDataProcessor;
import org.kaaproject.kaa.client.channel.impl.channels.DefaultBootstrapChannel;
import org.kaaproject.kaa.client.channel.impl.channels.DefaultOperationTcpChannel;
import org.kaaproject.kaa.client.channel.impl.transports.DefaultBootstrapTransport;
import org.kaaproject.kaa.client.channel.impl.transports.DefaultConfigurationTransport;
import org.kaaproject.kaa.client.channel.impl.transports.DefaultEventTransport;
import org.kaaproject.kaa.client.channel.impl.transports.DefaultLogTransport;
import org.kaaproject.kaa.client.channel.impl.transports.DefaultMetaDataTransport;
import org.kaaproject.kaa.client.channel.impl.transports.DefaultNotificationTransport;
import org.kaaproject.kaa.client.channel.impl.transports.DefaultProfileTransport;
import org.kaaproject.kaa.client.channel.impl.transports.DefaultRedirectionTransport;
import org.kaaproject.kaa.client.channel.impl.transports.DefaultUserTransport;
import org.kaaproject.kaa.client.configuration.base.ConfigurationListener;
import org.kaaproject.kaa.client.configuration.base.ConfigurationManager;
import org.kaaproject.kaa.client.configuration.base.ResyncConfigurationManager;
import org.kaaproject.kaa.client.configuration.storage.ConfigurationStorage;
import org.kaaproject.kaa.client.context.TransportContext;
import org.kaaproject.kaa.client.event.DefaultEventManager;
import org.kaaproject.kaa.client.event.EndpointAccessToken;
import org.kaaproject.kaa.client.event.EndpointKeyHash;
import org.kaaproject.kaa.client.event.EventFamilyFactory;
import org.kaaproject.kaa.client.event.EventManager;
import org.kaaproject.kaa.client.event.FindEventListenersCallback;
import org.kaaproject.kaa.client.event.registration.AttachEndpointToUserCallback;
import org.kaaproject.kaa.client.event.registration.DefaultEndpointRegistrationManager;
import org.kaaproject.kaa.client.event.registration.DetachEndpointFromUserCallback;
import org.kaaproject.kaa.client.event.registration.OnAttachEndpointOperationCallback;
import org.kaaproject.kaa.client.event.registration.OnDetachEndpointOperationCallback;
import org.kaaproject.kaa.client.event.registration.UserAttachCallback;
import org.kaaproject.kaa.client.exceptions.KaaClusterConnectionException;
import org.kaaproject.kaa.client.exceptions.KaaException;
import org.kaaproject.kaa.client.exceptions.KaaRuntimeException;
import org.kaaproject.kaa.client.logging.AbstractLogCollector;
import org.kaaproject.kaa.client.logging.DefaultLogCollector;
import org.kaaproject.kaa.client.logging.LogStorage;
import org.kaaproject.kaa.client.logging.LogUploadStrategy;
import org.kaaproject.kaa.client.notification.DefaultNotificationManager;
import org.kaaproject.kaa.client.notification.NotificationListener;
import org.kaaproject.kaa.client.notification.NotificationTopicListListener;
import org.kaaproject.kaa.client.notification.UnavailableTopicException;
import org.kaaproject.kaa.client.persistence.KaaClientPropertiesState;
import org.kaaproject.kaa.client.persistence.KaaClientState;
import org.kaaproject.kaa.client.persistence.PersistentStorage;
import org.kaaproject.kaa.client.profile.DefaultProfileManager;
import org.kaaproject.kaa.client.profile.ProfileContainer;
import org.kaaproject.kaa.client.profile.ProfileManager;
import org.kaaproject.kaa.client.transport.AbstractHttpClient;
import org.kaaproject.kaa.client.transport.TransportException;
import org.kaaproject.kaa.common.endpoint.gen.Topic;
import org.kaaproject.kaa.common.hash.EndpointObjectHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Abstract class that holds general elements of Kaa library.
 * </p>
 *
 * <p>
 * This class creates and binds Kaa library modules. Public access to each
 * module is performed using {@link KaaClient} interface.
 * </p>
 *
 * <p>
 * Class contains abstract methods
 * {@link AbstractKaaClient#createHttpClient(String, PrivateKey, PublicKey, PublicKey)}
 * and {@link AbstractKaaClient#createPersistentStorage()} which are used to
 * reference the platform-specific implementation of http client and Kaa's state
 * persistent storage.
 * </p>
 *
 * <p>
 * Http client ({@link AbstractHttpClient}) is used to provide basic
 * communication with Bootstrap and Operation servers using HTTP protocol.
 * </p>
 *
 * @author Yaroslav Zeygerman
 * @author Andrew Shvayka
 *
 * @see KaaClient
 * @see AbstractHttpClient
 * @see PersistentStorage
 */
public abstract class AbstractKaaClient implements GenericKaaClient {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractKaaClient.class);
    private static final long LONG_POLL_TIMEOUT = 60000L;

    private volatile boolean isInitialized = false;

    protected final ConfigurationManager configurationManager;
    protected final AbstractLogCollector logCollector;

    private final DefaultNotificationManager notificationManager;
    private final ProfileManager profileManager;

    private final KaaClientProperties properties;
    private final KaaClientState kaaClientState;
    private final BootstrapManager bootstrapManager;
    private final EventManager eventManager;
    private final EventFamilyFactory eventFamilyFactory;

    private final DefaultEndpointRegistrationManager endpointRegistrationManager;

    private final KaaInternalChannelManager channelManager;

    protected final KaaClientPlatformContext context;
    protected final KaaClientStateListener stateListener;

    AbstractKaaClient(KaaClientPlatformContext context, KaaClientStateListener listener) throws IOException, GeneralSecurityException {
        this.context = context;
        this.stateListener = listener;
        if (context.getProperties() != null) {
            this.properties = context.getProperties();
        } else {
            this.properties = new KaaClientProperties(context.getBase64());
        }

        Map<TransportProtocolId, List<TransportConnectionInfo>> bootstrapServers = this.properties.getBootstrapServers();
        if (bootstrapServers == null || bootstrapServers.isEmpty()) {
            throw new RuntimeException("Unable to obtain list of bootstrap servers."); // NOSONAR
        }

        for (Map.Entry<TransportProtocolId, List<TransportConnectionInfo>> cursor : bootstrapServers.entrySet()) {
            Collections.shuffle(cursor.getValue());
        }

        kaaClientState = new KaaClientPropertiesState(context.createPersistentStorage(), context.getBase64(), this.properties);

        TransportContext transportContext = buildTransportContext(properties, kaaClientState);
        
        bootstrapManager = buildBootstrapManager(properties, kaaClientState, transportContext);

        channelManager = buildChannelManager(bootstrapManager, bootstrapServers, transportContext);

        bootstrapManager.setChannelManager(channelManager);
        
        profileManager = buildProfileManager(properties, kaaClientState, transportContext);
        notificationManager = buildNotificationManager(properties, kaaClientState, transportContext);
        eventManager = buildEventManager(properties, kaaClientState, transportContext);
        endpointRegistrationManager = buildRegistrationManager(properties, kaaClientState, transportContext);
        logCollector = buildLogCollector(properties, kaaClientState, transportContext);
        configurationManager = buildConfigurationManager(properties, kaaClientState, transportContext);

        transportContext.getRedirectionTransport().setBootstrapManager(bootstrapManager);
        transportContext.getBootstrapTransport().setBootstrapManager(bootstrapManager);
        transportContext.getProfileTransport().setProfileManager(profileManager);
        transportContext.getEventTransport().setEventManager(eventManager);
        transportContext.getNotificationTransport().setNotificationProcessor(notificationManager);
        transportContext.getConfigurationTransport().setConfigurationHashContainer(configurationManager.getConfigurationHashContainer());
        transportContext.getConfigurationTransport().setConfigurationProcessor(configurationManager.getConfigurationProcessor());
        transportContext.getUserTransport().setEndpointRegistrationProcessor(endpointRegistrationManager);
        transportContext.getLogTransport().setLogProcessor(logCollector);
        transportContext.initTransports(this.channelManager, this.kaaClientState);
        
        eventFamilyFactory = new EventFamilyFactory(eventManager, context.getExecutorContext());
    }

    @Override
    public void start() {
        context.getExecutorContext().init();
        getLifeCycleExecutor().submit(new Runnable() {
            @Override
            public void run() {
                LOG.debug("Client startup initiated");
                try {
                    if (!isInitialized) {
                        isInitialized = true;
                    } else {
                        LOG.warn("Client is already initialized!");
                        return;
                    }
                    // Load configuration
                    configurationManager.init();
                    bootstrapManager.receiveOperationsServerList();
                    if (stateListener != null) {
                        stateListener.onStarted();
                    }
                } catch (TransportException e) {
                    LOG.error("Start failed", e);
                    if (stateListener != null) {
                        stateListener.onStartFailure(new KaaClusterConnectionException(e));
                    }
                } catch (KaaRuntimeException e) {
                    LOG.error("Start failed", e);
                    if (stateListener != null) {
                        stateListener.onStartFailure(new KaaException(e));
                    }
                }
            }
        });
    }

    @Override
    public void stop() {
        getLifeCycleExecutor().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    logCollector.stop();
                    kaaClientState.persist();
                    channelManager.shutdown();
                    isInitialized = false;
                    if (stateListener != null) {
                        stateListener.onStopped();
                    }
                } catch (Exception e) {
                    LOG.error("Stop failed", e);
                    if (stateListener != null) {
                        stateListener.onStopFailure(new KaaException(e));
                    }
                }
            }
        });
        context.getExecutorContext().stop();
    }

    @Override
    public void pause() {
        getLifeCycleExecutor().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    kaaClientState.persist();
                    channelManager.pause();
                    if (stateListener != null) {
                        stateListener.onPaused();
                    }
                } catch (Exception e) {
                    LOG.error("Pause failed", e);
                    if (stateListener != null) {
                        stateListener.onPauseFailure(new KaaException(e));
                    }
                }
            }
        });
    }

    @Override
    public void resume() {
        getLifeCycleExecutor().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    channelManager.resume();
                    if (stateListener != null) {
                        stateListener.onResume();
                    }
                } catch (Exception e) {
                    LOG.error("Resume failed", e);
                    if (stateListener != null) {
                        stateListener.onResumeFailure(new KaaException(e));
                    }
                }
            }
        });
    }

    private ExecutorService getLifeCycleExecutor() {
        return context.getExecutorContext().getLifeCycleExecutor();
    }

    @Override
    public void setProfileContainer(ProfileContainer container) {
        this.profileManager.setProfileContainer(container);
    };

    @Override
    public void updateProfile() {
        this.profileManager.updateProfile();
    }

    @Override
    public void setConfigurationStorage(ConfigurationStorage storage) {
        this.configurationManager.setConfigurationStorage(storage);
    }

    @Override
    public boolean addConfigurationListener(ConfigurationListener listener) {
        return this.configurationManager.addListener(listener);
    }

    @Override
    public boolean removeConfigurationListener(ConfigurationListener listener) {
        return this.configurationManager.removeListener(listener);
    }

    @Override
    public List<Topic> getTopics() {
        return this.notificationManager.getTopics();
    }

    @Override
    public void addTopicListListener(NotificationTopicListListener listener) {
        this.notificationManager.addTopicListListener(listener);
    }

    @Override
    public void removeTopicListListener(NotificationTopicListListener listener) {
        this.notificationManager.removeTopicListListener(listener);
    }

    @Override
    public void addNotificationListener(NotificationListener listener) {
        this.notificationManager.addNotificationListener(listener);
    }

    @Override
    public void addNotificationListener(String topicId, NotificationListener listener) throws UnavailableTopicException {
        this.notificationManager.addNotificationListener(topicId, listener);
    }

    @Override
    public void removeNotificationListener(NotificationListener listener) {
        this.notificationManager.removeNotificationListener(listener);
    }

    @Override
    public void removeNotificationListener(String topicId, NotificationListener listener) throws UnavailableTopicException {
        this.notificationManager.removeNotificationListener(topicId, listener);
    }

    @Override
    public void subscribeToTopic(String topicId) throws UnavailableTopicException {
        this.notificationManager.subscribeToTopic(topicId, true);
    }

    @Override
    public void subscribeToTopic(String topicId, boolean forceSync) throws UnavailableTopicException {
        this.notificationManager.subscribeToTopic(topicId, forceSync);
    }

    @Override
    public void subscribeToTopics(List<String> topicIds) throws UnavailableTopicException {
        this.notificationManager.subscribeToTopics(topicIds, true);
    }

    @Override
    public void subscribeToTopics(List<String> topicIds, boolean forceSync) throws UnavailableTopicException {
        this.notificationManager.subscribeToTopics(topicIds, forceSync);
    }

    @Override
    public void unsubscribeFromTopic(String topicId) throws UnavailableTopicException {
        this.notificationManager.unsubscribeFromTopic(topicId, true);
    }

    @Override
    public void unsubscribeFromTopic(String topicId, boolean forceSync) throws UnavailableTopicException {
        this.notificationManager.unsubscribeFromTopic(topicId, forceSync);
    }

    @Override
    public void unsubscribeFromTopics(List<String> topicIds) throws UnavailableTopicException {
        this.notificationManager.unsubscribeFromTopics(topicIds, true);
    }

    @Override
    public void unsubscribeFromTopics(List<String> topicIds, boolean forceSync) throws UnavailableTopicException {
        this.notificationManager.unsubscribeFromTopics(topicIds, forceSync);
    }

    @Override
    public void syncTopicsList() {
        this.notificationManager.sync();
    }

    @Override
    public void setLogStorage(LogStorage storage) {
        this.logCollector.setStorage(storage);
    }

    @Override
    public void setLogUploadStrategy(LogUploadStrategy strategy) {
        this.logCollector.setStrategy(strategy);
    }

    @Override
    public EventFamilyFactory getEventFamilyFactory() {
        return eventFamilyFactory;
    }

    @Override
    public void findEventListeners(List<String> eventFQNs, FindEventListenersCallback listener) {
        eventManager.findEventListeners(eventFQNs, listener);
    }

    @Override
    public KaaChannelManager getChannelManager() {
        return channelManager;
    }

    @Override
    public PublicKey getClientPublicKey() {
        return kaaClientState.getPublicKey();
    }

    @Override
    public String getEndpointKeyHash() {
        return kaaClientState.getEndpointKeyHash().getKeyHash();
    }

    @Override
    public PrivateKey getClientPrivateKey() {
        return kaaClientState.getPrivateKey();
    }

    @Override
    public void setEndpointAccessToken(String token) {
        endpointRegistrationManager.updateEndpointAccessToken(token);
    }

    @Override
    public String refreshEndpointAccessToken() {
        return endpointRegistrationManager.refreshEndpointAccessToken();
    }

    @Override
    public String getEndpointAccessToken() {
        return kaaClientState.getEndpointAccessToken();
    }

    @Override
    public void attachEndpoint(EndpointAccessToken endpointAccessToken, OnAttachEndpointOperationCallback resultListener) {
        endpointRegistrationManager.attachEndpoint(endpointAccessToken, resultListener);
    }

    @Override
    public void detachEndpoint(EndpointKeyHash endpointKeyHash, OnDetachEndpointOperationCallback resultListener) {
        endpointRegistrationManager.detachEndpoint(endpointKeyHash, resultListener);
    }

    @Override
    public void attachUser(String userExternalId, String userAccessToken, UserAttachCallback callback) {
        endpointRegistrationManager.attachUser(userExternalId, userAccessToken, callback);
    }

    @Override
    public void attachUser(String userVerifierToken, String userExternalId, String userAccessToken, UserAttachCallback callback) {
        endpointRegistrationManager.attachUser(userVerifierToken, userExternalId, userAccessToken, callback);
    }

    @Override
    public boolean isAttachedToUser() {
        return kaaClientState.isAttachedToUser();
    }

    @Override
    public void setAttachedListener(AttachEndpointToUserCallback listener) {
        endpointRegistrationManager.setAttachedCallback(listener);
    }

    @Override
    public void setDetachedListener(DetachEndpointFromUserCallback listener) {
        endpointRegistrationManager.setDetachedCallback(listener);
    }

    protected TransportContext buildTransportContext(KaaClientProperties properties, KaaClientState kaaClientState) {
        BootstrapTransport bootstrapTransport = buildBootstrapTransport(properties, kaaClientState);
        ProfileTransport profileTransport = buildProfileTransport(properties, kaaClientState);
        EventTransport eventTransport = buildEventTransport(properties, kaaClientState);
        NotificationTransport notificationTransport = buildNotificationTransport(properties, kaaClientState);
        ConfigurationTransport configurationTransport = buildConfigurationTransport(properties, kaaClientState);
        UserTransport userTransport = buildUserTransport(properties, kaaClientState);
        RedirectionTransport redirectionTransport = buildRedirectionTransport(properties, kaaClientState);
        LogTransport logTransport = buildLogTransport(properties, kaaClientState);

        MetaDataTransport mdTransport = new DefaultMetaDataTransport();
        EndpointObjectHash publicKeyHash = EndpointObjectHash.fromSHA1(kaaClientState.getPublicKey().getEncoded());
        mdTransport = new DefaultMetaDataTransport();
        mdTransport.setClientProperties(properties);
        mdTransport.setClientState(kaaClientState);
        mdTransport.setEndpointPublicKeyhash(publicKeyHash);
        mdTransport.setTimeout(LONG_POLL_TIMEOUT);

        return new TransportContext(mdTransport, bootstrapTransport, profileTransport, eventTransport, notificationTransport,
                configurationTransport, userTransport, redirectionTransport, logTransport);
    }

    protected KaaInternalChannelManager buildChannelManager(BootstrapManager bootstrapManager, Map<TransportProtocolId, List<TransportConnectionInfo>> bootstrapServers, TransportContext transportContext) {
        KaaInternalChannelManager channelManager = new DefaultChannelManager(bootstrapManager, bootstrapServers);
        channelManager.setConnectivityChecker(context.createConnectivityChecker());

        DefaultBootstrapDataProcessor bootstrapDataProcessor = new DefaultBootstrapDataProcessor();
        bootstrapDataProcessor.setBootstrapTransport(transportContext.getBootstrapTransport());

        DefaultOperationDataProcessor operationsDataProcessor = new DefaultOperationDataProcessor();
        operationsDataProcessor.setConfigurationTransport(transportContext.getConfigurationTransport());
        operationsDataProcessor.setEventTransport(transportContext.getEventTransport());
        operationsDataProcessor.setMetaDataTransport(transportContext.getMdTransport());
        operationsDataProcessor.setNotificationTransport(transportContext.getNotificationTransport());
        operationsDataProcessor.setProfileTransport(transportContext.getProfileTransport());
        operationsDataProcessor.setRedirectionTransport(transportContext.getRedirectionTransport());
        operationsDataProcessor.setUserTransport(transportContext.getUserTransport());
        operationsDataProcessor.setLogTransport(transportContext.getLogTransport());

        KaaDataChannel bootstrapChannel = new DefaultBootstrapChannel(this, kaaClientState);
        bootstrapChannel.setMultiplexer(bootstrapDataProcessor);
        bootstrapChannel.setDemultiplexer(bootstrapDataProcessor);
        channelManager.addChannel(bootstrapChannel);

        KaaDataChannel operationsChannel = new DefaultOperationTcpChannel(kaaClientState, channelManager);
        operationsChannel.setMultiplexer(operationsDataProcessor);
        operationsChannel.setDemultiplexer(operationsDataProcessor);
        channelManager.addChannel(operationsChannel);

        return channelManager;
    }
    

    protected ResyncConfigurationManager buildConfigurationManager(KaaClientProperties properties, KaaClientState kaaClientState, TransportContext transportContext) {
        return new ResyncConfigurationManager(properties);
    }

    protected DefaultLogCollector buildLogCollector(KaaClientProperties properties, KaaClientState kaaClientState, TransportContext transportContext) {
        return new DefaultLogCollector(transportContext.getLogTransport(), context.getExecutorContext(), channelManager);
    }

    protected DefaultEndpointRegistrationManager buildRegistrationManager(KaaClientProperties properties, KaaClientState kaaClientState, TransportContext transportContext) {
        return new DefaultEndpointRegistrationManager(kaaClientState, context.getExecutorContext(),
                transportContext.getUserTransport(), transportContext.getProfileTransport());
    }

    protected DefaultEventManager buildEventManager(KaaClientProperties properties, KaaClientState kaaClientState, TransportContext transportContext) {
        return new DefaultEventManager(kaaClientState, context.getExecutorContext(), transportContext.getEventTransport());
    }

    protected DefaultNotificationManager buildNotificationManager(KaaClientProperties properties, KaaClientState kaaClientState, TransportContext transportContext) {
        return new DefaultNotificationManager(kaaClientState, context.getExecutorContext(), transportContext.getNotificationTransport());
    }

    protected ProfileManager buildProfileManager(KaaClientProperties properties, KaaClientState kaaClientState, TransportContext transportContext) {
        return new DefaultProfileManager(transportContext.getProfileTransport());
    }

    protected BootstrapManager buildBootstrapManager(KaaClientProperties properties, KaaClientState kaaClientState, TransportContext transportContext) {
        return new DefaultBootstrapManager(transportContext.getBootstrapTransport());
    }

    public AbstractHttpClient createHttpClient(String url, PrivateKey privateKey, PublicKey publicKey, PublicKey remotePublicKey) {
        return context.createHttpClient(url, privateKey, publicKey, remotePublicKey);
    }

    protected BootstrapTransport buildBootstrapTransport(KaaClientProperties properties, KaaClientState kaaClientState) {
        return new DefaultBootstrapTransport(properties.getApplicationToken());
    }

    protected ProfileTransport buildProfileTransport(KaaClientProperties properties, KaaClientState kaaClientState) {
        ProfileTransport transport = new DefaultProfileTransport();
        transport.setClientProperties(this.properties);
        return transport;
    }

    protected ConfigurationTransport buildConfigurationTransport(KaaClientProperties properties, KaaClientState kaaClientState) {
        ConfigurationTransport transport = new DefaultConfigurationTransport();
        // TODO: this should be part of properties and provided by user during
        // SDK generation
        transport.setResyncOnly(true);
        return transport;
    }

    protected NotificationTransport buildNotificationTransport(KaaClientProperties properties, KaaClientState kaaClientState) {
        return new DefaultNotificationTransport();
    }

    protected DefaultUserTransport buildUserTransport(KaaClientProperties properties, KaaClientState kaaClientState) {
        return new DefaultUserTransport();
    }

    protected EventTransport buildEventTransport(KaaClientProperties properties, KaaClientState kaaClientState) {
        return new DefaultEventTransport(kaaClientState);
    }

    protected LogTransport buildLogTransport(KaaClientProperties properties, KaaClientState kaaClientState) {
        return new DefaultLogTransport();
    }

    protected RedirectionTransport buildRedirectionTransport(KaaClientProperties properties, KaaClientState kaaClientState) {
        return new DefaultRedirectionTransport();
    }
}
