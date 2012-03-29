/*
 * Copyright 2012 the original author or authors.
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

package org.gradle.api.internal.artifacts.repositories.transport.http;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;
import org.apache.http.util.EntityUtils;
import org.gradle.api.internal.externalresource.transfer.ExternalResourceUploader;

import java.io.File;
import java.io.IOException;

public class HttpResourceUploader implements ExternalResourceUploader {

    private final HttpClientHelper http;

    public HttpResourceUploader(HttpClientHelper http) {
        this.http = http;
    }

    public void upload(File source, String destination, boolean overwrite) throws IOException {
        HttpPut method = new HttpPut(destination);
        method.setEntity(new FileEntity(source, "application/octet-stream"));
        HttpResponse response = http.performHttpRequest(method);
        EntityUtils.consume(response.getEntity());
        if (!http.wasSuccessful(response)) {
            throw new IOException(String.format("Could not PUT '%s'. Received status code %s from server: %s",
                    destination, response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase()));
        }

    }
}