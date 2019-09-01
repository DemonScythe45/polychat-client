package club.moddedminecraft.polychat.client.threads;


public abstract class HeartbeatThread {

    private int interval;
    private Thread thread;

    public HeartbeatThread(int interval) {
        this.interval = interval;
        this.thread = new Thread(this::runThread);
    }

    protected abstract void run() throws InterruptedException;

    public void start() {
        this.thread.start();
    }

    public void interrupt() {
        this.thread.interrupt();
    }

    private void runThread() {
        while (true) {
            try {
                run();
                Thread.sleep(this.interval);
            } catch (InterruptedException ignored) {
                System.out.println("Heartbeat thread " + this.getClass().getSimpleName() + "interrupted, stopping...");
            }
        }
    }

}
