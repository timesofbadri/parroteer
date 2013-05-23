package com.tngtech.leapdrone.drone;

import com.google.inject.Inject;
import com.tngtech.leapdrone.drone.commands.ControlDataCommand;
import com.tngtech.leapdrone.drone.commands.SetConfigValueCommand;
import com.tngtech.leapdrone.drone.data.Config;
import com.tngtech.leapdrone.drone.data.DroneConfiguration;
import com.tngtech.leapdrone.drone.data.NavData;
import com.tngtech.leapdrone.drone.listeners.DroneConfigurationListener;
import com.tngtech.leapdrone.drone.listeners.NavDataListener;
import com.tngtech.leapdrone.helpers.components.AddressComponent;
import com.tngtech.leapdrone.helpers.components.ReadyStateComponent;
import com.tngtech.leapdrone.helpers.components.ThreadComponent;
import com.tngtech.leapdrone.helpers.components.UdpComponent;

import static com.tngtech.leapdrone.helpers.ThreadHelper.sleep;

public class CommandSenderCoordinator extends CommandSender implements NavDataListener, DroneConfigurationListener
{
  private NavData currentNavData;

  private DroneConfiguration droneConfiguration;

  @Inject
  public CommandSenderCoordinator(ThreadComponent threadComponent, AddressComponent addressComponent, UdpComponent udpComponent,
                                  ReadyStateComponent readyStateComponent, NavigationDataRetriever navigationDataRetriever,
                                  ConfigurationDataRetriever configurationDataRetriever)
  {
    super(threadComponent, addressComponent, udpComponent, readyStateComponent);
    navigationDataRetriever.addNavDataListener(this);
    configurationDataRetriever.addDroneConfigurationListener(this);
  }

  public void sendLogin(String sessionId, String profileId, String applicationId)
  {
    sendBareConfigCommand(new SetConfigValueCommand(DroneConfiguration.SESSION_ID_KEY, sessionId));
    sendBareConfigCommand(new SetConfigValueCommand(DroneConfiguration.PROFILE_ID_KEY, profileId));
    sendBareConfigCommand(new SetConfigValueCommand(DroneConfiguration.APPLICATION_ID_KEY, applicationId));
  }

  public void sendConfigCommand(SetConfigValueCommand configCommand)
  {
    sendBareConfigCommand(configCommand);
    sendRefreshDroneConfigurationCommand();
  }

  public void sendBareConfigCommand(SetConfigValueCommand configCommand)
  {
    sendResetControlDataAcknowledgementFlagCommand();

    sendCommand(configCommand);
    waitForCommandAcknowledgeFlagToBe(true);
  }

  private void sendResetControlDataAcknowledgementFlagCommand()
  {
    sendCommand(new ControlDataCommand(ControlDataCommand.ControlDataMode.RESET_ACK_FLAG));
    waitForCommandAcknowledgeFlagToBe(false);
  }


  private void waitForCommandAcknowledgeFlagToBe(boolean value)
  {
    while (currentNavData == null || currentNavData.getState().isControlReceived() != value)
    {
      sleep(Config.WAIT_TIMEOUT);
    }
  }

  public void sendRefreshDroneConfigurationCommand()
  {
    sendResetControlDataAcknowledgementFlagCommand();

    resetConfigurationData();
    sendCommand(new ControlDataCommand(ControlDataCommand.ControlDataMode.GET_CONFIGURATION_DATA));
    waitForConfigurationData();
  }

  private void resetConfigurationData()
  {
    droneConfiguration = null;
  }

  private void waitForConfigurationData()
  {
    while (droneConfiguration == null)
    {
      sleep(Config.WAIT_TIMEOUT);
    }
  }

  @Override
  public void onDroneConfiguration(DroneConfiguration configuration)
  {
    droneConfiguration = configuration;
  }

  @Override
  public void onNavData(NavData navData)
  {
    currentNavData = navData;
  }
}