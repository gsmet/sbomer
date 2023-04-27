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
package org.jboss.sbomer.rest.v1alpha1;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.pnc.common.Strings;
import org.jboss.pnc.rest.api.parameters.PaginationParameters;
import org.jboss.sbomer.core.enums.GeneratorImplementation;
import org.jboss.sbomer.core.enums.ProcessorImplementation;
import org.jboss.sbomer.core.enums.SbomStatus;
import org.jboss.sbomer.core.errors.ApiException;
import org.jboss.sbomer.core.errors.NotFoundException;
import org.jboss.sbomer.core.utils.SbomUtils;
import org.jboss.sbomer.core.utils.UrlUtils;
import org.jboss.sbomer.model.Sbom;
import org.jboss.sbomer.rest.dto.Page;
import org.jboss.sbomer.service.SbomService;

import com.fasterxml.jackson.databind.JsonNode;

@Path("/api/v1alpha1/sboms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "SBOMs", description = "Endpoints related to SBOM handling, version v1alpha1")
public class SBOMResource {

    @Inject
    SbomService sbomService;

    @GET
    @Operation(summary = "List SBOMs", description = "List SBOMs available in the system, paginated.")
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "List of SBOMs in the system for a particular page and size.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)), })
    public Page<Sbom> list(@Valid @BeanParam PaginationParameters paginationParams) {
        return sbomService.list(paginationParams.getPageIndex(), paginationParams.getPageSize());
    }

    @GET
    @Path("{id}")
    @Operation(summary = "Get specific SBOM", description = "Get specific SBOM with the provided ID.")
    @Parameter(name = "id", description = "SBOM identifier", example = "429305915731435500")
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "The SBOM",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "400",
                    description = "Could not parse provided arguments",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "404",
                    description = "Requested SBOM could not be found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)), })
    public Sbom getById(@PathParam("id") String sbomId) {
        Sbom sbom = null;

        try {
            sbom = sbomService.get(Long.valueOf(sbomId));
        } catch (NumberFormatException e) {
            throw new ApiException(400, "Invalid SBOM id provided: '{}', a number was expected", sbomId);
        }

        if (sbom == null) {
            throw new NotFoundException("Sbom with id '{}' not found", sbomId);
        }

        return sbom;
    }

    @GET
    @Path("{id}/bom")
    @Operation(
            summary = "Get the BOM content of particular SBOM",
            description = "Get the BOM content of particular SBOM")
    @Parameter(name = "id", description = "SBOM identifier", example = "429305915731435500")
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "The BOM",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "400",
                    description = "Could not parse provided arguments",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "404",
                    description = "Requested SBOM could not be found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)), })
    public Response getBomById(@PathParam("id") String sbomId) {
        Sbom sbom = null;

        try {
            sbom = sbomService.get(Long.valueOf(sbomId));
        } catch (NumberFormatException e) {
            throw new ApiException(400, "Invalid SBOM id provided: '{}', a number was expected", sbomId);
        }

        if (sbom == null) {
            throw new NotFoundException("Sbom with id '{}' not found", sbomId);
        }

        return Response.status(Status.OK).entity(SbomUtils.toJsonNode(sbom.getCycloneDxBom())).build();
    }

    /**
     * Update the Bom within the {@link Sbom} resource.
     *
     * @param sbom {@link Sbom}
     * @return
     */
    @POST
    @Operation(
            summary = "Update Bom for specified SBOM",
            description = "Save submitted SBOM. This endpoint expects a SBOM in the CycloneDX format encapsulated in the structure.")
    @Parameter(name = "sbom", description = "The SBOM to save")
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "The SBOM was successfully saved",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "400",
                    description = "Could not parse provided arguments",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "422",
                    description = "Provided SBOM couldn't be saved because of validation failures",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)), })
    @Path("{id}/bom")
    public Response updateSbom(@PathParam("id") String sbomId, final JsonNode bom) {
        Long id = null;

        try {
            id = Long.valueOf(sbomId);
        } catch (NumberFormatException e) {
            throw new ApiException(400, "Invalid SBOM id provided: '{}', a number was expected", sbomId);
        }

        Sbom sbom = sbomService.updateBom(id, bom);

        return Response.status(Status.OK).entity(sbom).build();
    }

    @GET
    @Path("/build/{buildId}")
    @Operation(
            summary = "Get all SBOMs related to a PNC build",
            description = "Get all the SBOMs related to the specified PNC build.")
    @Parameter(name = "buildId", description = "PNC build identifier", example = "ARYT3LBXDVYAC")
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "The SBOMs related to a specific PNC buildId",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "404",
                    description = "No SBOMs could be found for the specified PNC build",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)), })
    public List<Sbom> listAllWithPncBuildId(@PathParam("buildId") String buildId) {
        return sbomService.listAllSbomsWithBuildId(buildId);
    }

    @GET
    @Path("/build/{buildId}/base")
    @Operation(summary = "Get the base SBOM related to a PNC build")
    @Parameter(name = "buildId", description = "PNC build identifier", example = "ARYT3LBXDVYAC")
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "The base SBOM",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "404",
                    description = "Requested base SBOM could not be found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)), })
    public Sbom getBaseSbomWithPncBuildId(@PathParam("buildId") String buildId) {

        return sbomService.getBaseSbomByBuildId(buildId);
    }

    @GET
    @Path("/build/{buildId}/base/bom")
    @Operation(summary = "Get the base SBOM content related to a PNC build")
    @Parameter(name = "buildId", description = "PNC build identifier", example = "ARYT3LBXDVYAC")
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "The base SBOM content in CycloneDX format",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "404",
                    description = "Requested base SBOM could not be found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)), })
    public Response getBomOfBaseSbomWithPncBuildId(@PathParam("buildId") String buildId) {
        return Response.status(Status.OK)
                .entity(SbomUtils.toJsonNode(sbomService.getBaseSbomByBuildId(buildId).getCycloneDxBom()))
                .build();
    }

    @GET
    @Path("/build/{buildId}/enriched")
    @Operation(summary = "Get the enriched SBOM related to a PNC build")
    @Parameter(name = "buildId", description = "PNC build identifier", example = "ARYT3LBXDVYAC")
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "The enriched SBOM",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "404",
                    description = "Requested enriched SBOM could not be found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)), })
    public Sbom getEnrichedSbomWithPncBuildId(@PathParam("buildId") String buildId) {

        return sbomService.getEnrichedSbomByBuildId(buildId);
    }

    @GET
    @Path("/build/{buildId}/enriched/bom")
    @Operation(summary = "Get the enriched SBOM content related to a PNC build")
    @Parameter(name = "buildId", description = "PNC build identifier", example = "ARYT3LBXDVYAC")
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "The enriched SBOM content in CycloneDX format",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "404",
                    description = "Requested enriched SBOM could not be found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)), })
    public Response getBomOfEnrichedSbomWithPncBuildId(@PathParam("buildId") String buildId) {
        return Response.status(Status.OK)
                .entity(SbomUtils.toJsonNode(sbomService.getEnrichedSbomByBuildId(buildId).getCycloneDxBom()))
                .build();
    }

    @GET
    @Path("/purl/{rootPurl}/base")
    @Operation(summary = "Get the base SBOM related to a root component purl")
    @Parameter(
            name = "rootPurl",
            description = "Root component purl",
            example = "pkg:maven/cpaas.tp/cpaas-test-pnc-maven@1.0.0.redhat-04562?type=pom")
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "The base SBOM",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "404",
                    description = "Requested base SBOM could not be found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)), })
    public Sbom getBaseSbomWithRootPurl(@PathParam("rootPurl") String rootPurl) {

        return sbomService.getBaseSbomByRootPurl(UrlUtils.urldecode(rootPurl));
    }

    @GET
    @Path("/purl/{rootPurl}/base/bom")
    @Operation(summary = "Get the base SBOM content related to a root component purl")
    @Parameter(
            name = "rootPurl",
            description = "Root component purl",
            example = "pkg:maven/cpaas.tp/cpaas-test-pnc-maven@1.0.0.redhat-04562?type=pom")
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "The base SBOM content in CycloneDX format",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "404",
                    description = "Requested base SBOM could not be found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)), })
    public Response getBomOfBaseSbomWithRootPurl(@PathParam("rootPurl") String rootPurl) {
        return Response.status(Status.OK)
                .entity(
                        SbomUtils.toJsonNode(
                                sbomService.getBaseSbomByRootPurl(UrlUtils.urldecode(rootPurl)).getCycloneDxBom()))
                .build();
    }

    @GET
    @Path("/purl/{rootPurl}/enriched")
    @Operation(summary = "Get the enriched SBOM related to a root component purl")
    @Parameter(
            name = "buildId",
            description = "Root component purl",
            example = "pkg:maven/cpaas.tp/cpaas-test-pnc-maven@1.0.0.redhat-04562?type=pom")
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "The enriched SBOM",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "404",
                    description = "Requested enriched SBOM could not be found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)), })
    public Sbom getEnrichedSbomWithRootPurl(@PathParam("rootPurl") String rootPurl) {

        return sbomService.getEnrichedSbomByRootPurl(UrlUtils.urldecode(rootPurl));
    }

    @GET
    @Path("/purl/{rootPurl}/enriched/bom")
    @Operation(summary = "Get the enriched SBOM content related to a root component purl")
    @Parameter(
            name = "buildId",
            description = "Root component purl",
            example = "pkg:maven/cpaas.tp/cpaas-test-pnc-maven@1.0.0.redhat-04562?type=pom")
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "The enriched SBOM content in CycloneDX format",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "404",
                    description = "Requested enriched SBOM could not be found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)), })
    public Response getBomOfEnrichedSbomWithRootPurl(@PathParam("rootPurl") String rootPurl) {
        return Response.status(Status.OK)
                .entity(
                        SbomUtils.toJsonNode(
                                sbomService.getEnrichedSbomByRootPurl(UrlUtils.urldecode(rootPurl)).getCycloneDxBom()))
                .build();

    }

    public Page<Sbom> listAllWithPncBuildId(
            @PathParam("id") String buildId,
            @Valid @BeanParam PaginationParameters paginationParams) {
        return sbomService.list(buildId, paginationParams.getPageIndex(), paginationParams.getPageSize());
    }

    @POST
    @Operation(
            summary = "Generate a base SBOM based on the PNC build",
            description = "SBOM base generation for a particular PNC build Id offloaded to the service.")
    @Parameter(name = "id", description = "PNC build identifier", example = "ARYT3LBXDVYAC")
    @Parameter(
            name = "generator",
            description = "Generator to use to generate the SBOM. If not specified, CycloneDX will be used. Options are `DOMINO`, `CYCLONEDX`",
            example = "CYCLONEDX")
    @Path("/generate/build/{id}")
    @APIResponses({ @APIResponse(
            responseCode = "202",
            description = "Schedules generation of a SBOM for a particular PNC buildId. This is an asynchronous call. It does execute the generation behind the scenes.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)) })
    public Response generate(@PathParam("id") String buildId, @QueryParam("generator") String generator)
            throws Exception {

        GeneratorImplementation gen = GeneratorImplementation.CYCLONEDX;

        if (!Strings.isEmpty(generator)) {
            try {
                gen = GeneratorImplementation.valueOf(generator);
            } catch (IllegalArgumentException iae) {
                throw new ApiException(
                        Status.BAD_REQUEST.getStatusCode(),
                        "The specified generator does not exist, allowed values are `CYCLONEDX` or `DOMINO`. Leave empty to use `CYCLONEDX`",
                        iae);
            }
        }

        Sbom sbom = sbomService.generate(buildId, gen);

        return Response.status(Status.ACCEPTED).entity(sbom).build();
    }

    @POST
    @Operation(summary = "Process selected SBOM", description = "Process selected SBOM using specified prcoessor")
    @Parameter(name = "id", description = "The SBOM identifier")
    @Parameter(
            name = "processor",
            description = "Processor to use to enrich the SBOM. If not specified, DEFAULT will be used. Options are `PROPERTIES`, `PEDIGREE`",
            example = "DEFAULT")
    @Path("/{id}/process")
    @APIResponses({
            @APIResponse(
                    responseCode = "202",
                    description = "The SBOM enrichment process was accepted.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "400",
                    description = "Could not parse provided arguments",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "404",
                    description = "Requested SBOM could not be found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)), })
    public Response processEnrichmentOfBaseSbom(
            @PathParam("id") final String sbomId,
            @QueryParam("processor") String processor) throws Exception {

        Sbom sbom = null;

        // TODO share this code
        try {
            sbom = sbomService.get(Long.valueOf(sbomId));
        } catch (NumberFormatException e) {
            throw new ApiException(400, "Invalid SBOM id provided: '{}', a number was expected", sbomId);
        }

        if (sbom == null) {
            throw new NotFoundException("Sbom with id '{}' not found", sbomId);
        }

        if (sbom.getStatus() != SbomStatus.READY) {
            throw new ApiException(
                    400,
                    "Sbom with id '{}' is not ready yet, current status: {}",
                    sbomId,
                    sbom.getStatus());
        }

        ProcessorImplementation proc = ProcessorImplementation.DEFAULT;

        if (!Strings.isEmpty(processor)) {
            try {
                proc = ProcessorImplementation.valueOf(processor);
            } catch (IllegalArgumentException iae) {
                throw new ApiException(
                        Status.BAD_REQUEST.getStatusCode(),
                        "The specified processor does not exist, allowed values are `PROPERTIES`, `DEFAULT`. Leave empty to use `DEFAULT`",
                        iae);
            }
        }

        sbom = sbomService.process(sbom, proc);

        return Response.status(Status.ACCEPTED).entity(sbom).build();
    }

}