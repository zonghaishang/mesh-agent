package com.alibaba.mesh.remoting;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A queue of pending writes to a {@link Channel} that is flushed as a single unit.
 */
public class WriteQueue {

    // Dequeue in chunks, so we don't have to acquire the queue's log too often.
    static final int DEQUE_CHUNK_SIZE = 128;

    /**
     * {@link Runnable} used to schedule work onto the tail of the event loop.
     */
    private final Runnable later = new Runnable() {
        @Override
        public void run() {
            flush();
        }
    };

    private final Channel channel;
    private final Queue<QueuedCommand> queue;
    private final AtomicBoolean scheduled = new AtomicBoolean();

    public WriteQueue(Channel channel) {
        this.channel = channel;
        queue = new ConcurrentLinkedQueue<QueuedCommand>();
    }

    /**
     * Schedule a flush on the channel.
     */
    public void scheduleFlush() {
        if (scheduled.compareAndSet(false, true)) {
            // Add the queue to the tail of the event loop so writes will be executed immediately
            // inside the event loop. Note DO NOT do channel.write outside the event loop as
            // it will not wake up immediately without a flush.
            channel.eventLoop().execute(later);
        }
    }

    /**
     * Enqueue a write command on the channel.
     *
     * @param command a write to be executed on the channel.
     * @param flush   true if a flush of the write should be schedule, false if a later call to
     *                enqueue will schedule the flush.
     */
    public ChannelFuture enqueue(QueuedCommand command, boolean flush) {
        return enqueue(command, channel.newPromise(), flush);
    }

    /**
     * Enqueue a write command with a completion listener.
     *
     * @param command a write to be executed on the channel.
     * @param promise to be marked on the completion of the write.
     * @param flush   true if a flush of the write should be schedule, false if a later call to
     *                enqueue will schedule the flush.
     */
    public ChannelFuture enqueue(QueuedCommand command, ChannelPromise promise, boolean flush) {
        // Detect erroneous code that tries to reuse command objects.
        command.promise(promise);
        queue.add(command);
        if (flush) {
            scheduleFlush();
        }
        return promise;
    }

    /**
     * Enqueue the runnable. It is not safe for another thread to queue an Runnable directly to the
     * event loop, because it will be out-of-order with writes. This method allows the Runnable to be
     * processed in-order with writes.
     */
    public void enqueue(Runnable runnable, boolean flush) {
        queue.add(new RunnableCommand(runnable));
        if (flush) {
            scheduleFlush();
        }
    }

    /**
     * Process the queue of commands and dispatch them to the stream. This method is only
     * called in the event loop
     */
    private void flush() {
        try {
            QueuedCommand cmd;
            int i = 0;
            boolean flushedOnce = false;
            while ((cmd = queue.poll()) != null) {
                cmd.run(channel);
                if (++i == DEQUE_CHUNK_SIZE) {
                    i = 0;
                    // Flush each chunk so we are releasing buffers periodically. In theory this loop
                    // might never end as new events are continuously added to the queue, if we never
                    // flushed in that case we would be guaranteed to OOM.
                    channel.flush();
                    flushedOnce = true;
                }
            }
            // Must flush at least once, even if there were no writes.
            if (i != 0 || !flushedOnce) {
                channel.flush();
            }
        } finally {
            // Mark the write as done, if the queue is non-empty after marking trigger a new write.
            scheduled.set(false);
            if (!queue.isEmpty()) {
                scheduleFlush();
            }
        }
    }

    public static class RunnableCommand implements QueuedCommand {
        private final Runnable runnable;

        public RunnableCommand(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public final void promise(ChannelPromise promise) {
            throw new UnsupportedOperationException();
        }

        @Override
        public final ChannelPromise promise() {
            throw new UnsupportedOperationException();
        }

        @Override
        public final void run(Channel channel) {
            runnable.run();
        }
    }

    public abstract static class AbstractQueuedCommand implements QueuedCommand {

        private ChannelPromise promise;

        @Override
        public final void promise(ChannelPromise promise) {
            this.promise = promise;
        }

        @Override
        public final ChannelPromise promise() {
            return promise;
        }

        @Override
        public final void run(Channel channel) {
            channel.write(this, promise);
        }
    }

    /**
     * Simple wrapper type around a command and its optional completion listener.
     */
    public interface QueuedCommand {
        /**
         * Returns the promise beeing notified of the success/failure of the write.
         */
        ChannelPromise promise();

        /**
         * Sets the promise.
         */
        void promise(ChannelPromise promise);

        void run(Channel channel);
    }
}
