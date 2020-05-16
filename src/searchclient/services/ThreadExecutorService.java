package src.searchclient.services;


import searchclient.AgentThread;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadExecutorService {

    private static ThreadPoolExecutor threadExecutor;

    public static void execute(AgentThread thread) {
        threadExecutor.execute(thread);
    }

    public static void shutdown() {
        threadExecutor.shutdown();
    }

    public static ThreadPoolExecutor getAgentExecutor() {
        return threadExecutor;
    }

    public static void launch(int numberOfAgents) {
        // (ThreadPoolExecutor) Executors.newFixedThreadPool(numberOfAgents, Thread::new)
        threadExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        // Maybe change the keepalivetime
    }
}