/*
 * Copyright 2014 CyberVision, Inc.
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

package org.kaaproject.kaa.client.channel.impl.channels;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.kaaproject.kaa.client.AbstractKaaClient;
import org.kaaproject.kaa.client.channel.ChannelDirection;
import org.kaaproject.kaa.client.channel.IPTransportInfo;
import org.kaaproject.kaa.client.channel.KaaDataChannel;
import org.kaaproject.kaa.client.channel.KaaDataDemultiplexer;
import org.kaaproject.kaa.client.channel.KaaDataMultiplexer;
import org.kaaproject.kaa.client.channel.TransportConnectionInfo;
import org.kaaproject.kaa.client.channel.TransportProtocolId;
import org.kaaproject.kaa.client.channel.TransportProtocolIdConstants;
import org.kaaproject.kaa.client.channel.connectivity.ConnectivityChecker;
import org.kaaproject.kaa.client.persistence.KaaClientState;
import org.kaaproject.kaa.client.transport.AbstractHttpClient;
import org.kaaproject.kaa.common.TransportType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractHttpChannel implements KaaDataChannel {
    public static final Logger LOG = LoggerFactory // NOSONAR
            .getLogger(AbstractHttpChannel.class);

    private IPTransportInfo currentServer;
    private final AbstractKaaClient client;
    private final KaaClientState state;

    private volatile ExecutorService executor;

    private volatile boolean lastConnectionFailed = false;
    private volatile boolean isShutdown = false;
    private volatile boolean isPaused = false;

    private AbstractHttpClient httpClient;
    private KaaDataDemultiplexer demultiplexer;
    private KaaDataMultiplexer multiplexer;

    public AbstractHttpChannel(AbstractKaaClient client, KaaClientState state) {
        this.client = client;
        this.state = state;
    }

    protected ExecutorService createExecutor() {
        LOG.info("Creating a new executor for channel {}", getId());
        return Executors.newSingleThreadExecutor();
    }

    @Override
    public TransportProtocolId getTransportProtocolId() {
        return TransportProtocolIdConstants.HTTP_TRANSPORT_ID;
    }

    @Override
    public synchronized void sync(TransportType type) {
        sync(Collections.singleton(type));
    }

    @Override
    public synchronized void sync(Set<TransportType> types) {
        if (isShutdown) {
            LOG.info("Can't sync. Channel {} is down", getId());
            return;
        }
        if (isPaused) {
            LOG.info("Can't sync. Channel {} is paused", getId());
            return;
        }
        if (multiplexer == null) {
            LOG.warn("Can't sync. Channel {} multiplexer is not set", getId());
            return;
        }
        if (demultiplexer == null) {
            LOG.warn("Can't sync. Channel {} demultiplexer is not set", getId());
            return;
        }
        if (currentServer == null) {
            lastConnectionFailed = true;
            LOG.warn("Can't sync. Server is null");
        }
        Map<TransportType, ChannelDirection> typeMap = new HashMap<>(1);
        for (TransportType type : types) {
            LOG.info("Processing sync {} for channel {}", type, getId());
            ChannelDirection direction = getSupportedTransportTypes().get(type);
            if (direction != null) {
                typeMap.put(type, direction);
            } else {
                LOG.error("Unsupported type {} for channel {}", type, getId());
            }
        }
        executor.submit(createChannelRunnable(typeMap));
    }

    @Override
    public synchronized void syncAll() {
        if (isShutdown) {
            LOG.info("Can't sync. Channel {} is down", getId());
            return;
        }
        if (isPaused) {
            LOG.info("Can't sync. Channel {} is paused", getId());
            return;
        }
        LOG.info("Processing sync all for channel {}", getId());
        if (multiplexer != null && demultiplexer != null) {
            if (currentServer != null) {
                executor.submit(createChannelRunnable(getSupportedTransportTypes()));
            } else {
                lastConnectionFailed = true;
                LOG.warn("Can't sync. Server is null");
            }
        }
    }

    @Override
    public void syncAck(TransportType type) {
        syncAck(Collections.singleton(type));
    }

    @Override
    public void syncAck(Set<TransportType> types) {
        LOG.info("Sync ack message is ignored for Channel {}", getId());
    }

    @Override
    public synchronized void setDemultiplexer(KaaDataDemultiplexer demultiplexer) {
        if (demultiplexer != null) {
            this.demultiplexer = demultiplexer;
        }
    }

    @Override
    public synchronized void setMultiplexer(KaaDataMultiplexer multiplexer) {
        if (multiplexer != null) {
            this.multiplexer = multiplexer;
        }
    }

    @Override
    public synchronized void setServer(TransportConnectionInfo server) {
        if (isShutdown) {
            LOG.info("Can't set server. Channel {} is down", getId());
            return;
        }
        if (executor == null && !isPaused) {
            executor = createExecutor();
        }
        if (server != null) {
            this.currentServer = new IPTransportInfo(server);
            this.httpClient = client.createHttpClient(currentServer.getURL() + getURLSufix(), state.getPrivateKey(), state.getPublicKey(),
                    currentServer.getPublicKey());
            if (lastConnectionFailed && !isPaused) {
                lastConnectionFailed = false;
                syncAll();
            }
        }
    }

    protected abstract String getURLSufix();

    @Override
    public TransportConnectionInfo getServer() {
        return currentServer;
    }

    @Override
    public void setConnectivityChecker(ConnectivityChecker checker) {
    }

    @Override
    public void shutdown() {
        if (!isShutdown) {
            isShutdown = true;
            if (executor != null) {
                executor.shutdownNow();
            }
        }
    }

    @Override
    public void pause() {
        if (isShutdown) {
            LOG.info("Can't pause channel. Channel [{}] is down", getId());
            return;
        }
        if (!isPaused) {
            isPaused = true;
            if (executor != null) {
                executor.shutdownNow();
                executor = null;
            }
        }
    }

    @Override
    public void resume() {
        if (isShutdown) {
            LOG.info("Can't resume channel. Channel [{}] is down", getId());
            return;
        }
        if (isPaused) {
            isPaused = false;
            if (executor == null) {
                executor = createExecutor();
            }
            if (lastConnectionFailed) {
                lastConnectionFailed = false;
                syncAll();
            }
        }
    }

    protected void connectionFailed(boolean failed) {
        lastConnectionFailed = failed;
        if (failed) {
            client.getChannelManager().onServerFailed(currentServer);
        }
    }

    protected KaaDataMultiplexer getMultiplexer() {
        return multiplexer;
    }

    protected KaaDataDemultiplexer getDemultiplexer() {
        return demultiplexer;
    }

    protected AbstractHttpClient getHttpClient() {
        return httpClient;
    }

    protected abstract Runnable createChannelRunnable(Map<TransportType, ChannelDirection> typeMap);

}
