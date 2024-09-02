// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.unomi.plugins.baseplugin.actions;

import org.slf4j.LoggerFactory;
import org.apache.unomi.api.Profile;
import java.text.ParseException;
import java.text.DateFormat;
import org.apache.unomi.api.Consent;
import java.util.Map;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import org.apache.unomi.api.Event;
import org.apache.unomi.api.actions.Action;
import org.slf4j.Logger;
import org.apache.unomi.api.actions.ActionExecutor;

public class ModifyConsentAction implements ActionExecutor
{
    private static final Logger logger;
    public static final String CONSENT_PROPERTY_NAME = "consent";
    
    public int execute(final Action action, final Event event) {
        final Profile profile = event.getProfile();
        boolean isProfileUpdated = false;
        final ISO8601DateFormat dateFormat = new ISO8601DateFormat();
        final Map consentMap = event.getProperties().get("consent");
        if (consentMap != null) {
            if (consentMap.containsKey("typeIdentifier") && consentMap.containsKey("status")) {
                Consent consent = null;
                try {
                    consent = new Consent(consentMap, (DateFormat)dateFormat);
                    isProfileUpdated = profile.setConsent(consent);
                }
                catch (ParseException e) {
                    ModifyConsentAction.logger.error("Error parsing date format", (Throwable)e);
                }
            }
            else {
                ModifyConsentAction.logger.warn("Event properties for modifyConsent is missing typeIdentifier and grant properties. We will ignore this event.");
            }
        }
        return isProfileUpdated ? 2 : 0;
    }
    
    static {
        logger = LoggerFactory.getLogger(ModifyConsentAction.class.getName());
    }
}
