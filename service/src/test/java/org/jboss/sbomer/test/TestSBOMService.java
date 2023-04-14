/**
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
package org.jboss.sbomer.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Iterator;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.jboss.sbomer.core.enums.GeneratorImplementation;
import org.jboss.sbomer.model.Sbom;
import org.jboss.sbomer.processor.SbomProcessor;
import org.jboss.sbomer.rest.dto.Page;
import org.jboss.sbomer.service.SbomService;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import lombok.extern.slf4j.Slf4j;

@QuarkusTest
@Slf4j
public class TestSBOMService {

    @Inject
    SbomService sbomService;

    // @Inject
    // PncServiceMock pncServiceMock;

    @Any
    @Inject
    Instance<SbomProcessor> processors;

    private static final String INITIAL_BUILD_ID = "ARYT3LBXDVYAC";

    @Test
    public void testGetBaseSbom() throws IOException {
        log.info("testGetBaseSbom ...");
        Sbom baseSBOM = sbomService.getSbom(INITIAL_BUILD_ID, GeneratorImplementation.CYCLONEDX, null);
        assertNotNull(baseSBOM);
    }

    @Test
    public void testListBaseSboms() throws IOException {
        log.info("testListBaseSboms ...");

        Page<Sbom> page = sbomService.listSboms(0, 50);
        assertEquals(0, page.getPageIndex());
        assertEquals(50, page.getPageSize());
        assertTrue(page.getTotalHits() > 0);
        assertEquals(1, page.getTotalPages());
        assertTrue(page.getContent().size() > 0);

        Sbom foundSbom = null;
        Iterator<Sbom> contentIterator = page.getContent().iterator();
        while (contentIterator.hasNext()) {
            Sbom sbom = contentIterator.next();
            if (sbom.getBuildId().equals(INITIAL_BUILD_ID)) {
                foundSbom = sbom;
                break;
            }
        }

        assertNotNull(foundSbom);
    }

    @Test
    public void testBaseSbomNotFound() throws IOException {
        log.info("testBaseSbomNotFound ...");
        try {
            sbomService.getSbom("I_DO_NOT_EXIST", GeneratorImplementation.CYCLONEDX, null);
            fail("It should have thrown a 404 exception");
        } catch (NotFoundException nfe) {
        }
    }
}