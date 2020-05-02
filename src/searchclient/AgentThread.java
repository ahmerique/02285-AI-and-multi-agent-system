package searchclient;

import com.google.common.eventbus.Subscribe;
import searchclient.events.EventTemplate;
import searchclient.services.EventBusService;

public class AgentThread implements Runnable {

    private Agent agent;

    public AgentThread() {
    }

    public AgentThread(Agent agent) {
        this.agent = agent;
    }

    @Override
    public void run() {
        prepareSubscriber();
        // register all events handled by this class
        EventBusService.register(this);
        // System.err.println(Thread.currentThread().getName() + ": Started agent: " + BDIService.getInstance().getAgent().getLabel());
        finishSubscriber();
    }

    private void prepareSubscriber() {
        // update board and have access to all necessary service
        // get the threadLocal instance
        //BDIService bdiService = AgentService.getInstance().getBDIServiceInstance(agent);
        //BDIService.setInstance(bdiService);
        // update BDI level
        //BDIService.getInstance().updateBDILevelService();
    }

    private void finishSubscriber() {
        // close what has to be closed when finish

        // add the agent and BDI back
        /*
        AgentService.getInstance().addAgent(
                agent,
                BDIService.getInstance()
        );
        */
        // agent = null;
    }

    /**
     * The Agency offered a goal - The agents bid on it
     *
     * @param event
     */
    @Subscribe
    public void goalOfferEventSubscriber(EventTemplate event) {
    }

    /**
     * The Agency assigned the agent a goal - The agent solve it
     *
     * @param event
     */
    @Subscribe
    public void goalAssignmentEventSubscriber(EventTemplate event) {
    }
}
