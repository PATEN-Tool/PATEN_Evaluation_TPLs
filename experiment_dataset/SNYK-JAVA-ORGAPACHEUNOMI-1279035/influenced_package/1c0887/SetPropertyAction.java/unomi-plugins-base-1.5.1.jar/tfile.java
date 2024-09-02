// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.unomi.plugins.baseplugin.actions;

import java.util.Map;
import org.apache.unomi.api.Item;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import org.apache.unomi.persistence.spi.PropertyHelper;
import org.apache.unomi.api.Event;
import org.apache.unomi.api.actions.Action;
import org.apache.unomi.api.services.EventService;
import org.apache.unomi.api.actions.ActionExecutor;

public class SetPropertyAction implements ActionExecutor
{
    private EventService eventService;
    private boolean useEventToUpdateProfile;
    
    public SetPropertyAction() {
        this.useEventToUpdateProfile = false;
    }
    
    public void setUseEventToUpdateProfile(final boolean useEventToUpdateProfile) {
        this.useEventToUpdateProfile = useEventToUpdateProfile;
    }
    
    public int execute(final Action action, final Event event) {
        final boolean storeInSession = Boolean.TRUE.equals(action.getParameterValues().get("storeInSession"));
        if (storeInSession && event.getSession() == null) {
            return 0;
        }
        final String propertyName = action.getParameterValues().get("setPropertyName");
        Object propertyValue = action.getParameterValues().get("setPropertyValue");
        if (propertyValue == null) {
            propertyValue = action.getParameterValues().get("setPropertyValueMultiple");
        }
        final Object propertyValueInteger = action.getParameterValues().get("setPropertyValueInteger");
        final Object setPropertyValueMultiple = action.getParameterValues().get("setPropertyValueMultiple");
        final Object setPropertyValueBoolean = action.getParameterValues().get("setPropertyValueBoolean");
        if (propertyValue == null) {
            if (propertyValueInteger != null) {
                propertyValue = PropertyHelper.getInteger(propertyValueInteger);
            }
            if (setPropertyValueMultiple != null) {
                propertyValue = setPropertyValueMultiple;
            }
            if (setPropertyValueBoolean != null) {
                propertyValue = PropertyHelper.getBooleanValue(setPropertyValueBoolean);
            }
        }
        if (propertyValue != null && propertyValue.equals("now")) {
            final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            propertyValue = format.format(event.getTimeStamp());
        }
        if (storeInSession) {
            if (PropertyHelper.setProperty((Object)event.getSession(), propertyName, propertyValue, (String)action.getParameterValues().get("setPropertyStrategy"))) {
                return 2;
            }
        }
        else if (this.useEventToUpdateProfile) {
            final Map<String, Object> propertyToUpdate = new HashMap<String, Object>();
            propertyToUpdate.put(propertyName, propertyValue);
            final Event updateProperties = new Event("updateProperties", event.getSession(), event.getProfile(), event.getScope(), (Item)null, (Item)event.getProfile(), new Date());
            updateProperties.setPersistent(false);
            updateProperties.setProperty("update", (Object)propertyToUpdate);
            final int changes = this.eventService.send(updateProperties);
            if ((changes & 0x4) == 0x4) {
                return 4;
            }
        }
        else if (PropertyHelper.setProperty((Object)event.getProfile(), propertyName, propertyValue, (String)action.getParameterValues().get("setPropertyStrategy"))) {
            return 4;
        }
        return 0;
    }
    
    public void setEventService(final EventService eventService) {
        this.eventService = eventService;
    }
}
