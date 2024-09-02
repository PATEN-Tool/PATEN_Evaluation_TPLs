// 
// Decompiled by Procyon v0.5.36
// 

package org.opencastproject.caption.endpoint;

import org.slf4j.LoggerFactory;
import org.opencastproject.job.api.JobProducer;
import javax.xml.transform.Transformer;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import java.io.Writer;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Node;
import org.opencastproject.util.XmlSafeParser;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import javax.ws.rs.Produces;
import javax.ws.rs.POST;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.apache.commons.lang3.StringUtils;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import javax.ws.rs.core.Response;
import javax.ws.rs.FormParam;
import org.osgi.service.component.ComponentContext;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.caption.api.CaptionService;
import org.slf4j.Logger;
import org.opencastproject.util.doc.rest.RestService;
import javax.ws.rs.Path;
import org.opencastproject.rest.AbstractJobProducerEndpoint;

@Path("/")
@RestService(name = "caption", title = "Caption Service", abstractText = "This service enables conversion from one caption format to another.", notes = { "All paths above are relative to the REST endpoint base (something like http://your.server/files)", "If the service is down or not working it will return a status 503, this means the the underlying service is not working and is either restarting or has failed", "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In other words, there is a bug! You should file an error report with your server logs from the time when the error occurred: <a href=\"https://github.com/opencast/opencast/issues\">Opencast Issue Tracker</a>" })
public class CaptionServiceRestEndpoint extends AbstractJobProducerEndpoint
{
    private static final Logger logger;
    protected CaptionService service;
    protected ServiceRegistry serviceRegistry;
    
    public CaptionServiceRestEndpoint() {
        this.serviceRegistry = null;
    }
    
    public void activate(final ComponentContext cc) {
    }
    
    protected void setCaptionService(final CaptionService service) {
        this.service = service;
    }
    
    protected void unsetCaptionService(final CaptionService service) {
        this.service = null;
    }
    
    protected void setServiceRegistry(final ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }
    
    @POST
    @Path("convert")
    @Produces({ "text/xml" })
    @RestQuery(name = "convert", description = "Convert captions from one format to another.", restParameters = { @RestParameter(description = "Captions to be converted.", isRequired = true, name = "captions", type = RestParameter.Type.TEXT), @RestParameter(description = "Caption input format (for example: dfxp, subrip,...).", isRequired = false, defaultValue = "dfxp", name = "input", type = RestParameter.Type.STRING), @RestParameter(description = "Caption output format (for example: dfxp, subrip,...).", isRequired = false, defaultValue = "subrip", name = "output", type = RestParameter.Type.STRING), @RestParameter(description = "Caption language (for those formats that store such information).", isRequired = false, defaultValue = "en", name = "language", type = RestParameter.Type.STRING) }, responses = { @RestResponse(description = "OK, Conversion successfully completed.", responseCode = 200) }, returnDescription = "The converted captions file")
    public Response convert(@FormParam("input") final String inputType, @FormParam("output") final String outputType, @FormParam("captions") final String catalogAsXml, @FormParam("language") final String lang) {
        MediaPackageElement element;
        try {
            element = MediaPackageElementParser.getFromXml(catalogAsXml);
            if (!Catalog.TYPE.equals((Object)element.getElementType())) {
                return Response.status(Response.Status.BAD_REQUEST).entity((Object)"Captions must be of type catalog.").build();
            }
        }
        catch (Exception e) {
            CaptionServiceRestEndpoint.logger.info("Unable to parse serialized captions");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        try {
            Job job;
            if (StringUtils.isNotBlank((CharSequence)lang)) {
                job = this.service.convert((MediaPackageElement)element, inputType, outputType, lang);
            }
            else {
                job = this.service.convert((MediaPackageElement)element, inputType, outputType);
            }
            return Response.ok().entity((Object)new JaxbJob(job)).build();
        }
        catch (Exception e) {
            CaptionServiceRestEndpoint.logger.error("Unable to convert captions: {}", (Object)e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @POST
    @Path("languages")
    @Produces({ "text/xml" })
    @RestQuery(name = "languages", description = "Get information about languages in caption catalog (if such information is available).", restParameters = { @RestParameter(description = "Captions to be examined.", isRequired = true, name = "captions", type = RestParameter.Type.TEXT), @RestParameter(description = "Caption input format (for example: dfxp, subrip,...).", isRequired = false, defaultValue = "dfxp", name = "input", type = RestParameter.Type.STRING) }, responses = { @RestResponse(description = "OK, information was extracted and retrieved", responseCode = 200) }, returnDescription = "Returned information about languages present in captions.")
    public Response languages(@FormParam("input") final String inputType, @FormParam("captions") final String catalogAsXml) {
        try {
            final MediaPackageElement element = MediaPackageElementParser.getFromXml(catalogAsXml);
            if (!Catalog.TYPE.equals((Object)element.getElementType())) {
                return Response.status(Response.Status.BAD_REQUEST).entity((Object)"Captions must be of type catalog").build();
            }
            final String[] languageArray = this.service.getLanguageList((MediaPackageElement)element, inputType);
            final DocumentBuilder docBuilder = XmlSafeParser.newDocumentBuilderFactory().newDocumentBuilder();
            final Document doc = docBuilder.newDocument();
            final Element root = doc.createElement("languages");
            root.setAttribute("type", inputType);
            root.setAttribute("url", element.getURI().toString());
            for (final String lang : languageArray) {
                final Element language = doc.createElement("language");
                language.appendChild(doc.createTextNode(lang));
                root.appendChild(language);
            }
            final DOMSource domSource = new DOMSource(root);
            final StringWriter writer = new StringWriter();
            final StreamResult result = new StreamResult(writer);
            final Transformer transformer = XmlSafeParser.newTransformerFactory().newTransformer();
            transformer.transform(domSource, result);
            return Response.status(Response.Status.OK).entity((Object)writer.toString()).build();
        }
        catch (Exception e) {
            CaptionServiceRestEndpoint.logger.error("Unable to parse captions: {}", (Object)e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    public JobProducer getService() {
        if (this.service instanceof JobProducer) {
            return (JobProducer)this.service;
        }
        return null;
    }
    
    public ServiceRegistry getServiceRegistry() {
        return this.serviceRegistry;
    }
    
    static {
        logger = LoggerFactory.getLogger((Class)CaptionServiceRestEndpoint.class);
    }
}
