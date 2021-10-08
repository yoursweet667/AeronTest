import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AeronSender {

    public static void main(String[] args) throws InterruptedException {

        final String channel = "aeron:udp?endpoint=localhost:7777";
        final String message = "PING";
        final IdleStrategy idle = new SleepingIdleStrategy();
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(ByteBuffer.allocate(256));

        long numberOfIterations = 1_000_000;
        long i = 0;
        List<Double> latencyList = new ArrayList<>();

        Thread.sleep(100);

        long startTime = System.currentTimeMillis();

        try (
                Aeron aeron = Aeron.connect();
                Subscription sub = aeron.addSubscription(channel, 2);
                Publication pub = aeron.addPublication(channel, 1)) {
            while (!pub.isConnected()) {
                idle.idle();
            }

            while (i < numberOfIterations) {

                double start = System.nanoTime();

                //sending
                unsafeBuffer.putStringAscii(0, message);
                while (pub.offer(unsafeBuffer) < 0) {
                    idle.idle();
                }

                //receiving
                FragmentHandler handler = (buffer, offset, length, header) ->
                        buffer.getStringAscii(offset);
                while (sub.poll(handler, 1) <= 0) {
                    idle.idle();
                }

                double end = System.nanoTime();

                double result = (end - start) / 1000;
                System.out.println(result + " микросекунд.");

                i++;

                latencyList.add(result);

                if (i == numberOfIterations) {
                    long endTime = System.currentTimeMillis();

                    double meanTime = (endTime - startTime);

                    int percentile50 = (int) ((numberOfIterations) * 0.50);
                    int percentile90 = (int) ((numberOfIterations) * 0.90);
                    int percentile99 = (int) ((numberOfIterations) * 0.99);

                    Collections.sort(latencyList);
                    System.out.println("PING-PONG TEST");
                    System.out.println("Количество итераций: " + numberOfIterations);
                    System.out.println("Общее время выполнения: " + meanTime / 1000 + " секунд.");

                    System.out.println("Percentile 50: " + latencyList.get(percentile50) + " микросекунд.");
                    System.out.println("Percentile 90: " + latencyList.get(percentile90) + " микросекунд.");
                    System.out.println("Percentile 99: " + latencyList.get(percentile99) + " микросекунд.");
                    System.out.println("Max latency: " + Collections.max(latencyList) + " микросекунд.");
                }
            }
        }
    }
}
