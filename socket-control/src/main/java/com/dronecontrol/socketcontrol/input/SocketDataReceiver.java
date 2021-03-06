package com.dronecontrol.socketcontrol.input;

import com.dronecontrol.socketcontrol.input.data.MovementData;
import com.dronecontrol.socketcontrol.input.data.PilotAction;
import com.dronecontrol.socketcontrol.input.data.PilotData;
import com.dronecontrol.socketcontrol.input.events.MovementDataListener;
import com.dronecontrol.socketcontrol.input.events.PilotActionListener;
import com.dronecontrol.socketcontrol.input.socket.SocketClientDataListener;
import com.google.common.collect.Sets;
import org.codehaus.jackson.map.ObjectMapper;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SocketDataReceiver implements SocketClientDataListener
{
  private final int FAILSAFE_DELAY = 200;

  private final ScheduledExecutorService worker;

  private final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SocketDataReceiver.class);

  private final Runnable movementValidityChecker = new Runnable()
  {
    @Override
    public void run()
    {
      checkDelayForLastCommand();
    }
  };

  private final ObjectMapper objectMapper;

  private Set<MovementDataListener> movementDataListeners;

  private Set<PilotActionListener> pilotActionListeners;

  private long lastCommandTimeStamp;

  @Inject
  public SocketDataReceiver(ObjectMapper objectMapper)
  {
    this.objectMapper = objectMapper;

    movementDataListeners = Sets.newCopyOnWriteArraySet();
    pilotActionListeners = Sets.newCopyOnWriteArraySet();

    worker = Executors.newSingleThreadScheduledExecutor();
    lastCommandTimeStamp = getCurrentTimeStamp();

    startCheckingMovementValidity();
  }

  public void startCheckingMovementValidity()
  {
    worker.scheduleAtFixedRate(movementValidityChecker, FAILSAFE_DELAY, FAILSAFE_DELAY, TimeUnit.MILLISECONDS);
  }

  public void dispose()
  {
    worker.shutdown();
  }

  private void checkDelayForLastCommand()
  {
    if (timeSinceLastCommand() > FAILSAFE_DELAY)
    {
      emitMovementData(MovementData.NO_MOVEMENT);
    }
  }

  private long timeSinceLastCommand()
  {
    return getCurrentTimeStamp() - lastCommandTimeStamp;
  }

  private long getCurrentTimeStamp()
  {
    return new java.util.Date().getTime();
  }

  @Override
  public void OnData(String message)
  {
    PilotData pilotData = getPilotData(message);
    if (pilotData != null)
    {
      emitMovementData(pilotData.getMovementData());
      emitPilotActions(pilotData.getPilotActions());
    }
  }

  private PilotData getPilotData(String message)
  {
    try
    {
      return objectMapper.readValue(message, PilotData.class);
    } catch (IOException e)
    {
      logger.warn(String.format("Error while deserializing movement data '%s': %s", message, e.getMessage()));
      return null;
    }
  }

  private void emitMovementData(MovementData movementData)
  {
    lastCommandTimeStamp = getCurrentTimeStamp();
    invokeMovementDataListeners(movementData);
  }

  private void invokeMovementDataListeners(MovementData movementData)
  {
    for (MovementDataListener listener : movementDataListeners)
    {
      listener.onMovementData(movementData);
    }
  }

  private void emitPilotActions(Collection<PilotAction> pilotActions)
  {
    for (PilotAction pilotAction : pilotActions)
    {
      invokePilotActionListeners(pilotAction);
    }
  }

  private void invokePilotActionListeners(PilotAction pilotAction)
  {
    for (PilotActionListener listener : pilotActionListeners)
    {
      listener.onPilotAction(pilotAction);
    }
  }

  public synchronized void addMovementDataListener(MovementDataListener listener)
  {
    movementDataListeners.add(listener);
  }

  public synchronized void addPilotActionListener(PilotActionListener listener)
  {
    pilotActionListeners.add(listener);
  }
}