/*
 *
 * Copyright 2015-2018 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.github.bucket4j.remote;

import io.github.bucket4j.BucketConfiguration;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface Backend<K> {

    <T> CommandResult<T> execute(K key, RemoteCommand<T> command);

    void createInitialState(K key, BucketConfiguration configuration);

    <T> T createInitialStateAndExecute(K key, BucketConfiguration configuration, RemoteCommand<T> command);

    <T> CompletableFuture<CommandResult<T>> executeAsync(K key, RemoteCommand<T> command);

    <T> CompletableFuture<T> createInitialStateAndExecuteAsync(K key, BucketConfiguration configuration, RemoteCommand<T> command);

    Optional<BucketConfiguration> getConfiguration(K key);

    boolean isAsyncModeSupported();

}