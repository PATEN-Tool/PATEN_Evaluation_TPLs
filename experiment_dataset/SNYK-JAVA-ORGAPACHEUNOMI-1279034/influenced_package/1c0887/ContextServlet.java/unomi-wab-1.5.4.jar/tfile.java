// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.unomi.web;

import org.slf4j.LoggerFactory;
import java.util.LinkedHashMap;
import org.apache.unomi.api.conditions.Condition;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import org.apache.unomi.api.Persona;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.io.Writer;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.unomi.api.PersonaWithSessions;
import org.apache.unomi.api.Profile;
import org.apache.commons.io.IOUtils;
import javax.servlet.ServletRequest;
import org.apache.unomi.api.ContextResponse;
import org.apache.unomi.api.Item;
import org.apache.unomi.api.Event;
import org.apache.unomi.api.Session;
import org.apache.commons.lang3.StringUtils;
import org.apache.unomi.api.ContextRequest;
import org.apache.unomi.persistence.spi.CustomObjectMapper;
import javax.servlet.ServletResponse;
import java.util.Date;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import org.apache.unomi.api.services.ConfigSharingService;
import org.apache.unomi.api.services.PersonalizationService;
import org.apache.unomi.api.services.PrivacyService;
import org.apache.unomi.api.services.RulesService;
import org.apache.unomi.api.services.EventService;
import org.apache.unomi.api.services.ProfileService;
import org.slf4j.Logger;
import javax.servlet.http.HttpServlet;

public class ContextServlet extends HttpServlet
{
    private static final long serialVersionUID = 2928875830103325238L;
    private static final Logger logger;
    private static final int MAX_COOKIE_AGE_IN_SECONDS = 31536000;
    private String profileIdCookieName;
    private String profileIdCookieDomain;
    private int profileIdCookieMaxAgeInSeconds;
    private ProfileService profileService;
    private EventService eventService;
    private RulesService rulesService;
    private PrivacyService privacyService;
    private PersonalizationService personalizationService;
    private ConfigSharingService configSharingService;
    private boolean sanitizeConditions;
    
