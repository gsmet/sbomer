/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.sbomer.core.dto.v1alpha1;

import java.time.Instant;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.jboss.sbomer.core.features.sbom.config.Config;

import com.fasterxml.jackson.databind.JsonNode;

@Schema(name = "V1Alpha1SbomRecord")
public record SbomRecord(
        @Schema(example = "3D4E6A6836AE457") String id,
        @Schema(example = "A6XHP5F42DYAA") String buildId,
        @Schema(example = "pkg:maven/io.hawt/project@4.0.0?type=pom") String rootPurl,
        Instant creationTime,
        @Schema(implementation = Map.class) JsonNode sbom,
        @Schema(example = "0") Integer configIndex,
        String statusMessage,
        org.jboss.sbomer.core.dto.v1alpha1.SbomGenerationRequestRecord generationRequest) {

    public SbomRecord(
            String id,
            String buildId,
            String rootPurl,
            Instant creationTime,
            JsonNode sbom,
            Integer configIndex,
            String statusMessage,
            String gId,
            Instant gCreationTime,
            String gStatus,
            String gResult,
            String gBuildId,
            Config gConfig,
            String gReason) {
        this(
                id,
                buildId,
                rootPurl,
                creationTime,
                sbom,
                configIndex,
                statusMessage,
                new org.jboss.sbomer.core.dto.v1alpha1.SbomGenerationRequestRecord(
                        gId,
                        gCreationTime,
                        gStatus,
                        gResult,
                        gBuildId,
                        gConfig,
                        gReason));
    }

}
