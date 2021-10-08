import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

public class AeronMirror {

    public static void main(String[] args) {
        final String channel = "aeron:udp?endpoint=localhost:7777";
        final String message = "PONG";
        final IdleStrategy idle = new SleepingIdleStrategy();
        final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(ByteBuffer.allocate(256));
        try (
                Aeron aeron = Aeron.connect();
                Subscription sub = aeron.addSubscription(channel, 1);
                Publication pub = aeron.addPublication(channel, 2)) {
            while (!pub.isConnected()) {
                idle.idle();
            }

            while (true) {

                //receiving
                FragmentHandler handler = (buffer, offset, length, header) ->
                        buffer.getStringAscii(offset);
                while (sub.poll(handler, 1) <= 0) {
                    idle.idle();
                }

                //sending
                unsafeBuffer.putStringAscii(0, message);
                while (pub.offer(unsafeBuffer) < 0) {
                    idle.idle();
                }
            }
        }
    }
}
