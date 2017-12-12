/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.wso2.broker.core;

import org.wso2.broker.common.data.types.FieldTable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages the bindings for a given {@link Exchange}.
 * TODO why do we repeat routing key in two places (as key and as field in binding object)?
 * Feels like we need to refactor this class.
 */
final class BindingsRegistry {

    private final Map<String, BindingSet> routingKeyToBindingMap;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    BindingsRegistry() {
        this.routingKeyToBindingMap = new ConcurrentHashMap<>();
    }

    void bind(Queue queue, String bindingKey, FieldTable arguments) throws BrokerException {
        lock.writeLock().lock();
        try {
            BindingSet bindingSet = routingKeyToBindingMap.computeIfAbsent(bindingKey, k -> new BindingSet());
            bindingSet.add(new Binding(queue, bindingKey, arguments));
        } finally {
            lock.writeLock().unlock();
        }
    }

    void unbind(Queue queue, String routingKey) {
        lock.writeLock().lock();
        try {
            BindingSet bindingSet = routingKeyToBindingMap.get(routingKey);
            bindingSet.remove(queue);

            if (bindingSet.isEmpty()) {
                routingKeyToBindingMap.remove(routingKey);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    BindingSet getBindingsForRoute(String routingKey) {
        lock.readLock().lock();
        try {
            BindingSet bindingSet = routingKeyToBindingMap.get(routingKey);
            if (bindingSet == null) {
                bindingSet = BindingSet.emptySet();
            }
            return bindingSet;
        } finally {
            lock.readLock().unlock();
        }
    }

    boolean isEmpty() {
        lock.readLock().lock();
        try {
            return routingKeyToBindingMap.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

}