    public ContextServlet() {
        this.profileIdCookieName = "context-profile-id";
        this.profileIdCookieMaxAgeInSeconds = 31536000;
        this.sanitizeConditions = Boolean.parseBoolean(System.getProperty("org.apache.unomi.security.personalization.sanitizeConditions", "true"));
    }
    
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);
        this.configSharingService.setProperty("profileIdCookieName", (Object)this.profileIdCookieName);
        this.configSharingService.setProperty("profileIdCookieDomain", (Object)this.profileIdCookieDomain);
        this.configSharingService.setProperty("profileIdCookieMaxAgeInSeconds", (Object)this.profileIdCookieMaxAgeInSeconds);
        ContextServlet.logger.info("ContextServlet initialized.");
    }
    
    public void service(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        final Date timestamp = new Date();
        if (request.getParameter("timestamp") != null) {
            timestamp.setTime(Long.parseLong(request.getParameter("timestamp")));
        }
        HttpUtils.setupCORSHeaders(request, (ServletResponse)response);
        final String httpMethod = request.getMethod();
        if ("options".equals(httpMethod.toLowerCase())) {
            response.flushBuffer();
            if (ContextServlet.logger.isDebugEnabled()) {
                ContextServlet.logger.debug("OPTIONS request received. No context will be returned.");
            }
            return;
        }
        Profile profile = null;
        Session session = null;
        final String personaId = request.getParameter("personaId");
        if (personaId != null) {
            final PersonaWithSessions personaWithSessions = this.profileService.loadPersonaWithSessions(personaId);
            if (personaWithSessions == null) {
                ContextServlet.logger.error("Couldn't find persona with id=" + personaId);
                profile = null;
            }
            else {
                profile = (Profile)personaWithSessions.getPersona();
                session = (Session)personaWithSessions.getLastSession();
            }
        }
        ContextRequest contextRequest = null;
        String scope = null;
        String sessionId = null;
        String profileId = null;
        final String stringPayload = HttpUtils.getPayload(request);
        if (stringPayload != null) {
            final ObjectMapper mapper = CustomObjectMapper.getObjectMapper();
            final JsonFactory factory = mapper.getFactory();
            try {
                contextRequest = (ContextRequest)mapper.readValue(factory.createParser(stringPayload), (Class)ContextRequest.class);
            }
            catch (Exception e) {
                response.sendError(400, "Check logs for more details");
                ContextServlet.logger.error("Cannot read payload " + stringPayload, (Throwable)e);
                return;
            }
            if (contextRequest.getSource() != null) {
                scope = contextRequest.getSource().getScope();
            }
            sessionId = contextRequest.getSessionId();
            profileId = contextRequest.getProfileId();
        }
        if (sessionId == null) {
            sessionId = request.getParameter("sessionId");
        }
        if (profileId == null) {
            profileId = ServletCommon.getProfileIdCookieValue(request, this.profileIdCookieName);
        }
        if (profileId == null && sessionId == null && personaId == null) {
            response.sendError(400, "Check logs for more details");
            ContextServlet.logger.error("Couldn't find profileId, sessionId or personaId in incoming request! Stopped processing request. See debug level for more information");
            if (ContextServlet.logger.isDebugEnabled()) {
                ContextServlet.logger.debug("Request dump: {}", (Object)HttpUtils.dumpRequestInfo(request));
            }
            return;
        }
        int changes = 0;
        if (profile == null) {
            boolean profileCreated = false;
            final boolean invalidateProfile = request.getParameter("invalidateProfile") != null && new Boolean(request.getParameter("invalidateProfile"));
            if (profileId == null || invalidateProfile) {
                profile = this.createNewProfile(null, (ServletResponse)response, timestamp);
                profileCreated = true;
            }
            else {
                profile = this.profileService.load(profileId);
                if (profile == null) {
                    profile = this.createNewProfile(profileId, (ServletResponse)response, timestamp);
                    profileCreated = true;
                }
                else {
                    final Changes changesObject = this.checkMergedProfile((ServletResponse)response, profile, session);
                    changes |= changesObject.getChangeType();
                    profile = changesObject.getProfile();
                }
            }
            final boolean invalidateSession = request.getParameter("invalidateSession") != null && new Boolean(request.getParameter("invalidateSession"));
            if (StringUtils.isNotBlank((CharSequence)sessionId) && !invalidateSession) {
                session = this.profileService.loadSession(sessionId, timestamp);
                if (session != null) {
                    Profile sessionProfile = session.getProfile();
                    final boolean anonymousSessionProfile = sessionProfile.isAnonymousProfile();
                    if (!profile.isAnonymousProfile() && !anonymousSessionProfile && !profile.getItemId().equals(sessionProfile.getItemId())) {
                        profile = this.profileService.load(sessionProfile.getItemId());
                        if (profile != null) {
                            HttpUtils.sendProfileCookie(profile, (ServletResponse)response, this.profileIdCookieName, this.profileIdCookieDomain, this.profileIdCookieMaxAgeInSeconds);
                        }
                        else {
                            ContextServlet.logger.warn("Couldn't load profile {} referenced in session {}", (Object)sessionProfile.getItemId(), (Object)session.getItemId());
                        }
                    }
                    final Boolean requireAnonymousBrowsing = this.privacyService.isRequireAnonymousBrowsing(profile);
                    if (!requireAnonymousBrowsing || !anonymousSessionProfile) {
                        if (requireAnonymousBrowsing && !anonymousSessionProfile) {
                            sessionProfile = this.privacyService.getAnonymousProfile(profile);
                            session.setProfile(sessionProfile);
                            changes |= 0x2;
                        }
                        else if (!requireAnonymousBrowsing && anonymousSessionProfile) {
                            sessionProfile = profile;
                            session.setProfile(sessionProfile);
                            changes |= 0x2;
                        }
                        else if (!requireAnonymousBrowsing && !anonymousSessionProfile) {
                            sessionProfile = profile;
                            if (!session.getProfileId().equals(sessionProfile.getItemId())) {
                                changes |= 0x2;
                            }
                            session.setProfile(sessionProfile);
                        }
                    }
                }
            }
            if (session == null || invalidateSession) {
                final Profile sessionProfile = this.privacyService.isRequireAnonymousBrowsing(profile) ? this.privacyService.getAnonymousProfile(profile) : profile;
                if (StringUtils.isNotBlank((CharSequence)sessionId)) {
                    session = new Session(sessionId, sessionProfile, timestamp, scope);
                    changes |= 0x2;
                    final Event event = new Event("sessionCreated", session, profile, scope, (Item)null, (Item)session, timestamp);
                    if (sessionProfile.isAnonymousProfile()) {
                        event.setProfileId((String)null);
                    }
                    event.getAttributes().put("http_request", request);
                    event.getAttributes().put("http_response", response);
                    if (ContextServlet.logger.isDebugEnabled()) {
                        ContextServlet.logger.debug("Received event {} for profile={} session={} target={} timestamp={}", new Object[] { event.getEventType(), profile.getItemId(), session.getItemId(), event.getTarget(), timestamp });
                    }
                    changes |= this.eventService.send(event);
                }
            }
            if (profileCreated) {
                changes |= 0x4;
                final Event profileUpdated = new Event("profileUpdated", session, profile, scope, (Item)null, (Item)profile, timestamp);
                profileUpdated.setPersistent(false);
                profileUpdated.getAttributes().put("http_request", request);
                profileUpdated.getAttributes().put("http_response", response);
                if (ContextServlet.logger.isDebugEnabled()) {
                    ContextServlet.logger.debug("Received event {} for profile={} {} target={} timestamp={}", new Object[] { profileUpdated.getEventType(), profile.getItemId(), " session=" + ((session != null) ? session.getItemId() : null), profileUpdated.getTarget(), timestamp });
                }
                changes |= this.eventService.send(profileUpdated);
            }
        }
        final ContextResponse contextResponse = new ContextResponse();
        contextResponse.setProfileId(profile.getItemId());
        if (session != null) {
            contextResponse.setSessionId(session.getItemId());
        }
        else if (sessionId != null) {
            contextResponse.setSessionId(sessionId);
        }
        if (contextRequest != null) {
            final Changes changesObject2 = this.handleRequest(contextRequest, session, profile, contextResponse, (ServletRequest)request, (ServletResponse)response, timestamp);
            changes |= changesObject2.getChangeType();
            profile = changesObject2.getProfile();
        }
        if ((changes & 0x4) == 0x4) {
            this.profileService.save(profile);
            contextResponse.setProfileId(profile.getItemId());
        }
        if ((changes & 0x2) == 0x2 && session != null) {
            this.profileService.saveSession(session);
            contextResponse.setSessionId(session.getItemId());
        }
        if ((changes & 0x1) == 0x1) {
            response.setStatus(500);
        }
        final String extension = request.getRequestURI().substring(request.getRequestURI().lastIndexOf(".") + 1);
        final boolean noScript = "json".equals(extension);
        final String contextAsJSONString = CustomObjectMapper.getObjectMapper().writeValueAsString((Object)contextResponse);
        response.setCharacterEncoding("UTF-8");
        Writer responseWriter;
        if (noScript) {
            responseWriter = response.getWriter();
            response.setContentType("application/json");
            IOUtils.write(contextAsJSONString, responseWriter);
        }
        else {
            responseWriter = response.getWriter();
            responseWriter.append((CharSequence)"window.digitalData = window.digitalData || {};\n").append((CharSequence)"var cxs = ").append((CharSequence)contextAsJSONString).append((CharSequence)";\n");
        }
        responseWriter.flush();
    }
    
    private Changes checkMergedProfile(final ServletResponse response, Profile profile, final Session session) {
        int changes = 0;
        if (profile.getMergedWith() != null && !this.privacyService.isRequireAnonymousBrowsing(profile) && !profile.isAnonymousProfile()) {
            final Profile currentProfile = profile;
            final String masterProfileId = profile.getMergedWith();
            final Profile masterProfile = this.profileService.load(masterProfileId);
            if (masterProfile != null) {
                if (ContextServlet.logger.isDebugEnabled()) {
                    ContextServlet.logger.debug("Current profile was merged with profile {}, replacing profile in session", (Object)masterProfileId);
                }
                profile = masterProfile;
                if (session != null) {
                    session.setProfile(profile);
                    changes = 2;
                }
                HttpUtils.sendProfileCookie(profile, response, this.profileIdCookieName, this.profileIdCookieDomain, this.profileIdCookieMaxAgeInSeconds);
            }
            else {
                ContextServlet.logger.warn("Couldn't find merged profile {}, falling back to profile {}", (Object)masterProfileId, (Object)currentProfile.getItemId());
                profile = currentProfile;
                profile.setMergedWith((String)null);
                changes = 4;
            }
        }
        return new Changes(changes, profile);
    }
    
    private Changes handleRequest(final ContextRequest contextRequest, final Session session, Profile profile, final ContextResponse data, final ServletRequest request, final ServletResponse response, final Date timestamp) {
        final Changes changes = ServletCommon.handleEvents(contextRequest.getEvents(), session, profile, request, response, timestamp, this.privacyService, this.eventService);
        data.setProcessedEvents(changes.getProcessedItems());
        profile = changes.getProfile();
        if (contextRequest.isRequireSegments()) {
            data.setProfileSegments(profile.getSegments());
        }
        if (contextRequest.getRequiredProfileProperties() != null) {
            final Map<String, Object> profileProperties = new HashMap<String, Object>(profile.getProperties());
            if (!contextRequest.getRequiredProfileProperties().contains("*")) {
                profileProperties.keySet().retainAll(contextRequest.getRequiredProfileProperties());
            }
            data.setProfileProperties((Map)profileProperties);
        }
        if (session != null) {
            data.setSessionId(session.getItemId());
            if (contextRequest.getRequiredSessionProperties() != null) {
                final Map<String, Object> sessionProperties = new HashMap<String, Object>(session.getProperties());
                if (!contextRequest.getRequiredSessionProperties().contains("*")) {
                    sessionProperties.keySet().retainAll(contextRequest.getRequiredSessionProperties());
                }
                data.setSessionProperties((Map)sessionProperties);
            }
        }
        this.processOverrides(contextRequest, profile, session);
        final List<PersonalizationService.PersonalizedContent> filterNodes = (List<PersonalizationService.PersonalizedContent>)contextRequest.getFilters();
        if (filterNodes != null) {
            data.setFilteringResults((Map)new HashMap());
            for (final PersonalizationService.PersonalizedContent personalizedContent : this.sanitizePersonalizedContentObjects(filterNodes)) {
                data.getFilteringResults().put(personalizedContent.getId(), this.personalizationService.filter(profile, session, personalizedContent));
            }
        }
        final List<PersonalizationService.PersonalizationRequest> personalizations = (List<PersonalizationService.PersonalizationRequest>)contextRequest.getPersonalizations();
        if (personalizations != null) {
            data.setPersonalizations((Map)new HashMap());
            for (final PersonalizationService.PersonalizationRequest personalization : this.sanitizePersonalizations(personalizations)) {
                data.getPersonalizations().put(personalization.getId(), this.personalizationService.personalizeList(profile, session, personalization));
            }
        }
        if (!(profile instanceof Persona)) {
            data.setTrackedConditions(this.rulesService.getTrackedConditions(contextRequest.getSource()));
        }
        else {
            data.setTrackedConditions((Set)Collections.emptySet());
        }
        data.setAnonymousBrowsing((boolean)this.privacyService.isRequireAnonymousBrowsing(profile));
        data.setConsents(profile.getConsents());
        return changes;
    }
    
    private void processOverrides(final ContextRequest contextRequest, final Profile profile, final Session session) {
        if (profile instanceof Persona && contextRequest.getProfileOverrides() != null) {
            if (contextRequest.getProfileOverrides().getScores() != null) {
                profile.setScores(contextRequest.getProfileOverrides().getScores());
            }
            if (contextRequest.getProfileOverrides().getSegments() != null) {
                profile.setSegments(contextRequest.getProfileOverrides().getSegments());
            }
            if (contextRequest.getProfileOverrides().getProperties() != null) {
                profile.setProperties(contextRequest.getProfileOverrides().getProperties());
            }
            if (contextRequest.getSessionPropertiesOverrides() != null && session != null) {
                session.setProperties(contextRequest.getSessionPropertiesOverrides());
            }
        }
    }
    
    private Profile createNewProfile(final String existingProfileId, final ServletResponse response, final Date timestamp) {
        String profileId = existingProfileId;
        if (profileId == null) {
            profileId = UUID.randomUUID().toString();
        }
        final Profile profile = new Profile(profileId);
        profile.setProperty("firstVisit", (Object)timestamp);
        HttpUtils.sendProfileCookie(profile, response, this.profileIdCookieName, this.profileIdCookieDomain, this.profileIdCookieMaxAgeInSeconds);
        return profile;
    }
    
    public void destroy() {
        ContextServlet.logger.info("Context servlet shutdown.");
    }
    
    public void setProfileService(final ProfileService profileService) {
        this.profileService = profileService;
    }
    
    public void setEventService(final EventService eventService) {
        this.eventService = eventService;
    }
    
    public void setRulesService(final RulesService rulesService) {
        this.rulesService = rulesService;
    }
    
    public void setProfileIdCookieDomain(final String profileIdCookieDomain) {
        this.profileIdCookieDomain = profileIdCookieDomain;
    }
    
    public void setProfileIdCookieName(final String profileIdCookieName) {
        this.profileIdCookieName = profileIdCookieName;
    }
    
    public void setProfileIdCookieMaxAgeInSeconds(final int profileIdCookieMaxAgeInSeconds) {
        this.profileIdCookieMaxAgeInSeconds = profileIdCookieMaxAgeInSeconds;
    }
    
    public void setPrivacyService(final PrivacyService privacyService) {
        this.privacyService = privacyService;
    }
    
    public void setPersonalizationService(final PersonalizationService personalizationService) {
        this.personalizationService = personalizationService;
    }
    
    public void setConfigSharingService(final ConfigSharingService configSharingService) {
        this.configSharingService = configSharingService;
    }
    
    private List<PersonalizationService.PersonalizedContent> sanitizePersonalizedContentObjects(final List<PersonalizationService.PersonalizedContent> personalizedContentObjects) {
        if (!this.sanitizeConditions) {
            return personalizedContentObjects;
        }
        final List<PersonalizationService.PersonalizedContent> result = new ArrayList<PersonalizationService.PersonalizedContent>();
        for (final PersonalizationService.PersonalizedContent personalizedContentObject : personalizedContentObjects) {
            boolean foundInvalidCondition = false;
            if (personalizedContentObject.getFilters() != null) {
                for (final PersonalizationService.Filter filter : personalizedContentObject.getFilters()) {
                    if (this.sanitizeCondition(filter.getCondition()) == null) {
                        foundInvalidCondition = true;
                        break;
                    }
                }
            }
            if (!foundInvalidCondition) {
                result.add(personalizedContentObject);
            }
        }
        return result;
    }
    
    private List<PersonalizationService.PersonalizationRequest> sanitizePersonalizations(final List<PersonalizationService.PersonalizationRequest> personalizations) {
        if (!this.sanitizeConditions) {
            return personalizations;
        }
        final List<PersonalizationService.PersonalizationRequest> result = new ArrayList<PersonalizationService.PersonalizationRequest>();
        for (final PersonalizationService.PersonalizationRequest personalizationRequest : personalizations) {
            final List<PersonalizationService.PersonalizedContent> personalizedContents = this.sanitizePersonalizedContentObjects(personalizationRequest.getContents());
            if (personalizedContents != null && personalizedContents.size() > 0) {
                result.add(personalizationRequest);
            }
        }
        return result;
    }
    
    private Condition sanitizeCondition(final Condition condition) {
        final Map<String, Object> newParameterValues = new LinkedHashMap<String, Object>();
        for (final Map.Entry<String, Object> parameterEntry : condition.getParameterValues().entrySet()) {
            final Object sanitizedValue = this.sanitizeValue(parameterEntry.getValue());
            if (sanitizedValue == null) {
                return null;
            }
            newParameterValues.put(parameterEntry.getKey(), parameterEntry.getValue());
        }
        return condition;
    }
    
    private Object sanitizeValue(final Object value) {
        if (value instanceof String) {
            final String stringValue = (String)value;
            if (stringValue.startsWith("script::") || stringValue.startsWith("parameter::")) {
                ContextServlet.logger.warn("Scripting detected in context request with value {}, filtering out...", value);
                return null;
            }
            return stringValue;
        }
        else {
            if (value instanceof List) {
                final List values = (List)value;
                final List newValues = new ArrayList();
                for (final Object listObject : values) {
                    final Object newObject = this.sanitizeValue(listObject);
                    if (newObject != null) {
                        newValues.add(newObject);
                    }
                }
                return values;
            }
            if (value instanceof Map) {
                final Map<Object, Object> newMap = new LinkedHashMap<Object, Object>();
                final Object newObject2;
                final Map<Object, Object> map;
                ((Map)value).forEach((key, value1) -> {
                    newObject2 = this.sanitizeValue(value1);
                    if (newObject2 != null) {
                        map.put(key, newObject2);
                    }
                    return;
                });
                return newMap;
            }
            if (value instanceof Condition) {
                return this.sanitizeCondition((Condition)value);
            }
            return value;
        }
    }
    
    static {
        logger = LoggerFactory.getLogger(ContextServlet.class.getName());
    }
}
