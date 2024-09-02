// 
// Decompiled by Procyon v0.5.36
// 

package org.onosproject.cli.net;

import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.behaviour.ControllerConfig;
import org.onosproject.net.driver.DriverService;
import java.util.Arrays;
import java.util.ArrayList;
import org.onosproject.net.behaviour.ControllerInfo;
import java.util.List;
import org.onosproject.net.DeviceId;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;

@Command(scope = "onos", name = "device-setcontrollers", description = "sets the list of controllers for the given infrastructure device")
public class DeviceSetControllersCommand extends AbstractShellCommand
{
    @Argument(index = 0, name = "uri", description = "Device ID", required = true, multiValued = false)
    String uri;
    @Argument(index = 1, name = "controllersListStrings", description = "list of controllers to set for the specified device", required = true, multiValued = true)
    String[] controllersListStrings;
    private DeviceId deviceId;
    private List<ControllerInfo> newControllers;
    
    public DeviceSetControllersCommand() {
        this.uri = null;
        this.controllersListStrings = null;
        this.newControllers = new ArrayList<ControllerInfo>();
    }
    
    @Override
    protected void execute() {
        Arrays.asList(this.controllersListStrings).forEach(cInfoString -> this.newControllers.add(new ControllerInfo(cInfoString)));
        final DriverService service = AbstractShellCommand.get(DriverService.class);
        this.deviceId = DeviceId.deviceId(this.uri);
        final DriverHandler h = service.createHandler(this.deviceId, new String[0]);
        final ControllerConfig config = (ControllerConfig)h.behaviour((Class)ControllerConfig.class);
        this.print("before:", new Object[0]);
        config.getControllers().forEach(c -> this.print(c.target(), new Object[0]));
        try {
            config.setControllers((List)this.newControllers);
        }
        catch (NullPointerException e) {
            this.print("No Device with requested parameters {} ", this.uri);
        }
        this.print("after:", new Object[0]);
        config.getControllers().forEach(c -> this.print(c.target(), new Object[0]));
        this.print("size %d", config.getControllers().size());
    }
}
